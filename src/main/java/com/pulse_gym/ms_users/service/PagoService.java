package com.pulse_gym.ms_users.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.merchantorder.MerchantOrder;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.pulse_gym.lb_common.client.ReportesClient;
import com.pulse_gym.lb_common.dto.AnularPagoRequestDTO;
import com.pulse_gym.lb_common.dto.EventoPagoRequestDTO;
import com.pulse_gym.lb_common.dto.FiltroPagosRequestDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PagoResponseDTO;
import com.pulse_gym.lb_common.dto.PagoResultResponseDTO;
import com.pulse_gym.lb_common.dto.PaymentSummaryDTO;
import com.pulse_gym.lb_common.dto.PreferenceResponseDTO;
import com.pulse_gym.lb_common.dto.RegistrarPagoRequestDTO;
import com.pulse_gym.lb_common.dto.SocioDashboardPagosDTO;
import com.pulse_gym.lb_common.dto.TokenizedPaymentRequestDTO;
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

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    /** Repositorio de pagos */
    private final PagoRepository pagoRepository;

    /** Repositorio de membresías de socios */
    private final SocioMembresiaRepository socioMembresiaRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Servicio para generar comprobantes PDF */
    private final PagoPDFService pagoPDFService;

    /** Servicio de membresías de socios */
    private final SocioMembresiaService socioMembresiaService;

    /** Cliente Feign para reportes */
    private final ReportesClient reportesClient;

    /** Servicio asíncrono para eventos de pago */
    private final EventoPagoAsyncService eventoPagoAsyncService;

    /** Access Token de Mercado Pago */
    @Value("${MERCADOPAGO_ACCESS_TOKEN}")
    private String mpAccessToken;

    /** Secret para validar firmas de webhooks */
    @Value("${MERCADOPAGO_WEBHOOK_SECRET:}")
    private String mpWebhookSecret;

    /** Habilita/deshabilita la validación de firma de webhooks */
    @Value("${MERCADOPAGO_VALIDATE_SIGNATURE:true}")
    private boolean validarFirmaHabilitada;

    /** ID de usuario de prueba esperado para validación de ambiente */
    @Value("${MERCADOPAGO_TEST_USER_ID:3481341455}")
    private String mpTestUserIdEsperado;

    /**
     * Procesa un pago con token desde la aplicación móvil (Checkout API)
     * 
     * @param requestDTO Datos del pago tokenizado
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Resultado del pago
     * @throws RuntimeException               Si el socio o membresía no existen
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     */
    @Transactional
    public PagoResultResponseDTO procesarPagoConTokenApp(TokenizedPaymentRequestDTO requestDTO, String userRol,
            String userEmail) {

        log.info("==================================================================");
        log.info("=== 🚀 INICIANDO PAGO APP (CHECKOUT API) PARA SOCIO ===");
        log.info("==================================================================");
        log.info("-> Email recibido del header: {}", userEmail);
        log.info("-> Rol recibido del header: {}", userRol);
        log.info("-> ID Socio Membresía en Request: {}", requestDTO.getIdSocioMembresia());
        log.info("-> PaymentMethodId: {}", requestDTO.getPaymentMethodId());
        log.info("-> Cuotas (Installments): {}", requestDTO.getInstallments());
        log.info("-> Método de Pago seleccionado desde UI: {}", requestDTO.getMetodoPago());
        log.info("-> Token de Tarjeta (Primeros chars): {}",
                requestDTO.getToken() != null && requestDTO.getToken().length() > 6
                        ? requestDTO.getToken().substring(0, 6) + "..."
                        : "NULO/CORTO");
        log.info("-> Payer Email en Request: {}", requestDTO.getPayerEmail());
        log.info("-> Payer ID Type: {}", requestDTO.getPayerIdentificationType());
        log.info("-> Payer ID Number: {}", requestDTO.getPayerIdentificationNumber());

        ValidacionDeRoles.validarCualquierRol(userRol);

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("❌ SOCIO NO ENCONTRADO CON EMAIL: {}", userEmail);
                    return new RuntimeException("Socio no encontrado con email: " + userEmail);
                });
        log.info("-> Socio encontrado en BD. ID Usuario: {}, Nombre: {} {}", socio.getIdUsuario(), socio.getNombre(),
                socio.getApellido());

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> {
                    log.error("❌ ASIGNACIÓN DE MEMBRESÍA NO ENCONTRADA CON ID: {}",
                            requestDTO.getIdSocioMembresia());
                    return new RuntimeException(
                            "Asignación de membresía no encontrada con ID: " + requestDTO
                                    .getIdSocioMembresia());
                });
        log.info("-> Membresía de socio encontrada. ID Socio Dueño: {}, Estado actual: {}",
                socioMembresia.getSocio().getIdUsuario(), socioMembresia.getEstado());

        if (!socioMembresia.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
            log.warn(
                    "🚨 ACCESO DENEGADO: El socio ID {} intentó pagar la membresía ID {} que pertenece al usuario ID {}",
                    socio.getIdUsuario(), socioMembresia.getIdSocioMembresia(),
                    socioMembresia.getSocio().getIdUsuario());
            throw new SecurityAuthorizationException("Acceso denegado. No puedes pagar una membresía ajena.");
        }

        Membresia membresiaBase = socioMembresia.getMembresia();

        RegistrarPagoRequestDTO montoHelper = new RegistrarPagoRequestDTO();
        montoHelper.setMonto(requestDTO.getMonto());
        montoHelper.setCantidadDias(requestDTO.getCantidadDias());
        BigDecimal montoMembresia = calcularMontoMembresia(montoHelper, socioMembresia, membresiaBase);
        BigDecimal montoFormateado = montoMembresia.setScale(2, RoundingMode.HALF_UP);

        log.info("-> Monto final calculado para el pago: {}", montoFormateado);

        try {
            String idempotencyKey = UUID.randomUUID().toString();
            log.info("-> Generada X-Idempotency-Key: {}", idempotencyKey);

            MPRequestOptions requestOptions = MPRequestOptions.builder()
                    .accessToken(getAccessTokenValidado())
                    .customHeaders(Map.of("X-Idempotency-Key", idempotencyKey))
                    .build();

            IdentificationRequest identification = IdentificationRequest.builder()
                    .type(requestDTO.getPayerIdentificationType())
                    .number(requestDTO.getPayerIdentificationNumber())
                    .build();

            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(requestDTO.getPayerEmail())
                    .identification(identification)
                    .build();

            PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.builder()
                    .transactionAmount(montoFormateado)
                    .token(requestDTO.getToken())
                    .description("Pulse GYM - Membresia: " + membresiaBase.getNombre())
                    .installments(requestDTO.getInstallments())
                    .paymentMethodId(requestDTO.getPaymentMethodId())
                    .issuerId(requestDTO.getIssuerId())
                    .payer(payer)
                    .externalReference(socioMembresia.getIdSocioMembresia().toString())
                    .statementDescriptor("PULSEGYM")
                    .binaryMode(true)
                    .build();

            log.info("-> Enviando petición PaymentClient.create() a Mercado Pago...");
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.create(paymentCreateRequest, requestOptions);

            log.info("==================================================================");
            log.info("=== 📥 RESPUESTA EXITOSA DE MERCADO PAGO API ===");
            log.info("==================================================================");
            log.info("-> Payment ID MP: {}", payment.getId());
            log.info("-> Status: {}", payment.getStatus());
            log.info("-> Status Detail: {}", payment.getStatusDetail());
            log.info("-> Payment Method ID: {}", payment.getPaymentMethodId());
            log.info("-> Statement Descriptor: {}", payment.getStatementDescriptor());

            EnumMetodoPago metodoPagoFinal;
            if (requestDTO.getMetodoPago() != null) {
                metodoPagoFinal = requestDTO.getMetodoPago();
            } else {
                metodoPagoFinal = mapearMetodoPagoDesdePaymentMethodId(requestDTO.getPaymentMethodId());
            }

            Pago nuevoPago = new Pago();
            nuevoPago.setSocioMembresia(socioMembresia);
            nuevoPago.setMonto(montoFormateado);
            nuevoPago.setFechaPago(LocalDateTime.now());
            nuevoPago.setMetodoPago(metodoPagoFinal);
            nuevoPago.setPaymentIdMp(String.valueOf(payment.getId()));
            nuevoPago.setNumeroComprobante("MP-" + payment.getId());
            nuevoPago.setAnulado(false);
            nuevoPago.setEstado(mapearEstadoPago(payment.getStatus()));

            pagoRepository.save(nuevoPago);
            log.info("-> Pago local guardado con éxito. ID Local: {}, Estado: {}", nuevoPago.getIdPago(),
                    nuevoPago.getEstado());

            String mensaje;

            if ("approved".equalsIgnoreCase(payment.getStatus())) {
                mensaje = "¡Pago aprobado exitosamente!";
                enviarEventoPago(nuevoPago);
                actualizarMembresiaTrasPagoAprobado(socioMembresia);
            } else if ("in_process".equalsIgnoreCase(payment.getStatus())
                    || "pending".equalsIgnoreCase(payment.getStatus())) {
                mensaje = "Tu pago está siendo procesado. Te notificaremos cuando se confirme.";
            } else if ("rejected".equalsIgnoreCase(payment.getStatus())) {
                mensaje = "El pago fue rechazado: " + traducirStatusDetail(payment.getStatusDetail());
            } else {
                mensaje = "Estado de pago desconocido: " + payment.getStatus();
            }

            return new PagoResultResponseDTO(
                    nuevoPago.getIdPago(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    String.valueOf(payment.getId()),
                    montoFormateado,
                    mensaje);

        } catch (MPApiException apiException) {
            log.error("==================================================================");
            log.error("❌ 🚨 ERROR DE MERCADO PAGO (MPApiException) 🚨 ❌");
            log.error("==================================================================");
            log.error("-> HTTP Status Code de MP: {}", apiException.getStatusCode());
            if (apiException.getApiResponse() != null) {
                log.error("-> Contenido JSON de respuesta MP: {}", apiException.getApiResponse().getContent());
            } else {
                log.error("-> Mensaje de excepción MP: {}", apiException.getMessage());
            }
            throw new RuntimeException("Mercado Pago rechazó la solicitud: "
                    + (apiException.getApiResponse() != null ? apiException.getApiResponse().getContent()
                            : apiException.getMessage()));
        } catch (Exception e) {
            log.error("==================================================================");
            log.error("❌ 🚨 ERROR GENERAL EN PROCESO DE PAGO TOKENIZADO 🚨 ❌");
            log.error("==================================================================", e);
            throw new RuntimeException("Error general al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Actualiza la membresía del socio tras un pago aprobado
     * 
     * @param socioMembresia Membresía a actualizar
     */
    private void actualizarMembresiaTrasPagoAprobado(SocioMembresia socioMembresia) {
        try {
            boolean esFlexible = socioMembresia.getMembresia().getEsFlexible() != null
                    && socioMembresia.getMembresia().getEsFlexible();

            if (esFlexible) {
                int diasASumar = (socioMembresia.getCantidadDias() != null && socioMembresia.getCantidadDias() > 0)
                        ? socioMembresia.getCantidadDias()
                        : 30;

                LocalDate fechaBase = (socioMembresia.getFechaVencimiento() != null
                        && socioMembresia.getFechaVencimiento().isAfter(LocalDate.now()))
                                ? socioMembresia.getFechaVencimiento()
                                : LocalDate.now();

                socioMembresia.setFechaVencimiento(fechaBase.plusDays(diasASumar));
                socioMembresia.setEstado(EnumEstadoSocioMembresia.ACTIVA);
                socioMembresiaRepository.save(socioMembresia);
                log.info("✅ Membresía flexible actualizada. Nueva fecha de vencimiento: {}",
                        socioMembresia.getFechaVencimiento());
            } else {
                socioMembresiaService.actualizarEstadoMembresiaPorPago(socioMembresia.getIdSocioMembresia());
                log.info("✅ Membresía fija actualizada con éxito.");
            }
        } catch (Exception e) {
            log.error("Error al actualizar la membresía tras el pago tokenizado: {}", e.getMessage(), e);
        }
    }

    /**
     * Mapea el paymentMethodId de Mercado Pago a EnumMetodoPago
     * 
     * @param paymentMethodId ID del método de pago
     * @return EnumMetodoPago correspondiente
     */
    private EnumMetodoPago mapearMetodoPagoDesdePaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null) {
            return EnumMetodoPago.OTRO;
        }
        String pmLower = paymentMethodId.toLowerCase();
        if (pmLower.contains("debit") || pmLower.contains("maestro")) {
            return EnumMetodoPago.TARJETA_DEBITO;
        }
        return EnumMetodoPago.TARJETA_CREDITO;
    }

    /**
     * Mapea el estado de Mercado Pago a EnumEstadoPago
     * 
     * @param mpStatus Estado de MP
     * @return EnumEstadoPago correspondiente
     */
    private EnumEstadoPago mapearEstadoPago(String mpStatus) {
        if (mpStatus == null) {
            return EnumEstadoPago.PENDIENTE;
        }
        switch (mpStatus.toLowerCase()) {
            case "approved":
                return EnumEstadoPago.APROBADO;
            case "rejected":
            case "cancelled":
                return EnumEstadoPago.RECHAZADO;
            default:
                return EnumEstadoPago.PENDIENTE;
        }
    }

    /**
     * Traduce el statusDetail de Mercado Pago a un mensaje amigable
     * 
     * @param statusDetail Detalle del estado
     * @return Mensaje traducido
     */
    private String traducirStatusDetail(String statusDetail) {
        if (statusDetail == null) {
            return "Motivo desconocido.";
        }
        switch (statusDetail) {
            case "cc_rejected_insufficient_amount":
                return "Fondos insuficientes.";
            case "cc_rejected_bad_filled_security_code":
                return "El código de seguridad (CVV) es incorrecto.";
            case "cc_rejected_bad_filled_date":
                return "La fecha de vencimiento es incorrecta.";
            case "cc_rejected_bad_filled_card_number":
                return "El número de tarjeta es incorrecto.";
            case "cc_rejected_call_for_authorize":
                return "Debes autorizar el pago con tu banco.";
            case "cc_rejected_card_disabled":
                return "La tarjeta está inhabilitada.";
            case "cc_rejected_duplicated_payment":
                return "Ya existe un pago igual reciente.";
            case "cc_rejected_high_risk":
                return "El pago fue rechazado por seguridad.";
            case "cc_rejected_max_attempts":
                return "Se alcanzó el límite de intentos.";
            default:
                return "Verifica los datos de tu tarjeta o intenta con otro medio de pago.";
        }
    }

    /**
     * Obtiene el Access Token de Mercado Pago validado
     * 
     * @return Access Token
     * @throws IllegalStateException Si no está configurado
     */
    private String getAccessTokenValidado() {
        if (mpAccessToken == null || mpAccessToken.isBlank()) {
            throw new IllegalStateException(
                    "MERCADOPAGO_ACCESS_TOKEN no está configurado. Verifica las variables de entorno.");
        }
        return mpAccessToken.trim();
    }

    /**
     * Convierte una entidad Pago a PagoResponseDTO
     * 
     * @param pago Entidad a convertir
     * @return DTO del pago
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

        if (pago.getEstado() != null) {
            dto.setEstado(pago.getEstado().name());
        }

        return dto;
    }

    /**
     * Registra un pago manual desde la administración
     * 
     * @param requestDTO Datos del pago
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return Mensaje de confirmación
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

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new RuntimeException("No se pudo identificar el usuario autenticado desde el token.");
        }

        UsuarioPerfil admin = usuarioRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new RuntimeException(
                        "Usuario administrador/personal no encontrado con el correo del token: "
                                + userEmail));

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
     * Valida el ambiente de Mercado Pago al iniciar la aplicación.
     */
    @PostConstruct
    private void validarAmbienteMercadoPago() {
        if (mpAccessToken == null || mpAccessToken.isBlank()) {
            log.error("⚠️ MERCADOPAGO_ACCESS_TOKEN no está configurado.");
            return;
        }

        String[] partes = mpAccessToken.split("-");
        String userIdEnToken = partes.length > 0 ? partes[partes.length - 1] : "desconocido";

        boolean coincideConCuentaDePrueba = mpTestUserIdEsperado.equals(userIdEnToken);

        if (!coincideConCuentaDePrueba) {
            log.error("🚨 ALERTA: El Access Token configurado pertenece al User ID '{}', pero se esperaba " +
                    "la cuenta de prueba (Seller Test User) con ID '{}'. " +
                    "Verifica que estés usando el token correcto desde 'Credenciales de prueba' en el panel de MP, " +
                    "no el de tu cuenta real. De lo contrario, TODAS las transacciones serán reales.",
                    userIdEnToken, mpTestUserIdEsperado);
        } else {
            log.info(
                    "✅ Access Token de Mercado Pago validado correctamente. Corresponde a la cuenta de prueba User ID: {}",
                    userIdEnToken);
        }
    }

    /**
     * Inicia un pago de membresía desde la aplicación (Checkout Pro - Legacy)
     * 
     * @param requestDTO Datos del pago
     * @param userRol    Rol del usuario autenticado
     * @param userEmail  Email del usuario autenticado
     * @return DTO con la preferencia de pago
     */
    @Transactional
    public PreferenceResponseDTO iniciarPagoMembresiaApp(RegistrarPagoRequestDTO requestDTO, String userRol,
            String userEmail) {
        log.info("=== INICIANDO PAGO APP PARA SOCIO (CHECKOUT PRO - LEGACY) ===");
        log.info("Email: {}, ID Socio Membresía: {}, Método: {}", userEmail, requestDTO.getIdSocioMembresia(),
                requestDTO.getMetodoPago());

        if (requestDTO.getMetodoPago() == EnumMetodoPago.EFECTIVO) {
            log.warn("Intento de pago en efectivo bloqueado para la app móvil.");
            throw new IllegalArgumentException(
                    "El método de pago en efectivo no está permitido para transacciones desde la aplicación móvil.");
        }

        ValidacionDeRoles.validarCualquierRol(userRol);

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        SocioMembresia socioMembresia = socioMembresiaRepository.findById(requestDTO.getIdSocioMembresia())
                .orElseThrow(() -> new RuntimeException(
                        "Asignación de membresía no encontrada con ID: " + requestDTO
                                .getIdSocioMembresia()));

        if (!socioMembresia.getSocio().getIdUsuario().equals(socio.getIdUsuario())) {
            throw new SecurityAuthorizationException("Acceso denegado. No puedes pagar una membresía ajena.");
        }

        try {
            PreferenceClient client = new PreferenceClient();
            MPRequestOptions requestOptions = getMPRequestOptions();

            Membresia membresiaBase = socioMembresia.getMembresia();
            BigDecimal montoMembresia = calcularMontoMembresia(requestDTO, socioMembresia, membresiaBase);
            BigDecimal montoFormateado = montoMembresia.setScale(2, RoundingMode.HALF_UP);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(membresiaBase.getIdMembresia() != null ? membresiaBase.getIdMembresia().toString()
                            : socioMembresia.getIdSocioMembresia().toString())
                    .title("Pulse GYM - Membresia: " + membresiaBase.getNombre())
                    .quantity(1)
                    .unitPrice(montoFormateado)
                    .currencyId("COP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://front-end-pulsegym.pages.dev/success.html")
                    .failure("https://front-end-pulsegym.pages.dev/failure.html")
                    .pending("https://front-end-pulsegym.pages.dev/pending.html")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
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
            nuevoPago.setPreferenceId(preference.getId());
            nuevoPago.setNumeroComprobante(preference.getId());

            pagoRepository.save(nuevoPago);

            return new PreferenceResponseDTO(preference.getId(), preference.getSandboxInitPoint());

        } catch (MPApiException apiException) {
            throw new RuntimeException("Mercado Pago falló: "
                    + (apiException.getApiResponse() != null ? apiException.getApiResponse().getContent()
                            : apiException.getMessage()));
        } catch (Exception e) {
            throw new RuntimeException("Error general al inicializar pago: " + e.getMessage());
        }
    }

    /**
     * Calcula el monto de la membresía según el tipo (fija o flexible)
     * 
     * @param requestDTO     Datos del pago
     * @param socioMembresia Asignación de membresía
     * @param membresiaBase  Membresía base
     * @return Monto calculado
     */
    private BigDecimal calcularMontoMembresia(RegistrarPagoRequestDTO requestDTO, SocioMembresia socioMembresia,
            Membresia membresiaBase) {

        BigDecimal montoMembresia;
        boolean esFlexible = membresiaBase.getEsFlexible() != null && membresiaBase.getEsFlexible();

        if (esFlexible) {
            if (requestDTO.getMonto() != null && requestDTO.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                montoMembresia = requestDTO.getMonto();
            } else if (socioMembresia.getPrecioReal() != null
                    && socioMembresia.getPrecioReal().compareTo(BigDecimal.ZERO) > 0) {
                montoMembresia = socioMembresia.getPrecioReal();
            } else {
                int diasASumar = (requestDTO.getCantidadDias() != null && requestDTO.getCantidadDias() > 0)
                        ? requestDTO.getCantidadDias()
                        : (socioMembresia.getCantidadDias() != null && socioMembresia.getCantidadDias() > 0
                                ? socioMembresia.getCantidadDias()
                                : 30);

                BigDecimal precioBaseMensual = membresiaBase.getPrecioTotal();
                BigDecimal precioPorDia = precioBaseMensual.divide(BigDecimal.valueOf(30), 4,
                        RoundingMode.HALF_UP);
                montoMembresia = precioPorDia.multiply(BigDecimal.valueOf(diasASumar));
            }
        } else {
            montoMembresia = membresiaBase.getPrecioTotal();
        }

        if (montoMembresia == null || montoMembresia.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No se pudo determinar un precio válido para la membresía.");
        }

        return montoMembresia;
    }

    /**
     * Procesa la notificación de webhook de Mercado Pago
     * 
     * @param payload     Payload recibido
     * @param xSignature  Header x-signature
     * @param xRequestId  Header x-request-id
     * @param dataIdParam ID de datos
     */
    @Transactional
    public void procesarNotificacionMercadoPago(Map<String, Object> payload, String xSignature,
            String xRequestId, String dataIdParam) {
        try {
            log.info("=== RECIBIENDO NOTIFICACIÓN DE WEBHOOK DE MERCADO PAGO ===");
            log.info("Payload completo recibido: {}", payload);

            String type = (String) payload.get("type");
            String action = (String) payload.get("action");

            if (type != null) {
                if (type.contains("merchant_order")) {
                    type = "merchant_order";
                } else if (type.contains("payment")) {
                    type = "payment";
                }
            }
            if (type == null && action != null) {
                if (action.startsWith("payment")) {
                    type = "payment";
                } else if (action.startsWith("merchant_order")) {
                    type = "merchant_order";
                }
            }

            log.info("Tipo de notificación procesado: {}", type);

            if (!"payment".equals(type) && !"merchant_order".equals(type)) {
                log.info("Notificación ignorada, tipo no relevante: {}", type);
                return;
            }

            String identifierStr = extraerIdentificador(payload);
            if (identifierStr == null || identifierStr.trim().isEmpty()) {
                log.warn("Notificación de webhook sin un ID válido en la raíz ni en data.");
                return;
            }
            log.info("ID extraído de la notificación: {}", identifierStr);

            String dataIdParaFirma = (dataIdParam != null && !dataIdParam.isBlank()) ? dataIdParam : identifierStr;

            if (validarFirmaHabilitada) {
                boolean firmaValida = validarFirmaWebhook(xSignature, xRequestId, dataIdParaFirma);

                if (!firmaValida) {
                    if ("payment".equals(type)) {
                        log.error("❌ FIRMA DE WEBHOOK INVÁLIDA para notificación de tipo PAYMENT. " +
                                "Se rechaza por seguridad. x-signature={}, x-request-id={}, dataId={}",
                                xSignature, xRequestId, dataIdParaFirma);
                        throw new SecurityException("Firma de webhook inválida");
                    } else {
                        log.warn("⚠️ Firma no coincide para notificación MERCHANT_ORDER. Se continúa igualmente.");
                    }
                } else {
                    log.info("✅ Firma de webhook validada correctamente.");
                }
            }

            Long paymentId = null;
            try {
                paymentId = Long.valueOf(identifierStr);
            } catch (NumberFormatException e) {
                log.warn("El ID recibido no es numérico: {}", identifierStr);
            }
            if (paymentId != null) {
                Pago pagoLocalExistente = pagoRepository.findByPaymentIdMp(String.valueOf(paymentId)).orElse(null);
                if (pagoLocalExistente != null && pagoLocalExistente.getEstado() == EnumEstadoPago.APROBADO) {
                    log.info(
                            "✅ El pago con Payment ID MP={} ya se encuentra registrado y APROBADO localmente (ID Local: {}). Webhook procesado idempotentemente.",
                            paymentId, pagoLocalExistente.getIdPago());
                    return;
                }
            }

            String externalReference = null;
            String paymentStatus = null;
            String paymentStatusDetail = null;
            MercadoPagoConfig.setAccessToken(getAccessTokenValidado());
            MPRequestOptions requestOptions = getMPRequestOptions();

            if ("payment".equals(type) && paymentId != null) {
                try {
                    log.info("Consultando API de Mercado Pago para PAYMENT ID: {}", paymentId);
                    PaymentClient paymentClient = new PaymentClient();
                    Payment mpPayment = paymentClient.get(paymentId, requestOptions);

                    if (mpPayment != null) {
                        paymentStatus = mpPayment.getStatus();
                        paymentStatusDetail = mpPayment.getStatusDetail();
                        externalReference = mpPayment.getExternalReference();
                    } else {
                        log.warn("No se encontró el pago en la API de MP con ID: {}", paymentId);
                        return;
                    }
                } catch (MPApiException apiEx) {
                    log.error("❌ Error MPApiException consultando payment ID {}: Status={}, Content={}",
                            paymentId, apiEx.getStatusCode(),
                            apiEx.getApiResponse() != null ? apiEx.getApiResponse().getContent()
                                    : "Sin contenido");
                    return;
                }
            }

            if (paymentId == null && "merchant_order".equals(type)) {
                try {
                    Long merchantOrderId = Long.valueOf(identifierStr);
                    MerchantOrderClient merchantOrderClient = new MerchantOrderClient();
                    MerchantOrder merchantOrder = merchantOrderClient.get(merchantOrderId, requestOptions);

                    if (merchantOrder != null && merchantOrder.getPayments() != null
                            && !merchantOrder.getPayments().isEmpty()) {
                        externalReference = merchantOrder.getExternalReference();
                        paymentId = merchantOrder.getPayments().get(0).getId();

                        PaymentClient paymentClient = new PaymentClient();
                        Payment mpPayment = paymentClient.get(paymentId, requestOptions);
                        if (mpPayment != null) {
                            paymentStatus = mpPayment.getStatus();
                            paymentStatusDetail = mpPayment.getStatusDetail();
                        }
                    }
                } catch (MPApiException apiEx) {
                    log.error("❌ Error consultando merchant_order: Status={}, Content={}", apiEx.getStatusCode(),
                            apiEx.getApiResponse() != null ? apiEx.getApiResponse().getContent()
                                    : "Sin contenido");
                    return;
                }
            }

            if (paymentId == null) {
                log.warn("❌ No se pudo obtener un payment_id válido de la notificación.");
                return;
            }

            Pago pago = buscarPagoLocalPendiente(externalReference, identifierStr, paymentId);

            if (pago == null) {
                log.warn("❌ No se encontró ningún pago local asociado al paymentId={}.", paymentId);
                return;
            }

            if (pago.getEstado() == EnumEstadoPago.APROBADO) {
                log.info("El pago ID: {} ya estaba APROBADO. Se ignora la notificación duplicada.", pago.getIdPago());
                return;
            }

            pago.setPaymentIdMp(String.valueOf(paymentId));
            pago.setNumeroComprobante("MP-" + paymentId);

            procesarResultadoPago(pago, paymentStatus, paymentStatusDetail);
            log.info("=== FIN PROCESAMIENTO DE WEBHOOK DE MERCADO PAGO ===");

        } catch (SecurityException se) {
            throw se;
        } catch (OptimisticLockingFailureException lockEx) {
            log.warn("⚠️ El pago ya estaba siendo procesado por otra notificación concurrente. {}",
                    lockEx.getMessage());
        } catch (Exception e) {
            log.error("❌ Error crítico procesando webhook de Mercado Pago: {}", e.getMessage(), e);
            throw new RuntimeException("Error en webhook: " + e.getMessage());
        }
    }

    /**
     * Extrae el identificador del payload de la notificación
     * 
     * @param payload Payload recibido
     * @return Identificador extraído
     */
    @SuppressWarnings("unchecked")
    private String extraerIdentificador(Map<String, Object> payload) {
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data != null && data.containsKey("id") && data.get("id") != null) {
            return String.valueOf(data.get("id"));
        }

        if (payload.containsKey("id") && payload.get("id") != null) {
            return String.valueOf(payload.get("id"));
        }

        return null;
    }

    /**
     * Busca un pago local pendiente asociado a la notificación
     * 
     * @param externalReference Referencia externa
     * @param identifierStr     Identificador
     * @param paymentId         ID de pago
     * @return Pago encontrado o null
     */
    private Pago buscarPagoLocalPendiente(String externalReference, String identifierStr, Long paymentId) {
        Pago pago = null;

        if (paymentId != null) {
            pago = pagoRepository.findByPaymentIdMp(String.valueOf(paymentId)).orElse(null);
            if (pago != null) {
                log.info("Pago encontrado por paymentIdMp. ID Pago: {}", pago.getIdPago());
                return pago;
            }
        }

        if (externalReference != null) {
            try {
                Long idSocioMembresia = Long.valueOf(externalReference);
                pago = pagoRepository
                        .findFirstBySocioMembresia_IdSocioMembresiaAndEstadoOrderByFechaPagoDesc(
                                idSocioMembresia, EnumEstadoPago.PENDIENTE)
                        .orElse(null);
                if (pago != null) {
                    log.info("Pago encontrado por externalReference. ID Pago: {}", pago.getIdPago());
                    return pago;
                }
            } catch (NumberFormatException e) {
                log.warn("externalReference no es numérico: {}", externalReference);
            }
        }

        if (identifierStr != null) {
            pago = pagoRepository.findByPreferenceIdAndEstado(identifierStr, EnumEstadoPago.PENDIENTE).orElse(null);
            if (pago != null) {
                log.info("Pago encontrado por preferenceId. ID Pago: {}", pago.getIdPago());
                return pago;
            }
        }

        return null;
    }

    /**
     * Procesa el resultado del pago según el estado recibido
     * 
     * @param pago                Pago local
     * @param paymentStatus       Estado del pago
     * @param paymentStatusDetail Detalle del estado
     */
    private void procesarResultadoPago(Pago pago, String paymentStatus, String paymentStatusDetail) {
        if ("approved".equalsIgnoreCase(paymentStatus)) {
            log.info("✅ PAGO APROBADO POR MERCADO PAGO. Actualizando pago ID: {}", pago.getIdPago());
            pago.setEstado(EnumEstadoPago.APROBADO);
            pagoRepository.save(pago);

            enviarEventoPago(pago);
            actualizarMembresiaTrasPagoAprobado(pago.getSocioMembresia());

        } else if ("rejected".equalsIgnoreCase(paymentStatus) || "cancelled".equalsIgnoreCase(paymentStatus)) {
            log.warn("❌ PAGO RECHAZADO O CANCELADO. ID Pago local: {}, Status Detail: {}", pago.getIdPago(),
                    paymentStatusDetail);
            pago.setEstado(EnumEstadoPago.RECHAZADO);
            pagoRepository.save(pago);

        } else if ("in_process".equalsIgnoreCase(paymentStatus) || "pending".equalsIgnoreCase(paymentStatus)) {
            log.info("⏳ PAGO EN PROCESO O PENDIENTE. ID Pago local: {}, Status Detail: {}",
                    pago.getIdPago(), paymentStatusDetail);
            pagoRepository.save(pago);

        } else {
            log.info("ℹ️ Estado de pago desconocido: status={}, detail={}", paymentStatus, paymentStatusDetail);
        }
    }

    /**
     * Valida la firma del webhook de Mercado Pago
     * 
     * @param xSignature Header x-signature
     * @param xRequestId Header x-request-id
     * @param dataId     ID de datos
     * @return true si la firma es válida
     */
    private boolean validarFirmaWebhook(String xSignature, String xRequestId, String dataId) {
        if (mpWebhookSecret == null || mpWebhookSecret.isBlank()) {
            log.error("MERCADOPAGO_WEBHOOK_SECRET no configurado. No se puede validar la firma del webhook.");
            return false;
        }
        if (xSignature == null || xRequestId == null || dataId == null) {
            log.warn("Faltan headers necesarios para validar la firma del webhook (x-signature/x-request-id/data.id).");
            return false;
        }

        String ts = null;
        String hashRecibido = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                if ("ts".equals(key))
                    ts = value;
                if ("v1".equals(key))
                    hashRecibido = value;
            }
        }

        if (ts == null || hashRecibido == null) {
            log.warn("Formato de x-signature inválido: {}", xSignature);
            return false;
        }

        String dataIdNormalizado = dataId.matches("\\d+") ? dataId : dataId.toLowerCase();
        String manifest = String.format("id:%s;request-id:%s;ts:%s;", dataIdNormalizado, xRequestId, ts);

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    mpWebhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hmacBytes) {
                hexBuilder.append(String.format("%02x", b));
            }
            String hmacCalculado = hexBuilder.toString();

            boolean coincide = hmacCalculado.equalsIgnoreCase(hashRecibido);
            if (!coincide) {
                log.error("Firma no coincide. Manifest usado: {}", manifest);
            }
            return coincide;
        } catch (Exception e) {
            log.error("Error calculando HMAC para validar firma del webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtiene las opciones de request para Mercado Pago
     * 
     * @return MPRequestOptions configurado
     */
    private MPRequestOptions getMPRequestOptions() {
        return MPRequestOptions.builder()
                .accessToken(getAccessTokenValidado())
                .build();
    }

    /**
     * Consulta el historial de pagos de un socio
     * 
     * @param idSocio           ID del socio
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Lista de pagos
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
                        "Acceso denegado. Solo puede consultar su propio historial.");
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
     * Filtra pagos con criterios y paginación
     * 
     * @param filtro  DTO con los filtros
     * @param userRol Rol del usuario autenticado
     * @return Página de pagos
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
                Join<SocioMembresia, Membresia> membresiaJoin = socioMembresiaJoin.join("membresia",
                        JoinType.LEFT);

                Predicate nombreSocioMatch = cb.like(
                        cb.lower(cb.concat(cb.concat(socioJoin.get("nombre"), " "), socioJoin
                                .get("apellido"))),
                        searchTerm);
                Predicate emailMatch = cb.like(cb.lower(socioJoin.get("email")), searchTerm);
                Predicate comprobanteMatch = cb.like(cb.lower(root.get("numeroComprobante")), searchTerm);
                Predicate planMatch = cb.like(cb.lower(membresiaJoin.get("nombre")), searchTerm);

                predicates.add(cb.or(nombreSocioMatch, emailMatch, comprobanteMatch, planMatch));
            }

            if (filtro.getIdSocio() != null) {
                predicates.add(cb.equal(root.get("socioMembresia").get("socio").get("idUsuario"), filtro
                        .getIdSocio()));
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
     * @param requestDTO Datos de la anulación
     * @param userRol    Rol del usuario autenticado
     * @return Mensaje de confirmación
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
     * @param idPago            ID del pago
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return DTO del pago
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
     * Genera el comprobante PDF de un pago
     * 
     * @param idPago            ID del pago
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     * @return Array de bytes del PDF
     */
    @Transactional(readOnly = true)
    public byte[] generarComprobantePDF(Long idPago, String userRol, Long userIdAutenticado, String userEmail) {
        PagoResponseDTO pagoDTO = generarComprobante(idPago, userRol, userIdAutenticado, userEmail);
        return pagoPDFService.generarComprobantePDF(pagoDTO);
    }

    /**
     * Obtiene un pago por ID
     * 
     * @param idPago ID del pago
     * @return Pago encontrado
     */
    @Transactional(readOnly = true)
    public Pago obtenerPagoPorId(Long idPago) {
        return pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + idPago));
    }

    /**
     * Envía un evento de pago de forma asíncrona
     * 
     * @param pago Pago registrado
     */
    private void enviarEventoPago(Pago pago) {
        try {
            EventoPagoRequestDTO dto = new EventoPagoRequestDTO();
            dto.setSocioId(pago.getSocioMembresia().getSocio().getIdUsuario());
            dto.setMonto(pago.getMonto());
            dto.setFechaPago(pago.getFechaPago());
            dto.setTipoMembresia(pago.getSocioMembresia().getMembresia().getNombre());
            dto.setMetodoPago(pago.getMetodoPago().name());
            eventoPagoAsyncService.enviarEventoPago(dto);
        } catch (Exception e) {
            log.warn("Error al enviar evento de pago de forma asíncrona: {}", e.getMessage());
        }
    }

    /**
     * Obtiene el resumen de pagos para el dashboard
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

    /**
     * Obtiene el dashboard de pagos de un socio
     * 
     * @param userEmail Email del socio
     * @return DTO con el dashboard de pagos
     */
    @Transactional(readOnly = true)
    public SocioDashboardPagosDTO obtenerDashboardPagosSocio(String userEmail) {
        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        List<SocioMembresia> membresias = socioMembresiaRepository.findBySocio_IdUsuario(socio.getIdUsuario());

        SocioMembresia membresiaActualEntity = membresias.stream()
                .filter(m -> m.getEstado() == EnumEstadoSocioMembresia.ACTIVA)
                .findFirst()
                .orElse(membresias.isEmpty() ? null : membresias.get(0));

        SocioDashboardPagosDTO.SocioMembresiaResumenDTO membresiaResumen = null;
        if (membresiaActualEntity != null) {
            membresiaResumen = new SocioDashboardPagosDTO.SocioMembresiaResumenDTO();
            membresiaResumen.setIdSocioMembresia(membresiaActualEntity.getIdSocioMembresia());
            if (membresiaActualEntity.getMembresia() != null) {
                membresiaResumen.setIdMembresia(membresiaActualEntity.getMembresia().getIdMembresia());
                membresiaResumen.setNombreMembresia(membresiaActualEntity.getMembresia().getNombre());
                BigDecimal precioFinal = membresiaActualEntity.getPrecioReal() != null
                        ? membresiaActualEntity.getPrecioReal()
                        : membresiaActualEntity.getMembresia().getPrecioTotal();
                membresiaResumen.setPrecioReal(precioFinal);
            }
            membresiaResumen.setFechaInicio(membresiaActualEntity.getFechaInicio());
            membresiaResumen.setFechaVencimiento(membresiaActualEntity.getFechaVencimiento());
            membresiaResumen.setEstado(
                    membresiaActualEntity.getEstado() != null ? membresiaActualEntity.getEstado().name()
                            : null);
            membresiaResumen.setCantidadDias(membresiaActualEntity.getCantidadDias());
        }

        List<Pago> pagos = pagoRepository.findBySocioId(socio.getIdUsuario());
        List<PagoResponseDTO> historialPagosDTO = pagos.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());

        SocioDashboardPagosDTO responseDTO = new SocioDashboardPagosDTO();
        responseDTO.setSocioId(socio.getIdUsuario());
        responseDTO.setNombreSocio(socio.getNombre() + " " + socio.getApellido());
        responseDTO.setEmailSocio(socio.getEmail());
        responseDTO.setMembresiaActual(membresiaResumen);
        responseDTO.setHistorialPagos(historialPagosDTO);

        return responseDTO;
    }

    /**
     * Genera el comprobante PDF de un pago perteneciente al socio autenticado por
     * su email
     * 
     * @param idPago    ID del pago
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del socio autenticado
     * @return Array de bytes del PDF del comprobante
     */
    @Transactional(readOnly = true)
    public byte[] generarComprobantePDFPropio(Long idPago, String userRol, String userEmail) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + idPago));

        UsuarioPerfil socioPago = pago.getSocioMembresia().getSocio();
        if (!socio.getIdUsuario().equals(socioPago.getIdUsuario())) {
            log.warn(
                    "ACCESO DENEGADO: El socio ID {} intentó descargar el comprobante ID {} perteneciente al socio ID {}",
                    socio.getIdUsuario(), idPago, socioPago.getIdUsuario());
            throw new SecurityAuthorizationException("Acceso denegado. No puedes descargar comprobantes ajenos.");
        }

        PagoResponseDTO pagoDTO = convertirAResponseDTO(pago);
        return pagoPDFService.generarComprobantePDF(pagoDTO);
    }

    /**
     * Filtra y pagina los pagos de un socio o de todo el sistema según el rol y
     * filtros
     * 
     * @param filtro    DTO con los filtros (estado, metodoPago, referencia/search,
     *                  fechas, paginación)
     * @param userRol   Rol del usuario autenticado
     * @param userEmail Email del usuario autenticado
     * @return Página de pagos filtrados
     */
    @Transactional(readOnly = true)
    public Page<PagoResponseDTO> filtrarPagosSocioPaginados(FiltroPagosRequestDTO filtro, String userRol,
            String userEmail) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        // Si es socio, forzamos que solo pueda ver sus propios pagos por seguridad
        if (userRol.equalsIgnoreCase(EnumRol.socio.name())) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));
            filtro.setIdSocio(socio.getIdUsuario());
        }

        Pageable pageable = PageRequest.of(
                filtro.getPage(),
                filtro.getSize(),
                Sort.by("fechaPago").descending());

        Specification<Pago> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filtro.getSearch() != null && !filtro.getSearch().trim().isEmpty()) {
                String searchTerm = "%" + filtro.getSearch().trim().toLowerCase() + "%";

                Join<Pago, SocioMembresia> socioMembresiaJoin = root.join("socioMembresia", JoinType.LEFT);
                Join<SocioMembresia, UsuarioPerfil> socioJoin = socioMembresiaJoin.join("socio", JoinType.LEFT);
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

            if (filtro.getMetodoPago() != null && !filtro.getMetodoPago().equalsIgnoreCase("TODOS")
                    && !filtro.getMetodoPago().isBlank()) {
                try {
                    EnumMetodoPago metodo = EnumMetodoPago.valueOf(filtro.getMetodoPago().toUpperCase());
                    predicates.add(cb.equal(root.get("metodoPago"), metodo));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Filtro por Estado
            if (filtro.getEstado() != null && !filtro.getEstado().equalsIgnoreCase("TODOS")
                    && !filtro.getEstado().isBlank()) {
                try {
                    EnumEstadoPago estadoEnum = EnumEstadoPago.valueOf(filtro.getEstado().toUpperCase());
                    predicates.add(cb.equal(root.get("estado"), estadoEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Filtro por Rango de Fechas
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
}