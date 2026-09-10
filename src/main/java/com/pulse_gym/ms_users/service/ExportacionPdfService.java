package com.pulse_gym.ms_users.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
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

    /** Color primario azul corporativo moderno (Pulse Gym) */
    private static final Color COLOR_PRIMARIO = new DeviceRgb(15, 23, 42);

    /** Color de acento para cabeceras y títulos destacados */
    private static final Color COLOR_ACENTO = new DeviceRgb(0, 102, 204);

    /** Color secundario para fondos de tarjetas */
    private static final Color COLOR_SECUNDARIO = new DeviceRgb(248, 250, 252);

    /** Color para bordes sutiles estilo UI */
    private static final Color COLOR_BORDE = new DeviceRgb(226, 232, 240);

    /**
     * Exporta una rutina a formato PDF con un diseño moderno tipo Dashboard Web
     */
    public byte[] exportarRutinaPdf(RutinaExportacionDTO rutina) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(30, 30, 30, 30);

        PdfFont fontNormal = PdfFontFactory.createFont();

        Table mainHeader = new Table(new float[] { 1f, 3.5f, 2f })
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER);
        try {
            String logoPath = getClass().getResource("/images/logo_sin_fondo_dos.png").toString();
            ImageData imageData = ImageDataFactory.create(logoPath);
            Image logo = new Image(imageData);
            logo.scaleToFit(100, 100);
            logoCell.add(logo);
        } catch (Exception e) {
            logoCell.add(new Paragraph("[PULSE GYM]").setFont(fontNormal).setFontSize(9).setBold()
                    .setFontColor(COLOR_ACENTO));
            log.warn("No se pudo cargar el logo de Pulse Gym: {}", e.getMessage());
        }
        mainHeader.addCell(logoCell);

        Cell titleCell = new Cell().setBorder(Border.NO_BORDER);
        titleCell.add(
                new Paragraph("PULSE GYM").setFont(fontNormal).setFontSize(16).setBold().setFontColor(COLOR_PRIMARIO));
        titleCell.add(
                new Paragraph(rutina.getNombre() != null ? rutina.getNombre().toUpperCase() : "RUTINA DE ENTRENAMIENTO")
                        .setFont(fontNormal).setFontSize(9).setBold().setFontColor(COLOR_ACENTO));
        mainHeader.addCell(titleCell);

        Cell metaCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        metaCell.add(new Paragraph("Versión: " + (rutina.getVersion() != null ? "v" + rutina.getVersion() : "v1"))
                .setFont(fontNormal).setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO));
        metaCell.add(new Paragraph(rutina.getFechaGeneracion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(fontNormal).setFontSize(8).setFontColor(new DeviceRgb(100, 116, 139)));
        mainHeader.addCell(metaCell);

        document.add(mainHeader);

        Table infoCard = new Table(new float[] { 1f, 1f })
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        Cell socioBox = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(new SolidBorder(COLOR_BORDE, 1))
                .setPadding(8);
        socioBox.add(new Paragraph("SOCIO ASIGNADO").setFont(fontNormal).setFontSize(8).setBold()
                .setFontColor(COLOR_ACENTO));
        socioBox.add(new Paragraph(rutina.getNombreSocio() + " " + rutina.getApellidoSocio()).setFont(fontNormal)
                .setFontSize(9).setBold());
        socioBox.add(new Paragraph(rutina.getEmailSocio()).setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139)));
        infoCard.addCell(socioBox);

        Cell auditBox = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(new SolidBorder(COLOR_BORDE, 1))
                .setPadding(8);
        auditBox.add(new Paragraph("ESTADO Y MODIFICACIÓN").setFont(fontNormal).setFontSize(8).setBold()
                .setFontColor(COLOR_ACENTO));
        if (rutina.getModificadoPor() != null && !rutina.getModificadoPor().isEmpty()) {
            auditBox.add(new Paragraph("Modificado por: " + rutina.getModificadoPor()).setFont(fontNormal)
                    .setFontSize(8.5f));
            auditBox.add(new Paragraph("Motivo: "
                    + (rutina.getMotivoModificacion() != null ? rutina.getMotivoModificacion() : "Ajuste manual"))
                    .setFont(fontNormal).setFontSize(8).setFontColor(new DeviceRgb(100, 116, 139)));
        } else {
            auditBox.add(new Paragraph("Rutina original generada por IA").setFont(fontNormal).setFontSize(8.5f));
            auditBox.add(new Paragraph("Sin modificaciones posteriores").setFont(fontNormal).setFontSize(8)
                    .setFontColor(new DeviceRgb(100, 116, 139)));
        }
        infoCard.addCell(auditBox);

        document.add(infoCard);

        if (rutina.getDescripcion() != null && !rutina.getDescripcion().isEmpty()) {
            document.add(crearTarjetaNota("DESCRIPCIÓN DE LA RUTINA", rutina.getDescripcion(), fontNormal));
        }

        if (rutina.getExplicacionIA() != null && !rutina.getExplicacionIA().isEmpty()) {
            document.add(crearTarjetaNota("EXPLICACIÓN DE LA IA", rutina.getExplicacionIA(), fontNormal));
        }

        document.add(new Paragraph("EJERCICIOS DETALLADOS")
                .setFont(fontNormal).setFontSize(11).setBold().setFontColor(COLOR_PRIMARIO).setMarginTop(8)
                .setMarginBottom(6));

        if (rutina.getDetalles() != null && !rutina.getDetalles().isEmpty()) {
            int orden = 1;
            for (DetalleRutinaExportacionDTO detalle : rutina.getDetalles()) {
                Table ejercicioCard = new Table(new float[] { 1f })
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(6);

                Cell cell = new Cell()
                        .setBackgroundColor(COLOR_SECUNDARIO)
                        .setBorderLeft(new SolidBorder(COLOR_ACENTO, 3))
                        .setBorderTop(new SolidBorder(COLOR_BORDE, 1))
                        .setBorderRight(new SolidBorder(COLOR_BORDE, 1))
                        .setBorderBottom(new SolidBorder(COLOR_BORDE, 1))
                        .setPadding(6);

                String tituloEjercicio = orden + ". " + detalle.getNombreEjercicio() +
                        (detalle.getGrupoMuscular() != null ? " (" + detalle.getGrupoMuscular().toUpperCase() + ")"
                                : "");
                cell.add(new Paragraph(tituloEjercicio).setFont(fontNormal).setFontSize(9.5f).setBold()
                        .setFontColor(COLOR_PRIMARIO));

                String repsText = "-";
                if (detalle.getRepeticionesMin() != null && detalle.getRepeticionesMax() != null) {
                    repsText = detalle.getRepeticionesMin() + " - " + detalle.getRepeticionesMax() + " reps";
                } else if (detalle.getRepeticionesMin() != null) {
                    repsText = detalle.getRepeticionesMin() + " reps";
                }

                String infoTecnica = String.format(
                        "Series: %s   |   Reps: %s   |   Peso: %s   |   Descanso: %s   |   Día: %s",
                        detalle.getSeries() != null ? detalle.getSeries() : "-",
                        repsText,
                        detalle.getPesoSugerido() != null ? detalle.getPesoSugerido() + " kg" : "Libre",
                        detalle.getDescansoSegundos() != null ? detalle.getDescansoSegundos() + "s" : "-",
                        detalle.getDiaSemana() != null ? "Día " + detalle.getDiaSemana() : "-");

                cell.add(new Paragraph(infoTecnica).setFont(fontNormal).setFontSize(8.5f).setFontColor(COLOR_ACENTO)
                        .setMarginTop(2));

                if (detalle.getEquipoRequerido() != null && !detalle.getEquipoRequerido().isEmpty()) {
                    cell.add(new Paragraph("Equipamiento: " + detalle.getEquipoRequerido()).setFont(fontNormal)
                            .setFontSize(8).setFontColor(new DeviceRgb(100, 116, 139)));
                }

                if (detalle.getNotas() != null && !detalle.getNotas().isEmpty()) {
                    cell.add(new Paragraph("💡 " + detalle.getNotas()).setFont(fontNormal).setFontSize(8).setItalic()
                            .setFontColor(new DeviceRgb(71, 85, 105)).setMarginTop(2));
                }

                ejercicioCard.addCell(cell);
                document.add(ejercicioCard);
                orden++;
            }
        } else {
            document.add(new Paragraph("No hay ejercicios registrados en esta rutina.").setFont(fontNormal)
                    .setFontSize(9).setFontColor(new DeviceRgb(150, 150, 150)));
        }

        agregarPieDePagina(document, fontNormal);
        document.close();
        return baos.toByteArray();
    }

    /**
     * Exporta un plan nutricional a formato PDF con un diseño moderno tipo
     * Dashboard Web
     */
    public byte[] exportarPlanNutricionalPdf(PlanNutricionalExportacionDTO plan) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(30, 30, 30, 30);

        PdfFont fontNormal = PdfFontFactory.createFont();

        Table mainHeader = new Table(new float[] { 1f, 3.5f, 2f })
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER);
        try {
            String logoPath = getClass().getResource("/images/logo_sin_fondo_dos.png").toString();
            ImageData imageData = ImageDataFactory.create(logoPath);
            Image logo = new Image(imageData);
            logo.scaleToFit(100, 100);
            logoCell.add(logo);
        } catch (Exception e) {
            logoCell.add(new Paragraph("[PULSE GYM]").setFont(fontNormal).setFontSize(9).setBold()
                    .setFontColor(COLOR_ACENTO));
            log.warn("No se pudo cargar el logo de Pulse Gym: {}", e.getMessage());
        }
        mainHeader.addCell(logoCell);

        Cell titleCell = new Cell().setBorder(Border.NO_BORDER);
        titleCell.add(
                new Paragraph("PULSE GYM").setFont(fontNormal).setFontSize(16).setBold().setFontColor(COLOR_PRIMARIO));
        titleCell.add(new Paragraph("PLAN NUTRICIONAL Y MACRONUTRIENTES").setFont(fontNormal).setFontSize(9).setBold()
                .setFontColor(COLOR_ACENTO));
        mainHeader.addCell(titleCell);

        Cell metaCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        metaCell.add(new Paragraph("Versión: " + (plan.getVersion() != null ? "v" + plan.getVersion() : "v1"))
                .setFont(fontNormal).setFontSize(9).setBold().setFontColor(COLOR_PRIMARIO));
        metaCell.add(new Paragraph(plan.getFechaGeneracion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(fontNormal).setFontSize(8).setFontColor(new DeviceRgb(100, 116, 139)));
        mainHeader.addCell(metaCell);

        document.add(mainHeader);

        Table infoCard = new Table(new float[] { 1f, 1f })
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        Cell socioBox = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(new SolidBorder(COLOR_BORDE, 1))
                .setPadding(8);
        socioBox.add(new Paragraph("SOCIO ASIGNADO").setFont(fontNormal).setFontSize(8).setBold()
                .setFontColor(COLOR_ACENTO));
        socioBox.add(new Paragraph(plan.getNombreSocio() + " " + plan.getApellidoSocio()).setFont(fontNormal)
                .setFontSize(9).setBold());
        socioBox.add(new Paragraph(plan.getEmailSocio()).setFont(fontNormal).setFontSize(8)
                .setFontColor(new DeviceRgb(100, 116, 139)));
        infoCard.addCell(socioBox);

        Cell restBox = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(new SolidBorder(COLOR_BORDE, 1))
                .setPadding(8);
        restBox.add(new Paragraph("RESTRICCIONES DIETÉTICAS").setFont(fontNormal).setFontSize(8).setBold()
                .setFontColor(COLOR_ACENTO));
        restBox.add(new Paragraph(
                plan.getRestriccionesDieteticas() != null ? plan.getRestriccionesDieteticas() : "Sin restricciones")
                .setFont(fontNormal).setFontSize(8.5f));
        infoCard.addCell(restBox);

        document.add(infoCard);

        Table macrosGrid = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(12);

        macrosGrid.addCell(crearCajaMacro("CALORÍAS",
                plan.getCaloriasDiarias() != null ? plan.getCaloriasDiarias() + " kcal" : "-", fontNormal));
        macrosGrid.addCell(crearCajaMacro("PROTEÍNAS",
                plan.getProteinasG() != null ? String.format("%.1fg", plan.getProteinasG()) : "-", fontNormal));
        macrosGrid.addCell(crearCajaMacro("CARBOHIDRATOS",
                plan.getCarbohidratosG() != null ? String.format("%.1fg", plan.getCarbohidratosG()) : "-", fontNormal));
        macrosGrid.addCell(crearCajaMacro("GRASAS",
                plan.getGrasasG() != null ? String.format("%.1fg", plan.getGrasasG()) : "-", fontNormal));

        document.add(macrosGrid);

        if (plan.getExplicacionIA() != null && !plan.getExplicacionIA().isEmpty()) {
            document.add(crearTarjetaNota("ESTRATEGIA NUTRICIONAL (IA)", plan.getExplicacionIA(), fontNormal));
        }

        document.add(new Paragraph("PLAN DE COMIDAS SUGERIDO")
                .setFont(fontNormal).setFontSize(11).setBold().setFontColor(COLOR_PRIMARIO).setMarginTop(8)
                .setMarginBottom(6));

        if (plan.getSugerenciasComidas() != null && !plan.getSugerenciasComidas().isEmpty()) {
            String[] ordenComidas = { "desayuno", "colaciones", "almuerzo", "cena" };
            Map<String, String> nombresComidas = Map.of(
                    "desayuno", "DESAYUNO",
                    "colaciones", "COLACIONES / SNACKS",
                    "almuerzo", "ALMUERZO",
                    "cena", "CENA");

            for (String tipo : ordenComidas) {
                if (plan.getSugerenciasComidas().containsKey(tipo)) {
                    List<SugerenciaComidaExportacionDTO> comidas = plan.getSugerenciasComidas().get(tipo);
                    if (comidas != null && !comidas.isEmpty()) {
                        document.add(new Paragraph(nombresComidas.getOrDefault(tipo, tipo.toUpperCase()))
                                .setFont(fontNormal).setFontSize(9).setBold().setFontColor(COLOR_ACENTO).setMarginTop(4)
                                .setMarginBottom(3));

                        for (SugerenciaComidaExportacionDTO comida : comidas) {
                            Table comidaCard = new Table(new float[] { 1f })
                                    .setWidth(UnitValue.createPercentValue(100))
                                    .setMarginBottom(5);

                            Cell cell = new Cell()
                                    .setBackgroundColor(COLOR_SECUNDARIO)
                                    .setBorder(new SolidBorder(COLOR_BORDE, 1))
                                    .setPadding(6);

                            String nombreCalorias = (comida.getNombre() != null ? comida.getNombre() : "Opción") +
                                    (comida.getCalorias() != null ? " (" + comida.getCalorias() + " kcal)" : "");

                            cell.add(new Paragraph(nombreCalorias).setFont(fontNormal).setFontSize(9).setBold()
                                    .setFontColor(COLOR_PRIMARIO));

                            String macrosComida = String.format(
                                    "Proteínas: %.1fg  |  Carbohidratos: %.1fg  |  Grasas: %.1fg",
                                    comida.getProteinas() != null ? comida.getProteinas() : 0.0,
                                    comida.getCarbohidratos() != null ? comida.getCarbohidratos() : 0.0,
                                    comida.getGrasas() != null ? comida.getGrasas() : 0.0);
                            cell.add(new Paragraph(macrosComida).setFont(fontNormal).setFontSize(8)
                                    .setFontColor(COLOR_ACENTO).setMarginTop(1));

                            if (comida.getIngredientes() != null && !comida.getIngredientes().isEmpty()) {
                                cell.add(new Paragraph("Ingredientes: " + comida.getIngredientes())
                                        .setFont(fontNormal).setFontSize(8).setFontColor(new DeviceRgb(71, 85, 105)));
                            }

                            if (comida.getPreparacion() != null && !comida.getPreparacion().isEmpty()) {
                                cell.add(new Paragraph("Preparación: " + comida.getPreparacion()).setFont(fontNormal)
                                        .setFontSize(8).setFontColor(new DeviceRgb(71, 85, 105)));
                            }

                            comidaCard.addCell(cell);
                            document.add(comidaCard);
                        }
                    }
                }
            }
        } else {
            document.add(new Paragraph("No hay sugerencias de comidas disponibles.").setFont(fontNormal).setFontSize(9)
                    .setFontColor(new DeviceRgb(150, 150, 150)));
        }

        agregarPieDePagina(document, fontNormal);
        document.close();
        return baos.toByteArray();
    }

    /**
     * Crea un bloque de tarjeta con borde izquierdo acentuado para notas o
     * descripciones
     */
    private Table crearTarjetaNota(String titulo, String contenido, PdfFont font) {
        Table table = new Table(new float[] { 1f }).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorderLeft(new SolidBorder(COLOR_ACENTO, 3))
                .setBorderTop(new SolidBorder(COLOR_BORDE, 1))
                .setBorderRight(new SolidBorder(COLOR_BORDE, 1))
                .setBorderBottom(new SolidBorder(COLOR_BORDE, 1))
                .setPadding(6);

        cell.add(new Paragraph(titulo).setFont(font).setFontSize(8.5f).setBold().setFontColor(COLOR_ACENTO));
        cell.add(new Paragraph(contenido).setFont(font).setFontSize(8.5f).setFontColor(new DeviceRgb(51, 65, 85)));
        table.addCell(cell);
        return table;
    }

    /**
     * Crea una caja de macronutrientes individual estilo widget web
     */
    private Cell crearCajaMacro(String etiqueta, String valor, PdfFont font) {
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_SECUNDARIO)
                .setBorder(new SolidBorder(COLOR_BORDE, 1))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
        cell.add(new Paragraph(valor).setFont(font).setFontSize(10).setBold().setFontColor(COLOR_PRIMARIO));
        cell.add(new Paragraph(etiqueta).setFont(font).setFontSize(7).setBold()
                .setFontColor(new DeviceRgb(100, 116, 139)));
        return cell;
    }

    /**
     * Agrega pie de página estandarizado
     */
    private void agregarPieDePagina(Document document, PdfFont font) {
        document.add(new Paragraph(" ")
                .setMarginTop(10)
                .setBorderTop(new SolidBorder(COLOR_BORDE, 1)));

        document.add(new Paragraph("Generado oficialmente por la plataforma Pulse Gym • "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setFont(font)
                .setFontSize(7.5f)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(148, 163, 184)));
    }
}