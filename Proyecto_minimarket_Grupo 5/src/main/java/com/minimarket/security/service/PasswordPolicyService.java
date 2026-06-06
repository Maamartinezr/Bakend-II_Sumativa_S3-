package com.minimarket.security.service;

import com.minimarket.entity.PasswordHistory;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.PasswordHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordPolicyService {
    private final int minLength;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryRepository passwordHistoryRepository;

    public PasswordPolicyService(
            @Value("${minimarket.password.min-length}") int minLength,
            PasswordEncoder passwordEncoder,
            PasswordHistoryRepository passwordHistoryRepository
    ) {
        this.minLength = minLength;
        this.passwordEncoder = passwordEncoder;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    public void validate(String username, String rawPassword, Usuario existingUser) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La password es obligatoria");
        }
        if (rawPassword.length() < minLength) {
            throw new IllegalArgumentException("La password debe tener al menos " + minLength + " caracteres");
        }
        if (!rawPassword.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("La password debe incluir al menos una mayuscula");
        }
        if (!rawPassword.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("La password debe incluir al menos una minuscula");
        }
        if (!rawPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("La password debe incluir al menos un numero");
        }
        if (!rawPassword.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("La password debe incluir al menos un caracter especial");
        }
        if (username != null && rawPassword.toLowerCase().contains(username.toLowerCase())) {
            throw new IllegalArgumentException("La password no debe contener el username");
        }
        if (existingUser != null && passwordEncoder.matches(rawPassword, existingUser.getPassword())) {
            throw new IllegalArgumentException("La password no puede reutilizar la password actual");
        }
        if (existingUser != null) {
            List<PasswordHistory> lastPasswords = passwordHistoryRepository.findTop5ByUsuarioOrderByChangedAtDesc(existingUser);
            boolean reused = lastPasswords.stream()
                    .anyMatch(history -> passwordEncoder.matches(rawPassword, history.getPasswordHash()));
            if (reused) {
                throw new IllegalArgumentException("La password no puede reutilizar las ultimas passwords registradas");
            }
        }
    }
}
