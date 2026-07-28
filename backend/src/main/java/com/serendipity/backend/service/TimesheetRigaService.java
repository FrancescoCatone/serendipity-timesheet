package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetRigaMapper;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetRigaDto;
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

    private TimesheetRigaMapper mapper = TimesheetRigaMapper.INSTANCE;

    @Autowired
    private UtenteRepository utenteRepository;

    public List<TimesheetRigaDto> findAll() {
        List<TimesheetRiga> righe = currentUserIsAdmin()
                ? rigaRepository.findAllOrdered()
                : rigaRepository.findByTimesheetUtenteId(getCurrentUserId());

        return righe.stream()
                .map(mapper::toDto)
                .toList();
    }

    public TimesheetRigaDto findById(Long id) {
        TimesheetRiga r = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));
        ensureOwnedOrAdmin(r.getTimesheet());
        return mapper.toDto(r);
    }

    public TimesheetRigaDto save(CreaTimesheetRigaDto dto) {
        Timesheet ts = timesheetRepository.findById(dto.getTimesheetId())
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato"));

        ensureSelfOrAdmin(ts.getUtente().getId());
        ensureTimesheetIsEditable(ts);
        ensureDataMatchesTimesheet(dto.getData(), ts);

        Cliente cliente = findCliente(dto.getClienteId());
        validateDuration(dto.getOre(), dto.getMinuti(), cliente);

        List<TimesheetRiga> sameDayRows = findRowsByTimesheetAndDate(ts.getId(), dto.getData());
        validateDailyConsistency(dto.getData(), cliente, sameDayRows, null);

        TimesheetRiga mergeCandidate = findMergeCandidate(sameDayRows, cliente.getId(), null);
        if (mergeCandidate != null) {
            int[] mergedDuration = sumDuration(
                    mergeCandidate.getOre(),
                    mergeCandidate.getMinuti(),
                    dto.getOre(),
                    dto.getMinuti()
            );
            apply(mergeCandidate, ts, cliente, dto.getData(), mergedDuration[0], mergedDuration[1]);
            return mapper.toDto(rigaRepository.save(mergeCandidate));
        }

        TimesheetRiga entity = new TimesheetRiga();
        apply(entity, ts, cliente, dto.getData(), dto.getOre(), dto.getMinuti());
        return mapper.toDto(rigaRepository.save(entity));
    }

    public TimesheetRigaDto update(Long id, CreaTimesheetRigaDto dto) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));

        Timesheet currentTs = existing.getTimesheet();
        ensureOwnedOrAdmin(currentTs);
        ensureTimesheetIsEditable(currentTs);

        if (!currentTs.getId().equals(dto.getTimesheetId())) {
            throw new AccessDeniedException("Non puoi cambiare il timesheet di appartenenza della riga");
        }

        ensureDataMatchesTimesheet(dto.getData(), currentTs);

        Cliente cliente = findCliente(dto.getClienteId());
        validateDuration(dto.getOre(), dto.getMinuti(), cliente);

        List<TimesheetRiga> sameDayRows = findRowsByTimesheetAndDate(currentTs.getId(), dto.getData());
        validateDailyConsistency(dto.getData(), cliente, sameDayRows, existing.getId());

        TimesheetRiga mergeCandidate = findMergeCandidate(sameDayRows, cliente.getId(), existing.getId());
        if (mergeCandidate != null) {
            int[] mergedDuration = sumDuration(
                    mergeCandidate.getOre(),
                    mergeCandidate.getMinuti(),
                    dto.getOre(),
                    dto.getMinuti()
            );
            apply(mergeCandidate, currentTs, cliente, dto.getData(), mergedDuration[0], mergedDuration[1]);
            rigaRepository.delete(existing);
            return mapper.toDto(rigaRepository.save(mergeCandidate));
        }

        apply(existing, currentTs, cliente, dto.getData(), dto.getOre(), dto.getMinuti());
        return mapper.toDto(rigaRepository.save(existing));
    }

    public void delete(Long id) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));
        Timesheet ts = existing.getTimesheet();
        ensureOwnedOrAdmin(ts);
        ensureTimesheetIsEditable(ts);
        rigaRepository.delete(existing);
    }

    public List<TimesheetRigaDto> filtra(Long clienteId, Long utenteId, String dataStr) {
        boolean admin = currentUserIsAdmin();

        Long effectiveUtenteId = admin ? utenteId : getCurrentUserId();
        LocalDate data = dataStr != null ? LocalDate.parse(dataStr) : null;

        List<TimesheetRiga> righe = (data == null)
                ? rigaRepository.searchFilteredWithoutData(clienteId, effectiveUtenteId)
                : rigaRepository.searchFilteredWithData(clienteId, effectiveUtenteId, data);

        return righe.stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<TimesheetRigaDto> findByTimesheetId(Long timesheetId) {
        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + timesheetId));

        ensureOwnedOrAdmin(ts);

        return rigaRepository.findByTimesheetIdOrdered(timesheetId).stream()
                .map(mapper::toDto)
                .toList();
    }

    private void apply(TimesheetRiga entity,
                       Timesheet ts,
                       Cliente cliente,
                       LocalDate data,
                       int ore,
                       int minuti) {
        double orarioCalcolato = calcOrario(ore, minuti);
        double costoCalcolato = calcCosto(orarioCalcolato, cliente.getTariffaOraria());

        entity.setTimesheet(ts);
        entity.setCliente(cliente);
        entity.setData(data);
        entity.setOre(ore);
        entity.setMinuti(minuti);
        entity.setOrario(orarioCalcolato);
        entity.setCostoOrario(costoCalcolato);
    }

    private Cliente findCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));
    }

    private void validateDuration(int ore, int minuti, Cliente cliente) {
        if (ore < 0) {
            throw new IllegalArgumentException("Le ore non possono essere negative");
        }
        if (minuti < 0 || minuti > 59) {
            throw new IllegalArgumentException("I minuti devono essere compresi tra 0 e 59");
        }

        boolean zeroDuration = ore == 0 && minuti == 0;
        boolean nonLavorato = isNonLavorato(cliente);

        if (zeroDuration && !nonLavorato) {
            throw new IllegalArgumentException("Una riga con 0 ore e 0 minuti è consentita solo per il cliente NON LAVORATO");
        }

        if (!zeroDuration && nonLavorato) {
            throw new IllegalArgumentException("Il cliente NON LAVORATO deve avere 0 ore e 0 minuti");
        }
    }

    private List<TimesheetRiga> findRowsByTimesheetAndDate(Long timesheetId, LocalDate data) {
        List<TimesheetRiga> allRows = rigaRepository.findByTimesheetIdOrdered(timesheetId);
        if (allRows == null || allRows.isEmpty()) {
            return List.of();
        }

        return allRows.stream()
                .filter(row -> data.equals(row.getData()))
                .toList();
    }

    private void validateDailyConsistency(LocalDate data,
                                          Cliente cliente,
                                          List<TimesheetRiga> sameDayRows,
                                          Long ignoredRowId) {
        List<TimesheetRiga> otherRows = sameDayRows.stream()
                .filter(row -> ignoredRowId == null || !ignoredRowId.equals(row.getId()))
                .toList();

        if (otherRows.isEmpty()) {
            return;
        }

        if (isNonLavorato(cliente)) {
            throw new IllegalStateException(
                    "Non puoi inserire NON LAVORATO il " + data + " perché per quel giorno esistono già una o più righe"
            );
        }

        boolean hasNonLavoratoRow = otherRows.stream().anyMatch(row -> isNonLavorato(row.getCliente()));
        if (hasNonLavoratoRow) {
            throw new IllegalStateException(
                    "Non puoi inserire una lavorazione il " + data + " perché per quel giorno è già presente la riga NON LAVORATO"
            );
        }
    }

    private TimesheetRiga findMergeCandidate(List<TimesheetRiga> sameDayRows, Long clienteId, Long ignoredRowId) {
        return sameDayRows.stream()
                .filter(row -> ignoredRowId == null || !ignoredRowId.equals(row.getId()))
                .filter(row -> row.getCliente() != null)
                .filter(row -> row.getCliente().getId() != null)
                .filter(row -> row.getCliente().getId().equals(clienteId))
                .filter(row -> !isNonLavorato(row.getCliente()))
                .findFirst()
                .orElse(null);
    }

    private int[] sumDuration(int firstOre, int firstMinuti, int secondOre, int secondMinuti) {
        int totalMinutes = (firstOre * 60 + firstMinuti) + (secondOre * 60 + secondMinuti);
        return new int[]{totalMinutes / 60, totalMinutes % 60};
    }

    private boolean isNonLavorato(Cliente cliente) {
        return cliente.getNome() != null
                && cliente.getNome().equalsIgnoreCase(SystemClienti.NON_LAVORATO);
    }

    private void ensureTimesheetIsEditable(Timesheet ts) {
        TimesheetStato stato = ts.getStato();

        if (stato == TimesheetStato.CONFERMATO || stato == TimesheetStato.CHIUSO) {
            throw new IllegalStateException("Timesheet non modificabile nello stato attuale");
        }
    }

    private void ensureOwnedOrAdmin(Timesheet ts) {
        if (currentUserIsAdmin()) {
            return;
        }

        Long me = getCurrentUserId();
        if (!ts.getUtente().getId().equals(me)) {
            throw new EntityNotFoundException("Riga non trovata");
        }
    }

    private void ensureSelfOrAdmin(Long targetUserId) {
        if (currentUserIsAdmin()) {
            return;
        }

        Long me = getCurrentUserId();
        if (!me.equals(targetUserId)) {
            throw new AccessDeniedException("Operazione non consentita");
        }
    }

    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

    private void ensureDataMatchesTimesheet(LocalDate data, Timesheet ts) {
        var ymTs = java.time.YearMonth.of(ts.getAnno(), ts.getMese());
        if (!java.time.YearMonth.from(data).equals(ymTs)) {
            throw new IllegalArgumentException(
                    String.format("La data %s non appartiene al mese/anno del timesheet (%02d/%d)",
                            data, ts.getMese(), ts.getAnno())
            );
        }
    }

    private double calcOrario(int ore, int minuti) {
        var minutiTot = ore * 60 + minuti;
        return new java.math.BigDecimal(minutiTot)
                .divide(new java.math.BigDecimal(60), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double calcCosto(double orario, double tariffaOrariaCliente) {
        return new java.math.BigDecimal(orario)
                .multiply(new java.math.BigDecimal(tariffaOrariaCliente))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }
}
