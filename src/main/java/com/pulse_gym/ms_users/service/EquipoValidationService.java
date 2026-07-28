package com.pulse_gym.ms_users.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.client.EquipoClient;
import com.pulse_gym.lb_common.dto.ConsultaEquipoRequestDTO;
import com.pulse_gym.lb_common.dto.EquipoResponseWrapperDTO;
import com.pulse_gym.lb_common.entity.operation.Equipo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipoValidationService {

    private final EquipoClient equipoClient;

    /**
     * Valida que un equipo exista en pg-ms-operation por su nombre
     * 
     * @param nombreEquipo Nombre del equipo a validar
     * @return true si el equipo existe y está operativo, false en caso contrario
     */
    public boolean validarEquipoExistente(String nombreEquipo) {
        try {
            ConsultaEquipoRequestDTO request = new ConsultaEquipoRequestDTO();
            request.setNombre(nombreEquipo);

            EquipoResponseWrapperDTO response = equipoClient.consultarEquipos(request);

            if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || response.getData() == null) {
                log.warn("Equipo no encontrado o error en la consulta: {}", nombreEquipo);
                return false;
            }

            List<Equipo> equipos = response.getData();

            if (equipos.isEmpty()) {
                log.warn("Equipo no encontrado: {}", nombreEquipo);
                return false;
            }

            // Verificar que al menos uno esté OPERATIVO
            boolean existeOperativo = equipos.stream()
                    .anyMatch(e -> e.getEstado() != null &&
                            e.getEstado().name().equals("OPERATIVO"));

            if (!existeOperativo) {
                log.warn("El equipo '{}' existe pero no está en estado OPERATIVO", nombreEquipo);
                return false;
            }

            log.info("Equipo validado exitosamente: {}", nombreEquipo);
            return true;

        } catch (Exception e) {
            log.error("Error al validar equipo '{}': {}", nombreEquipo, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Valida que un equipo exista y lanza excepción si no existe
     * 
     * @param nombreEquipo Nombre del equipo a validar
     * @throws RuntimeException Si el equipo no existe o no está operativo
     */
    public void validarEquipoExistenteOrThrow(String nombreEquipo) {
        if (!validarEquipoExistente(nombreEquipo)) {
            throw new RuntimeException(
                    "El equipo '" + nombreEquipo + "' no existe en el inventario o no está operativo. " +
                            "Por favor, asegúrate de que el equipo esté registrado en el sistema de operaciones.");
        }
    }
}