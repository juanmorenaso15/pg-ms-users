package com.pulse_gym.ms_users.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.dto.DetalleEjercicioSesionDTO;
import com.pulse_gym.lb_common.dto.DetalleSesionResponseDTO;
import com.pulse_gym.lb_common.dto.RegistroSesionRequestDTO;
import com.pulse_gym.lb_common.dto.SesionResponseDTO;
import com.pulse_gym.lb_common.entity.user.DetalleRutina;
import com.pulse_gym.lb_common.entity.user.DetalleSesionEjercicio;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.SesionEntrenamiento;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoEjecucionEjercicio;
import com.pulse_gym.lb_common.enums.EnumEstadoSesion;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.DetalleSesionEjercicioRepository;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.SesionEntrenamientoRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeguimientoService {

    /** Repositorio de sesiones de entrenamiento */
    private final SesionEntrenamientoRepository sesionRepository;

    /** Repositorio de detalles de sesión */
    private final DetalleSesionEjercicioRepository detalleSesionRepository;

    /** Repositorio de detalles de rutina */
    private final DetalleRutinaRepository detalleRutinaRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de asignaciones entrenador-socio */
    private final EntrenadorSocioRepository entrenadorSocioRepository;

    /**
     * 
     * Registra una sesión de entrenamiento realizada por un socio
     * 
     * @param request   Datos de la sesión a registrar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO con la sesión registrada
     */
    @Transactional
    public SesionResponseDTO registrarSesion(RegistroSesionRequestDTO request,
            String userRol, String userEmail) {
        // Validar rol
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden registrar sesiones");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));
        if (!socio.getIdUsuario().equals(request.getIdSocio())) {
            throw new SecurityAuthorizationException("No puede registrar sesiones para otro socio");
        }

        SesionEntrenamiento sesion = new SesionEntrenamiento();
        sesion.setSocio(socio);

        if (request.getIdRutina() != null) {
            RutinaIA rutina = rutinaRepository.findById(request.getIdRutina())
                    .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));
            sesion.setRutina(rutina);
        }

        sesion.setDuracionMinutos(request.getDuracionMinutos());

        sesion.setEstado(EnumEstadoSesion.COMPLETADA);
        sesion.setObservaciones(request.getObservaciones());
        sesion = sesionRepository.save(sesion);

        if (request.getDetalles() != null) {
            for (DetalleEjercicioSesionDTO detalleDTO : request.getDetalles()) {
                DetalleRutina detalleRutina = detalleRutinaRepository.findById(detalleDTO.getIdDetalleRutina())
                        .orElseThrow(() -> new RuntimeException("Detalle de rutina no encontrado"));

                DetalleSesionEjercicio detalle = new DetalleSesionEjercicio();
                detalle.setSesion(sesion);
                detalle.setDetalleRutina(detalleRutina);
                detalle.setSeriesCompletadas(detalleDTO.getSeriesCompletadas());
                detalle.setRepeticionesRealizadas(detalleDTO.getRepeticionesRealizadas());
                detalle.setPesoUsado(detalleDTO.getPesoUsado());
                detalle.setEstado(detalleDTO.getEstado() != null ? detalleDTO.getEstado()
                        : EnumEstadoEjecucionEjercicio.COMPLETADO);
                detalle.setObservaciones(detalleDTO.getObservaciones());
                detalleSesionRepository.save(detalle);
            }
        }

        return convertirAResponseDTO(sesion);
    }

    /**
     * Convierte una entidad SesionEntrenamiento a SesionResponseDTO
     * 
     * @param sesion Entidad a convertir
     * @return DTO de la sesión
     */
    private SesionResponseDTO convertirAResponseDTO(SesionEntrenamiento sesion) {
        SesionResponseDTO dto = new SesionResponseDTO();
        dto.setIdSesion(sesion.getIdSesion());
        dto.setIdSocio(sesion.getSocio().getIdUsuario());
        dto.setNombreSocio(sesion.getSocio().getNombre() + " " + sesion.getSocio().getApellido());
        if (sesion.getRutina() != null) {
            dto.setIdRutina(sesion.getRutina().getIdRutinaIa());
            dto.setNombreRutina(
                    sesion.getRutina().getObjetivo() != null ? sesion.getRutina().getObjetivo() : "Rutina IA");
        }
        dto.setFechaSesion(sesion.getFechaSesion());
        dto.setDuracionMinutos(sesion.getDuracionMinutos());
        dto.setEstado(sesion.getEstado());

        List<DetalleSesionEjercicio> detalles = detalleSesionRepository
                .findBySesion_IdSesionOrderByIdDetalleSesionAsc(sesion.getIdSesion());

        List<DetalleSesionResponseDTO> detallesDTO = detalles.stream()
                .map(this::convertirDetalleAResponseDTO)
                .collect(Collectors.toList());
        dto.setDetalles(detallesDTO);
        return dto;
    }

}
