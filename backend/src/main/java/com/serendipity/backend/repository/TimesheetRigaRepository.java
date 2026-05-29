package com.serendipity.backend.repository;

import com.serendipity.backend.model.dto.TotaleClienteDto;
import com.serendipity.backend.model.dto.TotaliDto;
import com.serendipity.backend.model.dto.report.ReportClienteDipendenteDto;
import com.serendipity.backend.model.dto.report.ReportClienteGiornoDipendenteDto;
import com.serendipity.backend.model.dto.report.ReportDipendenteClienteDto;
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
              and lower(r.cliente.nome) <> lower(:excludedClienteNome)
            group by r.cliente.id, r.cliente.nome
            order by r.cliente.nome
            """)
    List<TotaleClienteDto> sumTotaliPerCliente(@Param("tsId") Long timesheetId,
                                               @Param("excludedClienteNome") String excludedClienteNome);

    @Query("""
            select distinct r.data
            from TimesheetRiga r
            where r.timesheet.id = :timesheetId
            """)
    List<LocalDate> findDistinctDatesByTimesheetId(@Param("timesheetId") Long timesheetId);

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

    @Query("""
            select new com.serendipity.backend.model.dto.report.ReportClienteDipendenteDto(
                r.timesheet.utente.id,
                r.timesheet.utente.nome,
                r.timesheet.utente.cognome,
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.cliente.id = :clienteId
              and (:anno is null or r.timesheet.anno = :anno)
              and (:mese is null or r.timesheet.mese = :mese)
            group by r.timesheet.utente.id, r.timesheet.utente.nome, r.timesheet.utente.cognome
            order by r.timesheet.utente.cognome asc, r.timesheet.utente.nome asc
            """)
    List<ReportClienteDipendenteDto> reportClientePerDipendente(@Param("clienteId") Long clienteId,
                                                                @Param("mese") Integer mese,
                                                                @Param("anno") Integer anno);

    @Query("""
            select new com.serendipity.backend.model.dto.TotaliDto(
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.cliente.id = :clienteId
              and (:anno is null or r.timesheet.anno = :anno)
              and (:mese is null or r.timesheet.mese = :mese)
            """)
    TotaliDto totaleReportCliente(@Param("clienteId") Long clienteId,
                                  @Param("mese") Integer mese,
                                  @Param("anno") Integer anno);

    @Query("""
            select new com.serendipity.backend.model.dto.report.ReportClienteGiornoDipendenteDto(
                r.timesheet.utente.id,
                r.timesheet.utente.nome,
                r.timesheet.utente.cognome,
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.cliente.id = :clienteId
              and r.data = :data
            group by r.timesheet.utente.id, r.timesheet.utente.nome, r.timesheet.utente.cognome
            order by r.timesheet.utente.cognome asc, r.timesheet.utente.nome asc
            """)
    List<ReportClienteGiornoDipendenteDto> reportClientePerDipendenteByDate(@Param("clienteId") Long clienteId,
                                                                             @Param("data") LocalDate data);

    @Query("""
            select new com.serendipity.backend.model.dto.TotaliDto(
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.cliente.id = :clienteId
              and r.data = :data
            """)
    TotaliDto totaleReportClienteByDate(@Param("clienteId") Long clienteId,
                                        @Param("data") LocalDate data);

    @Query("""
            select new com.serendipity.backend.model.dto.report.ReportDipendenteClienteDto(
                r.cliente.id,
                r.cliente.nome,
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.timesheet.utente.id = :utenteId
              and (:anno is null or r.timesheet.anno = :anno)
              and (:mese is null or r.timesheet.mese = :mese)
              and lower(r.cliente.nome) <> lower(:excludedClienteNome)
            group by r.cliente.id, r.cliente.nome
            order by r.cliente.nome asc
            """)
    List<ReportDipendenteClienteDto> reportDipendentePerCliente(@Param("utenteId") Long utenteId,
                                                                @Param("mese") Integer mese,
                                                                @Param("anno") Integer anno,
                                                                @Param("excludedClienteNome") String excludedClienteNome);

    @Query("""
            select new com.serendipity.backend.model.dto.TotaliDto(
                coalesce(sum(r.orario), 0),
                coalesce(sum(r.costoOrario), 0)
            )
            from TimesheetRiga r
            where r.timesheet.utente.id = :utenteId
              and (:anno is null or r.timesheet.anno = :anno)
              and (:mese is null or r.timesheet.mese = :mese)
              and lower(r.cliente.nome) <> lower(:excludedClienteNome)
            """)
    TotaliDto totaleReportDipendente(@Param("utenteId") Long utenteId,
                                     @Param("mese") Integer mese,
                                     @Param("anno") Integer anno,
                                     @Param("excludedClienteNome") String excludedClienteNome);

}
