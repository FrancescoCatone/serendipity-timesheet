package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.report.ReportClienteDto;
import com.serendipity.backend.model.dto.report.ReportClienteGiornoDto;
import com.serendipity.backend.model.dto.report.ReportDipendenteDto;
import com.serendipity.backend.service.ReportExportService;
import com.serendipity.backend.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService service;
    private final ReportExportService exportService;

    public ReportController(ReportService service, ReportExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    /**
     * Genera il report aggregato per cliente.
     * Solo ADMIN può consultarlo.
     *
     * @param clienteId ID del cliente
     * @param mese      mese opzionale
     * @param anno      anno opzionale
     * @return report cliente con totali e dettaglio per dipendente
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cliente")
    public ResponseEntity<ResponseMessage> reportPerCliente(
            @RequestParam Long clienteId,
            @RequestParam(required = false) Integer mese,
            @RequestParam(required = false) Integer anno
    ) {
        ReportClienteDto report = service.reportPerCliente(clienteId, mese, anno);
        return ResponseEntity.ok(new ResponseMessage(200, "Report cliente generato", report));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cliente/export")
    public ResponseEntity<byte[]> exportReportCliente(
            @RequestParam Long clienteId,
            @RequestParam(required = false) Integer mese,
            @RequestParam int anno,
            @RequestParam(defaultValue = "false") boolean mostraCosto
    ) {
        var file = exportService.exportCliente(clienteId, mese, anno, mostraCosto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cliente/giorno")
    public ResponseEntity<ResponseMessage> reportPerClienteGiorno(
            @RequestParam Long clienteId,
            @RequestParam LocalDate data
    ) {
        ReportClienteGiornoDto report = service.reportPerClienteGiorno(clienteId, data);
        return ResponseEntity.ok(new ResponseMessage(200, "Report cliente giornaliero generato", report));
    }

    /**
     * Genera il report aggregato per dipendente.
     * ADMIN può consultare qualsiasi dipendente.
     * DIPENDENTE può consultare solo il proprio report.
     *
     * @param utenteId ID del dipendente
     * @param mese     mese opzionale
     * @param anno     anno opzionale
     * @return report dipendente con totali e dettaglio per cliente
     */
    @PreAuthorize("hasAnyRole('ADMIN','DIPENDENTE')")
    @GetMapping("/dipendente")
    public ResponseEntity<ResponseMessage> reportPerDipendente(
            @RequestParam Long utenteId,
            @RequestParam(required = false) Integer mese,
            @RequestParam(required = false) Integer anno
    ) {
        ReportDipendenteDto report = service.reportPerDipendente(utenteId, mese, anno);
        return ResponseEntity.ok(new ResponseMessage(200, "Report dipendente generato", report));
    }
}
