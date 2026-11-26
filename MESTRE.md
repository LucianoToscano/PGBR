# PGBR — índice permanente de execução e continuidade

STATUS: EM_EXECUCAO
RELEASE_TAG: v0.00.47-LT

## Fonte canônica e regras

O histórico completo permanece em [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Este índice complementa, não substitui nem reduz o histórico. Consulte também [README](README.md), [CHANGELOG](CHANGELOG.md), [catálogo de recursos](docs/catalogo/PGBR_MAPA_RECURSOS_E_EVOLUCAO.md), issues, PRs e CI atuais. O acervo do Drive (pasta Arquivo Mestre, PROMPT-MESTRE-PGBR.txt e autonomo-pgbr.sh) foi localizado em 24/11/2026; consultar somente por acesso autorizado, sem publicar localizadores privados ou credenciais. Não confundir inventário inicial com auditoria integral concluída.

Ciclo: recuperar HEAD e checkpoint; inventariar; priorizar P0 (build/crash/perda), P1 regressões, P2 requisitos aprovados, P3 UI/PT-BR/assets, P4 qualidade/desempenho, P5 entrega/docs; corrigir no escopo autorizado; testar; registrar evidências, arquivo, causa, solução, SHA, PR, versão e próximo passo. Sem force push, sem desativar testes, sem modificar branches de terceiros. Sem atribuir integração ou teste físico a mocks. Respeitar licenças e interfaces autorizadas. A publicação de nova versão, merge e tag estão autorizados pelo proprietário após validação. Não substituir versão publicada. O executor agendado existente foi atualizado para frequência horária sem limite programado de ciclos; não criar executor concorrente. O script CLI é alternativa local, não execução simultânea.

## Checkpoint de 24/11/2026

1. [x] PGBR-UI-001 / P1 — layout compacto, Locais unificado e filtros recuperados em código. Causa: divergência da branch experimental. Arquivos: WorldMapDialog.java e test_route_trace_java.py. PR #201, merge 8c644bc33d06d8772b939a0f07b94d4de5f8a542. CI 45479212037 (5/5) e 45479212042 aprovados. Aceite físico separado abaixo.
2. [x] PGBR-DOC-001 / P5 — reconciliação documental #197/#198, catálogo e histórico preservados por #202. Main observada 2915b5b15374aff20a141009039d1362e6a106f6. Não implica conclusão das funcionalidades listadas.
3. [x] PGBR-BUILD-047 / P0 — build QA e integração #203 verificados: HEAD 4092c29b7d4759bce8864224527e2a0bad72b1f5; CI 45482026547, 45482026278 e QA 45482026778 aprovados; merge 7aae7fbbb78f66f5b5ec4bfb0035586d05051663. Artefato 14961904891 (205003803 bytes, digest ZIP sha256:19d942084c5998494c6b71f7685ea380ecdaf9c470ee80371937425a6a5072d2); hashes individuais no Mestre canônico. Publicação separada em PGBR-RELEASE-047.
4. [ ] PGBR-REG-200 / P1 — issue #200: validar ausência de regressões no pacote consolidado e identidade do artefato. Evidência de interface antiga nas capturas, correção de código #201. Próximo: verificar pacote exato; não confundir QA Arena com LT047.
5. [ ] PGBR-AUD-001 / P1 — inventário e triagem de todas as issues, PRs, branches e registros históricos, com critério de aceite e deduplicação; recuperar requisitos antigos sem tratá-los como implementados. Próximo: revisar histórico e catálogo em unidades verificáveis.
6. [ ] PGBR-ROUTE-199 / P2 — PR #199 isolada, protótipo experimental sem homologação; não sobrescrever branch Arena, não atribuir funcionamento comprovado. Próximo: revisar evidências e escopo autorizado; preservar demais correções.
7. [ ] PGBR-RELEASE-047 / P5 — entrega real com versão reservada, tag imutável, artefatos e hashes confirmados no commit aprovado. Não marcar concluída sem evidência.

## Ciclo 25/11/2026 — LT048 lote P0 (tela branca, Auto ATK, Supereficaz) + PT-BR

Branch `arena/5ed38273-pgbr`, base `24700b8cba7fb9a789052a0faa632c75b7fdf54d`. Detalhe completo no Mestre canônico.

8. [x] PGBR-BAT-01 / P0 — tela branca no ataque carregado: código de #275 consolidado (fallback que ampliava `Transform.localScale` em 1000× removido) + política de geometria `rocket_minigame_shape.h` com limites, recusa de NaN/infinito/escala extrema e “tudo ou nada”; geometria desconhecida não muta escala nem ativação. Fixtures host `rocket_minigame_shape_test.cpp` e contrato estático. Correção de código concluída; **homologação física separada** (issue #192).
9. [x] PGBR-BAT-03 / P0 — Auto ATK parava após a batalha até reiniciar: causa em `combat_host.cpp` (revogação da política em corrida de leitura do transporte; `pvp_leave` em qualquer leitura indisponível). Corrigido com `transport_option_policy.h` (tolerância de 1000 ms, falha fechada sem relógio) e `pvp_executor_policy.h` (KEEP/SUSPEND/LEAVE). Em suspensão o escopo permanece ancorado e **nenhuma** ação é invocada. Reprodução em aparelho pendente (issue #105).
10. [x] PGBR-BAT-02 / P0 — Supereficaz: Fantasma → Aço estava neutro (0) nas duas cópias da tabela; corrigido para 0,625× em `battle_counter.h` e `PvpChargedPreference.java`, com teste independente da tabela 18×18 inteira. Batalha real permanece pendente (#90).
11. [x] PGBR-I18N-01 / P1 — 17 rótulos ainda em inglês traduzidos no glossário e nas duas UIs (1547 entradas sincronizadas), com teste de varredura. VGP+ “Lenta (aprox. 3 s)” já existente e coberta.
12. [x] PGBR-QA-03 / P0 — **CI verde (7/7)** na PR #283, incluindo `Suite real check.py + compilacao Java + Root` (Windows/JDK/SDK) e NDK ARM64. Merge na main em `4e74063c51f325498a0f8e2caf75794e1ec8de3a`. **Build QA executado**: run `45526055689` (`main`, HEAD `4e74063c`), jobs `native-pvp-fixture` + `apk-test` aprovados, `tools/verify_release.py` aprovado, artefato `PGBR-LT047-Root-e-SemRoot-QA` (id `14982987001`). SHA-256 reais: `9EBA808C…4C34B51` (APK), `811BF2D2…869E95` (Root APK), `08748CAE…126C6` (módulo Root) — completos no Mestre canônico e em `SHA256SUMS.txt` do artefato. PR #275 fechada como substituída (`8459c37` preservado).
13. [x] PGBR-QA-04 / P0 — **APK QA `v0.00.48-LT` compilado na main**: run `45528225920` (workflow novo `.github/workflows/pgbr-lt048-qa.yml`, `workflow_dispatch`, `ref=main`, HEAD `4ceed40e`), jobs `native-pvp-fixture` + `apk-test` aprovados, `tools/verify_release.py` aprovado, artefato `PGBR-LT048-Root-e-SemRoot-QA` (id `14984545589`). SHA-256 reais: `46CDD675…E3C6B7C3` (APK), `EF07C25D…174081E` (Root APK), `B7927296…493D6BB8` (módulo Root). Sem publicação, sem tag, sem sobrescrever nada; download em `https://github.com/LucianoToscano/PGBR/actions/runs/45528225920`. `test_lt048_release_contract.py` agora com 6 testes (contrato histórico LT048 e matriz QA48 preservados).
14. [x] PGBR-QA-05 / P0 — rastreabilidade de rótulo: o run anterior `45526055689` (HEAD `4e74063c`) gerou o mesmo código rotulado `v0.00.47-LT` por contrato do workflow LT047; registrado no Mestre canônico e em `docs/releases/v0.00.48-LT.md` para ninguém confundir QA com release.
15. [ ] PGBR-HOM-01 / P0 — homologação física do lote, roteiro em `docs/validation/LT048_P0_TESTE_FISICO_20261125.md`. Bloqueia o encerramento das issues #192, #105 e #90.

Status dos testes locais: 465 testes Python (só falha a suíte que exige `javac` neste ambiente), 24 fixtures C++ host, 22 suítes Node, `-fsyntax-only` do host nativo. `device_tested=false`, `provider_connected=false`.

## Ciclo 25/11/2026 — P1 PT-BR lote 2 (textos ainda em inglês)

16. [x] PGBR-I18N-02 / P1 — 7 telas/overlays com texto inglês visível traduzidos na origem (`LogActivity`, `PreviewActivity`, `RootedLogDialog`, `QuickLocationDialogs`, `GymFeedOverlay`, `ShundoSniper`, `GameRoutesOverlay`); mensagens de limite do seletor de espécies/chefes passaram a usar substantivo traduzido (`nounPt`), sem mudar assinaturas; 1 entrada nova no dicionário (`All species (no selection)`, 1548/1548/1548). Regressão coberta por `pgrazor/test/test_ptbr_ui_strings.py` (5 testes). Homologação visual pendente (`device_tested=false`).
17. [x] PGBR-I18N-03 / P1 — decidido e registrado **não** rodar `tools/ptbr_wire.py --apply` em massa nesta branch: a varredura reescreve literais que contratos estáticos exigem (botão "Aplicar", limpeza de campo de key, mensagens de privacidade) e derrubou 5 suítes; tradução passa a ser cirúrgica, com teste por lote.

## Aceite em dispositivo (separado)

device_tested=false; provider_connected=false até evidência correspondente. Tela branca Rocket: aprovação funcional do proprietário registrada na #301 para V5, sem SHA instalado informado. Supereficaz, Auto ATK, interface e traçado visual não são homologados fisicamente pela aprovação de CI. Não alterar estados por conveniência.

## Próximo ciclo (checkpoint original preservado)

Verificar PR #203 e run 45482026778, registrar logs/resultado real, preservar o SHA do build em andamento e avançar as tarefas independentes. Antes de declarar STATUS: CONCLUIDO, revisar todos os registros canônicos e obter evidência de entrega real; a mera ausência de checkboxes não basta. A ID de auditoria de um eventual supervisor CLI só pode ser registrada pelo executor real desse supervisor.


### Ciclo 25/11/2026 — LT048 #192: camada branca do carregado (visual puro)

18. [x] PGBR-BAT-04 / P0 — camada branca do ataque carregado. Base funcional: PR #297 (`1b1286bc`), mecanismo intacto. Origem: o renderer do objeto de toque, ampliado 1000× pelo coletor histórico; o material branco **opaco** escapava da correção de alpha anterior. Correção exclusivamente visual: `Renderer.enabled=false` nos renderers da subárvore do colisor, com leitura de confirmação, restauração exata por renderer e limite de 24; nada de escala, colisor, material, shader, `SetActive` ou tempo. Config V4 (`376` bytes) com fallback V3/V2/V1 e `miniWhiteHideBound` no diagnóstico. Testes: `rocket_minigame_white_layer_test.cpp` (7 cenários) + `test_white_layer_visual_only.py` (9 testes) → 26 fixtures host, 500 testes Python. **Homologação física pendente** (#192).

# Ciclo 24/11/2026 — versão atual 0.00.47-LT

Main observada `56cff4b885b9e52aa914aab8152a67959516e852`. Fila numerada de trabalho: os itens de catálogo incluem verificação de recursos existentes, propostas e melhorias; aberto não significa bug comprovado nem autorização para executar ações em serviço terceiro. Dependências e critérios completos permanecem no catálogo; decisões posteriores do Mestre prevalecem (BYOK retirado, não reativar automaticamente). Os itens já integrados podem exigir aceite em dispositivo separado. Não marcar conclusão sem evidência.

19. [x] PGBR-CI-205 / P0 — Falha Bash printf corrigida em b67cd7ad1129c5a8733a5d48e16e4fb15359dffd; HEAD reconciliado 7b5a8424ff77eee98526c839e7db2b6cfdc102a0, CI 45493274906/45493274883/45493274892 aprovados, merge 606d90d455632fc7f7847eb8626a7ea99ada77e8. Primeira execução agendada na main deve ser conferida separadamente.
20. [x] PGBR-DOC-204 / P5 — Índice MESTRE.md integrado em 56cff4b885b9e52aa914aab8152a67959516e852, CI 45482888669/45482888540 aprovado.
21. [x] PGBR-UI-SMALL / P1 — Causa estática confirmada: altura disponível podia ser zero; rodapé e atribuição sobrepostos. Candidata: scroll de contingência, altura mínima conforme controles medidos, rodapé empilhado e altura flexível dos botões; teste host de aritmética e integração estática. CI e teste Android físico ainda exigidos. Atualização: correção de código integrada pela #207, HEAD f9d0d535318b9efa59ee88cc80be3c054b58b51f, CI 45494782561/45494782218 aprovados, merge be99a052d15e643622aa83ecf754dac9635f719a. Aceite físico permanece separado em QA-02.
22. [ ] PGBR-PR-TRIAGE / P2 — Reconciliar PRs antigas por equivalência comprovada e registrar decisão sem apagar trabalho.

### Inventário de 65 registros do catálogo

23. [ ] MAP-01 — Mapa mundial e POI. F: WorldMapDialog, MapProjection, MapTileSource/Cache, MapPoiLayer. Critério: Legibilidade, acessibilidade, cache, categorias, atribuição e indicação de fonte/idade. Prioridade original: P1.
24. [ ] MAP-02 — Locais e favoritos. I: Mestre/#188; SavedPlaces, Hotspots, GpxStore. Critério: Biblioteca única Favoritos/Pontos/Rotas/Eventos; busca, filtro múltiplo, ordenação, +, coordenadas e cópia independente; manter perfil e ícones coerentes. Prioridade original: P0.
25. [ ] MAP-03 — GPX e prévia. F/I: GpxStore, GpxRouteDialog, GpxCodec; #167/#172. Critério: Multi-segmento, nomes Unicode, limites, distância, edição, progresso, exportação e importação segura. Prioridade original: P1.
26. [ ] MAP-04 — Rota visual pessoal. I/P: #176/#183/#185. Critério: Rota GPX/Caminhar até aqui exclusivamente visual; progresso, cores, espessura e destino; modo automático só com interface autorizada e validação de projeção. Calibração inicial ainda manual. Prioridade original: P1/P3.
27. [ ] MAP-05 — Rotas oficiais. I/P: GameRoutesController/Dialog, #183/#187. Critério: Preservar separação, fluxos oficiais e recompensas; revalidar regressões com uso autorizado e sem ação automática nova. Prioridade original: P0.
28. [ ] MAP-06 — Joystick, movimento e GPS. F: Pad, MotionEngine, AutoWalkPlanner, GameLocationDelivery. Critério: Ergonomia, estados de pausa, diagnóstico, compatibilidade Android; sem ampliar manipulação de localização ou contornar controles. Prioridade original: P1.
29. [ ] MAP-07 — Pontos e mapas temáticos. N: catálogos de POI e mapas autorizados. Critério: Camadas por interesse, localização real/planejada, filtros, prévia 3D própria, favoritos offline. Prioridade original: P2.
30. [ ] COL-01 — Espécies e seleção. F: PokemonCategories, PokemonOptionFilter, PokemonSpeciesDialog, PokemonSelectorPolicy. Critério: Unificar IDs, formas e sprites; manter filtro conhecido/desconhecido. Prioridade original: P1.
31. [ ] COL-02 — Coleção, IV, nível e proteção. I/P: LT046 prévia individual/lote, etiquetas tri-state. Critério: Revisar positivos/negativos e ausência de dados; preservar favoritos, shiny, XXL/XXS, fantasia, fundo e formas; nenhuma classificação isolada autoriza transferência. Prioridade original: P0.
32. [ ] COL-03 — Evoluções, MT e fortalecimento. F/P: evolve_transfer.cpp, batch_evolution.cpp, #134, matriz QA. Critério: Consulta de requisitos, custo, ataques elegíveis e legados; simulador de PC/IV; prévia sem gastar recursos; confirmar fontes e eventos. Prioridade original: P1.
33. [ ] COL-04 — Pokédex e coleção visual. N: WWM + sprites autorizados. Critério: Busca por geração, tipo, forma, fantasia, fundo, shiny; checklist manual, progresso, filtros e exportação local. Prioridade original: P2.
34. [ ] COL-05 — Ranks PvP e avaliação. P/N: simulador local LT046. Critério: Rank por liga/limite de PC, nível, IV e versão da tabela; comparar evoluções, mostrar pressupostos. Prioridade original: P2.
35. [ ] INV-01 — Inventário e ícones. F/I: ItemBadge, ItemSpriteFit, LiveFeedIcons, #174/#181; galeria por ID. Critério: Poeira estelar, frutas, bolas, MT, incubadoras, evolução, módulos, poções e revives; auditoria de correspondência ID/asset, fallback legível; só assets licenciados. Prioridade original: P0.
36. [ ] INV-02 — Gestão de itens. F: arquitetura, inventário65; LT046 planejamento local de frutas. Critério: Resumo, reservas e alertas; prévia de consumo sem ações irreversíveis; alertas de limite. Prioridade original: P1.
37. [ ] CAP-01 — Captura, Quick Catch, Hunt/Farm/Transfer. F/P: Control, CatchOutcomeController, VgpController, release/mestre. Critério: Preservar interface e preferências; revisar feedback, Lenta 3s, estados e proteção; testar em fixtures sem ampliar automação do jogo. Prioridade original: P0.
38. [ ] VGP-01 — VGP+. I/P: LT045, VgpController; etiquetas/raridade #133 pendentes. Critério: Classificação e etiquetas locais; status correto, não confundir dado desconhecido com ausência; preservar proteção e compatibilidade. Prioridade original: P0.
39. [ ] FEED-01 — Sniper 1/2, Radar, Savant, coletor IV100. I/P: LT045 QA; SourceQuery, LiveFeedClient/Data/Filters/Dialog. Critério: Identificação e filtros de catálogo; rótulo de fonte/idade/ausência; nenhum feed privado ou coordenadas ao vivo presumidas. Provedor autorizado obrigatório. Prioridade original: P1/B.
40. [x] BAT-01 — Rocket: tela branca carregado. V5 #300 e aprovação do proprietário #301 prevalecem sobre o registro histórico a seguir; reabrir apenas com evidência de regressão. P: #193 HEAD 955eb76aaf9c48e1a25d0f51f741608fe8fa002f, integrada na main; correção candidata sem homologação física. Critério: Prioridade Rocket por ser fluxo compartilhado; documentar fase, comparar registros e visibilidade em ensaio autorizado; investigar material/overlay próprio e regressão sem remover função; efeito discreto somente em UI própria; nenhuma causa comprovada. Prioridade original: P0.
41. [ ] BAT-02 — Supereficaz e Ataque Inteligente. P: #193 candidata, política local e matriz. Critério: Validar types/moves como referência independente; simulações de tipo simples/duplo, resistências, metadados desconhecidos, dois carregados; separar recomendação local da execução real. Prioridade original: P0.
42. [ ] BAT-03 — PvP e treinamento. P: política local, testes, #90/#105. Critério: Simulador, recomendação de equipes, diagnóstico de lifecycle/rearme; não declarar executor homologado. Prioridade original: P0.
43. [ ] BAT-04 — Reides e ginásios. F/P: arquitetura, runtime e histórico LT; API RaidBoss/Pokebattler. Critério: Central de chefes, counters, tipos, clima, custos, equipe manual; avisos de agenda e disponibilidade. Prioridade original: P2.
44. [ ] BAT-05 — Max / Dynamax / Gigantamax. P: AutoMaxBattleSessionTest, preparação manual #174, #131/#154 pendentes. Critério: Central Max, papéis, elegibilidade, partículas, custos, sugestões de equipe; sem iniciar/gastar recursos automaticamente. Prioridade original: P1/P2.
45. [ ] BAT-06 — Central Rocket. N: invasions + grunt-quotes/grunts. Critério: Frases, possíveis equipes e fraquezas; mostrar fonte, data e incerteza, consulta manual. Prioridade original: P2.
46. [ ] EVT-01 — Eventos. I: #66, histórico LT033; menu Evento e Locais no Mestre. Critério: Calendário oficial, hoje/próximos, horários locais, bônus, espécies e ataques exclusivos, links e fontes; cache e expiração. Prioridade original: P1.
47. [ ] EVT-02 — Missões e pesquisas. F/P: research e busca documentadas; Quest types/conditions/rewards. Critério: Catálogo pesquisas de campo, temporárias, especiais, coleções e recompensas; checklist manual, alertas, filtros por item/espécie. Prioridade original: P1/P2.
48. [ ] EVT-03 — Planejador de eventos. N: integração de dados públicos autorizados. Critério: Agenda, lembrete opt-in, dia comunitário, hora destacada, hora de reide, GO Fest/Tour, Max, Rocket, bônus e janelas de evolução; fuso IANA e mudança de data; nada de ações de jogo agendadas. Prioridade original: P2.
49. [ ] EVT-04 — Presets e rotinas locais. N: preferências e eventos. Critério: Perfis de interface, filtros de consulta, lembretes, lista de tarefas e planejamento; prévia, confirmação, botão cancelar, histórico e reversão. Prioridade original: P2.
50. [ ] AUX-01 — Companheiro, ovos, presentes, medalhas. F/P: histórico e estados PT-BR no Mestre; escopo detalhado ainda não auditado. Critério: Painel manual de progresso, planejamento de incubação, alertas e glossário; não inventar estado de conta. Prioridade original: P2.
51. [ ] AUX-02 — Eternatus e funções sazonais. F/P: AGENTS e histórico; guardas já documentados. Critério: Catálogo sazonal, checklist e diagnóstico; preservar fluxo existente, sem conceder progresso por inferência. Prioridade original: P1.
52. [ ] SYS-01 — UI, menu e tradução. F: Pad, SettingsPanel, SettingsUi, SettingsSearch. Critério: PT-BR integral (catch/xfer/feeding e outros), estados, rótulos de acessibilidade, escala de fontes, tema claro/escuro, tablet. Prioridade original: P0.
53. [ ] SYS-02 — Estatísticas. F: SessionStats, SessionStatsHistory e testes. Critério: Painel 1h/24h, histórico, metas, exportação sanitizada, taxas com denominadores explícitos; sem afirmar exatidão antes de QA. Prioridade original: P1.
54. [ ] SYS-03 — Perfis, contas e privacidade. F: AccountProfileStore/Profiles/Dialog/SessionVault, SettingsWriter. Critério: Preservar isolamento, máscara local e backup; diagnóstico sem dados pessoais; exclusão e exportação explícitas. Prioridade original: P0.
55. [ ] SYS-04 — Cofre e IA opcional. I/P: #168/#173; prévia independente; sem provedor. Critério: BYOK somente com consentimento, Keystore, limite de gasto, explicação de envio, revogação, fallback; assistente de catálogo, sem ações de jogo. Prioridade original: P2/B. Decisão posterior: BYOK retirado; revisar apenas cofre local e privacidade, não reintroduzir integração.
56. [ ] SYS-05 — Dados e cache. N: Data Engine; já existem MapTileCache, cache de sprites, catálogos específicos. Critério: Normalizar, validar e atualizar snapshot offline por versão, checksum, rollback e adaptadores; não duplicar componentes existentes. Prioridade original: P1.
57. [ ] SYS-06 — GeoLab. I/P: #165, pesquisa separada. Critério: Laboratório de desempenho para dados geográficos públicos/sintéticos, não runtime de produção. Prioridade original: P3.
58. [ ] SYS-07 — QA, build e release. I/Q: workflows e matriz LT046. Critério: Rastrear HEAD, fixtures, assinatura/hashes, três variantes, QA Android; não publicar sem autorização explícita. Prioridade original: P0.
59. [ ] SYS-08 — Governança e documentação. F: Mestre e AGENTS. Critério: Catálogo por ID, mapa de dependências, registro de regressões, licenças, fonte e data; resolver conflito documental preexistente em PR separada. Prioridade original: P0.
60. [ ] DATA-01 — Inventário de bases já embarcadas e IDs. Critério: Diff contra WWM por revisão, sem substituir dados existentes cegamente.
61. [ ] DATA-02 — Manifesto de proveniência. Critério: Fonte, revisão, licença, hash, esquema, data, validade, atribuição e permissões de cada dataset/asset.
62. [ ] DATA-03 — Modelo comum e cache offline. Critério: IDs estáveis e namespace por tipo; atualização atômica, snapshot anterior preservado, tamanho limitado, migração testada.
63. [ ] DATA-04 — Traduções PT-BR. Critério: Chaves por categoria, fallback explícito, glossário de rótulos próprios separado de textos externos.
64. [ ] DATA-05 — Validação de tipos, ataques e formas. Critério: Fixtures com dados desconhecidos, dual-type, resistências, mudanças de moves, fantasia não lançada; sem usar metadados como prova de disponibilidade.
65. [ ] DATA-06 — Itens e sprites. Critério: Manifesto ID→arquivo autorizado, hash, dimensões, transparência, fallback, relatório de faltantes, testes de colisões.
66. [ ] EVT-05 — Ingestão de calendário. Critério: Fontes oficiais prioritárias; termos e atribuição; atualização por ETag/TTL quando suportados, horário local versus UTC, eventos expirados não ativos.
67. [ ] EVT-06 — Motor de lembretes locais. Critério: Opt-in, notificações Android, fuso IANA, cancelamento/edição, deduplicação, sem disparo de ações no jogo.
68. [ ] EVT-07 — Checklist inteligente. Critério: Plano diário/semanal manual; progresso confirmado pelo usuário, prioridades, reset por janela, estado desconhecido explícito.
69. [ ] EVT-08 — Central de bônus e janelas. Critério: Mostrar bônus, pesquisa, reide, Max, Rocket, ataques exclusivos com fonte, intervalo e elegibilidade; sem inferir garantia.
70. [ ] AUTO-01 — Catálogo de rotinas. Critério: Apenas rotinas de dados, atualização, cache, notificação, exportação e planejamento; opt-in, estado, auditoria, limites, pausa e cancelamento.
71. [ ] AUTO-02 — Regras de segurança. Critério: Nenhuma nova ação irreversível; teste de presets sem execução; não alterar runtime existente por integração de dados.
72. [ ] VIS-01 — Renderização própria. Critério: Canvas/vetores ou renderer independente; assets autorizados, benchmark em aparelho e fallback 2D.
73. [ ] VIS-02 — Rota visual automática. Critério: Pesquisa de interface de projeção autorizada; teste de perspectiva, zoom, rotação, perda de referência; se indisponível, usar mapa próprio e deixar overlay experimental.
74. [ ] QA-01 — Tela branca. Critério: Reproduzir primeiro Rocket, vídeo/log sanitizado, identificar camada real, comparar antes/depois, preservar visibilidade e interações legítimas; depois testar PvP/treino.
75. [ ] QA-02 — Suporte e regressão. Critério: Matriz Root/sem Root, troca de tela, tema, resolução, permissões, versão Android, carga, memória, bateria; sem marcar como executado até evidência.
76. [ ] AUTO-03 — Eternatus. EternatusRouteRunner.java; events/EventPanel.java; AGENTS. Critério: Preservar coordenador e progresso por resgate confirmado; catalogar modelo sazonal e regressões, não reescrever.
77. [ ] AUTO-04 — Halloween. events/EventSettings.java (HALLOWEEN_2026), EventPanel; histórico PR #67. Critério: Perfil sazonal e validade; fixtures históricas não provam evento real.
78. [ ] AUTO-05 — Safari / Área Selvagem. EventSettings (GO_WILD_NOVEMBER_2026), Control; histórico PR #67. Critério: Reutilizar apresentação e preferências existentes; catálogo de bônus, itens e elegibilidade.
79. [ ] AUTO-06 — Auto-Raid / batalha de reides. AutoRaid.java, SettingsPanel.java, Control. Critério: Código presente com ciclo de sessão; distinguir planejamento de execução; revisar regressões de status e cancelamento, sem declarar homologação.
80. [ ] AUTO-07 — Caçador de Shundo. ShundoSniper.java; SettingsPanel (Experimental). Critério: Preservar recurso e registrar dependência de fontes autorizadas; planejamento e filtros de catálogo, sem prometer encontro ou IV real.
81. [ ] AUTO-08 — Outras automações legadas. OneShotAutomation.java; Control; VgpController. Critério: Inventário de funcionalidades e limites, conflitos de sessão, pausa, encerramento e isolamento de perfil.
82. [ ] AUTO-09 — Editor de macros. N. Critério: Criar sequências de cartões editáveis, biblioteca, duplicação, importação/exportação versionada e prévia.
83. [ ] AUTO-10 — Motor de macros. N. Critério: Simulador offline, fluxo sequencial, condições, repetição limitada, interrupção, timeout, registros e teste de recuperação.
84. [ ] AUTO-11 — Templates de eventos. N. Critério: Modelos por evento, revisão/validade/fuso, checklist, planejamento de itens, pesquisa, reides e espécies; sem ativar evento expirado.
85. [ ] AUTO-12 — Rotinas de reides e falhas. P/N. Critério: Pedido de automatizar um “bug das reides” registrado sem detalhes identificadores; classificar como investigação de defeito, reprodução em ambiente autorizado e correção, não macro operacional de exploração.
86. [ ] AUTO-13 — Biblioteca e histórico. N. Critério: Favoritos, nome, descrição, revisão, execução simulada, exportação sem credenciais/locais privados, status e motivo de interrupção.
87. [ ] AUTO-14 — Permissões e QA. N. Critério: Capacidades explícitas, teste sem conta real, pausa/stop, sem concorrência destrutiva, sem ação desconhecida.

### Próxima ação executável

SYS-07/QA-02: #222–#227 integradas; conferir CI da main consolidada e manter recibos LT048. Retomar requisitos P0 de integridade e regressão, triagem histórica por equivalência comprovada e matriz de homologação física separada. Não confundir LT048 identificada com APK entregue.

Registro anterior (histórico): SYS-07/QA-02: LT048 identificada na main, #215–#218 integradas após CI aprovado. Verificar documentação e integridade da revisão consolidada; não anunciar APK sem compilação autorizada e verificada. Retomar fila original de P0/P1 e separar evidência física. PGBR-PR-TRIAGE: confirmar CI da reconciliação documental #182 antes de considerar encerramento por substituição.

Registro anterior (histórico): SYS-07/QA-02: conferir CI dos HEADs #215 (LT048), #216 (diagnóstico) e #217 (Locais); integrar apenas mudanças aprovadas e não conflitantes. Confirmar proveniência e processo de compilação autorizado antes de nova entrega; não reutilizar como LT048 o artefato QA LT047 14961904891. Retomar fila aberta pela prioridade original e preservar histórico, incluindo aceite físico separado.

Registro anterior (histórico): PGBR-CI-205: confirmar CI do novo HEAD #205 e corrigir eventual falha. Em paralelo, QA LT047 real disponível em https://github.com/LucianoToscano/PGBR/actions/runs/45482026778/artifacts/14961904891 ; publicação ainda distinta. Testar layout em condições de tamanho reduzido e preservar o estado das tarefas anteriores. O executor agendado deve retomar desta fila e manter IDs, evidências e histórico de cada ciclo.


### Ciclo 24/11/2026 — layout de viewport reduzido

PGBR-CI-205 integrado após CI completo; PGBR-UI-SMALL candidata de correção em `WorldMapDialog.java`, `MapLayoutPolicy.java` e `test_route_trace_java.py`. Sem mudanças em motor, movimento, rotas, captura ou batalhas; `device_tested=false`. Próximo: verificar CI no HEAD desta PR, testar viewport Android e confirmar primeira execução agendada do workflow na main.


### Ciclo 24/11/2026 — integração de viewport e correções independentes

- PGBR-UI-SMALL: correção de código integrada e verificada por CI; aceite em aparelho ainda não realizado. Não confundir com APK LT047 anterior.
- PGBR-PR-TRIAGE: cinco propostas substituídas encerradas sem excluir branches ou histórico: #195 (blobs de Java/teste idênticos à #201); #197/#198 (sufixos documentais presentes na main e catálogo #198 idêntico); #194/#196 (todas as linhas não vazias preservadas, exceto marcador de conflito removido). Triagem global permanece aberta.
- MAP-01/SYS-05: #208, HEAD 0aed289be53ff0ea7ca6644e7c7651f504825048; substituição atômica de cache, trava compartilhada e testes de falha de gravação. CI 45495173497/45495173519, conferir resultado final.
- MAP-03/SYS-03: #209, HEAD 1756b7ad034927b12ea5024127b652bc7f5a26b0; validação uniforme de GPX armazenado, rejeição de links simbólicos e arquivo canônico com múltiplas rotas; CI 45495347808/45495347684, conferir resultado final.
- SYS-02: #210, HEAD cd7c2f89ae71c6ef02921aea9ed9a2542bdb53f3; inclui leitura mais recente na interpolação dentro do último intervalo e rejeita janelas não positivas; CI 45495517445/45495517220, conferir resultado final.
- Próximo: integrar correções independentes após reconciliação e checks do HEAD exato, preservar as tarefas abrangentes abertas; conferir execução agendada da #205 e preparar nova QA quando alterações estiverem integradas. device_tested=false. Nenhuma publicação nova neste ciclo.


### Ciclo 24/11/2026 — recibos, GPX, cache e estatísticas

- MAP-03/SYS-03: #209 integrada em 8f3cca68406642a86710fca78f02b7a6e091389d, CI 45495347684/45495347808 aprovados. Correção de integridade de armazenamento GPX verificada; escopo MAP-03 completo ainda aberto.
- SYS-02: #210 integrada em c77e1bde89288ccc01450623a6355df194e0cca8, CI 45495517220/45495517445 aprovados. Correção da interpolação de janelas curtas verificada; demais requisitos de estatísticas abertos.
- MAP-01/SYS-05: #208 integrada em e01b841fd745e57cb05f580d972cbdc5921e4855, HEAD fae0eba7098f61dbfb38dc649e98dcfeaa401d9d, CI 45495955974/45495956007 aprovados. Cache atômico e concorrência corrigidos; demais requisitos mantidos abertos.
- SYS-08/PGBR-PR-TRIAGE: #212 integrada em ea1420ccd49abd60b4f797abfb41da31e0a1ebd0; CI 45495838111/45495838001 aprovados. #180/#190 encerradas após comparação textual de todos os arquivos, sem exclusão de histórico; sete propostas substituídas encerradas no total. Preservada prioridade Rocket e transparência, sem comprovação física.
- SYS-03: #213 HEAD 50b658ef8b7f1829e60a12ac1045fcc415f2c3c7, validação do índice local de contas; auditoria 45496180632 aprovada, código 45496180585 em execução na consulta. Não declarar integrada antes de confirmação.
- PGBR-RELEASE-047: recibo histórico acrescentado às notas LT047; artefato original não expirado, hashes individuais registrados pelo CI. Nenhuma nova compilação ou publicação neste ciclo. A próxima QA deve incluir as correções integradas e distinguir sua revisão do artefato LT047 anterior.
- Próximo: validar #213 e integrar somente se checks e comparação de arquivos confirmarem ausência de conflito. Preparar versão seguinte para QA consolidada e retomar requisitos abertos. device_tested=false.


### Ciclo 24/11/2026 — preparação LT048 e integridade local

- SYS-03: #213 integrada em b5da9b91c71e72303a5b0cb1aefbba94d5581561; CI 45496180585/45496180632 aprovados. Índice inválido não é reinicializado; validação em aparelho separada.
- #214 integrada em 6a044d6fd0e943c82ff4888bd388a8d6f5d0e26a; CI 45496827365/45496827406 aprovados. Recibo histórico QA LT047 preservado.
- SYS-07/QA-02: #215 HEAD 51ffc47afb8235abe10fbbe04f1d3ce0be02e5b5. Identificação candidata LT048, 15 contratos históricos e 10 casos incrementais de homologação; CI 45497444322/45497444474. Sem nova compilação ou publicação confirmada.
- SYS-03/QA-02: #216 HEAD 70add17a5e07a71cbc8d4b5d8aed1db92add6d1e. Limites de diagnóstico filtrado e validação antes de abrir destino; CI 45497730735/45497730714. Sem garantia de atomicidade de provedor externo.
- MAP-02/SYS-03: #217 HEAD 6004cf1857c0a8fee8fa06c3eb1576ed4dabdf11. Trava compartilhada no mesmo processo para mutações de Locais; testes concorrentes, CI 45497948852/45497948863. Transações entre processos e importações multi-etapas fora do escopo.
- PGBR-PR-TRIAGE: #184 comparada com main; VgpController.java, VgpControllerTest.java e test_vgp_appearance_two_choices.py têm blobs idênticos. Outros quatro arquivos diferem; não encerrar a PR sem reconciliar diferenças e Mestre histórico.
- Os 76 IDs e seis itens marcados permanecem. Não confundir correção parcial com conclusão de todos os requisitos. device_tested=false.


### Ciclo 24/11/2026 — LT048 e triagem histórica

- #215 integrada em 42e2ce33ed3b57b5adc223a9463d976948eab449; CI 45497444474/45497444322 aprovados. VERSION 0.00.48-LT e matriz incremental; nenhum APK LT048 confirmado.
- #216 integrada em 5be5a2cfc8d6ca9500321be7fe7a16a259a0a1d7; CI 45497730714/45497730735 aprovados. Exportação filtrada compatível com limites de resumo; não garante atomicidade do provedor externo.
- #217 integrada em 7a5d557e5b926291f2460ef50d68b46a8bde8176; CI 45497948852/45497948863 aprovados. Serialização local de mutações de Locais, sem garantia entre processos ou transações multi-etapas.
- #218 integrada em 94e4d6a033db0b919ca4e88281580e6273aeebb3; CI 45498140270/45498140526 aprovados.
- #184 encerrada após verificar três blobs idênticos, Mestre preservado e nenhuma linha nova ausente nos quatro arquivos divergentes; comentário 7145898538. Oito PRs substituídas encerradas no total, sem apagar histórico.
- #182: #183 integrada com a mesma finalidade de separação, API posterior explícita e testes de token/isolamento. Recuperado recibo histórico original (duas linhas não presentes na main); não reintroduzir código antigo. Manter #182 aberta até validação da recuperação documental e decisão registrada.
- 76 IDs, seis encerrados. device_tested=false; provider_connected=false; nenhum APK novo confirmado.


### Ciclo 24/11/2026 — integridade e continuidade LT048

- #219 integrada em d34434d9a96618d496b71618bc5c1db8fd42477d; CI 45499028422/45499028123 aprovados. #182 encerrada após comprovar recuperação de todas as linhas históricas adicionadas; comentário 7146134197. Nove propostas substituídas encerradas, sem apagar branches.
- MAP-02/SYS-03: #220 integrada em 80c3854dc68468c21da99951527b692a46b43129; CI 45499596415/45499596408 aprovados. Preserva original danificado quando backup inviável.
- SYS-07/SYS-08: #221 integrada em b1b4a579409f5972bb3b87833a531d9080a04740; CI 45500290845/45500290820/45500290817 aprovados. Auditoria da fila numerada na HEAD exata; não é executor IA.
- INV-01/DATA-02: #222 HEAD 3a5427c5c80847c5b488fae7f11e6217116ef200; sintaxe reparada após falha 45500723980. Nova auditoria 45501102327 aprovada; código 45501103417 em validação na consulta.
- COL-02: #223 HEAD 698d8a672e51616c36f3b8bbcfba5fc54bdb2915; proteção positiva IV extremo na prévia local. CI 45501246673/45501246674; sem alteração de transferência.
- SYS-01: #224 HEAD 1d3f0d7c1442f9255dce6f0a94dc1d6dca1636cb; busca Unicode e resultados adaptáveis. CI 45501436357/45501436477; aceite físico separado.
- BAT-01: #193 confirmada merged; tela branca Rocket e supereficácia ainda exigem teste autorizado, sem atribuição de causa física.
- Mantidos 76 IDs e seis concluídos. device_tested=false; provider_connected=false; nenhum APK LT048 confirmado.


### Ciclo 24/11/2026 — recibos LT048 e proteção de dados

- #222 integrada em c806ee8bac690df1811afd3532f290d30b336227; CI do HEAD da PR 45501103417 / 45501102327 aprovado. INV-01/DATA-02: unicidade e correspondência de IDs de ícones.
- #223 integrada em 8e0d2427376a6f732443cbf88d481fd125627350; CI do HEAD da PR 45501246674 / 45501246673 aprovado. COL-02: proteção IV extremo na prévia local.
- #224 integrada em f6aab3f5ab20513af574018aae752c19dba7a3f7; CI do HEAD da PR 45501436357 / 45501436477 aprovado. SYS-01: busca Unicode e acessibilidade.
- #225 integrada em 963650627aa9281bc3ef8b0684a6b70972c52b05; CI do HEAD da PR 45501588304 / 45501588364 aprovado. SYS-07/SYS-08: recibos e homologação.
- #226 integrada em 132479131c51193d4eef5491044cf64bfcde6dbf; CI do HEAD da PR 45501792994 / 45501793000 aprovado. SYS-03/MAP-02: validação de tipos e nomes antes da restauração.
- #227 integrada em 3eaae4a0adf2813713d54ded93923d6dfef7d667; CI do HEAD da PR 45502406072 / 45502406273 aprovado. MAP-03/SYS-03: codificação GPX antes de abertura truncante.
- #226 rejeita tipos incorretos e nomes inválidos antes da restauração. #227 preserva o destino quando a codificação falha, sem garantia de atomicidade após abertura em provedor externo.
- Main observada 3eaae4a0adf2813713d54ded93923d6dfef7d667. CI da revisão consolidada deve ser conferido separadamente. Mantidos os 76 IDs e seis concluídos; requisitos abrangentes permanecem abertos. device_tested=false; provider_connected=false; sem APK LT048 confirmado.

### 2026-11-24 — Consolidação das investigações PGSharp/Lua e diagnóstico local FEED-01

- PR #229: corrigidos diagnóstico de campos em registros elegíveis (nulos, demo e expirados), disponibilidade de rank na liga solicitada e consulta OU vazia, com cenários sintéticos em EventFilterEngineTest. CI da #229 aprovado (45514194051/45514194103); correção transposta para base consolidada, aguardar CI do novo HEAD; manter FEED-01 aberto e não afirmar fonte conectada.
- Arquitetura: preservar SpawnContract, SourceAdapter, SpawnFilters, EventFilterEngine, PresetCodec e SharedBattleOptions; configuração declarativa JSON versionada e validada, sem introduzir VM Lua ou duplicar modelos. Auditar capacidade por fonte, atributos ausentes, proveniência e validade; somente fontes autorizadas. Não inferir gênero a partir de forma, nem preencher IV/nível ausentes.
- BAT-01/QA-01: Rocket primeiro; comparar gravações e diagnóstico sanitizado da UI própria, identificar fase e camada do clarão, não presumir causa Unity/GPU. #193 permanece candidata, sem teste físico.
- BAT-02: manter comparação independente e testes locais de tipos, imunidades, dupla tipagem e valores desconhecidos. BAT-03: separar preferências persistentes de contexto efêmero por sessão/conta; estados desconhecidos não equivalem a zero; validar interface própria em cenários sintéticos e no ensaio autorizado. Não ampliar executor de ações do jogo.
- Referência histórica PGSharp 1.33.0: https://github.com/romainthomas/pgsharp e https://romainthomas.fr/post/21-11-pgsharp-analysis/ ; Lua no PGSharp 1.278.0 segue não comprovada. Arquivo de correlação no Drive: https://drive.google.com/file/d/13gF5iC0aZgEabsnco3o72uIG-CYg4kmU/view . Não importar bytecode proprietário. PvPoke e GoBattleSim como referências de simulação local, licenças a conferir antes de reutilizar código.
- Nenhum ID encerrado nesta rodada. device_tested=false; provider_connected=false; nenhum APK novo confirmado.

### Ciclo 25/11/2026 — código local e homologação separada

- BAT-02: #230 integrada em 6be33b63c5b86c61f1edab418755dc1a16674547, CI 45514546252/45514546359 aprovados. Testes da matriz local: 324 simples e 5508 duplas ordenadas; zero divergências na comparação independente. Subtarefa de teste de código concluída; execução no aparelho e demais critérios BAT-02/QA-01 continuam separados.
- FEED-01: #229 aprovada em CI mas conflito documental com main; dois blobs de código/testes preservados integralmente nesta branch, sem sobrescrever histórico da main. Revalidar novo HEAD antes de integrar e só então encerrar PR substituída.
- Lotes independentes de 10–15 subtarefas quando viável; não inflar contagens, preservar os 76 IDs, distinguir aceite de código e QA física. device_tested=false; provider_connected=false; APK LT048 não confirmado nesta rodada.

### Ciclo 24/11/2026 — lote acelerado de integridade, filtros e GPX

- FEED-01: #232 integrada em 467945105b5e6c17d19c7eb35065e8ed1a3c3376, CI 45515175889/45515175981 aprovados. #229 encerrada por equivalência comprovada (dois blobs idênticos na main, notas preservadas; comentário 7148200514).
- EVT-04/SYS-03: #233 integrada em d942b13a3ef53728d7efc6c012d42540964508ef, CI 45515249997/45515249888 aprovados. Presets: origem estável, versões e limite simétrico de exportação.
- SYS-02: #231 integrada em d876e22729045723a6bf46076aff40e9ab074309, CI 45514953258/45514953119 aprovados. Restauração cronológica e campos vazios, sem descartar histórico válido.
- MAP-03/DATA-03: #234 integrada em f332644d47392b4055e7a9630b34487e769d17a9, CI 45515320279/45515320294 aprovados. UTF-8, BOM, versão do formato e associação de nomes GPX.
- DATA-03/SYS-03: #235 HEAD c6574a2b0251792193c214a1deb1e96687556d47; validação de envelope, versão e lista antes de mutação, testes de preservação. CI 45515660354 aprovado, 45515660276 em execução na consulta.
- MAP-03/VIS-01: #236 HEAD 70a2086cbe2fed9c08e004f6a777578bb4d30cc8; limiares de progresso GPX e testes de ida/volta 0–100% em três rotas. CI 45515717355 aprovado, 45515717296 em execução na consulta.
- SYS-01/MAP-03: #237 HEAD 26a5f4b3301ac9070752daa5957b28b0aed52a93; tradução PT-BR das validações GPX e feedback de UI, quatro testes. CI 45515984710/45515984684 ainda em fila na consulta.
- Encerradas as subtarefas de código integradas acima; IDs abrangentes preservados. Aceite físico independente; device_tested=false, provider_connected=false. Nenhum novo APK LT048 comprovado. Continuar verificando HEADs, integrar após CI e preservar escopo de branches paralelas.


### Recibo de integração — lote #235–#238

- #235 integrada em `128c30b94c3024b34147cb5b7b0c01c8fcaa0fb6`, CI da PR 45515660354/45515660276 aprovado; validação de envelope local concluída em código.
- #236 integrada em `05b69e86fd6475b78d669473488d95ca12aa4392`, CI da PR 45515717355/45515717296 aprovado; limiares da prévia GPX corrigidos em código.
- #237 integrada em `11737b318ec26da76f31c0e1aa43bc3bd87bbb89`, CI da PR 45515984710/45515984684 aprovado; mensagens GPX traduzidas, QA física separada.
- #238 integrada em `622f49bb26eee08e7bef33dffc252ff054fd43a2`, CI da PR 45516042862/45516042856 aprovado. Na consulta subsequente, CI da main 45517452346/45517452317 ainda não concluído; verificar antes de build. Sem sobreposição de arquivos detectada na comparação antes de cada merge.
- Preservados os IDs e critérios abrangentes; nenhum APK LT048 comprovado, device_tested=false, provider_connected=false. Próximo: validar a revisão consolidada, avançar código independente e acionar build oficial por executor autorizado.


### Lote LT048 — regressões de estatísticas e cache

- SYS-02: #245, HEAD 609e2886fbf2487ea36be19f0c634e03e7683dde, corrige ganhos fictícios em timestamps simultâneos, mantendo a primeira amostra de slots com horários distintos; inclui teste de restauração e ganho posterior. CI a verificar.
- MAP-01/SYS-05: branch `fix/lt048-cache-trim-reopen`, base `a605ceea945f7df63ce710b2b53a575fbddc9a77`: MapTileCache passa a executar trim também na primeira gravação bem-sucedida de cada instância, mantendo intervalo de 32 gravações. Evita que reaberturas curtas adiem indefinidamente a limpeza. Teste Java host extraído do método real, 100 reaberturas simuladas e periodicidade. CI a verificar. Sem alterar rede, movimento ou jogo.
- #243/#244: checks documentais aprovados, suíte de código ainda em andamento na última consulta. Sem APK novo ou homologação física confirmados; requisitos abrangentes preservados.


### LT048 — integração e novo lote de código

- #245 merge 0b78ab729cd51b5a437d00f9a463b13f96789297, CI 45518975698/45518976132 aprovado. #247 merge 83c7f2aca8559bfc7711ccee525af5bbf1d75942, CI 45519061722/45519061926 aprovado. #248 merge b5861f1a4d6aec6f0aded2f8463b4be07cdbf2b9, CI 45519113153/45519113237 aprovado. #249 merge 9ef3dabacf227af43183628df5f4d09a5fa6dfb0, CI 45519293193/45519294836 aprovado.
- SYS-02: #250 HEAD b3a164b19072c7f59d07bee993876217e40a3c87 combina #245 integrada com #246 aprovada (45519061890/45519062287), preservando todos os testes. Novo CI 45520347773/45520347718; encerrar #246 apenas após integração e comparação.
- COL-02: #251 HEAD c8ef16bde66e80fa0b91665aa33cad14086323da, seletor exclusivo XXL/XXS em busca manual copiável, API antiga preservada, 240 combinações de teste; CI 45520427236/45520427290. Sem ação na coleção.
- MAP-03/SYS-03: #252 HEAD 0a4768f251d5ee4aa28a99d0d88f5fb0046e5a1f, nomes Unicode canonicamente equivalentes não sobrescrevem entradas, falha de leitura não representa biblioteca vazia; CI 45520467043/45520466964. Testes de preservação.
- CI main 1b1c84388bdbe4d4949ca14195329f471708fe37 aprovado (45519197070/45519196972); CI da nova main 9ef3dabacf227af43183628df5f4d09a5fa6dfb0 45520301024 ainda em execução na consulta, auditoria 45520300957 aprovada. Nenhum APK LT048 comprovado; workflows de build identificados ainda referem LT047. Não relabelar artefato antigo. device_tested=false; provider_connected=false; preservar IDs abrangentes.


### LT048 — recibos de integração e correção de interface

- #251 merge da0a00ee32c38e73f85358625f7b6efc77fa66fc, CI 45520427236/45520427290 aprovado. Busca copiável XXL/XXS exclusiva. #253 merge 15fe953ca787886f0cb5f0c81350edee0b7f0efc, CI 45520559873/45520559810 aprovado.
- #255 merge cd56e6ec36087d1a5a99e205484183b891de4be6, CI 45520661488/45520661232 aprovado; nomes de Locais comparados por NFC. #257 merge ebf0eb67f4a2665d1c7b8b4f7cda9629c7b2342d, CI 45520782614/45520782015 aprovado; cancelamento da leitura na busca de locais.
- #258 merge 6dcca9b7766fb2e14186dbb6397cf5b5220a95e2, CI 45520800266/45520800236 aprovado; apresenta separadamente configuração e disponibilidade da sessão PvP, sem comprovar execução. #259 merge 375e76eb9d3da86180b4b246b793fb345973b498, CI 45520927516/45520927309 aprovado; escala inválida volta ao padrão e mantém controles acessíveis.
- COL-02: #261 HEAD 5507b6cb511ab5bf90b142b466b9e7e87579c4c0, nomes inteiramente compostos por espaços Unicode rejeitados na prévia local; quatro regressões, CI 45521434503/45521434499 em andamento. #256 HEAD a6d15291f80dbdb6a8edcb26a52244bf78d0a2e3 corrige fixture Unicode; #260 HEAD 69da7257277d3ac12bce61bf3d7532b50a76ceab preserva eventos de encerramento em diagnóstico filtrado; ambos aguardam suíte de código na consulta.
- CI consolidado anterior cancelado por novas integrações; revisão 375e76eb9d3da86180b4b246b793fb345973b498 tem checks 45521465870/45521465648 ainda pendentes. Nenhum APK LT048 confirmado. QA física separada; device_tested=false; provider_connected=false. Sem encerrar IDs abrangentes nem atribuir correção de batalha a mudanças de apresentação.

### Consolidação temática deste chat — PGSharp/Lua, Radar e PvP

- Registro detalhado e referências no [Mestre único](PGBR_ARQUIVO_MESTRE_UNICO.md), seção "Consolidação integral temática do chat PGSharp, Lua, Radar e PvP (LT048)". Preservar inventário histórico Lua, materiais Drive, limites de evidência moderna, fontes autorizadas, decisão Java/C++ e JSON; não duplicar modelos nem inferir atributos ausentes.
- BAT-03: #258 integrada em 6dcca9b7766fb2e14186dbb6397cf5b5220a95e2, CI 45520800266/45520800236 aprovado; interface apresenta configuração, disponibilidade e sessão separadas. Auto ATK após fim/troca de janela/conta (#105) ainda não corrigido integralmente. Preferências persistentes e contexto efêmero separados; não persistir estado de combate. BAT-01/QA-01: clarão no carregado Rocket primeiro, depois PvP/Treino; #193 candidata, sem causa comprovada. BAT-02: #230 matriz local aprovada; execução real não comprovada, #90 mantida.
- MAP-01/SYS-05: #240 integrada 4bf51b776c3b01c2b6eca8e3d0f1a0fd81516a3b (CI 45517573559/45517573508); #242 integrada 606ae0f26e0b9b8345e1ec3d912a21e694c0b1cb (CI 45517724069/45517724089). MAP-03/DATA-03: #241 apresentou falha CI, sucedida pela #243 integrada 25041658df6c52814183df4b2bd5b19f6cc172a5 (CI 45518593302/45518593279 aprovados). EVT-04/SYS-03: #244 integrada 1b1c84388bdbe4d4949ca14195329f471708fe37 (CI 45518678982/45518679186). SYS-02 #250 integrada 0768ad5a1f779b0d615b39353b74151fc27b6e34 (CI 45520347773/45520347718). MAP-03/SYS-03 #252 integrada c2539574832c1dbe0a5cb1fcc66e604135d1d41c (CI 45520467043/45520466964).
- Modo acelerado preservado; subtarefas integradas não dependem de QA física para aceite de código. Preservar 76 IDs e critérios abrangentes. APK LT048 ainda não comprovado, device_tested=false, provider_connected=false.


### LT048 — consolidação P0 #285 / #287 (candidata)
- Base main `91b3827efadfe7207a60688dac99dd98f9c8fa8d` (inclui #289 PT-BR). Preservados os arquivos de inicialização e testes do hotfix #285 (`e5c0777b`), cujo CI e QA 45526952847 passaram, artefato 14983826362. Causa do crash ainda necessita confirmação física (#284).
- Fantasma → Aço é neutro (1×), conforme https://pokemondb.net/go/type ; a correção #287 tinha uma linha Java de 17 valores e CI 45528082540 reprovado. Corrigida a célula 9 sem deslocar Psíquico, Sombrio ou Fada; matriz Java verificada estruturalmente 18×18; preservada referência independente e casos duplos de #287. Nenhum executor alterado.
- Branch isolada de consolidação, workflow QA sem publicação. CI e APK desta HEAD ainda não confirmados. Não encerrar #285/#287 nem issues físicas antes de equivalência e validação. Preservar registros anteriores e demais requisitos; `device_tested=false`.


### P0 #192 — retorno físico: branco ausente, assistência do minigame não funciona
- Proprietário relata que o clarão sumiu mas agora precisa acertar os círculos manualmente. Não marcar BAT-01 homologado. A revisão anterior abandona geometria não reconhecida e não há camada branca independente identificada no código. Não restaurar escala visual de 1000× nem presumir alpha de material.
- Candidata isolada sobre #290 (3546da64): corrigida leitura além do `System.Single` do raio e rejeição indevida de eixos não dominantes do colisor esférico; mantidas guardas de limites, BoxCollider, restauração e ausência de escrita visual. Logs PGRazorMini com motivos fixos, uma vez por processo, sem dados do treinador. Teste de esfera achatada e contrato de leitura adicionados. Se persistir, identificar motivo e classe real por diagnóstico em ambiente autorizado; não afirmar restauração da função sem teste de dispositivo. CI/QA desta revisão ainda não confirmados. Nenhum outro sistema alterado neste lote.


### P0 25/11 — nova evidência de treinamento e controles em combate
- Log enviado pelo proprietário: 01:40:39 e 01:40:54 selecionou Charge RESISTED quando Charge2 NEUTRAL ainda não estava pronta (`only-one-ready`); vídeo mostra Mergulho contra Sealeo, não muito eficaz. 109 observações stale-effect; não confundir com 109 ataques. Nenhuma prova de HP/dano mínimo/escudo para exceção de nocaute. Política VNext agora bloqueia a inferior quando a superior conhecida ainda carrega, inclusive normal vs resistido; empate continua imediato, seleção manual e preferências OFF preservadas. Teste de política e fixture do executor real com objetos sintéticos.
- Atalhos Ágil/Carregado/Escudo no overlay durante escopos Rocket/PvP/Treino reconhecidos, vinculados aos bits existentes, sem criar executor. Outros modos não abrangidos neste lote. Validação de layout em aparelho necessária.
- #192 continua aberto: proprietário pede SOMENTE alpha=0 no efeito branco específico, sem alterar funcionamento anterior nem ocultar renderers indiscriminadamente. PR #293 é candidata distinta com QA, NÃO aplicar merge automaticamente porque usa supressão de renderer e não ajuste comprovado de cor. Identidade de material/cor e blend ainda não comprovadas. Sem regressão visual declarada concluída. #293 preservada para revisão, não sobreposta.


### P0 #192 — candidata de cor exclusivamente (alpha=0), consolidada sobre #294
- Não usa forceRenderingOff, não muda shader, não desliga componente, não esconde câmera, não altera RGB nem escreve material compartilhado. Procura um único Renderer no objeto exato do tapCollider, um único material, e confirma branco semitransparente por leitura de Color (ABI 16 bytes); altera apenas A=0 do material de instância, confirma leitura, mantém geometria histórica quando confirmado e restaura alpha/escala na saída. Se não houver identificação inequívoca, mantém caminho de geometria limitada existente. Fixture testa cor/RGB/alpha, rejeição de cor diferente, múltiplos materiais, proprietário incorreto, restauração e falha do setter. A identidade do efeito branco real e o suporte ao alpha pelo shader ainda exigem teste no aparelho; não confundir leitura da propriedade com resultado visual. #293 preservada sem merge; #294 incluída nesta candidata. Sem exceção de nocaute sem dados válidos; sem liberar homologação automática.


### P0 #192 — reprovação física reportada após oferta de Alpha Zero
- Usuário relata novamente que não corrigiu; SHA efetivamente instalado não fornecido. #295 não integrada, código/QA verdes não comprovam funcionamento. Não liberar como corrigido. A recuperação da geometria antiga depende de um material branco local único; se não for localizado, a candidata volta ao caminho limitado que já falhou fisicamente.
- Candidata de diagnóstico: exporta vínculo alfa/forma, callback observado, thread/política e estágios de seleção/resultado da cor, com enums fixos e sem dados de conta. Não muda ação, geometria ou gates. Testes de transporte filtrado e coleta de bits. Objetivo: diagnóstico pelo próprio ZIP, sem ADB, para evitar outro ajuste por tentativa. Não é correção funcional nem homologação.


### P0 #192 — restauração explícita do coletor pré-correção visual
- Ordem do proprietário: restaurar o mecanismo funcional anterior mesmo se o branco persistir; a alteração de alpha não pode impedir a assistência. Referência histórica 21badfd1: snapshot de scale/active, loop original de escala e SetActive recuperados; removida a seleção obrigatória por geometria/material. Alpha opcional somente no material local identificado, sem latch de erro do coletor por falha visual. Preservados gate original, restauração, diagnósticos, prioridade/controles #294 e demais alterações da base #296. Teste host do do_collect com material branco, material não correspondente, exceção do setter e binding ausente; não é teste no jogo. Pode voltar a aparecer branco se efeito real não for identificado ou shader ignorar alpha; não homologar até teste físico.


## 25/11/2026 — BAT-01 V5 CanvasRenderer (PR #300)

- Relato físico: o branco persistiu na QA da PR #298; não homologada. A origem UI é hipótese ainda não confirmada no dispositivo.
- Entrega Gemini `55df3948d09789488db100bf6087b3ba65635fa8`, patch `0003-gemini-fix-charged-white-layer-canvas-alpha.patch`, portada via API sobre a PR #299 (`aa2ba5f0ebb1039e1c6fc34063b5998851c2e9fe`, base `e28ca3f`). PR #300 contém #299; não duplicar integração.
- V5 opcional: `CanvasRenderer.GetAlpha/SetAlpha(0)` localizado na subárvore do colisor, captura/restauração do alpha, busca pós-ativação e reforço por frame de renderizadores capturados. Mantidos V1–V4, escala funcional 1000×, SetActive, colisor, running e lógica de batalha. Reforço condicionado a V5; diagnósticos 21–25 da #299 preservados, 26–31 para V5.
- Fixture host Gemini adaptada e registrada em `tools/native_host_tests.py`. Gemini reportou 5 cenários aprovados; CI desta integração ainda não confirmado. Verificar `miniCanvasAlphaBound` e `white-canvas-alpha-zero`/`white-canvas-reenforced`; se persistir, não ampliar ocultação sem identificar o componente exato.
- Estado: PR aberta, sem merge ou APK confirmado; `device_tested=false`. Não declarar resolvido até homologação física. Issue #192 permanece aberta.


## 25/11/2026 — BAT-01: V5 homologada pelo proprietário (SUCESSO)

- **Resultado físico informado pelo proprietário:** “Funcionou! Foi corrigido! Agora e registrar como sucesso!” A correção da camada branca no ataque carregado foi aprovada pelo usuário. `white_layer_device_tested=true` (relato do proprietário, escopo BAT-01); não generalizar para outras funcionalidades, modos de combate ou homologação global do APK.
- **Referência funcional a preservar:** PR #300 (inclui #299), main `ec3bfc5fbab6e62546e99460bdf9d987d233685e`, com V5 CanvasRenderer alpha 0, reforço por frame, restauração visual e fallback V1–V4. Não alterar escala 1000×, SetActive, colisor, running, coletor funcional da #297 ou lógica de combate ao trabalhar em apresentação visual. Preservar testes de regressão.
- CI da PR: 6/6 aprovado (run 45564763588 e 45564763611). Build QA concluído com sucesso: run 45566111133, artifact 15001260160, `PGBR-LT048-Root-e-SemRoot-QA`, commit `ec3bfc5fbab6e62546e99460bdf9d987d233685e`. SHA-256 do APK Sem Root produzido: `080e4e1e241ce71da38d06ffbc2f89c85cb059ebbb14ab79846665df8cdfa0de`. Não foi informado o SHA efetivamente instalado, modo específico ou log físico; a confirmação funcional é o relato direto acima.
- **BAT-01 / issue #192: aprovado pelo proprietário, não repetir correção sem evidência de regressão.** As issues #90 (Supereficaz) e #105 (Auto ATK) não recebem homologação por este relato. A solução anterior #298 permanece registrada historicamente como tentativa visual insuficiente. Não atribuir causalidade exclusiva ao CanvasRenderer sem diagnóstico do dispositivo.


## Lote upstream v0.3.7 — 25/11/2026
- #304 integrada: consulta de versões estáveis, instalação manual.
- #306/#307 integradas: Locais e critérios locais Dynamax/Gigantamax.
- #308 integrada: visibilidade temporária do joystick, merge f199496edb21db9cf5253563f2020b19e8423194.
- #309/#310/#311 integradas no merge 53f2029d76715506da7c4aa4825c13516bcc8aca; CI conjunto 45731449524/45731449535 aprovado; sem teste físico.
- Nova candidata: validar variante e manifesto na consulta automática de atualizações; planejamento manual de bônus com estoque desconhecido explícito. Não são novas automações de jogo.
- P0 restante: versão 0.439.0 não validada; QA física das regressões #90/#105; release LT049 não gerada.

- Entrada 0.439.0: hashes de arquivo/splits e CRC verificados localmente; certificados e compatibilidade ainda não verificados. Ver tools/game_input_0439_manifest.json.


## 26/11/2026 UTC — recibo #312 e validação de entrada
- #312 integrada: merge daeda9cfdae5f96bbf4f74933a60b88878fdfe08, HEAD 260d94178299dea72f5fcab17d32f8d06f827de9, workflows 45732025138/45732024960 concluídos com sucesso.
- Verificadas localmente as duas partes do APKM, 85000000 e 84083658 bytes; concatenação SHA-256 c6d3c04c38ca2f2446957633b4fee8c93dcc9cebdbbaddc9cb9651722ef8f54f. Nova candidata adiciona reconstrução temporária com hashes fixados e verificação opcional por apksigner oficial, com assinatura esperada do perfil aprovado anterior; teste de assinatura real ainda não executado. Perfil 0.437.0 mantido.

- #313 integrada com CI 45734462973/45734462962 aprovado, merge 74d98a31df205dda822348f5a387aca0a7390a89. Assinatura real 0.439.0 ainda não confirmada. Nova candidata SYS-01/SYS-03: ocultar coordenadas na lista de contas em modo privado, preservar lifecycle da cópia de aparência e respeitar desativação de avisos pendentes. Homologação física separada.


## 26/11/2026 — Checkpoint Arena LT049: QA local e bloqueios P0

Branch ativa `arena/1be548a6-pgbr`, base/HEAD `2357ae1d74b7fdad200fb695d55978d395954a83`; nenhum código de release, APK ou perfil foi alterado neste ciclo. Mantidos BAT-01 V5 e `current.json` em 0.437.0. PR #315 segue como receita QA baseada em 0.437.0; seus checks não satisfazem o alvo obrigatório 0.439.0.

- QA local: `python -B tools/native_host_tests.py` passou (27 fixtures C++ host, incluindo `rocket_minigame_white_layer_test`; sem cobertura de Android/dispositivo). `unittest discover -s pgrazor/test -p 'test_*.py'`: 519 testes, 78 skips e 1 erro de ambiente: teste Java real `test_iflowgo_request_policy_java` não encontrou `javac`. `python -B tools/check.py` parou antes de modificar arquivos: faltam `java`, `javac`, Android SDK platform/sources `android-36`. Não interpretar como falha funcional nem como suíte verde.
- P0 startup #284 permanece aberto. O hotfix #290 e seus checks de CI (run 45538006194; QA 45538006290) foram aprovados, mas o proprietário reportou novo crash físico em 25/11 sem hash do APK instalado nem logcat. Portanto o suspeito regex está corrigido no código atual, mas não é causa física confirmada nem aceite de inicialização; não mascarar, apagar dados ou distribuir como corrigido.
- Entrada 0.439.0: manifesto local fixa APKM e splits; certificado real continua sem validação criptográfica nesta sessão; perfil/runtime compatíveis não demonstrados. Não ativar `current.json` nem compilar LT049 a partir do perfil 0.437.0.
- A busca exata do `PGBR_Lista_Geral_Pendencias_2026-11-25.docx` não encontrou resultado em Drive nem Gmail; documento continua pendente, sem substituição por arquivos parecidos.
- Próximo: obter hash exato e logcat sanitizado da reprovação de startup e testar em aparelho autorizado preservando dados; obter JDK/SDK para suíte completa; concluir validação assinada e inspeção/compatibilidade 0.439.0 antes de elaborar perfil novo. `device_tested=false` para LT049; `provider_connected=false`.

## 26/11/2026 — Lote LT049 #316: gate estrito da assinatura de entrada

Incorporados ao branch ativo `arena/1be548a6-pgbr` os três commits de código/teste/documentação de #316 por cherry-pick, após confirmar base comum `2357ae1d74b7fdad200fb695d55978d395954a83`, revisão sem conflito de arquivos de runtime e CI do PR verde (suite real Java/Root, NDK ARM64, host C++, website e governança). Commits locais: `d2f9c5c`, `c859abf`, `55a1b71`. O verificador agora tem opção `--require-signatures`, falha antes do relatório quando falta apksigner confiável, e reporta explicitamente `signed_manifest_identity_verified=false`; inspeção hash-only segue disponível e não é apresentada como validação criptográfica. `python -B -m unittest pgrazor.test.test_candidate_input -v`: 5/5 aprovados; `git diff --check` aprovado. PR #316 continua aberta na origem; não mesclar novamente o mesmo patch. Assinatura real, identidade de manifesto, compatibilidade de execução, migração para 0.439.0 e build LT049 continuam pendentes.

## 26/11/2026 — Reconciliação da branch ativa com main após #316

`origin/main` avançou de `2357ae1d74b7fdad200fb695d55978d395954a83` para `617e91f79ac57f9fcd61ad57c380ce977c2da0ed`, merge da #316. Reconciliação da branch `arena/1be548a6-pgbr` em andamento, preservando os commits próprios e mantendo uma única cópia dos arquivos funcionais do gate. PRs #317 (manifesto assinado) e #318 (integridade de sprites) são trabalho paralelo declarado do ChatGPT; acompanhar checks e evitar duplicar seus arquivos. Para a frente LMArena, priorizar código de Map/Locais/GPX/visual/PT-BR em commits independentes, sem aguardar bloqueios 0.439.0/#284/dispositivo. V5 permanece intocada.

## 26/11/2026 — MAP-01 / acessibilidade do mapa por teclado

Em `WorldMapDialog.MapView`, o mapa passa a aceitar foco de teclado: setas deslocam o viewport (sem mudar posição do jogo), Enter/centro seleciona as coordenadas do centro no rótulo/preview e anuncia a leitura do rótulo de coordenadas atualizado. Não chama listener de teleporte, `walk.onDestination` ou movimento. A descrição acessível informa esses comandos; chave PT-BR sincronizada em `PtBr.java`, `RootPtBr.java` e `i18n/pt-BR.json`. Novo contrato `test_worldmap_keyboard_accessibility.py` cobre foco, direções, seleção sem movimento e sincronização textual.

Validação deste lote: 24 testes direcionados aprovados; 4 JVM reais de projeção/layout ficaram skipped por ausência de JDK; contratos estáticos e `git diff --check` passaram. Sem alteração de GPX, movimento, preferências, rotas oficiais, POIs ou V5. Ainda exige build Android/uso com teclado e leitor de tela autorizado; `device_tested=false`.

## Continuidade LMArena — PR #319 e triagem #200/#199

PR [#319](https://github.com/LucianoToscano/PGBR/pull/319) aberta da branch ativa com escopo MAP-01 restrito; `translate` passou, demais checks estavam pendentes na última consulta. #317 estava com todos os checks verdes; #318 ainda aguardava `Suite real check.py + compilação Java + Root`; não sobrepor nem duplicar esses arquivos. Issue #200 descreve capturas antigas cujo hash do APK não foi identificado e cujo mapa não estava em modo de rota ativa; código atual já tem linha compacta, filtros recolhidos e rota trace opt-in (testes de contrato passaram). #199 é protótipo visual divergente e não deve ser integrado sem reconciliação, CI na main atual e evidência de projeção autorizada. Não alegar homologação visual nem alterar V5.

## 26/11/2026 — MAP-01 / validade explícita de POIs do mapa

Continuação independente: `Control` expõe a idade monotônica do snapshot da camada de locais próximos junto com o conteúdo. Após 10 s, os pontos são descartados (`fail closed`) e o estado mostra “dados expiraram; aguardando atualização”, em vez de apresentar um feed vazio como se nunca tivesse carregado. Estado desconhecido inicial é diferenciado de expirado. Política isolada em `MapPoiFreshness.java`; fixture executável Java verifica limites 0/9.999/10.000 ms, idade negativa e descarte de conteúdo, mais contrato estático de status.

PT-BR sincronizado em Sem Root/Root/glossário e fixture dinâmica. Validação: nesta fatia, 21 testes passaram e 5 foram skipped (JDK ausente); compilação/execução Java real não ocorreu aqui. Erro de contrato antigo no teste de apresentação após alteração da interface foi atualizado e reexecutado com sucesso. Essa fatia integra PR #319, ainda sem CI completa na última consulta.

### Validação ampla local (26/11)

`python -B -m unittest discover -s pgrazor/test -v`: 526 testes, 79 skips por ferramentas/dispositivos ausentes e 1 erro de ambiente no teste existente `test_iflowgo_request_policy_java.test_actual_java_policy` (`FileNotFoundError: javac`). Não é falha da lógica MAP-01; comprova que a suíte completa local não ficou inteiramente verde. Os focados de MAP-01 acima passaram; a checagem real Java/Root do CI da #319 ainda estava pendente. Não concluir build Android nesta máquina.

### Correção de CI MAP-01 identificada após publicação

No HEAD `364d567`, o job GitHub `Suite real check.py + compilacao Java + Root` falhou no teste que compila `Control.java`: a fixture `test_control_java.py` lista explicitamente fontes Java, mas ainda não incluía o novo `MapPoiFreshness.java`. Inspecionei as anotações do check-run; a falha não era regra temporal nem compilação Android. Atualizado o input-source list do harness e acrescentado contrato que impede nova omissão. Reexecutar CI no novo HEAD; não considerar a falha do HEAD anterior corrigida sem resultado verde.

## 26/11/2026 — nova main após #317 e reconciliação de entrada

`origin/main` avançou para `84691417ea6c89be545db39a1af5d2909984b699` ao mesclar PR #317 (assinatura/identidade de manifesto); diff de main foi só em candidato/docs/tests e não conflita com MAP-01. Merge não-ff concluído e enviado como `e498ce6a`, conservando os dois checkpoints documentais. PR #318 segue aberta; Java/Root estava pendente na consulta mais recente. PR #319 está no HEAD `e498ce6a026a52142fbd875477bd5029d30fdbfd`, base main. Corrigida a omissão de `MapPoiFreshness.java` no harness explícito de Control. CI manual do HEAD atual `45743167687` passou em todos os seis jobs (inclui suite real `check.py`, Java/Root); workflow de governança `45743171548` também passou. Os checks associados à PR #319 (run 45743161378, governança 45743161217, translate 45743161408) estão todos verdes. #317 está merged; não duplicar #317/#318.

## 26/11/2026 — main após #318 (integridade de sprites)

`origin/main` avançou para `18a5cbeb665acba00a0211c64f07e3a8cc0d89ee` ao mesclar #318 após seis checks verdes (run 45742981881, suite Java/Root 3m03; governance 45742981824). Patch de `pgrazor/build.py` e fixture #318 foi recebido via merge não-ff e preservado em `67642193`; conflito exclusivamente documental resolvido conservando ambos os registros. Fixture #318 passou localmente (2/2). CI da revisão `67642193` passou: PR code run `45743740630` (6/6 incluindo Java/Root), governance `45743740395` (também manual `45743744722`) e PT-BR translate `45743740326`. PR #319 continua aberta; não remover trabalho de terceiros.

## 26/11/2026 — main após #322 e CI MAP-01 reconciliado

`origin/main` atual `2f559ef912213fd82ffe785409fa09b0b863fbad` inclui #317, #318 e #322 (hardening do manifesto de sprites). Main foi mesclada na branch ativa em `ce312b3743c70953e7d4cb06d65405735df5fb33`; conflito somente documental, preservando ambos os históricos e sem cherry-pick repetido. A fixture atualizada de sprites passou localmente 4/4.

PR #319 HEAD code `ce312b37` permanece aberta. CI do PR `45744289804` passou 6/6, incluindo `check.py`, compilação do app Java e Root (4m19s); governança `45744289838` e PT-BR `45744289872` também passaram. Falha histórica no head `364d567` foi corrigida ao incluir `MapPoiFreshness.java` no harness. Commit subsequente `b2f6b3c8` é documentação/merge only; não confundir CI verde anterior com validação física. `device_tested=false`.

Reconciliação candidata MAP-01 #319; CI atual obrigatório, device_tested=false.


## LT049 — atualização obrigatória para 0.439.0
- #314 integrada no merge 2357ae1d74b7fdad200fb695d55978d395954a83, CI 45735208617/45735208658 aprovado.
- Proprietário determinou LT049 sobre 0.439.0, não 0.437.0. Receita #315 atualizada: gate de identidade 0.439.0 antes de recuperar o original e assinatura, sem fallback; verificação apksigner antes da compilação. Perfil ativo ainda 0.437.0 até validação da migração. Original íntegro e hashes internos correspondem ao upstream; 537 entradas comuns alteradas e 25 específicas PGBR ausentes no upstream. Compatibilidade e arquivo no draft privado ainda não confirmados. Sem build ou publicação.

Reconciliação QA LT049 #315: receita 0.439.0 separada, gates de assinatura e identidade obrigatórios; perfil ativo anterior preservado. Sem APK gerado nem teste físico.


## 26/11/2026 — LT049: feedback de oito capturas do proprietário (lote FB-049-01)

Status: ABERTO; capturas de uma versão instalada sem SHA informado. Reconciliar com main e PRs em andamento, evitando duplicação de implementação. Itens abaixo são critérios específicos de aceite dos requisitos existentes, não substituem o histórico.

88. [ ] FB-049-01 / P1 — Shundo Sniper (AUTO-07): investigar interrupção de carregamento registrada após 26 deslocamentos, 22 verificações, 0 brilhantes, 0 capturas e 4 perdidos. Diagnóstico e estados de pausa/erro/cancelamento coerentes, mensagem completa em PT-BR, testes de indisponibilidade em fixture offline; preservar limites e parada segura, sem ampliar automação.
89. [ ] FB-049-02 / P1 — Fontes PGSharp 100% IV e Standard: verificar disponibilidade de integração oficialmente autorizada, licença e contrato documentado; chave armazenada não significa autenticação. Separar origem, acesso e erros; não apresentar dados de outra fonte como PGSharp, não contornar autenticação, não expor chaves. Manter bloqueado acesso sem autorização comprovada.
90. [ ] FB-049-03 / P2 — Pesquisas: completar apresentação de recompensas (sprite de encontro, ícone e quantidade de item, espécie e quantidade de Megaenergia/doces quando conhecidas); distinguir dados ausentes de zero. Catálogo autorizado/local e testes offline.
91. [ ] FB-049-04 / P3 — Pesquisas: resolver rótulos CHALLENGE_BASE_CATCH_WATER_JUNE23 e CHALLENGE_GOWA2026_LEADOUT_03 por catálogo PT-BR verificado; fallback legível sem inventar objetivo; substituir Candy/Item genéricos quando houver dados.
92. [ ] FB-049-05 / P2 — Pesquisas: diferenciação comum/evento/bônus/patrocinado; busca, seleção total/parcial, preservação de escolhas por ID estável após atualização e distinção entre manter e excluir. Reproduzir 25 evento, 18 encontro, 15 item (14 selecionados) da captura como fixture, não como catálogo atual.
93. [ ] FB-049-06 / P3 — Bolsa: revisar correspondência dos 41 tipos apresentados, tamanho, qualidade e contraste dos ícones com acervo autorizado; organizar categorias comuns, frutas, bolas, medicamentos e especiais; não alterar regras por engano.
94. [ ] FB-049-07 / P0 — Segurança do descarte: prévia local quantidade atual, limite e excedente; proteger raros/premium por padrão, confirmação explícita de limite zero e itens valiosos, preservar não marcados; validar limites e cancelamento sem descarte; testes offline, sem ampliar ações de jogo.
95. [ ] FB-049-08 / P3 — PT-BR: auditar mapa, pesquisas, bolsa, VGP+ e Shundo. Traduzir Running, Checked, Shiny, Caught, Missed, From start, Search places or coordinates, PokéStops, Gyms, Raids, Remaining, Candy, Pinap, Leader Map Fragment, Leader Map conforme glossário oficial (validar equivalência do item); não expor IDs ou traduzir nomes próprios indevidamente.
96. [ ] FB-049-09 / P3 — Interface: corrigir botão Guardar minha PGSharp Key cortado, cabeçalhos encobertos, mensagens truncadas do Shundo, sobreposições e alinhamento; testar fonte ampliada, tela pequena, rolagem, contraste, tema e janela flutuante.
97. [ ] FB-049-10 / P3 — Mapa Mundial: legibilidade, sobreposição e densidade de marcadores, filtros horizontais, painel inferior de detalhes; manter camadas, Locais e rotas existentes; testar apresentação com dados locais/fixtures.
98. [ ] FB-049-11 / P1 — Privacidade: nome do treinador aparece na janela flutuante da captura embora outras telas mostrem asteriscos. Reconciliar máscara em mapa, perfil, overlays, janelas e snapshots controlados pelo app, sem alterar dados reais; testar alternância, lifecycle e isolamento.
99. [ ] FB-049-12 / P5 — Reconciliar com as correções em andamento; preservar V5 homologada, funcionalidades, seleções, proteções e rotas oficiais. Revisão, CI no SHA exato, build APK versionado e hashes/download direto quando produzido; homologação física e conexão de provedor separadas. Nenhuma correção declarada entregue por este registro.
