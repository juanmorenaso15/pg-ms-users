package com.pulse_gym.ms_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse_gym.lb_common.entity.user.HistorialPlanNutricionalVersion;

public interface HistorialPlanNutricionalVersionRepository
        extends JpaRepository<HistorialPlanNutricionalVersion, Long> {

    /**
     * Obtiene el historial de versiones de un plan nutricional específico, ordenado por versión descendente.
     * @param idPlan El ID del plan nutricional para el cual se desea obtener el historial.
     * @return Una lista de objetos HistorialPlanNutricionalVersion correspondientes al plan nutricional especificado, ordenados por versión descendente.
     */
    List<HistorialPlanNutricionalVersion> findByPlanNutricional_IdPlanNutricionalOrderByVersionDesc(Long idPlan);
}