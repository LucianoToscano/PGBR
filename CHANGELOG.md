## PGBR LT044 (v0.00.44-LT) — candidata QA pós-LT043 (23/11/2026)

- PT-BR das telas dos chats paralelos e terminologia Teleporte (#101), preservando novas mensagens de rollback de backup da LT043.
- GPS defensivo: valores sintéticos fora da faixa e não finitos rejeitados sem bloquear o callback original; teste com retorno 360° para 0° (#109).
- **Ataque Inteligente Rocket/VNext (PR #150):** o carregado classificado já pronto é acionado sem esperar outro; se ambos estão prontos, usa Super eficaz > Normal > Pouco eficaz. Mantidos os bloqueios para metadata e ator desconhecidos, os tempos, o Max preparatório, a assinatura e o PokeSearch. QA `device_tested=false`, aguardando a validação de build; Snipers, radar e Savant continuam pendentes.

## PGBR 0.00.43-LT — integração QA consolidada (23/11/2026)

- **Publicação oficial validada:** [release LT043](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.43-LT), [APK sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.43-LT/PGBR-v0.00.43-LT.apk), Root e Zygisk com hashes SHA-256 no [Mestre](PGBR_ARQUIVO_MESTRE_UNICO.md); [CI #45280739464](https://github.com/LucianoToscano/PGBR/actions/runs/45280739464) aprovado, `device_tested=false`.

- Merge [PR #141](https://github.com/LucianoToscano/PGBR/pull/141) preservando duas linhagens sem perda de commits: Rocket QA6 corrigindo a omissão do APK PR136; mapas/HUD/Pokédex IV; sessão Max preparatória; importação PGSharp local de favoritos/rotas/filtros arquivados.
- Conservada a prioridade conservadora Charge/Charge2 quando metadados ou identidade do alvo não são válidos; **não homologado em dispositivo**. Batalha Max não dispara ações sem validação nativa confiável.
- Merge [PR #142](https://github.com/LucianoToscano/PGBR/pull/142) prepara release imutável com variantes sem Root, Root e módulo Zygisk; [CI #45277310216](https://github.com/LucianoToscano/PGBR/actions/runs/45277310216) aprovou três pacotes QA com SHA-256; publicação depende da conclusão do workflow oficial.
- Artefatos prévios LT042 preservados; nenhuma remoção de arquivos na comparação anterior ao merge; scripts de assinatura/empacotamento, motor de ação e VGP idênticos aos da LT042.
- **Exclusões documentadas, sem declarar funcionalidades prontas:** #135 (Savant), #115/#121/#118 (Snipers), #108/#111/#114 (radar), #109 (GPS) e #101 (PT-BR). Pendências em [issue #144](https://github.com/LucianoToscano/PGBR/issues/144). `device_tested=false`.

### Segurança — Batalha Max (PR #136 / issue #137)
- Bloqueada a injeção da máscara temporária Max em opções globais sem identificação nativa confiável da batalha; preservadas preferências permanentes e testes de regressão. Sessão Max permanece preparatória, sem execução automática, até implementação segura do contexto Max. `device_tested=false`.

## Não lançado — correção de ordenação da linha de IV (fork Pokematrix)
- Painel de IV reaplica a ordem de desenho do canvas a cada exibição para evitar sobreposição pelo modelo quando aberto pelo Pokédex.
- Um arquivo C++ isolado; testes de compilação e Android necessários, `device_tested=false`.
- Branch `fix/pokematrix-iv-canvas-20261123`; sem alteração de LT042 publicada.

## Não lançado — Batalha Max por sessão (fork belalsalah74; QA 23/11/2026)
- Opção de sessão manual para habilitar provisoriamente Ágil, Carregado, Max e Esquiva, com painel PT-BR. Usa executor de combate existente; não inicia Power Spots nem captura.
- Para automações conflitantes, impede execução conjunta com Auto-Raid e VGP+, cessa caminhada ociosa e cancela sessão no segundo plano, troca de conta e perda de prontidão.
- Preferências permanentes, cadências, Ataque Inteligente, Root e branches paralelas preservadas. Teste JVM incluído; CI e teste em aparelho pendentes, `device_tested=false`.
- Branch de QA: `feat/fork-max-session-20261123`; sem merge/release automático. Consultar Arquivo Mestre.

## Não lançado — integração seletiva Pokematrix (branch QA 23/11/2026)
- Duplo toque do mapa oferece Caminhar ou Teleporte; «Não perguntar novamente» exige escolha explícita e é revogado quando o modo muda.
- Badge compacto do companheiro com EP, estrela e manutenção, sem retirar o status completo de acessibilidade.
- Botão para exibir/ocultar ícone de ginásios diretamente nas configurações; usa preferência existente.
- Backup e testes JVM estendidos para consentimento de duplo toque. Não altera motor de batalha, captura nativa, modos Root ou versões LT042 publicadas.
- **Branch, ainda não lançado nem validado em aparelho**: `feat/pokematrix-map-tap-hud-20261123`. Ver Arquivo Mestre Único.

## v0.00.42-LT — candidato QA do Ataque Inteligente VNext (22/11/2026)
- [PR #124](https://github.com/LucianoToscano/PGBR/pull/124) integrada à main por `828807c15672d31614e1236458cc448927301c73`, incluindo o ancestral da PR #117 (cura e transporte F3). Com dois carregados VNext prontos, metadados validos e eficácia atual, ON prioriza super eficaz > normal > pouco eficaz. OFF, só um pronto, label incerta ou desatualizada: executor original imediato.
- LT042 preparada com workflow QA isolado e três variantes assinadas. LT041 congelada e preservada; sem alterar cadências, escudos, trocas, Legacy/Raid/Gym/Max ou a questão da tela branca. `device_tested=false` e issues #90/#105 abertas. [Notas LT042](docs/releases/v0.00.42-LT.md). **Publicada:** [pré-release QA v0.00.42-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.42-LT), 23/11/2026 00:38:40 UTC. [Build e publicação #45088743052](https://github.com/LucianoToscano/PGBR/actions/runs/45088743052) `completed/success`: seis assets, três pacotes assinados e hashes verificados, `device_tested=false`.

## v0.00.41-LT — candidata de pré-release QA (22/11/2026)
- Corrige iFlowGo ao impedir requisições com raio inválido ou #Dex fora do intervalo; Reides não são rotuladas como globais quando o conector é apenas por área.
- Documentação de instalação/compilação atualizada; master e auditoria de regressões preservados.
- Inclui a PR #116 já mesclada à main: recomendação experimental de ataque (não conectada ao executor) e backup de preferências.
- **Candidata em validação:** nenhuma nova release nem APK confirmado até o GitHub Actions compilar, assinar e publicar. `device_tested=false`. Histórico LT040 preservado.

## Não lançado — correção de política Ataque Inteligente simples e auditoria (22/11/2026)
- [PR #120](https://github.com/LucianoToscano/PGBR/pull/120): remove espera deliberada de até 5,5 s para farmar um carregado mais eficaz na política Java PvP; quando existe golpe pronto, prioriza categorias **super eficaz > neutro > resistido** e usa o primeiro em empates. Aceita só um carregado conhecido; preserva preferência OFF, confirmação de prontidão, máscaras de ataque/escudo e as cadências dos executores.
- Estendidos testes de política para matriz 3×3, prontidão exclusiva, empate e adversário desconhecido sem inferir eficácia. Comparação LT040→main mostrou histórico linear e nenhum arquivo removido, não homologação física.
- **Limitação de escopo:** política PvP Java não se encontra ligada ao executor nativo de combate; PR #117 contém regra C++ isolada e transporte F3 ainda sem adaptação runtime. Não afirmar que o segundo golpe é acionado por eficácia real nem publicar APK como solução da issue #90. [Relatório](docs/AUDITORIA_PGBR_ATAQUE_SIMPLES_2026-11-22.md). `device_tested=false`.
- Sem alteração de versão, sem merge da PR #117 e sem sobrescrever as releases QA v0.00.39/v0.00.40.

## Não lançado — Ataque Inteligente simples VNext (22/11/2026)
- Implementação experimental condicionada a runtime tipado: chamar o original de `ObserveBattleState` antes da observação, resolver indicação Super/Normal/Pouco e slot `Charge/Charge2` diretamente do display, comparar somente dois carregados VNext prontos, com correspondência de atualização e frescor. Sem espera de energia nem alteração de cadências.
- Mantém o primeiro carregado tradicional quando OFF, dados ausentes, apenas um botão pronto, atualização antiga, metadata incompatível ou sessão ambígua. Escudos, trocas, Cura e executores Legacy/PvE não foram alterados.
- Acrescentadas fixtures de classificação, escolha de slot e executor Rocket com objetos simulados. **Não confirmado em aparelho**; testes e compilação não são homologação de gameplay. Documento: [Integração VNext](docs/ATAQUE_INTELIGENTE_SIMPLES_VNEXT_2026-11-22.md). Preservar PR #117 até revisão da integração.

## Não lançado — prioridade simples de eficácia (22/11/2026)
- Nova política pura compara categorias de eficácia e reutiliza a tabela de tipos. Testes usam objetos simulados; ela ainda não está ligada aos executores nem altera a escolha no jogo. A investigação identificou um indicador candidato no display VNext, mas não comprovou sua atualização para a sessão/adversário atual. Sem release.

## Não lançado — Ataque Inteligente: investigação do original (22/11/2026)
- O APKM original 0.437.0 da biblioteca foi reconstruído e sua identidade conferida com o perfil fixado; metadados e biblioteca ARM64 foram examinados estaticamente fora do Git. Os nomes candidatos encontrados não comprovam a associação de golpes aos botões. Sem integração do motor, teste físico ou release. [Limites e evidências](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md).

## Não lançado — Ataque Inteligente: transporte da preferência (22/11/2026)
- A preferência independente é incluída no snapshot nativo F3, preservando leitura legada F2 e tempos existentes. IDs de instância no motor não são mais truncados para 32 bits. Testes de compatibilidade e backup ampliados.
- **Ainda não seleciona golpes no jogo**: não há associação comprovada dos IDs de golpes aos botões, leitura completa de energia e homologação. Nenhuma release nova.

## Não lançado — proteção de cura e auditoria de batalha (22/11/2026)
- Cura no mapa revalida configuração e permissão após consultas gerenciadas e antes do uso de item; a fixture host do runtime inclui revogação durante consultas. Não é garantia de atomicidade nem teste em aparelho.
- Interface não promete classificação por tipo nem integração ainda inexistente. Motor de ataque, estado dinâmico, ligação a botões e homologação por modalidade continuam pendentes; tela branca sem causa comprovada. [Relatório](docs/AUDITORIA_BATALHA_CURA_2026-11-22.md). Nenhuma versão ou artefato novo.

## v0.00.40-LT — pré-release QA publicada: recuperar PvP/duelos e escopo do assistente (22/11/2026)
- [PR #107](https://github.com/LucianoToscano/PGBR/pull/107) integrada na main no commit `f873c8652bac5dc4cded154e49fbab82bf778445` após 5 checks `success` (C++ host, NDK ARM64, Java/Root, site e governança). Correção condicional para observação de Hub/Preparação e confirmação PvP, em treino NPC, GBL e duelos com escopo nativo válido.
- Publicação da máscara Ágil/Carregado/Escudo do usuário apenas em escopo reconhecido; desarmada em mapa, resultado, opção desligada, falha, lease expirada e alvos desconhecidos. Raid/Ginásio/Max continuam com seus motores próprios.
- Escudo 0/1.500/3.000 ms e Troca KO 400/2.000/6.000 ms, opção `Desativado` retirada; valor legado 3 migra ao padrão. Sem alterar a cadência ágil/carregada e janelas manuais.
- Diagnósticos de probe, GUI/escopo e motivo de bloqueio para a investigação no aparelho. **Não reivindicar funcionalidade comprovada em Android: `device_tested=false`.** Ataque Inteligente por eficácia real e tela branca ainda não resolvidos.
- [Workflow LT040](.github/workflows/pgbr-lt040-release.yml) e [notas LT040](docs/releases/v0.00.40-LT.md) são **preparação de nova pré-release**; não declarar pacote disponível antes da build assinada e dos links reais. Preservar LT037/LT038/LT039.

- **Publicação comprovada:** [pré-release v0.00.40-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.40-LT) publicada em 22/11/2026 18:32:42 UTC após [workflow #45054751331](https://github.com/LucianoToscano/PGBR/actions/runs/45054751331) `completed/success`, com APK sem Root, APK Root e ZIP Zygisk assinados, checados por `tools/verify_release.py` e seis assets preservados. Commits de código #107 e release #110 listados no Mestre. Não houve homologação física: `device_tested=false`. Seleção nativa Super eficaz > Neutro > Pouco eficaz continua pendente (#90), assim como reconhecimento VNext-only em alguns duelos (#105) e a tela branca.

## v0.00.39-LT — candidata QA: escudo/troca ajustáveis (22/11/2026)
- Integração da PR #100 em `848127b8582ce665a3ce9c4c41f00dadd3324e1c`; sete checks aprovados e [build experimental](https://github.com/LucianoToscano/PGBR/actions/runs/45043935988) `success`.
- Escudo: 0 ms, 1.500 ms (padrão), 3.000 ms, desativado; nocaute VNext: 400 ms, 2.000 ms (padrão), 6.000 ms, desativado. Prioridade manual e preferências persistentes, transporte F2 no slot 21.
- Mantidos os 250 ms pós-escudo, cadência original de ataques e esquivas, troca estratégica 2.300 ms, retry KO 1.000 ms/8.000 ms.
- [Workflow LT039](.github/workflows/pgbr-lt039-release.yml) compila os pacotes sem Root, Root e Zygisk, verifica hashes e publica somente a partir da main. [Notas LT039](docs/releases/v0.00.39-LT.md).
- Não reivindicar homologação física, priorização automática de eficácia ou conserto da tela branca; `device_tested=false`. Não substituir links da LT038.

- **Publicação confirmada:** [pré-release v0.00.39-LT](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.39-LT) em `2026-11-22T17:01:31Z`, [Actions #45045923119](https://github.com/LucianoToscano/PGBR/actions/runs/45045923119) `completed/success`, 3 pacotes APK/Root/Zygisk e `SHA256SUMS.txt` disponibilizados. Revisão pós-publicação arquivada no Mestre. A LT038 permanece imutável.

## Não lançado — Escudo Inteligente e Troca após Nocaute configuráveis (22/11/2026)
- Branch isolada `feat/smart-shield-swap-timings-20261122` sobre `be01d9b59a9e1f31f292b8c562744e5801993d08` (LT038 preservada; sem merge ou nova release automática).
- Menu PT-BR em Ataque Inteligente: escudo 0 / 1.500 (padrão) / 3.000 ms / desativado; KO VNext 400 / 2.000 (padrão) / 6.000 ms / desativado; escolhas únicas persistentes.
- Snapshot tipado F2 no slot 21 já existente, verificação de frescor/geração, política nativa de escudo e swap KO; seletor manual respeitado e Batalha Inteligente não preempta KO.
- Intervalo técnico pós-escudo de 250 ms, cadências ágil/carregado e política de troca estratégica de 2.300 ms **não alterados**. Retentativa KO de 1.000 ms e limite de 8.000 ms preservados.
- Regressões de política C++, contrato de fonte e persistência JVM atualizadas; resultados de CI e homologação física devem ser registrados com evidência. Não há APK desta branch até build verificado.

## v0.00.38-LT — pré-release QA publicada: menu único, atrasos manuais e priorização ainda não conectada (22/11/2026)
- Batalha Inteligente → Ataque Inteligente → Cura Inteligente, com apenas uma preferência persistente.
- Espera de 2,3 segundos para troca manual e 1,7 segundo para escudo, sem adiar ataque carregado.
- Motor Ágil/Carregado/Escudo LT037 preservado; ao usuário já funcionou nesse build anterior.
- **Ainda pendente**: a escolha do carregado Super eficaz > Neutro > Pouco eficaz no botão real; mesmo sendo política Java testada, não há adaptador nativo válido. Tela branca do minijogo segue sob auditoria, sem risco introduzido pela alteração.
- QA/CI e publicação pendentes; `device_tested=false` para menus e timers desta revisão.

- [LT038 publicada](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.38-LT) em 2026-11-22T15:46:48Z; CI [#45039119340](https://github.com/LucianoToscano/PGBR/actions/runs/45039119340) `completed/success`. Artefatos sem Root, Root e Zygisk com digests registrados no Mestre; **não** houve homologação física dos recursos LT038.

## v0.00.37-LT — pré-release QA publicada: Rocket compartilhado por allowlist (22/11/2026)
- O mesmo motor Rocket de ágil, carregado e escudo é autorizado automaticamente em Rocket, PvP tipado/desafios privados confirmados e treinamento/NPC reconhecido, mantendo as preferências do usuário.
- Bloqueio explícito `RAID / GYM / MAX / WILD / UNKNOWN` no motor Rocket; `battles.cpp` permanece íntegro e independente.
- Identificação `CombatTypeExtensions.IsPvPCombat` validada + VNext; interstitial NPC precisa de preparação e VNext, sem falsos positivos por Mateo.
- Revogação na saída de VNext, resultados, escopo PvE, prioridade Rocket, perda de lease, parada; sem herança de um combate para outro.
- Testes C++ e contratos Windows/Android; nova versão/release separada da LT036, `device_tested=false` e sem prova de ações aceitas.

- [Release QA LT037](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.37-LT) publicada, build [Actions #45028477340](https://github.com/LucianoToscano/PGBR/actions/runs/45028477340) aprovado. **Teste em aparelho segue pendente.**

## v0.00.36-LT — candidato QA: treinamento e diagnóstico de instalação (22/11/2026)
- Candidato NPC de 45 s precisa de preparação e VNext antes de habilitar motor Rocket/PvP de fast/charged/shield.
- Elimina falso positivo por interstitial isolado de Mateo; status `mode=training`, Rocket prioritário, sem mudança de ABI.
- O instalador sem Root continua usando o mesmo packageId de Pokémon GO e assinatura PGBR: não pode atualizar sobre assinatura diferente.
- Testes nativos/Java/Python e workflow LT036 separado; `device_tested=false`, sem comprovação de autoataques aceitos no jogo.

## v0.00.35-LT — pré-release QA publicada (22/11/2026)
- Observador PvP validado tolera bootstrap tardio e ausência dos hooks opcionais de animação.
- A sessão PvP reconhecida renova o prazo somente com callbacks VNext, conservando teto de 20 minutos e bloqueio de NPC/resultados.
- Regressões de código, teste de C++ do classificador e workflow de QA independente da LT034.
- **Sem cópia de código executável PGSharp e sem homologação física.** Ainda não é prova de ataques ou escudos operacionais em GBL.

# Changelog

## Unreleased

PGBR development QA: cross-platform test portability, mandatory verification checks in PRs and main, pinned CI actions, source safeguards and PT-BR accessibility instrumentation. These changes are **not** an APK release or a claim of on-device validation. The dated v0.3.x notes below are upstream history, not new PGBR releases.

## v0.00.34-LT — pré-release QA (22/11/2026)

- Sniper 100 IV manual do PGBR com validação de validade, coordenadas, espécies, duplicatas, sprites, filtro de Pokédex e confirmação antes de teleporte.
- Origem PGSharp separada com indicação Free/Standard e cofre criptografado opt-in para key Standard; key guardada não equivale a licença verificada.
- **Limite:** sem consulta direta aos servidores PGSharp nem login remoto de key nesta versão; origem 100 IV atual continua sendo o feed herdado do PGBR, sujeito à disponibilidade real.
- Menus e funcionalidades de LT033 preservados. Compilação e entrega em QA; sem homologação Android físico.

[Notas da versão](docs/releases/v0.00.34-LT.md)

## v0.00.33-LT — pré-release QA (22/11/2026)

- Eventos retornam ao Menu Pokébola com opção para reexibir o atalho; seletor dos eventos Halloween, GO Wild e Rota Doces Eternatus.
- Corrigida persistência das 14 entradas do menu: o Selector Pokémon passa a aceitar o bit 8192 sem afetar Eventos (512).
- Incluídas atualizações selecionadas do PGRazor 0.3.5/0.3.6: Mateo nas Rotas, busca nas configurações, limite de IV, filtro de doces, estatísticas de sessão, correções de ginásios, Pesquisa, VGP+ e fusos de Hotspots.
- Preservadas variantes sem Root, instalador Root e módulo Zygisk com a mesma revisão.
- **Limitação:** pré-release de QA; PvP real, priorização supereficaz do segundo golpe, feeds privados, VGP+ no jogo, Root+Vector, joystick e privacidade visual ainda não homologados no aparelho.

[Notas da versão](docs/releases/v0.00.33-LT.md)

## v0.3.4

[Full release notes](docs/releases/v0.3.4.md)

### Improvements & fixes

- Updated to latest game version (0.437.0)
- Gift batch sending skips friends who still have an unopened gift from you, and the pause between gift sends and opens is shorter.
- Auto-Raid closes the "You received a free raid pass" screen itself and keeps the pass.
- When account switching cannot start in a session, the other features keep working on the profile the game started with.

## v0.3.3

[Full release notes](docs/releases/v0.3.3.md)

### New features

- **Shiny Scanner (Experimental):** Automation can check wild Pokémon within 80 m for shinies without catching them. A shiny gets a popup, a phone notification, a ✨ above it on the map and a badge; walking stops and the shiny is held so it can still be caught after it leaves the map. Catch it on the popup opens the encounter, or throws for you once it has left the map. Speed, species and a 24-hour encounter limit are adjustable.
- **VGP+ Catch only shinies:** VGP+ still opens every Pokémon but throws only at shinies and leaves the rest on the map.
- **Encounter counter:** Wild encounters per account over a rolling 24 hours, shown next to the PGBR title with an optional map badge. It counts encounters made on this device while PGBR is running.
- **Auto-Raid Stay within:** Limits raids to 1, 2, 5 or 10 km from where the session started, and returns there once when nothing is left in range. No limit is the default.

### Improvements & fixes

- The world map search accepts coordinates and places the destination pin there, ready for Teleport or Walk here.
- The quick menu's Teleport dialog is now "Go to coordinates" and can Walk as well as Teleport.
- Coordinates can be entered with a space as well as a comma.

## v0.3.2

[Full release notes](docs/releases/v0.3.2.md)

### New features

- **UI Updates:** Settings opens on ten categories, each with an icon and a summary of its current values, and each opens its own page with a back button. Display gains a Dark theme switch.
- **VGP+ Catch speed:** Normal, Fast or Fastest shortens the wait before each throw and before the next scan. Normal is the default and keeps the previous timing.
- **Keep costume Pokemon:** Auto-transfer's "Keep event / costume Pokemon" is now two options, so costumes can be kept without keeping every Pokémon caught during an event. Existing settings and backups keep both ticked if the old option was on.

### Improvements & fixes

- The VGP+ status shows why a Pokémon was kept, such as "Kept: IV 90%+", "Kept: shiny" or "Kept: event", instead of "Kept: protected". With Evolve first on it shows "Queued to evolve, then transfer".
- Pokémon lists have a Mighty (GO Wild Area) category covering the GO Wild Area 2026: Global Mighty Pokémon.
- Research auto-catch uses your current VGP+ ball, throw rating and curve settings instead of the defaults.
- Auto-Raid waits for the map to return after leaving a lobby before opening the next gym, taps a gym again if it did not open, and retries raids left for too few players after 3 minutes instead of 10.
- The auto ball rows under Go Plus are renamed "VGP+ auto ball".
- Shundo Sniper throws a Safari Ball at a Mighty shiny while you have any, and goes back to Ultra, Great or Poké Balls if you run out or the game does not accept it.

## v0.3.0

[Full release notes](docs/releases/v0.3.0.md)

### New features

- **Auto Throw:** Throws has a default-off toggle for automatic throws using your throw settings, selected ball and berry, with Always Hit, plus optional Catch on map for tapped wild, lure and incense Pokémon, with a pin above the one being caught. A tap made while VGP+ is busy waits for it, caught Pokémon are removed from the map, and reward Pokémon from Go Pass and research are left for manual throws. Auto-Raid shares the throwing logic and always uses it, even when the toggle is off, with built-in Excellent curveballs.
- **Go Wild November 2026:** Events has separate, default-off Safari Ball preferences for Mighty Pokémon in VGP+ and Auto Ball. Normal ball settings apply when Safari Balls are unavailable or the encounter is ineligible.
- **Auto-Raid:** Automation can find nearby matching raids, travel to them, wait for enough players, battle using your auto-attack settings, and attempt the bonus catch. Run it by going to Automation -> Auto-Raid
- **PGBR Root (experimental):** A separate installer loads PGBR into the official game with logs, settings backups, account switching, an on/off switch that takes effect on the next game launch, and an always-available Reboot button. This update needs one reboot to install the new loader; switching afterward needs no reboot, and the choice survives updates.

### Improvements & fixes

- Berry priorities can also be selected in Settings under Catch & summary, using the same preferences as the encounter overlay.
- Quick Catch adds experimental acceleration for Dynamax catch, breakout, and flee animations, with Max-specific speed and cleanup diagnostics.
- Pokémon lists support relevant category filters, including Legendary, Mega-capable, and second- or third-stage evolutions, with selection by name.
- Auto incubate or Skip cutscenes now skips the post-hatch Eggs page by completing the hatch directly. Manually opened Eggs pages remain accessible.
- Hatch outcomes and page-dismissal failure reasons are retained when run logs rotate.
- Rooted startup can resolve Android's loader through its loaded system libraries when the default symbol lookup is unavailable, with more specific failure diagnostics.
- Account restoration waits briefly for the game activity to resume before requesting its restart, avoiding a blocked handoff during sign-in transitions.
- Shundo Sniper waits for account and gameplay readiness before teleporting and shows the blocking reason instead of continuing to display “Checking”.
- Brought the rooted build up to date with v0.2.5 features and Pokémon GO 0.435.2 support, including hook/readiness diagnostics and phone details. Rooted startup logs distinguish waiting for Unity rendering from native library loading.
- Added more cutscene skips, including the "New level unlocks" and other feature-unlock cards. A hidden level-up screen no longer blocks taps for up to 30 seconds.
- Added a startup fallback for phones where features sometimes did not load until the game was restarted. Exported logs now record the reason if it still happens, plus more account and settings details.
- Shundo Sniper: stops and asks you to restart the game when the map stops loading Pokémon, instead of teleporting with no results. It also uses less data and searches the map less often while waiting.
- Run logs keep more history, and PGBR no longer leaves 5 MB of old files behind each time the game starts.

## v0.2.5

[Full release notes](docs/releases/v0.2.5.md)

### Improvements & fixes

- More useful diagnostics for failed hooks, account readiness waits and VGP+ readiness, with clearer startup failure reasons and background/resume tracking.
- Logs include phone manufacturer/model, Android version
- Reduced repetitive logging and retained key startup diagnostics when logs rotate.

## v0.2.4

[Full release notes](docs/releases/v0.2.4.md)

### New features

- **Local time:** optional draggable clock in General Settings showing the time at your spoofed location.
- **Gym battles:** new "Battle until defeated" toggle to repeat full rounds from the same gym screen, with Smart healing between rounds when enabled for after battle. Uses your existing battle settings; off by default.
- **Evolve before transferring:** evolve new catches once or through all available stages before transferring. Use your current transfer rules or a separate species list; keep protections still apply.
- **Smart battling:** picks counters for the Pokémon you are fighting. It builds your team before gym, raid, and Rocket grunt battles, and swaps to your best counter during gym and Rocket battles. Off by default.

### Improvements & fixes

- Auto incubate dismisses the Eggs page opened after a hatch.
- Adventure Sync prompts are dismissed...hopefully.
- Skip cutscenes fixes
- During Dyndmax battles, Max energy orbs are automatically grabbed.
- Updated to latest game version (0.435.2)
- UI and performance updates

## v0.2.3

[Full release notes](docs/releases/v0.2.3.md)

### New features

- **Battery usage:** lower resolution, 20 FPS cap, or fewer effects (General Settings).
- **Throw variety:** random throw ratings from the ones you pick, under Throws and in VGP+. Off by default.
- **VGP+ catch-only list:** VGP+ catches only the species you choose.
- **Shundo Sniper black screen:** tap Dark on its card; tap the screen to return.

### Improvements & fixes

- Smart healing: new "Always heal fully" item choice.
- Shundo Sniper: starts sooner, moves on faster when its target doesn't appear, and shows missed checks.
- Transfer on catch: new "Keep 0% IV (0/0/0)" option.
- Gym Slotter and Berry Feeder: placing a defender now comes before feeding.
- World map tiles load faster.
- More cutscene skips.

## v0.2.2

[Full release notes](docs/releases/v0.2.2.md)

### New features

- **Skip cutscenes:** Off by default; turn it on under Settings > General Settings.
- **Smart healing:** Heals damaged Pokémon after battles, periodically, or both, choosing the mix of potions (and optionally revives) that wastes the least, so a Pokémon missing 5 HP gets a Potion, not a Max Potion. Choose the HP threshold (below 25%, 50% or 75%, or any HP missing) under Settings > Team Rocket, Gyms, Raids & Dynamax.
- **Shundo Sniper (experimental):** Teleports to 100% IV Pokémon from the PGBR 100% IV feed (no sign-in needed, you're welcome you greedy bastards), LivePokeMap and PoGoCoordinates, checks each one in the background for shiny and catches only shinies. A shiny found during a cooldown is held until the cooldown ends, then caught. Open it from Automation. Its constant teleporting raises the risk of an account warning or ban, so use an alt account.

### Improvements & fixes

- Team GO Rocket battles: while any Rocket automation option is on, the next Pokémon is picked automatically when one faints.
- Location & overlays is renamed General Settings.
- Team Rocket settings now sit with gyms, raids and Dynamax under Team Rocket, Gyms, Raids & Dynamax.
- World map: PokéStops invaded by Team GO Rocket show darkened with a red R.
- Skip cutscenes also closes the "You're going too fast" warning.
- Teleports and walking could stop moving you on the map until the game was restarted, after the game switched between its location services; your position now keeps updating.
- Research: a selected task whose deletion fails is tried again after 30 seconds and then after 2 minutes, instead of staying in your task list until the game restarts.

## v0.2.1

[Full release notes](docs/releases/v0.2.1.md)

- **Catch chance:** Manual encounters show each ball's chance to catch, from the server's catch rate for that encounter with your berry, throw rating and curveball applied. The server's rate already includes your type medal bonus. Turn it off under Settings > Throws.
- **Auto ball:** Optionally picks the cheapest Poké, Great or Ultra Ball that reaches your target chance when a manual encounter opens and after each escape. A ball you pick by hand is kept until the next escape, and it never uses a Master Ball. Turn it on under Settings > Throws.
- **VGP+ auto ball:** VGP+ can also pick the cheapest Poké, Great or Ultra Ball that reaches your target chance for each throw, using the server's catch rate with your VGP+ berry, throw rating and curveball. It replaces the standard ball; the shiny, IV and CP/level Ultra Ball rules still apply, and it never uses a Master Ball. Turn it on under Virtual Go Plus > VGP+.
- **IV display:** Encounter and Pokémon stats screen IVs show on a light panel like the storage list. On the Pokémon stats screen the panel sits just above the name, in front of the Pokémon. Under Settings > Location & overlays > IV Display, each screen (Encounter, Pokémon stats screen, Pokémon storage list) has one button that cycles Off, Lv · IV · 15/14/12, Lv · IV and Lv · 15/14/12, and the storage list can now show the level too.
- **PvP keep:** Auto-transfer can keep catches with good PvP IVs, with a separate rank for Great League (default top 50), Ultra League (default top 10) and Master League (off by default), counting the Pokémon and anything it evolves into when that species is in PvPoke's top 150 for the league. Set it under Transfer on catch.
- **PvP IV display:** A new PvP format for the encounter, Pokémon stats screen and storage list IVs adds a second row with each league's crest and rank: the IV rank in Great and Ultra League, and the species' PvPoke rank in Master League.
- **PoGoCoordinates feed:** Feeds now asks which service to open: LivePokeMap or PoGoCoordinates. PoGoCoordinates searches Pokémon, raids, quests, weather, Rocket and power spots by IV, level, PvP rank and more, with Discord sign-in for higher result limits or guest use. Each service keeps its own sign-in.
- **VGP incense catches:** Virtual GO Plus and VGP+ now catch Pokémon attracted by your incense, not only wild spawns, with the same VGP+ rules, auto ball and transfer settings.
- **LivePokeMap PvP search:** A new PvP only filter lists just Pokémon ranked within your max rank in the leagues you tick, for example Great League top 50, ignoring the IV, level and size filters. Feed rows show any top-100 Great, Ultra or Little Cup rank.
- LivePokeMap's My Shinies tab and its filter are removed, since LivePokeMap no longer offers My Shinies.
- The quick menu and draggable overlays stay inside the screen, clear of the notch and navigation bar.

## v0.2.0

[Full release notes](docs/releases/v0.2.0.md)

- **Research:** Auto-claims completed field research, auto-deletes the field research tasks you select from a list grouped into Item, Encounter and Event, and can catch waiting field research encounters without the encounter screen, applying your transfer rules. Open it from the quick menu.
- **Hide overlay:** A new quick menu entry makes the game look stock after a confirmation: it hides every PGBR button, status bar and message, plus the in-game IV text. Tap the screen with three fingers at once to show them again.
- **Storage list IVs:** Shows each Pokémon's IVs under its CP in the Pokémon storage list, as attack/defense/HP, a percentage, or both, colored like the encounter IV overlay. All IV display settings (inventory, encounter and storage list) are now together under Settings > Location & overlays > IV Display.
- **Encounter IVs:** The encounter IV overlay now also shows on field research reward encounters, not only wild spawns.
- **Always Hit:** Now also works when catching after a Max Battle.
- **AR+ throw bonus:** Wild catches now get the AR+ catch bonus.
- **GPX routes:** Starting a GPX route in Autowalk now asks where to begin: from the closest point on the route, from the beginning, or by teleporting to the beginning.
- **Copy coordinates:** The World map has Copy my location and Copy pin location buttons, which center the map on that spot and copy its coordinates. The quick menu Teleport dialog shows your current coordinates with Copy and Favorite buttons; Favorite saves the spot to Favorites under a name you choose.
- Menu options and buttons now look like buttons, with an outline and space between neighbouring buttons.

## v0.1.10

[Full release notes](docs/releases/v0.1.10.md)

- Quick Catch returns to the map faster after a successful catch.
- Added backwards compatibility for older Android versions.

## v0.1.9

[Full release notes](docs/releases/v0.1.9.md)

- Updated compatibility for Pokémon GO 0.435.1.
- Reduced reward-summary flashing during Quick Catch.

## v0.1.8

[Full release notes](docs/releases/v0.1.8.md)

- Group battle checkboxes under Gyms, Raids, and Dynamax, with Gym Slotter & Berry Feeder first inside Gyms. Existing choices are preserved.

- Add Location & overlays > Customize Quick Menu: hide individual menu entries without changing feature settings. Choices are saved per account and included in settings backups; Settings remains accessible.

- Add Automation > Send & Open Gifts: one manually started batch, confirmed progress, Stop, and an option to attempt opening with a full item bag using the game's reward setting. Game eligibility and daily limits still apply.


- Restore automatic gift opening and sending when friend-screen callbacks arrive between native scheduler ticks.

- Reduce VGP+ idle scene scans from up to 10 per second to 1 per second, retaining responsive pending catches and cancellation.

- Preserve the trainer profile's scroll position during background player-stat updates, including VGP+ catches and transfers.

- Make buddy care lighter and usable behind menus: idle checks run once per second, with one scene lookup per evaluation and berry-selection service lookups reduced from up to 5 to 1. Cancellation stays responsive.

- Recover map readiness after a temporary straight-down camera angle instead of permanently stopping camera sampling and leaving VGP or buddy care waiting for the map.

## v0.1.7

[Full release notes](docs/releases/v0.1.7.md)

- Start buddy snapshot sessions with the game's buddy photo context and server-adjusted start time before completing the snapshot. A successful request is reported separately from unverified heart credit.

- Fix map-dependent automation stopping after long sessions; continuous camera updates no longer hit the diagnostic observation limit. Gym overlay now distinguishes map and inventory waits. Session logs rotate at 1 MiB with six recent segments and six diagnostic tails retained.
- Fix gym berry inventory waits: use the game's item-bag feeding operation, remember its cache update across later berry gains, and recover from delayed inventory acknowledgement instead of permanently pausing after five seconds.
- Add an expandable Automation section in Settings and an Automation entry in Quick Menu. Move Mass Evolve there alongside two experimental, manually started tasks.
- Add a Daily Incense session: activate incense in the game, choose a heading from 0 to 359 degrees, then start a straight walk at 10.5 km/h for up to 15 minutes. Stop, manual movement takeover, backgrounding, or lost account/game/location readiness ends the session. Loaded Galarian bird detection blocks VGP and stops the owned walk, attempts the normal encounter tap once, and requests sound/vibration notification. Catch manually; no automatic throw or Master Ball use. No spawn count or timing is guaranteed. Joystick movement releases the idle-walking hold after completion or a bird alert. The live Galarian-bird encounter handoff remains unverified.
- Add a single bounded Remote Gym Feeding batch for gyms with your own deployed defenders. Feed surplus Nanab, then Razz, with optional Pinap and separate reserves; premium berries are excluded. Stop cancels unsent work, server limits still apply, and counters reflect confirmed responses only. No recurring schedule or reward payout is promised. Confirmed accepted feeding and clean Stop behavior on MuMu.
- Add optional Idle walking under Location & overlays. After 3 seconds idle, follow a 10 m-radius circle within 20 m of the starting point, using the selected speed up to 5 km/h. Joystick and routes take priority; speed 0, backgrounding and lost location readiness pause movement. The setting is saved per account and included in backups. Walking distance credit remains game-dependent.
- Expand Gym Slotter, Berry Feeder and VGP+ gym-disc spinning to the 80 m base interaction range, retaining eligibility, cooldown and server-result checks.
- Recover gym discovery after manually viewing a gym. Preserve inventory acknowledgement and timeout safeguards for outstanding actions, and show native pause reasons instead of getting stuck at Starting.
- Show gym-disc activity on VGP+ instead of overwriting the Gym Slotter & Berry Feeder overlay. Gym spinning remains controlled by VGP+.
- Limit the Keep All overlay to manual encounters. VGP and VGP+ continue using the main transfer switch, protection rules and keep list.
- Resume unfinished buddy feeding cycles after selected berries are restocked above reserve. Preserve the remaining feed budget and confirmed cooldowns; existing saved retry timers are not cleared.
- Update the feature guides for gym range and manual encounter transfer controls.
- Note in Settings and the feature guides that Spawn booster can cause lag in busy areas with many nearby Pokémon.
- Reduce overlay work on the game's UI thread: the hidden Settings panel is no longer refreshed every 100 ms, and overlay status text and backgrounds are updated only when they change. Session logs record VGP marker phase changes and periodic positions, spawn-range changes, and event-box results or availability changes plus periodic heartbeats. Idle event-box scan counters do not trigger extra reports.

## v0.1.7-experimental.3

[Full release notes](docs/releases/v0.1.7-experimental.3.md)

- Fix gym deployment-to-feeding transitions latching Paused when a settings, readiness, or gym eligibility check changes before the next request is sent. Recheck unsent work; keep uncertain in-flight outcomes paused. User confirmed recovery across two successful gym deployments on Android.

- Add Auto-spin Gym discs under VGP+, controlled by the VGP overlay, with bag-space checks and per-gym cooldown handling. Gym-disc spinning has been user-verified.

- Gym auto quick/charged attacks now activate Battle on the next-opponent dialog without waiting for the countdown; manual exit and disabled automation cancel continuation.

- Recheck nearby gyms promptly after temporary no-action results instead of excluding them for 60 seconds; keep reserve and retry status visible.

- Add a draggable gym-and-berry overlay that starts/stops the entire Gym Slotter & Berry Feeder while preserving deployment and feeding choices. Active cooldown stops both actions; restart manually after it ends. The master starts off each session. A small status line follows the icon and shows deployment, feeding, and cooldown activity.

- Add Batch evolution: preview a named tag, review candy/item costs and skipped entries, then evolve eligible Pokemon sequentially without opening the cutscene. Include Stop, inventory-confirmed progress, server-reported XP, and optional removal of the selected tag after successful evolution. Preserve other tags and skipped entries; special or ambiguous evolutions are skipped.
- Validate tag-removal request construction during preview and handle replacement Pokemon IDs when confirming evolutions. Evolution and optional tag removal have been user-tested on MuMu.

- Add Walk and Teleport actions to world-map Stop, Gym, and Raid details, including the destination cooldown confirmation.

- Fix Quick Spin losing pending spins during short frame stalls or concurrent settings updates. Continue to revoke automation when settings are disabled or stale.

- Fix buddy care pausing on gift results and carrying a paused state into a newly selected buddy. Wait until the buddy is on the map before opening gifts.
- Add a separate EP overlay visibility setting and compact Clear/Reset controls in the buddy berry-priority picker.

- Consolidate buddy care into the Buddy Emotion Engine dropdown, with ordered berry selection and reserve-aware fallback. Preserve existing berry choices and retire the separate Auto feed buddy control.
- Rename Smart Background Catcher to VGP+ and group its controls under Virtual Go Plus. Gray out dependent settings when their parent is off, while preserving saved choices.

- Keep Buddy Emotion automation independent of location/joystick activation, and show when feeding is blocked by berry reserves, fullness/cooldown, or unavailable game services.

- Add a native Buddy Emotion Engine with automatic snapshots, petting, hunger-based normal-berry feeding, optional gift collection, berry reserves, and a draggable status display using game-reported EP. Preserve recent action history across restarts. Maintenance countdown is an estimate; walking and battles remain manual.

- Fix gym deployment and feeding blocked by an unreliable local closed flag; honor server results and feeding cooldowns, keep placement lockouts separate, and report skipped-gym reasons.

- Fix event collection skipping visible, manually collectable boxes because of an extra local expiry cutoff.

- Add a Gym Slotter & Berry Feeder with separate deployment and feeding switches, Gym-tag priority, optional low-CP fallback, berry reserves, and native request pacing. Deployment and consecutive feeding with Stardust rewards verified on MuMu.

- Add Smart Background Catcher with configurable balls, berries, throw quality, and species priorities. Continue catching while in-game menus are open and consider loaded spawns beyond the standard VGP distance limit, subject to server acceptance.
- Fix Smart Catcher rejecting valid encounter responses with a native VGP error; preserve catch retries and automatic transfer behavior.

- Restore wild-catch cooldown history by preserving native encounter types. Show zero cooldown when no actions are recorded.

- Show the cooldown warning before every user teleport, including coordinates, Favorites, World Map destinations, feeds, hotspots, and in-game map-tap teleport controls.

- Add 14 curated hotspots to the World Map and Quick Menu. Map choices select a waypoint; Quick Menu offers Walk or Teleport, and hotspot teleports show a destination cooldown estimate before confirmation.

- Show a live blue current-location dot on the World Map independently of the red destination pin.

- Add World Map place search for businesses, parks, landmarks, and addresses, biased toward the viewed map area. Selecting a result centers its destination pin; Walk and Teleport remain explicit actions. Search uses Photon/OpenStreetMap and results may be incomplete.

### Using tags for gym defenders and evolution

These controls are included in v0.1.7-experimental.3.

**Gym defenders**

1. In Pokémon GO's Pokémon storage, open **Tags**, create a tag named `Gym`, and apply it to Pokémon you want to deploy. `#Gym` is also recognized, but `Gym` is the simplest name to use.
2. Open **PGBR Settings → Raids, gyms & Dynamax → Gym Slotter & Berry Feeder**, then enable **Auto-deploy to open Gym slots**.
3. Start the feature with its gym-and-berry overlay icon or **Run Gym Slotter & Berry Feeder**, then stay on the game map near a friendly gym within 40 m with an open slot. VGP does not need to be running. The slotter prefers eligible tagged defenders; favorites, your current buddy, injured or deployed Pokémon, and other ineligible defenders are skipped. It also avoids species already defending that gym.
4. Leave **Allow lowest-CP fallback** off to restrict deployment to your tagged choices. Enable it only if you also want eligible untagged Pokémon considered when none of your tagged defenders can be used. Deployment does not remove the `Gym` tag.

Deployment and berry feeding have separate selection switches; the overlay starts/stops the whole feature without clearing either choice. Feeding does not require tagging the defenders. Enable **Show gym automation overlay** for the draggable gym-and-berry icon. Active cooldown stops both actions; restart manually after it ends. It uses Nanab, then Razz, only above your configured reserves, and respects server refusals and feeding limits.

**Batch evolution**

1. Create a tag such as `Evolve` in Pokémon storage and apply it only to Pokémon you want evolved. You can use another tag name; enter that name in **PGBR Settings → Batch evolution**.
2. Optionally check **Remove this tag after each confirmed evolution** before previewing. It defaults off. When enabled, it removes only the selected tag from successful evolutions; other tags and skipped or failed entries are preserved.
3. Select **Preview batch** and review the eligible count, candy/item costs, and skip reasons. Favorites, your buddy, deployed Pokémon, eggs, Shadow/purified Pokémon, and conditional or ambiguous evolution branches are skipped.
4. Activate a Lucky Egg yourself if wanted, then choose **Start batch** and confirm. Each selected Pokémon evolves once per batch, even if another evolution is available afterward. **Stop** prevents further requests; an in-flight action may still finish.

If an error appears, check Pokémon storage and the completed count before retrying: the evolution may have succeeded even if tag cleanup failed. The engine does not automatically retry uncertain evolutions.

## v0.1.7-experimental.2

[Full release notes](docs/releases/v0.1.7-experimental.2.md)

- Fix Rocket, Gym, Raid, and Dynamax battle automation observing natural game callbacks.
- Resume VGP after confirmed inventory cleanup; query selected item counts and process large cleanup batches incrementally.
- Allow other boxes to collect after a missing encounter-animation callback while preserving late-screen suppression.

- Keep nearby autowalk's default at 50 stops while allowing counts up to all currently loaded unique PokéStops and Gyms. The displayed maximum follows nearby availability; leaving the count blank selects All.

## v0.1.7-experimental.1

[Full release notes](docs/releases/v0.1.7-experimental.1.md)

- Add an Events screen and a Halloween toggle to open visible boxes and skip encounter screens.
- Rewrite the gameplay runtime in compiled native C++ and Java.
- Fix buddy berry feeding on MuMu and Routes pausing during location updates.
- Stop periodic full inventory scans when auto-discard is disabled. Item settings request counts while visible, and berry controls query only berry stock; unchanged catalogs are no longer republished.
- Fix the native VGP pilot pausing after an unsent request is refused during a routine settings refresh. Restrictive settings still stop requests; uncertain outcomes remain paused.

## v0.1.6

[Full release notes](docs/releases/v0.1.6.md)

- Add Switch account to the Quick Menu and deletion of inactive saved accounts, including their remembered sessions and local profile data.

- Add experimental account profiles under Settings for Google/PTC sign-in, with separate settings, last location, cooldown history, favorites and saved routes. Switching stops automation and restores an encrypted saved session after restarting; each account needs an initial sign-in, with normal sign-in fallback when its session expires.

- Show the PGBR version beside the theme button in Settings.

- Automatically dismiss Adventure Sync prompts when location spoofing is enabled and Android location permission is denied.

- Add experimental in-game Route walking from the quick menu: discover nearby Routes, choose the shortest total walk, follow the recorded path, and confirm rewards before continuing, with a configurable completion limit and a draggable icon for live path details.

## v0.1.5

[Full release notes](docs/releases/v0.1.5.md)

- Add an Always hit throw option that converts missed ball collisions into hits while preserving normal catch outcomes.
- Preserve the LivePokeMap login form during password-manager autofill and support username/password autofill hints.
- Use one latitude, longitude field for quick-menu teleport, accepting coordinates with or without a space after the comma.

## v0.1.4

[Full release notes](docs/releases/v0.1.4.md)

- Ship one APK for ARM64 Android phones and MuMu, selecting the appropriate runtimes automatically.
- Connect MuMu's Android controls to translated ARM gameplay hooks for Quick Catch, VGP, berries, transfers, IV display, and other native features.
- Fix early permission initialization so spoofing can start with app location permission denied.
- Adapt gift dialog observation and executable-code validation for MuMu while retaining the existing phone runtime path.

## v0.1.3

[Full release notes](docs/releases/v0.1.3.md)

- Ask before a game-map tap stops a planned route or Autowalk; cancelling keeps the route running.

- Avoid replacing the native location-update method; normalize provider events through listener adapters to address Android runtime dispatch crashes.

- Add My location centering and a direct Clear route action to the world map.

- Import GPX waypoints, routes, and tracks; save and export routes from the world map and choose saved GPX routes in Autowalk.

- Recover Dynamax attack controls from the active battle when their setup callbacks were missed, allowing automatic quick and charged attacks to start.
- Vary the circle radius within the selected Nice, Great, or Excellent throw band, preserving one sampled value across a throw's submit/hit callbacks.
- Separate native feature state machines, GPS delivery, VGP/toast policy, and map tile/POI layers into modules with explicit ownership.
- Split the settings menu into named section builders and refresh binders, share theme styling, and stop item-dialog polling when its panel closes.
- Save settings through an ordered background writer, with flush checks for export/import and protection against stale reloads after a failed save.
- Discover all device-free test suites in CI, replace moved-feature source-slice fixtures with module tests, and record hashes for composed runtime scripts and their sources.
- Keep map tile requests tracked until their completion callback runs, avoiding a queue-overflow race during viewport changes.
- Restore catch animation speed and release IV labels during cleanup even when the runtime clock fails.

## v0.1.2

[Full release notes](docs/releases/v0.1.2.md)

- Allow location spoofing with Android's system Location switch off or the app's location permission denied. System Location can stay enabled for other apps; disabling spoofing restores normal device-location requirements.

- Keep spoofed location accuracy, timestamps, and provider status consistent when real GPS updates arrive.

- Make the joystick more transparent with no backing panel, retaining the drag handle; add a tap-to-walk/double-tap-to-teleport game-map mode.

- Add saved 100%, 75%, 50%, and 25% map-overlay UI scale presets under Location & Overlays.

- Simplify the catching berry overlay to an icon/count and add three saved berry priorities with inventory fallback, including out-of-stock selections.

- Add a searchable custom species keep list and optional persistent Keep All transfer overlay, shared by manual and VGP catches and included in settings backup.

- Allow VGP to run while other in-game screens are open; exclude the current manual encounter target and keep world markers map-only.

## v0.1.1

[Full release notes](docs/releases/v0.1.1.md)

- Restore the saved Location spoofing toggle; disabling returns to device location and stops simulated movement.

- Add separate saved IV/level display toggles for encounters and owned Pokémon details, included in settings export/import.

- Quick Catch can skip creating the post-catch details screen for confirmed manual catches, preserving auto-transfer and the game's dismissal callback. Unsupported paths retain normal dismissal.

- Quick Catch also shortens the successful-catch presentation wait to 100 ms starting with the first catch. Summary dismissal and auto-transfer retain their existing completion checks.

## v0.1.0

[Full release notes](docs/releases/v0.1.0.md)

- Fix Google sign-in by opening Google authentication in the system browser.
- Add optional Discord release role pings.

- Pause automated gym, raid, and Dynamax attacks and dodges while the Pokémon switch picker is open; resume after it closes.

## v0.1.0-alpha.4

[Full release notes](docs/releases/v0.1.0-alpha.4.md)

My Shinies, expanded Feed display filters and local presets, plus portable settings export/import.

## v0.1.0-alpha.3

[Full release notes](docs/releases/v0.1.0-alpha.3.md)

Quick menu, map layers and routes, LivePokeMap integration, movable overlays, unlimited egg incubation, VGP improvements, Discord footer, and restored Pokémon GO launcher label.

## Earlier pre-release work

- Rename the project, Java namespace, assets, settings, logs, signing alias, and tooling to PGBR throughout; remove legacy settings migrations. A fresh installation is required.

- Added independent XXS and XXL keep rules for caught-Pokémon transfers, shared by manual catches and VGP.
- Changed the VGP settings switch to control overlay visibility; the overlay power button starts/stops VGP and displays ON/OFF.
- Added a compact VGP activity display, projected target marker, caught-ID transfer integration, and item cleanup sequencing.
- Introduced private single-APK packaging and a guarded GitHub Actions prerelease workflow.
- Prepared source-only CI artifacts, build/contribution documentation, licensing notices, and repository exclusions.

The new single-APK release path and latest UI changes still require device testing.

Earlier release notes are available on [GitHub Releases](https://github.com/jpitty03/PGRazor/releases).
