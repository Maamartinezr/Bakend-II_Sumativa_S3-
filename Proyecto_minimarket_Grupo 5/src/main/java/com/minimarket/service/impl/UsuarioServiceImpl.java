package com.minimarket.service.impl;

import com.minimarket.entity.Usuario;
import com.minimarket.entity.PasswordHistory;
import com.minimarket.repository.PasswordHistoryRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.security.service.PasswordPolicyService;
import com.minimarket.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordPolicyService passwordPolicyService;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario save(Usuario usuario) {
        Usuario existingUser = null;
        if (usuario.getId() != null) {
            existingUser = usuarioRepository.findById(usuario.getId()).orElse(null);
        }

        if (usuario.getPassword() != null && !isBCryptHash(usuario.getPassword())) {
            passwordPolicyService.validate(usuario.getUsername(), usuario.getPassword(), existingUser);
            String previousPasswordHash = existingUser != null ? existingUser.getPassword() : null;
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            Usuario saved = usuarioRepository.save(usuario);
            if (previousPasswordHash != null) {
                registerPasswordHistory(saved, previousPasswordHash);
            }
            return saved;
        }
        return usuarioRepository.save(usuario);
    }

    private boolean isBCryptHash(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }

    private void registerPasswordHistory(Usuario usuario, String passwordHash) {
        PasswordHistory history = new PasswordHistory();
        history.setUsuario(usuario);
        history.setPasswordHash(passwordHash);
        history.setChangedAt(LocalDateTime.now());
        passwordHistoryRepository.save(history);
    }
}
