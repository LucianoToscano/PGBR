# PGBR — integração inicial da prioridade simples por eficácia VNext

Data: 22/11/2026. Branch: `fix/ataque-vnext-pos-observacao-20261122`.
Base: PR #117 (`b7306354a531fc8acf71868e4c87748a0af13d1b`), com o motor e a configuração F3; a PR #117 **não foi mesclada**. A `main` avançou independentemente pela PR #122; não sobrescrever essas alterações.

## Problema

`ChargeAttackDisplayVNext.ObserveBattleState` era encaminhado aos executores antes do método original. A classificação exibida poderia ainda pertencer ao adversário anterior. Os executores escolhiam o primeiro carregado pronto, sem considerar eficácia; o `smartAttack` F3 e a política simples C++ existiam mas não eram consumidos.

## Alterações de código

- `combat_host.cpp`: observa depois do método original, preserva argumentos pinados e revalida owner/scheduler. Com o metadata compatível, lê a eficácia do **mesmo objeto** `ChargeAttackDisplayVNext` e sua enumeração `Charge/Charge2`; em falha de leitura envia `aux=0`.
- `charge_effect_ui.h`: resolvedor opcional dos campos `ervu` (MoveType), `erwh` (eficácia) e `showEffectiveness`; valida classes, tipos, enumerações e valores literais em runtime antes de usar. Ausência de indicação, `showEffectiveness=false` ou categoria desconhecida não significam neutralidade.
- `rocket_combat.cpp`: mantém amostras ligadas às referências **enraizadas** dos displays e expira após 200 ms; compara apenas quando há exatamente dois candidatos prontos, com posições distintas Charge/Charge2, classificações tipadas e token da mesma observação. No empate escolhe slot Charge. Quando ON usa super eficaz > neutro > resistido; OFF, um golpe só, amostra antiga, classificação ausente ou token discordante preservam o primeiro pronto. As verificações originais de lease, ticket de feature 20, prontidão, liveness, interactable e cadências permanecem.
- `smart_attack_priority.h` e fixtures: novo seletor de displays e cenários de integridade. O executor Rocket VNext é exercitado com objetos simulados (segundo super eficaz, OFF, token divergente, só um pronto, empate com captura invertida).
- Não se alteraram os caminhos Legacy, Raid/Ginásio/Max separados, temporizadores, troca, escudos, Cura Inteligente, PokeSearch, recursos visuais ou menus.

## Limites de evidência

A associação `MoveType`–display agora é inferida por metadados validados em runtime, **não** por ordem de captura, MoveId ou pixels. O token obtido de `BattleState` é uma pista de coerência de atualização e **não certifica a identidade do adversário** ou uma reclassificação concluída. O campo `showEffectiveness` pode permanecer false mesmo com valor normal. Uma troca/KO pode não redefinir esses campos no mesmo frame; por isso o fallback é obrigatório. Em caso de runtime incompatível, o seletor adicional fica inativo. Este mecanismo só opera no executor compartilhado Rocket/PvP/Treinamento que já possua um escopo de combate válido e dois displays VNext, não automaticamente nos executores PvE ou Legacy.

**Não declarar eficácia selecionada em partida real** antes de testar a versão Pokémon GO 0.437.0 ARM64 em dispositivo, incluindo Rocket, PvP e treino, duas opções visíveis, troca de oponente, KO e root/não root. Testes de host/CI comprovam apenas comportamento em simulação e compilação de código. Nenhum APK ou release deve ser denominado "Ataque Inteligente homologado" sem essa prova.

Documentação de origem na PR #117: `docs/AUDITORIA_BATALHA_CURA_2026-11-22.md`. Issue de acompanhamento: #90.
