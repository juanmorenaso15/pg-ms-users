package com.pulse_gym.ms_users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pulse_gym.lb_common.dto.DocumentoLegalRequestDTO;
import com.pulse_gym.lb_common.dto.DocumentoLegalResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.DocumentoLegalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios/documentos")
@RequiredArgsConstructor
public class DocumentoLegalController {

    /** Servicio de documentos legales */
    private final DocumentoLegalService documentoLegalService;

    /**
     * Carga un documento legal para un usuario específico.
     * 
     * @param requestDTO Los datos del documento legal a cargar, incluyendo el ID
     *                   del usuario, el tipo de documento y la URL del archivo
     *                   firmado
     * @param userRol    El rol del usuario que realiza la solicitud, obtenido del
     *                   encabezado "X-User-Rol"
     * @return
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> cargarDocumentoLegal(
            @Valid @RequestBody DocumentoLegalRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = documentoLegalService.cargarDocumentoLegal(requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Consulta todos los documentos legales vigentes.
     * 
     * @param userRol El rol del usuario que realiza la consulta
     * @return Una lista de documentos legales vigentes
     */
    @GetMapping()
    public ResponseEntity<List<DocumentoLegalResponseDTO>> consultarTodosLosDocumentosLegales(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<DocumentoLegalResponseDTO> documentos = documentoLegalService
                    .consultarTodosLosDocumentosLegales(userRol);
            return ResponseEntity.status(HttpStatus.OK).body(documentos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar documentos", e);
        }
    }

    /**
     * Consulta los documentos legales vigentes de un usuario específico. Los
     * usuarios con rol SOCIO solo pueden consultar sus propios documentos, mientras
     * que los usuarios con rol ADMIN o RECEPCIONISTA pueden consultar los
     * documentos de cualquier usuario.
     * 
     * @param idUsuario         El ID del usuario cuyos documentos legales se desean
     *                          consultar
     * @param userRol           El rol del usuario que realiza la consulta, obtenido
     *                          del encabezado "X-User-Rol"
     * @param userIdAutenticado El ID del usuario autenticado
     * @return Una lista de documentos legales vigentes asociados al usuario
     *         especificado
     */
    @GetMapping("/socio/{idUsuario}")
    public ResponseEntity<List<DocumentoLegalResponseDTO>> consultarDocumentosLegales(
            @PathVariable Long idUsuario,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            List<DocumentoLegalResponseDTO> documentos = documentoLegalService
                    .consultarDocumentosLegales(idUsuario, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.OK).body(documentos);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar documentos", e);
        }
    }

    /**
     * Elimina un documento legal vigente. Solo los usuarios con rol ADMIN o
     * RECEPCIONISTA pueden realizar esta acción.
     * 
     * @param idDocumento El ID del documento legal a eliminar
     * @param userRol     El rol del usuario que realiza la solicitud
     * @return Un mensaje global indicando el resultado de la operación
     */
    @DeleteMapping("/{idDocumento}")
    public ResponseEntity<MessegeGlobalDTO> eliminarDocumentoLegal(
            @PathVariable Long idDocumento,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = documentoLegalService.eliminarDocumentoLegal(idDocumento, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar documento", e);
        }
    }

    /**
     * Verifica si un usuario tiene un consentimiento informado vigente.
     * 
     * @param idUsuario         El ID del usuario para el cual se verifica el
     *                          consentimiento informado
     * @param userRol           El rol del usuario que realiza la consulta
     * @param userIdAutenticado El ID del usuario autenticado
     * @return true si el usuario tiene un consentimiento informado vigente, false
     *         en caso contrario
     */
    @GetMapping("/consentimiento/{idUsuario}")
    public ResponseEntity<Boolean> tieneConsentimientoDatosSensibles(
            @PathVariable Long idUsuario,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol,
            @RequestHeader(value = "X-User-Id", required = false) Long userIdAutenticado) {
        try {
            Boolean tieneConsentimiento = documentoLegalService
                    .tieneConsentimientoDatosSensibles(idUsuario, userRol, userIdAutenticado);
            return ResponseEntity.status(HttpStatus.OK).body(tieneConsentimiento);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al verificar consentimiento", e);
        }
    }
}