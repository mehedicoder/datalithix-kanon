package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.Organization;
import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Optional<Organization> findById(String tenantId, String organizationId);
    List<Organization> findByTenantId(String tenantId);
}
