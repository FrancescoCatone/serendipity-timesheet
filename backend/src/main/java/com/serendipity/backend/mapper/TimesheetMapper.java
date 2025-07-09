package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.TimesheetDto;
import com.serendipity.backend.model.entity.Timesheet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TimesheetMapper {

    TimesheetMapper INSTANCE = Mappers.getMapper(TimesheetMapper.class);

    @Mapping(source = "utente.id", target = "utenteId")
    TimesheetDto toDto(Timesheet entity);
}
