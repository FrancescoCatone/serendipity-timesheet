package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetMapper;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private TimesheetMapper mapper;

    /** Restituisce la lista di tutti i timesheet.
     *
     * @return Lista di Timesheet come TimesheetDto
     */
    public List<TimesheetDto> findAll() {
        return timesheetRepository.findAll().stream().map(mapper::toDto).toList();
    }

    /** Trova un timesheet per ID.
     *
     * @param id ID del timesheet da cercare
     * @return TimesheetDto se trovato
     * @throws EntityNotFoundException se il timesheet non esiste
     */
    public TimesheetDto findById(Long id) {
        return mapper.toDto(timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id)));
    }

    /** Crea un nuovo timesheet.
     *
     * @param dto Dati del timesheet da creare
     * @return TimesheetDto creato
     */
    public TimesheetDto create(CreaTimesheetDto dto) {
        Timesheet entity = new Timesheet();
        entity.setAnno(dto.getAnno());
        entity.setMese(dto.getMese());
        entity.setDataCompilazione(null); // verrà impostata solo alla chiusura
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));
        return mapper.toDto(timesheetRepository.save(entity));
    }

    /** Aggiorna un timesheet esistente.
     *
     * @param id  ID del timesheet da aggiornare
     * @param dto Dati aggiornati del timesheet
     * @return TimesheetDto aggiornato
     * @throws EntityNotFoundException se il timesheet non esiste
     */
    public TimesheetDto update(Long id, CreaTimesheetDto dto) {
        Timesheet entity = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id));

        entity.setMese(dto.getMese());
        entity.setAnno(dto.getAnno());
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));

        return mapper.toDto(timesheetRepository.save(entity));
    }

    /** Elimina un timesheet.
     *
     * @param id ID del timesheet da eliminare
     * @throws EntityNotFoundException se il timesheet non esiste
     */
    public void delete(Long id) {
        if (!timesheetRepository.existsById(id)) {
            throw new EntityNotFoundException("Timesheet non trovato con ID: " + id);
        }
        timesheetRepository.deleteById(id);
    }

    /** Cerca timesheet in base a mese, anno e opzionalmente utenteId.
     * Gli utenti con ruolo ADMIN possono cercare per qualsiasi utenteId,
     * mentre gli utenti con ruolo DIPENDENTE possono vedere solo i propri timesheet.
     *
     * @param mese     Mese del timesheet (1-12)
     * @param anno     Anno del timesheet (es. 2023)
     * @param utenteId (opzionale) ID dell'utente per filtrare i timesheet (solo per ADMIN)
     * @return Lista di TimesheetDto che corrispondono ai criteri di ricerca
     * @throws IllegalArgumentException se mese o anno non sono validi
     */
    public List<TimesheetDto> search(Integer mese, Integer anno, Long utenteId) {
        if (mese == null || anno == null) {
            throw new IllegalArgumentException("Mese e anno sono obbligatori");
        }
        if (mese < 1 || mese > 12) {
            throw new IllegalArgumentException("Il mese deve essere compreso tra 1 e 12");
        }

        boolean isAdmin = currentUserIsAdmin();
        List<Timesheet> results;

        if (isAdmin) {
            // ADMIN: se specificato un utenteId, filtra per quello. Altrimenti tutti per mese/anno.
            if (utenteId != null) {
                results = timesheetRepository.findByUtenteIdAndMeseAndAnno(utenteId, mese, anno);
            } else {
                results = timesheetRepository.findByMeseAndAnno(mese, anno);
            }
        } else {
            // DIPENDENTE: ignora qualsiasi utenteId passato, usa l'utente corrente
            Long currentUserId = getCurrentUserId();
            results = timesheetRepository.findByUtenteIdAndMeseAndAnno(currentUserId, mese, anno);
        }

        return results.stream().map(mapper::toDto).toList();
    }

    /** Restituisce gli anni distinti per cui esistono timesheet.
     *
     * @return Lista di anni disponibili
     */
    public List<Integer> anniDisponibili() {
        return timesheetRepository.findDistinctAnni();
    }

    /** Restituisce i mesi distinti per un dato anno in cui esistono timesheet.
     *
     * @param anno Anno per cui cercare i mesi
     * @return Lista di mesi disponibili per l'anno specificato
     */
    public List<Integer> mesiDisponibiliPerAnno(int anno) {
        return timesheetRepository.findDistinctMesiByAnno(anno);
    }

    /** Chiude e invia un timesheet.
     * Imposta la data di compilazione al giorno corrente se non già chiuso.
     * Gli utenti con ruolo ADMIN possono chiudere/inviare qualsiasi timesheet,
     * mentre gli utenti con ruolo DIPENDENTE possono chiudere/inviare solo i propri timesheet.
     *
     * @param id ID del timesheet da chiudere/inviare
     * @return TimesheetDto aggiornato
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    public TimesheetDto chiudiInvia(Long id) {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id));

        // Autorizzazione: ADMIN ok; DIPENDENTE solo se proprietario
        boolean isAdmin = currentUserIsAdmin();
        if (!isAdmin) {
            Long currentUserId = getCurrentUserId();
            if (!ts.getUtente().getId().equals(currentUserId)) {
                // evita information leakage
                throw new EntityNotFoundException("Timesheet non trovato");
            }
        }

        if (ts.getDataCompilazione() != null) {
            // già chiuso → ritorna lo stato attuale
            return mapper.toDto(ts);
        }

        ts.setDataCompilazione(LocalDate.now());
        return mapper.toDto(timesheetRepository.save(ts));
    }

    /** Metodi di utilità per gestione sicurezza */
    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /** Recupera l'ID dell'utente attualmente autenticato.
     *
     * @return ID dell'utente corrente
     * @throws EntityNotFoundException se l'utente non viene trovato
     */
    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

}
