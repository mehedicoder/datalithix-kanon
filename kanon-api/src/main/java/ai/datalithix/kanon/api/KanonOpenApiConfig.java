package ai.datalithix.kanon.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KanonOpenApiConfig {

    @Bean
    public OpenAPI kanonOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kanon Platform API")
                        .description("REST API for Kanon — the domain-configurable agentic operating system")
                        .version("0.1.0")
                        .license(new License()
                                .name("Proprietary")
                                .url("https://datalithix.ai")));
    }
}
