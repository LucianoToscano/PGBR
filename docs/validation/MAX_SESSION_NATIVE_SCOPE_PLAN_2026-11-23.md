# Sessão Max — plano de integração nativa segura (issue #137)

Status: **projeto técnico / não implementado**. Branch de QA PR #136. `device_tested=false`.

## Evidências verificadas

- `pgrazor/native/battles.h` define `PGR_BATTLE_MODE_RAID=0`, `GYM=1`, `MAX=2`. O executor PvE é a feature 19, independente do gate Rocket/PvP.
- `pgrazor/native/battles.cpp` recebe `PGR_BATTLE_OBS_SCOPE_ENTER`, valida a classe do objeto contra o descritor e publica `g_mode_pub`; mantém `g_scope_active`, `g_failed_pub`, `g_scope_serial` e uma geração de política. `battle_clear()` revoga o escopo e incrementa `g_epoch`.
- O executor aplica `g_mask` por modo, com lease máximo de 2500 ms, rechecagem de contexto/epoch antes de mutação, e bloqueio em overflow da fila.
- `AutoMaxBattle` atualmente mantém seleção temporária apenas na camada Java. `Control.battleOptions()` retorna somente opções permanentes; **não** reintroduzir OR global.

## Condições de habilitação (todas obrigatórias)

1. No coordenador `runtime/native-battles.js`, obter prova fresca do `sample()` nativo: `scope_active=1`, `mode=PGR_BATTLE_MODE_MAX`, `failed=0`, `scope_serial` válido e lease ativo. Não usar string de status, tela aparente ou intenção do usuário como prova.
2. Implementar canal temporário de consentimento da sessão Java até o coordenador, com geração/epoch e revogação explícita. **Não usar `battleOptions`**. Conferir assinatura e transporte antes de modificar ABI; preferir contrato independente e versão compatível.
3. No ponto onde o coordenador constrói a máscara PvE, aplicar somente os bits Max correspondentes à seleção, **após** confirmar o escopo Max nativo e a geração. Não aplicar em RAID/GYM e não alterar os bits permanentes de outros modos.
4. Revogar a autorização na saída do escopo, mudança de modo, falha nativa, lease vencido, troca de conta, background, perda de readiness, troca de cena e Stop. Falha de telemetria/transporte = máscara temporária zero.
5. Preservar cadências originais do executor (`QUICK=150ms`, `CHARGED=600ms`, `MAX=700ms`, `SWAP_GRACE=600ms`) e as opções do usuário. Não acelerar escudo ou seleção de próximo Pokémon.
6. Testar política pura e integração negativa: modo desconhecido, RAID, GYM, Rocket, PvP, MAX sem scope, MAX com falha, scope antigo, serial trocado, lease expirado, interrupção e reentrada; verificar que `battleOptions` global nunca recebe bits da sessão.
7. Exigir NDK ARM64, JVM, governança e QA físico em Pokémon GO para habilitar a opção. Registrar capturas/logs sem identificadores pessoais.

## Decisão

A sessão Max permanece **preparatória e sem automação** até que exista o caminho de consentimento temporário, prova nativa e testes negativos. Não confundir o suporte Max já existente na feature 19 com a ativação segura desta nova sessão. Não realizar merge/release com promessa de automação Max funcional.
