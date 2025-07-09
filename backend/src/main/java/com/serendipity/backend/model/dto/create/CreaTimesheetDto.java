package com.serendipity.backend.model.dto.create;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CreaTimesheetDto {

    @Min(value = 1)
    @Max(value = 12)
    private int mese;

    @Min(value = 2000)
    private int anno;

    @NotNull(message = "La data di compilazione è obbligatoria")
    private LocalDate dataCompilazione;

    @NotNull(message = "L'ID utente è obbligatorio")
    private Long utenteId;

    public int getMese() {
        return mese;
    }

    public void setMese(int mese) {
        this.mese = mese;
    }

    public int getAnno() {
        return anno;
    }

    public void setAnno(int anno) {
        this.anno = anno;
    }

    public LocalDate getDataCompilazione() {
        return dataCompilazione;
    }

    public void setDataCompilazione(LocalDate dataCompilazione) {
        this.dataCompilazione = dataCompilazione;
    }

    public Long getUtenteId() {
        return utenteId;
    }

    public void setUtenteId(Long utenteId) {
        this.utenteId = utenteId;
    }
}
