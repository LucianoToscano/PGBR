# LT046 — Itens parciais e pendentes: estado, evidência e condição de desbloqueio (24/11/2026)

Este documento registra, item a item, o que está integrado, o que foi verificado e o que continua **bloqueado** — e por quê. Nenhum item aqui é declarado homologado. `device_tested=false`; `provider_connected=false`. CI não equivale a aparelho.

## Integradas nesta rodada (24/11/2026)

| Item | Situação | Evidência |
|---|---|---|
| Segurança do cofre | **Integrada na `main`** | PR #173 (`62e82bc` HEAD) mesclado em `5f05c96` (24/11/2026 10:44 UTC); CI 6/6 success (`45374098860`, `45374098817`). Revisão da janela de cofre independente: serialização por provedor, edição bloqueada durante consulta/salvamento/remoção, callbacks ignorados após Activity encerrada. |
| GeoLab | **Integrada e isolada na `main`** | PR #165 (HEAD `a9d67a3`, CI 3/3 success: `45373978316`/`45373978363`/`45373978395`), reconciliado e fundido mantendo `pgrazor/research/geolab/` e o workflow `pgbr-geolab-ci.yml` fora do APK de produção. Laboratório de pesquisa; não é APK PGBR; Drive real não validado; sem publicação. |
| Interface e ícones | **Integrada na `main`** | PR #174 (HEAD `7bc9953`, CI 6/6 success: `45374762685`, `45374762335`), reconciliado e fundido na `main`. Galeria local consulta ID exato sem adivinhar por semelhança; resumo filtrado separa ausência/erro por ID e contabiliza eventos de contexto permitidos; preparação Max com componentes ofensivo/defensivo independentes. |

## Parciais — o que foi feito e o que falta

### PvP e VGP+ (Parcial)
- **Presente:** Rocket QA6 com prioridade imediata; política Java de seleção por eficácia (tipos, 3×3, prontidão); classificador VNext nativo com fail-closed; escopo restrito a Treinamento/PvP/duelo confirmado (sem Raid/Ginásio/Max); diagnóstico de contexto no resumo filtrado (#174); espelhamento Java→nativo F3 com backup/restore.
- **Pendente (não resolvido nesta rodada, e NÃO corrigido em código):**
  1. Segundo carregado supereficaz não ligado ao executor nativo (#90) — exige instrumentação nativa e prova em aparelho.
  2. Perda de ativação após troca de janela sem rearme (#105) — sintoma observado em aparelho; exige sessão física e logs reais para localizar o ponto de falha (Java `Control.java` apenas reflete `pvpExecutorReady` publicado pelo nativo; não há rearme automático no host).
  3. VGP+ por etiquetas/raridade no jogo (#133) e atalhos MT (#134) — exigem hooks nativos verificados; não implementável com segurança por inspeção estática.
- **Não reivindicar:** "PvP funcional", "VGP+ por etiquetas ativo", qualquer homologação.
- **Desbloqueio:** sessão de homologação no aparelho com a matriz `LT046_HOMOLOGACAO.md` + implementação nativa das lacunas #90/#105/#133/#134 com testes host antes de build.

### Batalha Max (Parcial)
- **Presente:** assistente manual de preparação Max (até 3 candidatos, elegibilidade tri-state, multiplicadores por componente, orçamento de partículas informado) — nunca inicia batalha nem gasta recursos; guard de injeção de máscara Max (#137); teste host `AutoMaxBattleSessionTest`.
- **Pendente:** sessão Max completa em jogo (seleção de equipe, execução, Power Spots) não homologada; o executor nativo não está validado para Max. #131/#154 abertos e divergentes.
- **Desbloqueio:** validação nativa de combate Max em aparelho, com sessão isolada e reversão segura.

### Snipers e Radar (Parcial)
- **Presente na LT045:** Snipers 1/2 e filtros de Radar integrados no APK publicado (`device_tested=false`); sprites por ID exato com fallback legível; filtros de busca corrigidos (sem Global falso de Reides/Dex inválido).
- **Pendente:** fontes reais autorizadas e teste físico; PR #121 (auditoria de regressões, divergente) e #149 (regressões QA pós-LT044) não mescladas — a #121 depende de #115/#118 e exige seleção manual de hunks por arquivo; #118 (PokeList Partner) e #119 (B1–B10, staging) exigem contrato e credenciais do operador; #108/#111 (radar/feed) e #129 (importação PGSharp) aguardam revisão seletiva.
- **Não mesclar às cegas.** Gates da auditoria `AUDITORIA_SNIPERS_POS_LT044_2026-11-23.md` permanecem válidos.
- **Desbloqueio:** contrato escrito do operador + merge seletivo de #121/#149 com CI completo + QA no aparelho.

## Pendente — bloqueio externo (não é defeito de código)

### Integrações externas
- Nenhuma fonte externa está ativada. Exige: contrato/autorização do operador, credenciais fora do repositório, consentimento explícito do usuário, endpoints/hosts validados, política 401/403/429 e expiração de observações — nada disso existe para PokeList/PGSharp/Data Hub/gateway/IA neste momento.
- O cofre Gemini/Groq (#168/#173) é apenas o armazenamento local independente (prévia `dev.pgrazor.preview`): nenhum provedor foi conectado e nenhuma chamada de IA existe no cliente do jogo.
- **Desbloqueio:** documentos de contrato + secrets do operador + sessão de testes isolada. Sem isso, o item permanece vermelho por definição.

## Compilação e Publicação LT046

- **Compilação:** workflow `pgbr-lt046-qa.yml` executado em 24/11/2026 (run `45382217542`, `completed/success`); artefato `PGBR-LT046-Root-e-SemRoot-QA` (id `14914618192`, digest do ZIP `sha256:ca8775edc8015282351286c813b1c9dedf086852a4f771a8d82bb5368110260a`); hashes individuais dentro do artefato. Sem publicação.
- **Publicação:** **não autorizada** nesta etapa; permanece pendente até build verificado e autorização explícita do proprietário. LT045 continua a pré-release publicada vigente.

## Regra de atualização

Ao desbloquear qualquer item acima, atualizar este arquivo, o Arquivo Mestre e a versão/release correspondente com evidências reais (commit, CI, hashes, configuração do aparelho), sem apagar este histórico.
