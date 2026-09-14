package com.pulse_gym.ms_users.controller;

import com.pulse_gym.lb_common.dto.SocioSimpleDTO;
import com.pulse_gym.ms_users.repository.EntrenadorSocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/entrenador")
@RequiredArgsConstructor
public class EntrenadorController {

    private final EntrenadorSocioRepository entrenadorSocioRepository;

    @GetMapping("/socios/{entrenadorId}")
    public ResponseEntity<List<SocioSimpleDTO>> obtenerSociosAsignados(@PathVariable Long entrenadorId) {
        List<SocioSimpleDTO> socios = entrenadorSocioRepository.findSociosSimplesActivosByEntrenador(entrenadorId);
        return ResponseEntity.ok(socios);
    }
}