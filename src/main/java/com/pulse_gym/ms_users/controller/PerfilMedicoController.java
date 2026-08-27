package com.pulse_gym.ms_users.controller;

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

import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PerfilMedicoRequestDTO;
import com.pulse_gym.lb_common.dto.PerfilMedicoResponseDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.PerfilMedicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios/perfil-medico")
@RequiredArgsConstructor
public class PerfilMedicoController {

    private final PerfilMedicoService perfilMedicoService;

    /**
     * Endpoint para registrar un nuevo perfil médico para un socio específico.
     * 
     * @param requestDTO DTO con los datos del perfil médico a registrar
     * @param userRol    Rol del usuario que realiza la acción (obtenido del token
     *                   de autenticación)
     * @return Mensaje de éxito o error en el registro del perfil médico
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> registrarPerfilMedico(
            @Valid @RequestBody PerfilMedicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.registrarPerfilMedico(requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar la certificación",
                    e);
        }
    }

    /**
     * Endpoint para consultar el perfil médico de un socio específico.
     * 
     * @param idSocio El ID del socio para el cual consultar el perfil médico
     * @param userRol El rol del usuario que realiza la acción (obtenido del token
     *                de autenticación)
     * @return El DTO con los datos del perfil médico consultado
     */
    @GetMapping("/{idSocio}")
    public ResponseEntity<PerfilMedicoResponseDTO> consultarPerfilMedico(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            PerfilMedicoResponseDTO perfil = perfilMedicoService.consultarPerfilMedico(idSocio, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(perfil);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar la certificación",
                    e);
        }
    }

    /**
     * Endpoint para consultar el perfil médico del socio autenticado
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header) - Extraído del token
     * @return El DTO con los datos del perfil médico del socio autenticado
     */
    @GetMapping("/mi-perfil-medico")
    public ResponseEntity<PerfilMedicoResponseDTO> consultarMiPerfilMedico(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            PerfilMedicoResponseDTO perfil = perfilMedicoService.consultarMiPerfilMedico(userRol, userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(perfil);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar el perfil médico del socio autenticado", e);
        }
    }

    /**
     * Endpoint para actualizar el perfil médico de un socio específico.
     * 
     * @param idSocio    El ID del socio para el cual actualizar el perfil médico
     * @param requestDTO El DTO con los datos del perfil médico a actualizar
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   token
     *                   de autenticación)
     * @return Un mensaje de éxito o error en la actualización del perfil médico
     */
    @PutMapping("/{idSocio}")
    public ResponseEntity<MessegeGlobalDTO> actualizarPerfilMedico(
            @PathVariable Long idSocio,
            @Valid @RequestBody PerfilMedicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.actualizarPerfilMedico(idSocio, requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar la certificación",
                    e);
        }
    }
}