package com.example.demo.repository;

import com.example.demo.model.Postagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostagemRepository extends JpaRepository<Postagem, UUID> {
    java.util.List<Postagem> findByConteudoIdOrderByDataCriacaoDesc(UUID idConteudo);
    java.util.List<Postagem> findByCriadorIdOrderByDataCriacaoDesc(UUID idAutor);
    java.util.List<Postagem> findDistinctByTagsIdOrderByDataCriacaoDesc(UUID idTag);
}
