package com.pulse_gym.ms_users.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.pulse_gym.lb_common.dto.PagoResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor 
public class PagoPDFService {

    /** Formateador de fecha para el comprobante */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Servicio para generación de códigos QR */
    private final QRCodeService qrCodeService;

    /**
     * Genera un comprobante de pago en formato PDF con logo y código QR
     * 
     * @param pago Datos del pago a incluir en el comprobante
     * @return Array de bytes del PDF generado
     * @throws RuntimeException Si ocurre un error al generar el PDF
     */
    public byte[] generarComprobantePDF(PagoResponseDTO pago) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 20, 80 }))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell logoCell = new Cell();
            try {
                InputStream logoStream = getClass().getResourceAsStream("/images/LOGO_OFICIAL.jpg");
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    Image logo = new Image(ImageDataFactory.create(logoBytes));
                    logo.scaleToFit(80, 80);
                    logoCell.add(logo);
                } else {
                    logoCell.add(new Paragraph("🏋️").setFontSize(40));
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo: {}", e.getMessage());
                logoCell.add(new Paragraph("🏋️").setFontSize(40));
            }
            logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            logoCell.setPadding(8);
            headerTable.addCell(logoCell);

            Cell titleCell = new Cell();
            titleCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            titleCell.setPadding(8);

            Paragraph title = new Paragraph("PULSE GYM")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(ColorConstants.BLUE);
            titleCell.add(title);

            Paragraph subtitle = new Paragraph("COMPROBANTE DE PAGO")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(ColorConstants.DARK_GRAY);
            titleCell.add(subtitle);

            headerTable.addCell(titleCell);
            document.add(headerTable);

            document.add(new Paragraph("________________________________________")
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[] { 40, 60 }))
                    .setWidth(UnitValue.createPercentValue(100));

            addRow(table, "ID Pago:", pago.getIdPago().toString());
            addRow(table, "Socio:", pago.getNombreSocio());
            addRow(table, "Email:", pago.getEmailSocio());
            addRow(table, "Membresía:", pago.getNombreMembresia());
            addRow(table, "Monto:", "$ " + String.format("%,.0f", pago.getMonto()));
            addRow(table, "Método de Pago:", pago.getMetodoPago());
            addRow(table, "Fecha:", pago.getFechaPago().format(DATE_FORMATTER));
            addRow(table, "Comprobante:", pago.getNumeroComprobante());

            if (pago.getNombreAdminRegistro() != null) {
                addRow(table, "Registrado por:", pago.getNombreAdminRegistro());
            }

            if (pago.getAnulado() != null && pago.getAnulado()) {
                addRow(table, "Estado:", "ANULADO");
                if (pago.getMotivoAnulacion() != null) {
                    addRow(table, "Motivo Anulación:", pago.getMotivoAnulacion());
                }
            } else {
                addRow(table, "Estado:", "APROBADO");
            }

            document.add(table);
            document.add(new Paragraph(" "));

            try {
                byte[] qrBytes = qrCodeService.generarQRComprobante(pago.getIdPago());
                Image qrImage = new Image(ImageDataFactory.create(qrBytes));
                qrImage.scaleToFit(120, 120);

                Table qrTable = new Table(UnitValue.createPercentArray(new float[] { 100 }))
                        .setWidth(UnitValue.createPercentValue(100));

                Cell qrCell = new Cell();
                qrCell.add(qrImage);
                qrCell.setTextAlignment(TextAlignment.CENTER);
                qrCell.setPadding(10);
                qrTable.addCell(qrCell);

                document.add(qrTable);

                Paragraph qrText = new Paragraph("Escanea el código QR para validar el comprobante")
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.GRAY);
                document.add(qrText);

            } catch (Exception e) {
                log.error("Error al generar el código QR: {}", e.getMessage());
                document.add(new Paragraph("Código QR no disponible")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.RED));
            }

            document.add(new Paragraph(" "));

            Paragraph footer1 = new Paragraph("Este comprobante es válido como constancia de pago.")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY);
            document.add(footer1);

            Paragraph footer2 = new Paragraph("Pulse Gym - Tu bienestar, nuestra pasión")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY);
            document.add(footer2);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar el PDF del comprobante: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el comprobante PDF: " + e.getMessage());
        }
    }

    /**
     * Agrega una fila a la tabla del PDF
     * 
     * @param table Tabla donde se agregará la fila
     * @param label Etiqueta de la fila
     * @param value Valor de la fila
     */
    private void addRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(8)
                .setFontSize(12)
                .setBold();
        labelCell.add(new Paragraph(label));

        Cell valueCell = new Cell()
                .setPadding(8)
                .setFontSize(12);
        valueCell.add(new Paragraph(value != null ? value : "-"));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}