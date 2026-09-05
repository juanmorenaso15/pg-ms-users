package com.pulse_gym.ms_users.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.EquipoClient;
import com.pulse_gym.lb_common.client.ReportesClient;
import com.pulse_gym.lb_common.dto.DashboardResumenDTO;
import com.pulse_gym.lb_common.dto.MembresiaPorVencerDTO;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardAdminService {

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Servicio de membresías de socios */
    private final SocioMembresiaService socioMembresiaService;

    /** Cliente Feign para reportes */
    private final ReportesClient reportesClient;

    /** Cliente Feign para equipos */
    private final EquipoClient equipoClient;

    /**
     * Obtiene el resumen del dashboard para administradores, entrenadores y
     * recepcionistas
     * 
     * @param userRol Rol del usuario autenticado
     * @return DTO con el resumen del dashboard
     */
    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerResumenDashboard(String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        long totalUsuarios = usuarioRepository.count();
        long activos = usuarioRepository.countByEstado(EnumEstadoUsuario.ACTIVO);
        long inactivos = usuarioRepository.countByEstado(EnumEstadoUsuario.INACTIVO);

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long nuevosDelMes = usuarioRepository.countNuevosDesde(inicioMes);

        List<MembresiaPorVencerDTO> porVencer = socioMembresiaService.obtenerMembresiasPorVencer();

        LocalDate ayer = LocalDate.now().minusDays(1);

        var afluenciaHoy = reportesClient.obtenerAfluenciaHoy(userRol);
        var afluenciaAyer = reportesClient.obtenerAfluenciaPorDia(ayer);
        var ingresos = reportesClient.obtenerIngresosUltimosSeisMeses();
        Integer equiposMantenimiento = equipoClient.obtenerConteoPorEstado("MANTENIMIENTO");

        return DashboardResumenDTO.builder()
                .totalUsuarios(totalUsuarios)
                .usuariosActivos(activos)
                .usuariosInactivos(inactivos)
                .nuevosDelMes(nuevosDelMes)
                .membresiasPorVencer(porVencer)
                .afluenciaHoy(afluenciaHoy)
                .afluenciaAyer(afluenciaAyer)
                .ingresosSeisMeses(ingresos)
                .equiposEnMantenimiento(equiposMantenimiento)
                .build();
    }
}