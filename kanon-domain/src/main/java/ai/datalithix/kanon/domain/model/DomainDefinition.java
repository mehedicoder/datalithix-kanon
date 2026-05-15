package ai.datalithix.kanon.domain.model;

import ai.datalithix.kanon.common.DomainType;
import java.util.List;

public record DomainDefinition(DomainType type, String name, List<String> entities, List<String> rules, List<String> agentKeys) {}
