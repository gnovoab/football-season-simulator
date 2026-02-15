package com.example.footballseasonsimulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for production use.
 *
 * <p>This configuration provides centralized CORS settings that can be
 * customized via application properties. It replaces the per-controller
 * {@code @CrossOrigin} annotations with a global configuration.
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code cors.allowed-origins} - Comma-separated list of allowed origins</li>
 *   <li>{@code cors.allowed-methods} - Comma-separated list of allowed HTTP methods</li>
 *   <li>{@code cors.allowed-headers} - Comma-separated list of allowed headers</li>
 *   <li>{@code cors.allow-credentials} - Whether to allow credentials</li>
 *   <li>{@code cors.max-age} - Max age for preflight cache (seconds)</li>
 * </ul>
 *
 * <p>Example configuration in application.yaml:
 * <pre>
 * cors:
 *   allowed-origins: https://example.com,https://app.example.com
 *   allowed-methods: GET,POST,PUT,DELETE,OPTIONS
 *   allowed-headers: "*"
 *   allow-credentials: true
 *   max-age: 3600
 * </pre>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = parseCommaSeparated(allowedOrigins);
        List<String> methods = parseCommaSeparated(allowedMethods);
        List<String> headers = parseCommaSeparated(allowedHeaders);

        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods(methods.toArray(new String[0]))
                .allowedHeaders(headers.toArray(new String[0]))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);

        // Also configure CORS for actuator endpoints
        registry.addMapping("/actuator/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders(headers.toArray(new String[0]))
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }

    /**
     * Provides a CorsConfigurationSource bean for use with Spring Security
     * if security is added in the future.
     *
     * @return CorsConfigurationSource with the configured CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseCommaSeparated(allowedOrigins));
        configuration.setAllowedMethods(parseCommaSeparated(allowedMethods));
        configuration.setAllowedHeaders(parseCommaSeparated(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        return source;
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of("*");
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

