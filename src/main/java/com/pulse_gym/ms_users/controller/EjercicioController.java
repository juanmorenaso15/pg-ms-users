package com.pulse_gym.ms_users.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.pulse_gym.lb_common.dto.EjercicioRequestDTO;
import com.pulse_gym.lb_common.dto.EjercicioResponseDTO;
import com.pulse_gym.lb_common.dto.EjercicioUpdateDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.EjercicioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ejercicios")
@RequiredArgsConstructor
public class EjercicioController {

    private final EjercicioService ejercicioService;

    /**
     * Crea un nuevo ejercicio
     * 
     * @param request DTO con los datos del ejercicio
     * @param userRol Rol del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> crearEjercicio(
            @Valid @RequestBody EjercicioRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            MessegeGlobalDTO response = ejercicioService.crearEjercicio(request, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear el ejercicio", e);
        }
    }

    /**
     * Consulta ejercicios aplicando filtros de búsqueda
     * 
     * @param nombre          Nombre del ejercicio (búsqueda parcial)
     * @param grupoMuscular   Grupo muscular del ejercicio
     * @param equipoNecesario Equipo necesario (búsqueda parcial)
     * @param dificultadMin   Dificultad mínima
     * @param dificultadMax   Dificultad máxima
     * @param userRol         Rol del usuario autenticado (header)
     * @return Mapa con resultado de la consulta
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> consultarEjercicios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String grupoMuscular,
            @RequestParam(required = false) String equipoNecesario,
            @RequestParam(required = false) Integer dificultadMin,
            @RequestParam(required = false) Integer dificultadMax,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<EjercicioResponseDTO> ejercicios = ejercicioService.consultarEjercicios(
                    nombre, grupoMuscular, equipoNecesario, dificultadMin, dificultadMax, userRol);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Consulta exitosa");
            response.put("count", ejercicios.size());
            response.put("data", ejercicios);

            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar ejercicios", e);
        }
    }

    /**
     * Obtiene un ejercicio por su ID
     * 
     * @param id      ID del ejercicio a consultar
     * @param userRol Rol del usuario autenticado (header)
     * @return DTO del ejercicio
     */
    @GetMapping("/{id}")
    public ResponseEntity<EjercicioResponseDTO> obtenerEjercicioPorId(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            EjercicioResponseDTO ejercicio = ejercicioService.obtenerEjercicioPorId(id, userRol);
            return ResponseEntity.ok(ejercicio);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener el ejercicio", e);
        }
    }

    /**
     * Actualiza un ejercicio existente
     * 
     * @param id      ID del ejercicio a actualizar
     * @param request DTO con los datos a actualizar
     * @param userRol Rol del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessegeGlobalDTO> actualizarEjercicio(
            @PathVariable Long id,
            @Valid @RequestBody EjercicioUpdateDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = ejercicioService.actualizarEjercicio(id, request, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el ejercicio", e);
        }
    }

    /**
     * Desactiva un ejercicio (eliminación lógica)
     * 
     * @param id      ID del ejercicio a desactivar
     * @param userRol Rol del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessegeGlobalDTO> eliminarEjercicio(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = ejercicioService.eliminarEjercicio(id, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar el ejercicio", e);
        }
    }

    /**
     * Obtiene la lista de grupos musculares válidos
     * 
     * @param userRol Rol del usuario autenticado (header)
     * @return Mapa con la lista de grupos musculares
     */
    @GetMapping("/grupos-musculares")
    public ResponseEntity<Map<String, Object>> obtenerGruposMusculares(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<String> grupos = ejercicioService.obtenerGruposMusculares(userRol);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", grupos);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener grupos musculares", e);
        }
    }

    /**
     * Obtiene la lista de equipos necesarios disponibles
     * 
     * @param userRol Rol del usuario autenticado (header)
     * @return Mapa con la lista de equipos
     */
    @GetMapping("/equipos")
    public ResponseEntity<Map<String, Object>> obtenerEquiposNecesarios(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<String> equipos = ejercicioService.obtenerEquiposNecesarios(userRol);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", equipos);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener equipos necesarios", e);
        }
    }
}
