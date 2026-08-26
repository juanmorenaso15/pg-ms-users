package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.HistorialFisico;

public interface HistorialFisicoRepository extends JpaRepository<HistorialFisico, Long> {
    
    /**
     * Obtiene el historial físico de un socio ordenado por fecha de medición descendente
     * @param idSocio ID del socio
     * @return Lista de registros de historial físico del socio
     */
    List<HistorialFisico> findBySocio_IdUsuarioOrderByFechaMedicionDesc(Long idSocio);
    
    /**
     * Obtiene el historial físico de un socio dentro de un rango de fechas ordenado por fecha de medición ascendente
     * @param idSocio ID del socio
     * @param inicio Fecha de inicio
     * @param fin Fecha de fin
     * @return Lista de registros de historial físico del socio
     */
    List<HistorialFisico> findBySocio_IdUsuarioAndFechaMedicionBetweenOrderByFechaMedicionAsc(
        Long idSocio, LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Verifica si existe un registro de historial físico para un socio en una fecha específica
     * @param idSocio ID del socio
     * @param fechaMedicion Fecha de medición
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuarioAndFechaMedicion(Long idSocio, LocalDateTime fechaMedicion);
    
    /**
     * Obtiene la última medición de historial físico para un socio
     * @param idSocio ID del socio
     * @return Último registro de historial físico del socio
     */
    @Query("SELECT h FROM HistorialFisico h WHERE h.socio.idUsuario = :idSocio ORDER BY h.fechaMedicion DESC LIMIT 1")
    HistorialFisico findLastMedicionBySocio(@Param("idSocio") Long idSocio);

    /**
     * Obtiene todos los registros de historial físico ordenados por fecha descendente
     * @return Lista de registros físicos de todos los socios
     */
    List<HistorialFisico> findAllByOrderByFechaMedicionDesc();
}