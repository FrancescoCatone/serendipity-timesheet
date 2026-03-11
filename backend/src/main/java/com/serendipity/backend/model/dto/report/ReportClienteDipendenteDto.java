package com.serendipity.backend.model.dto.report;

/**
 * È una riga di dettaglio dentro il report cliente.
 * Serve a dire, per quel cliente, quanto ha lavorato ogni dipendente e quanto costo ha generato.
 *
 * @param utenteId
 * @param nome
 * @param cognome
 * @param oreTotali
 * @param costoTotale
 */
public record ReportClienteDipendenteDto(
        Long utenteId,
        String nome,
        String cognome,
        double oreTotali,
        double costoTotale
) {
}
