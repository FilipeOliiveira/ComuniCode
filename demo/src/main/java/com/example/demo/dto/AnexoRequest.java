package com.example.demo.dto;

import com.example.demo.model.Anexo;
import com.example.demo.model.TipoAnexo;
import java.net.URI;
import java.time.LocalDateTime;

/** Metadados de arquivo ja hospedado; esta API ainda nao realiza upload. */
public record AnexoRequest(String nome, String url, TipoAnexo tipo, Long tamanho) {
    public Anexo toEntity() {
        if (nome == null || nome.isBlank() || nome.trim().length() > 255
                || url == null || url.isBlank() || tipo == null || tamanho == null || tamanho < 0) {
            throw new IllegalArgumentException("Informe nome, URL, tipo e tamanho valido do anexo.");
        }
        URI uri;
        try { uri = URI.create(url); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("URL do anexo invalida."); }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("A URL do anexo deve ser HTTP ou HTTPS, sem credenciais.");
        }
        Anexo anexo = new Anexo();
        anexo.setNome(nome.trim());
        anexo.setUrl(url);
        anexo.setTipo(tipo);
        anexo.setTamanho(tamanho);
        anexo.setDataUpload(LocalDateTime.now());
        return anexo;
    }
}
