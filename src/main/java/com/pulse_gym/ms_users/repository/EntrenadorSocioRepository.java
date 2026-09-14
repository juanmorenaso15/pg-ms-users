package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pulse_gym.lb_common.dto.SocioSimpleDTO;
import com.pulse_gym.lb_common.entity.user.EntrenadorSocio;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;

import feign.Param;

public interface EntrenadorSocioRepository extends JpaRepository<EntrenadorSocio, Long> {

    /**x
     * Busca los socios activos asignados a un entrenador
     * 
     * @param idEntrenador ID del entrenador
     * @return Lista de socios activos del entrenador
     */
    @Query("SELECT es.socio FROM EntrenadorSocio es WHERE es.entrenador.idUsuario = :idEntrenador AND es.activa = true")
    List<UsuarioPerfil> findSociosActivosByEntrenador(@Param("idEntrenador") Long idEntrenador);

    /**
     * Verifica si un socio está asignado activamente a un entrenador
     * 
     * @param idEntrenador ID del entrenador
     * @param idSocio      ID del socio
     * @return true si está asignado, false en caso contrario
     */
    boolean existsByEntrenador_IdUsuarioAndSocio_IdUsuarioAndActivaTrue(Long idEntrenador, Long idSocio);

    /**
     * Cuenta los socios activos asignados a un entrenador
     * 
     * @param idEntrenador ID del entrenador
     * @return Número de socios activos asignados
     */
    @Query("SELECT COUNT(es) FROM EntrenadorSocio es " +
            "WHERE es.entrenador.idUsuario = :idEntrenador AND es.activa = true")
    Long countByEntrenadorAndActivaTrue(@Param("idEntrenador") Long idEntrenador);

    /**
     * Cuenta los socios nuevos asignados a un entrenador desde una fecha específica
     * 
     * @param idEntrenador ID del entrenador
     * @param fechaInicio  Fecha de corte para considerar al socio nuevo
     * @return Número de socios nuevos en ese periodo
     */
    @Query("SELECT COUNT(es) FROM EntrenadorSocio es " +
            "WHERE es.entrenador.idUsuario = :idEntrenador AND es.activa = true AND es.fechaAsignacion >= :fechaInicio")
    Long countSociosNuevosPorEntrenador(@Param("idEntrenador") Long idEntrenador,
            @Param("fechaInicio") LocalDateTime fechaInicio);


    @Query("SELECT new com.pulse_gym.lb_common.dto.SocioSimpleDTO(es.socio.idUsuario, es.socio.nombre, es.socio.apellido, es.socio.email, es.socio.telefono) " +
           "FROM EntrenadorSocio es WHERE es.entrenador.idUsuario = :idEntrenador AND es.activa = true")
    List<SocioSimpleDTO> findSociosSimplesActivosByEntrenador(@Param("idEntrenador") Long idEntrenador);

}