package com.serendipity.backend.export;

import com.serendipity.backend.model.dto.TimesheetSnapshotDto;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class PdfRenderer {
    private static final String LOGO_CLASSPATH_LOCATION = "static/pdf/logo-serendipity.jpg";
    private static final float LOGO_WIDTH_POINTS = 96f;
    private static final float LOGO_MARGIN_RIGHT = 24f;
    private static final float LOGO_MARGIN_BOTTOM = 14f;
    private static final float LOGO_OPACITY = 0.12f;

    private final SpringTemplateEngine templateEngine;
    private final byte[] logoBytes;

    public PdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.logoBytes = loadLogoBytes();
    }

    public byte[] render(TimesheetSnapshotDto dto) {
        return render("pdf/timesheet-pdf", Map.of("ts", dto));
    }

    public byte[] render(String templateName, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            variables.forEach(ctx::setVariable);

            String html = templateEngine.process(templateName, ctx);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(out);
                return addLogoToPdf(out.toByteArray());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Errore durante la generazione del PDF", e);
        }
    }

    private byte[] loadLogoBytes() {
        ClassPathResource resource = new ClassPathResource(LOGO_CLASSPATH_LOCATION);
        try {
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    return StreamUtils.copyToByteArray(inputStream);
                }
            }

            Path fallbackPath = resolveLogoFallbackPath();
            try (InputStream inputStream = new FileInputStream(fallbackPath.toFile())) {
                return StreamUtils.copyToByteArray(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile caricare il logo del PDF", e);
        }
    }

    private Path resolveLogoFallbackPath() throws IOException {
        Path[] candidates = new Path[] {
                Path.of("src", "main", "resources", LOGO_CLASSPATH_LOCATION),
                Path.of("backend", "src", "main", "resources", LOGO_CLASSPATH_LOCATION)
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized) && Files.isRegularFile(normalized)) {
                return normalized;
            }
        }

        throw new IOException("Logo PDF non trovato nel classpath o nei path locali attesi");
    }

    private byte[] addLogoToPdf(byte[] pdfBytes) throws Exception {
        try (ByteArrayOutputStream stampedOut = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(pdfBytes);
            PdfStamper stamper = new PdfStamper(reader, stampedOut);

            try {
                int totalPages = reader.getNumberOfPages();
                for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                    Rectangle pageSize = reader.getPageSize(pageNumber);

                    Image logo = Image.getInstance(logoBytes);
                    logo.scaleToFit(LOGO_WIDTH_POINTS, 1000f);

                    float x = pageSize.getRight() - LOGO_MARGIN_RIGHT - logo.getScaledWidth();
                    float y = pageSize.getBottom() + LOGO_MARGIN_BOTTOM;
                    logo.setAbsolutePosition(x, y);

                    PdfContentByte canvas = stamper.getOverContent(pageNumber);
                    PdfGState gState = new PdfGState();
                    gState.setFillOpacity(LOGO_OPACITY);
                    gState.setStrokeOpacity(LOGO_OPACITY);

                    canvas.saveState();
                    canvas.setGState(gState);
                    canvas.addImage(logo);
                    canvas.restoreState();
                }
            } finally {
                stamper.close();
                reader.close();
            }

            return stampedOut.toByteArray();
        }
    }
}
