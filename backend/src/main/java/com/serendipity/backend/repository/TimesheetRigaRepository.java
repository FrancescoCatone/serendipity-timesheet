package com.serendipity.backend.repository;

import com.serendipity.backend.model.record.TotaleClienteDto;
import com.serendipity.backend.model.record.TotaliDto;
import com.serendipity.backend.model.entity.TimesheetRiga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimesheetRigaRepository extends JpaRepository<TimesheetRiga, Long> {
    List<TimesheetRiga> findByTimesheetId(Long timesheetId);

    @Query("select distinct tr.data " +
            "from TimesheetRiga tr " +
            "where tr.timesheet.id = :timesheetId")
    List<LocalDate> findDistinctDateByTimesheetId(@Param("timesheetId") Long timesheetId);

    @Query("""
            select new com.serendipity.backend.model.record.TotaliDto(
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.timesheet.id = :tsId
            """)
    TotaliDto sumTotaliByTimesheetId(@Param("tsId") Long timesheetId);

    @Query("""
            select new com.serendipity.backend.model.record.TotaleClienteDto(
                r.cliente.id,
                r.cliente.nome,
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.timesheet.id = :tsId
            group by r.cliente.id, r.cliente.nome
            order by r.cliente.nome
            """)
    List<TotaleClienteDto> sumTotaliPerCliente(@Param("tsId") Long timesheetId);
}
