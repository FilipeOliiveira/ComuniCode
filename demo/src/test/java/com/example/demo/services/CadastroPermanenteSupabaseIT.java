package com.example.demo.services;

import com.example.demo.model.Aluno;
import com.example.demo.model.Professor;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Carga manual PERMANENTE. Executar com -Dtest=CadastroPermanenteSupabaseIT. */
@SpringBootTest
@ActiveProfiles({"supabase", "integracao-supabase"})
class CadastroPermanenteSupabaseIT {
    private static final String EMAIL_JOAO = "joao.teste@example.invalid";
    private static final String EMAIL_GERALDO = "geraldo.teste@example.invalid";

    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    // Sem @Transactional de teste: a transacao abaixo faz COMMIT de verdade.
    @Test
    void deveGuardarJoaoEGeraldoPermanentemente() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Usuario joao = usuarioRepository.findByEmailIgnoreCase(EMAIL_JOAO).orElseGet(() -> {
                Aluno aluno = new Aluno();
                aluno.setNome("João");
                aluno.setEmail(EMAIL_JOAO);
                aluno.setMatricula("TESTE-JOAO-001");
                aluno.setSemestreAtual(1);
                return usuarioService.cadastrarAluno(aluno, UUID.randomUUID().toString());
            });
            assertInstanceOf(Aluno.class, joao);
            assertEquals("João", joao.getNome());
            assertEquals("TESTE-JOAO-001", ((Aluno) joao).getMatricula());

            Usuario geraldo = usuarioRepository.findByEmailIgnoreCase(EMAIL_GERALDO).orElseGet(() -> {
                Professor professor = new Professor();
                professor.setNome("Geraldo");
                professor.setEmail(EMAIL_GERALDO);
                professor.setRegistro("TESTE-GERALDO-001");
                professor.setPesoAvaliacao(BigDecimal.ONE);
                return usuarioService.cadastrarProfessor(professor, UUID.randomUUID().toString());
            });
            assertInstanceOf(Professor.class, geraldo);
            assertEquals("Geraldo", geraldo.getNome());
            assertEquals("TESTE-GERALDO-001", ((Professor) geraldo).getRegistro());
        });

        // Consultas fora da transacao confirmam a leitura APOS o commit.
        assertEquals("João", jdbc.queryForObject(
                "select u.nome from usuario u join aluno a on a.id = u.id where u.email = ?",
                String.class, EMAIL_JOAO));
        assertEquals("Geraldo", jdbc.queryForObject(
                "select u.nome from usuario u join professor p on p.id = u.id where u.email = ?",
                String.class, EMAIL_GERALDO));
        System.out.println("[CADASTRO PERMANENTE] Commit confirmado: João (" + EMAIL_JOAO
                + ") e Geraldo (" + EMAIL_GERALDO + "). Os registros permanecem no Supabase.");
    }
}
