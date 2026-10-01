package com.example.demo.dto;

import com.example.demo.model.Disciplina;
import java.util.UUID;

public record DisciplinaResponse(UUID id, String nome, String codigo, String descricao, Integer cargaHoraria, Integer periodo) {
    public static DisciplinaResponse de(Disciplina disciplina) {
        return new DisciplinaResponse(disciplina.getId(), disciplina.getNome(), disciplina.getCodigo(),
                disciplina.getDescricao(), disciplina.getCargaHoraria(), disciplina.getPeriodo());
    }
}
