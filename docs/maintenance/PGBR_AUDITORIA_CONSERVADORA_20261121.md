# PGBR — Auditoria conservadora de inventário e limpeza

> **Data de referência:** 21/11/2026 (GitHub; não é homologação em aparelho).
> **Fonte única de status:** [PGBR_ARQUIVO_MESTRE_UNICO.md](../../PGBR_ARQUIVO_MESTRE_UNICO.md), canônica na `main`.
> **Escopo:** repositório `LucianoToscano/PGBR`. PokeSearch, Drive, fontes privadas e Android físico fora deste inventário.

## 1. Evidência do inventário

Foram consultados os objetos Git do repositório via API do GitHub, árvore recursiva não truncada. A contagem se refere a **arquivos rastreados**, não ao cache local, histórico completo de commits, Downloads do Actions ou pastas de Drive.

| Referência | Arquivos rastreados | Bytes estimados dos blobs |
| --- | ---: | ---: |
| `main` (`284e5a583e6de9fcb256ae5b213bb4b352a09819`) | 1.966 | 34.782.810 |
| LT032 candidato / PR #40 (`15252c491d0c823ceb924516ae950abd685e931a`) | 2.176 | 38.012.286 |
| LT032 Root QA / PR #41 (`fb42bbf37783cd458772dd9bf3896ee020d4854c`) | 2.177 | 38.021.062 |
| LT032 diagnóstico VGP / PR #42 (`a24aa693aefb858ff0f350e1e13257bc5b01de22`) | 2.181 | 38.019.329 |

Comparação de caminhos `main` → candidato LT032 no snapshot acima: **210 arquivos adicionados, 123 modificados e 0 removidos**. As branches LT032 são **paralelas e não podem ser somadas cegamente**; PR #41 e PR #42 têm bases e dependências próprias.

## 2. Estrutura funcional a preservar

| Área | Local | Diretriz de conservação |
| --- | --- | --- |
| Governança | Mestre, `AGENTS.md`, `.github/copilot-instructions.md`, teste de governança | Um mestre canônico, sem status concorrentes |
| Java / UI | `pgrazor/src/dev/pgrazor/` | Preserve interface pública, preferências, serviços, VGP e controles de PvP |
| Native e compatibilidade | `pgrazor/native/` e `pgrazor/native/schema/` | Não mover/remover hosts, metadata, contratos e perfis antigos sem comparação |
| Recursos visuais | `pgrazor/assets/`, `pgrazor/branding/`, `website/public/` | Preservar sprites, ícones, origens/licenças e variantes de distribuição |
| Empacotamento Root/sem Root | `pgrazor/nonroot.py`, `pgrazor/release_apk.py`, `pgrazor/rooted.py`, `.github/workflows/` | Não unificar por exclusão nem confundir módulo Zygisk com APK |
| Testes e diagnósticos | `pgrazor/test/`, `tools/`, `docs/validation/` | Não excluir fixtures ou provas só por data/nome |
| Histórico de origem | `upstream-history/`, `upstream-workflows/` | Material de rollback, proveniência e licenças; não é lixo |
| Terceiros | `pgrazor/vendor/`, `THIRD_PARTY.md`, `LICENSE` | Manter hashes, derivações e avisos de licença |

Os blobs maiores identificados incluem o bundle de histórico `upstream-history/PGRazor-upstream-full.bundle` (13.613.466 bytes) e imagens do website. **Não são candidatos a remoção por tamanho.**

## 3. Duplicados por conteúdo — proteger até analisar referências

A `main` contém **1 grupo de arquivos idênticos**, uma cópia SVG compartilhada entre branding e site. O candidato LT032 contém **7 grupos**, sendo logos e cinco sprites de bagas reutilizados em caminhos de consumo diferentes:

- Logos: `pgrazor/branding/pgbr-logo.webp`, `pgrazor/root-installer/res/drawable/pgbr_logo.webp`, `website/public/logo.webp`.
- SVG: `pgrazor/branding/pgrazor.svg`, `website/public/logo.svg`.
- Berries IDs **701, 703, 705, 706, 707**: `pgrazor/assets/item-icons/item-ID.png` e `pgrazor/assets/map-icons/berry-ID.png`.

**Decisão:** manter todas as cópias. A identidade binária não estabelece que os consumidores Java, Android Resources ou website aceitem mudança de caminho. Uma futura deduplicação só pode ocorrer após mapa de dependências, compilação Root/sem Root e testes de UI.

## 4. Correções não destrutivas desta revisão

1. Atualizar o `README.md` sem apagar o histórico de instalação LT001: apresentar links **diretos verificados** dos três instaláveis da última release pública identificada (LT023), distinguir builds LT032 em Actions de releases e evitar anunciar homologação inexistente.
2. Corrigir no `PGBR_IMPORTACAO.md` o caminho incorreto para o bundle histórico (`PGRazor-upstream-full.bundle`) e esclarecer que a licença MIT da origem não cobre conteúdo de terceiros.
3. Remover a entrada equivocada `AGENTS.md` do `.gitignore` (o arquivo é obrigatório, já rastreado) e acrescentar ignores para **resíduos locais gerados**, sem apagar arquivos históricos rastreados.
4. Adicionar `tools/repo_inventory.py`, scanner **read-only** dos arquivos rastreados: áreas, hashes, grupos idênticos, caminhos de revisão sensíveis e ausências; `--json` para inventário detalhado, `--strict` para reprovar alerta, nunca para excluir.
5. Adicionar `pgrazor/test/test_repo_inventory.py`: casos de classificação, alertas, duplicação e preservação dos bytes após inventariar um repositório temporário.
6. Registrar o resultado neste documento e, após verificação de commits/PR, adicionar ponteiro ao **único Arquivo Mestre**.

## 5. Procedimento seguro para futuras limpezas

1. Ler integralmente o mestre da `main`, `AGENTS.md`, regras de compilação e documentos de arquitetura.
2. Identificar branch, commit, componentes gerados, licenças, consumidores, recursos exclusivos e versão de rollback.
3. Executar `python -B tools/repo_inventory.py --json` em checkout local, salvar saída **fora de Git** se contiver caminhos privados. Esse comando **não altera** arquivos; não confundir o inventário de API com sua execução local.
4. Confirmar cada candidato por referência, licenças, hash, uso em Root e sem Root, e impacto em testes, menus, sprites, assets e workflows.
5. Somente propor exclusão em PR independente depois de prova verificável de desuso e backup. Caso contrário, manter e documentar.
6. Validar `python -B tools/check.py`, artefatos, assinaturas e, para funcionalidade Android, instalar em aparelho. Build verde é só build verde.

## 6. Limites e pendências

- **Nenhum arquivo funcional, sprite, release, branch ou backup foi apagado.** Nenhuma limpeza destrutiva foi autorizada por um indicador de duplicação isolado.
- O inventário via GitHub detecta caminhos e igualdade de blobs, mas **não prova ausência de segredos no conteúdo**, uso em tempo de execução, integridade do Drive nem compatibilidade de Android.
- Os testes do script e do mestre foram **executados com sucesso** em [CI PR #43](https://github.com/LucianoToscano/PGBR/actions/runs/44972179155) e [CI push](https://github.com/LucianoToscano/PGBR/actions/runs/44972174481); isso não comprova homologação de Android nem suíte geral do app.
- A promoção da LT032 e dos PRs #40/#41/#42 para `main` é tarefa de integração separada. Não concluir por esta auditoria que os recursos avançados existem na `main`.
- O documento complementa o Arquivo Mestre, sem reivindicar status canônico alternativo.

## 7. Complemento — navegação, documentação e Drive (21/11/2026)

- **Dois links locais obsoletos na `main` identificados:** `AGENTS.md` exigia `docs/releases/v0.00.13-LT.md` e `AUDITORIA_IA_COMECE_AQUI_LT013.md`, presentes na branch LT032 mas **inexistentes na `main`**. Nesta revisão, a leitura obrigatória continua, mas os documentos históricos são associados explicitamente à branch que os contém.
- **Um mestre, múltiplos pontos de entrada:** índice navegável e mapa de armazenamento incorporados ao próprio `PGBR_ARQUIVO_MESTRE_UNICO.md`; `CONTRIBUTING.md` e modelo de PR passam a exigir consulta. Não foi criado um segundo documento de autoridade.
- **Scanner de links e governança:** ampliado `test_master_governance.py` para testar destinos relativos do mestre e documentação local do `AGENTS.md`. Um workflow isolado, sem secrets e só com `contents: read`, executou inventário e testes em GitHub Actions. As execuções **44972179155 e 44972174481** terminaram com sucesso; o resultado vale apenas para o escopo documental/inventário.
- **Revisão de pastas do Drive:** `Projetos/PGBR` contém áreas separadas para Mestre, APKs QA, Código Fonte, Relatórios, Builds/Módulos e material restrito. O mestre mantém um **apontamento**, não uma cópia documental concorrente; fontes históricas PokeGoBR existentes na pasta Mestre foram mantidas intactas.
- **Candidatos de limpeza:** nenhum arquivo rastreado com extensões típicas de APK, chaves ou resíduos (`.tmp`, `.pyc`, `.log`) encontrado nas árvores revisadas; a análise por nomes **não descarta segredos embutidos em outros arquivos**. As sete famílias de conteúdo idêntico em LT032 continuam intocadas.
- **Critério de conclusão:** a manutenção está apta à **revisão para integração documental**, pois o CI do PR #43 passou e o diff registra **zero exclusões**; a revisão/merge continuam etapas distintas. CI de governança não substitui `tools/check.py` integral nem verificação física de APK Root/sem Root.
