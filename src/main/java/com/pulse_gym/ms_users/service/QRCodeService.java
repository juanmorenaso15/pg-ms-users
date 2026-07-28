package com.pulse_gym.ms_users.service;

import java.io.ByteArrayOutputStream;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class QRCodeService {

    /**
     * Genera un código QR como imagen PNG
     * 
     * @param content Contenido del QR (URL o texto)
     * @param width   Ancho de la imagen
     * @param height  Alto de la imagen
     * @return Array de bytes con la imagen PNG del QR
     */
    public byte[] generateQRCode(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar el código QR: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el código QR: " + e.getMessage());
        }
    }

    /**
     * Genera un código QR con URL para validación del comprobante
     * 
     * @param pagoId ID del pago
     * @return Array de bytes con la imagen PNG del QR
     */
    public byte[] generarQRComprobante(Long pagoId) {
        String baseUrl = System.getenv().getOrDefault("APP_URL", "http://localhost:8081");
        String url = baseUrl + "/validar-comprobante/" + pagoId;

        return generateQRCode(url, 150, 150);
    }

    /**
     * Genera un código QR con texto de validación
     * 
     * @param pagoId            ID del pago
     * @param numeroComprobante Número de comprobante
     * @return Array de bytes con la imagen PNG del QR
     */
    public byte[] generarQRValidacion(Long pagoId, String numeroComprobante) {
        String content = String.format(
                "Comprobante: %s\nID Pago: %d\nPulse Gym - Comprobante válido",
                numeroComprobante, pagoId);
        return generateQRCode(content, 150, 150);
    }
}