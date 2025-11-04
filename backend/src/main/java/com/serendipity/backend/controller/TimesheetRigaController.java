package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
import com.serendipity.backend.service.TimesheetRigaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timesheet-righe")
public class TimesheetRigaController {

    @Autowired
    private TimesheetRigaService service;

    /**
     * Recupera tutte le righe del timesheet.
     *
     * @return una lista di DTO contenenti i dati di tutte le righe del timesheet
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<TimesheetRigaDto> list = service.findAll();
        return ResponseEntity.ok(new ResponseMessage(200, "Lista righe timesheet", list));
    }

    /**
     * Trova una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da cercare
     * @return TimesheetRigaDto se trovato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        TimesheetRigaDto dto = service.findById(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga trovata", dto));
    }

    /**
     * Crea una nuova riga del timesheet.
     *
     * @param dto Dati della riga del timesheet da creare
     * @return TimesheetRigaDto creato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaTimesheetRigaDto dto) {
        TimesheetRigaDto created = service.save(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Riga creata", created));
    }

    /**
     * Aggiorna una riga del timesheet esistente.
     *
     * @param id  ID della riga del timesheet da aggiornare
     * @param dto Dati aggiornati della riga del timesheet
     * @return TimesheetRigaDto aggiornato
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> update(@PathVariable Long id, @RequestBody @Valid CreaTimesheetRigaDto dto) {
        TimesheetRigaDto updated = service.update(id, dto);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga aggiornata", updated));
    }

    /**
     * Elimina una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da eliminare
     * @return messaggio di conferma dell'eliminazione
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga eliminata"));
    }

    /**
     * Filtra le righe del timesheet in base a parametri opzionali.
     *
     * @param clienteId (opzionale) ID del cliente per filtrare
     * @param utenteId  (opzionale) ID dell'utente per filtrare
     * @param data      (opzionale) Data in formato "yyyy-MM-dd" per filtrare
     * @return una lista di DTO contenenti i dati delle righe del timesheet che corrispondono ai criteri di filtro
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping("/filter")
    public ResponseEntity<ResponseMessage> filter(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long utenteId,
            @RequestParam(required = false) String data // formato "yyyy-MM-dd"
    ) {
        List<TimesheetRigaDto> risultati = service.filtra(clienteId, utenteId, data);
        return ResponseEntity.ok(new ResponseMessage(200, "Filtrati", risultati));
    }

    /**
     * Recupera tutte le righe associate a uno specifico timesheet.
     *
     * @param timesheetId ID del timesheet di cui recuperare le righe
     * @return una lista di DTO contenenti i dati delle righe del timesheet specificato
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @GetMapping("/by-timesheet/{timesheetId}")
    public ResponseEntity<ResponseMessage> getByTimesheet(@PathVariable Long timesheetId) {
        List<TimesheetRigaDto> righe = service.findByTimesheetId(timesheetId);
        return ResponseEntity.ok(new ResponseMessage(200, "Righe del timesheet", righe));
    }

}


