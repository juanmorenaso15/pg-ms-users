package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

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

}