package com.pulse_gym.ms_users.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pulse_gym.lb_common.dto.CalculoMembresiaFlexibleDTO;
import com.pulse_gym.lb_common.dto.MembresiaConSociosDTO;
import com.pulse_gym.lb_common.dto.MembresiaFlexibleCalculadaDTO;
import com.pulse_gym.lb_common.dto.MembresiaRequestDTO;
import com.pulse_gym.lb_common.dto.MembresiaResponseDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.dto.SocioAsignadoDTO;
import com.pulse_gym.lb_common.entity.user.Membresia;
import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.enums.EnumTipoDuracion;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.MembresiaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembresiaService {

    /** El repositorio de membresías */
    private final MembresiaRepository membresiaRepository;

    /**
     * Convierte una entidad Membresia a un DTO de respuesta
     * 
     * @param membresia La entidad de membresía a convertir
     * @return Un DTO con la información de la membresía para la respuesta
     */
    private MembresiaResponseDTO convertirAResponseDTO(Membresia membresia) {
        MembresiaResponseDTO dto = new MembresiaResponseDTO();
        dto.setIdMembresia(membresia.getIdMembresia());
        dto.setNombre(membresia.getNombre());
        dto.setPrecioTotal(membresia.getPrecioTotal());
        dto.setCantidad(membresia.getCantidad());
        dto.setTipoDuracion(membresia.getTipoDuracion().name());
        dto.setDuracionDescripcion(membresia.getDuracionDescripcion());
        dto.setIncluyeIA(membresia.getIncluyeIA());
        dto.setEsFlexible(membresia.getEsFlexible());
        dto.setPrecioPorDia(membresia.getPrecioPorDia());
        dto.setBeneficios(membresia.getBeneficios());
        dto.setRestricciones(membresia.getRestricciones());
        dto.setActivo(membresia.getActivo());
        return dto;
    }

    /**
     * Crea una nueva membresía
     * 
     * @param requestDTO Los datos para crear la membresía
     * @param userRol    El rol del usuario que realiza la acción
     * @return Un mensaje global con la información de la membresía creada
     */
    @Transactional
    public MessegeGlobalDTO crearMembresia(MembresiaRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        if (membresiaRepository.existsByNombreAndActivoTrue(requestDTO.getNombre())) {
            throw new RuntimeException("Ya existe una membresía activa con el nombre: " + requestDTO.getNombre());
        }

        EnumTipoDuracion tipoDuracion;
        try {
            tipoDuracion = EnumTipoDuracion.valueOf(requestDTO.getTipoDuracion().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Tipo de duración no válido. Valores: DIA, SEMANA, MES, TRIMESTRE, SEMESTRE, ANUAL");
        }

        if (requestDTO.getPrecioPorDia() == null || requestDTO.getPrecioPorDia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El precio por día es obligatorio y debe ser mayor a 0");
        }

        Membresia membresia = new Membresia();
        membresia.setNombre(requestDTO.getNombre());
        membresia.setCantidad(requestDTO.getCantidad() != null ? requestDTO.getCantidad() : 1);
        membresia.setTipoDuracion(tipoDuracion);
        membresia.setIncluyeIA(requestDTO.getIncluyeIA());
        membresia.setEsFlexible(requestDTO.getEsFlexible());
        membresia.setPrecioPorDia(requestDTO.getPrecioPorDia());
        membresia.setBeneficios(requestDTO.getBeneficios());
        membresia.setRestricciones(requestDTO.getRestricciones());
        membresia.setActivo(requestDTO.getActivo() != null ? requestDTO.getActivo() : true);

        BigDecimal precioTotalCalculado = membresia.calcularPrecioTotal();
        membresia.setPrecioTotal(precioTotalCalculado);

        membresiaRepository.save(membresia);

        String tipoMembresia = requestDTO.getEsFlexible() ? "Flexible " : "";
        String iaTexto = requestDTO.getIncluyeIA() ? "con IA" : "sin IA";

        return new MessegeGlobalDTO(
                String.format("Membresía %s%s %s creada correctamente. Duración: %s, Precio total: $%,.0f",
                        tipoMembresia, requestDTO.getNombre(), iaTexto,
                        membresia.getDuracionDescripcion(), precioTotalCalculado));
    }

    /**
     * Consulta las membresías activas
     * 
     * @param userRol    El rol del usuario que realiza la acción
     * @param incluyeIA  Filtro por inclusión de IA
     * @param esFlexible Filtro por flexibilidad
     * @return Una lista de DTOs con la información de las membresías consultadas
     */
    @Transactional(readOnly = true)
    public List<MembresiaResponseDTO> consultarMembresias(String userRol, Boolean incluyeIA, Boolean esFlexible) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        List<Membresia> membresias = membresiaRepository.findByActivoTrue();

        if (incluyeIA != null) {
            membresias = membresias.stream()
                    .filter(m -> m.getIncluyeIA().equals(incluyeIA))
                    .collect(Collectors.toList());
        }

        if (esFlexible != null) {
            membresias = membresias.stream()
                    .filter(m -> m.getEsFlexible().equals(esFlexible))
                    .collect(Collectors.toList());
        }

        if (membresias.isEmpty()) {
            throw new RuntimeException("No hay membresías activas disponibles");
        }

        return membresias.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza una membresía existente
     * 
     * @param idMembresia El ID de la membresía a actualizar
     * @param requestDTO  Los datos para actualizar la membresía
     * @param userRol     El rol del usuario que realiza la acción
     * @return
     */
    @Transactional
    public MessegeGlobalDTO actualizarMembresia(Long idMembresia, MembresiaRequestDTO requestDTO, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        Membresia membresia = membresiaRepository.findById(idMembresia)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con ID: " + idMembresia));

        boolean necesitaRecalcular = false;

        if (requestDTO.getNombre() != null) {
            if (!membresia.getNombre().equals(requestDTO.getNombre()) &&
                    membresiaRepository.existsByNombreAndActivoTrue(requestDTO.getNombre())) {
                throw new RuntimeException("Ya existe otra membresía activa con el nombre: " + requestDTO.getNombre());
            }
            membresia.setNombre(requestDTO.getNombre());
        }

        if (requestDTO.getCantidad() != null) {
            if (requestDTO.getCantidad() < 1) {
                throw new RuntimeException("La cantidad debe ser al menos 1");
            }
            membresia.setCantidad(requestDTO.getCantidad());
            necesitaRecalcular = true;
        }

        if (requestDTO.getTipoDuracion() != null) {
            try {
                EnumTipoDuracion tipoDuracion = EnumTipoDuracion.valueOf(requestDTO.getTipoDuracion().toUpperCase());
                membresia.setTipoDuracion(tipoDuracion);
                necesitaRecalcular = true;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Tipo de duración no válido");
            }
        }

        if (requestDTO.getIncluyeIA() != null) {
            membresia.setIncluyeIA(requestDTO.getIncluyeIA());
            necesitaRecalcular = true;
        }

        if (requestDTO.getEsFlexible() != null) {
            membresia.setEsFlexible(requestDTO.getEsFlexible());
        }

        if (requestDTO.getPrecioPorDia() != null) {
            if (requestDTO.getPrecioPorDia().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El precio por día debe ser mayor a 0");
            }
            membresia.setPrecioPorDia(requestDTO.getPrecioPorDia());
            necesitaRecalcular = true;
        }

        if (requestDTO.getBeneficios() != null) {
            membresia.setBeneficios(requestDTO.getBeneficios());
        }

        if (requestDTO.getRestricciones() != null) {
            membresia.setRestricciones(requestDTO.getRestricciones());
        }

        if (necesitaRecalcular) {
            BigDecimal precioTotalCalculado = membresia.calcularPrecioTotal();
            membresia.setPrecioTotal(precioTotalCalculado);
        }

        membresiaRepository.save(membresia);

        return new MessegeGlobalDTO(String.format("Membresía actualizada correctamente. Nuevo precio total: $%,.0f",
                membresia.getPrecioTotal()));
    }

    /**
     * Elimina (desactiva) una membresía existente
     * 
     * @param idMembresia El ID de la membresía a eliminar
     * @param userRol     El rol del usuario que realiza la acción
     * @return Un mensaje global con la información de la membresía eliminada
     */
    @Transactional
    public MessegeGlobalDTO eliminarMembresia(Long idMembresia, String userRol) {
        ValidacionDeRoles.validarAdminORecepcionista(userRol);

        Membresia membresia = membresiaRepository.findById(idMembresia)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con ID: " + idMembresia));

        membresia.setActivo(false);
        membresiaRepository.save(membresia);

        return new MessegeGlobalDTO("Membresía '" + membresia.getNombre() + "' eliminada (desactivada) correctamente");
    }

    /**
     * Consulta las membresías activas filtradas por categoría (con o sin IA)
     * 
     * @param incluyeIA Indica si se deben incluir solo membresías con IA (true) o
     *                  sin IA (false)
     * @param userRol   El rol del usuario que realiza la acción (obtenido del
     *                  header "X-User-Rol")
     * @return Una lista con las membresías que cumplen con los criterios de
     *         búsqueda
     */
    @Transactional(readOnly = true)
    public List<MembresiaResponseDTO> obtenerMembresiasPorCategoria(Boolean incluyeIA, String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        List<Membresia> membresias = membresiaRepository.findByActivoTrueAndIncluyeIA(incluyeIA);

        if (membresias.isEmpty()) {
            String categoria = incluyeIA ? "con IA" : "sin IA";
            throw new RuntimeException("No hay membresías activas " + categoria + " disponibles");
        }

        return membresias.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calcula el precio total de una membresía flexible basada en la cantidad de
     * días y la categoría de IA
     * 
     * @param calculoDTO Los datos necesarios para realizar el cálculo de la
     *                   membresía flexible, incluyendo el ID de la membresía, la
     *                   cantidad de días y si incluye o no IA
     * @param userRol    El rol del usuario que realiza la acción (obtenido del
     *                   header "X-User-Rol")
     * @return Un DTO con la información de la membresía flexible calculada,
     *         incluyendo el precio total basado en los días y la categoría de
     */
    @Transactional(readOnly = true)
    public MembresiaFlexibleCalculadaDTO calcularMembresiaFlexible(CalculoMembresiaFlexibleDTO calculoDTO,
            String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        Membresia membresia = membresiaRepository.findById(calculoDTO.getIdMembresia())
                .orElseThrow(
                        () -> new RuntimeException("Membresía no encontrada con ID: " + calculoDTO.getIdMembresia()));

        if (!membresia.getEsFlexible()) {
            throw new RuntimeException("Esta membresía no es flexible. No se puede calcular por días");
        }

        if (membresia.getPrecioPorDia() == null) {
            throw new RuntimeException("La membresía flexible no tiene precio por día configurado");
        }

        if (!membresia.getIncluyeIA().equals(calculoDTO.getIncluyeIA())) {
            throw new RuntimeException("La categoría de IA no coincide con la membresía seleccionada");
        }

        BigDecimal precioTotalCalculado = membresia.getPrecioPorDia()
                .multiply(BigDecimal.valueOf(calculoDTO.getCantidadDias()));

        MembresiaFlexibleCalculadaDTO resultado = new MembresiaFlexibleCalculadaDTO();
        resultado.setIdMembresia(membresia.getIdMembresia());
        resultado.setNombre(membresia.getNombre());
        resultado.setCantidadDias(calculoDTO.getCantidadDias());
        resultado.setPrecioPorDia(membresia.getPrecioPorDia());
        resultado.setPrecioTotalCalculado(precioTotalCalculado);
        resultado.setIncluyeIA(membresia.getIncluyeIA());

        return resultado;
    }

    @Transactional(readOnly = true)
    public MembresiaConSociosDTO consultarMembresiaConSocios(Long idMembresia, String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        Membresia membresia = membresiaRepository.findByIdWithSociosAsignados(idMembresia)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con ID: " + idMembresia));

        return convertirAMembresiaConSociosDTO(membresia);
    }

    /**
     * Consulta una membresía por ID con solo socios activos
     * 
     * @param idMembresia ID de la membresía a consultar
     * @param userRol     Rol del usuario autenticado
     * @return DTO con la información de la membresía y sus socios activos
     */
    @Transactional(readOnly = true)
    public MembresiaConSociosDTO consultarMembresiaConSociosActivos(Long idMembresia, String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        Membresia membresia = membresiaRepository.findByIdWithSociosActivos(idMembresia)
                .orElseThrow(() -> new RuntimeException("Membresía no encontrada con ID: " + idMembresia));

        return convertirAMembresiaConSociosDTO(membresia);
    }

    /**
     * Obtiene todas las membresías activas con sus socios asignados
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de DTOs con información de membresías y sus socios
     */
    @Transactional(readOnly = true)
    public List<MembresiaConSociosDTO> consultarTodasMembresiasConSocios(String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        List<Membresia> membresias = membresiaRepository.findAllWithSociosAsignados();

        if (membresias.isEmpty()) {
            throw new RuntimeException("No hay membresías activas disponibles");
        }

        return membresias.stream()
                .map(this::convertirAMembresiaConSociosDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Membresia a un DTO de respuesta que incluye los socios
     * asignados
     * 
     * @param membresia La entidad de membresía a convertir
     * @return Un DTO con la información de la membresía y sus socios asignados
     */
    private MembresiaConSociosDTO convertirAMembresiaConSociosDTO(Membresia membresia) {
        List<SocioAsignadoDTO> sociosDTO = membresia.getSocioMembresias().stream()
                .map(this::convertirSocioMembresiaASocioAsignadoDTO)
                .collect(Collectors.toList());

        return MembresiaConSociosDTO.builder()
                .idMembresia(membresia.getIdMembresia())
                .nombre(membresia.getNombre())
                .precioTotal(membresia.getPrecioTotal())
                .cantidad(membresia.getCantidad())
                .tipoDuracion(membresia.getTipoDuracion().name())
                .duracionDescripcion(membresia.getDuracionDescripcion())
                .incluyeIA(membresia.getIncluyeIA())
                .esFlexible(membresia.getEsFlexible())
                .precioPorDia(membresia.getPrecioPorDia())
                .beneficios(membresia.getBeneficios())
                .restricciones(membresia.getRestricciones())
                .activo(membresia.getActivo())
                .sociosAsignados(sociosDTO)
                .totalSociosAsignados(sociosDTO.size())
                .build();
    }


    /**
     * Convierte una entidad SocioMembresia a un DTO de respuesta que incluye la
     * información del socio asignado
     * 
     * @param socioMembresia La entidad de socio-membresía a convertir
     * @return Un DTO con la información del socio asignado y su membresía
     */
    private SocioAsignadoDTO convertirSocioMembresiaASocioAsignadoDTO(SocioMembresia socioMembresia) {
        Long diasRestantes = socioMembresia.getDiasRestantes();

        String nombreCompleto = socioMembresia.getSocio().getNombre();
        if (socioMembresia.getSocio().getApellido() != null && !socioMembresia.getSocio().getApellido().isEmpty()) {
            nombreCompleto += " " + socioMembresia.getSocio().getApellido();
        }

        return SocioAsignadoDTO.builder()
                .idSocio(socioMembresia.getSocio().getIdUsuario())
                .nombreCompleto(nombreCompleto)
                .email(socioMembresia.getSocio().getEmail())
                .telefono(socioMembresia.getSocio().getTelefono())
                .idSocioMembresia(socioMembresia.getIdSocioMembresia())
                .fechaInicio(socioMembresia.getFechaInicio())
                .fechaVencimiento(socioMembresia.getFechaVencimiento())
                .estado(socioMembresia.getEstado().name())
                .diasRestantes(diasRestantes)
                .observaciones(socioMembresia.getObservaciones())
                .fechaCreacion(socioMembresia.getFechaCreacion())
                .build();
    }

}
