package com.example.demo.services;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Realiza o login do usuário verificando e-mail e senha.
     * 
     * @param email E-mail do usuário
     * @param senha Senha digitada
     * @return O objeto Usuario caso o login seja bem-sucedido
     */
    public Usuario realizarLogin(String email, String senha) {
        
        // 1. Busca o usuário pelo e-mail no banco de dados
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Falha no login: Usuário não encontrado com este e-mail."));

        // 2. Compara a senha digitada com a senha do banco
        // Atenção: Verifique se o método de pegar a senha no seu Usuario.java é getSenha() ou getPassword()
        if (!usuario.getSenhaHash().equals(senha)) {
            throw new RuntimeException("Falha no login: Senha incorreta.");
        }

        // 3. Se passou pelas validações, retorna o usuário autenticado
        return usuario;
    }
}
