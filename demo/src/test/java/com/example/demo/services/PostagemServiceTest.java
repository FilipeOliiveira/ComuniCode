package com.example.demo.services;

import com.example.demo.model.Postagem;
import com.example.demo.repository.PostagemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostagemServiceTest {

    @Mock
    private PostagemRepository postagemRepository;

    @InjectMocks
    private PostagemService service;

    @Test
    void deveListarPostagens() {
        Postagem postagem = new Postagem();
        postagem.setTitulo("Material de Java");

        when(postagemRepository.findAll())
                .thenReturn(List.of(postagem));

        List<Postagem> resultado = service.listarTodas();

        assertEquals(1, resultado.size());
        assertEquals("Material de Java", resultado.getFirst().getTitulo());
        verify(postagemRepository).findAll();
    }

    @Test
    void deveRejeitarPostagemSemTitulo() {
        Postagem postagem = new Postagem();

        assertThrows(IllegalArgumentException.class, () ->
                service.criarPostagem(
                        postagem,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        List.of(UUID.randomUUID())
                )
        );

        verifyNoInteractions(postagemRepository);
    }
}