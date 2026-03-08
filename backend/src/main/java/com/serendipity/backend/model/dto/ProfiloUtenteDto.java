package com.serendipity.backend.model.dto;

public record ProfiloUtenteDto(
        String codiceFiscale,
        String nome,
        String cognome,
        String email,
        String ruolo) {
}
