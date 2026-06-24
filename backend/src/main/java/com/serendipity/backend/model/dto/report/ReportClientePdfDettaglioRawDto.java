package com.serendipity.backend.model.dto.report;

import java.time.LocalDate;

public record ReportClientePdfDettaglioRawDto(
        LocalDate data,
        Long utenteId,
        String nome,
        String cognome,
        Long totaleMinuti,
        double orarioTotale,
        double costoTotale
) {
}
