package net.datatecsolution.admintools.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * US-100 — token firmado para la URL pública de reimpresión por QR.
 *
 * No existe id único global de factura (identidad = caja + número), así que
 * el QR lleva HMAC-SHA256(caja|numero, secreto) truncado a 128 bits: sin el
 * secreto del servidor no se pueden enumerar facturas ajenas. Sin migración.
 *
 * app.public-invoice.secret: si está vacío la feature queda APAGADA
 * (token() = null → el POS no imprime QR; matches() = false → 404).
 * En producción configurar un secreto por cliente; cambiarlo invalida los
 * QR ya impresos.
 */
@Service
public class InvoiceQrTokenService {

    @Value("${app.public-invoice.secret:}")
    private String secret;

    /** Token hex (32 chars) para caja+numero, o null si la feature está apagada. */
    public String token(int caja, int numero) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal((caja + "|" + numero).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 16);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }

    /** Comparación constant-time; false si la feature está apagada o token nulo. */
    public boolean matches(int caja, int numero, String provided) {
        String expected = token(caja, numero);
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
}
