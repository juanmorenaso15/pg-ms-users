package com.pulse_gym.ms_users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulse_gym.lb_common.dto.EstadoMembresiaResponseDTO;
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
}