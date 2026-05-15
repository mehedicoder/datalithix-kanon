package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.Tenant;
import java.util.List;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(String tenantId);
    List<Tenant> findAll();
}
