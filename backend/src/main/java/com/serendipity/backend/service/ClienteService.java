package com.serendipity.backend.service;

import com.serendipity.backend.mapper.ClienteMapper;
import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    private ClienteMapper clienteMapper = ClienteMapper.INSTANCE;

    @Autowired
    private AdminNotificationService notificationService;

    /**
     * Restituisce la lista di tutti i clienti.
     *
     * @return Lista di Clienti come ClienteDto
     */
    public List<ClienteDto> findAll() {
        return clienteRepository.findAll().stream()
                .map(clienteMapper::toDto)
                .toList();
    }

    /**
     * Trova un cliente per ID.
     *
     * @param id ID del cliente da cercare
     * @return ClienteDto se trovato
     * @throws EntityNotFoundException se il cliente non esiste
     */
    public ClienteDto findById(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato con ID: " + id));
        return clienteMapper.toDto(cliente);
    }

    /**
     * Crea un nuovo cliente.
     *
     * @param dto Dati del cliente da creare
     * @return ClienteDto creato
     * @throws DataIntegrityViolationException se esiste già un cliente con lo stesso nome
     */
    public ClienteDto create(CreaClienteDto dto) {
        String nomeNorm = dto.getNome().trim();
        if (clienteRepository.existsByNomeIgnoreCase(nomeNorm)) {
            throw new DataIntegrityViolationException(
                    "Esiste già un cliente con nome: " + nomeNorm
            );
        }
        Cliente cliente = clienteMapper.fromCreateDto(dto);
        cliente.setNome(nomeNorm);
        Cliente saved = clienteRepository.save(cliente);
        notificationService.notifyClienteCreated(getCurrentUsernameSafe(), saved);
        return clienteMapper.toDto(saved);
    }

    /**
     * Aggiorna un cliente esistente.
     *
     * @param id  ID del cliente da aggiornare
     * @param dto Dati aggiornati del cliente
     * @return ClienteDto aggiornato
     * @throws EntityNotFoundException         se il cliente non esiste
     * @throws DataIntegrityViolationException se esiste già un cliente con lo stesso nome
     */
    public ClienteDto update(Long id, CreaClienteDto dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato con ID: " + id));
        Cliente before = snapshotCliente(cliente);

        String nuovoNome = dto.getNome().trim();

        // se il nome cambia, verifica unicità (case-insensitive)
        if (!cliente.getNome().equalsIgnoreCase(nuovoNome)
                && clienteRepository.existsByNomeIgnoreCase(nuovoNome)) {
            throw new DataIntegrityViolationException(
                    "Esiste già un cliente con nome: " + nuovoNome
            );
        }

        cliente.setNome(nuovoNome);
        cliente.setTariffaOraria(dto.getTariffaOraria());
        Cliente saved = clienteRepository.save(cliente);
        notificationService.notifyClienteUpdated(getCurrentUsernameSafe(), before, snapshotCliente(saved));
        return clienteMapper.toDto(saved);
    }

    /**
     * Elimina un cliente per ID.
     *
     * @param id ID del cliente da eliminare
     * @throws EntityNotFoundException se il cliente non esiste
     */
    public void delete(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato con ID: " + id));
        Cliente snapshot = snapshotCliente(cliente);
        clienteRepository.deleteById(id);
        notificationService.notifyClienteDeleted(getCurrentUsernameSafe(), snapshot);
    }

    /**
     * Restituisce la lista di dipendenti con le ore totali lavorate per un cliente specifico in un mese e anno.
     *
     * @param clienteId ID del cliente
     * @param mese      Mese di riferimento
     * @param anno      Anno di riferimento
     * @return Lista di oggetti contenenti ID utente, nome, cognome e ore totali
     */
    public List<Object[]> getDipendentiConOreTotaliPerCliente(Long clienteId, int mese, int anno) {
        return clienteRepository.findDipendentiConOreTotali(clienteId, mese, anno);
    }

    /**
     * Calcola il totale delle ore lavorate per un cliente specifico in un mese e anno.
     *
     * @param clienteId ID del cliente
     * @param mese      Mese di riferimento
     * @param anno      Anno di riferimento
     * @return Totale ore lavorate
     */
    public double getTotaleOrePerCliente(Long clienteId, int mese, int anno) {
        return clienteRepository.sommaTotaleOrePerCliente(clienteId, mese, anno);
    }

    /**
     * Aggiorna parzialmente un cliente.
     *
     * @param id      ID del cliente da aggiornare
     * @param updates Mappa di campi e valori da aggiornare
     * @return Messaggio di risposta
     * @throws EntityNotFoundException         se il cliente non esiste
     * @throws DataIntegrityViolationException se esiste già un cliente con lo stesso nome
     * @throws IllegalArgumentException        se i dati forniti non sono validi
     */
    @Transactional
    public ResponseMessage aggiornaParziale(Long id, Map<String, Object> updates) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));
        Cliente before = snapshotCliente(c);

        // nome (trim, not blank, max 100, unicità case-insensitive)
        if (updates.containsKey("nome")) {
            String v = ((String) updates.get("nome")).trim();
            if (v.isBlank()) throw new IllegalArgumentException("Il nome del cliente è obbligatorio");
            if (v.length() > 100)
                throw new IllegalArgumentException("Il nome del cliente non può superare 100 caratteri");
            if (!c.getNome().equalsIgnoreCase(v) && clienteRepository.existsByNomeIgnoreCase(v)) {
                throw new DataIntegrityViolationException("Esiste già un cliente con nome: " + v);
            }
            c.setNome(v);
        }

        // tariffaOraria (> 0)
        if (updates.containsKey("tariffaOraria")) {
            Double t = null;
            Object raw = updates.get("tariffaOraria");
            if (raw instanceof Number n) t = n.doubleValue();
            else if (raw instanceof String s && !s.isBlank()) t = Double.valueOf(s);
            if (t == null || t <= 0) throw new IllegalArgumentException("La tariffa oraria deve essere maggiore di 0");
            c.setTariffaOraria(t);
        }

        clienteRepository.save(c);
        notificationService.notifyClienteUpdated(getCurrentUsernameSafe(), before, snapshotCliente(c));
        return new ResponseMessage(200, "Cliente aggiornato", null, null);
    }

    private String getCurrentUsernameSafe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "sistema";
        }
        return authentication.getName();
    }

    private Cliente snapshotCliente(Cliente source) {
        if (source == null) {
            return null;
        }

        Cliente copy = new Cliente();
        copy.setId(source.getId());
        copy.setNome(source.getNome());
        copy.setTariffaOraria(source.getTariffaOraria());
        return copy;
    }

}
