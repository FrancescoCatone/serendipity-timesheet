package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.export.ReportClientePdfAssembler;
import com.serendipity.backend.model.dto.report.ReportClientePdfAnnualSnapshotDto;
import com.serendipity.backend.model.dto.report.ReportClientePdfSnapshotDto;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private ReportClientePdfAssembler assembler;

    @Mock
    private PdfRenderer renderer;

    @InjectMocks
    private ReportExportService service;

    @Test
    void exportCliente_ok_generatesPdfAndFilename() {
        ReportClientePdfSnapshotDto snapshot = new ReportClientePdfSnapshotDto(
                10L,
                "Acme S.p.A.",
                3,
                "marzo",
                2026,
                LocalDateTime.now(),
                List.of(),
                new BigDecimal("12.50"),
                new BigDecimal("250.00"),
                false
        );

        when(assembler.build(10L, 3, 2026, false)).thenReturn(snapshot);
        when(renderer.render(eq("pdf/report-cliente-pdf"), anyMap())).thenReturn(new byte[]{1, 2, 3});

        ReportExportService.ExportFile out = service.exportCliente(10L, 3, 2026, false);

        assertThat(out.content()).isEqualTo(new byte[]{1, 2, 3});
        assertThat(out.filename()).isEqualTo("report_cliente_acme_spa_marzo_2026.pdf");
    }

    @Test
    void exportCliente_invalidMese_throws() {
        assertThatThrownBy(() -> service.exportCliente(10L, 13, 2026, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mese");
    }

    @Test
    void exportCliente_invalidAnno_throws() {
        assertThatThrownBy(() -> service.exportCliente(10L, 3, 1999, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anno");
    }

    @Test
    void exportClienteAnnuale_ok_generatesPdfAndFilename() {
        ReportClientePdfAnnualSnapshotDto snapshot = new ReportClientePdfAnnualSnapshotDto(
                10L,
                "Acme S.p.A.",
                2026,
                LocalDateTime.now(),
                List.of(),
                new BigDecimal("125.50"),
                new BigDecimal("2510.00"),
                false
        );

        when(assembler.buildAnnual(10L, 2026, false)).thenReturn(snapshot);
        when(renderer.render(eq("pdf/report-cliente-annuale-pdf"), anyMap())).thenReturn(new byte[]{7, 8, 9});

        ReportExportService.ExportFile out = service.exportCliente(10L, null, 2026, false);

        assertThat(out.content()).isEqualTo(new byte[]{7, 8, 9});
        assertThat(out.filename()).isEqualTo("report_cliente_acme_spa_2026.pdf");
    }

    @Test
    void exportCliente_notFound_propagates() {
        when(assembler.build(99L, 3, 2026, false)).thenThrow(new EntityNotFoundException("Cliente non trovato"));

        assertThatThrownBy(() -> service.exportCliente(99L, 3, 2026, false))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente non trovato");
    }

    @Test
    void exportCliente_emptyPdf_throws() {
        ReportClientePdfSnapshotDto snapshot = new ReportClientePdfSnapshotDto(
                10L,
                "Acme S.p.A.",
                3,
                "marzo",
                2026,
                LocalDateTime.now(),
                List.of(),
                BigDecimal.ONE,
                BigDecimal.ONE,
                false
        );

        when(assembler.build(10L, 3, 2026, false)).thenReturn(snapshot);
        when(renderer.render(eq("pdf/report-cliente-pdf"), anyMap())).thenReturn(new byte[0]);

        assertThatThrownBy(() -> service.exportCliente(10L, 3, 2026, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PDF");
    }
}
