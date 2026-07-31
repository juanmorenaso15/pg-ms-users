package com.pulse_gym.ms_users.service;

import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.client.EventoPagoClient;
import com.pulse_gym.lb_common.dto.EventoPagoRequestDTO;

import lombok.RequiredArgsConstructor;

/**
 * EventoPagoAsyncService
 */
@Service
@RequiredArgsConstructor
public class EventoPagoAsyncService {

    private final EventoPagoClient eventoPagoClient;

    public void enviarEventoPago(EventoPagoRequestDTO eventoPagoRequestDTO) {
        try {
            eventoPagoClient.enviarEventoPago(eventoPagoRequestDTO);
        } catch (Exception e) {
            // Manejo de errores, por ejemplo, loguear el error o reintentar
            System.err.println("Error al enviar evento de pago: " + e.getMessage());
        }
    }

}
