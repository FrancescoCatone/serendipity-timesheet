package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.service.TimesheetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
public class TimesheetController {

    @Autowired
    private TimesheetService service;

    /**
     * Recupera tutti i timesheet.
     *
     * @return una lista di DTO contenenti i dati di tutti i timesheet
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<TimesheetDto> result = service.findAll();
        return ResponseEntity.ok(new ResponseMessage(200, "Lista timesheet", result));
    }

    /**
     * Trova un timesheet per ID.
     *
     * @param id ID del timesheet da cercare
     * @return TimesheetDto se trovato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        TimesheetDto dto = service.findById(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet trovato", dto));
    }

    /**
     * Crea un nuovo timesheet.
     *
     * @param dto Dati del timesheet da creare
     * @return TimesheetDto creato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaTimesheetDto dto) {
        TimesheetDto created = service.create(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Timesheet creato", created));
    }

    /**
     * Aggiorna un timesheet esistente.
     *
     * @param id  ID del timesheet da aggiornare
     * @param dto Dati aggiornati del timesheet
     * @return TimesheetDto aggiornato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> update(@PathVariable Long id, @RequestBody @Valid CreaTimesheetDto dto) {
        TimesheetDto updated = service.update(id, dto);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet aggiornato", updated));
    }

    /**
     * Elimina un timesheet.
     *
     * @param id ID del timesheet da eliminare
     * @return messaggio di conferma dell'eliminazione
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet eliminato"));
    }

    /**
     * Cerca timesheet per mese, anno e opzionalmente utente (se ADMIN).
     *
     * @param mese     Mese del timesheet
     * @param anno     Anno del timesheet
     * @param utenteId (opzionale) ID dell'utente (usato solo se ADMIN)
     * @return Lista di TimesheetDto che corrispondono ai criteri di ricerca
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @GetMapping("/search")
    public ResponseEntity<ResponseMessage> search(
            @RequestParam Integer mese,
            @RequestParam Integer anno,
            @RequestParam(required = false) Long utenteId // usato solo se ADMIN
    ) {
        List<TimesheetDto> result = service.search(mese, anno, utenteId);
        return ResponseEntity.ok(new ResponseMessage(200, "Risultati ricerca timesheet", result));
    }

    /**
     * Recupera gli anni per cui esistono timesheet.
     *
     * @return Lista di anni disponibili
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @GetMapping("/anni")
    public ResponseEntity<ResponseMessage> anni() {
        return ResponseEntity.ok(new ResponseMessage(200, "Anni disponibili", service.anniDisponibili()));
    }

    /**
     * Recupera i mesi per cui esistono timesheet in un dato anno.
     *
     * @param anno Anno per cui recuperare i mesi
     * @return Lista di mesi disponibili per l'anno specificato
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @GetMapping("/mesi")
    public ResponseEntity<ResponseMessage> mesi(@RequestParam int anno) {
        return ResponseEntity.ok(new ResponseMessage(200, "Mesi disponibili per anno", service.mesiDisponibiliPerAnno(anno)));
    }

    /**
     * Chiude un timesheet (lo rende non più modificabile).
     *
     * @param id ID del timesheet da chiudere
     * @return TimesheetDto chiuso
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @PutMapping("/{id}/chiudi")
    public ResponseEntity<ResponseMessage> chiudi(@PathVariable Long id) {
        TimesheetDto dto = service.chiudiInvia(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet chiuso", dto));
    }

}

