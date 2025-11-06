package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetRigaMapper;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TimesheetRigaService {

    @Autowired
    private TimesheetRigaRepository rigaRepository;

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TimesheetRigaMapper mapper;

    @Autowired
    private UtenteRepository utenteRepository;

    /**
     * Recupera tutte le righe del timesheet.
     *
     * @return una lista di DTO contenenti i dati di tutte le righe del timesheet
     */
    public List<TimesheetRigaDto> findAll() {
        if (currentUserIsAdmin()) {
            return rigaRepository.findAll().stream().map(mapper::toDto).toList();
        }
        Long me = getCurrentUserId();
        return rigaRepository.findAll().stream()
                .filter(r -> r.getTimesheet() != null && r.getTimesheet().getUtente().getId().equals(me))
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Trova una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da cercare
     * @return TimesheetRigaDto se trovato
     * @throws EntityNotFoundException se la riga non esiste o l'utente non è autorizzato
     */
    public TimesheetRigaDto findById(Long id) {
        TimesheetRiga r = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));
        ensureOwnedOrAdmin(r.getTimesheet());
        return mapper.toDto(r);
    }

    /**
     * Crea una nuova riga del timesheet.
     *
     * @param dto Dati della riga del timesheet da creare
     * @return TimesheetRigaDto creato
     * @throws EntityNotFoundException se il timesheet o il cliente non esistono
     * @throws AccessDeniedException   se l'utente non ha i permessi necessari
     */
    public TimesheetRigaDto save(CreaTimesheetRigaDto dto) {
        Timesheet ts = timesheetRepository.findById(dto.getTimesheetId())
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));

        // permessi sul proprietario del TS
        ensureSelfOrAdmin(ts.getUtente().getId());
        // stato del TS
        ensureTimesheetIsEditable(ts);

        TimesheetRiga entity = new TimesheetRiga();
        apply(dto, entity, ts);
        return mapper.toDto(rigaRepository.save(entity));
    }

    /**
     * Aggiorna una riga del timesheet esistente.
     *
     * @param id  ID della riga del timesheet da aggiornare
     * @param dto Dati aggiornati della riga del timesheet
     * @return TimesheetRigaDto aggiornato
     * @throws EntityNotFoundException se la riga o il timesheet non esistono
     * @throws AccessDeniedException   se l'utente non ha i permessi necessari o il timesheet non è modificabile
     */
    public TimesheetRigaDto update(Long id, CreaTimesheetRigaDto dto) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));

        Timesheet currentTs = existing.getTimesheet();
        ensureOwnedOrAdmin(currentTs);
        ensureTimesheetIsEditable(currentTs);

        if (!currentTs.getId().equals(dto.getTimesheetId())) {
            throw new AccessDeniedException("Non puoi cambiare il timesheet di appartenenza della riga");
        }

        apply(dto, existing, currentTs);
        return mapper.toDto(rigaRepository.save(existing));
    }

    /**
     * Elimina una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da eliminare
     * @throws EntityNotFoundException se la riga non esiste o l'utente non è autorizzato
     * @throws AccessDeniedException   se il timesheet è in uno stato non modificabile
     */
    public void delete(Long id) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));
        Timesheet ts = existing.getTimesheet();
        ensureOwnedOrAdmin(ts);
        ensureTimesheetIsEditable(ts);
        rigaRepository.deleteById(id);
    }

    /**
     * Filtra le righe del timesheet in base a cliente, utente e data, con controlli di autorizzazione.
     *
     * @param clienteId ID del cliente da filtrare (opzionale)
     * @param utenteId  ID dell'utente da filtrare (opzionale, solo per ADMIN)
     * @param dataStr   Data da filtrare in formato ISO (opzionale)
     * @return Lista di TimesheetRigaDto che soddisfano i criteri di filtro
     */
    public List<TimesheetRigaDto> filtra(Long clienteId, Long utenteId, String dataStr) {
        boolean admin = currentUserIsAdmin();
        Long me = admin ? null : getCurrentUserId();

        return rigaRepository.findAll().stream()
                .filter(r -> clienteId == null || r.getCliente().getId().equals(clienteId))
                .filter(r -> {
                    Long owner = r.getTimesheet().getUtente().getId();
                    return admin ? (utenteId == null || owner.equals(utenteId)) : owner.equals(me);
                })
                .filter(r -> dataStr == null || r.getData().equals(LocalDate.parse(dataStr)))
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Recupera tutte le righe associate a un dato timesheet, con controlli di autorizzazione.
     *
     * @param timesheetId ID del timesheet di cui recuperare le righe
     * @return Lista di TimesheetRigaDto associati al timesheet specificato
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    public List<TimesheetRigaDto> findByTimesheetId(Long timesheetId) {
        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + timesheetId));

        ensureOwnedOrAdmin(ts); // ADMIN ok; DIP solo proprietario

        return rigaRepository.findByTimesheetId(timesheetId).stream()
                .map(mapper::toDto)
                .toList();
    }

    /* ------------------------- HELPERS ------------------------- */

    /**
     * Applica i dati dal DTO all'entità TimesheetRiga, eseguendo le validazioni e i calcoli necessari.
     *
     * @param dto    il DTO contenente i dati da applicare
     * @param entity l'entità TimesheetRiga da aggiornare
     * @param ts     il Timesheet associato alla riga
     */
    private void apply(CreaTimesheetRigaDto dto, TimesheetRiga entity, Timesheet ts) {

        // 1) vincolo mese/anno coerente col TS
        ensureDataMatchesTimesheet(dto.getData(), ts);

        // 2) carico cliente
        var cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));

        // 3) validazioni base
        if (dto.getOre() < 0) {
            throw new IllegalArgumentException("Le ore non possono essere negative");
        }
        if (dto.getMinuti() < 0 || dto.getMinuti() > 59) {
            throw new IllegalArgumentException("I minuti devono essere compresi tra 0 e 59");
        }

        // 4) calcoli
        double orarioCalcolato = calcOrario(dto.getOre(), dto.getMinuti()); // esempio: 1h20m -> 1.33
        double costoCalcolato = calcCosto(orarioCalcolato, cliente.getTariffaOraria()); // 1.33 * 7 -> 9.31

        // 5) set campi
        entity.setTimesheet(ts);
        entity.setCliente(cliente);
        entity.setData(dto.getData());
        entity.setOre(dto.getOre());
        entity.setMinuti(dto.getMinuti());
        entity.setOrario(orarioCalcolato);
        entity.setCostoOrario(costoCalcolato);

    }

    /**
     * Vietiamo modifiche se TS è CHIUSO. Se CONFERMATO → solo ADMIN può modificare.
     */
    private void ensureTimesheetIsEditable(Timesheet ts) {
        TimesheetStato stato = ts.getStato();
        if (stato == TimesheetStato.CHIUSO) {
            throw new AccessDeniedException("Timesheet CHIUSO: non modificabile");
        }
        if (stato == TimesheetStato.CONFERMATO && !currentUserIsAdmin()) {
            throw new IllegalStateException("Timesheet CONFERMATO: riaprire (→ APERTO) prima di modificare");
        }
    }

    /**
     * Controlla che l’utente corrente sia ADMIN o sia l’owner del timesheet.
     */
    private void ensureOwnedOrAdmin(Timesheet ts) {
        if (currentUserIsAdmin()) return;
        Long me = getCurrentUserId();
        if (!ts.getUtente().getId().equals(me)) {
            // evitiamo information leakage
            throw new EntityNotFoundException("Riga non trovata");
        }
    }

    /**
     * Controlla che l’utente corrente sia ADMIN o sia l’owner indicato.
     */
    private void ensureSelfOrAdmin(Long targetUserId) {
        if (currentUserIsAdmin()) return;
        Long me = getCurrentUserId();
        if (!me.equals(targetUserId)) {
            throw new AccessDeniedException("Operazione non consentita");
        }
    }

    /**
     * Controlla se l’utente corrente ha il ruolo ADMIN.
     */
    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Recupera l’ID dell’utente corrente dal contesto di sicurezza.
     */
    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

    /**
     * Verifica che la data fornita appartenga al mese e anno del timesheet.
     *
     * @param data la data da verificare
     * @param ts   il timesheet di riferimento
     * @throws IllegalArgumentException se la data non appartiene al mese/anno del timesheet
     */
    private void ensureDataMatchesTimesheet(LocalDate data, Timesheet ts) {
        var ymTs = java.time.YearMonth.of(ts.getAnno(), ts.getMese());
        if (!java.time.YearMonth.from(data).equals(ymTs)) {
            throw new IllegalArgumentException(
                    String.format("La data %s non appartiene al mese/anno del timesheet (%02d/%d)",
                            data, ts.getMese(), ts.getAnno())
            );
        }
    }

    /**
     * Calcola l’orario in formato decimale a partire da ore e minuti.
     *
     * @param ore    numero di ore
     * @param minuti numero di minuti
     * @return orario in formato decimale (es. 1h20m -> 1.33)
     */
    private double calcOrario(int ore, int minuti) {
        var minutiTot = ore * 60 + minuti;
        return new java.math.BigDecimal(minutiTot)
                .divide(new java.math.BigDecimal(60), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Calcola il costo totale in base all’orario e alla tariffa oraria del cliente.
     *
     * @param orario                orario in formato decimale
     * @param tariffaOrariaCliente  tariffa oraria del cliente
     * @return costo totale arrotondato a 2 decimali
     */
    private double calcCosto(double orario, double tariffaOrariaCliente) {
        return new java.math.BigDecimal(orario)
                .multiply(new java.math.BigDecimal(tariffaOrariaCliente))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

}
