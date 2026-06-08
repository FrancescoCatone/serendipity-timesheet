package com.serendipity.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
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
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests per TimesheetController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TimesheetControllerIntegrationTest {

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

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Utente admin;
    private Utente dipendente;

    @BeforeEach
    void setUp() {
        // admin coerente con @WithMockUser(username="raffaele.vermiglio@serendipitycoop.it")
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

        // utente dipendente reale, usato dove serve getCurrentUserId()
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
    }

    private CreaTimesheetDto buildTsDto(int mese, int anno, Long utenteId) {
        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setMese(mese);
        dto.setAnno(anno);
        dto.setUtenteId(utenteId);
        return dto;
    }

    private Cliente ensureCliente(String nome, double tariffaOraria) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setTariffaOraria(tariffaOraria);
        return clienteRepository.save(cliente);
    }

    private void persistRiga(Timesheet timesheet, Cliente cliente, LocalDate data, int ore, int minuti, double orario, double costo) {
        TimesheetRiga riga = new TimesheetRiga();
        riga.setTimesheet(timesheet);
        riga.setCliente(cliente);
        riga.setData(data);
        riga.setOre(ore);
        riga.setMinuti(minuti);
        riga.setOrario(orario);
        riga.setCostoOrario(costo);
        rigaRepository.save(riga);
    }

    /* ======================= GET /api/timesheets ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void getAll_asAdmin_ok() throws Exception {
        mockMvc.perform(get("/api/timesheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lista timesheet"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getAll_asDipendente_onlyOwn() throws Exception {
        // TS per dipendente
        Timesheet ts1 = new Timesheet();
        ts1.setAnno(2025);
        ts1.setMese(10);
        ts1.setUtente(dipendente);
        timesheetRepository.save(ts1);
        // TS per admin (non deve essere visto dal dipendente)
        Timesheet ts2 = new Timesheet();
        ts2.setAnno(2025);
        ts2.setMese(10);
        ts2.setUtente(admin);
        timesheetRepository.save(ts2);

        MvcResult result = mockMvc.perform(get("/api/timesheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lista timesheet"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        assertThat(data).allMatch(map -> ((Integer) map.get("utenteId")).longValue() == dipendente.getId());
    }

    /* ======================= POST /api/timesheets ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void create_asAdmin_ok() throws Exception {
        CreaTimesheetDto dto = buildTsDto(9, 2025, dipendente.getId());

        mockMvc.perform(post("/api/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Timesheet creato"))
                .andExpect(jsonPath("$.data.utenteId").value(dipendente.getId().intValue()))
                .andExpect(jsonPath("$.data.mese").value(9))
                .andExpect(jsonPath("$.data.anno").value(2025));
    }

    /* ======================= GET /api/timesheets/{id} ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void getById_admin_ok() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(7);
        ts.setUtente(dipendente);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(get("/api/timesheets/{id}", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet trovato"))
                .andExpect(jsonPath("$.data.id").value(ts.getId().intValue()));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void getById_asDipendente_otherTimesheet_notFound() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(7);
        ts.setUtente(admin);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(get("/api/timesheets/{id}", ts.getId()))
                .andExpect(status().isNotFound());
    }

    /* ======================= PUT /api/timesheets/{id} ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void update_conflictWhenChiuso() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(1);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CHIUSO);
        ts = timesheetRepository.save(ts);

        CreaTimesheetDto dto = buildTsDto(2, 2025, dipendente.getId());

        mockMvc.perform(put("/api/timesheets/{id}", ts.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = {"ADMIN"})
    void update_ok_asAdmin() throws Exception {
        // Timesheet in stato APERTO → modificabile
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(9);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.APERTO);
        ts = timesheetRepository.save(ts);

        CreaTimesheetDto dto = buildTsDto(10, 2025, dipendente.getId());

        mockMvc.perform(put("/api/timesheets/{id}", ts.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet aggiornato"))
                .andExpect(jsonPath("$.data.id").value(ts.getId().intValue()))
                .andExpect(jsonPath("$.data.mese").value(10))
                .andExpect(jsonPath("$.data.anno").value(2025));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = {"DIPENDENTE"})
    void conferma_ok_asDipendente() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(10);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.APERTO);
        ts = timesheetRepository.save(ts);

        Cliente cliente = ensureCliente("Acme Conferma", 10.0);
        for (int day = 1; day <= 31; day++) {
            LocalDate date = LocalDate.of(2025, 10, day);
            if (date.getDayOfWeek().getValue() == 7) {
                continue;
            }
            persistRiga(ts, cliente, date, 1, 0, 1.0, 10.0);
        }

        mockMvc.perform(put("/api/timesheets/{id}/conferma", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet confermato"))
                .andExpect(jsonPath("$.data.id").value(ts.getId().intValue()))
                .andExpect(jsonPath("$.data.stato").value("CONFERMATO"));

        assertThat(rigaRepository.findByTimesheetIdOrdered(ts.getId())).hasSize(31);
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = {"DIPENDENTE"})
    void conferma_conflict_whenMonthNotCovered() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(10);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.APERTO);
        ts = timesheetRepository.save(ts);

        Cliente cliente = ensureCliente("Acme Incompleto", 10.0);
        persistRiga(ts, cliente, LocalDate.of(2025, 10, 1), 1, 0, 1.0, 10.0);

        mockMvc.perform(put("/api/timesheets/{id}/conferma", ts.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("giorni feriali del mese non compilati")));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void update_conflictWhenConfermato() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(1);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CONFERMATO);
        ts = timesheetRepository.save(ts);

        CreaTimesheetDto dto = buildTsDto(2, 2025, dipendente.getId());

        mockMvc.perform(put("/api/timesheets/{id}", ts.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Timesheet non modificabile nello stato attuale"));
    }


    /* ======================= DELETE /api/timesheets/{id} ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void delete_asAdmin_ok() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(3);
        ts.setUtente(dipendente);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(delete("/api/timesheets/{id}", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet eliminato"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void delete_asAdmin_alsoDeletesRelatedRows() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(5);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CHIUSO);
        ts = timesheetRepository.save(ts);

        Cliente cliente = ensureCliente("Cliente Delete Test", 20.0);
        persistRiga(ts, cliente, LocalDate.of(2025, 5, 10), 2, 30, 2.5, 50.0);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/timesheets/{id}", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet eliminato"));

        Long timesheetCount = jdbcTemplate.queryForObject(
                "select count(*) from timesheet where id = ?",
                Long.class,
                ts.getId()
        );
        Long righeCount = jdbcTemplate.queryForObject(
                "select count(*) from timesheet_riga where timesheet_id = ?",
                Long.class,
                ts.getId()
        );

        assertThat(timesheetCount).isZero();
        assertThat(righeCount).isZero();
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void delete_asDipendente_forbidden() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(3);
        ts.setUtente(dipendente);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(delete("/api/timesheets/{id}", ts.getId()))
                .andExpect(status().isForbidden());
    }

    /* ======================= GET /api/timesheets/search ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void search_asAdmin_withAndWithoutUserId() throws Exception {
        // due TS in 04/2025: uno per dipendente, uno per admin
        Timesheet a = new Timesheet();
        a.setAnno(2025);
        a.setMese(4);
        a.setUtente(dipendente);
        timesheetRepository.save(a);
        Timesheet b = new Timesheet();
        b.setAnno(2025);
        b.setMese(4);
        b.setUtente(admin);
        timesheetRepository.save(b);

        // senza utenteId → entrambi
        MvcResult all = mockMvc.perform(get("/api/timesheets/search")
                        .param("mese", "4").param("anno", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Risultati ricerca timesheet"))
                .andReturn();
        Map<String, Object> allMap = objectMapper.readValue(all.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> dataAll = (List<Map<String, Object>>) allMap.get("data");
        assertThat(dataAll).hasSize(2);

        // con utenteId → solo di quell'utente
        MvcResult onlyUser = mockMvc.perform(get("/api/timesheets/search")
                        .param("mese", "4").param("anno", "2025")
                        .param("utenteId", dipendente.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> onlyUserMap = objectMapper.readValue(onlyUser.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> dataUser = (List<Map<String, Object>>) onlyUserMap.get("data");
        assertThat(dataUser).hasSize(1);
        assertThat(((Integer) dataUser.getFirst().get("utenteId")).longValue()).isEqualTo(dipendente.getId());
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void search_asDipendente_seesOnlyOwn() throws Exception {
        Timesheet mine = new Timesheet();
        mine.setAnno(2025);
        mine.setMese(5);
        mine.setUtente(dipendente);
        timesheetRepository.save(mine);
        Timesheet other = new Timesheet();
        other.setAnno(2025);
        other.setMese(5);
        other.setUtente(admin);
        timesheetRepository.save(other);

        MvcResult res = mockMvc.perform(get("/api/timesheets/search")
                        .param("mese", "5").param("anno", "2025"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> map = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");
        assertThat(data).hasSize(1);
        assertThat(((Integer) data.getFirst().get("utenteId")).longValue()).isEqualTo(dipendente.getId());
    }

    /* ======================= GET /api/timesheets/anni & /mesi ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void anniEMesi_ok() throws Exception {
        Timesheet t1 = new Timesheet();
        t1.setAnno(2024);
        t1.setMese(12);
        t1.setUtente(dipendente);
        timesheetRepository.save(t1);
        Timesheet t2 = new Timesheet();
        t2.setAnno(2025);
        t2.setMese(1);
        t2.setUtente(dipendente);
        timesheetRepository.save(t2);
        Timesheet t3 = new Timesheet();
        t3.setAnno(2025);
        t3.setMese(2);
        t3.setUtente(dipendente);
        timesheetRepository.save(t3);

        mockMvc.perform(get("/api/timesheets/anni"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.hasItems(2025, 2024)));

        mockMvc.perform(get("/api/timesheets/mesi").param("anno", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.hasItems(1, 2)));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void anni_asDipendente_onlyOwn() throws Exception {
        Timesheet mine1 = new Timesheet();
        mine1.setAnno(2025);
        mine1.setMese(1);
        mine1.setUtente(dipendente);
        timesheetRepository.save(mine1);

        Timesheet mine2 = new Timesheet();
        mine2.setAnno(2026);
        mine2.setMese(2);
        mine2.setUtente(dipendente);
        timesheetRepository.save(mine2);

        Timesheet other = new Timesheet();
        other.setAnno(2024);
        other.setMese(3);
        other.setUtente(admin);
        timesheetRepository.save(other);

        mockMvc.perform(get("/api/timesheets/anni"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.hasItems(2026, 2025)))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(2024))));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void mesi_asDipendente_onlyOwn() throws Exception {
        Timesheet mine1 = new Timesheet();
        mine1.setAnno(2025);
        mine1.setMese(3);
        mine1.setUtente(dipendente);
        timesheetRepository.save(mine1);

        Timesheet mine2 = new Timesheet();
        mine2.setAnno(2025);
        mine2.setMese(5);
        mine2.setUtente(dipendente);
        timesheetRepository.save(mine2);

        Timesheet other = new Timesheet();
        other.setAnno(2025);
        other.setMese(9);
        other.setUtente(admin);
        timesheetRepository.save(other);

        mockMvc.perform(get("/api/timesheets/mesi").param("anno", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.hasItems(3, 5)))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(9))));
    }

    /* ======================= Stato: conferma / riapri / chiudi ======================= */


    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void riapri_conflictIfAlreadyAperto() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(7);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.APERTO);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(put("/api/timesheets/{id}/riapri", ts.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Il timesheet è già nello stato APERTO e non può essere riaperto"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void riapri_fromChiuso_asAdmin_ok() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(8);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CHIUSO);
        ts = timesheetRepository.save(ts);

        // CHIUSO → CONFERMATO (primo step del riapri; per tornare APERTO serve un secondo riapri)
        mockMvc.perform(put("/api/timesheets/{id}/riapri", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet riaperto"))
                .andExpect(jsonPath("$.data.stato").value("CONFERMATO"));
    }

    @Test
    @WithMockUser(username = "user@serendipity.com", roles = "DIPENDENTE")
    void riapri_fromChiuso_asDipendente_forbidden() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(9);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CHIUSO);
        ts = timesheetRepository.save(ts);

        mockMvc.perform(put("/api/timesheets/{id}/riapri", ts.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo ADMIN può riaprire un timesheet CHIUSO"));
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void chiudi_okIfConfermato() throws Exception {
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(10);
        ts.setUtente(dipendente);
        ts.setStato(TimesheetStato.CONFERMATO);
        ts = timesheetRepository.save(ts);

        MvcResult res = mockMvc.perform(put("/api/timesheets/{id}/chiudi", ts.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet chiuso"))
                .andExpect(jsonPath("$.data.stato").value("CHIUSO"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(res.getResponse().getContentAsString(), new TypeReference<>() {
        });
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data.get("dataCompilazione")).isNotNull();
    }

    /* ======================= GET /api/timesheets/{id}/totali ======================= */

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void totali_overallAndPerCliente_ok() throws Exception {
        // TS + due clienti + 2 righe
        Timesheet ts = new Timesheet();
        ts.setAnno(2025);
        ts.setMese(11);
        ts.setUtente(dipendente);
        ts = timesheetRepository.save(ts);

        Cliente c1 = new Cliente();
        c1.setNome("Acme");
        c1.setTariffaOraria(10.0);
        c1 = clienteRepository.save(c1);
        Cliente c2 = new Cliente();
        c2.setNome("Globex");
        c2.setTariffaOraria(20.0);
        c2 = clienteRepository.save(c2);

        // Riga 1: 1h 30m per c1 → orario=1.50, costo=15.00
        TimesheetRiga r1 = new TimesheetRiga();
        r1.setTimesheet(ts);
        r1.setCliente(c1);
        r1.setData(LocalDate.of(2025, 11, 3));
        r1.setOre(1);
        r1.setMinuti(30);
        r1.setOrario(1.50);
        r1.setCostoOrario(15.00);
        rigaRepository.save(r1);

        // Riga 2: 2h 00m per c2 → orario=2.00, costo=40.00
        TimesheetRiga r2 = new TimesheetRiga();
        r2.setTimesheet(ts);
        r2.setCliente(c2);
        r2.setData(LocalDate.of(2025, 11, 4));
        r2.setOre(2);
        r2.setMinuti(0);
        r2.setOrario(2.00);
        r2.setCostoOrario(40.00);
        rigaRepository.save(r2);

        // totali complessivi
        mockMvc.perform(get("/api/timesheets/{id}/totali", ts.getId())
                        .param("perCliente", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Totali timesheet"))
                .andExpect(jsonPath("$.data.totaleOrario").value(3.5))
                .andExpect(jsonPath("$.data.totaleCosto").value(55.00));

        // totali per cliente
        mockMvc.perform(get("/api/timesheets/{id}/totali", ts.getId())
                        .param("perCliente", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Totali per cliente"))
                .andExpect(jsonPath("$.data[0].clienteNome").value("Acme"))
                .andExpect(jsonPath("$.data[1].clienteNome").value("Globex"));
    }
}

