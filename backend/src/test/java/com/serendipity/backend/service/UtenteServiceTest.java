package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.CreaUtenteDto;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class UtenteServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UtenteMapper utenteMapper;

    @InjectMocks
    private UtenteService utenteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void creaUtente_codiceFiscaleGiaEsistente_throwsException() {
        CreaUtenteDto dto = new CreaUtenteDto();
        dto.setCodiceFiscale("RSSMRA85T10A562S");
        when(utenteRepository.existsByCodiceFiscale(dto.getCodiceFiscale())).thenReturn(true);

        assertThrows(Exception.class, () -> utenteService.creaUtente(dto));
    }

    @Test
    void getAll_returnsListOfUtenti() {
        when(utenteRepository.findAll()).thenReturn(List.of(new Utente()));
        when(utenteMapper.toDto(any())).thenReturn(new UtenteDto(1L, "RSSMRA85T10A562S", "Mario", "Rossi", "mario.rossi@serendipity.com", "DIPENDENTE"));

        var result = utenteService.getAll();
        assertFalse(result.isEmpty());
    }

    @Test
    void findById_notFound_throwsException() {
        when(utenteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(Exception.class, () -> utenteService.findById(1L));
    }

    @Test
    void eliminaUtente_notFound_throwsException() {
        when(utenteRepository.existsById(1L)).thenReturn(false);
        assertThrows(Exception.class, () -> utenteService.eliminaUtente(1L));
    }

    @Test
    void aggiornaUtente_emailGiaEsistente_throwsException() {
        CreaUtenteDto dto = new CreaUtenteDto();
        dto.setEmail("test@test.com");
        Utente existing = new Utente();
        existing.setEmail("altro@test.com");
        when(utenteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utenteRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(Exception.class, () -> utenteService.aggiornaUtente(1L, dto));
    }
}
