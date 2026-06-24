package com.serendipity.backend.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AccontoMovimentoDto(
        Long id,
        Long utenteId,
        String utenteNome,
        String utenteCognome,
        int mese,
        int anno,
        BigDecimal importo,
        String note,
        LocalDate dataMovimento,
        LocalDateTime createdAt
) {
}
