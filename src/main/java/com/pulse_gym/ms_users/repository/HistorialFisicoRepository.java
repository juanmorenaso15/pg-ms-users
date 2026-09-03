package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.HistorialFisico;

public interface HistorialFisicoRepository extends JpaRepository<HistorialFisico, Long> {

    /**
     * Busca el historial físico de un socio ordenado por fecha descendente
     * 
     * @param idSocio ID del socio
     * @return Lista de historiales del socio
     */
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
     * Obtiene la última medición física de un socio
     * 
     * @param idSocio ID del socio
     * @return Último historial físico del socio
     */
    @Query("SELECT h FROM HistorialFisico h WHERE h.socio.idUsuario = :idSocio ORDER BY h.fechaMedicion DESC LIMIT 1")
    HistorialFisico findLastMedicionBySocio(@Param("idSocio") Long idSocio);

    /**
     * Obtiene todos los historiales físicos ordenados por fecha descendente
     * 
     * @return Lista de todos los historiales
     */
    List<HistorialFisico> findAllByOrderByFechaMedicionDesc();

    /**
     * Busca historiales físicos con filtros y paginación (native query)
     * 
     * @param idSocio     Filtro por ID del socio
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin    Fecha de fin del rango
     * @param busqueda    Búsqueda por nombre o apellido
     * @param pageable    Configuración de paginación
     * @return Página de historiales físicos
     */
    @Query(value = "SELECT h.* FROM historial_fisico h " +
            "LEFT JOIN usuario_perfil s ON s.id_usuario = h.fk_id_usuario_socio " +
            "LEFT JOIN usuario_perfil r ON r.id_usuario = h.fk_id_usuario_recepcionista " +
            "WHERE (CAST(:idSocio AS text) IS NULL OR s.id_usuario = :idSocio) " +
            "AND (CAST(:fechaInicio AS text) IS NULL OR h.fecha_medicion >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS text) IS NULL OR h.fecha_medicion <= :fechaFin) " +
            "AND (CAST(:busqueda AS text) IS NULL OR " +
            "     s.nombre ILIKE CONCAT('%', :busqueda, '%') OR " +
            "     s.apellido ILIKE CONCAT('%', :busqueda, '%') OR " +
            "     r.nombre ILIKE CONCAT('%', :busqueda, '%') OR " +
            "     r.apellido ILIKE CONCAT('%', :busqueda, '%')) " +
            "ORDER BY h.fecha_medicion DESC", countQuery = "SELECT COUNT(*) FROM historial_fisico h " +
                    "LEFT JOIN usuario_perfil s ON s.id_usuario = h.fk_id_usuario_socio " +
                    "LEFT JOIN usuario_perfil r ON r.id_usuario = h.fk_id_usuario_recepcionista " +
                    "WHERE (CAST(:idSocio AS text) IS NULL OR s.id_usuario = :idSocio) " +
                    "AND (CAST(:fechaInicio AS text) IS NULL OR h.fecha_medicion >= :fechaInicio) " +
                    "AND (CAST(:fechaFin AS text) IS NULL OR h.fecha_medicion <= :fechaFin) " +
                    "AND (CAST(:busqueda AS text) IS NULL OR " +
                    "     s.nombre ILIKE CONCAT('%', :busqueda, '%') OR " +
                    "     s.apellido ILIKE CONCAT('%', :busqueda, '%') OR " +
                    "     r.nombre ILIKE CONCAT('%', :busqueda, '%') OR " +
                    "     r.apellido ILIKE CONCAT('%', :busqueda, '%'))", nativeQuery = true)
    Page<HistorialFisico> findWithFilters(
            @Param("idSocio") Long idSocio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("busqueda") String busqueda,
            Pageable pageable);

}