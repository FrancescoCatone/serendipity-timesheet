package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.CreaUtenteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.service.UtenteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired
    private UtenteService utenteService;

    /**
     * Recupera un utente per ID.
     *
     * @param id l'ID dell'utente da cercare
     * @return un messaggio di risposta con lo stato e il DTO dell'utente trovato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/id/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        ResponseMessage response = utenteService.findById(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }


    /**
     * Recupera tutti gli utenti.
     *
     * @return una lista di DTO contenenti i dati di tutti gli utenti
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<UtenteDto> utenti = utenteService.getAll();
        ResponseMessage response = new ResponseMessage(200, "Lista utenti", utenti);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea un nuovo utente.
     *
     * @param dto i dati dell'utente da creare
     * @return un messaggio di risposta con lo stato e i dati dell'utente creato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseMessage> creaUtente(@RequestBody @Valid CreaUtenteDto dto) {
        ResponseMessage response = utenteService.creaUtente(dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Recupera un utente per codice fiscale.
     *
     * @param codiceFiscale il codice fiscale dell'utente da cercare
     * @return un messaggio di risposta con lo stato e il DTO dell'utente trovato
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
     * @param id  l'ID dell'utente da aggiornare
     * @param dto i nuovi dati dell'utente
     * @return un messaggio di risposta con lo stato e i dati dell'utente aggiornato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> aggiornaUtente(@PathVariable Long id, @RequestBody @Valid CreaUtenteDto dto) {
        ResponseMessage response = utenteService.aggiornaUtente(id, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    /**
     * Elimina un utente per ID.
     *
     * @param id l'ID dell'utente da eliminare
     * @return un messaggio di risposta con lo stato dell'operazione
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> eliminaUtente(@PathVariable Long id) {
        ResponseMessage response = utenteService.eliminaUtente(id);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
