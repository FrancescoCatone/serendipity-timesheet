package com.serendipity.backend.config;

import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.UtenteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfig {

    @Bean
    CommandLineRunner bootstrapAdmin(UtenteRepository utenteRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (utenteRepository.count() == 0) {
                Utente admin = new Utente();
                admin.setCodiceFiscale("ADMNTST85T10A562Z");
                admin.setNome("Admin");
                admin.setCognome("Test");
                admin.setEmail("admin@serendipity.com");
                admin.setPassword(passwordEncoder.encode("AdminTest123!"));
                admin.setRuolo(Ruolo.ADMIN);
                utenteRepository.save(admin);
                System.out.println(">>> Bootstrap: creato ADMIN admin@serendipity.com / AdminTest123!");
            }
        };
    }
}
