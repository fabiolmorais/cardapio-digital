package br.com.cardapio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI cardapioOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Cardápio Digital API")
                .version("v1")
                .description("API do cardápio digital e atendimento por mesa"));
    }
}
