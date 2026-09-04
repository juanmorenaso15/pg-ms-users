package com.pulse_gym.ms_users.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /** Servicio de perfil médico */
    private final PerfilMedicoService perfilMedicoService;

    /**
     * Registra un nuevo perfil médico para un socio
     * 
     * @param requestDTO Datos del perfil médico
     * @param userRol    Rol del usuario autenticado (header)
     * @param userEmail  Email del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> registrarPerfilMedico(
            @Valid @RequestBody PerfilMedicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.registrarPerfilMedico(requestDTO, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException | IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar el perfil médico",
                    e);
        }
    }

    /**
     * Consulta perfiles médicos con búsqueda y paginación
     * 
     * @param busqueda  Búsqueda por nombre, apellido o documento
     * @param page      Número de página
     * @param size      Tamaño de página
     * @param sortBy    Campo de ordenamiento
     * @param direction Dirección de ordenamiento
     * @param userRol   Rol del usuario autenticado (header)
     * @return Página de perfiles médicos
     */
    @GetMapping
    public ResponseEntity<Page<PerfilMedicoResponseDTO>> consultarPerfilesMedicosPaginados(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idPerfilMedico") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<PerfilMedicoResponseDTO> resultado = perfilMedicoService.consultarPerfilesMedicosPaginados(busqueda,
                    pageable, userRol);
            return ResponseEntity.ok(resultado);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar los perfiles médicos", e);
        }
    }

    /**
     * Consulta el perfil médico del socio autenticado
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO del perfil médico
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar tu perfil médico",
                    e);
        }
    }

    /**
     * Consulta el perfil médico de un socio específico
     * 
     * @param idSocio   ID del socio
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO del perfil médico
     */
    @GetMapping("/{idSocio}")
    public ResponseEntity<PerfilMedicoResponseDTO> consultarPerfilMedico(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            PerfilMedicoResponseDTO perfil = perfilMedicoService.consultarPerfilMedico(idSocio, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(perfil);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar el perfil médico",
                    e);
        }
    }

    /**
     * Actualiza el perfil médico de un socio
     * 
     * @param idSocio    ID del socio
     * @param requestDTO Datos a actualizar
     * @param userRol    Rol del usuario autenticado (header)
     * @param userEmail  Email del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PutMapping("/{idSocio}")
    public ResponseEntity<MessegeGlobalDTO> actualizarPerfilMedico(
            @PathVariable Long idSocio,
            @Valid @RequestBody PerfilMedicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.actualizarPerfilMedico(idSocio, requestDTO, userRol,
                    userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar el perfil médico",
                    e);
        }
    }

    /**
     * Actualiza el perfil médico del socio autenticado usando solo el token
     * 
     * @param requestDTO Datos a actualizar
     * @param userRol    Rol del usuario autenticado (header)
     * @param userEmail  Email del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @PutMapping("/mi-perfil-medico")
    public ResponseEntity<MessegeGlobalDTO> actualizarMiPerfilMedico(
            @Valid @RequestBody PerfilMedicoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.actualizarMiPerfilMedico(requestDTO, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar tu perfil médico",
                    e);
        }
    }

    /**
     * Elimina (desactiva) el perfil médico de un socio
     * 
     * @param idSocio ID del socio
     * @param userRol Rol del usuario autenticado (header)
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/{idSocio}")
    public ResponseEntity<MessegeGlobalDTO> eliminarPerfilMedico(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = perfilMedicoService.eliminarPerfilMedico(idSocio, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar el perfil médico",
                    e);
        }
    }
}