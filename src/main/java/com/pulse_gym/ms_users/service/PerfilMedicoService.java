package com.pulse_gym.ms_users.service;

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

    /** Repositorio para acceder a los perfiles médicos */
    private final PerfilMedicoRepository perfilMedicoRepository;

    /** Repositorio para acceder a los perfiles de usuario */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio para acceder a los documentos legales */
    private final DocumentoLegalRepository documentoLegalRepository;

    /** Cliente para acceder al servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /**
     * Valida que el socio tenga un consentimiento informado vigente antes de
     * permitir la gestión del perfil médico.
     * 
     * @param idSocio El ID del socio para el cual se va a gestionar el perfil
     *                médico
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
     * Registra un nuevo perfil médico para un socio.
     *
     * @param requestDTO El DTO que contiene los datos del perfil médico a
     *                   registrar.
     * @param userRol    El rol del usuario que realiza la operación (debe ser admin
     *                   o recepcionista).
     * @return Un mensaje indicando el resultado de la operación.
     */
    @Transactional
    public MessegeGlobalDTO registrarPerfilMedico(PerfilMedicoRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        validarConsentimientoInformado(requestDTO.getIdSocio());

        if (perfilMedicoRepository.existsBySocio_IdUsuario(requestDTO.getIdSocio())) {
            throw new RuntimeException("El socio ya tiene un perfil médico registrado");
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

        perfilMedicoRepository.save(perfilMedico);
        return new MessegeGlobalDTO("Perfil médico registrado correctamente");
    }

    /**
     * Consulta el perfil médico de un socio.
     *
     * @param idSocio El ID del socio para el cual consultar el perfil médico.
     * @param userRol El rol del usuario que realiza la operación.
     * @return El DTO con los datos del perfil médico consultado.
     */
    @Transactional(readOnly = true)
    public PerfilMedicoResponseDTO consultarPerfilMedico(Long idSocio, String userRol) {

        if (userRol.equals(EnumRol.socio.name())) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Los socios no pueden ver su propio perfil médico por razones de seguridad.");
        }

        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuario(idSocio)
                .orElseThrow(() -> new RuntimeException("Perfil médico no encontrado para el socio: " + idSocio));

        PerfilMedicoResponseDTO dto = new PerfilMedicoResponseDTO();
        dto.setIdPerfilMedico(perfilMedico.getIdPerfilMedico());
        dto.setIdSocio(perfilMedico.getSocio().getIdUsuario());
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
     * Consulta el perfil médico del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol   Rol del usuario autenticado (debe ser SOCIO)
     * @param userEmail Email del socio autenticado (extraído del token)
     * @return El DTO con los datos del perfil médico del socio autenticado
     */
    @Transactional(readOnly = true)
    public PerfilMedicoResponseDTO consultarMiPerfilMedico(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo los socios pueden consultar su propio perfil médico.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuario(socio.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Perfil médico no encontrado para el socio autenticado"));

        PerfilMedicoResponseDTO dto = new PerfilMedicoResponseDTO();
        dto.setIdPerfilMedico(perfilMedico.getIdPerfilMedico());
        dto.setIdSocio(perfilMedico.getSocio().getIdUsuario());
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
     * Actualiza el perfil médico de un socio.
     * 
     * @param idSocio    El ID del socio para el cual actualizar el perfil médico.
     * @param requestDTO El DTO con los datos del perfil médico a actualizar.
     * @param userRol    El rol del usuario que realiza la operación.
     * @return Un mensaje de éxito o error en la actualización del perfil médico.
     */
    @Transactional
    public MessegeGlobalDTO actualizarPerfilMedico(Long idSocio, PerfilMedicoRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        validarConsentimientoInformado(idSocio);

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuario(idSocio)
                .orElseThrow(() -> new RuntimeException("Perfil médico no encontrado para el socio: " + idSocio));

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
}