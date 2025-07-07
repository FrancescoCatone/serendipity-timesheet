package com.serendipity.backend.model.entity;

import com.serendipity.backend.model.enums.Ruolo;
import jakarta.persistence.*;

@Entity
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codiceFiscale;

    private String nome;

    private String cognome;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Ruolo ruolo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ruolo getRuolo() {
        return ruolo;
    }

    public void setRuolo(Ruolo ruolo) {
        this.ruolo = ruolo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public Utente() {
    }

    public Utente(Long id, Ruolo ruolo, String password, String email, String cognome, String nome, String codiceFiscale) {
        this.id = id;
        this.ruolo = ruolo;
        this.password = password;
        this.email = email;
        this.cognome = cognome;
        this.nome = nome;
        this.codiceFiscale = codiceFiscale;
    }
}
