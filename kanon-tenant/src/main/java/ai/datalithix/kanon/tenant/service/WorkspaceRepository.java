package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.Workspace;
import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {
    Workspace save(Workspace workspace);
    Optional<Workspace> findById(String tenantId, String organizationId, String workspaceId);
    List<Workspace> findByOrganizationId(String tenantId, String organizationId);
}
