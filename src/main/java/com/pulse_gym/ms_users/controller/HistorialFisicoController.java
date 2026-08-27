package com.pulse_gym.ms_users.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.EvolucionFisicaDTO;
import com.pulse_gym.lb_common.dto.HistorialFisicoRequestDTO;
import com.pulse_gym.lb_common.dto.HistorialFisicoResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.HistorialFisicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios/historial-fisico")
@RequiredArgsConstructor
public class HistorialFisicoController {

    /** Servicio del historial físico */
    private final HistorialFisicoService historialService;

    /**
     * Registra una nueva medición física para un socio
     * 
     * @param historial Medición física a registrar
     * @return Mensaje de éxito si la medición se registró correctamente
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> registrarMedicion(
            @Valid @RequestBody HistorialFisicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = historialService.registrarMedicion(requestDTO, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar medición", e);
        }
    }

    /**
     * Consulta el historial físico de un socio
     * 
     * @param idSocio           ID del socio
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Lista de mediciones físicas del socio
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<HistorialFisicoResponseDTO>> consultarHistorial(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            List<HistorialFisicoResponseDTO> historial = historialService.consultarHistorial(idSocio, userRol,
                    userIdAutenticado);
            return ResponseEntity.ok(historial);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar historial", e);
        }
    }

    /**
     * Actualiza una medición física existente
     * 
     * @param idHistorial ID del historial físico a actualizar
     * @param requestDTO  Datos de la medición física a actualizar
     * @param userRol     Rol del usuario autenticado
     * @return Mensaje de éxito si la medición se actualizó correctamente
     */
    @PutMapping("/{idHistorial}")
    public ResponseEntity<MessegeGlobalDTO> actualizarMedicion(
            @PathVariable Long idHistorial,
            @Valid @RequestBody HistorialFisicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = historialService.actualizarMedicion(idHistorial, requestDTO, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar medición", e);
        }
    }

    /**
     * Obtiene la evolución física de un socio
     * 
     * @param idSocio           ID del socio
     * @param fechaInicio       Fecha de inicio del periodo
     * @param fechaFin          Fecha de fin del periodo
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return DTO con la evolución física del socio
     */
    @GetMapping("/evolucion/{idSocio}")
    public ResponseEntity<EvolucionFisicaDTO> obtenerEvolucion(
            @PathVariable Long idSocio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            EvolucionFisicaDTO evolucion = historialService.obtenerEvolucion(idSocio, userRol, userIdAutenticado,
                    fechaInicio, fechaFin);
            return ResponseEntity.ok(evolucion);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener evolución", e);
        }
    }

    /**
     * Obtiene todos los registros de historial físico de todos los socios
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista general de historiales físicos
     */
    @GetMapping
    public ResponseEntity<List<HistorialFisicoResponseDTO>> obtenerTodosHistoriales(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<HistorialFisicoResponseDTO> historial = historialService.obtenerTodosHistoriales(userRol);
            return ResponseEntity.ok(historial);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener todos los historiales", e);
        }
    }

    /**
     * Endpoint para consultar el historial físico del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header) - Extraído del token
     * @return Lista de registros del historial físico del socio autenticado
     * @throws SecurityAuthorizationException Si el usuario no es un socio
     * @throws RuntimeException               Si el socio no tiene registros
     */
    @GetMapping("/mi-historial")
    public ResponseEntity<List<HistorialFisicoResponseDTO>> consultarMiHistorial(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            List<HistorialFisicoResponseDTO> historial = historialService.consultarMiHistorial(userRol, userEmail);
            return ResponseEntity.ok(historial);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("No tienes registros")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar tu historial físico", e);
        }
    }

    /**
     * Endpoint para obtener la evolución física del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol     Rol del usuario autenticado (header)
     * @param userEmail   Email del usuario autenticado (header) - Extraído del
     *                    token
     * @param fechaInicio Fecha de inicio del período (opcional)
     * @param fechaFin    Fecha de fin del período (opcional)
     * @return DTO con la evolución física del socio autenticado
     * @throws SecurityAuthorizationException Si el usuario no es un socio
     * @throws RuntimeException               Si no se encuentra el socio
     */
    @GetMapping("/mi-evolucion")
    public ResponseEntity<EvolucionFisicaDTO> obtenerMiEvolucion(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            EvolucionFisicaDTO evolucion = historialService.obtenerMiEvolucion(
                    userRol, userEmail, fechaInicio, fechaFin);
            return ResponseEntity.ok(evolucion);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrado")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener tu evolución física", e);
        }
    }
}
