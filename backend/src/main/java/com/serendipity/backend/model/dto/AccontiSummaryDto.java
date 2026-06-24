package com.serendipity.backend.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccontiSummaryDto(
        Long utenteId,
        String utenteNome,
        String utenteCognome,
        int mese,
        int anno,
        String timesheetStato,
        BigDecimal maturato,
        BigDecimal totaleAcconti,
        BigDecimal totaleMovimenti,
        BigDecimal saldoResiduo,
        List<AccontoMovimentoDto> movimenti
) {
}
