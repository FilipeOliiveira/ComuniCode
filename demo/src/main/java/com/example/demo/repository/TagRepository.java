package com.example.demo.repository;

import com.example.demo.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    @Query(
            "select t from Tag t where lower(trim(t.nome)) = lower(trim(:nome))")
    Optional<Tag> findByNomeNormalizado(@Param("nome") String nome);

    @Query("select t from Tag t order by lower(trim(t.nome)), t.id")
    List<Tag> listarOrdenadasPorNome();
}
