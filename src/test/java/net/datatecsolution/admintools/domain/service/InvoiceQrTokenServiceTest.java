package net.datatecsolution.admintools.domain.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-100 — token HMAC del QR de reimpresión pública. El secreto es @Value,
 * así que se inyecta por reflexión. Se prueba: determinismo, aceptación del
 * token correcto, rechazo de alteraciones/ajenos, y la feature apagada
 * (secreto vacío/blank → token null, matches false).
 */
class InvoiceQrTokenServiceTest {

    private InvoiceQrTokenService serviceConSecreto(String secret) {
        InvoiceQrTokenService svc = new InvoiceQrTokenService();
        ReflectionTestUtils.setField(svc, "secret", secret);
        return svc;
    }

    @Test
    void token_conSecreto_esDeterministicoYNoNulo() {
        InvoiceQrTokenService svc = serviceConSecreto("test-secret");

        String t1 = svc.token(3, 1050);
        String t2 = svc.token(3, 1050);

        assertThat(t1).isNotNull().isEqualTo(t2);
    }

    @Test
    void matches_aceptaElTokenCorrecto() {
        InvoiceQrTokenService svc = serviceConSecreto("test-secret");
        String token = svc.token(3, 1050);

        assertThat(svc.matches(3, 1050, token)).isTrue();
    }

    @Test
    void matches_rechazaTokenAlterado() {
        InvoiceQrTokenService svc = serviceConSecreto("test-secret");
        String token = svc.token(3, 1050);

        assertThat(svc.matches(3, 1050, token + "ff")).isFalse();
    }

    @Test
    void matches_rechazaTokenDeOtraFactura() {
        InvoiceQrTokenService svc = serviceConSecreto("test-secret");
        String tokenOtraCaja = svc.token(4, 1050);
        String tokenOtroNumero = svc.token(3, 1051);

        assertThat(svc.matches(3, 1050, tokenOtraCaja)).isFalse();
        assertThat(svc.matches(3, 1050, tokenOtroNumero)).isFalse();
    }

    @Test
    void token_conSecretoBlank_devuelveNull_featureApagada() {
        assertThat(serviceConSecreto("").token(3, 1050)).isNull();
        assertThat(serviceConSecreto("   ").token(3, 1050)).isNull();
    }

    @Test
    void matches_conSecretoBlank_devuelveFalse_featureApagada() {
        InvoiceQrTokenService svc = serviceConSecreto("");

        assertThat(svc.matches(3, 1050, "cualquiercosa")).isFalse();
    }

    @Test
    void matches_conProvidedNull_devuelveFalse() {
        InvoiceQrTokenService svc = serviceConSecreto("test-secret");

        assertThat(svc.matches(3, 1050, null)).isFalse();
    }
}
