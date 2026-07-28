package com.pulse_gym.ms_users.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.dto.PlanNutricionalGeneracionRequestDTO;
import com.pulse_gym.lb_common.entity.user.HistorialFisico;
import com.pulse_gym.lb_common.entity.user.PerfilMedico;
import com.pulse_gym.lb_common.entity.user.RutinaIA;
import com.pulse_gym.lb_common.entity.user.SocioMembresia;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.lb_common.exception.SecurityAuthorizationException;
import com.pulse_gym.ms_users.repository.HistorialFisicoRepository;
import com.pulse_gym.ms_users.repository.PerfilMedicoRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.SocioMembresiaRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanNutricionalIAService {

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de perfiles médicos */
    private final PerfilMedicoRepository perfilMedicoRepository;

    /** Repositorio de historial físico */
    private final HistorialFisicoRepository historialFisicoRepository;

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de membresías de socios */
    private final SocioMembresiaRepository socioMembresiaRepository;

    /**
     * Calcula la edad a partir de la fecha de nacimiento
     * 
     * @param fechaNacimiento Fecha de nacimiento
     * @return Edad en años, o 0 si la fecha es nula
     */
    private int calcularEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null)
            return 0;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /**
     * Valida que el socio tenga una membresía activa
     * 
     * @param idSocio ID del socio a validar
     */
    public void validarMembresiaActiva(Long idSocio) {
        log.info("Validando membresía activa para socio ID: {}", idSocio);

        SocioMembresia membresiaActiva = socioMembresiaRepository.findMembresiaActivaBySocio(idSocio)
                .orElseThrow(() -> new RuntimeException("El socio no tiene una membresía activa"));

        if (!membresiaActiva.isActiva()) {
            throw new RuntimeException("La membresía del socio está inactiva o vencida");
        }

        log.info("Membresía activa confirmada para socio ID: {}", idSocio);
    }

    /**
     * Valida que el usuario tenga permisos para generar planes nutricionales
     * 
     * @param userRol           Rol del usuario autenticado
     * @param idSocio           ID del socio para el que se genera el plan
     * @param userIdAutenticado ID del usuario autenticado
     * @param userEmail         Email del usuario autenticado
     */
    public void validarRolGeneracion(String userRol, Long idSocio, Long userIdAutenticado, String userEmail) {
        if (userRol == null) {
            throw new SecurityAuthorizationException("Usuario no autenticado");
        }

        if (EnumRol.administrador.name().equals(userRol)) {
            return;
        }

        if (EnumRol.entrenador.name().equals(userRol)) {
            return;
        }

        if (EnumRol.recepcionista.name().equals(userRol)) {
            return;
        }

        if (EnumRol.socio.name().equals(userRol)) {
            UsuarioPerfil socio = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Socio no encontrado con email: " + userEmail));

            if (!socio.getIdUsuario().equals(idSocio)) {
                throw new SecurityAuthorizationException(
                        String.format("Acceso denegado. Los socios solo pueden generar planes para sí mismos. " +
                                "Tu ID en usuario_perfil: %d, ID solicitado: %d",
                                socio.getIdUsuario(), idSocio));
            }
            return;
        }

        throw new SecurityAuthorizationException(
                "Acceso denegado. Rol '" + userRol + "' no autorizado para generar planes nutricionales");
    }

    /**
     * 
     * Recopila todos los datos relevantes del socio para la generación del plan
     * nutricional
     * 
     * @param idSocio ID del socio
     * @return Mapa con los datos del socio
     */
    public Map<String, Object> recopilarDatosSocio(Long idSocio) {
        log.info("Recopilando datos del socio ID: {}", idSocio);

        Map<String, Object> datos = new HashMap<>();

        UsuarioPerfil socio = usuarioRepository.findById(idSocio)
                .orElseThrow(() -> new RuntimeException("Socio no encontrado con ID: " + idSocio));

        datos.put("idSocio", socio.getIdUsuario());
        datos.put("nombre", socio.getNombre());
        datos.put("apellido", socio.getApellido());
        datos.put("email", socio.getEmail());
        datos.put("edad", calcularEdad(socio.getFechaNacimiento()));
        datos.put("objetivoPrincipal", socio.getObjetivoPrincipal());
        datos.put("nivelExperiencia", socio.getNivelExperiencia().name());

        PerfilMedico perfilMedico = perfilMedicoRepository.findBySocio_IdUsuario(idSocio)
                .orElse(null);

        if (perfilMedico != null) {
            datos.put("peso", perfilMedico.getPesoKg());
            datos.put("estatura", perfilMedico.getEstaturaCm());
            datos.put("alergias", perfilMedico.getAlergias());
            datos.put("condicionesCronicas", perfilMedico.getCondicionesCronicas());
            datos.put("porcentajeGrasa", perfilMedico.getPorcentajeGrasa());
        } else {
            log.warn("El socio ID: {} no tiene perfil médico registrado", idSocio);
        }

        HistorialFisico ultimaMedicion = historialFisicoRepository.findLastMedicionBySocio(idSocio);
        if (ultimaMedicion != null) {
            datos.put("pesoKg", ultimaMedicion.getPesoKg());
            datos.put("porcentajeGrasa", ultimaMedicion.getPorcentajeGrasa());
            datos.put("porcentajeMusculo", ultimaMedicion.getPorcentajeMusculo());
        }

        RutinaIA rutinaActiva = rutinaRepository.findRutinaActivaReciente(idSocio)
                .orElse(null);

        if (rutinaActiva != null) {
            datos.put("tieneRutinaActiva", true);

            datos.put("diasEntrenamiento", 3);
        } else {
            datos.put("tieneRutinaActiva", false);
            datos.put("diasEntrenamiento", 0);
        }

        log.info("Datos recopilados para socio ID: {} - {} campos", idSocio, datos.size());
        return datos;
    }

    /**
     * Construye el contexto con los datos del socio y preferencias para la IA
     * 
     * @param idSocio ID del socio
     * @param request Preferencias del socio para el plan nutricional
     * @return Mapa con el contexto completo para la IA
     */
    public Map<String, Object> construirContextoIA(Long idSocio, PlanNutricionalGeneracionRequestDTO request) {
        Map<String, Object> contexto = recopilarDatosSocio(idSocio);

        if (request != null) {
            contexto.put("restriccionesDieteticas", request.getRestriccionesDieteticas());
            contexto.put("alergias", request.getAlergias());
            contexto.put("intolerancias", request.getIntolerancias());
            contexto.put("objetivoEspecifico", request.getObjetivoEspecifico());
            contexto.put("idRutina", request.getIdRutina());
        }

        log.info("Contexto construido con: nombre={}, peso={}, edad={}",
                contexto.get("nombre"), contexto.get("peso"), contexto.get("edad"));

        return contexto;
    }
}
