package net.datatecsolution.admintools.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import javax.sql.DataSource;

@Configuration
public class SecurityConfig  {

    private final DataSource dataSource;

    public SecurityConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                                .requestMatchers("/products/**","/orders/**","/costomers/**").permitAll()
                                .anyRequest().authenticated()  // Cualquier otra ruta requerirá autenticación
                        //.requestMatchers("/api/**").authenticated()  // Proteger rutas de API con autenticación
                        // .requestMatchers("mi**").permitAll()  // Proteger rutas de API con autenticación
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No usar sesiones en las APIs
                ).csrf(csrf->csrf.disable())  // Desactivar CSRF para APIs
                .formLogin(formLogin->formLogin.disable())  // Desactivar el formulario de login por defecto
                .httpBasic(httpBasic->httpBasic.disable());  // Desactivar la autenticación HTTP básica

//                .authorizeHttpRequests((authz) -> authz
//                        .requestMatchers("/public/**").permitAll()  // Permitir rutas públicas sin autenticación
//                        .requestMatchers("/api/**").permitAll()     // Permitir que las rutas de API sean accesibles
//                        .anyRequest().authenticated()               // Requerir autenticación para cualquier otra solicitud
//
//                ).formLogin((form) -> form
//                        .loginPage("/login")  // Especificar una página de inicio de sesión personalizada
//                        .permitAll()  // Permitir el acceso a la página de inicio de sesión sin autenticación
//                        .defaultSuccessUrl("/home", true)  // Redireccionar a "/home" después del inicio de sesión exitoso
//                        .failureUrl("/login?error=true")  // Redireccionar a "/login?error=true" en caso de error
//                )
//                .logout((logout) -> logout
//                        .logoutUrl("/logout")  // Especificar la URL para cerrar sesión
//                        .logoutSuccessUrl("/login?logout=true")  // Redireccionar a la página de inicio de sesión después de cerrar sesión
//                        .permitAll()
//                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        JdbcDaoImpl jdbcUserDetailsService = new JdbcDaoImpl();
        jdbcUserDetailsService.setDataSource(dataSource);
        jdbcUserDetailsService.setUsersByUsernameQuery("SELECT usuario, clave, enabled FROM usuario WHERE usuario = ?");
        jdbcUserDetailsService.setAuthoritiesByUsernameQuery("SELECT username, authority FROM authorities WHERE username = ?");
        return jdbcUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
