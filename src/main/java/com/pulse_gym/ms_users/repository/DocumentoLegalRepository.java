package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pulse_gym.lb_common.entity.user.DocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumEstadoDocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumTipoDocumentoLegal;

import feign.Param;

public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, Long> {

    /**
     * Busca los documentos legales de un usuario por su ID y estado.
     * 
     * @param fkIdUsuario El ID del usuario es obligatorio
     * @param estado      El estado del documento es obligatorio
     * @return Una lista de documentos legales que coinciden con el ID del usuario y
     *         el estado especificados
     */
    List<DocumentoLegal> findByUsuario_IdUsuarioAndEstado(Long idUsuario, EnumEstadoDocumentoLegal estado);

    /**
     * Busca un documento legal por su ID y estado.
     * 
     * @param idDocumento El ID del documento es obligatorio
     * @param estado      El estado del documento es obligatorio
     * @return Un Optional que contiene el documento legal si se encuentra, o vacío
     *         si no se encuentra o si el estado no coincide
     */
    Optional<DocumentoLegal> findByIdDocumentoAndEstado(Long idDocumento, EnumEstadoDocumentoLegal estado);

    /**
     * Busca un documento legal por el ID del usuario, el tipo de documento y el
     * estado.
     * 
     * @param idUsuario El ID del usuario es obligatorio
     * @param tipo      El tipo de documento es obligatorio
     * @param estado    El estado del documento es obligatorio
     * @return Un Optional que contiene el documento legal si se encuentra, o vacío
     *         si no se encuentra o si el estado no coincide
     */
    @Query("SELECT d FROM DocumentoLegal d WHERE d.usuario.idUsuario = :idUsuario AND d.tipoDocumento = :tipo AND d.estado = :estado")
    Optional<DocumentoLegal> findDocumentoPorTipo(
            @Param("idUsuario") Long idUsuario,
            @Param("tipo") EnumTipoDocumentoLegal tipo,
            @Param("estado") EnumEstadoDocumentoLegal estado);

    /**
     * Busca los documentos legales por su estado.
     * 
     * @param estado El estado del documento es obligatorio
     * @return Una lista de documentos legales que coinciden con el estado
     *         especificado
     */
    List<DocumentoLegal> findByEstado(EnumEstadoDocumentoLegal estado);

    /**
     * Consulta documentos legales con filtros y paginación
     * 
     * @param estado        Filtro por estado del documento
     * @param tipoDocumento Filtro por tipo de documento
     * @param search        Búsqueda por nombre o apellido del usuario
     * @param pageable      Configuración de paginación
     * @return Página de documentos legales
     */
    @Query("SELECT d FROM DocumentoLegal d WHERE " +
            "(:estado IS NULL OR d.estado = :estado) AND " +
            "(:tipoDocumento IS NULL OR d.tipoDocumento = :tipoDocumento) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(d.usuario.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(d.usuario.apellido) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(CONCAT(d.usuario.nombre, ' ', d.usuario.apellido)) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DocumentoLegal> consultarDocumentosPaginadosFiltros(
            @Param("estado") EnumEstadoDocumentoLegal estado,
            @Param("tipoDocumento") EnumTipoDocumentoLegal tipoDocumento,
            @Param("search") String search,
            Pageable pageable);

}
