package ai.datalithix.kanon.domain.service;

import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.domain.model.DomainDefinition;
import java.util.Optional;

public interface DomainRegistry {
    Optional<DomainDefinition> findByType(DomainType domainType);
}
