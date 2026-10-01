package com.example.demo.dto;

import com.example.demo.model.Conteudo;
import java.util.UUID;

public record ConteudoResponse(UUID id, String titulo, String descricao, Integer ordem, UUID disciplinaId) {
    public static ConteudoResponse de(Conteudo conteudo) {
        return new ConteudoResponse(conteudo.getId(), conteudo.getTitulo(), conteudo.getDescricao(),
                conteudo.getOrdem(), conteudo.getDisciplina().getId());
    }
}
