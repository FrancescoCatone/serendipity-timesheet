package com.serendipity.backend.model.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportClientePdfAnnualSnapshotDto(
        Long clienteId,
        String clienteNome,
        int anno,
        LocalDateTime dataGenerazione,
        List<MeseReport> mesi,
        BigDecimal totaleOrario,
        BigDecimal totaleCosto
) {
    public record MeseReport(
            int mese,
            String meseNome,
            List<ReportClientePdfSnapshotDto.Riga> righe,
            BigDecimal totaleOrario,
            BigDecimal totaleCosto
    ) {
    }
}
