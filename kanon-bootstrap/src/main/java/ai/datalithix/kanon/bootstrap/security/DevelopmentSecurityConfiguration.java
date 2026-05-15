package ai.datalithix.kanon.bootstrap.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
@Profile("!test")
public class DevelopmentSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/VAADIN/**",
                        "/frontend/**",
                        "/images/**",
                        "/icons/**",
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/offline.html",
                        "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
        );
        http.formLogin(Customizer.withDefaults());
        http.logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        http.csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
