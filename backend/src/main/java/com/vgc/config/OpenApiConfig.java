package com.vgc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI greenOfficeOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Green Office API")
                .version("1.0")
                .description("Public API documentation for green-office.uk"));
    }
}
