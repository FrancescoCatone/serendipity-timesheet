package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    boolean existsByUtenteId(Long utenteId);

    boolean existsByUtenteIdAndMeseAndAnno(Long utenteId, int mese, int anno);

    List<Timesheet> findByUtenteIdOrderByAnnoAscMeseAsc(Long utenteId);

    @Query("select distinct t.anno from Timesheet t order by t.anno desc")
    List<Integer> findDistinctAnni();

    @Query("select distinct t.mese from Timesheet t where t.anno = :anno order by t.mese asc")
    List<Integer> findDistinctMesiByAnno(@Param("anno") int anno);

    @Query("""
                select t
                from Timesheet t
                where (:mese is null or t.mese = :mese)
                  and (:anno is null or t.anno = :anno)
                  and (:utenteId is null or t.utente.id = :utenteId)
                order by t.anno asc, t.mese asc, t.utente.cognome asc, t.utente.nome asc
            """)
    List<Timesheet> searchFiltered(@Param("mese") Integer mese,
                                   @Param("anno") Integer anno,
                                   @Param("utenteId") Long utenteId);

    @Query("select distinct t.anno from Timesheet t where t.utente.id = :utenteId order by t.anno desc")
    List<Integer> findDistinctAnniByUtenteId(@Param("utenteId") Long utenteId);

    @Query("""
             select distinct t.mese
             from Timesheet t
             where t.anno = :anno
               and t.utente.id = :utenteId
             order by t.mese asc
            """)
    List<Integer> findDistinctMesiByAnnoAndUtenteId(@Param("anno") int anno,
                                                    @Param("utenteId") Long utenteId);
}
