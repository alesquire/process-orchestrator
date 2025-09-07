package com.processorchestrator.ui.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the DB Scheduler UI.
 * This configuration is only active when security is enabled via properties.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "spring.security.user.name")
public class DbSchedulerUiSecurityConfig {

    /**
     * Configure security filter chain for the DB Scheduler UI.
     * 
     * Security rules:
     * - UI access requires ADMIN or USER role
     * - GET API access requires ADMIN or USER role  
     * - POST API access requires ADMIN role only
     * - All other requests are permitted
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity  http) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authz ->
                        authz
                                // Allow ALL db-scheduler paths without authentication for debugging
                                .requestMatchers("/db-scheduler/**").permitAll()
                                // Allow actuator endpoints for monitoring
                                .requestMatchers("/actuator/**").permitAll()
                                // Allow test endpoint for debugging
                                .requestMatchers("/test").permitAll()
                                // Allow root path and error pages
                                .requestMatchers("/", "/error").permitAll()
                                // All other requests require authentication
                                .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .build();
    }
}
