package com.example.demo.services;

import com.example.demo.model.Conteudo;
import com.example.demo.model.Disciplina;
import com.example.demo.repository.DisciplinaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:conteudo_integracao;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test")
@Transactional
class ConteudoIntegracaoTest {
    @Autowired private ConteudoService service;
    @Autowired private DisciplinaRepository disciplinaRepository;
    @Autowired private EntityManager entityManager;

    private Disciplina disciplina(String codigo) {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome(codigo);
        disciplina.setCodigo(codigo);
        disciplina.setCargaHoraria(60);
        disciplina.setPeriodo(1);
        return disciplinaRepository.save(disciplina);
    }

    private Conteudo cadastrar(String titulo, int ordem, Disciplina disciplina) {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(titulo);
        conteudo.setOrdem(ordem);
        return service.criarConteudo(conteudo, disciplina.getId());
    }

    @Test
    void devePersistirBuscarEListarSomenteConteudosDaDisciplinaEmOrdem() {
        Disciplina java = disciplina("JAVA");
        Disciplina banco = disciplina("BANCO");
        Conteudo avancado = cadastrar("Avancado", 2, java);
        Conteudo basico = cadastrar("Basico", 0, java);
        Conteudo abertura = cadastrar("Abertura", 0, java);
        cadastrar("SQL", 0, banco);
        entityManager.flush();
        entityManager.clear();

        Conteudo lido = service.buscarPorId(avancado.getId());
        assertEquals("Avancado", lido.getTitulo());
        assertEquals(java.getId(), lido.getDisciplina().getId());
        assertEquals(List.of(abertura.getId(), basico.getId(), avancado.getId()),
                service.listarPorDisciplina(java.getId()).stream().map(Conteudo::getId).toList());
    }
}
