package com.serendipity.backend.export;

import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Component
public class PdfRenderer {
    private final SpringTemplateEngine templateEngine;
    private final String logoDataUri;

    public PdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.logoDataUri = loadLogoDataUri();
    }

    public byte[] render(TimesheetSnapshotDto dto) {
        try {
            Context ctx = new Context();
            ctx.setVariable("ts", dto);
            ctx.setVariable("logoDataUri", logoDataUri);

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

    private String loadLogoDataUri() {
        ClassPathResource resource = new ClassPathResource("static/pdf/logo-serendipity.jpg");
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile caricare il logo del PDF", e);
        }
    }
}
