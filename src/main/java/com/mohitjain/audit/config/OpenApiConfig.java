package com.mohitjain.audit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auditPipelineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kafka Audit Pipeline API")
                        .description("Event-driven audit pipeline with Kafka producer/consumer, "
                                + "PostgreSQL partitioned tables, and 7-year retention policy.")
                        .version("1.0.0")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                        .contact(new Contact()
                                .name("Mohit Jain")
                                .url("https://github.com/jainmohit0807")));
    }
}
