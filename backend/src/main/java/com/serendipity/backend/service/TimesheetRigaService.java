package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetRigaMapper;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
     * Restituisce la lista di tutte le righe del timesheet.
     *
     * @return Lista di TimesheetRiga come TimesheetRigaDto
     */
    public List<TimesheetRigaDto> findAll() {
        return rigaRepository.findAll().stream().map(mapper::toDto).toList();
    }

    /**
     * Trova una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da cercare
     * @return TimesheetRigaDto se trovato
     * @throws EntityNotFoundException se la riga non esiste
     */
    public TimesheetRigaDto findById(Long id) {
        return mapper.toDto(
                rigaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Riga non trovata"))
        );
    }

    /**
     * Crea una nuova riga del timesheet.
     *
     * @param dto Dati della riga del timesheet da creare
     * @return TimesheetRigaDto creato
     */
    public TimesheetRigaDto save(CreaTimesheetRigaDto dto) {
        TimesheetRiga entity = new TimesheetRiga();
        return getTimesheetRigaDto(dto, entity);
    }

    /**
     * Imposta i campi dell'entità TimesheetRiga e salva l'entità nel repository.
     *
     * @param dto    Dati della riga del timesheet
     * @param entity Entità TimesheetRiga da aggiornare
     * @return TimesheetRigaDto salvato
     */
    private TimesheetRigaDto getTimesheetRigaDto(CreaTimesheetRigaDto dto, TimesheetRiga entity) {
        entity.setCliente(clienteRepository.findById(dto.getClienteId()).orElseThrow());
        entity.setTimesheet(timesheetRepository.findById(dto.getTimesheetId()).orElseThrow());
        entity.setData(dto.getData());
        entity.setOre(dto.getOre());
        entity.setMinuti(dto.getMinuti());
        entity.setOrario(dto.getOrario().doubleValue());
        entity.setCostoOrario(dto.getCostoOrario().doubleValue());

        return mapper.toDto(rigaRepository.save(entity));
    }

    /**
     * Aggiorna una riga esistente del timesheet.
     *
     * @param id  ID della riga del timesheet da aggiornare
     * @param dto Dati aggiornati della riga del timesheet
     * @return TimesheetRigaDto aggiornato
     * @throws EntityNotFoundException se la riga non esiste
     */
    public TimesheetRigaDto update(Long id, CreaTimesheetRigaDto dto) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));

        return getTimesheetRigaDto(dto, existing);
    }

    /**
     * Elimina una riga del timesheet per ID.
     *
     * @param id ID della riga del timesheet da eliminare
     * @throws EntityNotFoundException se la riga non esiste
     */
    public void delete(Long id) {
        if (!rigaRepository.existsById(id)) {
            throw new EntityNotFoundException("Riga non trovata");
        }
        rigaRepository.deleteById(id);
    }

    /**
     * Filtra le righe del timesheet in base a clienteId, utenteId e data.
     *
     * @param clienteId ID del cliente (opzionale)
     * @param utenteId  ID dell'utente (opzionale)
     * @param dataStr   Data in formato stringa (opzionale)
     * @return Lista di TimesheetRigaDto che soddisfano i criteri di filtro
     */
    public List<TimesheetRigaDto> filtra(Long clienteId, Long utenteId, String dataStr) {
        return rigaRepository.findAll().stream()
                .filter(r -> clienteId == null || r.getCliente().getId().equals(clienteId))
                .filter(r -> utenteId == null || r.getTimesheet().getUtente().getId().equals(utenteId))
                .filter(r -> dataStr == null || r.getData().equals(LocalDate.parse(dataStr)))
                .map(mapper::toDto)
                .toList();
    }

    /** Recupera tutte le righe associate a un timesheet specifico, con controlli di autorizzazione.
     *
     * @param timesheetId ID del timesheet di cui recuperare le righe
     * @return Lista di TimesheetRigaDto associati al timesheet
     * @throws EntityNotFoundException se il timesheet non esiste o l'utente non è autorizzato
     */
    public List<TimesheetRigaDto> findByTimesheetId(Long timesheetId) {
        // 1) Verifica esistenza timesheet
        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + timesheetId));


        // 2) Autorizzazione: ADMIN ok, DIPENDENTE solo se proprietario del TS
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Long currentUserId = utenteRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato")).getId();
            if (!ts.getUtente().getId().equals(currentUserId)) {
                throw new EntityNotFoundException("Timesheet non trovato");
            }
        }

        // 3) Recupero righe
        return rigaRepository.findByTimesheetId(timesheetId).stream()
                .map(mapper::toDto)
                .toList();
    }
}
