package com.serendipity.backend.service;

import com.serendipity.backend.mapper.UtenteMapper;
import com.serendipity.backend.model.dto.CreaUtenteDto;
import com.serendipity.backend.model.dto.UtenteDto;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtenteService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UtenteMapper utenteMapper;

    /**
     * Restituisce tutti gli utenti.
     *
     * @return una lista di DTO contenenti i dati di tutti gli utenti
     */
    public List<UtenteDto> getAll() {
        return utenteRepository.findAll()
                .stream()
                .map(utenteMapper::toDto)
                .toList();
    }

    /**
     * Crea un nuovo utente.
     *
     * @param dto il DTO contenente i dati dell'utente da creare
     * @return il DTO dell'utente creato
     */
    public UtenteDto creaUtente(CreaUtenteDto dto) {
        Utente utente = utenteMapper.fromCreateDto(dto);
        utente.setPassword(passwordEncoder.encode(utente.getPassword()));
        return utenteMapper.toDto(utenteRepository.save(utente));
    }

    /**
     * Trova un utente per codice fiscale.
     *
     * @param codiceFiscale il codice fiscale dell'utente
     * @return il DTO dell'utente corrispondente al codice fiscale
     */
    public UtenteDto findByCodiceFiscale(String codiceFiscale) {
        Utente utente = utenteRepository.findByCodiceFiscale(codiceFiscale);
        return utenteMapper.toDto(utente);
    }
}
