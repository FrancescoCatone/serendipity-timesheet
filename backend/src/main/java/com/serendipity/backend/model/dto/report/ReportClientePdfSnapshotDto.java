package com.serendipity.backend.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReportClientePdfSnapshotDto(
        Long clienteId,
        String clienteNome,
        int mese,
        String meseNome,
        int anno,
        LocalDateTime dataGenerazione,
        List<Riga> righe,
        BigDecimal totaleOrario,
        BigDecimal totaleCosto,
        boolean mostraCosto
) {
    public record Riga(
            LocalDate data,
            String dipendenteNomeCompleto,
            int ore,
            int minuti,
            BigDecimal orario,
            BigDecimal costo
    ) {
    }
}
