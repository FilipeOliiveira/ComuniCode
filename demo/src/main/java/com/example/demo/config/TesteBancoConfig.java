package com.example.demo.config;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Teste manual do schema em database/criar_banco_portal.sql.
 * Ative o perfil teste-banco: ele inclui automaticamente o perfil supabase.
 * Cada execucao grava cinco linhas novas na mesma transacao.
 * Usuario ficticio inativo; este teste nao implementa login HTTP.
 * Consulte README_SUPABASE.md para configuracao e comandos.
 */
@Configuration(proxyBeanMethods = false)
@Profile("teste-banco")
public class TesteBancoConfig {

    private static final Logger log = LoggerFactory.getLogger(TesteBancoConfig.class);
    private static final String STATUS_POSTAGEM = "RASCUNHO";

    @Bean
    public ApplicationRunner testarConexaoEInsercao(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager) {

        return args -> {
            // Nao imprime URL JDBC, senha ou hash no console.
            String banco = jdbc.queryForObject("SELECT current_database()", String.class);
            log.info("Conexao PostgreSQL estabelecida. Banco: {}", banco);

            UUID usuarioId = UUID.randomUUID();
            UUID disciplinaId = UUID.randomUUID();
            UUID conteudoId = UUID.randomUUID();
            UUID postagemId = UUID.randomUUID();
            String identificador = usuarioId.toString();
            String email = "teste.banco." + identificador + "@example.invalid";
            String senhaHash = new BCryptPasswordEncoder()
                    .encode(UUID.randomUUID().toString());

            TransactionTemplate transacao = new TransactionTemplate(transactionManager);
            transacao.executeWithoutResult(status -> {
                jdbc.update(
                    "INSERT INTO public.usuario (id, nome, email, senha_hash, ativo) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    usuarioId, "Aluno de teste JDBC", email, senhaHash, false);

                // JOINED: aluno usa exatamente o mesmo UUID de usuario.
                jdbc.update(
                    "INSERT INTO public.aluno (id, matricula, semestre_atual) "
                    + "VALUES (?, ?, ?)",
                    usuarioId, "TESTE-" + identificador, 1);

                jdbc.update(
                    "INSERT INTO public.disciplina "
                    + "(id, nome, codigo, descricao, carga_horaria, periodo) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    disciplinaId, "Banco de Dados - teste JDBC",
                    "BD-TESTE-" + identificador,
                    "Disciplina ficticia para conferir a conexao com o Supabase.", 60, 1);

                jdbc.update(
                    "INSERT INTO public.conteudo "
                    + "(id, disciplina_id, titulo, descricao, ordem) VALUES (?, ?, ?, ?, ?)",
                    conteudoId, disciplinaId, "Introducao ao SQL",
                    "Conteudo ficticio criado pelo perfil teste-banco.", 1);

                jdbc.update(
                    "INSERT INTO public.postagem "
                    + "(id, conteudo_id, usuario_id, titulo, descricao, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    postagemId, conteudoId, usuarioId,
                    "Primeira postagem de teste",
                    "INSERT realizado pelo Spring Boot usando JdbcTemplate.",
                    STATUS_POSTAGEM);
            });

            // Executado depois do commit: confirma leitura e relacionamentos salvos.
            Map<String, Object> registro = jdbc.queryForMap(
                "SELECT p.id AS postagem_id, u.nome AS aluno, a.matricula, "
                + "d.nome AS disciplina, c.titulo AS conteudo, "
                + "p.titulo AS postagem, p.status "
                + "FROM public.postagem p "
                + "JOIN public.usuario u ON u.id = p.usuario_id "
                + "JOIN public.aluno a ON a.id = u.id "
                + "JOIN public.conteudo c ON c.id = p.conteudo_id "
                + "JOIN public.disciplina d ON d.id = c.disciplina_id "
                + "WHERE p.id = ?", postagemId);

            log.info("TESTE CONCLUIDO: 5 registros inseridos e leitura confirmada apos commit.");
            log.info("Dados recuperados: {}", registro);
            log.info("IDs: usuario/aluno={}, disciplina={}, conteudo={}, postagem={}",
                    usuarioId, disciplinaId, conteudoId, postagemId);
            log.info("Desative o perfil teste-banco para voltar a iniciar normalmente.");
        };
    }
}
