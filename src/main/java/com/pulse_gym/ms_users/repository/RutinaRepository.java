package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.RutinaIA;

public interface RutinaRepository extends JpaRepository<RutinaIA, Long> {

    /**
     * Busca rutinas activas de un socio
     * 
     * @param idSocio ID del socio
     * @return Lista de rutinas activas
     */
    List<RutinaIA> findBySocio_IdUsuarioAndActivaTrue(Long idSocio);

    /**
     * Busca todas las rutinas de un socio ordenadas por fecha descendente
     * 
     * @param idSocio ID del socio
     * @return Lista de rutinas ordenadas
     */
    List<RutinaIA> findBySocio_IdUsuarioOrderByFechaGeneracionDesc(Long idSocio);

    /**
     * Busca la rutina activa más reciente de un socio
     * 
     * @param idSocio ID del socio
     * @return Rutina activa más reciente
     */
    @Query("SELECT r FROM RutinaIA r WHERE r.socio.idUsuario = :idSocio AND r.activa = true ORDER BY r.fechaGeneracion DESC LIMIT 1")
    Optional<RutinaIA> findRutinaActivaReciente(@Param("idSocio") Long idSocio);

    /**
     * Busca rutinas generadas por IA de un socio
     * 
     * @param idSocio ID del socio
     * @return Lista de rutinas generadas por IA
     */
    @Query("SELECT r FROM RutinaIA r WHERE r.socio.idUsuario = :idSocio AND r.modeloIa IS NOT NULL")
    List<RutinaIA> findRutinasGeneradasPorIA(@Param("idSocio") Long idSocio);

    /**
     * Busca rutinas de un socio por versión
     * 
     * @param idSocio ID del socio
     * @param version Versión de la rutina
     * @return Lista de rutinas con esa versión
     */
    List<RutinaIA> findBySocio_IdUsuarioAndVersion(Long idSocio, Integer version);

    /**
     * Busca rutinas modificadas por un entrenador
     * 
     * @param idEntrenador ID del entrenador
     * @return Lista de rutinas modificadas por el entrenador
     */
    @Query("SELECT r FROM RutinaIA r WHERE r.entrenador.idUsuario = :idEntrenador")
    List<RutinaIA> findRutinasModificadasPorEntrenador(@Param("idEntrenador") Long idEntrenador);

    /**
     * Verifica si un socio tiene al menos una rutina activa
     * 
     * @param idSocio ID del socio
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuarioAndActivaTrue(Long idSocio);

    /**
     * Busca todas las rutinas activas
     * 
     * @return Lista de rutinas activas
     */
    List<RutinaIA> findByActivaTrue();
}