package com.serendipity.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.dto.update.AggiornaUtenteDto;
import com.serendipity.backend.model.enums.Ruolo;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UtenteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreaUtenteDto buildValidAdminDto() {
        CreaUtenteDto dto = new CreaUtenteDto();
        dto.setNome("TestAdmin");
        dto.setCognome("User");
        dto.setCodiceFiscale("TSTADM85T10A562Y");
        dto.setEmail("admin.test@serendipity.com");
        dto.setPassword("AdminTest123!");
        dto.setRuolo(Ruolo.ADMIN);
        return dto;
    }

    private AggiornaUtenteDto buildValidUpdateDto() {
        AggiornaUtenteDto dto = new AggiornaUtenteDto();
        dto.setNome("TestAdmin");
        dto.setCognome("User");
        dto.setCodiceFiscale("TSTADM85T10A562Y");
        dto.setEmail("admin.test@serendipity.com");
        dto.setPassword("");
        dto.setRuolo(Ruolo.ADMIN);
        return dto;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreaUtente_success() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();

        mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Utente creato con successo"));
    }

    @Test
    @WithMockUser(roles = "DIPENDENTE")
    void testCreaUtente_forbiddenForDipendente() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();

        mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUtenti_success() throws Exception {
        mockMvc.perform(get("/api/utenti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lista utenti"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetById_success() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();
        MvcResult result = mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        Map<String, Object> utenteCreato = (Map<String, Object>) responseMap.get("data");
        Integer id = (Integer) utenteCreato.get("id");

        mockMvc.perform(get("/api/utenti/id/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente trovato"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteUtente_success() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();
        MvcResult result = mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        Map<String, Object> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        Map<String, Object> utenteCreato = (Map<String, Object>) responseMap.get("data");
        Integer id = (Integer) utenteCreato.get("id");

        mockMvc.perform(delete("/api/utenti/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente eliminato con successo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAggiornaUtente_success() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();
        MvcResult result = mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        Map<String, Object> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });
        Map<String, Object> utenteCreato = (Map<String, Object>) responseMap.get("data");
        Integer id = (Integer) utenteCreato.get("id");

        AggiornaUtenteDto updateDto = buildValidUpdateDto();
        updateDto.setNome("AdminUpdated");

        mockMvc.perform(put("/api/utenti/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente aggiornato con successo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreaUtente_invalidEmail_returns400() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();
        dto.setEmail("admin.test@serendipity");

        mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetByCodiceFiscale_success() throws Exception {
        // 1️⃣ Crea un utente
        CreaUtenteDto dto = buildValidAdminDto();
        MvcResult result = mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        // 2️⃣ Recupera il codice fiscale dal body
        Map<String, Object> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                }
        );
        Map<String, Object> utenteCreato = (Map<String, Object>) responseMap.get("data");
        String codiceFiscale = (String) utenteCreato.get("codiceFiscale");

        // 3️⃣ Invoca l’endpoint di ricerca per codice fiscale
        mockMvc.perform(get("/api/utenti/codiceFiscale/{codiceFiscale}", codiceFiscale))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente trovato"))
                .andExpect(jsonPath("$.data.codiceFiscale").value(codiceFiscale));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetByCodiceFiscale_notFound() throws Exception {
        // Nessun utente creato → deve restituire 404
        mockMvc.perform(get("/api/utenti/codiceFiscale/{codiceFiscale}", "ABCD123456789XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utente non trovato con codice fiscale: ABCD123456789XYZ"));
    }

    @Test
    void testAggiornaParzialeUtente_success() throws Exception {
        // crea un utente come ADMIN
        CreaUtenteDto dto = buildValidAdminDto();
        dto.setEmail("user.one@serendipity.com");
        dto.setCodiceFiscale("USRONE85T10A562Z");
        dto.setRuolo(Ruolo.DIPENDENTE);
        dto.setPagaOraria(12.0);

        MvcResult createRes = mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(createRes.getResponse().getContentAsString(), new TypeReference<>() {
        });
        Integer id = (Integer) ((Map<String, Object>) body.get("data")).get("id");

        // patch: aggiorna nome + email (lowercase enforced)
        Map<String, Object> updates = Map.of("nome", "Mario", "email", "NEW@MAIL.IT");

        mockMvc.perform(patch("/api/utenti/{id}", id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente aggiornato"));
    }

    @Test
    void testAggiornaParzialeUtente_notFound() throws Exception {
        Map<String, Object> updates = Map.of("nome", "X");
        mockMvc.perform(patch("/api/utenti/{id}", 999_999)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAggiornaParzialeUtente_emailConflict() throws Exception {
        // crea utente A
        CreaUtenteDto a = buildValidAdminDto();
        a.setEmail("user.a@serendipity.com");
        a.setCodiceFiscale("USERA185T10A562X");
        a.setRuolo(Ruolo.DIPENDENTE);
        a.setPagaOraria(12.0);
        MvcResult resA = mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(a)))
                .andExpect(status().isCreated())
                .andReturn();
        Integer idA = (Integer) ((Map<String, Object>) objectMapper.readValue(resA.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {
                })
                .get("data")).get("id");

        // crea utente B (email che causerà conflitto)
        CreaUtenteDto b = buildValidAdminDto();
        b.setEmail("user.b@serendipity.com");
        b.setCodiceFiscale("USERB185T10A562W");
        b.setRuolo(Ruolo.DIPENDENTE);
        b.setPagaOraria(12.0);
        mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isCreated());

        // prova a patchare A assegnandogli l'email di B -> 409
        Map<String, Object> updates = Map.of("email", "user.b@serendipity.com");
        mockMvc.perform(patch("/api/utenti/{id}", idA)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isConflict());
    }

    @Test
    void testAggiornaUtente_systemOperator_daAltroAdmin_forbidden() throws Exception {
        CreaUtenteDto dto = buildValidAdminDto();
        dto.setEmail("system.operator@serendipitycoop.it");
        dto.setCodiceFiscale("SYSOPR85T10A562Q");

        MvcResult result = mockMvc.perform(post("/api/utenti")
                        .with(user("system.operator@serendipitycoop.it").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer id = (Integer) ((Map<String, Object>) objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<Map<String, Object>>() {
                }).get("data")).get("id");

        AggiornaUtenteDto updateDto = buildValidUpdateDto();
        updateDto.setEmail("system.operator@serendipitycoop.it");
        updateDto.setCodiceFiscale("SYSOPR85T10A562Q");
        updateDto.setNome("Updated");

        mockMvc.perform(put("/api/utenti/{id}", id)
                        .with(user("raffaele.vermiglio@serendipitycoop.it").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCambiaPassword_success() throws Exception {
        // crea l'utente su cui lavorare (come ADMIN)
        CreaUtenteDto dto = buildValidAdminDto(); // email: admin.test@serendipity.com, pwd: AdminTest123!
        mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // cambia password autenticandosi come quell'utente (qualsiasi ruolo, basta isAuthenticated)
        Map<String, String> payload = Map.of(
                "oldPassword", "AdminTest123!",
                "newPassword", "NewPwd123!"
        );

        mockMvc.perform(patch("/api/utenti/me/password")
                        .with(user("admin.test@serendipity.com").roles("DIPENDENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password aggiornata"));
    }

    @Test
    void testCambiaPassword_badOldPassword_unauthorized() throws Exception {
        // crea l'utente
        CreaUtenteDto dto = buildValidAdminDto();
        mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // oldPassword errata -> 401
        Map<String, String> payload = Map.of(
                "oldPassword", "WrongOld!",
                "newPassword", "NewPwd123!"
        );

        mockMvc.perform(patch("/api/utenti/me/password")
                        .with(user("admin.test@serendipity.com").roles("DIPENDENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCambiaPassword_unauthenticated_401() throws Exception {
        Map<String, String> payload = Map.of(
                "oldPassword", "whatever",
                "newPassword", "whatever2"
        );

        mockMvc.perform(patch("/api/utenti/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    /* ------------------- GET /me (profilo utente corrente) ------------------- */

    @Test
    void testGetProfiloUtenteCorrente_ok() throws Exception {
        // crea utente nel DB
        CreaUtenteDto dto = buildValidAdminDto(); // email: admin.test@serendipity.com
        mockMvc.perform(post("/api/utenti")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // richiede il profilo autenticandosi con quell'email
        mockMvc.perform(get("/api/utenti/me")
                        .with(user("admin.test@serendipity.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profilo utente corrente"))
                .andExpect(jsonPath("$.data.email").value("admin.test@serendipity.com"))
                .andExpect(jsonPath("$.data.nome").value("TestAdmin"))
                .andExpect(jsonPath("$.data.cognome").value("User"))
                .andExpect(jsonPath("$.data.codiceFiscale").value("TSTADM85T10A562Y"))
                .andExpect(jsonPath("$.data.ruolo").value("ADMIN"));
    }

    @Test
    void testGetProfiloUtenteCorrente_unauthenticated_403() throws Exception {
        // nessuna autenticazione → Spring Security deve rispondere 403
        mockMvc.perform(get("/api/utenti/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetProfiloUtenteCorrente_userNotInDb_404() throws Exception {
        // utente autenticato nel SecurityContext ma non presente nel DB → 404
        mockMvc.perform(get("/api/utenti/me")
                        .with(user("fantasma@serendipity.com").roles("DIPENDENTE")))
                .andExpect(status().isNotFound());
    }

}
