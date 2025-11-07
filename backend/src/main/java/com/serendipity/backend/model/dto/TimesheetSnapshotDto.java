package com.serendipity.backend.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TimesheetSnapshotDto(
        String utenteNomeCompleto,
        String meseNome,            // es. "Gennaio"
        int anno,
        LocalDateTime dataGenerazione,
        List<Riga> righe,
        BigDecimal totaleOrario,
        BigDecimal totaleCosto
) {
    public record Riga(
            String clienteNome,
            LocalDate data,
            int ore,
            int minuti,
            BigDecimal orario,
            BigDecimal costoOrario
    ) {
    }
}
