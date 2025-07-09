package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteMapper INSTANCE = Mappers.getMapper(ClienteMapper.class);

    ClienteDto toDto(Cliente entity);

    Cliente fromCreateDto(CreaClienteDto dto);
}
