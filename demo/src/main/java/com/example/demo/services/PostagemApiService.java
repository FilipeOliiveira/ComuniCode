package com.example.demo.services;

import com.example.demo.dto.*;
import com.example.demo.model.Postagem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/** Converte relacionamentos lazy em DTOs antes de encerrar a transacao. */
@Service
@Transactional(readOnly = true)
public class PostagemApiService {
    private final PostagemService service;
    public PostagemApiService(PostagemService service) { this.service = service; }

    public List<PostagemResponse> listar(UUID conteudoId, UUID autorId, UUID tagId) {
        int filtros = (conteudoId == null ? 0 : 1) + (autorId == null ? 0 : 1) + (tagId == null ? 0 : 1);
        if (filtros > 1) throw new IllegalArgumentException("Informe apenas um filtro por consulta.");
        List<Postagem> postagens = conteudoId != null ? service.listarPorConteudo(conteudoId)
                : autorId != null ? service.listarPorAutor(autorId)
                : tagId != null ? service.listarPorTag(tagId) : service.listarTodas();
        return postagens.stream().map(PostagemResponse::de).toList();
    }

    public PostagemResponse buscar(UUID id) { return PostagemResponse.de(service.buscarPorId(id)); }

    @Transactional
    public PostagemResponse criar(PostagemRequest request, UUID autorId) {
        return PostagemResponse.de(service.criarPostagem(request.toEntity(), autorId, request.conteudoId(), request.tagsIds()));
    }

    @Transactional
    public PostagemResponse editar(UUID id, UUID autorId, String titulo, String descricao) {
        return PostagemResponse.de(service.editarPostagem(id, autorId, titulo, descricao));
    }

    @Transactional
    public void excluir(UUID id, UUID autorId) { service.excluirPostagem(id, autorId); }
}
