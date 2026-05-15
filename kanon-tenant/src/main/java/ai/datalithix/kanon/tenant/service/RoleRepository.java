package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findByKey(String roleKey);
    List<Role> findByKeys(Collection<String> roleKeys);
    List<Role> findAllRoles();
}
