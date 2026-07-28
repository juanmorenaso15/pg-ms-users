package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.Ejercicio;

public interface EjercicioRepository extends JpaRepository<Ejercicio, Long>, JpaSpecificationExecutor<Ejercicio> {

    /**
     * Busca todos los ejercicios activos
     * 
     * @return Lista de ejercicios activos
     */
    List<Ejercicio> findByActivoTrue();

    /**
     * Busca ejercicios activos por grupo muscular
     * 
     * @param grupoMuscular Grupo muscular a filtrar
     * @return Lista de ejercicios activos del grupo
     */
    List<Ejercicio> findByGrupoMuscularAndActivoTrue(String grupoMuscular);

    /**
     * Busca ejercicios activos por equipo necesario
     * 
     * @param equipoNecesario Equipo a filtrar
     * @return Lista de ejercicios activos que usan ese equipo
     */
    List<Ejercicio> findByEquipoNecesarioAndActivoTrue(String equipoNecesario);

    /**
     * Busca ejercicios activos con dificultad entre un rango
     * 
     * @param min Dificultad mínima
     * @param max Dificultad máxima
     * @return Lista de ejercicios dentro del rango
     */
    List<Ejercicio> findByDificultadBetweenAndActivoTrue(Integer min, Integer max);

    /**
     * Verifica si existe un ejercicio activo con el nombre indicado
     * 
     * @param nombre Nombre del ejercicio
     * @return true si existe, false en caso contrario
     */
    boolean existsByNombreAndActivoTrue(String nombre);

    /**
     * Busca ejercicios activos por grupo muscular y rango de dificultad
     * 
     * @param grupoMuscular Grupo muscular del ejercicio
     * @param dificultadMin Dificultad mínima
     * @param dificultadMax Dificultad máxima
     * @return Lista de ejercicios que coinciden con los filtros
     */
    @Query("SELECT e FROM Ejercicio e WHERE e.activo = true AND e.grupoMuscular = :grupoMuscular AND e.dificultad BETWEEN :dificultadMin AND :dificultadMax")
    List<Ejercicio> findByGrupoMuscularAndDificultadBetween(
            @Param("grupoMuscular") String grupoMuscular,
            @Param("dificultadMin") Integer dificultadMin,
            @Param("dificultadMax") Integer dificultadMax);

    /**
     * Busca ejercicios activos que usan equipamientos específicos
     * 
     * @param equipamientos Lista de equipamientos
     * @return Lista de ejercicios que usan esos equipamientos
     */
    @Query("SELECT e FROM Ejercicio e WHERE e.activo = true AND e.equipoNecesario IN :equipamientos")
    List<Ejercicio> findByEquipamientoIn(@Param("equipamientos") List<String> equipamientos);

    /**
     * Busca ejercicios de cardio aleatorios
     * 
     * @param limit Número máximo de ejercicios a retornar
     * @return Lista de ejercicios de cardio aleatorios
     */
    @Query(value = "SELECT * FROM ejercicio WHERE activo = true AND grupo_muscular = 'CARDIO' ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Ejercicio> findRandomCardioEjercicios(@Param("limit") int limit);

    /**
     * Busca un ejercicio activo por su nombre
     * 
     * @param nombre Nombre del ejercicio
     * @return Ejercicio si existe y está activo
     */
    Optional<Ejercicio> findByNombreAndActivoTrue(String nombre);
}