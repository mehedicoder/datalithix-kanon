package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelProfile;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface ModelProfileRepository extends PagedQueryPort<ModelProfile> {
    ModelProfile save(ModelProfile modelProfile);

    Optional<ModelProfile> findByProfileKey(String tenantId, String profileKey);

    List<ModelProfile> findEnabledByTenant(String tenantId);
}
