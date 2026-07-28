package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteDto toDto(Cliente entity);

    Cliente fromCreateDto(CreaClienteDto dto);
}
