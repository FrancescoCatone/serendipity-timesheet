package com.serendipity.backend.model.dto.create;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreaTimesheetRigaDto {

    @NotNull(message = "ID del timesheet obbligatorio")
    private Long timesheetId;

    @NotNull(message = "ID del cliente obbligatorio")
    private Long clienteId;

    @NotNull(message = "La data è obbligatoria")
    private LocalDate data;

    @Min(value = 0, message = "Le ore non possono essere negative")
    private int ore;

    @Min(value = 0)
    @Max(value = 59, message = "I minuti devono essere tra 0 e 59")
    private int minuti;

    @DecimalMin(value = "0.0", inclusive = false, message = "L'orario deve essere maggiore di 0")
    private BigDecimal orario;

    @DecimalMin(value = "0.0", inclusive = false, message = "Il costo orario deve essere maggiore di 0")
    private BigDecimal costoOrario;

    public CreaTimesheetRigaDto() {
    }

    public Long getTimesheetId() {
        return timesheetId;
    }

    public void setTimesheetId(Long timesheetId) {
        this.timesheetId = timesheetId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public int getOre() {
        return ore;
    }

    public void setOre(int ore) {
        this.ore = ore;
    }

    public int getMinuti() {
        return minuti;
    }

    public void setMinuti(int minuti) {
        this.minuti = minuti;
    }

    public BigDecimal getOrario() {
        return orario;
    }

    public void setOrario(BigDecimal orario) {
        this.orario = orario;
    }

    public BigDecimal getCostoOrario() {
        return costoOrario;
    }

    public void setCostoOrario(BigDecimal costoOrario) {
        this.costoOrario = costoOrario;
    }

}
