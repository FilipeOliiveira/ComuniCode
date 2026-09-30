package com.example.demo.repository;

import com.example.demo.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DisciplinaRepository extends JpaRepository<Disciplina, UUID> {
    
    // Verifica se já existe uma disciplina com esse código (ignorando maiúsculas/minúsculas)
    boolean existsByCodigoIgnoreCase(String codigo);
    
    // Verifica se já existe uma disciplina com esse nome
    boolean existsByNomeIgnoreCase(String nome);
}