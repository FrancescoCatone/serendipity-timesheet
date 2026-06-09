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
import com.serendipity.backend.support.SystemClienti;
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

    private Timesheet tsUserAperto;
    private Timesheet tsUserConfermato;
    private Timesheet tsUserChiuso;

    private Cliente c1;

    @BeforeEach
    void setUp() {
        admin = new Utente();
        setUtente(admin, 100L, "admin@acme.it");
        user = new Utente();
        setUtente(user, 200L, "user@acme.it");

        tsUserAperto = ts(1L, user, TimesheetStato.APERTO);
        tsUserConfermato = ts(2L, user, TimesheetStato.CONFERMATO);
        tsUserChiuso = ts(3L, user, TimesheetStato.CHIUSO);

        c1 = new Cliente();
        setCliente(c1);
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

    private Timesheet ts(Long id, Utente owner, TimesheetStato stato) {
        Timesheet t = new Timesheet();
        t.setId(id);
        t.setMese(10);
        t.setAnno(2025);
        t.setUtente(owner);
        t.setStato(stato);
        return t;
    }

    private void setCliente(Cliente c) {
        try {
            var f = Cliente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, (Long) 10L);
        } catch (Exception ignore) {
        }
        c.setNome("Acme");
        c.setTariffaOraria(12.0);
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

        when(rigaRepository.findAllOrdered()).thenReturn(List.of(r1, r2));
        when(mapper.toDto(r1)).thenReturn(dtoFrom(r1));
        when(mapper.toDto(r2)).thenReturn(dtoFrom(r2));

        var out = service.findAll();
        assertThat(out).hasSize(2);
    }

    @Test
    void findAll_user_getsOnlyOwn() {
        authAsUser();

        TimesheetRiga rOwn = new TimesheetRiga();
        rOwn.setId(1L);
        rOwn.setTimesheet(tsUserAperto);

        when(rigaRepository.findByTimesheetUtenteId(user.getId())).thenReturn(List.of(rOwn));
        when(mapper.toDto(rOwn)).thenReturn(dtoFrom(rOwn));

        var out = service.findAll();
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getTimesheetId()).isEqualTo(tsUserAperto.getId());
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
        Timesheet otherTs = ts(5L, admin, TimesheetStato.APERTO);
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
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
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
                .hasMessageContaining("non modificabile");
    }

    @Test
    void save_illegal_whenTsConfermato_evenForAdmin() {
        authAsAdmin();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserConfermato.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 3));
        dto.setOre(0);
        dto.setMinuti(45);

        when(timesheetRepository.findById(tsUserConfermato.getId())).thenReturn(Optional.of(tsUserConfermato));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
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

    @Test
    void save_ok_zeroDuration_whenClienteNonLavorato() {
        authAsUser();

        Cliente nonLavorato = new Cliente();
        nonLavorato.setId(99L);
        nonLavorato.setNome(SystemClienti.NON_LAVORATO);
        nonLavorato.setTariffaOraria(0);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(nonLavorato.getId());
        dto.setData(LocalDate.of(2025, 10, 16));
        dto.setOre(0);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(nonLavorato.getId())).thenReturn(Optional.of(nonLavorato));
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.save(dto);
        assertThat(out.getOre()).isEqualTo(0);
        assertThat(out.getMinuti()).isEqualTo(0);
    }

    @Test
    void save_sameClientSameDay_mergesExistingRow() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(900L);
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(c1);
        existing.setData(LocalDate.of(2025, 10, 16));
        existing.setOre(2);
        existing.setMinuti(45);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 16));
        dto.setOre(1);
        dto.setMinuti(30);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
        when(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId())).thenReturn(List.of(existing));
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.save(dto);

        assertThat(out.getId()).isEqualTo(900L);
        assertThat(out.getOre()).isEqualTo(4);
        assertThat(out.getMinuti()).isEqualTo(15);
        assertThat(out.getOrario()).isEqualByComparingTo("4.25");
        assertThat(out.getCostoOrario()).isEqualByComparingTo("51.00");
    }

    @Test
    void save_rejectsNonLavoratoWhenWorkingRowsAlreadyExist() {
        authAsUser();

        Cliente nonLavorato = new Cliente();
        nonLavorato.setId(99L);
        nonLavorato.setNome(SystemClienti.NON_LAVORATO);
        nonLavorato.setTariffaOraria(0);

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(901L);
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(c1);
        existing.setData(LocalDate.of(2025, 10, 17));
        existing.setOre(2);
        existing.setMinuti(0);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(nonLavorato.getId());
        dto.setData(LocalDate.of(2025, 10, 17));
        dto.setOre(0);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(nonLavorato.getId())).thenReturn(Optional.of(nonLavorato));
        when(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NON LAVORATO")
                .hasMessageContaining("2025-10-17");
    }

    @Test
    void save_rejectsWorkingRowWhenNonLavoratoAlreadyExists() {
        authAsUser();

        Cliente nonLavorato = new Cliente();
        nonLavorato.setId(99L);
        nonLavorato.setNome(SystemClienti.NON_LAVORATO);
        nonLavorato.setTariffaOraria(0);

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(902L);
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(nonLavorato);
        existing.setData(LocalDate.of(2025, 10, 18));
        existing.setOre(0);
        existing.setMinuti(0);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 18));
        dto.setOre(3);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));
        when(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NON LAVORATO")
                .hasMessageContaining("2025-10-18");
    }

    @Test
    void save_illegal_zeroDuration_whenClienteNormale() {
        authAsUser();

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 16));
        dto.setOre(0);
        dto.setMinuti(0);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(clienteRepository.findById(c1.getId())).thenReturn(Optional.of(c1));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 ore e 0 minuti");
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
                .hasMessageContaining("non modificabile");
    }

    @Test
    void update_illegal_whenTsConfermato_evenForAdmin() {
        authAsAdmin();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(62L);
        existing.setTimesheet(tsUserConfermato);

        when(rigaRepository.findById(62L)).thenReturn(Optional.of(existing));

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserConfermato.getId());
        dto.setClienteId(c1.getId());
        dto.setData(LocalDate.of(2025, 10, 10));
        dto.setOre(1);
        dto.setMinuti(0);

        assertThatThrownBy(() -> service.update(62L, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
    }

    @Test
    void update_sameClientSameDay_mergesRowsAndDeletesEditedRow() {
        authAsUser();

        Cliente clienteB = new Cliente();
        clienteB.setId(20L);
        clienteB.setNome("Beta");
        clienteB.setTariffaOraria(10.0);

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(1000L);
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(c1);
        existing.setData(LocalDate.of(2025, 10, 20));
        existing.setOre(1);
        existing.setMinuti(0);

        TimesheetRiga mergeTarget = new TimesheetRiga();
        mergeTarget.setId(1001L);
        mergeTarget.setTimesheet(tsUserAperto);
        mergeTarget.setCliente(clienteB);
        mergeTarget.setData(LocalDate.of(2025, 10, 21));
        mergeTarget.setOre(2);
        mergeTarget.setMinuti(30);

        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsUserAperto.getId());
        dto.setClienteId(clienteB.getId());
        dto.setData(LocalDate.of(2025, 10, 21));
        dto.setOre(1);
        dto.setMinuti(45);

        when(rigaRepository.findById(1000L)).thenReturn(Optional.of(existing));
        when(clienteRepository.findById(clienteB.getId())).thenReturn(Optional.of(clienteB));
        when(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId())).thenReturn(List.of(existing, mergeTarget));
        when(rigaRepository.save(any(TimesheetRiga.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDto(any(TimesheetRiga.class))).thenAnswer(inv -> dtoFrom(inv.getArgument(0)));

        var out = service.update(1000L, dto);

        assertThat(out.getId()).isEqualTo(1001L);
        assertThat(out.getOre()).isEqualTo(4);
        assertThat(out.getMinuti()).isEqualTo(15);
        verify(rigaRepository).delete(existing);
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
        verify(rigaRepository).delete(existing);
    }

    @Test
    void delete_forbidden_onChiuso() {
        authAsUser();

        TimesheetRiga existing = new TimesheetRiga();
        existing.setId(71L);
        existing.setTimesheet(tsUserChiuso);

        when(rigaRepository.findById(71L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delete(71L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non modificabile");
    }

    /* =========================== filtra =========================== */

    @Test
    void filtra_admin_filtersByClienteUserData() {
        authAsAdmin();

        Timesheet tsAdmin = ts(4L, admin, TimesheetStato.APERTO);
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

        when(rigaRepository.searchFilteredWithData(
                c1.getId(),
                admin.getId(),
                LocalDate.of(2025, 10, 10)))
                .thenReturn(List.of(r2));

        when(mapper.toDto(r2)).thenReturn(dtoFrom(r2));

        var out = service.filtra(c1.getId(), admin.getId(), "2025-10-10");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getId()).isEqualTo(2L);
    }

    @Test
    void filtra_user_seesOnlyOwn_ignoresUserIdParam() {
        authAsUser();

        Timesheet tsAdmin = ts(4L, admin, TimesheetStato.APERTO);
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

        when(rigaRepository.searchFilteredWithData(
                null,
                user.getId(),
                LocalDate.of(2025, 10, 10)))
                .thenReturn(List.of(rMine));

        when(mapper.toDto(rMine)).thenReturn(dtoFrom(rMine));

        var out = service.filtra(null, 999L, "2025-10-10");
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getId()).isEqualTo(10L);
    }

    @Test
    void filtra_admin_withoutData_usesSearchFilteredWithoutData() {
        authAsAdmin();

        TimesheetRiga r1 = new TimesheetRiga();
        r1.setId(20L);
        r1.setTimesheet(tsUserAperto);
        r1.setCliente(c1);

        when(rigaRepository.searchFilteredWithoutData(c1.getId(), user.getId()))
                .thenReturn(List.of(r1));
        when(mapper.toDto(r1)).thenReturn(dtoFrom(r1));

        var out = service.filtra(c1.getId(), user.getId(), null);

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getId()).isEqualTo(20L);
    }

    /* =========================== findByTimesheetId =========================== */

    @Test
    void findByTimesheetId_ok_ownerOrAdmin() {
        authAsUser();

        TimesheetRiga r = new TimesheetRiga();
        r.setId(80L);
        r.setTimesheet(tsUserAperto);

        when(timesheetRepository.findById(tsUserAperto.getId())).thenReturn(Optional.of(tsUserAperto));
        when(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId())).thenReturn(List.of(r));
        when(mapper.toDto(r)).thenReturn(dtoFrom(r));

        var out = service.findByTimesheetId(tsUserAperto.getId());
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().getId()).isEqualTo(80L);
    }

    @Test
    void findByTimesheetId_hiddenForNotOwner() {
        authAsUser();

        Timesheet tsOther = ts(8L, admin, TimesheetStato.APERTO);
        when(timesheetRepository.findById(tsOther.getId())).thenReturn(Optional.of(tsOther));

        assertThatThrownBy(() -> service.findByTimesheetId(tsOther.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
