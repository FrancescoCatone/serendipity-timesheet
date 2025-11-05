package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetMapper;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private TimesheetRigaRepository rigaRepository;

    @Autowired
    private TimesheetMapper mapper;


    /**
     * Trova un timesheet per ID.
     *
     * @param id ID del timesheet da cercare
     * @return TimesheetDto se trovato
     * @throws EntityNotFoundException se il timesheet non esiste
     */
    public TimesheetDto findById(Long id) {
        return mapper.toDto(timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id)));
    }

    /**
     * Crea un nuovo timesheet.
     *
     * @param dto Dati del timesheet da creare
     * @return TimesheetDto creato
     * @throws DataIntegrityViolationException se esiste già un timesheet per lo stesso utente, mese e anno
     */
    public TimesheetDto create(CreaTimesheetDto dto) {

        Long utenteId = dto.getUtenteId();
        int mese = dto.getMese();
        int anno = dto.getAnno();

        ensureSelfOrAdmin(utenteId);

        if (timesheetRepository.existsByUtenteIdAndMeseAndAnno(utenteId, mese, anno)) {
            throw new DataIntegrityViolationException(
                    String.format("Esiste già un timesheet per l'utente %d nel %02d/%d", utenteId, mese, anno)
            );
        }

        Timesheet entity = new Timesheet();
        entity.setAnno(dto.getAnno());
        entity.setMese(dto.getMese());
        entity.setDataCompilazione(null); // verrà impostata solo alla chiusura
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));

        return mapper.toDto(timesheetRepository.save(entity));
    }

    /**
     * Aggiorna un timesheet esistente.
     *
     * @param id  ID del timesheet da aggiornare
     * @param dto Dati aggiornati del timesheet
     * @return TimesheetDto aggiornato
     * @throws EntityNotFoundException         se il timesheet non esiste
     * @throws DataIntegrityViolationException se esiste già un timesheet per lo stesso utente, mese e anno
     */
    public TimesheetDto update(Long id, CreaTimesheetDto dto) {

        Timesheet entity = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id));

        Long newUtenteId = dto.getUtenteId();
        int newMese = dto.getMese();
        int newAnno = dto.getAnno();

        ensureSelfOrAdmin(newUtenteId);

        if (entity.getStato() == TimesheetStato.CHIUSO) {
            throw new AccessDeniedException("Timesheet CHIUSO: impossibile modificare");
        }
        if (entity.getStato() == TimesheetStato.CONFERMATO && !currentUserIsAdmin()) {
            throw new IllegalStateException("Timesheet CONFERMATO: riaprire (→ APERTO) prima di modificare");
        }

        // Se la combinazione cambia, verifica che non esista già su un altro record
        if (!entity.getUtente().getId().equals(newUtenteId)
                || entity.getMese() != newMese
                || entity.getAnno() != newAnno) {

            boolean exists = timesheetRepository.existsByUtenteIdAndMeseAndAnno(newUtenteId, newMese, newAnno);
            if (exists) {
                throw new DataIntegrityViolationException(
                        String.format("Esiste già un timesheet per l'utente %d nel %02d/%d", newUtenteId, newMese, newAnno)
                );
            }
        }

        entity.setMese(dto.getMese());
        entity.setAnno(dto.getAnno());
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));

        return mapper.toDto(timesheetRepository.save(entity));
    }

    /**
     * Elimina un timesheet.
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

    /**
     * Cerca timesheet in base a mese, anno e opzionalmente utenteId.
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

    /**
     * Restituisce gli anni distinti per cui esistono timesheet.
     *
     * @return Lista di anni disponibili
     */
    public List<Integer> anniDisponibili() {
        return timesheetRepository.findDistinctAnni();
    }

    /**
     * Restituisce i mesi distinti per un dato anno in cui esistono timesheet.
     *
     * @param anno Anno per cui cercare i mesi
     * @return Lista di mesi disponibili per l'anno specificato
     */
    public List<Integer> mesiDisponibiliPerAnno(int anno) {
        return timesheetRepository.findDistinctMesiByAnno(anno);
    }

    /**
     * Restituisce tutti i timesheet filtrati in base al ruolo dell'utente corrente.
     * Gli utenti con ruolo ADMIN vedono tutti i timesheet,
     * mentre gli utenti con ruolo DIPENDENTE vedono solo i propri timesheet.
     *
     * @return Lista di TimesheetDto filtrati
     */
    public List<TimesheetDto> findAllFiltered() {
        boolean isAdmin = currentUserIsAdmin();

        if (isAdmin) {
            return timesheetRepository.findAll()
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        } else {
            Long currentUserId = getCurrentUserId();
            return timesheetRepository.findByUtenteId(currentUserId)
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        }
    }

    /**
     * Conferma un timesheet.
     * Un timesheet può essere confermato solo se è nello stato APERTO e
     * se è completo (ha righe per tutti i giorni del mese).
     *
     * @param id ID del timesheet da confermare
     * @return TimesheetDto confermato
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     * @throws IllegalStateException   se il timesheet non è APERTO o non è completo
     */
    public TimesheetDto conferma(Long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id); // helper: carica e verifica proprietà/permessi
        if (ts.getStato() != TimesheetStato.APERTO) {
            throw new IllegalStateException("Puoi confermare solo un timesheet APERTO");
        }
        if (!isCompleto(ts.getId(), ts.getMese(), ts.getAnno())) {
            throw new IllegalStateException("Timesheet non completo: mancano righe per alcuni giorni");
        }
        ts.setStato(TimesheetStato.CONFERMATO);
        return mapper.toDto(timesheetRepository.save(ts));
    }

    /**
     * Riapre un timesheet.
     * Un timesheet CHIUSO può essere riaperto solo dagli utenti con ruolo ADMIN.
     * Un timesheet CONFERMATO può essere riaperto dal proprietario o da ADMIN.
     *
     * @param id ID del timesheet da riaprire
     * @return TimesheetDto riaperto
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     * @throws AccessDeniedException se un DIPENDENTE tenta di riaprire un timesheet CHIUSO
     */
    public TimesheetDto riapri(Long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id);
        if (ts.getStato() == TimesheetStato.CHIUSO && !currentUserIsAdmin()) {
            throw new AccessDeniedException("Solo ADMIN può riaprire un timesheet CHIUSO");
        }
        if (ts.getStato() == TimesheetStato.APERTO) {
            return mapper.toDto(ts);
        }
        ts.setStato(TimesheetStato.APERTO);
        return mapper.toDto(timesheetRepository.save(ts));
    }

    /**
     * Chiude un timesheet.
     * Un timesheet può essere chiuso solo se è nello stato CONFERMATO.
     *
     * @param id ID del timesheet da chiudere
     * @return TimesheetDto chiuso
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     * @throws IllegalStateException   se il timesheet non è CONFERMATO
     */
    public TimesheetDto chiudi(Long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id);
        if (ts.getStato() != TimesheetStato.CONFERMATO) {
            throw new IllegalStateException("Puoi chiudere solo un timesheet CONFERMATO");
        }
        ts.setStato(TimesheetStato.CHIUSO);
        ts.setDataCompilazione(LocalDate.now());
        return mapper.toDto(timesheetRepository.save(ts));
    }


    /**
     * Metodi di utilità per gestione sicurezza
     */
    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Recupera l'ID dell'utente attualmente autenticato.
     *
     * @return ID dell'utente corrente
     * @throws EntityNotFoundException se l'utente non viene trovato
     */
    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

    /**
     * Verifica se un timesheet è completo per un dato mese e anno.
     * Un timesheet è considerato completo se ha almeno una riga per ogni giorno del mese.
     *
     * @param timesheetId ID del timesheet da verificare
     * @param mese        Mese da verificare (1-12)
     * @param anno        Anno da verificare (es. 2023)
     * @return true se il timesheet è completo, false altrimenti
     */
    private boolean isCompleto(Long timesheetId, int mese, int anno) {
        List<LocalDate> date = rigaRepository.findDistinctDateByTimesheetId(timesheetId);
        YearMonth ym = YearMonth.of(anno, mese);
        int giorni = ym.lengthOfMonth();
        // ogni giorno del mese deve avere almeno una riga
        Set<LocalDate> set = new HashSet<>(date);
        for (int d = 1; d <= giorni; d++) {
            if (!set.contains(LocalDate.of(anno, mese, d))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifica che l'utente corrente sia ADMIN o corrisponda all'ID utente specificato.
     *
     * @param targetUserId ID dell'utente da verificare
     * @throws AccessDeniedException se l'utente non è autorizzato
     */
    private void ensureSelfOrAdmin(Long targetUserId) {
        if (currentUserIsAdmin()) return;
        Long me = getCurrentUserId();
        if (!me.equals(targetUserId)) {
            throw new AccessDeniedException("Operazione non consentita");
        }
    }

    /**
     * Carica un timesheet e verifica che l'utente corrente sia ADMIN o proprietario del timesheet.
     *
     * @param id ID del timesheet da caricare
     * @return Timesheet caricato
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    private Timesheet mustReadOwnedOrAdmin(Long id) {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));
        if (!currentUserIsAdmin()) {
            if (!ts.getUtente().getId().equals(getCurrentUserId())) {
                throw new EntityNotFoundException("Timesheet non trovato"); // anti-leak
            }
        }
        return ts;
    }


}
