package com.serendipity.backend.model.entity;

import com.serendipity.backend.model.enums.AccontoMovimentoTipo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "acconto_movimento")
public class AccontoMovimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @Column(nullable = false)
    private int mese;

    @Column(nullable = false)
    private int anno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AccontoMovimentoTipo tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importo;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDate dataMovimento;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (tipo == null) {
            tipo = AccontoMovimentoTipo.ACCONTO;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utente getUtente() {
        return utente;
    }

    public void setUtente(Utente utente) {
        this.utente = utente;
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

    public AccontoMovimentoTipo getTipo() {
        return tipo;
    }

    public void setTipo(AccontoMovimentoTipo tipo) {
        this.tipo = tipo;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
