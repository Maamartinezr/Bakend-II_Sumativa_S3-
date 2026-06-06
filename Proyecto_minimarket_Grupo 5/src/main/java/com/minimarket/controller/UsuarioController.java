package com.minimarket.controller;

import com.minimarket.dto.UsuarioRequest;
import com.minimarket.dto.UsuarioResponse;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepository;

    @GetMapping
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioService.findAll().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);
        return usuario.map(UsuarioResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public UsuarioResponse guardarUsuario(@Valid @RequestBody UsuarioRequest request) {
        Usuario usuario = toEntity(request);
        return UsuarioResponse.fromEntity(usuarioService.save(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        Optional<Usuario> usuarioExistente = usuarioService.findById(id);
        if (usuarioExistente.isPresent()) {
            Usuario usuario = toEntity(request);
            usuario.setId(id);
            return ResponseEntity.ok(UsuarioResponse.fromEntity(usuarioService.save(usuario)));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);
        if (usuario.isPresent()) { // Verifica si el usuario existe
            usuarioService.deleteById(id); // Elimina al usuario
            return ResponseEntity.noContent().build(); // Respuesta 204 (sin contenido)
        }
        return ResponseEntity.notFound().build();
    }

    private Usuario toEntity(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setPassword(request.getPassword());
        usuario.setRoles(resolveRoles(request.getRoles()));
        return usuario;
    }

    private Set<Rol> resolveRoles(Set<String> roles) {
        return roles.stream()
                .map(this::normalizeRole)
                .map(roleName -> rolRepository.findByNombre(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Rol no valido: " + roleName)))
                .collect(Collectors.toSet());
    }

    private String normalizeRole(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
