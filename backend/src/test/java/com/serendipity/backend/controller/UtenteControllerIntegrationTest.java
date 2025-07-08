package com.serendipity.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serendipity.backend.model.dto.CreaUtenteDto;
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

        dto.setNome("AdminUpdated");

        mockMvc.perform(put("/api/utenti/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utente aggiornato con successo"));
    }

}
