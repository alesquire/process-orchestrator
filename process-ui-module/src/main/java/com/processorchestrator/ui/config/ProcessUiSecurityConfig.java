package com.processorchestrator.ui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the Process UI module.
 * Defines access rules for various endpoints.
 */
@Configuration
@EnableWebSecurity
public class ProcessUiSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authz ->
                        authz
                                // Allow static resources without authentication
                                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                                // Allow actuator endpoints for monitoring
                                .requestMatchers("/actuator/**").permitAll()
                                // Allow API endpoints for both users and admins
                                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "USER")
                                // Only admins can modify data
                                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN")
                                // Protect the main UI page - requires ADMIN or USER role
                                .requestMatchers("/", "/dashboard").hasAnyRole("ADMIN", "USER")
                                // All other requests require authentication
                                .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .build();
    }
}
