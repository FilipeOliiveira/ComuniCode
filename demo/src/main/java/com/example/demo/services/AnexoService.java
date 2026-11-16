package com.example.demo.services;

import com.example.demo.exception.RecursoNaoEncontradoException;
import com.example.demo.model.Anexo;
import com.example.demo.model.TipoAnexo;
import com.example.demo.repository.AnexoRepository;
import com.example.demo.storage.ArmazenamentoArquivos;
import com.example.demo.storage.ReferenciaArmazenamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/** Acesso aos arquivos hospedados pelo portal, sempre por link temporario. */
@Service
public class AnexoService {
    public static final Duration VALIDADE_LINK = Duration.ofSeconds(60);

    private final AnexoRepository anexos;
    private final ArmazenamentoArquivos armazenamento;

    public AnexoService(AnexoRepository anexos, ArmazenamentoArquivos armazenamento) {
        this.anexos = anexos;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public URI linkTemporario(UUID idAnexo) {
        if (idAnexo == null) throw new IllegalArgumentException("O ID e obrigatorio.");
        Anexo anexo = anexos.findById(idAnexo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo nao encontrado."));
        if (!ReferenciaArmazenamento.ehInterna(anexo.getUrl())) {
            throw new RecursoNaoEncontradoException("Este anexo nao esta hospedado no portal.");
        }
        // PDF, imagem e video abrem no navegador; documentos sao baixados com o nome original.
        String nomeDownload = anexo.getTipo() == TipoAnexo.DOCUMENTO ? anexo.getNome() : null;
        return armazenamento.linkTemporario(ReferenciaArmazenamento.de(anexo.getUrl()), VALIDADE_LINK, nomeDownload);
    }
}
