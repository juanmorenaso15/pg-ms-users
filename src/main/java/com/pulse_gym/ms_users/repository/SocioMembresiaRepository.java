package com.pulse_gym.ms_users.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.enums.EnumEstadoSocioMembresia;

public interface SocioMembresiaRepository extends JpaRepository<SocioMembresia, Long> {

    /**
     * Busca todas las membresías asignadas a un socio
     */
    List<SocioMembresia> findBySocio_IdUsuarioOrderByFechaCreacionDesc(Long idSocio);

    /**
     * Busca la membresía activa actual de un socio
     */
    @Query("SELECT sm FROM SocioMembresia sm WHERE sm.socio.idUsuario = :idSocio AND sm.estado = 'ACTIVA' AND sm.fechaVencimiento >= CURRENT_DATE ORDER BY sm.fechaCreacion DESC")
    Optional<SocioMembresia> findMembresiaActivaBySocio(@Param("idSocio") Long idSocio);

    /**
     * Busca todas las membresías vencidas que deberían actualizarse
     */
    @Query("SELECT sm FROM SocioMembresia sm WHERE sm.estado = 'ACTIVA' AND sm.fechaVencimiento < CURRENT_DATE")
    List<SocioMembresia> findVencidasActivas();

    /**
     * Busca membresías por estado
     */
    List<SocioMembresia> findByEstado(EnumEstadoSocioMembresia estado);

    /**
     * Busca membresías por socio y estado
     */
    List<SocioMembresia> findBySocio_IdUsuarioAndEstado(Long idSocio, EnumEstadoSocioMembresia estado);

    /**
     * Verifica si un socio tiene una membresía activa
     */
    boolean existsBySocio_IdUsuarioAndEstado(Long idSocio, EnumEstadoSocioMembresia estado);

    /**
     * Busca membresías por estado y fecha de vencimiento
     * 
     * @param estado estado de la membresía
     * @param fecha  fecha de vencimiento antes de la cual se buscan las membresías
     * @return una lista de membresías que cumplen con los criterios
     */
    List<SocioMembresia> findByEstadoAndFechaVencimientoBefore(
            EnumEstadoSocioMembresia estado,
            LocalDate fecha);

    /***
     *  Busca membresías vencidas, incluyendo las que están suspendidas
     * @return una lista de membresías vencidas, incluyendo las suspendidas
     */
    @Query("SELECT sm FROM SocioMembresia sm WHERE sm.estado IN ('ACTIVA', 'SUSPENDIDA') AND sm.fechaVencimiento < CURRENT_DATE")
    List<SocioMembresia> findVencidasIncluyendoSuspendidas();

}