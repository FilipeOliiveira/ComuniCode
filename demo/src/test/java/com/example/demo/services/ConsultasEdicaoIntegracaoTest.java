package com.example.demo.services;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:consultas_edicao;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test")
@Transactional
class ConsultasEdicaoIntegracaoTest {
    @Autowired private DisciplinaService disciplinas;
    @Autowired private PostagemService postagens;
    @Autowired private DisciplinaRepository disciplinaRepository;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ConteudoRepository conteudos;
    @Autowired private TagRepository tags;
    @Autowired private EntityManager em;

    private Disciplina disciplina(String nome) {
        Disciplina d = new Disciplina();
        d.setNome(nome);
        d.setCodigo(UUID.randomUUID().toString());
        d.setPeriodo(1);
        d.setCargaHoraria(60);
        return disciplinaRepository.save(d);
    }

    @Test
    void deveConsultarDisciplinas() {
        Disciplina java = disciplina("Java");
        disciplina("Banco de Dados");
        em.flush(); em.clear();
        assertEquals("Java", disciplinas.buscarPorId(java.getId()).getNome());
        assertEquals(List.of("Banco de Dados", "Java"), disciplinas.listarTodas().stream().map(Disciplina::getNome).toList());
        assertEquals(java.getId(), disciplinas.buscarPorNome(" AV ").getFirst().getId());
        assertTrue(disciplinas.buscarPorNome("ausente").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> disciplinas.buscarPorId(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> disciplinas.buscarPorId(null));
        assertThrows(IllegalArgumentException.class, () -> disciplinas.buscarPorNome(" "));
    }

    private Usuario usuario(boolean admin) {
        Usuario u = admin ? new Administrador() : new Aluno();
        u.setNome("Teste");
        u.setEmail(UUID.randomUUID() + "@example.invalid");
        return usuarios.save(u);
    }

    private Conteudo conteudo() {
        Conteudo c = new Conteudo();
        c.setTitulo("Java");
        c.setOrdem(0);
        c.setDisciplina(disciplina("Programacao"));
        return conteudos.save(c);
    }

    private Tag tag() {
        Tag t = new Tag();
        t.setNome(UUID.randomUUID().toString());
        return tags.save(t);
    }

    private Postagem postagem(Usuario autor, Conteudo conteudo, Tag tag) {
        Postagem p = new Postagem();
        p.setTitulo("Original");
        return postagens.criarPostagem(p, autor.getId(), conteudo.getId(), List.of(tag.getId()));
    }

    @Test
    void deveFiltrarPostagensEOrdenarMaisRecentesPrimeiro() {
        Usuario autor = usuario(false);
        Conteudo c = conteudo();
        Tag t = tag();
        Postagem antiga = postagem(autor, c, t);
        antiga.setDataCriacao(LocalDateTime.now().minusDays(1));
        Postagem nova = postagem(autor, c, t);
        postagem(usuario(false), conteudo(), tag());
        em.flush(); em.clear();
        List<UUID> esperados = List.of(nova.getId(), antiga.getId());
        assertEquals(esperados, postagens.listarPorAutor(autor.getId()).stream().map(Postagem::getId).toList());
        assertEquals(esperados, postagens.listarPorConteudo(c.getId()).stream().map(Postagem::getId).toList());
        assertEquals(esperados, postagens.listarPorTag(t.getId()).stream().map(Postagem::getId).toList());
        assertEquals(StatusPostagem.RASCUNHO, postagens.buscarPorId(nova.getId()).getStatus());
        assertTrue(postagens.listarPorTag(UUID.randomUUID()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> postagens.buscarPorId(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> postagens.listarPorAutor(null));
        assertThrows(IllegalArgumentException.class, () -> postagens.listarPorConteudo(null));
        assertThrows(IllegalArgumentException.class, () -> postagens.listarPorTag(null));
    }

    @Test
    void devePermitirEdicaoPeloAutorEAdministradorPreservandoDados() {
        Usuario autor = usuario(false);
        Usuario admin = usuario(true);
        Conteudo c = conteudo();
        Tag t = tag();
        Postagem p = postagem(autor, c, t);
        LocalDateTime criacao = p.getDataCriacao();
        postagens.editarPostagem(p.getId(), autor.getId(), " Novo titulo ", "Descricao");
        em.flush(); em.clear();
        Postagem lida = postagens.buscarPorId(p.getId());
        assertEquals("Novo titulo", lida.getTitulo());
        assertEquals("Descricao", lida.getDescricao());
        assertEquals(autor.getId(), lida.getCriador().getId());
        assertEquals(c.getId(), lida.getConteudo().getId());
        assertEquals(t.getId(), lida.getTags().getFirst().getId());
        assertEquals(0, lida.getPontuacao().signum());
        assertEquals(StatusPostagem.RASCUNHO, lida.getStatus());
        assertTrue(lida.getDataAtualizacao().isAfter(criacao));
        postagens.editarPostagem(p.getId(), admin.getId(), "Revisado", null);
        em.flush(); em.clear();
        assertEquals("Revisado", postagens.buscarPorId(p.getId()).getTitulo());
    }

    @Test
    void deveNegarEdicaoPorOutroUsuarioSemAlterarPostagem() {
        Postagem p = postagem(usuario(false), conteudo(), tag());
        Usuario outro = usuario(false);
        assertThrows(IllegalArgumentException.class,
                () -> postagens.editarPostagem(p.getId(), outro.getId(), "Alterado", null));
        assertEquals("Original", p.getTitulo());
        assertThrows(IllegalArgumentException.class,
                () -> postagens.editarPostagem(p.getId(), p.getCriador().getId(), " ", null));
    }
}
