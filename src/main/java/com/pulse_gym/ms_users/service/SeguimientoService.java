package com.pulse_gym.ms_users.service;

import org.springframework.stereotype.Service;

import com.pulse_gym.ms_users.repository.DetalleRutinaRepository;
import com.pulse_gym.ms_users.repository.DetalleSesionEjercicioRepository;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import com.pulse_gym.ms_users.repository.RutinaRepository;
import com.pulse_gym.ms_users.repository.SesionEntrenamientoRepository;
import com.pulse_gym.ms_users.repository.UsuarioPerfilRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeguimientoService {

    /** Repositorio de sesiones de entrenamiento */
    private final SesionEntrenamientoRepository sesionRepository;

    /** Repositorio de detalles de sesión */
    private final DetalleSesionEjercicioRepository detalleSesionRepository;

    /** Repositorio de detalles de rutina */
    private final DetalleRutinaRepository detalleRutinaRepository;

    /** Repositorio de usuarios */
    private final UsuarioPerfilRepository usuarioRepository;

    /** Repositorio de rutinas */
    private final RutinaRepository rutinaRepository;

    /** Repositorio de asignaciones entrenador-socio */
    private final EntrenadorSocioRepository entrenadorSocioRepository;
}
