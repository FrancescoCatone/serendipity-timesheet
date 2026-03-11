package com.serendipity.backend.service;

import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.report.ReportClienteDipendenteDto;
import com.serendipity.backend.model.dto.report.ReportClienteDto;
import com.serendipity.backend.model.dto.report.ReportDipendenteClienteDto;
import com.serendipity.backend.model.dto.report.ReportDipendenteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReportService {

    private final ClienteRepository clienteRepository;
    private final UtenteRepository utenteRepository;
    private final TimesheetRigaRepository rigaRepository;

    public ReportService(ClienteRepository clienteRepository,
                         UtenteRepository utenteRepository,
                         TimesheetRigaRepository rigaRepository) {
        this.clienteRepository = clienteRepository;
        this.utenteRepository = utenteRepository;
        this.rigaRepository = rigaRepository;
    }

    /**
     * Genera un report per cliente, con dettaglio per dipendente.
     * Solo gli admin possono accedere a questo report.
     *
     * @param clienteId l'id del cliente
     * @param mese      il mese del report (opzionale)
     * @param anno      l'anno del report (opzionale)
     * @return il report per cliente
     */
    public ReportClienteDto reportPerCliente(Long clienteId, Integer mese, Integer anno) {
        if (!currentUserIsAdmin()) {
            throw new AccessDeniedException("Solo ADMIN può consultare il report per cliente");
        }

        validatePeriodo(mese, anno);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente non trovato"));

        List<ReportClienteDipendenteDto> dettaglio = rigaRepository
                .reportClientePerDipendente(clienteId, mese, anno)
                .stream()
                .map(item -> new ReportClienteDipendenteDto(
                        item.utenteId(),
                        item.nome(),
                        item.cognome(),
                        round(item.oreTotali()),
                        round(item.costoTotale())
                ))
                .toList();

        TotaliDto totali = rigaRepository.totaleReportCliente(clienteId, mese, anno);

        return new ReportClienteDto(
                cliente.getId(),
                cliente.getNome(),
                mese,
                anno,
                round(totali.totaleOrario()),
                round(totali.totaleCosto()),
                dettaglio
        );
    }

    /**
     * Genera un report per dipendente, con dettaglio per cliente.
     * Gli admin possono accedere al report di qualsiasi dipendente, mentre i dipendenti possono accedere solo al proprio report.
     *
     * @param utenteId l'id del dipendente
     * @param mese     il mese del report (opzionale)
     * @param anno     l'anno del report (opzionale)
     * @return il report per dipendente
     */
    public ReportDipendenteDto reportPerDipendente(Long utenteId, Integer mese, Integer anno) {
        validatePeriodo(mese, anno);

        if (!currentUserIsAdmin() && !getCurrentUserId().equals(utenteId)) {
            throw new AccessDeniedException("Non puoi consultare il report di un altro dipendente");
        }

        Utente utente = utenteRepository.findById(utenteId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        List<ReportDipendenteClienteDto> dettaglio = rigaRepository
                .reportDipendentePerCliente(utenteId, mese, anno)
                .stream()
                .map(item -> new ReportDipendenteClienteDto(
                        item.clienteId(),
                        item.clienteNome(),
                        round(item.oreTotali()),
                        round(item.costoTotale())
                ))
                .toList();

        TotaliDto totali = rigaRepository.totaleReportDipendente(utenteId, mese, anno);

        return new ReportDipendenteDto(
                utente.getId(),
                utente.getNome(),
                utente.getCognome(),
                mese,
                anno,
                round(totali.totaleOrario()),
                round(totali.totaleCosto()),
                dettaglio
        );
    }


    /**
     * Validazione semplice per mese e anno.
     * Il mese deve essere tra 1 e 12, l'anno deve essere >= 2000.
     * Se i parametri sono null, vengono considerati come "tutti i mesi" o "tutti gli anni".
     *
     * @param mese il mese da validare
     * @param anno l'anno da validare
     */
    private void validatePeriodo(Integer mese, Integer anno) {
        if (mese != null && (mese < 1 || mese > 12)) {
            throw new IllegalArgumentException("Il mese deve essere compreso tra 1 e 12");
        }

        if (anno != null && anno < 2000) {
            throw new IllegalArgumentException("L'anno deve essere maggiore o uguale a 2000");
        }
    }

    /**
     * Controlla se l'utente corrente ha il ruolo ADMIN.
     *
     * @return true se l'utente è admin, false altrimenti
     */
    private boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Ottiene l'id dell'utente corrente dal contesto di sicurezza.
     *
     * @return l'id dell'utente corrente
     */
    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utenteRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utente corrente non trovato"))
                .getId();
    }

    /**
     * Arrotonda un valore double a 2 decimali.
     *
     * @param value il valore da arrotondare
     * @return il valore arrotondato a 2 decimali
     */
    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
