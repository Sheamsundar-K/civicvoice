package com.civicvoice.config;

import com.civicvoice.auth.filter.JwtAuthenticationFilter;
import com.civicvoice.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final AppProperties appProperties;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/**",
        "/api/v1/issues",
        "/api/v1/issues/nearby",
        "/api/v1/issues/heatmap",
        "/api/v1/issues/{id}",
        "/api/v1/polls",
        "/api/v1/polls/{id}",
        "/api/v1/polls/{id}/results",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**",
        "/actuator/health",
        "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**", "/api/v1/issues", "/api/v1/upload/multiple").permitAll()
                .requestMatchers("/api/v1/analytics/**").hasAnyRole("AUTHORITY", "NGO", "ADMIN")
                .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/issues/*/status").hasAnyRole("AUTHORITY", "NGO", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/issues/*/assign").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/polls").hasAnyRole("AUTHORITY", "NGO", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/polls/*/close").hasAnyRole("AUTHORITY", "NGO", "ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/uploads/**", config);
        return source;
    }
}
