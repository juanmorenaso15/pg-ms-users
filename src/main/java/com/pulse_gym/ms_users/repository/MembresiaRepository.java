package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pulse_gym.lb_common.entity.user.Membresia;
import com.pulse_gym.lb_common.enums.EnumTipoDuracion;

import feign.Param;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    /**
     * Obtiene todas las membresías activas.
     * 
     * @return Lista de membresías activas.
     */
    List<Membresia> findByActivoTrue();

    /**
     * Obtiene las membresías activas que incluyen IA.
     * 
     * @param incluyeIA Indica si la membresía incluye IA.
     * @return Lista de membresías activas que incluyen IA.
     */
    List<Membresia> findByActivoTrueAndIncluyeIA(Boolean incluyeIA);

    /**
     * Obtiene las membresías activas que son flexibles.
     * 
     * @param esFlexible Indica si la membresía es flexible.
     * @return Lista de membresías activas que son flexibles.
     */
    List<Membresia> findByActivoTrueAndEsFlexible(Boolean esFlexible);

    /**
     * Verifica si existe una membresía activa con el nombre especificado.
     * 
     * @param nombre El nombre de la membresía a verificar.
     * @return true si existe una membresía activa con el nombre, false en caso
     *         contrario.
     */
    boolean existsByNombreAndActivoTrue(String nombre);

    /**
     * Obtiene las membresías activas que tienen un tipo de duración específico.
     * 
     * @param tipoDuracion El tipo de duración de la membresía.
     * @return Lista de membresías activas con el tipo de duración especificado.
     */
    List<Membresia> findByActivoTrueAndTipoDuracion(EnumTipoDuracion tipoDuracion);

    /**
     * Obtiene una membresía por ID con sus socios asignados.
     * 
     * @param idMembresia El ID de la membresía.
     * @return La membresía con sus socios asignados, si existe.
     */
    @Query("SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.idMembresia = :idMembresia AND m.activo = true")
    Optional<Membresia> findByIdWithSociosAsignados(@Param("idMembresia") Long idMembresia);

    /**
     * Obtiene una membresía por ID con SOLO socios ACTIVOS
     * 
     * @param idMembresia El ID de la membresía
     * @return La membresía con sus socios activos, si existe
     */
    @Query("SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.idMembresia = :idMembresia " +
            "AND m.activo = true " +
            "AND sm.estado = 'ACTIVA'") // ⭐ SOLO ACTIVAS
    Optional<Membresia> findByIdWithSociosActivos(@Param("idMembresia") Long idMembresia);

    /**
     * Obtiene una membresía por ID con socios ACTIVOS y SUSPENDIDOS
     * 
     * @param idMembresia El ID de la membresía
     * @return La membresía con sus socios activos y suspendidos, si existe
     */
    @Query("SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.idMembresia = :idMembresia " +
            "AND m.activo = true " +
            "AND sm.estado IN ('ACTIVA', 'SUSPENDIDA')")
    Optional<Membresia> findByIdWithSociosActivosYSuspendidas(@Param("idMembresia") Long idMembresia);

    /**
     * Obtiene todas las membresías activas con sus socios asignados.
     * 
     * @return Lista de membresías activas con sus socios asignados.
     */
    @Query("SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.activo = true " +
            "ORDER BY m.idMembresia")
    List<Membresia> findAllWithSociosAsignados();

    /**
     * Cuenta la cantidad de socios activos (ACTIVA o SUSPENDIDA) asociados a una
     * membresía.
     * 
     * @param idMembresia El ID de la membresía.
     * @return La cantidad de socios activos asociados a la membresía.
     */
    @Query("SELECT COUNT(sm) FROM SocioMembresia sm " +
            "WHERE sm.membresia.idMembresia = :idMembresia " +
            "AND sm.estado IN ('ACTIVA', 'SUSPENDIDA')")
    Long countSociosActivosByMembresia(@Param("idMembresia") Long idMembresia);

    @Query("SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.activo = true " +
            "AND sm.estado = 'ACTIVA' " +
            "ORDER BY m.idMembresia")
    List<Membresia> findAllWithSociosActivos();

    /**
     * Busca todas las membresías activas paginadas
     * 
     * @param pageable Configuración de paginación
     * @return Página de membresías activas
     */
    Page<Membresia> findByActivoTrue(Pageable pageable);

    /**
     * Busca todas las membresías activas con sus socios asignados (fetch join)
     * 
     * @param pageable Configuración de paginación
     * @return Página de membresías con socios precargados
     */
    @Query(value = "SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.activo = true", countQuery = "SELECT COUNT(m) FROM Membresia m WHERE m.activo = true")
    Page<Membresia> findAllWithSociosAsignadosPaginado(Pageable pageable);

    /**
     * Busca todas las membresías activas con socios activos asignados
     * 
     * @param pageable Configuración de paginación
     * @return Página de membresías con socios activos
     */
    @Query(value = "SELECT DISTINCT m FROM Membresia m " +
            "LEFT JOIN FETCH m.socioMembresias sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "WHERE m.activo = true AND (sm IS NULL OR sm.estado = 'ACTIVA')", countQuery = "SELECT COUNT(m) FROM Membresia m WHERE m.activo = true")
    Page<Membresia> findAllWithSociosActivosPaginado(Pageable pageable);

}