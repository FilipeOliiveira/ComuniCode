package com.example.demo.services;

import com.example.demo.model.Administrador;
import com.example.demo.model.Aluno;
import com.example.demo.model.Disciplina;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisciplinaServiceTest {
    @Mock
    private DisciplinaRepository disciplinaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private DisciplinaService service;

    private final UUID idAdmin = UUID.randomUUID();

    private void autorizarAdministrador() {
        when(usuarioRepository.findById(idAdmin)).thenReturn(Optional.of(new Administrador()));
    }

    private Disciplina disciplinaValida() {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("Programacao");
        disciplina.setCodigo("PROG01");
        disciplina.setCargaHoraria(60);
        disciplina.setPeriodo(1);
        return disciplina;
    }

    @Test
    void deveSalvarDisciplinaValidaParaAdministrador() {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        Disciplina salva = disciplinaValida();
        salva.setId(UUID.randomUUID());
        when(disciplinaRepository.save(disciplina)).thenReturn(salva);

        assertSame(salva, service.criarDisciplina(disciplina, idAdmin));
        verify(disciplinaRepository).save(disciplina);
    }

    @Test
    void deveRejeitarUsuarioInexistente() {
        when(usuarioRepository.findById(idAdmin)).thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.criarDisciplina(disciplinaValida(), idAdmin));

        assertEquals("Usuário não encontrado.", erro.getMessage());
        verifyNoInteractions(disciplinaRepository);
    }

    @Test
    void deveRejeitarUsuarioSemPermissao() {
        when(usuarioRepository.findById(idAdmin)).thenReturn(Optional.of(new Aluno()));

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.criarDisciplina(disciplinaValida(), idAdmin));

        assertEquals("Acesso negado: Apenas administradores podem criar disciplinas.", erro.getMessage());
        verifyNoInteractions(disciplinaRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarNomeVazio(String nome) {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setNome(nome);
        assertThrows(IllegalArgumentException.class, () -> service.criarDisciplina(disciplina, idAdmin));
        verifyNoInteractions(disciplinaRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarCodigoVazio(String codigo) {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setCodigo(codigo);
        assertThrows(IllegalArgumentException.class, () -> service.criarDisciplina(disciplina, idAdmin));
        verifyNoInteractions(disciplinaRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void deveRejeitarCargaHorariaInvalida(Integer cargaHoraria) {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setCargaHoraria(cargaHoraria);
        assertThrows(IllegalArgumentException.class, () -> service.criarDisciplina(disciplina, idAdmin));
        verifyNoInteractions(disciplinaRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void deveRejeitarPeriodoInvalido(Integer periodo) {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setPeriodo(periodo);
        assertThrows(IllegalArgumentException.class, () -> service.criarDisciplina(disciplina, idAdmin));
        verifyNoInteractions(disciplinaRepository);
    }

    @Test
    void deveRejeitarCodigoDuplicado() {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setCodigo(" PROG01 ");
        when(disciplinaRepository.existsByCodigoIgnoreCase("PROG01")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.criarDisciplina(disciplina, idAdmin));

        assertEquals("Já existe uma disciplina cadastrada com o código informado.", erro.getMessage());
        verify(disciplinaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarNomeDuplicado() {
        autorizarAdministrador();
        Disciplina disciplina = disciplinaValida();
        disciplina.setNome(" Programacao ");
        when(disciplinaRepository.existsByNomeIgnoreCase("Programacao")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.criarDisciplina(disciplina, idAdmin));

        assertEquals("Já existe uma disciplina cadastrada com o nome informado.", erro.getMessage());
        verify(disciplinaRepository, never()).save(any());
    }
}
