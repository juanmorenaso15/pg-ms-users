package com.pulse_gym.ms_users.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.DashboardMonitoreoEntrenadorDTO;
import com.pulse_gym.lb_common.dto.DashboardProgresoSocioDTO;
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

    /**
     * Obtiene el historial de sesiones de un socio
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Lista de sesiones del socio
     */
    @GetMapping("/historial/{idSocio}")
    public ResponseEntity<List<SesionResponseDTO>> obtenerHistorial(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Obteniendo historial de sesiones para socio ID: {} con email: {}",
                    idSocio, userEmail);
            List<SesionResponseDTO> historial = seguimientoService.obtenerHistorialSesiones(
                    idSocio, userRol, userEmail);
            return ResponseEntity.ok(historial);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener historial", e);
        }
    }

    /**
     * Obtiene el dashboard de progreso de un socio
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO con el dashboard de progreso del socio
     */
    @GetMapping("/dashboard/socio/{idSocio}")
    public ResponseEntity<DashboardProgresoSocioDTO> obtenerDashboardSocio(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Obteniendo dashboard de progreso para socio ID: {} con email: {}",
                    idSocio, userEmail);
            DashboardProgresoSocioDTO dashboard = seguimientoService.obtenerDashboardSocio(
                    idSocio, userRol, userEmail);
            return ResponseEntity.ok(dashboard);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener dashboard de socio: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener dashboard de socio", e);
        }
    }

    /**
     * Obtiene el dashboard de monitoreo para entrenadores con sus socios asignados
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO con el dashboard de monitoreo
     */
    @GetMapping("/dashboard/entrenador")
    public ResponseEntity<DashboardMonitoreoEntrenadorDTO> obtenerDashboardEntrenador(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Obteniendo dashboard de monitoreo para entrenador con email: {}", userEmail);
            DashboardMonitoreoEntrenadorDTO dashboard = seguimientoService.obtenerDashboardMonitoreo(
                    userRol, userEmail);
            return ResponseEntity.ok(dashboard);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener dashboard de entrenador: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener dashboard de entrenador", e);
        }
    }

    /**
     * Exporta una rutina a formato PDF
     * 
     * @param idSocio   ID del socio dueño de la rutina
     * @param idRutina  ID de la rutina a exportar (opcional)
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Archivo PDF de la rutina
     */
    @GetMapping("/rutina/{idSocio}/exportar-pdf")
    public ResponseEntity<byte[]> exportarRutinaPdf(
            @PathVariable Long idSocio,
            @RequestParam(required = false) Long idRutina,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Exportando rutina a PDF para socio ID: {}", idSocio);

            byte[] pdfBytes = seguimientoService.exportarRutinaPdf(idSocio, idRutina, userRol, userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rutina_" + idSocio + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al exportar rutina a PDF: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al exportar rutina a PDF", e);
        }
    }

    /**
     * Exporta un plan nutricional a formato PDF
     * @param idSocio ID del socio dueño del plan nutricional
     * @param idPlan ID del plan nutricional a exportar (opcional)
     * @param userRol Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Archivo PDF del plan nutricional
     */
    @GetMapping("/plan-nutricional/{idSocio}/exportar-pdf")
    public ResponseEntity<byte[]> exportarPlanNutricionalPdf(
            @PathVariable Long idSocio,
            @RequestParam(required = false) Long idPlan,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            log.info("Exportando plan nutricional a PDF para socio ID: {}", idSocio);

            byte[] pdfBytes = seguimientoService.exportarPlanNutricionalPdf(idSocio, idPlan, userRol, userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "plan_nutricional_" + idSocio + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al exportar plan nutricional a PDF: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al exportar plan nutricional a PDF", e);
        }
    }
}