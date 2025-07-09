package com.serendipity.backend.service;

import com.serendipity.backend.mapper.ClienteMapper;
import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteMapper clienteMapper;

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
     */
    public ClienteDto create(CreaClienteDto dto) {
        Cliente cliente = clienteMapper.fromCreateDto(dto);
        return clienteMapper.toDto(clienteRepository.save(cliente));
    }

    /**
     * Aggiorna un cliente esistente.
     *
     * @param id  ID del cliente da aggiornare
     * @param dto Nuovi dati del cliente
     * @return ClienteDto aggiornato
     * @throws EntityNotFoundException se il cliente non esiste
     */
    public ClienteDto update(Long id, CreaClienteDto dto) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato con ID: " + id));

        cliente.setNome(dto.getNome());
        cliente.setTariffaOraria(dto.getTariffaOraria());

        return clienteMapper.toDto(clienteRepository.save(cliente));
    }

    /**
     * Elimina un cliente per ID.
     *
     * @param id ID del cliente da eliminare
     * @throws EntityNotFoundException se il cliente non esiste
     */
    public void delete(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new EntityNotFoundException("Cliente non trovato con ID: " + id);
        }
        clienteRepository.deleteById(id);
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
}
