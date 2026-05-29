package com.serendipity.backend.model.dto.report;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO principale del report cliente per uno specifico giorno.
 *
 * @param clienteId
 * @param clienteNome
 * @param data
 * @param totaleOre
 * @param totaleCosto
 * @param dettaglioDipendenti
 */
public record ReportClienteGiornoDto(
        Long clienteId,
        String clienteNome,
        LocalDate data,
        double totaleOre,
        double totaleCosto,
        List<ReportClienteGiornoDipendenteDto> dettaglioDipendenti
) {
}
