package com.example.demo.services;

import com.example.demo.model.Aluno;
import com.example.demo.model.Professor;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public Aluno cadastrarAluno(Aluno aluno, String senha) {
        validarUsuario(aluno, senha);
        aluno.setMatricula(textoObrigatorio(aluno.getMatricula(), "Matricula"));
        if (aluno.getSemestreAtual() == null || aluno.getSemestreAtual() < 0) {
            throw new IllegalArgumentException("O semestre deve ser zero ou maior.");
        }
        prepararCadastro(aluno, senha);
        return usuarioRepository.save(aluno);
    }

    @Transactional
    public Professor cadastrarProfessor(Professor professor, String senha) {
        validarUsuario(professor, senha);
        professor.setRegistro(textoObrigatorio(professor.getRegistro(), "Registro"));
        if (professor.getPesoAvaliacao() == null || professor.getPesoAvaliacao().signum() <= 0) {
            throw new IllegalArgumentException("O peso de avaliacao deve ser maior que zero.");
        }
        prepararCadastro(professor, senha);
        return usuarioRepository.save(professor);
    }

    private void validarUsuario(Usuario usuario, String senha) {
        if (usuario == null) {
            throw new IllegalArgumentException("O usuario e obrigatorio.");
        }
        if (usuario.getId() != null) {
            throw new IllegalArgumentException("O cadastro deve receber um usuario sem ID.");
        }
        usuario.setNome(textoObrigatorio(usuario.getNome(), "Nome"));
        usuario.setEmail(normalizarEmail(usuario.getEmail()));
        if (!usuario.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("E-mail invalido.");
        }
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("A senha e obrigatoria.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha deve ter no maximo 72 bytes para BCrypt.");
        }
    }

    private void prepararCadastro(Usuario usuario, String senha) {
        if (usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ja existe um usuario cadastrado com este e-mail.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setAtivo(true);
    }

    private String textoObrigatorio(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }
        return texto.trim();
    }

    private String normalizarEmail(String email) {
        return textoObrigatorio(email, "E-mail").toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Usuario realizarLogin(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .orElseThrow(() -> new RuntimeException("Falha no login: Usuário não encontrado com este e-mail."));
        if (senha == null || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new RuntimeException("Falha no login: Senha incorreta.");
        }
        return usuario;
    }
}
