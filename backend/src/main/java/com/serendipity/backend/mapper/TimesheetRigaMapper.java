package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.TimesheetRigaDto;
import com.serendipity.backend.model.entity.TimesheetRiga;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TimesheetRigaMapper {
    TimesheetRigaMapper INSTANCE = Mappers.getMapper(TimesheetRigaMapper.class);

    @Mapping(source = "timesheet.id", target = "timesheetId")
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "cliente.nome", target = "clienteNome")
    TimesheetRigaDto toDto(TimesheetRiga riga);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timesheet", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    TimesheetRiga toEntity(TimesheetRigaDto dto);
}
