package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DefaultModelRouter implements ModelRouter {
    
    private final ModelProfileRepository modelProfileRepository;
    
    public DefaultModelRouter(ModelProfileRepository modelProfileRepository) {
        this.modelProfileRepository = modelProfileRepository;
    }
    
    @Override
    public ChatModelRoute resolve(TenantContext tenantContext, TaskDescriptor taskDescriptor) {
        // Get all enabled models for the tenant
        List<ModelProfile> enabledModels = modelProfileRepository.findEnabledByTenant(tenantContext.tenantId());
        
        if (enabledModels.isEmpty()) {
            return new ChatModelRoute(null, null, "No enabled models found for tenant");
        }
        
        // Filter models that support the required task type
        List<ModelProfile> capableModels = enabledModels.stream()
            .filter(model -> model.taskCapabilities().contains(taskDescriptor.taskType()))
            .toList();
        
        if (capableModels.isEmpty()) {
            // Fallback: use any enabled model
            capableModels = enabledModels;
        }
        
        // Apply tenant preferences
        List<ModelProfile> preferredModels = applyTenantPreferences(capableModels, tenantContext);
        
        // Sort by priority (higher priority first)
        List<ModelProfile> sortedModels = preferredModels.stream()
            .sorted(Comparator.comparingInt(ModelProfile::priority).reversed())
            .toList();
        
        // Select primary and fallback
        String primaryKey = sortedModels.isEmpty() ? null : sortedModels.get(0).profileKey();
        String fallbackKey = sortedModels.size() > 1 ? sortedModels.get(1).profileKey() : null;
        
        // Build routing reason
        String reason = buildRoutingReason(taskDescriptor, tenantContext, sortedModels);
        
        return new ChatModelRoute(primaryKey, fallbackKey, reason);
    }
    
    private List<ModelProfile> applyTenantPreferences(List<ModelProfile> models, TenantContext tenantContext) {
        if (tenantContext.preferLocalModels()) {
            // Prefer local models
            List<ModelProfile> localModels = models.stream()
                .filter(ModelProfile::local)
                .toList();
            
            if (!localModels.isEmpty()) {
                return localModels;
            }
        }
        
        if (!tenantContext.allowCloudModels()) {
            // Filter out cloud models (keep only local)
            return models.stream()
                .filter(ModelProfile::local)
                .toList();
        }
        
        return models;
    }
    
    private String buildRoutingReason(TaskDescriptor taskDescriptor, TenantContext tenantContext, List<ModelProfile> selectedModels) {
        if (selectedModels.isEmpty()) {
            return "No suitable models available";
        }
        
        StringBuilder reason = new StringBuilder();
        reason.append("Task: ").append(taskDescriptor.taskType());
        
        if (tenantContext.preferLocalModels()) {
            reason.append(", Local-first policy");
        }
        
        if (!tenantContext.allowCloudModels()) {
            reason.append(", Cloud models disabled");
        }
        
        ModelProfile primary = selectedModels.get(0);
        reason.append(" → Selected: ").append(primary.modelName())
              .append(" (priority: ").append(primary.priority()).append(")");
        
        return reason.toString();
    }
}
