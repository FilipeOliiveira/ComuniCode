package com.example.demo.services;

import com.example.demo.dto.PostagemResponse;
import com.example.demo.dto.PublicacaoRequest;
import com.example.demo.model.Anexo;
import com.example.demo.model.Postagem;
import com.example.demo.model.StatusPostagem;
import com.example.demo.storage.ArmazenamentoArquivos;
import com.example.demo.storage.ReferenciaArmazenamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Publicacao pela interface: valida, envia os arquivos ao Storage e so entao grava a postagem.
 * Se a gravacao no banco falhar, os arquivos ja enviados sao apagados (nada fica pela metade).
 * Sem @Transactional na classe: o commit acontece antes do retorno, dentro do TransactionTemplate.
 */
@Service
public class PublicacaoService {
    private static final Logger log = LoggerFactory.getLogger(PublicacaoService.class);

    private final PostagemService postagemService;
    private final ValidadorArquivos validador;
    private final ArmazenamentoArquivos armazenamento;
    private final TransactionTemplate transacao;

    public PublicacaoService(PostagemService postagemService, ValidadorArquivos validador,
                             ArmazenamentoArquivos armazenamento, TransactionTemplate transacao) {
        this.postagemService = postagemService;
        this.validador = validador;
        this.armazenamento = armazenamento;
        this.transacao = transacao;
    }

    public PostagemResponse publicar(PublicacaoRequest dados, List<MultipartFile> arquivos, UUID autorId) {
        if (dados == null) throw new IllegalArgumentException("Informe os dados da postagem.");
        if (dados.titulo() == null || dados.titulo().isBlank() || dados.titulo().trim().length() > 255) {
            throw new IllegalArgumentException("O titulo deve conter entre 1 e 255 caracteres.");
        }
        if (dados.conteudoId() == null) throw new IllegalArgumentException("Escolha o conteudo da disciplina.");
        if (dados.tagsIds() == null || dados.tagsIds().isEmpty()) {
            throw new IllegalArgumentException("A postagem deve conter pelo menos uma tag.");
        }
        List<ValidadorArquivos.ArquivoValidado> validados = validador.validar(arquivos);

        List<ReferenciaArmazenamento> enviados = new ArrayList<>();
        try {
            List<Anexo> anexos = new ArrayList<>();
            for (ValidadorArquivos.ArquivoValidado arquivo : validados) {
                String caminho = "postagens/" + UUID.randomUUID() + "." + arquivo.extensao();
                ReferenciaArmazenamento referencia = armazenamento.enviar(caminho, bytes(arquivo.arquivo()), arquivo.contentType());
                enviados.add(referencia);
                anexos.add(anexo(arquivo, referencia));
            }
            return transacao.execute(status -> {
                Postagem postagem = new Postagem();
                postagem.setTitulo(dados.titulo());
                postagem.setDescricao(dados.descricao());
                postagem.setStatus(StatusPostagem.PUBLICADA);
                postagem.setAnexos(anexos);
                return PostagemResponse.de(postagemService.criarPostagem(postagem, autorId, dados.conteudoId(), dados.tagsIds()));
            });
        } catch (RuntimeException ex) {
            descartar(enviados);
            throw ex;
        }
    }

    private static Anexo anexo(ValidadorArquivos.ArquivoValidado arquivo, ReferenciaArmazenamento referencia) {
        Anexo anexo = new Anexo();
        anexo.setNome(arquivo.nome());
        anexo.setUrl(referencia.toString());
        anexo.setTipo(arquivo.tipo());
        anexo.setTamanho(arquivo.arquivo().getSize());
        anexo.setDataUpload(LocalDateTime.now());
        return anexo;
    }

    private static byte[] bytes(MultipartFile arquivo) {
        try { return arquivo.getBytes(); }
        catch (IOException ex) { throw new IllegalArgumentException("Nao foi possivel ler o arquivo enviado."); }
    }

    private void descartar(List<ReferenciaArmazenamento> enviados) {
        if (enviados.isEmpty()) return;
        try { armazenamento.excluir(enviados); }
        catch (RuntimeException ex) { log.error("Arquivos orfaos no Storage apos falha na publicacao: {}", enviados, ex); }
    }
}
