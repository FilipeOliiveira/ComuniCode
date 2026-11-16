package com.example.demo.dto;

import java.util.List;
import java.util.UUID;

/** Parte "dados" do envio multipart; os arquivos chegam na parte "arquivos". */
public record PublicacaoRequest(String titulo, String descricao, UUID conteudoId, List<UUID> tagsIds) {
}
