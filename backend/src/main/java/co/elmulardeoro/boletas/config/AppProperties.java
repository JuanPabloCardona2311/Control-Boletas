package co.elmulardeoro.boletas.config;

import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(String zonaHoraria, List<String> frontendUrl, Jwt jwt, Admin admin) {

    public record Jwt(String secreto, int dias) {}

    public record Admin(String usuario, String clave) {}

    public ZoneId zona() {
        return ZoneId.of(zonaHoraria);
    }
}
