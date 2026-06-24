package com.serendipity.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.create.CreaAccontoMovimentoDto;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccontiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        admin = utenteRepository.findByEmail("raffaele.vermiglio@serendipitycoop.it").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("ADMNTST85T10A562Z");
            u.setNome("Admin");
            u.setCognome("Test");
            u.setEmail("raffaele.vermiglio@serendipitycoop.it");
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

        cliente = new Cliente();
        cliente.setNome("Acme S.p.A.");
        cliente.setTariffaOraria(20.0);
        cliente = clienteRepository.save(cliente);
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void createAndSummary_ok_asAdmin() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setUtente(dipendente);
        ts.setMese(3);
        ts.setAnno(2026);
        ts.setStato(TimesheetStato.CHIUSO);
        ts = timesheetRepository.save(ts);

        TimesheetRiga riga = new TimesheetRiga();
        riga.setTimesheet(ts);
        riga.setCliente(cliente);
        riga.setData(LocalDate.of(2026, 3, 10));
        riga.setOre(2);
        riga.setMinuti(0);
        riga.setOrario(2.0);
        riga.setCostoOrario(40.0);
        rigaRepository.save(riga);

        CreaAccontoMovimentoDto dto = new CreaAccontoMovimentoDto();
        dto.setUtenteId(dipendente.getId());
        dto.setMese(3);
        dto.setAnno(2026);
        dto.setImporto(new BigDecimal("50.00"));
        dto.setDataMovimento(LocalDate.of(2026, 3, 11));
        dto.setNote("anticipo");

        mockMvc.perform(post("/api/acconti/movimenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importo").value(50.0));

        mockMvc.perform(get("/api/acconti/summary")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maturato").value(40.0))
                .andExpect(jsonPath("$.data.totaleAcconti").value(50.0))
                .andExpect(jsonPath("$.data.saldoResiduo").value(-10.0))
                .andExpect(jsonPath("$.data.movimenti.length()").value(1));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void summary_openTimesheet_populatesMaturato() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setUtente(dipendente);
        ts.setMese(3);
        ts.setAnno(2026);
        ts.setStato(TimesheetStato.APERTO);
        ts = timesheetRepository.save(ts);

        TimesheetRiga riga = new TimesheetRiga();
        riga.setTimesheet(ts);
        riga.setCliente(cliente);
        riga.setData(LocalDate.of(2026, 3, 10));
        riga.setOre(3);
        riga.setMinuti(0);
        riga.setOrario(3.0);
        riga.setCostoOrario(60.0);
        rigaRepository.save(riga);

        mockMvc.perform(get("/api/acconti/summary")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timesheetStato").value("APERTO"))
                .andExpect(jsonPath("$.data.maturato").value(60.0));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void summary_forbidden_asDipendente() throws Exception {
        mockMvc.perform(get("/api/acconti/summary")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void create_badRequest_invalidImporto() throws Exception {
        CreaAccontoMovimentoDto dto = new CreaAccontoMovimentoDto();
        dto.setUtenteId(dipendente.getId());
        dto.setMese(3);
        dto.setAnno(2026);
        dto.setImporto(BigDecimal.ZERO);
        dto.setDataMovimento(LocalDate.of(2026, 3, 11));

        mockMvc.perform(post("/api/acconti/movimenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void deleteMovement_ok_asAdmin() throws Exception {
        CreaAccontoMovimentoDto dto = new CreaAccontoMovimentoDto();
        dto.setUtenteId(dipendente.getId());
        dto.setMese(3);
        dto.setAnno(2026);
        dto.setImporto(new BigDecimal("50.00"));
        dto.setDataMovimento(LocalDate.of(2026, 3, 11));

        String response = mockMvc.perform(post("/api/acconti/movimenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long createdId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/acconti/movimenti/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Movimento acconto eliminato"));

        mockMvc.perform(get("/api/acconti/summary")
                        .param("utenteId", dipendente.getId().toString())
                        .param("mese", "3")
                        .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.movimenti.length()").value(0))
                .andExpect(jsonPath("$.data.totaleAcconti").value(0.0));
    }
}
