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
import com.pulse_gym.lb_common.dto.RegistroHuellaRequestDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilRequestDTO;
import com.pulse_gym.lb_common.dto.UsuarioPerfilResponseDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.enums.EnumEventoAsociado;
import com.pulse_gym.lb_common.enums.EnumNivelExperiencia;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
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

    /**
     * Repositorio para operaciones de base de datos de usuarios
     */
    private final UsuarioPerfilRepository usuarioRepository;

    /**
     * Convierte una entidad UsuarioPerfil a su correspondiente DTO de respuesta
     * 
     * @param usuario Entidad de usuario a convertir (no puede ser nulo)
     * @return DTO con todos los datos del usuario mapeados desde la entidad
     */
    private UsuarioPerfilResponseDTO convertirADTO(UsuarioPerfil usuario) {
        UsuarioPerfilResponseDTO dto = new UsuarioPerfilResponseDTO();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
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

    private void enrichWithRol(UsuarioPerfilResponseDTO dto, UsuarioPerfil usuario) {
        EnumRol rol = authServiceClient.obtenerRolPorEmail(usuario.getEmail());
        if (rol != null) {
            dto.setRol(rol);
        }
    }

    /**
     * Genera un hash SHA-256 del deviceId para almacenar de forma segura.
     * 
     * @param deviceId Identificador del dispositivo biométrico (plano)
     * @return Hash SHA-256 en formato hexadecimal, o null si deviceId es null/vacío
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
     * Valida la calidad de la huella digital (simulación).
     * 
     * @param deviceId Identificador del dispositivo biométrico
     * @return true si la calidad es aceptable, false en caso contrario
     */
    private boolean validarCalidadHuella(String deviceId) {
        // Simulación: consideramos que una huella es de buena calidad si:
        // 1. No es nula ni vacía
        // 2. Tiene una longitud mínima de 10 caracteres (para evitar IDs muy cortos)
        // 3. Contiene al menos un número (para simular que tiene información variada)
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
     * Valida los campos obligatorios según el rol del usuario.
     * 
     * @param request DTO con los datos del perfil a completar
     * @param userRol Rol del usuario autenticado
     */
    private void validarCamposPorRol(CompletarPerfilRequestDTO request, String userRol) {
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
     * Completa el perfil de un usuario que ya existe en el sistema de autenticación
     * 
     * @param email      Email del usuario (identificador único)
     * @param requestDTO Datos del perfil a completar
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado (para validar que solo
     *                   complete su propio perfil)
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO completarPerfil(String email, CompletarPerfilRequestDTO request,
            String userRol, String userEmail) {

        if (!userEmail.equals(email)) {
            throw new SecurityAuthorizationException("Acceso denegado. Solo puede completar su propio perfil");
        }

        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("El usuario ya tiene un perfil completado");
        }

        validarCamposPorRol(request, userRol);

        UsuarioPerfil usuario = new UsuarioPerfil();
        usuario.setEmail(email);
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setTelefono(request.getTelefono());
        usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        usuario.setFotoUrl(request.getFotoUrl());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setContactoEmergenciaNombre(request.getContactoEmergenciaNombre());
        usuario.setContactoEmergenciaTelefono(request.getContactoEmergenciaTelefono());
        usuario.setIdSede(request.getIdSede());

        usuario.setEstado(EnumEstadoUsuario.ACTIVO);

        if (EnumRol.socio.name().equals(userRol)) {
            usuario.setObjetivoPrincipal(request.getObjetivoPrincipal());
            usuario.setNivelExperiencia(request.getNivelExperiencia());
            usuario.setEspecialidad(null);
            usuario.setAnosExperiencia(null);
            usuario.setHorarioDisponibilidad(null);
            usuario.setTarifaHora(null);
            usuario.setTurno(null);
            usuario.setFechaContratacion(null);
        } else if (EnumRol.entrenador.name().equals(userRol)) {
            usuario.setObjetivoPrincipal(request.getObjetivoPrincipal());
            usuario.setNivelExperiencia(request.getNivelExperiencia());
            usuario.setEspecialidad(request.getEspecialidad());
            usuario.setAnosExperiencia(request.getAnosExperiencia());
            usuario.setHorarioDisponibilidad(request.getHorarioDisponibilidad());
            usuario.setTarifaHora(request.getTarifaHora());
            usuario.setTurno(request.getTurno());
            usuario.setFechaContratacion(request.getFechaContratacion());
        } else if (EnumRol.administrador.name().equals(userRol)) {
            usuario.setFechaContratacion(request.getFechaContratacion());
            usuario.setEspecialidad(null);
            usuario.setAnosExperiencia(null);
            usuario.setHorarioDisponibilidad(null);
            usuario.setTarifaHora(null);
            usuario.setTurno(null);
            usuario.setObjetivoPrincipal(null);
            usuario.setNivelExperiencia(EnumNivelExperiencia.intermedio);
        } else if (EnumRol.recepcionista.name().equals(userRol)) {
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
        usuarioRepository.save(usuario);

        enviarNotificacionBienvenida(usuario);

        return new MessegeGlobalDTO("Perfil completado correctamente");
    }

    /**
     * Envía notificación de bienvenida al completar perfil
     * 
     * @param usuario Usuario creado
     */
    private void enviarNotificacionBienvenida(UsuarioPerfil usuario) {
        try {
            // Obtener ID de auth del usuario
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
            // No fallar el registro si falla el envío de notificación
        }
    }

    /**
     * Obtiene la lista de todos los usuarios activos
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de DTOs con los datos de los usuarios activos
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
     * Obtiene un usuario activo por su ID
     * 
     * @param idUsuario ID del usuario a buscar
     * @param userRol   Rol del usuario autenticado
     * @return DTO con los datos del usuario
     */
    /**
     * Obtiene el perfil de usuario por email sin validacion de rol para integracion
     * interna
     *
     * @param email Email del usuario
     * @return DTO con los datos del perfil
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
     * Obtiene un usuario activo por su número de documento
     * 
     * @param documentoIdentidad Número de documento del usuario
     * @param userRol            Rol del usuario autenticado
     * @return DTO con los datos del usuario
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
     * Obtiene un usuario activo por su nombre
     * 
     * @param nombre  Nombre del usuario a buscar
     * @param userRol Rol del usuario autenticado
     * @return DTO con los datos del usuario
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
     * Actualiza los datos de un usuario según el rol
     * - Admin/Recepcionista: Pueden actualizar todos los campos
     * - Socio: Solo puede actualizar datos de contacto (teléfono, email, dirección)
     * 
     * @param idUsuario  ID del usuario a actualizar
     * @param requestDTO Datos actualizados
     * @param userRol    Rol del usuario que hace la petición
     * @param userEmail  Email del usuario autenticado (para validar que socio solo
     *                   actualice su propio perfil)
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO actualizarUsuario(Long idUsuario, UsuarioPerfilRequestDTO requestDTO,
            String userRol, String userEmail) {
        if (idUsuario == null) {
            throw new RuntimeException("El ID del usuario no puede ser nulo");
        }

        UsuarioPerfil usuarioExistente = usuarioRepository.findByIdAndEstado(idUsuario, EnumEstadoUsuario.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo con ID: " + idUsuario));

        if (usuarioExistente == null) {
            throw new RuntimeException("Error: Usuario no encontrado");

        }

        if (userRol.equals("socio")) {

            if (!usuarioExistente.getEmail().equals(userEmail)) {
                throw new SecurityAuthorizationException(
                        "Acceso denegado. Solo puede actualizar su propio perfil");
            }

            actualizarDatosContacto(usuarioExistente, requestDTO);

        } else if (userRol.equals("administrador") || userRol.equals("recepcionista")) {

            actualizarTodosLosCampos(usuarioExistente, requestDTO);

        } else {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Rol no autorizado para actualizar usuarios: " + userRol);
        }

        usuarioRepository.save(usuarioExistente);
        return new MessegeGlobalDTO("Usuario actualizado correctamente");
    }

    /**
     * Actualiza solo los datos de contacto del usuario (teléfono y email)
     * 
     * @param usuario    Entidad del usuario a actualizar
     * @param requestDTO DTO con los nuevos datos de contacto
     */
    private void actualizarDatosContacto(UsuarioPerfil usuario, UsuarioPerfilRequestDTO requestDTO) {

        if (requestDTO.getNombre() != null && !requestDTO.getNombre().isEmpty()) {
            usuario.setNombre(requestDTO.getNombre());
        }

        if (requestDTO.getApellido() != null && !requestDTO.getApellido().isEmpty()) {
            usuario.setApellido(requestDTO.getApellido());
        }

        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isEmpty()) {
            usuario.setEmail(requestDTO.getEmail());
        }
        if (requestDTO.getTelefono() != null) {
            usuario.setTelefono(requestDTO.getTelefono());
        }

        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isEmpty()) {
            usuario.setEmail(requestDTO.getEmail());
        }

        if (requestDTO.getDocumentoIdentidad() != null && !requestDTO.getDocumentoIdentidad().isEmpty()) {
            usuario.setDocumentoIdentidad(requestDTO.getDocumentoIdentidad());

        }
    }

    /**
     * Actualiza todos los campos del usuario (para administradores y
     * recepcionistas)
     * 
     * @param usuario    Entidad del usuario a actualizar
     * @param requestDTO DTO con los nuevos datos del usuario
     */
    private void actualizarTodosLosCampos(UsuarioPerfil usuario, UsuarioPerfilRequestDTO requestDTO) {

        if (requestDTO.getNombre() != null) {
            usuario.setNombre(requestDTO.getNombre());
        }

        if (requestDTO.getApellido() != null) {
            usuario.setApellido(requestDTO.getApellido());
        }

        if (requestDTO.getTelefono() != null) {
            usuario.setTelefono(requestDTO.getTelefono());
        }

        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isEmpty()) {

            usuario.setEmail(requestDTO.getEmail());
        }

        if (requestDTO.getDocumentoIdentidad() != null && !requestDTO.getDocumentoIdentidad().isEmpty()) {
            usuario.setDocumentoIdentidad(requestDTO.getDocumentoIdentidad());

        }
        if (requestDTO.getFotoUrl() != null) {
            usuario.setFotoUrl(requestDTO.getFotoUrl());
        }

        if (requestDTO.getFechaContratacion() != null) {
            usuario.setFechaContratacion(requestDTO.getFechaContratacion());
        }

        if (requestDTO.getEspecialidad() != null) {
            usuario.setEspecialidad(requestDTO.getEspecialidad());
        }

        if (requestDTO.getAnosExperiencia() != null) {
            usuario.setAnosExperiencia(requestDTO.getAnosExperiencia());
        }

        if (requestDTO.getHorarioDisponibilidad() != null) {
            usuario.setHorarioDisponibilidad(requestDTO.getHorarioDisponibilidad());
        }

        if (requestDTO.getTarifaHora() != null) {
            usuario.setTarifaHora(requestDTO.getTarifaHora());
        }

        if (requestDTO.getTurno() != null) {
            usuario.setTurno(requestDTO.getTurno());
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

        if (requestDTO.getObjetivoPrincipal() != null) {
            usuario.setObjetivoPrincipal(requestDTO.getObjetivoPrincipal());
        }

        if (requestDTO.getNivelExperiencia() != null) {
            usuario.setNivelExperiencia(requestDTO.getNivelExperiencia());
        }

        if (requestDTO.getIdSede() != null) {
            usuario.setIdSede(requestDTO.getIdSede());
        }
    }

    /**
     * Cambia el estado de un usuario (ACTIVO/INACTIVO)
     * 
     * @param idUsuario ID del usuario
     * @param estado    Nuevo estado del usuario
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
     * Desactiva un usuario (cambia a estado INACTIVO)
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
     * Activa un usuario (cambia a estado ACTIVO)
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
     * Servicio para obtener usuario sin validacion de roles para comunicacion entre
     * microservicios
     * 
     * @param idUsuario
     * @return
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
     * Registra una nueva huella digital para un socio.
     * Valida la calidad de la huella y almacena el hash del deviceId.
     * 
     * @param idUsuario         ID del usuario (socio)
     * @param request           DTO con el deviceId
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado (para validar que solo se
     *                          registre su propia huella)
     * @return Mensaje de confirmación
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     * @throws RuntimeException               Si la huella no es válida o el usuario
     *                                        no es socio
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

        // 4. Generar hash del deviceId
        String hashDeviceId = generarHashDeviceId(request.getDeviceId());
        if (hashDeviceId == null) {
            throw new RuntimeException("Error al procesar la huella. Intente nuevamente.");
        }

        // 5. Guardar el hash en lugar del deviceId plano
        usuario.setBiometricDeviceId(hashDeviceId);
        usuarioRepository.save(usuario);

        log.info("Huella registrada correctamente para usuario ID: {} (hash: {})", idUsuario,
                hashDeviceId.substring(0, 10) + "...");
        return new MessegeGlobalDTO("Huella registrada correctamente");
    }

    /**
     * Reemplaza una huella digital existente por una nueva.
     * Valida la calidad y genera un nuevo hash.
     * 
     * @param idUsuario         ID del usuario (socio)
     * @param request           DTO con el nuevo deviceId
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO reemplazarHuella(Long idUsuario, RegistroHuellaRequestDTO request, String userRol,
            Long userIdAutenticado) {

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar permisos
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
     * Elimina la huella digital de un socio.
     * 
     * @param idUsuario         ID del usuario (socio)
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO eliminarHuella(Long idUsuario, String userRol, Long userIdAutenticado) {

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar permisos
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
}