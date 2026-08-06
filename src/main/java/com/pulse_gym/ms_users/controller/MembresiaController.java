package com.pulse_gym.ms_users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.pulse_gym.lb_common.dto.CalculoMembresiaFlexibleDTO;
import com.pulse_gym.lb_common.dto.MembresiaConSociosDTO;
import com.pulse_gym.lb_common.dto.MembresiaFlexibleCalculadaDTO;
import com.pulse_gym.lb_common.dto.MembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.MembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.service.MembresiaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/membresias")
@RequiredArgsConstructor
public class MembresiaController {

    /** El servicio de membresías */
    private final MembresiaService membresiaService;

    /**
     * Endpoint para crear una nueva membresía
     * 
     * @param requestDTO Los datos para crear la membresía
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   header "X-User-Rol")
     * @return Un mensaje global con la información de la membresía creada
     */
    @PostMapping
    public ResponseEntity<MessegeGlobalDTO> crearMembresia(
            @Valid @RequestBody MembresiaRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = membresiaService.crearMembresia(requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear membresía", e);
        }
    }

    /**
     * Endpoint para consultar las membresías
     * 
     * @param incluyeIA  Indica si se deben mostrar solo las membresías que incluyen
     *                   IA
     * @param esFlexible Indica si se deben mostrar solo las membresías flexibles
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   header "X-User-Rol")
     * @return Una lista con las membresías que cumplen con los criterios de
     *         búsqueda
     */
    @GetMapping
    public ResponseEntity<List<MembresiaResponseDTO>> consultarMembresias(
            @RequestParam(required = false) Boolean incluyeIA,
            @RequestParam(required = false) Boolean esFlexible,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<MembresiaResponseDTO> membresias = membresiaService.consultarMembresias(userRol, incluyeIA,
                    esFlexible);
            return ResponseEntity.status(HttpStatus.OK).body(membresias);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al consultar membresías", e);
        }
    }

    /**
     * Endpoint para actualizar una membresía existente
     * 
     * @param id         El ID de la membresía a actualizar
     * @param requestDTO Los datos para actualizar la membresía
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   header "X-User-Rol")
     * @return Un mensaje global con la información de la membresía actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessegeGlobalDTO> actualizarMembresia(
            @PathVariable Long id,
            @Valid @RequestBody MembresiaRequestDTO requestDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = membresiaService.actualizarMembresia(id, requestDTO, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al actualizar membresía", e);
        }
    }

    /**
     * Endpoint para eliminar una membresía existente
     * 
     * @param id      El ID de la membresía a eliminar
     * @param userRol El rol del usuario que realiza la acción (obtenido del header
     *                "X-User-Rol")
     * @return Un mensaje global con la información de la membresía eliminada
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessegeGlobalDTO> eliminarMembresia(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MessegeGlobalDTO response = membresiaService.eliminarMembresia(id, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar membresía", e);
        }
    }

    @GetMapping("/categoria")
    public ResponseEntity<List<MembresiaResponseDTO>> obtenerMembresiasPorCategoria(
            @RequestParam Boolean incluyeIA,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            List<MembresiaResponseDTO> membresias = membresiaService.obtenerMembresiasPorCategoria(incluyeIA, userRol);
            return ResponseEntity.status(HttpStatus.OK).body(membresias);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar membresías por categoría", e);
        }
    }

    /**
     * Endpoint para calcular el precio total de una membresía flexible basada en la
     * cantidad de días y la categoría de IA
     * 
     * @param calculoDTO Los datos necesarios para realizar el cálculo de la
     *                   membresía flexible, incluyendo el ID de la membresía, la
     *                   cantidad de días y si incluye o no IA
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   header "X-User-Rol")
     * @return Un DTO con la información de la membresía flexible calculada,
     *         incluyendo el precio total basado en los días y la categoría de IA
     */
    @PostMapping("/calcular-flexible")
    public ResponseEntity<MembresiaFlexibleCalculadaDTO> calcularMembresiaFlexible(
            @Valid @RequestBody CalculoMembresiaFlexibleDTO calculoDTO,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {
        try {
            MembresiaFlexibleCalculadaDTO resultado = membresiaService.calcularMembresiaFlexible(calculoDTO, userRol);
            return ResponseEntity.ok(resultado);
        } catch (SecurityAuthorizationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al calcular membresía flexible",
                    e);
        }
    }

    /**
     * Consulta una membresía específica con sus socios asignados
     * 
     * @param idMembresia ID de la membresía a consultar
     * @param userRol     Rol del usuario autenticado (X-User-Rol)
     * @return DTO con la membresía y sus socios asignados
     */
    @GetMapping("/{idMembresia}/socios")
    public ResponseEntity<MembresiaConSociosDTO> consultarMembresiaConSocios(
            @PathVariable Long idMembresia,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {

        try {
            MembresiaConSociosDTO resultado = membresiaService.consultarMembresiaConSocios(
                    idMembresia, userRol);
            return ResponseEntity.ok(resultado);

        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrada")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar la membresía con sus socios",
                    e);
        }
    }

    /**
     * Consulta una membresía específica con sus socios activos
     * 
     * @param idMembresia ID de la membresía a consultar
     * @param userRol     Rol del usuario autenticado (X-User-Rol)
     * @return DTO con la membresía y sus socios activos
     */
    @GetMapping("/{idMembresia}/socios-activos")
    public ResponseEntity<MembresiaConSociosDTO> consultarMembresiaConSociosActivos(
            @PathVariable Long idMembresia,
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {

        try {
            MembresiaConSociosDTO resultado = membresiaService.consultarMembresiaConSociosActivos(
                    idMembresia, userRol);
            return ResponseEntity.ok(resultado);

        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("no encontrada")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar la membresía con sus socios activos",
                    e);
        }
    }

    /**
     * Lista todas las membresías con sus socios asignados
     * 
     * @param userRol Rol del usuario autenticado (X-User-Rol)
     * @return Lista de DTOs con membresías y sus socios
     */
    @GetMapping("/todos-con-socios")
    public ResponseEntity<List<MembresiaConSociosDTO>> consultarTodasMembresiasConSocios(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {

        try {
            List<MembresiaConSociosDTO> resultados = membresiaService.consultarTodasMembresiasConSocios(userRol);
            return ResponseEntity.ok(resultados);

        } catch (SecurityAuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al consultar las membresías con sus socios",
                    e);
        }
    }

}
