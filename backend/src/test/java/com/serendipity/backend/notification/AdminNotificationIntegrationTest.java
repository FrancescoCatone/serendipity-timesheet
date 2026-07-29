package com.serendipity.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminNotificationIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void registerMailProperties(DynamicPropertyRegistry registry) {
        registry.add("app.notifications.enabled", () -> "true");
        registry.add("app.notifications.recipient-email", () -> "raffaele.vermiglio@serendipitycoop.it");
        registry.add("app.notifications.from-email", () -> "noreply@serendipitycoop.it");
        registry.add("app.notifications.subject-prefix", () -> "Serendipity");
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> greenMail.getSmtp().getPort());
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearMailbox() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void creaUtente_inviaNotificaEmail() throws Exception {
        CreaUtenteDto dto = new CreaUtenteDto();
        dto.setNome("Mario");
        dto.setCognome("Rossi");
        dto.setCodiceFiscale("RSSMRA85T10A562S");
        dto.setEmail("mario.rossi@serendipity.com");
        dto.setPassword("Password123!");
        dto.setRuolo(Ruolo.DIPENDENTE);
        dto.setPagaOraria(12.0);

        mockMvc.perform(post("/api/utenti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("Serendipity - Utente creato");
        assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("raffaele.vermiglio@serendipitycoop.it");
        assertThat(messages[0].getContent().toString())
                .contains("Operazione: Creazione utente")
                .contains("Eseguita da: raffaele.vermiglio@serendipitycoop.it")
                .contains("mario.rossi@serendipity.com");
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void creaCliente_inviaNotificaEmail() throws Exception {
        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome("Acme S.p.A.");
        dto.setTariffaOraria(42.5);

        mockMvc.perform(post("/api/clienti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("Serendipity - Cliente creato");
        assertThat(messages[0].getContent().toString())
                .contains("Operazione: Creazione cliente")
                .contains("Acme S.p.A.")
                .contains("42,50");
    }

    @Test
    @WithMockUser(username = "raffaele.vermiglio@serendipitycoop.it", roles = "ADMIN")
    void eliminaTimesheet_inviaNotificaEmail() throws Exception {
        Utente dipendente = utenteRepository.findByEmail("user@serendipity.com").orElseGet(() -> {
            Utente u = new Utente();
            u.setCodiceFiscale("USRUSR85T10A562Q");
            u.setNome("Mario");
            u.setCognome("Rossi");
            u.setEmail("user@serendipity.com");
            u.setPassword(passwordEncoder.encode("UserTest123!"));
            u.setRuolo(Ruolo.DIPENDENTE);
            u.setPagaOraria(12.0);
            return utenteRepository.save(u);
        });

        CreaTimesheetDto dto = new CreaTimesheetDto();
        dto.setMese(6);
        dto.setAnno(2026);
        dto.setUtenteId(dipendente.getId());

        MvcResult createResult = mockMvc.perform(post("/api/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        @SuppressWarnings("unchecked")
        Integer id = (Integer) ((Map<String, Object>) objectMapper.readValue(
                createResult.getResponse().getContentAsString(), Map.class).get("data")).get("id");

        greenMail.purgeEmailFromAllMailboxes();

        mockMvc.perform(delete("/api/timesheets/{id}", id))
                .andExpect(status().isOk());

        Timesheet deleted = timesheetRepository.findById(Long.valueOf(id)).orElse(null);
        assertThat(deleted).isNull();

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getSubject()).isEqualTo("Serendipity - Timesheet eliminato");
        assertThat(messages[0].getContent().toString())
                .contains("Operazione: Eliminazione timesheet")
                .contains("periodo=06/2026")
                .contains("user@serendipity.com");
    }
}
