package com.pulse_gym.ms_users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.dto.AuthUserDTO;
import com.pulse_gym.lb_common.dto.CompletarPerfilRequestDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.RegistroCompletoSocioRequestDTO;
import com.pulse_gym.lb_common.dto.RegistroCompletoSocioResponseDTO;
import com.pulse_gym.lb_common.dto.RegistroHuellaRequestDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilResponseDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilUpdateDTO;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.UsuarioPerfilService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioPerfilController {

    /**
     * Servicio de usuarios para la lógica de negocio
     */
    private final UsuarioPerfilService usuarioService;

    /**
     * Servicio de autenticación para verificar usuarios
     */
    private final AuthServiceClient authServiceClient;

    @PostMapping("/completar-perfil")
    public ResponseEntity<MessegeGlobalDTO> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {

        try {

            MessegeGlobalDTO response = usuarioService.completarPerfil(request, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al completar el perfil", e);
        }
    }

    /**
     * Obtiene la lista de todos los usuarios registrados
     * 
     * @param userRol Rol del usuario que hace la petición (desde header X-User-Rol)
     * @return Lista de usuarios con sus datos completos
     */
    @GetMapping
    public ResponseEntity<List<UsuarioPerfilResponseDTO>> obtenerTodosLosUsuarios(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<UsuarioPerfilResponseDTO> usuarios = usuarioService.obtenerTodosLosUsuarios(userRol);
            return ResponseEntity.status(HttpStatus.OK).body(usuarios);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener la lista de usuarios",
                    e);
        }
    }

    /**
     * Obtiene la lista de usuarios activos registrados
     * 
     * @param userRol Rol del usuario que hace la petición (desde header X-User-Rol)
     * @return Lista de usuarios activos con sus datos completos
     */
    @GetMapping("/activo")
    public ResponseEntity<List<UsuarioPerfilResponseDTO>> obtenerTodosLosUsuariosActivos(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<UsuarioPerfilResponseDTO> usuarios = usuarioService.obtenerTodosLosUsuariosActivo(userRol);
            return ResponseEntity.status(HttpStatus.OK).body(usuarios);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener la lista de usuarios",
                    e);
        }
    }

    /**
     * Obtiene la lista de usuarios inactivos registrados
     * 
     * @param userRol Rol del usuario que hace la petición (desde header X-User-Rol)
     * @return Lista de usuarios inactivos con sus datos completos
     */
    @GetMapping("/inactivo")
    public ResponseEntity<List<UsuarioPerfilResponseDTO>> obtenerTodosLosUsuariosInactivos(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<UsuarioPerfilResponseDTO> usuarios = usuarioService.obtenerTodosLosUsuariosInactivo(userRol);
            return ResponseEntity.status(HttpStatus.OK).body(usuarios);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener la lista de usuarios",
                    e);
        }
    }

    /**
     * Obtiene un usuario por su ID único
     * 
     * @param idUsuario ID del usuario a obtener
     * @param userRol   Rol del usuario que hace la petición (desde header
     *                  X-User-Rol)
     * @return Datos completos del usuario encontrado o error si no existe
     */
    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioPerfilResponseDTO> obtenerUsuarioPorId(@PathVariable Long idUsuario,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            UsuarioPerfilResponseDTO usuario = usuarioService.obtenerUsuarioPorId(idUsuario, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(usuario);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener el usuario", e);
        }
    }

    /**
     * Obtiene una lista de usuarios que coinciden con el nombre proporcionado
     * (búsqueda por nombre)
     * 
     * @param nombre  Nombre del usuario a buscar
     * @param userRol Rol del usuario que hace la petición (desde header X-User-Rol)
     * @return Lista de usuarios que coinciden con el nombre proporcionado
     */
    @GetMapping("/buscar-por-nombre/{nombre}")
    public ResponseEntity<List<UsuarioPerfilResponseDTO>> obtenerUsuarioPorNombre(@PathVariable String nombre,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<UsuarioPerfilResponseDTO> usuarioPerfilResponseDTO = usuarioService.obtenerUsuariosPorNombre(nombre,
                    userRol);
            return ResponseEntity.status(HttpStatus.OK).body(usuarioPerfilResponseDTO);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener el usuario", e);
        }
    }

    /**
     * Actualiza completamente los datos de un usuario existente
     * 
     * @param idUsuario  ID del usuario a actualizar
     * @param requestDTO Datos actualizados del usuario
     * @param userRol    Rol del usuario que hace la petición (desde header
     *                   X-User-Rol)
     * @param userEmail  Email del usuario que hace la petición (desde header
     *                   X-User-Email)
     * @return Respuesta con mensaje de éxito o error
     */
    @PutMapping("/{idUsuario}")
    public ResponseEntity<MessegeGlobalDTO> actualizarUsuario(
            @PathVariable Long idUsuario,
            @RequestBody UsuarioPerfilUpdateDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = usuarioService.actualizarUsuario(idUsuario, requestDTO, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar el usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Actualiza parcialmente los datos de un usuario existente (solo los campos
     * proporcionados en el request)
     * 
     * @param idUsuario  ID del usuario a actualizar
     * @param requestDTO Datos actualizados del usuario (pueden ser solo algunos
     *                   campos)
     * @param userRol    Rol del usuario que hace la petición (desde header
     *                   X-User-Rol)
     * @param userEmail  Email del usuario que hace la petición (desde header
     *                   X-User-Email)
     * @return Respuesta con mensaje de éxito o error
     */
    @PatchMapping("/{idUsuario}")
    public ResponseEntity<MessegeGlobalDTO> actualizarUsuarioParcial(
            @PathVariable Long idUsuario,
            @RequestBody UsuarioPerfilUpdateDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = usuarioService.actualizarUsuario(idUsuario, requestDTO, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar el usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Cambia el estado de un usuario (ACTIVO/INACTIVO)
     * 
     * @param idUsuario ID del usuario a actualizar su estado
     * @param estado    Nuevo estado del usuario (EnumEstadoUsuario.ACTIVO o
     *                  EnumEstadoUsuario.INACTIVO)
     * @param userRol   Rol del usuario que hace la petición (desde header
     *                  X-User-Rol)
     * @return Respuesta con mensaje de éxito o error
     */
    @PatchMapping("/{idUsuario}/estado")
    public ResponseEntity<MessegeGlobalDTO> cambiarEstadoUsuario(
            @PathVariable Long idUsuario,
            @RequestParam EnumEstadoUsuario estado,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = usuarioService.cambiarEstadoUsuario(idUsuario, estado, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al cambiar el estado del usuario", e);
        }
    }

    /**
     * 
     * @param idUsuario
     * @param request
     * @param userRol
     * @param userIdAutenticado
     * @return
     */
    @PostMapping("/{idUsuario}/huella/registrar")
    public ResponseEntity<MessegeGlobalDTO> registrarHuella(
            @PathVariable Long idUsuario,
            @Valid @RequestBody RegistroHuellaRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = usuarioService.registrarHuella(idUsuario, request, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{idUsuario}/huella/reemplazar")
    public ResponseEntity<MessegeGlobalDTO> reemplazarHuella(
            @PathVariable Long idUsuario,
            @Valid @RequestBody RegistroHuellaRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = usuarioService.reemplazarHuella(idUsuario, request, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{idUsuario}/huella/eliminar")
    public ResponseEntity<MessegeGlobalDTO> eliminarHuella(
            @PathVariable Long idUsuario,
            @Valid @RequestBody RegistroHuellaRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = usuarioService.eliminarHuella(idUsuario, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * VERIFICAR USUARIO EN EL SISTEMA DE AUTENTICACIÓN
     * 
     * @param email Email del usuario a verificar
     * @return Información del usuario si existe en Auth
     */
    @GetMapping("/auth/verificar-usuario")
    public ResponseEntity<AuthUserDTO> verificarUsuario(
            @RequestParam String email,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            if (userRol == null ||
                    (!userRol.equalsIgnoreCase("administrador") &&
                            !userRol.equalsIgnoreCase("entrenador") &&
                            !userRol.equalsIgnoreCase("recepcionista"))) {
                throw new SecurityAuthorizationException("No tienes permisos para verificar usuarios");
            }

            if (email == null || email.trim().isEmpty()) {
                throw new RuntimeException("El email es obligatorio");
            }

            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(email.trim());

            if (authUser == null) {
                throw new RuntimeException("Usuario no encontrado en el sistema de autenticación");
            }

            return ResponseEntity.status(HttpStatus.OK).body(authUser);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al verificar el usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Endpoint para consultar el perfil del usuario autenticado.
     * Usa el email del token para identificar al usuario.
     * 
     * @param userRol   Rol del usuario autenticado (header) - Extraído del token
     * @param userEmail Email del usuario autenticado (header) - Extraído del token
     * @return DTO con los datos del perfil del usuario autenticado
     * @throws SecurityAuthorizationException Si el usuario no está autenticado
     * @throws RuntimeException               Si no se encuentra el usuario
     */
    @GetMapping("/mi-perfil")
    public ResponseEntity<UsuarioPerfilResponseDTO> consultarMiPerfil(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new SecurityAuthorizationException("Usuario no autenticado");
            }

            UsuarioPerfilResponseDTO perfil = usuarioService.consultarMiPerfil(userRol, userEmail);
            return ResponseEntity.ok(perfil);

        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrado")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar tu perfil", e);
        }
    }

    /**
     * Endpoint para actualizar el perfil del usuario autenticado.
     * Usa el email del token para identificar al usuario.
     * 
     * @param userRol    Rol del usuario autenticado (header) - Extraído del token
     * @param userEmail  Email del usuario autenticado (header) - Extraído del token
     * @param requestDTO Datos a actualizar del perfil
     * @return Mensaje de confirmación
     * @throws SecurityAuthorizationException Si el usuario no está autenticado
     * @throws RuntimeException               Si no se encuentra el usuario
     */
    @PutMapping("/mi-perfil")
    public ResponseEntity<MessegeGlobalDTO> actualizarMiPerfil(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody UsuarioPerfilUpdateDTO requestDTO) {
        try {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new SecurityAuthorizationException("Usuario no autenticado");
            }

            MessegeGlobalDTO response = usuarioService.actualizarMiPerfil(userRol, userEmail, requestDTO);
            return ResponseEntity.ok(response);

        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrado")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar tu perfil", e);
        }
    }

    /**
     * Registra un socio completo en una sola operación (perfil, membresía y huella)
     * 
     * @param request           Datos del socio a registrar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @return DTO con los datos del registro completo
     */
    @PostMapping("/registro-completo")
    public ResponseEntity<RegistroCompletoSocioResponseDTO> registrarSocioCompleto(
            @Valid @RequestBody RegistroCompletoSocioRequestDTO request,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            RegistroCompletoSocioResponseDTO response = usuarioService.registrarSocioCompleto(
                    request, userRol, userEmail, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al realizar el registro unificado del usuario: " + e.getMessage(), e);
        }
    }
}