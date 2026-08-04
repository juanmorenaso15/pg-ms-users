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

import com.pulse_gym.lb_common.dto.RutinaAjusteRequestDTO;
import com.pulse_gym.lb_common.dto.RutinaGeneracionRequestDTO;
import com.pulse_gym.lb_common.dto.RutinaGeneracionResponseDTO;
import com.pulse_gym.lb_common.dto.RutinaHistorialResponseDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.RutinaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rutinas")
@RequiredArgsConstructor
public class RutinaController {

    /** Servicio de rutinas para operaciones de negocio */
    private final RutinaService rutinaService;

    /**
     * Genera una rutina de entrenamiento personalizada
     * 
     * @param request           Datos del socio y preferencias
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @return DTO con la rutina generada
     */
    @PostMapping("/generar")
    public ResponseEntity<RutinaGeneracionResponseDTO> generarRutina(
            @Valid @RequestBody RutinaGeneracionRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            RutinaGeneracionResponseDTO response = rutinaService.generarRutinaIA(
                    request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar la rutina: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene las rutinas del usuario autenticado
     * 
     * @param userIdAutenticado ID del usuario autenticado (de auth - header)
     * @param userRol           Rol del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return Lista de rutinas del usuario
     */
    @GetMapping("/mis-rutinas")
    public ResponseEntity<List<RutinaGeneracionResponseDTO>> obtenerMisRutinas(
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<RutinaGeneracionResponseDTO> rutinas = rutinaService.obtenerMisRutinas(
                    userIdAutenticado, userRol, userEmail);
            return ResponseEntity.ok(rutinas);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener rutinas", e);
        }
    }

    /**
     * Obtiene las rutinas de un socio específico
     * 
     * @param idSocio           ID del socio a consultar (de usuario_perfil)
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (de auth - header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return Lista de rutinas del socio
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<RutinaGeneracionResponseDTO>> obtenerRutinasSocio(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<RutinaGeneracionResponseDTO> rutinas = rutinaService.obtenerRutinasSocio(
                    idSocio, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(rutinas);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener rutinas del socio", e);
        }
    }

    /**
     * Obtiene una rutina específica por su ID
     * 
     * @param idRutina          ID de la rutina a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (de auth - header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO de la rutina
     */
    @GetMapping("/{idRutina}")
    public ResponseEntity<RutinaGeneracionResponseDTO> obtenerRutina(
            @PathVariable Long idRutina,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            RutinaGeneracionResponseDTO rutina = rutinaService.obtenerRutina(
                    idRutina, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(rutina);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener la rutina", e);
        }
    }

    /**
     * 
     * Ajusta un detalle específico de una rutina
     * 
     * @param idRutina          ID de la rutina a ajustar
     * @param request           DTO con los datos a modificar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return Mapa con el resultado del ajuste
     */
    @PutMapping("/{idRutina}/ajustar")
    public ResponseEntity<Map<String, Object>> ajustarRutina(
            @PathVariable Long idRutina,
            @Valid @RequestBody RutinaAjusteRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            Map<String, Object> response = rutinaService.ajustarRutina(
                    idRutina, request, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(response);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al ajustar la rutina", e);
        }
    }

    /**
     * Obtiene el historial de versiones de una rutina
     * 
     * @param idRutina          ID de la rutina a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @return Lista del historial de versiones
     */
    @GetMapping("/{idRutina}/historial")
    public ResponseEntity<List<RutinaHistorialResponseDTO>> obtenerHistorialRutina(
            @PathVariable Long idRutina,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<RutinaHistorialResponseDTO> historial = rutinaService.obtenerHistorialRutina(
                    idRutina, userRol, userIdAutenticado, userEmail);
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