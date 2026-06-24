package com.afet.monitoring.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires up the interactive API console.
 *
 * <p>springdoc scans every {@code @RestController} and generates an OpenAPI 3 spec at
 * {@code /v3/api-docs}, rendered as a clickable UI at {@code /swagger-ui.html}. From there
 * each endpoint can be expanded and executed with "Try it out" — no curl needed.
 *
 * <p>The root path {@code /} is redirected to the Swagger UI so that simply opening the
 * host in a browser lands on something useful instead of a Whitelabel 404.
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Bean
    OpenAPI earthquakeMonitoringApi() {
        final String scheme = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                .title("Earthquake & Disaster Monitoring API")
                .version("0.0.1")
                .description("""
                        Seismic ingestion, STA/LTA detection and risk scoring backend.
                        Built with Spring Boot 3 + Hexagonal Architecture.

                        Most endpoints require a JWT: call POST /api/auth/login (e.g.
                        admin/admin123), copy the token, click "Authorize" and paste it.

                        Try the endpoints below directly: expand one, click "Try it out",
                        edit the request body and hit "Execute".""")
                .contact(new Contact().name("afet"))
                .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(scheme, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }

    /** Open the bare host -> land on the API console. */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }
}
