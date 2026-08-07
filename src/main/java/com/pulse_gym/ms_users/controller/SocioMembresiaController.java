package com.pulse_gym.ms_users.controller;

import java.util.List;

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

import com.pulse_gym.lb_common.dto.AsignarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.RenovarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.SocioMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.SuspenderMembresiaRequestDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;
import com.pulse_gym.ms_users.service.SocioMembresiaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/socios-membresias")
@RequiredArgsConstructor
public class SocioMembresiaController {

    /** El servicio de socio membresia */
    private final SocioMembresiaService socioMembresiaService;

    private final UsuarioPerfilRepository usuarioPerfilRepository;

    /**
     * Endpoint para asignar una membresía a un socio. Recibe un DTO con los datos
     * necesarios para la asignación y el rol del usuario que realiza la solicitud.
     * 
     * @param requestDTO DTO con los datos necesarios para asignar la membresía al
     *                   socio
     * @param userRol    Rol del usuario que realiza la solicitud, obtenido del
     *                   encabezado "X-User-Rol"
     * @return ResponseEntity con un mensaje global indicando el resultado de la
     *         operación y el código de estado HTTP correspondiente
     */
    @PostMapping("/asignar")
    public ResponseEntity<MessegeGlobalDTO> asignarMembresia(
            @Valid @RequestBody AsignarMembresiaRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = socioMembresiaService.asignarMembresia(requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al asignar membresía", e);
        }
    }

    /**
     * Endpoint para consultar todas las membresías de un socio.
     * 
     * @param idSocio           ID del socio a consultar (viene en la URL)
     * @param userRol           Rol del usuario autenticado (socio, administrador o
     *                          recepcionista) - header "X-User-Rol"
     * @param userIdAutenticado ID del usuario autenticado - header "X-User-Id"
     * @return Lista de membresías del socio con código HTTP 200, o excepción si no
     *         tiene permisos o no encuentra datos
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<SocioMembresiaResponseDTO>> consultarMembresiasSocio(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            List<SocioMembresiaResponseDTO> membresias = socioMembresiaService.consultarMembresiasSocio(
                    idSocio, userRol, userEmail);
            return ResponseEntity.ok(membresias);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar membresías", e);
        }
    }

    /**
     * Endpoint para renovar una membresía existente de un socio.
     * 
     * @param requestDTO        DTO con el idSocioMembresia de la membresía a
     *                          renovar
     * @param userRol           Rol del usuario autenticado - header "X-User-Rol"
     * @param userIdAutenticado ID del usuario autenticado - header "X-User-Id"
     * @return Mensaje de confirmación con la nueva fecha de vencimiento y código
     *         HTTP 200
     */
    @PutMapping("/renovar")
    public ResponseEntity<MessegeGlobalDTO> renovarMembresia(
            @Valid @RequestBody RenovarMembresiaRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = socioMembresiaService.renovarMembresia(requestDTO, userRol, userIdAutenticado);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al renovar membresía", e);
        }
    }

    /**
     * Endpoint para cancelar una membresía activa de un socio.
     * 
     * @param idSocioMembresia ID de la membresía a cancelar (viene en la URL)
     * @param motivo           Motivo de la cancelación (parámetro de consulta)
     * @param userRol          Rol del usuario autenticado - header "X-User-Rol"
     *                         (debe ser recepcionista)
     * @return Mensaje de confirmación de cancelación con el motivo incluido y
     *         código HTTP 200
     */
    @DeleteMapping("/{idSocioMembresia}/cancelar")
    public ResponseEntity<MessegeGlobalDTO> cancelarMembresia(
            @PathVariable Long idSocioMembresia,
            @RequestParam String motivo,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = socioMembresiaService.cancelarMembresia(idSocioMembresia, motivo, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cancelar membresía", e);
        }
    }

    /**
     * Endpoint para suspender una membresía activa de un socio.
     * 
     * @param requestDTO DTO con el idSocioMembresia y el motivo de la suspensión
     * @param userRol    Rol del usuario autenticado - header "X-User-Rol" (debe ser
     *                   recepcionista)
     * @return Mensaje de confirmación de suspensión con el motivo incluido y código
     *         HTTP 200
     */
    @PutMapping("/suspender")
    public ResponseEntity<MessegeGlobalDTO> suspenderMembresia(
            @Valid @RequestBody SuspenderMembresiaRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = socioMembresiaService.suspenderMembresia(requestDTO, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al suspender membresía", e);
        }
    }

    /**
     * Endpoint para obtener la membresía activa de un socio específico.
     * 
     * @param idSocio           ID del socio a consultar (viene en la URL)
     * @param userRol           Rol del usuario autenticado - header "X-User-Rol"
     * @param userIdAutenticado ID del usuario autenticado - header "X-User-Id"
     * @return DTO con los datos de la membresía activa del socio, o null si no
     *         tiene ninguna activa, con código HTTP 200
     */
@GetMapping("/socio/{idSocio}/activa")
public ResponseEntity<SocioMembresiaResponseDTO> obtenerMembresiaActiva(
        @PathVariable Long idSocio,
        @RequestHeader(value = "X-User-Rol", required = false) String userRol,
        @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    
    try {
        // Consultar todas las membresías del socio usando email
        List<SocioMembresiaResponseDTO> membresias = socioMembresiaService.consultarMembresiasSocio(
                idSocio, userRol, userEmail);
        
        // Filtrar la membresía activa
        SocioMembresiaResponseDTO activa = membresias.stream()
                .filter(SocioMembresiaResponseDTO::getEstaActiva)
                .findFirst()
                .orElse(null);
        
        return ResponseEntity.ok(activa);
        
    } catch (SecurityAuthorizationException e) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        
    } catch (RuntimeException e) {
        if (e.getMessage().contains("no encontrado")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        
    } catch (Exception e) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Error al obtener membresía activa", 
            e
        );
    }
}
    /**
     * Consultar estado de membresía desde app
     * 
     * @param userRol           Rol del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Estado de la membresía del socio autenticado
     */
    @GetMapping("/estado/mi-membresia")
    public ResponseEntity<EstadoMembresiaResponseDTO> consultarMiEstadoMembresia(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {

        try {
            UsuarioPerfil socio = usuarioPerfilRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

            EstadoMembresiaResponseDTO estado = socioMembresiaService.consultarEstadoMembresiaApp(
                    socio.getIdUsuario(), userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(estado);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar estado de membresía", e);
        }
    }

    /**
     * RF14.1: Consultar estado de membresía de un socio (Admin/Recepcionista)
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Estado de la membresía del socio
     */
    @GetMapping("/estado/socio/{idSocio}")
    public ResponseEntity<EstadoMembresiaResponseDTO> consultarEstadoMembresia(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {
            EstadoMembresiaResponseDTO estado = socioMembresiaService.consultarEstadoMembresiaApp(
                    idSocio, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(estado);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar estado de membresía", e);
        }
    }
}
