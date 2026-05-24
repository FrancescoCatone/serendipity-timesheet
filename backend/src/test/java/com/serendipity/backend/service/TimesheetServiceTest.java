package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetMapper;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import com.serendipity.backend.support.SystemClienti;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @InjectMocks
    private TimesheetService service;

    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TimesheetRigaRepository rigaRepository;
    @Mock
    private TimesheetMapper mapper;
    @Mock
    private CalendarioFestivitaService calendarioFestivitaService;

    private Utente admin;
    private Utente user;

    @BeforeEach
    void setup() {
        // utenti base
        admin = new Utente();
        setUtente(admin, 100L, "admin@acme.it");

        user = new Utente();
        setUtente(user, 200L, "user@acme.it");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // Helpers -----------------------------------------------------------------

    private void setUtente(Utente u, Long id, String email) {
        // poiché Utente è una entity del tuo progetto, assumo abbia setId/setEmail.
        try {
            var idField = Utente.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
        } catch (Exception ignore) {
        }
        try {
            var emailField = Utente.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(u, email);
        } catch (Exception ignore) {
        }
        // Se hai i setter pubblici usa direttamente u.setId(id); u.setEmail(email);
    }

    private void authAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                admin.getEmail(), "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsUser() {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), "x",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Timesheet ts(Long id, int mese, Utente owner, TimesheetStato stato) {
        Timesheet t = new Timesheet();
        t.setId(id);
        t.setMese(mese);
        t.setAnno(2025);
        t.setUtente(owner);
        t.setStato(stato);
        return t;
    }

    private TimesheetDto dtoFrom(Timesheet t) {
        String nomeCompleto = t.getUtente() != null
                ? t.getUtente().getNome() + " " + t.getUtente().getCognome()
                : null;
        return new TimesheetDto(t.getId(), t.getMese(), t.getAnno(), t.getDataCompilazione(),
                t.getUtente() != null ? t.getUtente().getId() : null,
                nomeCompleto,
                t.getStato() != null ? t.getStato().name() : null);
    }

    private void stubCurrentUserLookupAsUser() {
        when(utenteRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // findById ----------------------------------------------------------------

    @Test
    void findById_ok() {
        authAsAdmin();
        var t = ts(1L, 10, admin, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));
        when(mapper.toDto(t)).thenReturn(dtoFrom(t));

        var out = service.findById(1L);

        assertThat(out.id()).isEqualTo(1L);
        assertThat(out.mese()).isEqualTo(10);
    }

    @Test
    void findById_notFound() {
        authAsAdmin();
        when(timesheetRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // create ------------------------------------------------------------------

    @Test
    void create_ok_selfUser() {
        authAsUser();

        stubCurrentUserLookupAsUser();

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setAnno(2025);
        dto.setMese(10);
        dto.setUtenteId(200L); // stesso user

        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(200L, 10, 2025)).thenReturn(false);
        when(utenteRepository.findById(200L)).thenReturn(Optional.of(user));

        var saved = ts(10L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.create(dto);

        assertThat(out.id()).isEqualTo(10L);
        verify(timesheetRepository).save(any(Timesheet.class));
    }

    @Test
    void create_ok_asAdmin_forAnotherUser() {
        authAsAdmin();

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setAnno(2025);
        dto.setMese(9);
        dto.setUtenteId(200L); // admin può creare per altri

        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(200L, 9, 2025)).thenReturn(false);
        when(utenteRepository.findById(200L)).thenReturn(Optional.of(user));

        var saved = ts(11L, 9, user, TimesheetStato.APERTO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.create(dto);

        assertThat(out.id()).isEqualTo(11L);
    }

    @Test
    void create_conflict_duplicate() {
        authAsAdmin();
        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setAnno(2025);
        dto.setMese(10);
        dto.setUtenteId(200L);

        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(200L, 10, 2025)).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void create_forbidden_otherUser_whenNotAdmin() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setAnno(2025);
        dto.setMese(10);
        dto.setUtenteId(999L); // altro utente

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_userNotFound() {
        authAsAdmin();
        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setAnno(2025);
        dto.setMese(10);
        dto.setUtenteId(123L);

        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(123L, 10, 2025)).thenReturn(false);
        when(utenteRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // update ------------------------------------------------------------------

    @Test
    void update_ok_changeMonthYear_sameOwner() {
        authAsAdmin();
        var existing = ts(1L, 9, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.findById(200L)).thenReturn(Optional.of(user));
        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(200L, 10, 2025)).thenReturn(false);

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(10);
        dto.setAnno(2025);

        var saved = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.update(1L, dto);
        assertThat(out.mese()).isEqualTo(10);
    }

    @Test
    void update_notFound() {
        authAsAdmin();
        when(timesheetRepository.findById(999L)).thenReturn(Optional.empty());

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(10);
        dto.setAnno(2025);

        assertThatThrownBy(() -> service.update(999L, dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_illegal_whenClosed() {
        authAsAdmin();
        var existing = ts(1L, 9, user, TimesheetStato.CHIUSO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(9);
        dto.setAnno(2025);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
    }

    @Test
    void update_illegal_whenConfirmed_andNotAdmin() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var existing = ts(1L, 9, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(9);
        dto.setAnno(2025);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
    }

    @Test
    void update_conflict_whenChangingToExistingCombination() {
        authAsAdmin();
        var existing = ts(1L, 9, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(timesheetRepository.existsByUtenteIdAndMeseAndAnno(200L, 10, 2025)).thenReturn(true);

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(10);
        dto.setAnno(2025);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // delete ------------------------------------------------------------------

    @Test
    void delete_ok() {
        authAsAdmin();
        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));
        service.delete(1L);

        verify(timesheetRepository).delete(t);
    }

    @Test
    void delete_notFound() {
        authAsAdmin();
        when(timesheetRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // search ------------------------------------------------------------------

    @Test
    void search_argumentsInvalid() {
        authAsAdmin();
        assertThatThrownBy(() -> service.search(0, 2025, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(13, 2025, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_admin_noUserFilter_ok() {
        authAsAdmin();
        var t1 = ts(1L, 10, user, TimesheetStato.APERTO);
        var t2 = ts(2L, 10, admin, TimesheetStato.APERTO);

        // admin + utenteId=null → searchFiltered(10, 2025, null)
        when(timesheetRepository.searchFiltered(10, 2025, null)).thenReturn(List.of(t1, t2));
        when(mapper.toDto(t1)).thenReturn(dtoFrom(t1));
        when(mapper.toDto(t2)).thenReturn(dtoFrom(t2));

        var all = service.search(10, 2025, null);
        assertThat(all).hasSize(2);
    }

    @Test
    void search_admin_withUserFilter_ok() {
        authAsAdmin();
        var t1 = ts(1L, 10, user, TimesheetStato.APERTO);

        // admin + utenteId=200L → searchFiltered(10, 2025, 200L)
        when(timesheetRepository.searchFiltered(10, 2025, 200L)).thenReturn(List.of(t1));
        when(mapper.toDto(t1)).thenReturn(dtoFrom(t1));

        var onlyUser = service.search(10, 2025, 200L);
        assertThat(onlyUser).extracting(TimesheetDto::utenteId).containsExactly(200L);
    }

    @Test
    void search_nonAdmin_onlyOwn_ok() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var own = ts(9L, 10, user, TimesheetStato.APERTO);

        // non-admin → utenteId passato (999L) viene ignorato, si forza il proprio id (200L)
        when(timesheetRepository.searchFiltered(10, 2025, 200L)).thenReturn(List.of(own));
        when(mapper.toDto(own)).thenReturn(dtoFrom(own));

        var res = service.search(10, 2025, 999L /* ignorato per non-admin */);
        assertThat(res).hasSize(1);
        assertThat(res.getFirst().utenteId()).isEqualTo(200L);
    }

    @Test
    void search_notFound_throws() {
        authAsAdmin();
        when(timesheetRepository.searchFiltered(10, 2025, null)).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> service.search(10, 2025, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // anni/mesi disponibili ---------------------------------------------------

    @Test
    void anniDisponibili_admin_ok() {
        authAsAdmin();
        when(timesheetRepository.findDistinctAnni()).thenReturn(List.of(2025, 2024));

        assertThat(service.anniDisponibili()).containsExactly(2025, 2024);
    }

    @Test
    void anniDisponibili_user_onlyOwn() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        when(timesheetRepository.findDistinctAnniByUtenteId(200L)).thenReturn(List.of(2025));

        assertThat(service.anniDisponibili()).containsExactly(2025);
    }

    @Test
    void mesiDisponibiliPerAnno_admin_ok() {
        authAsAdmin();
        when(timesheetRepository.findDistinctMesiByAnno(2025)).thenReturn(List.of(9, 10));

        assertThat(service.mesiDisponibiliPerAnno(2025)).containsExactly(9, 10);
    }

    @Test
    void mesiDisponibiliPerAnno_user_onlyOwn() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        when(timesheetRepository.findDistinctMesiByAnnoAndUtenteId(2025, 200L)).thenReturn(List.of(10));

        assertThat(service.mesiDisponibiliPerAnno(2025)).containsExactly(10);
    }

    // findAllFiltered ---------------------------------------------------------

    @Test
    void findAllFiltered_admin_getsAll() {
        authAsAdmin();
        var t1 = ts(1L, 10, admin, TimesheetStato.APERTO);
        var t2 = ts(2L, 9, user, TimesheetStato.APERTO);

        when(timesheetRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(t1, t2));
        when(mapper.toDto(t1)).thenReturn(dtoFrom(t1));
        when(mapper.toDto(t2)).thenReturn(dtoFrom(t2));

        var res = service.findAllFiltered();
        assertThat(res).hasSize(2);
    }

    @Test
    void findAllFiltered_nonAdmin_getsOwn() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var own = ts(3L, 10, user, TimesheetStato.APERTO);

        when(timesheetRepository.findByUtenteIdOrderByAnnoAscMeseAsc(200L)).thenReturn(List.of(own));
        when(mapper.toDto(own)).thenReturn(dtoFrom(own));

        var res = service.findAllFiltered();
        assertThat(res).hasSize(1);
        assertThat(res.getFirst().utenteId()).isEqualTo(200L);
    }

    // conferma ----------------------------------------------------------------

    @Test
    void conferma_ok_whenAperto() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));
        when(rigaRepository.findDistinctDatesByTimesheetId(1L)).thenReturn(List.of(
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 2),
                LocalDate.of(2025, 10, 3),
                LocalDate.of(2025, 10, 4),
                LocalDate.of(2025, 10, 6),
                LocalDate.of(2025, 10, 7),
                LocalDate.of(2025, 10, 8),
                LocalDate.of(2025, 10, 9),
                LocalDate.of(2025, 10, 10),
                LocalDate.of(2025, 10, 11),
                LocalDate.of(2025, 10, 13),
                LocalDate.of(2025, 10, 14),
                LocalDate.of(2025, 10, 15),
                LocalDate.of(2025, 10, 16),
                LocalDate.of(2025, 10, 17),
                LocalDate.of(2025, 10, 18),
                LocalDate.of(2025, 10, 20),
                LocalDate.of(2025, 10, 21),
                LocalDate.of(2025, 10, 22),
                LocalDate.of(2025, 10, 23),
                LocalDate.of(2025, 10, 24),
                LocalDate.of(2025, 10, 25),
                LocalDate.of(2025, 10, 27),
                LocalDate.of(2025, 10, 28),
                LocalDate.of(2025, 10, 29),
                LocalDate.of(2025, 10, 30),
                LocalDate.of(2025, 10, 31)
        ));
        when(calendarioFestivitaService.isFestivo(any(LocalDate.class))).thenAnswer(inv ->
                inv.<LocalDate>getArgument(0).getDayOfWeek().getValue() == 7
        );
        Cliente nonLavorato = new Cliente();
        nonLavorato.setNome(SystemClienti.NON_LAVORATO);
        nonLavorato.setTariffaOraria(0);
        when(clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO)).thenReturn(Optional.of(nonLavorato));

        var saved = ts(1L, 10, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.conferma(1L);
        assertThat(out.stato()).isEqualTo(TimesheetStato.CONFERMATO.name());
        verify(rigaRepository).saveAll(argThat(rows -> {
            List<?> list = (List<?>) rows;
            return list.size() == 4;
        }));
    }

    @Test
    void conferma_illegal_whenRequiredDayMissing() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));
        when(rigaRepository.findDistinctDatesByTimesheetId(1L)).thenReturn(List.of(
                LocalDate.of(2025, 10, 1),
                LocalDate.of(2025, 10, 2)
        ));
        when(calendarioFestivitaService.isFestivo(any(LocalDate.class))).thenReturn(false);

        assertThatThrownBy(() -> service.conferma(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("giorni feriali del mese non compilati");
    }

    @Test
    void conferma_illegal_whenNotAperto() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.conferma(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APERTO");
    }

    @Test
    void update_illegal_whenConfirmed_evenForAdmin() {
        authAsAdmin();

        var existing = ts(1L, 9, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setUtenteId(200L);
        dto.setMese(9);
        dto.setAnno(2025);

        assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
    }


    // riapri ------------------------------------------------------------------

    @Test
    void riapri_illegal_whenAlreadyAperto() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.riapri(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("già nello stato APERTO");
    }

    @Test
    void riapri_forbidden_whenChiuso_andNotAdmin() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.CHIUSO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.riapri(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo ADMIN");
    }

    @Test
    void riapri_ok_whenChiuso_andAdmin() {
        authAsAdmin();
        var t = ts(1L, 10, user, TimesheetStato.CHIUSO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        // CHIUSO → CONFERMATO (primo step; per tornare APERTO serve un secondo riapri)
        var saved = ts(1L, 10, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.riapri(1L);
        assertThat(out.stato()).isEqualTo(TimesheetStato.CONFERMATO.name());
    }

    @Test
    void riapri_ok_whenConfermato_andUserOwner() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        var saved = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.riapri(1L);
        assertThat(out.stato()).isEqualTo(TimesheetStato.APERTO.name());
    }

    // chiudi ------------------------------------------------------------------

    @Test
    void chiudi_illegal_whenNotConfermato() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.chiudi(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFERMATO");
    }

    @Test
    void chiudi_ok_setsDate_andState() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.CONFERMATO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        var saved = ts(1L, 10, user, TimesheetStato.CHIUSO);
        saved.setDataCompilazione(LocalDate.now());
        when(timesheetRepository.save(any(Timesheet.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dtoFrom(saved));

        var out = service.chiudi(1L);
        assertThat(out.stato()).isEqualTo(TimesheetStato.CHIUSO.name());
        assertThat(out.dataCompilazione()).isNotNull();
    }

    // totali ------------------------------------------------------------------

    @Test
    void totali_overall_ok_rounded() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        when(rigaRepository.sumTotaliByTimesheetId(1L))
                .thenReturn(new TotaliDto(7.234, 123.456));

        Object res = service.totali(1L, false);
        TotaliDto tot = (TotaliDto) res;
        assertThat(tot.totaleOrario()).isEqualTo(7.23);
        assertThat(tot.totaleCosto()).isEqualTo(123.46);
    }

    @Test
    void totali_perCliente_ok_emptyList() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var t = ts(1L, 10, user, TimesheetStato.APERTO);
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(t));

        when(rigaRepository.sumTotaliPerCliente(1L, SystemClienti.NON_LAVORATO)).thenReturn(Collections.emptyList());

        Object res = service.totali(1L, true);
        assertThat(res).isInstanceOf(List.class);
        assertThat((List<?>) res).isEmpty();
    }

    // autorizzazioni mustReadOwnedOrAdmin ------------------------------------

    @Test
    void mustReadOwnedOrAdmin_hidesOthersTimesheetForUser() {
        authAsUser();
        stubCurrentUserLookupAsUser();

        var others = ts(1L, 10, admin, TimesheetStato.APERTO); // owner = admin (id=100), non è mio (id=200)
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.conferma(1L))
                .isInstanceOf(EntityNotFoundException.class); // anti-leak
    }
}
