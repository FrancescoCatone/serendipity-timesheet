package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.TimesheetRiga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimesheetRigaRepository extends JpaRepository<TimesheetRiga, Long> {

}
