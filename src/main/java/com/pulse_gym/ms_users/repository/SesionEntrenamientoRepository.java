package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.SesionEntrenamiento;

public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {

    /**
     * Busca todas las sesiones de un socio ordenadas por fecha descendente
     * 
     * @param idSocio ID del socio
     * @return Lista de sesiones ordenadas
     */
    List<SesionEntrenamiento> findBySocio_IdUsuarioOrderByFechaSesionDesc(Long idSocio);

    /**
     * Cuenta las sesiones de un socio en un período específico
     * 
     * @param idSocio     ID del socio
     * @param fechaInicio Fecha desde la cual contar
     * @return Número de sesiones en el período
     */
    @Query("SELECT COUNT(s) FROM SesionEntrenamiento s WHERE s.socio.idUsuario = :idSocio AND s.fechaSesion >= :fechaInicio")
    Long countSesionesEnPeriodo(@Param("idSocio") Long idSocio, @Param("fechaInicio") LocalDateTime fechaInicio);

    /**
     * Busca las sesiones de un socio desde una fecha específica
     * 
     * @param idSocio     ID del socio
     * @param fechaInicio Fecha desde la cual buscar
     * @return Lista de sesiones desde la fecha
     */
    @Query("SELECT s FROM SesionEntrenamiento s WHERE s.socio.idUsuario = :idSocio AND s.fechaSesion >= :fechaInicio ORDER BY s.fechaSesion DESC")
    List<SesionEntrenamiento> findSesionesDesdeFecha(@Param("idSocio") Long idSocio,
            @Param("fechaInicio") LocalDateTime fechaInicio);

    /**
     * Cuenta el total de sesiones de un socio
     * 
     * @param idSocio ID del socio
     * @return Número total de sesiones
     */
    Long countBySocio_IdUsuario(Long idSocio);
}