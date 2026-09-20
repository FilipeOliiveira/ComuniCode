package com.example.demo.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Aluno extends Usuario {
    private String matricula;
    private Integer semestreAtual;
}