# Auditoria do diagnóstico real — Rocket QA5 → QA6

## Evidência recebida

O usuário enviou o JSON exato da versão QA5:

```json
{"smartAttackMode":"training","smartAttackEnabled":true,"classifierBound":true,"actorIdentity":"unknown","chargeCount":3,"decision":"hold","reason":"actor-unknown","chargeTier":"unknown","charge2Tier":"unknown","chargeReady":false,"charge2Ready":false}
```

### Interpretação comprovável

1. O motor Rocket estava ativo para escopo `training` e a preferência F3 **chegou como ON efetivo**; não repetir a hipótese de que o toggle estava OFF.
2. `classifierBound:true` confirma **resolução estática de metadados** do classificador, não necessariamente leitura funcional de cada slot na partida.
3. `actorIdentity:unknown` significa que a função read-only `pgr_smart_battle_vnext_actor_stamp` retornou 0 para esse snapshot. A implementação QA5 não distinguiu bootstrap ausente, busy, ator local ausente, Pokémon local não identificado, coleção de adversários ausente/vazia, Pokémon adversário sem ID ou exceção de leitura.
4. `chargeCount:3` é **quantidade de objetos ChargeAttackDisplayVNext capturados** (até 4), não quantidade real de movimentos. A lista pode conter duas referências para o mesmo slot, mas o JSON não prova isso.
5. `decision:hold` e `reason:actor-unknown` indicam que **nenhum botão carregado foi autorizado naquele tick pelo motor Rocket**. Não demonstram se houve clique manual, outro executor ou ação em tick diferente.
6. Na fonte QA5, a condição `g_actor_pair_stamp>0` cercava toda a consulta aos getters dos botões. Quando o ator estava desconhecido, `chargeTier:unknown` e `chargeReady:false` eram **valores não medidos**, não um resultado real de classificação/prontidão. Este foi um defeito de diagnóstico confirmado pela leitura da fonte.

## Alterações investigativas QA6

- `smart_battle.cpp`: nova `pgr_smart_battle_vnext_actor_probe(state,&reason)`, mesma identidade tipada de `get_LocalActor/ActiveOpponents/ActivePokemonId`, mas devolve um estágio anônimo de falha. A função antiga continua chamando a nova com `reason=nullptr`. Nenhum ID de Pokémon, conta ou treinador é emitido.
- `combat_host.cpp`: envia `PGR_RC_OBS_ACTOR_PROBE` com o estágio (0..10), antes do `PGR_RC_OBS_ACTOR_PAIR`, e mantém os eventos tipados de `PGR_RC_OBS_CHARGE`.
- `rocket_combat.cpp`: se existirem os **dois slots tipados**, consulta os getters somente de leitura `get_IsCharged/get_IsVisible/IsInteractable` e os níveis de eficácia **mesmo quando o adversário não foi identificado**. Esta inspeção **não autoriza** clique se o ator estiver explicitamente desconhecido; `decision=hold/reason=actor-unknown` permanece obrigatório nesse caso.
- Telemetria acrescenta `actorProbe` (ex.: `opponent-actor-missing`, `metadata-exception`) e `chargeSeen/charge2Seen`. O JSON anterior continua compatível. A UI não indica `carregando` quando o botão simplesmente não foi observado.
- `chargeCount:3` continua aceito como lista de objetos; o executor agrupa por `Slot::FIRST/SECOND` e usa a observação mais recente por slot. **Não presumir que o terceiro objeto é erro ou terceiro ataque real.**
- Suíte Rocket ampliada para **70 cenários**; o cenário 68 replica três objetos, ator desconhecido, duas categorias conhecidas e os dois botões carregados: assertiva `hold`, ausência de `ChargeAttackButtonTapped`, prontidão/tiers reportados e estágio de falha preservado. Cenário 69 valida que ator confirmado mantém autorização do melhor golpe. [Host QA](https://github.com/LucianoToscano/PGBR/actions/runs/45109853225) terminou `success`.

### Limites restantes

O erro exato de `actorIdentity:unknown` **ainda não pode ser determinado apenas do JSON QA5**. O campo novo `actorProbe` da QA6 foi introduzido para determinar se o problema vem do layout do VNext no treinamento ou de outro caminho. A QA6 não adiciona classificação por dano absoluto e não altera as cadências Ágil 150 ms, Carregado Legacy 500 ms e Carregado VNext 600 ms. Não é prova de comportamento em aparelho. `device_tested=false`.
