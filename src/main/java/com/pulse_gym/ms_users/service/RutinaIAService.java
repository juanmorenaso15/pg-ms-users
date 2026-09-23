package com.pulse_gym.ms_users.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse_gym.lb_common.client.AiClient;
import com.pulse_gym.lb_common.client.EquipoClient;
import com.pulse_gym.lb_common.dto.EquipoResponseDTO;
import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.RutinaGeneracionRequestDTO;
import com.pulse_gym.lb_common.entity.user.Ejercicio;
import com.pulse_gym.lb_common.entity.user.HistorialFisico;
import com.pulse_gym.lb_common.entity.user.PerfilMedico;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.EjercicioRepository;
import com.pulse_gym.ms_users.repository.HistorialFisicoRepository;
import com.pulse_gym.ms_users.repository.PerfilMedicoRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.SocioMembresiaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutinaIAService {

    private final UsuarioPerfilRepository usuarioRepository;
    private final PerfilMedicoRepository perfilMedicoRepository;
    private final HistorialFisicoRepository historialFisicoRepository;
    private final EjercicioRepository ejercicioRepository;
    private final SocioMembresiaRepository socioMembresiaRepository;
    private final RutinaRepository rutinaRepository;
    private final DetalleRutinaRepository detalleRutinaRepository;
    private final AiClient aiClient;
    private final EquipoClient equipoClient;
    private final ObjectMapper objectMapper;

    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null)
            return 0;
        return Period.between(fechaNacimiento, com.pulse_gym.lb_common.util.FechaUtils.ahoraColombia().toLocalDate()).getYears();
    }

    public void validarMembresiaActiva(Long idSocio) {
        log.info("Validando membresía activa para socio ID: {}", idSocio);

        SocioMembresia membresiaActiva = socioMembresiaRepository.findMembresiaActivaBySocio(idSocio)
                .orElseThrow(() -> new RuntimeException(
                        "El socio no tiene una membresía activa. No puede generar rutinas."));

        if (!membresiaActiva.isActiva()) {
            throw new RuntimeException(
                    "La membresía del socio está inactiva o vencida. Estado actual: " + membresiaActiva.getEstado());
        }

        log.info("Membresía activa confirmada para socio ID: {}, vence el: {}",
                idSocio, membresiaActiva.getFechaVencimiento());
    }

    public EstadoMembresiaResponseDTO obtenerEstadoMembresia(Long idSocio) {
        SocioMembresia membresiaActiva = socioMembresiaRepository.findMembresiaActivaBySocio(idSocio)
                .orElse(null);

        if (membresiaActiva == null) {
            return EstadoMembresiaResponseDTO.builder()
                    .idSocio(idSocio)
                    .estado("SIN_MEMBRESIA")
                    .activa(false)
                    .vencida(false)
                    .diasRestantes(0L)
                    .mensaje("El socio no tiene membresía activa")
                    .build();
        }

        return EstadoMembresiaResponseDTO.builder()
                .idSocio(idSocio)
                .idSocioMembresia(membresiaActiva.getIdSocioMembresia())
                .idMembresia(membresiaActiva.getMembresia().getIdMembresia())
                .nombreMembresia(membresiaActiva.getMembresia().getNombre())
                .fechaInicio(membresiaActiva.getFechaInicio())
                .fechaVencimiento(membresiaActiva.getFechaVencimiento())
                .estado(membresiaActiva.getEstado().name())
                .activa(membresiaActiva.isActiva())
                .vencida(membresiaActiva.isVencida())
                .diasRestantes(membresiaActiva.getDiasRestantes())
                .mensaje(membresiaActiva.isActiva() ? "Membresía activa" : "Membresía inactiva")
                .build();
    }

    public Map<String, Object> recopilarDatosSocio(Long idSocio) {
        log.info("Recopilando datos del socio ID: {}", idSocio);

        Map<String, Object> datos = new HashMap<>();

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        datos.put("idSocio", socio.getIdUsuario());
        datos.put("nombre", socio.getNombre());
        datos.put("apellido", socio.getApellido());
        datos.put("email", socio.getEmail());
        datos.put("edad", calcularEdad(socio.getFechaNacimiento()));
        datos.put("fechaNacimiento", socio.getFechaNacimiento().toString());
        datos.put("objetivoPrincipal", socio.getObjetivoPrincipal());
        datos.put("nivelExperiencia", socio.getNivelExperiencia().name());
        datos.put("idSede", socio.getIdSede());

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuario(idSocio)
                .orElse(null);

        if (perfilMedico != null) {
            datos.put("peso", perfilMedico.getPesoKg());
            datos.put("estatura", perfilMedico.getEstaturaCm());
            datos.put("alergias", perfilMedico.getAlergias());
            datos.put("condicionesCronicas", perfilMedico.getCondicionesCronicas());
            datos.put("lesionesPrevias", perfilMedico.getLesionesPrevias());
            datos.put("porcentajeGrasa", perfilMedico.getPorcentajeGrasa());
        } else {
            log.warn("El socio ID: {} no tiene perfil médico registrado", idSocio);
        }

        HistorialFisico ultimaMedicion = historialFisicoRepository.findLastMedicionBySocio(idSocio);
        if (ultimaMedicion != null) {
            datos.put("pesoKg", ultimaMedicion.getPesoKg());
            datos.put("porcentajeGrasa", ultimaMedicion.getPorcentajeGrasa());
            datos.put("porcentajeMusculo", ultimaMedicion.getPorcentajeMusculo());
            datos.put("fechaMedicion", ultimaMedicion.getFechaMedicion().toString());
        }

        SocioMembresia membresia = socioMembresiaRepository.findMembresiaActivaBySocio(idSocio)
                .orElse(null);
        if (membresia != null) {
            datos.put("membresiaActiva", true);
            datos.put("membresiaNombre", membresia.getMembresia().getNombre());
            datos.put("fechaVencimiento", membresia.getFechaVencimiento().toString());
        } else {
            datos.put("membresiaActiva", false);
        }

        List<RutinaIA> rutinasAnteriores = rutinaRepository.findBySocio_IdUsuarioOrderByFechaGeneracionDesc(idSocio);
        if (!rutinasAnteriores.isEmpty()) {
            datos.put("tieneRutinasAnteriores", true);
            datos.put("cantidadRutinas", rutinasAnteriores.size());
            datos.put("ultimaRutinaFecha", rutinasAnteriores.get(0).getFechaGeneracion().toString());
        } else {
            datos.put("tieneRutinasAnteriores", false);
        }

        List<Ejercicio> ejercicios = ejercicioRepository.findByActivoTrue();
        datos.put("ejerciciosDisponibles", ejercicios.stream()
                .map(this::convertirEjercicioParaIA)
                .collect(Collectors.toList()));

        List<Map<String, Object>> equiposDisponibles = obtenerEquiposDisponibles(socio.getIdSede());
        datos.put("equiposDisponibles", equiposDisponibles);

        Map<String, Object> statsEjercicios = new HashMap<>();
        statsEjercicios.put("total", (long) ejercicios.size());
        statsEjercicios.put("porGrupoMuscular", ejercicios.stream()
                .collect(Collectors.groupingBy(Ejercicio::getGrupoMuscular, Collectors.counting())));

        datos.put("statsEjercicios", statsEjercicios);

        log.info("Datos recopilados para socio ID: {} - {} campos, {} equipos disponibles válidos",
                idSocio, datos.size(), equiposDisponibles.size());
        return datos;
    }

    /**
     * Obtiene los equipos disponibles en la sede del socio desde operation.
     * Filtra equipos operativos y descarta cualquier registro incompleto
     * (nombre nulo/vacío) que pueda venir por un mapeo incorrecto del DTO,
     * ya que enviarle a la IA equipos sin nombre hace que los ignore por
     * completo y termine generando siempre rutinas de peso corporal.
     * 
     * @param idSede ID de la sede
     * @return Lista de equipos en formato para IA
     */
    private List<Map<String, Object>> obtenerEquiposDisponibles(Integer idSede) {
        List<Map<String, Object>> equiposFormat = new ArrayList<>();
        try {
            List<EquipoResponseDTO> equipos;
            if (idSede != null) {
                equipos = equipoClient.obtenerEquiposPorSede(idSede);
            } else {
                equipos = equipoClient.obtenerTodosLosEquipos();
            }

            // Log de diagnóstico: nos permite ver EXACTAMENTE qué llega desde ms-operation
            if (equipos != null) {
                log.info("Equipos crudos recibidos de ms-operation (sede {}): {}", idSede, equipos.size());
                equipos.forEach(e -> log.info(
                        "  -> idEquipo={}, nombreEquipo='{}', estado='{}', descripcion='{}', idSede={}",
                        e.getIdEquipo(), e.getNombreEquipo(), e.getEstado(), e.getDescripcion(), e.getIdSede()));
            } else {
                log.warn("ms-operation devolvió null en la lista de equipos para sede {}", idSede);
            }

            if (equipos != null && !equipos.isEmpty()) {
                equiposFormat = equipos.stream()
                        .filter(e -> e.getEstado() != null && "OPERATIVO".equalsIgnoreCase(e.getEstado().trim()))
                        .filter(e -> e.getNombreEquipo() != null && !e.getNombreEquipo().trim().isEmpty())
                        .map(this::convertirEquipoParaIA)
                        .collect(Collectors.toList());

                if (equiposFormat.isEmpty()) {
                    log.error("ATENCIÓN: ms-operation devolvió {} equipos pero NINGUNO quedó válido " +
                            "tras el filtro (revisar si el DTO se está mapeando correctamente, " +
                            "posible mismatch de nombres de campo en el JSON de respuesta).",
                            equipos.size());
                } else {
                    log.info("Se encontraron {} equipos operativos y válidos en la sede {}",
                            equiposFormat.size(), idSede);
                }
            } else {
                log.warn("No se encontraron equipos en la sede {}", idSede);
            }
        } catch (Exception e) {
            log.error("Error al obtener equipos desde ms-operation (sede {}): {}", idSede, e.getMessage(), e);
        }
        return equiposFormat;
    }

    private Map<String, Object> convertirEquipoParaIA(EquipoResponseDTO equipo) {
        Map<String, Object> eq = new HashMap<>();
        eq.put("id", equipo.getIdEquipo());
        eq.put("nombre", equipo.getNombreEquipo());
        eq.put("marca", "");
        eq.put("modelo", "");
        eq.put("ubicacion", equipo.getDescripcion() != null ? equipo.getDescripcion() : "No especificada");
        eq.put("estado", equipo.getEstado() != null ? equipo.getEstado() : "OPERATIVO");
        return eq;
    }

    private Map<String, Object> convertirEjercicioParaIA(Ejercicio ejercicio) {
        Map<String, Object> ej = new HashMap<>();
        ej.put("id", ejercicio.getIdEjercicio());
        ej.put("nombre", ejercicio.getNombre());
        ej.put("grupoMuscular", ejercicio.getGrupoMuscular());
        ej.put("equipoNecesario", ejercicio.getEquipoNecesario());
        ej.put("dificultad", ejercicio.getDificultad());
        ej.put("urlImagen", ejercicio.getUrlImagen());
        return ej;
    }

    public void validarRolGeneracion(String userRol, Long idSocio, Long userIdAutenticado, String userEmail) {
        if (userRol == null) {
            throw new SecurityAuthorizationException("Usuario no autenticado");
        }

        if (EnumRol.administrador.name().equals(userRol)) {
            return;
        }

        if (EnumRol.entrenador.name().equals(userRol)) {
            return;
        }

        if (EnumRol.recepcionista.name().equals(userRol)) {
            return;
        }

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!socio.getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Los socios solo pueden generar rutinas para sí mismos. " +
                                "Tu ID en usuario_perfil: %d, ID solicitado: %d",
                                socio.getIdUsuario(), idSocio));
            }
            return;
        }

        throw new SecurityAuthorizationException(
                "Acceso denegado. Rol '" + userRol + "' no autorizado para generar rutinas");
    }

    public Map<String, Object> construirContextoIA(Long idSocio, RutinaGeneracionRequestDTO request) {
        Map<String, Object> contexto = recopilarDatosSocio(idSocio);

        if (request != null) {
            contexto.put("diasPorSemana", request.getDiasPorSemana() != null ? request.getDiasPorSemana() : 3);
            contexto.put("duracionSemanas", request.getDuracionSemanas() != null ? request.getDuracionSemanas() : 4);
            contexto.put("preferenciasEquipamiento", request.getPreferenciasEquipamiento());
            contexto.put("evitarEjercicios", request.getEvitarEjercicios());
            contexto.put("preferenciasGruposMusculares", request.getPreferenciasGruposMusculares());
            contexto.put("objetivoEspecifico", request.getObjetivoEspecifico());
            contexto.put("incluirCardio", request.getIncluirCardio() != null ? request.getIncluirCardio() : true);
        }

        return contexto;
    }
}