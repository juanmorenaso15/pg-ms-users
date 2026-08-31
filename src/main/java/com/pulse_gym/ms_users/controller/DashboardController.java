package com.pulse_gym.ms_users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulse_gym.lb_common.dto.DashboardResumenDTO;
import com.pulse_gym.ms_users.service.DashboardAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /** Servicio de dashboard para administradores */
    private final DashboardAdminService dashboardAdminService;

    /**
     * Obtiene el resumen general del dashboard
     * 
     * @param userRol Rol del usuario autenticado (header)
     * @return DTO con el resumen del dashboard
     */
    @GetMapping("/resumen")
    public ResponseEntity<DashboardResumenDTO> obtenerResumenGeneral(
            @RequestHeader(value = "X-User-Rol", required = false) String userRol) {

        DashboardResumenDTO resumen = dashboardAdminService.obtenerResumenDashboard(userRol);
        return ResponseEntity.ok(resumen);
    }
}