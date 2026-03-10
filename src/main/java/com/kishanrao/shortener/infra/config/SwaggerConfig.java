package com.kishanrao.shortener.infra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SpringDocUtils.getConfig().addResponseTypeToIgnore(RedirectView.class);

        var devServer = new Server().description("Developer server");

        var contact = new Contact()
                .name("Kishan Rao")
                .url("https://github.com/kishanrao");

        var info = new Info()
                .title("SwiftLink API")
                .version("2.0")
                .contact(contact)
                .description("High-scale URL shortener with JWT auth, TTL, custom aliases, rate limiting, and QR codes.");

        var jwtScheme = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", jwtScheme));
    }
}
