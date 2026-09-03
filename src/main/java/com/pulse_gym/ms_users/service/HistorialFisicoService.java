package com.pulse_gym.ms_users.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.client.AuthServiceClient;
import com.pulse_gym.lb_common.dto.EvolucionFisicaDTO;
import com.pulse_gym.lb_common.dto.HistorialFisicoRequestDTO;
import com.pulse_gym.lb_common.dto.HistorialFisicoResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.entity.user.HistorialFisico;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.HistorialFisicoRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistorialFisicoService {

    /** Repositorio de historial físico */
    private final HistorialFisicoRepository historialRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Cliente Feign para consultar el servicio de autenticación */
    private final AuthServiceClient authServiceClient;

    /**
     * Convierte una entidad HistorialFisico a HistorialFisicoResponseDTO
     * 
     * @param historial Entidad a convertir
     * @return DTO del historial físico
     */
    private HistorialFisicoResponseDTO convertirAResponseDTO(HistorialFisico historial) {
        HistorialFisicoResponseDTO dto = new HistorialFisicoResponseDTO();
        dto.setIdHistorialFisico(historial.getIdHistorialFisico());
        dto.setIdSocio(historial.getSocio().getIdUsuario());
        dto.setNombreSocio(historial.getSocio().getNombre() + " " + historial.getSocio().getApellido());

        if (historial.getRecepcionista() != null) {
            dto.setIdRecepcionista(historial.getRecepcionista().getIdUsuario());
            dto.setNombreRecepcionista(
                    historial.getRecepcionista().getNombre() + " " + historial.getRecepcionista().getApellido());
        }

        dto.setFechaMedicion(historial.getFechaMedicion());
        dto.setPesoKg(historial.getPesoKg());
        dto.setAlturaCm(historial.getAlturaCm());

        if (historial.getPesoKg() != null && historial.getAlturaCm() != null
                && historial.getAlturaCm().doubleValue() > 0) {
            double alturaM = historial.getAlturaCm().doubleValue() / 100.0;
            double imc = historial.getPesoKg().doubleValue() / (alturaM * alturaM);
            dto.setImc(BigDecimal.valueOf(imc).setScale(2, RoundingMode.HALF_UP));
        }

        dto.setPorcentajeGrasa(historial.getPorcentajeGrasa());
        dto.setPorcentajeMusculo(historial.getPorcentajeMusculo());
        dto.setCuelloCm(historial.getCuelloCm());
        dto.setCinturaEscapularCm(historial.getCinturaEscapularCm());
        dto.setCinturaCm(historial.getCinturaCm());
        dto.setCaderaCm(historial.getCaderaCm());
        dto.setToraxCm(historial.getToraxCm());
        dto.setPechoCm(historial.getPechoCm());
        dto.setBrazoIzqCm(historial.getBrazoIzqCm());
        dto.setBrazoDerCm(historial.getBrazoDerCm());
        dto.setPiernaIzqCm(historial.getPiernaIzqCm());
        dto.setPiernaDerCm(historial.getPiernaDerCm());
        dto.setPantorrillaIzqCm(historial.getPantorrillaIzqCm());
        dto.setPantorrillaDerCm(historial.getPantorrillaDerCm());

        return dto;
    }

    /**
     * Registra una nueva medición física para un socio
     * 
     * @param requestDTO        Datos de la medición
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Mensaje de confirmación
     * @throws RuntimeException Si el socio no existe o no es socio
     */
    @Transactional
    public MessegeGlobalDTO registrarMedicion(HistorialFisicoRequestDTO requestDTO, String userRol,
            Long userIdAutenticado) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        UsuarioPerfil socio = usuarioRepository.findById(requestDTO.getIdSocio())
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + requestDTO.getIdSocio()));

        EnumRol rolSocio = authServiceClient.obtenerRolPorEmail(socio.getEmail());
        if (rolSocio == null || rolSocio != EnumRol.socio) {
            throw new RuntimeException("El usuario no es un socio. Rol actual: " + rolSocio);
        }

        UsuarioPerfil recepcionista = null;
        if (requestDTO.getIdRecepcionista() != null) {
            recepcionista = usuarioRepository.findById(requestDTO.getIdRecepcionista())
                    .orElseThrow(() -> new RuntimeException(
                            "Recepcionista no encontrado con ID: " + requestDTO.getIdRecepcionista()));
        }

        HistorialFisico historial = new HistorialFisico();
        historial.setSocio(socio);
        historial.setRecepcionista(recepcionista);
        historial.setFechaMedicion(
                requestDTO.getFechaMedicion() != null ? requestDTO.getFechaMedicion() : LocalDateTime.now());
        historial.setPesoKg(requestDTO.getPesoKg());
        historial.setAlturaCm(requestDTO.getAlturaCm());
        historial.setCuelloCm(requestDTO.getCuelloCm());
        historial.setCinturaEscapularCm(requestDTO.getCinturaEscapularCm());
        historial.setCinturaCm(requestDTO.getCinturaCm());
        historial.setCaderaCm(requestDTO.getCaderaCm());
        historial.setToraxCm(requestDTO.getToraxCm());
        historial.setPechoCm(requestDTO.getPechoCm());
        historial.setBrazoIzqCm(requestDTO.getBrazoIzqCm());
        historial.setBrazoDerCm(requestDTO.getBrazoDerCm());
        historial.setPiernaIzqCm(requestDTO.getPiernaIzqCm());
        historial.setPiernaDerCm(requestDTO.getPiernaDerCm());
        historial.setPantorrillaIzqCm(requestDTO.getPantorrillaIzqCm());
        historial.setPantorrillaDerCm(requestDTO.getPantorrillaDerCm());

        calcularComposicionSiEsNecesario(historial, requestDTO, socio);

        historialRepository.save(historial);

        return new MessegeGlobalDTO("Medición física registrada correctamente para el socio: " + socio.getNombre());
    }

    /**
     * Calcula el porcentaje de grasa y músculo si no fueron proporcionados
     * directamente
     * 
     * @param historial  Entidad a actualizar
     * @param requestDTO DTO con los datos de la medición
     * @param socio      Socio para obtener datos adicionales
     */
    private void calcularComposicionSiEsNecesario(HistorialFisico historial, HistorialFisicoRequestDTO requestDTO,
            UsuarioPerfil socio) {
        BigDecimal peso = historial.getPesoKg();
        BigDecimal altura = historial.getAlturaCm();

        if ((altura == null || altura.doubleValue() <= 0) && socio.getPerfilMedico() != null
                && socio.getPerfilMedico().getEstaturaCm() != null) {
            altura = BigDecimal.valueOf(socio.getPerfilMedico().getEstaturaCm());
            historial.setAlturaCm(altura);
        }

        BigDecimal grasa = requestDTO != null ? requestDTO.getPorcentajeGrasa() : historial.getPorcentajeGrasa();
        BigDecimal musculo = requestDTO != null ? requestDTO.getPorcentajeMusculo() : historial.getPorcentajeMusculo();

        boolean calcularGrasa = (grasa == null || grasa.doubleValue() <= 0);
        boolean calcularMusculo = (musculo == null || musculo.doubleValue() <= 0);

        if (calcularGrasa && peso != null && peso.doubleValue() > 0) {
            double grasaCalculada = 0.0;
            BigDecimal cintura = historial.getCinturaCm();
            BigDecimal cuello = historial.getCuelloCm();
            BigDecimal cadera = historial.getCaderaCm();

            String sexoStr = socio.getSexo() != null ? socio.getSexo().name().toUpperCase() : "MASCULINO";
            boolean esFemenino = sexoStr.contains("FEM");

            double altCm = (altura != null && altura.doubleValue() > 0) ? altura.doubleValue() : 170.0;

            // Fórmula Marina EE.UU. (US Navy Body Fat Formula) para porcentaje exacto por
            // antropometría
            if (cintura != null && cuello != null && cintura.doubleValue() > cuello.doubleValue()) {
                if (esFemenino && cadera != null && cadera.doubleValue() > 0) {
                    double sumaCinCad = cintura.doubleValue() + cadera.doubleValue() - cuello.doubleValue();
                    if (sumaCinCad > 0) {
                        grasaCalculada = 495.0
                                / (1.29579 - 0.35004 * Math.log10(sumaCinCad) + 0.22100 * Math.log10(altCm)) - 450.0;
                    }
                } else {
                    double difCinCue = cintura.doubleValue() - cuello.doubleValue();
                    if (difCinCue > 0) {
                        grasaCalculada = 495.0
                                / (1.0324 - 0.19077 * Math.log10(difCinCue) + 0.15456 * Math.log10(altCm)) - 450.0;
                    }
                }
            }

            // Si faltan medidas secundarias (cuello/cadera), se aplica la fórmula basada en
            // IMC y Edad
            if (grasaCalculada <= 0.0) {
                double alturaM = altCm / 100.0;
                double imc = peso.doubleValue() / (alturaM * alturaM);

                int edad = 25;
                if (socio.getFechaNacimiento() != null) {
                    edad = Period.between(socio.getFechaNacimiento(), LocalDate.now()).getYears();
                }

                int factorSexo = esFemenino ? 0 : 1;
                grasaCalculada = (1.20 * imc) + (0.23 * edad) - (10.8 * factorSexo) - 5.4;
            }

            grasaCalculada = Math.max(4.0, Math.min(55.0, grasaCalculada));
            grasa = BigDecimal.valueOf(grasaCalculada).setScale(2, RoundingMode.HALF_UP);
        } else if (grasa == null) {
            grasa = BigDecimal.ZERO.setScale(2);
        }

        if (calcularMusculo) {
            if (grasa != null && grasa.doubleValue() > 0) {
                double porcentajeMasaMagra = 100.0 - grasa.doubleValue();
                double musculoCalculado = porcentajeMasaMagra * 0.52;
                musculo = BigDecimal.valueOf(musculoCalculado).setScale(2, RoundingMode.HALF_UP);
            } else {
                musculo = BigDecimal.ZERO.setScale(2);
            }
        } else if (musculo == null) {
            musculo = BigDecimal.ZERO.setScale(2);
        }

        historial.setPorcentajeGrasa(grasa);
        historial.setPorcentajeMusculo(musculo);
    }

    /**
     * Consulta el historial físico de un socio
     * 
     * @param idSocio           ID del socio
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @return Lista de registros del historial físico
     * @throws SecurityAuthorizationException Si el usuario no tiene permisos
     */
    @Transactional(readOnly = true)
    public List<HistorialFisicoResponseDTO> consultarHistorial(Long idSocio, String userRol, Long userIdAutenticado) {

        if (userRol.equals(EnumRol.socio.name())) {
            if (!userIdAutenticado.equals(idSocio)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede ver su propio historial físico");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) &&
                !userRol.equals(EnumRol.entrenador.name()) &&
                !userRol.equals(EnumRol.recepcionista.name())) {
            throw new SecurityAuthorizationException("Acceso denegado. Rol no autorizado: " + userRol);
        }

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        List<HistorialFisico> historial = historialRepository.findBySocio_IdUsuarioOrderByFechaMedicionDesc(idSocio);

        if (historial.isEmpty()) {
            throw new RuntimeException("El socio " + socio.getNombre() + " no tiene registros de historial físico");
        }

        return historial.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una medición física existente
     * 
     * @param idHistorial ID del registro a actualizar
     * @param requestDTO  Datos actualizados
     * @param userRol     Rol del usuario autenticado
     * @return Mensaje de confirmación
     */
    @Transactional
    public MessegeGlobalDTO actualizarMedicion(Long idHistorial, HistorialFisicoRequestDTO requestDTO, String userRol) {

        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        HistorialFisico historial = historialRepository.findById(idHistorial)
                .orElseThrow(() -> new RuntimeException("Registro de historial no encontrado con ID: " + idHistorial));

        if (requestDTO.getPesoKg() != null)
            historial.setPesoKg(requestDTO.getPesoKg());
        if (requestDTO.getAlturaCm() != null)
            historial.setAlturaCm(requestDTO.getAlturaCm());
        if (requestDTO.getPorcentajeGrasa() != null)
            historial.setPorcentajeGrasa(requestDTO.getPorcentajeGrasa());
        if (requestDTO.getPorcentajeMusculo() != null)
            historial.setPorcentajeMusculo(requestDTO.getPorcentajeMusculo());
        if (requestDTO.getCuelloCm() != null)
            historial.setCuelloCm(requestDTO.getCuelloCm());
        if (requestDTO.getCinturaEscapularCm() != null)
            historial.setCinturaEscapularCm(requestDTO.getCinturaEscapularCm());
        if (requestDTO.getCinturaCm() != null)
            historial.setCinturaCm(requestDTO.getCinturaCm());
        if (requestDTO.getCaderaCm() != null)
            historial.setCaderaCm(requestDTO.getCaderaCm());
        if (requestDTO.getToraxCm() != null)
            historial.setToraxCm(requestDTO.getToraxCm());
        if (requestDTO.getPechoCm() != null)
            historial.setPechoCm(requestDTO.getPechoCm());
        if (requestDTO.getBrazoIzqCm() != null)
            historial.setBrazoIzqCm(requestDTO.getBrazoIzqCm());
        if (requestDTO.getBrazoDerCm() != null)
            historial.setBrazoDerCm(requestDTO.getBrazoDerCm());
        if (requestDTO.getPiernaIzqCm() != null)
            historial.setPiernaIzqCm(requestDTO.getPiernaIzqCm());
        if (requestDTO.getPiernaDerCm() != null)
            historial.setPiernaDerCm(requestDTO.getPiernaDerCm());
        if (requestDTO.getPantorrillaIzqCm() != null)
            historial.setPantorrillaIzqCm(requestDTO.getPantorrillaIzqCm());
        if (requestDTO.getPantorrillaDerCm() != null)
            historial.setPantorrillaDerCm(requestDTO.getPantorrillaDerCm());

        calcularComposicionSiEsNecesario(historial, requestDTO, historial.getSocio());

        historialRepository.save(historial);

        return new MessegeGlobalDTO("Medición física actualizada correctamente");
    }

    /**
     * Obtiene la evolución física de un socio en un período de tiempo
     * 
     * @param idSocio           ID del socio
     * @param userRol           Rol del usuario autenticado
     * @param userIdAutenticado ID del usuario autenticado
     * @param fechaInicio       Fecha de inicio del período
     * @param fechaFin          Fecha de fin del período
     * @return DTO con la evolución física
     */
    @Transactional(readOnly = true)
    public EvolucionFisicaDTO obtenerEvolucion(Long idSocio, String userRol, Long userIdAutenticado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {

        if (userRol.equals(EnumRol.socio.name())) {
            if (!userIdAutenticado.equals(idSocio)) {
                throw new SecurityAuthorizationException("Acceso denegado. Solo puede ver su propia evolución");
            }
        } else if (!userRol.equals(EnumRol.administrador.name()) &&
                !userRol.equals(EnumRol.entrenador.name())) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Rol no autorizado para ver evolución: " + userRol);
        }

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now().minusMonths(6);
        }
        if (fechaFin == null) {
            fechaFin = LocalDateTime.now();
        }

        List<HistorialFisico> historial = historialRepository
                .findBySocio_IdUsuarioAndFechaMedicionBetweenOrderByFechaMedicionAsc(idSocio, fechaInicio, fechaFin);

        EvolucionFisicaDTO evolucion = new EvolucionFisicaDTO();
        evolucion.setIdSocio(idSocio);
        evolucion.setNombreSocio(socio.getNombre() + " " + socio.getApellido());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionPeso = historial.stream()
                .filter(h -> h.getPesoKg() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPesoKg()))
                .collect(Collectors.toList());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionGrasa = historial.stream()
                .filter(h -> h.getPorcentajeGrasa() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPorcentajeGrasa()))
                .collect(Collectors.toList());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionMusculo = historial.stream()
                .filter(h -> h.getPorcentajeMusculo() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPorcentajeMusculo()))
                .collect(Collectors.toList());

        evolucion.setEvolucionPeso(evolucionPeso);
        evolucion.setEvolucionGrasa(evolucionGrasa);
        evolucion.setEvolucionMusculo(evolucionMusculo);

        return evolucion;
    }

    /**
     * Obtiene todos los historiales físicos del sistema
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de todos los historiales
     */
    @Transactional(readOnly = true)
    public List<HistorialFisicoResponseDTO> obtenerTodosHistoriales(String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        List<HistorialFisico> historial = historialRepository.findAllByOrderByFechaMedicionDesc();

        return historial.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Consulta el historial físico del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol   Rol del usuario autenticado (debe ser SOCIO)
     * @param userEmail Email del socio autenticado (extraído del token)
     * @return Lista de registros del historial físico del socio autenticado
     * @throws SecurityAuthorizationException Si el usuario no es un socio
     * @throws RuntimeException               Si no se encuentra el socio o no tiene
     *                                        registros
     */
    @Transactional(readOnly = true)
    public List<HistorialFisicoResponseDTO> consultarMiHistorial(String userRol, String userEmail) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo los socios pueden consultar su propio historial físico.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        List<HistorialFisico> historial = historialRepository
                .findBySocio_IdUsuarioOrderByFechaMedicionDesc(socio.getIdUsuario());

        if (historial.isEmpty()) {
            throw new RuntimeException("No tienes registros de historial físico");
        }

        return historial.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la evolución física del socio autenticado.
     * Usa el email del token para identificar al socio.
     * 
     * @param userRol     Rol del usuario autenticado (debe ser SOCIO)
     * @param userEmail   Email del socio autenticado (extraído del token)
     * @param fechaInicio Fecha de inicio del período (opcional)
     * @param fechaFin    Fecha de fin del período (opcional)
     * @return DTO con la evolución física del socio autenticado
     * @throws SecurityAuthorizationException Si el usuario no es un socio
     * @throws RuntimeException               Si no se encuentra el socio
     */
    @Transactional(readOnly = true)
    public EvolucionFisicaDTO obtenerMiEvolucion(String userRol, String userEmail,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (!EnumRol.socio.name().equals(userRol)) {
            throw new SecurityAuthorizationException(
                    "Acceso denegado. Solo los socios pueden consultar su propia evolución física.");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new SecurityAuthorizationException("Email de usuario no proporcionado");
        }

        UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now().minusMonths(6);
        }
        if (fechaFin == null) {
            fechaFin = LocalDateTime.now();
        }

        List<HistorialFisico> historial = historialRepository
                .findBySocio_IdUsuarioAndFechaMedicionBetweenOrderByFechaMedicionAsc(
                        socio.getIdUsuario(), fechaInicio, fechaFin);

        EvolucionFisicaDTO evolucion = new EvolucionFisicaDTO();
        evolucion.setIdSocio(socio.getIdUsuario());
        evolucion.setNombreSocio(socio.getNombre() + " " + socio.getApellido());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionPeso = historial.stream()
                .filter(h -> h.getPesoKg() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPesoKg()))
                .collect(Collectors.toList());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionGrasa = historial.stream()
                .filter(h -> h.getPorcentajeGrasa() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPorcentajeGrasa()))
                .collect(Collectors.toList());

        List<EvolucionFisicaDTO.PuntoEvolucion> evolucionMusculo = historial.stream()
                .filter(h -> h.getPorcentajeMusculo() != null)
                .map(h -> new EvolucionFisicaDTO.PuntoEvolucion(h.getFechaMedicion(), h.getPorcentajeMusculo()))
                .collect(Collectors.toList());

        evolucion.setEvolucionPeso(evolucionPeso);
        evolucion.setEvolucionGrasa(evolucionGrasa);
        evolucion.setEvolucionMusculo(evolucionMusculo);

        return evolucion;
    }

    /**
     * Obtiene historiales físicos con filtros y paginación
     * 
     * @param userRol     Rol del usuario autenticado
     * @param idSocio     Filtro por ID del socio
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin    Fecha de fin del rango
     * @param busqueda    Búsqueda por nombre o apellido
     * @param pageable    Configuración de paginación
     * @return Página de historiales físicos
     */
    @Transactional(readOnly = true)
    public Page<HistorialFisicoResponseDTO> obtenerHistorialesPaginados(
            String userRol, Long idSocio, LocalDateTime fechaInicio, LocalDateTime fechaFin, String busqueda,
            Pageable pageable) {

        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        // Enviar solo el texto limpio (o null si está vacío)
        String busquedaParam = (busqueda == null || busqueda.trim().isEmpty()) ? null : busqueda.trim();

        Page<HistorialFisico> paginaHistorial = historialRepository.findWithFilters(
                idSocio, fechaInicio, fechaFin, busquedaParam, pageable);

        return paginaHistorial.map(this::convertirAResponseDTO);
    }
}