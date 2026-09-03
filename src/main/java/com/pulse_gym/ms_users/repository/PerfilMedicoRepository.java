package com.pulse_gym.ms_users.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.PerfilMedico;

public interface PerfilMedicoRepository extends JpaRepository<PerfilMedico, Long> {

    /**
     * Busca un perfil médico por el ID del socio al que está asociado
     * 
     * @param idSocio ID del socio
     * @return Perfil médico encontrado o vacío si no se encuentra
     */
    Optional<PerfilMedico> findBySocio_IdUsuario(Long idSocio);

    /**
     * Verifica si existe un perfil médico para el socio con el ID especificado
     * 
     * @param idSocio ID del socio
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuario(Long idSocio);

    /**
     * Busca un perfil médico activo por el ID del socio
     * 
     * @param idSocio ID del socio
     * @return Perfil médico activo del socio
     */
    Optional<PerfilMedico> findBySocio_IdUsuarioAndActivoTrue(Long idSocio);

    /**
     * Busca un perfil médico activo por su ID
     * 
     * @param idPerfilMedico ID del perfil médico
     * @return Perfil médico activo
     */
    Optional<PerfilMedico> findByIdPerfilMedicoAndActivoTrue(Long idPerfilMedico);

    /**
     * Verifica si existe un perfil médico activo para el socio
     * 
     * @param idSocio ID del socio
     * @return true si existe, false en caso contrario
     */
    boolean existsBySocio_IdUsuarioAndActivoTrue(Long idSocio);

    /**
     * Consulta perfiles médicos activos con búsqueda y paginación
     * 
     * @param busqueda Búsqueda por nombre, apellido o documento del socio
     * @param pageable Configuración de paginación
     * @return Página de perfiles médicos
     */
    @Query("SELECT p FROM PerfilMedico p WHERE p.activo = true AND " +
            "(:busqueda IS NULL OR :busqueda = '' OR " +
            "p.socio.nombre ILIKE CONCAT('%', :busqueda, '%') OR " +
            "p.socio.apellido ILIKE CONCAT('%', :busqueda, '%') OR " +
            "p.socio.documentoIdentidad ILIKE CONCAT('%', :busqueda, '%'))")
    Page<PerfilMedico> consultarPerfilesMedicosPaginados(@Param("busqueda") String busqueda, Pageable pageable);
}