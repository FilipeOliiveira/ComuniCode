package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;
import java.util.List;

@Data
@Entity
public class Disciplina {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String codigo;
    
    @Column(columnDefinition = "TEXT")
    private String descricao;
    private Integer cargaHoraria;
    private Integer periodo;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL)
    private List<Conteudo> conteudos;
}