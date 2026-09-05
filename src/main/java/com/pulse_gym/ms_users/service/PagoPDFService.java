package com.pulse_gym.ms_users.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
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

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(26, 82, 118);  
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(240, 243, 244); 
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(39, 174, 96);    
    private static final DeviceRgb DANGER_COLOR = new DeviceRgb(192, 57, 43);     
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(44, 62, 80);         
    /**
     * Genera un comprobante de pago rediseñado en formato PDF con logo, estilos modernos y código QR apuntando al Front-End.
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
            
            document.setMargins(30, 35, 30, 35);

            Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 25, 75 }))
                    .setWidth(UnitValue.createPercentValue(100));

            Cell logoCell = new Cell();
            try {
                InputStream logoStream = getClass().getResourceAsStream("/images/LOGO_OFICIAL.jpg");
                if (logoStream != null) {
                    byte[] logoBytes = logoStream.readAllBytes();
                    Image logo = new Image(ImageDataFactory.create(logoBytes));
                    logo.scaleToFit(65, 65);
                    logoCell.add(logo);
                } else {
                    logoCell.add(new Paragraph("🏋️").setFontSize(32));
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo: {}", e.getMessage());
                logoCell.add(new Paragraph("🏋️").setFontSize(32));
            }
            logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            logoCell.setBorder(null);
            headerTable.addCell(logoCell);

            Cell titleCell = new Cell();
            titleCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            titleCell.setBorder(null);

            Paragraph title = new Paragraph("PULSE GYM")
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR);
            titleCell.add(title);

            Paragraph subtitle = new Paragraph("COMPROBANTE OFICIAL DE PAGO")
                    .setFontSize(11)
                    .setBold()
                    .setFontColor(new DeviceRgb(127, 140, 141));
            titleCell.add(subtitle);

            headerTable.addCell(titleCell);
            document.add(headerTable);

            Table divider = new Table(1).setWidth(UnitValue.createPercentValue(100));
            divider.addCell(new Cell().setHeight(2).setBackgroundColor(PRIMARY_COLOR).setBorder(null));
            document.add(divider);
            
            document.add(new Paragraph(" ").setFontSize(4));

            Table table = new Table(UnitValue.createPercentArray(new float[] { 35, 65 }))
                    .setWidth(UnitValue.createPercentValue(100));

            addStyledRow(table, "ID de Pago:", "#" + pago.getIdPago(), true);
            addStyledRow(table, "Socio:", pago.getNombreSocio(), false);
            addStyledRow(table, "Correo Electrónico:", pago.getEmailSocio(), true);
            addStyledRow(table, "Membresía:", pago.getNombreMembresia(), false);
            addStyledRow(table, "Monto Pagado:", "$ " + String.format("%,.2f", pago.getMonto()), true);
            addStyledRow(table, "Método de Pago:", formatMetodoPago(pago.getMetodoPago()), false);
            addStyledRow(table, "Fecha y Hora:", pago.getFechaPago().format(DATE_FORMATTER), true);
            addStyledRow(table, "N° Comprobante:", pago.getNumeroComprobante(), false);

            if (pago.getNombreAdminRegistro() != null) {
                addStyledRow(table, "Registrado por:", pago.getNombreAdminRegistro(), true);
            }

            boolean isAnulado = pago.getAnulado() != null && pago.getAnulado();
            Cell estadoLabelCell = createCell("Estado del Pago:", true, true);
            
            Paragraph estadoVal = new Paragraph(isAnulado ? "ANULADO" : "APROBADO")
                    .setBold()
                    .setFontSize(10)
                    .setFontColor(isAnulado ? DANGER_COLOR : SUCCESS_COLOR);
            
            Cell estadoValCell = new Cell().add(estadoVal)
                    .setPadding(6)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBorder(null)
                    .setBackgroundColor(SECONDARY_COLOR);
            
            table.addCell(estadoLabelCell);
            table.addCell(estadoValCell);

            if (isAnulado && pago.getMotivoAnulacion() != null) {
                addStyledRow(table, "Motivo Anulación:", pago.getMotivoAnulacion(), true);
            }

            document.add(table);
            document.add(new Paragraph(" ").setFontSize(6));

            try {
                String qrTargetUrl = "https://front-end-pulsegym.pages.dev/auth/login?idPago=" + pago.getIdPago();

                byte[] qrBytes = qrCodeService.generarQRComprobante(pago.getIdPago());
                
                Image qrImage = new Image(ImageDataFactory.create(qrBytes));
                qrImage.scaleToFit(90, 90);
                qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER);

                Table qrTable = new Table(UnitValue.createPercentArray(new float[] { 100 }))
                        .setWidth(UnitValue.createPercentValue(40))
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);

                Cell qrCell = new Cell();
                qrCell.add(qrImage);
                qrCell.setTextAlignment(TextAlignment.CENTER);
                qrCell.setPadding(6);
                qrCell.setBorder(null);
                qrCell.setBackgroundColor(SECONDARY_COLOR);
                qrTable.addCell(qrCell);

                document.add(qrTable);

                Paragraph qrText = new Paragraph("Escanea el código QR para acceder al sistema")
                        .setFontSize(8)
                        .setItalic()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(149, 165, 166));
                document.add(qrText);

            } catch (Exception e) {
                log.error("Error al generar el código QR: {}", e.getMessage());
                document.add(new Paragraph("Código QR no disponible")
                        .setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(DANGER_COLOR));
            }

            document.add(new Paragraph(" ").setFontSize(6));

            Paragraph footer1 = new Paragraph("Este documento es un comprobante digital válido emitido por Pulse Gym.")
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(127, 140, 141));
            document.add(footer1);

            Paragraph footer2 = new Paragraph("Plataforma web: https://front-end-pulsegym.pages.dev/auth/login")
                    .setFontSize(8)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(PRIMARY_COLOR);
            document.add(footer2);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar el PDF del comprobante: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el comprobante PDF: " + e.getMessage());
        }
    }

    private void addStyledRow(Table table, String label, String value, boolean isEven) {
        DeviceRgb rowBg = isEven ? SECONDARY_COLOR : new DeviceRgb(255, 255, 255);

        Cell labelCell = createCell(label, true, isEven);
        Cell valueCell = new Cell()
                .add(new Paragraph(value != null ? value : "-").setFontSize(10).setFontColor(TEXT_DARK))
                .setPadding(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(null)
                .setBackgroundColor(rowBg);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Cell createCell(String text, boolean isLabel, boolean isEven) {
        DeviceRgb rowBg = isEven ? SECONDARY_COLOR : new DeviceRgb(255, 255, 255);
        Paragraph p = new Paragraph(text)
                .setFontSize(10)
                .setBold()
                .setFontColor(isLabel ? PRIMARY_COLOR : TEXT_DARK);

        return new Cell()
                .add(p)
                .setPadding(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(null)
                .setBackgroundColor(rowBg);
    }

    private String formatMetodoPago(String metodo) {
        if (metodo == null) return "-";
        switch (metodo) {
            case "EFECTIVO": return "Efectivo";
            case "TRANSFERENCIA_BANCOLOMBIA": return "Transferencia Bancolombia";
            case "TARJETA_CREDITO": return "Tarjeta de Crédito";
            case "TARJETA_DEBITO": return "Tarjeta de Débito";
            default: return metodo;
        }
    }
}