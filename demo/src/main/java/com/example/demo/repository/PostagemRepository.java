package com.example.demo.repository;

import com.example.demo.model.Postagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PostagemRepository extends JpaRepository<Postagem, UUID> {
    // O Spring cria comandos SQL automaticamente. 
    // Ex: List<Postagem> findByVerificadaTrue();
}