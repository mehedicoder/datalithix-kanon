package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.Membership;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository {
    Membership save(Membership membership);
    Optional<Membership> findByMembershipId(String membershipId);
    List<Membership> findMembershipsByUserId(String userId);
    List<Membership> findByWorkspaceId(String tenantId, String organizationId, String workspaceId);
    List<Membership> findAllMemberships();
}
