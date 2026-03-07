package com.nevis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WealthTech Search API")
                        .version("1.0.0")
                        .description("Search API for a WealthTech automation platform. " +
                                     "Supports keyword search across clients and semantic search across documents.")
                        .contact(new Contact()
                                .name("Nevis Wealth")
                                .email("grigory.sobko@neviswealth.com")));
    }
}
