package com.pulse_gym.ms_users.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.AnularPagoRequestDTO;
import com.pulse_gym.lb_common.dto.FiltroPagosRequestDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PagoResponseDTO;
import com.pulse_gym.lb_common.dto.PagoResultResponseDTO;
import com.pulse_gym.lb_common.dto.PaymentSummaryDTO;
import com.pulse_gym.lb_common.dto.PreferenceResponseDTO;
import com.pulse_gym.lb_common.dto.RegistrarPagoRequestDTO;
import com.pulse_gym.lb_common.dto.SocioDashboardPagosDTO;
import com.pulse_gym.lb_common.dto.TokenizedPaymentRequestDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.service.PagoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    /** Servicio para operaciones con pagos */
    private final PagoService pagoService;

    /**
     * Endpoint para registrar un nuevo pago de una membresía.
     * 
     * @param requestDTO        DTO con los datos del pago (idSocioMembresia, monto,
     *                          metodoPago, etc.)
     * @param userRol           Rol del usuario autenticado - header "X-User-Rol"
     * @param userIdAutenticado ID del usuario que registra el pago - header
     *                          "X-User-Id"
     * @return Mensaje de confirmación con código HTTP 201 (Created)
     */
    @PostMapping("/registrar")
    public ResponseEntity<MessegeGlobalDTO> registrarPago(
            @Valid @RequestBody RegistrarPagoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            MessegeGlobalDTO response = pagoService.registrarPago(requestDTO, userRol, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al registrar pago", e);
        }
    }

    /**
     * Endpoint para que un socio inicie un pago de membresía desde la aplicación
     * móvil.
     * 
     * @param requestDTO DTO con los datos del pago (idSocioMembresia, metodoPago)
     * @param userRol    Rol del usuario autenticado - header "X-User-Rol" (debe ser
     *                   socio)
     * @param userEmail  Email del socio autenticado - header "X-User-Email"
     * @return DTO con ID de preferencia y URL de pago de MercadoPago (código 200)
     *         o mensaje de error (código 400 o 500)
     */
    @PostMapping("/pago-app")
    public ResponseEntity<?> realizarPagoApp(
            @RequestBody RegistrarPagoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol") String userRol,
            @RequestHeader(value = "X-User-Email") String userEmail) {
        try {
            PreferenceResponseDTO response = pagoService.iniciarPagoMembresiaApp(requestDTO, userRol, userEmail);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Consulta el historial de pagos de un socio
     * 
     * @param idSocio           ID del socio a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return Lista de pagos del socio
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     * @throws ResponseStatusException        Si ocurre un error interno
     */
    @GetMapping("/socio/{idSocio}")
    public ResponseEntity<List<PagoResponseDTO>> consultarHistorialPagos(
            @PathVariable Long idSocio,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            List<PagoResponseDTO> pagos = pagoService.consultarHistorialPagos(
                    idSocio, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(pagos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar historial de pagos", e);
        }
    }

    /**
     * Filtra pagos aplicando criterios de búsqueda
     * 
     * @param filtro  DTO con los filtros a aplicar
     * @param userRol Rol del usuario autenticado (header)
     * @return Lista de pagos que coinciden con los filtros
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     * @throws ResponseStatusException        Si ocurre un error interno
     */
    @PostMapping("/filtrar-paginado")
    public ResponseEntity<Page<PagoResponseDTO>> filtrarPagosPaginados(
            @RequestBody FiltroPagosRequestDTO filtro,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            Page<PagoResponseDTO> pagos = pagoService.filtrarPagosPaginados(filtro, userRol);
            return ResponseEntity.ok(pagos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al filtrar pagos paginados", e);
        }
    }

    /**
     * Anula un pago existente
     * 
     * @param requestDTO DTO con el ID del pago y motivo de anulación
     * @param userRol    Rol del usuario autenticado (header)
     * @return Mensaje de confirmación de la anulación
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     * @throws ResponseStatusException        Si ocurre un error interno
     */
    @PutMapping("/anular")
    public ResponseEntity<MessegeGlobalDTO> anularPago(
            @Valid @RequestBody AnularPagoRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = pagoService.anularPago(requestDTO, userRol);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al anular pago", e);
        }
    }

    /**
     * Genera el comprobante de un pago
     * 
     * @param idPago            ID del pago a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return DTO con los datos del pago
     */
    @GetMapping("/comprobante/{idPago}")
    public ResponseEntity<PagoResponseDTO> generarComprobante(
            @PathVariable Long idPago,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            PagoResponseDTO comprobante = pagoService.generarComprobante(
                    idPago, userRol, userIdAutenticado, userEmail);
            return ResponseEntity.ok(comprobante);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar comprobante", e);
        }
    }

    /**
     * Genera y descarga un comprobante de pago en formato PDF
     * 
     * @param idPago            ID del pago a consultar
     * @param userRol           Rol del usuario autenticado (header)
     * @param userIdAutenticado ID del usuario autenticado (header)
     * @param userEmail         Email del usuario autenticado (header)
     * @return Archivo PDF del comprobante
     */
    @GetMapping(value = "/comprobante/{idPago}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> generarComprobantePDF(
            @PathVariable Long idPago,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            byte[] pdfBytes = pagoService.generarComprobantePDF(
                    idPago, userRol, userIdAutenticado, userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "comprobante-pago-" + idPago + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar el comprobante PDF", e);
        }
    }

    /**
     * Obtiene el resumen de pagos para el dashboard
     * 
     * @param userRol Rol del usuario autenticado (header)
     * @return DTO con el resumen de pagos
     */
    @GetMapping("/resumen")
    public ResponseEntity<PaymentSummaryDTO> obtenerResumenPagos(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            ValidacionDeRoles.validarAdminORecepcionista(userRol);
            PaymentSummaryDTO resumen = pagoService.obtenerResumenPagos();
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al obtener resumen de pagos", e);
        }
    }

    /**
     * Endpoint para recibir notificaciones de pagos desde MercadoPago (webhook).
     * 
     * @param payload Mapa con los datos de la notificación enviada por MercadoPago.
     * @return ResponseEntity con código HTTP 200 si se procesa correctamente, o 500
     *         en caso de error.
     */
    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> recibirWebhookMercadoPago(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdParam) {
        try {
            pagoService.procesarNotificacionMercadoPago(payload, xSignature, xRequestId, dataIdParam);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            // Firma inválida: no reintentar en bucle, pero tampoco des-configurar tu app
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para procesar un pago tokenizado desde el Payment Brick
     * (Checkout API). El usuario nunca sale de nuestro dominio.
     *
     * @param requestDTO DTO con el token de la tarjeta, método de pago, cuotas y
     *                   datos del pagador
     * @param userRol    Rol del usuario autenticado - header "X-User-Rol"
     * @param userEmail  Email del socio autenticado - header "X-User-Email"
     * @return DTO con el resultado inmediato del pago (200) o error (400/500)
     */
    @PostMapping("/pago-app-token")
    public ResponseEntity<?> realizarPagoAppConToken(
            @Valid @RequestBody TokenizedPaymentRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol") String userRol,
            @RequestHeader(value = "X-User-Email") String userEmail) {
        try {
            PagoResultResponseDTO response = pagoService.procesarPagoConTokenApp(requestDTO, userRol, userEmail);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error de validación: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Endpoint para que un socio obtenga su historial de pagos desde la app móvil.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol  Rol del usuario autenticado (header)
     * @param userEmail Email del socio autenticado (header)
     * @return DTO con el historial de pagos del socio
     */
    @GetMapping("/mi-historial-pagos")
    public ResponseEntity<?> obtenerMiHistorialYPagos(
            @RequestHeader(value = "X-User-Rol") String userRol,
            @RequestHeader(value = "X-User-Email") String userEmail) {
        try {
            ValidacionDeRoles.validarCualquierRol(userRol);
            SocioDashboardPagosDTO response = pagoService.obtenerDashboardPagosSocio(userEmail);
            return ResponseEntity.ok(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener la información de pagos: " + e.getMessage());
        }
    }

    /**
     * Endpoint para que un socio descargue el comprobante PDF de su propio pago
     * validando su identidad mediante el token (email).
     * 
     * @param idPago    ID del pago
     * @param userRol   Rol del usuario autenticado (header "X-User-Rol")
     * @param userEmail Email del socio autenticado (header "X-User-Email")
     * @return Archivo PDF del comprobante
     */
    @GetMapping(value = "/mi-comprobante/{idPago}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> generarMiComprobantePDF(
            @PathVariable Long idPago,
            @RequestHeader(value = "X-User-Rol") String userRol,
            @RequestHeader(value = "X-User-Email") String userEmail) {
        try {
            byte[] pdfBytes = pagoService.generarComprobantePDFPropio(idPago, userRol, userEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "mi-comprobante-pago-" + idPago + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al generar tu comprobante PDF", e);
        }
    }

    /**
     * Endpoint para filtrar, buscar por referencia/estado/tipo de pago/fechas y paginar 
     * los pagos (aplicable para vistas de socios o administración).
     * 
     * @param filtro    DTO con los criterios de filtrado y paginación
     * @param userRol   Rol del usuario autenticado (header "X-User-Rol")
     * @param userEmail Email del usuario autenticado (header "X-User-Email")
     * @return Página con los pagos filtrados
     */
    @PostMapping("/mis-pagos/filtrar-paginado")
    public ResponseEntity<Page<PagoResponseDTO>> filtrarMisPagosPaginados(
            @RequestBody FiltroPagosRequestDTO filtro,
            @RequestHeader(value = "X-User-Rol") String userRol,
            @RequestHeader(value = "X-User-Email") String userEmail) {
        try {
            Page<PagoResponseDTO> pagos = pagoService.filtrarPagosSocioPaginados(filtro, userRol, userEmail);
            return ResponseEntity.ok(pagos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error al filtrar los pagos paginados", e);
        }
    }
}
