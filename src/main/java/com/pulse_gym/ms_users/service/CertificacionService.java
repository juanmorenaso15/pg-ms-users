package com.pulse_gym.ms_users.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.dto.CertificacionRequestDTO;
import com.pulse_gym.lb_common.dto.CertificacionResponseDTO;
import com.pulse_gym.lb_common.dto.CertificacionUpdateDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.entity.user.Certificacion;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.CertificacionRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CertificacionService {

    /** Cliente para interactuar con el servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /** Repositorio para gestionar las certificaciones */
    private final CertificacionRepository certificacionRepository;

    /** Repositorio para gestionar los perfiles de usuario */
    private final UsuarioPerfilRepository usuarioRepository;

    /**
     * Registra una nueva certificación para un entrenador específico.
     * 
     * @param requestDTO DTO con los datos de la certificación a registrar
     * @param userRol    Rol del usuario que realiza la acción (obtenido del token
     *                   de autenticación)
     * @return Mensaje de éxito o error en el registro de la certificación
     */
    @Transactional
    public MessegeGlobalDTO registrarCertificacion(CertificacionRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        UsuarioPerfil entrenador = usuarioRepository.findById(requestDTO.getIdEntrenador())
                .orElseThrow(
                        () -> new RuntimeException("Entrenador no encontrado con ID: " + requestDTO.getIdEntrenador()));

        EnumRol rolEntrenador = authServiceClient.obtenerRolPorEmail(entrenador.getEmail());

        if (rolEntrenador == null) {
            throw new RuntimeException("No se pudo verificar el rol del usuario");
        }

        if (rolEntrenador != EnumRol.entrenador) {
            throw new RuntimeException(
                    "Solo se pueden registrar certificaciones para usuarios con rol ENTRENADOR. Rol actual: "
                            + rolEntrenador);
        }

        Certificacion certificacion = new Certificacion();
        certificacion.setEntrenador(entrenador);
        certificacion.setNombre(requestDTO.getNombre());
        certificacion.setUrlPdf(requestDTO.getUrlPdf());

        certificacionRepository.save(certificacion);
        return new MessegeGlobalDTO("Certificación registrada correctamente");
    }

    /**
     * Consulta las certificaciones de un entrenador específico.
     * 
     * @param idEntrenador      ID del entrenador del cual se quieren consultar las
     *                          certificaciones
     * @param userRol           Rol del usuario que realiza la acción (obtenido del
     *                          token de autenticación)
     * @param userIdAutenticado ID del usuario autenticado
     * @return Lista de certificaciones del entrenador
     */
    @Transactional(readOnly = true)
    public List<CertificacionResponseDTO> consultarCertificaciones(Long idEntrenador, String userRol,
            Long userIdAutenticado) {

        if (userRol.equals(EnumRol.socio.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Los socios no pueden ver certificaciones");
        }

        if (userRol.equals(EnumRol.entrenador.name())) {
            if (!userIdAutenticado.equals(idEntrenador)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede ver sus propias certificaciones");
            }
        }

        UsuarioPerfil entrenador = usuarioRepository.findById(idEntrenador)
                .orElseThrow(() -> new RuntimeException("Entrenador no encontrado con ID: " + idEntrenador));

        EnumRol rolEntrenador = authServiceClient.obtenerRolPorEmail(entrenador.getEmail());

        if (rolEntrenador == null) {
            throw new RuntimeException("No se pudo verificar el rol del usuario");
        }

        if (rolEntrenador != EnumRol.entrenador) {
            throw new RuntimeException("El usuario no es un entrenador. Rol actual: " + rolEntrenador);
        }

        List<Certificacion> certificaciones = certificacionRepository.findByEntrenador_IdUsuario(idEntrenador);

        final String nombreEntrenador = entrenador.getNombre() + " " + entrenador.getApellido();

        return certificaciones.stream()
                .map(cert -> {
                    CertificacionResponseDTO dto = new CertificacionResponseDTO();
                    dto.setIdCertificacion(cert.getIdCertificacion());
                    dto.setIdEntrenador(cert.getEntrenador().getIdUsuario());
                    dto.setNombreEntrenador(nombreEntrenador);
                    dto.setNombreCertificacion(cert.getNombre());
                    dto.setUrlPdf(cert.getUrlPdf());
                    dto.setFechaSubida(cert.getFechaSubida());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una certificación existente.
     * 
     * @param idCertificacion ID de la certificación a actualizar
     * @param requestDTO      DTO con los nuevos datos de la certificación
     * @param userRol         Rol del usuario que realiza la acción (obtenido del
     *                        token de autenticación)
     * @return Mensaje de éxito o error en la actualización de la certificación
     */
    @Transactional
    public MessegeGlobalDTO actualizarCertificacion(Long idCertificacion, CertificacionUpdateDTO requestDTO,
            String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        Certificacion certificacion = certificacionRepository.findById(idCertificacion)
                .orElseThrow(() -> new RuntimeException("Certificación no encontrada con ID: " + idCertificacion));

        if (requestDTO.getNombre() != null && !requestDTO.getNombre().isEmpty()) {
            certificacion.setNombre(requestDTO.getNombre());
        }

        if (requestDTO.getUrlPdf() != null && !requestDTO.getUrlPdf().isEmpty()) {
            certificacion.setUrlPdf(requestDTO.getUrlPdf());
        }

        certificacionRepository.save(certificacion);
        return new MessegeGlobalDTO("Certificación actualizada correctamente");
    }

    /**
     * Elimina una certificación existente.
     * 
     * @param idCertificacion ID de la certificación a eliminar
     * @param userRol         Rol del usuario que realiza la acción (obtenido del
     *                        token de autenticación)
     * @return Mensaje de éxito o error en la eliminación de la certificación
     */
    @Transactional
    public MessegeGlobalDTO eliminarCertificacion(Long idCertificacion, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        Certificacion certificacion = certificacionRepository.findById(idCertificacion)
                .orElseThrow(() -> new RuntimeException("Certificación no encontrada con ID: " + idCertificacion));

        certificacionRepository.delete(certificacion);
        return new MessegeGlobalDTO("Certificación eliminada correctamente");
    }
}
