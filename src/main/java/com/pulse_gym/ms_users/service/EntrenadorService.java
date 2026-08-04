package com.pulse_gym.ms_users.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pulse_gym.lb_common.client.AuthClient;
import com.pulse_gym.lb_common.dto.AuthUserDTO;
import com.pulse_gym.lb_common.entity.user.UsuarioPerfil;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.lb_common.enums.EnumRol;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntrenadorService {

    
    /** Repositorio para gestionar perfiles de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;
    
    /** Repositorio para gestionar la relación entre entrenadores y socios */
    private final EntrenadorSocioRepository entrenadorSocioRepository;

    /** Cliente para validar roles con el servicio de autenticación */
    private final AuthClient authClient;

    /**
     * Busca un entrenador disponible para asignar a un socio.
     * Primero valida con el servicio de auth que el usuario sea ENTRENADOR.
     * Si auth no responde, usa especialidad como respaldo.
     * 
     * @return El entrenador disponible con menos socios asignados, o null si no hay
     *         entrenadores activos.
     */
    public UsuarioPerfil buscarEntrenadorDisponible() {
        List<UsuarioPerfil> usuariosActivos = usuarioRepository.findByEstado(EnumEstadoUsuario.ACTIVO);

        if (usuariosActivos.isEmpty()) {
            log.warn("No hay usuarios activos en el sistema");
            return null;
        }

        List<UsuarioPerfil> entrenadores = new ArrayList<>();

        for (UsuarioPerfil usuario : usuariosActivos) {
            try {
                AuthUserDTO authInfo = authClient.validarRolPorEmail(usuario.getEmail());
                if (authInfo != null && authInfo.getRol() == EnumRol.entrenador) {
                    entrenadores.add(usuario);
                    log.debug("Usuario {} es ENTRENADOR según auth", usuario.getEmail());
                }
            } catch (Exception e) {
                log.warn("No se pudo validar rol para {}: {}", usuario.getEmail(), e.getMessage());
            }
        }

        if (entrenadores.isEmpty()) {
            log.warn("Auth no disponible o sin entrenadores, usando especialidad como respaldo");
            entrenadores = usuarioRepository.findEntrenadoresActivos();
        }

        if (entrenadores.isEmpty()) {
            log.warn("No hay entrenadores activos en el sistema");
            return null;
        }

        if (entrenadores.size() == 1) {
            log.info("Entrenador seleccionado (único): {} {} (ID: {})",
                    entrenadores.get(0).getNombre(),
                    entrenadores.get(0).getApellido(),
                    entrenadores.get(0).getIdUsuario());
            return entrenadores.get(0);
        }

        UsuarioPerfil entrenadorSeleccionado = null;
        int menorCantidadSocios = Integer.MAX_VALUE;

        for (UsuarioPerfil entrenador : entrenadores) {
            Long cantidadSocios = entrenadorSocioRepository.countByEntrenadorAndActivaTrue(
                    entrenador.getIdUsuario());

            if (cantidadSocios < menorCantidadSocios) {
                menorCantidadSocios = cantidadSocios.intValue();
                entrenadorSeleccionado = entrenador;
            }
        }

        if (entrenadorSeleccionado != null) {
            log.info("Entrenador seleccionado: {} {} (ID: {}) con {} socios asignados",
                    entrenadorSeleccionado.getNombre(),
                    entrenadorSeleccionado.getApellido(),
                    entrenadorSeleccionado.getIdUsuario(),
                    menorCantidadSocios);
        }

        return entrenadorSeleccionado;
    }

    /**
     * Verifica si un usuario específico es entrenador usando el
     * servicio de auth
     * 
     * @param email Email del usuario a verificar
     * @return true si es entrenador, false en caso contrario
     */
    public boolean esEntrenador(String email) {
        try {
            AuthUserDTO authInfo = authClient.validarRolPorEmail(email);
            return authInfo != null && authInfo.getRol() == EnumRol.entrenador;
        } catch (Exception e) {
            log.warn("No se pudo validar rol para {}: {}", email, e.getMessage());
            UsuarioPerfil usuario = usuarioRepository.findByEmail(email).orElse(null);
            return usuario != null && usuario.getEspecialidad() != null && !usuario.getEspecialidad().isEmpty();
        }
    }

    /**
     * Obtiene todos los entrenadores activos (con rol validado en
     * auth)
     * 
     * @return Lista de entrenadores activos
     */
    public List<UsuarioPerfil> obtenerTodosLosEntrenadores() {
        List<UsuarioPerfil> usuariosActivos = usuarioRepository.findByEstado(EnumEstadoUsuario.ACTIVO);
        List<UsuarioPerfil> entrenadores = new ArrayList<>();

        for (UsuarioPerfil usuario : usuariosActivos) {
            try {
                AuthUserDTO authInfo = authClient.validarRolPorEmail(usuario.getEmail());
                if (authInfo != null && authInfo.getRol() == EnumRol.entrenador) {
                    entrenadores.add(usuario);
                }
            } catch (Exception e) {
                log.warn("No se pudo validar rol para {}: {}", usuario.getEmail(), e.getMessage());
            }
        }

        if (entrenadores.isEmpty()) {
            entrenadores = usuarioRepository.findEntrenadoresActivos();
        }

        return entrenadores;
    }
}
