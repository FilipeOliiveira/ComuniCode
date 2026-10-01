package com.example.demo.dto;

import com.example.demo.model.Anexo;
import com.example.demo.model.TipoAnexo;
import java.util.UUID;

public record AnexoResponse(UUID id, String nome, String url, TipoAnexo tipo, Long tamanho) {
    public static AnexoResponse de(Anexo anexo) {
        return new AnexoResponse(anexo.getId(), anexo.getNome(), anexo.getUrl(), anexo.getTipo(), anexo.getTamanho());
    }
}
