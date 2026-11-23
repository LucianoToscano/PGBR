# Auditoria — seleção de carregados após troca de adversário (Rocket QA4)

**Data:** 2026-11-23 (America/Fortaleza). **Evidência:** vídeo `Screen_Recording_20261122_235941.mp4` de aproximadamente 61,6 s, anexado pelo usuário. **Escopo:** Equipe GO Rocket e modalidades tipadas que compartilham o motor `rocket_combat.cpp`, especialmente treinamento com líder.

## Evidências e limites

- O vídeo mostra Toxtricity contra Sneasel, depois Sealeo e Vaporeon; `Ataque Veloz` foi utilizado por volta da primeira troca, enquanto `Ataque Selvagem` aparece como `SUPEREFICAZ` contra Sealeo quando a interface já está estabilizada. Depois `Ataque Selvagem` é utilizado corretamente.
- Os frames demonstram qual golpe foi aplicado, mas não expõem `chargeChoice`, `smartAttackMode`, nem distinguem de forma conclusiva acionamento manual de automático. O vídeo também não prova qual binário foi instalado.
- Hipótese sustentada pela fonte QA3: o ramo `g_charge_count>=2 || g_smart_seen_pair` permitia retornar ao primeiro carregado quando o novo VNext GUI havia capturado apenas um botão, sem distinguir adversário novo de Pokémon com um único golpe.

## Verificações de arquitetura

- `smart_battle.cpp` já usa `BattleState.get_LocalActor`, `ActiveOpponents` e `ActorProto.ActivePokemonId` para identificar o Pokémon ativo e o alvo vivo ao avaliar troca estratégica.
- A atualização dos displays `ChargeAttackDisplayVNext.ObserveBattleState` passa por `combat_host.cpp::hook_shared_vn_observe` (após a chamada original) e segue para o **mesmo** `rocket_combat.cpp` que executa ações de Rocket, treinamento, duelo de amigos e GBL quando os escopos são classificados com segurança.
- O `state_batch_token` vindo do ponteiro de `BattleState` é somente uma indicação de lote, **não** identifica o adversário; sua igualdade não pode manter dados de eficácia após troca.

## Correção aplicada na PR #128

- `smart_battle.cpp` expõe a leitura **somente de identificação**, independente da preferência Batalha Inteligente: `pgr_smart_battle_vnext_actor_stamp(state)`. Usa os campos tipados já usados pela própria Batalha Inteligente. Retorna token de comparação do par `active_id + opponent_id`, com 31 bits, **somente em memória**, sem expor IDs de conta, Pokémon, posição ou dados de instância na telemetria. `0` significa `UNKNOWN`. Uma colisão criptográfica não é excluída formalmente; o token não é identidade/autenticação externa.
- A tentativa adicional `pgr_smart_battle_vnext_gui_actor_stamp(gui)` lê o estado pelo `IBattleDirector` do GUI após seu Update, **quando esse layout existe**; falhas não presumem identidade. A leitura principal permanece o `BattleState` fornecido ao `ObserveBattleState`.
- `combat_host.cpp` encaminha `PGR_RC_OBS_ACTOR_PAIR` **antes de** `PGR_RC_OBS_CHARGE` na atualização dos displays. Um par novo ou ausente invalida imediatamente todas as classificações antigas no Rocket.
- Com Ataque Inteligente **ON**, ao observar um novo par, o Rocket guarda `g_smart_seen_pair=true` para negar a autorização de carregado com apenas um display recapturado. Exige dois slots tipados e observados após a mudança, com mesmo lote e leituras até 200 ms, antes de reautorizar o melhor carregado pelo método original `ChargeAttackButtonTapped()`. Sem alvo conhecido, recusa carregados automáticos até a identificação restabelecida; o Ataque Ágil ainda pode continuar.
- O modo **OFF** preserva o comportamento original, e toques manuais continuam livres. Para uma batalha com um único ataque carregado e Ataque Inteligente ON, se o provedor de par de atores estiver ativo, a nova trava pode impedir ataques carregados automáticos; esta é uma limitação conhecida até que a quantidade real de movimentos seja verificada pelo catálogo vivo. Não simular uma contagem de movimentos.
- O algoritmo ainda prioriza pela categoria `SuperEffective > Normal > NotVeryEffective` quando verificada. `smart_battle.cpp` também tem dados Game Master de energia, STAB e dano estimado, e `smart_attack_decision.h` dispõe de um comparador, **mas não há correspondência verificada entre `move2/move3` do catálogo e o objeto `Charge/Charge2` vivo**. Não autorizar nem rotular cálculo de dano absoluto nesta QA.
- Não foram alteradas cadências Rocket de Ágil 150 ms e Carregado VNext 600 ms / Legacy 500 ms, Escudo, Troca, Raid, Ginásio e Batalha Max.

## Testes

- A suíte sintética Rocket foi ampliada de **62 para 66 cenários**; adicionou transição de Sneasel→Sealeo com somente Charge observado primeiro, invalidar decisão no adversário UNKNOWN, rearmar ao identificar dois novos displays e preservar modo OFF.
- A primeira versão do teste introduziu um toque carregado antes de transcorrer a cadência do Ágil, causando falha do cenário 62. Foi corrigida **a simulação**, mantendo os intervalos de produção intactos. Ambas as amostras são atualizadas antes de selecionar Charge2.
- [GitHub Actions Rocket QA #45104638975](https://github.com/LucianoToscano/PGBR/actions/runs/45104638975) executou todos os 66 casos e terminou `completed/success`.
- **Estado de validação:** simulação host aprovada, execução em aparelho não realizada (`device_tested=false`). A publicação de APK QA4 e integridade dos artefatos são passos independentes deste registro.
