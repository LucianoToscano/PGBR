# Auditoria PGBR — integridade, regressões e Ataque Inteligente simples (22/11/2026)

**Estado:** revisão estática e correção de política Java; **não** é homologação de gameplay, build de APK nem aprovação da funcionalidade completa.

## Base e método

- Repositório: `LucianoToscano/PGBR`; base imutável desta branch: `b355def7f6693b4d7f88c802b6ea40df018f0490`.
- Instruções consultadas: `AGENTS.md`, Arquivo Mestre canônico, `README.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md`, `docs/GAME_UPDATES.md`, `THIRD_PARTY.md`.
- Revisados: histórico de releases e tags `v0.00.39-LT` e `v0.00.40-LT`, diff até `main`, PRs abertas, código de UI/controle, motor C++, política PvP Java, transporte, executores Rocket/PvE e testes.
- `v0.00.40-LT` (`57f6789b`) é ancestral da main examinada: 16 commits à frente, 0 atrás. O diff de GitHub lista **nenhum arquivo removido** entre essa tag e a main. Isso exclui um downgrade de ancestralidade/versão nesse intervalo, **não** prova ausência de regressão funcional.
- A comparação `v0.00.39-LT` → `v0.00.40-LT` é linear (57 commits, zero divergência); a mudança de reconhecimento PvP/duelos é documentada e respaldada por testes de escopo, mas a issue #105 continua aberta. Não há ensaio físico novo de comportamento.
- PR #116 está na main, com o motor de recomendação sem executor; PR #117 permanece *draft* com F3 e política nativa simples ainda sem consumo dos executores. Outras PRs simultâneas permanecem intocadas.

## Matriz de achados

| ID | Severidade | Área | Evidência | Estado |
| --- | --- | --- | --- | --- |
| ATK-01 | Crítica | Execução do Ataque Inteligente | `smart_attack_decision.h` só é consumido por testes; `rocket_combat.cpp` e `battles.cpp` percorrem o primeiro candidato válido | **Aberto**, issue #90 |
| ATK-02 | Crítica | Identidade dos dois golpes | Falta prova runtime de eficácia atual associada a cada botão e sessão; PR #117 contém política sintética, não adaptador | **Aberto** |
| ATK-03 | Alta | Política antiga Java | `PvpChargedPreference.select()` podia retornar `WAIT` mesmo com um carregado pronto para aguardar golpe superior (até 5,5 s no controlador) | **Corrigido nesta branch, somente política** |
| ATK-04 | Média | Política de empate | Java antiga favorecia ganho por potência/STAB ou maior resistência dentro da mesma categoria, divergindo da prioridade simplificada do usuário | **Corrigido nesta branch:** empate usa primeiro pronto |
| ATK-05 | Média | Um só carregado conhecido | Se `secondMove==null`, a política antiga devolvia `UNKNOWN` sempre | **Corrigido nesta branch:** prioriza primeiro pronto |
| ATK-06 | Alta | PvP/duelos | Classificação de escopo VNext/Legacy é experimental; issue #105 relata perda ao mudar janelas | **Em aberto**, não alterado |
| ATK-07 | Alta | Versões instaláveis | A tag publicada LT040 antecede as mudanças de PR #116 e #117. Não há release posterior para esta funcionalidade | **Não gerar release afirmando seleção pronta** |
| ATK-08 | Alta | Screenshots de batalha | Indicador “SUPEREFICAZ”/“NÃO É MUITO EFICAZ” é visível no botão; mensagem após dano é distinta. Campos estáticos candidatos podem ser antigos/não expostos no runtime | **Não comprova conexão segura** |

## Correção implementada

Na política pura Java PvP, trocar espera por comparação imediata quando há carregado disponível:

1. Em dois carregados prontos, escolher **super eficaz > neutro > resistido**.
2. Em empate de categoria, escolher o primeiro controle confirmado, sem comparação de STAB, dano ou energia para desempate.
3. Com apenas um carregado pronto, usá-lo imediatamente, sem tentar carregar alternativa melhor; com nenhum pronto, `WAIT` significa manter o caminho de ataque ágil, não bloquear ações.
4. Aceitar um único golpe `firstMove` conhecido com `secondMove == null`.
5. Preservar método e parâmetros existentes, regras de modalidade, validade e energia; **nenhuma mudança nos temporizadores nativos, escudos, trocas ou executor**.

Caminhos modificados: `pgrazor/src/dev/pgrazor/PvpChargedPreference.java` e `pgrazor/test/PvpChargedPreferenceTest.java`. Foram adicionados nove casos de categoria (3x3), uso instantâneo de golpe pronto, empate, golpe único, ambos indisponíveis e consumo duplicado.

**Limitação primordial:** `PvpBattleController` é política Java desacoplada do executor real. Os testes demonstram decisão no módulo, não execução em Pokémon GO. O motor C++ complexo permanece preservado e a política simples C++ da PR #117 continua isolada. Não confundir o ajuste Java com a conclusão da issue #90.

## Validação e publicação

- Histórico e árvore da main inspecionados; não foi feita exclusão, reset, merge, mudança de versão ou alteração em PR de terceiros.
- A aprovação do CI desta branch deve ser conferida na PR antes de qualquer integração; até existir, testes não devem ser declarados aprovados.
- SDK/NDK Android, assinatura da aplicação e dispositivo real não estão disponíveis nesta sessão de edição. A existência de entrada privada pinada na release `build-inputs` é comprovada, mas não constitui APK compilado por esta revisão.
- Não alterar a release `v0.00.40-LT`, os hashes ou assets históricos. Para uma nova release após integração funcional e homologação, criar workflow/versionamento próprios; nunca reutilizar a tag antiga.

## Critério de aceitação pendente

É necessário um adaptador validado que associe **slot visual ↔ indicador de eficácia ↔ botão vivo**, observe geração/sessão/adversário e consumo do carregado e revalide antes da ação. O texto visual ausente não é prova de categoria neutra. Repetir para os diferentes controladores somente com seus próprios contratos; sem isso a regra simples permanece uma recomendação, não um toque inteligente real.

Referências: [issue #90](https://github.com/LucianoToscano/PGBR/issues/90), [issue #105](https://github.com/LucianoToscano/PGBR/issues/105), [PR #116](https://github.com/LucianoToscano/PGBR/pull/116), [PR #117](https://github.com/LucianoToscano/PGBR/pull/117), [LT040](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.40-LT).
