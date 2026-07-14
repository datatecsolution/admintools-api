package net.datatecsolution.admintools.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * US-049 (Fase 4 OWASP A07) — throttle anti-fuerza-bruta en memoria.
 * Se ejercita solo la lógica determinística (conteo por clave, limpieza en
 * éxito, independencia de claves, tolerancia a null). La expiración por
 * tiempo (ventana de 5 min) NO se prueba con sleeps reales a propósito.
 */
class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void isBlocked_conMenosDeMaxFallos_noBloquea() {
        String key = "caja1|10.0.0.5";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.recordFailure(key);
        }

        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void isBlocked_alAlcanzarMaxFallos_bloquea() {
        String key = "caja1|10.0.0.5";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(key);
        }

        assertThat(service.isBlocked(key)).isTrue();
    }

    @Test
    void recordSuccess_limpiaElContadorYDesbloquea() {
        String key = "caja1|10.0.0.5";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(key);
        }
        assertThat(service.isBlocked(key)).isTrue();

        service.recordSuccess(key);

        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    void claves_distintas_soneIndependientes() {
        String bloqueada = "caja1|10.0.0.5";
        String otra = "caja1|10.0.0.9";
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(bloqueada);
        }

        assertThat(service.isBlocked(bloqueada)).isTrue();
        assertThat(service.isBlocked(otra)).isFalse();
    }

    @Test
    void isBlocked_conClaveNull_noExplotaYNoBloquea() {
        assertThat(service.isBlocked(null)).isFalse();
    }

    @Test
    void recordFailureYSuccess_conClaveNull_noExplotan() {
        assertThatCode(() -> {
            service.recordFailure(null);
            service.recordSuccess(null);
        }).doesNotThrowAnyException();
    }
}
