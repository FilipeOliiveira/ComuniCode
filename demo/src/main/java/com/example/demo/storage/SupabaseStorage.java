package com.example.demo.storage;

import com.example.demo.exception.ArmazenamentoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Cliente da API REST do Supabase Storage. Usa a chave secreta (service_role), que fica
 * apenas no backend (demo/.env). O bucket deve ser privado.
 */
@Component
public class SupabaseStorage implements ArmazenamentoArquivos {
    private static final Logger log = LoggerFactory.getLogger(SupabaseStorage.class);

    private final RestClient http;
    private final String urlBase;
    private final String chave;
    private final String bucket;

    public SupabaseStorage(@Value("${SUPABASE_URL:}") String url,
                           @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String chave,
                           @Value("${SUPABASE_STORAGE_BUCKET:materiais}") String bucket) {
        String base = url == null ? "" : url.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        this.urlBase = base;
        this.chave = chave == null ? "" : chave.trim();
        this.bucket = bucket == null || bucket.isBlank() ? "materiais" : bucket.trim();
        this.http = RestClient.builder().build();
    }

    @Override
    public ReferenciaArmazenamento enviar(String caminho, byte[] conteudo, String contentType) {
        exigirConfiguracao();
        ReferenciaArmazenamento referencia = new ReferenciaArmazenamento(bucket, caminho);
        try {
            http.post().uri(objeto("object", referencia)).headers(autenticacao())
                    .header("x-upsert", "false").contentType(MediaType.parseMediaType(contentType))
                    .body(conteudo).retrieve().toBodilessEntity();
            return referencia;
        } catch (RestClientException ex) {
            log.error("Falha ao enviar {} ao Supabase Storage: {}", referencia, ex.getMessage());
            throw new ArmazenamentoIndisponivelException("Nao foi possivel salvar o arquivo. Tente novamente.", ex);
        }
    }

    @Override
    public void excluir(List<ReferenciaArmazenamento> referencias) {
        if (referencias == null || referencias.isEmpty()) return;
        exigirConfiguracao();
        Map<String, List<ReferenciaArmazenamento>> porBucket = referencias.stream()
                .collect(Collectors.groupingBy(ReferenciaArmazenamento::bucket));
        porBucket.forEach((nomeBucket, itens) -> {
            try {
                http.method(HttpMethod.DELETE)
                        .uri(URI.create(urlBase + "/storage/v1/object/" + segmento(nomeBucket)))
                        .headers(autenticacao()).contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("prefixes", itens.stream().map(ReferenciaArmazenamento::caminho).toList()))
                        .retrieve().toBodilessEntity();
            } catch (RestClientException ex) {
                log.error("Falha ao excluir {} do Supabase Storage: {}", itens, ex.getMessage());
                throw new ArmazenamentoIndisponivelException("Nao foi possivel excluir os arquivos.", ex);
            }
        });
    }

    @Override
    public URI linkTemporario(ReferenciaArmazenamento referencia, Duration validade, String nomeDownload) {
        exigirConfiguracao();
        Map<?, ?> resposta;
        try {
            resposta = http.post().uri(objeto("object/sign", referencia)).headers(autenticacao())
                    .contentType(MediaType.APPLICATION_JSON).body(Map.of("expiresIn", validade.toSeconds()))
                    .retrieve().body(Map.class);
        } catch (RestClientException ex) {
            log.error("Falha ao assinar {} no Supabase Storage: {}", referencia, ex.getMessage());
            throw new ArmazenamentoIndisponivelException("Nao foi possivel abrir o arquivo. Tente novamente.", ex);
        }
        Object assinado = resposta == null ? null : resposta.get("signedURL");
        if (!(assinado instanceof String caminhoAssinado) || !caminhoAssinado.startsWith("/")) {
            throw new ArmazenamentoIndisponivelException("O armazenamento retornou um link invalido.");
        }
        String link = urlBase + "/storage/v1" + caminhoAssinado;
        if (nomeDownload != null && !nomeDownload.isBlank()) {
            link += (link.contains("?") ? "&" : "?") + "download="
                    + UriUtils.encodeQueryParam(nomeDownload, StandardCharsets.UTF_8);
        }
        return URI.create(link);
    }

    private URI objeto(String operacao, ReferenciaArmazenamento referencia) {
        StringBuilder caminho = new StringBuilder(urlBase).append("/storage/v1/").append(operacao)
                .append('/').append(segmento(referencia.bucket()));
        for (String parte : referencia.caminho().split("/")) caminho.append('/').append(segmento(parte));
        return URI.create(caminho.toString());
    }

    private static String segmento(String valor) { return UriUtils.encodePathSegment(valor, StandardCharsets.UTF_8); }

    /** Chaves legadas (JWT) vao tambem no Authorization; as novas (sb_secret_...) so no apikey. */
    private Consumer<HttpHeaders> autenticacao() {
        return headers -> {
            headers.set("apikey", chave);
            if (chave.startsWith("eyJ")) headers.setBearerAuth(chave);
        };
    }

    private void exigirConfiguracao() {
        if (urlBase.isEmpty() || chave.isEmpty()) {
            throw new ArmazenamentoIndisponivelException(
                    "Envio de arquivos nao configurado no servidor (SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY).");
        }
    }
}
