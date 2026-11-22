# PGBR — Auditoria PT-BR LT002 (20/11/2026)

## Escopo
Branch `feature/pt-br-traducao`. Revisão guiada por capturas da interface do PGBR e documentação do repositório. Não alterar a branch `main` nem o PokeSearch.

## Correções realizadas
- Pesquisa de Campo: explicação da captura, mensagem de catálogo vazio e resumo dinâmico com contagens.
- Inventário: nomes dos itens conhecidos traduzidos somente no texto exibido; IDs, regras de remoção e valores mantidos.
- Companheiro: prioridade das frutas traduzida item a item, inclusive nomes opcionais.
- Incubação: prioridade, mensagens de início e estados básicos em português.
- Mapa: glossário ampliado para pesquisa e categorias; a tradução só aparece onde a interface usa `PtBr.tr`.
- Documentação: `docs/BUILDING.md`, `docs/ROOTED.md` e normas das notas de lançamento traduzidos.
- Versionamento: incremento para `v0.00.03-LT`, com workflow, README e site coerentes.

## Questões pendentes de validação
- Confirmar no aparelho se os textos recebidos dinamicamente do jogo combinam com as traduções disponíveis.
- Mapear todos os nomes possíveis dos itens retornados pelo catálogo nativo; novas formas podem permanecer no idioma original.
- Examinar componentes de mapa de fontes externas: a captura com filtros `Power s` não corresponde claramente aos controles verificados no código; não modificar UI de outro aplicativo sem confirmação.
- Revisar os guias `docs/ARCHITECTURE.md`, `docs/GAME_UPDATES.md` e instruções históricas ainda em inglês.
- A compatibilidade do jogo e de suas funções nativas não é comprovada somente por compilação ou testes de código.

## Compatibilidade e segurança
`dev.pgrazor`, caminhos `pgrazor/`, nomes de módulos, identificadores de protocolo e variáveis `PGRAZOR_*` são internos e devem ser preservados até uma migração explicitamente testada. Dados de conta, segredos e assinatura não foram alterados. Créditos obrigatórios do projeto original permanecem.
