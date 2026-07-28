package com.pulse_gym.ms_users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pulse_gym.lb_common.entity.user.DocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumEstadoDocumentoLegal;
import com.pulse_gym.lb_common.enums.EnumTipoDocumentoLegal;

import feign.Param;

public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, Long> {

    /**
     * Busca los documentos legales de un usuario por su ID y estado.       
     * @param fkIdUsuario El ID del usuario es obligatorio
     * @param estado El estado del documento es obligatorio
     * @return Una lista de documentos legales que coinciden con el ID del usuario y el estado especificados
     */
    List<DocumentoLegal> findByUsuario_IdUsuarioAndEstado(Long idUsuario, EnumEstadoDocumentoLegal estado);

    /**
     * Busca un documento legal por su ID y estado.
     * @param idDocumento El ID del documento es obligatorio
     * @param estado El estado del documento es obligatorio
     * @return Un Optional que contiene el documento legal si se encuentra, o vacío si no se encuentra o si el estado no coincide
     */
    Optional<DocumentoLegal> findByIdDocumentoAndEstado(Long idDocumento, EnumEstadoDocumentoLegal estado);

    /**
     * Busca un documento legal por el ID del usuario, el tipo de documento y el estado.
     * @param idUsuario El ID del usuario es obligatorio
     * @param tipo El tipo de documento es obligatorio
     * @param estado El estado del documento es obligatorio
     * @return Un Optional que contiene el documento legal si se encuentra, o vacío si no se encuentra o si el estado no coincide
     */
    @Query("SELECT d FROM DocumentoLegal d WHERE d.usuario.idUsuario = :idUsuario AND d.tipoDocumento = :tipo AND d.estado = :estado")
    Optional<DocumentoLegal> findDocumentoPorTipo(
            @Param("idUsuario") Long idUsuario,
            @Param("tipo") EnumTipoDocumentoLegal tipo,
            @Param("estado") EnumEstadoDocumentoLegal estado);

    /**
     * Busca los documentos legales por su estado.
     * @param estado El estado del documento es obligatorio
     * @return Una lista de documentos legales que coinciden con el estado especificado
     */
    List<DocumentoLegal> findByEstado(EnumEstadoDocumentoLegal estado);

}

