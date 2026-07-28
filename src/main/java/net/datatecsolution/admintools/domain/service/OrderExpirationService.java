package net.datatecsolution.admintools.domain.service;

import net.datatecsolution.admintools.persistence.crud.OrdenCRUD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * US-118 (Fase 2 stock reservado) — expiración de pedidos: un pedido vivo
 * (estado NOT IN (3,5) — incluye "Enviado", que reserva por decisión US-115)
 * congela inventario; sin límite, un pedido abandonado reserva PARA SIEMPRE
 * (dulce acumuló 27 pendientes). Decisión 2026-07-28: auto-anular (estado 5,
 * mismo soft-delete del flujo manual) los pedidos con más de N días.
 *
 * Configurable con app.orders.expiration-days (default 7; 0 = desactivado).
 * Corre a las 03:30 de Honduras; idempotente (re-ejecutar no encuentra nada).
 */
@Service
public class OrderExpirationService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationService.class);
    private static final ZoneId HONDURAS = ZoneId.of("America/Tegucigalpa");

    private final OrdenCRUD ordenCRUD;

    @Value("${app.orders.expiration-days:7}")
    private int expirationDays;

    public OrderExpirationService(OrdenCRUD ordenCRUD) {
        this.ordenCRUD = ordenCRUD;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Tegucigalpa")
    @Transactional
    public void expirarPedidosViejos() {
        if (expirationDays <= 0) {
            return; // desactivado por configuración
        }
        LocalDateTime limite = LocalDate.now(HONDURAS).minusDays(expirationDays).atStartOfDay();
        List<Integer> viejos = ordenCRUD.findVivosAnterioresA(limite);
        if (viejos.isEmpty()) {
            return;
        }
        int anulados = ordenCRUD.expirarAnterioresA(limite);
        log.info("US-118: {} pedidos con más de {} días anulados (estado 5), liberando su reserva: {}",
                anulados, expirationDays, viejos);
    }
}
