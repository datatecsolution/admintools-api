package net.datatecsolution.admintools.config;

import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {

        this.customUserDetailsService = customUserDetailsService;
    }

    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws
    // Exception {
    // http
    // .authorizeHttpRequests((authz) -> authz
    // .requestMatchers("/admin_tools/**","/products/**","/orders/**","/costomers/**","/auth/**","/price/**","/users/**","/static/**","/favicon.ico").permitAll()
    // .anyRequest().authenticated() // Cualquier otra ruta requerirá autenticación
    // //.requestMatchers("/api/**").authenticated() // Proteger rutas de API con
    // autenticación
    // // .requestMatchers("mi**").permitAll() // Proteger rutas de API con
    // autenticación
    // )
    // .sessionManagement(session -> session
    // .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No usar sesiones
    // en las APIs
    // ).csrf(csrf->csrf.disable()) // Desactivar CSRF para APIs
    // .formLogin(formLogin->formLogin.disable()) // Desactivar el formulario de
    // login por defecto
    // .httpBasic(httpBasic->httpBasic.disable()); // Desactivar la autenticación
    // HTTP básica
    //
    //// .authorizeHttpRequests((authz) -> authz
    //// .requestMatchers("/public/**").permitAll() // Permitir rutas públicas sin
    // autenticación
    //// .requestMatchers("/api/**").permitAll() // Permitir que las rutas de API
    // sean accesibles
    //// .anyRequest().authenticated() // Requerir autenticación para cualquier otra
    // solicitud
    ////
    //// ).formLogin((form) -> form
    //// .loginPage("/login") // Especificar una página de inicio de sesión
    // personalizada
    //// .permitAll() // Permitir el acceso a la página de inicio de sesión sin
    // autenticación
    //// .defaultSuccessUrl("/home", true) // Redireccionar a "/home" después del
    // inicio de sesión exitoso
    //// .failureUrl("/login?error=true") // Redireccionar a "/login?error=true" en
    // caso de error
    //// )
    //// .logout((logout) -> logout
    //// .logoutUrl("/logout") // Especificar la URL para cerrar sesión
    //// .logoutSuccessUrl("/login?logout=true") // Redireccionar a la página de
    // inicio de sesión después de cerrar sesión
    //// .permitAll()
    //// );
    //
    // return http.build();
    // }
    //
    // // 2. DEFINIMOS LAS REGLAS DE CORS (El "Portero")
    // @Bean
    // CorsConfigurationSource corsConfigurationSource() {
    // CorsConfiguration configuration = new CorsConfiguration();
    //
    // // Usamos setAllowedOriginPatterns en lugar de setAllowedOrigins
    // // porque a veces es más flexible con los puertos y protocolos.
    // configuration.setAllowedOriginPatterns(Arrays.asList("*")); // PERMITIR TODO
    // TEMPORALMENTE
    //
    // configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE",
    // "OPTIONS", "PATCH"));
    // configuration.setAllowedHeaders(Arrays.asList("*"));
    // configuration.setAllowCredentials(true);
    //
    // UrlBasedCorsConfigurationSource source = new
    // UrlBasedCorsConfigurationSource();
    // source.registerCorsConfiguration("/**", configuration);
    // return source;
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Permitir OPTIONS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. RUTAS PÚBLICAS (Login y Errores)
                        .requestMatchers(
                                "/auth/**",
                                "/api/auth/**",
                                "/admin_tools/api/auth/**",
                                "/error",
                                "/favicon.ico")
                        .permitAll()

                        // 3. RUTAS DE LA APP (¡AQUÍ ESTÁ LA SOLUCIÓN!)
                        // Agregamos esto para que puedas ver los datos sin que te bloquee
                        // .requestMatchers(
                        // "/admin_tools/api/products/**",
                        // "/admin_tools/api/orders/**",
                        // "/admin_tools/api/customers/**",
                        // "/admin_tools/api/users/**",
                        // "/admin_tools/api/price/**",
                        // // Variantes por si acaso
                        // "/api/products/**",
                        // "/api/orders/**",
                        // "/api/customers/**"
                        // ).permitAll()
                        .requestMatchers(
                                "/admin_tools/**",
                                "/products/**",
                                "/orders/**",
                                "/costomers/**",
                                "/auth/**",
                                "/price/**",
                                "/users/**",
                                "/static/**",
                                "/favicon.ico")
                        .permitAll()

                        // 4. Cualquier otra cosa requiere autenticación
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 5. SEGURIDAD: Solo permitimos tu IP y puerto de React
        configuration.setAllowedOrigins(Arrays.asList(
                "http://201.190.38.238:8091",
                "http://localhost:8091", // Útil para pruebas locales
                "http://localhost:3000", // Docker Local
                "https://pedidos.distribuidorasharon.com" // Producción
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicamos CORS a todas las rutas posibles
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
