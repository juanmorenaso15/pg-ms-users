package com.pulse_gym.ms_users.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.RegistroSesionRequestDTO;
import com.pulse_gym.lb_common.dto.SesionResponseDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.SeguimientoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/seguimiento")
@RequiredArgsConstructor
@Slf4j
public class SeguimientoController {

    /** Servicio de seguimiento y progreso */
    private final SeguimientoService seguimientoService;

    /**
     * Registra una sesión de entrenamiento realizada por un socio
     * 
     * @param request           Datos de la sesión a registrar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PostMapping("/sesion")
    public ResponseEntity<SesionResponseDTO> registrarSesion(
            @Valid @RequestBody RegistroSesionRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Registrando sesión de entrenamiento para socio ID: {} con email: {}",
                    request.getIdSocio(), userEmail);

            SesionResponseDTO response = seguimientoService.registrarSesion(request, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al registrar sesión: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar sesión", e);
        }
    }

}