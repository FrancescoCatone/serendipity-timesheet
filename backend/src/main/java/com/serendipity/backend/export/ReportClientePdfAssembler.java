package com.serendipity.backend.export;

import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.report.ReportClientePdfAnnualSnapshotDto;
import com.serendipity.backend.model.dto.report.ReportClientePdfDettaglioRawDto;
import com.serendipity.backend.model.dto.report.ReportClientePdfSnapshotDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ReportClientePdfAssembler {

    private final ClienteRepository clienteRepository;
    private final TimesheetRigaRepository rigaRepository;

    public ReportClientePdfAssembler(ClienteRepository clienteRepository,
                                     TimesheetRigaRepository rigaRepository) {
        this.clienteRepository = clienteRepository;
        this.rigaRepository = rigaRepository;
    }

    public ReportClientePdfSnapshotDto build(Long clienteId, int mese, int anno, boolean mostraCosto) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));

        List<ReportClientePdfSnapshotDto.Riga> righe = rigaRepository.reportClientePdfDettaglio(clienteId, mese, anno)
                .stream()
                .map(this::toSnapshotRow)
                .toList();

        TotaliDto totali = rigaRepository.totaleReportCliente(clienteId, mese, anno);

        return new ReportClientePdfSnapshotDto(
                cliente.getId(),
                cliente.getNome(),
                mese,
                Month.of(mese).getDisplayName(TextStyle.FULL, Locale.ITALIAN),
                anno,
                LocalDateTime.now(),
                righe,
                scale(totali.totaleOrario()),
                scale(totali.totaleCosto()),
                mostraCosto
        );
    }

    public ReportClientePdfAnnualSnapshotDto buildAnnual(Long clienteId, int anno, boolean mostraCosto) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));

        List<ReportClientePdfAnnualSnapshotDto.MeseReport> mesi = new ArrayList<>();
        for (int mese = 1; mese <= 12; mese++) {
            List<ReportClientePdfSnapshotDto.Riga> righe = rigaRepository.reportClientePdfDettaglio(clienteId, mese, anno)
                    .stream()
                    .map(this::toSnapshotRow)
                    .toList();

            TotaliDto totaliMese = rigaRepository.totaleReportCliente(clienteId, mese, anno);
            BigDecimal totaleOrarioMese = scale(totaliMese.totaleOrario());
            BigDecimal totaleCostoMese = scale(totaliMese.totaleCosto());

            if (righe.isEmpty() && totaleOrarioMese.signum() == 0 && totaleCostoMese.signum() == 0) {
                continue;
            }

            mesi.add(new ReportClientePdfAnnualSnapshotDto.MeseReport(
                    mese,
                    Month.of(mese).getDisplayName(TextStyle.FULL, Locale.ITALIAN),
                    righe,
                    totaleOrarioMese,
                    totaleCostoMese
            ));
        }

        TotaliDto totaliAnnuali = rigaRepository.totaleReportCliente(clienteId, null, anno);

        return new ReportClientePdfAnnualSnapshotDto(
                cliente.getId(),
                cliente.getNome(),
                anno,
                LocalDateTime.now(),
                mesi,
                scale(totaliAnnuali.totaleOrario()),
                scale(totaliAnnuali.totaleCosto()),
                mostraCosto
        );
    }

    private ReportClientePdfSnapshotDto.Riga toSnapshotRow(ReportClientePdfDettaglioRawDto raw) {
        long totalMinutes = raw.totaleMinuti() == null ? 0 : raw.totaleMinuti();
        int ore = (int) (totalMinutes / 60);
        int minuti = (int) (totalMinutes % 60);

        return new ReportClientePdfSnapshotDto.Riga(
                raw.data(),
                (raw.nome() + " " + raw.cognome()).trim(),
                ore,
                minuti,
                scale(raw.orarioTotale()),
                scale(raw.costoTotale())
        );
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
