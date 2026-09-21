package com.pulse_gym.ms_users.service;

import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.client.NotificacionClient;
import com.pulse_gym.lb_common.dto.AuthUserDTO;
import com.pulse_gym.lb_common.dto.EnvioEventoNotificacionDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEventoAsociado;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara la notificacion de bienvenida (WELCOME) de forma realmente
 * asincrona. Antes vivia dentro de UsuarioPerfilService sin @Async siquiera
 * (ni @EnableAsync estaba habilitado en este microservicio), asi que la
 * llamada a auth + notificaciones corria en el mismo hilo de la respuesta de
 * "completar perfil". Al estar en un bean aparte, @Async si aplica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionAsyncTrigger {

    private final AuthServiceClient authServiceClient;
    private final NotificacionClient notificacionClient;

    @Async
    public void enviarNotificacionBienvenida(UsuarioPerfil usuario) {
        try {
            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(usuario.getEmail());
            if (authUser == null) {
                return;
            }

            EnvioEventoNotificacionDTO eventoDTO = new EnvioEventoNotificacionDTO();
            eventoDTO.setUsuarioId(authUser.getId());
            eventoDTO.setEvento(EnumEventoAsociado.WELCOME);
            eventoDTO.setVariablesAdicionales(Map.of(
                    "nombre", usuario.getNombre(),
                    "apellido", usuario.getApellido() != null ? usuario.getApellido() : ""));
            notificacionClient.enviarPorEvento(eventoDTO);
        } catch (Exception e) {
            log.error("Error enviando notificación de bienvenida: {}", e.getMessage());
        }
    }
}
