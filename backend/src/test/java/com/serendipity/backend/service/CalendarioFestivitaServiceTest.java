package com.serendipity.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CalendarioFestivitaServiceTest {

    @InjectMocks
    private CalendarioFestivitaService service;

    // ------------------------------------------------------------------ domenica

    @Test
    void domenica_isFestivo() {
        // 2025-03-09 è una domenica
        assertThat(service.isFestivo(LocalDate.of(2025, 3, 9))).isTrue();
    }

    @Test
    void domenica_diversaSettimana_isFestivo() {
        // 2024-12-29 è una domenica
        assertThat(service.isFestivo(LocalDate.of(2024, 12, 29))).isTrue();
    }

    // ------------------------------------------------------------------ sabato (non festivo)

    @Test
    void sabato_isNotFestivo() {
        // 2025-03-08 è un sabato
        assertThat(service.isFestivo(LocalDate.of(2025, 3, 8))).isFalse();
    }

    // ------------------------------------------------------------------ giorno lavorativo normale

    @Test
    void giornoFeriale_isNotFestivo() {
        // 2025-03-10 è un lunedì qualunque, non festivo
        assertThat(service.isFestivo(LocalDate.of(2025, 3, 10))).isFalse();
    }

    // ------------------------------------------------------------------ festività fisse

    @ParameterizedTest(name = "{0}/{1} deve essere festivo")
    @CsvSource({
            "2025, 1,  1",   // Capodanno
            "2025, 1,  6",   // Epifania
            "2025, 4, 25",   // Liberazione
            "2025, 5,  1",   // Festa del Lavoro
            "2025, 6,  2",   // Festa della Repubblica
            "2025, 8, 15",   // Ferragosto
            "2025,11,  1",   // Ognissanti
            "2025,12,  8",   // Immacolata
            "2025,12, 25",   // Natale
            "2025,12, 26",   // Santo Stefano
    })
    void festivitaFissa_isFestivo(int anno, int mese, int giorno) {
        assertThat(service.isFestivo(LocalDate.of(anno, mese, giorno))).isTrue();
    }

    @ParameterizedTest(name = "Festività fisse in anni diversi: {0}/{1}/{2}")
    @CsvSource({
            "2024, 1,  1",
            "2024, 8, 15",
            "2026,12, 25",
            "2026, 4, 25",
    })
    void festivitaFissa_anniDiversi_isFestivo(int anno, int mese, int giorno) {
        assertThat(service.isFestivo(LocalDate.of(anno, mese, giorno))).isTrue();
    }

    // ------------------------------------------------------------------ il giorno prima/dopo una festività non è festivo

    @Test
    void giornoDopoNatale_26dicembre_isFestivo() {
        // 26/12 è Santo Stefano → festivo
        assertThat(service.isFestivo(LocalDate.of(2025, 12, 26))).isTrue();
    }

    @Test
    void giornoDopoSantoStefano_nonFestivo() {
        // 27/12/2025 è sabato → non festivo per la regola del sabato, ma non è domenica né festività
        assertThat(service.isFestivo(LocalDate.of(2025, 12, 27))).isFalse();
    }

    // ------------------------------------------------------------------ Pasquetta

    @ParameterizedTest(name = "Pasquetta {0}")
    @CsvSource({
            // anno, mese, giorno della Pasquetta
            "2024, 4,  1",   // Pasquetta 2024
            "2025, 4, 21",   // Pasquetta 2025
            "2026, 4,  6",   // Pasquetta 2026
            "2027, 3, 29",   // Pasquetta 2027
            "2028, 4, 17",   // Pasquetta 2028
    })
    void pasquetta_isFestivo(int anno, int mese, int giorno) {
        assertThat(service.isFestivo(LocalDate.of(anno, mese, giorno))).isTrue();
    }

    @Test
    void pasqua_stessa_isFestivoPercheDomenica() {
        // Pasqua 2025 cade il 20 aprile (domenica) → festivo per la regola domenica
        assertThat(service.isFestivo(LocalDate.of(2025, 4, 20))).isTrue();
    }

    @Test
    void giornoNonPasquetta_stessaPeriodo_isNotFestivo() {
        // 22/04/2025 è martedì dopo Pasquetta → non festivo
        assertThat(service.isFestivo(LocalDate.of(2025, 4, 22))).isFalse();
    }

    @Test
    void giornoDopoCapodanno_isNotFestivo() {
        // 02/01/2025 è giovedì → non festivo
        assertThat(service.isFestivo(LocalDate.of(2025, 1, 2))).isFalse();
    }

    @Test
    void giornoDopoEpifania_isNotFestivo() {
        // 07/01/2025 è martedì → non festivo
        assertThat(service.isFestivo(LocalDate.of(2025, 1, 7))).isFalse();
    }

    @Test
    void venticinquaprile_domenica_isFestivo() {
        // 25/04/2021 è domenica → festivo sia per domenica che per festività fissa
        assertThat(service.isFestivo(LocalDate.of(2021, 4, 25))).isTrue();
    }

    @Test
    void primomaggio_domenica_isFestivo() {
        // 01/05/2022 è domenica
        assertThat(service.isFestivo(LocalDate.of(2022, 5, 1))).isTrue();
    }
}

