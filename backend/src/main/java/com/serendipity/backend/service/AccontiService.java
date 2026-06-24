package com.serendipity.backend.service;

import com.serendipity.backend.model.dto.AccontiSummaryDto;
import com.serendipity.backend.model.dto.AccontoMovimentoDto;
import com.serendipity.backend.model.dto.create.CreaAccontoMovimentoDto;
import com.serendipity.backend.model.entity.AccontoMovimento;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.TimesheetStato;
import com.serendipity.backend.repository.AccontoMovimentoRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccontiService {

    private final AccontoMovimentoRepository accontoMovimentoRepository;
    private final UtenteRepository utenteRepository;
    private final TimesheetRepository timesheetRepository;
    private final TimesheetRigaRepository timesheetRigaRepository;

    public AccontiService(AccontoMovimentoRepository accontoMovimentoRepository,
                          UtenteRepository utenteRepository,
                          TimesheetRepository timesheetRepository,
                          TimesheetRigaRepository timesheetRigaRepository) {
        this.accontoMovimentoRepository = accontoMovimentoRepository;
        this.utenteRepository = utenteRepository;
        this.timesheetRepository = timesheetRepository;
        this.timesheetRigaRepository = timesheetRigaRepository;
    }

    public AccontiSummaryDto summary(Long utenteId, int mese, int anno) {
        validatePeriodo(mese, anno);

        Utente utente = utenteRepository.findById(utenteId)
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        List<AccontoMovimento> movimenti = accontoMovimentoRepository
                .findByUtenteIdAndMeseAndAnnoOrderByCreatedAtDescIdDesc(utenteId, mese, anno);

        BigDecimal totaleAcconti = sumImporti(movimenti);
        BigDecimal totaleMovimenti = totaleAcconti;

        Timesheet timesheet = timesheetRepository.findByUtenteIdAndMeseAndAnno(utenteId, mese, anno).orElse(null);
        BigDecimal maturato = BigDecimal.ZERO;
        String timesheetStato = null;
        if (timesheet != null) {
            timesheetStato = timesheet.getStato().name();
            Double costo = timesheetRigaRepository.sumCostoByUtenteIdAndMeseAndAnno(utenteId, mese, anno);
            maturato = scale(costo == null ? 0D : costo);
        }

        BigDecimal saldoResiduo = scale(maturato.subtract(totaleMovimenti));

        return new AccontiSummaryDto(
                utente.getId(),
                utente.getNome(),
                utente.getCognome(),
                mese,
                anno,
                timesheetStato,
                scale(maturato),
                totaleAcconti,
                totaleMovimenti,
                saldoResiduo,
                movimenti.stream().map(this::toDto).toList()
        );
    }

    public AccontoMovimentoDto create(CreaAccontoMovimentoDto dto) {
        validatePeriodo(dto.getMese(), dto.getAnno());
        validateDataMovimento(dto.getDataMovimento(), dto.getMese(), dto.getAnno());

        Utente utente = utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato"));

        AccontoMovimento entity = new AccontoMovimento();
        entity.setUtente(utente);
        entity.setMese(dto.getMese());
        entity.setAnno(dto.getAnno());
        entity.setImporto(scale(dto.getImporto()));
        entity.setNote(dto.getNote() == null || dto.getNote().isBlank() ? null : dto.getNote().trim());
        entity.setDataMovimento(dto.getDataMovimento());

        return toDto(accontoMovimentoRepository.save(entity));
    }

    public void delete(Long movimentoId) {
        AccontoMovimento entity = accontoMovimentoRepository.findById(movimentoId)
                .orElseThrow(() -> new EntityNotFoundException("Movimento acconto non trovato"));
        accontoMovimentoRepository.delete(entity);
    }

    private void validatePeriodo(int mese, int anno) {
        if (mese < 1 || mese > 12) {
            throw new IllegalArgumentException("Il mese deve essere compreso tra 1 e 12");
        }
        if (anno < 2000) {
            throw new IllegalArgumentException("L'anno deve essere maggiore o uguale a 2000");
        }
    }

    private void validateDataMovimento(LocalDate dataMovimento, int mese, int anno) {
        if (dataMovimento == null) {
            throw new IllegalArgumentException("La data movimento è obbligatoria");
        }
        if (dataMovimento.getMonthValue() != mese || dataMovimento.getYear() != anno) {
            throw new IllegalArgumentException("La data movimento deve appartenere al mese e anno selezionati");
        }
    }

    private BigDecimal sumImporti(List<AccontoMovimento> movimenti) {
        return scale(movimenti.stream()
                .map(AccontoMovimento::getImporto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private AccontoMovimentoDto toDto(AccontoMovimento entity) {
        return new AccontoMovimentoDto(
                entity.getId(),
                entity.getUtente().getId(),
                entity.getUtente().getNome(),
                entity.getUtente().getCognome(),
                entity.getMese(),
                entity.getAnno(),
                scale(entity.getImporto()),
                entity.getNote(),
                entity.getDataMovimento(),
                entity.getCreatedAt()
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
