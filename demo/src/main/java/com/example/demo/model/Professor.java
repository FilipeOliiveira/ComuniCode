package com.example.demo.model;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Professor extends Usuario {
    private String registro;
    private String titulacao;
    private java.math.BigDecimal pesoAvaliacao;
}
