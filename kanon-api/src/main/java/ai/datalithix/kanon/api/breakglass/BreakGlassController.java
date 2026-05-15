package ai.datalithix.kanon.api.breakglass;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.PageSpec;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import ai.datalithix.kanon.common.security.BreakGlassGrant;
import ai.datalithix.kanon.policy.security.BreakGlassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/break-glass")
@Tag(name = "Break Glass", description = "Emergency break-glass access grant management endpoints")
public class BreakGlassController {
    private final BreakGlassService breakGlassService;

    public BreakGlassController(BreakGlassService breakGlassService) {
        this.breakGlassService = breakGlassService;
    }

    @PostMapping("/grants")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "Request a break-glass grant",
            description = "Creates a new break-glass access request for a user with specified permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant requested"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already has active grant")
    })
    public BreakGlassGrant requestGrant(@RequestBody RequestGrantRequest request) {
        return breakGlassService.request(
                request.tenantId(), request.userId(), request.reason(),
                request.requestedPermissions(), request.expiresAt(), request.actorId());
    }

    @PostMapping("/grants/{grantId}/approve")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "Approve a break-glass grant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grant approved"),
            @ApiResponse(responseCode = "404", description = "Grant not found")
    })
    public BreakGlassGrant approveGrant(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Grant ID") @PathVariable String grantId,
            @RequestBody ApproveDenyRequest request) {
        return breakGlassService.approve(grantId, tenantId, request.actorId());
    }

    @PostMapping("/grants/{grantId}/deny")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "Deny a break-glass grant")
    public BreakGlassGrant denyGrant(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Grant ID") @PathVariable String grantId,
            @RequestBody DenyGrantRequest request) {
        return breakGlassService.deny(grantId, tenantId, request.actorId(), request.reason());
    }

    @PostMapping("/grants/{grantId}/revoke")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "Revoke an approved break-glass grant")
    public BreakGlassGrant revokeGrant(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Grant ID") @PathVariable String grantId,
            @RequestBody RevokeGrantRequest request) {
        return breakGlassService.revoke(grantId, tenantId, request.actorId(), request.reason());
    }

    @GetMapping("/grants/active")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Find active break-glass grant for a user")
    public BreakGlassGrant findActiveGrant(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        return breakGlassService.findActiveGrant(tenantId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No active grant for user: " + userId));
    }

    @GetMapping("/grants/{grantId}")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN", "ROLE_DOMAIN_MANAGER"})
    @Operation(summary = "Get a break-glass grant by ID")
    public BreakGlassGrant getGrant(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Grant ID") @PathVariable String grantId) {
        return breakGlassService.findById(tenantId, grantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Grant not found: " + grantId));
    }

    @GetMapping("/grants")
    @Secured({"ROLE_PLATFORM_ADMIN", "ROLE_TENANT_ADMIN"})
    @Operation(summary = "List break-glass grants for a tenant")
    public PageResult<BreakGlassGrant> listGrants(
            @Parameter(description = "Tenant ID") @RequestParam(defaultValue = "demo-tenant") String tenantId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return breakGlassService.findPage(new QuerySpec(tenantId, new PageSpec(page, size, null, SortDirection.ASC), null, null));
    }

    @Schema(description = "Request payload to create a new break-glass grant")
    public record RequestGrantRequest(
            @Schema(description = "Tenant ID", example = "demo-tenant") String tenantId,
            @Schema(description = "User ID to grant access to", example = "alice") String userId,
            @Schema(description = "Reason for break-glass access") String reason,
            @Schema(description = "Set of permission keys to grant", example = "[\"EVIDENCE_VIEW\",\"SOURCE_VIEW\"]") Set<String> requestedPermissions,
            @Schema(description = "Grant expiry timestamp") java.time.Instant expiresAt,
            @Schema(description = "Actor ID requesting the grant") String actorId) {}

    @Schema(description = "Request payload for approve/deny actions by actor")
    public record ApproveDenyRequest(
            @Schema(description = "Actor ID performing the action") String actorId) {}

    @Schema(description = "Request payload for denying a grant")
    public record DenyGrantRequest(
            @Schema(description = "Actor ID performing the denial") String actorId,
            @Schema(description = "Reason for denial") String reason) {}

    @Schema(description = "Request payload for revoking a grant")
    public record RevokeGrantRequest(
            @Schema(description = "Actor ID performing the revocation") String actorId,
            @Schema(description = "Reason for revocation") String reason) {}
}
