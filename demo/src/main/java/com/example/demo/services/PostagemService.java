package com.example.demo.services;

import com.example.demo.exception.*;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PostagemService {
    @Transactional(readOnly = true)
    public Postagem buscarPorId(UUID idPostagem) {
        validarId(idPostagem);
        return postagemRepository.findById(idPostagem)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Postagem nao encontrada."));
    }

    @Transactional(readOnly = true)
    public List<Postagem> listarPorConteudo(UUID idConteudo) {
        validarId(idConteudo);
        return postagemRepository.findByConteudoIdOrderByDataCriacaoDesc(idConteudo);
    }

    @Transactional(readOnly = true)
    public List<Postagem> listarPorAutor(UUID idAutor) {
        validarId(idAutor);
        return postagemRepository.findByCriadorIdOrderByDataCriacaoDesc(idAutor);
    }

    @Transactional(readOnly = true)
    public List<Postagem> listarPorTag(UUID idTag) {
        validarId(idTag);
        return postagemRepository.findDistinctByTagsIdOrderByDataCriacaoDesc(idTag);
    }

    /** Edicao de texto pelo autor ou administrador; outros campos sao preservados. */
    @Transactional
    public Postagem editarPostagem(UUID idPostagem, UUID idUsuarioLogado, String titulo, String descricao) {
        validarId(idUsuarioLogado);
        validarTitulo(titulo);
        Postagem postagem = buscarPorId(idPostagem);
        Usuario usuario = usuarioRepository.findById(idUsuarioLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
        if (!postagem.getCriador().getId().equals(usuario.getId()) && !(usuario instanceof Administrador)) {
            throw new PermissaoNegadaException("Acesso negado: sem permissao para editar esta postagem.");
        }
        postagem.setTitulo(titulo.trim());
        postagem.setDescricao(descricao);
        postagem.setDataAtualizacao(LocalDateTime.now());
        return postagemRepository.save(postagem);
    }

    private void validarId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID e obrigatorio.");
        }
    }

    private void validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank() || titulo.trim().length() > 255) {
            throw new IllegalArgumentException("O titulo deve conter entre 1 e 255 caracteres.");
        }
    }

    @Autowired
    private PostagemRepository postagemRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConteudoRepository conteudoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<Postagem> listarTodas() {
        return postagemRepository.findAll();
    }

    @Transactional
    public Postagem criarPostagem(Postagem postagem) {
        if (postagem == null) {
            throw new IllegalArgumentException("A postagem e obrigatoria.");
        }
        if (postagem.getCriador() == null || postagem.getCriador().getId() == null) {
            throw new IllegalArgumentException("O criador da postagem e obrigatorio.");
        }
        if (postagem.getConteudo() == null || postagem.getConteudo().getId() == null) {
            throw new IllegalArgumentException("O conteudo da postagem e obrigatorio.");
        }
        List<UUID> idsTags = postagem.getTags() == null ? List.of()
                : postagem.getTags().stream().map(Tag::getId).toList();
        return criarPostagem(postagem, postagem.getCriador().getId(),
                postagem.getConteudo().getId(), idsTags);
    }

    // =======================================================
    // 1. CRIAR POSTAGEM
    // =======================================================
    @Transactional
    public Postagem criarPostagem(Postagem postagem, UUID idCriador, UUID idConteudo, List<UUID> idsTags) {
        if (postagem == null || postagem.getId() != null) {
            throw new IllegalArgumentException("Informe uma nova postagem sem ID.");
        }
        validarTitulo(postagem.getTitulo());
        validarId(idCriador);
        validarId(idConteudo);
        
        // 1. Validações básicas de preenchimento
        if (postagem.getTitulo() == null || postagem.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("O título da postagem é obrigatório.");
        }
        if (idsTags == null || idsTags.isEmpty()) {
            throw new IllegalArgumentException("A postagem deve conter pelo menos uma tag.");
        }

        if (idsTags.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("As tags devem possuir IDs validos.");
        }

        // 2. Buscar Entidades Relacionadas no Banco (Garante que elas existem)
        Usuario criador = usuarioRepository.findById(idCriador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Criador não encontrado."));
        
        Conteudo conteudo = conteudoRepository.findById(idConteudo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado."));
        
        List<Tag> tags = tagRepository.findAllById(idsTags);
        if (tags.size() != idsTags.size()) {
            throw new RecursoNaoEncontradoException("Uma ou mais Tags informadas não foram encontradas no sistema.");
        }

        // 3. Montar o objeto Postagem
        postagem.setTitulo(postagem.getTitulo().trim());
        postagem.setCriador(criador);
        postagem.setConteudo(conteudo);
        postagem.setTags(tags);
        
        // Valores padrão (Default)
        postagem.setDataCriacao(LocalDateTime.now());
        postagem.setDataAtualizacao(LocalDateTime.now());
        postagem.setPontuacao(BigDecimal.ZERO);
        postagem.setScoreRelevancia(BigDecimal.ZERO);
        postagem.setVerificada(false);
        if (postagem.getStatus() == null) {
            postagem.setStatus(StatusPostagem.RASCUNHO);
        }

        // 4. Configurar relacionamento bidirecional dos Anexos (Necessário para o CascadeType.ALL)
        if (postagem.getAnexos() != null && !postagem.getAnexos().isEmpty()) {
            for (Anexo anexo : postagem.getAnexos()) {
                anexo.setPostagem(postagem); // Associa a postagem ao anexo antes de salvar
            }
        }

        // Salva a postagem (junto com os anexos, por causa do Cascade)
        return postagemRepository.save(postagem);
    }

    // =======================================================
    // 2. EXCLUIR POSTAGEM
    // =======================================================
    @Transactional
    public void excluirPostagem(UUID idPostagem, UUID idUsuarioLogado) {
        
        Postagem postagem = postagemRepository.findById(idPostagem)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Postagem não encontrada."));

        Usuario usuario = usuarioRepository.findById(idUsuarioLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        // Regra: Apenas o criador da postagem ou um Administrador podem excluí-la
        boolean isCriador = postagem.getCriador().getId().equals(usuario.getId());
        boolean isAdmin = usuario instanceof Administrador;

        if (!isCriador && !isAdmin) {
            throw new PermissaoNegadaException("Acesso negado: Você não tem permissão para excluir esta postagem.");
        }

        // Como a Postagem tem cascade=CascadeType.ALL nos Anexos, os anexos também serão deletados
        postagemRepository.delete(postagem);
    }
}
