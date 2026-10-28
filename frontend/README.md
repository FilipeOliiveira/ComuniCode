# COMUNICODE — Frontend

Tela inicial em React e Vite, baseada na referência fornecida. Estilização com Tailwind CSS e ajustes responsivos para desktop, tablet e celular.

## Executar

```sh
npm install
npm run dev
```

No PowerShell com restrição de scripts, use `npm.cmd install` e `npm.cmd run dev`.

## Gerar versão de produção

```sh
npm run build
npm run preview
```

Os filtros de professor, disciplina e formato atuam em conjunto com a pesquisa. A pesquisa ignora diferenças de acentuação. A barra de disciplinas permite rolagem horizontal, e o seletor de disciplina usa o mesmo estado. “PDS” e seu nome completo selecionam a mesma disciplina. “Limpar Filtros” restaura também a pesquisa e o contador mostra os resultados reais.

Os quatro primeiros materiais possuem PDFs estáticos em `public/materials/`, com visualização em nova aba e download pelo navegador. O arquivo de rotulagem encontrado nos arquivos fornecidos chama-se `4_5_Sequence_Label.pdf`, sem o sufixo `(3)`. Geraldo Braz é o professor do material de componentes. Os professores desses exemplos possuem selo de verificado e etiqueta “Material de aula”. Os dois exemplos restantes apresentam os alunos João Victor de Oliveira Carneiro e Ana Luiza Oliveira Montelo, de Ciência da Computação — UFMA, 8º período; seus arquivos não foram fornecidos.

As datas dos seis exemplos são ilustrativas e identificadas nos cartões. Novos envios recebem a data real, exibida no fuso America/Fortaleza.

O modal “Enviar material” abre pelos dois botões da página. Aceita documentos (PDF, Word, slides, planilhas, TXT e CSV), vídeos (MP4, WebM, MOV e M4V), áudios, imagens e ZIP, com limite de 100 MB por arquivo. Valida campos e arquivos, permite arrastar e soltar e salva conteúdo e metadados no IndexedDB deste navegador. Os materiais aparecem em “Seus envios recentes”, participam dos filtros e podem ser baixados após recarregar a página. A visualização de vídeos e outros formatos depende do suporte do navegador.

Não há envio para um servidor nem compartilhamento com outros usuários ou dispositivos. Limpar os dados do site remove os envios locais. Novos envios não concedem automaticamente um selo de professor verificado. Não há alterações no backend. A navegação para futuras telas aguarda as próximas etapas.

## Validar

```sh
npm test
npm run build
```

Os testes verificam filtros combinados, pesquisa, professores e disciplinas, renderização React e entrega HTTP dos quatro PDFs, comparando o conteúdo servido com os arquivos estáticos.

As fontes, os ícones e os retratos da referência usam recursos externos e precisam de acesso à internet.
