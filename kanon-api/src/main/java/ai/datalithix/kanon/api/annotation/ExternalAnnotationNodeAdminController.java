package ai.datalithix.kanon.api.annotation;

import ai.datalithix.kanon.annotation.model.AnnotationNodeVerificationResult;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationNode;
import ai.datalithix.kanon.annotation.model.ExternalAnnotationProviderType;
import ai.datalithix.kanon.annotation.service.ExternalAnnotationNodeService;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/admin/annotation-nodes")
public class ExternalAnnotationNodeAdminController {
    private final ExternalAnnotationNodeService nodeService;
    private final CurrentUserContextService currentUserContextService;

    public ExternalAnnotationNodeAdminController(
            ExternalAnnotationNodeService nodeService,
            CurrentUserContextService currentUserContextService
    ) {
        this.nodeService = nodeService;
        this.currentUserContextService = currentUserContextService;
    }

    @GetMapping
    public List<ExternalAnnotationNode> list() {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireReadPermission(context);
        return nodeService.list(context.activeTenantId(), platformScoped(context));
    }

    @PostMapping
    public ExternalAnnotationNode create(@RequestBody CreateExternalAnnotationNodeRequest request) {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireManagePermission(context);
        try {
            return nodeService.create(
                    context.activeTenantId(),
                    platformScoped(context),
                    request.tenantId(),
                    request.displayName(),
                    request.providerType(),
                    request.baseUrl(),
                    request.secretRef(),
                    request.storageBucket(),
                    context.username()
            );
        } catch (SecurityException exception) {
            throw new ResponseStatusException(FORBIDDEN, "Annotation node access denied");
        }
    }

    @PutMapping("/{nodeId}")
    public ExternalAnnotationNode update(@PathVariable String nodeId, @RequestBody UpdateExternalAnnotationNodeRequest request) {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireManagePermission(context);
        try {
            return nodeService.update(
                    context.activeTenantId(),
                    platformScoped(context),
                    nodeId,
                    request.displayName(),
                    request.baseUrl(),
                    request.secretRef(),
                    request.storageBucket(),
                    request.enabled(),
                    context.username()
            );
        } catch (SecurityException exception) {
            throw new ResponseStatusException(FORBIDDEN, "Annotation node access denied");
        }
    }

    @PostMapping("/{nodeId}/test")
    public AnnotationNodeVerificationResult testConnection(@PathVariable String nodeId) {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireTestPermission(context);
        try {
            return nodeService.testConnection(context.activeTenantId(), platformScoped(context), nodeId, context.username());
        } catch (SecurityException exception) {
            throw new ResponseStatusException(FORBIDDEN, "Annotation node access denied");
        }
    }

    @DeleteMapping("/{nodeId}")
    public void delete(@PathVariable String nodeId) {
        CurrentUserContext context = currentUserContextService.currentUser();
        requireManagePermission(context);
        try {
            nodeService.delete(context.activeTenantId(), platformScoped(context), nodeId, context.username());
        } catch (SecurityException exception) {
            throw new ResponseStatusException(FORBIDDEN, "Annotation node access denied");
        }
    }

    private static void requireReadPermission(CurrentUserContext context) {
        if (hasAny(context, "platform.config.manage", "platform.config.read", "tenant.config.manage", "tenant.config.read")) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Annotation node access denied");
    }

    private static void requireManagePermission(CurrentUserContext context) {
        if (hasAny(context, "platform.config.manage", "tenant.config.manage")) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Annotation node administration denied");
    }

    private static void requireTestPermission(CurrentUserContext context) {
        if (hasAny(context, "platform.config.manage", "tenant.config.manage", "workspace.model.test")) {
            return;
        }
        throw new ResponseStatusException(FORBIDDEN, "Annotation node test denied");
    }

    private static boolean hasAny(CurrentUserContext context, String... permissions) {
        for (String permission : permissions) {
            if (context.permissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    private static boolean platformScoped(CurrentUserContext context) {
        return hasAny(context, "platform.config.manage", "platform.config.read");
    }

    public record CreateExternalAnnotationNodeRequest(
            String tenantId,
            String displayName,
            ExternalAnnotationProviderType providerType,
            String baseUrl,
            String secretRef,
            String storageBucket
    ) {
    }

    public record UpdateExternalAnnotationNodeRequest(
            String displayName,
            String baseUrl,
            String secretRef,
            String storageBucket,
            boolean enabled
    ) {
    }
}
