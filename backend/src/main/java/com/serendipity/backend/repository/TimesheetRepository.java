package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    List<Timesheet> findByUtenteId(Long utenteId);

    List<Timesheet> findByMeseAndAnno(int mese, int anno);

    List<Timesheet> findByUtenteIdAndMeseAndAnno(Long utenteId, int mese, int anno);

    @Query("select distinct t.anno from Timesheet t order by t.anno desc")
    List<Integer> findDistinctAnni();

    @Query("select distinct t.mese from Timesheet t where t.anno = :anno order by t.mese asc")
    List<Integer> findDistinctMesiByAnno(@Param("anno") int anno);
}
