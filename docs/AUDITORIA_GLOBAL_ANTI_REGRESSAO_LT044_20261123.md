# Auditoria de integridade PGBR pós-LT043 — 23/11/2026

**Escopo:** código `main` em `273b6aed128e6bbb315af47c2f58fd6ec33dcf5d`, pré-release LT043 publicada, ramo LT044 em trabalho paralelo (#148), histórico PR #128/#135/#136/#140/#141/#146 e forks públicos. Esta auditoria é **estática + testes automatizados**; `device_tested=false` e nenhuma afirmação de eficácia em partida real.

## Método / anti-downgrade

1. Consultados `AGENTS.md`, `.github/copilot-instructions.md`, `PGBR_ARQUIVO_MESTRE_UNICO.md`, documentação de build/release e GitHub API atual.
2. Verificado que `main` contém merge #141 (Rocket QA6 + Pokematrix/Max/HUD/IV + importação de dados PGSharp) e merge #146 (PT-BR + validação defensiva GPS). Tag `v0.00.43-LT` é QA publicada e **não contém** #146. O branch antigo #140 está incorporado ancestralmente: **não reexecutar cherry-pick da fonte antiga**.
3. Comparação `v0.00.43-LT...main`: 61 commits adiante, zero atrás, 23 arquivos alterados; o delta principal é PT-BR/GPS/testes/documentação, sem arquivos removidos. Isso comprova continuidade de árvore, **não** ausência de bugs de execução.
4. Preservados `Control.battleOptions()` sem OR de sessão Max, a distinção Rocket/PvP frente a PvE, `rocket_actions.cpp`, cadências, lógica de assinatura e o certificado do Actions. Outras branches não foram reescritas.

## Falhas / regressões identificadas

| ID | Severidade | Achado | Ação |
|---|---|---|---|
| ATK-LT044-01 | Alta | O `admit_display` da QA6 segurava **qualquer** carregado tipado pronto se outro de melhor eficácia ainda carregava. Isso atrasa ataque que o usuário pediu imediato e contradiz a prioridade simples `Super > Normal > Pouco eficaz` *entre disponíveis*. | Corrigido no ramo: com dois slots tipados, lote coerente e dados frescos, se apenas um está pronto e sua eficácia foi confirmada, autorizar somente esse; com ambos prontos, escolher maior tier. |
| ATK-LT044-02 | Alta | Em QA5/QA6 o treinamento já registrou `actorIdentity:unknown`. O motor nega carregados automáticos sem prova de adversário e pode continuar no ágil. | **Não inventar ponte de ator**: preservar bloqueio e diagnósticos `actorProbe`, exigir coleta real em QA. Não registrar como solucionado. |
| ATK-LT044-03 | Alta | Sem classificador/binding `Charge/Charge2`, em combate VNext o motor não consegue provar qual botão é super eficaz. | Preservar proteção que rejeita metadata ausente, vencida, batch divergente, slot duplicado ou alvo desconhecido; testes negativos obrigatórios. |
| ATK-LT044-04 | Média | A descrição da UI ainda prometia esperar o golpe superior mesmo com outro tipado pronto. | Texto alterado para explicar disparo pronto, hierarquia e limitações. |
| INT-LT044-05 | Alta | A PR #140 e a PR #139 permanecem abertas apesar de a PR #141 já ter incorporado a fonte funcional na `main`. | Marcar como trabalho historicamente coberto, evitar reaplicação e preservá-las para fechamento rastreado; não sobrescrever branches. |
| FEED-LT044-06 | Alta | Fontes de Sniper/Radar e APIs externas não estão homologadas para uso mundial nem presentes na árvore principal. | Não ativar coordenadas falsas, scraping sem autorização ou filtrar UNKNOWN como zero. Revisão separada das PRs #115/#118/#121 e #108/#111/#114. |
| ROOT-LT044-07 | Alta | O patch Savant (#135) acrescenta proteção Root/VsSeeker mas tem conflitos com `combat_host.cpp` integrado da QA6. | Reconciliar separadamente com tests de compatibilidade; **não mesclar a cópia inteira**. |
| MAX-LT044-08 | Alta | A sessão Max tem gate de escopo planejado mas não possui ponte de consentimento até o executor. | Manter preparatória/fail-closed; não anunciar ataques Max funcionais. |

## Prioridade corrigida nesta branch

- **Dois carregados prontos e classificados:** escolher `SUPER` antes de `NEUTRAL` antes de `RESISTED`; em empate manter slot `Charge` confirmado.
- **Um pronto, outro ainda carregando:** usar o pronto **sem aguardar** (somente se já existirem os dois displays com identidade/tier/frescura comprovados; sem fabricar Normal para UNKNOWN).
- **Nenhum pronto:** ataque ágil permanece; sem mudança de cadência.
- **Classificador ausente/ator desconhecido/tela antiga:** não pressionar um carregado incerto para parecer que houve priorização. A entrada manual continua disponível.
- O caminho é o executor compartilhado Rocket/VNext para Rocket, PvP, duelo e treino quando tipados. `battles.cpp` de reide/ginásio/Max **não** passa a usar a política automaticamente.

Arquivos de correção: `pgrazor/native/smart_attack_priority.h`, `pgrazor/native/smart_attack_priority_test.cpp`, `pgrazor/native/rocket_combat_test.cpp`, `pgrazor/src/dev/pgrazor/SettingsPanel.java`. Casos adicionais cobrem par pronto, pronto sozinho, adversário/label desconhecido e ausência de prontidão. Nenhuma mudança de ABI/hook/temporizador.

## Forks / upstream — fotografia datada

| Origem | Commit observado | Incluído no PGBR main | Lacuna / decisão |
|---|---|---|---|
| jpitty03/PGRazor | `084d700126` (23/11/26), último release `v0.3.6` (21/11) | O patch ART ARM64 `tramp_size >=16` e fixture de encadeamento já existem em `pgrazor/art/CMakeLists.txt` e `pgrazor/art/tests/hook_chain_test.cpp`. | Não duplicar. Último commit `0bae6ebc6` evolui serviço `tools/hundo-feed` (PoGoCoordinates exact/map/token, limites, cotas); o PGBR possui uma cópia anterior. Servidor externo depende de credenciais, cotas, direitos e acesso comprovados; modernização por PR isolada, sem acoplar feed falso ao APK. |
| hugoalves6/PGRazor_Pokematrix | `de777d2b9` (22/11/26) | Duplo toque Caminhar/Teleporte, badge compacto Buddy, ícone de ginásio opt-in e ordem do painel IV integrados via #130/#132/#141. | **Ainda ausentes:** VGP+ Catch only/Candy por etiquetas, critérios de raro e fruta; TM rápida/carregada na tela do Pokémon, regras de fruta da captura manual, alguns divisores/ajustes de menu, VGP text switch e relógio compacto. PRs/issue #133/#134 rastreiam partes. Não copiar IDs de item/assets sem provar proveniência. |
| dojistarman/PGRazor-Savant | `4da955651` (21/11/26) | PGBR já tem sua própria política Rocket/PvP tipada com prioridade (não é simples primeiro pronto do Savant). | Guard de coexistência Root e sinal adicional VsSeeker em PR #135; ainda não integrado porque modifica `combat_host.cpp` e precisa conciliar QA6 + testar dispositivo. Não mudar ID/signature Root. |
| belalsalah74/PGRazor | `10c523009` (21/11/26) | Sessão Max foi adaptada na #131/#136, mas mantida sem novo ataque automático por falta de escopo nativo comprovado; workflow de **APK preview** do fork não é jogo completo. | Não ativar OR global de batalha nem distribuir preview como APK funcional. |

## Repositórios/PRs paralelos sem perda

- **Na main:** #141 e #146, PT-BR (`Teleporte`), GPS de input defensivo, Rocket QA6, mapa/HUD/IV, importação **local** PGSharp, ART ARM64, preservação dos bancos e versão LT043 QA.
- **Aguardando revisão individual:** #148 (build LT044 PT-BR/GPS, CI e APK QA em andamento), #135 (Savant/Root/VsSeeker), #115/#118/#121 (Snipers/Partner), #108/#111/#114 (Radar/feeds), #133/#134 (itens do Pokematrix), fluxos LT032 históricos de joystick/eventos, privacidade visual ainda sem metadata validada.
- A importação de filtros pessoais PGSharp é **armazenamento local**; não torna uma fonte de coordenadas funcional. O PokeSearch e outros repositórios permanecem intocados.

## Plano de verificação e publicação

1. Testes de política C++ host e fixture vinculada Rocket (incluindo casos 36 e outros 70), Java Control/backup, NDK ARM64, website e governança; validar que CI não retirou testes legados. Nenhuma aprovação presumida antes do workflow.
2. Conferir `main` antes de merge desta branch e da PR #148. Não sobrescrever mudanças posteriores; separar versão de build do patch de combate. Evitar versões/release simultâneas com tag igual.
3. Produzir APK sem Root, instalador Root e módulo Zygisk a partir de um mesmo commit **aprovado** com entrada ARM64 fixada `0.437.0`, assinatura existente, `verify_release.py`, SHA-256 e `device_tested=false`. Não reutilizar LT043 nem renomear APK antigo.
4. Validar no Android, comparando em Rocket, treino/PvP, contra adversário do tipo estável e após KO/troca, com dois carregados, um carregado, ambos prontos, classificador OFF e ator UNKNOWN. Anexar diagnóstico anônimo; sem isso o APK continua pré-release QA.

**Status deste arquivo:** implementações da branch já feitas, integrações/CI/merge e publicação ainda sujeitos à conferência posterior. Não anunciar todos os forks como portados.
