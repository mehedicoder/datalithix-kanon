package ai.datalithix.kanon.airouting.service;

import ai.datalithix.kanon.airouting.model.ModelProfile;
import java.util.Optional;

public interface ModelRegistry {
    Optional<ModelProfile> findByProfileKey(String profileKey);
}
