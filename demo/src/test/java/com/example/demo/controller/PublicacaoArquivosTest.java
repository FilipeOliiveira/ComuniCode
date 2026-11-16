package com.example.demo.controller;

import com.example.demo.exception.ArmazenamentoIndisponivelException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.storage.ArmazenamentoArquivos;
import com.example.demo.storage.ReferenciaArmazenamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Upload real pela API, com o Supabase Storage simulado (nenhum arquivo sai da maquina). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:publicacao_arquivos;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicacaoArquivosTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ConteudoRepository conteudos;
    @Autowired TagRepository tags;
    @Autowired AnexoRepository anexos;
    @MockitoBean ArmazenamentoArquivos armazenamento;

    private Aluno aluno;
    private Aluno outroAluno;
    private Conteudo conteudo;
    private Tag tag;
    private static final String SENHA = "senha-de-teste";
    private static final byte[] PDF = "%PDF-1.7\nconteudo".getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void preparar() {
        aluno = criarAluno();
        outroAluno = criarAluno();
        String sufixo = UUID.randomUUID().toString();
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("Estruturas " + sufixo);
        disciplina.setCodigo(sufixo);
        disciplina.setCargaHoraria(60);
        disciplina.setPeriodo(2);
        disciplina = disciplinas.save(disciplina);
        conteudo = new Conteudo();
        conteudo.setTitulo("Arvores");
        conteudo.setOrdem(0);
        conteudo.setDisciplina(disciplina);
        conteudo = conteudos.save(conteudo);
        tag = new Tag();
        tag.setNome("Tag " + sufixo);
        tag = tags.save(tag);
        when(armazenamento.enviar(anyString(), any(), anyString()))
                .thenAnswer(chamada -> new ReferenciaArmazenamento("materiais", chamada.getArgument(0)));
    }

    private Aluno criarAluno() {
        String sufixo = UUID.randomUUID().toString();
        Aluno novo = new Aluno();
        novo.setMatricula(sufixo);
        novo.setSemestreAtual(1);
        novo.setNome("aluno-" + sufixo);
        novo.setEmail("aluno-" + sufixo + "@example.invalid");
        novo.setSenhaHash(encoder.encode(SENHA));
        novo.setAtivo(true);
        novo.setDataCadastro(LocalDateTime.now());
        return usuarios.save(novo);
    }

    private MockHttpSession login(Usuario usuario) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", usuario.getEmail()).param("senha", SENHA))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }

    private MockMultipartFile dados(UUID conteudoId) {
        String json = """
                {"titulo":"Lista de arvores","descricao":"Exercicios","conteudoId":"%s","tagsIds":["%s"]}
                """.formatted(conteudoId, tag.getId());
        return new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile pdf(String nome) {
        return new MockMultipartFile("arquivos", nome, "application/pdf", PDF);
    }

    private JsonNode publicar(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId()))
                        .file(pdf("lista.pdf")).session(session).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void devePublicarComArquivoSemExporAReferenciaInterna() throws Exception {
        JsonNode postagem = publicar(login(aluno));

        assertEquals("PUBLICADA", postagem.get("status").asText());
        assertEquals(aluno.getId().toString(), postagem.get("autorId").asText());
        JsonNode anexo = postagem.get("anexos").get(0);
        assertEquals("lista.pdf", anexo.get("nome").asText());
        assertEquals("PDF", anexo.get("tipo").asText());
        assertEquals(PDF.length, anexo.get("tamanho").asLong());
        assertEquals("/api/anexos/" + anexo.get("id").asText() + "/arquivo", anexo.get("url").asText());

        String gravada = anexos.findById(UUID.fromString(anexo.get("id").asText())).orElseThrow().getUrl();
        assertTrue(gravada.matches("storage://materiais/postagens/[0-9a-f-]{36}\\.pdf"), gravada);
        verify(armazenamento).enviar(startsWith("postagens/"), eq(PDF), eq("application/pdf"));
    }

    @Test
    void deveExigirLoginECsrfParaPublicar() throws Exception {
        mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId())).file(pdf("a.pdf")).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId())).file(pdf("a.pdf")).session(login(aluno)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(armazenamento);
    }

    @Test
    void deveRecusarArquivoInvalidoSemEnviarNadaAoStorage() throws Exception {
        var falso = new MockMultipartFile("arquivos", "falso.pdf", "application/pdf", "nao sou pdf".getBytes());
        mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId())).file(falso)
                        .session(login(aluno)).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400));
        mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId())).session(login(aluno)).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("mensagem").value("Envie pelo menos um arquivo."));
        verify(armazenamento, never()).enviar(anyString(), any(), anyString());
    }

    @Test
    void deveRecusarCamposDesconhecidosNosDados() throws Exception {
        var dados = new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, """
                {"titulo":"x","conteudoId":"%s","tagsIds":["%s"],"status":"ARQUIVADA"}
                """.formatted(conteudo.getId(), tag.getId()).getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/postagens").file(dados).file(pdf("a.pdf")).session(login(aluno)).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(armazenamento);
    }

    @Test
    void deveApagarArquivosEnviadosQuandoOBancoRecusaAPostagem() throws Exception {
        mvc.perform(multipart("/api/postagens").file(dados(UUID.randomUUID())).file(pdf("a.pdf")).file(pdf("b.pdf"))
                        .session(login(aluno)).with(csrf()))
                .andExpect(status().isNotFound());
        verify(armazenamento, times(2)).enviar(anyString(), any(), anyString());
        verify(armazenamento).excluir(argThat(lista -> lista.size() == 2));
    }

    @Test
    void deveResponder503QuandoOStorageFalha() throws Exception {
        doThrow(new ArmazenamentoIndisponivelException("Nao foi possivel salvar o arquivo. Tente novamente."))
                .when(armazenamento).enviar(anyString(), any(), anyString());
        mvc.perform(multipart("/api/postagens").file(dados(conteudo.getId())).file(pdf("a.pdf"))
                        .session(login(aluno)).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("mensagem").value("Nao foi possivel salvar o arquivo. Tente novamente."));
    }

    @Test
    void deveRedirecionarParaLinkTemporarioSoComSessao() throws Exception {
        String anexoId = publicar(login(aluno)).get("anexos").get(0).get("id").asText();
        when(armazenamento.linkTemporario(any(), any(), any()))
                .thenReturn(URI.create("https://projeto.supabase.co/storage/v1/object/sign/materiais/x.pdf?token=t"));

        mvc.perform(get("/api/anexos/" + anexoId + "/arquivo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/anexos/" + anexoId + "/arquivo").session(login(outroAluno)))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://projeto.supabase.co/storage/v1/object/sign/materiais/x.pdf?token=t"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(armazenamento).linkTemporario(argThat(ref -> ref.caminho().startsWith("postagens/")), any(), isNull());
        mvc.perform(get("/api/anexos/" + UUID.randomUUID() + "/arquivo").session(login(aluno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveApagarArquivosDoStorageAoExcluirPostagem() throws Exception {
        MockHttpSession session = login(aluno);
        JsonNode postagem = publicar(session);
        String id = postagem.get("id").asText();

        mvc.perform(delete("/api/postagens/" + id).session(login(outroAluno)).with(csrf()))
                .andExpect(status().isForbidden());
        verify(armazenamento, never()).excluir(any());

        mvc.perform(delete("/api/postagens/" + id).session(session).with(csrf())).andExpect(status().isNoContent());
        verify(armazenamento).excluir(argThat(lista -> lista.size() == 1
                && lista.get(0).caminho().startsWith("postagens/")));
        assertTrue(anexos.findById(UUID.fromString(postagem.get("anexos").get(0).get("id").asText())).isEmpty());
    }

    @Test
    void naoDeveAceitarReferenciaDeStoragePelaApiJson() throws Exception {
        String json = """
                {"titulo":"x","conteudoId":"%s","tagsIds":["%s"],
                 "anexos":[{"nome":"a.pdf","url":"storage://materiais/postagens/alheio.pdf","tipo":"PDF","tamanho":1}]}
                """.formatted(conteudo.getId(), tag.getId());
        mvc.perform(post("/api/postagens").session(login(aluno)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        assertEquals(List.of(), anexos.findAll().stream().filter(a -> a.getUrl().contains("alheio")).toList());
    }
}
