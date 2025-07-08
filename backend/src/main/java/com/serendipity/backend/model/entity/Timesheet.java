package com.serendipity.backend.model.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int mese;

    private int anno;

    private LocalDate dataCompilazione;

    @ManyToOne
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetRiga> timesheetRighe = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDataCompilazione() {
        return dataCompilazione;
    }

    public void setDataCompilazione(LocalDate dataCompilazione) {
        this.dataCompilazione = dataCompilazione;
    }

    public Utente getUtente() {
        return utente;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
    }

    public List<TimesheetRiga> getTimesheetRighe() {
        return timesheetRighe;
    }

    public void setTimesheetRighe(List<TimesheetRiga> righe) {
        this.timesheetRighe = righe;
    }

    public Timesheet() {
    }
}
