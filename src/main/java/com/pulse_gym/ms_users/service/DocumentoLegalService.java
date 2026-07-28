package com.pulse_gym.ms_users.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.dto.DocumentoLegalRequestDTO;
import com.pulse_gym.lb_common.dto.DocumentoLegalResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.entity.user.DocumentoLegal;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoDocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.enums.EnumTipoDocumentoLegal;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.DocumentoLegalRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentoLegalService {

    /** Cliente para interactuar con el servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /** Repositorio de documentos legales */
    private final DocumentoLegalRepository documentoLegalRepository;

    /** Repositorio de perfiles de usuario */
    private final UsuarioPerfilRepository usuarioRepository;

    /**
     * Carga un documento legal para un usuario específico. Solo los usuarios con
     * rol ADMIN o RECEPCIONISTA pueden realizar esta acción, y el documento solo
     * puede ser cargado para usuarios con rol SOCIO.
     * 
     * @param requestDTO DTO con los datos del documento legal a cargar
     * @param userRol    Rol del usuario que realiza la acción (obtenido del token
     *                   de autenticación)
     * @return Mensaje de éxito o error en la carga del documento legal
     */
    @Transactional
    public MessegeGlobalDTO cargarDocumentoLegal(DocumentoLegalRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        UsuarioPerfil usuario = usuarioRepository.findById(requestDTO.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + requestDTO.getIdUsuario()));

        EnumRol rolSocio = authServiceClient.obtenerRolPorEmail(usuario.getEmail());

        if (rolSocio == null) {
            throw new RuntimeException("No se pudo verificar el rol del usuario");
        }

        if (rolSocio != EnumRol.socio) {
            throw new RuntimeException("Solo se pueden cargar documentos legales para socios. Rol actual: " + rolSocio);
        }

        DocumentoLegal documento = new DocumentoLegal();
        documento.setUsuario(usuario);
        documento.setTipoDocumento(requestDTO.getTipoDocumento());
        documento.setUrlArchivoFirmado(requestDTO.getUrlArchivoFirmado());
        documento.setEstado(EnumEstadoDocumentoLegal.VIGENTE);

        documentoLegalRepository.save(documento);
        return new MessegeGlobalDTO("Documento legal cargado correctamente");
    }

    /**
     * Consulta todos los documentos legales vigentes.
     * 
     * @param userRol El rol del usuario que realiza la consulta
     * @return Una lista de documentos legales vigentes
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegalResponseDTO> consultarTodosLosDocumentosLegales(String userRol) {

        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        List<DocumentoLegal> documentos = documentoLegalRepository
                .findByEstado(EnumEstadoDocumentoLegal.VIGENTE);

        return documentos.stream()
                .map(doc -> {
                    DocumentoLegalResponseDTO dto = new DocumentoLegalResponseDTO();
                    dto.setIdDocumento(doc.getIdDocumento());
                    dto.setIdUsuario(doc.getUsuario().getIdUsuario());
                    dto.setNombreUsuario(doc.getUsuario().getNombre() + " " + doc.getUsuario().getApellido());
                    dto.setTipoDocumento(doc.getTipoDocumento());
                    dto.setFechaFirma(doc.getFechaFirma());
                    dto.setUrlArchivoFirmado(doc.getUrlArchivoFirmado());
                    dto.setEstado(doc.getEstado());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Consulta los documentos legales vigentes de un usuario específico. Los
     * usuarios con rol SOCIO solo pueden consultar sus propios documentos, mientras
     * que los usuarios con rol ADMIN o RECE
     * 
     * @param idUsuario         El ID del usuario cuyos documentos legales se desean
     *                          consultar
     * @param userRol           El rol del usuario que realiza la consulta
     * @param userIdAutenticado El ID del usuario autenticado
     * @return Una lista de documentos legales vigentes asociados al usuario
     *         especificado
     */
    @Transactional(readOnly = true)
    public List<DocumentoLegalResponseDTO> consultarDocumentosLegales(Long idUsuario, String userRol,
            Long userIdAutenticado) {

        if (userRol.equals(EnumRol.socio.name())) {
            if (!userIdAutenticado.equals(idUsuario)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede ver sus propios documentos");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) && !userRol.equals(EnumRol.recepcionista.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado");
        }

        UsuarioPerfil usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + idUsuario));

        List<DocumentoLegal> documentos = documentoLegalRepository
                .findByUsuario_IdUsuarioAndEstado(idUsuario, EnumEstadoDocumentoLegal.VIGENTE);

        final String nombreCompleto = usuario.getNombre() + " " + usuario.getApellido();

        return documentos.stream()
                .map(doc -> {
                    DocumentoLegalResponseDTO dto = new DocumentoLegalResponseDTO();
                    dto.setIdDocumento(doc.getIdDocumento());
                    dto.setIdUsuario(doc.getUsuario().getIdUsuario());
                    dto.setNombreUsuario(nombreCompleto);
                    dto.setTipoDocumento(doc.getTipoDocumento());
                    dto.setFechaFirma(doc.getFechaFirma());
                    dto.setUrlArchivoFirmado(doc.getUrlArchivoFirmado());
                    dto.setEstado(doc.getEstado());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Elimina un documento legal vigente.
     * 
     * @param idDocumento El ID del documento legal a eliminar
     * @param userRol     El rol del usuario que realiza la solicitud
     * @return Un mensaje global indicando el resultado de la operación
     */
    @Transactional
    public MessegeGlobalDTO eliminarDocumentoLegal(Long idDocumento, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        DocumentoLegal documento = documentoLegalRepository
                .findByIdDocumentoAndEstado(idDocumento, EnumEstadoDocumentoLegal.VIGENTE)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado o ya no está vigente"));

        documento.setEstado(EnumEstadoDocumentoLegal.VENCIDO);
        documentoLegalRepository.save(documento);

        return new MessegeGlobalDTO("Documento legal eliminado correctamente");
    }

    /**
     * Verifica si un usuario tiene un consentimiento informado vigente.
     * 
     * @param idUsuario         El ID del usuario para el cual se verifica el
     *                          consentimiento informado
     * @param userRol           El rol del usuario que realiza la consulta
     * @param userIdAutenticado El ID del usuario autenticado
     * @return true si el usuario tiene un consentimiento informado vigente, false
     *         en caso contrario
     */
    @Transactional(readOnly = true)
    public Boolean tieneConsentimientoDatosSensibles(Long idUsuario, String userRol, Long userIdAutenticado) {

        if (userRol.equals(EnumRol.socio.name())) {
            if (!userIdAutenticado.equals(idUsuario)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede ver su propio consentimiento");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) && !userRol.equals(EnumRol.entrenador.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado");
        }

        return documentoLegalRepository
                .findDocumentoPorTipo(idUsuario, EnumTipoDocumentoLegal.CONSENTIEMIENTO_INFORMADO,
                        EnumEstadoDocumentoLegal.VIGENTE)
                .isPresent();
    }
}
