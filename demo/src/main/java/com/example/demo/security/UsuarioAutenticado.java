package com.example.demo.security;

import com.example.demo.model.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import java.util.UUID;

/** Principal sem entidade JPA; o Security apaga a senha apos o login. */
public class UsuarioAutenticado extends User {
    private final UUID id;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getSenhaHash(), Boolean.TRUE.equals(usuario.getAtivo()),
                true, true, true, AuthorityUtils.createAuthorityList("ROLE_" + perfil(usuario)));
        this.id = usuario.getId();
    }

    public UUID getId() { return id; }

    public static String perfil(Usuario usuario) {
        if (usuario instanceof Administrador) return "ADMIN";
        if (usuario instanceof Professor) return "PROFESSOR";
        return "ALUNO";
    }
}
