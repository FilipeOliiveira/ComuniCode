package com.example.demo.services;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tag_integracao;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test")
@Transactional
class TagIntegracaoTest {
    @Autowired private TagService service;
    @Autowired private TagRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    void devePersistirBuscarEOrdenarIgnorandoMaiusculas() {
        Tag java = service.criarTag(" Java ");
        service.criarTag("banco");
        service.criarTag("Algoritmos");
        entityManager.flush();
        entityManager.clear();

        assertEquals(java.getId(), service.buscarPorNome(" JAVA ").getId());
        assertEquals(List.of("Algoritmos", "banco", "Java"),
                service.listarTodas().stream().map(Tag::getNome).toList());
    }

    @Test
    void deveRejeitarDuplicataMesmoComEspacosNoRegistroExistente() {
        Tag existente = new Tag();
        existente.setNome(" Java ");
        repository.saveAndFlush(existente);
        entityManager.clear();

        assertEquals(existente.getId(), service.buscarPorNome("java").getId());
        assertThrows(IllegalArgumentException.class, () -> service.criarTag(" JAVA "));
        assertEquals(1, repository.count());
    }
}
