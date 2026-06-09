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
import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfRenderer {
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
        try {
            Context ctx = new Context();
            ctx.setVariable("ts", dto);

            String html = templateEngine.process("pdf/timesheet-pdf", ctx);

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
        ClassPathResource resource = new ClassPathResource("static/pdf/logo-serendipity.jpg");
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToByteArray(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile caricare il logo del PDF", e);
        }
    }

    private byte[] addLogoToPdf(byte[] pdfBytes) throws Exception {
        try (ByteArrayOutputStream stampedOut = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(pdfBytes);
            PdfStamper stamper = new PdfStamper(reader, stampedOut);

            try {
                int targetPage = reader.getNumberOfPages();
                Rectangle pageSize = reader.getPageSize(targetPage);

                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(LOGO_WIDTH_POINTS, 1000f);

                float x = pageSize.getRight() - LOGO_MARGIN_RIGHT - logo.getScaledWidth();
                float y = pageSize.getBottom() + LOGO_MARGIN_BOTTOM;
                logo.setAbsolutePosition(x, y);

                PdfContentByte canvas = stamper.getOverContent(targetPage);
                PdfGState gState = new PdfGState();
                gState.setFillOpacity(LOGO_OPACITY);
                gState.setStrokeOpacity(LOGO_OPACITY);

                canvas.saveState();
                canvas.setGState(gState);
                canvas.addImage(logo);
                canvas.restoreState();
            } finally {
                stamper.close();
                reader.close();
            }

            return stampedOut.toByteArray();
        }
    }
}
