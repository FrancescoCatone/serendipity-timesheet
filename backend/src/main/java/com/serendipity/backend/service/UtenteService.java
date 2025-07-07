package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.CreaUtenteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UtenteMapper utenteMapper;

    /**
     * Trova un utente per ID.
     *
     * @param id l'ID dell'utente da cercare
     * @return un messaggio di risposta con lo stato e il DTO dell'utente trovato
     */
    public ResponseMessage findById(Long id) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato con id: " + id));

        UtenteDto dto = utenteMapper.toDto(utente);
        return new ResponseMessage(200, "Utente trovato", dto);
    }

    /**
     * Recupera tutti gli utenti.
     *
     * @return una lista di DTO contenenti i dati di tutti gli utenti
     */
    public List<UtenteDto> getAll() {
        return utenteRepository.findAll()
                .stream()
                .map(utenteMapper::toDto)
                .toList();
    }

    /**
     * Crea un nuovo utente.
     *
     * @param dto il DTO contenente i dati dell'utente da creare
     * @return un messaggio di risposta con lo stato e il messaggio di successo
     */
    public ResponseMessage creaUtente(CreaUtenteDto dto) {
        if (utenteRepository.existsByCodiceFiscale(dto.getCodiceFiscale())) {
            throw new DataIntegrityViolationException("Codice fiscale già esistente");
        }
        if (utenteRepository.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("Email già esistente");
        }

        if (dto.getRuolo() == Ruolo.ADMIN && !utenteCorrenteIsAdmin()) {
            throw new AccessDeniedException("Non puoi creare un utente con ruolo ADMIN.");
        }

        Utente nuovoUtente = new Utente();
        nuovoUtente.setCodiceFiscale(dto.getCodiceFiscale());
        nuovoUtente.setNome(dto.getNome());
        nuovoUtente.setCognome(dto.getCognome());
        nuovoUtente.setEmail(dto.getEmail().toLowerCase().trim());
        nuovoUtente.setPassword(passwordEncoder.encode(dto.getPassword()));
        nuovoUtente.setRuolo(dto.getRuolo());

        UtenteDto result = utenteMapper.toDto(utenteRepository.save(nuovoUtente));

        return new ResponseMessage(201, "Utente creato con successo", result);
    }

    /**
     * Trova un utente per codice fiscale.
     *
     * @param codiceFiscale il codice fiscale dell'utente da cercare
     * @return un messaggio di risposta con lo stato e il DTO dell'utente trovato
     */
    public ResponseMessage findByCodiceFiscale(String codiceFiscale) {
        Utente utente = utenteRepository.findByCodiceFiscale(codiceFiscale);
        if (utente == null) {
            throw new EntityNotFoundException("Utente non trovato con codice fiscale: " + codiceFiscale);
        }
        return new ResponseMessage(200, "Utente trovato", utenteMapper.toDto(utente));
    }

    /**
     * Aggiorna un utente esistente.
     *
     * @param id  l'ID dell'utente da aggiornare
     * @param dto il DTO contenente i nuovi dati dell'utente
     * @return un messaggio di risposta con lo stato e il messaggio di successo
     */
    public ResponseMessage aggiornaUtente(Long id, CreaUtenteDto dto) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (!utente.getCodiceFiscale().equals(dto.getCodiceFiscale())
                && utenteRepository.existsByCodiceFiscale(dto.getCodiceFiscale())) {
            throw new DataIntegrityViolationException("Codice fiscale già esistente");
        }

        if (!utente.getEmail().equalsIgnoreCase(dto.getEmail())
                && utenteRepository.existsByEmail(dto.getEmail())) {
            throw new DataIntegrityViolationException("Email già esistente");
        }

        utente.setNome(dto.getNome());
        utente.setCognome(dto.getCognome());
        utente.setCodiceFiscale(dto.getCodiceFiscale());
        utente.setEmail(dto.getEmail().toLowerCase());
        utente.setPassword(passwordEncoder.encode(dto.getPassword()));
        utente.setRuolo(dto.getRuolo());

        utenteRepository.save(utente);

        return new ResponseMessage(200, "Utente aggiornato con successo");
    }

    /**
     * Elimina un utente per ID.
     *
     * @param id l'ID dell'utente da eliminare
     * @return un messaggio di risposta con lo stato dell'operazione
     */
    public ResponseMessage eliminaUtente(Long id) {
        if (!utenteRepository.existsById(id)) {
            throw new EntityNotFoundException("Utente non trovato");
        }

        utenteRepository.deleteById(id);
        return new ResponseMessage(200, "Utente eliminato con successo");
    }

    private boolean utenteCorrenteIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

}
