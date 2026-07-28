package com.pulse_gym.ms_users.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulse_gym.ms_users.service.ValidacionComprobanteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/validar-comprobante")
@RequiredArgsConstructor
public class ValidacionComprobanteController {

    /** Servicio de validación de comprobantes de pago */
    private final ValidacionComprobanteService validacionService;

    /**
     * Endpoint para validar un comprobante y mostrar HTML
     * 
     * @param idPago ID del pago a validar
     * @return HTML con la información del comprobante
     */
    @GetMapping(value = "/{idPago}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> validarComprobante(@PathVariable Long idPago) {
        String html = validacionService.generarHTMLValidacion(idPago);
        return ResponseEntity.ok(html);
    }
}