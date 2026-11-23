# Auditoria Rocket — ataque carregado inferior liberado na QA2

**Data:** 2026-11-23 UTC / 2026-11-22 America/Fortaleza. **Escopo:** arquivo enviado `Screen_Recording_20261122_232934.mp4` (32,673 s), código da branch `feat/pvp-smart-charge-qa-sync-20261122`, PR #128, release anterior `v0.00.43-RocketQA2-LT`.

## Evidência e limitação da gravação

- Treinamento contra líder com Toxtricity versus Vaporeon, não uma partida da Equipe GO Rocket. O vídeo contém diversos acionamentos/carregamentos e mensagens **`Toxtricity usou Ataque Veloz!`**, apesar de haver `Ataque Selvagem`, cujo botão exibe classificação **`SUPEREFICAZ`** em momentos do vídeo.
- A gravação comprova qual golpe foi executado, mas os marcadores visuais não permitem atribuir inequivocamente a ação a um toque manual ou ao código automático. Também não expõe JSON `chargeChoice`, `smartAttackMode` ou o estado do toggle em cada tick.
- Há reprodução de resultado indesejado no cenário do usuário; não afirmar causa única sem telemetria do aparelho.

## Arquitetura verificada

- **Motor central:** `pgrazor/native/rocket_combat.cpp::vnext_tick()` (feature 21) decide e chama `ChargeAttackDisplayVNext.ChargeAttackButtonTapped()`. O ataque ágil continua em `CombatVNextGestureHandler`; Legacy mantém `CombatDirectorV2.TryFastAttack()`.
- **Observação/encaminhamento:** `pgrazor/native/combat_host.cpp` valida escopos, observa `ChargeAttackDisplayVNext.ObserveBattleState` após o original e encaminha `PGR_RC_OBS_CHARGE`. `pvp_scene.h` + host classificam treinamento, duelo e Liga GO; as variantes validadas chamam o **mesmo Rocket executor** por `PGR_RC_OBS_PVP_SCOPE_ENTER`. Não existe novo motor PvP.
- Raid, Ginásio e Batalha Max têm escopos/executores distintos e não foram alterados nesta correção.

## Causas estruturais reproduzíveis na fonte QA2

1. **Referências obsoletas após mudança de GUI:** `drop_vui()` descartava o GUI handle e amostras de eficácia, mas **não liberava/limpava `g_charges[]` nem `g_charge_count`**. Se um VNext GUI fosse substituído sem o fechamento convencional, objetos anteriores permaneciam: três/quatro candidatos tornavam `g_charge_count==2` falso e o código acionava o primeiro pronto.
2. **Fallback inseguro:** quando `inspect_display()` não selecionava um índice (efeito desconhecido, batch diferente ou amostra com idade superior a **200 ms**), o fluxo antigo **voltava ao laço original de primeiro pronto**, mesmo com Ataque Inteligente ligado. Os testes QA2 simulavam amostras imediatamente antes do tick, sub-representando o cenário de uma luta real.
3. **Fallback dentro da execução:** mesmo quando o melhor carregado era selecionado, o laço original ainda podia tentar outra entrada caso o primeiro objeto ficasse indisponível entre as verificações. Uma preferência calculada não era uma autorização exclusiva.
4. **Preferência ON perdida em snapshot inválido:** a leitura `pgr_battle_timing::read()` devolve falha quando o snapshot está vencido/incoerente. `simple_attack_enabled()` interpretava toda falha como **OFF**, permitindo voltar à ordem original. Um OFF explícito e uma falha transitória não são equivalentes.
5. **Par observado antes do primeiro tick:** se dois botões fossem vistos e um sumisse antes da primeira execução, o estado histórico de dupla captura podia não ter sido registrado.

Estas cinco falhas foram confirmadas pela leitura e por cenários simulados, mas o vídeo isoladamente não identifica quais delas ocorreram no aparelho.

## Correção aplicada exclusivamente na PR #128

- `drop_vui()` agora desanexa e libera o GUI **e todos os handles de carregados** antes das callbacks de liberação, zera contagem/evidência e preserva reentrância GC.
- `g_smart_seen_pair` registra o fato de que mais de um botão foi observado **no próprio callback de captura**, mesmo antes do primeiro tick; evita confundir desaparecimento temporário com Pokémon de apenas um golpe.
- Para dois ou mais displays capturados, o Rocket usa o slot tipado `Charge/Charge2` e a observação mais recente de cada um. Suporta contagem 3–4 sem escolher o primeiro por omissão. Não deduz o slot pela posição X/Y nem pelo índice de captura.
- `smart_attack_priority.h::admit_display` dá **autorização exclusiva a um objeto** se identidade, batch, frescor e eficácia são suficientes. Se o melhor ainda não estiver pronto, ou a classificação for desconhecida, antiga ou incoerente, **não invoca nenhum ataque carregado automático**. O Ágil preserva suas cadências e continua quando habilitado. Em igualdade de categoria, um golpe já pronto pode ser usado. `SuperEffective` verificado tem prioridade sobre `UNKNOWN`, mas UNKNOWN **não** é convertido em Normal.
- Após escolha válida, `vnext_tick` tenta **apenas o candidato autorizado**, sem loop que possa acabar acionando outro carregado inferior.
- `simple_attack_enabled()` mantém a última preferência ON de snapshot F3 válido diante de falha **transitória** de leitura; um OFF recebido num snapshot válido desativa imediatamente a política. Nenhuma preferência é inventada se nunca houve snapshot válido.
- `PGR_RC_EVT_SMART_DIAG` emite status limitado por mudança/5 s: modo (rocket/training/pvp), total de objetos, eficácia por slot, prontidão e motivo `hold/selected/original`. Não expõe conta, identificador ou coordenadas. A UI informa quando está aguardando dados ou o melhor golpe.
- A opção Smart Attack OFF preserva integralmente a ordem anterior. Pokémon com um único display capturado e sem histórico de segundo continuam usando a execução original. Ataques Legacy, Escudo, Troca, cura, Ágil e as cadências 150/500/600 ms não foram alterados.

### Trade-off explícito

No modo Smart Attack ON, se dois botões existirem e a classificação permanecer inválida, a automação **não gastará carregados** até conseguir uma decisão segura, embora o jogador possa acioná-los manualmente. Essa opção é intencional, atendendo à exigência de não liberar o golpe inferior sem comparação confiável. A telemetria é essencial para diferenciar leitura ausente, vencida e verdadeiro bloqueio do melhor golpe.

A biblioteca `smart_battle.cpp` e `battle_counter.h` calcula tipos/movimentos/equipes, mas **ainda não vincula com prova de runtime o ID exato de `move2/move3` ao display `Charge/Charge2` vivo**. Por isso não foi implementada comparação por dano absoluto. O algoritmo atual é prioridade por eficácia comprovada.

## Validação

- `smart_attack_priority_test.cpp`: autorizar Super, bloquear inferior, stale >200 ms, batch discordante, UNKNOWN, empate, ON/OFF.
- `rocket_combat_test.cpp`: aumento para **62 cenários nativos**. Casos novos: amostra com 280 ms vencidos, três widgets com slot duplicado, troca de GUI que libera todos os GC roots, modo OFF preservado, desaparecimento de um dos dois botões antes do primeiro tick, único botão real, três objetos sem metadata, empate com apenas um pronto.
- `tools/native_host_tests.py` e fixture Rocket dedicada aprovados em [Actions #45101446539](https://github.com/LucianoToscano/PGBR/actions/runs/45101446539). O teste real no aparelho permanece pendente.

**Estado:** PR #128 draft, sem merge na main; LT042, QA1 e QA2 imutáveis. O resultado em batalha real e a compilação de QA3 precisam ser validados separadamente (`device_tested=false`).
