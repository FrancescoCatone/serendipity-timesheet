package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.ProfiloUtenteDto;
import com.serendipity.backend.model.dto.ResponseMessage;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.dto.update.AggiornaPasswordDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    /**
     * Aggiorna parzialmente un utente esistente.
     *
     * @param id      l'ID dell'utente da aggiornare
     * @param updates una mappa contenente i campi da aggiornare e i loro nuovi valori
     * @return un messaggio di risposta con lo stato e il messaggio di successo
     */
    @Transactional
    public ResponseMessage aggiornaParziale(Long id, Map<String, Object> updates) {
        Utente u = utenteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        // nome
        if (updates.containsKey("nome")) {
            String v = (String) updates.get("nome");
            if (v == null || v.isBlank()) throw new IllegalArgumentException("Nome non valido");
            u.setNome(v);
        }
        // cognome
        if (updates.containsKey("cognome")) {
            String v = (String) updates.get("cognome");
            if (v == null || v.isBlank()) throw new IllegalArgumentException("Cognome non valido");
            u.setCognome(v);
        }
        // email (unicità + lowercase)
        if (updates.containsKey("email")) {
            String v = ((String) updates.get("email")).toLowerCase();
            if (!u.getEmail().equalsIgnoreCase(v) && utenteRepository.existsByEmail(v)) {
                throw new DataIntegrityViolationException("Email già esistente");
            }
            u.setEmail(v);
        }
        // codiceFiscale (unicità)
        if (updates.containsKey("codiceFiscale")) {
            String v = (String) updates.get("codiceFiscale");
            if (!u.getCodiceFiscale().equals(v) && utenteRepository.existsByCodiceFiscale(v)) {
                throw new DataIntegrityViolationException("Codice fiscale già esistente");
            }
            u.setCodiceFiscale(v);
        }
        // ruolo
        if (updates.containsKey("ruolo")) {
            String v = (String) updates.get("ruolo");
            u.setRuolo(Ruolo.valueOf(v)); // lancia se non valido
        }
        // password (opzionale)
        if (updates.containsKey("password")) {
            String raw = (String) updates.get("password");
            if (raw != null && !raw.isBlank()) {
                u.setPassword(passwordEncoder.encode(raw));
            }
        }

        utenteRepository.save(u);
        return new ResponseMessage(200, "Utente aggiornato", null, null);
    }

    /**
     * Cambia la password dell'utente corrente.
     *
     * @param dto il DTO contenente la vecchia e la nuova password
     * @return un messaggio di risposta con lo stato e il messaggio di successo
     */
    @Transactional
    public ResponseMessage cambiaPasswordUtenteCorrente(AggiornaPasswordDto dto) {
        String email = getCurrentUsername(); // preleviamo l'email dal Principal
        Utente u = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        if (!passwordEncoder.matches(dto.getOldPassword(), u.getPassword())) {
            throw new BadCredentialsException("Password attuale errata");
        }
        u.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        utenteRepository.save(u);
        return new ResponseMessage(200, "Password aggiornata");
    }

    /**
     * Ottiene il profilo dell'utente attualmente autenticato.
     *
     * @return un DTO contenente i dati del profilo dell'utente corrente
     */
    public ProfiloUtenteDto getProfiloUtenteCorrente() {
        String email = getCurrentUsername();

        Utente utente = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        return new ProfiloUtenteDto(
                utente.getCodiceFiscale(),
                utente.getNome(),
                utente.getCognome(),
                utente.getEmail(),
                utente.getRuolo().name()
        );
    }

    /**
     * Verifica se l'utente corrente ha il ruolo ADMIN.
     *
     * @return true se l'utente corrente è un ADMIN, false altrimenti
     */
    private boolean utenteCorrenteIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    /**
     * Ottiene il nome utente (email) dell'utente attualmente autenticato.
     *
     * @return il nome utente dell'utente corrente
     * @throws IllegalStateException se l'utente non è autenticato
     */
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Utente non autenticato");
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return String.valueOf(principal);
    }

}
