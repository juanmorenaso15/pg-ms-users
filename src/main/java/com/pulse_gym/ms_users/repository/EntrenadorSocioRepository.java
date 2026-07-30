package com.pulse_gym.ms_users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pulse_gym.lb_common.entity.user.EntrenadorSocio;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;

import feign.Param;

public interface EntrenadorSocioRepository extends JpaRepository<EntrenadorSocio, Long> {

    /**
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
}