package com.example.demo.dto;

import com.example.demo.model.Conteudo;
import java.util.UUID;

public record ConteudoRequest(String titulo, String descricao, Integer ordem, UUID disciplinaId) {
    public Conteudo toEntity() {
        Conteudo conteudo = new Conteudo();
        conteudo.setTitulo(titulo);
        conteudo.setDescricao(descricao);
        conteudo.setOrdem(ordem);
        return conteudo;
    }
}
