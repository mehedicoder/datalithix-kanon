package ai.datalithix.kanon.agentruntime.service;

import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.agentruntime.model.AgentType;
import ai.datalithix.kanon.common.service.PagedQueryPort;
import java.util.List;
import java.util.Optional;

public interface AgentProfileRepository extends PagedQueryPort<AgentProfile> {
    AgentProfile save(AgentProfile agentProfile);

    Optional<AgentProfile> findById(String tenantId, String agentId);

    void deleteById(String tenantId, String agentId);

    List<AgentProfile> findEnabledByTenant(String tenantId);

    List<AgentProfile> findEnabledByType(String tenantId, AgentType agentType);
}
