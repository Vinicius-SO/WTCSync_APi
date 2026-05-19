# specs/ — Feature Specifications

Specs sao escritas **antes da implementacao** para pesquisar, decidir e documentar como uma feature sera construida.

## Formato do arquivo

- **Nome:** `kebab-case.md` descrevendo a feature (ex: `drizzle.md`, `code-editor-syntax-highlight.md`)
- **Linguagem:** Portugues para prosa, Ingles para codigo

## Secoes obrigatorias

```markdown
# Spec: <Feature Name>

## Resumo
Uma ou duas frases descrevendo o que sera feito e por que.

## Pesquisa realizada
Tabelas comparativas de abordagens, bibliotecas ou ferramentas avaliadas.
Cada tabela deve ter coluna de **Veredicto** com a decisao.

## Decisao
Arquitetura escolhida com justificativa. Diagramas ASCII quando util.

## Especificacao de implementacao
- Componentes/arquivos a criar ou modificar
- APIs e assinaturas de funcoes (pseudocodigo TypeScript)
- Detalhes tecnicos relevantes (CSS, performance, etc.)

## Dependencias novas
Tabela com pacote, motivo e estimativa de bundle.
Omitir se nao houver dependencias novas.

## Riscos e consideracoes
Lista numerada de riscos com estrategias de mitigacao.

## TODOs de implementacao
Checklist com `- [ ]` de cada passo na ordem de execucao.
Marcar com `- [x]` conforme implementado.
```

## Guidelines

- Specs sao **documentos vivos** — atualizar TODOs conforme a implementacao progride
- Incluir **estimativas de bundle size** quando adicionar dependencias client-side
- Usar **tabelas** para comparar alternativas (mantem decisoes rastreaveis)
- Blocos de codigo sao **pseudocodigo** — detalhes suficientes para guiar implementacao, nao codigo final