package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.HistorialRutinaVersion;

public interface HistorialRutinaVersionRepository extends JpaRepository<HistorialRutinaVersion, Long> {

    /**
     * Busca el historial de versiones de una rutina ordenado por versión
     * descendente
     * 
     * @param idRutina ID de la rutina
     * @return Lista de historial ordenado
     */
    List<HistorialRutinaVersion> findByRutinaIa_IdRutinaIaOrderByVersionDesc(Long idRutina);

    /**
     * Busca una versión específica del historial de una rutina
     * 
     * @param idRutina ID de la rutina
     * @param version  Versión a buscar
     * @return Historial de la versión específica
     */
    Optional<HistorialRutinaVersion> findByRutinaIa_IdRutinaIaAndVersion(Long idRutina, Integer version);

    /**
     * Busca la última versión del historial de una rutina
     * 
     * @param idRutina ID de la rutina
     * @return Última versión del historial
     */
    @Query("SELECT h FROM HistorialRutinaVersion h WHERE h.rutinaIa.idRutinaIa = :idRutina ORDER BY h.version DESC LIMIT 1")
    Optional<HistorialRutinaVersion> findUltimaVersion(@Param("idRutina") Long idRutina);

    /**
     * Busca el historial de una rutina con los datos del modificador precargados
     * 
     * @param idRutina ID de la rutina
     * @return Lista de historial con modificador precargado
     */
    @Query("SELECT h FROM HistorialRutinaVersion h JOIN FETCH h.modificadoPor m WHERE h.rutinaIa.idRutinaIa = :idRutina ORDER BY h.version DESC")
    List<HistorialRutinaVersion> findHistorialConModificador(@Param("idRutina") Long idRutina);

    /**
     * Cuenta el número de versiones en el historial de una rutina
     * 
     * @param idRutina ID de la rutina
     * @return Cantidad de versiones
     */
    long countByRutinaIa_IdRutinaIa(Long idRutina);
}