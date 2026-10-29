export const professors = [
  { id: 'carlos', name: 'PROF. DR. Carlos de Salles' },
  { id: 'geraldo', name: 'PROF. DR. Geraldo Braz' },
  { id: 'mario', name: 'PROF. DR. Mário Meireles' },
  { id: 'anselmo', name: 'PROF. DR. Anselmo Paiva' },
  { id: 'luciano', name: 'PROF. DR. Luciano Coutinho' },
  { id: 'simara', name: 'PROF. DR. Simara Rocha' },
  { id: 'luis', name: 'PROF. DR. Luis Rivero' },
];

// PDS e seu nome completo compartilham o mesmo filtro.
export const disciplines = [
  { id: 'Todas', label: 'Todas' },
  { id: 'calculo', label: 'Cálculo II' },
  { id: 'dados-ii', label: 'Estruturas de Dados II' },
  { id: 'ia', label: 'Inteligência Artificial' },
  { id: 'algoritmos', label: 'Algoritmos II' },
  { id: 'dados-i', label: 'Estrutura de Dados I' },
  { id: 'pds', label: 'PDS' },
  { id: 'pln', label: 'Processamento de Linguagem Natural (PLN)' },
  { id: 'laboratorio', label: 'Laboratório de Programação' },
  { id: 'pds', label: 'Processo de Desenvolvimento de Software (PDS)' },
  { id: 'teste', label: 'Teste de Software' },
];

export const materials = [
  {
    id: 'rotulagem-sequencias', section: 'featured',
    title: 'Rotulagem de Sequências',
    discipline: 'Processamento de Linguagem Natural (PLN)', disciplineId: 'pln',
    professor: 'PROF. DR. Anselmo Paiva', professorId: 'anselmo', authorRole: 'professor', verified: true,
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Rotulagem de sequências, classificação gramatical (POS Tagging) e reconhecimento de entidades nomeadas.',
    format: 'PDF', filename: '4_5_Sequence_Label.pdf', bytes: 5275737,
  },
  {
    id: 'componentizando-interface', section: 'featured',
    title: 'Componentizando a Interface',
    discipline: 'Laboratório de Programação', disciplineId: 'laboratorio',
    professor: 'PROF. DR. Geraldo Braz', professorId: 'geraldo', authorRole: 'professor', verified: true,
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Criação de interfaces reutilizáveis em React, organização de componentes e desenvolvimento de páginas.',
    format: 'PDF', filename: 'b_Componentes.pdf', bytes: 666904,
  },
  {
    id: 'diagrama-sequencia', section: 'featured',
    title: 'Diagrama de Sequência',
    discipline: 'Processo de Desenvolvimento de Software (PDS)', disciplineId: 'pds',
    professor: 'PROF. DR. Simara Rocha', professorId: 'simara', authorRole: 'professor', verified: true,
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Diagramas UML, atores, linhas de vida, mensagens e interações entre objetos.',
    format: 'PDF', filename: 'pds_Aula11_remoto.pdf', bytes: 5053828,
  },
  {
    id: 'gerenciamento-testes', section: 'popular',
    title: 'Gerenciamento de Testes',
    discipline: 'Teste de Software', disciplineId: 'teste',
    professor: 'PROF. DR. Luis Rivero', professorId: 'luis', authorRole: 'professor', verified: true,
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Gerenciamento de defeitos, severidade, prioridades e acompanhamento de correções.',
    format: 'PDF', filename: 'Teste_Unidade_5_Gerenciamento.pdf', bytes: 800138,
  },
  {
    id: 'estruturas-avancadas', section: 'popular',
    title: 'Manual de Estruturas de Dados Avançadas em C++',
    discipline: 'Engenharia de Software', disciplineId: 'dados-ii',
    author: 'João Victor de Oliveira Carneiro', professorId: null, authorRole: 'student',
    course: 'Ciência da Computação', institution: 'UFMA', semester: '8º período',
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Árvores AVL, Red-Black, Grafos com Dijkstra e B-Trees com benchmarks de complexidade assintótica detalhados.',
    format: 'DOCX', filename: null,
  },
  {
    id: 'resumo-testes-software', section: 'popular',
    title: 'Resumo de Testes de Software: Testes Unitários e de Integração',
    discipline: 'Teste de Software', disciplineId: 'teste',
    author: 'Ana Luiza Oliveira Montelo', professorId: null, authorRole: 'student',
    course: 'Ciência da Computação', institution: 'UFMA', semester: '8º período',
    uploadedAt: '2026-10-28T12:00:00-03:00', demoDate: true,
    description: 'Conceitos de testes unitários e de integração, uso de mocks, cobertura de código e exemplos práticos com Jest em aplicações React.',
    format: 'PDF', filename: null,
  },
];

export function materialUrl(material) {
  if (material.objectUrl) return material.objectUrl;
  return material.filename ? `${import.meta.env?.BASE_URL ?? '/'}materials/${encodeURIComponent(material.filename)}` : null;
}

const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export function filterMaterials(items, { format = 0, discipline = 'Todas', professor = 'todos', search = '' } = {}) {
  const terms = normalize(search).trim().split(/\s+/).filter(Boolean);
  return items.filter(material => {
    const matchesFormat = format === 0 || (format === 1 && material.format === 'PDF') || (format === 2 && ['DOC', 'DOCX'].includes(material.format)) || (format === 3 && material.mediaType === 'video') || (format === 4 && material.format !== 'PDF' && !['DOC', 'DOCX'].includes(material.format) && material.mediaType !== 'video');
    const matchesDiscipline = discipline === 'Todas' || material.disciplineId === discipline;
    const matchesProfessor = professor === 'todos' || material.professorId === professor;
    const text = normalize([material.title, material.description, material.discipline, material.professor, material.author, material.course, material.institution].join(' '));
    return matchesFormat && matchesDiscipline && matchesProfessor && terms.every(term => text.includes(term));
  });
}
