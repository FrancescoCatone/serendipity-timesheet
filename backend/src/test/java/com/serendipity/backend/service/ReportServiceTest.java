package com.serendipity.backend.service;

import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.report.ReportClienteDipendenteDto;
import com.serendipity.backend.model.dto.report.ReportDipendenteClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.repository.ClienteRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private TimesheetRigaRepository rigaRepository;

    @InjectMocks
    private ReportService service;

    private Utente admin;
    private Utente dipendente;
    private Utente altroDipendente;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        admin = new Utente();
        admin.setId(1L);
        admin.setNome("Admin");
        admin.setCognome("Test");
        admin.setEmail("admin@serendipity.com");

        dipendente = new Utente();
        dipendente.setId(2L);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setEmail("mario.rossi@test.com");

        altroDipendente = new Utente();
        altroDipendente.setId(3L);
        altroDipendente.setNome("Anna");
        altroDipendente.setCognome("Bianchi");
        altroDipendente.setEmail("anna.bianchi@test.com");

        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNome("Acme S.p.A.");
        cliente.setTariffaOraria(25.0);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                admin.getEmail(),
                "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsDipendenteWithoutStub() {
        var auth = new UsernamePasswordAuthenticationToken(
                dipendente.getEmail(),
                "x",
                List.of(new SimpleGrantedAuthority("ROLE_DIPENDENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsDipendente() {
        authAsDipendenteWithoutStub();
        when(utenteRepository.findByEmail(dipendente.getEmail()))
                .thenReturn(Optional.of(dipendente));
    }

    /* ========================= reportPerCliente ========================= */

    @Test
    void reportPerCliente_ok_asAdmin() {
        authAsAdmin();

        when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));

        when(rigaRepository.reportClientePerDipendente(cliente.getId(), 3, 2026))
                .thenReturn(List.of(
                        new ReportClienteDipendenteDto(2L, "Mario", "Rossi", 10.126, 253.789),
                        new ReportClienteDipendenteDto(3L, "Anna", "Bianchi", 5.555, 111.111)
                ));

        when(rigaRepository.totaleReportCliente(cliente.getId(), 3, 2026))
                .thenReturn(new TotaliDto(15.681, 364.900));

        var out = service.reportPerCliente(cliente.getId(), 3, 2026);

        assertThat(out.clienteId()).isEqualTo(cliente.getId());
        assertThat(out.clienteNome()).isEqualTo("Acme S.p.A.");
        assertThat(out.mese()).isEqualTo(3);
        assertThat(out.anno()).isEqualTo(2026);
        assertThat(out.totaleOre()).isEqualTo(15.68);
        assertThat(out.totaleCosto()).isEqualTo(364.90);
        assertThat(out.dettaglioDipendenti()).hasSize(2);
        assertThat(out.dettaglioDipendenti().get(0).oreTotali()).isEqualTo(10.13);
        assertThat(out.dettaglioDipendenti().get(0).costoTotale()).isEqualTo(253.79);
    }

    @Test
    void reportPerCliente_forbidden_asDipendente() {
        authAsDipendenteWithoutStub();

        assertThatThrownBy(() -> service.reportPerCliente(cliente.getId(), 3, 2026))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo ADMIN");
    }

    @Test
    void reportPerCliente_notFound_cliente() {
        authAsAdmin();

        when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportPerCliente(cliente.getId(), 3, 2026))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente non trovato");
    }

    @Test
    void reportPerCliente_invalidMese() {
        authAsAdmin();

        assertThatThrownBy(() -> service.reportPerCliente(cliente.getId(), 13, 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mese");
    }

    @Test
    void reportPerCliente_invalidAnno() {
        authAsAdmin();

        assertThatThrownBy(() -> service.reportPerCliente(cliente.getId(), 3, 1999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anno");
    }

    /* ========================= reportPerDipendente ========================= */

    @Test
    void reportPerDipendente_ok_asAdmin() {
        authAsAdmin();

        when(utenteRepository.findById(dipendente.getId())).thenReturn(Optional.of(dipendente));

        when(rigaRepository.reportDipendentePerCliente(dipendente.getId(), 3, 2026, SystemClienti.NON_LAVORATO))
                .thenReturn(List.of(
                        new ReportDipendenteClienteDto(10L, "Acme S.p.A.", 12.345, 222.229),
                        new ReportDipendenteClienteDto(11L, "Globex SRL", 8.111, 100.005)
                ));

        when(rigaRepository.totaleReportDipendente(dipendente.getId(), 3, 2026, SystemClienti.NON_LAVORATO))
                .thenReturn(new TotaliDto(20.456, 322.234));

        var out = service.reportPerDipendente(dipendente.getId(), 3, 2026);

        assertThat(out.utenteId()).isEqualTo(dipendente.getId());
        assertThat(out.nome()).isEqualTo("Mario");
        assertThat(out.cognome()).isEqualTo("Rossi");
        assertThat(out.mese()).isEqualTo(3);
        assertThat(out.anno()).isEqualTo(2026);
        assertThat(out.totaleOre()).isEqualTo(20.46);
        assertThat(out.totaleCosto()).isEqualTo(322.23);
        assertThat(out.dettaglioClienti()).hasSize(2);
        assertThat(out.dettaglioClienti().get(0).clienteNome()).isEqualTo("Acme S.p.A.");
        assertThat(out.dettaglioClienti().get(0).oreTotali()).isEqualTo(12.35);
    }

    @Test
    void reportPerDipendente_ok_asOwner() {
        authAsDipendente();

        when(utenteRepository.findById(dipendente.getId())).thenReturn(Optional.of(dipendente));

        when(rigaRepository.reportDipendentePerCliente(dipendente.getId(), 3, 2026, SystemClienti.NON_LAVORATO))
                .thenReturn(List.of(
                        new ReportDipendenteClienteDto(10L, "Acme S.p.A.", 7.0, 140.0)
                ));

        when(rigaRepository.totaleReportDipendente(dipendente.getId(), 3, 2026, SystemClienti.NON_LAVORATO))
                .thenReturn(new TotaliDto(7.0, 140.0));

        var out = service.reportPerDipendente(dipendente.getId(), 3, 2026);

        assertThat(out.utenteId()).isEqualTo(dipendente.getId());
        assertThat(out.totaleOre()).isEqualTo(7.0);
        assertThat(out.totaleCosto()).isEqualTo(140.0);
        assertThat(out.dettaglioClienti()).hasSize(1);
    }

    @Test
    void reportPerDipendente_forbidden_forAnotherUser() {
        authAsDipendente();

        assertThatThrownBy(() -> service.reportPerDipendente(altroDipendente.getId(), 3, 2026))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("altro dipendente");
    }

    @Test
    void reportPerDipendente_notFound_utente() {
        authAsAdmin();

        when(utenteRepository.findById(dipendente.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportPerDipendente(dipendente.getId(), 3, 2026))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Utente non trovato");
    }

    @Test
    void reportPerDipendente_invalidMese() {
        authAsAdmin();

        assertThatThrownBy(() -> service.reportPerDipendente(dipendente.getId(), 0, 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mese");
    }

    @Test
    void reportPerDipendente_invalidAnno() {
        authAsAdmin();

        assertThatThrownBy(() -> service.reportPerDipendente(dipendente.getId(), 3, 1999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anno");
    }
}
