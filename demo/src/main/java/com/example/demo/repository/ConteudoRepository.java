package com.example.demo.repository;

import com.example.demo.model.Conteudo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConteudoRepository extends JpaRepository<Conteudo, UUID> {
    java.util.List<Conteudo> findByDisciplinaIdOrderByOrdemAscTituloAsc(UUID idDisciplina);
}
