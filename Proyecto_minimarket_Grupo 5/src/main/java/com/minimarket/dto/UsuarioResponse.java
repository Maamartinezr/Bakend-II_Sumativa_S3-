package com.minimarket.dto;

import com.minimarket.entity.Usuario;

import java.util.Set;
import java.util.stream.Collectors;

public class UsuarioResponse {
    private Long id;
    private String username;
    private Set<String> roles;

    public static UsuarioResponse fromEntity(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setUsername(usuario.getUsername());
        response.setRoles(usuario.getRoles().stream()
                .map(rol -> rol.getNombre())
                .collect(Collectors.toSet()));
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
