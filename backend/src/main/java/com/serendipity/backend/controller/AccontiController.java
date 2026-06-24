package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.AccontiSummaryDto;
import com.serendipity.backend.model.dto.AccontoMovimentoDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.create.CreaAccontoMovimentoDto;
import com.serendipity.backend.service.AccontiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/acconti")
@PreAuthorize("hasRole('ADMIN')")
public class AccontiController {

    private final AccontiService service;

    public AccontiController(AccontiService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<ResponseMessage> summary(@RequestParam Long utenteId,
                                                   @RequestParam int mese,
                                                   @RequestParam int anno) {
        AccontiSummaryDto payload = service.summary(utenteId, mese, anno);
        return ResponseEntity.ok(new ResponseMessage(200, "Riepilogo acconti generato", payload));
    }

    @PostMapping("/movimenti")
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaAccontoMovimentoDto dto) {
        AccontoMovimentoDto payload = service.create(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Movimento acconto creato", payload));
    }

    @DeleteMapping("/movimenti/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Movimento acconto eliminato"));
    }
}
