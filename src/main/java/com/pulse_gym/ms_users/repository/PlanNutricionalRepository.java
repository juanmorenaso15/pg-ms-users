package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse_gym.lb_common.entity.user.PlanNutricionalIA;

public interface PlanNutricionalRepository extends JpaRepository<PlanNutricionalIA, Long> {

    /**
     * Busca todos los planes nutricionales de un socio ordenados por fecha
     * descendente
     * 
     * @param idSocio ID del socio
     * @return Lista de planes nutricionales ordenados
     */
    List<PlanNutricionalIA> findBySocio_IdUsuarioOrderByFechaGeneracionDesc(Long idSocio);

    /**
     * Busca el plan nutricional activo de un socio
     * 
     * @param idSocio ID del socio
     * @return Plan nutricional activo
     */
    Optional<PlanNutricionalIA> findBySocio_IdUsuarioAndActivoTrue(Long idSocio);

    /**
     * Busca todos los planes nutricionales activos de un socio ordenados por fecha
     * descendente
     * 
     * @param idSocio ID del socio
     * @return Lista de planes nutricionales activos
     */
    List<PlanNutricionalIA> findBySocio_IdUsuarioAndActivoTrueOrderByFechaGeneracionDesc(Long idSocio);
}