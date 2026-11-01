package co.elmulardeoro.boletas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BoletasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoletasApplication.class, args);
    }
}
