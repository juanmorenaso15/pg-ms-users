package com.pulse_gym.ms_users.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.pulse_gym.lb_common.entity.user.PlanNutricionalIA;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.SesionEntrenamiento;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoEjecucionEjercicio;
import com.pulse_gym.lb_common.enums.EnumEstadoSesion;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.DetalleSesionEjercicioRepository;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
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

    /** Servicio para exportar rutinas a PDF */
    private final ExportacionPdfService exportacionPdfService;

    /** Repositorio de planes nutricionales */
    private final PlanNutricionalRepository planNutricionalRepository;

    /** ObjectMapper para conversiones de objetos */
    private final ObjectMapper objectMapper;

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

    /**
     * Convierte una entidad DetalleSesionEjercicio a DetalleSesionResponseDTO
     * 
     * @param detalle Entidad a convertir
     * @return DTO del detalle de sesión
     */
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

    /**
     * Busca un entrenador disponible con menor carga de socios asignados
     * 
     * @return Entrenador disponible o null si no hay
     */
    private UsuarioPerfil buscarEntrenadorDisponible() {
        List<UsuarioPerfil> entrenadores = usuarioRepository.findEntrenadoresActivos();

        if (entrenadores.isEmpty()) {
            entrenadores = usuarioRepository.findByEmailContainingAndEstado("entrenador", EnumEstadoUsuario.ACTIVO);
        }

        if (entrenadores.isEmpty()) {
            log.warn("No hay entrenadores activos en el sistema");
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

    /**
     * Asigna un socio a un entrenador si no tiene asignación activa
     * 
     * @param socio      Socio a asignar
     * @param entrenador Entrenador asignado
     */
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
            log.info("Socio {} asignado automáticamente al entrenador {} al registrar sesión",
                    socio.getIdUsuario(), entrenador.getIdUsuario());
        }
    }

    /**
     * Registra una sesión de entrenamiento con asignación automática de entrenador
     * 
     * @param request   Datos de la sesión a registrar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO con la sesión registrada
     */
    @Transactional
    public SesionResponseDTO registrarSesion(RegistroSesionRequestDTO request,
            String userRol,
            String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden registrar sesiones");
        }

        UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        if (!socioAutenticado.getIdUsuario().equals(request.getIdSocio())) {
            throw new SecurityAuthorizationException("No puede registrar sesiones para otro socio");
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
                if (entrenadorAsignado != null) {
                    log.info("Asignando entrenador activo ID: {} al socio ID: {}",
                            entrenadorAsignado.getIdUsuario(), socioAutenticado.getIdUsuario());
                } else {
                    log.warn("No se encontró ningún entrenador activo disponible para asignar al socio ID: {}",
                            socioAutenticado.getIdUsuario());
                }
            }

            if (entrenadorAsignado != null) {
                asignarSocioAEntrenadorSiNoExiste(socioAutenticado, entrenadorAsignado);
            }
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
     * Obtiene el historial de sesiones de un socio con validación de permisos
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Lista de sesiones del socio
     */
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

    /**
     * Obtiene el dashboard de progreso de un socio con validación de permisos
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO con el dashboard de progreso del socio
     */
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
        dashboard.setEvolucionEjercicios(calcularEvolucionEjercicios(idSocio));

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSesiones", sesionRepository.countBySocio_IdUsuario(idSocio));
        estadisticas.put("promedioDuracion", calcularPromedioDuracion(idSocio));
        dashboard.setEstadisticas(estadisticas);

        return dashboard;
    }

    /**
     * Obtiene el dashboard de monitoreo para entrenadores con sus socios asignados
     * 
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO con el dashboard de monitoreo
     */
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
                .map(socio -> construirResumenSocio(socio))
                .collect(Collectors.toList());

        dashboard.setSociosAsignados(resumenSocios);
        return dashboard;
    }

    /**
     * Construye el resumen de un socio para el dashboard del entrenador
     * 
     * @param socio Socio a construir el resumen
     * @return DTO con el resumen del socio
     */
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

    /**
     * Calcula el número de días sin entrenar de un socio
     * 
     * @param idSocio ID del socio
     * @return Número de días sin entrenar
     */
    private Integer calcularDiasSinEntrenar(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones.isEmpty())
            return 30;
        LocalDateTime ultima = sesiones.get(0).getFechaSesion();
        long dias = java.time.temporal.ChronoUnit.DAYS.between(ultima, LocalDateTime.now());
        return (int) Math.max(dias, 0);
    }

    /**
     * Calcula la evolución de cargas de un socio basado en el peso usado en los
     * ejercicios
     * 
     * @param idSocio ID del socio
     * @return Estado de evolución (PROGRESO, RETROCESO, ESTANCADO)
     */
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

    /**
     * Calcula el porcentaje de cumplimiento semanal de un socio
     * 
     * @param idSocio ID del socio
     * @return Porcentaje de cumplimiento (0-100)
     */
    private Double calcularCumplimientoSemanal(Long idSocio) {
        LocalDateTime inicioSemana = LocalDateTime.now().minusDays(7);
        Long sesiones = sesionRepository.countSesionesEnPeriodo(idSocio, inicioSemana);
        double meta = 3.0;
        double cumplimiento = Math.min((sesiones / meta) * 100, 100.0);
        return Math.round(cumplimiento * 10.0) / 10.0;
    }

    /**
     * Calcula el porcentaje de cumplimiento de la semana anterior de un socio
     * 
     * @param idSocio ID del socio
     * @return Porcentaje de cumplimiento de la semana anterior (0-100)
     */
    private Double calcularCumplimientoSemanaAnterior(Long idSocio) {
        LocalDateTime inicio = LocalDateTime.now().minusDays(14);
        LocalDateTime fin = LocalDateTime.now().minusDays(7);
        Long sesiones = sesionRepository.countSesionesEnPeriodo(idSocio, inicio, fin);
        double meta = 3.0;
        double cumplimiento = Math.min((sesiones / meta) * 100, 100.0);
        return Math.round(cumplimiento * 10.0) / 10.0;
    }

    /**
     * Calcula la evolución de cada ejercicio de un socio basado en el peso usado
     * 
     * @param idSocio ID del socio
     * @return Lista de evolución por ejercicio
     */
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

    /**
     * Calcula el promedio de duración de las sesiones de un socio
     * 
     * @param idSocio ID del socio
     * @return Promedio de duración en minutos
     */
    private Double calcularPromedioDuracion(Long idSocio) {
        List<SesionEntrenamiento> sesiones = sesionRepository.findBySocio_IdUsuarioOrderByFechaSesionDesc(idSocio);
        if (sesiones.isEmpty())
            return 0.0;
        return sesiones.stream().mapToInt(SesionEntrenamiento::getDuracionMinutos).average().orElse(0.0);
    }

    /**
     * Calcula la racha de días consecutivos entrenando de un socio
     * 
     * @param idSocio ID del socio
     * @return Número de días consecutivos entrenando
     */
    private Integer calcularRachaDias(Long idSocio) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(30);
        List<SesionEntrenamiento> sesiones = sesionRepository.findSesionesDesdeFecha(idSocio, fechaLimite);
        if (sesiones.isEmpty())
            return 0;

        Set<LocalDate> fechasConSesion = sesiones.stream()
                .map(s -> s.getFechaSesion().toLocalDate())
                .collect(Collectors.toSet());

        int racha = 0;
        LocalDate fechaActual = LocalDate.now();
        while (fechasConSesion.contains(fechaActual)) {
            racha++;
            fechaActual = fechaActual.minusDays(1);
        }
        return racha;
    }

    /**
     * Exporta una rutina a formato PDF con validación de permisos
     * 
     * @param idSocio   ID del socio dueño de la rutina
     * @param idRutina  ID de la rutina a exportar (opcional, si es null se usa la
     *                  activa)
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Array de bytes del PDF generado
     */
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(rutina.getRutinaGenerada());

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

    /**
     * Exporta un plan nutricional a formato PDF con validación de permisos
     * 
     * @param idSocio   ID del socio dueño del plan
     * @param idPlan    ID del plan nutricional a exportar (opcional, si es null se
     *                  usa el activo)
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Array de bytes del PDF generado
     */
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
}