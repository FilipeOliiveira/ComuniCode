package com.example.demo.services;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Tag criarTag(String nome) {
        String nomeValidado = validarNome(nome);
        if (tagRepository.findByNomeNormalizado(nomeValidado).isPresent()) {
            throw new IllegalArgumentException("Ja existe uma tag cadastrada com este nome.");
        }
        Tag tag = new Tag();
        tag.setNome(nomeValidado);
        return tagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public List<Tag> listarTodas() {
        return tagRepository.listarOrdenadasPorNome();
    }

    @Transactional(readOnly = true)
    public Tag buscarPorNome(String nome) {
        return tagRepository.findByNomeNormalizado(validarNome(nome))
                .orElseThrow(() -> new IllegalArgumentException("Tag nao encontrada."));
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da tag e obrigatorio.");
        }
        String normalizado = nome.trim();
        if (normalizado.length() > 255) {
            throw new IllegalArgumentException("O nome da tag deve ter no maximo 255 caracteres.");
        }
        return normalizado;
    }
}
