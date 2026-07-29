package com.serendipity.backend.service;

import com.serendipity.backend.model.dto.AccontiSummaryDto;
import com.serendipity.backend.model.dto.AccontoMovimentoDto;
import com.serendipity.backend.model.dto.create.CreaAccontoMovimentoDto;
import com.serendipity.backend.model.entity.AccontoMovimento;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.AccontoMovimentoTipo;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.AccontoMovimentoRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccontiServiceTest {

    @Mock
    private AccontoMovimentoRepository repository;
    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private TimesheetRigaRepository timesheetRigaRepository;

    @InjectMocks
    private AccontiService service;

    @Test
    void summary_closedMonth_usesMaturatoAndNegativeSaldoWhenNeeded() {
        Utente utente = utente(7L, "Mario", "Rossi");
        Timesheet timesheet = timesheet(utente, 3, 2026, TimesheetStato.CHIUSO);

        when(utenteRepository.findById(7L)).thenReturn(Optional.of(utente));
        when(timesheetRepository.findByUtenteIdAndMeseAndAnno(7L, 3, 2026)).thenReturn(Optional.of(timesheet));
        when(timesheetRigaRepository.sumCostoByUtenteIdAndMeseAndAnno(7L, 3, 2026)).thenReturn(200.0);
        when(repository.findByUtenteIdAndMeseAndAnnoOrderByCreatedAtDescIdDesc(7L, 3, 2026)).thenReturn(List.of(
                movimento(1L, utente, 3, 2026, "250.00"),
                movimento(2L, utente, 3, 2026, "20.00")
        ));

        AccontiSummaryDto out = service.summary(7L, 3, 2026);

        assertThat(out.timesheetStato()).isEqualTo("CHIUSO");
        assertThat(out.maturato()).isEqualByComparingTo("200.00");
        assertThat(out.totaleAcconti()).isEqualByComparingTo("270.00");
        assertThat(out.totaleMovimenti()).isEqualByComparingTo("270.00");
        assertThat(out.saldoResiduo()).isEqualByComparingTo("-70.00");
        assertThat(out.movimenti()).hasSize(2);
    }

    @Test
    void summary_openMonth_exposesCurrentMaturato() {
        Utente utente = utente(7L, "Mario", "Rossi");
        Timesheet timesheet = timesheet(utente, 3, 2026, TimesheetStato.APERTO);

        when(utenteRepository.findById(7L)).thenReturn(Optional.of(utente));
        when(timesheetRepository.findByUtenteIdAndMeseAndAnno(7L, 3, 2026)).thenReturn(Optional.of(timesheet));
        when(timesheetRigaRepository.sumCostoByUtenteIdAndMeseAndAnno(7L, 3, 2026)).thenReturn(125.0);
        when(repository.findByUtenteIdAndMeseAndAnnoOrderByCreatedAtDescIdDesc(7L, 3, 2026)).thenReturn(List.of());

        AccontiSummaryDto out = service.summary(7L, 3, 2026);

        assertThat(out.timesheetStato()).isEqualTo("APERTO");
        assertThat(out.maturato()).isEqualByComparingTo("125.00");
        assertThat(out.saldoResiduo()).isEqualByComparingTo("125.00");
    }

    @Test
    void create_ok_persistsMovement() {
        Utente utente = utente(7L, "Mario", "Rossi");
        when(utenteRepository.findById(7L)).thenReturn(Optional.of(utente));
        when(repository.save(any(AccontoMovimento.class))).thenAnswer(invocation -> {
            AccontoMovimento entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0));
            return entity;
        });

        CreaAccontoMovimentoDto dto = new CreaAccontoMovimentoDto();
        dto.setUtenteId(7L);
        dto.setMese(3);
        dto.setAnno(2026);
        dto.setImporto(new BigDecimal("300"));
        dto.setDataMovimento(LocalDate.of(2026, 3, 20));
        dto.setNote("anticipo");

        AccontoMovimentoDto out = service.create(dto);

        assertThat(out.id()).isEqualTo(10L);
        assertThat(out.importo()).isEqualByComparingTo("300.00");

        ArgumentCaptor<AccontoMovimento> captor = ArgumentCaptor.forClass(AccontoMovimento.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getImporto()).isEqualByComparingTo("300.00");
        assertThat(captor.getValue().getTipo()).isEqualTo(AccontoMovimentoTipo.ACCONTO);
    }

    @Test
    void create_invalidDateOutsidePeriod_throws() {
        CreaAccontoMovimentoDto dto = new CreaAccontoMovimentoDto();
        dto.setUtenteId(7L);
        dto.setMese(3);
        dto.setAnno(2026);
        dto.setImporto(new BigDecimal("300"));
        dto.setDataMovimento(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data movimento");
    }

    @Test
    void summary_missingUtente_throws() {
        when(utenteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summary(99L, 3, 2026))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Utente non trovato");
    }

    @Test
    void delete_existingMovement_removesIt() {
        AccontoMovimento movimento = movimento(5L, utente(7L, "Mario", "Rossi"), 3, 2026, "10.00");
        when(repository.findById(5L)).thenReturn(Optional.of(movimento));

        service.delete(5L);

        verify(repository).delete(movimento);
    }

    @Test
    void delete_missingMovement_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Movimento acconto non trovato");
    }

    private Utente utente(Long id, String nome, String cognome) {
        Utente utente = new Utente();
        utente.setId(id);
        utente.setNome(nome);
        utente.setCognome(cognome);
        return utente;
    }

    private Timesheet timesheet(Utente utente, int mese, int anno, TimesheetStato stato) {
        Timesheet timesheet = new Timesheet();
        timesheet.setUtente(utente);
        timesheet.setMese(mese);
        timesheet.setAnno(anno);
        timesheet.setStato(stato);
        return timesheet;
    }

    private AccontoMovimento movimento(Long id,
                                       Utente utente,
                                       int mese,
                                       int anno,
                                       String importo) {
        AccontoMovimento movimento = new AccontoMovimento();
        movimento.setId(id);
        movimento.setUtente(utente);
        movimento.setMese(mese);
        movimento.setAnno(anno);
        movimento.setTipo(AccontoMovimentoTipo.ACCONTO);
        movimento.setImporto(new BigDecimal(importo));
        movimento.setDataMovimento(LocalDate.of(2026, mese, 10));
        movimento.setCreatedAt(LocalDateTime.of(2026, mese, 10, 9, 0));
        return movimento;
    }
}
