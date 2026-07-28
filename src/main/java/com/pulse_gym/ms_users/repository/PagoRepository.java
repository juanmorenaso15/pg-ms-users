package com.pulse_gym.ms_users.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pulse_gym.lb_common.entity.user.Pago;
import com.pulse_gym.lb_common.enums.EnumMetodoPago;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    /**
     * Busca todos los pagos de un socio específico, ordenados del más reciente al
     * más antiguo
     * 
     * @param idSocio ID del socio
     * @return Lista de pagos del socio
     */
    @Query("SELECT p FROM Pago p WHERE p.socioMembresia.socio.idUsuario = :idSocio ORDER BY p.fechaPago DESC")
    List<Pago> findBySocioId(@Param("idSocio") Long idSocio);

    /**
     * Busca pagos de un socio en un rango de fechas específico
     * 
     * @param idSocio     ID del socio
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin    Fecha de fin del rango
     * @return Lista de pagos del socio en el rango de fechas
     */
    @Query("SELECT p FROM Pago p WHERE p.socioMembresia.socio.idUsuario = :idSocio AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin ORDER BY p.fechaPago DESC")
    List<Pago> findBySocioIdAndFechaPagoBetween(@Param("idSocio") Long idSocio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    /**
     * Busca pagos por método de pago
     * 
     * @param metodoPago Método de pago a filtrar
     * @return Lista de pagos con el método de pago especificado
     */
    List<Pago> findByMetodoPago(EnumMetodoPago metodoPago);

    /**
     * Busca todos los pagos que han sido anulados
     * 
     * @return Lista de pagos anulados
     */
    List<Pago> findByAnuladoTrue();

    /**
     * Busca todos los pagos de una membresía asignada específica, ordenados del más
     * reciente al más antiguo
     * 
     * @param idSocioMembresia ID de la membresía asignada
     * @return Lista de pagos asociados a la membresía
     */
    List<Pago> findBySocioMembresia_IdSocioMembresiaOrderByFechaPagoDesc(Long idSocioMembresia);
}