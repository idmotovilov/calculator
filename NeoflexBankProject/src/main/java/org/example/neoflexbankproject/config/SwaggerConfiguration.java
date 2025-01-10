package org.example.neoflexbankproject.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration


public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vacation Calculator API")
                        .description("API для расчета отпускных")
                        .version("1.0")  // Указывается версия API
                        .contact(new Contact()
                                .name("Ваше имя")
                                .url("https://yourcompany.com")
                                .email("your.email@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}


