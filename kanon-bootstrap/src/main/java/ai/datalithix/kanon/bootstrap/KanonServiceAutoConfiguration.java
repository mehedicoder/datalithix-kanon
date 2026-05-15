package ai.datalithix.kanon.bootstrap;

import ai.datalithix.kanon.agentruntime.service.AgentAdministrationService;
import ai.datalithix.kanon.agentruntime.service.AgentProfileRepository;
import ai.datalithix.kanon.agentruntime.service.DefaultAgentAdministrationService;
import ai.datalithix.kanon.annotation.service.AnnotationNodeVerificationService;
import ai.datalithix.kanon.annotation.service.AnnotationProviderVerificationClient;
import ai.datalithix.kanon.annotation.service.AnnotationNode;
import ai.datalithix.kanon.annotation.service.AnnotationNodeRegistry;
import ai.datalithix.kanon.annotation.service.AnnotationSyncRecordRepository;
import ai.datalithix.kanon.annotation.service.DefaultAnnotationNodeRegistry;
import ai.datalithix.kanon.annotation.service.DefaultAnnotationNodeVerificationService;
import ai.datalithix.kanon.annotation.service.DefaultExternalAnnotationNodeService;
import ai.datalithix.kanon.annotation.service.EnvironmentExternalAnnotationSecretResolver;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeRepository;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeService;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeUsageGuard;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationSecretResolver;
import ai.datalithix.kanon.annotation.service.InMemoryAnnotationSyncRecordRepository;
import ai.datalithix.kanon.annotation.service.InMemoryExternalAnnotationNodeRepository;
import ai.datalithix.kanon.annotation.service.InMemoryMediaAssetRepository;
import ai.datalithix.kanon.annotation.service.InMemoryVideoAnnotationRepository;
import ai.datalithix.kanon.annotation.service.MediaAssetRepository;
import ai.datalithix.kanon.annotation.service.VideoAnnotationRepository;
import ai.datalithix.kanon.annotation.service.NoopExternalAnnotationNodeUsageGuard;
import ai.datalithix.kanon.annotation.service.cvat.CvatVerificationClient;
import ai.datalithix.kanon.annotation.service.labelstudio.LabelStudioVerificationClient;
import ai.datalithix.kanon.activelearning.service.ActiveLearningCycleRepository;
import ai.datalithix.kanon.activelearning.service.InMemoryActiveLearningCycleRepository;
import ai.datalithix.kanon.bootstrap.storage.ObjectStorageConfigurationProperties;
import ai.datalithix.kanon.bootstrap.storage.S3CompatibleObjectStorageClient;
import ai.datalithix.kanon.common.storage.ObjectStorageClient;
import ai.datalithix.kanon.dataset.service.DatasetRepository;
import ai.datalithix.kanon.dataset.service.InMemoryDatasetRepository;
import ai.datalithix.kanon.evidence.service.EvidenceLedger;
import ai.datalithix.kanon.common.storage.ObjectStorageObject;
import ai.datalithix.kanon.common.storage.ObjectStoragePutRequest;
import ai.datalithix.kanon.ingestion.service.V1ConnectorProperties;
import java.net.URI;
import java.time.Duration;
import ai.datalithix.kanon.ingestion.service.ConnectorConfigurationRepository;
import ai.datalithix.kanon.ingestion.service.ConnectorHealthRepository;
import ai.datalithix.kanon.ingestion.service.IngestionBatchRepository;
import ai.datalithix.kanon.ingestion.service.InMemoryConnectorConfigurationRepository;
import ai.datalithix.kanon.ingestion.service.InMemoryConnectorHealthRepository;
import ai.datalithix.kanon.ingestion.service.InMemoryIngestionBatchRepository;
import ai.datalithix.kanon.ingestion.service.InMemorySourceDescriptorRepository;
import ai.datalithix.kanon.ingestion.service.InMemorySourceTraceRepository;
import ai.datalithix.kanon.ingestion.service.SourceDescriptorRepository;
import ai.datalithix.kanon.ingestion.service.SourceTraceRepository;
import ai.datalithix.kanon.modelregistry.service.InMemoryModelRegistryRepository;
import ai.datalithix.kanon.modelregistry.service.ModelRegistryRepository;
import ai.datalithix.kanon.policy.security.BreakGlassGrantRepository;
import ai.datalithix.kanon.policy.security.InMemoryBreakGlassGrantRepository;
import ai.datalithix.kanon.policy.security.InMemorySecurityAuditEventRepository;
import ai.datalithix.kanon.policy.security.SecurityAuditEventRepository;
import ai.datalithix.kanon.training.service.ComputeBackendAdapter;
import ai.datalithix.kanon.training.service.DefaultTrainingOrchestrationService;
import ai.datalithix.kanon.training.service.InMemoryTrainingJobRepository;
import ai.datalithix.kanon.training.service.TrainingJobRepository;
import ai.datalithix.kanon.training.service.TrainingOrchestrationService;
import java.net.http.HttpClient;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({ObjectStorageConfigurationProperties.class, V1ConnectorProperties.class})
public class KanonServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AgentAdministrationService.class)
    public AgentAdministrationService agentAdministrationService(AgentProfileRepository agentProfileRepository) {
        return new DefaultAgentAdministrationService(agentProfileRepository);
    }

    @Bean
    public HttpClient annotationVerificationHttpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AnnotationProviderVerificationClient labelStudioVerificationClient(HttpClient annotationVerificationHttpClient) {
        return new LabelStudioVerificationClient(annotationVerificationHttpClient);
    }

    @Bean
    public AnnotationProviderVerificationClient cvatVerificationClient(HttpClient annotationVerificationHttpClient) {
        return new CvatVerificationClient(annotationVerificationHttpClient);
    }

    @Bean
    @Primary
    public ExternalAnnotationSecretResolver externalAnnotationSecretResolver() {
        return new EnvironmentExternalAnnotationSecretResolver();
    }

    @Bean
    @ConditionalOnMissingBean(ExternalAnnotationNodeRepository.class)
    public ExternalAnnotationNodeRepository externalAnnotationNodeRepository() {
        return new InMemoryExternalAnnotationNodeRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ExternalAnnotationNodeUsageGuard.class)
    public ExternalAnnotationNodeUsageGuard externalAnnotationNodeUsageGuard() {
        return new NoopExternalAnnotationNodeUsageGuard();
    }

    @Bean
    @ConditionalOnMissingBean(AnnotationNodeVerificationService.class)
    public AnnotationNodeVerificationService annotationNodeVerificationService(
            ExternalAnnotationSecretResolver externalAnnotationSecretResolver,
            List<AnnotationProviderVerificationClient> providerVerificationClients,
            HttpClient annotationVerificationHttpClient
    ) {
        return new DefaultAnnotationNodeVerificationService(
                externalAnnotationSecretResolver,
                providerVerificationClients,
                annotationVerificationHttpClient
        );
    }

    @Bean
    @ConditionalOnMissingBean(AnnotationNodeRegistry.class)
    public AnnotationNodeRegistry annotationNodeRegistry(List<AnnotationNode> annotationNodes) {
        return new DefaultAnnotationNodeRegistry(annotationNodes);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalAnnotationNodeService.class)
    public ExternalAnnotationNodeService externalAnnotationNodeService(
            ExternalAnnotationNodeRepository externalAnnotationNodeRepository,
            AnnotationNodeVerificationService annotationNodeVerificationService,
            ExternalAnnotationNodeUsageGuard externalAnnotationNodeUsageGuard,
            EvidenceLedger evidenceLedger
    ) {
        return new DefaultExternalAnnotationNodeService(
                externalAnnotationNodeRepository,
                annotationNodeVerificationService,
                externalAnnotationNodeUsageGuard,
                evidenceLedger
        );
    }

    @Bean
    @ConditionalOnMissingBean(DatasetRepository.class)
    public DatasetRepository datasetRepository() {
        return new InMemoryDatasetRepository();
    }

    @Bean
    @ConditionalOnMissingBean(TrainingJobRepository.class)
    public TrainingJobRepository trainingJobRepository() {
        return new InMemoryTrainingJobRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ModelRegistryRepository.class)
    public ModelRegistryRepository modelRegistryRepository() {
        return new InMemoryModelRegistryRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ActiveLearningCycleRepository.class)
    public ActiveLearningCycleRepository activeLearningCycleRepository() {
        return new InMemoryActiveLearningCycleRepository();
    }

    @Bean
    @ConditionalOnMissingBean(BreakGlassGrantRepository.class)
    public BreakGlassGrantRepository breakGlassGrantRepository() {
        return new InMemoryBreakGlassGrantRepository();
    }

    @Bean
    @ConditionalOnMissingBean(TrainingOrchestrationService.class)
    public TrainingOrchestrationService trainingOrchestrationService(
            TrainingJobRepository trainingJobRepository,
            ai.datalithix.kanon.dataset.service.DatasetRepository datasetRepository,
            EvidenceLedger evidenceLedger,
            java.util.List<ComputeBackendAdapter> adapters
    ) {
        return new DefaultTrainingOrchestrationService(trainingJobRepository, datasetRepository, evidenceLedger, adapters);
    }

    @Bean
    @ConditionalOnMissingBean(SourceTraceRepository.class)
    public SourceTraceRepository sourceTraceRepository() {
        return new InMemorySourceTraceRepository();
    }

    @Bean
    @ConditionalOnMissingBean(SourceDescriptorRepository.class)
    public SourceDescriptorRepository sourceDescriptorRepository() {
        return new InMemorySourceDescriptorRepository();
    }

    @Bean
    @ConditionalOnMissingBean(IngestionBatchRepository.class)
    public IngestionBatchRepository ingestionBatchRepository() {
        return new InMemoryIngestionBatchRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ConnectorConfigurationRepository.class)
    public ConnectorConfigurationRepository connectorConfigurationRepository() {
        return new InMemoryConnectorConfigurationRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ConnectorHealthRepository.class)
    public ConnectorHealthRepository connectorHealthRepository() {
        return new InMemoryConnectorHealthRepository();
    }

    @Bean
    @ConditionalOnMissingBean(AnnotationSyncRecordRepository.class)
    public AnnotationSyncRecordRepository annotationSyncRecordRepository() {
        return new InMemoryAnnotationSyncRecordRepository();
    }

    @Bean
    @ConditionalOnMissingBean(VideoAnnotationRepository.class)
    public VideoAnnotationRepository videoAnnotationRepository() {
        return new InMemoryVideoAnnotationRepository();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityAuditEventRepository.class)
    public SecurityAuditEventRepository securityAuditEventRepository() {
        return new InMemorySecurityAuditEventRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "kanon.object-storage.enabled", havingValue = "true")
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient s3ObjectStorageClient(ObjectStorageConfigurationProperties props) {
        return new S3CompatibleObjectStorageClient(props);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    public ObjectStorageClient noopObjectStorageClient() {
        return new ObjectStorageClient() {
            @Override
            public ObjectStorageObject put(ObjectStoragePutRequest request) {
                throw new UnsupportedOperationException("Object storage is not configured/disabled");
            }
            @Override
            public ObjectStorageObject metadata(String tenantId, String objectKey) {
                return null;
            }
            @Override
            public URI presignedReadUrl(String tenantId, String objectKey, Duration ttl) {
                throw new UnsupportedOperationException("Object storage is not configured/disabled");
            }
            @Override
            public URI presignedWriteUrl(String tenantId, String objectKey, Duration ttl) {
                throw new UnsupportedOperationException("Object storage is not configured/disabled");
            }
            @Override
            public void deleteMarker(String tenantId, String objectKey) {
            }
            @Override
            public boolean verifyChecksum(String tenantId, String objectKey, String checksumSha256) {
                return true;
            }
        };
    }
}
