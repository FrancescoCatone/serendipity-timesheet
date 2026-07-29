package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.dto.update.AggiornaPasswordDto;
import com.serendipity.backend.model.dto.update.AggiornaUtenteDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtenteServiceTest {

    // usa una password "neutra" (o prendi da env) per evitare segreti hard-coded
    private static final String TEST_PWD = System.getenv().getOrDefault("TEST_PWD", "not-a-secret");

    @Mock
    private UtenteRepository utenteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UtenteMapper utenteMapper;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private TimesheetRigaRepository timesheetRigaRepository;
    @Mock
    private AdminNotificationService notificationService;

    @InjectMocks
    private UtenteService service;

    @BeforeEach
    void init() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    /* ----------------------------- helpers ----------------------------- */

    private CreaUtenteDto dto(String email, String cf, Ruolo ruolo) {
        CreaUtenteDto d = new CreaUtenteDto();
        d.setNome("Mario");
        d.setCognome("Rossi");
        d.setEmail(email);
        d.setPassword(TEST_PWD);
        d.setCodiceFiscale(cf);
        d.setRuolo(ruolo);
        d.setPagaOraria(ruolo == Ruolo.DIPENDENTE ? 12.5 : null);
        return d;
    }

    private AggiornaUtenteDto updateDto(String email, String cf, Ruolo ruolo) {
        AggiornaUtenteDto d = new AggiornaUtenteDto();
        d.setNome("Mario");
        d.setCognome("Rossi");
        d.setEmail(email);
        d.setPassword("");
        d.setCodiceFiscale(cf);
        d.setRuolo(ruolo);
        d.setPagaOraria(ruolo == Ruolo.DIPENDENTE ? 12.5 : null);
        return d;
    }

    private Utente ent(Long id, String email, String cf, Ruolo ruolo) {
        Utente u = new Utente();
        u.setId(id);
        u.setNome("Mario");
        u.setCognome("Rossi");
        u.setEmail(email);
        u.setPassword("hash");
        u.setCodiceFiscale(cf);
        u.setRuolo(ruolo);
        u.setPagaOraria(ruolo == Ruolo.DIPENDENTE ? 12.5 : null);
        return u;
    }

    private void authAsUser() {
        var auth = new UsernamePasswordAuthenticationToken(
                "user@acme.it", "x",
                List.of(new SimpleGrantedAuthority("ROLE_DIPENDENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@acme.it", "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authAsSystemOperator() {
        var auth = new UsernamePasswordAuthenticationToken(
                "system.operator@serendipitycoop.it", "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /* ----------------------------- getAll ------------------------------ */

    @Test
    void getAll_mapsToDto() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findAll()).thenReturn(List.of(u));
        when(utenteMapper.toDto(u))
                .thenReturn(new UtenteDto(1L, u.getCodiceFiscale(), u.getNome(), u.getCognome(), u.getEmail(), u.getRuolo().name(), u.getPagaOraria()));

        var out = service.getAll();

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().email()).isEqualTo("a@a.it");
    }

    /* ----------------------------- findById ---------------------------- */

    @Test
    void findById_notFound_throws404() {
        when(utenteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /* --------------------------- creaUtente ---------------------------- */

    @Test
    void creaUtente_cfDuplicato_conflict409() {
        var d = dto("mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.existsByCodiceFiscale("RSSMRA85T10A562S")).thenReturn(true);

        assertThatThrownBy(() -> service.creaUtente(d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Codice fiscale");
    }

    @Test
    void creaUtente_emailDuplicata_conflict409() {
        var d = dto("mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.existsByCodiceFiscale(d.getCodiceFiscale())).thenReturn(false);
        when(utenteRepository.existsByEmail("mario@acme.it")).thenReturn(true);

        assertThatThrownBy(() -> service.creaUtente(d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void creaUtente_emailSenzaTld_badRequest() {
        var d = dto("mario@acme", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.creaUtente(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email non valida");
    }

    @Test
    void creaUtente_ruoloAdmin_daNonAdmin_forbidden403() {
        authAsUser(); // non admin
        var d = dto("mario@acme.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        when(utenteRepository.existsByCodiceFiscale(any())).thenReturn(false);
        when(utenteRepository.existsByEmail(any())).thenReturn(false);

        assertThatThrownBy(() -> service.creaUtente(d))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ruolo ADMIN");
    }

    @Test
    void creaUtente_ruoloAdmin_daAdmin_ok_emailLowerTrim_passwordEncoded() {
        authAsAdmin();
        var d = dto("  Admin@ACME.IT  ", "RSSMRA85T10A562S", Ruolo.ADMIN);

        when(utenteRepository.existsByCodiceFiscale(any())).thenReturn(false);
        when(utenteRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(TEST_PWD)).thenReturn("ENC"); // <<<<<< sostituito
        ArgumentCaptor<Utente> cap = ArgumentCaptor.forClass(Utente.class);
        when(utenteRepository.save(any(Utente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utenteMapper.toDto(any(Utente.class)))
                .thenReturn(new UtenteDto(10L, d.getCodiceFiscale(), d.getNome(), d.getCognome(), "admin@acme.it", "ADMIN", null));

        var resp = service.creaUtente(d);
        assertThat(resp.getStatus()).isEqualTo(201);

        verify(passwordEncoder).encode(TEST_PWD); // <<<<<< sostituito
        verify(utenteRepository).save(cap.capture());
        Utente saved = cap.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@acme.it");
        assertThat(saved.getPassword()).isEqualTo("ENC");
        assertThat(saved.getRuolo()).isEqualTo(Ruolo.ADMIN);
    }

    /* ------------------------- findByCodiceFiscale --------------------- */

    @Test
    void findByCodiceFiscale_ok_mapsToDto() {
        Utente u = ent(7L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findByCodiceFiscale("RSSMRA85T10A562S")).thenReturn(u);
        when(utenteMapper.toDto(u)).thenReturn(new UtenteDto(7L, u.getCodiceFiscale(), u.getNome(), u.getCognome(), u.getEmail(), "DIPENDENTE", u.getPagaOraria()));

        var resp = service.findByCodiceFiscale("RSSMRA85T10A562S");

        assertThat(resp.getStatus()).isEqualTo(200);
        var dto = (UtenteDto) resp.getData();
        assertThat(dto.id()).isEqualTo(7L);
    }

    /* ---------------------------- aggiornaUtente ----------------------- */

    @Test
    void aggiornaUtente_notFound_throws404() {
        when(utenteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.aggiornaUtente(1L, updateDto("x@x.it", "RSSMRA...", Ruolo.DIPENDENTE)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void aggiornaUtente_cfCambiato_maGiaUsato_conflict409() {
        Utente existing = ent(1L, "old@acme.it", "OLDOLD85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.existsByCodiceFiscale("NEWNEW85T10A562S")).thenReturn(true);

        var d = updateDto("old@acme.it", "NEWNEW85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Codice fiscale");
    }

    @Test
    void aggiornaUtente_emailCambiata_maGiaUsata_conflict409() {
        Utente existing = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.existsByEmail("new@acme.it")).thenReturn(true);

        var d = updateDto("new@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void aggiornaUtente_emailSenzaTld_badRequest() {
        Utente existing = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));

        var d = updateDto("new@acme", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email non valida");
    }

    @Test
    void aggiornaUtente_ok_saves_and_encodesPassword() {
        Utente existing = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(TEST_PWD)).thenReturn("ENC2"); // <<<<<< sostituito

        var d = updateDto("NEW@ACME.IT", "RSSMRA85T10A562S", Ruolo.ADMIN);
        d.setPassword(TEST_PWD);

        var resp = service.aggiornaUtente(1L, d);
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(existing.getEmail()).isEqualTo("new@acme.it");
        assertThat(existing.getPassword()).isEqualTo("ENC2");
        assertThat(existing.getRuolo()).isEqualTo(Ruolo.ADMIN);

        verify(utenteRepository).save(existing);
    }

    @Test
    void aggiornaUtente_systemOperator_daAltroAdmin_forbidden() {
        authAsAdmin();
        Utente existing = ent(1L, "system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));

        var d = updateDto("system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("system operator");
    }

    @Test
    void aggiornaUtente_systemOperator_daSeStesso_ok() {
        authAsSystemOperator();
        Utente existing = ent(1L, "system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(TEST_PWD)).thenReturn("ENC2");

        var d = updateDto("system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        d.setPassword(TEST_PWD);

        var resp = service.aggiornaUtente(1L, d);
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    /* ----------------------------- eliminaUtente ----------------------- */

    @Test
    void eliminaUtente_notFound_throws404() {
        when(utenteRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.eliminaUtente(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminaUtente_ok_deletes() {
        when(utenteRepository.findById(9L)).thenReturn(Optional.of(ent(9L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE)));
        when(timesheetRepository.existsByUtenteId(9L)).thenReturn(false);
        var resp = service.eliminaUtente(9L);
        assertThat(resp.getStatus()).isEqualTo(200);
        verify(utenteRepository).deleteById(9L);
    }

    @Test
    void eliminaUtente_conTimesheetAssociati_conflict409() {
        when(utenteRepository.findById(9L)).thenReturn(Optional.of(ent(9L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE)));
        when(timesheetRepository.existsByUtenteId(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminaUtente(9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timesheet associati");
        verify(utenteRepository, never()).deleteById(anyLong());
    }

    @Test
    void eliminaUtente_systemOperator_daAltroAdmin_forbidden() {
        authAsAdmin();
        Utente existing = ent(9L, "system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        when(utenteRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.eliminaUtente(9L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("system operator");
        verify(utenteRepository, never()).deleteById(anyLong());
    }

    /* ---------------------------- aggiornaParziale --------------------- */

    @Test
    void aggiornaParziale_notFound_throws404() {
        when(utenteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("nome", "X")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void aggiornaParziale_nome_blank_illegalArgument() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("nome", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_email_duplicate_conflict409() {
        Utente u = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));
        when(utenteRepository.existsByEmail("new@acme.it")).thenReturn(true);

        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("email", "NEW@ACME.IT")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_email_lowercase_and_saved() {
        Utente u = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));
        when(utenteRepository.existsByEmail("new@acme.it")).thenReturn(false);

        var updates = new HashMap<String, Object>();
        updates.put("email", "NeW@AcMe.It");
        var resp = service.aggiornaParziale(1L, updates);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(u.getEmail()).isEqualTo("new@acme.it");
        verify(utenteRepository).save(u);
    }

    @Test
    void aggiornaParziale_emailSenzaTld_badRequest() {
        Utente u = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("email", "new@acme")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email non valida");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_systemOperator_daAltroAdmin_forbidden() {
        authAsAdmin();
        Utente u = ent(1L, "system.operator@serendipitycoop.it", "RSSMRA85T10A562S", Ruolo.ADMIN);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("nome", "Nuovo")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("system operator");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_cf_duplicate_conflict409() {
        Utente u = ent(1L, "a@a.it", "OLDOLD85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));
        when(utenteRepository.existsByCodiceFiscale("NEWNEW85T10A562S")).thenReturn(true);

        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("codiceFiscale", "NEWNEW85T10A562S")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Codice fiscale");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_ruolo_nonValido_illegalArgument() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.aggiornaParziale(1L, Map.of("ruolo", "NOT_A_ROLE")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_password_blank_ignored() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        u.setPassword("OLD_HASH");
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));

        var updates = new HashMap<String, Object>();
        updates.put("password", "   "); // blank -> non deve encodare
        var resp = service.aggiornaParziale(1L, updates);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(u.getPassword()).isEqualTo("OLD_HASH");
        verify(passwordEncoder, never()).encode(any());
        verify(utenteRepository).save(u);
    }

    @Test
    void aggiornaParziale_password_nonBlank_encoded() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("nuovaPwd")).thenReturn("ENC_NEW");

        var updates = new HashMap<String, Object>();
        updates.put("password", "nuovaPwd");
        var resp = service.aggiornaParziale(1L, updates);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(u.getPassword()).isEqualTo("ENC_NEW");
        verify(passwordEncoder).encode("nuovaPwd");
        verify(utenteRepository).save(u);
    }

    @Test
    void aggiornaParziale_nome_e_cognome_ok() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(u));

        Map<String, Object> updates = new HashMap<>();
        updates.put("nome", "Luca");
        updates.put("cognome", "Bianchi");
        var resp = service.aggiornaParziale(1L, updates);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(u.getNome()).isEqualTo("Luca");
        assertThat(u.getCognome()).isEqualTo("Bianchi");
        verify(utenteRepository).save(u);
    }

    /* ---------------------- cambiaPasswordUtenteCorrente --------------- */

    @Test
    void cambiaPassword_notAuthenticated_illegalState() {
        // nessuna auth nel SecurityContext
        AggiornaPasswordDto d = new AggiornaPasswordDto();
        d.setOldPassword("old");
        d.setNewPassword("new");
        assertThatThrownBy(() -> service.cambiaPasswordUtenteCorrente(d))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non autenticato");
    }

    @Test
    void cambiaPassword_userNotFound_404() {
        // utente autenticato come string principal (email)
        var auth = new UsernamePasswordAuthenticationToken("mario@acme.it", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(utenteRepository.findByEmail("mario@acme.it")).thenReturn(Optional.empty());

        AggiornaPasswordDto d = new AggiornaPasswordDto();
        d.setOldPassword("old");
        d.setNewPassword("new");

        assertThatThrownBy(() -> service.cambiaPasswordUtenteCorrente(d))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void cambiaPassword_oldMismatch_badCredentials() {
        var auth = new UsernamePasswordAuthenticationToken("mario@acme.it", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Utente u = ent(1L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        u.setPassword("HASH");
        when(utenteRepository.findByEmail("mario@acme.it")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "HASH")).thenReturn(false);

        AggiornaPasswordDto d = new AggiornaPasswordDto();
        d.setOldPassword("old");
        d.setNewPassword("new");

        assertThatThrownBy(() -> service.cambiaPasswordUtenteCorrente(d))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("attuale errata");
        verify(utenteRepository, never()).save(any());
    }

    @Test
    void cambiaPassword_ok_encoded_and_saved() {
        var auth = new UsernamePasswordAuthenticationToken("mario@acme.it", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Utente u = ent(1L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        u.setPassword("OLD_HASH");
        when(utenteRepository.findByEmail("mario@acme.it")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("ENC_NEW");

        AggiornaPasswordDto d = new AggiornaPasswordDto();
        d.setOldPassword("old");
        d.setNewPassword("new");

        var resp = service.cambiaPasswordUtenteCorrente(d);

        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(u.getPassword()).isEqualTo("ENC_NEW");
        verify(utenteRepository).save(u);
    }

    /* ------------------- getProfiloUtenteCorrente ---------------------- */

    @Test
    void getProfiloUtenteCorrente_notAuthenticated_illegalState() {
        // nessuna auth nel SecurityContext
        assertThatThrownBy(() -> service.getProfiloUtenteCorrente())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non autenticato");
    }

    @Test
    void getProfiloUtenteCorrente_userNotFound_404() {
        var auth = new UsernamePasswordAuthenticationToken("fantasma@acme.it", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(utenteRepository.findByEmail("fantasma@acme.it")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfiloUtenteCorrente())
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getProfiloUtenteCorrente_ok_returnsDto() {
        var auth = new UsernamePasswordAuthenticationToken("mario@acme.it", "x", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Utente u = ent(1L, "mario@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findByEmail("mario@acme.it")).thenReturn(Optional.of(u));

        var profilo = service.getProfiloUtenteCorrente();

        assertThat(profilo.codiceFiscale()).isEqualTo("RSSMRA85T10A562S");
        assertThat(profilo.nome()).isEqualTo("Mario");
        assertThat(profilo.cognome()).isEqualTo("Rossi");
        assertThat(profilo.email()).isEqualTo("mario@acme.it");
        assertThat(profilo.ruolo()).isEqualTo("DIPENDENTE");
    }
}
