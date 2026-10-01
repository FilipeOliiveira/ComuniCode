package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sem transacao no teste: cada requisicao deve funcionar com open-in-view=false. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_integracao;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegracaoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ConteudoRepository conteudos;
    @Autowired TagRepository tags;
    @Autowired PostagemRepository postagens;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private Aluno aluno;
    private Administrador admin;
    private Conteudo conteudo;
    private Tag tag;
    private static final String SENHA = "senha-de-teste";

    @BeforeEach
    void preparar() {
        String sufixo = UUID.randomUUID().toString();
        aluno = new Aluno();
        aluno.setMatricula(sufixo);
        aluno.setSemestreAtual(1);
        preencherUsuario(aluno, "aluno-" + sufixo);
        aluno = usuarios.save(aluno);
        admin = new Administrador();
        admin.setNivelAcesso("TOTAL");
        preencherUsuario(admin, "admin-" + sufixo);
        admin = usuarios.save(admin);
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("Programacao " + sufixo);
        disciplina.setCodigo(sufixo);
        disciplina.setCargaHoraria(60);
        disciplina.setPeriodo(1);
        disciplina = disciplinas.save(disciplina);
        conteudo = new Conteudo();
        conteudo.setTitulo("Java");
        conteudo.setOrdem(0);
        conteudo.setDisciplina(disciplina);
        conteudo = conteudos.save(conteudo);
        tag = new Tag();
        tag.setNome("Tag " + sufixo);
        tag = tags.save(tag);
    }

    private void preencherUsuario(Usuario usuario, String nome) {
        usuario.setNome(nome);
        usuario.setEmail(nome + "@example.invalid");
        usuario.setSenhaHash(encoder.encode(SENHA));
        usuario.setAtivo(true);
        usuario.setDataCadastro(LocalDateTime.now());
    }

    private MockHttpSession login(Usuario usuario) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", usuario.getEmail()).param("senha", SENHA))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    private String novaPostagem() {
        return """
                {"titulo":"Minha postagem","descricao":"Texto","conteudoId":"%s","tagsIds":["%s"],
                 "anexos":[{"nome":"Material","url":"https://example.invalid/material.pdf","tipo":"PDF","tamanho":120}]}
                """.formatted(conteudo.getId(), tag.getId());
    }

    private String criarPostagem(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(post("/api/postagens").session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(novaPostagem()))
                .andExpect(status().isCreated()).andExpect(jsonPath("autorId").value(aluno.getId().toString()))
                .andExpect(jsonPath("senhaHash").doesNotExist())
                .andExpect(jsonPath("criador").doesNotExist()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void deveExigirLoginEProtegerRequisicoesSemCsrf() throws Exception {
        mvc.perform(get("/api/postagens")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").param("email", aluno.getEmail()).param("senha", SENHA))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/cadastro/aluno").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveFazerLoginComTokenRealRenovarSessaoEFazerLogout() throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = mapper.readTree(csrfResult.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String idAnterior = session.getId();
        mvc.perform(post("/api/auth/login").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .param("email", " " + aluno.getEmail().toUpperCase() + " ").param("senha", SENHA))
                .andExpect(status().isNoContent());
        assertNotEquals(idAnterior, session.getId());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("id").value(aluno.getId().toString()))
                .andExpect(jsonPath("perfil").value("ALUNO"))
                .andExpect(jsonPath("senhaHash").doesNotExist());
        // O token anterior ao login deixa de ser valido.
        mvc.perform(post("/api/auth/logout").session(session)
                .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isForbidden());
        MvcResult novoToken = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        var atual = mapper.readTree(novoToken.getResponse().getContentAsString());
        mvc.perform(post("/api/auth/logout").session(session)
                .header(atual.get("headerName").asText(), atual.get("token").asText()))
                .andExpect(status().isNoContent());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarSenhaIncorretaUsuarioAusenteEInativoSemDistinguirMensagem() throws Exception {
        String resposta = mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", aluno.getEmail()).param("senha", "errada"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", "ausente@example.invalid").param("senha", SENHA))
                .andExpect(status().isUnauthorized()).andExpect(content().json(resposta));
        aluno.setAtivo(false);
        usuarios.save(aluno);
        mvc.perform(post("/api/auth/login").with(csrf())
                .param("email", aluno.getEmail()).param("senha", SENHA))
                .andExpect(status().isUnauthorized()).andExpect(content().json(resposta));
    }

    @Test
    void deveCadastrarAlunoRetornarConflitoEPermitirLogin() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        String body = """
                {"nome":"Novo aluno","email":"%s","senha":"senha-de-teste","matricula":"%s","semestreAtual":1}
                """.formatted(email, UUID.randomUUID());
        mvc.perform(post("/api/auth/cadastro/aluno").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("perfil").value("ALUNO"))
                .andExpect(jsonPath("senhaHash").doesNotExist()).andExpect(jsonPath("senha").doesNotExist());
        assertTrue(encoder.matches(SENHA, usuarios.findByEmailIgnoreCase(email).orElseThrow().getSenhaHash()));
        mvc.perform(post("/api/auth/cadastro/aluno").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("status").value(409));
        mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("senha", SENHA))
                .andExpect(status().isNoContent());
    }

    @Test
    void deveRejeitarCamposPrivilegiadosEAutorForjado() throws Exception {
        mvc.perform(post("/api/auth/cadastro/aluno").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        MockHttpSession session = login(aluno);
        String forjado = novaPostagem().trim();
        forjado = forjado.substring(0, forjado.length() - 1) + ",\"criador\":{\"id\":\"" + admin.getId() + "\"}}";
        long antes = postagens.count();
        mvc.perform(post("/api/postagens").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(forjado))
                .andExpect(status().isBadRequest());
        assertEquals(antes, postagens.count());
    }

    @Test
    void deveCriarConsultarEditarEExcluirPostagemSemExporEntidades() throws Exception {
        MockHttpSession session = login(aluno);
        String id = criarPostagem(session);
        mvc.perform(get("/api/postagens/" + id).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("tags[0].id").value(tag.getId().toString()))
                .andExpect(jsonPath("anexos[0].nome").value("Material"))
                .andExpect(jsonPath("anexos[0].postagem").doesNotExist());
        mvc.perform(get("/api/postagens").param("conteudoId", conteudo.getId().toString()).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(patch("/api/postagens/" + id).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Editada\",\"descricao\":\"Novo texto\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("titulo").value("Editada"));
        mvc.perform(delete("/api/postagens/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        assertFalse(postagens.existsById(UUID.fromString(id)));
        assertEquals(0, jdbc.queryForObject("select count(*) from anexo where postagem_id = ?", Integer.class, UUID.fromString(id)));
        mvc.perform(get("/api/postagens/" + id).session(session)).andExpect(status().isNotFound());
    }

    @Test
    void deveNegarEdicaoEExclusaoPorOutroAlunoEPermitirAdmin() throws Exception {
        String id = criarPostagem(login(aluno));
        Aluno outro = new Aluno();
        outro.setMatricula(UUID.randomUUID().toString());
        outro.setSemestreAtual(1);
        preencherUsuario(outro, UUID.randomUUID().toString());
        outro = usuarios.save(outro);
        MockHttpSession session = login(outro);
        mvc.perform(patch("/api/postagens/" + id).session(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Tentativa\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/postagens/" + id).session(session).with(csrf())).andExpect(status().isForbidden());
        assertTrue(postagens.existsById(UUID.fromString(id)));
        mvc.perform(delete("/api/postagens/" + id).session(login(admin)).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void deveRestringirCadastrosAdministrativos() throws Exception {
        MockHttpSession session = login(aluno);
        for (String rota : new String[]{"/api/disciplinas", "/api/conteudos", "/api/tags", "/api/auth/cadastro/professor"}) {
            mvc.perform(post(rota).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        MockHttpSession adminSession = login(admin);
        String sufixo = UUID.randomUUID().toString();
        mvc.perform(post("/api/auth/cadastro/professor").session(adminSession).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Professor","email":"%s@example.invalid","senha":"senha-de-teste","registro":"%s"}
                        """.formatted(sufixo, sufixo)))
                .andExpect(status().isCreated()).andExpect(jsonPath("perfil").value("PROFESSOR"));
        Usuario professor = usuarios.findByEmailIgnoreCase(sufixo + "@example.invalid").orElseThrow();
        MockHttpSession professorSession = login(professor);
        mvc.perform(post("/api/tags").session(professorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + sufixo + "\"}" )).andExpect(status().isCreated());
        mvc.perform(post("/api/conteudos").session(professorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Novo\",\"disciplinaId\":\"" + conteudo.getDisciplina().getId() + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("ordem").value(0));
        mvc.perform(post("/api/disciplinas").session(professorSession).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/disciplinas").session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Nova %s","codigo":"%s","cargaHoraria":60,"periodo":1}
                        """.formatted(sufixo, sufixo))).andExpect(status().isCreated());
    }

    @Test
    void deveConsultarCatalogoEValidarParametros() throws Exception {
        MockHttpSession session = login(aluno);
        mvc.perform(get("/api/disciplinas/" + conteudo.getDisciplina().getId()).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("conteudos").doesNotExist());
        mvc.perform(get("/api/conteudos").session(session).param("disciplinaId", conteudo.getDisciplina().getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(conteudo.getId().toString()));
        mvc.perform(get("/api/conteudos/" + conteudo.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/tags/busca").session(session).param("nome", tag.getNome()))
                .andExpect(status().isOk()).andExpect(jsonPath("id").value(tag.getId().toString()));
        mvc.perform(get("/api/conteudos").session(session)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/postagens/id-invalido").session(session)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/postagens").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/conteudos/" + UUID.randomUUID()).session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/usuarios").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void deveRejeitarReferenciasAusentesTagsNulasEAnexosInvalidos() throws Exception {
        MockHttpSession session = login(aluno);
        mvc.perform(post("/api/postagens").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(novaPostagem().replace(tag.getId().toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/postagens").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(novaPostagem().replace("\"" + tag.getId() + "\"", "null")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/postagens").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(novaPostagem().replace("https://example.invalid/material.pdf", "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarConflitoParaDisciplinaETagDuplicadas() throws Exception {
        MockHttpSession session = login(admin);
        mvc.perform(post("/api/tags").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("nome", tag.getNome()))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/disciplinas").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("nome", "Outra", "codigo", conteudo.getDisciplina().getCodigo(),
                        "cargaHoraria", 60, "periodo", 1))))
                .andExpect(status().isConflict());
    }
}
