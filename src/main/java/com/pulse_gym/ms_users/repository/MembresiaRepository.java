package com.pulse_gym.ms_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse_gym.lb_common.entity.user.Membresia;
import com.pulse_gym.lb_common.enums.EnumTipoDuracion;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
    
    /**
     * Obtiene todas las membresías activas.
     * @return Lista de membresías activas.
     */
    List<Membresia> findByActivoTrue();
    
    /**
     * Obtiene las membresías activas que incluyen IA.
     * @param incluyeIA Indica si la membresía incluye IA.
     * @return Lista de membresías activas que incluyen IA.
     */
    List<Membresia> findByActivoTrueAndIncluyeIA(Boolean incluyeIA);
    
    /**
     * Obtiene las membresías activas que son flexibles.
     * @param esFlexible Indica si la membresía es flexible.
     * @return Lista de membresías activas que son flexibles.
     */
    List<Membresia> findByActivoTrueAndEsFlexible(Boolean esFlexible);
    
    /**
     * Verifica si existe una membresía activa con el nombre especificado.
     * @param nombre El nombre de la membresía a verificar.
     * @return true si existe una membresía activa con el nombre, false en caso contrario.
     */
    boolean existsByNombreAndActivoTrue(String nombre);
    
    /**
     * Obtiene las membresías activas que tienen un tipo de duración específico.
     * @param tipoDuracion El tipo de duración de la membresía.
     * @return Lista de membresías activas con el tipo de duración especificado.
     */
    List<Membresia> findByActivoTrueAndTipoDuracion(EnumTipoDuracion tipoDuracion);
}