package co.elmulardeoro.boletas.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebConfig {

    /** El tablero se consulta cada pocos segundos; si nada cambió responde 304 sin volver a mandar los datos. */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> etagTablero() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registro = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registro.addUrlPatterns("/api/rifas/*/tablero");
        return registro;
    }
}
