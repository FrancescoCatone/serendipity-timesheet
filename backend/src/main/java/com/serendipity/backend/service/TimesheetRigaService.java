package com.serendipity.backend.service;

import com.serendipity.backend.mapper.TimesheetRigaMapper;
import com.serendipity.backend.model.dto.CreaTimesheetRigaDto;
import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.entity.TimesheetRiga;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.TimesheetRepository;
import com.serendipity.backend.repository.TimesheetRigaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<TimesheetRigaDto> findAll() {
        return rigaRepository.findAll().stream().map(mapper::toDto).toList();
    }

    public TimesheetRigaDto findById(Long id) {
        return mapper.toDto(
                rigaRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Riga non trovata"))
        );
    }

    public TimesheetRigaDto save(CreaTimesheetRigaDto dto) {
        TimesheetRiga entity = new TimesheetRiga();
        return getTimesheetRigaDto(dto, entity);
    }

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

    public TimesheetRigaDto update(Long id, CreaTimesheetRigaDto dto) {
        TimesheetRiga existing = rigaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Riga non trovata"));

        return getTimesheetRigaDto(dto, existing);
    }

    public void delete(Long id) {
        if (!rigaRepository.existsById(id)) {
            throw new EntityNotFoundException("Riga non trovata");
        }
        rigaRepository.deleteById(id);
    }

    public List<TimesheetRigaDto> filtra(Long clienteId, Long utenteId, String dataStr) {
        return rigaRepository.findAll().stream()
                .filter(r -> clienteId == null || r.getCliente().getId().equals(clienteId))
                .filter(r -> utenteId == null || r.getTimesheet().getUtente().getId().equals(utenteId))
                .filter(r -> dataStr == null || r.getData().equals(LocalDate.parse(dataStr)))
                .map(mapper::toDto)
                .toList();
    }
}
