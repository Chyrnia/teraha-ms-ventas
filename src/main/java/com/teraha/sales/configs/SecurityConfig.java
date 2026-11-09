package com.teraha.sales.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public API documentation
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        // Consultar ventas: permitido para vendedores, gerentes y sysadmin
                        .requestMatchers(HttpMethod.GET, "/sales", "/sales/**")
                        .hasAnyRole("SELLER", "MANAGER", "SYSADMIN")

                        // Registrar una nueva venta: permitido para vendedores, gerentes y sysadmin
                        .requestMatchers(HttpMethod.POST, "/sales")
                        .hasAnyRole("SELLER", "MANAGER", "SYSADMIN")

                        // Consultar clientes: permitido para vendedores, gerentes y sysadmin
                        .requestMatchers(HttpMethod.GET, "/clients", "/clients/**")
                        .hasAnyRole("SELLER", "MANAGER", "SYSADMIN")

                        // Registrar un nuevo cliente: permitido para vendedores, gerentes y sysadmin
                        .requestMatchers(HttpMethod.POST, "/clients")
                        .hasAnyRole("SELLER", "MANAGER", "SYSADMIN")

                        // Cancelar o eliminar un cliente: estrictamente reservado para SYSADMIN
                        .requestMatchers(HttpMethod.DELETE, "/clients/**")
                        .hasAnyRole("MANAGER", "SYSADMIN")

						// Solicitar reportes: permitido a gerentes y sysadmin
						.requestMatchers(HttpMethod.GET, "/reports/sales/**")
						.hasAnyRole("MANAGER", "SYSADMIN")

                        // Cualquier otra petición requiere autenticación general
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}


