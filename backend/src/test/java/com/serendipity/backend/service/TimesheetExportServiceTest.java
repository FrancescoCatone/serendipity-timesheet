package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.export.TimesheetSnapshotAssembler;
import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetExportServiceTest {

    @Mock
    private TimesheetRepository tsRepo;

    @Mock
    private TimesheetRigaRepository rigaRepo;

    @Mock
    private TimesheetSnapshotAssembler assembler;

    @Mock
    private PdfRenderer renderer;

    @InjectMocks
    private TimesheetExportService service;

    private Timesheet tsChiuso(String nome, String cognome, int mese) {
        Utente u = new Utente();
        u.setNome(nome);
        u.setCognome(cognome);

        Timesheet ts = new Timesheet();
        ts.setUtente(u);
        ts.setMese(mese);
        ts.setAnno(2025);
        ts.setStato(TimesheetStato.CHIUSO);
        return ts;
    }

    private Timesheet tsAperto() {
        Utente u = new Utente();
        u.setNome("Mario");
        u.setCognome("Rossi");

        Timesheet ts = new Timesheet();
        ts.setUtente(u);
        ts.setMese(5);
        ts.setAnno(2025);
        ts.setStato(TimesheetStato.APERTO);
        return ts;
    }

    private TimesheetSnapshotDto dummySnapshot(String meseNome) {
        return new TimesheetSnapshotDto(
                "dummy-user",
                meseNome,
                2025,
                LocalDateTime.now(),
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    @BeforeEach
    void setLocale() {
        // assicura mesi in italiano se l'ambiente di build ha locale diverso
        Locale.setDefault(Locale.ITALIAN);

        // imposta un utente ADMIN nel SecurityContext:
        // currentUserIsAdmin() → true, quindi getCurrentUserId() non viene mai chiamato
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@serendipity.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /* ---------------------- not found ---------------------- */

    @Test
    void export_notFound_throwsEntityNotFound() {
        long id = 99L;
        when(tsRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.export(id));

        verify(tsRepo).findById(id);
        verifyNoInteractions(assembler, renderer);
    }

    /* ---------------------- stato != CHIUSO ---------------------- */

    @Test
    void export_notClosed_throwsIllegalState() {
        long id = 7L;
        Timesheet ts = tsAperto();
        when(tsRepo.findById(id)).thenReturn(Optional.of(ts));

        assertThrows(IllegalStateException.class, () -> service.export(id));

        verify(tsRepo).findById(id);
        verifyNoInteractions(assembler, renderer);
    }

    /* ---------------------- OK: flusso completo ---------------------- */

    @Test
    void export_ok_simpleAscii_buildsExpectedFilenameAndPdf() {
        long id = 1L;
        Timesheet ts = tsChiuso("Mario", "Rossi", 5); // maggio
        when(tsRepo.findById(id)).thenReturn(Optional.of(ts));

        var snap = dummySnapshot("maggio");
        when(assembler.build(id)).thenReturn(snap);

        byte[] pdf = new byte[]{1, 2, 3};
        when(renderer.render(snap)).thenReturn(pdf);

        TimesheetExportService.ExportFile out = service.export(id);

        assertThat(out.content()).isEqualTo(pdf);
        assertThat(out.filename()).isEqualTo("mario_rossi_maggio_2025.pdf");

        verify(tsRepo).findById(id);
        verify(assembler).build(id);
        verify(renderer).render(snap);
    }

    @Test
    void export_ok_filenameNormalization_removesAccentsAndUnsafeChars() {
        long id = 2L;
        // nome/cognome con spazi, apostrofo e accenti
        Timesheet ts = tsChiuso("Álvaro José", "D'Amìcò", 1); // gennaio
        when(tsRepo.findById(id)).thenReturn(Optional.of(ts));

        var snap = dummySnapshot("gennaio");
        when(assembler.build(id)).thenReturn(snap);

        byte[] pdf = new byte[]{9, 9, 9};
        when(renderer.render(snap)).thenReturn(pdf);

        TimesheetExportService.ExportFile out = service.export(id);

        // atteso: minuscolo, spazi -> '-', apostrofo rimosso, accenti rimossi
        assertThat(out.filename()).isEqualTo("alvaro_jose_damico_gennaio_2025.pdf");
        assertThat(out.content()).isEqualTo(pdf);

        verify(tsRepo).findById(id);
        verify(assembler).build(id);
        verify(renderer).render(snap);
    }
}
