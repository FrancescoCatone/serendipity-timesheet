package com.serendipity.backend.export;

import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Component
public class TimesheetSnapshotAssembler {
    private final TimesheetRepository tsRepo;
    private final TimesheetRigaRepository rigaRepo;

    public TimesheetSnapshotAssembler(TimesheetRepository tsRepo, TimesheetRigaRepository rigaRepo) {
        this.tsRepo = tsRepo;
        this.rigaRepo = rigaRepo;
    }

    public TimesheetSnapshotDto build(Long timesheetId) {
        Timesheet ts = tsRepo.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));

        if (ts.getStato() != TimesheetStato.CHIUSO) {
            throw new IllegalStateException("Puoi esportare il timesheet solo quando è nello stato CHIUSO");
        }

        List<TimesheetSnapshotDto.Riga> righeDto = rigaRepo.findByTimesheetIdOrdered(timesheetId).stream()
                .map(r -> new TimesheetSnapshotDto.Riga(
                        r.getCliente().getNome(),
                        r.getData(),
                        r.getOre(),
                        r.getMinuti(),
                        BigDecimal.valueOf(r.getOrario()).setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(r.getCostoOrario()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        TotaliDto tot = rigaRepo.sumTotaliByTimesheetId(timesheetId);

        var totOrario = BigDecimal.valueOf(tot.totaleOrario())
                .setScale(2, RoundingMode.HALF_UP);
        var totCosto = BigDecimal.valueOf(tot.totaleCosto())
                .setScale(2, RoundingMode.HALF_UP);

        String meseNome = java.time.Month.of(ts.getMese())
                .getDisplayName(TextStyle.FULL, Locale.ITALIAN);

        String nomeCompleto = ts.getUtente().getNome() + " " + ts.getUtente().getCognome();

        return new TimesheetSnapshotDto(
                nomeCompleto,
                capitalize(meseNome),
                ts.getAnno(),
                LocalDateTime.now(),
                righeDto,
                totOrario,
                totCosto
        );
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
