package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.HistorialFisico;

public interface HistorialFisicoRepository
        extends JpaRepository<HistorialFisico, Long>, JpaSpecificationExecutor<HistorialFisico> {

    List<HistorialFisico> findBySocio_IdUsuarioOrderByFechaMedicionDesc(Long idSocio);

    /**
     * Busca el historial físico de un socio en un rango de fechas
     * 
     * @param idSocio ID del socio
     * @param inicio  Fecha de inicio del rango
     * @param fin     Fecha de fin del rango
     * @return Lista de historiales en el rango ordenados ascendentemente
     */
    List<HistorialFisico> findBySocio_IdUsuarioAndFechaMedicionBetweenOrderByFechaMedicionAsc(
            Long idSocio, LocalDateTime inicio, LocalDateTime fin);

    /**
     * Verifica si existe un historial físico para un socio en una fecha específica
     * 
     * @param idSocio       ID del socio
     * @param fechaMedicion Fecha de la medición
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuarioAndFechaMedicion(Long idSocio, LocalDateTime fechaMedicion);

    /**
     * Busca la última medición física de un socio
     * 
     * @param idSocio ID del socio
     * @return La última medición física del socio
     */
    @Query("SELECT h FROM HistorialFisico h WHERE h.socio.idUsuario = :idSocio ORDER BY h.fechaMedicion DESC LIMIT 1")
    HistorialFisico findLastMedicionBySocio(@Param("idSocio") Long idSocio);

    /**
     * Obtiene todos los historiales físicos ordenados por fecha descendente
     * 
     * @return Lista de todos los historiales
     */
    List<HistorialFisico> findAllByOrderByFechaMedicionDesc();
}