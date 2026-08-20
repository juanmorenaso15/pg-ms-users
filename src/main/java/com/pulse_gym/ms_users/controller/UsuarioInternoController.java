package com.pulse_gym.ms_users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulse_gym.lb_common.dto.UsuarioPerfilResponseDTO;
import com.pulse_gym.lb_common.enums.EnumEstadoUsuario;
import com.pulse_gym.ms_users.service.UsuarioPerfilService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/usuarios")
public class UsuarioInternoController {

    private final UsuarioPerfilService usuarioPerfilService;

    /**
     * Obtiene el perfil de usuario por email para otros microservicios
     *
     * @param email Email del usuario
     * @return Perfil del usuario
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioPerfilResponseDTO> obtenerPorEmail(@PathVariable String email) {
        UsuarioPerfilResponseDTO usuario = usuarioPerfilService.obtenerUsuarioPorEmailInterno(email);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Obtener el id sin validacion de roles, ya que es una peticion interna entre
     * microservicios
     * 
     * @param idUsuario
     * @return
     */
    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioPerfilResponseDTO> obtenerPorId(@PathVariable Long idUsuario) {
        UsuarioPerfilResponseDTO usuario = usuarioPerfilService.obtenerUsuarioPorIdInterno(idUsuario);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Cambia el estado de un usuario (activo/inactivo) y sincroniza con el perfil
     * del usuario.
     * 
     * @param email  Email del usuario a cambiar
     * @param estado Nuevo estado del usuario
     */
    @PutMapping("/email/estado")
    public ResponseEntity<Void> cambiarEstadoInternoPorEmail(
            @RequestParam String email,
            @RequestParam EnumEstadoUsuario estado) {
        usuarioPerfilService.actualizarEstadoInternoPorEmail(email, estado);
        return ResponseEntity.ok().build();
    }

}
