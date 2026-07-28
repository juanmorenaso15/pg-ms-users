package com.pulse_gym.ms_users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse_gym.lb_common.entity.user.PerfilMedico;

public interface PerfilMedicoRepository extends JpaRepository<PerfilMedico, Long> {

    /**
     * Busca un perfil médico por el ID del socio al que está asociado
     * 
     * @param idSocio El ID del socio
     * @return El perfil médico encontrado o vacío si no se encuentra
     */
    Optional<PerfilMedico> findBySocio_IdUsuario(Long idSocio);

    /**
     * Verifica si existe un perfil médico para el socio con el ID especificado
     * 
     * @param idSocio El ID del socio
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuario(Long idSocio);
}