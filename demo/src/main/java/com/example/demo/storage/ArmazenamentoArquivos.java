package com.example.demo.storage;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/** Guarda os arquivos dos anexos fora do banco. A implementacao real usa o Supabase Storage. */
public interface ArmazenamentoArquivos {
    /** Envia o arquivo e devolve a referencia permanente a gravar em anexo.url. */
    ReferenciaArmazenamento enviar(String caminho, byte[] conteudo, String contentType);

    /** Remove os objetos; referencias inexistentes sao ignoradas pelo Storage. */
    void excluir(List<ReferenciaArmazenamento> referencias);

    /** Gera um link temporario de leitura. Com nomeDownload, o navegador baixa com esse nome. */
    URI linkTemporario(ReferenciaArmazenamento referencia, Duration validade, String nomeDownload);
}
