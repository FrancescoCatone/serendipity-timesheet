package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetRigaMapper;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimesheetRigaServiceTest {

    @InjectMocks
    private TimesheetRigaService service;

    @Mock
    private TimesheetRigaRepository rigaRepository;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private TimesheetRigaMapper mapper;

    private Utente admin;
    private Utente user;

    private Timesheet tsUserAperto;       // TS owner=user, APERTO
    private Timesheet tsUserConfermato;   // TS owner=user, CONFERMATO
    private Timesheet tsUserChiuso;       // TS owner=user, CHIUSO

    private Cliente c1;

    @BeforeEach
    void setUp() {
        admin = new Utente();
        setUtente(admin, 100L, "admin@acme.it");
        user = new Utente();
        setUtente(user, 200L, "user@acme.it");

        tsUserAperto = ts(1L, 10, 2025, user, TimesheetStato.APERTO);
        tsUserConfermato = ts(2L, 10, 2025, user, TimesheetStato.CONFERMATO);
        tsUserChiuso = ts(3L, 10, 2025, user, TimesheetStato.CHIUSO);

        c1 = new Cliente();
        setCliente(c1, 10L, "Acme", 12.0);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /* =========================== Helpers =========================== */

    private void setUtente(Utente u, Long id, String email) {
        try {
            var f = Utente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception ignore) {
        }
        u.setEmail(email);
    }

    private Timesheet ts(Long id, int mese, int anno, Utente owner, TimesheetStato stato) {
        Timesheet t = new Timesheet();
        t.setId(id);
        t.setMese(mese);
        t.setAnno(anno);
        t.setUtente(owner);
        t.setStato(stato);
        return t;
    }

    private void setCliente(Cliente c, Long id, String nome, double tariffa) {
        try {
            var f = Cliente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignore) {
        }
        c.setNome(nome);
        c.setTariffaOraria(tariffa);
    }

    private void authAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                admin.getEmail(), "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsUser() {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(utenteRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private TimesheetRigaDto dtoFrom(TimesheetRiga r) {
        TimesheetRigaDto dto = new TimesheetRigaDto();
        dto.setId(r.getId());
        dto.setTimesheetId(r.getTimesheet() != null ? r.getTimesheet().getId() : null);
        dto.setClienteId(r.getCliente() != null ? r.getCliente().getId() : null);
        dto.setClienteNome(r.getCliente() != null ? r.getCliente().getNome() : null);
        dto.setData(r.getData());
        dto.setOre(r.getOre());
        dto.setMinuti(r.getMinuti());
        dto.setOrario(java.math.BigDecimal.valueOf(r.getOrario()));
        dto.setCostoOrario(java.math.BigDecimal.valueOf(r.getCostoOrario()));
        return dto;
    }

    /* =========================== findAll =========================== */

    @Test
    void findAll_admin_getsAll() {
        authAsAdmin();
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setId(1L);
        r1.setTimesheet(tsUserAperto);
        TimesheetRiga r2 = new TimesheetRiga();
        r2.setId(2L);
        r2.setTimesheet(tsUserAperto);

        when(rigaRepository.findAll()).thenReturn(List.of(r1, r2));
        when(mapper.toDto(r1)).thenReturn(dtoFrom(r1));
        when(mapper.toDto(r2)).thenReturn(dtoFrom(r2));

        var out = service.findAll();
        assertThat(out).hasSize(2);
    }

    @Test
    void findAll_user_getsOnlyOwn() {
        authAsUser();

        Timesheet tsOtherOwner = ts(9L, 10, 2025, admin, TimesheetStato.APERTO);
        TimesheetRiga rOwn = new TimesheetRiga();
        rOwn.setId(1L);
        rOwn.setTimesheet(tsUserAperto);
        TimesheetRiga rOther = new TimesheetRiga();
        rOther.setId(2L);
        rOther.setTimesheet(tsOtherOwner);

        when(rigaRepository.findAll()).thenReturn(List.of(rOwn, rOther));
        when(mapper.toDto(rOwn)).thenReturn(dtoFrom(rOwn));

        var out = service.findAll();
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTimesheetId()).isEqualTo(tsUserAperto.getId());
    }

    /* =========================== findById =========================== */

    @Test
    void findById_ok_ownerOrAdmin() {
        authAsUser();
        TimesheetRiga r = new TimesheetRiga();
        r.setId(77L);
        r.setTimesheet(tsUserAperto);
        when(rigaRepository.findById(77L)).thenReturn(Optional.of(r));
        when(mapper.toDto(r)).thenReturn(dtoFrom(r));

        var out = service.findById(77L);
        assertThat(out.getId()).isEqualTo(77L);
    }

    @Test
    void findById_hiddenForNotOwner() {
        // utente autenticato è 'user', la riga appartiene a TS di admin
        authAsUser();
        Timesheet otherTs = ts(5L, 10, 2025, admin, TimesheetStato.APERTO);
        TimesheetRiga r = new TimesheetRiga();
        r.setId(10L);
        r.setTimesheet(otherTs);
        when(rigaRepository.findById(10L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /* =========================== save =========================== */

    @Test
    void save_ok_ownerOnAperto_computesFields() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 15)); // coerente con TS (10/2025)
        dto.setOre(1);
        dto.setMinuti(30);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));

        ArgumentCaptor<TimesheetRiga> captor = ArgumentCaptor.forClass(TimesheetRiga.class);
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> {
            TimesheetRiga rr = inv.getArgument(0);
            rr.setId(500L);
            return rr;
        });
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.save(dto);

        verify(rigaRepository).save(captor.capture());
        TimesheetRiga saved = captor.getValue();

        assertThat(saved.getOrario()).isEqualTo(1.50);          // 1h30m → 1.50
        assertThat(saved.getCostoOrario()).isEqualTo(18.00);    // 1.50 * 12.00
        assertThat(out.getId()).isEqualTo(500L);
    }

    @Test
    void save_forbidden_whenTsChiuso() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserChiuso.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 2));
        dto.setOre(1);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserChiuso.getId())).thenReturn(Optional.of(tsUserChiuso));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Timesheet CHIUSO");
    }

    @Test
    void save_illegal_whenTsConfermato_andNotAdmin() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserConfermato.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 2));
        dto.setOre(1);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserConfermato.getId())).thenReturn(Optional.of(tsUserConfermato));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFERMATO");
    }

    @Test
    void save_ok_whenTsConfermato_andAdmin() {
        authAsAdmin();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserConfermato.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 3));
        dto.setOre(0);
        dto.setMinuti(45);

        when(timesheetRepository.findById(tsUserConfermato.getId())).thenReturn(Optional.of(tsUserConfermato));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.save(dto);
        assertThat(out.getMinuti()).isEqualTo(45);
    }

    @Test
    void save_illegal_dateOutsideTimesheetMonth() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 9, 30)); // NON coerente con 10/2025
        dto.setOre(1);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non appartiene al mese/anno del timesheet");
    }

    @Test
    void save_illegal_minutesOutOfRange() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 2));
        dto.setOre(1);
        dto.setMinuti(73); // non valido

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("compresi tra 0 e 59");
    }

    /* =========================== update =========================== */

    @Test
    void update_ok_ownerOnAperto() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(50L);
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(c1);
        existing.setData(LocalDate.of(2025, 10, 1));
        existing.setOre(1);
        existing.setMinuti(0);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId()); // non cambia
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 2));
        dto.setOre(2);
        dto.setMinuti(15);

        when(rigaRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.update(50L, dto);
        assertThat(out.getOre()).isEqualTo(2);
        assertThat(out.getMinuti()).isEqualTo(15);
    }

    @Test
    void update_forbidden_changeTimesheetId() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(60L);
        existing.setTimesheet(tsUserAperto);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(999L); // tentativo di cambiare TS
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 5));

        when(rigaRepository.findById(60L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(60L, dto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Non puoi cambiare il timesheet");
    }

    @Test
    void update_illegal_whenTsConfermato_andNotAdmin() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(61L);
        existing.setTimesheet(tsUserConfermato);

        when(rigaRepository.findById(61L)).thenReturn(Optional.of(existing));

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserConfermato.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 10));
        dto.setOre(1);
        dto.setMinuti(0);

        assertThatThrownBy(() -> service.update(61L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFERMATO");
    }

    /* =========================== delete =========================== */

    @Test
    void delete_ok_onAperto() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(70L);
        existing.setTimesheet(tsUserAperto);

        when(rigaRepository.findById(70L)).thenReturn(Optional.of(existing));

        service.delete(70L);
        verify(rigaRepository).deleteById(70L);
    }

    @Test
    void delete_forbidden_onChiuso() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(71L);
        existing.setTimesheet(tsUserChiuso);

        when(rigaRepository.findById(71L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delete(71L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Timesheet CHIUSO");
    }

    /* =========================== filtra =========================== */

    @Test
    void filtra_admin_filtersByClienteUserData() {
        authAsAdmin();

        Timesheet tsAdmin = ts(4L, 10, 2025, admin, TimesheetStato.APERTO);
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setId(1L);
        r1.setTimesheet(tsUserAperto);
        r1.setCliente(c1);
        r1.setData(LocalDate.of(2025, 10, 10));
        TimesheetRiga r2 = new TimesheetRiga();
        r2.setId(2L);
        r2.setTimesheet(tsAdmin);
        r2.setCliente(c1);
        r2.setData(LocalDate.of(2025, 10, 10));
        TimesheetRiga r3 = new TimesheetRiga();
        r3.setId(3L);
        r3.setTimesheet(tsAdmin);
        r3.setCliente(c1);
        r3.setData(LocalDate.of(2025, 10, 11));

        when(rigaRepository.findAll()).thenReturn(List.of(r1, r2, r3));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.filtra(c1.getId(), admin.getId(), "2025-10-10");
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void filtra_user_seesOnlyOwn_ignoresUserIdParam() {
        authAsUser();

        Timesheet tsAdmin = ts(4L, 10, 2025, admin, TimesheetStato.APERTO);
        TimesheetRiga rMine = new TimesheetRiga();
        rMine.setId(10L);
        rMine.setTimesheet(tsUserAperto);
        rMine.setCliente(c1);
        rMine.setData(LocalDate.of(2025, 10, 10));
        TimesheetRiga rOther = new TimesheetRiga();
        rOther.setId(11L);
        rOther.setTimesheet(tsAdmin);
        rOther.setCliente(c1);
        rOther.setData(LocalDate.of(2025, 10, 10));

        when(rigaRepository.findAll()).thenReturn(List.of(rMine, rOther));
        when(mapper.toDto(rMine)).thenReturn(dtoFrom(rMine));

        var out = service.filtra(null, 999L, "2025-10-10");
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(10L);
    }

    /* =========================== findByTimesheetId =========================== */

    @Test
    void findByTimesheetId_ok_ownerOrAdmin() {
        authAsUser();

        TimesheetRiga r = new TimesheetRiga();
        r.setId(80L);
        r.setTimesheet(tsUserAperto);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(rigaRepository.findByTimesheetId(tsUserAperto.getId())).thenReturn(List.of(r));
        when(mapper.toDto(r)).thenReturn(dtoFrom(r));

        var out = service.findByTimesheetId(tsUserAperto.getId());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(80L);
    }

    @Test
    void findByTimesheetId_hiddenForNotOwner() {
        authAsUser();

        Timesheet tsOther = ts(8L, 10, 2025, admin, TimesheetStato.APERTO);
        when(timesheetRepository.findById(tsOther.getId())).thenReturn(Optional.of(tsOther));

        assertThatThrownBy(() -> service.findByTimesheetId(tsOther.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
