package com.pulse_gym.ms_users.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.dto.ValidacionComprobanteDTO;
import com.pulse_gym.lb_common.entity.user.Pago;
import com.pulse_gym.ms_users.repository.PagoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidacionComprobanteService {

    private final PagoRepository pagoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Valida un comprobante y genera el HTML para mostrarlo
     * 
     * @param idPago ID del pago a validar
     * @return HTML con la información del comprobante
     */
    @Transactional(readOnly = true)
    public String generarHTMLValidacion(Long idPago) {
        try {
            Pago pago = pagoRepository.findById(idPago)
                    .orElseThrow(() -> new RuntimeException("Comprobante no encontrado"));

            ValidacionComprobanteDTO dto = ValidacionComprobanteDTO.builder()
                    .idPago(pago.getIdPago())
                    .socioNombre(pago.getSocioMembresia().getSocio().getNombre() + " " +
                            pago.getSocioMembresia().getSocio().getApellido())
                    .socioEmail(pago.getSocioMembresia().getSocio().getEmail())
                    .membresiaNombre(pago.getSocioMembresia().getMembresia().getNombre())
                    .monto(pago.getMonto())
                    .fechaPago(pago.getFechaPago())
                    .metodoPago(pago.getMetodoPago().name())
                    .numeroComprobante(pago.getNumeroComprobante())
                    .estado(pago.getEstado() != null ? pago.getEstado().name() : "SIN ESTADO")
                    .anulado(pago.getAnulado())
                    .motivoAnulacion(pago.getMotivoAnulacion())
                    .build();

            if (pago.getAnulado()) {
                dto.setValido(false);
                dto.setMensaje("Este comprobante ha sido ANULADO");
            } else {
                dto.setValido(true);
                dto.setMensaje("COMPROBANTE VÁLIDO");
            }

            return generarHTML(dto);

        } catch (Exception e) {
            log.error("Error al validar comprobante: {}", e.getMessage());
            return generarHTML("Comprobante no encontrado");
        }
    }

    /**
     * Genera el HTML para la validación del comprobante
     * 
     * @param dto Datos del comprobante a validar
     * @return String con el HTML generado
     */
    private String generarHTML(ValidacionComprobanteDTO dto) {
        String estadoColor = dto.getValido() ? "#28a745" : "#dc3545";
        String estadoBg = dto.getValido() ? "#d4edda" : "#f8d7da";
        String estadoBorder = dto.getValido() ? "#c3e6cb" : "#f5c6cb";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Validación de Comprobante - Pulse Gym</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            line-height: 1.6;
                            color: #2c3e50;
                            margin: 0;
                            padding: 0;
                            background: linear-gradient(135deg, #e0eafc 0%%, #cfdef3 100%%);
                        }
                        .container {
                            max-width: 550px;
                            margin: 30px auto;
                            padding: 0;
                            background-color: #ffffff;
                            border-radius: 20px;
                            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #2c4b77 0%%, #8bb5d6 100%%);
                            color: white;
                            padding: 35px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 300;
                            letter-spacing: 1px;
                        }
                        .header p {
                            margin: 10px 0 0;
                            opacity: 0.9;
                            font-size: 14px;
                        }
                        .content {
                            padding: 40px 35px;
                            background-color: #ffffff;
                        }
                        .status-badge {
                            display: inline-block;
                            padding: 10px 30px;
                            border-radius: 30px;
                            font-size: 18px;
                            font-weight: 700;
                            margin-bottom: 25px;
                            background-color: %s;
                            color: %s;
                            border: 2px solid %s;
                            text-align: center;
                            width: 100%%;
                            box-sizing: border-box;
                        }
                        .info-card {
                            background: #f8f9fc;
                            border-radius: 12px;
                            padding: 20px;
                            margin-bottom: 20px;
                        }
                        .info-row {
                            display: flex;
                            justify-content: space-between;
                            padding: 10px 0;
                            border-bottom: 1px solid #e8edf2;
                        }
                        .info-row:last-child {
                            border-bottom: none;
                        }
                        .info-label {
                            color: #64748B;
                            font-size: 13px;
                            font-weight: 600;
                        }
                        .info-value {
                            color: #0F172A;
                            font-size: 14px;
                            font-weight: 500;
                            text-align: right;
                        }
                        .footer {
                            background-color: #f8f9fc;
                            padding: 20px 30px;
                            text-align: center;
                            border-top: 1px solid #e8edf2;
                        }
                        .footer-text {
                            color: #9aabbb;
                            font-size: 11px;
                            margin: 5px 0;
                        }
                        .highlight {
                            color: #6c8ebf;
                            text-decoration: none;
                        }
                        .icon {
                            font-size: 48px;
                            text-align: center;
                            margin-bottom: 10px;
                        }
                        .warning-box {
                            background-color: #f8f9fc;
                            border-left: 3px solid #6c8ebf;
                            padding: 15px 20px;
                            margin: 20px 0;
                            border-radius: 8px;
                        }
                        .warning-text {
                            color: #7a8b9e;
                            font-size: 12px;
                            margin: 0;
                            line-height: 1.4;
                        }
                        .error-message {
                            text-align: center;
                            color: #e74c3c;
                            font-size: 18px;
                            padding: 30px 0;
                        }
                        .error-icon {
                            font-size: 64px;
                            text-align: center;
                            display: block;
                            margin-bottom: 15px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1><strong>Pulse Gym</strong></h1>
                            <p>Validación de Comprobante de Pago</p>
                        </div>

                        <div class="content">
                            <div class="icon">%s</div>
                            <div class="status-badge" style="background-color: %s; color: %s; border-color: %s;">
                                %s
                            </div>

                            <div class="info-card">
                                <div class="info-row">
                                    <span class="info-label">ID Pago</span>
                                    <span class="info-value">%d</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Socio</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Email</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Membresía</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Monto</span>
                                    <span class="info-value">$ %,.0f</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Fecha</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Método de Pago</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Comprobante</span>
                                    <span class="info-value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="info-label">Estado</span>
                                    <span class="info-value" style="color: %s; font-weight: 700;">%s</span>
                                </div>
                                %s
                            </div>

                            <div class="warning-box">
                                <div class="warning-text">
                                    <strong>ℹImportante:</strong> Este comprobante es válido como constancia de pago.
                                    Si tienes alguna duda, por favor contacta con la recepción de Pulse Gym.
                                </div>
                            </div>
                        </div>

                        <div class="footer">
                            <div class="footer-text">
                                © 2026 Pulse Gym - Todos los derechos reservados
                            </div>
                            <div class="footer-text">
                                <span class="highlight">Pulse Gym</span> - Tu bienestar, nuestra pasión
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                estadoBg, estadoColor, estadoBorder,
                dto.getValido() ? "✅" : "❌",
                estadoBg, estadoColor, estadoBorder,
                dto.getMensaje(),
                dto.getIdPago(),
                dto.getSocioNombre(),
                dto.getSocioEmail(),
                dto.getMembresiaNombre(),
                dto.getMonto(),
                dto.getFechaPago().format(DATE_FORMATTER),
                dto.getMetodoPago(),
                dto.getNumeroComprobante(),
                estadoColor,
                dto.getEstado(),
                dto.getAnulado() ? String.format("""
                        <div class="info-row">
                            <span class="info-label">Motivo Anulación</span>
                            <span class="info-value" style="color: #dc3545;">%s</span>
                        </div>
                        """, dto.getMotivoAnulacion() != null ? dto.getMotivoAnulacion() : "Sin especificar") : "");
    }

    /**
     * Genera el HTML para mostrar un error en la validación del comprobante
     * 
     * @param error Mensaje de error a mostrar
     * @return String con el HTML generado
     */
    private String generarHTML(String error) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Validación de Comprobante - Pulse Gym</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            line-height: 1.6;
                            color: #2c3e50;
                            margin: 0;
                            padding: 0;
                            background: linear-gradient(135deg, #e0eafc 0%%, #cfdef3 100%%);
                        }
                        .container {
                            max-width: 550px;
                            margin: 30px auto;
                            padding: 0;
                            background-color: #ffffff;
                            border-radius: 20px;
                            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #2c4b77 0%%, #8bb5d6 100%%);
                            color: white;
                            padding: 35px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 300;
                            letter-spacing: 1px;
                        }
                        .header p {
                            margin: 10px 0 0;
                            opacity: 0.9;
                            font-size: 14px;
                        }
                        .content {
                            padding: 40px 35px;
                            background-color: #ffffff;
                            text-align: center;
                        }
                        .error-icon {
                            font-size: 64px;
                            display: block;
                            margin-bottom: 15px;
                        }
                        .error-title {
                            font-size: 24px;
                            font-weight: 700;
                            color: #dc3545;
                            margin-bottom: 10px;
                        }
                        .error-message {
                            color: #5d6d7e;
                            font-size: 16px;
                            margin-bottom: 20px;
                        }
                        .warning-box {
                            background-color: #f8f9fc;
                            border-left: 3px solid #6c8ebf;
                            padding: 15px 20px;
                            margin: 20px 0;
                            border-radius: 8px;
                            text-align: left;
                        }
                        .warning-text {
                            color: #7a8b9e;
                            font-size: 12px;
                            margin: 0;
                            line-height: 1.4;
                        }
                        .footer {
                            background-color: #f8f9fc;
                            padding: 20px 30px;
                            text-align: center;
                            border-top: 1px solid #e8edf2;
                        }
                        .footer-text {
                            color: #9aabbb;
                            font-size: 11px;
                            margin: 5px 0;
                        }
                        .highlight {
                            color: #6c8ebf;
                            text-decoration: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1><strong>Pulse Gym</strong></h1>
                            <p>Validación de Comprobante de Pago</p>
                        </div>

                        <div class="content">
                            <span class="error-icon">🔍</span>
                            <div class="error-title">Comprobante No Encontrado</div>
                            <div class="error-message">%s</div>

                            <div class="warning-box">
                                <div class="warning-text">
                                    <strong>ℹSugerencia:</strong> Verifica que el ID del comprobante sea correcto.
                                    Si el problema persiste, contacta con la recepción de Pulse Gym.
                                </div>
                            </div>
                        </div>

                        <div class="footer">
                            <div class="footer-text">
                                © 2026 Pulse Gym - Todos los derechos reservados
                            </div>
                            <div class="footer-text">
                                <span class="highlight">Pulse Gym</span> - Tu bienestar, nuestra pasión
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """, error);
    }
}
