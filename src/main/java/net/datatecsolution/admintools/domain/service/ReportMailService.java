package net.datatecsolution.admintools.domain.service;

import jakarta.mail.internet.MimeMessage;
import net.datatecsolution.admintools.domain.dto.AbcItem;
import net.datatecsolution.admintools.domain.dto.AbcReportResponse;
import net.datatecsolution.admintools.domain.dto.CajaDailyReport;
import net.datatecsolution.admintools.domain.dto.CategoryProjection;
import net.datatecsolution.admintools.domain.dto.CategorySalesResponse;
import net.datatecsolution.admintools.domain.dto.CategorySalesRow;
import net.datatecsolution.admintools.domain.dto.DailyReportResponse;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionItem;
import net.datatecsolution.admintools.domain.dto.PurchaseSuggestionsResponse;
import net.datatecsolution.admintools.domain.dto.RotationItem;
import net.datatecsolution.admintools.domain.dto.RotationReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * US-068 — arma el CSV de cada reporte (reusando los services de US-047/062/
 * 063/064/106) y lo envía por correo con reintentos.
 *
 * Failover SMTP: hasta {@link #MAX_INTENTOS} intentos con espera creciente;
 * si todos fallan, el llamador registra FALLO con el detalle en el histórico
 * — el scheduler nunca revienta por un SMTP caído.
 */
@Service
public class ReportMailService {

    private static final Logger log = LoggerFactory.getLogger(ReportMailService.class);
    static final int MAX_INTENTOS = 3;
    private static final long ESPERA_BASE_MS = 2000;

    private final JavaMailSender mailSender;
    private final DailyReportService dailyReportService;
    private final AbcAnalysisService abcAnalysisService;
    private final RotationReportService rotationReportService;
    private final PurchaseSuggestionService purchaseSuggestionService;
    private final CategorySalesService categorySalesService;

    @Value("${app.mail.from:reportes@admintools.local}")
    private String from;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public ReportMailService(JavaMailSender mailSender, DailyReportService dailyReportService,
                             AbcAnalysisService abcAnalysisService, RotationReportService rotationReportService,
                             PurchaseSuggestionService purchaseSuggestionService,
                             CategorySalesService categorySalesService) {
        this.mailSender = mailSender;
        this.dailyReportService = dailyReportService;
        this.abcAnalysisService = abcAnalysisService;
        this.rotationReportService = rotationReportService;
        this.purchaseSuggestionService = purchaseSuggestionService;
        this.categorySalesService = categorySalesService;
    }

    /** Resultado de un envío: OK o el último error tras los reintentos. */
    public record Resultado(boolean ok, int intentos, String detalle) {}

    /**
     * Genera el reporte y lo envía. Nunca lanza: el resultado (incluido el
     * fallo) es un valor, para que el histórico siempre quede escrito.
     */
    public Resultado generarYEnviar(String reporte, String destinatarios, LocalDate hoy) {
        String asunto;
        String csv;
        try {
            Contenido c = generar(reporte, hoy);
            asunto = c.asunto();
            csv = c.csv();
        } catch (Exception e) {
            log.error("US-068: fallo generando el reporte {}", reporte, e);
            return new Resultado(false, 0, "Error generando el reporte: " + e.getMessage());
        }
        if (mailHost == null || mailHost.isBlank()) {
            return new Resultado(false, 0,
                    "SMTP sin configurar (MAIL_HOST vacío) — configurar variables MAIL_* del contenedor");
        }

        String ultimoError = "";
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                enviar(destinatarios, asunto, csv, reporte, hoy);
                return new Resultado(true, intento, null);
            } catch (Exception e) {
                ultimoError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.warn("US-068: intento {}/{} de envío de {} falló: {}", intento, MAX_INTENTOS, reporte, ultimoError);
                if (intento < MAX_INTENTOS) {
                    try {
                        Thread.sleep(ESPERA_BASE_MS * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return new Resultado(false, intento, "Interrumpido durante el reintento");
                    }
                }
            }
        }
        return new Resultado(false, MAX_INTENTOS, ultimoError);
    }

    private void enviar(String destinatarios, String asunto, String csv, String reporte, LocalDate hoy)
            throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(destinatarios.split("\\s*,\\s*"));
        helper.setSubject(asunto);
        helper.setText("Adjunto el reporte generado automáticamente por AdminTools.\n\n"
                + "Este correo se envía según la programación configurada en el panel (Analítica → Envíos).", false);
        helper.addAttachment(reporte.toLowerCase() + "-" + hoy + ".csv",
                new org.springframework.core.io.ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)),
                "text/csv");
        mailSender.send(msg);
    }

    private record Contenido(String asunto, String csv) {}

    private Contenido generar(String reporte, LocalDate hoy) {
        return switch (reporte) {
            case "DAILY" -> {
                LocalDate ayer = hoy.minusDays(1);
                DailyReportResponse r = dailyReportService.daily(ayer, null);
                yield new Contenido("Reporte diario — " + ayer, csvDaily(r));
            }
            case "ABC" -> {
                AbcReportResponse r = abcAnalysisService.abc(hoy.minusDays(29), hoy, null, null,
                        new BigDecimal("80"), new BigDecimal("95"), null);
                yield new Contenido("Análisis ABC (últimos 30 días) — " + hoy, csvAbc(r));
            }
            case "ROTATION" -> {
                RotationReportResponse r = rotationReportService.rotation(hoy.minusDays(89), hoy,
                        null, null, null, 30, 90, null);
                yield new Contenido("Rotación de inventario (90 días) — " + hoy, csvRotation(r));
            }
            case "PURCHASE" -> {
                PurchaseSuggestionsResponse r = purchaseSuggestionService.suggestions(hoy.minusDays(29), hoy,
                        null, null, null, 30, false, null);
                yield new Contenido("Sugerencias de compra — " + hoy, csvPurchase(r));
            }
            case "CATEGORY" -> {
                CategorySalesResponse r = categorySalesService.categorySales(hoy.getYear() - 1, hoy, null, null);
                yield new Contenido("Ventas por categoría (trimestral) — " + hoy, csvCategory(r));
            }
            default -> throw new IllegalArgumentException("Reporte desconocido: " + reporte);
        };
    }

    /* ---- CSVs (separador ';' + BOM, mismo contrato que los exports del POS) ---- */

    private static String bom() { return "﻿"; }

    private static String esc(String s) {
        if (s == null) return "";
        String guarded = s.matches("^[=+\\-@\\t\\r].*") ? "'" + s : s;
        return '"' + guarded.replace("\"", "\"\"") + '"';
    }

    private String csvDaily(DailyReportResponse r) {
        StringBuilder sb = new StringBuilder(bom())
                .append("caja;nombre;ventas;total;efectivo;tarjeta;credito;descuentos;anuladas;total_anulado\r\n");
        for (CajaDailyReport c : r.cajas()) sb.append(filaDaily(c, c.nombreCaja()));
        sb.append(filaDaily(r.consolidado(), "Consolidado"));
        return sb.toString();
    }

    private String filaDaily(CajaDailyReport c, String nombre) {
        return c.caja() + ";" + esc(nombre) + ";" + c.ventas() + ";" + c.total() + ";" + c.efectivo() + ";"
                + c.tarjeta() + ";" + c.credito() + ";" + c.descuentos() + ";" + c.anuladas() + ";"
                + c.totalAnulado() + "\r\n";
    }

    private String csvAbc(AbcReportResponse r) {
        StringBuilder sb = new StringBuilder(bom())
                .append("codigo;articulo;categoria;unidades;venta;participacion;acumulado;clase\r\n");
        for (AbcItem i : r.items()) {
            sb.append(i.codigoArticulo()).append(';').append(esc(i.articulo())).append(';')
              .append(esc(i.categoria())).append(';').append(i.unidades()).append(';')
              .append(i.venta()).append(';').append(i.participacion()).append(';')
              .append(i.acumulado()).append(';').append(i.clase()).append("\r\n");
        }
        return sb.toString();
    }

    private String csvRotation(RotationReportResponse r) {
        StringBuilder sb = new StringBuilder(bom())
                .append("codigo;articulo;categoria;unidades;venta;stock;dias_cobertura;sin_movimiento;clasificacion\r\n");
        for (RotationItem i : r.items()) {
            sb.append(i.codigoArticulo()).append(';').append(esc(i.articulo())).append(';')
              .append(esc(i.categoria())).append(';').append(i.unidades()).append(';')
              .append(i.venta()).append(';').append(i.stockActual()).append(';')
              .append(i.diasCobertura() != null ? i.diasCobertura() : "").append(';')
              .append(i.sinMovimiento() ? "SI" : "").append(';').append(i.clasificacion()).append("\r\n");
        }
        return sb.toString();
    }

    private String csvPurchase(PurchaseSuggestionsResponse r) {
        StringBuilder sb = new StringBuilder(bom())
                .append("codigo;articulo;categoria;unidades_30d;stock;minimo;dias_cobertura;sugerido;urgencia\r\n");
        for (PurchaseSuggestionItem i : r.items()) {
            sb.append(i.codigoArticulo()).append(';').append(esc(i.articulo())).append(';')
              .append(esc(i.categoria())).append(';').append(i.unidadesPeriodo()).append(';')
              .append(i.stockActual()).append(';').append(i.minimo()).append(';')
              .append(i.diasCobertura() != null ? i.diasCobertura() : "").append(';')
              .append(i.sugerido()).append(';').append(i.urgencia()).append("\r\n");
        }
        return sb.toString();
    }

    private String csvCategory(CategorySalesResponse r) {
        StringBuilder sb = new StringBuilder(bom())
                .append("categoria;anio;trimestre;unidades;venta;metodo_proyeccion\r\n");
        for (CategorySalesRow f : r.filas()) {
            sb.append(esc(f.categoria())).append(';').append(f.anio()).append(';').append(f.trimestre())
              .append(';').append(f.unidades()).append(';').append(f.venta()).append(";\r\n");
        }
        for (CategoryProjection p : r.proyecciones()) {
            sb.append(esc(p.categoria())).append(';').append(p.anio()).append(';').append(p.trimestre())
              .append(" py;").append(p.unidades() != null ? p.unidades() : "").append(';')
              .append(p.venta()).append(';').append(p.metodo()).append("\r\n");
        }
        return sb.toString();
    }
}
