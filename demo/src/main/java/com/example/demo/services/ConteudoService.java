package com.example.demo.services;

import com.example.demo.exception.*;

import com.example.demo.model.Conteudo;
import com.example.demo.model.Disciplina;
import com.example.demo.repository.ConteudoRepository;
import com.example.demo.repository.DisciplinaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConteudoService {
    private final ConteudoRepository conteudoRepository;
    private final DisciplinaRepository disciplinaRepository;

    public ConteudoService(ConteudoRepository conteudoRepository, DisciplinaRepository disciplinaRepository) {
        this.conteudoRepository = conteudoRepository;
        this.disciplinaRepository = disciplinaRepository;
    }

    @Transactional
    public Conteudo criarConteudo(Conteudo conteudo, UUID idDisciplina) {
        if (conteudo == null) {
            throw new IllegalArgumentException("O conteudo e obrigatorio.");
        }
        if (conteudo.getId() != null) {
            throw new IllegalArgumentException("Um novo conteudo nao deve possuir ID.");
        }
        if (conteudo.getTitulo() == null || conteudo.getTitulo().isBlank()) {
            throw new IllegalArgumentException("O titulo do conteudo e obrigatorio.");
        }
        if (conteudo.getTitulo().trim().length() > 255) {
            throw new IllegalArgumentException("O titulo deve ter no maximo 255 caracteres.");
        }
        if (conteudo.getOrdem() != null && conteudo.getOrdem() < 0) {
            throw new IllegalArgumentException("A ordem deve ser zero ou maior.");
        }
        Disciplina disciplina = buscarDisciplina(idDisciplina);
        conteudo.setTitulo(conteudo.getTitulo().trim());
        // O JPA envia NULL explicitamente, portanto o default SQL nao seria aplicado.
        if (conteudo.getOrdem() == null) {
            conteudo.setOrdem(0);
        }
        conteudo.setDisciplina(disciplina);
        return conteudoRepository.save(conteudo);
    }

    @Transactional(readOnly = true)
    public Conteudo buscarPorId(UUID idConteudo) {
        if (idConteudo == null) {
            throw new IllegalArgumentException("O ID do conteudo e obrigatorio.");
        }
        return conteudoRepository.findById(idConteudo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo nao encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Conteudo> listarPorDisciplina(UUID idDisciplina) {
        buscarDisciplina(idDisciplina);
        return conteudoRepository.findByDisciplinaIdOrderByOrdemAscTituloAsc(idDisciplina);
    }

    private Disciplina buscarDisciplina(UUID idDisciplina) {
        if (idDisciplina == null) {
            throw new IllegalArgumentException("O ID da disciplina e obrigatorio.");
        }
        return disciplinaRepository.findById(idDisciplina)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina nao encontrada."));
    }
}
