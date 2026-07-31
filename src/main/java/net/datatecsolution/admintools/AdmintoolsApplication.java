package net.datatecsolution.admintools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling // US-118: expiración diaria de pedidos
public class AdmintoolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdmintoolsApplication.class, args);
    }

    // Configuración para el LocaleResolver predeterminado
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        // Establecemos el locale predeterminado en español de Honduras
        slr.setDefaultLocale(new Locale("es", "HN"));
        return slr;
    }

    // Alternativa para configurar el Locale basado en el header de aceptación del cliente
    @Bean
    public AcceptHeaderLocaleResolver localeContextResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(new Locale("es", "HN")); // Español de Honduras
        return localeResolver;
    }
}
