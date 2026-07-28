package com.pulse_gym.ms_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.entity.user.DetalleRutina;

public interface DetalleRutinaRepository extends JpaRepository<DetalleRutina, Long> {

    /**
     * Busca detalles de una rutina ordenados por día y orden
     * 
     * @param idRutina ID de la rutina
     * @return Lista de detalles ordenados
     */
    List<DetalleRutina> findByRutinaIa_IdRutinaIaOrderByDiaSemanaAscOrdenAsc(Long idRutina);

    /**
     * Busca detalles de una rutina por día de la semana
     * 
     * @param idRutina  ID de la rutina
     * @param diaSemana Día de la semana (1-7)
     * @return Lista de detalles del día ordenados por orden
     */
    List<DetalleRutina> findByRutinaIa_IdRutinaIaAndDiaSemanaOrderByOrdenAsc(Long idRutina, Integer diaSemana);

    /**
     * Busca detalles de una rutina por ejercicio específico
     * 
     * @param idRutina    ID de la rutina
     * @param idEjercicio ID del ejercicio
     * @return Lista de detalles del ejercicio
     */
    List<DetalleRutina> findByRutinaIa_IdRutinaIaAndEjercicio_IdEjercicio(Long idRutina, Long idEjercicio);

    /**
     * Elimina todos los detalles de una rutina
     * 
     * @param idRutina ID de la rutina
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM DetalleRutina d WHERE d.rutinaIa.idRutinaIa = :idRutina")
    void deleteByRutinaId(@Param("idRutina") Long idRutina);

    /**
     * Cuenta el número de detalles de una rutina
     * 
     * @param idRutina ID de la rutina
     * @return Cantidad de detalles
     */
    long countByRutinaIa_IdRutinaIa(Long idRutina);

    /**
     * Busca detalles de una rutina con los datos del ejercicio precargados
     * 
     * @param idRutina ID de la rutina
     * @return Lista de detalles con ejercicio precargado
     */
    @Query("SELECT d FROM DetalleRutina d JOIN FETCH d.ejercicio e WHERE d.rutinaIa.idRutinaIa = :idRutina ORDER BY d.diaSemana ASC, d.orden ASC")
    List<DetalleRutina> findDetallesConEjercicio(@Param("idRutina") Long idRutina);

    /**
     * Busca detalles de una rutina que han sido modificados
     * 
     * @param idRutina ID de la rutina
     * @return Lista de detalles modificados
     */
    @Query("SELECT d FROM DetalleRutina d WHERE d.modificadoPor IS NOT NULL AND d.rutinaIa.idRutinaIa = :idRutina")
    List<DetalleRutina> findDetallesModificados(@Param("idRutina") Long idRutina);
}