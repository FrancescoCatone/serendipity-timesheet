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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapConfigTest {

    @Mock
    UtenteRepository utenteRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    ClienteRepository clienteRepository;

    @Captor
    ArgumentCaptor<Utente> utenteCaptor;

    @Captor
    ArgumentCaptor<Cliente> clienteCaptor;

    @Test
    void bootstrapAdmin_creaAdmin_quandoRepositoryVuoto() throws Exception {
        BootstrapConfig bootstrapConfig = new BootstrapConfig(buildBootstrapAdminProperties());

        when(utenteRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("UnaPasswordSicura123!")).thenReturn("ENC_PASS");
        when(utenteRepository.save(any(Utente.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        runner.run();

        verify(utenteRepository).count();
        verify(passwordEncoder).encode("UnaPasswordSicura123!");
        verify(utenteRepository).save(utenteCaptor.capture());

        Utente saved = utenteCaptor.getValue();
        assertThat(saved.getCodiceFiscale()).isEqualTo("RSSMRA80A01H501U");
        assertThat(saved.getNome()).isEqualTo("Mario");
        assertThat(saved.getCognome()).isEqualTo("Rossi");
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getPassword()).isEqualTo("ENC_PASS");
        assertThat(saved.getRuolo()).isEqualTo(Ruolo.ADMIN);
    }

    @Test
    void bootstrapAdmin_nonFaNulla_quandoRepositoryNonVuoto() throws Exception {
        BootstrapConfig bootstrapConfig = new BootstrapConfig(buildBootstrapAdminProperties());

        when(utenteRepository.count()).thenReturn(5L);

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        runner.run();

        verify(utenteRepository).count();
        verifyNoMoreInteractions(utenteRepository, passwordEncoder);
    }

    @Test
    void bootstrapAdmin_nonFaNulla_quandoDisabilitato() throws Exception {
        AppBootstrapAdminProperties properties = new AppBootstrapAdminProperties();
        properties.setEnabled(false);

        BootstrapConfig bootstrapConfig = new BootstrapConfig(properties);
        when(utenteRepository.count()).thenReturn(0L);

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        runner.run();

        verify(utenteRepository).count();
        verify(utenteRepository, never()).save(any(Utente.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapAdmin_lanciaEccezione_quandoConfigurazioneIncompleta() {
        AppBootstrapAdminProperties properties = new AppBootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setEmail("admin@example.com");

        BootstrapConfig bootstrapConfig = new BootstrapConfig(properties);
        when(utenteRepository.count()).thenReturn(0L);

        CommandLineRunner runner = bootstrapConfig.bootstrapAdmin(utenteRepository, passwordEncoder);

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bootstrap admin abilitato ma configurazione incompleta");
    }

    @Test
    void bootstrapSystemClients_creaNonLavorato_quandoAssente() throws Exception {
        BootstrapConfig bootstrapConfig = new BootstrapConfig(new AppBootstrapAdminProperties());

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
        BootstrapConfig bootstrapConfig = new BootstrapConfig(new AppBootstrapAdminProperties());

        Cliente existing = new Cliente();
        existing.setNome(SystemClienti.NON_LAVORATO);
        when(clienteRepository.findByNomeIgnoreCase(SystemClienti.NON_LAVORATO)).thenReturn(Optional.of(existing));

        CommandLineRunner runner = bootstrapConfig.bootstrapSystemClients(clienteRepository);

        runner.run();

        verify(clienteRepository).findByNomeIgnoreCase(SystemClienti.NON_LAVORATO);
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    private AppBootstrapAdminProperties buildBootstrapAdminProperties() {
        AppBootstrapAdminProperties properties = new AppBootstrapAdminProperties();
        properties.setEnabled(true);
        properties.setCodiceFiscale("RSSMRA80A01H501U");
        properties.setNome("Mario");
        properties.setCognome("Rossi");
        properties.setEmail("admin@example.com");
        properties.setPassword("UnaPasswordSicura123!");
        return properties;
    }
}
