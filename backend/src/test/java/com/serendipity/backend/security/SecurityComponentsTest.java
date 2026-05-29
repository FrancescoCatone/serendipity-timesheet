package com.serendipity.backend.security;

import com.serendipity.backend.config.AppSecurityProperties;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.UtenteRepository;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SecurityComponentsTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private AppSecurityProperties securityProperties;

    @Mock
    private UtenteRepository utenteRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(securityProperties.getJwtSecret()).thenReturn("change-me-with-a-long-random-secret-at-least-32-characters");
        when(securityProperties.getJwtExpirationMs()).thenReturn(36000000L);

        Utente u = new Utente();
        u.setEmail("user@serendipity.com");
        u.setPassword("encodedPass");
        u.setRuolo(Ruolo.DIPENDENTE);
        userDetails = UtenteUserDetails.build(u);
    }

    /* ==================== JWT SERVICE ==================== */

    @Test
    @DisplayName("generateToken e isTokenValid funzionano correttamente")
    void jwtService_generateAndValidateToken_ok() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals(userDetails.getUsername(), username);

        boolean valid = jwtService.isTokenValid(token, userDetails);
        assertTrue(valid);
    }

    @Test
    @DisplayName("isTokenValid ritorna false se username non corrisponde")
    void jwtService_tokenInvalid_usernameMismatch() {
        String token = jwtService.generateToken(userDetails);

        Utente fake = new Utente();
        fake.setEmail("altro@serendipity.com");
        fake.setPassword("encodedPass");
        fake.setRuolo(Ruolo.DIPENDENTE);
        UserDetails fakeUser = UtenteUserDetails.build(fake);

        assertFalse(jwtService.isTokenValid(token, fakeUser));
    }

    @Test
    @DisplayName("extractClaim lancia eccezione con token malformato")
    void jwtService_extractClaim_invalidToken_throwsException() {
        assertThrows(MalformedJwtException.class, () ->
                jwtService.extractUsername("token_invalido"));
    }

    /* ==================== USERDETAILS SERVICE ==================== */

    @Test
    @DisplayName("loadUserByUsername ritorna UtenteUserDetails se utente esiste")
    void userDetailsService_loadUserByUsername_ok() {
        Utente u = new Utente();
        u.setEmail("admin@serendipity.com");
        u.setPassword("encodedPass");
        u.setRuolo(Ruolo.ADMIN);
        when(utenteRepository.findByEmail("admin@serendipity.com")).thenReturn(Optional.of(u));

        var details = userDetailsService.loadUserByUsername("admin@serendipity.com");

        assertEquals("admin@serendipity.com", details.getUsername());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("loadUserByUsername lancia UsernameNotFoundException se utente non trovato")
    void userDetailsService_userNotFound_throwsException() {
        when(utenteRepository.findByEmail("non@esiste.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("non@esiste.com"));
    }

    /* ==================== UTENTEUSERDETAILS ==================== */

    @Test
    @DisplayName("UtenteUserDetails.build crea correttamente authorities e campi base")
    void utenteUserDetails_build_ok() {
        assertEquals("user@serendipity.com", userDetails.getUsername());
        assertEquals("encodedPass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DIPENDENTE")));
    }

    @Test
    @DisplayName("equals e hashCode funzionano correttamente")
    void utenteUserDetails_equalsAndHashCode() {
        UtenteUserDetails u1 = (UtenteUserDetails) userDetails;
        UtenteUserDetails u2 = (UtenteUserDetails) UtenteUserDetails.build(new Utente() {{
            setEmail("user@serendipity.com");
            setPassword("encodedPass");
            setRuolo(Ruolo.DIPENDENTE);
        }});

        UtenteUserDetails u3 = (UtenteUserDetails) UtenteUserDetails.build(new Utente() {{
            setEmail("altro@serendipity.com");
            setPassword("encodedPass");
            setRuolo(Ruolo.DIPENDENTE);
        }});

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertNotEquals(u1, u3);
    }
}
