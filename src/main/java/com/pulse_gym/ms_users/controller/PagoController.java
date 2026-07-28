package com.pulse_gym.ms_users.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.AnularPagoRequestDTO;
import com.pulse_gym.lb_common.dto.FiltroPagosRequestDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.PagoResponseDTO;
import com.pulse_gym.lb_common.dto.PreferenceResponseDTO;
import com.pulse_gym.lb_common.dto.RegistrarPagoRequestDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
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
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            MessegeGlobalDTO response = pagoService.registrarPago(requestDTO, userRol, userIdAutenticado);
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
    @PostMapping("/filtrar")
    public ResponseEntity<List<PagoResponseDTO>> filtrarPagos(
            @RequestBody FiltroPagosRequestDTO filtro,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<PagoResponseDTO> pagos = pagoService.filtrarPagos(filtro, userRol);
            return ResponseEntity.ok(pagos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al filtrar pagos", e);
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

    
}
