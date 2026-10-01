package com.example.demo.dto;

import com.example.demo.model.Postagem;
import java.util.List;
import java.util.UUID;

/** Autor, pontuacao e verificacao sao definidos exclusivamente no servidor. */
public record PostagemRequest(String titulo, String descricao, UUID conteudoId, List<UUID> tagsIds,
                              List<AnexoRequest> anexos) {
    public Postagem toEntity() {
        Postagem postagem = new Postagem();
        postagem.setTitulo(titulo);
        postagem.setDescricao(descricao);
        if (anexos != null) {
            if (anexos.stream().anyMatch(java.util.Objects::isNull)) {
                throw new IllegalArgumentException("Anexos nao podem ser nulos.");
            }
            postagem.setAnexos(new java.util.ArrayList<>(anexos.stream().map(AnexoRequest::toEntity).toList()));
        }
        return postagem;
    }
}
