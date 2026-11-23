# Auditoria geral de regressões PGBR — 22/11/2026 (QA, sem homologação física)

## Proveniência e proteção do trabalho paralelo

- Repositório: https://github.com/LucianoToscano/PGBR
- `main` no início da auditoria: `b355def7f6693b4d7f88c802b6ea40df018f0490` (merge da PR #116).
- Última versão pública comprovada no início: `v0.00.40-LT`, pré-release QA (tag anterior ao merge #116).
- Base desta correção: PR #115, HEAD `1932edfe1a538f86132456cf4d0a9451adc0b485`.
- Branch independente: `audit/pgbr-regressions-20261122-2020`; PR #121 direcionada à branch da PR #115, não diretamente à main.
- Dependente PR #118 (PokeList Partner) e paralela PR #117 (Batalha/Cura) preservadas. Nenhum merge, force-push, edição de chave, alteração do PokeSearch ou reversão das PRs de batalha foi executado.

## Auditoria antirregressão (downgrade)

Comparação GitHub `v0.00.37-LT...v0.00.40-LT`: avanço de 118 commits, nenhum arquivo listado como removido.
Comparação `v0.00.40-LT...main`: avanço de 16 commits, nenhum arquivo listado como removido; alterações concentradas em backup, documentação, verificadores, opções e políticas.
Comparação `main...PR #115`: avanço de 37 commits, sem atraso em relação à main à data da leitura.
**Esse inventário não prova ausência de downgrade funcional.** O usuário reportou regressão de ataque PvP em LT039, mitigada parcialmente na LT040 sem homologação física. O módulo de priorização de golpes do Ataque Inteligente segue sem evidência de vínculo MoveId → botão/ação no executor, segundo PR #117. Não foi alterado nesta branch. Versões LT037/LT038/LT039/LT040 permanecem imutáveis e publicadas separadamente.

## Defeitos da interface e normalização confirmados no código da PR #115

| Severidade | Sintoma | Causa | Correção nesta branch |
|---|---|---|---|
| Alta | Atualização automática selecionável, mas sem consulta periódica | `tickOnce` apenas retirava linhas expiradas | temporizador de 5 s observa `autoRefreshSeconds`, intervalo do usuário, mínimo do fornecedor e intervalo após tentativas pré-flight; somente enquanto o diálogo vive |
| Alta | Alterar ordenação mostrava rótulo novo e linhas na ordem anterior | `openSort` apenas salvava preferências e renderizava lista já ordenada | `SpawnSearch.refilter` sobre linhas recebidas, sem nova requisição |
| Alta | Trocar fonte podia mostrar registros da fonte anterior sob rótulo novo | `openSources` mantinha `result`, `source` e geração ativa | cancelar, incrementar geração, invalidar fonte e limpar resultados antes de trocar o rótulo |
| Alta | Global iFlowGo com mais de 2 páginas podia indicar `FULL_RESPONSE` | no limite local, `pagesAvailable` continuava igual à página consultada | se próxima página existir, registrar `page+1`; total ausente permanece desconhecido |
| Média | Linha expirada reaparecia ao aplicar filtro/ordenar | timer mutava apenas lista do adapter, mas `render()` relia snapshot imutável | `matches()` sempre desconsidera encontros expirados; tick renderiza de forma estável |
| Documental | README/Root e catálogo ainda mencionavam estados antigos | texto histórico confundido com release atual | destacar LT040 como último QA verificado e ajustar texto da paginação |

### Testes adicionados

`pgrazor/test/test_sniper_panel_contract.py` contém cinco novos testes de regressão por contrato: temporizador automático, expiração estável, invalidação de fonte, refiltragem na ordenação e cobertura parcial quando há mais páginas.

**Limite:** testes estáticos de fonte não comprovam execução em Android. Exigir também compilação SDK real, suíte Java/host, integração com dados de provedor autorizado, exame de interface, verificação da assinatura e instalação em aparelho.

## Pontos que permanecem pendentes e não podem ser apresentados como funcionais

1. As PRs #115/#118 não foram integradas à `main`; cliente PokeList Partner v1 requer URL, licença e chave do operador. Não reclassificar como feed real autorizado sem staging.
2. Nenhuma consulta ao vivo de provedor foi reproduzida nesta auditoria e não há garantia de cobertura mundial.
3. O Ataque Inteligente da PR #117 não tem vínculo confirmado do golpe recomendado com o botão do jogo; a tela branca e o PvP em jogo real não estão homologados.
4. APK publicado anteriormente: LT040 pré-release QA; teste físico não realizado (`device_tested=false`).
5. O caminho de release para uma candidata posterior deve usar o APKM ARM64 original aprovado e assinaturas em secrets, sem alterar releases antigas. A geração de artefato só é comprovada por run concluído e ativo/link verificável.

## Gates antes de integrar à main

- PR #121: checar CI do commit final, revisão e integração na PR #115.
- PR #115: revalidar testes após a integração da PR #121, concorrência e interfaces.
- PR #118: rebase após integrar PR #115; não presumir acesso ao parceiro.
- PR #117: concluir cadeia de Ataque Inteligente e teste real antes de anunciar funcional.
- Realizar prova de teleporte, cooldown, fontes, Rocket/PvP/Raid/Ginásio/Max, Root/sem Root, restauração e dispositivo.
- Atualizar Mestre após **cada** merge e release efetivamente concluídos, com SHA-256 e URLs verificadas.

Estado desta auditoria: mudanças de código e testes registradas na branch; **não** confundir CI com homologação do jogo. Resultados de novos workflows e qualquer novo APK/release devem ser anexados como evidência posterior, nunca presumidos.


## Resultado verificado da compilação, assinatura e pré-release LT041-QA

- **Execução final de build:** [GitHub Actions #45083864583](https://github.com/LucianoToscano/PGBR/actions/runs/45083864583), SHA exato `f758bc282205e24e434a48f382a581681bcabf4d`, `completed/success` em 22/11/2026 23:45 UTC. Os jobs `native-pvp-fixture` e `apk-test` foram aprovados. Passaram `tools/check.py`, compilação do aplicativo completo, montagem do APK, módulo Root/instalador, verificação cruzada de SHA-256 e publicação de pré-release.
- **Artefato Actions:** `PGBR-LT041-QA-AUDIT-Root-e-SemRoot`, ID `14777829836`, disponível na mesma execução.
- **Release pública QA efetivamente publicada:** [v0.00.41-LT-QA](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.41-LT-QA), ID `454837355`, `prerelease=true`, `draft=false`, publicada em 22/11/2026 23:45:37 UTC, apontando para o commit de código `f758bc28` **na branch isolada**, não na main.
- **APK completo sem Root:** [PGBR-v0.00.41-LT-QA.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT-QA/PGBR-v0.00.41-LT-QA.apk), 188.884.291 bytes, SHA-256 `6cf44a477f092c8eff1d3ff60f814975342584ca462e3694ac8f58ec7129ff58`.
- **Instalador Root:** [PGBR-v0.00.41-LT-QA-Root.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT-QA/PGBR-v0.00.41-LT-QA-Root.apk), 8.409.809 bytes, SHA-256 `1a38b7cf8d4e93b87dd318282159fb555a1d398af7ddac286d187e85e25189ea`.
- **Módulo Zygisk:** [PGBR-v0.00.41-LT-QA-Root-module.zip](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT-QA/PGBR-v0.00.41-LT-QA-Root-module.zip), 8.327.408 bytes, SHA-256 `423031f6ffcc79812c73e872e328dbc26d287ae52e9115fd115188ec573dc834`.
- **Manifest de hashes:** [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT-QA/SHA256SUMS.txt), verificado no workflow.

**Falha diagnosticada e removida, sem mascarar testes:** o primeiro run completo [#45083400679](https://github.com/LucianoToscano/PGBR/actions/runs/45083400679) falhou no `tools/check.py`, ao identificar um arquivo `rooted_installer.py` corrompido por edição de string e uma asserção obsoleta de paginação. O arquivo foi restaurado da main e a mudança de `versionCode` reaplicada corretamente (commit `01e3ca7`); teste de paginação atualizado (commit `f758bc2`). A suíte final passou sem ignorar regressões.

**Outras correções comprovadas na branch:** `SniperPanel.java` não aninha mais `ListView` com altura ponderada em `ScrollView`; seis regressões de contrato de painel, quatro testes da lógica de código de versão Root; `pgrazor/test/test_iflowgo_wiring.py` ajustado à política correta. O compilador Android do `tools/check.py` foi executado no CI com SDK real.

**Limites remanescentes:** `device_tested=false`; nenhuma execução no Android físico, nenhum feed em tempo real validado, nenhuma ação de combate/teleporte comprovada no Pokémon GO. O teste passou apenas para o SHA do código incluído na pré-release. PR #121 permanece sem merge, PR #115 e PR #118 continuam dependências. Este documento registra a nova QA experimental sem converter a versão histórica LT040 nem alterar o conteúdo dela.
