package com.example.demo.services;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Executar explicitamente com -Dtest=ServicesSupabaseIT; rollback apos cada teste. */
@SpringBootTest
@ActiveProfiles({"supabase", "integracao-supabase"})
@Transactional
class ServicesSupabaseIT {
    @Autowired private DisciplinaService disciplinaService;
    @Autowired private PostagemService postagemService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private DisciplinaRepository disciplinaRepository;
    @Autowired private ConteudoRepository conteudoRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private PostagemRepository postagemRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private Administrador admin;
    private Aluno aluno;

    @BeforeEach
    void prepararUsuarios() {
        admin = new Administrador();
        preencherUsuario(admin, "Administrador de teste");
        admin.setNivelAcesso("TOTAL");
        usuarioRepository.save(admin);

        aluno = new Aluno();
        preencherUsuario(aluno, "Aluno de teste");
        aluno.setMatricula(UUID.randomUUID().toString());
        aluno.setSemestreAtual(1);
        usuarioRepository.save(aluno);
        enviarSqlELimparCache();
    }

    private void preencherUsuario(Usuario usuario, String nome) {
        usuario.setNome(nome);
        usuario.setEmail(UUID.randomUUID() + "@example.invalid");
        usuario.setSenhaHash(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setAtivo(false);
    }

    private Disciplina novaDisciplina() {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("Programacao " + UUID.randomUUID());
        disciplina.setCodigo("PROG-" + UUID.randomUUID());
        disciplina.setCargaHoraria(60);
        disciplina.setPeriodo(1);
        return disciplina;
    }

    private void enviarSqlELimparCache() {
        // Forca INSERT/DELETE e impede que a proxima leitura use so objetos em memoria.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Disciplina: salvar pelo service e consultar no banco")
    void devePersistirDisciplina() {
        Disciplina criada = disciplinaService.criarDisciplina(novaDisciplina(), admin.getId());
        enviarSqlELimparCache();

        Disciplina lida = disciplinaRepository.findById(criada.getId()).orElseThrow();
        assertEquals(criada.getNome(), lida.getNome());
        assertEquals(60, lida.getCargaHoraria());
        assertEquals(criada.getCodigo(), jdbc.queryForObject(
                "select codigo from disciplina where id = ?", String.class, criada.getId()));
        System.out.printf("[INTEGRACAO] Disciplina gravada e lida: %s | %s%n", lida.getId(), lida.getNome());
    }

    @Test
    @DisplayName("Disciplina: rejeitar codigo ja cadastrado, ignorando maiusculas")
    void deveConsultarDuplicidadeNoBanco() {
        Disciplina primeira = disciplinaService.criarDisciplina(novaDisciplina(), admin.getId());
        enviarSqlELimparCache();
        Disciplina duplicada = novaDisciplina();
        duplicada.setCodigo(primeira.getCodigo().toLowerCase(java.util.Locale.ROOT));
        long quantidadeAntes = disciplinaRepository.count();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> disciplinaService.criarDisciplina(duplicada, admin.getId()));

        assertEquals("Já existe uma disciplina cadastrada com o código informado.", erro.getMessage());
        assertEquals(quantidadeAntes, disciplinaRepository.count());
        System.out.println("[INTEGRACAO] Codigo duplicado localizado no banco e rejeitado.");
    }

    @Test
    @DisplayName("Disciplina: consultar perfil persistido e impedir criacao por aluno")
    void deveValidarPermissaoComUsuarioDoBanco() {
        long quantidadeAntes = disciplinaRepository.count();
        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> disciplinaService.criarDisciplina(novaDisciplina(), aluno.getId()));
        assertEquals("Acesso negado: Apenas administradores podem criar disciplinas.", erro.getMessage());
        assertEquals(quantidadeAntes, disciplinaRepository.count());
        System.out.println("[INTEGRACAO] Perfil Aluno recuperado do banco; criacao negada.");
    }

    @Test
    @DisplayName("Usuario: consultar email no banco e rejeitar senha incorreta")
    void deveConsultarUsuarioERejeitarSenhaIncorreta() {
        Usuario encontrado = usuarioRepository.findByEmail(aluno.getEmail()).orElseThrow();
        assertEquals(aluno.getId(), encontrado.getId());
        assertInstanceOf(Aluno.class, encontrado);
        enviarSqlELimparCache();
        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> usuarioService.realizarLogin(aluno.getEmail(), "senha-incorreta"));
        assertEquals("Falha no login: Senha incorreta.", erro.getMessage());
        System.out.printf("[INTEGRACAO] Usuario consultado: %s; senha incorreta rejeitada.%n", encontrado.getId());
    }

    @AfterTransaction
    void confirmarRollback() {
        if (admin != null && admin.getId() != null) {
            assertEquals(0L, jdbc.queryForObject("select count(*) from usuario where id = ?", Long.class, admin.getId()));
        }
        if (aluno != null && aluno.getId() != null) {
            assertEquals(0L, jdbc.queryForObject("select count(*) from usuario where id = ?", Long.class, aluno.getId()));
        }
        System.out.println("[INTEGRACAO] Rollback confirmado: usuarios de teste nao permaneceram no banco.");
    }

    @Test
    @DisplayName("Usuario: cadastrar aluno e fazer login com BCrypt")
    void deveCadastrarAlunoEFazerLogin() {
        Aluno novo = new Aluno();
        novo.setNome("Aluno cadastro integracao");
        novo.setEmail(UUID.randomUUID() + "@example.invalid");
        novo.setMatricula(UUID.randomUUID().toString());
        novo.setSemestreAtual(1);
        Aluno salvo = usuarioService.cadastrarAluno(novo, "senha-ficticia-teste");
        enviarSqlELimparCache();
        assertEquals(1L, jdbc.queryForObject(
                "select count(*) from usuario u join aluno a on a.id = u.id where u.id = ?",
                Long.class, salvo.getId()));
        Usuario autenticado = usuarioService.realizarLogin(novo.getEmail().toUpperCase(java.util.Locale.ROOT), "senha-ficticia-teste");
        assertEquals(salvo.getId(), autenticado.getId());
        assertInstanceOf(Aluno.class, autenticado);
        assertNotEquals("senha-ficticia-teste", autenticado.getSenhaHash());
        System.out.println("[INTEGRACAO] Aluno cadastrado em usuario/aluno e login BCrypt confirmado.");
    }

    @Test
    @DisplayName("Usuario: cadastrar professor e fazer login com BCrypt")
    void deveCadastrarProfessorEFazerLogin() {
        Professor novo = new Professor();
        novo.setNome("Professor cadastro integracao");
        novo.setEmail(UUID.randomUUID() + "@example.invalid");
        novo.setRegistro(UUID.randomUUID().toString());
        novo.setPesoAvaliacao(java.math.BigDecimal.ONE);
        Professor salvo = usuarioService.cadastrarProfessor(novo, "senha-ficticia-teste");
        enviarSqlELimparCache();
        assertEquals(1L, jdbc.queryForObject(
                "select count(*) from usuario u join professor p on p.id = u.id where u.id = ?",
                Long.class, salvo.getId()));
        Usuario autenticado = usuarioService.realizarLogin(novo.getEmail(), "senha-ficticia-teste");
        assertEquals(salvo.getId(), autenticado.getId());
        assertInstanceOf(Professor.class, autenticado);
        System.out.println("[INTEGRACAO] Professor cadastrado em usuario/professor e login BCrypt confirmado.");
    }

    private Postagem criarPostagemCompleta() {
        Disciplina disciplina = disciplinaService.criarDisciplina(novaDisciplina(), admin.getId());
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo("Introducao ao Java");
        conteudo.setOrdem(1);
        conteudo.setDisciplina(disciplina);
        conteudoRepository.save(conteudo);

        Tag tag = new Tag();
        tag.setNome("java-" + UUID.randomUUID());
        tagRepository.save(tag);

        Anexo anexo = new Anexo();
        anexo.setNome("material.pdf");
        anexo.setUrl("https://example.invalid/material.pdf");
        anexo.setTipo(TipoAnexo.PDF);
        anexo.setTamanho(100L);
        anexo.setDataUpload(LocalDateTime.now());

        Postagem postagem = new Postagem();
        postagem.setTitulo("Material de integracao");
        postagem.setStatus(StatusPostagem.RASCUNHO);
        postagem.setAnexos(new ArrayList<>(List.of(anexo)));
        return postagemService.criarPostagem(postagem, aluno.getId(), conteudo.getId(), List.of(tag.getId()));
    }

    @Test
    @DisplayName("Postagem: gravar e ler autor, conteudo, tag e anexo")
    void devePersistirPostagemERelacionamentos() {
        Postagem criada = criarPostagemCompleta();
        enviarSqlELimparCache();

        Postagem lida = postagemRepository.findById(criada.getId()).orElseThrow();
        assertEquals("Material de integracao", lida.getTitulo());
        assertEquals(aluno.getId(), lida.getCriador().getId());
        assertEquals("Introducao ao Java", lida.getConteudo().getTitulo());
        assertEquals(1, lida.getTags().size());
        assertEquals(1, lida.getAnexos().size());
        assertEquals("material.pdf", lida.getAnexos().getFirst().getNome());
        assertEquals(0, lida.getPontuacao().signum());
        assertFalse(lida.getVerificada());
        assertTrue(postagemService.listarTodas().stream().anyMatch(p -> p.getId().equals(criada.getId())));
        assertEquals(1L, jdbc.queryForObject(
                "select count(*) from postagem_tag where postagem_id = ?", Long.class, criada.getId()));
        System.out.printf("[INTEGRACAO] Postagem %s lida com autor, conteudo, tag e anexo.%n", lida.getId());
    }

    @Test
    @DisplayName("Postagem: excluir pelo criador e remover anexo e associacao com tag")
    void deveExcluirPostagemEAnexo() {
        Postagem criada = criarPostagemCompleta();
        enviarSqlELimparCache();

        postagemService.excluirPostagem(criada.getId(), aluno.getId());
        enviarSqlELimparCache();

        assertFalse(postagemRepository.existsById(criada.getId()));
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from anexo where postagem_id = ?", Long.class, criada.getId()));
        assertEquals(0L, jdbc.queryForObject(
                "select count(*) from postagem_tag where postagem_id = ?", Long.class, criada.getId()));
        System.out.printf("[INTEGRACAO] Exclusao confirmada no banco: postagem %s e seus anexos.%n", criada.getId());
    }
}
