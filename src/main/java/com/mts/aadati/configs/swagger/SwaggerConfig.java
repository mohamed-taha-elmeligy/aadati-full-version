package com.mts.aadati.configs.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aadati API")
                        .version("2.0")
                        .description("""
                           This is a habit and task tracking API built with Java 21 and Spring Boot,
                           focused on consistency, progress tracking,
                           and production-oriented backend engineering.
                           """)
                        .contact(
                                new Contact()
                                        .name("Mohamed Taha Elmeligy")
                                        .email("mt.elmeligy.dev@gmail.com")
                                        .url("https://www.linkedin.com/in/mtelmeligy-backend-dev/")
                        )
                        .license(
                                new License()
                                        .name("MIT License")
                                        .url("https://github.com/mohamed-taha-elmeligy/aadati-full-version/blob/master/LICENSE"))
                )
                .components(
                        new Components().addSecuritySchemes(
                                "bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearer-key")
                );
    }
}


