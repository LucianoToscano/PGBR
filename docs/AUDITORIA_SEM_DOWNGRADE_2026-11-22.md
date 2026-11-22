# Auditoria de regressões e hardening PGBR — 22/11/2026

## Escopo e níveis de evidência
- **Base canônica antes de editar:** `main` `b355def7f6693b4d7f88c802b6ea40df018f0490`.
- **Versão instalável publicada consultada:** `v0.00.40-LT`, release QA de 22/11/2026, build Actions #45054751331 e SHA-256 publicados em `docs/releases/v0.00.40-LT.md`. Não substituir/reescrever seus artefatos.
- **Comparação GitHub `v0.00.40-LT...main`:** `ahead=16`, `behind=0`. A `main` incorporou a PR #116 (motor de recomendação puro + backup/rollback), mas não há nova versão instalável gerada desses commits na release LT040. Não houve exclusão de arquivo sinalizada na lista de alterações entre a tag e a `main`. **Isto NÃO prova paridade funcional no dispositivo.**
- **CI da `main` no HEAD anterior:** runs #45072268952 (código) e #45072268865 (governança) `completed/success`; não substituem testes Android físicos.
- **Trabalhos paralelos deixados intactos:** #101 (PT-BR), #108/#114 (radar), #109 (GPS), #115 (Snipers nativos), #117 (cura e Ataque Inteligente, draft), #118 (API parceira PokeList dependente da #115, draft). `#114` estava marcado não integrável; não combinar automaticamente mudanças divergentes no `LiveFeedDialog`.
- **Bloqueios funcionais reais:** issue #90 — preferência Ataque Inteligente não seleciona botão 1/2 no executor; issue #105 — relato de desarme PvP após alternância de janela, sem causa conclusiva nem homologação física. Nova estratégia C++ da #116 é somente recomendação, não execução.
- **Classificação:** auditoria de repositório/CI, contratos e pontos de código selecionados; **não** auditoria exaustiva de cada linha, conta, aparelho, modo, API externa ou funcionamento no jogo. `device_tested=false`.

## Falhas concretas observadas no código `main`
1. **Raio iFlowGo NaN:** `IFlowGoFeedClient.fetch` aceitava `NaN`, concatenava `radius_km=NaN` e enviava parâmetro inválido; havia apenas clamp para raio infinito e valores finitos.
2. **Espécie numérica inválida:** `IFlowGoFeedClient.fetchGlobal` ignorava Dex fora de 1..1025 e enviava pesquisa não filtrada. No modal, consulta `#2000` podia mostrar a primeira página não filtrada como se fosse o filtro pedido.
3. **Raide rotulada Global apesar de ser somente área:** `LiveFeedDialog` salvava/selecionava raio Global por padrão para iFlowGo em modal Reides; `IFlowGoFeedClient.fetch` convertia `+infinity` para raio local de 25 km. Usuário podia acreditar que era feed mundial.
4. **Documentação de build e Root defasada:** `docs/BUILDING.md` e `docs/ROOTED.md` ainda direcionavam a LT034 e a uma opção `publish` não disponível no workflow LT040 congelado.

## Correções nesta branch
- `IFlowGoRequestPolicy.java` concentra limites puros de raio (valida `NaN`, zero e negativos) e espécie (sentinela -1 explícita, ID 1..1025); exceções em dados inválidos antes do HTTP.
- `IFlowGoFeedClient.java` usa política, sem mudar endpoints, header, taxa, autenticação ou a paginação global limitada à primeira página.
- `LiveFeedDialog.java` não rotula busca de Reides em área como Global: por padrão escolhe 3 km em Reides e recusa seleção Global com mensagem clara; rejeita #Dex numérico inválido antes de consultar.
- `IFlowGoRequestPolicyTest.java` cobre limites reais em JVM; `test_iflowgo_request_policy_java.py` executa o Java real e verifica wiring de tela e cliente no CI.
- `docs/BUILDING.md`, `docs/ROOTED.md` removem instruções enganosas sobre LT034 e releases já publicadas.

## Matriz de não regressão e próximos gates
| Área | Evidência atual | Risco restante |
|---|---|---|
| Batalha Rocket, PvP, Treino, Duelo | CI de fonte e host da main, PR #116 | Teste físico de ativação/rearme e segundo carregado pendente (#90/#105) |
| Raid, Ginásio, Batalha Max | executor separado de Rocket (docs de motor) | Não aplicar patches PvP automaticamente |
| Escudo/Troca/Cura | LT040 timings documentados; #117 isolada | Homologação física e associação de UI |
| GPS/Teleporte/joystick | arquivos e testes históricos, PR #109 separada | Calibração e dispositivo por versão |
| Radar iFlowGo | bug de escopo e parâmetros corrigido na branch | Feed/paginação/autorização podem mudar; sem extrapolar cobertura |
| PokeList, RocketMap, parceiros | #114/#115/#118 aguardando; HTTP 403 e licenças | Não declarar integrados ou homologados |
| Configurações/backup | PR #116 foi mesclada; tests JVM | Resistência a morte de processo e falhas de armazenamento |
| PT-BR e ícones | PR #101 independente | Inspeção visual e testes Android |
| Pacote completo | LT040 release tem APK+Root+Zygisk e hashes | **Esta branch não gerou nova release nem APK.** |

## Condições para nova release
1. Checks da PR desta branch concluídos com sucesso; revalidar `main` e conflitos.
2. Inspecionar PRs paralelas por arquivos comuns, sobretudo `LiveFeedDialog`/mestre; integrar apenas o que foi revisado, sem apagar regressões ou recursos dos outros chats.
3. Branch/tag própria para versão nova, entrada original com hash validado, segredos de assinatura no Actions; gerar sem Root, Root e Zygisk pelo mesmo commit e verificar SHA-256 com `tools/verify_release.py`.
4. Registrar commit final, CI, links e digests no `PGBR_ARQUIVO_MESTRE_UNICO.md` depois da publicação efetiva. Não renomear LT040 como nova build.
5. Ainda `device_tested=false` até testes reais com consentimento do usuário em aparelho de teste, incluindo backup e possibilidade de rollback.

## Estado desta auditoria
Branch: `audit/20261122-no-downgrade-hardening`, derivada de `b355def7f6693b4d7f88c802b6ea40df018f0490`. Correções isoladas, reversíveis e sem mudanças em tempos de combate, executores nativos ou arquivos externos. Resultados de CI/PR devem ser registrados após os respectivos runs; não antecipar sucesso.

## CI e sincronização revisados

- Primeira verificação da PR #122: três jobs aprovados (C++ host, NDK ARM64, website); o job Windows falhou **por dois testes estáticos desatualizados** que exigiam expressões literais `provider == 2 ? 3 : 1` e `pokemon_id=`. Esses testes foram corrigidos para validar a nova política extraída, sem devolver o comportamento antigo nem relaxar filtros.
- Durante a auditoria, a `main` avançou em alterações independentes do PvP até `0e1a42a9dbe94cd726b4f611cb25d1a67f395bcf`. Houve integração explícita da nova `main` na branch com dois pais Git, preservando as alterações PvP e o conteúdo mais recente do Arquivo Mestre (merge auditado `e424d6d85ba67172d69e0e3e54dc7e62602dfbd2`).
- Repetir a verificação do GitHub Actions para a HEAD final da PR antes de mesclar; o resultado antigo de falha não comprova sucesso nem pode ser ignorado.

## Consolidação após merge auditado

- PR #122 foi mesclada em `main` em `97bc1d6c2ba25b7bbbe8e5953caea73da0a14db1` após checks finais de código e governança **success** nos Actions #45084122904 e #45084122863; não houve nova release por esse merge.
- Branch candidata LT041 reconciliada com essa `main` preservando o conteúdo concorrente de PvP, o histórico do Mestre e o código corrigido no radar. O workflow de release permanece isolado e bloqueado até os checks completos desta PR. Existe também um experimento de QA LT041 distinto na PR #121, sem integração presumida; não sobrepor a ele nem confundir variantes.
