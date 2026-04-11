package com.miftah.core_bank_system.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        private static final String[] SWAGGER_WHITELIST = {
                        "/authenticate",
                        "/swagger-resources/",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
        };

        private static final String[] USER_WHITELIST = {
                "/api/accounts/me",
        };

        public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
                this.jwtAuthFilter = jwtAuthFilter;
                this.authenticationProvider = authenticationProvider;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(SWAGGER_WHITELIST).permitAll()

                                                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                                                .requestMatchers("/api/users/**").hasRole("ADMIN")
                                                .requestMatchers("/api/profiles/**").hasAnyRole("ADMIN", "USER")

                                                .requestMatchers(USER_WHITELIST).hasRole("USER")

                                                .requestMatchers("/api/transactions/me", "/api/audits/me").hasRole("USER")

                                                .requestMatchers("/api/accounts/**", "/api/audits/**").hasRole("ADMIN")
                                                .requestMatchers("/api/transactions/**").hasAnyRole("ADMIN", "USER")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
