package com.serendipity.backend.model.dto;

public record CreaUtenteDto
        (String codiceFiscale,
         String nome,
         String cognome,
         String email,
         String password,
         String ruolo) {
}
