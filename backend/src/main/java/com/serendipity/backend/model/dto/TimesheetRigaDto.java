package com.serendipity.backend.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TimesheetRigaDto {
    private Long id;
    private Long timesheetId;
    private Long clienteId;
    private String clienteNome;
    private LocalDate data;
    private int ore;
    private int minuti;
    private BigDecimal orario;
    private BigDecimal costoOrario;

    public TimesheetRigaDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
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
