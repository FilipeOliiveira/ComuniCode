package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
public class Anexo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;
    private String url;

    @Enumerated(EnumType.STRING)
    private TipoAnexo tipo;

    private Long tamanho;
    private LocalDateTime dataUpload;

    @ManyToOne
    @JoinColumn(name = "postagem_id")
    private Postagem postagem;
}