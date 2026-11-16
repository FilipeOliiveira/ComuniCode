package com.example.demo.services;

import com.example.demo.dto.*;
import com.example.demo.model.Anexo;
import com.example.demo.model.Postagem;
import com.example.demo.storage.ArmazenamentoArquivos;
import com.example.demo.storage.ReferenciaArmazenamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.UUID;

/** Converte relacionamentos lazy em DTOs antes de encerrar a transacao. */
@Service
@Transactional(readOnly = true)
public class PostagemApiService {
    private static final Logger log = LoggerFactory.getLogger(PostagemApiService.class);

    private final PostagemService service;
    private final ArmazenamentoArquivos armazenamento;

    public PostagemApiService(PostagemService service, ArmazenamentoArquivos armazenamento) {
        this.service = service;
        this.armazenamento = armazenamento;
    }

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

    /** Apaga os arquivos do Storage so depois do commit; uma falha ali nao desfaz a exclusao. */
    @Transactional
    public void excluir(UUID id, UUID autorId) {
        List<Anexo> anexos = id == null ? List.of() : service.buscarPorId(id).getAnexos();
        List<ReferenciaArmazenamento> arquivos = anexos == null ? List.of() : anexos.stream()
                .map(Anexo::getUrl).filter(ReferenciaArmazenamento::ehInterna).map(ReferenciaArmazenamento::de).toList();
        service.excluirPostagem(id, autorId);
        if (arquivos.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { apagarArquivos(arquivos); }
            });
        } else {
            apagarArquivos(arquivos);
        }
    }

    private void apagarArquivos(List<ReferenciaArmazenamento> arquivos) {
        try { armazenamento.excluir(arquivos); }
        catch (RuntimeException ex) { log.error("Postagem excluida, mas os arquivos ficaram no Storage: {}", arquivos, ex); }
    }
}
