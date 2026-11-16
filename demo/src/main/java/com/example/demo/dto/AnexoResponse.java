package com.example.demo.dto;

import com.example.demo.model.Anexo;
import com.example.demo.model.TipoAnexo;
import com.example.demo.storage.ReferenciaArmazenamento;
import java.util.UUID;

/** Arquivos hospedados pelo portal saem como a rota de download; a referencia interna nunca e exposta. */
public record AnexoResponse(UUID id, String nome, String url, TipoAnexo tipo, Long tamanho) {
    public static AnexoResponse de(Anexo anexo) {
        String url = ReferenciaArmazenamento.ehInterna(anexo.getUrl())
                ? "/api/anexos/" + anexo.getId() + "/arquivo" : anexo.getUrl();
        return new AnexoResponse(anexo.getId(), anexo.getNome(), url, anexo.getTipo(), anexo.getTamanho());
    }
}
