package ai.datalithix.kanon.bootstrap;

import com.vaadin.flow.spring.annotation.EnableVaadin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@EnableVaadin("ai.datalithix.kanon")
@ConfigurationPropertiesScan("ai.datalithix.kanon.bootstrap")
@SpringBootApplication(scanBasePackages = "ai.datalithix.kanon")
public class KanonApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanonApplication.class, args);
    }
}
