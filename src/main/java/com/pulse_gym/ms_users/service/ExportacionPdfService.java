package com.pulse_gym.ms_users.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.pulse_gym.lb_common.dto.DetalleRutinaExportacionDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalExportacionDTO;
import com.pulse_gym.lb_common.dto.RutinaExportacionDTO;
import com.pulse_gym.lb_common.dto.SugerenciaComidaExportacionDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportacionPdfService {

    /** Color primario azul corporativo */
    private static final Color COLOR_PRIMARIO = new DeviceRgb(0, 102, 204);

    /** Color secundario para fondos */
    private static final Color COLOR_SECUNDARIO = new DeviceRgb(240, 245, 250);

    /** Color para bordes */
    private static final Color COLOR_BORDE = new DeviceRgb(200, 200, 200);

    /**
     * Exporta una rutina a formato PDF
     * 
     * @param rutina DTO con los datos de la rutina a exportar
     * @return Array de bytes del PDF generado
     * @throws IOException Si ocurre un error al generar el PDF
     */
    public byte[] exportarRutinaPdf(RutinaExportacionDTO rutina) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont fontTitulo = PdfFontFactory.createFont();
        PdfFont fontSubtitulo = PdfFontFactory.createFont();
        PdfFont fontNormal = PdfFontFactory.createFont();

        Paragraph titulo = new Paragraph("PULSE GYM - RUTINA DE ENTRENAMIENTO")
                .setFont(fontTitulo)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_PRIMARIO);
        document.add(titulo);

        document.add(new Paragraph(" ").setBorderBottom(Border.NO_BORDER));

        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 2 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        Cell cellLabel = new Cell();
        cellLabel.add(new Paragraph("Socio:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Email:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Fecha:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Versión:").setBold().setFont(fontNormal));
        cellLabel.setBorder(Border.NO_BORDER);
        cellLabel.setWidth(UnitValue.createPercentValue(30));
        headerTable.addCell(cellLabel);

        Cell cellValue = new Cell();
        cellValue.add(new Paragraph(rutina.getNombreSocio() + " " + rutina.getApellidoSocio()));
        cellValue.add(new Paragraph(rutina.getEmailSocio()));
        cellValue.add(new Paragraph(rutina.getFechaGeneracion()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        cellValue.add(new Paragraph(rutina.getVersion() != null ? "v" + rutina.getVersion() : "v1"));
        cellValue.setBorder(Border.NO_BORDER);
        headerTable.addCell(cellValue);

        document.add(headerTable);

        if (rutina.getModificadoPor() != null && !rutina.getModificadoPor().isEmpty()) {
            document.add(new Paragraph(" ")
                    .setBorderBottom(Border.NO_BORDER)
                    .setMarginTop(0));

            Table modTable = new Table(UnitValue.createPercentArray(new float[] { 1, 3 }))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            Cell modLabel = new Cell();
            modLabel.add(new Paragraph("Última modificación:").setBold().setFontSize(10));
            modLabel.setBorder(Border.NO_BORDER);
            modLabel.setWidth(UnitValue.createPercentValue(25));
            modTable.addCell(modLabel);

            String modInfo = "Por: " + rutina.getModificadoPor();
            if (rutina.getFechaModificacion() != null) {
                modInfo += " | Fecha: " + rutina.getFechaModificacion()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            if (rutina.getMotivoModificacion() != null && !rutina.getMotivoModificacion().isEmpty()) {
                modInfo += " | Motivo: " + rutina.getMotivoModificacion();
            }
            Cell modValue = new Cell();
            modValue.add(new Paragraph(modInfo).setFontSize(10).setFontColor(new DeviceRgb(80, 80, 80)));
            modValue.setBorder(Border.NO_BORDER);
            modTable.addCell(modValue);

            document.add(modTable);
        } else {
            document.add(new Paragraph(" ")
                    .setBorderBottom(Border.NO_BORDER)
                    .setMarginTop(0));

            Table modTable = new Table(UnitValue.createPercentArray(new float[] { 1, 3 }))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            Cell modLabel = new Cell();
            modLabel.add(new Paragraph("Estado:").setBold().setFontSize(10));
            modLabel.setBorder(Border.NO_BORDER);
            modLabel.setWidth(UnitValue.createPercentValue(25));
            modTable.addCell(modLabel);

            Cell modValue = new Cell();
            modValue.add(new Paragraph("Rutina original - Sin modificaciones").setFontSize(10)
                    .setFontColor(new DeviceRgb(80, 80, 80)));
            modValue.setBorder(Border.NO_BORDER);
            modTable.addCell(modValue);

            document.add(modTable);
        }

        document.add(new Paragraph("DESCRIPCIÓN")
                .setFont(fontSubtitulo)
                .setFontSize(14)
                .setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setMarginTop(10));

        document.add(new Paragraph(
                rutina.getDescripcion() != null ? rutina.getDescripcion() : "Sin descripción")
                .setFont(fontNormal)
                .setFontSize(11)
                .setMarginBottom(10));

        if (rutina.getExplicacionIA() != null && !rutina.getExplicacionIA().isEmpty()) {
            document.add(new Paragraph("EXPLICACIÓN DE LA IA")
                    .setFont(fontSubtitulo)
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(COLOR_PRIMARIO)
                    .setMarginTop(10));

            document.add(new Paragraph(rutina.getExplicacionIA())
                    .setFont(fontNormal)
                    .setFontSize(11)
                    .setItalic()
                    .setMarginBottom(10));
        }

        document.add(new Paragraph("EJERCICIOS")
                .setFont(fontSubtitulo)
                .setFontSize(14)
                .setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setMarginTop(15));

        float[] columnWidths = { 0.5f, 2f, 1f, 0.8f, 0.8f, 1f, 1f, 1f, 1f };
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5);

        String[] headers = { "#", "Ejercicio", "Grupo", "Día", "Series", "Reps", "Peso", "Descanso", "Notas" };
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(9))
                    .setBackgroundColor(COLOR_PRIMARIO)
                    .setFontColor(DeviceRgb.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
            table.addCell(headerCell);
        }

        if (rutina.getDetalles() != null) {
            int orden = 1;
            for (DetalleRutinaExportacionDTO detalle : rutina.getDetalles()) {
                table.addCell(createCell(String.valueOf(orden), TextAlignment.CENTER, 9));
                table.addCell(createCell(detalle.getNombreEjercicio(), TextAlignment.LEFT, 9));
                table.addCell(createCell(detalle.getGrupoMuscular(), TextAlignment.CENTER, 9));

                String dia = detalle.getDiaSemana() != null ? "Día " + detalle.getDiaSemana() : "-";
                table.addCell(createCell(dia, TextAlignment.CENTER, 9));

                table.addCell(createCell(
                        detalle.getSeries() != null ? String.valueOf(detalle.getSeries()) : "-",
                        TextAlignment.CENTER, 9));

                String reps = "";
                if (detalle.getRepeticionesMin() != null && detalle.getRepeticionesMax() != null) {
                    reps = detalle.getRepeticionesMin() + " - " + detalle.getRepeticionesMax();
                } else if (detalle.getRepeticionesMin() != null) {
                    reps = String.valueOf(detalle.getRepeticionesMin());
                } else {
                    reps = "-";
                }
                table.addCell(createCell(reps, TextAlignment.CENTER, 9));

                table.addCell(createCell(
                        detalle.getPesoSugerido() != null ? detalle.getPesoSugerido() + " kg" : "-",
                        TextAlignment.CENTER, 9));

                table.addCell(createCell(
                        detalle.getDescansoSegundos() != null ? detalle.getDescansoSegundos() + "s" : "-",
                        TextAlignment.CENTER, 9));

                table.addCell(createCell(
                        detalle.getNotas() != null && !detalle.getNotas().isEmpty() ? detalle.getNotas() : "-",
                        TextAlignment.LEFT, 8));

                orden++;
            }
        }

        document.add(table);

        document.add(new Paragraph(" ")
                .setMarginTop(20)
                .setBorderTop(Border.NO_BORDER));

        document.add(new Paragraph("Generado por PULSE GYM - " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFont(fontNormal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(128, 128, 128)));

        document.close();
        return baos.toByteArray();
    }

    /**
     * Crea una celda para la tabla del PDF
     * 
     * @param text      Texto de la celda
     * @param alignment Alineación del texto
     * @param fontSize  Tamaño de fuente
     * @return Celda formateada
     */
    private Cell createCell(String text, TextAlignment alignment, int fontSize) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-")
                        .setFontSize(fontSize))
                .setTextAlignment(alignment)
                .setPadding(4)
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(DeviceRgb.WHITE);
    }

    /**
     * Exporta un plan nutricional a formato PDF
     * 
     * @param plan DTO con los datos del plan nutricional a exportar
     * @return Array de bytes del PDF generado
     * @throws IOException Si ocurre un error al generar el PDF
     */
    public byte[] exportarPlanNutricionalPdf(PlanNutricionalExportacionDTO plan) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont fontTitulo = PdfFontFactory.createFont();
        PdfFont fontSubtitulo = PdfFontFactory.createFont();
        PdfFont fontNormal = PdfFontFactory.createFont();
        PdfFont fontBold = PdfFontFactory.createFont();

        Paragraph titulo = new Paragraph("PULSE GYM - PLAN NUTRICIONAL")
                .setFont(fontTitulo)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(COLOR_PRIMARIO);
        document.add(titulo);

        document.add(new Paragraph(" ").setBorderBottom(Border.NO_BORDER));

        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 2 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        Cell cellLabel = new Cell();
        cellLabel.add(new Paragraph("Socio:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Email:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Fecha:").setBold().setFont(fontNormal));
        cellLabel.add(new Paragraph("Versión:").setBold().setFont(fontNormal));
        cellLabel.setBorder(Border.NO_BORDER);
        cellLabel.setWidth(UnitValue.createPercentValue(30));
        headerTable.addCell(cellLabel);

        Cell cellValue = new Cell();
        cellValue.add(new Paragraph(plan.getNombreSocio() + " " + plan.getApellidoSocio()));
        cellValue.add(new Paragraph(plan.getEmailSocio()));
        cellValue.add(new Paragraph(plan.getFechaGeneracion()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        cellValue.add(new Paragraph(plan.getVersion() != null ? "v" + plan.getVersion() : "v1"));
        cellValue.setBorder(Border.NO_BORDER);
        headerTable.addCell(cellValue);

        document.add(headerTable);

        if (plan.getModificadoPor() != null && !plan.getModificadoPor().isEmpty()) {
            document.add(new Paragraph(" ")
                    .setBorderBottom(Border.NO_BORDER)
                    .setMarginTop(0));

            Table modTable = new Table(UnitValue.createPercentArray(new float[] { 1, 3 }))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            Cell modLabel = new Cell();
            modLabel.add(new Paragraph("Última modificación:").setBold().setFontSize(10));
            modLabel.setBorder(Border.NO_BORDER);
            modLabel.setWidth(UnitValue.createPercentValue(25));
            modTable.addCell(modLabel);

            String modInfo = "Por: " + plan.getModificadoPor();
            if (plan.getFechaModificacion() != null) {
                modInfo += " | Fecha: " + plan.getFechaModificacion()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            if (plan.getMotivoModificacion() != null && !plan.getMotivoModificacion().isEmpty()) {
                modInfo += " | Motivo: " + plan.getMotivoModificacion();
            }
            Cell modValue = new Cell();
            modValue.add(new Paragraph(modInfo).setFontSize(10).setFontColor(new DeviceRgb(80, 80, 80)));
            modValue.setBorder(Border.NO_BORDER);
            modTable.addCell(modValue);

            document.add(modTable);
        } else {
            document.add(new Paragraph(" ")
                    .setBorderBottom(Border.NO_BORDER)
                    .setMarginTop(0));

            Table modTable = new Table(UnitValue.createPercentArray(new float[] { 1, 3 }))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(10);

            Cell modLabel = new Cell();
            modLabel.add(new Paragraph("Estado:").setBold().setFontSize(10));
            modLabel.setBorder(Border.NO_BORDER);
            modLabel.setWidth(UnitValue.createPercentValue(25));
            modTable.addCell(modLabel);

            Cell modValue = new Cell();
            modValue.add(new Paragraph("Plan original - Sin modificaciones").setFontSize(10)
                    .setFontColor(new DeviceRgb(80, 80, 80)));
            modValue.setBorder(Border.NO_BORDER);
            modTable.addCell(modValue);

            document.add(modTable);
        }

        document.add(new Paragraph("RESUMEN NUTRICIONAL DIARIO")
                .setFont(fontSubtitulo)
                .setFontSize(14)
                .setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setMarginTop(10));

        Table resumenTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5);

        String[] resumenHeaders = { "Calorías", "Proteínas", "Carbohidratos", "Grasas" };
        for (String header : resumenHeaders) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setBold().setFontSize(10))
                    .setBackgroundColor(COLOR_PRIMARIO)
                    .setFontColor(DeviceRgb.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
            resumenTable.addCell(headerCell);
        }

        resumenTable.addCell(createCell(
                plan.getCaloriasDiarias() != null ? plan.getCaloriasDiarias() + " kcal" : "-",
                TextAlignment.CENTER, 10));
        resumenTable.addCell(createCell(
                plan.getProteinasG() != null ? String.format("%.1fg", plan.getProteinasG()) : "-",
                TextAlignment.CENTER, 10));
        resumenTable.addCell(createCell(
                plan.getCarbohidratosG() != null ? String.format("%.1fg", plan.getCarbohidratosG())
                        : "-",
                TextAlignment.CENTER, 10));
        resumenTable.addCell(createCell(
                plan.getGrasasG() != null ? String.format("%.1fg", plan.getGrasasG()) : "-",
                TextAlignment.CENTER, 10));

        document.add(resumenTable);

        if (plan.getRestriccionesDieteticas() != null && !plan.getRestriccionesDieteticas().isEmpty()) {
            document.add(new Paragraph("Restricciones: " + plan.getRestriccionesDieteticas())
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setMarginTop(5)
                    .setFontColor(new DeviceRgb(100, 100, 100)));
        }

        document.add(new Paragraph(" "));

        if (plan.getExplicacionIA() != null && !plan.getExplicacionIA().isEmpty()) {
            document.add(new Paragraph("EXPLICACIÓN DE LA IA")
                    .setFont(fontSubtitulo)
                    .setFontSize(14)
                    .setBold()
                    .setFontColor(COLOR_PRIMARIO)
                    .setMarginTop(10));

            document.add(new Paragraph(plan.getExplicacionIA())
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setItalic()
                    .setMarginBottom(10));
        }

        document.add(new Paragraph("SUGERENCIAS DE COMIDAS")
                .setFont(fontSubtitulo)
                .setFontSize(14)
                .setBold()
                .setFontColor(COLOR_PRIMARIO)
                .setMarginTop(15));

        if (plan.getSugerenciasComidas() != null && !plan.getSugerenciasComidas().isEmpty()) {
            String[] ordenComidas = { "desayuno", "colaciones", "almuerzo", "cena" };
            Map<String, String> nombresComidas = Map.of(
                    "desayuno", "Desayuno",
                    "colaciones", "Colaciones",
                    "almuerzo", "Almuerzo",
                    "cena", "Cena");

            for (String tipo : ordenComidas) {
                if (plan.getSugerenciasComidas().containsKey(tipo)) {
                    List<SugerenciaComidaExportacionDTO> comidas = plan.getSugerenciasComidas()
                            .get(tipo);
                    if (comidas != null && !comidas.isEmpty()) {
                        document.add(new Paragraph(nombresComidas.getOrDefault(tipo, tipo))
                                .setFont(fontBold)
                                .setFontSize(12)
                                .setFontColor(COLOR_PRIMARIO)
                                .setMarginTop(8));

                        for (SugerenciaComidaExportacionDTO comida : comidas) {
                            String nombreCalorias = comida.getNombre() +
                                    (comida.getCalorias() != null
                                            ? " (" + comida.getCalorias()
                                                    + " kcal)"
                                            : "");
                            document.add(new Paragraph("  • " + nombreCalorias)
                                    .setFont(fontBold)
                                    .setFontSize(10));

                            String infoNutricional = String.format(
                                    "    Proteínas: %.1fg | Carbohidratos: %.1fg | Grasas: %.1fg",
                                    comida.getProteinas() != null
                                            ? comida.getProteinas()
                                            : 0.0,
                                    comida.getCarbohidratos() != null
                                            ? comida.getCarbohidratos()
                                            : 0.0,
                                    comida.getGrasas() != null ? comida.getGrasas()
                                            : 0.0);
                            document.add(new Paragraph(infoNutricional)
                                    .setFont(fontNormal)
                                    .setFontSize(9)
                                    .setFontColor(new DeviceRgb(80, 80, 80)));

                            if (comida.getIngredientes() != null
                                    && !comida.getIngredientes().isEmpty()) {
                                document.add(new Paragraph("    📝 Ingredientes: "
                                        + comida.getIngredientes())
                                        .setFont(fontNormal)
                                        .setFontSize(9)
                                        .setFontColor(new DeviceRgb(60, 60,
                                                60)));
                            }

                            if (comida.getPreparacion() != null
                                    && !comida.getPreparacion().isEmpty()) {
                                document.add(new Paragraph("    🔪 Preparación: "
                                        + comida.getPreparacion())
                                        .setFont(fontNormal)
                                        .setFontSize(9)
                                        .setFontColor(new DeviceRgb(60, 60,
                                                60)));
                            }

                            if (comida.getDescripcion() != null
                                    && !comida.getDescripcion().isEmpty()) {
                                document.add(new Paragraph(
                                        "    " + comida.getDescripcion())
                                        .setFont(fontNormal)
                                        .setFontSize(9)
                                        .setFontColor(new DeviceRgb(60, 60,
                                                60)));
                            }

                            document.add(new Paragraph(" "));
                        }
                    }
                }
            }
        } else {
            document.add(new Paragraph("No hay sugerencias de comidas disponibles para este plan")
                    .setFont(fontNormal)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(150, 150, 150)));
        }

        document.add(new Paragraph(" ")
                .setMarginTop(20)
                .setBorderTop(Border.NO_BORDER));

        document.add(new Paragraph("Generado por PULSE GYM - " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFont(fontNormal)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(128, 128, 128)));

        document.close();
        return baos.toByteArray();
    }
}