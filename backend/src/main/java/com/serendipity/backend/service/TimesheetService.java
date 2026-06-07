package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetMapper;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.TotaleClienteDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import com.serendipity.backend.support.SystemClienti;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TimesheetRigaRepository rigaRepository;

    @Autowired
    private TimesheetMapper mapper;

    @Autowired
    private CalendarioFestivitaService calendarioFestivitaService;


    /**
     * Trova un timesheet per ID.
     *
     * @param id ID del timesheet da cercare
     * @return TimesheetDto se trovato
     * @throws EntityNotFoundException se il timesheet non esiste
     */
    public TimesheetDto findById(Long id) {
        return mapper.toDto(mustReadOwnedOrAdmin(id));
    }

    /**
     * Crea un nuovo timesheet.
     *
     * @param dto Dati del timesheet da creare
     * @return TimesheetDto creato
     * @throws AccessDeniedException           se l'utente non è autorizzato a creare un timesheet per l'utente specificato
     * @throws DataIntegrityViolationException se esiste già un timesheet per lo stesso utente, mese e anno
     */
    public TimesheetDto create(CreaTimesheetDto dto) {
        ensureSelfOrAdmin(dto.getUtenteId());

        if (timesheetRepository.existsByUtenteIdAndMeseAndAnno(dto.getUtenteId(), dto.getMese(), dto.getAnno())) {
            throw new DataIntegrityViolationException(
                    String.format("Esiste già un timesheet per l’utente selezionato nel periodo %02d/%d",
                            dto.getMese(), dto.getAnno())
            );
        }

        Timesheet entity = new Timesheet();
        entity.setAnno(dto.getAnno());
        entity.setMese(dto.getMese());
        entity.setDataCompilazione(null);
        entity.setUtente(
                utenteRepository.findById(dto.getUtenteId())
                        .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"))
        );

        return mapper.toDto(timesheetRepository.save(entity));
    }

    /**
     * Aggiorna un timesheet esistente.
     *
     * @param id  ID del timesheet da aggiornare
     * @param dto Dati del timesheet aggiornati
     * @return TimesheetDto aggiornato
     * @throws EntityNotFoundException         se il timesheet non esiste o l'utente non è autorizzato
     * @throws DataIntegrityViolationException se esiste già un timesheet per lo stesso utente, mese e anno
     * @throws IllegalStateException           se il timesheet è in uno stato che non consente modifiche
     */
    public TimesheetDto update(Long id, CreaTimesheetDto dto) {
        Timesheet entity = mustReadOwnedOrAdmin(id);

        boolean isAdmin = currentUserIsAdmin();

        if (!isAdmin && !entity.getUtente().getId().equals(dto.getUtenteId())) {
            throw new AccessDeniedException("Non puoi assegnare il timesheet a un altro utente");
        }

        if (entity.getStato() == TimesheetStato.CONFERMATO || entity.getStato() == TimesheetStato.CHIUSO) {
            throw new IllegalStateException("Timesheet non modificabile nello stato attuale");
        }

        if (!entity.getUtente().getId().equals(dto.getUtenteId())
                || entity.getMese() != dto.getMese()
                || entity.getAnno() != dto.getAnno()) {

            boolean exists = timesheetRepository.existsByUtenteIdAndMeseAndAnno(
                    dto.getUtenteId(),
                    dto.getMese(),
                    dto.getAnno()
            );

            if (exists) {
                throw new DataIntegrityViolationException(
                        String.format("Esiste già un timesheet per l’utente selezionato nel periodo %02d/%d",
                                dto.getMese(), dto.getAnno())
                );
            }
        }

        entity.setMese(dto.getMese());
        entity.setAnno(dto.getAnno());
        entity.setUtente(
                utenteRepository.findById(dto.getUtenteId())
                        .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"))
        );

        return mapper.toDto(timesheetRepository.save(entity));
    }

    /**
     * Elimina un timesheet.
     *
     * @param id ID del timesheet da eliminare
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    @Transactional
    public void delete(Long id) {
        Timesheet ts = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id));
        rigaRepository.deleteByTimesheetId(ts.getId());
        timesheetRepository.delete(ts);
        timesheetRepository.flush();
    }

    /**
     * Cerca timesheet filtrati per mese, anno e utente.
     * Gli utenti con ruolo ADMIN possono filtrare per qualsiasi utente,
     * mentre gli utenti con ruolo DIPENDENTE possono filtrare solo per se stessi (ignora il filtro utenteId).
     *
     * @param mese     Mese da filtrare (1-12), opzionale
     * @param anno     Anno da filtrare (es. 2023), opzionale
     * @param utenteId ID dell'utente da filtrare, opzionale (considerato solo se l'utente è ADMIN)
     * @return Lista di TimesheetDto che corrispondono ai filtri
     * @throws IllegalArgumentException se i valori di mese o anno non sono validi
     * @throws EntityNotFoundException  se non vengono trovati timesheet con i filtri selezionati
     */
    public List<TimesheetDto> search(Integer mese, Integer anno, Long utenteId) {
        if (mese != null && (mese < 1 || mese > 12)) {
            throw new IllegalArgumentException("Il mese deve essere compreso tra 1 e 12");
        }

        if (anno != null && anno < 2000) {
            throw new IllegalArgumentException("L'anno deve essere maggiore o uguale a 2000");
        }

        Long effectiveUtenteId = currentUserIsAdmin() ? utenteId : getCurrentUserId();

        List<Timesheet> results = timesheetRepository.searchFiltered(mese, anno, effectiveUtenteId);

        if (results.isEmpty()) {
            throw new EntityNotFoundException("Nessun timesheet trovato con i filtri selezionati");
        }

        return results.stream().map(mapper::toDto).toList();
    }

    /**
     * Restituisce la lista degli anni per cui esistono timesheet.
     * Gli utenti con ruolo ADMIN vedono gli anni di tutti i timesheet, mentre gli utenti con ruolo DIPENDENTE vedono solo gli anni dei propri timesheet.
     *
     * @return Lista di anni disponibili
     */
    public List<Integer> anniDisponibili() {
        if (currentUserIsAdmin()) {
            return timesheetRepository.findDistinctAnni();
        }

        return timesheetRepository.findDistinctAnniByUtenteId(getCurrentUserId());
    }

    /**
     * Restituisce la lista dei mesi per un dato anno per cui esistono timesheet.
     * Gli utenti con ruolo ADMIN vedono i mesi di tutti i timesheet, mentre gli utenti con ruolo DIPENDENTE vedono solo i mesi dei propri timesheet.
     *
     * @param anno Anno per cui recuperare i mesi
     * @return Lista di mesi disponibili per l'anno specificato
     */
    public List<Integer> mesiDisponibiliPerAnno(int anno) {
        if (currentUserIsAdmin()) {
            return timesheetRepository.findDistinctMesiByAnno(anno);
        }

        return timesheetRepository.findDistinctMesiByAnnoAndUtenteId(anno, getCurrentUserId());
    }

    /**
     * Restituisce tutti i timesheet filtrati in base al ruolo dell'utente.
     * Gli utenti con ruolo ADMIN vedono tutti i timesheet, mentre gli utenti con ruolo DIPENDENTE vedono solo i propri timesheet.
     *
     * @return Lista di TimesheetDto filtrati
     */
    public List<TimesheetDto> findAllFiltered() {
        boolean isAdmin = currentUserIsAdmin();

        if (isAdmin) {
            return timesheetRepository.findAll(Sort.by(
                            Sort.Direction.ASC, "anno", "mese"
                    ))
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        } else {
            Long currentUserId = getCurrentUserId();
            return timesheetRepository.findByUtenteIdOrderByAnnoAscMeseAsc(currentUserId)
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        }
    }

    /**
     * Conferma un timesheet.
     * Un timesheet può essere confermato solo se è nello stato APERTO.
     *
     * @param id ID del timesheet da confermare
     * @return TimesheetDto confermato
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     * @throws IllegalStateException   se il timesheet non è APERTO
     */
    public TimesheetDto conferma(Long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id);

        if (ts.getStato() != TimesheetStato.APERTO) {
            throw new IllegalStateException("Puoi confermare solo un timesheet APERTO");
        }

        ensureRequiredDaysCovered(ts);
        createMissingHolidayRows(ts);

        ts.setStato(TimesheetStato.CONFERMATO);
        return mapper.toDto(timesheetRepository.save(ts));
    }

    /**
     * Riapre un timesheet.
     * Un timesheet APERTO non può essere riaperto.
     * Un timesheet CONFERMATO può essere riaperto da chiunque (diventa APERTO).
     * Un timesheet CHIUSO può essere riaperto solo da ADMIN (diventa CONFERMATO).
     *
     * @param id ID del timesheet da riaprire
     * @return TimesheetDto riaperto
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     * @throws IllegalStateException   se il timesheet è APERTO o se un DIPENDENTE tenta di riaprire un timesheet CHIUSO
     */
    public TimesheetDto riapri(Long id) {
        Timesheet ts = mustReadOwnedOrAdmin(id);

        if (ts.getStato() == TimesheetStato.APERTO) {
            throw new IllegalStateException("Il timesheet è già nello stato APERTO e non può essere riaperto");
        }

        if (ts.getStato() == TimesheetStato.CONFERMATO) {
            ts.setStato(TimesheetStato.APERTO);
            return mapper.toDto(timesheetRepository.save(ts));
        }

        if (ts.getStato() == TimesheetStato.CHIUSO) {
            if (!currentUserIsAdmin()) {
                throw new AccessDeniedException("Solo ADMIN può riaprire un timesheet CHIUSO");
            }
            ts.setStato(TimesheetStato.CONFERMATO);
            ts.setDataCompilazione(null);
            return mapper.toDto(timesheetRepository.save(ts));
        }

        throw new IllegalStateException("Transizione di stato non valida");
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
     * Calcola i totali di orario e costo per un timesheet.
     * Può restituire i totali complessivi o suddivisi per cliente.
     *
     * @param timesheetId ID del timesheet
     * @param perCliente  Se true, restituisce i totali per cliente; altrimenti totali complessivi
     * @return TotaliDto con orario e costo arrotondati a 2 decimali, o lista di TotaleClienteDto
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    public Object totali(Long timesheetId, boolean perCliente) {

        mustReadOwnedOrAdmin(timesheetId);

        if (!perCliente) {
            TotaliDto raw = rigaRepository.sumTotaliByTimesheetId(timesheetId);
            // safe: la query ritorna sempre una riga
            double orarioRounded = BigDecimal.valueOf(raw.totaleOrario()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            double costoRounded = BigDecimal.valueOf(raw.totaleCosto()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            return new TotaliDto(orarioRounded, costoRounded);
        } else {
            return rigaRepository.sumTotaliPerCliente(timesheetId, SystemClienti.NON_LAVORATO).stream()
                    .map(p -> new TotaleClienteDto(
                            p.clienteId(),
                            p.clienteNome(),
                            BigDecimal.valueOf(p.orario()).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                            BigDecimal.valueOf(p.costo()).setScale(2, RoundingMode.HALF_UP).doubleValue()
                    ))
                    .toList();
        }
    }

    /* ------------------------- HELPERS ------------------------- */

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
                throw new EntityNotFoundException("Timesheet non trovato");
            }
        }
        return ts;
    }

    private void ensureRequiredDaysCovered(Timesheet ts) {
        YearMonth yearMonth = YearMonth.of(ts.getAnno(), ts.getMese());
        Set<LocalDate> compiledDates = rigaRepository.findDistinctDatesByTimesheetId(ts.getId()).stream()
                .collect(Collectors.toSet());

        List<LocalDate> missingDates = Stream.iterate(
                        yearMonth.atDay(1),
                        date -> !date.isAfter(yearMonth.atEndOfMonth()),
                        date -> date.plusDays(1)
                )
                .filter(date -> !calendarioFestivitaService.isFestivo(date))
                .filter(date -> !compiledDates.contains(date))
                .toList();

        if (!missingDates.isEmpty()) {
            throw new IllegalStateException(
                    "Non puoi confermare il timesheet: ci sono ancora giorni feriali del mese non compilati"
            );
        }
    }

    private void createMissingHolidayRows(Timesheet ts) {
        YearMonth yearMonth = YearMonth.of(ts.getAnno(), ts.getMese());
        Set<LocalDate> compiledDates = rigaRepository.findDistinctDatesByTimesheetId(ts.getId()).stream()
                .collect(Collectors.toSet());
        Cliente nonLavoratoCliente = clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO)
                .orElseThrow(() -> new EntityNotFoundException("Cliente di sistema NON LAVORATO non trovato"));

        List<TimesheetRiga> missingHolidayRows = Stream.iterate(
                        yearMonth.atDay(1),
                        date -> !date.isAfter(yearMonth.atEndOfMonth()),
                        date -> date.plusDays(1)
                )
                .filter(calendarioFestivitaService::isFestivo)
                .filter(date -> !compiledDates.contains(date))
                .map(date -> buildHolidayRow(ts, nonLavoratoCliente, date))
                .toList();

        if (!missingHolidayRows.isEmpty()) {
            rigaRepository.saveAll(missingHolidayRows);
        }
    }

    private TimesheetRiga buildHolidayRow(Timesheet ts, Cliente cliente, LocalDate date) {
        TimesheetRiga row = new TimesheetRiga();
        row.setTimesheet(ts);
        row.setCliente(cliente);
        row.setData(date);
        row.setOre(0);
        row.setMinuti(0);
        row.setOrario(0);
        row.setCostoOrario(0);
        return row;
    }

}
