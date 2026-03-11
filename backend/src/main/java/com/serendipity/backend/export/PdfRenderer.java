package com.serendipity.backend.export;

import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

@Component
public class PdfRenderer {
    private final SpringTemplateEngine templateEngine;

    public PdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(TimesheetSnapshotDto dto) {
        try {
            Context ctx = new Context();
            ctx.setVariable("ts", dto);

            String html = templateEngine.process("pdf/timesheet-pdf", ctx);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la generazione del PDF", e);
        }
    }
}
