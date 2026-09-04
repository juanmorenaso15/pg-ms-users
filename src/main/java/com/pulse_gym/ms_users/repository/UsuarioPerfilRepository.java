package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;

public interface UsuarioPerfilRepository extends JpaRepository<UsuarioPerfil, Long> {

    /**
     * Busca un usuario por su documento de identidad.
     *
     * @param documentoIdentidad El documento del usuario
     * @return El usuario si existe, o vacío si no se encuentra
     */
    Optional<UsuarioPerfil> findByDocumentoIdentidad(String documentoIdentidad);

    /**
     * Busca un usuario por su documento de identidad y estado ACTIVO
     *
     * @param documentoIdentidad El documento del usuario
     * @return El usuario si existe y está ACTIVO, o vacío si no se encuentra
     */
    Optional<UsuarioPerfil> findByDocumentoIdentidadAndEstado(String documentoIdentidad, EnumEstadoUsuario estado);

    /**
     * Busca un usuario por su nombre (ignorando mayúsculas/minúsculas)
     *
     * @param nombre El nombre del usuario
     * @return El usuario si existe, o vacío si no se encuentra
     */
    Optional<UsuarioPerfil> findByNombreIgnoreCase(String nombre);

    /**
     * Busca un usuario por su nombre (ignorando mayúsculas/minúsculas) y estado
     * ACTIVO
     *
     * @param nombre El nombre del usuario
     * @return El usuario si existe y está ACTIVO, o vacío si no se encuentra
     */
    List<UsuarioPerfil> findByNombreIgnoreCaseAndEstado(String nombre, EnumEstadoUsuario estado);

    /**
     * Busca un usuario por su email.
     * 
     * @param email El email del usuario
     * @return El usuario si existe, o vacío si no se encuentra
     */
    Optional<UsuarioPerfil> findByEmail(String email);

    /**
     * Busca un usuario por su email y estado ACTIVO
     *
     * @param email El email del usuario
     * @return El usuario si existe y está ACTIVO, o vacío si no se encuentra
     */
    Optional<UsuarioPerfil> findByEmailAndEstado(String email, EnumEstadoUsuario estado);

    /**
     * Busca un usuario por ID y estado ACTIVO
     *
     * @param id El ID del usuario
     * @return El usuario si existe y está ACTIVO, o vacío si no se encuentra
     */
    @Query("SELECT u FROM UsuarioPerfil u WHERE u.idUsuario = :idUsuario AND u.estado = :estado")
    Optional<UsuarioPerfil> findByIdAndEstado(@Param("idUsuario") Long idUsuario,
            @Param("estado") EnumEstadoUsuario estado);

    /**
     * Busca todos los usuarios por su estado (ACTIVO/INACTIVO)
     * 
     * @param estado El estado de los usuarios a buscar
     * @return Lista de usuarios que coinciden con el estado especificado
     */
    List<UsuarioPerfil> findByEstado(EnumEstadoUsuario estado);

    /**
     * Busca usuarios por email (búsqueda parcial) y estado
     * 
     * @param emailSubstring Subcadena del email a buscar
     * @param estado         Estado del usuario
     * @return Lista de usuarios que coinciden
     */
    List<UsuarioPerfil> findByEmailContainingAndEstado(String emailSubstring, EnumEstadoUsuario estado);

    /**
     * Busca entrenadores activos con especialidad definida
     * 
     * @param estado Estado del usuario
     * @return Lista de entrenadores activos
     */
    @Query("SELECT u FROM UsuarioPerfil u " +
            "WHERE u.estado = :estado " +
            "AND u.especialidad IS NOT NULL " +
            "AND u.especialidad != ''")
    List<UsuarioPerfil> findEntrenadoresActivos(@Param("estado") EnumEstadoUsuario estado);

    /**
     * Busca entrenadores activos con especialidad definida (estado ACTIVO por
     * defecto)
     * 
     * @return Lista de entrenadores activos
     */
    default List<UsuarioPerfil> findEntrenadoresActivos() {
        return findEntrenadoresActivos(EnumEstadoUsuario.ACTIVO);
    }

    /**
     * Cuenta los usuarios por estado
     * 
     * @param estado Estado del usuario (ACTIVO, INACTIVO, SUSPENDIDO)
     * @return Cantidad de usuarios en el estado indicado
     */
    long countByEstado(EnumEstadoUsuario estado);

    /**
     * Cuenta los usuarios registrados desde una fecha específica
     * 
     * @param fechaInicio Fecha desde la cual contar
     * @return Cantidad de usuarios registrados desde la fecha
     */
    @Query("SELECT COUNT(u) FROM UsuarioPerfil u WHERE u.fechaRegistro >= :fechaInicio")
    long countNuevosDesde(@Param("fechaInicio") LocalDateTime fechaInicio);

    Page<UsuarioPerfil> findByEstado(EnumEstadoUsuario estado, Pageable pageable);

    /**
     * Busca usuarios por estado con paginación y filtro de búsqueda
     * 
     * @param estado   Estado del usuario (ACTIVO, INACTIVO, null = todos)
     * @param busqueda Texto de búsqueda (opcional)
     * @param pageable Configuración de paginación
     * @return Página de usuarios
     */
    @Query(value = "SELECT * FROM usuario_perfil u WHERE " +
            "(:estado IS NULL OR u.estado = CAST(:estado AS text)) AND " +
            "(:busqueda IS NULL OR (" +
            "  (:p1 IS NULL OR u.nombre ILIKE CONCAT('%', :p1, '%') OR u.apellido ILIKE CONCAT('%', :p1, '%') OR u.email ILIKE CONCAT('%', :p1, '%') OR u.documento_identidad ILIKE CONCAT('%', :p1, '%')) AND "
            +
            "  (:p2 IS NULL OR u.nombre ILIKE CONCAT('%', :p2, '%') OR u.apellido ILIKE CONCAT('%', :p2, '%') OR u.email ILIKE CONCAT('%', :p2, '%') OR u.documento_identidad ILIKE CONCAT('%', :p2, '%')) AND "
            +
            "  (:p3 IS NULL OR u.nombre ILIKE CONCAT('%', :p3, '%') OR u.apellido ILIKE CONCAT('%', :p3, '%') OR u.email ILIKE CONCAT('%', :p3, '%') OR u.documento_identidad ILIKE CONCAT('%', :p3, '%'))"
            +
            "))", countQuery = "SELECT COUNT(*) FROM usuario_perfil u WHERE " +
                    "(:estado IS NULL OR u.estado = CAST(:estado AS text)) AND " +
                    "(:busqueda IS NULL OR (" +
                    "  (:p1 IS NULL OR u.nombre ILIKE CONCAT('%', :p1, '%') OR u.apellido ILIKE CONCAT('%', :p1, '%') OR u.email ILIKE CONCAT('%', :p1, '%') OR u.documento_identidad ILIKE CONCAT('%', :p1, '%')) AND "
                    +
                    "  (:p2 IS NULL OR u.nombre ILIKE CONCAT('%', :p2, '%') OR u.apellido ILIKE CONCAT('%', :p2, '%') OR u.email ILIKE CONCAT('%', :p2, '%') OR u.documento_identidad ILIKE CONCAT('%', :p2, '%')) AND "
                    +
                    "  (:p3 IS NULL OR u.nombre ILIKE CONCAT('%', :p3, '%') OR u.apellido ILIKE CONCAT('%', :p3, '%') OR u.email ILIKE CONCAT('%', :p3, '%') OR u.documento_identidad ILIKE CONCAT('%', :p3, '%'))"
                    +
                    "))", nativeQuery = true)
    Page<UsuarioPerfil> findUsuariosConFiltros(
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            @Param("p1") String p1,
            @Param("p2") String p2,
            @Param("p3") String p3,
            Pageable pageable);

    /**
     * Busca usuarios con filtros y paginación (incluye filtro por IDs)
     * 
     * @param estado   Estado del usuario
     * @param busqueda Búsqueda por texto
     * @param p1       Primer término de búsqueda
     * @param p2       Segundo término de búsqueda
     * @param p3       Tercer término de búsqueda
     * @param userIds  Lista de IDs de usuarios
     * @param pageable Configuración de paginación
     * @return Página de usuarios
     */
    @Query(value = "SELECT * FROM usuario_perfil u WHERE " +
            "(:estado IS NULL OR u.estado = CAST(:estado AS text)) AND " +
            "(:busqueda IS NULL OR (" +
            "  (:p1 IS NULL OR u.nombre ILIKE CONCAT('%', :p1, '%') OR u.apellido ILIKE CONCAT('%', :p1, '%') OR u.email ILIKE CONCAT('%', :p1, '%') OR u.documento_identidad ILIKE CONCAT('%', :p1, '%')) AND "
            +
            "  (:p2 IS NULL OR u.nombre ILIKE CONCAT('%', :p2, '%') OR u.apellido ILIKE CONCAT('%', :p2, '%') OR u.email ILIKE CONCAT('%', :p2, '%') OR u.documento_identidad ILIKE CONCAT('%', :p2, '%')) AND "
            +
            "  (:p3 IS NULL OR u.nombre ILIKE CONCAT('%', :p3, '%') OR u.apellido ILIKE CONCAT('%', :p3, '%') OR u.email ILIKE CONCAT('%', :p3, '%') OR u.documento_identidad ILIKE CONCAT('%', :p3, '%'))"
            +
            ")) AND " +
            "(:userIds IS NULL OR u.id_usuario IN (:userIds))", countQuery = "SELECT COUNT(*) FROM usuario_perfil u WHERE "
                    +
                    "(:estado IS NULL OR u.estado = CAST(:estado AS text)) AND " +
                    "(:busqueda IS NULL OR (" +
                    "  (:p1 IS NULL OR u.nombre ILIKE CONCAT('%', :p1, '%') OR u.apellido ILIKE CONCAT('%', :p1, '%') OR u.email ILIKE CONCAT('%', :p1, '%') OR u.documento_identidad ILIKE CONCAT('%', :p1, '%')) AND "
                    +
                    "  (:p2 IS NULL OR u.nombre ILIKE CONCAT('%', :p2, '%') OR u.apellido ILIKE CONCAT('%', :p2, '%') OR u.email ILIKE CONCAT('%', :p2, '%') OR u.documento_identidad ILIKE CONCAT('%', :p2, '%')) AND "
                    +
                    "  (:p3 IS NULL OR u.nombre ILIKE CONCAT('%', :p3, '%') OR u.apellido ILIKE CONCAT('%', :p3, '%') OR u.email ILIKE CONCAT('%', :p3, '%') OR u.documento_identidad ILIKE CONCAT('%', :p3, '%'))"
                    +
                    ")) AND " +
                    "(:userIds IS NULL OR u.id_usuario IN (:userIds))", nativeQuery = true)
    Page<UsuarioPerfil> findUsuariosConFiltrosYRoles(
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            @Param("p1") String p1,
            @Param("p2") String p2,
            @Param("p3") String p3,
            @Param("userIds") List<Long> userIds,
            Pageable pageable);

    /**
     * Busca usuarios con filtros sin paginación
     * 
     * @param estado   Estado del usuario
     * @param busqueda Búsqueda por texto
     * @param p1       Primer término de búsqueda
     * @param p2       Segundo término de búsqueda
     * @param p3       Tercer término de búsqueda
     * @return Lista de usuarios
     */
    @Query(value = "SELECT * FROM usuario_perfil u WHERE " +
            "(:estado IS NULL OR u.estado = CAST(:estado AS text)) AND " +
            "(:busqueda IS NULL OR (" +
            "  (:p1 IS NULL OR u.nombre ILIKE CONCAT('%', :p1, '%') OR u.apellido ILIKE CONCAT('%', :p1, '%') OR u.email ILIKE CONCAT('%', :p1, '%') OR u.documento_identidad ILIKE CONCAT('%', :p1, '%')) AND "
            +
            "  (:p2 IS NULL OR u.nombre ILIKE CONCAT('%', :p2, '%') OR u.apellido ILIKE CONCAT('%', :p2, '%') OR u.email ILIKE CONCAT('%', :p2, '%') OR u.documento_identidad ILIKE CONCAT('%', :p2, '%')) AND "
            +
            "  (:p3 IS NULL OR u.nombre ILIKE CONCAT('%', :p3, '%') OR u.apellido ILIKE CONCAT('%', :p3, '%') OR u.email ILIKE CONCAT('%', :p3, '%') OR u.documento_identidad ILIKE CONCAT('%', :p3, '%'))"
            +
            "))", nativeQuery = true)
    List<UsuarioPerfil> findUsuariosConFiltrosSinPaginacion(
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            @Param("p1") String p1,
            @Param("p2") String p2,
            @Param("p3") String p3);
}
