package com.civicvoice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI civicVoiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("CivicVoice API")
                .description("""
                    **CivicVoice – Citizen Feedback & Governance Platform**
                    
                    A unified platform for geo-tagged issue reporting, complaint tracking,
                    public polling, authority dashboards, and SLA-driven resolution notifications.
                    
                    **Roles:**
                    - `CITIZEN` – Can report issues, upvote, comment, vote on polls
                    - `AUTHORITY` – Can update issue status, create polls, view analytics
                    - `NGO` – Can update issue status, create polls, view analytics (non-governmental organization)
                    - `ADMIN` – Full access including user management and audit logs
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("CivicVoice Engineering")
                    .email("api@civicvoice.gov")
                    .url("https://civicvoice.gov"))
                .license(new License()
                    .name("Government Open License 2.0")
                    .url("https://civicvoice.gov/license")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.civicvoice.gov").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Provide a valid JWT access token obtained from /api/v1/auth/login")));
    }
}
