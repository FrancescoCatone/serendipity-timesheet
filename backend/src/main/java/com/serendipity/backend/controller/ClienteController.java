package com.serendipity.backend.controller;

import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clienti")
public class ClienteController {

    @Autowired
    private ClienteService service;

    /**
     * Restituisce la lista di tutti i clienti.
     *
     * @return Lista di Clienti come ClienteDto
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ResponseMessage> getAll() {
        List<ClienteDto> clienti = service.findAll();
        return ResponseEntity.ok(new ResponseMessage(200, "Lista clienti", clienti));
    }

    /**
     * Trova un cliente per ID.
     *
     * @param id ID del cliente da cercare
     * @return ClienteDto se trovato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage> getById(@PathVariable Long id) {
        ClienteDto dto = service.findById(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Cliente trovato", dto));
    }

    /**
     * Crea un nuovo cliente.
     *
     * @param dto Dati del cliente da creare
     * @return ClienteDto creato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseMessage> create(@RequestBody @Valid CreaClienteDto dto) {
        ClienteDto created = service.create(dto);
        return ResponseEntity.status(201).body(new ResponseMessage(201, "Cliente creato", created));
    }

    /**
     * Aggiorna un cliente esistente.
     *
     * @param id  ID del cliente da aggiornare
     * @param dto Dati del cliente aggiornati
     * @return ClienteDto aggiornato
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseMessage> update(@PathVariable Long id, @RequestBody @Valid CreaClienteDto dto) {
        ClienteDto updated = service.update(id, dto);
        return ResponseEntity.ok(new ResponseMessage(200, "Cliente aggiornato", updated));
    }

    /**
     * Elimina un cliente per ID.
     *
     * @param id ID del cliente da eliminare
     * @return Messaggio di conferma dell'eliminazione
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ResponseMessage(200, "Cliente eliminato"));
    }

    /**
     * Recupera i dipendenti e le ore totali lavorate per un cliente specifico in un mese e anno dati.
     *
     * @param clienteId ID del cliente
     * @param mese      Mese per il quale recuperare i dati
     * @param anno      Anno per il quale recuperare i dati
     * @return Lista di dipendenti con le ore totali lavorate
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{clienteId}/dipendenti")
    public ResponseEntity<ResponseMessage> getDipendentiPerClienteEMese(
            @PathVariable Long clienteId,
            @RequestParam int mese,
            @RequestParam int anno
    ) {
        List<Object[]> aggregati = service.getDipendentiConOreTotaliPerCliente(clienteId, mese, anno);

        List<Map<String, Object>> risultati = aggregati.stream().map(row -> {
            Map<String, Object> mappa = new HashMap<>();
            mappa.put("utenteId", row[0]);
            mappa.put("nome", row[1]);
            mappa.put("cognome", row[2]);
            mappa.put("oreTotali", row[3]);
            return mappa;
        }).toList();

        return ResponseEntity.ok(new ResponseMessage(200, "Dipendenti per cliente e mese", risultati));
    }

    /**
     * Recupera il totale delle ore lavorate per un cliente specifico in un mese e anno dati.
     *
     * @param clienteId ID del cliente
     * @param mese      Mese per il quale recuperare il totale delle ore
     * @param anno      Anno per il quale recuperare il totale delle ore
     * @return Totale delle ore lavorate
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{clienteId}/totale-ore")
    public ResponseEntity<ResponseMessage> getTotaleOreLavoratePerClienteEMese(
            @PathVariable Long clienteId,
            @RequestParam int mese,
            @RequestParam int anno
    ) {
        double totale = service.getTotaleOrePerCliente(clienteId, mese, anno);
        return ResponseEntity.ok(new ResponseMessage(200, "Totale ore lavorate", totale));
    }
}
