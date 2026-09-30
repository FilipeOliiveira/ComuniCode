package com.example.demo.services;

import com.example.demo.model.Administrador;
import com.example.demo.model.Disciplina;
import com.example.demo.model.Usuario;
import com.example.demo.repository.DisciplinaRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DisciplinaService {

    @Autowired
    private DisciplinaRepository disciplinaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Cria uma nova disciplina após validar regras de negócio e permissão de Admin.
     * 
     * @param novaDisciplina O objeto Disciplina a ser salvo
     * @param idAdmin O ID do usuário que está tentando criar a disciplina
     * @return A disciplina salva no banco de dados
     */
    public Disciplina criarDisciplina(Disciplina novaDisciplina, UUID idAdmin) {
        
        // 1. Validar se o usuário existe e se é um Administrador
        Usuario usuario = usuarioRepository.findById(idAdmin)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (!(usuario instanceof Administrador)) {
            throw new RuntimeException("Acesso negado: Apenas administradores podem criar disciplinas.");
        }

        // 2. Validar se as informações básicas foram preenchidas
        if (novaDisciplina.getNome() == null || novaDisciplina.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da disciplina é obrigatório.");
        }
        if (novaDisciplina.getCodigo() == null || novaDisciplina.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("O código da disciplina é obrigatório.");
        }
        if (novaDisciplina.getCargaHoraria() == null || novaDisciplina.getCargaHoraria() <= 0) {
            throw new IllegalArgumentException("A carga horária deve ser maior que zero.");
        }
        if (novaDisciplina.getPeriodo() == null || novaDisciplina.getPeriodo() <= 0) {
            throw new IllegalArgumentException("O período deve ser um valor válido (maior que zero).");
        }

        // 3. Validar se a disciplina já existe (Regra de unicidade)
        if (disciplinaRepository.existsByCodigoIgnoreCase(novaDisciplina.getCodigo().trim())) {
            throw new IllegalArgumentException("Já existe uma disciplina cadastrada com o código informado.");
        }
        if (disciplinaRepository.existsByNomeIgnoreCase(novaDisciplina.getNome().trim())) {
            throw new IllegalArgumentException("Já existe uma disciplina cadastrada com o nome informado.");
        }

        // 4. Salvar no banco de dados (o ID será gerado automaticamente pelo UUID)
        return disciplinaRepository.save(novaDisciplina);
    }
}