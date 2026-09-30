package com.example.demo.services;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;
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
class TagServiceTest {
    @Mock private TagRepository repository;
    @InjectMocks private TagService service;

    @Test
    void deveCriarTagRemovendoEspacos() {
        when(repository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            assertNull(tag.getId());
            tag.setId(UUID.randomUUID());
            return tag;
        });
        Tag tag = service.criarTag(" Java ");
        assertEquals("Java", tag.getNome());
        assertNotNull(tag.getId());
        verify(repository).findByNomeNormalizado("Java");
        verify(repository).save(tag);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void deveRejeitarNomeVazio(String nome) {
        assertThrows(IllegalArgumentException.class, () -> service.criarTag(nome));
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorNome(nome));
        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarNomeLongo() {
        assertThrows(IllegalArgumentException.class, () -> service.criarTag("a".repeat(256)));
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorNome("a".repeat(256)));
        verifyNoInteractions(repository);
    }

    @Test
    void deveRejeitarDuplicata() {
        when(repository.findByNomeNormalizado("java")).thenReturn(Optional.of(new Tag()));
        assertThrows(IllegalArgumentException.class, () -> service.criarTag(" java "));
        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarPorNome() {
        Tag tag = new Tag();
        when(repository.findByNomeNormalizado("JAVA")).thenReturn(Optional.of(tag));
        assertSame(tag, service.buscarPorNome(" JAVA "));
    }

    @Test
    void deveInformarTagInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorNome("ausente"));
    }

    @Test
    void deveListarTags() {
        List<Tag> tags = List.of(new Tag());
        when(repository.listarOrdenadasPorNome()).thenReturn(tags);
        assertEquals(tags, service.listarTodas());
    }

    @Test
    void deveRetornarListaVazia() {
        assertTrue(service.listarTodas().isEmpty());
    }
}
