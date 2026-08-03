package com.pulse_gym.ms_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.DetalleSesionEjercicio;

public interface DetalleSesionEjercicioRepository extends JpaRepository<DetalleSesionEjercicio, Long> {
    /**
     * Busca los detalles de una sesión ordenados por ID
     * 
     * @param idSesion ID de la sesión
     * @return Lista de detalles de la sesión
     */
    List<DetalleSesionEjercicio> findBySesion_IdSesionOrderByIdDetalleSesionAsc(Long idSesion);

    /**
     * Busca todos los detalles de ejercicios de un socio
     * 
     * @param idSocio ID del socio
     * @return Lista de detalles de ejercicios del socio
     */
    @Query("SELECT d FROM DetalleSesionEjercicio d WHERE d.sesion.socio.idUsuario = :idSocio")
    List<DetalleSesionEjercicio> findDetallesBySocio(@Param("idSocio") Long idSocio);
}