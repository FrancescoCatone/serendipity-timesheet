package com.serendipity.backend.repository;

import com.serendipity.backend.model.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    Optional<Utente> findByEmail(String email);

    Utente findByCodiceFiscale(String codiceFiscale);

}
