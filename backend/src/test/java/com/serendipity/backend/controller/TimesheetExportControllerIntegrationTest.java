package com.serendipity.backend.controller;

import com.serendipity.backend.service.TimesheetExportService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TimesheetExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimesheetExportService exportService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void export_ok_admin_receivesPdfWithFilename() throws Exception {
        long id = 42L;
        byte[] pdf = new byte[]{1, 2, 3, 4};
        String filename = "mario_rossi_maggio_2025.pdf";
        when(exportService.export(id)).thenReturn(new TimesheetExportService.ExportFile(pdf, filename));

        mockMvc.perform(get("/api/timesheets/{id}/export", id))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"" + filename + "\"")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        verify(exportService).export(id);
    }

    @Test
    @WithMockUser(username = "user", roles = {"DIPENDENTE"})
    void export_forbidden_dipendente_notAllowed() throws Exception {
        mockMvc.perform(get("/api/timesheets/{id}/export", 7L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(exportService);
    }

    @Test
    void export_unauthenticated_forbidden() throws Exception {
        mockMvc.perform(get("/api/timesheets/{id}/export", 1L))
                .andExpect(status().isForbidden());
        verifyNoInteractions(exportService);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void export_notFound_404_fromService() throws Exception {
        when(exportService.export(999L)).thenThrow(new EntityNotFoundException("Timesheet non trovato"));

        mockMvc.perform(get("/api/timesheets/{id}/export", 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Timesheet non trovato"));

        verify(exportService).export(999L);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void export_illegalState_409_fromService() throws Exception {
        when(exportService.export(10L))
                .thenThrow(new IllegalStateException("Puoi esportare il timesheet solo quando è nello stato CHIUSO"));

        mockMvc.perform(get("/api/timesheets/{id}/export", 10L))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Puoi esportare il timesheet solo quando è nello stato CHIUSO"));

        verify(exportService).export(10L);
    }
}
