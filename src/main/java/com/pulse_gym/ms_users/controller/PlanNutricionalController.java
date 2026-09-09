package com.pulse_gym.ms_users.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.PlanNutricionalAjusteRequestDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalGeneracionRequestDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalGeneracionResponseDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalHistorialResponseDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.PlanNutricionalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/planes-nutricionales")
@RequiredArgsConstructor
public class PlanNutricionalController {

    /** Servicio de planes nutricionales para operaciones de negocio */
    private final PlanNutricionalService planNutricionalService;

    /**
     * Genera un plan nutricional personalizado (para entrenadores/admins)
     * 
     * @param request           Datos del socio y preferencias
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO con el plan nutricional generado
     */
    @PostMapping("/generar")
    public ResponseEntity<PlanNutricionalGeneracionResponseDTO> generarPlanNutricional(
            @Valid @RequestBody PlanNutricionalGeneracionRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            PlanNutricionalGeneracionResponseDTO response = planNutricionalService.generarPlanNutricional(
                    request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar el plan nutricional: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un plan nutricional personalizado para el socio autenticado.
     * El socio NO necesita enviar idSocio, se obtiene del token.
     * 
     * @param request           Preferencias del socio (idSocio es opcional)
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO con el plan nutricional generado
     */
    @PostMapping("/mi-plan/generar")
    public ResponseEntity<PlanNutricionalGeneracionResponseDTO> generarMiPlanNutricional(
            @Valid @RequestBody PlanNutricionalGeneracionRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            PlanNutricionalGeneracionResponseDTO response = planNutricionalService.generarMiPlanNutricional(
                    request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar tu plan nutricional: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el plan nutricional activo del socio autenticado.
     * El socio solo necesita su token, no necesita enviar idSocio.
     * 
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO del plan nutricional activo
     */
    @GetMapping("/mi-plan")
    public ResponseEntity<PlanNutricionalGeneracionResponseDTO> obtenerMiPlan(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            PlanNutricionalGeneracionResponseDTO plan = planNutricionalService.obtenerMiPlanActivo(
                    userRol, userEmail);
            return ResponseEntity.ok(plan);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener tu plan nutricional", e);
        }
    }

    /**
     * Ajusta el plan nutricional activo del socio autenticado.
     * El socio puede ajustar su propio plan usando solo el token.
     * 
     * @param request   Datos de ajuste del plan
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Mapa con información sobre el ajuste realizado
     */
    @PutMapping("/mi-plan/ajustar")
    public ResponseEntity<Map<String, Object>> ajustarMiPlanNutricional(
            @Valid @RequestBody PlanNutricionalAjusteRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            Map<String, Object> response = planNutricionalService.ajustarMiPlanNutricional(
                    request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al ajustar tu plan nutricional: " + e.getMessage(), e);
        }
    }

    /**
     * Ajusta un plan nutricional específico (para entrenadores/admins)
     * 
     * @param idPlan    ID del plan nutricional a ajustar
     * @param request   Datos de ajuste del plan
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Mapa con información sobre el ajuste realizado
     */
    @PutMapping("/{idPlan}/ajustar")
    public ResponseEntity<Map<String, Object>> ajustarPlanNutricional(
            @PathVariable Long idPlan,
            @Valid @RequestBody PlanNutricionalAjusteRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            Map<String, Object> response = planNutricionalService.ajustarPlanNutricional(
                    idPlan, request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al ajustar el plan nutricional: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el plan nutricional activo de un socio específico (para entrenadores/admins)
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO del plan nutricional activo
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<PlanNutricionalGeneracionResponseDTO> obtenerPlanSocio(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            PlanNutricionalGeneracionResponseDTO plan = planNutricionalService.obtenerPlanActivo(
                    idSocio, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(plan);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener el plan nutricional del socio", e);
        }
    }

    /**
     * Obtiene todos los planes nutricionales del usuario autenticado
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Lista de planes nutricionales del socio
     */
    @GetMapping("/mis-planes")
    public ResponseEntity<List<PlanNutricionalGeneracionResponseDTO>> obtenerMisPlanes(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<PlanNutricionalGeneracionResponseDTO> planes = planNutricionalService.obtenerMisPlanes(
                    userRol, userEmail);
            return ResponseEntity.ok(planes);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener planes nutricionales", e);
        }
    }

    /**
     * Obtiene todos los planes nutricionales de un socio específico
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Lista de planes nutricionales del socio
     */
    @GetMapping("/socio/{idSocio}/todos")
    public ResponseEntity<List<PlanNutricionalGeneracionResponseDTO>> obtenerPlanesSocio(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<PlanNutricionalGeneracionResponseDTO> planes = planNutricionalService.obtenerPlanesSocio(
                    idSocio, userRol, userEmail);
            return ResponseEntity.ok(planes);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener planes nutricionales del socio", e);
        }
    }

    /**
     * Obtiene un plan nutricional específico por su ID
     * 
     * @param idPlan    ID del plan nutricional a consultar
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO del plan nutricional
     */
    @GetMapping("/{idPlan}")
    public ResponseEntity<PlanNutricionalGeneracionResponseDTO> obtenerPlanNutricional(
            @PathVariable Long idPlan,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            PlanNutricionalGeneracionResponseDTO plan = planNutricionalService.obtenerPlanNutricional(
                    idPlan, userRol, userEmail);
            return ResponseEntity.ok(plan);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener el plan nutricional", e);
        }
    }

    /**
     * Obtiene el historial de modificaciones de un plan nutricional específico
     * 
     * @param idPlan    ID del plan nutricional a consultar
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return Lista de DTOs con el historial de modificaciones
     */
    @GetMapping("/{idPlan}/historial")
    public ResponseEntity<List<PlanNutricionalHistorialResponseDTO>> obtenerHistorialPlan(
            @PathVariable Long idPlan,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<PlanNutricionalHistorialResponseDTO> historial = planNutricionalService.obtenerHistorialPlan(
                    idPlan, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(historial);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener historial", e);
        }
    }
}