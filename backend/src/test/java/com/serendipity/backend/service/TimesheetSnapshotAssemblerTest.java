package com.serendipity.backend.service;

import com.serendipity.backend.export.TimesheetSnapshotAssembler;
import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetSnapshotAssemblerTest {

    @Mock
    private TimesheetRepository tsRepo;
    @Mock
    private TimesheetRigaRepository rigaRepo;

    @InjectMocks
    private TimesheetSnapshotAssembler assembler;

    private Timesheet ts(long id, String nome, String cognome, int mese, TimesheetStato stato) {
        Utente u = new Utente();
        u.setId(123L);
        u.setNome(nome);
        u.setCognome(cognome);
        u.setEmail("x@y.z");
        u.setPassword("hash");
        u.setRuolo(Ruolo.DIPENDENTE);

        Timesheet t = new Timesheet();
        t.setId(id);
        t.setUtente(u);
        t.setMese(mese);
        t.setAnno(2025);
        t.setStato(stato);
        return t;
    }

    private TimesheetRiga riga(Cliente c, LocalDate data, int ore, int min, double orario, double costo) {
        TimesheetRiga r = new TimesheetRiga();
        r.setCliente(c);
        r.setData(data);
        r.setOre(ore);
        r.setMinuti(min);
        r.setOrario(orario);
        r.setCostoOrario(costo);
        return r;
    }

    @Test
    void build_notFound_throws404() {
        when(tsRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assembler.build(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Timesheet non trovato");

        verify(tsRepo).findById(99L);
        verifyNoInteractions(rigaRepo);
    }

    @Test
    void build_illegalState_ifNotClosed() {
        Timesheet t = ts(1L, "Mario", "Rossi", 5, TimesheetStato.APERTO);
        when(tsRepo.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> assembler.build(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stato CHIUSO");

        verify(tsRepo).findById(1L);
        verifyNoInteractions(rigaRepo);
    }

    @Test
    void build_ok_mapsRowsAndTotals_andFormatsMonthAndName() {
        // timesheet CHIUSO
        Timesheet t = ts(2L, "Anna", "Bianchi", 1, TimesheetStato.CHIUSO);
        when(tsRepo.findById(2L)).thenReturn(Optional.of(t));

        // righe ordinate restituite dal repo
        Cliente cli = new Cliente();
        cli.setNome("Globex");

        var r1 = riga(cli, LocalDate.of(2025, 1, 10), 1, 30, 1.5, 45.555); // sarà arrotondato a 45.56
        var r2 = riga(cli, LocalDate.of(2025, 1, 11), 2, 0, 2.0, 60.0);

        when(rigaRepo.findByTimesheetIdOrdered(2L)).thenReturn(List.of(r1, r2));
        when(rigaRepo.sumTotaliByTimesheetId(2L)).thenReturn(new TotaliDto(3.5, 105.555));

        TimesheetSnapshotDto dto = assembler.build(2L);

        // Nome utente concatenato
        assertThat(dto.utenteNomeCompleto()).isEqualTo("Anna Bianchi");
        // Mese in italiano capitalizzato
        assertThat(dto.meseNome()).isEqualTo("Gennaio");
        assertThat(dto.anno()).isEqualTo(2025);

        // Data generazione presente
        assertThat(dto.dataGenerazione()).isNotNull();

        // Righe mappate e arrotondate a 2 decimali
        assertThat(dto.righe()).hasSize(2);
        assertThat(dto.righe().getFirst().clienteNome()).isEqualTo("Globex");
        assertThat(dto.righe().getFirst().orario()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(dto.righe().getFirst().costoOrario()).isEqualByComparingTo(new BigDecimal("45.56"));

        // Totali arrotondati
        assertThat(dto.totaleOrario()).isEqualByComparingTo(new BigDecimal("3.50"));
        assertThat(dto.totaleCosto()).isEqualByComparingTo(new BigDecimal("105.56"));

        verify(tsRepo).findById(2L);
        verify(rigaRepo).findByTimesheetIdOrdered(2L);
        verify(rigaRepo).sumTotaliByTimesheetId(2L);
    }
}
