package net.datatecsolution.admintools.config;

import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // US-021: habilita @PreAuthorize en controllers / services
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final JwtRequestFilter jwtRequestFilter;
        private final List<String> allowedOrigins;

        public SecurityConfig(
                        CustomUserDetailsService customUserDetailsService,
                        JwtRequestFilter jwtRequestFilter,
                        @Value("${cors.allowed-origins}") String allowedOriginsCsv) {
                this.customUserDetailsService = customUserDetailsService;
                this.jwtRequestFilter = jwtRequestFilter;
                this.allowedOrigins = Arrays.stream(allowedOriginsCsv.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
        }

        // @Bean
        // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws
        // Exception {
        // http
        // .authorizeHttpRequests((authz) -> authz
        // .requestMatchers("/admin_tools/**","/products/**","/orders/**","/customers/**","/auth/**","/price/**","/users/**","/static/**","/favicon.ico").permitAll()
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
                                // US-049 (Fase 4 OWASP): headers de seguridad. X-Content-Type-Options
                                // (nosniff) y X-Frame-Options (DENY) ya vienen por default de Spring
                                // Security y no se sobreescriben; acá se agregan los que faltaban.
                                // La API sirve JSON (no HTML propio salvo swagger, apagado en pdn), así
                                // que la CSP del SPA vive en su nginx; acá solo `frame-ancestors` como
                                // anti-clickjacking (no rompe swagger-ui en dev). HSTS necesita
                                // server.forward-headers-strategy=framework tras el proxy TLS (pdn).
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("frame-ancestors 'none'"))
                                                .referrerPolicy(ref -> ref
                                                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)))
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

                                                // 3. RUTAS DE LA APP que necesitan ser públicas (estáticos, etc)
                                                .requestMatchers(
                                                                "/static/**",
                                                                "/favicon.ico")
                                                .permitAll()

                                                // 3b. Imagen de producto (US-079): los <img src> del POS no
                                                // mandan JWT — solo el GET es público; POST/DELETE llevan
                                                // @PreAuthorize(ADMIN) en el controller.
                                                .requestMatchers(HttpMethod.GET, "/products/*/image")
                                                .permitAll()

                                                // 3c. Factura pública por QR (US-100): el acceso real lo
                                                // protege el token HMAC (?t=) — sin token válido → 404.
                                                .requestMatchers(HttpMethod.GET, "/public/invoices/**")
                                                .permitAll()

                                                // 4. OpenAPI / Swagger UI publicos (documentacion + try-it-out)
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/v3/api-docs.yaml")
                                                .permitAll()

                                                // 5. Imagenes uploadeadas via US-030 — publicas
                                                //    (el upload requiere ADMIN pero el serving es publico
                                                //    para que el frontend las muestre sin JWT en el <img>).
                                                .requestMatchers("/uploads/**").permitAll()

                                                // 4. Cualquier otra cosa requiere autenticación
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.sendError(
                                                                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                                                                        "Unauthorized");
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Origenes permitidos vienen de cors.allowed-origins (env var
                // CORS_ALLOWED_ORIGINS en prod, default en application-dev.properties).
                configuration.setAllowedOrigins(allowedOrigins);

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

        // ============================================================
        // US-021 — Role hierarchy
        //
        // Mapeo desde usuario.tipo_permiso (Swing) → ROLE_*
        // (en CustomUserDetailsService):
        //   4 root       -> ROLE_ADMIN
        //   1 supervisor -> ROLE_INVENTORY
        //   2 cajero     -> ROLE_CASHIER
        //   3 vendedor   -> ROLE_SELLER
        //   otro/null    -> ROLE_USER
        //
        // Jerarquia: un solo rol implica todos los menores.
        // Vendedor crea ordenes pero NO factura (ROLE_SELLER < CASHIER).
        // ============================================================

        /**
         * STATIC a proposito (mismo motivo que methodSecurityExpressionHandler
         * abajo): los beans usados por la infra de @EnableMethodSecurity tienen
         * que estar disponibles antes de que se construyan los
         * AuthorizationManager. Sin static, el bean se resuelve tarde y la
         * jerarquia no llega a aplicarse — hasRole() sigue siendo match literal.
         */
        @Bean
        static RoleHierarchy roleHierarchy() {
                RoleHierarchyImpl r = new RoleHierarchyImpl();
                r.setHierarchy(
                                "ROLE_ADMIN > ROLE_INVENTORY\n" +
                                "ROLE_INVENTORY > ROLE_CASHIER\n" +
                                "ROLE_CASHIER > ROLE_SELLER\n" +
                                "ROLE_SELLER > ROLE_USER");
                return r;
        }

        /**
         * Necesario para que {@code @PreAuthorize("hasRole(...)")} respete la
         * jerarquia. Sin este bean, hasRole solo matchea el rol literal.
         *
         * STATIC a proposito: en Spring Security 6 los AuthorizationManager
         * de @EnableMethodSecurity se inicializan temprano en el ciclo de
         * BeanPostProcessors. Si el bean no es static, su factory method se
         * resuelve TARDE y la jerarquia no llega a aplicarse — los hasRole()
         * siguen siendo match literal. Hacerlo static lo cablea antes.
         */
        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
                DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
                handler.setRoleHierarchy(roleHierarchy);
                return handler;
        }
}
