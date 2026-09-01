package com.pulse_gym.ms_users.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse_gym.lb_common.client.AiClient;
import com.pulse_gym.lb_common.dto.PlanNutricionalAjusteRequestDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalGeneracionRequestDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalGeneracionResponseDTO;
import com.pulse_gym.lb_common.dto.PlanNutricionalHistorialResponseDTO;
import com.pulse_gym.lb_common.dto.SugerenciaComidaDTO;
import com.pulse_gym.lb_common.entity.user.EntrenadorSocio;
import com.pulse_gym.lb_common.entity.user.HistorialPlanNutricionalVersion;
import com.pulse_gym.lb_common.entity.user.PlanNutricionalIA;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.HistorialPlanNutricionalVersionRepository;
import com.pulse_gym.ms_users.repository.PlanNutricionalRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanNutricionalService {

    /** Repositorio de planes nutricionales */
    private final PlanNutricionalRepository planNutricionalRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Servicio de generación de planes nutricionales con IA */
    private final PlanNutricionalIAService planNutricionalIAService;

    /** Cliente Feign para consumir el servicio de IA */
    private final AiClient aiClient;

    /** Mapper para convertir objetos a JSON */
    private final ObjectMapper objectMapper;

    /** Servicio de gestión de entrenadores */
    private final EntrenadorService entrenadorService;

    /** Repositorio de relaciones entre entrenadores y socios */
    private final EntrenadorSocioRepository entrenadorSocioRepository;

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de historial de versiones de planes nutricionales */
    private final HistorialPlanNutricionalVersionRepository historialPlanNutricionalVersionRepository;

    /**
     * 
     * Genera un plan nutricional usando IA
     * 
     * @param request           Preferencias para la generación del plan
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return DTO con el plan nutricional generado
     */
    @Transactional
    public PlanNutricionalGeneracionResponseDTO generarPlanNutricional(
            PlanNutricionalGeneracionRequestDTO request,
            String userRol,
            Long userIdAutenticado,
            String userEmail) {

        log.info("Generando plan nutricional para socio ID: {}", request.getIdSocio());

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

        planNutricionalIAService.validarRolGeneracion(userRol, request.getIdSocio(), userIdAutenticado, userEmail);
        planNutricionalIAService.validarMembresiaActiva(request.getIdSocio());

        Map<String, Object> contexto = planNutricionalIAService.construirContextoIA(request.getIdSocio(), request);

        PlanNutricionalGeneracionResponseDTO respuestaIA = null;
        try {
            String respuestaJson = aiClient.generarPlanNutricionalConContexto(contexto);

            log.info("JSON recibido de Python (primeros 300 chars): {}",
                    respuestaJson.length() > 300 ? respuestaJson.substring(0, 300) + "..." : respuestaJson);

            respuestaIA = objectMapper.readValue(respuestaJson, PlanNutricionalGeneracionResponseDTO.class);

            log.info("Plan nutricional generado correctamente: {} calorías diarias",
                    respuestaIA.getCaloriasDiarias());
        } catch (Exception e) {
            log.error("Error al llamar al servicio de IA: {}", e.getMessage());
            throw new RuntimeException("Error al generar plan nutricional con IA: " + e.getMessage());
        }

        PlanNutricionalIA plan = guardarPlan(socio, respuestaIA, request);

        respuestaIA.setIdPlanNutricional(plan.getIdPlanNutricional());
        respuestaIA.setVersion(plan.getVersion());
        respuestaIA.setFechaGeneracion(plan.getFechaGeneracion());
        respuestaIA.setGeneradoPorIA(true);

        log.info("Plan nutricional generado exitosamente con ID: {}, para socio: {}",
                plan.getIdPlanNutricional(), socio.getNombre());

        return respuestaIA;
    }

    /**
     * Asigna un socio a un entrenador si no existe una relación activa entre ellos.
     * 
     * @param socio      El socio a asignar
     * @param entrenador El entrenador al que se asignará el socio
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
     * Guarda el plan nutricional generado en la base de datos.
     * 
     * @param socio       El socio al que pertenece el plan
     * @param respuestaIA El DTO con los datos generados por la IA
     * @param request     La solicitud de generación del plan nutricional
     * @return El plan nutricional guardado
     */
    private PlanNutricionalIA guardarPlan(UsuarioPerfil socio,
            PlanNutricionalGeneracionResponseDTO respuestaIA,
            PlanNutricionalGeneracionRequestDTO request) {

        PlanNutricionalIA plan = new PlanNutricionalIA();
        plan.setSocio(socio);
        plan.setCaloriasDiarias(respuestaIA.getCaloriasDiarias());
        plan.setProteinasG(respuestaIA.getProteinasG());
        plan.setCarbohidratosG(respuestaIA.getCarbohidratosG());
        plan.setGrasasG(respuestaIA.getGrasasG());
        plan.setExplicacionIA(respuestaIA.getExplicacionIA());

        if (request.getIdRutina() != null) {
            rutinaRepository.findById(request.getIdRutina()).ifPresent(rutina -> {
                if (rutina.getEntrenador() != null) {
                    asignarSocioAEntrenadorSiNoExiste(socio, rutina.getEntrenador());
                    log.info("Entrenador de la rutina asignado al plan nutricional: {}",
                            rutina.getEntrenador().getEmail());
                }
            });
        } else {
            UsuarioPerfil entrenador = entrenadorService.buscarEntrenadorDisponible();
            if (entrenador != null) {
                asignarSocioAEntrenadorSiNoExiste(socio, entrenador);
                log.info("Entrenador disponible asignado al plan nutricional: {} {} (ID: {})",
                        entrenador.getNombre(), entrenador.getApellido(), entrenador.getIdUsuario());
            } else {
                log.warn("No se encontró entrenador disponible para asignar al plan nutricional del socio ID: {}",
                        socio.getIdUsuario());
            }
        }

        List<String> restricciones = respuestaIA.getRestriccionesDieteticas();
        if (restricciones == null || restricciones.isEmpty()) {
            restricciones = request.getRestriccionesDieteticas();
            respuestaIA.setRestriccionesDieteticas(restricciones);
        }

        if (restricciones != null && !restricciones.isEmpty()) {
            plan.setRestriccionesDieteticas(String.join(", ", restricciones));
        } else {
            plan.setRestriccionesDieteticas("Sin restricciones dietéticas");
        }

        try {
            if (respuestaIA.getSugerenciasComidas() != null) {
                String sugerenciasJson = objectMapper.writeValueAsString(respuestaIA.getSugerenciasComidas());
                plan.setSugerenciasComidas(sugerenciasJson);
            }
        } catch (JsonProcessingException e) {
            log.warn("Error al serializar sugerencias de comidas: {}", e.getMessage());
        }

        try {
            String planJson = objectMapper.writeValueAsString(respuestaIA);
            plan.setPlanGenerado(planJson);
        } catch (JsonProcessingException e) {
            log.warn("Error al serializar plan nutricional: {}", e.getMessage());
            plan.setPlanGenerado(respuestaIA.toString());
        }

        plan.setModeloIa("groq/compound");
        plan.setVersion(1);
        plan.setActivo(true);

        List<PlanNutricionalIA> planesAnteriores = planNutricionalRepository
                .findBySocio_IdUsuarioAndActivoTrueOrderByFechaGeneracionDesc(socio.getIdUsuario());
        for (PlanNutricionalIA p : planesAnteriores) {
            p.setActivo(false);
        }
        if (!planesAnteriores.isEmpty()) {
            planNutricionalRepository.saveAll(planesAnteriores);
        }

        guardarHistorialVersionInicial(plan);

        return planNutricionalRepository.save(plan);
    }

    /**
     * 
     * Obtiene el plan nutricional activo de un socio
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return DTO del plan nutricional activo
     */
    public PlanNutricionalGeneracionResponseDTO obtenerPlanActivo(Long idSocio, String userRol,
            Long userIdAutenticado, String userEmail) {

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!idSocio.equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Solo puede ver su propio plan. " +
                                "Tu ID en usuario_perfil: %d, ID solicitado: %d",
                                socio.getIdUsuario(), idSocio));
            }
        }

        PlanNutricionalIA plan = planNutricionalRepository.findBySocio_IdUsuarioAndActivoTrue(idSocio)
                .orElseThrow(() -> new RuntimeException("El socio no tiene un plan nutricional activo"));

        return convertirAResponseDTO(plan);
    }

    /**
     * Convierte una entidad PlanNutricionalIA a
     * PlanNutricionalGeneracionResponseDTO
     * 
     * @param plan Entidad a convertir
     * @return DTO del plan nutricional
     */
    private PlanNutricionalGeneracionResponseDTO convertirAResponseDTO(PlanNutricionalIA plan) {
        PlanNutricionalGeneracionResponseDTO dto = new PlanNutricionalGeneracionResponseDTO();
        dto.setIdPlanNutricional(plan.getIdPlanNutricional());
        dto.setCaloriasDiarias(plan.getCaloriasDiarias());
        dto.setProteinasG(plan.getProteinasG());
        dto.setCarbohidratosG(plan.getCarbohidratosG());
        dto.setGrasasG(plan.getGrasasG());
        dto.setVersion(plan.getVersion());
        dto.setGeneradoPorIA(plan.getModeloIa() != null);
        dto.setFechaGeneracion(plan.getFechaGeneracion());
        dto.setExplicacionIA(plan.getExplicacionIA());

        dto.setModificadoPor(plan.getModificadoPor());
        dto.setFechaModificacion(plan.getFechaModificacion());
        dto.setMotivoModificacion(plan.getMotivoModificacion());

        if (plan.getRestriccionesDieteticas() != null && !plan.getRestriccionesDieteticas().isEmpty()) {
            dto.setRestriccionesDieteticas(List.of(plan.getRestriccionesDieteticas().split(", ")));
        }

        if (plan.getSugerenciasComidas() != null) {
            try {
                Map<String, List<SugerenciaComidaDTO>> sugerencias = objectMapper.readValue(
                        plan.getSugerenciasComidas(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<SugerenciaComidaDTO>>>() {
                        });
                dto.setSugerenciasComidas(sugerencias);
            } catch (Exception e) {
                log.warn("Error al parsear sugerencias de comidas: {}", e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Obtiene todos los planes nutricionales del usuario autenticado
     * 
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Lista de planes nutricionales del socio
     */
    public List<PlanNutricionalGeneracionResponseDTO> obtenerMisPlanes(
            String userRol, String userEmail) {

        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException("Solo los socios pueden acceder a sus planes nutricionales");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        return obtenerPlanesSocio(socio.getIdUsuario(), userRol, userEmail);
    }

    /**
     * Obtiene todos los planes nutricionales de un socio específico
     * 
     * @param idSocio   ID del socio a consultar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Lista de planes nutricionales del socio
     */
    public List<PlanNutricionalGeneracionResponseDTO> obtenerPlanesSocio(
            Long idSocio, String userRol, String userEmail) {

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));
            if (!idSocio.equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Solo puede ver sus propios planes. " +
                                "Tu ID en usuario_perfil: %d, ID solicitado: %d",
                                socio.getIdUsuario(), idSocio));
            }
        } else if (!EnumRol.entrenador.name().equals(userRol) && !EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "No tiene permisos para ver los planes nutricionales de este socio");
        }

        List<PlanNutricionalIA> planes = planNutricionalRepository
                .findBySocio_IdUsuarioOrderByFechaGeneracionDesc(idSocio);

        if (planes.isEmpty()) {
            throw new RuntimeException("El socio no tiene planes nutricionales generados");
        }

        return planes.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un plan nutricional específico por su ID
     * 
     * @param idPlan    ID del plan nutricional a consultar
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return DTO del plan nutricional
     */
    public PlanNutricionalGeneracionResponseDTO obtenerPlanNutricional(
            Long idPlan, String userRol, String userEmail) {

        PlanNutricionalIA plan = planNutricionalRepository.findById(idPlan)
                .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado con ID: " + idPlan));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));
            if (!plan.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Solo puede ver sus propios planes. " +
                                "Tu ID en usuario_perfil: %d, ID de la rutina: %d",
                                socio.getIdUsuario(), plan.getSocio().getIdUsuario()));
            }
        } else if (!EnumRol.entrenador.name().equals(userRol) && !EnumRol.administrador.name().equals(userRol)) {
            throw new SecurityAuthorizationException("No tiene permisos para ver este plan nutricional");
        }

        return convertirAResponseDTO(plan);
    }

    /**
     * Ajusta un plan nutricional existente
     * 
     * @param idPlan    ID del plan nutricional a ajustar
     * @param request   Datos de ajuste del plan
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Mapa con información sobre el ajuste realizado
     */
    @Transactional
    public Map<String, Object> ajustarPlanNutricional(Long idPlan,
            PlanNutricionalAjusteRequestDTO request,
            String userRol, Long userIdAutenticado, String userEmail) {

        log.info("Ajustando plan nutricional ID: {}", idPlan);

        if (!EnumRol.entrenador.name().equals(userRol) &&
                !EnumRol.administrador.name().equals(userRol) &&
                !EnumRol.recepcionista.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo entrenadores, administradores o recepcionistas pueden ajustar planes nutricionales");
        }

        PlanNutricionalIA plan = planNutricionalRepository.findById(idPlan)
                .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado con ID: " + idPlan));

        String nombreModificador = userEmail;
        try {
            if (userEmail != null) {
                UsuarioPerfil usuario = usuarioRepository.findByEmail(userEmail).orElse(null);
                if (usuario != null) {
                    nombreModificador = usuario.getNombre() + " " + usuario.getApellido();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el nombre del usuario: {}", e.getMessage());
        }

        boolean huboCambios = false;

        if (request.getCaloriasDiarias() != null) {
            plan.setCaloriasDiarias(request.getCaloriasDiarias());
            huboCambios = true;
        }
        if (request.getProteinasG() != null) {
            plan.setProteinasG(request.getProteinasG());
            huboCambios = true;
        }
        if (request.getCarbohidratosG() != null) {
            plan.setCarbohidratosG(request.getCarbohidratosG());
            huboCambios = true;
        }
        if (request.getGrasasG() != null) {
            plan.setGrasasG(request.getGrasasG());
            huboCambios = true;
        }
        if (request.getExplicacionIA() != null) {
            plan.setExplicacionIA(request.getExplicacionIA());
            huboCambios = true;
        }
        if (request.getRestriccionesDieteticas() != null) {
            if (request.getRestriccionesDieteticas().isEmpty()) {
                plan.setRestriccionesDieteticas("Sin restricciones dietéticas");
            } else {
                plan.setRestriccionesDieteticas(String.join(", ", request.getRestriccionesDieteticas()));
            }
            huboCambios = true;
        }

        if (request.getSugerenciasComidas() != null && !request.getSugerenciasComidas().isEmpty()) {
            try {
                String sugerenciasJson = objectMapper.writeValueAsString(request.getSugerenciasComidas());
                plan.setSugerenciasComidas(sugerenciasJson);
                huboCambios = true;
            } catch (JsonProcessingException e) {
                log.error("Error al serializar sugerencias de comidas: {}", e.getMessage());
                throw new RuntimeException("Error al guardar las sugerencias de comidas: " + e.getMessage());
            }
        }

        if (!huboCambios) {
            throw new RuntimeException("No se especificaron cambios para realizar");
        }

        plan.setVersion(plan.getVersion() + 1);
        plan.setActivo(true);

        plan.setModificadoPor(nombreModificador);
        plan.setFechaModificacion(LocalDateTime.now());
        plan.setMotivoModificacion(request.getMotivo());

        try {
            PlanNutricionalGeneracionResponseDTO dto = convertirAResponseDTO(plan);
            if (request.getExplicacionIA() != null) {
                dto.setExplicacionIA(request.getExplicacionIA());
            }
            if (request.getRestriccionesDieteticas() != null) {
                dto.setRestriccionesDieteticas(request.getRestriccionesDieteticas());
            }
            dto.setModificadoPor(nombreModificador);
            dto.setFechaModificacion(LocalDateTime.now());
            dto.setMotivoModificacion(request.getMotivo());

            String planJson = objectMapper.writeValueAsString(dto);
            plan.setPlanGenerado(planJson);
        } catch (JsonProcessingException e) {
            log.warn("Error al actualizar planGenerado: {}", e.getMessage());
        }

        plan = planNutricionalRepository.save(plan);

        log.info("Plan nutricional ID: {} ajustado correctamente por: {}, nueva versión: {}",
                idPlan, nombreModificador, plan.getVersion());

        guardarHistorialVersion(plan, nombreModificador, request.getMotivo());
        return Map.of(
                "success", true,
                "message", "Plan nutricional ajustado correctamente",
                "idPlan", idPlan,
                "nuevaVersion", plan.getVersion(),
                "modificadoPor", nombreModificador,
                "fechaModificacion", plan.getFechaModificacion().toString(),
                "motivo", request.getMotivo());

    }

    /**
     * Guarda la versión inicial del historial de un plan nutricional
     * 
     * @param plan El plan nutricional para el cual se guarda el historial
     */
    private void guardarHistorialVersionInicial(PlanNutricionalIA plan) {
        try {
            PlanNutricionalGeneracionResponseDTO dto = convertirAResponseDTO(plan);
            String planJson = objectMapper.writeValueAsString(dto);

            HistorialPlanNutricionalVersion historial = new HistorialPlanNutricionalVersion();
            historial.setPlanNutricional(plan);
            historial.setVersion(1);
            historial.setDatosJson(planJson);
            historial.setMotivo("Generación inicial");
            historial.setFechaModificacion(plan.getFechaGeneracion());
            historialPlanNutricionalVersionRepository.save(historial);

            log.info("Historial inicial guardado para plan nutricional ID: {}", plan.getIdPlanNutricional());
        } catch (JsonProcessingException e) {
            log.error("Error al serializar JSON para historial: {}", e.getMessage());
            try {
                HistorialPlanNutricionalVersion historial = new HistorialPlanNutricionalVersion();
                historial.setPlanNutricional(plan);
                historial.setVersion(1);
                historial.setDatosJson(plan.getPlanGenerado());
                historial.setMotivo("Generación inicial (fallback)");
                historial.setFechaModificacion(plan.getFechaGeneracion());
                historialPlanNutricionalVersionRepository.save(historial);
                log.info("Historial inicial guardado con fallback para plan ID: {}", plan.getIdPlanNutricional());
            } catch (Exception ex) {
                log.error("Error al guardar historial inicial con fallback: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al guardar historial inicial: {}", e.getMessage());
        }
    }

    /**
     * Guarda una nueva versión del historial de un plan nutricional
     * 
     * @param plan          El plan nutricional para el cual se guarda el historial
     * @param modificadoPor Nombre del usuario que realizó la modificación
     * @param motivo        Motivo de la modificación
     */
    private void guardarHistorialVersion(PlanNutricionalIA plan, String modificadoPor, String motivo) {
        try {
            PlanNutricionalGeneracionResponseDTO dto = convertirAResponseDTO(plan);
            String planJson = objectMapper.writeValueAsString(dto);

            HistorialPlanNutricionalVersion historial = new HistorialPlanNutricionalVersion();
            historial.setPlanNutricional(plan);
            historial.setVersion(plan.getVersion());
            historial.setDatosJson(planJson);
            historial.setMotivo(motivo != null ? motivo : "Ajuste manual - Versión " + plan.getVersion());

            if (modificadoPor != null && !modificadoPor.isEmpty()) {
                String email = modificadoPor.contains("(")
                        ? modificadoPor.substring(modificadoPor.indexOf("(") + 1, modificadoPor.indexOf(")"))
                        : modificadoPor;
                usuarioRepository.findByEmail(email).ifPresent(historial::setModificadoPor);
                historial.setModificadoPorNombre(modificadoPor);
            }

            historial.setFechaModificacion(LocalDateTime.now());
            historialPlanNutricionalVersionRepository.save(historial);

            log.info("Historial guardado para plan ID: {}, versión: {}",
                    plan.getIdPlanNutricional(), plan.getVersion());
        } catch (JsonProcessingException e) {
            log.error("Error al serializar JSON para historial: {}", e.getMessage());
            try {
                HistorialPlanNutricionalVersion historial = new HistorialPlanNutricionalVersion();
                historial.setPlanNutricional(plan);
                historial.setVersion(plan.getVersion());
                historial.setDatosJson(plan.getPlanGenerado());
                historial.setMotivo(
                        motivo != null ? motivo : "Ajuste manual - Versión " + plan.getVersion() + " (fallback)");

                if (modificadoPor != null && !modificadoPor.isEmpty()) {
                    String email = modificadoPor.contains("(")
                            ? modificadoPor.substring(modificadoPor.indexOf("(") + 1, modificadoPor.indexOf(")"))
                            : modificadoPor;
                    usuarioRepository.findByEmail(email).ifPresent(historial::setModificadoPor);
                    historial.setModificadoPorNombre(modificadoPor);
                }

                historial.setFechaModificacion(LocalDateTime.now());
                historialPlanNutricionalVersionRepository.save(historial);
                log.info("Historial guardado con fallback para plan ID: {}, versión: {}",
                        plan.getIdPlanNutricional(), plan.getVersion());
            } catch (Exception ex) {
                log.error("Error al guardar historial con fallback: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Error al guardar historial: {}", e.getMessage());
        }
    }

    /**
     * Convierte un objeto HistorialPlanNutricionalVersion a
     * PlanNutricionalHistorialResponseDTO
     * 
     * @param historial El historial a convertir
     * @return DTO correspondiente al historial
     */
    private PlanNutricionalHistorialResponseDTO convertirHistorialAResponseDTO(
            HistorialPlanNutricionalVersion historial) {
        PlanNutricionalHistorialResponseDTO dto = new PlanNutricionalHistorialResponseDTO();
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

        if (historial.getModificadoPor() != null) {
            dto.setModificadoPor(historial.getModificadoPor().getEmail());
        } else if (historial.getModificadoPorNombre() != null) {
            dto.setModificadoPor(historial.getModificadoPorNombre());
        } else {
            dto.setModificadoPor("Sistema");
        }

        return dto;
    }

    /**
     * Obtiene el historial de versiones de un plan nutricional específico
     * 
     * @param idPlan            ID del plan nutricional
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Lista de DTOs con el historial de versiones del plan nutricional
     */
    public List<PlanNutricionalHistorialResponseDTO> obtenerHistorialPlan(Long idPlan, String userRol,
            Long userIdAutenticado, String userEmail) {

        log.info("Obteniendo historial de plan nutricional ID: {}", idPlan);

        PlanNutricionalIA plan = planNutricionalRepository.findById(idPlan)
                .orElseThrow(() -> new RuntimeException("Plan nutricional no encontrado con ID: " + idPlan));

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));
            if (!plan.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
                throw new SecurityAuthorizationException(
                        "Acceso denegado. Solo puede ver el historial de sus propios planes");
            }
        }

        List<HistorialPlanNutricionalVersion> historial = historialPlanNutricionalVersionRepository
                .findByPlanNutricional_IdPlanNutricionalOrderByVersionDesc(idPlan);

        if (historial.isEmpty()) {
            log.info("No hay historial para el plan ID: {}, creando entrada inicial", idPlan);

            try {
                PlanNutricionalGeneracionResponseDTO dto = convertirAResponseDTO(plan);
                String planJson = objectMapper.writeValueAsString(dto);

                HistorialPlanNutricionalVersion versionInicial = new HistorialPlanNutricionalVersion();
                versionInicial.setPlanNutricional(plan);
                versionInicial.setVersion(1);
                versionInicial.setDatosJson(planJson);
                versionInicial.setMotivo("Generación inicial");
                versionInicial.setFechaModificacion(plan.getFechaGeneracion());
                historialPlanNutricionalVersionRepository.save(versionInicial);
                historial = List.of(versionInicial);
            } catch (JsonProcessingException e) {
                log.error("Error al serializar JSON para historial inicial: {}", e.getMessage());
                HistorialPlanNutricionalVersion versionInicial = new HistorialPlanNutricionalVersion();
                versionInicial.setPlanNutricional(plan);
                versionInicial.setVersion(1);
                versionInicial.setDatosJson(plan.getPlanGenerado());
                versionInicial.setMotivo("Generación inicial (fallback)");
                versionInicial.setFechaModificacion(plan.getFechaGeneracion());
                historialPlanNutricionalVersionRepository.save(versionInicial);
                historial = List.of(versionInicial);
            }
        }

        return historial.stream()
                .map(this::convertirHistorialAResponseDTO)
                .collect(Collectors.toList());
    }
}
