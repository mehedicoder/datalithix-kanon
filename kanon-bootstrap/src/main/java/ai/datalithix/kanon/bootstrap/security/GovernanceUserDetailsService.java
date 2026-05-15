package ai.datalithix.kanon.bootstrap.security;

import ai.datalithix.kanon.tenant.model.GovernanceStatus;
import ai.datalithix.kanon.tenant.model.Membership;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.service.MembershipRepository;
import ai.datalithix.kanon.tenant.service.UserAccountRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GovernanceUserDetailsService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;

    public GovernanceUserDetailsService(
            UserAccountRepository userAccountRepository,
            MembershipRepository membershipRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        List<SimpleGrantedAuthority> authorities = membershipRepository.findMembershipsByUserId(userAccount.userId()).stream()
                .map(Membership::roleKeys)
                .flatMap(java.util.Set::stream)
                .distinct()
                .map(roleKey -> new SimpleGrantedAuthority("ROLE_" + roleKey))
                .toList();

        return User.withUsername(userAccount.username())
                .password(userAccount.passwordHash())
                .authorities(authorities)
                .disabled(userAccount.status() != GovernanceStatus.ACTIVE)
                .build();
    }
}
