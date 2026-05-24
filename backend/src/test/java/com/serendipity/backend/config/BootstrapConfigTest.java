package com.serendipity.backend.config;

import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.model.entity.Utente;
import com.serendipity.backend.model.enums.Ruolo;
import com.serendipity.backend.repository.ClienteRepository;
import com.serendipity.backend.repository.UtenteRepository;
import com.serendipity.backend.support.SystemClienti;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BootstrapConfigTest {

    @Mock
    UtenteRepository utenteRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    BootstrapConfig bootstrapConfig; // istanzia la config “pura” (niente Spring context)

    @Captor
    ArgumentCaptor<Utente> utenteCaptor;

    @Captor
    ArgumentCaptor<Cliente> clienteCaptor;

    @Test
    void bootstrapAdmin_creaAdmin_quandoRepositoryVuoto() throws Exception {
        // arrange
        when(utenteRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("AdminTest123!")).thenReturn("ENC_PASS");
        // evita NullPointer su save: ritorna lo stesso entity settandogli un id simulato
        when(utenteRepository.save(any(Utente.class))).thenAnswer(inv -> {
            Utente u = inv.getArgument(0);
            // opzionale: simulare id generato
            return u;
        });

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        // act
        runner.run(); // esegue il lambda

        // assert
        verify(utenteRepository).count();
        verify(passwordEncoder).encode("AdminTest123!");
        verify(utenteRepository).save(utenteCaptor.capture());

        Utente saved = utenteCaptor.getValue();
        assertThat(saved.getCodiceFiscale()).isEqualTo("ADMNTST85T10A562Z");
        assertThat(saved.getNome()).isEqualTo("Admin");
        assertThat(saved.getCognome()).isEqualTo("Test");
        assertThat(saved.getEmail()).isEqualTo("admin@serendipity.com");
        assertThat(saved.getPassword()).isEqualTo("ENC_PASS");
        assertThat(saved.getRuolo()).isEqualTo(Ruolo.ADMIN);
    }

    @Test
    void bootstrapAdmin_nonFaNulla_quandoRepositoryNonVuoto() throws Exception {
        // arrange
        when(utenteRepository.count()).thenReturn(5L);

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        // act
        runner.run();

        // assert
        verify(utenteRepository).count();
        verifyNoMoreInteractions(utenteRepository, passwordEncoder);
    }

    @Test
    void bootstrapSystemClients_creaNonLavorato_quandoAssente() throws Exception {
        when(clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandLineRunner runner = bootstrapConfig.bootstrapSystemClients(clienteRepository);

        runner.run();

        verify(clienteRepository).findByNomeIgnoreCase(SystemClienti.NON_LAVORATO);
        verify(clienteRepository).save(clienteCaptor.capture());

        Cliente saved = clienteCaptor.getValue();
        assertThat(saved.getNome()).isEqualTo(SystemClienti.NON_LAVORATO);
        assertThat(saved.getTariffaOraria()).isZero();
    }

    @Test
    void bootstrapSystemClients_nonFaNulla_quandoGiaPresente() throws Exception {
        Cliente existing = new Cliente();
        existing.setNome(SystemClienti.NON_LAVORATO);
        when(clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO)).thenReturn(Optional.of(existing));

        CommandLineRunner runner = bootstrapConfig.bootstrapSystemClients(clienteRepository);

        runner.run();

        verify(clienteRepository).findByNomeIgnoreCase(SystemClienti.NON_LAVORATO);
        verify(clienteRepository, never()).save(any(Cliente.class));
    }
}
