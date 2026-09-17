package com.example.demo;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OlaController {

    private final JdbcTemplate jdbcTemplate;

    public OlaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public ResponseEntity<?> inicio() {
        List<Map<String, Object>> usuarios = jdbcTemplate.queryForList("""
            SELECT u.id, u.nome, u.email, u.ativo,
                   u.data_cadastro, a.matricula, a.semestre_atual
            FROM public.usuario u
            JOIN public.aluno a ON a.id = u.id
            WHERE u.email LIKE ?
            ORDER BY u.data_cadastro DESC, u.id DESC
            LIMIT 1
            """, "teste.banco.%@example.invalid");

        if (usuarios.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "mensagem",
                "Nenhum aluno de teste encontrado. Execute o perfil teste-banco para cadastrar os dados."
            ));
        }

        return ResponseEntity.ok(usuarios.get(0));
    }
}