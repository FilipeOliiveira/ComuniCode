package com.example.demo.dto;

import com.example.demo.model.Professor;
import java.math.BigDecimal;

public record CadastroProfessorRequest(String nome, String email, String senha, String registro, String titulacao) {
    public Professor toEntity() {
        Professor professor = new Professor();
        professor.setNome(nome);
        professor.setEmail(email);
        professor.setRegistro(registro);
        professor.setTitulacao(titulacao);
        professor.setPesoAvaliacao(BigDecimal.ONE);
        return professor;
    }
}
