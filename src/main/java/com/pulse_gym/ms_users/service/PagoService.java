package com.pulse_gym.ms_users.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.preference.Preference;
import com.pulse_gym.lb_common.client.EventoPagoClient;
import com.pulse_gym.lb_common.client.ReportesClient;
import com.pulse_gym.lb_common.dto.AnularPagoRequestDTO;
import com.pulse_gym.lb_common.dto.EventoPagoRequestDTO;
import com.pulse_gym.lb_common.dto.FiltroPagosRequestDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PagoResponseDTO;
import com.pulse_gym.lb_common.dto.PaymentSummaryDTO;
import com.pulse_gym.lb_common.dto.PreferenceResponseDTO;
import com.pulse_gym.lb_common.dto.RegistrarPagoRequestDTO;
import com.pulse_gym.lb_common.entity.user.Membresia;
import com.pulse_gym.lb_common.entity.user.Pago;
import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoPago;
import com.pulse_gym.lb_common.enums.EnumEstadoSocioMembresia;
import com.pulse_gym.lb_common.enums.EnumMetodoPago;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.PagoRepository;
import com.pulse_gym.ms_users.repository.SocioMembresiaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    /** Repositorio para operaciones con pagos */
    private final PagoRepository pagoRepository;

    /** Repositorio para operaciones con membresías asignadas */
    private final SocioMembresiaRepository socioMembresiaRepository;

    /** Repositorio para operaciones con usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Servicio para generar comprobantes de pago en PDF */
    private final PagoPDFService pagoPDFService;

    /** Servicio para gestión de membresías de socios */
    private final SocioMembresiaService socioMembresiaService;

    /** Servicio para enviar eventos de pago de manera asíncrona */
    private final ReportesClient reportesClient;

    private final EventoPagoAsyncService eventoPagoAsyncService; // Opcional, si quieres async

    /**
     * Token de acceso para la integración con la API de MercadoPago, configurado
     * desde variables de entorno
     */
    @Value("${MERCADOPAGO_ACCESS_TOKEN}")
    private String mpAccessToken;

    /**
     * Convierte una entidad Pago a PagoResponseDTO
     * 
     * @param pago Entidad de pago a convertir
     * @return DTO con los datos del pago
     */
    private PagoResponseDTO convertirAResponseDTO(Pago pago) {
        PagoResponseDTO dto = new PagoResponseDTO();
        dto.setIdPago(pago.getIdPago());
        dto.setIdSocio(pago.getSocioMembresia().getSocio().getIdUsuario());
        dto.setNombreSocio(pago.getSocioMembresia().getSocio().getNombre() + " " +
                pago.getSocioMembresia().getSocio().getApellido());
        dto.setEmailSocio(pago.getSocioMembresia().getSocio().getEmail());
        dto.setIdSocioMembresia(pago.getSocioMembresia().getIdSocioMembresia());
        dto.setNombreMembresia(pago.getSocioMembresia().getMembresia().getNombre());
        dto.setMonto(pago.getMonto());
        dto.setFechaPago(pago.getFechaPago());
        dto.setMetodoPago(pago.getMetodoPago().name());
        dto.setNumeroComprobante(pago.getNumeroComprobante());

        if (pago.getAdminRegistro() != null) {
            dto.setIdAdminRegistro(pago.getAdminRegistro().getIdUsuario());
            dto.setNombreAdminRegistro(pago.getAdminRegistro().getNombre() + " " +
                    pago.getAdminRegistro().getApellido());
        }

        dto.setObservaciones(pago.getObservaciones());
        dto.setAnulado(pago.getAnulado());
        dto.setMotivoAnulacion(pago.getMotivoAnulacion());
        dto.setFechaAnulacion(pago.getFechaAnulacion());

        return dto;
    }

    /**
     * Registra un nuevo pago para una membresía asignada a un socio.
     * Valida que el usuario tenga rol autorizado (admin, entrenador o
     * recepcionista),
     * que la membresía asignada exista, que el método de pago sea válido,
     * que la membresía tenga un precio válido y genera un comprobante automático si
     * no se proporciona.
     * 
     * @param requestDTO        DTO con los datos del pago (idSocioMembresia,
     *                          metodoPago, numeroComprobante, observaciones)
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario que registra el pago
     * @return Mensaje de confirmación con el socio, monto, método y comprobante
     */
    @Transactional
    public MessegeGlobalDTO registrarPago(RegistrarPagoRequestDTO requestDTO, String userRol, String userEmail) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> new RuntimeException(
                        "Asignación de membresía no encontrada con ID: "
                                + requestDTO.getIdSocioMembresia()));

        if (requestDTO.getMetodoPago() == null) {
            throw new RuntimeException(
                    "Método de pago no válido o ausente. Valores: EFECTIVO, TRANSFERENCIA_BANCOLOMBIA, TARJETA_CREDITO, TARJETA_DEBITO, OTRO");
        }

        EnumMetodoPago metodoPago = requestDTO.getMetodoPago();

        // Búsqueda del usuario autenticado por correo del token
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado desde el token.");
        }

        UsuarioPerfil admin = usuarioRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario administrador/personal no encontrado con el correo del token: " + userEmail));

        BigDecimal montoMembresia;
        if (requestDTO.getMonto() != null && requestDTO.getMonto().compareTo(BigDecimal.ZERO) > 0) {
            montoMembresia = requestDTO.getMonto();
        } else {
            montoMembresia = socioMembresia.getMembresia().getPrecioTotal();
        }

        if (montoMembresia == null || montoMembresia.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("La membresía asociada no tiene un precio válido asignado.");
        }

        String comprobanteFinal = requestDTO.getNumeroComprobante();
        if (comprobanteFinal == null || comprobanteFinal.trim().isEmpty()) {
            String codigoUnico = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            comprobanteFinal = "REC-" + codigoUnico;
        }

        Pago pago = new Pago();
        pago.setSocioMembresia(socioMembresia);
        pago.setMonto(montoMembresia);
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodoPago(metodoPago);
        pago.setNumeroComprobante(comprobanteFinal);
        pago.setAdminRegistro(admin);
        pago.setObservaciones(requestDTO.getObservaciones());
        pago.setAnulado(false);
        pago.setEstado(EnumEstadoPago.APROBADO);

        pagoRepository.save(pago);
        enviarEventoPago(pago);

        try {
            boolean esFlexible = socioMembresia.getMembresia().getEsFlexible();

            if (esFlexible) {

                int diasASumar = (requestDTO.getCantidadDias() != null && requestDTO.getCantidadDias() > 0)
                        ? requestDTO.getCantidadDias()
                        : (socioMembresia.getCantidadDias() != null && socioMembresia.getCantidadDias() > 0
                                ? socioMembresia.getCantidadDias()
                                : 30);

                LocalDate fechaBase = (socioMembresia.getFechaVencimiento() != null
                        && socioMembresia.getFechaVencimiento().isAfter(LocalDate.now()))
                                ? socioMembresia.getFechaVencimiento()
                                : LocalDate.now();

                socioMembresia.setFechaVencimiento(fechaBase.plusDays(diasASumar));

                int diasActuales = socioMembresia.getCantidadDias() != null ? socioMembresia.getCantidadDias() : 0;
                socioMembresia.setCantidadDias(diasActuales + diasASumar);
                socioMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);

                socioMembresiaRepository.save(socioMembresia);
            } else {
                socioMembresiaService.actualizarEstadoMembresiaPorPago(socioMembresia.getIdSocioMembresia());
            }

        } catch (Exception e) {
            log.warn("Error al actualizar estado/vencimiento de membresía por pago: {}", e.getMessage());
        }

        return new MessegeGlobalDTO(String.format(
                "Pago registrado correctamente. Socio: %s, Monto: $%,.0f, Método: %s, Comprobante: %s",
                socioMembresia.getSocio().getNombre(),
                pago.getMonto(),
                metodoPago.name(),
                pago.getNumeroComprobante()));
    }

    /**
     * Inicia un pago de membresía desde la aplicación móvil integrando con
     * MercadoPago.
     * Valida que el usuario sea socio, que la membresía le pertenezca,
     * que el método de pago no sea efectivo, y genera una preferencia de pago en
     * MercadoPago.
     * 
     * @param requestDTO DTO con los datos del pago (idSocioMembresia, metodoPago)
     * @param userRol    Rol del usuario autenticado (debe ser socio)
     * @param userEmail  Email del socio autenticado
     * @return DTO con el ID de preferencia y URL de pago de MercadoPago
     */
    @Transactional
    public PreferenceResponseDTO iniciarPagoMembresiaApp(RegistrarPagoRequestDTO requestDTO, String userRol,
            String userEmail) {

        if (requestDTO.getMetodoPago() == EnumMetodoPago.EFECTIVO) {
            throw new IllegalArgumentException(
                    "El método de pago en efectivo no está permitido para transacciones desde la aplicación móvil.");
        }

        ValidacionDeRoles.validarSocio(userRol);

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado"));

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> new RuntimeException("Asignación de membresía no encontrada"));

        if (!socioMembresia.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. No puedes pagar una membresía ajena.");
        }

        try {
            PreferenceClient client = new PreferenceClient();

            String tokenFinal = (this.mpAccessToken != null && !this.mpAccessToken.isEmpty())
                    ? this.mpAccessToken
                    : "TEST-4168132953531234-061910-c114382583896dfa26bfe218e860956b-272097072";

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(tokenFinal.trim())
                    .build();

            BigDecimal montoMembresia = socioMembresia.getMembresia().getPrecioTotal();
            if (montoMembresia == null || montoMembresia.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("La membresía asociada no tiene un precio válido asignado.");
            }
            BigDecimal montoFormateado = montoMembresia.setScale(2, RoundingMode.HALF_UP);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(socioMembresia.getIdSocioMembresia().toString())
                    .title("Pulse GYM - Membresia: " + socioMembresia.getMembresia().getNombre())
                    .quantity(1)
                    .unitPrice(montoFormateado)
                    .currencyId("COP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:5500/success.html")
                    .failure("http://localhost:5500/failure.html")
                    .pending("http://localhost:5500/pending.html")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .externalReference(socioMembresia.getIdSocioMembresia().toString())
                    .build();

            Preference preference = client.create(preferenceRequest, requestOptions);

            Pago nuevoPago = new Pago();
            nuevoPago.setSocioMembresia(socioMembresia);
            nuevoPago.setMonto(montoFormateado);
            nuevoPago.setFechaPago(LocalDateTime.now());
            nuevoPago.setMetodoPago(requestDTO.getMetodoPago());
            nuevoPago.setEstado(EnumEstadoPago.PENDIENTE);
            nuevoPago.setAnulado(false);

            nuevoPago.setNumeroComprobante(preference.getId());

            pagoRepository.save(nuevoPago);

            enviarEventoPago(nuevoPago);

            return new PreferenceResponseDTO(preference.getId(), preference.getSandboxInitPoint());

        } catch (MPApiException apiException) {
            System.err.println("=== ERROR DETALLADO DE MERCADO PAGO ===");
            System.err.println("Status Código: " + apiException.getStatusCode());
            System.err.println("Cuerpo de Respuesta de MP: " + apiException.getApiResponse().getContent());
            System.err.println("=======================================");
            throw new RuntimeException("Mercado Pago falló: " + apiException.getApiResponse().getContent());
        } catch (Exception e) {
            throw new RuntimeException("Error general al inicializar pago: " + e.getMessage());
        }
    }

    /**
     * Consulta el historial de pagos de un socio con validación de permisos
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Lista de pagos del socio
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     * @throws RuntimeException               Si no se encuentra el socio o no tiene
     *                                        pagos
     */
    @Transactional(readOnly = true)
    public List<PagoResponseDTO> consultarHistorialPagos(Long idSocio, String userRol, Long userIdAutenticado,
            String userEmail) {

        if (userRol.equals(EnumRol.socio.name())) {
            UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException(
                            "Socio autenticado no encontrado con email: " + userEmail));

            UsuarioPerfil socioConsultado = usuarioRepository.findById(idSocio)
                    .orElseThrow(() -> new RuntimeException(
                            "Socio no encontrado con ID: " + idSocio));

            if (!socioAutenticado.getEmail().equals(socioConsultado.getEmail())) {
                throw new SecurityAuthorizationException(
                        "Acceso denegado. Solo puede consultar su propio historial. Tu email: "
                                +
                                socioAutenticado.getEmail() + ", consultado: "
                                + socioConsultado.getEmail());
            }
        } else if (!userRol.equals(EnumRol.administrador.name())
                && !userRol.equals(EnumRol.recepcionista.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
        }

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        List<Pago> pagos = pagoRepository.findBySocioId(idSocio);

        if (pagos.isEmpty()) {
            throw new RuntimeException("El socio " + socio.getNombre() + " no tiene pagos registrados");
        }

        return pagos.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filtra pagos aplicando criterios de búsqueda
     * 
     * @param filtro  DTO con los filtros a aplicar
     * @param userRol Rol del usuario autenticado
     * @return Lista de pagos que coinciden con los filtros
     * @throws RuntimeException Si no se encuentran pagos
     */
    @Transactional(readOnly = true)
    public Page<PagoResponseDTO> filtrarPagosPaginados(FiltroPagosRequestDTO filtro, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        Pageable pageable = PageRequest.of(
                filtro.getPage(),
                filtro.getSize(),
                Sort.by("fechaPago").descending());

        Specification<Pago> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getSearch() != null && !filtro.getSearch().trim().isEmpty()) {
                String searchTerm = "%" + filtro.getSearch().trim().toLowerCase() + "%";

                Join<Pago, SocioMembresia> socioMembresiaJoin = root.join("socioMembresia");
                Join<SocioMembresia, UsuarioPerfil> socioJoin = socioMembresiaJoin.join("socio");
                Join<SocioMembresia, Membresia> membresiaJoin = socioMembresiaJoin.join("membresia", JoinType.LEFT);

                Predicate nombreSocioMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(socioJoin.get("nombre"), " "), socioJoin.get("apellido"))),
                        searchTerm);
                Predicate emailMatch = cb.like(cb.lower(socioJoin.get("email")), searchTerm);
                Predicate comprobanteMatch = cb.like(cb.lower(root.get("numeroComprobante")), searchTerm);
                Predicate planMatch = cb.like(cb.lower(membresiaJoin.get("nombre")), searchTerm);

                predicates.add(cb.or(nombreSocioMatch, emailMatch, comprobanteMatch, planMatch));
            }

            if (filtro.getIdSocio() != null) {
                predicates.add(cb.equal(root.get("socioMembresia").get("socio").get("idUsuario"), filtro.getIdSocio()));
            }

            if (filtro.getMetodoPago() != null && !filtro.getMetodoPago().equalsIgnoreCase("TODOS")) {
                try {
                    EnumMetodoPago metodo = EnumMetodoPago.valueOf(filtro.getMetodoPago().toUpperCase());
                    predicates.add(cb.equal(root.get("metodoPago"), metodo));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (filtro.getEstado() != null && !filtro.getEstado().equalsIgnoreCase("TODOS")) {
                try {
                    EnumEstadoPago estadoEnum = EnumEstadoPago.valueOf(filtro.getEstado().toUpperCase());
                    predicates.add(cb.equal(root.get("estado"), estadoEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
                predicates.add(cb.between(root.get("fechaPago"), filtro.getFechaInicio(), filtro.getFechaFin()));
            } else if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaPago"), filtro.getFechaInicio()));
            } else if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaPago"), filtro.getFechaFin()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Pago> paginaPagos = pagoRepository.findAll(spec, pageable);
        return paginaPagos.map(this::convertirAResponseDTO);
    }

    /**
     * Anula un pago existente
     * 
     * @param requestDTO DTO con el ID del pago y motivo de anulación
     * @param userRol    Rol del usuario autenticado
     * @return Mensaje de confirmación de la anulación
     * @throws RuntimeException Si el pago no existe o ya está anulado
     */
    @Transactional
    public MessegeGlobalDTO anularPago(AnularPagoRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        Pago pago = pagoRepository.findById(requestDTO.getIdPago())
                .orElseThrow(() -> new RuntimeException(
                        "Pago no encontrado con ID: " + requestDTO.getIdPago()));

        if (pago.isAnulado()) {
            throw new RuntimeException("Este pago ya está anulado");
        }

        pago.setAnulado(true);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setMotivoAnulacion(requestDTO.getMotivo());
        pago.setEstado(EnumEstadoPago.ANULADO);

        pagoRepository.save(pago);

        try {
            Long socioId = pago.getSocioMembresia().getSocio().getIdUsuario();
            LocalDateTime fechaPago = pago.getFechaPago();
            reportesClient.anularEventoPago(socioId, fechaPago);
            log.info("Evento de pago sincronizado y anulado en pg-ms-reports para el pago ID: {}", pago.getIdPago());
        } catch (Exception e) {
            log.error("Error al notificar la anulación del pago al microservicio de reportes: {}", e.getMessage());
        }

        return new MessegeGlobalDTO(String.format(
                "Pago ID: %d anulado correctamente. Motivo: %s",
                pago.getIdPago(),
                requestDTO.getMotivo()));
    }

    /**
     * Genera el comprobante de un pago
     * 
     * @param idPago            ID del pago a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return DTO con los datos del pago
     */
    @Transactional(readOnly = true)
    public PagoResponseDTO generarComprobante(Long idPago, String userRol, Long userIdAutenticado,
            String userEmail) {

        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + idPago));

        if (userRol != null) {
            if (userRol.equals(EnumRol.socio.name())) {
                UsuarioPerfil socioAutenticado = usuarioRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new RuntimeException(
                                "Socio autenticado no encontrado con email: " + userEmail));

                UsuarioPerfil socioPago = pago.getSocioMembresia().getSocio();

                if (!socioAutenticado.getEmail().equals(socioPago.getEmail())) {
                    throw new SecurityAuthorizationException(
                            "Acceso denegado. Solo puede ver sus propios comprobantes");
                }
            } else if (!userRol.equals(EnumRol.administrador.name())
                    && !userRol.equals(EnumRol.recepcionista.name())) {
                throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
            }
        }

        return convertirAResponseDTO(pago);
    }

    /**
     * Genera un comprobante de pago en formato PDF
     * 
     * @param idPago            ID del pago a consultar
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Array de bytes del PDF generado
     */
    @Transactional(readOnly = true)
    public byte[] generarComprobantePDF(Long idPago, String userRol, Long userIdAutenticado, String userEmail) {
        PagoResponseDTO pagoDTO = generarComprobante(idPago, userRol, userIdAutenticado, userEmail);

        return pagoPDFService.generarComprobantePDF(pagoDTO);
    }

    /**
     * 
     * @param idPago
     * @return
     */
    @Transactional(readOnly = true)
    public Pago obtenerPagoPorId(Long idPago) {
        return pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + idPago));
    }

    private void enviarEventoPago(Pago pago) {
        EventoPagoRequestDTO dto = new EventoPagoRequestDTO();
        dto.setSocioId(pago.getSocioMembresia().getSocio().getIdUsuario());
        dto.setMonto(pago.getMonto());
        dto.setFechaPago(pago.getFechaPago());
        dto.setTipoMembresia(pago.getSocioMembresia().getMembresia().getNombre());
        dto.setMetodoPago(pago.getMetodoPago().name());
        eventoPagoAsyncService.enviarEventoPago(dto);

    }

    /**
     * Obtiene el resumen de pagos con estadísticas generales
     * 
     * @return DTO con el resumen de pagos
     */
    @Transactional(readOnly = true)
    public PaymentSummaryDTO obtenerResumenPagos() {
        List<Pago> pagos = pagoRepository.findAll();

        java.time.LocalDate hoy = java.time.LocalDate.now();
        java.time.Month mesActual = hoy.getMonth();
        int anioActual = hoy.getYear();

        java.time.LocalDate mesAnteriorDate = hoy.minusMonths(1);
        java.time.Month mesPasado = mesAnteriorDate.getMonth();
        int anioPasado = mesAnteriorDate.getYear();

        BigDecimal ingresosMes = pagos.stream()
                .filter(p -> p.getEstado() == EnumEstadoPago.APROBADO && !p.isAnulado() && p.getFechaPago() != null)
                .filter(p -> p.getFechaPago().getMonth() == mesActual && p.getFechaPago().getYear() == anioActual)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ingresosMesAnterior = pagos.stream()
                .filter(p -> p.getEstado() == EnumEstadoPago.APROBADO && !p.isAnulado() && p.getFechaPago() != null)
                .filter(p -> p.getFechaPago().getMonth() == mesPasado && p.getFechaPago().getYear() == anioPasado)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pagosEsteMes = pagos.stream()
                .filter(p -> p.getFechaPago() != null
                        && p.getFechaPago().getMonth() == mesActual && p.getFechaPago().getYear() == anioActual)
                .count();

        long pendientes = pagos.stream().filter(p -> p.getEstado() == EnumEstadoPago.PENDIENTE).count();
        long completados = pagos.stream().filter(p -> p.getEstado() == EnumEstadoPago.APROBADO).count();

        PaymentSummaryDTO dto = new PaymentSummaryDTO();
        dto.setIngresosMes(ingresosMes.doubleValue());
        dto.setIngresosMesAnterior(ingresosMesAnterior.doubleValue());
        dto.setPagosEsteMes((int) pagosEsteMes);
        dto.setPendientesCount((int) pendientes);
        dto.setVencidosCount(0);
        dto.setCompletadosCount((int) completados);
        return dto;
    }
}