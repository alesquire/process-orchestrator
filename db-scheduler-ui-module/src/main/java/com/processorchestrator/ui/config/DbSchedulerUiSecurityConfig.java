package com.processorchestrator.ui.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(authz ->
                        authz
                                // Protect the UI - both admins and users can view
                                .requestMatchers("/db-scheduler/**").hasAnyRole("ADMIN", "USER")
                                
                                // Allow read access to the API for both users and admins
                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/db-scheduler-api/**")
                                .hasAnyRole("ADMIN", "USER")
                                
                                // Only admins can delete tasks, alter scheduling, etc.
                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/db-scheduler-api/**")
                                .hasRole("ADMIN")
                                
                                // Allow actuator endpoints for monitoring
                                .requestMatchers("/actuator/**").permitAll()
                                
                                // All other requests are permitted
                                .anyRequest().permitAll()
                )
                .httpBasic(withDefaults())
                .build();
    }
}
