package ai.datalithix.kanon.tenant.service;

import ai.datalithix.kanon.tenant.model.UserAccount;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {
    UserAccount save(UserAccount userAccount);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByUserId(String userId);
    List<UserAccount> findAllUsers();
}
