package com.serendipity.backend.model.dto;

import java.time.LocalDate;

public record TimesheetDto(
        Long id,
        int mese,
        int anno,
        LocalDate dataCompilazione,
        Long utenteId
) {}
