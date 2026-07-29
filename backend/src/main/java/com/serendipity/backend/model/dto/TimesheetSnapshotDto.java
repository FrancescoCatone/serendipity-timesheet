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
        int totaleOre,
        int totaleMinuti,
        BigDecimal totaleOrario,
        BigDecimal totaleCosto,
        BigDecimal maturatoAcconti,
        BigDecimal totaleAcconti,
        BigDecimal saldoResiduo,
        List<AccontoMovimento> accontiMovimenti
) {
    public record Riga(
            String clienteNome,
            LocalDate data,
            int ore,
            int minuti,
            BigDecimal orario,
            BigDecimal costoOrario,
            boolean festivo
    ) {
        public Riga(
                String clienteNome,
                LocalDate data,
                int ore,
                int minuti,
                BigDecimal orario,
                BigDecimal costoOrario
        ) {
            this(clienteNome, data, ore, minuti, orario, costoOrario, false);
        }
    }

    public record AccontoMovimento(
            LocalDate dataMovimento,
            BigDecimal importo,
            String note,
            LocalDateTime registratoIl
    ) {
    }
}
