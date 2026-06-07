package com.serendipity.backend.controller;

import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private TimesheetRigaRepository rigaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Utente admin;
    private Utente dipendente;
    private Utente altroDipendente;

    private Cliente clienteA;
    private Cliente clienteB;

    @BeforeEach
    void setUp() {
        admin = utenteRepository.findByEmail("admin@serendipity.com").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("ADMNTST85T10A562Z");
            u.setNome("Admin");
            u.setCognome("Test");
            u.setEmail("admin@serendipity.com");
            u.setPassword(passwordEncoder.encode("AdminTest123!"));
            u.setRuolo(Ruolo.ADMIN);
            return utenteRepository.save(u);
        });

        dipendente = utenteRepository.findByEmail("user@serendipity.com").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("USRUSR85T10A562Q");
            u.setNome("Mario");
            u.setCognome("Rossi");
            u.setEmail("user@serendipity.com");
            u.setPassword(passwordEncoder.encode("UserTest123!"));
            u.setRuolo(Ruolo.DIPENDENTE);
            return utenteRepository.save(u);
        });

        altroDipendente = utenteRepository.findByEmail("anna@serendipity.com").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("ANNABN85T10A562Q");
            u.setNome("Anna");
            u.setCognome("Bianchi");
            u.setEmail("anna@serendipity.com");
            u.setPassword(passwordEncoder.encode("UserTest123!"));
            u.setRuolo(Ruolo.DIPENDENTE);
            return utenteRepository.save(u);
        });

        clienteA = new Cliente();
        clienteA.setNome("Acme S.p.A.");
        clienteA.setTariffaOraria(20.0);
        clienteA = clienteRepository.save(clienteA);

        clienteB = new Cliente();
        clienteB.setNome("Globex SRL");
        clienteB.setTariffaOraria(30.0);
        clienteB = clienteRepository.save(clienteB);
    }

    private Timesheet persistTimesheet(Utente owner, int mese, int anno, TimesheetStato stato) {
        Timesheet t = new Timesheet();
        t.setUtente(owner);
        t.setMese(mese);
        t.setAnno(anno);
        t.setStato(stato);
        return timesheetRepository.save(t);
    }

    private TimesheetRiga persistRiga(Timesheet ts, Cliente cliente, LocalDate data, int ore, int minuti) {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(ts);
        r.setCliente(cliente);
        r.setData(data);
        r.setOre(ore);
        r.setMinuti(minuti);

        double orario = new java.math.BigDecimal(ore * 60 + minuti)
                .divide(new java.math.BigDecimal(60), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        double costo = new java.math.BigDecimal(orario)
                .multiply(new java.math.BigDecimal(cliente.getTariffaOraria()))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        r.setOrario(orario);
        r.setCostoOrario(costo);

        return rigaRepository.save(r);
    }

    /* ====================== REPORT CLIENTE ====================== */

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportCliente_ok_asAdmin() throws Exception {
        Timesheet ts1 = persistTimesheet(dipendente, 3, 2026, TimesheetStato.APERTO);
        Timesheet ts2 = persistTimesheet(altroDipendente, 3, 2026, TimesheetStato.APERTO);

        persistRiga(ts1, clienteA, LocalDate.of(2026, 3, 2), 1, 30); // 1.5h -> 30.00
        persistRiga(ts1, clienteA, LocalDate.of(2026, 3, 3), 2, 0);  // 2.0h -> 40.00
        persistRiga(ts2, clienteA, LocalDate.of(2026, 3, 4), 1, 0);  // 1.0h -> 20.00

        mockMvc.perform(get("/api/report/cliente")
                        .param("clienteId", clienteA.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Report cliente generato"))
                .andExpect(jsonPath("$.data.clienteId").value(clienteA.getId().intValue()))
                .andExpect(jsonPath("$.data.clienteNome").value("Acme S.p.A."))
                .andExpect(jsonPath("$.data.totaleOre").value(4.5))
                .andExpect(jsonPath("$.data.totaleCosto").value(90.0))
                .andExpect(jsonPath("$.data.dettaglioDipendenti.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void reportCliente_forbidden_asDipendente() throws Exception {
        mockMvc.perform(get("/api/report/cliente")
                        .param("clienteId", clienteA.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportCliente_notFound() throws Exception {
        mockMvc.perform(get("/api/report/cliente")
                        .param("clienteId", "999999")
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportCliente_badRequest_invalidMese() throws Exception {
        mockMvc.perform(get("/api/report/cliente")
                        .param("clienteId", clienteA.getId().toString())
                        .param("mese", "13")
                        .param("anno", "2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportClienteGiorno_ok_asAdmin() throws Exception {
        Timesheet ts1 = persistTimesheet(dipendente, 3, 2026, TimesheetStato.APERTO);
        Timesheet ts2 = persistTimesheet(altroDipendente, 3, 2026, TimesheetStato.APERTO);

        persistRiga(ts1, clienteA, LocalDate.of(2026, 3, 12), 2, 0);  // 2.0h -> 40.00
        persistRiga(ts2, clienteA, LocalDate.of(2026, 3, 12), 1, 30); // 1.5h -> 30.00
        persistRiga(ts2, clienteB, LocalDate.of(2026, 3, 12), 3, 0);  // altro cliente

        mockMvc.perform(get("/api/report/cliente/giorno")
                        .param("clienteId", clienteA.getId().toString())
                        .param("data", "2026-03-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Report cliente giornaliero generato"))
                .andExpect(jsonPath("$.data.clienteId").value(clienteA.getId().intValue()))
                .andExpect(jsonPath("$.data.clienteNome").value("Acme S.p.A."))
                .andExpect(jsonPath("$.data.data").value("2026-03-12"))
                .andExpect(jsonPath("$.data.totaleOre").value(3.5))
                .andExpect(jsonPath("$.data.totaleCosto").value(70.0))
                .andExpect(jsonPath("$.data.dettaglioDipendenti.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void reportClienteGiorno_forbidden_asDipendente() throws Exception {
        mockMvc.perform(get("/api/report/cliente/giorno")
                        .param("clienteId", clienteA.getId().toString())
                        .param("data", "2026-03-12"))
                .andExpect(status().isForbidden());
    }

    /* ====================== REPORT DIPENDENTE ====================== */

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportDipendente_ok_asAdmin() throws Exception {
        Timesheet ts = persistTimesheet(dipendente, 3, 2026, TimesheetStato.APERTO);

        persistRiga(ts, clienteA, LocalDate.of(2026, 3, 2), 1, 30); // 1.5h -> 30.00
        persistRiga(ts, clienteB, LocalDate.of(2026, 3, 3), 2, 0);  // 2.0h -> 60.00

        mockMvc.perform(get("/api/report/dipendente")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Report dipendente generato"))
                .andExpect(jsonPath("$.data.utenteId").value(dipendente.getId().intValue()))
                .andExpect(jsonPath("$.data.nome").value("Mario"))
                .andExpect(jsonPath("$.data.cognome").value("Rossi"))
                .andExpect(jsonPath("$.data.totaleOre").value(3.5))
                .andExpect(jsonPath("$.data.totaleCosto").value(90.0))
                .andExpect(jsonPath("$.data.dettaglioClienti.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void reportDipendente_ok_asOwner() throws Exception {
        Timesheet ts = persistTimesheet(dipendente, 3, 2026, TimesheetStato.APERTO);

        persistRiga(ts, clienteA, LocalDate.of(2026, 3, 2), 2, 0); // 2.0h -> 40.00

        mockMvc.perform(get("/api/report/dipendente")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.utenteId").value(dipendente.getId().intValue()))
                .andExpect(jsonPath("$.data.totaleOre").value(2.0))
                .andExpect(jsonPath("$.data.totaleCosto").value(40.0))
                .andExpect(jsonPath("$.data.dettaglioClienti.length()").value(1));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void reportDipendente_forbidden_forAnotherUser() throws Exception {
        mockMvc.perform(get("/api/report/dipendente")
                        .param("utenteId", altroDipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportDipendente_notFound() throws Exception {
        mockMvc.perform(get("/api/report/dipendente")
                        .param("utenteId", "999999")
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@serendipity.com", roles = "ADMIN")
    void reportDipendente_badRequest_invalidAnno() throws Exception {
        mockMvc.perform(get("/api/report/dipendente")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "1999"))
                .andExpect(status().isBadRequest());
    }
}
