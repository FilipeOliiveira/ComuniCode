/*
PORTAL DE MATERIAIS ACADEMICOS - PostgreSQL / Supabase
Fontes: User Management Framework-2026-09-12-194317.pdf e
        User Management Framework-2026-09-16-173039.pdf.
As duas vistas foram conferidas: 13 classes e 1 associacao N:N = 14 tabelas.

EXECUCAO
1. No projeto Supabase vazio, abra SQL Editor > New query.
2. Cole este arquivo inteiro e execute uma unica vez como postgres.
3. Consulte as 14 tabelas no schema public.
A transacao evita criacao parcial. Nao ha DROP nem limpeza de dados.
Uma segunda execucao falhara propositalmente: alteracoes futuras devem ser
migracoes, em vez de ocultar divergencias com CREATE TABLE IF NOT EXISTS.

DECISOES DE IMPLEMENTACAO (nao sao regras explicitas dos PDFs)
- Usuario e subclasses: tabelas separadas, com a mesma PK UUID, para JPA JOINED.
  No Java, Usuario deve ser abstract e usar @Inheritance(strategy=JOINED).
  A aplicacao deve cadastrar usuario + UMA subclasse na mesma transacao.
  O SQL garante a existencia do pai, mas nao exclusividade/completude de subtipos.
  Definir se uma pessoa pode acumular perfis antes de implementar esse caso.
- E-mail unico ignorando caixa e espacos nas extremidades; matricula, registro,
  codigo de disciplina e nome de tag unicos (tag tambem ignora caixa/espacos).
  Essas unicidades sao propostas tecnicas; confirmar conforme as regras do curso.
- Campos essenciais NOT NULL; descricoes, foto e titulacao opcionais.
  Ordem e semestre/periodo admitem zero; carga horaria e pesos devem ser positivos.
- StatusPostagem, StatusComentario, StatusDenuncia, MotivoDenuncia e TipoAnexo:
  VARCHAR(255), sem lista de valores inventada e sem default. O backend deve
  enviar os nomes dos enums. Depois de defini-los, adicionar CHECKs por migracao.
  A nota aceita INTEGER sem escala presumida; validar a escala no backend e
  adicionar CHECK quando o dominio for definido.
- Nao foi imposta unicidade (usuario, postagem) em avaliacao: o diagrama nao
  determina se ha apenas um voto ou historico. Decidir antes de implementar votos.
  ProfessorDisciplina admite historico, mas apenas um vinculo ATIVO por par.
- UUID: gen_random_uuid(); Java UUID. Decimal: NUMERIC(19,6); Java BigDecimal.
  Datas/horas: TIMESTAMPTZ; Java OffsetDateTime/Instant. Date: DATE/LocalDate.
  Long: BIGINT/Long. Strings longas (URLs, descricoes): TEXT.
- CASCADE apenas em subclasses e composicoes (disciplina/conteudo/postagem e
  comentario/avaliacao/anexo) e na associacao postagem_tag. Denuncias preservam
  rastreabilidade: RESTRICT impede apagar postagens denunciadas. Usuarios autores
  ou professores com vinculos tambem nao sao apagados em cascata; usar ativo=false.
- Pontuacao, score_relevancia e verificada sao mantidos pelo backend. Nao ha
  formula de ranking nem regra de verificacao especificada nos PDFs.
  Peso em avaliacao e uma fotografia do peso aplicado no momento do voto.
- Metodos UML (criarPostagem, moderarPostagem, calcularRanking etc.) pertencem
  aos services Java; nao equivalem automaticamente a procedures SQL.

AUTENTICACAO E ACESSO
Este modelo segue senhaHash do diagrama: autenticacao no Spring Security,
Supabase usado como PostgreSQL. Nao integra public.usuario com auth.users.
Gravar somente hash de senha produzido por PasswordEncoder; nunca senha pura.
Se optar por Supabase Auth, revisar usuario e remover a duplicacao de credenciais.
RLS e habilitado e os grants de anon/authenticated sao removidos SOMENTE nestas
14 tabelas. Nao ha acesso direto do frontend pela Data API nesta arquitetura.
O JDBC do backend deve usar um papel autorizado: para configuracao inicial,
o postgres disponibilizado em Connect (proprietario/bypass RLS). Esse papel e
privilegiado; em producao, provisionar papel dedicado com grants/policies adequados.
RLS nao substitui autenticacao/autorizacao no Spring Security quando JDBC usa
postgres. Nunca colocar credenciais JDBC ou service_role no frontend.

SPRING BOOT - application.properties (exemplo; estas linhas sao comentarios)
 spring.datasource.url=${DB_URL}
 spring.datasource.username=${DB_USERNAME}
 spring.datasource.password=${DB_PASSWORD}
 spring.jpa.hibernate.ddl-auto=validate
 spring.jpa.properties.hibernate.default_schema=public
 spring.jpa.properties.hibernate.jdbc.time_zone=UTC
 spring.sql.init.mode=never
 spring.flyway.enabled=false
Copie host/porta/usuario de Connect > JDBC no Supabase. DB_URL deve conter
jdbc:postgresql://HOST:PORTA/postgres?sslmode=require, sem senha embutida.
Use conexao direta se houver IPv6, ou Session pooler para backend persistente
em rede IPv4. Usuario/host do pooler diferem dos da conexao direta.
Dependencias: Spring Data JPA e driver org.postgresql:postgresql (runtime),
gerenciadas pela versao do Spring Boot do seu projeto.

MAPEAMENTO JPA
 @Entity @Table(name="usuario", schema="public")
 @Inheritance(strategy=InheritanceType.JOINED)
 public abstract class Usuario { ... }
Nas subclasses, @Entity, @Table(name="aluno"/"professor"/"administrador") e
@PrimaryKeyJoinColumn(name="id"). Nao declarar outro @Id nas subclasses.
Gerar UUID no Java (por exemplo @GeneratedValue(strategy=GenerationType.UUID)
se a sua versao JPA suportar); default SQL cobre insercoes fora do Hibernate.
FKs obrigatorias: @ManyToOne(optional=false) e @JoinColumn(nullable=false).
Enums: @Enumerated(EnumType.STRING) e coluna VARCHAR; se Hibernate inferir
outro tipo, explicitar @JdbcTypeCode(SqlTypes.VARCHAR), conforme sua versao.
BigDecimal: @Column(precision=19, scale=6). TEXT: @Column(columnDefinition="text").
Datas de atualizacao sao alteradas pelo trigger; refresh/recarregar a entidade
para obter o valor depois de UPDATE. Defaults SQL nao se aplicam quando o JPA
envia NULL explicitamente: inicializar flags/datas no Java ou mapear como geradas.

FLYWAY (ALTERNATIVA A EXECUCAO MANUAL)
Se ainda nao executou o SQL, pode versiona-lo como
src/main/resources/db/migration/V1__criar_banco_portal.sql.
Remover BEGIN e COMMIT desse arquivo: deixar o Flyway gerir a transacao.
Habilitar Flyway e suas dependencias compativeis com a versao do Spring Boot.
Escolha apenas UMA forma de criacao inicial. Se ja executou manualmente, nao
aplique V1 novamente nem ative baseline automatico sem reconciliar o schema.

Referencias oficiais consultadas:
https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot
https://supabase.com/docs/guides/database/connecting-to-postgres
https://supabase.com/docs/guides/database/postgres/row-level-security
https://docs.spring.io/spring-boot/how-to/data-initialization.html
*/

BEGIN;

CREATE TABLE public.usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL CHECK (btrim(nome) <> ''),
    email VARCHAR(255) NOT NULL CHECK (btrim(email) <> ''),
    senha_hash VARCHAR(255) NOT NULL CHECK (btrim(senha_hash) <> ''),
    foto_perfil TEXT,
    data_cadastro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_usuario_email ON public.usuario (lower(btrim(email)));

CREATE TABLE public.aluno (
    id UUID PRIMARY KEY REFERENCES public.usuario(id) ON DELETE CASCADE,
    matricula VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(matricula) <> ''),
    semestre_atual INTEGER NOT NULL CHECK (semestre_atual >= 0)
);

CREATE TABLE public.administrador (
    id UUID PRIMARY KEY REFERENCES public.usuario(id) ON DELETE CASCADE,
    nivel_acesso VARCHAR(255) NOT NULL CHECK (btrim(nivel_acesso) <> '')
);

CREATE TABLE public.professor (
    id UUID PRIMARY KEY REFERENCES public.usuario(id) ON DELETE CASCADE,
    registro VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(registro) <> ''),
    titulacao VARCHAR(255),
    peso_avaliacao NUMERIC(19,6) NOT NULL CHECK (peso_avaliacao > 0 AND peso_avaliacao <> 'NaN'::numeric)
);

CREATE TABLE public.disciplina (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL CHECK (btrim(nome) <> ''),
    codigo VARCHAR(255) NOT NULL UNIQUE CHECK (btrim(codigo) <> ''),
    descricao TEXT,
    carga_horaria INTEGER NOT NULL CHECK (carga_horaria > 0),
    periodo INTEGER NOT NULL CHECK (periodo >= 0)
);

CREATE TABLE public.conteudo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    disciplina_id UUID NOT NULL REFERENCES public.disciplina(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL CHECK (btrim(titulo) <> ''),
    descricao TEXT,
    ordem INTEGER NOT NULL DEFAULT 0 CHECK (ordem >= 0)
);
CREATE INDEX idx_conteudo_disciplina_ordem ON public.conteudo (disciplina_id, ordem);

CREATE TABLE public.postagem (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conteudo_id UUID NOT NULL REFERENCES public.conteudo(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    titulo VARCHAR(255) NOT NULL CHECK (btrim(titulo) <> ''),
    descricao TEXT,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(255) NOT NULL CHECK (btrim(status) <> ''),
    pontuacao NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK (pontuacao <> 'NaN'::numeric),
    verificada BOOLEAN NOT NULL DEFAULT FALSE,
    score_relevancia NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK (score_relevancia <> 'NaN'::numeric)
);
CREATE INDEX idx_postagem_conteudo_data ON public.postagem (conteudo_id, data_criacao DESC);
CREATE INDEX idx_postagem_usuario ON public.postagem (usuario_id);
CREATE INDEX idx_postagem_ranking ON public.postagem (conteudo_id, status, score_relevancia DESC);

CREATE TABLE public.comentario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    postagem_id UUID NOT NULL REFERENCES public.postagem(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    texto TEXT NOT NULL CHECK (btrim(texto) <> ''),
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(255) NOT NULL CHECK (btrim(status) <> '')
);
CREATE INDEX idx_comentario_postagem_data ON public.comentario (postagem_id, data_criacao);
CREATE INDEX idx_comentario_usuario ON public.comentario (usuario_id);

CREATE TABLE public.avaliacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    postagem_id UUID NOT NULL REFERENCES public.postagem(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    nota INTEGER NOT NULL,
    peso NUMERIC(19,6) NOT NULL CHECK (peso > 0 AND peso <> 'NaN'::numeric),
    data_avaliacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_avaliacao_postagem ON public.avaliacao (postagem_id);
CREATE INDEX idx_avaliacao_usuario ON public.avaliacao (usuario_id);

CREATE TABLE public.anexo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    postagem_id UUID NOT NULL REFERENCES public.postagem(id) ON DELETE CASCADE,
    nome VARCHAR(255) NOT NULL CHECK (btrim(nome) <> ''),
    url TEXT NOT NULL CHECK (btrim(url) <> ''),
    tipo VARCHAR(255) NOT NULL CHECK (btrim(tipo) <> ''),
    tamanho BIGINT NOT NULL CHECK (tamanho >= 0),
    data_upload TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_anexo_postagem ON public.anexo (postagem_id);
COMMENT ON COLUMN public.anexo.tamanho IS 'Tamanho em bytes; convencao adotada nesta implementacao.';
COMMENT ON COLUMN public.anexo.url IS 'Localizador persistente do arquivo; nao armazenar URL assinada que expira.';
-- Excluir a linha de anexo NAO apaga objetos do Supabase Storage.
-- Upload, autorizacao de download e limpeza de objetos pertencem ao backend.

CREATE TABLE public.tag (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL CHECK (btrim(nome) <> '')
);
CREATE UNIQUE INDEX uq_tag_nome ON public.tag (lower(btrim(nome)));

CREATE TABLE public.postagem_tag (
    postagem_id UUID NOT NULL REFERENCES public.postagem(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES public.tag(id) ON DELETE CASCADE,
    PRIMARY KEY (postagem_id, tag_id)
);
CREATE INDEX idx_postagem_tag_tag ON public.postagem_tag (tag_id);

CREATE TABLE public.denuncia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    postagem_id UUID NOT NULL REFERENCES public.postagem(id) ON DELETE RESTRICT,
    usuario_id UUID NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    motivo VARCHAR(255) NOT NULL CHECK (btrim(motivo) <> ''),
    descricao TEXT,
    data_criacao TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(255) NOT NULL CHECK (btrim(status) <> '')
);
CREATE INDEX idx_denuncia_postagem ON public.denuncia (postagem_id);
CREATE INDEX idx_denuncia_usuario ON public.denuncia (usuario_id);
CREATE INDEX idx_denuncia_status_data ON public.denuncia (status, data_criacao);

CREATE TABLE public.professor_disciplina (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    professor_id UUID NOT NULL REFERENCES public.professor(id) ON DELETE RESTRICT,
    disciplina_id UUID NOT NULL REFERENCES public.disciplina(id) ON DELETE RESTRICT,
    data_vinculo DATE NOT NULL DEFAULT CURRENT_DATE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_professor_disciplina_professor ON public.professor_disciplina (professor_id);
CREATE INDEX idx_professor_disciplina_disciplina ON public.professor_disciplina (disciplina_id);
CREATE UNIQUE INDEX uq_professor_disciplina_ativo
    ON public.professor_disciplina (professor_id, disciplina_id) WHERE ativo;

CREATE FUNCTION public.portal_definir_data_atualizacao()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
BEGIN
    NEW.data_atualizacao := statement_timestamp();
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION public.portal_definir_data_atualizacao() FROM PUBLIC;

CREATE TRIGGER trg_postagem_data_atualizacao
BEFORE UPDATE ON public.postagem
FOR EACH ROW EXECUTE FUNCTION public.portal_definir_data_atualizacao();

CREATE TRIGGER trg_comentario_data_atualizacao
BEFORE UPDATE ON public.comentario
FOR EACH ROW EXECUTE FUNCTION public.portal_definir_data_atualizacao();

-- Protege somente os objetos desta aplicacao, sem alterar auth/storage nem
-- tabelas de outros sistemas. Sem politicas publicas permissivas.
DO $$
DECLARE
    tabela TEXT;
    papel TEXT;
BEGIN
    FOREACH tabela IN ARRAY ARRAY[
        'usuario', 'aluno', 'administrador', 'professor', 'disciplina',
        'conteudo', 'postagem', 'comentario', 'avaliacao', 'anexo', 'tag',
        'postagem_tag', 'denuncia', 'professor_disciplina'
    ] LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', tabela);
        EXECUTE format('REVOKE ALL ON TABLE public.%I FROM PUBLIC', tabela);
        FOREACH papel IN ARRAY ARRAY['anon', 'authenticated'] LOOP
            IF EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = papel) THEN
                EXECUTE format('REVOKE ALL ON TABLE public.%I FROM %I', tabela, papel);
            END IF;
        END LOOP;
    END LOOP;
END;
$$;

COMMIT;

-- Consulta opcional apos executar (retire os dois hifens de cada linha):
-- SELECT tablename, rowsecurity FROM pg_catalog.pg_tables
-- WHERE schemaname = 'public' ORDER BY tablename;
