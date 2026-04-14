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
                "/api/v1/accounts/me",
                "/api/v1/accounts/me/**"
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

                                                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                                                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/profiles/**").hasAnyRole("ADMIN", "USER")

                                                .requestMatchers(USER_WHITELIST).hasRole("USER")

                                                .requestMatchers("/api/v1/transactions/me", "/api/v1/audits/me", "/api/v1/notifications/**").hasRole("USER")

                                                .requestMatchers("/api/v1/accounts/**", "/api/v1/audits/**").hasRole("ADMIN")
                                                .requestMatchers("/api/v1/transactions/**").hasAnyRole("ADMIN", "USER")
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
