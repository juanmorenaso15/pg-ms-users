package com.pulse_gym.ms_users.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse_gym.lb_common.client.AiClient;
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

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de perfiles médicos */
    private final PerfilMedicoRepository perfilMedicoRepository;

    /** Repositorio de historial físico */
    private final HistorialFisicoRepository historialFisicoRepository;

    /** Repositorio de ejercicios */
    private final EjercicioRepository ejercicioRepository;

    /** Repositorio de membresías de socios */
    private final SocioMembresiaRepository socioMembresiaRepository;

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de detalles de rutina */
    private final DetalleRutinaRepository detalleRutinaRepository;

    /** Cliente Feign para consumir el servicio de IA */
    private final AiClient aiClient;

    /** Mapper para convertir objetos a JSON */
    private final ObjectMapper objectMapper;

    /**
     * Calcula la edad a partir de la fecha de nacimiento
     * 
     * @param fechaNacimiento Fecha de nacimiento
     * @return Edad en años, o 0 si la fecha es nula
     */
    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null)
            return 0;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /**
     * Valida que el socio tenga una membresía activa
     * 
     * @param idSocio ID del socio a validar
     */
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

    /**
     * Obtiene el estado de la membresía de un socio
     * 
     * @param idSocio ID del socio a consultar
     * @return DTO con el estado de la membresía
     */
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

        Map<String, Object> statsEjercicios = new HashMap<>();
        statsEjercicios.put("total", (long) ejercicios.size());
        statsEjercicios.put("porGrupoMuscular", ejercicios.stream()
                .collect(Collectors.groupingBy(Ejercicio::getGrupoMuscular, Collectors.counting())));

        datos.put("statsEjercicios", statsEjercicios);

        log.info("Datos recopilados para socio ID: {} - {} campos", idSocio, datos.size());
        return datos;
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

    /**
     * Valida que el usuario tenga permisos para generar rutinas
     * 
     * @param userRol           Rol del usuario autenticado
     * @param idSocio           ID del socio para el que se genera la rutina
     * @param userIdAutenticado ID del usuario autenticado
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     */
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

    /**
     * Construye el contexto con los datos del socio y preferencias para la IA
     * 
     * @param idSocio ID del socio
     * @param request Preferencias del socio para la rutina
     * @return Mapa con el contexto completo para la IA
     */
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
