package com.pulse_gym.ms_users.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse_gym.lb_common.dto.DashboardMonitoreoEntrenadorDTO;
import com.pulse_gym.lb_common.dto.DashboardProgresoSocioDTO;
import com.pulse_gym.lb_common.dto.DetalleEjercicioSesionDTO;
import com.pulse_gym.lb_common.dto.DetalleRutinaExportacionDTO;
import com.pulse_gym.lb_common.dto.DetalleSesionResponseDTO;
import com.pulse_gym.lb_common.dto.EvolucionEjercicioDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalExportacionDTO;
import com.pulse_gym.lb_common.dto.RegistroSesionRequestDTO;
import com.pulse_gym.lb_common.dto.ResumenSocioDTO;
import com.pulse_gym.lb_common.dto.RutinaExportacionDTO;
import com.pulse_gym.lb_common.dto.SesionResponseDTO;
import com.pulse_gym.lb_common.dto.SugerenciaComidaDTO;
import com.pulse_gym.lb_common.dto.SugerenciaComidaExportacionDTO;
import com.pulse_gym.lb_common.entity.user.DetalleRutina;
import com.pulse_gym.lb_common.entity.user.DetalleSesionEjercicio;
import com.pulse_gym.lb_common.entity.user.EntrenadorSocio;
import com.pulse_gym.lb_common.entity.user.HistorialRutinaVersion;
import com.pulse_gym.lb_common.entity.user.PlanNutricionalIA;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.SesionEntrenamiento;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoEjecucionEjercicio;
import com.pulse_gym.lb_common.enums.EnumEstadoSesion;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.util.FechaUtils;
import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.DetalleSesionEjercicioRepository;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.HistorialRutinaVersionRepository;
import com.pulse_gym.ms_users.repository.PlanNutricionalRepository;
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

    private final SesionEntrenamientoRepository sesionRepository;
    private final DetalleSesionEjercicioRepository detalleSesionRepository;
    private final DetalleRutinaRepository detalleRutinaRepository;
    private final UsuarioPerfilRepository usuarioRepository;
    private final RutinaRepository rutinaRepository;
    private final EntrenadorSocioRepository entrenadorSocioRepository;
    private final ExportacionPdfService exportacionPdfService;
    private final PlanNutricionalRepository planNutricionalRepository;
    private final ObjectMapper objectMapper;
    private final HistorialRutinaVersionRepository historialRutinaVersionRepository;

    private LocalDateTime obtenerFechaHoraColombia() {
        return FechaUtils.ahoraColombia();
    }

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

    private DetalleSesionResponseDTO convertirDetalleAResponseDTO(DetalleSesionEjercicio detalle) {
        DetalleSesionResponseDTO dto = new DetalleSesionResponseDTO();
        dto.setIdDetalleSesion(detalle.getIdDetalleSesion());
        dto.setIdDetalleRutina(detalle.getDetalleRutina().getIdDetalleRutina());
        dto.setNombreEjercicio(detalle.getDetalleRutina().getEjercicio().getNombre());
        dto.setGrupoMuscular(detalle.getDetalleRutina().getEjercicio().getGrupoMuscular());
        dto.setSeriesCompletadas(detalle.getSeriesCompletadas());
        dto.setRepeticionesRealizadas(detalle.getRepeticionesRealizadas());
        dto.setPesoUsado(detalle.getPesoUsado());
        dto.setEstado(detalle.getEstado());
        dto.setObservaciones(detalle.getObservaciones());
        return dto;
    }

    private UsuarioPerfil buscarEntrenadorDisponible() {
        List<UsuarioPerfil> entrenadores = usuarioRepository.findEntrenadoresActivos();
        if (entrenadores.isEmpty()) {
            entrenadores = usuarioRepository.findByEmailContainingAndEstado("entrenador", EnumEstadoUsuario.ACTIVO);
        }
        if (entrenadores.isEmpty()) {
            return null;
        }
        if (entrenadores.size() == 1) {
            return entrenadores.get(0);
        }

        UsuarioPerfil entrenadorSeleccionado = null;
        int menorCantidadSocios = Integer.MAX_VALUE;

        for (UsuarioPerfil entrenador : entrenadores) {
            Long cantidadSocios = entrenadorSocioRepository.countByEntrenadorAndActivaTrue(
                    entrenador.getIdUsuario());
            if (cantidadSocios < menorCantidadSocios) {
                menorCantidadSocios = cantidadSocios.intValue();
                entrenadorSeleccionado = entrenador;
            }
        }
        return entrenadorSeleccionado;
    }

    private void asignarSocioAEntrenadorSiNoExiste(UsuarioPerfil socio, UsuarioPerfil entrenador) {
        boolean existe = entrenadorSocioRepository
                .existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(
                        entrenador.getIdUsuario(),
                        socio.getIdUsuario());

        if (!existe) {
            EntrenadorSocio asignacion = new EntrenadorSocio();
            asignacion.setEntrenador(entrenador);
            asignacion.setSocio(socio);
            asignacion.setActiva(true);
            entrenadorSocioRepository.save(asignacion);
        }
    }

    @Transactional
    public SesionResponseDTO registrarSesion(RegistroSesionRequestDTO request, String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden registrar sesiones");
        }

        UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        LocalDateTime ahoraColombia = obtenerFechaHoraColombia();
        LocalDate hoyColombia = ahoraColombia.toLocalDate();

        List<SesionEntrenamiento> sesionesRegistradasHoy = sesionRepository
                .findBySocio_IdUsuarioOrderByFechaSesionDesc(socioAutenticado.getIdUsuario())
                .stream()
                .filter(s -> s.getFechaSesion().toLocalDate().equals(hoyColombia))
                .collect(Collectors.toList());

        if (!sesionesRegistradasHoy.isEmpty()) {
            throw new RuntimeException(
                    "Ya has registrado un seguimiento el día de hoy. Podrás registrar tu siguiente sesión a partir de mañana.");
        }

        SesionEntrenamiento sesion = new SesionEntrenamiento();
        sesion.setSocio(socioAutenticado);

        RutinaIA rutina = null;
        UsuarioPerfil entrenadorAsignado = null;

        if (request.getIdRutina() != null) {
            rutina = rutinaRepository.findById(request.getIdRutina())
                    .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));
            sesion.setRutina(rutina);
            entrenadorAsignado = rutina.getEntrenador();

            if (entrenadorAsignado == null) {
                entrenadorAsignado = buscarEntrenadorDisponible();
            }

            if (entrenadorAsignado != null) {
                asignarSocioAEntrenadorSiNoExiste(socioAutenticado, entrenadorAsignado);
            }
        }

        sesion.setDuracionMinutos(request.getDuracionMinutos());
        sesion.setEstado(EnumEstadoSesion.COMPLETADA);
        sesion.setObservaciones(request.getObservaciones());
        sesion.setFechaSesion(ahoraColombia);
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

    public List<SesionResponseDTO> obtenerHistorialSesiones(Long idSocio, String userRol, String userEmail) {
        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil autenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
            if (!socio.getIdUsuario().equals(autenticado.getIdUsuario())) {
                throw new SecurityAuthorizationException("Solo puede ver su propio historial");
            }
        } else if (EnumRol.entrenador.name().equals(userRol)) {
            UsuarioPerfil entrenador = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));
            boolean esAsignado = entrenadorSocioRepository
                    .existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(entrenador.getIdUsuario(), idSocio);
            if (!esAsignado) {
                throw new SecurityAuthorizationException("No tiene acceso al historial de este socio");
            }
        } else if (!EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("No tiene permisos para ver este historial");
        }

        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        return sesiones.stream().map(this::convertirAResponseDTO).collect(Collectors.toList());
    }

    public DashboardProgresoSocioDTO obtenerDashboardSocio(Long idSocio, String userRol, String userEmail) {
        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil autenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
            if (!socio.getIdUsuario().equals(autenticado.getIdUsuario())) {
                throw new SecurityAuthorizationException("Solo puede ver su propio dashboard");
            }
        } else if (EnumRol.entrenador.name().equals(userRol)) {
            UsuarioPerfil entrenador = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));
            boolean esAsignado = entrenadorSocioRepository
                    .existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(entrenador.getIdUsuario(), idSocio);
            if (!esAsignado) {
                throw new SecurityAuthorizationException("No tiene acceso al dashboard de este socio");
            }
        } else if (!EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("No tiene permisos para ver este dashboard");
        }

        DashboardProgresoSocioDTO dashboard = new DashboardProgresoSocioDTO();
        dashboard.setIdSocio(idSocio);
        dashboard.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        dashboard.setRachaDiasEntrenando(calcularRachaDias(idSocio));
        dashboard.setPorcentajeCumplimientoSemanal(calcularCumplimientoSemanal(idSocio));
        dashboard.setPorcentajeCumplimientoSemanaAnterior(calcularCumplimientoSemanaAnterior(idSocio));
        dashboard.setDiasEntrenadosSemana(calcularDiasEntrenadosSemana(idSocio));
        dashboard.setEvolucionEjercicios(calcularEvolucionEjercicios(idSocio));

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSesiones", sesionRepository.countBySocio_IdUsuario(idSocio));
        estadisticas.put("promedioDuracion", calcularPromedioDuracion(idSocio));
        dashboard.setEstadisticas(estadisticas);

        return dashboard;
    }

    public DashboardMonitoreoEntrenadorDTO obtenerDashboardMonitoreo(String userRol, String userEmail) {
        if (!EnumRol.entrenador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo entrenadores pueden acceder a este dashboard");
        }

        UsuarioPerfil entrenador = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));

        DashboardMonitoreoEntrenadorDTO dashboard = new DashboardMonitoreoEntrenadorDTO();
        dashboard.setIdEntrenador(entrenador.getIdUsuario());
        dashboard.setNombreEntrenador(entrenador.getNombre() + " " + entrenador.getApellido());

        List<UsuarioPerfil> socios = entrenadorSocioRepository.findSociosActivosByEntrenador(entrenador.getIdUsuario());
        List<ResumenSocioDTO> resumenSocios = socios.stream()
                .map(this::construirResumenSocio)
                .collect(Collectors.toList());

        dashboard.setSociosAsignados(resumenSocios);
        return dashboard;
    }

    private ResumenSocioDTO construirResumenSocio(UsuarioPerfil socio) {
        ResumenSocioDTO resumen = new ResumenSocioDTO();
        resumen.setIdSocio(socio.getIdUsuario());
        resumen.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        resumen.setPorcentajeCumplimiento(calcularCumplimientoSemanal(socio.getIdUsuario()));
        resumen.setRachaActual(calcularRachaDias(socio.getIdUsuario()));
        resumen.setDiasSinEntrenar(calcularDiasSinEntrenar(socio.getIdUsuario()));
        resumen.setEstadoEvolucionCargas(calcularEvolucionCargas(socio.getIdUsuario()));
        return resumen;
    }

    private Integer calcularDiasSinEntrenar(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones.isEmpty())
            return 30;
        LocalDateTime ultima = sesiones.get(0).getFechaSesion();
        long dias = java.time.temporal.ChronoUnit.DAYS.between(ultima, obtenerFechaHoraColombia());
        return (int) Math.max(dias, 0);
    }

    private String calcularEvolucionCargas(Long idSocio) {
        List<DetalleSesionEjercicio> detalles = detalleSesionRepository.findDetallesBySocio(idSocio);
        if (detalles.isEmpty())
            return "ESTANCADO";

        Map<Long, List<DetalleSesionEjercicio>> agrupado = detalles.stream()
                .collect(Collectors.groupingBy(d -> d.getDetalleRutina().getEjercicio().getIdEjercicio()));

        int progresos = 0, retrocesos = 0, estancados = 0;
        for (List<DetalleSesionEjercicio> lista : agrupado.values()) {
            lista.sort((a, b) -> b.getSesion().getFechaSesion().compareTo(a.getSesion().getFechaSesion()));
            if (lista.size() >= 2) {
                BigDecimal reciente = lista.get(0).getPesoUsado() != null ? lista.get(0).getPesoUsado()
                        : BigDecimal.ZERO;
                BigDecimal anterior = lista.get(1).getPesoUsado() != null ? lista.get(1).getPesoUsado()
                        : BigDecimal.ZERO;
                int comp = reciente.compareTo(anterior);
                if (comp > 0)
                    progresos++;
                else if (comp < 0)
                    retrocesos++;
                else
                    estancados++;
            }
        }
        if (progresos > retrocesos && progresos > estancados)
            return "PROGRESO";
        else if (retrocesos > progresos && retrocesos > estancados)
            return "RETROCESO";
        else
            return "ESTANCADO";
    }

    private Double calcularCumplimientoSemanal(Long idSocio) {
        LocalDateTime ahora = obtenerFechaHoraColombia();
        LocalDateTime inicioSemana = ahora.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                .atStartOfDay();
        LocalDateTime finSemana = ahora.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(23,
                59, 59);

        Long sesiones = sesionRepository.countSesionesEnPeriodo(idSocio, inicioSemana, finSemana);
        double meta = 3.0;
        double cumplimiento = Math.min((sesiones / meta) * 100, 100.0);
        return Math.round(cumplimiento * 10.0) / 10.0;
    }

    private Double calcularCumplimientoSemanaAnterior(Long idSocio) {
        LocalDateTime ahora = obtenerFechaHoraColombia();
        LocalDateTime inicioSemanaActual = ahora.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate()
                .atStartOfDay();
        LocalDateTime inicioSemanaAnterior = inicioSemanaActual.minusWeeks(1);
        LocalDateTime finSemanaAnterior = inicioSemanaActual.minusSeconds(1);

        Long sesiones = sesionRepository.countSesionesEnPeriodo(idSocio, inicioSemanaAnterior, finSemanaAnterior);
        double meta = 3.0;
        double cumplimiento = Math.min((sesiones / meta) * 100, 100.0);
        return Math.round(cumplimiento * 10.0) / 10.0;
    }

    /**
     * Calcula los días de la semana actual (1 = Lunes, ..., 7 = Domingo) 
     * en los que el socio registró asistencia real usando la hora de Colombia.
     */
    private List<Integer> calcularDiasEntrenadosSemana(Long idSocio) {
        LocalDateTime ahora = obtenerFechaHoraColombia();
        LocalDateTime inicioSemana = ahora.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();
        LocalDateTime finSemana = ahora.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(23, 59, 59);

        List<SesionEntrenamiento> sesionesSemana = sesionRepository.findSesionesDesdeFecha(idSocio, inicioSemana)
                .stream()
                .filter(s -> !s.getFechaSesion().isAfter(finSemana))
                .collect(Collectors.toList());

        return sesionesSemana.stream()
                .map(s -> s.getFechaSesion().getDayOfWeek().getValue())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<EvolucionEjercicioDTO> calcularEvolucionEjercicios(Long idSocio) {
        List<DetalleSesionEjercicio> detalles = detalleSesionRepository.findDetallesBySocio(idSocio);
        if (detalles.isEmpty())
            return new ArrayList<>();

        Map<Long, List<DetalleSesionEjercicio>> agrupado = detalles.stream()
                .collect(Collectors.groupingBy(d -> d.getDetalleRutina().getEjercicio().getIdEjercicio()));

        List<EvolucionEjercicioDTO> evoluciones = new ArrayList<>();
        for (List<DetalleSesionEjercicio> lista : agrupado.values()) {
            lista.sort((a, b) -> b.getSesion().getFechaSesion().compareTo(a.getSesion().getFechaSesion()));
            if (lista.size() >= 2) {
                BigDecimal pesoReciente = lista.get(0).getPesoUsado() != null ? lista.get(0).getPesoUsado()
                        : BigDecimal.ZERO;
                BigDecimal pesoAnterior = lista.get(1).getPesoUsado() != null ? lista.get(1).getPesoUsado()
                        : BigDecimal.ZERO;

                EvolucionEjercicioDTO evo = new EvolucionEjercicioDTO();
                evo.setNombreEjercicio(lista.get(0).getDetalleRutina().getEjercicio().getNombre());
                int comp = pesoReciente.compareTo(pesoAnterior);
                if (comp > 0)
                    evo.setEstado("PROGRESO");
                else if (comp < 0)
                    evo.setEstado("RETROCESO");
                else
                    evo.setEstado("ESTANCADO");
                evo.setProgreso(pesoReciente);
                evoluciones.add(evo);
            }
        }
        return evoluciones;
    }

    private Double calcularPromedioDuracion(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones.isEmpty())
            return 0.0;
        return sesiones.stream().mapToInt(SesionEntrenamiento::getDuracionMinutos).average().orElse(0.0);
    }

    private Integer calcularRachaDias(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones == null || sesiones.isEmpty()) {
            return 0;
        }

        List<LocalDate> fechasSesiones = sesiones.stream()
                .map(s -> s.getFechaSesion().toLocalDate())
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .collect(Collectors.toList());

        LocalDate hoy = obtenerFechaHoraColombia().toLocalDate();
        LocalDate ultimaSesion = fechasSesiones.get(0);

        long diasDesdeUltimaSesion = java.time.temporal.ChronoUnit.DAYS.between(ultimaSesion, hoy);
        if (diasDesdeUltimaSesion > 3) {
            return 0;
        }

        int rachaAcumulada = 0;
        LocalDate fechaEsperada = null;

        for (LocalDate fecha : fechasSesiones) {
            if (fechaEsperada == null) {
                rachaAcumulada++;
                fechaEsperada = fecha;
            } else {
                long diferenciaDias = java.time.temporal.ChronoUnit.DAYS.between(fecha, fechaEsperada);
                if (diferenciaDias >= 1 && diferenciaDias <= 4) {
                    rachaAcumulada++;
                    fechaEsperada = fecha;
                } else {
                    break;
                }
            }
        }

        return rachaAcumulada;
    }

    public byte[] exportarRutinaPdf(Long idSocio, Long idRutina, String userRol, String userEmail) {
        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil autenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
            if (!socio.getIdUsuario().equals(autenticado.getIdUsuario())) {
                throw new SecurityAuthorizationException("Solo puede exportar su propia rutina");
            }
        } else if (EnumRol.entrenador.name().equals(userRol)) {
            UsuarioPerfil entrenador = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));
            boolean esAsignado = entrenadorSocioRepository
                    .existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(entrenador.getIdUsuario(), idSocio);
            if (!esAsignado) {
                throw new SecurityAuthorizationException("No tiene acceso a la rutina de este socio");
            }
        } else if (!EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("No tiene permisos para exportar esta rutina");
        }

        RutinaIA rutina;
        if (idRutina != null) {
            rutina = rutinaRepository.findById(idRutina)
                    .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));
            if (!rutina.getSocio().getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException("La rutina no pertenece al socio especificado");
            }
        } else {
            rutina = rutinaRepository.findRutinaActivaReciente(idSocio)
                    .orElseThrow(() -> new RuntimeException("El socio no tiene una rutina activa"));
        }

        String descripcion = rutina.getObjetivo() != null ? rutina.getObjetivo()
                : "Rutina de entrenamiento personalizada";
        String explicacionIA = rutina.getExplicacionIa();

        if (rutina.getRutinaGenerada() != null && !rutina.getRutinaGenerada().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(rutina.getRutinaGenerada());
                if (root.has("descripcion") && !root.get("descripcion").asText().isEmpty()) {
                    descripcion = root.get("descripcion").asText();
                }
                if ((explicacionIA == null || explicacionIA.isEmpty()) && root.has("explicacionIA")) {
                    explicacionIA = root.get("explicacionIA").asText();
                }
            } catch (Exception e) {
                log.warn("No se pudo parsear el JSON de la rutina: {}", e.getMessage());
            }
        }

        RutinaExportacionDTO exportDTO = new RutinaExportacionDTO();
        exportDTO.setIdRutina(rutina.getIdRutinaIa());
        exportDTO.setNombre(rutina.getObjetivo() != null ? rutina.getObjetivo() : "Rutina de entrenamiento");
        exportDTO.setDescripcion(descripcion);
        exportDTO.setExplicacionIA(explicacionIA);
        exportDTO.setNombreSocio(socio.getNombre());
        exportDTO.setApellidoSocio(socio.getApellido());
        exportDTO.setEmailSocio(socio.getEmail());
        exportDTO.setFechaGeneracion(rutina.getFechaGeneracion());
        exportDTO.setVersion(rutina.getVersion());
        exportDTO.setGeneradaPorIA(rutina.getModeloIa() != null);

        try {
            List<HistorialRutinaVersion> historial = historialRutinaVersionRepository
                    .findByRutinaIa_IdRutinaIaOrderByVersionDesc(rutina.getIdRutinaIa());

            if (historial != null && !historial.isEmpty()) {
                HistorialRutinaVersion ultimo = historial.get(0);
                if (ultimo.getVersion() > 1 ||
                        (ultimo.getModificadoPor() != null) ||
                        (ultimo.getModificadoPorNombre() != null && !ultimo.getModificadoPorNombre().isEmpty())) {

                    String modificador = null;
                    if (ultimo.getModificadoPor() != null) {
                        modificador = ultimo.getModificadoPor().getEmail();
                    } else if (ultimo.getModificadoPorNombre() != null) {
                        modificador = ultimo.getModificadoPorNombre();
                    }

                    if (modificador == null && ultimo.getVersion() > 1) {
                        modificador = "Sistema";
                    }

                    exportDTO.setModificadoPor(modificador);
                    exportDTO.setFechaModificacion(ultimo.getFechaModificacion());
                    exportDTO.setMotivoModificacion(ultimo.getMotivo());
                }
            }
        } catch (Exception e) {
            log.warn("Error al obtener historial para auditoría: {}", e.getMessage());
        }

        List<DetalleRutinaExportacionDTO> detalles = new ArrayList<>();
        if (rutina.getDetalles() != null) {
            for (DetalleRutina detalle : rutina.getDetalles()) {
                DetalleRutinaExportacionDTO detalleDTO = new DetalleRutinaExportacionDTO();
                detalleDTO.setNombreEjercicio(detalle.getEjercicio().getNombre());
                detalleDTO.setGrupoMuscular(detalle.getEjercicio().getGrupoMuscular());
                detalleDTO.setDiaSemana(detalle.getDiaSemana());
                detalleDTO.setOrden(detalle.getOrden());
                detalleDTO.setSeries(detalle.getSeries());
                detalleDTO.setRepeticionesMin(detalle.getRepeticionesMin());
                detalleDTO.setRepeticionesMax(detalle.getRepeticionesMax());
                detalleDTO.setPesoSugerido(
                        detalle.getPesoSugerido() != null ? detalle.getPesoSugerido().toString() : null);
                detalleDTO.setDescansoSegundos(detalle.getDescansoSegundos());
                detalleDTO.setNotas(detalle.getNotas());
                detalleDTO.setUrlImagen(detalle.getEjercicio().getUrlImagen());
                detalles.add(detalleDTO);
            }
        }
        exportDTO.setDetalles(detalles);

        try {
            return exportacionPdfService.exportarRutinaPdf(exportDTO);
        } catch (IOException e) {
            log.error("Error al generar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la rutina", e);
        }
    }

    public byte[] exportarPlanNutricionalPdf(Long idSocio, Long idPlan, String userRol, String userEmail) {
        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil autenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
            if (!socio.getIdUsuario().equals(autenticado.getIdUsuario())) {
                throw new SecurityAuthorizationException("Solo puede exportar su propio plan nutricional");
            }
        } else if (EnumRol.entrenador.name().equals(userRol)) {
            UsuarioPerfil entrenador = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));
            boolean esAsignado = entrenadorSocioRepository
                    .existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(entrenador.getIdUsuario(), idSocio);
            if (!esAsignado) {
                throw new SecurityAuthorizationException("No tiene acceso al plan nutricional de este socio");
            }
        } else if (!EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("No tiene permisos para exportar este plan nutricional");
        }

        PlanNutricionalIA plan;
        if (idPlan != null) {
            plan = planNutricionalRepository.findById(idPlan)
                    .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado"));
            if (!plan.getSocio().getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException("El plan nutricional no pertenece al socio especificado");
            }
        } else {
            plan = planNutricionalRepository.findTopBySocio_IdUsuarioAndActivoTrueOrderByFechaGeneracionDesc(idSocio)
                    .orElseThrow(() -> new RuntimeException("El socio no tiene un plan nutricional activo"));
        }

        PlanNutricionalExportacionDTO exportDTO = new PlanNutricionalExportacionDTO();
        exportDTO.setIdPlan(plan.getIdPlanNutricional());
        exportDTO.setNombreSocio(socio.getNombre());
        exportDTO.setApellidoSocio(socio.getApellido());
        exportDTO.setEmailSocio(socio.getEmail());
        exportDTO.setFechaGeneracion(plan.getFechaGeneracion());
        exportDTO.setVersion(plan.getVersion());
        exportDTO.setGeneradoPorIA(plan.getModeloIa() != null);
        exportDTO.setCaloriasDiarias(plan.getCaloriasDiarias());
        exportDTO.setProteinasG(plan.getProteinasG() != null ? plan.getProteinasG().doubleValue() : 0.0);
        exportDTO.setCarbohidratosG(plan.getCarbohidratosG() != null ? plan.getCarbohidratosG().doubleValue() : 0.0);
        exportDTO.setGrasasG(plan.getGrasasG() != null ? plan.getGrasasG().doubleValue() : 0.0);

        if (plan.getRestriccionesDieteticas() != null && !plan.getRestriccionesDieteticas().isEmpty()) {
            exportDTO.setRestriccionesDieteticas(plan.getRestriccionesDieteticas());
        } else {
            exportDTO.setRestriccionesDieteticas("Sin restricciones dietéticas");
        }

        exportDTO.setExplicacionIA(plan.getExplicacionIA());
        exportDTO.setModificadoPor(plan.getModificadoPor());
        exportDTO.setFechaModificacion(plan.getFechaModificacion());
        exportDTO.setMotivoModificacion(plan.getMotivoModificacion());

        Map<String, List<SugerenciaComidaExportacionDTO>> sugerenciasExport = new HashMap<>();
        if (plan.getSugerenciasComidas() != null && !plan.getSugerenciasComidas().isEmpty()) {
            try {
                Map<String, List<SugerenciaComidaDTO>> sugerenciasMap = objectMapper.readValue(
                        plan.getSugerenciasComidas(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<SugerenciaComidaDTO>>>() {
                        });

                for (Map.Entry<String, List<SugerenciaComidaDTO>> entry : sugerenciasMap.entrySet()) {
                    List<SugerenciaComidaExportacionDTO> listaExport = new ArrayList<>();
                    for (SugerenciaComidaDTO item : entry.getValue()) {
                        SugerenciaComidaExportacionDTO exportItem = new SugerenciaComidaExportacionDTO();
                        exportItem.setNombre(item.getNombre());
                        exportItem.setDescripcion(item.getDescripcion());
                        exportItem.setCalorias(item.getCalorias() != null ? item.getCalorias().intValue() : 0);
                        exportItem.setIngredientes(item.getIngredientes());
                        exportItem.setPreparacion(item.getPreparacion());
                        exportItem.setProteinas(item.getProteinas() != null ? item.getProteinas().doubleValue() : 0.0);
                        exportItem.setCarbohidratos(
                                item.getCarbohidratos() != null ? item.getCarbohidratos().doubleValue() : 0.0);
                        exportItem.setGrasas(item.getGrasas() != null ? item.getGrasas().doubleValue() : 0.0);
                        listaExport.add(exportItem);
                    }
                    sugerenciasExport.put(entry.getKey(), listaExport);
                }
            } catch (Exception e) {
                log.error("Error al parsear sugerencias de comidas: {}", e.getMessage());
            }
        }
        exportDTO.setSugerenciasComidas(sugerenciasExport);

        try {
            return exportacionPdfService.exportarPlanNutricionalPdf(exportDTO);
        } catch (IOException e) {
            log.error("Error al generar PDF del plan nutricional: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF del plan nutricional", e);
        }
    }

    public DashboardProgresoSocioDTO obtenerMiDashboard(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            log.warn("Intento de acceso al dashboard por usuario no socio: {}", userEmail);
            throw new SecurityAuthorizationException("Solo los socios pueden acceder a su dashboard de progreso");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            log.warn("Intento de acceso al dashboard con email nulo o vacío");
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Socio no encontrado con email: {}", userEmail);
                    return new RuntimeException("Socio no encontrado con email: " + userEmail);
                });

        if (!EnumEstadoUsuario.ACTIVO.equals(socio.getEstado())) {
            log.warn("Intento de acceso al dashboard por socio inactivo: {}", userEmail);
            throw new SecurityAuthorizationException("El usuario no se encuentra activo en el sistema");
        }

        DashboardProgresoSocioDTO dashboard = new DashboardProgresoSocioDTO();
        dashboard.setIdSocio(socio.getIdUsuario());
        dashboard.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        dashboard.setRachaDiasEntrenando(calcularRachaDias(socio.getIdUsuario()));
        dashboard.setPorcentajeCumplimientoSemanal(calcularCumplimientoSemanal(socio.getIdUsuario()));
        dashboard.setPorcentajeCumplimientoSemanaAnterior(calcularCumplimientoSemanaAnterior(socio.getIdUsuario()));
        dashboard.setDiasEntrenadosSemana(calcularDiasEntrenadosSemana(socio.getIdUsuario()));
        dashboard.setEvolucionEjercicios(calcularEvolucionEjercicios(socio.getIdUsuario()));

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSesiones", sesionRepository.countBySocio_IdUsuario(socio.getIdUsuario()));
        estadisticas.put("promedioDuracion", calcularPromedioDuracion(socio.getIdUsuario()));
        estadisticas.put("ultimaSesion", obtenerUltimaSesion(socio.getIdUsuario()));
        dashboard.setEstadisticas(estadisticas);

        log.info("Dashboard generado exitosamente para socio: {} (ID: {})", userEmail, socio.getIdUsuario());
        return dashboard;
    }

    private LocalDateTime obtenerUltimaSesion(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones.isEmpty()) {
            return null;
        }
        return sesiones.get(0).getFechaSesion();
    }

    public byte[] exportarMiRutinaPdf(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden exportar su propia rutina");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        return exportarRutinaPdf(socio.getIdUsuario(), null, userRol, userEmail);
    }

    public byte[] exportarMiRutinaEspecificaPdf(Long idRutina, String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden exportar su propia rutina");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        RutinaIA rutina = rutinaRepository.findById(idRutina)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada"));

        if (!rutina.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
            throw new SecurityAuthorizationException("La rutina no pertenece al socio autenticado");
        }

        return exportarRutinaPdf(socio.getIdUsuario(), idRutina, userRol, userEmail);
    }

    public byte[] exportarMiPlanNutricionalPdf(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden exportar su propio plan nutricional");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        return exportarPlanNutricionalPdf(socio.getIdUsuario(), null, userRol, userEmail);
    }

    public byte[] exportarMiPlanNutricionalEspecificoPdf(Long idPlan, String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden exportar su propio plan nutricional");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        PlanNutricionalIA plan = planNutricionalRepository.findById(idPlan)
                .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado"));

        if (!plan.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
            throw new SecurityAuthorizationException("El plan nutricional no pertenece al socio autenticado");
        }

        return exportarPlanNutricionalPdf(socio.getIdUsuario(), idPlan, userRol, userEmail);
    }
}