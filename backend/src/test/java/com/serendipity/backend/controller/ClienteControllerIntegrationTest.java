package com.serendipity.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.repository.ClienteRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests per ClienteController.
 * Copre: GET all, GET byId, POST create, PUT update, DELETE, GET dipendenti, GET totale-ore.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    // -----------------------
    // Helpers
    // -----------------------

    private CreaClienteDto buildCreateDto(String nome, Double tariffa) {
        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome(nome);
        dto.setTariffaOraria(tariffa);
        return dto;
    }

    private Cliente persistCliente(String nome, double tariffa) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setTariffaOraria(tariffa);
        return clienteRepository.save(c);
    }

    // -----------------------
    // Sicurezza
    // -----------------------

    @Test
    @DisplayName("Sicurezza: senza ruolo ADMIN → 403 su qualsiasi endpoint protetto")
    @WithMockUser(username = "user", roles = {"USER"})
    void security_forbidden_without_admin_role() throws Exception {
        mockMvc.perform(get("/api/clienti"))
                .andExpect(status().isForbidden());
    }


    // -----------------------
    // GET /api/clienti/{id}
    // -----------------------

    @Test
    @DisplayName("GET /api/clienti/{id} - OK se esiste")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_ok() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);

        mockMvc.perform(get("/api/clienti/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.nome").value("Acme S.p.A."))
                .andExpect(jsonPath("$.data.tariffaOraria").value(45.0));
    }

    @Test
    @DisplayName("GET /api/clienti/{id} - 404 se non esiste")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_notFound() throws Exception {
        mockMvc.perform(get("/api/clienti/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    // -----------------------
    // POST /api/clienti (create)
    // -----------------------

    @Test
    @DisplayName("POST /api/clienti - crea OK (201)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_ok() throws Exception {
        var body = objectMapper.writeValueAsString(buildCreateDto("Acme S.p.A.", 45.0));

        mockMvc.perform(post("/api/clienti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("creato")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.nome").value("Acme S.p.A."))
                .andExpect(jsonPath("$.data.tariffaOraria").value(45.0));
    }

    @Test
    @DisplayName("POST /api/clienti - 400 validazione KO (nome blank, tariffa <= 0)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_validation_badRequest() throws Exception {
        var invalid = new CreaClienteDto();
        invalid.setNome("   "); // blank
        invalid.setTariffaOraria(0.0); // non valido (deve essere > 0)
        var body = objectMapper.writeValueAsString(invalid);

        mockMvc.perform(post("/api/clienti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/clienti - 409 se nome duplicato (case-insensitive)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_duplicate_conflict() throws Exception {
        persistCliente("Acme S.p.A.", 45.0);

        var dup = buildCreateDto("acme s.p.a.", 60.0); // stesso nome, case-insensitive
        var body = objectMapper.writeValueAsString(dup);

        mockMvc.perform(post("/api/clienti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // -----------------------
    // PUT /api/clienti/{id} (update)
    // -----------------------

    @Test
    @DisplayName("PUT /api/clienti/{id} - update OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_ok() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);
        var upd = buildCreateDto("Acme Updated", 52.5);
        var body = objectMapper.writeValueAsString(upd);

        mockMvc.perform(put("/api/clienti/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("aggiornato")))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.nome").value("Acme Updated"))
                .andExpect(jsonPath("$.data.tariffaOraria").value(52.5));
    }

    @Test
    @DisplayName("PUT /api/clienti/{id} - 404 se id inesistente")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_notFound() throws Exception {
        var upd = buildCreateDto("Qualcuno", 40.0);
        var body = objectMapper.writeValueAsString(upd);

        mockMvc.perform(put("/api/clienti/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/clienti/{id} - 409 se nome in uso da altro cliente")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_duplicate_conflict() throws Exception {
        Cliente c1 = persistCliente("Acme S.p.A.", 45.0);
        persistCliente("Globex SRL", 55.0);

        var upd = buildCreateDto("globex srl", 60.0); // collide con altro record (case-insensitive)
        var body = objectMapper.writeValueAsString(upd);

        mockMvc.perform(put("/api/clienti/{id}", c1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/clienti/{id} - 400 validazione KO")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_validation_badRequest() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);

        var invalid = new CreaClienteDto();
        invalid.setNome(""); // not blank
        invalid.setTariffaOraria(-10.0); // non valido
        var body = objectMapper.writeValueAsString(invalid);

        mockMvc.perform(put("/api/clienti/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // -----------------------
    // DELETE /api/clienti/{id}
    // -----------------------

    @Test
    @DisplayName("DELETE /api/clienti/{id} - OK")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_ok() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);

        mockMvc.perform(delete("/api/clienti/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("eliminato")));
    }

    @Test
    @DisplayName("DELETE /api/clienti/{id} - 404 se inesistente")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_notFound() throws Exception {
        mockMvc.perform(delete("/api/clienti/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    // -----------------------
    // GET /api/clienti/{clienteId}/dipendenti?mese&anno
    // -----------------------

    @Test
    @DisplayName("GET /api/clienti/{id}/dipendenti - OK (lista vuota se non ci sono righe)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getDipendentiPerClienteEMese_ok_empty() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);

        mockMvc.perform(get("/api/clienti/{clienteId}/dipendenti", saved.getId())
                        .param("mese", "10")
                        .param("anno", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("dipendenti")))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }

    // -----------------------
    // GET /api/clienti/{clienteId}/totale-ore?mese&anno
    // -----------------------

    @Test
    @DisplayName("GET /api/clienti/{id}/totale-ore - OK (0 se non ci sono righe)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getTotaleOrePerCliente_ok_zero() throws Exception {
        Cliente saved = persistCliente("Acme S.p.A.", 45.0);

        mockMvc.perform(get("/api/clienti/{clienteId}/totale-ore", saved.getId())
                        .param("mese", "10")
                        .param("anno", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("totale")))
                .andExpect(jsonPath("$.data", isA(Number.class)))
                .andExpect(jsonPath("$.data").value(0.0));
    }

    // -----------------------
    // Non-ADMIN: esempi puntuali di 403 sugli endpoint
    // -----------------------

    @Nested
    class ForbiddenExamples {

        @Test
        @DisplayName("POST /api/clienti - 403 se non ADMIN")
        @WithMockUser(username = "user", roles = {"USER"})
        void create_forbidden() throws Exception {
            var body = objectMapper.writeValueAsString(buildCreateDto("Acme", 40.0));
            mockMvc.perform(post("/api/clienti")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/clienti/{id} - 403 se non ADMIN")
        @WithMockUser(username = "user", roles = {"USER"})
        void delete_forbidden() throws Exception {
            mockMvc.perform(delete("/api/clienti/{id}", 1L))
                    .andExpect(status().isForbidden());
        }

    }
}
