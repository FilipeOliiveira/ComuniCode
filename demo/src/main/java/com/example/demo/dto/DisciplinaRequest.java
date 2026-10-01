package com.example.demo.dto;

import com.example.demo.model.Disciplina;

public record DisciplinaRequest(String nome, String codigo, String descricao, Integer cargaHoraria, Integer periodo) {
    public Disciplina toEntity() {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome(nome);
        disciplina.setCodigo(codigo);
        disciplina.setDescricao(descricao);
        disciplina.setCargaHoraria(cargaHoraria);
        disciplina.setPeriodo(periodo);
        return disciplina;
    }
}
