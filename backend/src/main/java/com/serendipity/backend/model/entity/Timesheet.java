package com.serendipity.backend.model.entity;

import com.serendipity.backend.model.enums.TimesheetStato;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "timesheet",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_timesheet_utente_mese_anno",
                columnNames = {"utente_id", "mese", "anno"}
        )
)
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int mese;

    @Column(nullable = false)
    private int anno;

    private LocalDate dataCompilazione;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimesheetStato stato = TimesheetStato.APERTO;

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

    public TimesheetStato getStato() {
        return stato;
    }

    public void setStato(TimesheetStato stato) {
        this.stato = stato;
    }

    public Timesheet() {
    }
}
