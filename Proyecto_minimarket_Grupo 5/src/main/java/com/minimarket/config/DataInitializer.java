package com.minimarket.config;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedSecurityData(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Rol cliente = findOrCreateRole(rolRepository, "ROLE_CLIENTE");
            Rol empleado = findOrCreateRole(rolRepository, "ROLE_EMPLEADO");
            Rol admin = findOrCreateRole(rolRepository, "ROLE_ADMIN");

            createUserIfMissing(usuarioRepository, passwordEncoder, "cliente", "Cliente123!", Set.of(cliente));
            createUserIfMissing(usuarioRepository, passwordEncoder, "empleado", "Empleado123!", Set.of(empleado));
            createUserIfMissing(usuarioRepository, passwordEncoder, "admin", "Admin123!", Set.of(admin));
        };
    }

    private Rol findOrCreateRole(RolRepository rolRepository, String roleName) {
        return rolRepository.findByNombre(roleName)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(roleName);
                    return rolRepository.save(rol);
                });
    }

    private void createUserIfMissing(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            Set<Rol> roles
    ) {
        usuarioRepository.findByUsername(username).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(passwordEncoder.encode(rawPassword));
            usuario.setRoles(roles);
            return usuarioRepository.save(usuario);
        });
    }
}
