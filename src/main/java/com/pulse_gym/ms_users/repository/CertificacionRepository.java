package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pulse_gym.lb_common.entity.user.Certificacion;

public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {

    /**
     * Busca todas las certificaciones asociadas a un entrenador específico.
     * 
     * @param idEntrenador ID del entrenador para el cual se buscan las
     *                     certificaciones.
     * @return Lista de certificaciones del entrenador. Si no se encuentran
     *         certificaciones, devuelve una lista vacía.
     */
    List<Certificacion> findByEntrenador_IdUsuario(Long idEntrenador);

    /**
     * Busca una certificación específica asociada a un entrenador.
     * 
     * @param idCertificacion ID de la certificación a buscar.
     * @param idEntrenador    ID del entrenador para el cual se busca la
     *                        certificación.
     * @return Optional con la certificación encontrada, o empty si no se encuentra.
     */
    Optional<Certificacion> findByIdCertificacionAndEntrenador_IdUsuario(Long idCertificacion, Long idEntrenador);
}