package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.export.TimesheetSnapshotAssembler;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class TimesheetExportService {

    private final TimesheetRepository tsRepo;
    private final TimesheetSnapshotAssembler assembler;
    private final PdfRenderer renderer;

    public TimesheetExportService(TimesheetRepository tsRepo,
                                  TimesheetSnapshotAssembler assembler,
                                  PdfRenderer renderer) {
        this.tsRepo = tsRepo;
        this.assembler = assembler;
        this.renderer = renderer;
    }

    public ExportFile export(long id) {
        Timesheet ts = tsRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));

        if (ts.getStato() != TimesheetStato.CHIUSO) {
            throw new IllegalStateException("Puoi esportare il timesheet solo quando è nello stato CHIUSO");
        }

        // L’assembler in più ti garantisce righe ordinate + totali
        var snap = assembler.build(id);
        byte[] pdf = renderer.render(snap);

        String filename = buildFilename(ts);
        return new ExportFile(pdf, filename);
    }

    private String buildFilename(Timesheet ts) {
        String nome = ts.getUtente().getNome();
        String cognome = ts.getUtente().getCognome();
        String mese = Month.of(ts.getMese()).getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        String base = (nome + "_" + cognome + "_" + mese + "_" + ts.getAnno())
                .toLowerCase(Locale.ITALIAN)
                .replace(' ', '_');

        // normalizza: rimuove accenti e caratteri non safe
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9_\\-\\.]", "");

        return normalized + ".pdf";
    }

    public record ExportFile(byte[] content, String filename) {
    }
}
