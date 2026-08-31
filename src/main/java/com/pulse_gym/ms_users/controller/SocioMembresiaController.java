package com.pulse_gym.ms_users.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import com.pulse_gym.lb_common.dto.AsignarMembresiaFlexibleRequestDTO;
import com.pulse_gym.lb_common.dto.AsignarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.MembresiaPorVencerDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.RenovarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.SocioAsignadoDTO;
import com.pulse_gym.lb_common.dto.SocioMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.SuspenderMembresiaRequestDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
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
                    e);
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

    /**
     * Endpoint para asignar una membresía flexible a un socio. Recibe un DTO con
     * los datos necesarios para la asignación y el rol del usuario que realiza la
     * solicitud.
     * 
     * @param requestDTO DTO con los datos necesarios para asignar la membresía
     *                   flexible al socio
     * @param userRol    Rol del usuario que realiza la solicitud, obtenido del
     *                   encabezado "X-User-Rol"
     * @return ResponseEntity con un mensaje global indicando el resultado de la
     *         operación y el código de estado HTTP correspondiente
     */
    @PostMapping("/asignar-flexible")
    public ResponseEntity<MessegeGlobalDTO> asignarMembresiaFlexible(
            @Valid @RequestBody AsignarMembresiaFlexibleRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = socioMembresiaService.asignarMembresiaFlexible(requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al asignar membresía flexible", e);
        }
    }

    /**
     * Obtiene todas las membresías activas que están por vencer (próximos 5 días)
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de membresías por vencer con su nivel de urgencia
     */
    @GetMapping("/por-vencer")
    public ResponseEntity<List<MembresiaPorVencerDTO>> obtenerMembresiasPorVencer(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);
            List<MembresiaPorVencerDTO> resultado = socioMembresiaService.obtenerMembresiasPorVencer();
            return ResponseEntity.ok(resultado);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener membresías por vencer", e);
        }
    }

    /**
     * Obtiene membresías por vencer en un rango específico de días
     * 
     * @param diasMinimo Días mínimos desde hoy (ej: 1)
     * @param diasMaximo Días máximos desde hoy (ej: 5)
     * @param userRol    Rol del usuario autenticado
     * @return Lista de membresías por vencer en el rango
     */
    @GetMapping("/por-vencer/rango")
    public ResponseEntity<List<MembresiaPorVencerDTO>> obtenerMembresiasPorVencerEnRango(
            @RequestParam(defaultValue = "1") int diasMinimo,
            @RequestParam(defaultValue = "5") int diasMaximo,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<MembresiaPorVencerDTO> resultado = socioMembresiaService
                    .obtenerMembresiasPorVencerEnRango(diasMinimo, diasMaximo, userRol);
            return ResponseEntity.ok(resultado);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener membresías por vencer en rango", e);
        }
    }

    /**
     * Endpoint para consultar todas las membresías del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header) - Extraído del token
     * @return Lista de membresías del socio autenticado con código HTTP 200
     * @throws SecurityAuthorizationException Si el usuario no es un socio
     * @throws RuntimeException               Si el socio no tiene membresías
     */
    @GetMapping("/mis-membresias")
    public ResponseEntity<List<SocioMembresiaResponseDTO>> consultarMisMembresias(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            List<SocioMembresiaResponseDTO> membresias = socioMembresiaService.consultarMisMembresias(userRol,
                    userEmail);
            return ResponseEntity.ok(membresias);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no tienes membresías")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar tus membresías", e);
        }
    }

    /**
     * Endpoint para consultar la membresía activa del socio autenticado.
     * 
     * @param userRol   Rol del usuario autenticado (header)
     * @param userEmail Email del usuario autenticado (header)
     * @return DTO con la membresía activa o null si no tiene ninguna
     */
    @GetMapping("/mi-membresia-activa")
    public ResponseEntity<SocioMembresiaResponseDTO> consultarMiMembresiaActiva(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            SocioMembresiaResponseDTO membresia = socioMembresiaService.consultarMiMembresiaActiva(userRol, userEmail);
            return ResponseEntity.ok(membresia);
        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar tu membresía activa", e);
        }
    }

    /**
     * Obtiene los socios asignados a una membresía específica paginados
     * 
     * @param idMembresia ID de la membresía
     * @param page        Número de página
     * @param size        Tamaño de página
     * @param userRol     Rol del usuario autenticado (header)
     * @return Página de socios asignados
     */
    @GetMapping("/membresia/{idMembresia}/socios-paginados")
    public ResponseEntity<Page<SocioAsignadoDTO>> consultarSociosAsignadosPaginados(
            @PathVariable Long idMembresia,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {

        ValidacionDeRoles.validarCualquierRol(userRol);
        Pageable pageable = PageRequest.of(page, size);
        Page<SocioAsignadoDTO> sociosPaginados = socioMembresiaService
                .obtenerSociosAsignadosPaginados(idMembresia, pageable);

        return ResponseEntity.ok(sociosPaginados);
    }
}
