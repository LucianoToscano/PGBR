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

## Aceite em dispositivo (separado)

device_tested=false; provider_connected=false até evidência correspondente. Tela branca Rocket, Supereficaz, interface e traçado visual não são homologados fisicamente pela aprovação de CI. Não alterar estados por conveniência.

## Próximo ciclo (checkpoint original preservado)

Verificar PR #203 e run 45482026778, registrar logs/resultado real, preservar o SHA do build em andamento e avançar as tarefas independentes. Antes de declarar STATUS: CONCLUIDO, revisar todos os registros canônicos e obter evidência de entrega real; a mera ausência de checkboxes não basta. A ID de auditoria de um eventual supervisor CLI só pode ser registrada pelo executor real desse supervisor.


## Ciclo 24/11/2026 — versão atual 0.00.47-LT

Main observada `56cff4b885b9e52aa914aab8152a67959516e852`. Fila numerada de trabalho: os itens de catálogo incluem verificação de recursos existentes, propostas e melhorias; aberto não significa bug comprovado nem autorização para executar ações em serviço terceiro. Dependências e critérios completos permanecem no catálogo; decisões posteriores do Mestre prevalecem (BYOK retirado, não reativar automaticamente). Os itens já integrados podem exigir aceite em dispositivo separado. Não marcar conclusão sem evidência.

8. [x] PGBR-CI-205 / P0 — Falha Bash printf corrigida em b67cd7ad1129c5a8733a5d48e16e4fb15359dffd; HEAD reconciliado 7b5a8424ff77eee98526c839e7db2b6cfdc102a0, CI 45493274906/45493274883/45493274892 aprovados, merge 606d90d455632fc7f7847eb8626a7ea99ada77e8. Primeira execução agendada na main deve ser conferida separadamente.
9. [x] PGBR-DOC-204 / P5 — Índice MESTRE.md integrado em 56cff4b885b9e52aa914aab8152a67959516e852, CI 45482888669/45482888540 aprovado.
10. [x] PGBR-UI-SMALL / P1 — Causa estática confirmada: altura disponível podia ser zero; rodapé e atribuição sobrepostos. Candidata: scroll de contingência, altura mínima conforme controles medidos, rodapé empilhado e altura flexível dos botões; teste host de aritmética e integração estática. CI e teste Android físico ainda exigidos. Atualização: correção de código integrada pela #207, HEAD f9d0d535318b9efa59ee88cc80be3c054b58b51f, CI 45494782561/45494782218 aprovados, merge be99a052d15e643622aa83ecf754dac9635f719a. Aceite físico permanece separado em QA-02.
11. [ ] PGBR-PR-TRIAGE / P2 — Reconciliar PRs antigas por equivalência comprovada e registrar decisão sem apagar trabalho.

### Inventário de 65 registros do catálogo

12. [ ] MAP-01 — Mapa mundial e POI. F: WorldMapDialog, MapProjection, MapTileSource/Cache, MapPoiLayer. Critério: Legibilidade, acessibilidade, cache, categorias, atribuição e indicação de fonte/idade. Prioridade original: P1.
13. [ ] MAP-02 — Locais e favoritos. I: Mestre/#188; SavedPlaces, Hotspots, GpxStore. Critério: Biblioteca única Favoritos/Pontos/Rotas/Eventos; busca, filtro múltiplo, ordenação, +, coordenadas e cópia independente; manter perfil e ícones coerentes. Prioridade original: P0.
14. [ ] MAP-03 — GPX e prévia. F/I: GpxStore, GpxRouteDialog, GpxCodec; #167/#172. Critério: Multi-segmento, nomes Unicode, limites, distância, edição, progresso, exportação e importação segura. Prioridade original: P1.
15. [ ] MAP-04 — Rota visual pessoal. I/P: #176/#183/#185. Critério: Rota GPX/Caminhar até aqui exclusivamente visual; progresso, cores, espessura e destino; modo automático só com interface autorizada e validação de projeção. Calibração inicial ainda manual. Prioridade original: P1/P3.
16. [ ] MAP-05 — Rotas oficiais. I/P: GameRoutesController/Dialog, #183/#187. Critério: Preservar separação, fluxos oficiais e recompensas; revalidar regressões com uso autorizado e sem ação automática nova. Prioridade original: P0.
17. [ ] MAP-06 — Joystick, movimento e GPS. F: Pad, MotionEngine, AutoWalkPlanner, GameLocationDelivery. Critério: Ergonomia, estados de pausa, diagnóstico, compatibilidade Android; sem ampliar manipulação de localização ou contornar controles. Prioridade original: P1.
18. [ ] MAP-07 — Pontos e mapas temáticos. N: catálogos de POI e mapas autorizados. Critério: Camadas por interesse, localização real/planejada, filtros, prévia 3D própria, favoritos offline. Prioridade original: P2.
19. [ ] COL-01 — Espécies e seleção. F: PokemonCategories, PokemonOptionFilter, PokemonSpeciesDialog, PokemonSelectorPolicy. Critério: Unificar IDs, formas e sprites; manter filtro conhecido/desconhecido. Prioridade original: P1.
20. [ ] COL-02 — Coleção, IV, nível e proteção. I/P: LT046 prévia individual/lote, etiquetas tri-state. Critério: Revisar positivos/negativos e ausência de dados; preservar favoritos, shiny, XXL/XXS, fantasia, fundo e formas; nenhuma classificação isolada autoriza transferência. Prioridade original: P0.
21. [ ] COL-03 — Evoluções, MT e fortalecimento. F/P: evolve_transfer.cpp, batch_evolution.cpp, #134, matriz QA. Critério: Consulta de requisitos, custo, ataques elegíveis e legados; simulador de PC/IV; prévia sem gastar recursos; confirmar fontes e eventos. Prioridade original: P1.
22. [ ] COL-04 — Pokédex e coleção visual. N: WWM + sprites autorizados. Critério: Busca por geração, tipo, forma, fantasia, fundo, shiny; checklist manual, progresso, filtros e exportação local. Prioridade original: P2.
23. [ ] COL-05 — Ranks PvP e avaliação. P/N: simulador local LT046. Critério: Rank por liga/limite de PC, nível, IV e versão da tabela; comparar evoluções, mostrar pressupostos. Prioridade original: P2.
24. [ ] INV-01 — Inventário e ícones. F/I: ItemBadge, ItemSpriteFit, LiveFeedIcons, #174/#181; galeria por ID. Critério: Poeira estelar, frutas, bolas, MT, incubadoras, evolução, módulos, poções e revives; auditoria de correspondência ID/asset, fallback legível; só assets licenciados. Prioridade original: P0.
25. [ ] INV-02 — Gestão de itens. F: arquitetura, inventário65; LT046 planejamento local de frutas. Critério: Resumo, reservas e alertas; prévia de consumo sem ações irreversíveis; alertas de limite. Prioridade original: P1.
26. [ ] CAP-01 — Captura, Quick Catch, Hunt/Farm/Transfer. F/P: Control, CatchOutcomeController, VgpController, release/mestre. Critério: Preservar interface e preferências; revisar feedback, Lenta 3s, estados e proteção; testar em fixtures sem ampliar automação do jogo. Prioridade original: P0.
27. [ ] VGP-01 — VGP+. I/P: LT045, VgpController; etiquetas/raridade #133 pendentes. Critério: Classificação e etiquetas locais; status correto, não confundir dado desconhecido com ausência; preservar proteção e compatibilidade. Prioridade original: P0.
28. [ ] FEED-01 — Sniper 1/2, Radar, Savant, coletor IV100. I/P: LT045 QA; SourceQuery, LiveFeedClient/Data/Filters/Dialog. Critério: Identificação e filtros de catálogo; rótulo de fonte/idade/ausência; nenhum feed privado ou coordenadas ao vivo presumidas. Provedor autorizado obrigatório. Prioridade original: P1/B.
29. [ ] BAT-01 — Rocket: tela branca carregado. P: #193 HEAD 955eb76aaf9c48e1a25d0f51f741608fe8fa002f, integrada na main; correção candidata sem homologação física. Critério: Prioridade Rocket por ser fluxo compartilhado; documentar fase, comparar registros e visibilidade em ensaio autorizado; investigar material/overlay próprio e regressão sem remover função; efeito discreto somente em UI própria; nenhuma causa comprovada. Prioridade original: P0.
30. [ ] BAT-02 — Supereficaz e Ataque Inteligente. P: #193 candidata, política local e matriz. Critério: Validar types/moves como referência independente; simulações de tipo simples/duplo, resistências, metadados desconhecidos, dois carregados; separar recomendação local da execução real. Prioridade original: P0.
31. [ ] BAT-03 — PvP e treinamento. P: política local, testes, #90/#105. Critério: Simulador, recomendação de equipes, diagnóstico de lifecycle/rearme; não declarar executor homologado. Prioridade original: P0.
32. [ ] BAT-04 — Reides e ginásios. F/P: arquitetura, runtime e histórico LT; API RaidBoss/Pokebattler. Critério: Central de chefes, counters, tipos, clima, custos, equipe manual; avisos de agenda e disponibilidade. Prioridade original: P2.
33. [ ] BAT-05 — Max / Dynamax / Gigantamax. P: AutoMaxBattleSessionTest, preparação manual #174, #131/#154 pendentes. Critério: Central Max, papéis, elegibilidade, partículas, custos, sugestões de equipe; sem iniciar/gastar recursos automaticamente. Prioridade original: P1/P2.
34. [ ] BAT-06 — Central Rocket. N: invasions + grunt-quotes/grunts. Critério: Frases, possíveis equipes e fraquezas; mostrar fonte, data e incerteza, consulta manual. Prioridade original: P2.
35. [ ] EVT-01 — Eventos. I: #66, histórico LT033; menu Evento e Locais no Mestre. Critério: Calendário oficial, hoje/próximos, horários locais, bônus, espécies e ataques exclusivos, links e fontes; cache e expiração. Prioridade original: P1.
36. [ ] EVT-02 — Missões e pesquisas. F/P: research e busca documentadas; Quest types/conditions/rewards. Critério: Catálogo pesquisas de campo, temporárias, especiais, coleções e recompensas; checklist manual, alertas, filtros por item/espécie. Prioridade original: P1/P2.
37. [ ] EVT-03 — Planejador de eventos. N: integração de dados públicos autorizados. Critério: Agenda, lembrete opt-in, dia comunitário, hora destacada, hora de reide, GO Fest/Tour, Max, Rocket, bônus e janelas de evolução; fuso IANA e mudança de data; nada de ações de jogo agendadas. Prioridade original: P2.
38. [ ] EVT-04 — Presets e rotinas locais. N: preferências e eventos. Critério: Perfis de interface, filtros de consulta, lembretes, lista de tarefas e planejamento; prévia, confirmação, botão cancelar, histórico e reversão. Prioridade original: P2.
39. [ ] AUX-01 — Companheiro, ovos, presentes, medalhas. F/P: histórico e estados PT-BR no Mestre; escopo detalhado ainda não auditado. Critério: Painel manual de progresso, planejamento de incubação, alertas e glossário; não inventar estado de conta. Prioridade original: P2.
40. [ ] AUX-02 — Eternatus e funções sazonais. F/P: AGENTS e histórico; guardas já documentados. Critério: Catálogo sazonal, checklist e diagnóstico; preservar fluxo existente, sem conceder progresso por inferência. Prioridade original: P1.
41. [ ] SYS-01 — UI, menu e tradução. F: Pad, SettingsPanel, SettingsUi, SettingsSearch. Critério: PT-BR integral (catch/xfer/feeding e outros), estados, rótulos de acessibilidade, escala de fontes, tema claro/escuro, tablet. Prioridade original: P0.
42. [ ] SYS-02 — Estatísticas. F: SessionStats, SessionStatsHistory e testes. Critério: Painel 1h/24h, histórico, metas, exportação sanitizada, taxas com denominadores explícitos; sem afirmar exatidão antes de QA. Prioridade original: P1.
43. [ ] SYS-03 — Perfis, contas e privacidade. F: AccountProfileStore/Profiles/Dialog/SessionVault, SettingsWriter. Critério: Preservar isolamento, máscara local e backup; diagnóstico sem dados pessoais; exclusão e exportação explícitas. Prioridade original: P0.
44. [ ] SYS-04 — Cofre e IA opcional. I/P: #168/#173; prévia independente; sem provedor. Critério: BYOK somente com consentimento, Keystore, limite de gasto, explicação de envio, revogação, fallback; assistente de catálogo, sem ações de jogo. Prioridade original: P2/B. Decisão posterior: BYOK retirado; revisar apenas cofre local e privacidade, não reintroduzir integração.
45. [ ] SYS-05 — Dados e cache. N: Data Engine; já existem MapTileCache, cache de sprites, catálogos específicos. Critério: Normalizar, validar e atualizar snapshot offline por versão, checksum, rollback e adaptadores; não duplicar componentes existentes. Prioridade original: P1.
46. [ ] SYS-06 — GeoLab. I/P: #165, pesquisa separada. Critério: Laboratório de desempenho para dados geográficos públicos/sintéticos, não runtime de produção. Prioridade original: P3.
47. [ ] SYS-07 — QA, build e release. I/Q: workflows e matriz LT046. Critério: Rastrear HEAD, fixtures, assinatura/hashes, três variantes, QA Android; não publicar sem autorização explícita. Prioridade original: P0.
48. [ ] SYS-08 — Governança e documentação. F: Mestre e AGENTS. Critério: Catálogo por ID, mapa de dependências, registro de regressões, licenças, fonte e data; resolver conflito documental preexistente em PR separada. Prioridade original: P0.
49. [ ] DATA-01 — Inventário de bases já embarcadas e IDs. Critério: Diff contra WWM por revisão, sem substituir dados existentes cegamente.
50. [ ] DATA-02 — Manifesto de proveniência. Critério: Fonte, revisão, licença, hash, esquema, data, validade, atribuição e permissões de cada dataset/asset.
51. [ ] DATA-03 — Modelo comum e cache offline. Critério: IDs estáveis e namespace por tipo; atualização atômica, snapshot anterior preservado, tamanho limitado, migração testada.
52. [ ] DATA-04 — Traduções PT-BR. Critério: Chaves por categoria, fallback explícito, glossário de rótulos próprios separado de textos externos.
53. [ ] DATA-05 — Validação de tipos, ataques e formas. Critério: Fixtures com dados desconhecidos, dual-type, resistências, mudanças de moves, fantasia não lançada; sem usar metadados como prova de disponibilidade.
54. [ ] DATA-06 — Itens e sprites. Critério: Manifesto ID→arquivo autorizado, hash, dimensões, transparência, fallback, relatório de faltantes, testes de colisões.
55. [ ] EVT-05 — Ingestão de calendário. Critério: Fontes oficiais prioritárias; termos e atribuição; atualização por ETag/TTL quando suportados, horário local versus UTC, eventos expirados não ativos.
56. [ ] EVT-06 — Motor de lembretes locais. Critério: Opt-in, notificações Android, fuso IANA, cancelamento/edição, deduplicação, sem disparo de ações no jogo.
57. [ ] EVT-07 — Checklist inteligente. Critério: Plano diário/semanal manual; progresso confirmado pelo usuário, prioridades, reset por janela, estado desconhecido explícito.
58. [ ] EVT-08 — Central de bônus e janelas. Critério: Mostrar bônus, pesquisa, reide, Max, Rocket, ataques exclusivos com fonte, intervalo e elegibilidade; sem inferir garantia.
59. [ ] AUTO-01 — Catálogo de rotinas. Critério: Apenas rotinas de dados, atualização, cache, notificação, exportação e planejamento; opt-in, estado, auditoria, limites, pausa e cancelamento.
60. [ ] AUTO-02 — Regras de segurança. Critério: Nenhuma nova ação irreversível; teste de presets sem execução; não alterar runtime existente por integração de dados.
61. [ ] VIS-01 — Renderização própria. Critério: Canvas/vetores ou renderer independente; assets autorizados, benchmark em aparelho e fallback 2D.
62. [ ] VIS-02 — Rota visual automática. Critério: Pesquisa de interface de projeção autorizada; teste de perspectiva, zoom, rotação, perda de referência; se indisponível, usar mapa próprio e deixar overlay experimental.
63. [ ] QA-01 — Tela branca. Critério: Reproduzir primeiro Rocket, vídeo/log sanitizado, identificar camada real, comparar antes/depois, preservar visibilidade e interações legítimas; depois testar PvP/treino.
64. [ ] QA-02 — Suporte e regressão. Critério: Matriz Root/sem Root, troca de tela, tema, resolução, permissões, versão Android, carga, memória, bateria; sem marcar como executado até evidência.
65. [ ] AUTO-03 — Eternatus. EternatusRouteRunner.java; events/EventPanel.java; AGENTS. Critério: Preservar coordenador e progresso por resgate confirmado; catalogar modelo sazonal e regressões, não reescrever.
66. [ ] AUTO-04 — Halloween. events/EventSettings.java (HALLOWEEN_2026), EventPanel; histórico PR #67. Critério: Perfil sazonal e validade; fixtures históricas não provam evento real.
67. [ ] AUTO-05 — Safari / Área Selvagem. EventSettings (GO_WILD_NOVEMBER_2026), Control; histórico PR #67. Critério: Reutilizar apresentação e preferências existentes; catálogo de bônus, itens e elegibilidade.
68. [ ] AUTO-06 — Auto-Raid / batalha de reides. AutoRaid.java, SettingsPanel.java, Control. Critério: Código presente com ciclo de sessão; distinguir planejamento de execução; revisar regressões de status e cancelamento, sem declarar homologação.
69. [ ] AUTO-07 — Caçador de Shundo. ShundoSniper.java; SettingsPanel (Experimental). Critério: Preservar recurso e registrar dependência de fontes autorizadas; planejamento e filtros de catálogo, sem prometer encontro ou IV real.
70. [ ] AUTO-08 — Outras automações legadas. OneShotAutomation.java; Control; VgpController. Critério: Inventário de funcionalidades e limites, conflitos de sessão, pausa, encerramento e isolamento de perfil.
71. [ ] AUTO-09 — Editor de macros. N. Critério: Criar sequências de cartões editáveis, biblioteca, duplicação, importação/exportação versionada e prévia.
72. [ ] AUTO-10 — Motor de macros. N. Critério: Simulador offline, fluxo sequencial, condições, repetição limitada, interrupção, timeout, registros e teste de recuperação.
73. [ ] AUTO-11 — Templates de eventos. N. Critério: Modelos por evento, revisão/validade/fuso, checklist, planejamento de itens, pesquisa, reides e espécies; sem ativar evento expirado.
74. [ ] AUTO-12 — Rotinas de reides e falhas. P/N. Critério: Pedido de automatizar um “bug das reides” registrado sem detalhes identificadores; classificar como investigação de defeito, reprodução em ambiente autorizado e correção, não macro operacional de exploração.
75. [ ] AUTO-13 — Biblioteca e histórico. N. Critério: Favoritos, nome, descrição, revisão, execução simulada, exportação sem credenciais/locais privados, status e motivo de interrupção.
76. [ ] AUTO-14 — Permissões e QA. N. Critério: Capacidades explícitas, teste sem conta real, pausa/stop, sem concorrência destrutiva, sem ação desconhecida.

### Próxima ação executável

SYS-07/QA-02: conferir CI de #222–#224, integrar somente HEADs aprovadas, manter matriz LT048 e recibos de cada revisão. #182 já encerrada por substituição com preservação de registros. Continuar requisitos P0 abertos; não confundir LT048 identificada com APK entregue.

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
