package com.example.demo.services;

import com.example.demo.model.Conteudo;
import com.example.demo.model.Disciplina;
import com.example.demo.repository.ConteudoRepository;
import com.example.demo.repository.DisciplinaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConteudoServiceTest {
    @Mock private ConteudoRepository conteudoRepository;
    @Mock private DisciplinaRepository disciplinaRepository;
    @InjectMocks private ConteudoService service;
    private final UUID idDisciplina = UUID.randomUUID();

    private Conteudo novoConteudo() {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(" Introducao ao Java ");
        return conteudo;
    }

    @Test
    void deveCadastrarComDisciplinaEOrdemPadrao() {
        Disciplina disciplina = new Disciplina();
        Conteudo conteudo = novoConteudo();
        when(disciplinaRepository.findById(idDisciplina)).thenReturn(Optional.of(disciplina));
        when(conteudoRepository.save(conteudo)).thenReturn(conteudo);
        assertSame(conteudo, service.criarConteudo(conteudo, idDisciplina));
        assertSame(disciplina, conteudo.getDisciplina());
        assertEquals(0, conteudo.getOrdem());
        assertEquals("Introducao ao Java", conteudo.getTitulo());
        verify(conteudoRepository).save(conteudo);
    }

    @Test
    void devePreservarOrdemInformada() {
        Conteudo conteudo = novoConteudo();
        conteudo.setOrdem(3);
        when(disciplinaRepository.findById(idDisciplina)).thenReturn(Optional.of(new Disciplina()));
        service.criarConteudo(conteudo, idDisciplina);
        assertEquals(3, conteudo.getOrdem());
        verify(conteudoRepository).save(conteudo);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarTituloVazio(String titulo) {
        Conteudo conteudo = novoConteudo();
        conteudo.setTitulo(titulo);
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(conteudo, idDisciplina));
        verifyNoInteractions(conteudoRepository, disciplinaRepository);
    }

    @Test
    void deveRejeitarDadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(null, idDisciplina));
        Conteudo conteudo = novoConteudo();
        conteudo.setId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(conteudo, idDisciplina));
        conteudo.setId(null);
        conteudo.setOrdem(-1);
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(conteudo, idDisciplina));
        conteudo.setOrdem(0);
        conteudo.setTitulo("a".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(conteudo, idDisciplina));
        verifyNoInteractions(conteudoRepository, disciplinaRepository);
    }

    @Test
    void deveRejeitarDisciplinaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(novoConteudo(), idDisciplina));
        assertThrows(IllegalArgumentException.class, () -> service.listarPorDisciplina(idDisciplina));
        verifyNoInteractions(conteudoRepository);
    }

    @Test
    void deveRejeitarIdsNulos() {
        assertThrows(IllegalArgumentException.class, () -> service.criarConteudo(novoConteudo(), null));
        assertThrows(IllegalArgumentException.class, () -> service.listarPorDisciplina(null));
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(null));
        verifyNoInteractions(conteudoRepository, disciplinaRepository);
    }

    @Test
    void deveBuscarPorId() {
        UUID id = UUID.randomUUID();
        Conteudo conteudo = novoConteudo();
        when(conteudoRepository.findById(id)).thenReturn(Optional.of(conteudo));
        assertSame(conteudo, service.buscarPorId(id));
    }

    @Test
    void deveRejeitarConteudoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(UUID.randomUUID()));
    }

    @Test
    void deveListarPorDisciplina() {
        when(disciplinaRepository.findById(idDisciplina)).thenReturn(Optional.of(new Disciplina()));
        List<Conteudo> conteudos = List.of(novoConteudo());
        when(conteudoRepository.findByDisciplinaIdOrderByOrdemAscTituloAsc(idDisciplina)).thenReturn(conteudos);
        assertEquals(conteudos, service.listarPorDisciplina(idDisciplina));
    }

    @Test
    void deveRetornarListaVaziaParaDisciplinaSemConteudos() {
        when(disciplinaRepository.findById(idDisciplina)).thenReturn(Optional.of(new Disciplina()));
        assertTrue(service.listarPorDisciplina(idDisciplina).isEmpty());
    }
}
