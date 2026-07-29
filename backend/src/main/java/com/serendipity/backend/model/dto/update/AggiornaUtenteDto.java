package com.serendipity.backend.model.dto.update;

import com.serendipity.backend.model.enums.Ruolo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AggiornaUtenteDto {

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    @Size(min = 16, max = 16, message = "Il codice fiscale deve contenere esattamente 16 caratteri")
    @Pattern(
            regexp = "^[A-Z0-9]{16}$",
            message = "Il codice fiscale deve contenere solo lettere maiuscole e numeri, 16 caratteri")
    private String codiceFiscale;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 50, message = "Il nome non può superare i 50 caratteri")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$", message = "Il nome può contenere solo lettere e spazi")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 50, message = "Il cognome non può superare i 50 caratteri")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$", message = "Il cognome può contenere solo lettere e spazi")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Email non valida")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Email non valida")
    private String email;

    @Size(min = 6, max = 50, message = "La password deve contenere almeno 6 caratteri")
    private String password;

    @NotNull(message = "Il ruolo è obbligatorio")
    private Ruolo ruolo;

    private Double pagaOraria;

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.isBlank()) {
            this.password = null;
            return;
        }
        this.password = password;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    public Double getPagaOraria() {
        return pagaOraria;
    }

    public void setPagaOraria(Double pagaOraria) {
        this.pagaOraria = pagaOraria;
    }
}
