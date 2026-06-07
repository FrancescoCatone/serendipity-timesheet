package com.serendipity.backend.service;

import com.serendipity.backend.mapper.ClienteMapper;
import com.serendipity.backend.model.dto.ClienteDto;
import com.serendipity.backend.model.dto.create.CreaClienteDto;
import com.serendipity.backend.model.entity.Cliente;
import com.serendipity.backend.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @Mock
    private AdminNotificationService notificationService;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente entity1;
    private Cliente entity2;
    private ClienteDto dto1;
    private ClienteDto dto2;

    @BeforeEach
    void init() {
        entity1 = new Cliente();
        entity1.setId(1L);
        entity1.setNome("Acme");
        entity1.setTariffaOraria(50.0);

        entity2 = new Cliente();
        entity2.setId(2L);
        entity2.setNome("Beta");
        entity2.setTariffaOraria(75.0);

        dto1 = new ClienteDto(1L, "Acme", 50.0);
        dto2 = new ClienteDto(2L, "Beta", 75.0);
    }

    // -------- findAll
    @Test
    void findAll_returnsMappedList() {
        when(clienteRepository.findAll()).thenReturn(List.of(entity1, entity2));
        when(clienteMapper.toDto(entity1)).thenReturn(dto1);
        when(clienteMapper.toDto(entity2)).thenReturn(dto2);

        List<ClienteDto> result = clienteService.findAll();

        assertEquals(2, result.size());
        assertEquals("Acme", result.get(0).nome());
        assertEquals("Beta", result.get(1).nome());
        verify(clienteRepository).findAll();
        verify(clienteMapper, times(2)).toDto(any(Cliente.class));
    }

    // -------- findById
    @Test
    void findById_found_returnsDto() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(entity1));
        when(clienteMapper.toDto(entity1)).thenReturn(dto1);

        ClienteDto result = clienteService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(clienteRepository).findById(1L);
        verify(clienteMapper).toDto(entity1);
    }

    @Test
    void findById_notFound_throwsEntityNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.findById(99L));
        verify(clienteRepository).findById(99L);
        verifyNoInteractions(clienteMapper);
    }

    // -------- create
    @Test
    void create_ok_trimsName_andSaves() {
        CreaClienteDto createDto = new CreaClienteDto();
        createDto.setNome("  Acme  ");
        createDto.setTariffaOraria(60.0);

        // Il mapper costruisce l'entità (con nome ancora con spazi),
        // poi il service normalizza con trim e la salva.
        Cliente mapped = new Cliente();
        mapped.setNome("  Acme  ");
        mapped.setTariffaOraria(60.0);

        when(clienteRepository.existsByNomeIgnoreCase("Acme")).thenReturn(false);
        when(clienteMapper.fromCreateDto(createDto)).thenReturn(mapped);

        // catturiamo cosa viene salvato per verificare il nome normalizzato
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);

        Cliente saved = new Cliente();
        saved.setId(10L);
        saved.setNome("Acme"); // atteso normalizzato
        saved.setTariffaOraria(60.0);

        when(clienteRepository.save(any(Cliente.class))).thenReturn(saved);
        when(clienteMapper.toDto(saved)).thenReturn(new ClienteDto(10L, "Acme", 60.0));

        ClienteDto result = clienteService.create(createDto);

        assertEquals("Acme", result.nome());
        assertEquals(60.0, result.tariffaOraria());

        verify(clienteRepository).existsByNomeIgnoreCase("Acme");
        verify(clienteMapper).fromCreateDto(createDto);
        verify(clienteRepository).save(captor.capture());
        assertEquals("Acme", captor.getValue().getNome(), "il nome deve essere trim-mato prima del save");
    }

    @Test
    void create_duplicateName_caseInsensitive_throwsDataIntegrity() {
        CreaClienteDto createDto = new CreaClienteDto();
        createDto.setNome("acme");
        createDto.setTariffaOraria(60.0);

        when(clienteRepository.existsByNomeIgnoreCase("acme")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> clienteService.create(createDto));
        verify(clienteRepository).existsByNomeIgnoreCase("acme");
        verifyNoMoreInteractions(clienteRepository);
        verifyNoInteractions(clienteMapper);
    }

    // -------- update
    @Test
    void update_ok_sameNameDifferentCase_doesNotTriggerUniquenessCheck_andSaves() {
        Long id = 1L;

        Cliente existing = new Cliente();
        existing.setId(id);
        existing.setNome("Acme");
        existing.setTariffaOraria(50.0);

        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome("acme"); // stesso nome ma diverso case
        dto.setTariffaOraria(65.0);

        when(clienteRepository.findById(id)).thenReturn(Optional.of(existing));
        when(clienteRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
        when(clienteMapper.toDto(existing)).thenReturn(new ClienteDto(id, "acme", 65.0));

        ClienteDto result = clienteService.update(id, dto);

        assertEquals("acme", result.nome()); // perché il service setta il nuovoNome (trim) sull'entità
        assertEquals(65.0, result.tariffaOraria());

        verify(clienteRepository).findById(id);
        verify(clienteRepository, never()).existsByNomeIgnoreCase(anyString()); // non deve controllare unicità se il nome è lo stesso (case-insensitive)
        verify(clienteRepository).save(existing);
        verify(clienteMapper).toDto(existing);
    }

    @Test
    void update_ok_changedNameUnique_saves() {
        Long id = 1L;

        Cliente existing = new Cliente();
        existing.setId(id);
        existing.setNome("Acme");
        existing.setTariffaOraria(50.0);

        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome("  Beta  "); // cambia davvero
        dto.setTariffaOraria(70.0);

        when(clienteRepository.findById(id)).thenReturn(Optional.of(existing));
        when(clienteRepository.existsByNomeIgnoreCase("Beta")).thenReturn(false);
        when(clienteRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));
        when(clienteMapper.toDto(existing)).thenReturn(new ClienteDto(id, "Beta", 70.0));

        ClienteDto result = clienteService.update(id, dto);

        assertEquals("Beta", result.nome());
        assertEquals(70.0, result.tariffaOraria());

        verify(clienteRepository).findById(id);
        verify(clienteRepository).existsByNomeIgnoreCase("Beta");
        verify(clienteRepository).save(existing);
        verify(clienteMapper).toDto(existing);
    }

    @Test
    void update_notFound_throwsEntityNotFound() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome("X");
        dto.setTariffaOraria(10.0);

        assertThrows(EntityNotFoundException.class, () -> clienteService.update(999L, dto));
        verify(clienteRepository).findById(999L);
        verifyNoMoreInteractions(clienteRepository);
        verifyNoInteractions(clienteMapper);
    }

    @Test
    void update_changedNameDuplicate_throwsDataIntegrity() {
        Long id = 1L;

        Cliente existing = new Cliente();
        existing.setId(id);
        existing.setNome("Acme");

        CreaClienteDto dto = new CreaClienteDto();
        dto.setNome("Beta"); // cambia
        dto.setTariffaOraria(80.0);

        when(clienteRepository.findById(id)).thenReturn(Optional.of(existing));
        when(clienteRepository.existsByNomeIgnoreCase("Beta")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class, () -> clienteService.update(id, dto));

        verify(clienteRepository).findById(id);
        verify(clienteRepository).existsByNomeIgnoreCase("Beta");
        verify(clienteRepository, never()).save(any());
        verifyNoInteractions(clienteMapper);
    }

    // -------- delete
    @Test
    void delete_ok_existing_deletes() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.of(entity2));

        assertDoesNotThrow(() -> clienteService.delete(2L));

        verify(clienteRepository).findById(2L);
        verify(clienteRepository).deleteById(2L);
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clienteService.delete(2L));

        verify(clienteRepository).findById(2L);
        verify(clienteRepository, never()).deleteById(anyLong());
    }

    // -------- deleghe report
    @Test
    void getDipendentiConOreTotaliPerCliente_delegatesToRepository() {
        when(clienteRepository.findDipendentiConOreTotali(1L, 10, 2025))
                .thenReturn(java.util.Collections.singletonList(
                        new Object[]{1L, "Mario", "Rossi", 120.0}
                ));

        List<Object[]> res = clienteService.getDipendentiConOreTotaliPerCliente(1L, 10, 2025);

        assertEquals(1, res.size());
        assertEquals("Mario", res.getFirst()[1]);
        verify(clienteRepository).findDipendentiConOreTotali(1L, 10, 2025);
    }

    @Test
    void getTotaleOrePerCliente_delegatesToRepository() {
        when(clienteRepository.sommaTotaleOrePerCliente(1L, 10, 2025)).thenReturn(185.5);

        double tot = clienteService.getTotaleOrePerCliente(1L, 10, 2025);

        assertEquals(185.5, tot);
        verify(clienteRepository).sommaTotaleOrePerCliente(1L, 10, 2025);
    }

// -------- aggiornaParziale

    @Test
    void aggiornaParziale_notFound_throwsEntityNotFound() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> clienteService.aggiornaParziale(999L, Map.of("nome", "X")));

        verify(clienteRepository).findById(999L);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_nome_blank_illegalArgument() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("nome", "   ")));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_nome_tooLong_illegalArgument() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        String veryLong = "A".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("nome", veryLong)));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_nome_duplicate_conflict() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(clienteRepository.existsByNomeIgnoreCase("Beta")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("nome", "  Beta  ")));

        verify(clienteRepository).findById(1L);
        verify(clienteRepository).existsByNomeIgnoreCase("Beta");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_nome_sameCaseInsensitive_noUniqCheck_andSaves() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        var resp = clienteService.aggiornaParziale(1L, Map.of("nome", "  acme  "));

        assertEquals(200, resp.getStatus());
        assertEquals("acme", c.getNome());
        verify(clienteRepository).findById(1L);
        verify(clienteRepository, never()).existsByNomeIgnoreCase(anyString());
        verify(clienteRepository).save(c);
    }

    @Test
    void aggiornaParziale_tariffa_zero_illegalArgument() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("tariffaOraria", 0)));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_tariffa_negative_illegalArgument() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("tariffaOraria", -10.0)));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_tariffa_blankString_illegalArgument() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class,
                () -> clienteService.aggiornaParziale(1L, Map.of("tariffaOraria", "   ")));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void aggiornaParziale_tariffa_fromString_ok() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        var resp = clienteService.aggiornaParziale(1L, Map.of("tariffaOraria", "72.5"));

        assertEquals(200, resp.getStatus());
        assertEquals(72.5, c.getTariffaOraria(), 0.0001);
        verify(clienteRepository).save(c);
    }

    @Test
    void aggiornaParziale_tariffa_fromNumber_ok() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        var resp = clienteService.aggiornaParziale(1L, Map.of("tariffaOraria", 99));

        assertEquals(200, resp.getStatus());
        assertEquals(99.0, c.getTariffaOraria(), 0.0001);
        verify(clienteRepository).save(c);
    }

    @Test
    void aggiornaParziale_updateNomeETariffa_ok() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Acme");
        c.setTariffaOraria(50.0);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(clienteRepository.existsByNomeIgnoreCase("Beta")).thenReturn(false);

        Map<String, Object> updates = Map.of("nome", "  Beta  ", "tariffaOraria", 80.0);
        var resp = clienteService.aggiornaParziale(1L, updates);

        assertEquals(200, resp.getStatus());
        assertEquals("Beta", c.getNome());
        assertEquals(80.0, c.getTariffaOraria(), 0.0001);
        verify(clienteRepository).existsByNomeIgnoreCase("Beta");
        verify(clienteRepository).save(c);
    }


}
