package com.serendipity.backend.model.dto;

public record UtenteDto(
        Long id,
        String codiceFiscale,
        String nome,
        String cognome,
        String email,
        String ruolo) {
}
