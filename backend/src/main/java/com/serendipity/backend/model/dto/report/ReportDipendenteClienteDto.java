package com.serendipity.backend.model.dto.report;

/**
 * È una riga di dettaglio dentro il report dipendente.
 * Serve a dire, per quel dipendente, su quali clienti ha lavorato e con quali totali di ore e costo.
 *
 * @param clienteId
 * @param clienteNome
 * @param oreTotali
 * @param costoTotale
 */
public record ReportDipendenteClienteDto(
        Long clienteId,
        String clienteNome,
        double oreTotali,
        double costoTotale) {
}
