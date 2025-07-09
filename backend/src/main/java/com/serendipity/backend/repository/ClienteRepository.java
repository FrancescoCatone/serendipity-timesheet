package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Cliente findByNome(String nome);

    @Query("""
            SELECT u.id, u.nome, u.cognome, SUM(tr.orario)
            FROM TimesheetRiga tr
            JOIN tr.timesheet t
            JOIN t.utente u
            WHERE tr.cliente.id = :clienteId
            AND t.mese = :mese
            AND t.anno = :anno
            GROUP BY u.id, u.nome, u.cognome
            ORDER BY u.cognome, u.nome
            """)
    List<Object[]> findDipendentiConOreTotali(@Param("clienteId") Long clienteId,
                                              @Param("mese") int mese,
                                              @Param("anno") int anno);

    @Query("""
            SELECT COALESCE(SUM(tr.orario), 0)
            FROM TimesheetRiga tr
            JOIN tr.timesheet t
            WHERE tr.cliente.id = :clienteId
            AND t.mese = :mese
            AND t.anno = :anno
            """)
    double sommaTotaleOrePerCliente(@Param("clienteId") Long clienteId,
                                    @Param("mese") int mese,
                                    @Param("anno") int anno);
}
