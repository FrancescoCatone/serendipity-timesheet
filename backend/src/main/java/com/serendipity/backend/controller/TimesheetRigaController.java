package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.CreaTimesheetRigaDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<TimesheetRigaDto> list = service.findAll();
        return ResponseEntity.ok(new ResponseMessage(200, "Lista righe timesheet", list));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        TimesheetRigaDto dto = service.findById(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga trovata", dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaTimesheetRigaDto dto) {
        TimesheetRigaDto created = service.save(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Riga creata", created));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> update(@PathVariable Long id, @RequestBody @Valid CreaTimesheetRigaDto dto) {
        TimesheetRigaDto updated = service.update(id, dto);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga aggiornata", updated));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Riga eliminata"));
    }

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
}


