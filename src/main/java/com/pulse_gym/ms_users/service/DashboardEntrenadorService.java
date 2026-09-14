package com.pulse_gym.ms_users.service;

import com.pulse_gym.lb_common.dto.DashboardEntrenadorDTO;
import com.pulse_gym.lb_common.dto.EvolucionDiariaDTO;
import com.pulse_gym.lb_common.dto.SocioEvolucionDTO;
import com.pulse_gym.lb_common.entity.user.HistorialFisico;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.HistorialFisicoRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardEntrenadorService {

    private final EntrenadorSocioRepository entrenadorSocioRepository;
    private final HistorialFisicoRepository historialFisicoRepository;
    private final UsuarioPerfilRepository usuarioPerfilRepository;

    public DashboardEntrenadorDTO obtenerDashboardEntrenador(Long entrenadorId, Long socioIdSeleccionado) {
        
        // 1. Obtener la información real del entrenador usando su ID
        UsuarioPerfil entrenador = usuarioPerfilRepository.findById(entrenadorId).orElse(null);
        String nombreEntrenador = entrenador != null ? entrenador.getNombre() + " " + entrenador.getApellido() : "Entrenador";

        // 2. Obtener la lista de socios activos asignados a este entrenador
        List<UsuarioPerfil> sociosActivos = entrenadorSocioRepository.findSociosActivosByEntrenador(entrenadorId);
        long totalSociosActivos = sociosActivos.size();

        // Si se pasó un filtro por socio específico, filtramos la lista, de lo contrario incluimos a todos
        if (socioIdSeleccionado != null) {
            sociosActivos = sociosActivos.stream()
                    .filter(s -> s.getIdUsuario().equals(socioIdSeleccionado))
                    .collect(Collectors.toList());
        }

        // 3. Generar la evolución histórica independiente para cada socio (para armar sus respectivas tablas)
        List<SocioEvolucionDTO> sociosEvolucion = sociosActivos.stream().map(socio -> {
            List<HistorialFisico> historiales = historialFisicoRepository.findBySocio_IdUsuarioOrderByFechaMedicionDesc(socio.getIdUsuario());
            
            List<EvolucionDiariaDTO> evolucionHistorica = historiales.stream()
                    .map(historial -> EvolucionDiariaDTO.builder()
                            .fecha(historial.getFechaMedicion() != null ? historial.getFechaMedicion().toLocalDate() : null)
                            .peso(historial.getPesoKg() != null ? historial.getPesoKg().doubleValue() : null)
                            .porcentajeGrasa(historial.getPorcentajeGrasa() != null ? historial.getPorcentajeGrasa().doubleValue() : null)
                            .masaMuscular(historial.getPorcentajeMusculo() != null ? historial.getPorcentajeMusculo().doubleValue() : null)
                            .build())
                    .collect(Collectors.toList());

            return SocioEvolucionDTO.builder()
                    .socioId(socio.getIdUsuario())
                    .nombreSocio(socio.getNombre() + " " + socio.getApellido())
                    .evolucionHistorica(evolucionHistorica)
                    .build();
        }).collect(Collectors.toList());

        return DashboardEntrenadorDTO.builder()
                .entrenadorId(entrenadorId)
                .nombreEntrenador(nombreEntrenador)
                .totalSociosActivos(totalSociosActivos)
                .sociosEvolucion(sociosEvolucion)
                .build();
    }
}