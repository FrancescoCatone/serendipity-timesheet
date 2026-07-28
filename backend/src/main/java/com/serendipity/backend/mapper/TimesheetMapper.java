package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.entity.Timesheet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TimesheetMapper {

    @Mapping(source = "utente.id", target = "utenteId")
    @Mapping(expression = "java(entity.getUtente().getNome() + \" \" + entity.getUtente().getCognome())", target = "utenteNomeCompleto")
    @Mapping(source = "stato", target = "stato")
    TimesheetDto toDto(Timesheet entity);
}
