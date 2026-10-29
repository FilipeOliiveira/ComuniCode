package com.example.demo.dto;

import com.example.demo.model.Postagem;
import com.example.demo.model.StatusPostagem;
import com.example.demo.security.UsuarioAutenticado;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PostagemResponse(UUID id, String titulo, String descricao, UUID autorId, String autorNome,
        UUID conteudoId, StatusPostagem status, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao,
        List<TagResponse> tags, List<AnexoResponse> anexos,
        String autorPerfil, UUID disciplinaId, String disciplinaNome, String conteudoTitulo, boolean verificada) {
    public static PostagemResponse de(Postagem postagem) {
        return new PostagemResponse(postagem.getId(), postagem.getTitulo(), postagem.getDescricao(),
                postagem.getCriador().getId(), postagem.getCriador().getNome(), postagem.getConteudo().getId(),
                postagem.getStatus(), postagem.getDataCriacao(), postagem.getDataAtualizacao(),
                postagem.getTags() == null ? List.of() : postagem.getTags().stream().map(TagResponse::de).toList(),
                postagem.getAnexos() == null ? List.of() : postagem.getAnexos().stream().map(AnexoResponse::de).toList(),
                UsuarioAutenticado.perfil(postagem.getCriador()), postagem.getConteudo().getDisciplina().getId(),
                postagem.getConteudo().getDisciplina().getNome(), postagem.getConteudo().getTitulo(),
                Boolean.TRUE.equals(postagem.getVerificada()));
    }
}
