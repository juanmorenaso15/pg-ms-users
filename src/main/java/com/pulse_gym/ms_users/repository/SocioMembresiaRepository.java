package com.pulse_gym.ms_users.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
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

    @Query("SELECT sm FROM SocioMembresia sm " +
            "WHERE sm.estado IN ('VENCIDA', 'SUSPENDIDA') " +
            "AND sm.fechaVencimiento >= :fechaInicio " +
            "AND sm.fechaVencimiento <= :fechaFin")
    List<SocioMembresia> findMorosos(@Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @Query("SELECT DISTINCT sm.socio FROM SocioMembresia sm " +
            "WHERE (sm.estado = 'VENCIDA' OR (sm.estado = 'ACTIVA' AND sm.fechaVencimiento < :fechaFin)) " +
            "AND (:fechaInicio IS NULL OR sm.fechaVencimiento >= :fechaInicio) " +
            "AND (:fechaFin IS NULL OR sm.fechaVencimiento <= :fechaFin)")
    List<UsuarioPerfil> findSociosEnMora(@Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

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
     * Busca membresías vencidas, incluyendo las que están suspendidas
     * 
     * @return una lista de membresías vencidas, incluyendo las suspendidas
     */
    @Query("SELECT sm FROM SocioMembresia sm WHERE sm.estado IN ('ACTIVA', 'SUSPENDIDA') AND sm.fechaVencimiento < CURRENT_DATE")
    List<SocioMembresia> findVencidasIncluyendoSuspendidas();

    /**
     * Busca membresías por vencer en un rango específico de fechas
     * 
     * @param fechaInicio fecha de inicio del rango
     * @param fechaFin    fecha de fin del rango
     * @return una lista de membresías por vencer en el rango especificado
     */
    @Query("SELECT sm FROM SocioMembresia sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "LEFT JOIN FETCH sm.membresia m " +
            "WHERE sm.estado = 'ACTIVA' " +
            "AND sm.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY sm.fechaVencimiento ASC")
    List<SocioMembresia> findMembresiasPorVencerEnRango(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    /**
     * Busca membresías activas próximas a vencer en un rango de fechas (paginado)
     * 
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin    Fecha de fin del rango
     * @param pageable    Configuración de paginación
     * @return Página de membresías próximas a vencer
     */
    @Query(value = "SELECT sm FROM SocioMembresia sm " +
            "LEFT JOIN FETCH sm.socio s " +
            "LEFT JOIN FETCH sm.membresia m " +
            "WHERE sm.estado = 'ACTIVA' " +
            "AND sm.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin " +
            "ORDER BY sm.fechaVencimiento ASC", countQuery = "SELECT COUNT(sm) FROM SocioMembresia sm " +
                    "WHERE sm.estado = 'ACTIVA' " +
                    "AND sm.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin")
    Page<SocioMembresia> findMembresiasPorVencerEnRangoPaginado(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            Pageable pageable);

    /**
     * Busca socios activos asignados a una membresía específica (paginado)
     * 
     * @param idMembresia ID de la membresía
     * @param pageable    Configuración de paginación
     * @return Página de socios activos asignados
     */
    @Query(value = "SELECT sm FROM SocioMembresia sm " +
            "JOIN FETCH sm.socio s " +
            "WHERE sm.membresia.idMembresia = :idMembresia " +
            "AND sm.estado = 'ACTIVA'", countQuery = "SELECT COUNT(sm) FROM SocioMembresia sm " +
                    "WHERE sm.membresia.idMembresia = :idMembresia " +
                    "AND sm.estado = 'ACTIVA'")
    Page<SocioMembresia> findSociosActivosByMembresiaId(
            @Param("idMembresia") Long idMembresia,
            Pageable pageable);

    /**
     * Busca socios activos con filtros y paginación
     * 
     * @param pageable    Configuración de paginación
     * @param busqueda    Búsqueda por nombre, apellido o email
     * @param incluyeIA   Filtro por membresía con IA incluida
     * @param esFlexible  Filtro por membresía flexible
     * @param idMembresia Filtro por ID de membresía
     * @return Página de socios activos
     */
    @Query(value = "SELECT DISTINCT sm.* FROM socio_membresia sm " +
            "JOIN usuario_perfil s ON s.id_usuario = sm.fk_id_socio " +
            "JOIN membresias m ON m.id_membresia = sm.fk_id_membresia " +
            "WHERE sm.estado = 'ACTIVA' " +
            "AND (:busqueda IS NULL OR s.nombre ILIKE CONCAT('%', :busqueda, '%') " +
            "     OR s.apellido ILIKE CONCAT('%', :busqueda, '%') " +
            "     OR s.email ILIKE CONCAT('%', :busqueda, '%')) " +
            "AND (:incluyeIA IS NULL OR m.incluye_ia = :incluyeIA) " +
            "AND (:esFlexible IS NULL OR m.es_flexible = :esFlexible) " +
            "AND (:idMembresia IS NULL OR m.id_membresia = :idMembresia) " +
            "ORDER BY sm.fecha_creacion DESC", countQuery = "SELECT COUNT(DISTINCT sm.id_socio_membresia) FROM socio_membresia sm "
                    +
                    "JOIN usuario_perfil s ON s.id_usuario = sm.fk_id_socio " +
                    "JOIN membresias m ON m.id_membresia = sm.fk_id_membresia " +
                    "WHERE sm.estado = 'ACTIVA' " +
                    "AND (:busqueda IS NULL OR s.nombre ILIKE CONCAT('%', :busqueda, '%') " +
                    "     OR s.apellido ILIKE CONCAT('%', :busqueda, '%') " +
                    "     OR s.email ILIKE CONCAT('%', :busqueda, '%')) " +
                    "AND (:incluyeIA IS NULL OR m.incluye_ia = :incluyeIA) " +
                    "AND (:esFlexible IS NULL OR m.es_flexible = :esFlexible) " +
                    "AND (:idMembresia IS NULL OR m.id_membresia = :idMembresia)", nativeQuery = true)
    Page<SocioMembresia> findSociosActivosConFiltros(
            Pageable pageable,
            @Param("busqueda") String busqueda,
            @Param("incluyeIA") Boolean incluyeIA,
            @Param("esFlexible") Boolean esFlexible,
            @Param("idMembresia") Long idMembresia);

    @Query(value = "SELECT sm.* FROM socio_membresia sm " +
            "JOIN usuario_perfil s ON s.id_usuario = sm.fk_id_socio " +
            "WHERE sm.fk_id_membresia = :idMembresia " +
            "AND sm.estado = 'ACTIVA' " +
            "AND (:busqueda IS NULL OR s.nombre ILIKE CONCAT('%', :busqueda, '%') " +
            "     OR s.apellido ILIKE CONCAT('%', :busqueda, '%') " +
            "     OR s.email ILIKE CONCAT('%', :busqueda, '%'))", countQuery = "SELECT COUNT(sm.id_socio_membresia) FROM socio_membresia sm "
                    +
                    "JOIN usuario_perfil s ON s.id_usuario = sm.fk_id_socio " +
                    "WHERE sm.fk_id_membresia = :idMembresia " +
                    "AND sm.estado = 'ACTIVA' " +
                    "AND (:busqueda IS NULL OR s.nombre ILIKE CONCAT('%', :busqueda, '%') " +
                    "     OR s.apellido ILIKE CONCAT('%', :busqueda, '%') " +
                    "     OR s.email ILIKE CONCAT('%', :busqueda, '%'))", nativeQuery = true)
    Page<SocioMembresia> findSociosActivosByMembresiaIdConBusqueda(
            @Param("idMembresia") Long idMembresia,
            @Param("busqueda") String busqueda,
            Pageable pageable);
}