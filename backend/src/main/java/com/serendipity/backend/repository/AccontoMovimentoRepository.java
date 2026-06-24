package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.AccontoMovimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccontoMovimentoRepository extends JpaRepository<AccontoMovimento, Long> {

    List<AccontoMovimento> findByUtenteIdAndMeseAndAnnoOrderByCreatedAtDescIdDesc(Long utenteId, int mese, int anno);
}
