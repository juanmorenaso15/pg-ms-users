package com.pulse_gym.ms_users.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pulse_gym.lb_common.dto.EjercicioRequestDTO;
import com.pulse_gym.lb_common.dto.EjercicioResponseDTO;
import com.pulse_gym.lb_common.dto.EjercicioUpdateDTO;
import com.pulse_gym.lb_common.dto.MessegeGlobalDTO;
import com.pulse_gym.lb_common.entity.user.Ejercicio;
import com.pulse_gym.lb_common.services.ValidacionDeRoles;
import com.pulse_gym.ms_users.repository.EjercicioRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EjercicioService {

    /** Repositorio para operaciones CRUD de ejercicios */
    private final EjercicioRepository ejercicioRepository;

    /** Servicio para validar equipos contra pg-ms-operation */
    private final EquipoValidationService equipoValidationService;

    /**
     * Lista de grupos musculares válidos según el diseño de BD
     */
    private static final List<String> GRUPOS_MUSCULARES = List.of(
            "PECHO", "ESPALDA", "PIERNA", "HOMBRO", "BRAZO", "CORE", "CARDIO");

    /**
     * Convierte una entidad Ejercicio a EjercicioResponseDTO
     * 
     * @param ejercicio Entidad a convertir
     * @return DTO del ejercicio
     */
    private EjercicioResponseDTO convertirAResponseDTO(Ejercicio ejercicio) {
        EjercicioResponseDTO dto = new EjercicioResponseDTO();
        dto.setIdEjercicio(ejercicio.getIdEjercicio());
        dto.setNombre(ejercicio.getNombre());
        dto.setGrupoMuscular(ejercicio.getGrupoMuscular());
        dto.setEquipoNecesario(ejercicio.getEquipoNecesario());
        dto.setExplicacionTecnica(ejercicio.getExplicacionTecnica());
        dto.setUrlImagen(ejercicio.getUrlImagen());
        dto.setDificultad(ejercicio.getDificultad());
        dto.setCaloriasPorMinuto(ejercicio.getCaloriasPorMinuto());
        dto.setActivo(ejercicio.getActivo());
        return dto;
    }

    /**
     * Crea un nuevo ejercicio
     * 
     * @param request DTO con los datos del ejercicio
     * @param userRol Rol del usuario autenticado
     * @return Mensaje de confirmación
     * @throws RuntimeException Si el grupo muscular no es válido, ya existe un
     *                          ejercicio con ese nombre
     *                          o el equipo no existe en el sistema
     */
    @Transactional
    public MessegeGlobalDTO crearEjercicio(EjercicioRequestDTO request, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        if (!GRUPOS_MUSCULARES.contains(request.getGrupoMuscular().toUpperCase())) {
            throw new RuntimeException("Grupo muscular no válido. Valores permitidos: " + GRUPOS_MUSCULARES);
        }

        if (ejercicioRepository.existsByNombreAndActivoTrue(request.getNombre())) {
            throw new RuntimeException("Ya existe un ejercicio activo con el nombre: " + request.getNombre());
        }

        equipoValidationService.validarEquipoExistenteOrThrow(request.getEquipoNecesario());

        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setNombre(request.getNombre());
        ejercicio.setGrupoMuscular(request.getGrupoMuscular().toUpperCase());
        ejercicio.setEquipoNecesario(request.getEquipoNecesario());
        ejercicio.setExplicacionTecnica(request.getExplicacionTecnica());
        ejercicio.setUrlImagen(request.getUrlImagen());
        ejercicio.setDificultad(request.getDificultad());
        ejercicio.setCaloriasPorMinuto(request.getCaloriasPorMinuto());
        ejercicio.setActivo(request.getActivo() != null ? request.getActivo() : true);

        ejercicioRepository.save(ejercicio);

        return new MessegeGlobalDTO("Ejercicio '" + ejercicio.getNombre() + "' creado correctamente");
    }

    /**
     * Consulta ejercicios aplicando filtros de búsqueda
     * 
     * @param nombre          Nombre del ejercicio (búsqueda parcial)
     * @param grupoMuscular   Grupo muscular del ejercicio
     * @param equipoNecesario Equipo necesario (búsqueda parcial)
     * @param dificultadMin   Dificultad mínima
     * @param dificultadMax   Dificultad máxima
     * @param userRol         Rol del usuario autenticado
     * @return Lista de ejercicios que coinciden con los filtros
     * @throws RuntimeException Si no se encuentran ejercicios
     */
    public List<EjercicioResponseDTO> consultarEjercicios(
            String nombre,
            String grupoMuscular,
            String equipoNecesario,
            Integer dificultadMin,
            Integer dificultadMax,
            String userRol) {

        ValidacionDeRoles.validarCualquierRol(userRol);

        Specification<Ejercicio> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(nombre)) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(grupoMuscular)) {
                predicates.add(cb.equal(root.get("grupoMuscular"), grupoMuscular.toUpperCase()));
            }

            if (StringUtils.hasText(equipoNecesario)) {
                predicates
                        .add(cb.like(cb.lower(root.get("equipoNecesario")), "%" + equipoNecesario.toLowerCase() + "%"));
            }

            if (dificultadMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dificultad"), dificultadMin));
            }

            if (dificultadMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dificultad"), dificultadMax));
            }

            predicates.add(cb.isTrue(root.get("activo")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Ejercicio> ejercicios = ejercicioRepository.findAll(spec);

        if (ejercicios.isEmpty()) {
            throw new RuntimeException("No se encontraron ejercicios con los filtros especificados");
        }

        return ejercicios.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un ejercicio por su ID
     * 
     * @param id      ID del ejercicio a consultar
     * @param userRol Rol del usuario autenticado
     * @return DTO del ejercicio
     * @throws RuntimeException Si el ejercicio no existe o no está activo
     */
    public EjercicioResponseDTO obtenerEjercicioPorId(Long id, String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado con ID: " + id));

        if (!ejercicio.getActivo()) {
            throw new RuntimeException("El ejercicio no está activo");
        }

        return convertirAResponseDTO(ejercicio);
    }

    /**
     * Actualiza un ejercicio existente
     * 
     * @param id      ID del ejercicio a actualizar
     * @param request DTO con los datos a actualizar
     * @param userRol Rol del usuario autenticado
     * @return Mensaje de confirmación
     * @throws RuntimeException Si el ejercicio no existe, el grupo muscular no es
     *                          válido,
     *                          ya existe un ejercicio con ese nombre o el equipo no
     *                          existe
     */
    @Transactional
    public MessegeGlobalDTO actualizarEjercicio(Long id, EjercicioUpdateDTO request, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado con ID: " + id));

        if (StringUtils.hasText(request.getGrupoMuscular())) {
            if (!GRUPOS_MUSCULARES.contains(request.getGrupoMuscular().toUpperCase())) {
                throw new RuntimeException("Grupo muscular no válido. Valores permitidos: " + GRUPOS_MUSCULARES);
            }
            ejercicio.setGrupoMuscular(request.getGrupoMuscular().toUpperCase());
        }

        if (StringUtils.hasText(request.getNombre()) && !request.getNombre().equals(ejercicio.getNombre())) {
            if (ejercicioRepository.existsByNombreAndActivoTrue(request.getNombre())) {
                throw new RuntimeException("Ya existe un ejercicio activo con el nombre: " + request.getNombre());
            }
            ejercicio.setNombre(request.getNombre());
        }

        if (StringUtils.hasText(request.getEquipoNecesario()) &&
                !request.getEquipoNecesario().equals(ejercicio.getEquipoNecesario())) {
            equipoValidationService.validarEquipoExistenteOrThrow(request.getEquipoNecesario());
            ejercicio.setEquipoNecesario(request.getEquipoNecesario());
        }

        if (request.getExplicacionTecnica() != null) {
            ejercicio.setExplicacionTecnica(request.getExplicacionTecnica());
        }
        if (request.getUrlImagen() != null) {
            ejercicio.setUrlImagen(request.getUrlImagen());
        }
        if (request.getDificultad() != null) {
            ejercicio.setDificultad(request.getDificultad());
        }
        if (request.getCaloriasPorMinuto() != null) {
            ejercicio.setCaloriasPorMinuto(request.getCaloriasPorMinuto());
        }
        if (request.getActivo() != null) {
            ejercicio.setActivo(request.getActivo());
        }

        ejercicioRepository.save(ejercicio);

        return new MessegeGlobalDTO("Ejercicio '" + ejercicio.getNombre() + "' actualizado correctamente");
    }

    /**
     * Desactiva un ejercicio (eliminación lógica)
     * 
     * @param id      ID del ejercicio a desactivar
     * @param userRol Rol del usuario autenticado
     * @return Mensaje de confirmación
     * @throws RuntimeException Si el ejercicio no existe o ya está desactivado
     */
    @Transactional
    public MessegeGlobalDTO eliminarEjercicio(Long id, String userRol) {
        ValidacionDeRoles.validarAdminOEntrenadorORecepcionista(userRol);

        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ejercicio no encontrado con ID: " + id));

        if (!ejercicio.getActivo()) {
            throw new RuntimeException("El ejercicio ya está desactivado");
        }

        ejercicio.setActivo(false);
        ejercicioRepository.save(ejercicio);

        return new MessegeGlobalDTO("Ejercicio '" + ejercicio.getNombre() + "' desactivado correctamente");
    }

    /**
     * Obtiene la lista de grupos musculares válidos
     * 
     * @param userRol Rol del usuario autenticado
     * @return Lista de grupos musculares
     */
    public List<String> obtenerGruposMusculares(String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);
        return GRUPOS_MUSCULARES;
    }

/**
 * Obtiene la lista de equipos necesarios utilizados en ejercicios activos
 * @param userRol Rol del usuario autenticado
 * @return Lista de equipos ordenada y sin duplicados
 */
    public List<String> obtenerEquiposNecesarios(String userRol) {
        ValidacionDeRoles.validarCualquierRol(userRol);

        return ejercicioRepository.findByActivoTrue()
                .stream()
                .map(Ejercicio::getEquipoNecesario)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
