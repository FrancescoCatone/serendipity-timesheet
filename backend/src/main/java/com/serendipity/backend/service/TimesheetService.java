package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetMapper;
import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.dto.create.CreaTimesheetDto;
import com.serendipity.backend.model.entity.Timesheet;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.UtenteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimesheetService {

    @Autowired
    private TimesheetRepository timesheetRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private TimesheetMapper mapper;

    public List<TimesheetDto> findAll() {
        return timesheetRepository.findAll().stream().map(mapper::toDto).toList();
    }

    public TimesheetDto findById(Long id) {
        return mapper.toDto(timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id)));
    }

    public TimesheetDto create(CreaTimesheetDto dto) {
        Timesheet entity = new Timesheet();
        entity.setAnno(dto.getAnno());
        entity.setMese(dto.getMese());
        entity.setDataCompilazione(dto.getDataCompilazione());
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));
        return mapper.toDto(timesheetRepository.save(entity));
    }

    public TimesheetDto update(Long id, CreaTimesheetDto dto) {
        Timesheet entity = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet non trovato con ID: " + id));

        entity.setMese(dto.getMese());
        entity.setAnno(dto.getAnno());
        entity.setDataCompilazione(dto.getDataCompilazione());
        entity.setUtente(utenteRepository.findById(dto.getUtenteId())
                .orElseThrow(() -> new EntityNotFoundException("Utente non trovato")));

        return mapper.toDto(timesheetRepository.save(entity));
    }

    public void delete(Long id) {
        if (!timesheetRepository.existsById(id)) {
            throw new EntityNotFoundException("Timesheet non trovato con ID: " + id);
        }
        timesheetRepository.deleteById(id);
    }
}
