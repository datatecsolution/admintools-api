package net.datatecsolution.admintools.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI adminToolsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AdminTools API")
                        .version("0.0.1")
                        .description("API que sirve a admintools-pos (POS) y at-ordenes-ventas (pedidos). "
                                + "La mayoria de endpoints requieren JWT en el header Authorization: Bearer <token>. "
                                + "Obtener token via POST /auth/login."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido por /auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
