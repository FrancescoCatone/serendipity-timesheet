package com.serendipity.backend.repository;

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

    @Query("select distinct tr.data from TimesheetRiga tr where tr.timesheet.id = :timesheetId")
    List<LocalDate> findDistinctDateByTimesheetId(@Param("timesheetId") Long timesheetId);
}
