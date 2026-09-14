package com.pulse_gym.ms_users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pulse_gym.lb_common.dto.DashboardEntrenadorDTO;
import com.pulse_gym.ms_users.service.DashboardEntrenadorService;

@RestController
@RequestMapping("/api/v1/dashboard/entrenador")
@RequiredArgsConstructor
public class DashboardEntrenadorController {
private final DashboardEntrenadorService dashboardEntrenadorService;

    @GetMapping("/{entrenadorId}")
    public ResponseEntity<DashboardEntrenadorDTO> obtenerDashboard(
            @PathVariable Long entrenadorId,
            @RequestParam(required = false) Long socioIdSeleccionado) {
        
        DashboardEntrenadorDTO dashboard = dashboardEntrenadorService.obtenerDashboardEntrenador(entrenadorId, socioIdSeleccionado);
        return ResponseEntity.ok(dashboard);
    }
}
