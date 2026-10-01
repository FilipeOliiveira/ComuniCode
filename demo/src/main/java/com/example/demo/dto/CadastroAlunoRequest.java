package com.example.demo.dto;

import com.example.demo.model.Aluno;

public record CadastroAlunoRequest(String nome, String email, String senha, String matricula, Integer semestreAtual) {
    public Aluno toEntity() {
        Aluno aluno = new Aluno();
        aluno.setNome(nome);
        aluno.setEmail(email);
        aluno.setMatricula(matricula);
        aluno.setSemestreAtual(semestreAtual);
        return aluno;
    }
}
