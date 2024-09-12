package net.datatecsolution.admintools.config;

import net.datatecsolution.admintools.domain.service.CustomUserDetailsService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig  {

    private final CustomUserDetailsService customUserDetailsService;



    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {

        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz
                                .requestMatchers("/products/**","/orders/**","/costomers/**","/auth/**").permitAll()
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Definimos el AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
