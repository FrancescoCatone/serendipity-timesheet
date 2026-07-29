package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.ProfiloUtenteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.dto.update.AggiornaPasswordDto;
import com.serendipity.backend.model.dto.update.AggiornaUtenteDto;
import com.serendipity.backend.service.UtenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired
    private UtenteService utenteService;

    /**
     * Ottiene un utente per ID.
     *
     * @param id ID dell'utente da cercare
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/id/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        ResponseMessage response = utenteService.findById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Ottiene tutti gli utenti.
     *
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<UtenteDto> utenti = utenteService.getAll();
        Map<String, Object> meta = new HashMap<>();
        meta.put("count", utenti.size());
        return ResponseEntity.ok(new ResponseMessage(200, "Lista utenti", utenti, meta));
    }

    /**
     * Crea un nuovo utente.
     *
     * @param dto Dati dell'utente da creare
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseMessage> creaUtente(@RequestBody @Valid CreaUtenteDto dto) {
        ResponseMessage response = utenteService.creaUtente(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Ottiene un utente per codice fiscale.
     *
     * @param codiceFiscale Codice fiscale dell'utente da cercare
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/codiceFiscale/{codiceFiscale}")
    public ResponseEntity<ResponseMessage> getByCodiceFiscale(@PathVariable String codiceFiscale) {
        ResponseMessage response = utenteService.findByCodiceFiscale(codiceFiscale);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Aggiorna un utente esistente.
     *
     * @param id  ID dell'utente da aggiornare
     * @param dto Dati aggiornati dell'utente
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> aggiornaUtente(@PathVariable Long id, @RequestBody @Valid AggiornaUtenteDto dto) {
        ResponseMessage response = utenteService.aggiornaUtente(id, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Elimina un utente.
     *
     * @param id ID dell'utente da eliminare
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> eliminaUtente(@PathVariable Long id) {
        ResponseMessage response = utenteService.eliminaUtente(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Aggiorna parzialmente un utente esistente.
     *
     * @param id      ID dell'utente da aggiornare
     * @param updates Mappa delle proprietà da aggiornare
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponseMessage> aggiornaParzialeUtente(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        ResponseMessage response = utenteService.aggiornaParziale(id, updates);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Cambia la password dell'utente corrente.
     *
     * @param dto Dati per l'aggiornamento della password
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me/password")
    public ResponseEntity<ResponseMessage> cambiaPassword(@Valid @RequestBody AggiornaPasswordDto dto) {
        ResponseMessage response = utenteService.cambiaPasswordUtenteCorrente(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Ottiene il profilo dell'utente corrente.
     *
     * @return ResponseEntity con il messaggio di risposta
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ResponseMessage> getProfiloUtenteCorrente() {
        ProfiloUtenteDto profilo = utenteService.getProfiloUtenteCorrente();
        return ResponseEntity.ok(new ResponseMessage(200, "Profilo utente corrente", profilo));
    }
}
