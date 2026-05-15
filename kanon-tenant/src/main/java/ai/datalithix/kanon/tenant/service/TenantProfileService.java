package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.TenantProfile;
import java.util.Optional;

public interface TenantProfileService {
    Optional<TenantProfile> findByTenantId(String tenantId);
}
