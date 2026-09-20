package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Data
@Entity
public class Postagem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    private StatusPostagem status;
    private BigDecimal pontuacao;
    private Boolean verificada;
    private BigDecimal scoreRelevancia;

    @ManyToOne
    @JoinColumn(name = "conteudo_id")
    private Conteudo conteudo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario criador; // Representa a relação "cria"[cite: 1]

    @OneToMany(mappedBy = "postagem", cascade = CascadeType.ALL)
    private List<Anexo> anexos;

    @ManyToMany
    @JoinTable(
        name = "postagem_tag",
        joinColumns = @JoinColumn(name = "postagem_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags;
}
