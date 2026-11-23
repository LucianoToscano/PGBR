# QA4 não corrigiu no aparelho — investigação de origem do disparo e diagnóstico QA5

**Data:** 2026-11-23. **Feedback do usuário:** "Não corrigiu". A QA4 (build `7c8eb42fcf6987c83322dd1f1b3569c4469d11b0`) permanece **não homologada**. Não presumir que o algoritmo passou porque 66 fixtures C++ host passaram. Esta versão é investigativa, não uma nova alegação de correção física.

## Caminhos reais de decisão confirmados em fonte

1. O menu `Control.java::setSmartAttack` persiste um booleano e `Control.smartBattleConfig()` publica `F3|...|smart-attack`. `NativeRuntimeTransport.java::buildSnapshot` escreve o getter em **string slot 21**, enquanto `battle_timing_policy.h::read` exige snapshot coerente/fresco e aceita F3, retornando `smart_attack`.
2. `rocket_combat.cpp::simple_attack_enabled` só vê efetivo ON se já recebeu um snapshot válido com essa opção. Se falhar ou ler F2 (legado), a UI pode estar ON e o executor atuar como OFF. Os testes anteriores verificavam Control/transport/rotina simulada em peças separadas, **não demonstravam o valor efetivo em combate real**.
3. `combat_host.cpp` só encaminha `ChargeAttackDisplayVNext.ObserveBattleState` quando `rocket_gate` e `g_charge_ui.valid`; o binding opcional é resolvido por nomes/tipos exatos `ervu`, `erwh`, `showEffectiveness` e enums `Charge/Charge2`, `Normal/SuperEffective/NotVeryEffective`. As fixtures não comprovam que esse binding resolve na versão do aparelho. Sem ele não há comparação real.
4. **Caminho permissivo remanescente da QA4:** `rocket_combat.cpp::vnext_tick` entrava na política rigorosa apenas se `g_charge_count>=2 || g_smart_seen_pair || g_actor_pair_observed`. Com Smart ON, primeira captura isolada e sem stamp anterior, `wait_for_better=false`, permitindo disparo de Charge por laço original. Esse escape foi corrigido **no código**.
5. `PGR_RC_EVT_SMART_DIAG` anteriormente só era emitido quando `simple_attack_enabled()==true`, sem diferenciação visível entre OFF efetivo e ausência de telemetria. O objeto JSON `smartAttackMode` chegava a `Control.event("battle-action", ...)`, mas **não era interpretado nem exibido pelo menu**. Assim, os relatos em vídeo não permitiam determinar se o carregado saiu por Rocket ON, por Rocket OFF/fallback, por toque manual, ou por outro executor.

## Mudanças QA5

- Com Smart Attack **efetivamente ON**, `rocket_combat.cpp` agora **sempre bloqueia** o carregado automático até ter dois displays tipados e uma admissão válida, inclusive no primeiro ciclo, antes de qualquer carimbo de adversário. `get_IsCharged`, visibilidade, interatividade, melhor categoria e alvo ainda condicionam a autorização. Ágil continua sem mudança. OFF preserva primeiro carregado.
- A cada mudança de status ou no máximo cada cinco segundos enquanto há charged mask ativo, Rocket publica `PGR_RC_EVT_SMART_DIAG` também no estado **OFF efetivo**, evitando que silêncio pareça escolha inteligente.
- `combat_host.cpp` acrescenta ao evento JSON `smartAttackEnabled` (leitura nativa real), `classifierBound` (metadata tipada realmente validada) e `actorIdentity` (`known`/`unknown`/`not-observed`). Nenhum identificador de Pokémon, conta, coordenada ou nome é transmitido. O buffer JSON é dimensionado para não truncar os campos novos.
- `Control.java::event` agora interpreta o JSON e guarda o estado **somente em RAM**. `SettingsPanel.java` exibe **Menu ON/OFF versus Motor ON/OFF**, classificações/estados de Charge/Charge2 e motivo de `hold/selected/original`, além de um botão **Copiar diagnóstico do Ataque Inteligente**. O botão copia apenas esse JSON ou uma descrição sem identificadores.
- O teste de um único Charge capturado mudou: com Smart ON a automação não o libera sem prova de que não existe um segundo. É uma limitação funcional **deliberada** para Pokémon com apenas um carregado; sem identificar de forma segura a quantidade de movimentos, não há como autorizar esse caso.
- A suíte nativa aumenta de **66 para 68 cenários**, garantindo ON bloqueia a captura inicial isolada, OFF mantém execução e evento de diagnóstico OFF é emitido.

## Como interpretar os dados reais

- **Menu ON / Motor OFF:** a preferência F3 não foi consumida como ON pelo Rocket (ou foi revogada no transporte). Investigar snapshot/lease e versão do APK; não ajustar ranking dos golpes.
- **Menu ON / Motor ON / Classificador INDISPONÍVEL:** os campos do display não foram resolvidos; precisa correção de binding validada no jogo, não cálculo por rótulos não comprovados.
- **Menu ON / Motor ON / Classificador OK / Adversário not-observed ou unknown:** o provedor de identidade não validou o estado. Comparação por adversário ainda não é comprovada.
- **Motor ON, decisão hold; mesmo assim golpe foi usado:** examinar outros caminhos de toque (manual, outro executor ou jogo), pois Rocket não autoriza seu próprio `ChargeAttackButtonTapped()` sob hold.
- **Motor ON, decisão selected, carregado errado:** obter `chargedSlot`, `chargeChoice` e confirmar mapeamento de slots/moves; a QA não compara dano absoluto.

**Restrições:** Não há vídeo ou logs internos desta execução QA4 nesta conversa que certifiquem o evento nativo. Esta revisão deve permanecer como **QA para diagnóstico**, não solução homologada. PR #128 draft, `main` intacta. Timing 150/500/600 ms, escudos, trocas e outras modalidades intactos.
