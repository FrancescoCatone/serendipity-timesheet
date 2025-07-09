package com.serendipity.backend.model.dto;

public record ClienteDto(
        Long id,
        String nome,
        double tariffaOraria) {
}
