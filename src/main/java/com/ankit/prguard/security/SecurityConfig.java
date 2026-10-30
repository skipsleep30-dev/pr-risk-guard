package com.ankit.prguard.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/github/webhook").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder,
            @Value("${PRGUARD_USER1:demo}") String user1,
            @Value("${PRGUARD_PASSWORD1:}") String password1,
            @Value("${PRGUARD_USER2:reviewer}") String user2,
            @Value("${PRGUARD_PASSWORD2:}") String password2) {
        if (password1.isBlank() || password2.isBlank() || user1.equals(user2)) {
            throw new IllegalStateException("Set distinct PRGUARD_USER1/USER2 and PRGUARD_PASSWORD1/2");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(user1).password(encoder.encode(password1)).roles("USER").build(),
                User.withUsername(user2).password(encoder.encode(password2)).roles("USER").build());
    }
}
