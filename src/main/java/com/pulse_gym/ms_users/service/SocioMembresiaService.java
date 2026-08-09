package com.pulse_gym.ms_users.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.dto.AsignarMembresiaFlexibleRequestDTO;
import com.pulse_gym.lb_common.dto.AsignarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.MembresiaPorVencerDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.RenovarMembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.SocioMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.SocioMoraDTO;
import com.pulse_gym.lb_common.dto.SuspenderMembresiaRequestDTO;
import com.pulse_gym.lb_common.entity.user.Membresia;
import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoSocioMembresia;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.MembresiaRepository;
import com.pulse_gym.ms_users.repository.SocioMembresiaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocioMembresiaService {

    /** Repositorio para gestionar las membresías de los socios */
    private final SocioMembresiaRepository socioMembresiaRepository;

    /** Repositorio para gestionar los perfiles de usuario */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio para gestionar las membresías */
    private final MembresiaRepository membresiaRepository;

    /**
     * Calcula la fecha de vencimiento de una membresía a partir de la fecha de
     * inicio y el tipo de duración de la membresía.
     * 
     * @param fechaInicio La fecha de inicio de la membresía
     * @param membresia   La membresía para la cual se va a calcular la fecha de
     *                    vencimiento
     * @return La fecha de vencimiento calculada para la membresía del socio
     */
    private LocalDate calcularFechaVencimiento(LocalDate fechaInicio, Membresia membresia) {
        int diasTotales = membresia.getTipoDuracion().calcularDiasTotales(
                membresia.getCantidad() != null ? membresia.getCantidad() : 1);
        return fechaInicio.plusDays(diasTotales);
    }

    /**
     * Convierte una entidad SocioMembresia a un DTO de respuesta
     * SocioMembresiaResponseDTO.
     * 
     * @param sm La entidad SocioMembresia a convertir
     * @return Un objeto SocioMembresiaResponseDTO con los datos de la membresía del
     *         socio
     */
    private SocioMembresiaResponseDTO convertirAResponseDTO(SocioMembresia sm) {
        SocioMembresiaResponseDTO dto = new SocioMembresiaResponseDTO();
        dto.setIdSocioMembresia(sm.getIdSocioMembresia());
        dto.setIdSocio(sm.getSocio().getIdUsuario());
        dto.setNombreSocio(sm.getSocio().getNombre() + " " + sm.getSocio().getApellido());
        dto.setEmailSocio(sm.getSocio().getEmail());
        dto.setIdMembresia(sm.getMembresia().getIdMembresia());
        dto.setNombreMembresia(sm.getMembresia().getNombre());

        dto.setPrecioTotal(calcularPrecioReal(sm));

        dto.setCantidad(sm.getMembresia().getCantidad());
        dto.setTipoDuracion(sm.getMembresia().getTipoDuracion().name());
        dto.setDuracionDescripcion(sm.getMembresia().getDuracionDescripcion());
        dto.setIncluyeIA(sm.getMembresia().getIncluyeIA());

        dto.setEsFlexible(sm.getMembresia().getEsFlexible());
        dto.setPrecioPorDia(sm.getMembresia().getPrecioPorDia());

        if (Boolean.TRUE.equals(sm.getMembresia().getEsFlexible())) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(
                    sm.getFechaInicio(),
                    sm.getFechaVencimiento());
            dto.setCantidadDias((int) dias);
        } else {
            dto.setCantidadDias(null);
        }

        dto.setFechaInicio(sm.getFechaInicio());
        dto.setFechaVencimiento(sm.getFechaVencimiento());
        dto.setEstado(sm.getEstado().name());
        dto.setDiasRestantes(sm.getDiasRestantes());
        dto.setEstaActiva(sm.isActiva());
        dto.setEstaVencida(sm.isVencida());
        dto.setBeneficios(sm.getMembresia().getBeneficios());
        dto.setRestricciones(sm.getMembresia().getRestricciones());
        dto.setFechaCreacion(sm.getFechaCreacion());
        dto.setFechaActualizacion(sm.getFechaActualizacion());

        return dto;
    }

    /**
     * Calcula el precio real de la membresía considerando si es flexible o no.
     * 
     * @param sm La entidad SocioMembresia a evaluar
     * @return El precio total de la membresía, calculado según su tipo (fija o
     *         flexible).
     */
    private BigDecimal calcularPrecioReal(SocioMembresia sm) {
        Membresia membresia = sm.getMembresia();

        if (Boolean.TRUE.equals(membresia.getEsFlexible())) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(
                    sm.getFechaInicio(),
                    sm.getFechaVencimiento());
            if (membresia.getPrecioPorDia() != null) {
                return membresia.getPrecioPorDia().multiply(BigDecimal.valueOf(Math.max(1, dias)));
            }
        }

        return membresia.getPrecioTotal();
    }

    /**
     * Asigna una membresía a un socio.
     * 
     * @param requestDTO Datos de la asignación (idSocio, idMembresia, fechaInicio,
     *                   renovacionAutomatica, observaciones)
     * @param userRol    Rol del usuario que hace la solicitud
     * @return Mensaje con el formato: "Membresía 'X' asignada correctamente al
     *         socio Y. Vence el: Z"
     */
    @Transactional
    public MessegeGlobalDTO asignarMembresia(AsignarMembresiaRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        UsuarioPerfil socio = usuarioRepository.findById(requestDTO.getIdSocio())
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + requestDTO.getIdSocio()));

        Membresia membresia = membresiaRepository.findById(requestDTO.getIdMembresia())
                .orElseThrow(
                        () -> new RuntimeException("Membresía no encontrada con ID: " + requestDTO.getIdMembresia()));

        if (!membresia.getActivo()) {
            throw new RuntimeException("La membresía no está activa");
        }

        if (socioMembresiaRepository.existsBySocio_IdUsuarioAndEstado(requestDTO.getIdSocio(),
                EnumEstadoSocioMembresia.ACTIVA)) {
            throw new RuntimeException(
                    "El socio ya tiene una membresía activa. Debe renovar o cancelar la actual primero.");
        }

        LocalDate fechaInicio = requestDTO.getFechaInicio() != null ? requestDTO.getFechaInicio() : LocalDate.now();
        LocalDate fechaVencimiento = calcularFechaVencimiento(fechaInicio, membresia);

        SocioMembresia socioMembresia = new SocioMembresia();
        socioMembresia.setSocio(socio);
        socioMembresia.setMembresia(membresia);
        socioMembresia.setFechaInicio(fechaInicio);
        socioMembresia.setFechaVencimiento(fechaVencimiento);
        socioMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);
        socioMembresia.setObservaciones(requestDTO.getObservaciones());

        socioMembresiaRepository.save(socioMembresia);

        return new MessegeGlobalDTO(String.format(
                "Membresía '%s' asignada correctamente al socio %s. Vence el: %s",
                membresia.getNombre(), socio.getNombre(), fechaVencimiento));
    }

    /**
     * Consulta todas las membresías de un socio.
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado (socio, administrador o
     *                          recepcionista)
     * @param userIdAutenticado ID del usuario que realiza la consulta
     * @return Lista de DTOs con los datos de cada membresía del socio (incluye
     *         fechas, estado, renovación automática, etc.)
     */
    @Transactional(readOnly = true)
    public List<SocioMembresiaResponseDTO> consultarMembresiasSocio(
            Long idSocio,
            String userRol,
            String userEmail) {

        if (userRol.equals(EnumRol.socio.name())) {
            UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio autenticado no encontrado"));

            if (!socioAutenticado.getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede consultar su propia membresía");
            }
        } else if (!userRol.equals(EnumRol.administrador.name())
                && !userRol.equals(EnumRol.recepcionista.name())
                && !userRol.equals(EnumRol.entrenador.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
        }

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        List<SocioMembresia> membresias = socioMembresiaRepository
                .findBySocio_IdUsuarioOrderByFechaCreacionDesc(idSocio);

        if (membresias.isEmpty()) {
            throw new RuntimeException("El socio " + socio.getNombre() + " no tiene membresías asignadas");
        }

        return membresias.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Renueva una membresía existente de un socio. Marca la membresía actual como
     * RENOVADA y crea una nueva asignación con fechas actualizadas.
     * 
     * @param requestDTO        DTO con el idSocioMembresia de la membresía a
     *                          renovar, más opciones de renovación automática y
     *                          observaciones
     * @param userRol           Rol del usuario autenticado (socio, administrador o
     *                          recepcionista)
     * @param userIdAutenticado ID del usuario que realiza la solicitud
     * @return Mensaje de confirmación con la nueva fecha de vencimiento
     */
    @Transactional
    public MessegeGlobalDTO renovarMembresia(RenovarMembresiaRequestDTO requestDTO, String userRol,
            Long userIdAutenticado) {

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> new RuntimeException(
                        "Asignación de membresía no encontrada con ID: " + requestDTO.getIdSocioMembresia()));

        if (userRol.equals(EnumRol.socio.name())) {
            if (!userIdAutenticado.equals(socioMembresia.getSocio().getIdUsuario())) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede renovar su propia membresía");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) && !userRol.equals(EnumRol.recepcionista.name())
                && !userRol.equals(EnumRol.entrenador.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
        }

        Membresia membresia = socioMembresia.getMembresia();
        if (!membresia.getActivo()) {
            throw new RuntimeException("La membresía base ya no está activa");
        }

        socioMembresia.setEstado(EnumEstadoSocioMembresia.RENOVADA);
        socioMembresiaRepository.save(socioMembresia);

        LocalDate nuevaFechaInicio = LocalDate.now();
        LocalDate nuevaFechaVencimiento = calcularFechaVencimiento(nuevaFechaInicio, membresia);

        SocioMembresia nuevaMembresia = new SocioMembresia();
        nuevaMembresia.setSocio(socioMembresia.getSocio());
        nuevaMembresia.setMembresia(membresia);
        nuevaMembresia.setFechaInicio(nuevaFechaInicio);
        nuevaMembresia.setFechaVencimiento(nuevaFechaVencimiento);
        nuevaMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);
        nuevaMembresia.setObservaciones("Renovación de membresía anterior ID: " + socioMembresia.getIdSocioMembresia() +
                (requestDTO.getObservaciones() != null ? " - " + requestDTO.getObservaciones() : ""));

        socioMembresiaRepository.save(nuevaMembresia);

        return new MessegeGlobalDTO(String.format(
                "Membresía renovada correctamente. Nueva fecha de vencimiento: %s",
                nuevaFechaVencimiento));
    }

    /**
     * Cancela una membresía activa de un socio. Solo puede ser realizada por un
     * recepcionista.
     * 
     * @param idSocioMembresia ID de la asignación de membresía a cancelar
     * @param motivo           Motivo de la cancelación
     * @param userRol          Rol del usuario autenticado (debe ser recepcionista)
     * @return Mensaje de confirmación indicando que la membresía fue cancelada con
     *         el motivo especificado
     */
    @Transactional
    public MessegeGlobalDTO cancelarMembresia(Long idSocioMembresia, String motivo, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(idSocioMembresia)
                .orElseThrow(() -> new RuntimeException(
                        "Asignación de membresía no encontrada con ID: " + idSocioMembresia));

        if (socioMembresia.getEstado() == EnumEstadoSocioMembresia.CANCELADA) {
            throw new RuntimeException("La membresía ya está cancelada");
        }

        socioMembresia.setEstado(EnumEstadoSocioMembresia.CANCELADA);
        socioMembresia.setObservaciones("Cancelada: " + motivo +
                (socioMembresia.getObservaciones() != null ? " - " + socioMembresia.getObservaciones() : ""));

        socioMembresiaRepository.save(socioMembresia);

        return new MessegeGlobalDTO("Membresía cancelada correctamente. Motivo: " + motivo);
    }

    /**
     * Suspende una membresía activa de un socio. Solo puede ser realizada por un
     * recepcionista.
     * 
     * @param requestDTO DTO con el idSocioMembresia y el motivo de la suspensión
     * @param userRol    Rol del usuario autenticado (debe ser recepcionista)
     * @return Mensaje de confirmación indicando que la membresía fue suspendida con
     *         el motivo especificado
     */
    @Transactional
    public MessegeGlobalDTO suspenderMembresia(SuspenderMembresiaRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> new RuntimeException(
                        "Asignación de membresía no encontrada con ID: " + requestDTO.getIdSocioMembresia()));

        if (socioMembresia.getEstado() != EnumEstadoSocioMembresia.ACTIVA) {
            throw new RuntimeException("Solo se pueden suspender membresías activas");
        }

        socioMembresia.setEstado(EnumEstadoSocioMembresia.SUSPENDIDA);
        socioMembresia.setObservaciones("Suspendida: " + requestDTO.getMotivo() +
                (socioMembresia.getObservaciones() != null ? " - " + socioMembresia.getObservaciones() : ""));

        socioMembresiaRepository.save(socioMembresia);

        return new MessegeGlobalDTO("Membresía suspendida correctamente. Motivo: " + requestDTO.getMotivo());
    }

    /**
     * Tarea programada que se ejecuta diariamente a medianoche para actualizar las
     * membresías vencidas.
     * Cambia el estado de las membresías activas vencidas a VENCIDA y, si tienen
     * renovación automática activada,
     * crea automáticamente una nueva membresía renovada con fechas actualizadas.
     * 
     * @return void (no retorna valor)
     */
    @Scheduled(cron = "0 0 * * * ?") // Cada hora en punto
    @Transactional
    public void actualizarMembresiasVencidas() {
        log.info("Ejecutando tarea programada: actualizar membresías vencidas");

        try {
            List<SocioMembresia> vencidas = socioMembresiaRepository
                    .findByEstadoAndFechaVencimientoBefore(
                            EnumEstadoSocioMembresia.ACTIVA,
                            LocalDate.now());

            if (vencidas.isEmpty()) {
                log.info("No hay membresías vencidas para actualizar");
                return;
            }

            // Actualizar cada membresía
            for (SocioMembresia sm : vencidas) {
                String observacionesAnteriores = sm.getObservaciones();
                String nuevaObservacion = String.format(
                        "Vencimiento automático el %s (días restantes: 0)%s",
                        LocalDate.now(),
                        observacionesAnteriores != null ? " - " + observacionesAnteriores : "");

                sm.setEstado(EnumEstadoSocioMembresia.VENCIDA);
                sm.setObservaciones(nuevaObservacion);
                log.debug("Membresía ID: {} marcada como VENCIDA", sm.getIdSocioMembresia());
            }

            socioMembresiaRepository.saveAll(vencidas);
            log.info("Membresías vencidas actualizadas exitosamente: {}", vencidas.size());

        } catch (Exception e) {
            log.error("Error al actualizar membresías vencidas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar membresías vencidas", e);
        }
    }

    /**
     * Consulta el estado de la membresía de un socio para control biométrico
     * 
     * @param idSocio ID del socio a consultar
     * @return DTO con el estado de la membresía
     */
    @Transactional(readOnly = true)
    public EstadoMembresiaResponseDTO consultarEstadoMembresiaBiometrico(Long idSocio) {

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        SocioMembresia membresiaActiva = socioMembresiaRepository.findMembresiaActivaBySocio(idSocio)
                .orElse(null);

        EstadoMembresiaResponseDTO.EstadoMembresiaResponseDTOBuilder builder = EstadoMembresiaResponseDTO.builder()
                .idSocio(socio.getIdUsuario())
                .nombreSocio(socio.getNombre() + " " + socio.getApellido())
                .emailSocio(socio.getEmail());

        if (membresiaActiva == null) {
            return builder
                    .estado("SIN_MEMBRESIA")
                    .activa(false)
                    .vencida(false)
                    .diasRestantes(0L)
                    .mensaje("El socio no tiene una membresía activa")
                    .build();
        }

        if (membresiaActiva.getEstado() == EnumEstadoSocioMembresia.SUSPENDIDA) {
            return builder
                    .idSocioMembresia(membresiaActiva.getIdSocioMembresia())
                    .idMembresia(membresiaActiva.getMembresia().getIdMembresia())
                    .nombreMembresia(membresiaActiva.getMembresia().getNombre())
                    .fechaInicio(membresiaActiva.getFechaInicio())
                    .fechaVencimiento(membresiaActiva.getFechaVencimiento())
                    .estado("SUSPENDIDA")
                    .activa(false)
                    .vencida(false)
                    .diasRestantes(membresiaActiva.getDiasRestantes())
                    .mensaje("La membresía está suspendida")
                    .build();
        }

        if (membresiaActiva.isVencida()) {
            return builder
                    .idSocioMembresia(membresiaActiva.getIdSocioMembresia())
                    .idMembresia(membresiaActiva.getMembresia().getIdMembresia())
                    .nombreMembresia(membresiaActiva.getMembresia().getNombre())
                    .fechaInicio(membresiaActiva.getFechaInicio())
                    .fechaVencimiento(membresiaActiva.getFechaVencimiento())
                    .estado("VENCIDA")
                    .activa(false)
                    .vencida(true)
                    .diasRestantes(0L)
                    .mensaje("La membresía está vencida")
                    .build();
        }

        return builder
                .idSocioMembresia(membresiaActiva.getIdSocioMembresia())
                .idMembresia(membresiaActiva.getMembresia().getIdMembresia())
                .nombreMembresia(membresiaActiva.getMembresia().getNombre())
                .fechaInicio(membresiaActiva.getFechaInicio())
                .fechaVencimiento(membresiaActiva.getFechaVencimiento())
                .estado("ACTIVA")
                .activa(true)
                .vencida(false)
                .diasRestantes(membresiaActiva.getDiasRestantes())
                .mensaje("Membresía activa - Acceso permitido")
                .build();
    }

    /**
     * Consulta el estado de la membresía de un socio desde la aplicación móvil
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return DTO con el estado de la membresía
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     */
    @Transactional(readOnly = true)
    public EstadoMembresiaResponseDTO consultarEstadoMembresiaApp(Long idSocio, String userRol,
            Long userIdAutenticado, String userEmail) {

        if (userRol.equals(EnumRol.socio.name())) {
            UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio autenticado no encontrado"));

            if (!socioAutenticado.getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede consultar su propio estado");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) && !userRol.equals(EnumRol.recepcionista.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
        }

        return consultarEstadoMembresiaBiometrico(idSocio);
    }

    /**
     * Actualiza el estado y fecha de vencimiento de una membresía después de un
     * pago
     * 
     * @param idSocioMembresia ID de la relación socio-membresía
     * @return Mensaje con el resultado de la operación
     * @throws RuntimeException Si la membresía asignada no existe
     */
    @Transactional
    public MessegeGlobalDTO actualizarEstadoMembresiaPorPago(Long idSocioMembresia) {
        SocioMembresia socioMembresia = socioMembresiaRepository.findById(idSocioMembresia)
                .orElseThrow(() -> new RuntimeException("Membresía asignada no encontrada"));

        if (socioMembresia.getEstado() == EnumEstadoSocioMembresia.VENCIDA ||
                socioMembresia.getEstado() == EnumEstadoSocioMembresia.SUSPENDIDA) {

            socioMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);

            Membresia membresia = socioMembresia.getMembresia();
            int diasTotales = membresia.getTipoDuracion().calcularDiasTotales(
                    membresia.getCantidad() != null ? membresia.getCantidad() : 1);
            LocalDate nuevaFechaVencimiento = LocalDate.now().plusDays(diasTotales);
            socioMembresia.setFechaVencimiento(nuevaFechaVencimiento);

            socioMembresiaRepository.save(socioMembresia);

            return new MessegeGlobalDTO(String.format(
                    "Membresía reactivada correctamente. Nueva fecha de vencimiento: %s",
                    nuevaFechaVencimiento));
        }

        if (socioMembresia.getEstado() == EnumEstadoSocioMembresia.ACTIVA) {
            Membresia membresia = socioMembresia.getMembresia();
            int diasTotales = membresia.getTipoDuracion().calcularDiasTotales(
                    membresia.getCantidad() != null ? membresia.getCantidad() : 1);

            LocalDate fechaBase = socioMembresia.getFechaVencimiento().isAfter(LocalDate.now())
                    ? socioMembresia.getFechaVencimiento()
                    : LocalDate.now();
            LocalDate nuevaFechaVencimiento = fechaBase.plusDays(diasTotales);
            socioMembresia.setFechaVencimiento(nuevaFechaVencimiento);

            socioMembresiaRepository.save(socioMembresia);

            return new MessegeGlobalDTO(String.format(
                    "Membresía renovada correctamente. Nueva fecha de vencimiento: %s",
                    nuevaFechaVencimiento));
        }

        return new MessegeGlobalDTO("No se requirió actualización del estado de la membresía");
    }

    public List<SocioMoraDTO> obtenerSociosEnMora(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            fechaInicio = LocalDate.of(1900, 1, 1);
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }

        List<SocioMembresia> morosos = socioMembresiaRepository.findMorosos(fechaInicio, fechaFin);
        if (morosos.isEmpty()) {
            return Collections.emptyList();
        }
        return morosos.stream().map(this::convertirAMoraDTO).collect(Collectors.toList());
    }

    private SocioMoraDTO convertirAMoraDTO(SocioMembresia sm) {
        UsuarioPerfil socio = sm.getSocio();
        SocioMoraDTO dto = new SocioMoraDTO();
        dto.setIdSocio(socio.getIdUsuario());
        dto.setNombreCompleto(socio.getNombre() + " " + socio.getApellido());
        dto.setIdentificacion(socio.getDocumentoIdentidad());
        dto.setTelefono(socio.getTelefono());
        dto.setEmail(socio.getEmail());
        dto.setTipoMembresia(sm.getMembresia().getNombre());
        dto.setEstadoMembresia(sm.getEstado().name());
        dto.setFechaVencimiento(sm.getFechaVencimiento().toString());
        return dto;
    }

    /**
     * Asigna una membresía flexible a un socio (días personalizados)
     * 
     * @param requestDTO Datos de la asignación
     * @param userRol    Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO asignarMembresiaFlexible(AsignarMembresiaFlexibleRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        UsuarioPerfil socio = usuarioRepository.findById(requestDTO.getIdSocio())
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + requestDTO.getIdSocio()));

        Membresia membresia = membresiaRepository.findById(requestDTO.getIdMembresia())
                .orElseThrow(
                        () -> new RuntimeException("Membresía no encontrada con ID: " + requestDTO.getIdMembresia()));

        if (!membresia.getEsFlexible()) {
            throw new RuntimeException("La membresía seleccionada no es flexible. Use el método de asignación normal.");
        }

        if (!membresia.getActivo()) {
            throw new RuntimeException("La membresía no está activa");
        }

        if (socioMembresiaRepository.existsBySocio_IdUsuarioAndEstado(requestDTO.getIdSocio(),
                EnumEstadoSocioMembresia.ACTIVA)) {
            throw new RuntimeException("El socio ya tiene una membresía activa.");
        }

        if (membresia.getPrecioPorDia() == null || membresia.getPrecioPorDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La membresía flexible no tiene precio por día configurado");
        }

        LocalDate fechaInicio = LocalDate.now();
        LocalDate fechaVencimiento = fechaInicio.plusDays(requestDTO.getCantidadDias());

        BigDecimal precioReal = membresia.getPrecioPorDia()
                .multiply(BigDecimal.valueOf(requestDTO.getCantidadDias()));

        SocioMembresia socioMembresia = new SocioMembresia();
        socioMembresia.setSocio(socio);
        socioMembresia.setMembresia(membresia);
        socioMembresia.setFechaInicio(fechaInicio);
        socioMembresia.setFechaVencimiento(fechaVencimiento);
        socioMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);

        String observacionCompleta = String.format(
                "Membresía flexible - %d días - Precio calculado: $%,.0f - %s",
                requestDTO.getCantidadDias(),
                precioReal,
                requestDTO.getObservaciones() != null ? requestDTO.getObservaciones() : "Sin observaciones");
        socioMembresia.setObservaciones(observacionCompleta);

        socioMembresiaRepository.save(socioMembresia);

        return new MessegeGlobalDTO(String.format(
                "Membresía flexible '%s' asignada correctamente a %s. " +
                        "Duración: %d días. " +
                        "Vence: %s. " +
                        "Precio total: $%,.0f " +
                        "($%,.0f por día x %d días)",
                membresia.getNombre(),
                socio.getNombre(),
                requestDTO.getCantidadDias(),
                fechaVencimiento,
                precioReal,
                membresia.getPrecioPorDia(),
                requestDTO.getCantidadDias()));
    }

    /**
     * Obtiene membresías por vencer en los próximos 5 días
     * 
     * @return Lista de DTOs con las membresías por vencer
     */
    @Transactional(readOnly = true)
    public List<MembresiaPorVencerDTO> obtenerMembresiasPorVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = hoy.plusDays(5);

        List<SocioMembresia> membresias = socioMembresiaRepository
                .findMembresiasPorVencerEnRango(hoy, fechaLimite);

        if (membresias.isEmpty()) {
            return Collections.emptyList();
        }

        return membresias.stream()
                .map(this::convertirAPorVencerDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene membresías por vencer en un rango específico de días
     * 
     * @param diasMinimo Días mínimos desde hoy (ej: 1)
     * @param diasMaximo Días máximos desde hoy (ej: 3)
     * @param userRol    Rol del usuario autenticado
     * @return Lista de DTOs con las membresías por vencer en el rango
     * @throws IllegalArgumentException Si los parámetros son inválidos
     */
    @Transactional(readOnly = true)
    public List<MembresiaPorVencerDTO> obtenerMembresiasPorVencerEnRango(
            int diasMinimo, int diasMaximo, String userRol) {

        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        if (diasMinimo < 0) {
            throw new IllegalArgumentException("diasMinimo debe ser mayor o igual a 0");
        }
        if (diasMaximo < diasMinimo) {
            throw new IllegalArgumentException("diasMaximo debe ser mayor o igual a diasMinimo");
        }
        if (diasMaximo > 365) {
            throw new IllegalArgumentException("diasMaximo no puede ser mayor a 365");
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaInicio = hoy.plusDays(diasMinimo);
        LocalDate fechaFin = hoy.plusDays(diasMaximo);

        List<SocioMembresia> membresias = socioMembresiaRepository
                .findMembresiasPorVencerEnRango(fechaInicio, fechaFin);

        if (membresias.isEmpty()) {
            return Collections.emptyList();
        }

        return membresias.stream()
                .map(this::convertirAPorVencerDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad SocioMembresia a un DTO de membresía por vencer
     * 
     * @param sm La entidad SocioMembresia a convertir
     * @return Un objeto MembresiaPorVencerDTO con los datos de la membresía por
     *         vencer
     */
    private MembresiaPorVencerDTO convertirAPorVencerDTO(SocioMembresia sm) {
        UsuarioPerfil socio = sm.getSocio();
        LocalDate hoy = LocalDate.now();
        long dias = hoy.until(sm.getFechaVencimiento()).getDays();
        int diasRestantes = (int) Math.max(0, dias);

        String urgencia;
        if (diasRestantes <= 1) {
            urgencia = "CRITICO";
        } else if (diasRestantes <= 2) {
            urgencia = "URGENTE";
        } else {
            urgencia = "PRONTO";
        }

        return MembresiaPorVencerDTO.builder()
                .idSocioMembresia(sm.getIdSocioMembresia())
                .idSocio(socio.getIdUsuario())
                .nombreSocio(socio.getNombre() + " " + socio.getApellido())
                .emailSocio(socio.getEmail())
                .telefono(socio.getTelefono())
                .idMembresia(sm.getMembresia().getIdMembresia())
                .nombreMembresia(sm.getMembresia().getNombre())
                .fechaVencimiento(sm.getFechaVencimiento())
                .diasRestantes(diasRestantes)
                .estado(sm.getEstado().name())
                .urgencia(urgencia)
                .avatarUrl(generarAvatarUrl(socio.getNombre()))
                .build();
    }

    /**
     * Genera una URL de avatar basada en el nombre del socio.
     * 
     * @param nombre Nombre del socio
     * @return URL del avatar generado
     */
    private String generarAvatarUrl(String nombre) {
        return "https://ui-avatars.com/api/?name=" +
                nombre.replace(" ", "+") +
                "&background=0F1C3F&color=fff&bold=true";
    }
}
