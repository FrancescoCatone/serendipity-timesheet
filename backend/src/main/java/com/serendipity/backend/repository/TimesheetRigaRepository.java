package com.serendipity.backend.repository;

import com.serendipity.backend.model.dto.TotaleClienteDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.entity.TimesheetRiga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimesheetRigaRepository extends JpaRepository<TimesheetRiga, Long> {

    @Query("""
            select new com.serendipity.backend.model.dto.TotaliDto(
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.timesheet.id = :tsId
            """)
    TotaliDto sumTotaliByTimesheetId(@Param("tsId") Long timesheetId);

    @Query("""
            select new com.serendipity.backend.model.dto.TotaleClienteDto(
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

    @Query("""
            select r
            from TimesheetRiga r
            where r.timesheet.id = :timesheetId
            order by r.data asc, r.cliente.nome asc, r.id asc
            """)
    List<TimesheetRiga> findByTimesheetIdOrdered(@Param("timesheetId") Long timesheetId);

    List<TimesheetRiga> findByTimesheetUtenteId(Long utenteId);

    @Query("""
             select r
             from TimesheetRiga r
             where (:clienteId is null or r.cliente.id = :clienteId)
               and (:utenteId is null or r.timesheet.utente.id = :utenteId)
             order by r.data asc, r.cliente.nome asc, r.id asc
            """)
    List<TimesheetRiga> searchFilteredWithoutData(@Param("clienteId") Long clienteId,
                                                  @Param("utenteId") Long utenteId);

    @Query("""
             select r
             from TimesheetRiga r
             where (:clienteId is null or r.cliente.id = :clienteId)
               and (:utenteId is null or r.timesheet.utente.id = :utenteId)
               and r.data = :data
             order by r.data asc, r.cliente.nome asc, r.id asc
            """)
    List<TimesheetRiga> searchFilteredWithData(@Param("clienteId") Long clienteId,
                                               @Param("utenteId") Long utenteId,
                                               @Param("data") LocalDate data);

    @Query("""
            select r
            from TimesheetRiga r
            order by r.data asc, r.cliente.nome asc, r.id asc
            """)
    List<TimesheetRiga> findAllOrdered();

}
