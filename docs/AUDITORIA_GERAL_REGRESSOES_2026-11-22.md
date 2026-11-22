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
