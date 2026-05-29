package com.serendipity.backend.model.dto.report;

/**
 * Riga di dettaglio del report giornaliero per cliente.
 * Indica quali dipendenti hanno lavorato per il cliente in una specifica data.
 *
 * @param utenteId
 * @param nome
 * @param cognome
 * @param oreTotali
 * @param costoTotale
 */
public record ReportClienteGiornoDipendenteDto(
        Long utenteId,
        String nome,
        String cognome,
        double oreTotali,
        double costoTotale
) {
}
