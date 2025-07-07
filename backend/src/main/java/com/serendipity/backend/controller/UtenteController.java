package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.CreaUtenteDto;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.service.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired
    private UtenteService utenteService;

    @PreAuthorize("hasAnyRole('ADMIN', 'DIPENDENTE')")
    @GetMapping
    public List<UtenteDto> getAll() {
        return utenteService.getAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public UtenteDto creaUtente(@RequestBody CreaUtenteDto creaUtenteDto) {
        return utenteService.creaUtente(creaUtenteDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{codiceFiscale}")
    public UtenteDto getByCodiceFiscale(@PathVariable String codiceFiscale) {
        return utenteService.findByCodiceFiscale(codiceFiscale);
    }
}
