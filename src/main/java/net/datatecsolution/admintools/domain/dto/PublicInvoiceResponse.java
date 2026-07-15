package net.datatecsolution.admintools.domain.dto;

/**
 * US-100 — respuesta del endpoint público del QR: la factura + el membrete
 * de la empresa (nombre/RTN/dirección — lo mismo que ya va impreso en cada
 * ticket), para que la página pública reimprima con membrete sin exponer
 * /company sin autenticación. El acceso sigue gateado por el token HMAC.
 */
public record PublicInvoiceResponse(
        InvoiceAdminDetailResponse invoice,
        CompanyResponse empresa
) {
}
