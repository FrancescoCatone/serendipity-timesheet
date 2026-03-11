package com.serendipity.backend.model.dto.report;

import java.util.List;

/**
 * È il DTO principale del report per dipendente.
 * Contiene i dati generali del report: dipendente, periodo, totale ore, totale costo e la lista di dettaglio.
 *
 * @param utenteId
 * @param nome
 * @param cognome
 * @param mese
 * @param anno
 * @param totaleOre
 * @param totaleCosto
 * @param dettaglioClienti
 */
public record ReportDipendenteDto(
        Long utenteId,
        String nome,
        String cognome,
        Integer mese,
        Integer anno,
        double totaleOre,
        double totaleCosto,
        List<ReportDipendenteClienteDto> dettaglioClienti
) {
}
