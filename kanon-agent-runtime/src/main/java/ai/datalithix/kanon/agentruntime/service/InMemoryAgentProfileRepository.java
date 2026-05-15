package ai.datalithix.kanon.agentruntime.service;

import ai.datalithix.kanon.agentruntime.model.AgentProfile;
import ai.datalithix.kanon.agentruntime.model.AgentType;
import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;
import ai.datalithix.kanon.common.model.SortDirection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryAgentProfileRepository implements AgentProfileRepository {
    private final Map<Key, AgentProfile> agentProfiles = new ConcurrentHashMap<>();

    @Override
    public AgentProfile save(AgentProfile agentProfile) {
        agentProfiles.put(new Key(agentProfile.tenantId(), agentProfile.agentId()), agentProfile);
        return agentProfile;
    }

    @Override
    public Optional<AgentProfile> findById(String tenantId, String agentId) {
        return Optional.ofNullable(agentProfiles.get(new Key(tenantId, agentId)));
    }

    @Override
    public void deleteById(String tenantId, String agentId) {
        agentProfiles.remove(new Key(tenantId, agentId));
    }

    @Override
    public List<AgentProfile> findEnabledByTenant(String tenantId) {
        return agentProfiles.values().stream()
                .filter(agentProfile -> agentProfile.tenantId().equals(tenantId))
                .filter(AgentProfile::enabled)
                .sorted(Comparator.comparing(AgentProfile::name))
                .toList();
    }

    @Override
    public List<AgentProfile> findEnabledByType(String tenantId, AgentType agentType) {
        return findEnabledByTenant(tenantId).stream()
                .filter(agentProfile -> agentProfile.agentType() == agentType)
                .toList();
    }

    @Override
    public PageResult<AgentProfile> findPage(QuerySpec query) {
        List<AgentProfile> filtered = agentProfiles.values().stream()
                .filter(agentProfile -> agentProfile.tenantId().equals(query.tenantId()))
                .filter(agentProfile -> matchesDimensions(agentProfile, query.dimensions()))
                .sorted(comparator(query.page().sortBy(), query.page().sortDirection() == SortDirection.DESC))
                .toList();
        int fromIndex = Math.min(query.page().pageNumber() * query.page().pageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + query.page().pageSize(), filtered.size());
        return new PageResult<>(
                filtered.subList(fromIndex, toIndex),
                query.page().pageNumber(),
                query.page().pageSize(),
                filtered.size()
        );
    }

    private static boolean matchesDimensions(AgentProfile agentProfile, Map<String, String> dimensions) {
        return dimensions.entrySet().stream().allMatch(entry -> switch (entry.getKey()) {
            case "agentType" -> agentProfile.agentType().name().equals(entry.getValue());
            case "status" -> agentProfile.status().name().equals(entry.getValue());
            case "enabled" -> Boolean.toString(agentProfile.enabled()).equals(entry.getValue());
            default -> true;
        });
    }

    private static Comparator<AgentProfile> comparator(String sortBy, boolean descending) {
        Comparator<AgentProfile> comparator = switch (sortBy == null ? "" : sortBy) {
            case "agentType" -> Comparator.comparing(agentProfile -> agentProfile.agentType().name());
            case "status" -> Comparator.comparing(agentProfile -> agentProfile.status().name());
            case "updatedAt" -> Comparator.comparing(agentProfile -> agentProfile.audit().updatedAt());
            default -> Comparator.comparing(AgentProfile::name);
        };
        return descending ? comparator.reversed() : comparator;
    }

    private record Key(String tenantId, String agentId) {}
}
