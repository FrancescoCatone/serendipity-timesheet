package com.serendipity.backend.controller;

import com.serendipity.backend.service.TimesheetExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timesheets")
public class TimesheetExportController {

    private final TimesheetExportService exportService;

    public TimesheetExportController(TimesheetExportService exportService) {
        this.exportService = exportService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable long id) {
        var file = exportService.export(id); // ritorna contenuto + filename
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }
}
