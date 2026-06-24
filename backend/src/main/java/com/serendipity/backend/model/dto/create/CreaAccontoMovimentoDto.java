package com.serendipity.backend.model.dto.create;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreaAccontoMovimentoDto {

    @NotNull(message = "L'ID utente è obbligatorio")
    private Long utenteId;

    @Min(value = 1, message = "Il mese deve essere compreso tra 1 e 12")
    @Max(value = 12, message = "Il mese deve essere compreso tra 1 e 12")
    private int mese;

    @Min(value = 2000, message = "L'anno deve essere maggiore o uguale a 2000")
    private int anno;

    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "L'importo deve essere maggiore di zero")
    private BigDecimal importo;

    @Size(max = 500, message = "Le note non possono superare 500 caratteri")
    private String note;

    @NotNull(message = "La data movimento è obbligatoria")
    private LocalDate dataMovimento;

    public Long getUtenteId() {
        return utenteId;
    }

    public void setUtenteId(Long utenteId) {
        this.utenteId = utenteId;
    }

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

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDate dataMovimento) {
        this.dataMovimento = dataMovimento;
    }
}
