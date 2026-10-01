package com.example.demo.services;

import com.example.demo.model.Aluno;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    private Aluno alunoValido() {
        Aluno aluno = new Aluno();
        aluno.setNome(" Aluno Teste ");
        aluno.setEmail(" ALUNO@example.com ");
        aluno.setMatricula("123");
        aluno.setSemestreAtual(0);
        return aluno;
    }

    @Test
    void deveCadastrarAlunoComHashEDadosNormalizados() {
        Aluno aluno = alunoValido();
        when(usuarioRepository.save(aluno)).thenReturn(aluno);
        assertSame(aluno, service.cadastrarAluno(aluno, "senha-de-teste"));
        assertEquals("aluno@example.com", aluno.getEmail());
        assertEquals("Aluno Teste", aluno.getNome());
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .matches("senha-de-teste", aluno.getSenhaHash()));
        assertNotEquals("senha-de-teste", aluno.getSenhaHash());
        assertNotNull(aluno.getDataCadastro());
        assertTrue(aluno.getAtivo());
        verify(usuarioRepository).findByEmailIgnoreCase("aluno@example.com");
        verify(usuarioRepository).save(aluno);
    }

    @Test
    void deveCadastrarProfessor() {
        var professor = new com.example.demo.model.Professor();
        professor.setNome("Professor Teste");
        professor.setEmail("professor@example.com");
        professor.setRegistro("REG-1");
        professor.setPesoAvaliacao(java.math.BigDecimal.ONE);
        when(usuarioRepository.save(professor)).thenReturn(professor);
        assertSame(professor, service.cadastrarProfessor(professor, "senha-de-teste"));
        assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .matches("senha-de-teste", professor.getSenhaHash()));
        verify(usuarioRepository).save(professor);
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        when(usuarioRepository.findByEmailIgnoreCase("aluno@example.com"))
                .thenReturn(Optional.of(new com.example.demo.model.Professor()));
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrarAluno(alunoValido(), "senha-de-teste"));
        verify(usuarioRepository, never()).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "nome", "email", "matricula", "semestre", "id", "senha", "senhaLonga"})
    void deveRejeitarCadastroInvalido(String campo) {
        Aluno aluno = alunoValido();
        String senha = "senha-de-teste";
        switch (campo) {
            case "nome" -> aluno.setNome(" ");
            case "email" -> aluno.setEmail("email-invalido");
            case "matricula" -> aluno.setMatricula(null);
            case "semestre" -> aluno.setSemestreAtual(-1);
            case "id" -> aluno.setId(java.util.UUID.randomUUID());
            case "senha" -> senha = " ";
            case "senhaLonga" -> senha = "a".repeat(73);
        }
        String senhaInformada = senha;
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrarAluno(aluno, senhaInformada));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveRejeitarProfessorComPesoInvalido() {
        var professor = new com.example.demo.model.Professor();
        professor.setNome("Professor");
        professor.setEmail("professor@example.com");
        professor.setRegistro("REG-1");
        professor.setPesoAvaliacao(java.math.BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrarProfessor(professor, "senha-de-teste"));
        verifyNoInteractions(usuarioRepository);
    }

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService service;

    @Test
    void deveRetornarUsuarioQuandoCredenciaisConferem() {
        Aluno aluno = new Aluno();
        aluno.setAtivo(true);
        aluno.setEmail("aluno@example.com");
        // Valida a senha digitada contra um hash BCrypt.
        aluno.setSenhaHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senha-de-teste"));
        when(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertSame(aluno, service.realizarLogin(aluno.getEmail(), "senha-de-teste"));
        verify(usuarioRepository).findByEmailIgnoreCase(aluno.getEmail());
    }

    @Test
    void deveRejeitarUsuarioInativoMesmoComSenhaCorreta() {
        Aluno aluno = new Aluno();
        aluno.setAtivo(false);
        aluno.setSenhaHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senha-de-teste"));
        when(usuarioRepository.findByEmailIgnoreCase("inativo@example.com")).thenReturn(Optional.of(aluno));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.realizarLogin("inativo@example.com", "senha-de-teste"));
    }

    @Test
    void deveRejeitarEmailNaoCadastrado() {
        when(usuarioRepository.findByEmailIgnoreCase("ausente@example.com")).thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.realizarLogin("ausente@example.com", "senha-de-teste"));

        assertEquals("Falha no login: Usuário não encontrado com este e-mail.", erro.getMessage());
    }

    @Test
    void deveRejeitarSenhaIncorreta() {
        Aluno aluno = new Aluno();
        aluno.setSenhaHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("senha-de-teste"));
        when(usuarioRepository.findByEmailIgnoreCase("aluno@example.com")).thenReturn(Optional.of(aluno));

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.realizarLogin("aluno@example.com", "senha-errada"));

        assertEquals("Falha no login: Senha incorreta.", erro.getMessage());
    }
}
