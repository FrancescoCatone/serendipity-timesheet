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

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<TimesheetDto> result = service.findAll();
        return ResponseEntity.ok(new ResponseMessage(200, "Lista timesheet", result));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        TimesheetDto dto = service.findById(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet trovato", dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaTimesheetDto dto) {
        TimesheetDto created = service.create(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Timesheet creato", created));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> update(@PathVariable Long id, @RequestBody @Valid CreaTimesheetDto dto) {
        TimesheetDto updated = service.update(id, dto);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet aggiornato", updated));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Timesheet eliminato"));
    }
}

