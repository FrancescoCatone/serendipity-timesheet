package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.export.TimesheetSnapshotAssembler;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class TimesheetExportService {

    private final TimesheetRepository tsRepo;
    private final UtenteRepository utenteRepository;
    private final TimesheetSnapshotAssembler assembler;
    private final PdfRenderer renderer;

    public TimesheetExportService(TimesheetRepository tsRepo,
                                  UtenteRepository utenteRepository,
                                  TimesheetSnapshotAssembler assembler,
                                  PdfRenderer renderer) {
        this.tsRepo = tsRepo;
        this.utenteRepository = utenteRepository;
        this.assembler = assembler;
        this.renderer = renderer;
    }

    public ExportFile export(long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id);

        if (ts.getStato() != TimesheetStato.CHIUSO) {
            throw new IllegalStateException("Puoi esportare il timesheet solo quando è nello stato CHIUSO");
        }

        var snap = assembler.build(id);
        byte[] pdf = renderer.render(snap);
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Errore durante la generazione del PDF");
        }

        String filename = buildFilename(ts);
        return new ExportFile(pdf, filename);
    }

    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

    private Timesheet mustReadOwnedOrAdmin(long id) {
        Timesheet ts = tsRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));

        if (!currentUserIsAdmin()) {
            if (!ts.getUtente().getId().equals(getCurrentUserId())) {
                throw new EntityNotFoundException("Timesheet non trovato");
            }
        }

        return ts;
    }

    private String buildFilename(Timesheet ts) {
        String nome = ts.getUtente().getNome();
        String cognome = ts.getUtente().getCognome();
        String mese = Month.of(ts.getMese()).getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        String base = (nome + "_" + cognome + "_" + mese + "_" + ts.getAnno())
                .toLowerCase(Locale.ITALIAN)
                .replace(' ', '_');

        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9_\\-.]", "");

        return normalized + ".pdf";
    }

    public record ExportFile(byte[] content, String filename) {
    }
}
