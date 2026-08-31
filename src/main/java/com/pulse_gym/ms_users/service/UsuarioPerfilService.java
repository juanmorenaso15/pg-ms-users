package com.pulse_gym.ms_users.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.AuthClient;
import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.client.NotificacionClient;
import com.pulse_gym.lb_common.dto.AuthUserDTO;
import com.pulse_gym.lb_common.dto.CompletarPerfilRequestDTO;
import com.pulse_gym.lb_common.dto.EnvioEventoNotificacionDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.RegistroCompletoSocioRequestDTO;
import com.pulse_gym.lb_common.dto.RegistroCompletoSocioResponseDTO;
import com.pulse_gym.lb_common.dto.RegistroHuellaRequestDTO;
import com.pulse_gym.lb_common.dto.SocioMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilResponseDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilUpdateDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.enums.EnumEventoAsociado;
import com.pulse_gym.lb_common.enums.EnumNivelExperiencia;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.SocioMembresiaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioPerfilService {

    /** Cliente para interactuar con el servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /** Cliente para interactuar con el servicio de autenticación (Feign) */
    private final AuthClient authClient;

    /** Cliente de notificaciones */
    private final NotificacionClient notificacionClient;

    /** Repositorio para operaciones de base de datos de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    private final SocioMembresiaService socioMembresiaService;
    private final SocioMembresiaRepository socioMembresiaRepository;

    /**
     * Convierte una entidad UsuarioPerfil a UsuarioPerfilResponseDTO
     * 
     * @param usuario Entidad a convertir
     * @return DTO del usuario
     */
    private UsuarioPerfilResponseDTO convertirADTO(UsuarioPerfil usuario) {
        UsuarioPerfilResponseDTO dto = new UsuarioPerfilResponseDTO();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setSexo(usuario.getSexo());
        dto.setTelefono(usuario.getTelefono());
        dto.setDocumentoIdentidad(usuario.getDocumentoIdentidad());
        dto.setFotoUrl(usuario.getFotoUrl());
        dto.setFechaContratacion(usuario.getFechaContratacion());
        dto.setEspecialidad(usuario.getEspecialidad());
        dto.setAnosExperiencia(usuario.getAnosExperiencia());
        dto.setHorarioDisponibilidad(usuario.getHorarioDisponibilidad());
        dto.setTarifaHora(usuario.getTarifaHora());
        dto.setTurno(usuario.getTurno());
        dto.setFechaNacimiento(usuario.getFechaNacimiento());
        dto.setContactoEmergenciaNombre(usuario.getContactoEmergenciaNombre());
        dto.setContactoEmergenciaTelefono(usuario.getContactoEmergenciaTelefono());
        dto.setObjetivoPrincipal(usuario.getObjetivoPrincipal());
        dto.setNivelExperiencia(usuario.getNivelExperiencia());
        dto.setFechaRegistro(usuario.getFechaRegistro());
        dto.setIdSede(usuario.getIdSede());
        dto.setEstado(usuario.getEstado());
        dto.setBiometricDeviceId(usuario.getBiometricDeviceId());
        return dto;
    }

    /**
     * Enriquece el DTO con el rol del usuario consultando el servicio de
     * autenticación
     * 
     * @param dto     DTO a enriquecer
     * @param usuario Usuario del cual obtener el rol
     */
    private void enrichWithRol(UsuarioPerfilResponseDTO dto, UsuarioPerfil usuario) {
        try {
            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(usuario.getEmail());
            if (authUser != null) {
                if (authUser.getRol() != null) {
                    dto.setRol(authUser.getRol());
                }
                if (authUser.getUsername() != null) {
                    dto.setUsername(authUser.getUsername());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener datos de autenticación para el usuario: {}", usuario.getEmail(), e);
        }
    }

    /**
     * Genera un hash SHA-256 del deviceId para almacenamiento seguro
     * 
     * @param deviceId ID del dispositivo biométrico
     * @return Hash del deviceId o null si es vacío
     * @throws RuntimeException Si ocurre un error al generar el hash
     */
    private String generarHashDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(deviceId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error al generar hash SHA-256: {}", e.getMessage());
            throw new RuntimeException("Error interno al procesar la huella", e);
        }
    }

    /**
     * Valida la calidad de la huella biométrica
     * 
     * @param deviceId ID del dispositivo a validar
     * @return true si la calidad es aceptable, false en caso contrario
     */
    private boolean validarCalidadHuella(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Calidad de huella rechazada: deviceId nulo o vacío");
            return false;
        }
        if (deviceId.length() < 10) {
            log.warn("Calidad de huella rechazada: deviceId demasiado corto ({})", deviceId.length());
            return false;
        }
        boolean tieneNumero = deviceId.matches(".*\\d.*");
        if (!tieneNumero) {
            log.warn("Calidad de huella rechazada: deviceId no contiene números");
            return false;
        }
        log.info("Calidad de huella aceptada para deviceId: {}", deviceId);
        return true;
    }

    /**
     * Valida los campos obligatorios según el rol del usuario
     * 
     * @param request DTO con los datos del perfil
     * @param userRol Rol del usuario a completar
     * @throws RuntimeException Si falta algún campo obligatorio
     */
    private void validarCamposPorRol(CompletarPerfilRequestDTO request, String userRol) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new RuntimeException("El email es obligatorio");
        }
        if (request.getNombre() == null || request.getNombre().isEmpty()) {
            throw new RuntimeException("El nombre es obligatorio");
        }
        if (request.getApellido() == null || request.getApellido().isEmpty()) {
            throw new RuntimeException("El apellido es obligatorio");
        }
        if (request.getDocumentoIdentidad() == null || request.getDocumentoIdentidad().isEmpty()) {
            throw new RuntimeException("El documento de identidad es obligatorio");
        }
        if (request.getFotoUrl() == null || request.getFotoUrl().isEmpty()) {
            throw new RuntimeException("La URL de la foto es obligatoria");
        }
        if (request.getFechaNacimiento() == null) {
            throw new RuntimeException("La fecha de nacimiento es obligatoria");
        }
        if (request.getContactoEmergenciaNombre() == null || request.getContactoEmergenciaNombre().isEmpty()) {
            throw new RuntimeException("El nombre del contacto de emergencia es obligatorio");
        }
        if (request.getContactoEmergenciaTelefono() == null || request.getContactoEmergenciaTelefono().isEmpty()) {
            throw new RuntimeException("El teléfono del contacto de emergencia es obligatorio");
        }
        if (request.getIdSede() == null) {
            throw new RuntimeException("El ID de la sede es obligatorio");
        }

        if (EnumRol.socio.name().equals(userRol) || EnumRol.entrenador.name().equals(userRol)) {
            if (request.getObjetivoPrincipal() == null || request.getObjetivoPrincipal().isEmpty()) {
                throw new RuntimeException("El objetivo principal es obligatorio para socios y entrenadores");
            }
            if (request.getNivelExperiencia() == null) {
                throw new RuntimeException("El nivel de experiencia es obligatorio para socios y entrenadores");
            }
        }

        if (EnumRol.entrenador.name().equals(userRol)) {
            if (request.getFechaContratacion() == null) {
                throw new RuntimeException("La fecha de contratación es obligatoria para entrenadores");
            }
            if (request.getEspecialidad() == null || request.getEspecialidad().isEmpty()) {
                throw new RuntimeException("La especialidad es obligatoria para entrenadores");
            }
            if (request.getAnosExperiencia() == null) {
                throw new RuntimeException("Los años de experiencia son obligatorios para entrenadores");
            }
            if (request.getHorarioDisponibilidad() == null || request.getHorarioDisponibilidad().isEmpty()) {
                throw new RuntimeException("El horario de disponibilidad es obligatorio para entrenadores");
            }
            if (request.getTarifaHora() == null) {
                throw new RuntimeException("La tarifa por hora es obligatoria para entrenadores");
            }
            if (request.getTurno() == null) {
                throw new RuntimeException("El turno es obligatorio para entrenadores");
            }
        }

        if (EnumRol.administrador.name().equals(userRol)) {
            if (request.getFechaContratacion() == null) {
                throw new RuntimeException("La fecha de contratación es obligatoria para administradores");
            }
        }

        if (EnumRol.recepcionista.name().equals(userRol)) {
            if (request.getFechaContratacion() == null) {
                throw new RuntimeException("La fecha de contratación es obligatoria para recepcionistas");
            }
            if (request.getTurno() == null) {
                throw new RuntimeException("El turno es obligatorio para recepcionistas");
            }
        }
    }

    /**
     * Completa el perfil de un usuario después del registro
     * 
     * @param request   Datos del perfil a completar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Mensaje de confirmación
     * @throws RuntimeException               Si el usuario ya tiene perfil o faltan
     *                                        datos
     * @throws SecurityAuthorizationException Si el rol no está autorizado
     */
    @Transactional
    public MessegeGlobalDTO completarPerfil(CompletarPerfilRequestDTO request,
            String userRol, String userEmail) {

        boolean esAdmin = EnumRol.administrador.name().equals(userRol);
        boolean esRecepcionista = EnumRol.recepcionista.name().equals(userRol);
        boolean esEntrenador = EnumRol.entrenador.name().equals(userRol);
        boolean esSocio = EnumRol.socio.name().equals(userRol);
        boolean esAdminORecepcionistaOEntrenador = esAdmin || esRecepcionista || esEntrenador;

        String emailUsuarioACompletar;

        if (esSocio) {
            emailUsuarioACompletar = userEmail;
            if (emailUsuarioACompletar == null || emailUsuarioACompletar.trim().isEmpty()) {
                throw new RuntimeException("No se pudo obtener el email del usuario autenticado");
            }
            if (request.getEmail() != null && !request.getEmail().isEmpty()) {
                throw new RuntimeException("Los socios no pueden especificar un email en la solicitud");
            }
        } else if (esAdminORecepcionistaOEntrenador) {
            emailUsuarioACompletar = request.getEmail();
            if (emailUsuarioACompletar == null || emailUsuarioACompletar.trim().isEmpty()) {
                throw new RuntimeException("El email del usuario a completar es obligatorio para este rol");
            }
        } else {
            throw new SecurityAuthorizationException("Rol no autorizado para completar perfiles");
        }

        AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(emailUsuarioACompletar);
        if (authUser == null) {
            throw new RuntimeException("El usuario con email " + emailUsuarioACompletar + " no existe en el sistema");
        }

        if (usuarioRepository.findByEmail(emailUsuarioACompletar).isPresent()) {
            throw new RuntimeException("El usuario ya tiene un perfil completado");
        }

        EnumRol rolUsuarioACompletar = authUser.getRol();
        validarCamposPorRol(request, rolUsuarioACompletar.name());

        UsuarioPerfil usuario = new UsuarioPerfil();
        usuario.setEmail(emailUsuarioACompletar);
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setSexo(request.getSexo());
        usuario.setTelefono(request.getTelefono());
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        usuario.setFotoUrl(request.getFotoUrl());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setContactoEmergenciaNombre(request.getContactoEmergenciaNombre());
        usuario.setContactoEmergenciaTelefono(request.getContactoEmergenciaTelefono());
        usuario.setIdSede(request.getIdSede());
        usuario.setEstado(EnumEstadoUsuario.ACTIVO);

        if (EnumRol.socio.name().equals(rolUsuarioACompletar.name())) {
            usuario.setObjetivoPrincipal(request.getObjetivoPrincipal());
            usuario.setNivelExperiencia(request.getNivelExperiencia());
            usuario.setEspecialidad(null);
            usuario.setAnosExperiencia(null);
            usuario.setHorarioDisponibilidad(null);
            usuario.setTarifaHora(null);
            usuario.setTurno(null);
            usuario.setFechaContratacion(null);
        } else if (EnumRol.entrenador.name().equals(rolUsuarioACompletar.name())) {
            usuario.setObjetivoPrincipal(request.getObjetivoPrincipal());
            usuario.setNivelExperiencia(request.getNivelExperiencia());
            usuario.setEspecialidad(request.getEspecialidad());
            usuario.setAnosExperiencia(request.getAnosExperiencia());
            usuario.setHorarioDisponibilidad(request.getHorarioDisponibilidad());
            usuario.setTarifaHora(request.getTarifaHora());
            usuario.setTurno(request.getTurno());
            usuario.setFechaContratacion(request.getFechaContratacion());
        } else if (EnumRol.administrador.name().equals(rolUsuarioACompletar.name())) {
            usuario.setFechaContratacion(request.getFechaContratacion());
            usuario.setEspecialidad(null);
            usuario.setAnosExperiencia(null);
            usuario.setHorarioDisponibilidad(null);
            usuario.setTarifaHora(null);
            usuario.setTurno(null);
            usuario.setObjetivoPrincipal(null);
            usuario.setNivelExperiencia(EnumNivelExperiencia.intermedio);
        } else if (EnumRol.recepcionista.name().equals(rolUsuarioACompletar.name())) {
            usuario.setFechaContratacion(request.getFechaContratacion());
            usuario.setTurno(request.getTurno());
            usuario.setEspecialidad(null);
            usuario.setAnosExperiencia(null);
            usuario.setHorarioDisponibilidad(null);
            usuario.setTarifaHora(null);
            usuario.setObjetivoPrincipal(null);
            usuario.setNivelExperiencia(EnumNivelExperiencia.intermedio);
        } else {
            throw new SecurityAuthorizationException("Rol no válido para completar perfil");
        }

        try {
            usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            String errorMessage = e.getMostSpecificCause().getMessage();
            if (errorMessage.contains("documento_identidad")) {
                throw new RuntimeException("El documento de identidad ya está registrado por otro usuario");
            } else if (errorMessage.contains("email")) {
                throw new RuntimeException("El email ya está registrado por otro usuario");
            } else {
                throw new RuntimeException("Error de integridad de datos: " + errorMessage);
            }
        }

        enviarNotificacionBienvenida(usuario);

        return new MessegeGlobalDTO("Perfil completado correctamente");
    }

    /**
     * Envía una notificación de bienvenida al usuario completando el perfil
     * 
     * @param usuario Usuario al que enviar la notificación
     */
    private void enviarNotificacionBienvenida(UsuarioPerfil usuario) {
        try {
            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(usuario.getEmail());
            if (authUser == null) {
                return;
            }

            EnvioEventoNotificacionDTO eventoDTO = new EnvioEventoNotificacionDTO();
            eventoDTO.setUsuarioId(authUser.getId());
            eventoDTO.setEvento(EnumEventoAsociado.WELCOME);
            eventoDTO.setVariablesAdicionales(java.util.Map.of(
                    "nombre", usuario.getNombre(),
                    "apellido", usuario.getApellido() != null ? usuario.getApellido() : ""));
            notificacionClient.enviarPorEvento(eventoDTO);
        } catch (Exception e) {
            // Error silencioso, no interrumpir el flujo principal
        }
    }

    /**
     * Obtiene todos los usuarios activos
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de usuarios activos
     */
    @Transactional(readOnly = true)
    public List<UsuarioPerfilResponseDTO> obtenerTodosLosUsuariosActivo(String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        return usuarioRepository.findByEstado(EnumEstadoUsuario.ACTIVO).stream()
                .map(usuario -> {
                    UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
                    enrichWithRol(dto, usuario);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los usuarios inactivos
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de usuarios inactivos
     */
    @Transactional(readOnly = true)
    public List<UsuarioPerfilResponseDTO> obtenerTodosLosUsuariosInactivo(String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        return usuarioRepository.findByEstado(EnumEstadoUsuario.INACTIVO).stream()
                .map(usuario -> {
                    UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
                    enrichWithRol(dto, usuario);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los usuarios (activos e inactivos)
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de todos los usuarios
     */
    @Transactional(readOnly = true)
    public List<UsuarioPerfilResponseDTO> obtenerTodosLosUsuarios(String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        return usuarioRepository.findAll().stream()
                .map(usuario -> {
                    UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
                    enrichWithRol(dto, usuario);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un usuario por email (uso interno)
     * 
     * @param email Email del usuario
     * @return DTO del usuario
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obtenerUsuarioPorEmailInterno(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("El email del usuario no puede ser nulo o vacio");
        }

        UsuarioPerfil usuario = usuarioRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email));

        UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
        enrichWithRol(dto, usuario);
        return dto;
    }

    /**
     * Obtiene un usuario por ID
     * 
     * @param idUsuario ID del usuario
     * @param userRol   Rol del usuario autenticado
     * @return DTO del usuario
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obtenerUsuarioPorId(Long idUsuario, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        if (idUsuario == null) {
            throw new RuntimeException("El ID del usuario no puede ser nulo");
        }

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
        enrichWithRol(dto, usuario);

        return dto;
    }

    /**
     * Obtiene un usuario por número de documento
     * 
     * @param documentoIdentidad Número de documento
     * @param userRol            Rol del usuario autenticado
     * @return DTO del usuario
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obtenerUsuarioPorNumeroDocumento(String documentoIdentidad, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        if (documentoIdentidad == null || documentoIdentidad.trim().isEmpty()) {
            throw new RuntimeException("El número de documento no puede ser nulo o vacío");
        }

        UsuarioPerfil usuario = usuarioRepository
                .findByDocumentoIdentidadAndEstado(documentoIdentidad, EnumEstadoUsuario.ACTIVO)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario no encontrado con número de documento: " + documentoIdentidad));

        UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
        enrichWithRol(dto, usuario);

        return dto;
    }

    /**
     * Obtiene usuarios por nombre (búsqueda exacta, case-insensitive)
     * 
     * @param nombre  Nombre del usuario
     * @param userRol Rol del usuario autenticado
     * @return Lista de usuarios que coinciden
     */
    @Transactional(readOnly = true)
    public List<UsuarioPerfilResponseDTO> obtenerUsuariosPorNombre(String nombre, String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new RuntimeException("El nombre del usuario no puede ser nulo o vacío");
        }

        String nombreLimpio = nombre.trim();

        List<UsuarioPerfil> usuarios = usuarioRepository
                .findByNombreIgnoreCaseAndEstado(nombreLimpio, EnumEstadoUsuario.ACTIVO);

        if (usuarios.isEmpty()) {
            throw new RuntimeException("No se encontraron usuarios con nombre: " + nombre);
        }

        return usuarios.stream()
                .map(usuario -> {
                    UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
                    enrichWithRol(dto, usuario);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Actualiza los campos básicos del usuario (comunes a todos los roles)
     * 
     * @param usuario Entidad a actualizar
     * @param dto     Datos con los nuevos valores
     */
    private void actualizarCamposBasicos(UsuarioPerfil usuario, UsuarioPerfilUpdateDTO dto) {
        if (dto.getNombre() != null)
            usuario.setNombre(dto.getNombre());
        if (dto.getApellido() != null)
            usuario.setApellido(dto.getApellido());
        if (dto.getSexo() != null)
            usuario.setSexo(dto.getSexo());
        if (dto.getTelefono() != null)
            usuario.setTelefono(dto.getTelefono());
        if (dto.getEmail() != null)
            usuario.setEmail(dto.getEmail());
        if (dto.getDocumentoIdentidad() != null)
            usuario.setDocumentoIdentidad(dto.getDocumentoIdentidad());
        if (dto.getFotoUrl() != null)
            usuario.setFotoUrl(dto.getFotoUrl());
        if (dto.getFechaNacimiento() != null)
            usuario.setFechaNacimiento(dto.getFechaNacimiento());
        if (dto.getContactoEmergenciaNombre() != null)
            usuario.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        if (dto.getContactoEmergenciaTelefono() != null)
            usuario.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        if (dto.getIdSede() != null)
            usuario.setIdSede(dto.getIdSede());
        if (dto.getObjetivoPrincipal() != null)
            usuario.setObjetivoPrincipal(dto.getObjetivoPrincipal());
        if (dto.getNivelExperiencia() != null)
            usuario.setNivelExperiencia(dto.getNivelExperiencia());
    }

    /**
     * Actualiza todos los campos del usuario (incluyendo campos específicos por
     * rol)
     * 
     * @param usuario Entidad a actualizar
     * @param dto     Datos con los nuevos valores
     */
    private void actualizarTodosLosCampos(UsuarioPerfil usuario, UsuarioPerfilUpdateDTO dto) {
        actualizarCamposBasicos(usuario, dto);

        if (dto.getFechaContratacion() != null)
            usuario.setFechaContratacion(dto.getFechaContratacion());
        if (dto.getEspecialidad() != null)
            usuario.setEspecialidad(dto.getEspecialidad());
        if (dto.getAnosExperiencia() != null)
            usuario.setAnosExperiencia(dto.getAnosExperiencia());
        if (dto.getHorarioDisponibilidad() != null)
            usuario.setHorarioDisponibilidad(dto.getHorarioDisponibilidad());
        if (dto.getTarifaHora() != null)
            usuario.setTarifaHora(dto.getTarifaHora());
        if (dto.getTurno() != null)
            usuario.setTurno(dto.getTurno());
    }

    /**
     * Actualiza un usuario existente
     * 
     * @param idUsuario  ID del usuario a actualizar
     * @param requestDTO Datos a actualizar
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Mensaje de confirmación
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     */
    @Transactional
    public MessegeGlobalDTO actualizarUsuario(Long idUsuario, UsuarioPerfilUpdateDTO requestDTO,
            String userRol, String userEmail) {

        if (idUsuario == null) {
            throw new RuntimeException("El ID del usuario no puede ser nulo");
        }

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        boolean esSocio = EnumRol.socio.name().equalsIgnoreCase(userRol);
        boolean esAdmin = EnumRol.administrador.name().equalsIgnoreCase(userRol);
        boolean esRecepcionista = EnumRol.recepcionista.name().equalsIgnoreCase(userRol);

        if (esSocio) {
            if (!usuario.getEmail().equals(userEmail)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede actualizar su propio perfil");
            }
            actualizarCamposBasicos(usuario, requestDTO);

        } else if (esAdmin || esRecepcionista) {
            actualizarTodosLosCampos(usuario, requestDTO);

        } else {
            throw new SecurityAuthorizationException("Rol no autorizado para actualizar usuarios: " + userRol);
        }

        usuarioRepository.save(usuario);
        return new MessegeGlobalDTO("Usuario actualizado correctamente");
    }

    /**
     * Cambia el estado de un usuario (activar/desactivar)
     * 
     * @param idUsuario ID del usuario
     * @param estado    Nuevo estado
     * @param userRol   Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO cambiarEstadoUsuario(Long idUsuario, EnumEstadoUsuario estado, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        if (idUsuario == null) {
            throw new RuntimeException("El ID del usuario no puede ser nulo");
        }

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        if (usuario.getEstado() == estado) {
            String mensajeActual = estado == EnumEstadoUsuario.ACTIVO
                    ? "El usuario ya está activo"
                    : "El usuario ya está inactivo";
            throw new RuntimeException(mensajeActual);
        }

        usuario.setEstado(estado);
        usuarioRepository.save(usuario);

        String mensaje = estado == EnumEstadoUsuario.ACTIVO
                ? "Usuario activado correctamente"
                : "Usuario desactivado correctamente";

        return new MessegeGlobalDTO(mensaje);
    }

    /**
     * Desactiva un usuario
     * 
     * @param idUsuario ID del usuario
     * @param userRol   Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO desactivarUsuario(Long idUsuario, String userRol) {
        return cambiarEstadoUsuario(idUsuario, EnumEstadoUsuario.INACTIVO, userRol);
    }

    /**
     * Activa un usuario
     * 
     * @param idUsuario ID del usuario
     * @param userRol   Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO activarUsuario(Long idUsuario, String userRol) {
        return cambiarEstadoUsuario(idUsuario, EnumEstadoUsuario.ACTIVO, userRol);
    }

    /**
     * Obtiene un usuario por ID (uso interno)
     * 
     * @param idUsuario ID del usuario
     * @return DTO del usuario
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO obtenerUsuarioPorIdInterno(Long idUsuario) {
        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));
        UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
        enrichWithRol(dto, usuario);
        return dto;
    }

    /**
     * Registra una huella biométrica para un usuario
     * 
     * @param idUsuario         ID del usuario
     * @param request           Datos de la huella
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO registrarHuella(Long idUsuario, RegistroHuellaRequestDTO request, String userRol,
            Long userIdAutenticado) {

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID:" + idUsuario));

        if (userRol.equals(EnumRol.socio.name())) {
            AuthUserDTO authUser = authClient.obtenerUsuarioPorId(userIdAutenticado);
            if (authUser == null) {
                throw new SecurityAuthorizationException("Usuario autenticado no encontrado");
            }
            if (!authUser.getEmail().equals(usuario.getEmail())) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede registrar su propia huella");
            }
        } else {
            ValidacionDeRoles.validarAdminORecepcionistaOSocio(userRol);
        }

        if (!validarCalidadHuella(request.getDeviceId())) {
            log.warn("Intento de registro de huella con calidad insuficiente para usuario ID: {}", idUsuario);
            throw new RuntimeException(
                    "La calidad de la huella no es suficiente. Intente nuevamente con una captura más clara.");
        }

        String hashDeviceId = generarHashDeviceId(request.getDeviceId());
        if (hashDeviceId == null) {
            throw new RuntimeException("Error al procesar la huella. Intente nuevamente.");
        }

        usuario.setBiometricDeviceId(hashDeviceId);
        usuarioRepository.save(usuario);

        log.info("Huella registrada correctamente para usuario ID: {} (hash: {})", idUsuario,
                hashDeviceId.substring(0, 10) + "...");
        return new MessegeGlobalDTO("Huella registrada correctamente");
    }

    /**
     * Reemplaza la huella biométrica de un usuario
     * 
     * @param idUsuario         ID del usuario
     * @param request           Datos de la nueva huella
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO reemplazarHuella(Long idUsuario, RegistroHuellaRequestDTO request, String userRol,
            Long userIdAutenticado) {

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (userRol.equals(EnumRol.socio.name())) {
            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorId(userIdAutenticado);
            if (authUser == null) {
                throw new SecurityAuthorizationException("Usuario autenticado no encontrado");
            }
            if (!authUser.getEmail().equals(usuario.getEmail())) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede reemplazar su propia huella");
            }
        } else {
            ValidacionDeRoles.validarAdminORecepcionistaOSocio(userRol);
        }

        if (!validarCalidadHuella(request.getDeviceId())) {
            log.warn("Intento de reemplazo de huella con calidad insuficiente para usuario ID: {}", idUsuario);
            throw new RuntimeException(
                    "La calidad de la huella no es suficiente. Intente nuevamente con una captura más clara.");
        }

        String hashDeviceId = generarHashDeviceId(request.getDeviceId());
        if (hashDeviceId == null) {
            throw new RuntimeException("Error al procesar la huella. Intente nuevamente.");
        }

        usuario.setBiometricDeviceId(hashDeviceId);
        usuarioRepository.save(usuario);

        log.info("Huella reemplazada correctamente para usuario ID: {}", idUsuario);
        return new MessegeGlobalDTO("Huella reemplazada correctamente");
    }

    /**
     * Elimina la huella biométrica de un usuario
     * 
     * @param idUsuario         ID del usuario
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO eliminarHuella(Long idUsuario, String userRol, Long userIdAutenticado) {

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (userRol.equals(EnumRol.socio.name())) {
            AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorId(userIdAutenticado);
            if (authUser == null) {
                throw new SecurityAuthorizationException("Usuario autenticado no encontrado");
            }
            if (!authUser.getEmail().equals(usuario.getEmail())) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede eliminar su propia huella");
            }
        } else {
            ValidacionDeRoles.validarAdminORecepcionistaOSocio(userRol);
        }

        usuario.setBiometricDeviceId(null);
        usuarioRepository.save(usuario);

        log.info("Huella eliminada correctamente para usuario ID: {}", idUsuario);
        return new MessegeGlobalDTO("Huella eliminada correctamente");
    }

    /**
     * Actualiza el estado de un usuario por email (uso interno)
     * 
     * @param email       Email del usuario
     * @param nuevoEstado Nuevo estado
     */
    @Transactional
    public void actualizarEstadoInternoPorEmail(String email, EnumEstadoUsuario nuevoEstado) {
        UsuarioPerfil usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Perfil de usuario no encontrado con email: " + email));

        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);
    }

    /**
     * Verifica que un usuario exista en el sistema de autenticación
     * 
     * @param email Email del usuario
     * @return Datos del usuario en autenticación
     * @throws RuntimeException Si el usuario no existe
     */
    public AuthUserDTO verificarUsuarioEnAuth(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("El email es obligatorio");
        }

        AuthUserDTO authUser = authServiceClient.obtenerUsuarioPorEmail(email.trim());

        if (authUser == null) {
            throw new RuntimeException("Usuario no encontrado en el sistema de autenticación");
        }

        return authUser;
    }

    /**
     * Consulta el perfil del usuario autenticado.
     * Usa el email del token para identificar al usuario.
     * 
     * @param userRol   Rol del usuario autenticado (cualquier rol)
     * @param userEmail Email del usuario autenticado (extraído del token)
     * @return DTO con los datos del perfil del usuario autenticado
     * @throws SecurityAuthorizationException
     * @throws RuntimeException
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilResponseDTO consultarMiPerfil(String userRol, String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        if (usuario.getEstado() != EnumEstadoUsuario.ACTIVO) {
            throw new RuntimeException("El usuario no está activo");
        }

        UsuarioPerfilResponseDTO dto = convertirADTO(usuario);
        enrichWithRol(dto, usuario);

        return dto;
    }

    /**
     * Actualiza el perfil del usuario autenticado.
     * Usa el email del token para identificar al usuario.
     * 
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado (extraído del token)
     * @param requestDTO Datos a actualizar del perfil
     * @return Mensaje de confirmación
     * @throws SecurityAuthorizationException Si el usuario no está autenticado
     * @throws RuntimeException               Si no se encuentra el usuario
     */
    @Transactional
    public MessegeGlobalDTO actualizarMiPerfil(String userRol, String userEmail, UsuarioPerfilUpdateDTO requestDTO) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + userEmail));

        if (usuario.getEstado() != EnumEstadoUsuario.ACTIVO) {
            throw new RuntimeException("El usuario no está activo");
        }

        if (requestDTO.getNombre() != null) {
            usuario.setNombre(requestDTO.getNombre());
        }
        if (requestDTO.getApellido() != null) {
            usuario.setApellido(requestDTO.getApellido());
        }
        if (requestDTO.getSexo() != null) {
            usuario.setSexo(requestDTO.getSexo());
        }
        if (requestDTO.getTelefono() != null) {
            usuario.setTelefono(requestDTO.getTelefono());
        }
        if (requestDTO.getEmail() != null) {
            if (!requestDTO.getEmail().equals(usuario.getEmail())) {
                if (usuarioRepository.findByEmail(requestDTO.getEmail()).isPresent()) {
                    throw new RuntimeException("El email ya está registrado por otro usuario");
                }
                usuario.setEmail(requestDTO.getEmail());
            }
        }
        if (requestDTO.getDocumentoIdentidad() != null) {
            if (usuarioRepository.findByDocumentoIdentidad(requestDTO.getDocumentoIdentidad())
                    .filter(u -> !u.getIdUsuario().equals(usuario.getIdUsuario()))
                    .isPresent()) {
                throw new RuntimeException("El documento de identidad ya está registrado por otro usuario");
            }
            usuario.setDocumentoIdentidad(requestDTO.getDocumentoIdentidad());
        }
        if (requestDTO.getFotoUrl() != null) {
            usuario.setFotoUrl(requestDTO.getFotoUrl());
        }
        if (requestDTO.getFechaNacimiento() != null) {
            usuario.setFechaNacimiento(requestDTO.getFechaNacimiento());
        }
        if (requestDTO.getContactoEmergenciaNombre() != null) {
            usuario.setContactoEmergenciaNombre(requestDTO.getContactoEmergenciaNombre());
        }
        if (requestDTO.getContactoEmergenciaTelefono() != null) {
            usuario.setContactoEmergenciaTelefono(requestDTO.getContactoEmergenciaTelefono());
        }
        if (requestDTO.getIdSede() != null) {
            usuario.setIdSede(requestDTO.getIdSede());
        }

        if (EnumRol.socio.name().equalsIgnoreCase(userRol)) {
            if (requestDTO.getObjetivoPrincipal() != null) {
                usuario.setObjetivoPrincipal(requestDTO.getObjetivoPrincipal());
            }
            if (requestDTO.getNivelExperiencia() != null) {
                usuario.setNivelExperiencia(requestDTO.getNivelExperiencia());
            }
        }

        usuarioRepository.save(usuario);

        log.info("Perfil actualizado correctamente para usuario: {}", usuario.getEmail());
        return new MessegeGlobalDTO("Perfil actualizado correctamente");
    }

    /**
     * Registra un socio completo en una sola operación (perfil, membresía y huella)
     * 
     * @param request           Datos del socio a registrar
     * @param userRol           Rol del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return DTO con los datos del registro completo
     */
    @Transactional
    public RegistroCompletoSocioResponseDTO registrarSocioCompleto(
            RegistroCompletoSocioRequestDTO request,
            String userRol,
            String userEmail,
            Long userIdAutenticado) {

        completarPerfil(request.getPerfil(), userRol, userEmail);

        String emailObjetivo = EnumRol.socio.name().equals(userRol) ? userEmail : request.getPerfil().getEmail();
        UsuarioPerfil usuarioPerfil = usuarioRepository.findByEmail(emailObjetivo)
                .orElseThrow(() -> new RuntimeException("Error al recuperar el perfil creado"));

        if (request.getAsignacionMembresia() != null) {
            request.getAsignacionMembresia().setIdSocio(usuarioPerfil.getIdUsuario());
            socioMembresiaService.asignarMembresia(request.getAsignacionMembresia(), userRol);
        } else if (request.getAsignacionMembresiaFlexible() != null) {
            request.getAsignacionMembresiaFlexible().setIdSocio(usuarioPerfil.getIdUsuario());
            socioMembresiaService.asignarMembresiaFlexible(request.getAsignacionMembresiaFlexible(), userRol);
        }

        boolean huellaRegistrada = false;
        if (request.getBiometricDeviceId() != null && !request.getBiometricDeviceId().trim().isEmpty()) {
            RegistroHuellaRequestDTO huellaDTO = new RegistroHuellaRequestDTO();
            huellaDTO.setDeviceId(request.getBiometricDeviceId());
            registrarHuella(usuarioPerfil.getIdUsuario(), huellaDTO, userRol, userIdAutenticado);
            huellaRegistrada = true;
        }

        UsuarioPerfilResponseDTO perfilDTO = convertirADTO(usuarioPerfil);
        enrichWithRol(perfilDTO, usuarioPerfil);

        SocioMembresiaResponseDTO membresiaDTO = socioMembresiaRepository
                .findMembresiaActivaBySocio(usuarioPerfil.getIdUsuario())
                .map(this::convertirAResponseDTODesdeSocioMembresia)
                .orElse(null);

        return RegistroCompletoSocioResponseDTO.builder()
                .mensaje("Registro de socio completado exitosamente en una sola operación")
                .perfil(perfilDTO)
                .membresia(membresiaDTO)
                .huellaRegistrada(huellaRegistrada)
                .build();
    }

    private SocioMembresiaResponseDTO convertirAResponseDTODesdeSocioMembresia(
            com.pulse_gym.lb_common.entity.user.SocioMembresia sm) {
        SocioMembresiaResponseDTO dto = new SocioMembresiaResponseDTO();
        dto.setIdSocioMembresia(sm.getIdSocioMembresia());
        dto.setIdSocio(sm.getSocio().getIdUsuario());
        dto.setNombreSocio(sm.getSocio().getNombre() + " " + sm.getSocio().getApellido());
        dto.setEmailSocio(sm.getSocio().getEmail());
        dto.setIdMembresia(sm.getMembresia().getIdMembresia());
        dto.setNombreMembresia(sm.getMembresia().getNombre());
        dto.setPrecioTotal(sm.getMembresia().getPrecioTotal());
        dto.setFechaInicio(sm.getFechaInicio());
        dto.setFechaVencimiento(sm.getFechaVencimiento());
        dto.setEstado(sm.getEstado().name());
        dto.setDiasRestantes(sm.getDiasRestantes());
        dto.setEstaActiva(sm.isActiva());
        dto.setEstaVencida(sm.isVencida());
        return dto;
    }
}