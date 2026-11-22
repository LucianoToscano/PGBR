# Auditoria de batalha e contrato do motor independente — 22/11/2026

Base: `main` `dc3f928e7bb4d8da9363ee45db3105bb8411e90c`; branch `arena/be6b0894-pgbr`. Escopo: inspeção de código e testes da linha LT040; não é auditoria exaustiva de todos os aparelhos/modos. PRs abertas relevantes consultadas: [#86](https://github.com/LucianoToscano/PGBR/pull/86), histórica, e [#106](https://github.com/LucianoToscano/PGBR/pull/106), diagnóstico. [#114](https://github.com/LucianoToscano/PGBR/pull/114) e [#115](https://github.com/LucianoToscano/PGBR/pull/115) são trabalhos paralelos de radar/sniper. Não incorporar nenhuma delas sem revisão. CI da main na base: [45056221730](https://github.com/LucianoToscano/PGBR/actions/runs/45056221730), success; não é CI desta alteração.

## Diagnóstico por recurso

| Área | Evidência e limite |
|---|---|
| Batalha Inteligente | `smart_battle.cpp` e `battle_counter.h` fazem seleção de equipe e troca em fluxos específicos; `SettingsPanel.java` limita explicitamente suporte. Não comprova suporte universal ou resultado físico. |
| Ataque Inteligente | `Control.java` armazena `smartAttack` e espelha bit 8 em `pvpBattleOptions`, sem alterar bits de ação 1/2/4. `PvpChargedPreference.java` e `PvpBattleController.java` têm política Java, mas não têm ligação comprovada ao executor. `rocket_combat.cpp` escolhe o primeiro carregado pronto. `battles.cpp` é executor separado. [Issue #90](https://github.com/LucianoToscano/PGBR/issues/90) continua aberta. O motor novo abaixo não altera o jogo. |
| Escudo e Troca Inteligentes | `battle_timing_policy.h`, `rocket_combat.cpp` e `rocket_swap.cpp` implementam preferências e prioridade à escolha manual em fluxos reconhecidos; `smart_battle.cpp` tem troca estratégica separada. Não houve alteração dos valores ou execução física nesta revisão. |
| Cura Inteligente | Controles em `Control.java`/`SettingsPanel.java` e código nativo; interface indica mapa/pós-batalha, não cura durante combate. Eficácia real e consumo de itens não foram testados. |
| Ágeis e carregados | `rocket_actions.cpp` preserva cadências técnicas; o nativo verifica estado e escopo. Invocação não comprova ação aceita. [Relato anterior](../PGBR_ARQUIVO_MESTRE_UNICO.md) de LT037 é limitado ao cenário informado pelo usuário. |
| Modalidades | `pvp_scene.h`, `battle_target_policy.h` e `shared_auto_battle_gate.h` fazem classificação/lease restritos; Rocket/PvP/treino não são o mesmo executor de Raid/Gym/Max. VNext-only e duelo físico ainda exigem investigação ([#105](https://github.com/LucianoToscano/PGBR/issues/105)). |
| Persistência | `Control` salva/restaura o toggle e temporizadores; desativar o toggle preserva ações compartilhadas. **Defeito verificado:** `SettingsBackupRules.SETTINGS` omitia `smartAttack`, `smartShieldTiming`, `smartFaintedSwapTiming`, logo exportação/restore de backup não transferia essas escolhas. Corrigido com validação estrita; arquivos antigos sem as chaves continuam aceitos e o restore mantém as escolhas locais ausentes do backup. |
| Estratégias e UI | UI possui interruptor e descreve que a seleção nativa ainda não está integrada; controles de tempos mostram estado via getters. Ordem, layout e atualização real no Android não foram homologados nesta revisão. Estratégia do motor independente é deliberadamente distinta da política PvP antiga e não foi conectada a ela. |

Tempos LT040 mantidos: escudo 0/1500/3000 ms; troca por nocaute VNext 400/2000/6000 ms; troca estratégica 2300 ms; cadências técnicas de `rocket_actions.cpp` inalteradas. Testes fonte e host não demonstram temporização em Android.

## Contrato do motor de recomendação

`pgrazor/native/smart_attack_decision.h` é C++ puro, sem hooks, offsets, I/O, estado global, acesso a cliente ou rede; usa a tabela de 18 tipos já presente em `battle_counter.h` (multiplicadores GO 1,6 / 0,625 / 0,390625 por tipo). O chamador fornece **snapshot completo e coerente**:

- IDs positivos do ativo e adversário; primeiro tipo de cada um obrigatório (1–18, ordem `HoloPokemonType` do cabeçalho citado), segundo 0 apenas quando **conhecidamente** ausente. Tipos duplicados são inválidos.
- Um a três golpes: no máximo um ágil e dois carregados; IDs únicos positivos, tipo, potência base específica da modalidade, custo de energia positivo para carregado ou ganho positivo para ágil, e disponibilidade explícita `READY`, `NOT_READY` ou `UNKNOWN`.
- Energia atual, limite da modalidade, multiplicador STAB explícito (por exemplo 1,2) e flag `enabled`. Valores ausentes, inválidos, duplicados, não finitos ou incompletos rejeitam o snapshot inteiro; não há inferência a partir de nomes/IDs.
- `decide(snapshot)` retorna `DISABLED`, `INVALID_DATA`, `NO_READY_MOVE` ou `RECOMMENDED`; campos `ideal` e `recommended` incluem ID, tipo de ação, efetividade, STAB, potência ajustada, eficiência e razão de bloqueio. `reason` distingue ideal pronto e alternativa pronta. Sem golpe disponível, ID recomendado é 0; estado desconhecido **não** é considerado pronto.

Critério determinístico: carregados antes de ágil; maior multiplicador de efetividade, depois potência ajustada por STAB e efetividade dividida pelo custo, depois potência ajustada e menor ID. Ágil é alternativa quando nenhum carregado estiver pronto; se não há carregados cadastrados, é o ideal. Uma recomendação alternativa é devolvida imediatamente quando o ideal estiver indisponível ou sem energia; **não** é uma ordem de execução, nem promessa de esperar energia. A potência ajustada é um indicador relativo, **não dano real**, DPS ou simulador completo: não inclui stats, duração, buffs, clima, escudos, prioridade, HP ou chance de nocaute. Recalcular a cada snapshot, inclusive troca de adversário; não há cache. Dados de espécie, golpes, energia e regras não são obtidos automaticamente pelo motor.

## Integração futura necessária (não implementada)

1. Provedor autorizado e versionado de dados de espécies/formas/tipos, potência e custos por modalidade; verificar procedência e atualização do catálogo.
2. Observação validada do Pokémon ativo, adversário atual, energia, prontidão de cada golpe e correspondência estável entre ID e botão **em cada modalidade**. Sem isso não inferir ordem visual nem efetividade por texto.
3. Capturar tudo na mesma geração/sessão com expiração, revogar em troca/saída, verificar novamente prontidão antes de agir; descartar dados incompletos, contraditórios ou antigos. A política não valida timestamp por conta própria.
4. Integrar opção persistida com adaptadores próprios para Rocket/PvP/treino e, separadamente, Raid/Gym/Max, mantendo gates, preferências independentes, prioridade manual e comportamento anterior em caso de falha. Decisão pura não habilita ação.
5. Compilar APK e validar em Android autorizado, com identificação de qual botão realmente foi acionado, dano/escudo aceitos e regressões por modo; verificar UI, backup, Root/sem Root e versões do jogo. Sem essa prova, `device_tested=false` e não anunciar controle de batalha real.

## Testes

- `python3 -B tools/native_host_tests.py`: 16 fixtures host aprovadas, incluindo `smart_attack_decision_test.cpp`, tempo, cena, gate, 33 cenários Rocket e executor separado Raid/Gym/Max. G++ em Linux; não é NDK nem Android.
- Testes do motor cobrem super/neutro/resistido, dupla resistência e dupla tipagem, STAB, custo/eficiência, energia insuficiente, ambos/um/nenhum prontos, disponibilidade desconhecida, troca de adversário, dados inválidos, ON/OFF e fallback ágil.
- `python3 -B pgrazor/test/test_battle_timing_controls.py`, `test_pvp_settings_ui.py`, `test_pvp_trace_bootstrap.py` e `python3 -B -m unittest discover -s pgrazor/test -p 'test_lt037_battle_allowlist.py'`: passaram; os três primeiros são contratos de fonte, último 2 testes.
- Adicionados testes JVM de validação do backup e cold start ON/OFF em `SettingsBackupRulesTest.java`/`ControlSettingsTest.java`; **não executados localmente** por falta de JDK/SDK Android.
- `python3 -B tools/check.py`: preflight bloqueado pela ausência de `javac`, `java` e plataforma/fontes Android SDK 36. Não houve APK, NDK, instalação ou teste físico. Consultar os checks da PR para resultados posteriores; nunca substituí-los por estes resultados host.
