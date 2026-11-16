package com.example.demo.services;

import com.example.demo.model.TipoAnexo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorArquivosTest {
    private final ValidadorArquivos validador = new ValidadorArquivos();

    static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivos", nome, "application/octet-stream", conteudo);
    }

    static byte[] pdf() { return "%PDF-1.7\n%conteudo".getBytes(StandardCharsets.US_ASCII); }

    @Test
    void deveAceitarFormatosPermitidosPeloConteudo() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
        byte[] mp4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        byte[] docx = {'P', 'K', 3, 4, 20, 0};
        var validados = validador.validar(List.of(arquivo("aula.pdf", pdf()), arquivo("Foto.PNG", png),
                arquivo("video.mp4", mp4), arquivo("lista.docx", docx),
                arquivo("notas.txt", "Ola, programação!".getBytes(StandardCharsets.UTF_8))));
        assertEquals(List.of(TipoAnexo.PDF, TipoAnexo.IMAGEM, TipoAnexo.VIDEO, TipoAnexo.DOCUMENTO, TipoAnexo.DOCUMENTO),
                validados.stream().map(ValidadorArquivos.ArquivoValidado::tipo).toList());
        assertEquals("application/pdf", validados.get(0).contentType());
        assertEquals("png", validados.get(1).extensao());
    }

    @Test
    void deveRecusarConteudoQueNaoCombinaComAExtensao() {
        var falso = arquivo("virus.pdf", "MZ executavel".getBytes(StandardCharsets.US_ASCII));
        var ex = assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of(falso)));
        assertTrue(ex.getMessage().contains("nao corresponde"));
        var binario = arquivo("dados.txt", new byte[]{'a', 0, 'b'});
        assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of(binario)));
    }

    @Test
    void deveRecusarExtensaoNaoPermitida() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> validador.validar(List.of(arquivo("script.exe", pdf()))));
        assertTrue(ex.getMessage().startsWith("Formato nao permitido"));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of(arquivo("semextensao", pdf()))));
    }

    @Test
    void deveExigirEntreUmECincoArquivosNaoVazios() {
        assertThrows(IllegalArgumentException.class, () -> validador.validar(null));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of()));
        List<MultipartFile> seis = Collections.nCopies(6, arquivo("a.pdf", pdf()));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(seis));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of(arquivo("vazio.pdf", new byte[0]))));
    }

    @Test
    void deveRecusarArquivoAcimaDe25Mb() {
        MultipartFile grande = new MockMultipartFile("arquivos", "grande.pdf", "application/pdf", pdf()) {
            @Override public long getSize() { return ValidadorArquivos.MAX_BYTES + 1; }
        };
        var ex = assertThrows(IllegalArgumentException.class, () -> validador.validar(List.of(grande)));
        assertTrue(ex.getMessage().contains("25 MB"));
    }

    @Test
    void deveLimparCaminhoDoNomeOriginal() {
        assertEquals("aula.pdf", ValidadorArquivos.nomeSeguro("C:\\Users\\aluno\\aula.pdf"));
        assertEquals("aula.pdf", ValidadorArquivos.nomeSeguro("../../aula.pdf"));
        assertThrows(IllegalArgumentException.class, () -> ValidadorArquivos.nomeSeguro("pasta/.."));
        assertThrows(IllegalArgumentException.class, () -> ValidadorArquivos.nomeSeguro("  "));
    }
}
