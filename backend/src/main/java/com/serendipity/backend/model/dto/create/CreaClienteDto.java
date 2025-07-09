package com.serendipity.backend.model.dto.create;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreaClienteDto {

    @NotBlank(message = "Il nome del cliente è obbligatorio")
    @Size(max = 100, message = "Il nome del cliente non può superare 100 caratteri")
    private String nome;

    @NotNull(message = "La tariffa oraria è obbligatoria")
    @DecimalMin(value = "0.0", inclusive = false, message = "La tariffa oraria deve essere maggiore di 0")
    private Double tariffaOraria;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getTariffaOraria() {
        return tariffaOraria;
    }

    public void setTariffaOraria(Double tariffaOraria) {
        this.tariffaOraria = tariffaOraria;
    }
}
