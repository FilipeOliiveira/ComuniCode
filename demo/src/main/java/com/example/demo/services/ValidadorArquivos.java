package com.example.demo.services;

import com.example.demo.model.TipoAnexo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Confere quantidade, tamanho e tipo dos arquivos enviados. O tipo e decidido pelos primeiros
 * bytes do arquivo (assinatura), e precisa combinar com a extensao; o Content-Type do navegador e ignorado.
 */
@Component
public class ValidadorArquivos {
    public static final int MAX_ARQUIVOS = 5;
    public static final long MAX_BYTES = 25L * 1024 * 1024;
    private static final int BYTES_CABECALHO = 8192;

    public record ArquivoValidado(MultipartFile arquivo, String nome, String extensao, TipoAnexo tipo,
                                  String contentType) {}

    private enum Assinatura { PDF, PNG, JPEG, WEBP, MP4, ZIP, OLE, TEXTO }

    private record Formato(Assinatura assinatura, TipoAnexo tipo, String contentType) {}

    private static final java.util.Map<String, Formato> FORMATOS = java.util.Map.ofEntries(
            java.util.Map.entry("pdf", new Formato(Assinatura.PDF, TipoAnexo.PDF, "application/pdf")),
            java.util.Map.entry("png", new Formato(Assinatura.PNG, TipoAnexo.IMAGEM, "image/png")),
            java.util.Map.entry("jpg", new Formato(Assinatura.JPEG, TipoAnexo.IMAGEM, "image/jpeg")),
            java.util.Map.entry("jpeg", new Formato(Assinatura.JPEG, TipoAnexo.IMAGEM, "image/jpeg")),
            java.util.Map.entry("webp", new Formato(Assinatura.WEBP, TipoAnexo.IMAGEM, "image/webp")),
            java.util.Map.entry("mp4", new Formato(Assinatura.MP4, TipoAnexo.VIDEO, "video/mp4")),
            java.util.Map.entry("docx", new Formato(Assinatura.ZIP, TipoAnexo.DOCUMENTO,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            java.util.Map.entry("pptx", new Formato(Assinatura.ZIP, TipoAnexo.DOCUMENTO,
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            java.util.Map.entry("odt", new Formato(Assinatura.ZIP, TipoAnexo.DOCUMENTO,
                    "application/vnd.oasis.opendocument.text")),
            java.util.Map.entry("doc", new Formato(Assinatura.OLE, TipoAnexo.DOCUMENTO, "application/msword")),
            java.util.Map.entry("ppt", new Formato(Assinatura.OLE, TipoAnexo.DOCUMENTO, "application/vnd.ms-powerpoint")),
            java.util.Map.entry("txt", new Formato(Assinatura.TEXTO, TipoAnexo.DOCUMENTO, "text/plain; charset=utf-8")));

    public static final List<String> EXTENSOES = FORMATOS.keySet().stream().sorted().toList();

    public List<ArquivoValidado> validar(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.isEmpty()) {
            throw new IllegalArgumentException("Envie pelo menos um arquivo.");
        }
        if (arquivos.size() > MAX_ARQUIVOS) {
            throw new IllegalArgumentException("Envie no maximo " + MAX_ARQUIVOS + " arquivos por postagem.");
        }
        List<ArquivoValidado> validados = new ArrayList<>();
        for (MultipartFile arquivo : arquivos) validados.add(validar(arquivo));
        return validados;
    }

    private ArquivoValidado validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Arquivo vazio nao pode ser enviado.");
        String nome = nomeSeguro(arquivo.getOriginalFilename());
        if (arquivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("O arquivo " + nome + " passa do limite de 25 MB.");
        }
        int ponto = nome.lastIndexOf('.');
        String extensao = ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
        Formato formato = FORMATOS.get(extensao);
        if (formato == null) {
            throw new IllegalArgumentException("Formato nao permitido: " + nome + ". Use " + String.join(", ", EXTENSOES) + ".");
        }
        if (!confere(formato.assinatura(), cabecalho(arquivo))) {
            throw new IllegalArgumentException("O conteudo de " + nome + " nao corresponde a extensao ." + extensao + ".");
        }
        return new ArquivoValidado(arquivo, nome, extensao, formato.tipo(), formato.contentType());
    }

    /** Remove caminhos (C:\..., ../) e caracteres de controle do nome original. */
    static String nomeSeguro(String original) {
        String nome = original == null ? "" : original;
        nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
        nome = nome.replaceAll("\\p{Cntrl}", "").trim();
        if (nome.isEmpty() || nome.equals(".") || nome.equals("..")) {
            throw new IllegalArgumentException("Todo arquivo precisa ter um nome.");
        }
        if (nome.length() > 255) throw new IllegalArgumentException("O nome do arquivo deve ter ate 255 caracteres.");
        return nome;
    }

    private static byte[] cabecalho(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return entrada.readNBytes(BYTES_CABECALHO);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Nao foi possivel ler o arquivo enviado.");
        }
    }

    private static boolean confere(Assinatura assinatura, byte[] b) {
        return switch (assinatura) {
            case PDF -> comeca(b, 0, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case PNG -> comeca(b, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
            case JPEG -> comeca(b, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case WEBP -> comeca(b, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && comeca(b, 8, "WEBP".getBytes(StandardCharsets.US_ASCII));
            case MP4 -> comeca(b, 4, "ftyp".getBytes(StandardCharsets.US_ASCII));
            case ZIP -> comeca(b, 0, new byte[]{'P', 'K', 0x03, 0x04});
            case OLE -> comeca(b, 0, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
            case TEXTO -> textoUtf8(b);
        };
    }

    private static boolean comeca(byte[] dados, int deslocamento, byte[] esperado) {
        return dados.length >= deslocamento + esperado.length
                && Arrays.equals(dados, deslocamento, deslocamento + esperado.length, esperado, 0, esperado.length);
    }

    /** Sem byte nulo e decodificavel como UTF-8 (tolerando um caractere cortado no fim do trecho lido). */
    private static boolean textoUtf8(byte[] b) {
        for (byte valor : b) if (valor == 0) return false;
        var decodificador = StandardCharsets.UTF_8.newDecoder();
        var entrada = java.nio.ByteBuffer.wrap(b);
        var saida = java.nio.CharBuffer.allocate(b.length);
        var resultado = decodificador.decode(entrada, saida, false);
        return !resultado.isError() && entrada.remaining() <= 3;
    }
}
