package com.serendipity.backend.model.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private double tariffaOraria; // quanto paga il cliente ad ora

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<TimesheetRiga> righe = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getTariffaOraria() {
        return tariffaOraria;
    }

    public void setTariffaOraria(double tariffaOraria) {
        this.tariffaOraria = tariffaOraria;
    }

    public List<TimesheetRiga> getRighe() {
        return righe;
    }

    public void setRighe(List<TimesheetRiga> righe) {
        this.righe = righe;
    }

    public Cliente() {
    }
}
