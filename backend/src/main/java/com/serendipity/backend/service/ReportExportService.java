package com.serendipity.backend.service;

import com.serendipity.backend.export.PdfRenderer;
import com.serendipity.backend.export.ReportClientePdfAssembler;
import com.serendipity.backend.model.dto.report.ReportClientePdfAnnualSnapshotDto;
import com.serendipity.backend.model.dto.report.ReportClientePdfSnapshotDto;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportExportService {

    private final ReportClientePdfAssembler reportClientePdfAssembler;
    private final PdfRenderer pdfRenderer;

    public ReportExportService(ReportClientePdfAssembler reportClientePdfAssembler,
                               PdfRenderer pdfRenderer) {
        this.reportClientePdfAssembler = reportClientePdfAssembler;
        this.pdfRenderer = pdfRenderer;
    }

    public ExportFile exportCliente(Long clienteId, Integer mese, int anno, boolean mostraCosto) {
        if (anno < 2000) {
            throw new IllegalArgumentException("L'anno deve essere maggiore o uguale a 2000");
        }

        if (mese == null) {
            ReportClientePdfAnnualSnapshotDto snapshot = reportClientePdfAssembler.buildAnnual(clienteId, anno, mostraCosto);
            byte[] pdf = pdfRenderer.render("pdf/report-cliente-annuale-pdf", Map.of("report", snapshot));
            if (pdf == null || pdf.length == 0) {
                throw new IllegalStateException("Errore durante la generazione del PDF");
            }

            return new ExportFile(pdf, buildAnnualFilename(snapshot));
        }

        if (mese < 1 || mese > 12) {
            throw new IllegalArgumentException("Il mese deve essere compreso tra 1 e 12");
        }

        ReportClientePdfSnapshotDto snapshot = reportClientePdfAssembler.build(clienteId, mese, anno, mostraCosto);
        byte[] pdf = pdfRenderer.render("pdf/report-cliente-pdf", Map.of("report", snapshot));
        if (pdf == null || pdf.length == 0) {
            throw new IllegalStateException("Errore durante la generazione del PDF");
        }

        return new ExportFile(pdf, buildFilename(snapshot));
    }

    private String buildFilename(ReportClientePdfSnapshotDto snapshot) {
        String meseNome = Month.of(snapshot.mese()).getDisplayName(TextStyle.FULL, Locale.ITALIAN);
        String base = "report_cliente_" + snapshot.clienteNome() + "_" + meseNome + "_" + snapshot.anno();

        return normalizeFilename(base) + ".pdf";
    }

    private String buildAnnualFilename(ReportClientePdfAnnualSnapshotDto snapshot) {
        return normalizeFilename("report_cliente_" + snapshot.clienteNome() + "_" + snapshot.anno()) + ".pdf";
    }

    private String normalizeFilename(String base) {
        return Normalizer.normalize(base.toLowerCase(Locale.ITALIAN).replace(' ', '_'), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9_\\-]", "");
    }

    public record ExportFile(byte[] content, String filename) {
    }
}
