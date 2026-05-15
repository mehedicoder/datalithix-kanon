package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.airouting.model.ChatModelRoute;
import ai.datalithix.kanon.airouting.model.ModelExecutionPolicy;
import ai.datalithix.kanon.airouting.model.ModelInvocationRequest;
import ai.datalithix.kanon.airouting.model.ModelInvocationResult;
import ai.datalithix.kanon.airouting.model.ModelInvocationStatus;
import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.airouting.service.ModelInvocationService;
import ai.datalithix.kanon.airouting.service.ModelProfileRepository;
import ai.datalithix.kanon.airouting.service.ModelRouter;
import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.model.AuditMetadata;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import ai.datalithix.kanon.common.security.AccessPurpose;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import com.vaadin.flow.component.UI;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelAdminViewTest {
    @BeforeEach
    void setUp() {
        UI.setCurrent(new UI());
        UI.getCurrent().setLocale(java.util.Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void exposesTestAndDryRunActionsForModelOperators() {
        ModelAdminView view = view(Set.of("workspace.model.test"));

        var actions = view.createModelActions(profile("primary", "Primary", 20));

        assertEquals(2, actions.getComponentCount());
    }

    @Test
    void keepsConnectionAndRoutingActionsOutOfReadOnlyRows() {
        ModelAdminView view = view(Set.of("workspace.model-route.read"));

        var actions = view.createModelActions(profile("primary", "Primary", 20));

        assertEquals(1, actions.getComponentCount());
    }

    @Test
    void buildsAuditableTestConnectionRequestForActiveTenant() {
        ModelAdminView view = view(Set.of("workspace.model.test"));

        ModelInvocationRequest request = view.testConnectionRequest(profile("primary", "Primary", 20), context(Set.of()));

        assertEquals("tenant-1", request.tenantId());
        assertEquals("primary", request.profileKey());
        assertEquals(AccessPurpose.MODEL_INVOCATION, request.accessContext().purpose());
        assertTrue(request.parameters().get("prompt").contains("Connection successful"));
    }

    @Test
    void rendersDryRunRouteDecisionForSelectedPrimaryModel() {
        FakeModelProfileRepository repository = new FakeModelProfileRepository(List.of(
                profile("primary", "Primary", 20),
                profile("fallback", "Fallback", 10)
        ));
        ModelAdminView view = new ModelAdminView(provider(repository), provider(new FakeInvocationService()), provider(new FakeRouter()), contextService(Set.of("workspace.model.test")));
        TenantContext tenantContext = new TenantContext("tenant-1", null, "US", null, true, false, Set.of());
        TaskDescriptor task = new TaskDescriptor(AiTaskType.REASONING, "case-1", "input", "1.0", true);

        String result = view.dryRunResult(
                profile("primary", "Primary", 20),
                tenantContext,
                task,
                new ChatModelRoute("primary", "fallback", "priority match")
        );

        assertTrue(result.contains("Primary Profile: primary"));
        assertTrue(result.contains("Fallback Profile: fallback"));
        assertTrue(result.contains("This model is the PRIMARY choice"));
    }

    private static ModelAdminView view(Set<String> permissions) {
        return new ModelAdminView(
                provider(new FakeModelProfileRepository(List.of(profile("primary", "Primary", 20)))),
                provider(new FakeInvocationService()),
                provider(new FakeRouter()),
                contextService(permissions)
        );
    }

    private static CurrentUserContextService contextService(Set<String> permissions) {
        return () -> context(permissions);
    }

    private static CurrentUserContext context(Set<String> permissions) {
        return new CurrentUserContext(
                "user-1",
                "operator",
                "tenant-1",
                "org-1",
                "workspace-1",
                Set.of(),
                permissions,
                List.of()
        );
    }

    private static <T> ObjectProvider<T> provider(T instance) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return instance;
            }

            @Override
            public T getIfAvailable() {
                return instance;
            }

            @Override
            public T getIfUnique() {
                return instance;
            }

            @Override
            public T getObject() {
                return instance;
            }

            @Override
            public Iterator<T> iterator() {
                return List.of(instance).iterator();
            }

            @Override
            public Stream<T> stream() {
                return Stream.of(instance);
            }

            @Override
            public Stream<T> orderedStream() {
                return Stream.of(instance);
            }
        };
    }

    private static ModelProfile profile(String profileKey, String name, int priority) {
        return new ModelProfile(
                profileKey,
                "Ollama",
                "LOCAL_SERVER",
                "llama3",
                name,
                "http://localhost:11434",
                true,
                false,
                true,
                Set.of(AiTaskType.REASONING),
                "LOW",
                "LOW",
                "LOCAL",
                Set.of("test"),
                true,
                "HEALTHY",
                null,
                priority,
                new ModelExecutionPolicy(
                        new ExecutionControls(Duration.ofSeconds(5), 1, 1, 60),
                        new RetryPolicy(1, Duration.ZERO, Duration.ZERO),
                        null,
                        true,
                        true,
                        true
                ),
                new AuditMetadata(Instant.parse("2026-04-18T00:00:00Z"), "test@tenant-1", Instant.parse("2026-04-18T00:00:00Z"), "test@tenant-1", 1)
        );
    }

    private static class FakeModelProfileRepository implements ModelProfileRepository {
        private final List<ModelProfile> profiles;

        private FakeModelProfileRepository(List<ModelProfile> profiles) {
            this.profiles = profiles;
        }

        @Override
        public ModelProfile save(ModelProfile modelProfile) {
            return modelProfile;
        }

        @Override
        public Optional<ModelProfile> findByProfileKey(String tenantId, String profileKey) {
            return profiles.stream().filter(profile -> profile.profileKey().equals(profileKey)).findFirst();
        }

        @Override
        public List<ModelProfile> findEnabledByTenant(String tenantId) {
            return profiles.stream().filter(ModelProfile::enabled).toList();
        }

        @Override
        public PageResult<ModelProfile> findPage(QuerySpec query) {
            return new PageResult<>(profiles, query.page().pageNumber(), query.page().pageSize(), profiles.size());
        }
    }

    private static class FakeInvocationService implements ModelInvocationService {
        @Override
        public ModelInvocationResult invoke(ModelInvocationRequest request) {
            return new ModelInvocationResult(
                    request.invocationId(),
                    request.tenantId(),
                    request.profileKey(),
                    ModelInvocationStatus.COMPLETED,
                    "ok",
                    "ok",
                    null,
                    Duration.ofMillis(1),
                    null,
                    Map.of(),
                    Instant.now()
            );
        }
    }

    private static class FakeRouter implements ModelRouter {
        @Override
        public ChatModelRoute resolve(TenantContext tenantContext, TaskDescriptor taskDescriptor) {
            return new ChatModelRoute("primary", "fallback", "priority match");
        }
    }
}
