package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.TimesheetRiga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimesheetRigaRepository extends JpaRepository<TimesheetRiga, Long> {
    List<TimesheetRiga> findByTimesheetId(Long timesheetId);
}
