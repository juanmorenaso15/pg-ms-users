package com.pulse_gym.ms_users.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PerfilMedicoRequestDTO;
import com.pulse_gym.lb_common.dto.PerfilMedicoResponseDTO;
import com.pulse_gym.lb_common.entity.user.PerfilMedico;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoDocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.enums.EnumTipoDocumentoLegal;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.DocumentoLegalRepository;
import com.pulse_gym.ms_users.repository.PerfilMedicoRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PerfilMedicoService {

    /** Repositorio de perfiles médicos */
    private final PerfilMedicoRepository perfilMedicoRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de documentos legales */
    private final DocumentoLegalRepository documentoLegalRepository;

    /** Cliente Feign para consultar el servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /**
     * Valida que el socio tenga un consentimiento informado vigente
     * 
     * @param idSocio ID del socio a validar
     * @throws RuntimeException               Si el socio no existe
     * @throws SecurityAuthorizationException Si no tiene consentimiento informado
     *                                        vigente
     */
    private void validarConsentimientoInformado(Long idSocio) {
        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        EnumRol rolSocio = authServiceClient.obtenerRolPorEmail(socio.getEmail());

        if (rolSocio == null || rolSocio != EnumRol.socio) {
            throw new RuntimeException("El usuario no es un socio. Rol actual: " + rolSocio);
        }

        boolean tieneConsentimiento = documentoLegalRepository
                .findDocumentoPorTipo(idSocio, EnumTipoDocumentoLegal.CONSENTIEMIENTO_INFORMADO,
                        EnumEstadoDocumentoLegal.VIGENTE)
                .isPresent();

        if (!tieneConsentimiento) {
            throw new SecurityAuthorizationException(
                    "No se puede gestionar el perfil médico. El socio no tiene un consentimiento informado vigente.");
        }
    }

    /**
     * Registra un nuevo perfil médico para un socio
     * 
     * @param requestDTO Datos del perfil médico
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Mensaje de confirmación
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos o
     *                                        falta consentimiento
     */
    @Transactional
    public MessegeGlobalDTO registrarPerfilMedico(PerfilMedicoRequestDTO requestDTO, String userRol, String userEmail) {
        if (EnumRol.socio.name().equals(userRol)) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new SecurityAuthorizationException("Email de usuario no proporcionado en el token");
            }
            UsuarioPerfil socioToken = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

            if (!socioToken.getIdUsuario().equals(requestDTO.getIdSocio())) {
                throw new SecurityAuthorizationException(
                        "No tienes permisos para crear el perfil médico de otro usuario.");
            }
        } else {
            ValidacionDeRoles.validarAdminORecepcionista(userRol);
        }

        validarConsentimientoInformado(requestDTO.getIdSocio());

        if (perfilMedicoRepository.existsBySocio_IdUsuarioAndActivoTrue(requestDTO.getIdSocio())) {
            throw new RuntimeException("El socio ya tiene un perfil médico activo registrado");
        }

        UsuarioPerfil socio = usuarioRepository.findById(requestDTO.getIdSocio())
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + requestDTO.getIdSocio()));

        PerfilMedico perfilMedico = new PerfilMedico();
        perfilMedico.setSocio(socio);
        perfilMedico.setPesoKg(requestDTO.getPesoKg());
        perfilMedico.setEstaturaCm(requestDTO.getEstaturaCm());
        perfilMedico.setAlergias(requestDTO.getAlergias());
        perfilMedico.setCondicionesCronicas(requestDTO.getCondicionesCronicas());
        perfilMedico.setLesionesPrevias(requestDTO.getLesionesPrevias());
        perfilMedico.setPorcentajeGrasa(requestDTO.getPorcentajeGrasa());
        perfilMedico.setActivo(true);

        perfilMedicoRepository.save(perfilMedico);
        return new MessegeGlobalDTO("Perfil médico registrado correctamente");
    }

    /**
     * Consulta perfiles médicos con búsqueda y paginación
     * 
     * @param busqueda Búsqueda por nombre, apellido o documento del socio
     * @param pageable Configuración de paginación
     * @param userRol  Rol del usuario autenticado
     * @return Página de perfiles médicos
     */
    @Transactional(readOnly = true)
    public Page<PerfilMedicoResponseDTO> consultarPerfilesMedicosPaginados(String busqueda, Pageable pageable,
            String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        Page<PerfilMedico> perfilesPage = perfilMedicoRepository.consultarPerfilesMedicosPaginados(busqueda, pageable);

        return perfilesPage.map(perfil -> {
            PerfilMedicoResponseDTO dto = new PerfilMedicoResponseDTO();
            dto.setIdPerfilMedico(perfil.getIdPerfilMedico());
            dto.setIdSocio(perfil.getSocio().getIdUsuario());
            dto.setNombreSocio(perfil.getSocio().getNombre() + " " + perfil.getSocio().getApellido());
            dto.setPesoKg(perfil.getPesoKg());
            dto.setEstaturaCm(perfil.getEstaturaCm());
            dto.setAlergias(perfil.getAlergias());
            dto.setCondicionesCronicas(perfil.getCondicionesCronicas());
            dto.setLesionesPrevias(perfil.getLesionesPrevias());
            dto.setPorcentajeGrasa(perfil.getPorcentajeGrasa());
            dto.setFechaActualizacion(perfil.getFechaActualizacion());
            return dto;
        });
    }

    /**
     * Consulta el perfil médico de un socio
     * 
     * @param idSocio   ID del socio
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO del perfil médico
     */
    @Transactional(readOnly = true)
    public PerfilMedicoResponseDTO consultarPerfilMedico(Long idSocio, String userRol, String userEmail) {
        if (EnumRol.socio.name().equals(userRol)) {
            return consultarMiPerfilMedico(userRol, userEmail);
        }

        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuarioAndActivoTrue(idSocio)
                .orElseThrow(
                        () -> new RuntimeException("Perfil médico activo no encontrado para el socio: " + idSocio));

        UsuarioPerfil socio = perfilMedico.getSocio();

        PerfilMedicoResponseDTO dto = new PerfilMedicoResponseDTO();
        dto.setIdPerfilMedico(perfilMedico.getIdPerfilMedico());
        dto.setIdSocio(socio.getIdUsuario());
        dto.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        dto.setPesoKg(perfilMedico.getPesoKg());
        dto.setEstaturaCm(perfilMedico.getEstaturaCm());
        dto.setAlergias(perfilMedico.getAlergias());
        dto.setCondicionesCronicas(perfilMedico.getCondicionesCronicas());
        dto.setLesionesPrevias(perfilMedico.getLesionesPrevias());
        dto.setPorcentajeGrasa(perfilMedico.getPorcentajeGrasa());
        dto.setFechaActualizacion(perfilMedico.getFechaActualizacion());

        return dto;
    }

    /**
     * Consulta el perfil médico del socio autenticado
     * 
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO del perfil médico
     */
    @Transactional(readOnly = true)
    public PerfilMedicoResponseDTO consultarMiPerfilMedico(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo los socios pueden consultar su propio perfil médico por esta vía.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado en el token");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuarioAndActivoTrue(socio.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Perfil médico no encontrado para el socio autenticado"));

        PerfilMedicoResponseDTO dto = new PerfilMedicoResponseDTO();
        dto.setIdPerfilMedico(perfilMedico.getIdPerfilMedico());
        dto.setIdSocio(socio.getIdUsuario());
        dto.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        dto.setPesoKg(perfilMedico.getPesoKg());
        dto.setEstaturaCm(perfilMedico.getEstaturaCm());
        dto.setAlergias(perfilMedico.getAlergias());
        dto.setCondicionesCronicas(perfilMedico.getCondicionesCronicas());
        dto.setLesionesPrevias(perfilMedico.getLesionesPrevias());
        dto.setPorcentajeGrasa(perfilMedico.getPorcentajeGrasa());
        dto.setFechaActualizacion(perfilMedico.getFechaActualizacion());

        return dto;
    }

    /**
     * Actualiza el perfil médico de un socio
     * 
     * @param idSocio    ID del socio
     * @param requestDTO Datos a actualizar
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO actualizarPerfilMedico(Long idSocio, PerfilMedicoRequestDTO requestDTO, String userRol,
            String userEmail) {
        if (EnumRol.socio.name().equals(userRol)) {
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new SecurityAuthorizationException("Email de usuario no proporcionado en el token");
            }
            UsuarioPerfil socioToken = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
            if (!socioToken.getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException(
                        "No tienes permisos para actualizar el perfil médico de otro usuario.");
            }
        } else {
            ValidacionDeRoles.validarAdminORecepcionista(userRol);
            validarConsentimientoInformado(idSocio);
        }

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuarioAndActivoTrue(idSocio)
                .orElseThrow(
                        () -> new RuntimeException("Perfil médico activo no encontrado para el socio: " + idSocio));

        if (requestDTO.getPesoKg() != null) {
            perfilMedico.setPesoKg(requestDTO.getPesoKg());
        }
        if (requestDTO.getEstaturaCm() != null) {
            perfilMedico.setEstaturaCm(requestDTO.getEstaturaCm());
        }
        if (requestDTO.getAlergias() != null) {
            perfilMedico.setAlergias(requestDTO.getAlergias());
        }
        if (requestDTO.getCondicionesCronicas() != null) {
            perfilMedico.setCondicionesCronicas(requestDTO.getCondicionesCronicas());
        }
        if (requestDTO.getLesionesPrevias() != null) {
            perfilMedico.setLesionesPrevias(requestDTO.getLesionesPrevias());
        }
        if (requestDTO.getPorcentajeGrasa() != null) {
            perfilMedico.setPorcentajeGrasa(requestDTO.getPorcentajeGrasa());
        }

        perfilMedicoRepository.save(perfilMedico);

        return new MessegeGlobalDTO("Perfil médico actualizado correctamente");
    }

    /**
     * Actualiza el perfil médico del socio autenticado basándose en su token
     * 
     * @param requestDTO Datos a actualizar
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO actualizarMiPerfilMedico(PerfilMedicoRequestDTO requestDTO, String userRol,
            String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo los socios pueden actualizar su propio perfil médico por esta vía.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado en el token");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuarioAndActivoTrue(socio.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Perfil médico no encontrado para el socio autenticado"));

        if (requestDTO.getPesoKg() != null) {
            perfilMedico.setPesoKg(requestDTO.getPesoKg());
        }
        if (requestDTO.getEstaturaCm() != null) {
            perfilMedico.setEstaturaCm(requestDTO.getEstaturaCm());
        }
        if (requestDTO.getAlergias() != null) {
            perfilMedico.setAlergias(requestDTO.getAlergias());
        }
        if (requestDTO.getCondicionesCronicas() != null) {
            perfilMedico.setCondicionesCronicas(requestDTO.getCondicionesCronicas());
        }
        if (requestDTO.getLesionesPrevias() != null) {
            perfilMedico.setLesionesPrevias(requestDTO.getLesionesPrevias());
        }
        if (requestDTO.getPorcentajeGrasa() != null) {
            perfilMedico.setPorcentajeGrasa(requestDTO.getPorcentajeGrasa());
        }

        perfilMedicoRepository.save(perfilMedico);

        return new MessegeGlobalDTO("Perfil médico actualizado correctamente");
    }

    /**
     * Elimina (desactiva) el perfil médico de un socio
     * 
     * @param idSocio ID del socio
     * @param userRol Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO eliminarPerfilMedico(Long idSocio, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuarioAndActivoTrue(idSocio)
                .orElseThrow(
                        () -> new RuntimeException("Perfil médico activo no encontrado para el socio: " + idSocio));

        perfilMedico.setActivo(false);
        perfilMedicoRepository.save(perfilMedico);

        return new MessegeGlobalDTO("Perfil médico eliminado correctamente");
    }
}