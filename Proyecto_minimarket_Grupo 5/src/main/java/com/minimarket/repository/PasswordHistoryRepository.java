package com.minimarket.repository;

import com.minimarket.entity.PasswordHistory;
import com.minimarket.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findTop5ByUsuarioOrderByChangedAtDesc(Usuario usuario);
}
