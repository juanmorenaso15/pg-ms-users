package com.pulse_gym.ms_users.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.SocioMoraDTO;
import com.pulse_gym.ms_users.service.SocioMembresiaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/socios-membresias")
@RequiredArgsConstructor
public class SocioMembresiaInternoController {

    private final SocioMembresiaService socioMembresiaService;

    /**
     * RF14: Consultar estado de membresía para acceso biométrico
     * Endpoint interno para el microservicio de acceso biométrico
     * 
     * @param idSocio ID del socio a consultar
     * @return Estado de la membresía del socio
     */
    @GetMapping("/biometrico/{idSocio}")
    public ResponseEntity<EstadoMembresiaResponseDTO> consultarEstadoBiometrico(@PathVariable Long idSocio) {
        EstadoMembresiaResponseDTO estado = socioMembresiaService.consultarEstadoMembresiaBiometrico(idSocio);
        return ResponseEntity.ok(estado);
    }

     /**
     * Obtiene la lista de socios en mora (membresía vencida o suspendida)
     * Filtro opcional por rango de fechas (fechaVencimiento).
     */
    @GetMapping("/mora")
    public List<SocioMoraDTO> obtenerSociosEnMora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return socioMembresiaService.obtenerSociosEnMora(fechaInicio, fechaFin);
    }
}