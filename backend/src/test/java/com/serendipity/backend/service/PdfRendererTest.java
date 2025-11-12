package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PdfRendererTest {

    @Test
    void render_ok_returnsValidPdfBytes() {
        // TemplateEngine mock: ritorna HTML minimale (è sufficiente per ITextRenderer)
        SpringTemplateEngine engine = Mockito.mock(SpringTemplateEngine.class);
        String html = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml" lang="it">
                  <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                    <title>Test</title>
                  </head>
                  <body><p>PDF test</p></body>
                </html>
                """;
        when(engine.process(eq("pdf/timesheet-pdf"), any(Context.class))).thenReturn(html);

        PdfRenderer renderer = new PdfRenderer(engine);

        TimesheetSnapshotDto dto = new TimesheetSnapshotDto(
                "Mario Rossi",
                "Maggio",
                2025,
                LocalDateTime.now(),
                List.of(new TimesheetSnapshotDto.Riga(
                        "Acme",
                        LocalDate.of(2025, 5, 10),
                        1, 30,
                        new BigDecimal("1.50"),
                        new BigDecimal("45.00")
                )),
                new BigDecimal("1.50"),
                new BigDecimal("45.00")
        );

        byte[] out = renderer.render(dto);

        assertThat(out).isNotEmpty();
        // convenzione PDF: inizia con "%PDF"
        assertThat(out.length).isGreaterThan(4);
        assertThat((char) out[0]).isEqualTo('%');
        assertThat((char) out[1]).isEqualTo('P');
        assertThat((char) out[2]).isEqualTo('D');
        assertThat((char) out[3]).isEqualTo('F');
    }

    @Test
    void render_templateFailure_wrapsInIllegalState() {
        SpringTemplateEngine engine = Mockito.mock(SpringTemplateEngine.class);
        when(engine.process(eq("pdf/timesheet-pdf"), any(Context.class)))
                .thenThrow(new RuntimeException("boom"));

        PdfRenderer renderer = new PdfRenderer(engine);

        TimesheetSnapshotDto dto = new TimesheetSnapshotDto(
                "X", "Y", 2025, LocalDateTime.now(), List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO
        );

        assertThatThrownBy(() -> renderer.render(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Errore durante la generazione del PDF");
    }
}
