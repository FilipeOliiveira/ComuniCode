package com.example.demo.service;

import com.example.demo.model.Postagem;
import com.example.demo.repository.PostagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PostagemService {

    @Autowired
    private PostagemRepository repository;

    public Postagem criarPostagem(Postagem postagem) {
        postagem.setDataCriacao(LocalDateTime.now());
        // Lógica de cálculo de ranking descrita no diagrama entraria aqui[cite: 1]
        return repository.save(postagem);
    }

    public List<Postagem> listarTodas() {
        return repository.findAll();
    }
}