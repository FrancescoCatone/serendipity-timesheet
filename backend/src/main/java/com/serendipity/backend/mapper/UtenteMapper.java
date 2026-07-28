package com.serendipity.backend.mapper;

import com.serendipity.backend.model.dto.create.CreaUtenteDto;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.entity.Utente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UtenteMapper {

    UtenteDto toDto(Utente utente);
    Utente toEntity(UtenteDto dto);
    Utente fromCreateDto(CreaUtenteDto dto);
}
