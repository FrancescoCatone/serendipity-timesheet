package com.serendipity.backend.model.dto.report;

import java.util.List;

/**
 * È il DTO principale del report per cliente.
 * Contiene i dati generali del report: cliente, periodo, totale ore, totale costo e la lista di dettaglio.
 *
 * @param clienteId
 * @param clienteNome
 * @param mese
 * @param anno
 * @param totaleOre
 * @param totaleCosto
 * @param dettaglioDipendenti
 */
public record ReportClienteDto(
        Long clienteId,
        String clienteNome,
        Integer mese,
        Integer anno,
        double totaleOre,
        double totaleCosto,
        List<ReportClienteDipendenteDto> dettaglioDipendenti
) {
}
