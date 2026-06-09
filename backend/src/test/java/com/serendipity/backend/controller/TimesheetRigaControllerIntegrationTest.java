package com.serendipity.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
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
import com.serendipity.backend.support.SystemClienti;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TimesheetRigaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private TimesheetRepository timesheetRepository;
    @Autowired
    private TimesheetRigaRepository rigaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Utente admin;
    private Utente user;
    private Cliente clienteA;
    private Cliente clienteB;
    private Timesheet tsUserAperto;
    private Timesheet tsUserConfermato;
    private Timesheet tsUserChiuso;
    private Timesheet tsAdminAperto;

    @BeforeEach
    void setUp() {
        // Assicuro admin coerente con @WithMockUser
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

        // Dipendente reale
        user = utenteRepository.findByEmail("user@serendipity.com").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("USRUSR85T10A562Q");
            u.setNome("Mario");
            u.setCognome("Rossi");
            u.setEmail("user@serendipity.com");
            u.setPassword(passwordEncoder.encode("UserTest123!"));
            u.setRuolo(Ruolo.DIPENDENTE);
            return utenteRepository.save(u);
        });

        // Clienti
        clienteA = new Cliente();
        clienteA.setNome("Acme");
        clienteA.setTariffaOraria(12.0);
        clienteA = clienteRepository.save(clienteA);

        clienteB = new Cliente();
        clienteB.setNome("Globex");
        clienteB.setTariffaOraria(20.0);
        clienteB = clienteRepository.save(clienteB);

        // Timesheet per i test
        // USER → mesi diversi per rispettare il vincolo (utente_id, mese, anno) unico
        tsUserAperto = new Timesheet();
        tsUserAperto.setAnno(2025);
        tsUserAperto.setMese(10); // OTTOBRE
        tsUserAperto.setUtente(user);
        tsUserAperto.setStato(TimesheetStato.APERTO);
        tsUserAperto = timesheetRepository.save(tsUserAperto);

        tsUserConfermato = new Timesheet();
        tsUserConfermato.setAnno(2025);
        tsUserConfermato.setMese(9); // SETTEMBRE (≠ 10)
        tsUserConfermato.setUtente(user);
        tsUserConfermato.setStato(TimesheetStato.CONFERMATO);
        tsUserConfermato = timesheetRepository.save(tsUserConfermato);

        tsUserChiuso = new Timesheet();
        tsUserChiuso.setAnno(2025);
        tsUserChiuso.setMese(8); // AGOSTO (≠ 10,9)
        tsUserChiuso.setUtente(user);
        tsUserChiuso.setStato(TimesheetStato.CHIUSO);
        tsUserChiuso = timesheetRepository.save(tsUserChiuso);

        // ADMIN → può restare 10/2025 (utente diverso → nessun conflitto)
        tsAdminAperto = new Timesheet();
        tsAdminAperto.setAnno(2025);
        tsAdminAperto.setMese(10);
        tsAdminAperto.setUtente(admin);
        tsAdminAperto.setStato(TimesheetStato.APERTO);
        tsAdminAperto = timesheetRepository.save(tsAdminAperto);
    }


    private CreaTimesheetRigaDto buildRigaDto(Long tsId, Long clienteId, LocalDate data, int ore, int minuti) {
        CreaTimesheetRigaDto dto = new CreaTimesheetRigaDto();
        dto.setTimesheetId(tsId);
        dto.setClienteId(clienteId);
        dto.setData(data);
        dto.setOre(ore);
        dto.setMinuti(minuti);
        return dto;
    }

    /* ===================== GET /api/timesheet-righe ===================== */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void getAll_asAdmin_ok() throws Exception {
        // 2 righe: 1 dell'user, 1 dell'admin
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setTimesheet(tsUserAperto);
        r1.setCliente(clienteA);
        r1.setData(LocalDate.of(2025, 10, 1));
        r1.setOre(1);
        r1.setMinuti(0);
        r1.setOrario(1.0);
        r1.setCostoOrario(12.0);
        r1 = rigaRepository.save(r1);

        TimesheetRiga r2 = new TimesheetRiga();
        r2.setTimesheet(tsAdminAperto);
        r2.setCliente(clienteA);
        r2.setData(LocalDate.of(2025, 10, 2));
        r2.setOre(2);
        r2.setMinuti(0);
        r2.setOrario(2.0);
        r2.setCostoOrario(24.0);
        r2 = rigaRepository.save(r2);

        MvcResult res = mockMvc.perform(get("/api/timesheet-righe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lista righe timesheet"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                res.getResponse().getContentAsString(),
                new com.fasterxml.jackson.core.type.TypeReference<>() {
                }
        );
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> data =
                (java.util.List<java.util.Map<String, Object>>) body.get("data");

        // Verifico che CI SIANO anche le due righe create nel test
        var ids = data.stream()
                .map(m -> ((Integer) m.get("id")).longValue())
                .toList();

        org.assertj.core.api.Assertions.assertThat(ids)
                .contains(r1.getId(), r2.getId());
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getAll_asDipendente_onlyOwn() throws Exception {
        // own
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setTimesheet(tsUserAperto);
        r1.setCliente(clienteA);
        r1.setData(LocalDate.of(2025, 10, 1));
        r1.setOre(1);
        r1.setMinuti(0);
        r1.setOrario(1.0);
        r1.setCostoOrario(12.0);
        rigaRepository.save(r1);
        // other
        TimesheetRiga r2 = new TimesheetRiga();
        r2.setTimesheet(tsAdminAperto);
        r2.setCliente(clienteA);
        r2.setData(LocalDate.of(2025, 10, 2));
        r2.setOre(2);
        r2.setMinuti(0);
        r2.setOrario(2.0);
        r2.setCostoOrario(24.0);
        rigaRepository.save(r2);

        MvcResult res = mockMvc.perform(get("/api/timesheet-righe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lista righe timesheet"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        assertThat(data).hasSize(1);
        assertThat(((Integer) data.getFirst().get("timesheetId")).longValue()).isEqualTo(tsUserAperto.getId());
    }

    /* ===================== GET /api/timesheet-righe/{id} ===================== */

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getById_asOwner_ok() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserAperto);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 10, 3));
        r.setOre(1);
        r.setMinuti(30);
        r.setOrario(1.5);
        r.setCostoOrario(18.0);
        r = rigaRepository.save(r);

        mockMvc.perform(get("/api/timesheet-righe/{id}", r.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Riga trovata"))
                .andExpect(jsonPath("$.data.id").value(r.getId().intValue()));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getById_notOwner_hidden() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsAdminAperto);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 10, 4));
        r.setOre(2);
        r.setMinuti(0);
        r.setOrario(2.0);
        r.setCostoOrario(24.0);
        r = rigaRepository.save(r);

        mockMvc.perform(get("/api/timesheet-righe/{id}", r.getId()))
                .andExpect(status().isNotFound());
    }

    /* ===================== POST /api/timesheet-righe ===================== */

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_asOwnerOnAperto_ok() throws Exception {
        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserAperto.getId(), clienteA.getId(),
                LocalDate.of(2025, 10, 5), 1, 30);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Riga creata"))
                .andExpect(jsonPath("$.data.timesheetId").value(tsUserAperto.getId().intValue()))
                .andExpect(jsonPath("$.data.clienteId").value(clienteA.getId().intValue()));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_sameClientSameDay_mergesIntoExistingRow() throws Exception {
        TimesheetRiga existing = new TimesheetRiga();
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(clienteA);
        existing.setData(LocalDate.of(2025, 10, 6));
        existing.setOre(2);
        existing.setMinuti(15);
        existing.setOrario(2.25);
        existing.setCostoOrario(27.0);
        existing = rigaRepository.save(existing);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserAperto.getId(), clienteA.getId(),
                LocalDate.of(2025, 10, 6), 1, 45);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(existing.getId().intValue()))
                .andExpect(jsonPath("$.data.ore").value(4))
                .andExpect(jsonPath("$.data.minuti").value(0));

        assertThat(rigaRepository.findByTimesheetIdOrdered(tsUserAperto.getId()))
                .filteredOn(riga -> LocalDate.of(2025, 10, 6).equals(riga.getData()))
                .hasSize(1);
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_rejectsWorkingRowWhenNonLavoratoExists() throws Exception {
        Cliente nonLavorato = new Cliente();
        nonLavorato.setNome(SystemClienti.NON_LAVORATO);
        nonLavorato.setTariffaOraria(0);
        nonLavorato = clienteRepository.save(nonLavorato);

        TimesheetRiga existing = new TimesheetRiga();
        existing.setTimesheet(tsUserAperto);
        existing.setCliente(nonLavorato);
        existing.setData(LocalDate.of(2025, 10, 7));
        existing.setOre(0);
        existing.setMinuti(0);
        existing.setOrario(0);
        existing.setCostoOrario(0);
        rigaRepository.save(existing);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserAperto.getId(), clienteA.getId(),
                LocalDate.of(2025, 10, 7), 2, 0);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NON LAVORATO")));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_conflict_onTsChiuso() throws Exception {
        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserChiuso.getId(), clienteA.getId(),
                LocalDate.of(2025, 8, 6), 1, 0);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_conflict_onTsConfermato_asUser() throws Exception {
        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserConfermato.getId(), clienteA.getId(),
                LocalDate.of(2025, 9, 7), 0, 45);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void create_conflict_onTsConfermato_asAdmin() throws Exception {
        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserConfermato.getId(), clienteB.getId(),
                LocalDate.of(2025, 9, 8), 2, 0);

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void create_validationError_missingClienteId() throws Exception {
        // manca clienteId => @NotNull su DTO → 400 "Validazione fallita"
        var json = """
                {
                  "timesheetId": %d,
                  "data": "2025-10-09",
                  "ore": 1,
                  "minuti": 0
                }
                """.formatted(tsUserAperto.getId());

        mockMvc.perform(post("/api/timesheet-righe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validazione fallita"))
                .andExpect(jsonPath("$.data.clienteId").exists());
    }

    /* ===================== PUT /api/timesheet-righe/{id} ===================== */

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void update_ok_onAperto() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserAperto);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 10, 10));
        r.setOre(1);
        r.setMinuti(0);
        r.setOrario(1.0);
        r.setCostoOrario(12.0);
        r = rigaRepository.save(r);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserAperto.getId(), clienteB.getId(),
                LocalDate.of(2025, 10, 11), 2, 15);

        mockMvc.perform(put("/api/timesheet-righe/{id}", r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Riga aggiornata"))
                .andExpect(jsonPath("$.data.minuti").value(15));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void update_forbidden_changeTimesheetId() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserAperto);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 10, 12));
        r = rigaRepository.save(r);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsAdminAperto.getId(), // diverso TS → vietato
                clienteA.getId(),
                LocalDate.of(2025, 10, 12), 1, 0);

        mockMvc.perform(put("/api/timesheet-righe/{id}", r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Non puoi cambiare il timesheet di appartenenza della riga"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void update_conflict_onConfermato_asUser() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserConfermato);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 9, 13));
        r = rigaRepository.save(r);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserConfermato.getId(), clienteA.getId(),
                LocalDate.of(2025, 9, 13), 1, 0);

        mockMvc.perform(put("/api/timesheet-righe/{id}", r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void update_conflict_onConfermato_asAdmin() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserConfermato);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 9, 20));
        r = rigaRepository.save(r);

        CreaTimesheetRigaDto dto = buildRigaDto(
                tsUserConfermato.getId(), clienteA.getId(),
                LocalDate.of(2025, 9, 20), 2, 0);

        mockMvc.perform(put("/api/timesheet-righe/{id}", r.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    /* ===================== DELETE /api/timesheet-righe/{id} ===================== */

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void delete_ok_onAperto() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserAperto);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 10, 14));
        r = rigaRepository.save(r);

        mockMvc.perform(delete("/api/timesheet-righe/{id}", r.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Riga eliminata"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void delete_conflict_onChiuso() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserChiuso);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 8, 15));
        r = rigaRepository.save(r);

        mockMvc.perform(delete("/api/timesheet-righe/{id}", r.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void delete_conflict_onConfermato() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserConfermato);
        r.setCliente(clienteA);
        r.setData(LocalDate.of(2025, 9, 21));
        r = rigaRepository.save(r);

        mockMvc.perform(delete("/api/timesheet-righe/{id}", r.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    /* ===================== GET /api/timesheet-righe/filter ===================== */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void filter_asAdmin_byClienteUserDate() throws Exception {
        // righe varie
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setTimesheet(tsUserAperto);
        r1.setCliente(clienteA);
        r1.setData(LocalDate.of(2025, 10, 16));
        rigaRepository.save(r1);

        TimesheetRiga r2 = new TimesheetRiga();
        r2.setTimesheet(tsAdminAperto);
        r2.setCliente(clienteA);
        r2.setData(LocalDate.of(2025, 10, 16));
        rigaRepository.save(r2);

        TimesheetRiga r3 = new TimesheetRiga();
        r3.setTimesheet(tsAdminAperto);
        r3.setCliente(clienteA);
        r3.setData(LocalDate.of(2025, 10, 17));
        rigaRepository.save(r3);

        mockMvc.perform(get("/api/timesheet-righe/filter")
                        .param("clienteId", clienteA.getId().toString())
                        .param("utenteId", admin.getId().toString())
                        .param("data", "2025-10-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Filtrati"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].timesheetId").value(tsAdminAperto.getId().intValue()));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void filter_asUser_seesOnlyOwn_evenIfUtenteIdProvided() throws Exception {
        TimesheetRiga rMine = new TimesheetRiga();
        rMine.setTimesheet(tsUserAperto);
        rMine.setCliente(clienteA);
        rMine.setData(LocalDate.of(2025, 10, 18));
        rigaRepository.save(rMine);

        TimesheetRiga rOther = new TimesheetRiga();
        rOther.setTimesheet(tsAdminAperto);
        rOther.setCliente(clienteA);
        rOther.setData(LocalDate.of(2025, 10, 18));
        rigaRepository.save(rOther);

        mockMvc.perform(get("/api/timesheet-righe/filter")
                        .param("utenteId", admin.getId().toString())
                        .param("data", "2025-10-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].timesheetId").value(tsUserAperto.getId().intValue()));
    }

    /* ===================== GET /api/timesheet-righe/by-timesheet/{id} ===================== */

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getByTimesheet_asOwner_ok() throws Exception {
        TimesheetRiga r = new TimesheetRiga();
        r.setTimesheet(tsUserAperto);
        r.setCliente(clienteB);
        r.setData(LocalDate.of(2025, 10, 19));
        rigaRepository.save(r);

        mockMvc.perform(get("/api/timesheet-righe/by-timesheet/{id}", tsUserAperto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Righe del timesheet"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getByTimesheet_notOwner_hidden() throws Exception {
        mockMvc.perform(get("/api/timesheet-righe/by-timesheet/{id}", tsAdminAperto.getId()))
                .andExpect(status().isNotFound());
    }
}

