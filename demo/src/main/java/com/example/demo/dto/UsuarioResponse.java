package com.example.demo.dto;

import com.example.demo.model.Usuario;
import com.example.demo.security.UsuarioAutenticado;
import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email, String perfil, boolean ativo) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                UsuarioAutenticado.perfil(usuario), Boolean.TRUE.equals(usuario.getAtivo()));
    }
}
