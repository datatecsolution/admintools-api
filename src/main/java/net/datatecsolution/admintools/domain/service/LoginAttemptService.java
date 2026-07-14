package net.datatecsolution.admintools.domain.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * US-049 (Fase 4 OWASP A07) — throttle anti-fuerza-bruta en memoria para los
 * endpoints de credenciales (/auth/login y /authorization/verify-admin), que
 * antes no tenían límite de intentos.
 *
 * Ventana deslizante simple por clave (usuario+IP): tras {@link #MAX_ATTEMPTS}
 * fallos dentro de {@link #WINDOW_MS} la clave queda bloqueada hasta que la
 * ventana expira. Un login exitoso limpia el contador. Es best-effort y por
 * instancia (no distribuido) — suficiente para frenar el brute-force online
 * en un despliegue de una sola API; **falla abierto** ante cualquier duda para
 * no dejar afuera a un usuario legítimo por un bug del throttle.
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 10;
    static final long WINDOW_MS = 5 * 60 * 1000L; // 5 minutos

    private static final class Counter {
        final AtomicInteger fails = new AtomicInteger(0);
        volatile long windowStart;
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    /** ¿La clave está bloqueada AHORA por exceso de fallos recientes? */
    public boolean isBlocked(String key) {
        if (key == null) {
            return false;
        }
        Counter c = counters.get(key);
        if (c == null) {
            return false;
        }
        if (now() - c.windowStart >= WINDOW_MS) {
            counters.remove(key, c); // ventana vencida → arranca limpio
            return false;
        }
        return c.fails.get() >= MAX_ATTEMPTS;
    }

    /** Registra un intento fallido; abre ventana nueva si la anterior venció. */
    public void recordFailure(String key) {
        if (key == null) {
            return;
        }
        Counter c = counters.computeIfAbsent(key, k -> {
            Counter nc = new Counter();
            nc.windowStart = now();
            return nc;
        });
        if (now() - c.windowStart >= WINDOW_MS) {
            c.windowStart = now();
            c.fails.set(0);
        }
        c.fails.incrementAndGet();
    }

    /** Éxito → limpia el contador de esa clave. */
    public void recordSuccess(String key) {
        if (key != null) {
            counters.remove(key);
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
