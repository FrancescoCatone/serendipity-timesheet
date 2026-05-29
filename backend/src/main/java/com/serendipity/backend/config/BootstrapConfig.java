package com.serendipity.backend.config;

import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.UtenteRepository;
import com.serendipity.backend.support.SystemClienti;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class BootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(BootstrapConfig.class);

    private final AppBootstrapAdminProperties bootstrapAdminProperties;

    public BootstrapConfig(AppBootstrapAdminProperties bootstrapAdminProperties) {
        this.bootstrapAdminProperties = bootstrapAdminProperties;
    }

    @Bean
    CommandLineRunner bootstrapAdmin(UtenteRepository utenteRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (utenteRepository.count() > 0) {
                return;
            }

            if (!bootstrapAdminProperties.isEnabled()) {
                log.warn("Bootstrap admin disabilitato e nessun utente presente nel database.");
                return;
            }

            validateBootstrapAdminProperties();

            Utente admin = new Utente();
            admin.setCodiceFiscale(bootstrapAdminProperties.getCodiceFiscale().trim());
            admin.setNome(bootstrapAdminProperties.getNome().trim());
            admin.setCognome(bootstrapAdminProperties.getCognome().trim());
            admin.setEmail(bootstrapAdminProperties.getEmail().trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(bootstrapAdminProperties.getPassword()));
            admin.setRuolo(Ruolo.ADMIN);
            utenteRepository.save(admin);

            log.info("Creato admin iniziale di bootstrap con email {}", admin.getEmail());
        };
    }

    @Bean
    CommandLineRunner bootstrapSystemClients(ClienteRepository clienteRepository) {
        return args -> {
            if (clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO).isEmpty()) {
                Cliente cliente = new Cliente();
                cliente.setNome(SystemClienti.NON_LAVORATO);
                cliente.setTariffaOraria(0);
                clienteRepository.save(cliente);
                log.info("Creato cliente di sistema {}", SystemClienti.NON_LAVORATO);
            }
        };
    }

    private void validateBootstrapAdminProperties() {
        if (!StringUtils.hasText(bootstrapAdminProperties.getCodiceFiscale())
                || !StringUtils.hasText(bootstrapAdminProperties.getNome())
                || !StringUtils.hasText(bootstrapAdminProperties.getCognome())
                || !StringUtils.hasText(bootstrapAdminProperties.getEmail())
                || !StringUtils.hasText(bootstrapAdminProperties.getPassword())) {
            throw new IllegalStateException(
                    "Bootstrap admin abilitato ma configurazione incompleta. "
                            + "Imposta APP_BOOTSTRAP_ADMIN_CODICE_FISCALE, APP_BOOTSTRAP_ADMIN_NOME, "
                            + "APP_BOOTSTRAP_ADMIN_COGNOME, APP_BOOTSTRAP_ADMIN_EMAIL e "
                            + "APP_BOOTSTRAP_ADMIN_PASSWORD."
            );
        }
    }
}
