package com.minimarket.security.config;

import com.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.security.filter.RequestAuditFilter;
import com.minimarket.security.filter.SecurityHeadersFilter;
import com.minimarket.security.filter.ThreatDetectionFilter;
import com.minimarket.security.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestAuditFilter requestAuditFilter;
    private final ThreatDetectionFilter threatDetectionFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestAuditFilter requestAuditFilter,
            ThreatDetectionFilter threatDetectionFilter,
            SecurityHeadersFilter securityHeadersFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestAuditFilter = requestAuditFilter;
        this.threatDetectionFilter = threatDetectionFilter;
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**", "/api/auth/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/inventario", "/api/inventario/**",
                                "/api/detalle-ventas", "/api/detalle-ventas/**"
                        ).hasAnyRole("EMPLEADO", "ADMIN")
                        .requestMatchers("/api/carrito", "/api/carrito/**").hasAnyRole("CLIENTE", "ADMIN")
                        .requestMatchers("/api/ventas", "/api/ventas/**").hasAnyRole("CLIENTE", "EMPLEADO", "ADMIN")
                        .requestMatchers(
                                "/api/productos", "/api/productos/**",
                                "/api/categorias", "/api/categorias/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(securityErrorJson(401, "No autenticado", "Debe autenticarse para acceder al recurso", request.getRequestURI()));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(securityErrorJson(403, "Acceso denegado", "No tiene permisos para acceder al recurso solicitado", request.getRequestURI()));
                        })
                )
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestAuditFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(threatDetectionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${minimarket.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        configuration.setExposedHeaders(List.of("X-Request-ID"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider databaseAuthenticationProvider = new DaoAuthenticationProvider();
        databaseAuthenticationProvider.setUserDetailsService(customUserDetailsService);
        databaseAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(List.of(databaseAuthenticationProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String securityErrorJson(int status, String error, String message, String path) {
        String requestId = MDC.get("requestId");
        return """
                {"status":%d,"error":"%s","message":"%s","path":"%s","requestId":"%s"}\
                """.formatted(status, error, message, path, requestId == null ? "" : requestId);
    }
}
