package com.pulse_gym.ms_users.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse_gym.lb_common.client.AiClient;
import com.pulse_gym.lb_common.dto.DetalleRutinaResponseDTO;
import com.pulse_gym.lb_common.dto.RutinaAjusteRequestDTO;
import com.pulse_gym.lb_common.dto.RutinaGeneracionRequestDTO;
import com.pulse_gym.lb_common.dto.RutinaGeneracionResponseDTO;
import com.pulse_gym.lb_common.dto.RutinaHistorialResponseDTO;
import com.pulse_gym.lb_common.entity.user.DetalleRutina;
import com.pulse_gym.lb_common.entity.user.Ejercicio;
import com.pulse_gym.lb_common.entity.user.EntrenadorSocio;
import com.pulse_gym.lb_common.entity.user.HistorialRutinaVersion;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.EjercicioRepository;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.HistorialRutinaVersionRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RutinaService {

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de detalles de rutina */
    private final DetalleRutinaRepository detalleRutinaRepository;

    /** Repositorio de historial de versiones de rutina */

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de ejercicios */
    private final EjercicioRepository ejercicioRepository;

    /** Repositorio de membresías de socios */

    /** Servicio de generación de rutinas con IA */
    private final RutinaIAService rutinaIAService;

    /** Cliente Feign para consumir el servicio de IA */
    private final AiClient aiClient;

    /** Mapper para convertir objetos a JSON */
    private final ObjectMapper objectMapper;

    /** Repositorio de historial de versiones de rutina */
    private final HistorialRutinaVersionRepository historialRutinaVersionRepository;

    /** Servicio de gestión de entrenadores */
    private final EntrenadorService entrenadorService;

    /** Repositorio de relaciones entre entrenadores y socios */
    private final EntrenadorSocioRepository entrenadorSocioRepository;

    /**
     * Convierte una entidad RutinaIA a RutinaGeneracionResponseDTO
     * 
     * @param rutina Entidad a convertir
     * @return DTO de la rutina
     */
    private RutinaGeneracionResponseDTO convertirAResponseDTO(RutinaIA rutina) {
        RutinaGeneracionResponseDTO dto = new RutinaGeneracionResponseDTO();
        dto.setIdRutina(rutina.getIdRutinaIa());
        dto.setNombre("Rutina " + rutina.getObjetivo());
        dto.setDescripcion("Rutina personalizada generada por IA");
        dto.setExplicacionIA(rutina.getExplicacionIa());
        dto.setVersion(rutina.getVersion());
        dto.setGeneradaPorIA(rutina.getModeloIa() != null);
        dto.setFechaGeneracion(rutina.getFechaGeneracion());

        List<DetalleRutina> detalles = detalleRutinaRepository
                .findByRutinaIa_IdRutinaIaOrderByDiaSemanaAscOrdenAsc(rutina.getIdRutinaIa());

        List<DetalleRutinaResponseDTO> detallesDTO = detalles.stream()
                .map(this::convertirDetalleAResponseDTO)
                .collect(Collectors.toList());

        dto.setDetalles(detallesDTO);

        return dto;
    }

    /**
     * Convierte una entidad DetalleRutina a DetalleRutinaResponseDTO
     * 
     * @param detalle Entidad a convertir
     * @return DTO del detalle
     */
    private DetalleRutinaResponseDTO convertirDetalleAResponseDTO(DetalleRutina detalle) {
        DetalleRutinaResponseDTO dto = new DetalleRutinaResponseDTO();
        dto.setIdDetalle(detalle.getIdDetalleRutina());
        dto.setIdEjercicio(detalle.getEjercicio().getIdEjercicio());
        dto.setNombreEjercicio(detalle.getEjercicio().getNombre());
        dto.setGrupoMuscular(detalle.getEjercicio().getGrupoMuscular());
        dto.setUrlImagen(detalle.getEjercicio().getUrlImagen());
        dto.setDiaSemana(detalle.getDiaSemana());
        dto.setOrden(detalle.getOrden());
        dto.setSeries(detalle.getSeries());
        dto.setRepeticionesMin(detalle.getRepeticionesMin());
        dto.setRepeticionesMax(detalle.getRepeticionesMax());
        dto.setPesoSugerido(detalle.getPesoSugerido());
        dto.setDescansoSegundos(detalle.getDescansoSegundos());
        dto.setNotas(detalle.getNotas());
        dto.setModificadoPor(detalle.getModificadoPor());
        return dto;
    }

    /**
     * Convierte una entidad HistorialRutinaVersion a RutinaHistorialResponseDTO
     * 
     * @param historial Entidad a convertir
     * @return DTO del historial
     */
    private RutinaHistorialResponseDTO convertirHistorialAResponseDTO(HistorialRutinaVersion historial) {
        RutinaHistorialResponseDTO dto = new RutinaHistorialResponseDTO();
        dto.setIdHistorial(historial.getIdHistorial());
        dto.setVersion(historial.getVersion());

        try {
            if (historial.getDatosJson() != null && !historial.getDatosJson().isEmpty()) {
                Object jsonObject = objectMapper.readValue(historial.getDatosJson(), Object.class);
                dto.setDatosJson(jsonObject);
            } else {
                dto.setDatosJson(null);
            }
        } catch (JsonProcessingException e) {
            log.warn("Error al parsear datosJson: {}", e.getMessage());
            dto.setDatosJson(historial.getDatosJson());
        }

        dto.setMotivo(historial.getMotivo());
        dto.setFechaModificacion(historial.getFechaModificacion());

        if (historial.getModificadoPorNombre() != null && !historial.getModificadoPorNombre().isEmpty()) {
            dto.setModificadoPor(historial.getModificadoPorNombre());
        } else if (historial.getModificadoPor() != null) {
            dto.setModificadoPor(historial.getModificadoPor().getEmail());
        } else {
            dto.setModificadoPor("Sistema");
        }

        return dto;
    }

    /**
     * Guarda el historial de la versión inicial de la rutina
     * 
     * @param rutina Rutina recién generada
     */
    /**
     * Guarda el historial de la versión inicial de la rutina
     * 
     * @param rutina Rutina recién generada
     */
    private void guardarHistorialVersionInicial(RutinaIA rutina) {
        try {
            RutinaGeneracionResponseDTO dto = convertirAResponseDTO(rutina);
            String rutinaJson = objectMapper.writeValueAsString(dto);

            HistorialRutinaVersion historial = new HistorialRutinaVersion();
            historial.setRutinaIa(rutina);
            historial.setVersion(1);
            historial.setDatosJson(rutinaJson);
            historial.setMotivo("Generación inicial");
            historial.setFechaModificacion(rutina.getFechaGeneracion());
            historial.setModificadoPorNombre("Sistema");
            historialRutinaVersionRepository.save(historial);

            log.info("Historial inicial guardado para rutina ID: {}", rutina.getIdRutinaIa());
        } catch (JsonProcessingException e) {
            log.error("Error al serializar JSON para historial: {}", e.getMessage());
            try {
                HistorialRutinaVersion historial = new HistorialRutinaVersion();
                historial.setRutinaIa(rutina);
                historial.setVersion(1);
                historial.setDatosJson(rutina.getRutinaGenerada());
                historial.setMotivo("Generación inicial (fallback)");
                historial.setFechaModificacion(rutina.getFechaGeneracion());
                historial.setModificadoPorNombre("Sistema"); // 🔥 VALOR POR DEFECTO
                historialRutinaVersionRepository.save(historial);
                log.info("Historial inicial guardado con fallback para rutina ID: {}", rutina.getIdRutinaIa());
            } catch (Exception ex) {
                log.error("Error al guardar historial inicial con fallback: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al guardar historial inicial: {}", e.getMessage());
        }
    }

    /**
     * Guarda un registro en el historial de versiones de la rutina
     * 
     * @param rutina        Rutina que se está modificando
     * @param modificadoPor Nombre del usuario que realizó la modificación
     * @param motivo        Motivo de la modificación
     */
    private void guardarHistorialVersion(RutinaIA rutina, String modificadoPor, String motivo) {
        try {
            RutinaGeneracionResponseDTO dto = convertirAResponseDTO(rutina);
            String rutinaJson = objectMapper.writeValueAsString(dto);

            HistorialRutinaVersion historial = new HistorialRutinaVersion();
            historial.setRutinaIa(rutina);
            historial.setVersion(rutina.getVersion());
            historial.setDatosJson(rutinaJson);
            historial.setMotivo(motivo != null ? motivo : "Ajuste manual - Versión " + rutina.getVersion());

            if (modificadoPor != null && !modificadoPor.isEmpty()) {
                historial.setModificadoPorNombre(modificadoPor);

                if (modificadoPor.contains("(")) {
                    String email = modificadoPor.substring(modificadoPor.indexOf("(") + 1, modificadoPor.indexOf(")"));
                    usuarioRepository.findByEmail(email).ifPresent(historial::setModificadoPor);
                }
            } else {
                historial.setModificadoPorNombre("Sistema");
            }

            historial.setFechaModificacion(LocalDateTime.now());
            historialRutinaVersionRepository.save(historial);

            log.info("Historial guardado para rutina ID: {}, versión: {}, modificado por: {}",
                    rutina.getIdRutinaIa(), rutina.getVersion(),
                    historial.getModificadoPorNombre() != null ? historial.getModificadoPorNombre() : "Sistema");
        } catch (JsonProcessingException e) {
            log.error("Error al serializar JSON para historial: {}", e.getMessage());
            try {
                HistorialRutinaVersion historial = new HistorialRutinaVersion();
                historial.setRutinaIa(rutina);
                historial.setVersion(rutina.getVersion());
                historial.setDatosJson(rutina.getRutinaGenerada());
                historial.setMotivo(
                        motivo != null ? motivo : "Ajuste manual - Versión " + rutina.getVersion() + " (fallback)");

                if (modificadoPor != null && !modificadoPor.isEmpty()) {
                    historial.setModificadoPorNombre(modificadoPor);
                } else {
                    historial.setModificadoPorNombre("Sistema");
                }

                historial.setFechaModificacion(LocalDateTime.now());
                historialRutinaVersionRepository.save(historial);
                log.info("Historial guardado con fallback para rutina ID: {}, versión: {}",
                        rutina.getIdRutinaIa(), rutina.getVersion());
            } catch (Exception ex) {
                log.error("Error al guardar historial con fallback: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al guardar historial: {}", e.getMessage());
        }
    }

    /**
     * Genera una rutina de entrenamiento usando IA
     * 
     * @param request           Preferencias para la generación de la rutina
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return DTO con la rutina generada
     * @throws RuntimeException Si ocurre un error en la generación
     */
    @Transactional
    public RutinaGeneracionResponseDTO generarRutinaIA(
            RutinaGeneracionRequestDTO request,
            String userRol,
            Long userIdAutenticado,
            String userEmail) {

        log.info("Iniciando generación de rutina IA para socio ID: {}", request.getIdSocio());

        UsuarioPerfil socio;
        if ("socio".equals(userRol)) {
            socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            request.setIdSocio(socio.getIdUsuario());
            log.info("Socio autenticado por email: {}, ID en usuario_perfil: {}", userEmail, socio.getIdUsuario());
        } else {
            socio = usuarioRepository.findById(request.getIdSocio())
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + request.getIdSocio()));
        }

        rutinaIAService.validarRolGeneracion(userRol, request.getIdSocio(), userIdAutenticado, userEmail);
        rutinaIAService.validarMembresiaActiva(request.getIdSocio());

        Map<String, Object> contexto = rutinaIAService.construirContextoIA(request.getIdSocio(), request);

        RutinaGeneracionResponseDTO respuestaIA = null;
        try {
            String respuestaJson = aiClient.generarRutinaConContexto(contexto);

            log.info("JSON recibido de Python (primeros 300 chars): {}",
                    respuestaJson.length() > 300 ? respuestaJson.substring(0, 300) + "..." : respuestaJson);
            com.fasterxml.jackson.core.type.TypeReference<RutinaGeneracionResponseDTO> typeRef = new com.fasterxml.jackson.core.type.TypeReference<RutinaGeneracionResponseDTO>() {
            };
            respuestaIA = objectMapper.readValue(respuestaJson, typeRef);

            if (respuestaIA.getDetalles() != null) {
                log.info("Detalles recibidos: {}", respuestaIA.getDetalles().size());
            } else {
                log.warn("No se recibieron detalles. Intentando parsear manualmente...");

                Map<String, Object> jsonMap = objectMapper.readValue(respuestaJson, Map.class);
                log.info("Claves del JSON: {}", jsonMap.keySet());

                if (jsonMap.containsKey("detalles")) {
                    log.info("'detalles' está presente en el JSON");
                    Object detallesObj = jsonMap.get("detalles");
                    log.info("detalles es de tipo: {}", detallesObj.getClass().getName());
                }
            }

            log.info("Respuesta de IA recibida correctamente");
        } catch (Exception e) {
            log.error("Error al llamar al servicio de IA: {}", e.getMessage());
            throw new RuntimeException("Error al generar rutina con IA: " + e.getMessage());
        }

        if (respuestaIA.getDetalles() == null) {
            log.warn("No hay detalles en la respuesta. Se creará rutina sin ejercicios.");
            respuestaIA.setDetalles(new ArrayList<>());
        }

        RutinaIA rutina = guardarRutina(socio, respuestaIA, request);
        enriquecerConImagenes(respuestaIA);

        respuestaIA.setIdRutina(rutina.getIdRutinaIa());
        respuestaIA.setVersion(rutina.getVersion());
        respuestaIA.setFechaGeneracion(rutina.getFechaGeneracion());
        respuestaIA.setGeneradaPorIA(true);

        log.info("Rutina generada exitosamente con ID: {}, para socio: {}",
                rutina.getIdRutinaIa(), socio.getNombre());

        return respuestaIA;
    }

    /**
     * Asigna un socio a un entrenador si no existe una relación activa entre ellos
     * 
     * @param socio      Socio a asignar
     * @param entrenador Entrenador al que se asigna el socio
     */
    private void asignarSocioAEntrenadorSiNoExiste(UsuarioPerfil socio, UsuarioPerfil entrenador) {
        try {
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
                log.info("Socio {} asignado automáticamente al entrenador {}",
                        socio.getIdUsuario(), entrenador.getIdUsuario());
            }
        } catch (Exception e) {
            log.warn("Error al asignar socio a entrenador: {}", e.getMessage());
        }
    }

    /**
     * Guarda la rutina generada por IA en la base de datos
     * 
     * @param socio       Socio al que se le asigna la rutina
     * @param respuestaIA DTO con los detalles de la rutina generada
     * @param request     DTO con las preferencias del socio
     * @return Entidad RutinaIA guardada
     */
    private RutinaIA guardarRutina(UsuarioPerfil socio, RutinaGeneracionResponseDTO respuestaIA,
            RutinaGeneracionRequestDTO request) {

        RutinaIA rutina = new RutinaIA();
        rutina.setSocio(socio);
        rutina.setObjetivo(request.getObjetivoEspecifico() != null ? request.getObjetivoEspecifico()
                : socio.getObjetivoPrincipal());
        rutina.setNivel(socio.getNivelExperiencia().name());
        rutina.setCondiciones("Días por semana: " + request.getDiasPorSemana() +
                ", Duración: " + request.getDuracionSemanas() + " semanas");
        rutina.setModeloIa("groq/compound");
        rutina.setVersion(1);
        rutina.setActiva(true);
        rutina.setExplicacionIa(respuestaIA.getExplicacionIA());

        UsuarioPerfil entrenador = entrenadorService.buscarEntrenadorDisponible();
        if (entrenador != null) {
            rutina.setEntrenador(entrenador);
            log.info("Entrenador asignado automáticamente a la rutina: {} {} (ID: {})",
                    entrenador.getNombre(), entrenador.getApellido(), entrenador.getIdUsuario());

            asignarSocioAEntrenadorSiNoExiste(socio, entrenador);
        } else {
            log.warn("No se encontró entrenador disponible para asignar a la rutina del socio ID: {}",
                    socio.getIdUsuario());
        }

        try {
            String rutinaJson = objectMapper.writeValueAsString(respuestaIA);
            rutina.setRutinaGenerada(rutinaJson);
        } catch (JsonProcessingException e) {
            log.warn("Error al serializar rutina a JSON: {}", e.getMessage());
            rutina.setRutinaGenerada(respuestaIA.toString());
        }

        rutina = rutinaRepository.save(rutina);
        log.info("Rutina guardada con ID: {}", rutina.getIdRutinaIa());

        if (respuestaIA.getDetalles() != null) {
            int detallesGuardados = 0;
            for (DetalleRutinaResponseDTO detalleDTO : respuestaIA.getDetalles()) {
                DetalleRutina detalle = new DetalleRutina();
                detalle.setRutinaIa(rutina);

                Ejercicio ejercicio = ejercicioRepository.findByNombreAndActivoTrue(detalleDTO.getNombreEjercicio())
                        .orElse(null);

                if (ejercicio == null) {
                    log.warn("Ejercicio no encontrado: {}, se omitirá", detalleDTO.getNombreEjercicio());
                    continue;
                }

                detalle.setEjercicio(ejercicio);
                detalle.setSeries(detalleDTO.getSeries() != null ? detalleDTO.getSeries() : 3);
                detalle.setRepeticionesMin(detalleDTO.getRepeticionesMin());
                detalle.setRepeticionesMax(detalleDTO.getRepeticionesMax());
                detalle.setPesoSugerido(detalleDTO.getPesoSugerido());
                detalle.setDescansoSegundos(detalleDTO.getDescansoSegundos());
                detalle.setDiaSemana(detalleDTO.getDiaSemana());
                detalle.setOrden(detalleDTO.getOrden());
                detalle.setNotas(detalleDTO.getNotas());

                detalleRutinaRepository.save(detalle);
                rutina.addDetalle(detalle);
                detallesGuardados++;
            }
            log.info("{} detalles guardados correctamente", detallesGuardados);
        }

        guardarHistorialVersionInicial(rutina);

        log.info("Rutina guardada con {} detalles totales", rutina.getDetalles().size());
        return rutina;
    }

    /**
     * Enriquece los detalles de la rutina con imágenes, videos y grupo muscular
     * desde la base de datos
     * 
     * @param respuesta DTO de respuesta de la IA a enriquecer
     */
    private void enriquecerConImagenes(RutinaGeneracionResponseDTO respuesta) {
        if (respuesta.getDetalles() == null) {
            return;
        }

        for (DetalleRutinaResponseDTO detalle : respuesta.getDetalles()) {
            ejercicioRepository.findByNombreAndActivoTrue(detalle.getNombreEjercicio())
                    .ifPresent(ejercicio -> {
                        detalle.setIdEjercicio(ejercicio.getIdEjercicio());
                        detalle.setUrlImagen(ejercicio.getUrlImagen());
                        detalle.setGrupoMuscular(ejercicio.getGrupoMuscular());
                    });
        }
    }

    public List<RutinaGeneracionResponseDTO> obtenerMisRutinas(
            Long userIdAutenticado, String userRol, String userEmail) {

        Long idSocio;

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));
            idSocio = socio.getIdUsuario();
            log.info("Socio autenticado por email: {}, ID en usuario_perfil: {}", userEmail, idSocio);
        } else {
            idSocio = userIdAutenticado;
        }

        return obtenerRutinasSocio(idSocio, userRol, userIdAutenticado, userEmail);
    }

    /**
     * Obtiene una rutina por su ID con validación de permisos
     * 
     * @param idRutina          ID de la rutina a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado (de auth)
     * @param userEmail         Email del usuario autenticado
     * @return DTO de la rutina
     */
    public RutinaGeneracionResponseDTO obtenerRutina(Long idRutina, String userRol,
            Long userIdAutenticado, String userEmail) {

        RutinaIA rutina = rutinaRepository.findById(idRutina)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada con ID: " + idRutina));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!rutina.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Solo puede ver sus propias rutinas. " +
                                "Tu ID en usuario_perfil: %d, ID de la rutina: %d",
                                socio.getIdUsuario(), rutina.getSocio().getIdUsuario()));
            }
        }

        return convertirAResponseDTO(rutina);
    }

    /**
     * Obtiene todas las rutinas de un socio con validación de permisos
     * 
     * @param idSocio           ID del socio (de usuario_perfil)
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado (de auth)
     * @param userEmail         Email del usuario autenticado
     * @return Lista de rutinas del socio
     */
    public List<RutinaGeneracionResponseDTO> obtenerRutinasSocio(Long idSocio, String userRol,
            Long userIdAutenticado, String userEmail) {

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!idSocio.equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Solo puede ver sus propias rutinas. " +
                                "Tu ID en usuario_perfil: %d, ID solicitado: %d",
                                socio.getIdUsuario(), idSocio));
            }
        }

        List<RutinaIA> rutinas = rutinaRepository.findBySocio_IdUsuarioOrderByFechaGeneracionDesc(idSocio);

        if (rutinas.isEmpty()) {
            throw new RuntimeException("El socio no tiene rutinas generadas");
        }

        return rutinas.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 
     * Ajusta un detalle específico de una rutina
     * 
     * @param idRutina          ID de la rutina a ajustar
     * @param request           DTO con los datos a modificar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Mapa con el resultado del ajuste
     */
    @Transactional
    public Map<String, Object> ajustarRutina(Long idRutina, RutinaAjusteRequestDTO request,
            String userRol, Long userIdAutenticado, String userEmail) {

        log.info("Ajustando rutina ID: {}, detalle ID: {}", idRutina, request.getIdDetalle());

        if (!EnumRol.entrenador.name().equals(userRol) &&
                !EnumRol.administrador.name().equals(userRol) &&
                !EnumRol.recepcionista.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo entrenadores, administradores o recepcionista pueden ajustar rutinas");
        }

        RutinaIA rutina = rutinaRepository.findById(idRutina)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada con ID: " + idRutina));

        DetalleRutina detalle = detalleRutinaRepository.findById(request.getIdDetalle())
                .orElseThrow(() -> new RuntimeException("Detalle no encontrado con ID: " + request.getIdDetalle()));

        if (!detalle.getRutinaIa().getIdRutinaIa().equals(idRutina)) {
            throw new RuntimeException("El detalle no pertenece a esta rutina");
        }

        String nombreModificador = "Sistema";
        try {
            if (userEmail != null && !userEmail.isEmpty()) {
                UsuarioPerfil usuario = usuarioRepository.findByEmail(userEmail).orElse(null);
                if (usuario != null) {
                    nombreModificador = usuario.getNombre() + " " + usuario.getApellido();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el nombre del usuario: {}", e.getMessage());
        }

        if (request.getSeries() != null) {
            detalle.setSeries(request.getSeries());
        }
        if (request.getRepeticionesMin() != null) {
            detalle.setRepeticionesMin(request.getRepeticionesMin());
        }
        if (request.getRepeticionesMax() != null) {
            detalle.setRepeticionesMax(request.getRepeticionesMax());
        }
        if (request.getPesoSugerido() != null) {
            detalle.setPesoSugerido(request.getPesoSugerido());
        }
        if (request.getDescansoSegundos() != null) {
            detalle.setDescansoSegundos(request.getDescansoSegundos());
        }
        if (request.getNotas() != null) {
            detalle.setNotas(request.getNotas());
        }

        detalle.setModificadoPor(nombreModificador);
        detalle.setFechaModificacion(LocalDateTime.now());

        detalleRutinaRepository.save(detalle);

        int nuevaVersion = rutina.getVersion() + 1;
        rutina.setVersion(nuevaVersion);
        rutinaRepository.save(rutina);

        guardarHistorialVersion(rutina, nombreModificador, request.getMotivo());

        log.info("Rutina ID: {} ajustada correctamente por: {}, nueva versión: {}",
                idRutina, nombreModificador, nuevaVersion);

        return Map.of(
                "success", true,
                "message", "Rutina ajustada correctamente",
                "idRutina", idRutina,
                "idDetalle", request.getIdDetalle(),
                "nuevaVersion", nuevaVersion,
                "modificadoPor", nombreModificador,
                "motivo", request.getMotivo() != null ? request.getMotivo() : "Ajuste manual");
    }

    /**
     * 
     * Obtiene el historial de versiones de una rutina
     * 
     * @param idRutina          ID de la rutina a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Lista del historial de versiones
     */
    public List<RutinaHistorialResponseDTO> obtenerHistorialRutina(Long idRutina, String userRol,
            Long userIdAutenticado, String userEmail) {

        log.info("Obteniendo historial de rutina ID: {}", idRutina);

        RutinaIA rutina = rutinaRepository.findById(idRutina)
                .orElseThrow(() -> new RuntimeException("Rutina no encontrada con ID: " + idRutina));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!rutina.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        "Acceso denegado. Solo puede ver el historial de sus propias rutinas");
            }
        }

        List<HistorialRutinaVersion> historial = historialRutinaVersionRepository
                .findByRutinaIa_IdRutinaIaOrderByVersionDesc(idRutina);

        if (historial.isEmpty()) {
            log.info("No hay historial para la rutina ID: {}, creando entrada inicial", idRutina);

            try {
                RutinaGeneracionResponseDTO dto = convertirAResponseDTO(rutina);
                String rutinaJson = objectMapper.writeValueAsString(dto);

                HistorialRutinaVersion versionInicial = new HistorialRutinaVersion();
                versionInicial.setRutinaIa(rutina);
                versionInicial.setVersion(1);
                versionInicial.setDatosJson(rutinaJson);
                versionInicial.setMotivo("Generación inicial");
                versionInicial.setFechaModificacion(rutina.getFechaGeneracion());

                historialRutinaVersionRepository.save(versionInicial);
                historial = List.of(versionInicial);
            } catch (JsonProcessingException e) {
                log.error("Error al serializar JSON para historial inicial: {}", e.getMessage());
                HistorialRutinaVersion versionInicial = new HistorialRutinaVersion();
                versionInicial.setRutinaIa(rutina);
                versionInicial.setVersion(1);
                versionInicial.setDatosJson(rutina.getRutinaGenerada());
                versionInicial.setMotivo("Generación inicial (fallback)");
                versionInicial.setFechaModificacion(rutina.getFechaGeneracion());
                historialRutinaVersionRepository.save(versionInicial);
                historial = List.of(versionInicial);
            }
        }

        return historial.stream()
                .map(this::convertirHistorialAResponseDTO)
                .collect(Collectors.toList());
    }

}