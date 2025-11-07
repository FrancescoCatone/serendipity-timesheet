package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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
        d.setPassword(TEST_PWD); // <<<<<< sostituito
        d.setCodiceFiscale(cf);
        d.setRuolo(ruolo);
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

    /* ----------------------------- getAll ------------------------------ */

    @Test
    void getAll_mapsToDto() {
        Utente u = ent(1L, "a@a.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findAll()).thenReturn(List.of(u));
        when(utenteMapper.toDto(u))
                .thenReturn(new UtenteDto(1L, u.getCodiceFiscale(), u.getNome(), u.getCognome(), u.getEmail(), u.getRuolo().name()));

        var out = service.getAll();

        assertThat(out).hasSize(1);
        assertThat(out.get(0).email()).isEqualTo("a@a.it");
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
                .thenReturn(new UtenteDto(10L, d.getCodiceFiscale(), d.getNome(), d.getCognome(), "admin@acme.it", "ADMIN"));

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
        when(utenteMapper.toDto(u)).thenReturn(new UtenteDto(7L, u.getCodiceFiscale(), u.getNome(), u.getCognome(), u.getEmail(), "DIPENDENTE"));

        var resp = service.findByCodiceFiscale("RSSMRA85T10A562S");

        assertThat(resp.getStatus()).isEqualTo(200);
        var dto = (UtenteDto) resp.getData();
        assertThat(dto.id()).isEqualTo(7L);
    }

    /* ---------------------------- aggiornaUtente ----------------------- */

    @Test
    void aggiornaUtente_notFound_throws404() {
        when(utenteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.aggiornaUtente(1L, dto("x@x.it", "RSSMRA...", Ruolo.DIPENDENTE)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void aggiornaUtente_cfCambiato_maGiaUsato_conflict409() {
        Utente existing = ent(1L, "old@acme.it", "OLDOLD85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.existsByCodiceFiscale("NEWNEW85T10A562S")).thenReturn(true);

        var d = dto("old@acme.it", "NEWNEW85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Codice fiscale");
    }

    @Test
    void aggiornaUtente_emailCambiata_maGiaUsata_conflict409() {
        Utente existing = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.existsByEmail("new@acme.it")).thenReturn(true);

        var d = dto("new@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);

        assertThatThrownBy(() -> service.aggiornaUtente(1L, d))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void aggiornaUtente_ok_saves_and_encodesPassword() {
        Utente existing = ent(1L, "old@acme.it", "RSSMRA85T10A562S", Ruolo.DIPENDENTE);
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(TEST_PWD)).thenReturn("ENC2"); // <<<<<< sostituito

        var d = dto("NEW@ACME.IT", "RSSMRA85T10A562S", Ruolo.ADMIN);

        var resp = service.aggiornaUtente(1L, d);
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(existing.getEmail()).isEqualTo("new@acme.it");
        assertThat(existing.getPassword()).isEqualTo("ENC2");
        assertThat(existing.getRuolo()).isEqualTo(Ruolo.ADMIN);

        verify(utenteRepository).save(existing);
    }

    /* ----------------------------- eliminaUtente ----------------------- */

    @Test
    void eliminaUtente_notFound_throws404() {
        when(utenteRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.eliminaUtente(9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void eliminaUtente_ok_deletes() {
        when(utenteRepository.existsById(9L)).thenReturn(true);
        var resp = service.eliminaUtente(9L);
        assertThat(resp.getStatus()).isEqualTo(200);
        verify(utenteRepository).deleteById(9L);
    }
}
