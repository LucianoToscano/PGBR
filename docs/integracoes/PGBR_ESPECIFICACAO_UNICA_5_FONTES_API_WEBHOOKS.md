# PGBR — Especificação unificada de integração de 5 fontes, API, scrapers e webhooks

> **ESCOPO ATUALIZADO — cinco integrações, exatamente:** (1) PokeList; (2) iFlowGo; (3) PoGoCoordinates + PokeSearch (uma única integração); (4) PGSharp; (5) iPogo. Menções antigas a Pokémod e PGTools são apenas referências históricas fora do escopo de implementação; não criar adaptadores, branches nem módulos adicionais para esses produtos. O gateway é infraestrutura compartilhada, não uma sexta fonte.


> **Versão:** 2026-11-23  |  **Estado:** instruções de implementação; não significa que APIs ou feeds estejam ativos.  
> **Projeto:** https://github.com/LucianoToscano/PGBR  
> **Arquivo mestre canônico:** https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md  
> **Base de dados Drive:** https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK

## Comando único para outra IA desenvolvedora

Leia **este arquivo inteiro**, incluindo as seções 00–06. Esta é uma **consolidação integral** de seis prompts técnicos e de um contrato OpenAPI, não um resumo. Execute a missão conforme a seção 00 e respeite os requisitos específicos das seções 01–05 e o contrato da seção 06. Priorize código real testável no PGBR. Conserve funcionalidades existentes, consulte `PGBR_ARQUIVO_MESTRE_UNICO.md` e GitHub atualizados, adote branch independente e PR sem merge automático, e não declare builds, resultados ou feeds como reais sem comprovação. Não repita perguntas de autorização para operações reversíveis no código próprio. Não trate os arquivos proprietários de referência como autorização de backend de terceiros.

**Papel deste arquivo no repositório:** documento técnico versionável. A IA com GitHub conectado pode fazer alterações incrementais neste `.md` e criar PRs com histórico; quem só tem acesso ao link pode ler e propor um patch, mas não editar o GitHub sem permissão. Para ler automaticamente, use a URL pública `raw.githubusercontent.com` da branch disponibilizada. A abertura da branch não constitui integração à `main`.

### Fontes, projetos e arquivos

- PGBR: https://github.com/LucianoToscano/PGBR
- PGSharp próprio: https://github.com/LucianoToscano/PGSharp
- PokeSearch: https://github.com/LucianoToscano/PokeSearch
- PvPPokeGo: https://github.com/LucianoToscano/PvPPokeGo
- PokeList: https://drive.google.com/drive/folders/1faW_VXpB2-5jotgbdFeQagF-vmQyn8gZ
- iFlowGo: https://drive.google.com/drive/folders/1ysfYNt8JfEy3OkM_YE-BuRiiAzBbBuGm
- PoGoCoordinates: https://drive.google.com/drive/folders/19SDhX8DZrUTjMFlinIREi17Z9rItjQ4c
- PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j
- iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x

### Índice

- [00 — Prompt mestre de execução e integração](#secao-00)
- [01 — PokeList / Moonani](#secao-01)
- [02 — iFlowGo](#secao-02)
- [03 — PoGoCoordinates + PokeSearch](#secao-03)
- [04 — PGSharp](#secao-04)
- [05 — iPogo](#secao-05)
- [06 — Contrato OpenAPI proposto](#secao-06)

---

## <a id="secao-00"></a>Seção 00 — Prompt mestre de execução e integração

**Origem integral:** `00_PROMPT_MESTRE_PGBR_5_FONTES_GATEWAY_2026-11-23.txt`  
**SHA-256 do texto integrado:** `bbab6fc858047775e0a38da3f98aa3a068a4cb7472a29d9a20f27615190438c2`  
**Tamanho:** 7255 bytes

~~~~text
PROMPT MESTRE — PGBR, 5 FONTES, API PRÓPRIA, SCRAPERS PERMITIDOS E WEBHOOKS
DATA: 23/11/2026 — COMANDO ÚNICO PARA IA DE DESENVOLVIMENTO

MISSÃO
Executar uma auditoria e integração incremental no PGBR usando 5 pacotes complementares: PokeList, iFlowGo, PoGoCoordinates + PokeSearch, PGSharp e iPogo. Não reconstruir o aplicativo. Consolidar busca, resultados com coordenadas somente quando autorizadas e válidas, filtros avançados, perfis e Snipers 1/2, catálogos, radar, notificações, API própria e webhooks próprios. Os prompts numerados 01–05 do ZIP são especificações subordinadas e devem ser lidos INTEGRALMENTE, sem descartar requisitos.

EXECUTAR SEM INTERRUPÇÕES DESNECESSÁRIAS
Esta mensagem autoriza executar trabalho no repositório/arquivos próprios sob acesso já concedido, sem solicitar aprovação em cada análise, commit, branch, teste ou PR. Qualquer recurso dependente de terceiros deve continuar desativado até que a fonte realmente permita integração; não exigir do usuário a repetição da autorização já fornecida para PGBR. Não falsificar direitos de fornecedores, tokens, planos VIP, dados ou status. Não integrar por bypass de serviço protegido. Preservar confirmações de teleporte/ação no aplicativo e evitar merges/deploys destrutivos.

ORDEM OBRIGATÓRIA
0. Ler PGBR_ARQUIVO_MESTRE_UNICO.md integral, AGENTS.md, THIRD_PARTY.md, ARCHITECTURE/BUILDING/ROOTED, todas PRs relevantes e HEAD atual; identificar base, branches paralelas, testes e o que está em main vs somente em branch. PGBR LT043 QA é snapshot de 23/11/2026, não garantia de instalação ou que todas as PRs estejam integradas.
1. Ler os acervos Drive: Banco de Dados Pokémon GO https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK ; PokeList https://drive.google.com/drive/folders/1faW_VXpB2-5jotgbdFeQagF-vmQyn8gZ ; iFlowGo https://drive.google.com/drive/folders/1ysfYNt8JfEy3OkM_YE-BuRiiAzBbBuGm ; PoGoCoordinates https://drive.google.com/drive/folders/19SDhX8DZrUTjMFlinIREi17Z9rItjQ4c ; PGSharp https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j ; iPogo https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x . Usar arquivos e relatórios como evidência do que foi estudado, NÃO como autorização automática de API remota.
2. Inventariar classes atuais FeedChooser/LiveFeed*/PogoFeed*/IFlowGo*/PokeList*/HundoFeed*/ShundoSniper/PgSharpFeed*/PgSharpDataImport/AccountProfiles/SettingsBackup/CoordinateClipboard, PokeSearch como referência sem modificar seu repositório, e código próprio LucianoToscano/PGSharp. Não recriar funcionalidades já existentes, nem apagar capacidades paralelas.
3. Definir MODELO ÚNICO: `source_id`, `event_id`, `origin_kind`, `kind`, `pokemon_id`, `form_id`, `lat`, `lon`, `coordinate_precision`, `IV/raw IV`, `cp`, `level`, `gender`, `size`, `weather`, `pvp`, `observed_at`, `retrieved_at`, `expires_at`, `data_quality`, `coverage`, `source_url`, `is_demo`, `has_more`, `next_cursor`, `server_filters_applied`, `local_filters_applied`. Campos ausentes=null, não inventados; TTL e source preservados.
4. Criar REPOSITÓRIO DE RESULTADOS e SourceAdapters para as fontes que realmente devolvem eventos válidos. Implementar uma API REST NOSSA (ver OpenAPI PROPOSTO no pacote) e importadores LOCAIS PGSharp JSON/GPX e iPogo quando houver export legítimo. Reusar a camada PGBR existente; criar backend separado apenas se não houver gateway equivalente. Nunca prometer que um `.apk`/`.ipa` hospeda feed próprio ou webhook externo.
5. Criar SCRAPERS SOMENTE para recursos públicos com permissão explícita/termos que não proíbam uso, respeitando robots, atribuição, ritmo/cotas e cache/ETag; preferir APIs abertas e exportações pessoais. Não acessar painéis VIP ou endpoints internos, não interceptar tokens, não imitar aplicativo criptografado nem contornar 403/429. Scrapers de documentação/versão/catálogo estático não substituem spawns em tempo real.
6. WEBHOOKS DE NOSSO GATEWAY: disparar eventos apenas de inserções/alterações reais permitidas, com HMAC, idempotência, filtro por espécie/raio/tipo e TTL, replay protection, retries+DLQ, unsub e auditoria. No app usar notificação/polling conforme arquitetura; sem feed ao vivo, permitir somente eventos de importação/status e marcar eventos de spawn como inativos.
7. Filtros PGBR/PGSharp/PokeSearch/iPogo: IV 100/0/%/A-D-S, PvP GL/UL, espécie, formas, clima, tamanho, gênero, PC, nível, raid, quest, Rocket, Power Spot conforme fonte, local/global com cobertura e paginação, presets, ordenação e TTL. Marcar filtros locais/remotos/não suportados por fonte e mostrar contagens reais, não 'global completo' com 25 itens.
8. Sniper 1 e 2 recebem somente registros normalizados com coordenadas EXATAS e válidas de fonte permitida; não exibir botão de teleporte quando indisponível/expirado. Preservar confirmação, cooldown e motor nativo de GPS; não adicionar teleporte automático oculto. Ranking e shiny só marcados confirmados quando origem comprovar.
9. Tests: contrato OpenAPI, importação PGSharp duplicada, dados iPogo referência sem import falso, 0,0 inválido para legendas, NaN, expirados, paginados, rate-limit 429, 401/403, memória, cancelamento, fontes misturadas, isolamento por usuário, assinatura webhook, replay, idempotência, receptor falhando, regressão de App PGBR, build Root/sem Root/Zygisk. Prova em aparelho separada de testes unitários/CI.
10. Salvar commits e PRs independentes por módulo; documentar matriz recurso -> fonte -> evidência -> situação -> teste; registrar melhorias no mesmo Mestre único sem reduzir histórico. Preferir staging, rollback; não promover main automaticamente. Se gerar APK, enviar link direto + SHA-256/assinatura comprovados.

CLASSIFICAÇÃO OBRIGATÓRIA DE FONTES
- PokeList: APK/esquema estudado; servidor pode recusar 403; backend de parceiro no Drive é implementação de referência sem feed de produção.
- iFlowGo: contrato de cliente Python observado; página global limitada não significa feed mundial completo; pacote gateway no Drive sem dados originais do fornecedor.
- PoGoCoordinates: `reveal_code` não é latitude/longitude; buscar e revelar com quotas conforme termos; protótipo Drive é DEMONSTRAÇÃO, não integração proprietária ativa.
- PGSharp: 100 IV Feed Free / All Coordinates Feed Standard são funcionalidades do produto; PGBR atual mantém conector NÃO VERIFICADO. Importador PGSData é local.
- iPogo: funções documentadas de radar e listas globais; APK/IPA no Drive fornece referência de compatibilidade, não API pública nem webhooks. Design próprio PGBR com fonte permitida.

ENTREGUE AO FINAL
A) diagnóstico e matriz de cinco fontes com prova; B) código implementado e PRs/commits; C) gateway REST e OpenAPI ou contrato honesto se implantação bloqueada; D) importadores e presets; E) testes e relatório; F) webhooks próprios reais somente onde haja eventos legítimos; G) release APK apenas se compilada de fato; H) atualização aditiva do arquivo Mestre e documentação Drive quando a operação realmente for executada. Não parar em pseudo-código se há acesso ao repositório para programar.
~~~~

---

## <a id="secao-01"></a>Seção 01 — PokeList / Moonani

**Origem integral:** `01_PROMPT_PGBR_POKELIST_MULTIFONTES_V2_2026-11-23.txt`  
**SHA-256 do texto integrado:** `06a5a66b96053f9459f9f887c5a0c0786f3d65153c0527e66bad422d88924af9`  
**Tamanho:** 21248 bytes

~~~~text
PROMPT 01/05 — PGBR | POKELIST / MOONANI
MISSÃO: COMPLETAR BUSCA POKÉMON, ROCKET, LÍDERES, REIDES E PESQUISAS

OBJETIVO ESPECÍFICO
Aproveitar a pesquisa técnica do PokeList 1.3.18 e o conector PokeList JÁ existente no PGBR. Criar funcionalidades úteis dentro da interface PGBR e completar filtro, parse, apresentação e testes, sem depender de contratar o desenvolvedor do PokeList para iniciar o trabalho local. Não confundir o APK de referência com código-fonte do backend, nem supor que endpoints historicamente observados estejam abertos para aplicativos terceiros.

EVIDÊNCIAS / MATERIAIS A CONSULTAR
- Acervo PokeList: https://drive.google.com/drive/folders/1faW_VXpB2-5jotgbdFeQagF-vmQyn8gZ
- `03_API_Webhook_Scrapers/API_PokeList_spec__COPIA_CENTRAL.md`: análise ESTÁTICA de `com.antvn.pokelist` v1.3.18; não equivale a autorização de rede. Avaliar `PokeList Open`, `01_APK_Original_e_Analise`, `02_Assets_Extraidos`, `04_GitHub_Bots_Projetos`, `05_Recriacao_e_Auditorias` e `06_Modulos_e_Historicos`.
- Entrega já preparada no Drive: https://drive.google.com/drive/folders/11BSUaiv2JjDti6KDmvebMGvrvIlTdQjS (API OpenAPI, SDK, backend experimental, migrações, webhooks e testes). A própria entrega registra que ainda NÃO tem feed de produção nem staging oficial, apesar de seus testes em ambiente próprio. Reaproveitar como CONTRATO/TESTES, não como serviço ativo.
- GitHub Lucario: https://github.com/KernelX-debug/Discord-Bot_Lucario_Moonaniphp (referência de integração não oficial; auditar licença e dados antes de copiar código).
- Código existente PGBR: `PokeListFeedClient.java`, `PokeListFeedData.java`, `LiveFeedDialog.java`, `LiveFeedFilters.java`, `LiveFeedFilterDialog.java`, `LiveFeedData.java`, `FeedChooser.java`; revisar também testes `PokeListFeedDataTest.java` e `test_pokelist_access_behavior.py`.

ROTAS E ESQUEMA HISTÓRICOS — APENAS MAPEAR, NÃO ASSUMIR DIREITO DE CHAMAR
- Página: https://moonani.com/PokeList/index.php e /quest.php
- Rota observada no APK: `GET /PokeList/api.v2.php` com `type` candidato `pokemon|raid|quest|rocket|rocket_leader`; campos de query históricos: `lat,lng,distance,mons,rockets,form,gender,size,iv_type,ivMin,ivMax,attackMin,attackMax,defenseMin,defenseMax,hpMin,hpMax,levelMin,levelMax,ispvp,_time`.
- Resposta histórica `type`, `pokemons[]`, com `id,name,number,country,latitude,longitude,startTime,endTime,level,cp,iv,attack,defense,hp,gender,form,shiny,rocket,rocket_type` — nomes, semântica, unidades, timestamps e cobertura ainda dependem de evidência.
- APK registra catálogos locais `pokemons.json` (1008), `rocket.json` (41), `rocketleader.json` (3) e alerta sonoro. Usar como referência sob proveniência/licença comprovada; preferir catálogo principal mais recente do projeto e cache/sprites existentes. Firebase citado no APK não pode ser lido diretamente sem permissões.

IMPLEMENTAR NO PGBR
1. Corrigir e estender adaptador PokeList existente; manter compatibilidade com os contratos atuais de `LiveFeedData.Snapshot`. Mapear `pokemon/raid/quest/rocket/rocket_leader` em tipos separados sem interpretar campo errado. Tratar `distance=0` ou raio infinito SOMENTE se confirmação no contrato da própria fonte; não fazer busca sem limite. Exibir cobertura real e restrição por espécie quando exigida.
2. Criar filtros úteis no app: Pokémon (#Dex, nome, forma, IV 100 e 0, faixa %, IVs brutos, PC, nível, gênero, tamanho), reides (chefe/nível/horário/restante), pesquisas (recompensa/tipo/Poképarada), Rocket (grunt, líder, tipo, horário/TTL). Filtro PvP GL/UL: calcular somente com dados completos e fórmula/catálogo validado; não inventar rankings.
3. Fazer parse robusto de `startTime/endTime` em s/ms, nulls, campos ausentes, IV 0, diferenças por `type`, duplicados, latitude/longitude, expiry. Preservar país/bandeira apenas quando grounded; não converter flag `shiny` em confirmação shiny daquela conta.
4. Interface: um submenu PokeList no menu de fontes já existente, abas Pokémon / Rocket / Líderes / Reides / Pesquisas, contagem por aba, campos de filtro e botão Buscar; retornar cartões com sprites e tipo, validade, fonte, distância, mapa/coords quando exatas e permitidas. Filtro desabilitado deve explicar a razão. Não deixar botões conectados a no-op.
5. Acrescentar `PokeListProviderStatus`: origem, base consultada, capability por categoria, limites conhecidos, status HTTP, última verificação, cobertura, filtros locais e remotos. Páginas negadas geram mensagem clara e NÃO ativam retry agressivo nem fallback secreto para outro provedor.
6. Preparar integração opcional com gateway próprio do PGBR para API/webhooks descritos no pacote Drive. Gateway pode receber feeds reais somente quando exista fonte autorizada e configuração de produção; até lá testes sintéticos e funcionalidade local/consulta independente permanecem úteis e identificados.
7. Testes: fixtures de todos 5 tipos, variações de epochs, parsing Rocket/leaders, quests/raid, ranking sem dados, 401/403/429, limite de região, importação de catálogos e regressão nas buscas iFlowGo, PoGoCoordinates, LivePokeMap, filtros e mapas existentes.

RESULTADO EXIGIDO
PR `feat/pgbr-pokelist-filtros-rocket-raids-quests-20261123` (adaptar nome se existir); código concreto, testes, UI PT-BR, documentação de cobertura e matriz de capacidade. Usar a entrega PokeList do Drive sem inventar backend oficial. Concluir tudo que é independente de credenciais de terceiros e deixar somente a conexão de produção explicitamente bloqueada onde realmente for.

============================================================
PROTOCOLO COMPARTILHADO — PGBR / FEEDS / SNIPERS — 23-11-2026
============================================================

PAPEL: Você é a IA responsável por DESENVOLVER E TESTAR CÓDIGO no repositório https://github.com/LucianoToscano/PGBR. A tarefa é acrescentar ou reparar um módulo DENTRO do PGBR (Android, PT-BR), não escrever uma carta ao proprietário de um site. Não entregar só planejamento, mock visual ou documentação se o código do PGBR estiver acessível.

MANDATO DE EXECUÇÃO: Inicie com as permissões já disponíveis no repositório e no Drive, sem pedir ao usuário confirmação a cada arquivo, etapa, teste, branch ou PR. Escolha soluções técnicas reversíveis. A autorização do titular do PGBR para trabalhar no PGBR não concede licença, credenciais, quotas VIP ou direitos de acesso a sistemas de terceiros. Programe a experiência e a lógica independente agora; ative chamadas de produção somente por fontes que realmente permitam a integração. Se uma fonte restringir integração, ofereça conector desabilitado com diagnóstico correto, entrada manual, importação de dados próprios, deep link ou outro feed permitido. Não contorne 401/403/429, captcha, assinatura, login, limites/reveal, paywall, TLS, Firebase, tokens privados ou termos de terceiros. Não peça nova autorização do usuário do PGBR para continuar partes locais independentes. Preserve confirmações de ação dentro do aplicativo quando necessárias para evitar teleporte acidental.

PRIMEIRA ETAPA OBRIGATÓRIA:
1. Ler INTEGRALMENTE `PGBR_ARQUIVO_MESTRE_UNICO.md`, `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/BUILDING.md`, `docs/ROOTED.md`, `THIRD_PARTY.md` e documentos de feeds/PRs pertinentes; depois confirmar HEAD de `main`, PRs e branches concorrentes, merges recentes, CI e releases. O Mestre contém snapshots históricos: código e CI atuais prevalecem sobre datas antigas.
2. Aplicar o PROTOCOLO GLOBAL — DESENVOLVIMENTO PARALELO SEM PERDAS: registrar commit-base, criar branch específica, comparar PRs abertas e arquivos afetados, preservar mudanças paralelas e criar PR revisável sem sobrescrever nem fazer merge automático em `main`. Não fazer reset destrutivo, não apagar histórico e não transformar PR de outro chat em branch-base sem auditar.
3. Inventariar classes do PGBR ANTES de criar duplicatas: `FeedChooser`, `LiveFeedDialog`, `LiveFeedClient`, `LiveFeedData`, `LiveFeedFilters`, `LiveFeedFilterDialog`, `PogoFeedDialog`, `PogoFeedClient`, `PogoFeedData`, `PogoFeedModes`, `IFlowGoFeedClient`, `IFlowGoFeedData`, `IFlowGoRequestPolicy`, `PokeListFeedClient`, `PokeListFeedData`, `HundoFeedDialog`, `ShundoSniper`, `PokemonSelectorDialog`, `CoordinateClipboard`, `Control`, `SettingsPanel`, `Pad`, `MapTheme`, `PtBr`, `LiveFeedSession`, `PgSharpFeedPolicy` e testes associados. Verifique os nomes/contratos na HEAD; não presumir implementação só por existir classe ou botão.
4. Preservar a arquitetura e as funcionalidades do PGRazor/PGBR existentes, incluindo controles de GPS/rotas, batalha, VGP+, inventário, sobreposições, Root, sem Root e módulo Zygisk. Não alterar o PokeSearch nem sua assinatura/release; reutilizar no PGBR apenas ideias, componentes e dados comprovadamente reutilizáveis, com créditos e proveniência, sem copiar recursos privados automaticamente.

ACERVO PARA PESQUISA (Drive conectado, confirmar itens/versões por leitura real):
- Banco de Dados Pokémon GO: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- Catálogos e recursos: `00 - Catálogo PGBR e PokeSearch`, `01 - Sprites, imagens, sons e modelos`, `02 - Dados, rankings e leitura de inventário`, `03 - Mapas, radar e fontes de eventos`, `04_APKs_de_Referencia`, `05_Links_Guias_e_Ferramentas`, `98_AUDITORIAS_APIS_WEBHOOKS_E_ENTREGAS`.
- Arquivo mestre de Drive do PGBR: https://drive.google.com/drive/folders/1aAXywzA-wjYhsk9ya4O_4-rgDeLT-DU8
- PokeSearch (consulta técnica, sem alterar): https://github.com/LucianoToscano/PokeSearch
- PGBR original: https://github.com/LucianoToscano/PGBR
- PGRazor upstream: https://github.com/jpitty03/PGRazor
- PokeMiners Game Master de referência: https://github.com/PokeMiners/game_masters
- PokeMiners/recursos e sprites encontrados no acervo: validar origem, versão, formas, licença e identidade dos assets antes de distribuir.

PROJETO DE DADOS COMPARTILHADOS — sem criar outro aplicativo:
A. Reaproveite classes existentes; quando faltar, criar uma interface pequena de adaptador de fontes, sem reescrever o PGBR: `capabilities()`, `search(query,cursor)`, `cancel()`, `sourceStatus()` e `attribution()`. Separar HTTP/credenciais, parse/normalização, persistência TTL, filtros, UI e ação de coordenadas. Não prometer endpoints genéricos que nenhum provedor oferece.
B. Modelo comum: `source_id`, `source_event_id`, `kind` (pokemon/raid/quest/rocket/... real), `species_id`, `form_id`, `lat`, `lon`, `coordinate_precision` (exact/approximate/unavailable), `iv_percent`, `iv_atk`, `iv_def`, `iv_sta`, `cp`, `level`, `gender`, `size`, `weather_boost`, `pvp_rank`, `observed_at_utc`, `expires_at_utc`, `retrieved_at_utc`, `is_demo`, `data_quality`, `coverage`, `source_url`, `server_filters_applied`, `local_filters_applied`, `next_cursor`, `has_more`, `total_known`. Não inventar campos, preencher com null quando ausentes; não confundir IV zero e campo ausente, shiny elegível com shiny garantido, rank calculado com rank de fonte.
C. Consolidar filtros REAIS por espécie/nome/#Dex, forma, IV 100/0/% e A/D/S, CP, nível, tamanho, gênero, clima, PvP GL/UL quando houver dados/algoritmo válido, tipo de evento, centro/raio/local, ordem e TTL. Identificar em cada origem quais filtros são remotos, locais ou não suportados, apresentando isso no app. Um filtro não deve retornar silenciosamente tudo e ser marcado como ativo. Modo Global deve declarar cobertura e paginação, não 'mundo inteiro' quando só há uma página ou hotspots.
D. Interface PT-BR no seletor já existente; estados `carregando`, `dados disponíveis`, `sem resultados reais`, `fora de cobertura`, `não autorizado pelo fornecedor`, `limite atingido`, `fonte indisponível`, `dado expirado`, `apenas demonstração`. Mostrar recebido/exibido/filtrado, nome de origem, hora UTC convertida para local, expiração, qualidade, cobertura e link de fonte. Não gerar coordenadas fictícias para telas normais.
E. Seleção de coordenadas: validar latitude/longitude (incluindo 0,0 e NaN), deduplicar, recusar expiração e coordenadas indisponíveis; permitir copiar `lat, lon` e usar a ação de teleporte que JÁ EXISTE no PGBR, com controles/cooldown/confirmação existentes. Não implementar novo sistema de GPS nem teleporte oculto. Camada Sniper 1/2 pode consumir a lista normalizada somente quando o usuário ativar a função e houver dados válidos/permitidos; não alterar ShundoSniper ou automações sem teste de regressão.
F. Rede robusta: HTTPS, timeouts, cancelamento, tamanho máximo do payload, validação estrita de JSON e timestamp (s/ms/ISO 8601), 429+Retry-After, backoff com jitter, caching apenas dentro do permitido, deduplicação, paginação limitada, circuit breaker por fonte e zero busy polling. Sem segredo de terceiro no APK nem token de usuário em log. Não usar WebView para extrair cookies/JWT nem executar scraping disfarçado.
G. Webhooks: o APK NÃO hospeda receptor público confiável. Integrar webhook por backend/gateway próprio autorizado do projeto, com assinatura HMAC, idempotência, replay protection, filas, retry e autenticação; push para app por um canal adequado ou consulta controlada. Se não existir stream verdadeiro, manter módulo desacoplado e mostrar 'Webhook não disponível', sem simular eventos ao vivo.
H. Filtros e preferências: armazenamento versionado, migração segura e perfil por fonte; presets IV 100, IV 0, PvP, raids, Rocket, XXL/XXS; exportação/importação local de JSON sem segredos. Reaproveitar sprites/cache da biblioteca central, mantendo identidade Dex+forma+shiny e fallback quando faltar imagem. Preservar nomes originais/links de atribuição.
I. Acessibilidade e UX: botões do PGBR coerentes com tema claro/escuro; PT-BR (preferir 'Teleporte' e não 'Teletransportar'); estados de carregamento e erro legíveis; não bloquear thread UI; não alterar layout global ou estilos não relacionados.

VALIDAÇÃO/PROVAS:
- Criar testes unitários do parser, filtro, cursor, paginação, TTL, datas/UTC, sexo/tamanho/forma, 0 IV x unknown, dedup, coordenadas inválidas, timeout/HTTP 401/403/410/429, Retry-After, cancelamento, cotas, isolamento entre contas, e fixtures realistas marcadas como DEMO.
- Testes de integração usam servidor/endpoint de teste que esteja permitido; resultados reais somente com fonte autorizada. Provar UI usando respostas positivas, vazias, expiradas e negadas. Nenhum '25 resultados' deverá ser anunciado como varredura global completa.
- Rodar `tools/check.py`, suíte Java/JVM aplicável, build Android no GitHub Actions (sem Root e Root/Zygisk conforme pipelines disponíveis), inspeção de assinatura/ABI/versionCode; distinguir testes automatizados de teste físico no Android (`device_tested=false` até comprovação).
- Anexar ao PR changelog, antes/depois dos componentes, inventário de arquivos alterados, commit-base, testes realmente executados, falhas, bloqueios, hashes, e caminho de rollback. Atualizar o MESMO `PGBR_ARQUIVO_MESTRE_UNICO.md` na branch/PR, acrescentando o histórico; não reduzir conteúdo anterior. Nunca afirmar APK, release, deploy, merge ou atualização do Drive sem URL de artefato verificável e leitura de volta.
- Se houver build/release realmente produzida, fornecer link DIRETO do APK instalável comprovado, não apenas a página de workflow; distinguir pré-release QA de homologação.

CRITÉRIO FINAL: Entregar implementação que funcione com dados comprovados; quando o provedor não permitir conexão, entregar recurso local funcional e adaptador inativo de forma honesta (não uma busca falsa). Prosseguir sem perguntas repetitivas. Informar claramente o que está FUNCIONANDO, o que está somente TESTADO COM DEMO, o que está BLOQUEADO POR FONTE e o que está EM PR.


============================================================
ADENDO V2 — PGSHARP + IPOGO + GATEWAY PRÓPRIO (23/11/2026)
============================================================
Este adendo COMPLEMENTA integralmente o prompt acima; nenhuma regra anterior foi retirada.

1) Integração multifontes: ao implementar esta fonte, respeitar também `PGSharpSourceAdapter` e `IPogoSourceAdapter` previstos nos prompts 04/05. Não confundir importação de filtros ou catálogos estáticos com um feed remoto ativo. Usar `source_id` verdadeiro (`pokelist`, `iflowgo`, `pogocoordinates`, `pgsharp`, `ipogo`, `pgbr_hundo`, `livepokemap`) e deduplicação por ID/fonte, TTL e referência de observação. Não rotular dados de um provedor como se fossem de outro.
2) ACERVO PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j ; APK/ZIP 1.278.0 (base 0.437.0), scripts PGSStats, configurações PGSData/PGSStats, código próprio em https://github.com/LucianoToscano/PGSharp e mapas de recursos. Os PNGs e layouts proprietários só entram em APK distribuído se os direitos forem verificados. `PgSharpDataImport` já lê JSON convertido e arquiva filtros; NÃO habilita feeds automaticamente.
3) ACERVO iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x ; APK 27.3 e Launcher Android; iPogo iOS 4.7.4; amostras extraídas sem jogo em https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_ . O relatório Android indica adaptação concentrada em `libmain.so` e um hook no `classes2.dex`; o relatório iOS aponta um framework híbrido `Sentry.framework` e IPA parcialmente incompleto. Isso NÃO é backend de dados, API pública nem SDK redistribuível.
4) GATEWAY DO PGBR: projetar serviço próprio separado do APK, com `/v1/capabilities`, `/v1/sources`, `/v1/search`, `/v1/imports`, `/v1/events`, `/v1/webhooks/subscriptions` e `/v1/health`; caminhos são PROPOSTOS para o nosso serviço, NÃO endpoints de fornecedores. Alimentar apenas por (a) APIs/feeds oficiais ou autorizados, (b) importação de dados próprios exportados conscientemente pelo usuário, (c) fontes abertas de metadados estáticos, ou (d) página pública que EXPRESSAMENTE permita coleta automatizada dentro dos limites. Respeitar robots/termos, sem bypass, sem cookies capturados, sem scraping de contas VIP, sem extração de chaves internas. Se a condição não for comprovada, desativar aquele conector remoto e continuar o gateway local.
5) PIPELINE: `ProviderAdapter -> PolicyGate -> Fetch/Import -> SchemaValidator -> Normalizer -> SourceRegistry -> Dedup/TTL Store -> FilterEngine -> API -> WebhookDispatcher -> PGBR Client`. Armazenar `observed_at`, `retrieved_at`, `expires_at`, `source`, `source_url`, `precision`, `quality`, `entitlement`, `coverage`, `is_demo`. Separar `static_reference`/`personal_import`/`live_external`/`simulated_fixture`. Sem atribuir status `live` a dados pessoais antigos.
6) WEBHOOK PRÓPRIO: emitir somente eventos derivados de alterações reais no nosso banco autorizado (`spawn.created/updated/expired`, `raid.*`, `quest.*`, `rocket.*`, `import.completed`, `source.unavailable` conforme fonte); assinatura HMAC-SHA256, `X-Event-Id`, `X-Event-Timestamp`, `X-Event-Signature`, proteção contra replay, idempotência, retries limitados, DLQ, revogação e configuração de destinos. Sem feed real, webhooks de spawn ficam INATIVOS; webhooks de importação/catálogo podem funcionar independentemente. Um scraper de polling, se permitido, não é webhook nativo do fabricante: declarar origem e cadência.
7) PRIORIZAR REAPROVEITAMENTO LOCAL: `PgSharpFeedPolicy`, `PgSharpFeedDialog`, `PgSharpLicenseVault`, `PgSharpDataImport`, `SettingsBackup`, `LiveFeedData`, `FeedChooser`, `LiveFeedFilters`, `ShundoSniper`, `HundoFeedDialog`, `PogoFeedClient`, `IFlowGoFeedClient`, `PokeListFeedClient`. PokeSearch é projeto separado; consultar seus filtros/catálogos e portar código de propriedade comprovada e compatível para PGBR via PR, sem alterar o aplicativo independente.
8) EXECUÇÃO SEM PERGUNTAS REPETIDAS: autorização do proprietário para editar/testar o repositório PGBR já está dada para esta tarefa. Não parar pedindo confirmação sobre branch, leitura, testes ou refatorações reversíveis. Respeitar direitos de terceiros, acesso a dados e dados pessoais; não inventar uma licença externa nem ativar plano pago. Ações de teleporte continuam com controle visível no app, cooldown e proteção de seleção.
9) VERIFICAÇÃO: primeiro testar com fixtures sintéticas e importações consentidas; depois com fonte real permitida (quando disponível). Evidenciar origem e sucesso de cada requisição e evento. Testar isolamento de dados, 401/403/429, quota, expiração, dedup e corte de acesso. Não fazer merge automático em main; branch por tarefa, PR, changelog e adendo no Mestre único. Não anunciar APK novo nem API/webhook em produção sem build/deploy verificado.
10) DOCUMENTAÇÃO COMUM: aplicar também o `00_PROMPT_MESTRE_PGBR_5_FONTES_GATEWAY_2026-11-23.txt`, `04_PROMPT_PGBR_PGSHARP_API_FILTROS_SNIPERS_2026-11-23.txt` e `05_PROMPT_PGBR_IPOGO_API_RADAR_2026-11-23.txt` do mesmo pacote, resolvendo conflitos em favor do HEAD atual do PGBR e mantendo este prompt específico.
~~~~

---

## <a id="secao-02"></a>Seção 02 — iFlowGo

**Origem integral:** `02_PROMPT_PGBR_IFLOWGO_MULTIFONTES_V2_2026-11-23.txt`  
**SHA-256 do texto integrado:** `fa9120a40a92d380ce39f03786e09470e926c43836239904684e02883166a2b1`  
**Tamanho:** 21173 bytes

~~~~text
PROMPT 02/05 — PGBR | IFLOWGO / POGO RADAR
MISSÃO: GLOBAL PAGINADO REAL + BUSCA LOCAL + CAMADAS DO MAPA

OBJETIVO ESPECÍFICO
Corrigir e enriquecer o radar iFlowGo já embutido no PGBR, especialmente a confusão entre GLOBAL e primeira página, filtros que parecem ativos mas não alteram o retorno e dados de diferentes camadas. Reutilizar o pipeline de PGBR e os materiais do Drive; não recriar interface vazia nem fazer varredura massiva de hotspots.

EVIDÊNCIAS / MATERIAIS A CONSULTAR
- Acervo iFlowGo: https://drive.google.com/drive/folders/1ysfYNt8JfEy3OkM_YE-BuRiiAzBbBuGm
- `02_CODIGO_CLIENTE_HOTSPOTS/iflowgo_client.py` e `hotspots.json`; `03_API_ENDPOINTS_SCHEMAS/03_API_ESQUEMA_CLIENTE_IFLOWGO.md`; `05_FILTROS_BUSCA_TESTES_PGBR` (inclui captura da UI com '25 recebidos' e scripts `poketest.py`, `raidtest.py`, `questtest.py`, `rockettest.py`, `pvptest.py`); `01_APLICATIVOS_WINDOWS_MAC_ANDROID` e assets de compatibilidade.
- Entrega de projeto de parceiro no Drive: https://drive.google.com/drive/folders/1LbHRvQRHyR6sgBD5EK9yBujKvZUIBSN5 com OpenAPI 3.1, SDK, backend ilustrativo, webhook receiver e testes. É pacote de desenvolvimento, NÃO prova de feed oficial já licenciado/implantado.
- Site: https://iflowgo.com ; PoGo Radar: https://pokecoords.iflowgo.com ; GitHub de referência Lucario: https://github.com/KernelX-debug/Discord-Bot_Lucario_Moonaniphp
- Código já existente no PGBR: `IFlowGoFeedClient`, `IFlowGoFeedData`, `IFlowGoRequestPolicy`, `LiveFeedDialog`, `LiveFeedFilters`, `LiveFeedData`, `PokemonSelectorDialog`; procurar regressões de LT041 e QA sobre radius NaN, espécie fora da faixa e estado das abas de raids.

CONTRATOS HISTÓRICOS NÃO OFICIAIS (USAR SOMENTE QUANDO O ACESSO FOR PERMITIDO)
- GET `https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/pokemon-search` com `sort_by,page,page_size,min_iv,max_iv,pokemon_id`; JSON candidato `items[],total_pages,total,page_size`.
- GET `https://pokecoords.iflowgo.com/iflowgopokecoords/api/v1/nearby` com `lat,lon,radius_km,layers,limit,pokemon_id,min_iv`; JSON candidato `spawns[]` e possivelmente outras camadas. Campos: `id,pokemon_id,lat,lon,percent_iv,cp,level,attack_iv,defense_iv,stamina_iv,gender,expires_at`.
- `hotspots.json` traz ~189 centros estáticos em 10 países, NÃO prova cobertura global, não é fonte de eventos e NÃO autoriza scan recorrente. `page_size=25`, `radius=25`, `limit=800` são escolhas de cliente de terceiros e não políticas oficiais.

IMPLEMENTAR NO PGBR
1. Separar `Global` (consulta por espécie/paginação permitida), `Próximos` (posição do PGBR e raio) e `Local definido` (coordenada digitada ou favorita). Implementar paginação por clique `Próxima página / Anterior` onde suportada e respeitada pela fonte; cancelar requisições antigas ao mudar filtros. Nunca fazer crawl de todas as páginas sem limitação. Exibir `Página 1`, `has_more`, total conhecido ou 'desconhecido', cobertura e última atualização. Não indicar 'todos no mundo' quando apenas 25 itens foram recebidos.
2. Filtragem por espécie via nome/#Dex com catálogo local, parâmetros IV mínimos/máximos, CP, nível, IVs A/D/S, validade, distância. Aplicar localmente apenas filtros cujos campos vieram no payload e informar `filtro local`; sem campo, marcar `não suportado` em vez de retornar registros falsamente compatíveis. Validar mudanças de modo, limpar paginação antiga ao alterar consulta e deduplicar páginas.
3. `nearby` possui camadas de Pokémon e reides previstas pelo cliente PGBR; implementar quests/Rocket/Power Spots SOMENTE quando resposta/schema e acesso disponíveis. Separar parse por camada e nunca apresentar um evento como spawn. Adicionar legenda de camada, contagem, filtros adequados, expiração e status por tipo.
4. Mapas, filtros e lista devem compartilhar o mesmo resultado normalizado; clique em Pokémon oferece copiar coords / destino no mapa / teleporte pela ação PGBR existente, respeitando estado/TTL. GPX somente para registros permitidos e selecionados manualmente; limitar export e impedir lista de coordenadas sintéticas ou expiradas.
5. Melhorar dados e UX de PGBR: sprites por Dex+forma, badges IV/CP/nível, distância km, ícones reides e tipo, seletor de raio 1/3/5 km e personalizado dentro de capacidade, filtros salvos, estados de rede. Reaproveitar `IFlowGoFeedData.MAX_ROWS`, mas não truncar silenciosamente sem aviso.
6. Validar condições de rede reais: 200 com linhas corretas, respostas vazias, total_pages=1, vários cursores/páginas, 401/403, 429, 5xx, NaN/Infinity, epoch TTL, clock skew, coordenadas duplicadas, envio de `pokemon_id`. Teste de 25 spawns prova 25 spawns e nada mais.
7. Alimentação futura por webhook/SSE: considerar apenas se API do fornecedor tiver eventos oficiais; o material `backend_app_webhooks.py` do Drive pode servir como gateway do PGBR com fonte interna autorizada e fixtures; não usar 189 hotspots como polling automático para simular push.
8. Confirmar que a alteração não interfere em LivePokeMap, RocketMap, PokeList, PoGoCoordinates, Sniper, PvP, controles de joystick/GPS e build Root/sem Root.

RESULTADO EXIGIDO
PR `feat/pgbr-iflowgo-radar-filtros-paginacao-20261123` (ajustar se ocupada); busca local e global com paginação honesta, filtros funcionais na modalidade efetivamente suportada, diagnóstico das camadas, testes, UI PT-BR e documentação de API disponível vs indisponível. Sem dependência de login/limites contornados, sem scraping e sem longas perguntas de confirmação.

============================================================
PROTOCOLO COMPARTILHADO — PGBR / FEEDS / SNIPERS — 23-11-2026
============================================================

PAPEL: Você é a IA responsável por DESENVOLVER E TESTAR CÓDIGO no repositório https://github.com/LucianoToscano/PGBR. A tarefa é acrescentar ou reparar um módulo DENTRO do PGBR (Android, PT-BR), não escrever uma carta ao proprietário de um site. Não entregar só planejamento, mock visual ou documentação se o código do PGBR estiver acessível.

MANDATO DE EXECUÇÃO: Inicie com as permissões já disponíveis no repositório e no Drive, sem pedir ao usuário confirmação a cada arquivo, etapa, teste, branch ou PR. Escolha soluções técnicas reversíveis. A autorização do titular do PGBR para trabalhar no PGBR não concede licença, credenciais, quotas VIP ou direitos de acesso a sistemas de terceiros. Programe a experiência e a lógica independente agora; ative chamadas de produção somente por fontes que realmente permitam a integração. Se uma fonte restringir integração, ofereça conector desabilitado com diagnóstico correto, entrada manual, importação de dados próprios, deep link ou outro feed permitido. Não contorne 401/403/429, captcha, assinatura, login, limites/reveal, paywall, TLS, Firebase, tokens privados ou termos de terceiros. Não peça nova autorização do usuário do PGBR para continuar partes locais independentes. Preserve confirmações de ação dentro do aplicativo quando necessárias para evitar teleporte acidental.

PRIMEIRA ETAPA OBRIGATÓRIA:
1. Ler INTEGRALMENTE `PGBR_ARQUIVO_MESTRE_UNICO.md`, `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/BUILDING.md`, `docs/ROOTED.md`, `THIRD_PARTY.md` e documentos de feeds/PRs pertinentes; depois confirmar HEAD de `main`, PRs e branches concorrentes, merges recentes, CI e releases. O Mestre contém snapshots históricos: código e CI atuais prevalecem sobre datas antigas.
2. Aplicar o PROTOCOLO GLOBAL — DESENVOLVIMENTO PARALELO SEM PERDAS: registrar commit-base, criar branch específica, comparar PRs abertas e arquivos afetados, preservar mudanças paralelas e criar PR revisável sem sobrescrever nem fazer merge automático em `main`. Não fazer reset destrutivo, não apagar histórico e não transformar PR de outro chat em branch-base sem auditar.
3. Inventariar classes do PGBR ANTES de criar duplicatas: `FeedChooser`, `LiveFeedDialog`, `LiveFeedClient`, `LiveFeedData`, `LiveFeedFilters`, `LiveFeedFilterDialog`, `PogoFeedDialog`, `PogoFeedClient`, `PogoFeedData`, `PogoFeedModes`, `IFlowGoFeedClient`, `IFlowGoFeedData`, `IFlowGoRequestPolicy`, `PokeListFeedClient`, `PokeListFeedData`, `HundoFeedDialog`, `ShundoSniper`, `PokemonSelectorDialog`, `CoordinateClipboard`, `Control`, `SettingsPanel`, `Pad`, `MapTheme`, `PtBr`, `LiveFeedSession`, `PgSharpFeedPolicy` e testes associados. Verifique os nomes/contratos na HEAD; não presumir implementação só por existir classe ou botão.
4. Preservar a arquitetura e as funcionalidades do PGRazor/PGBR existentes, incluindo controles de GPS/rotas, batalha, VGP+, inventário, sobreposições, Root, sem Root e módulo Zygisk. Não alterar o PokeSearch nem sua assinatura/release; reutilizar no PGBR apenas ideias, componentes e dados comprovadamente reutilizáveis, com créditos e proveniência, sem copiar recursos privados automaticamente.

ACERVO PARA PESQUISA (Drive conectado, confirmar itens/versões por leitura real):
- Banco de Dados Pokémon GO: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- Catálogos e recursos: `00 - Catálogo PGBR e PokeSearch`, `01 - Sprites, imagens, sons e modelos`, `02 - Dados, rankings e leitura de inventário`, `03 - Mapas, radar e fontes de eventos`, `04_APKs_de_Referencia`, `05_Links_Guias_e_Ferramentas`, `98_AUDITORIAS_APIS_WEBHOOKS_E_ENTREGAS`.
- Arquivo mestre de Drive do PGBR: https://drive.google.com/drive/folders/1aAXywzA-wjYhsk9ya4O_4-rgDeLT-DU8
- PokeSearch (consulta técnica, sem alterar): https://github.com/LucianoToscano/PokeSearch
- PGBR original: https://github.com/LucianoToscano/PGBR
- PGRazor upstream: https://github.com/jpitty03/PGRazor
- PokeMiners Game Master de referência: https://github.com/PokeMiners/game_masters
- PokeMiners/recursos e sprites encontrados no acervo: validar origem, versão, formas, licença e identidade dos assets antes de distribuir.

PROJETO DE DADOS COMPARTILHADOS — sem criar outro aplicativo:
A. Reaproveite classes existentes; quando faltar, criar uma interface pequena de adaptador de fontes, sem reescrever o PGBR: `capabilities()`, `search(query,cursor)`, `cancel()`, `sourceStatus()` e `attribution()`. Separar HTTP/credenciais, parse/normalização, persistência TTL, filtros, UI e ação de coordenadas. Não prometer endpoints genéricos que nenhum provedor oferece.
B. Modelo comum: `source_id`, `source_event_id`, `kind` (pokemon/raid/quest/rocket/... real), `species_id`, `form_id`, `lat`, `lon`, `coordinate_precision` (exact/approximate/unavailable), `iv_percent`, `iv_atk`, `iv_def`, `iv_sta`, `cp`, `level`, `gender`, `size`, `weather_boost`, `pvp_rank`, `observed_at_utc`, `expires_at_utc`, `retrieved_at_utc`, `is_demo`, `data_quality`, `coverage`, `source_url`, `server_filters_applied`, `local_filters_applied`, `next_cursor`, `has_more`, `total_known`. Não inventar campos, preencher com null quando ausentes; não confundir IV zero e campo ausente, shiny elegível com shiny garantido, rank calculado com rank de fonte.
C. Consolidar filtros REAIS por espécie/nome/#Dex, forma, IV 100/0/% e A/D/S, CP, nível, tamanho, gênero, clima, PvP GL/UL quando houver dados/algoritmo válido, tipo de evento, centro/raio/local, ordem e TTL. Identificar em cada origem quais filtros são remotos, locais ou não suportados, apresentando isso no app. Um filtro não deve retornar silenciosamente tudo e ser marcado como ativo. Modo Global deve declarar cobertura e paginação, não 'mundo inteiro' quando só há uma página ou hotspots.
D. Interface PT-BR no seletor já existente; estados `carregando`, `dados disponíveis`, `sem resultados reais`, `fora de cobertura`, `não autorizado pelo fornecedor`, `limite atingido`, `fonte indisponível`, `dado expirado`, `apenas demonstração`. Mostrar recebido/exibido/filtrado, nome de origem, hora UTC convertida para local, expiração, qualidade, cobertura e link de fonte. Não gerar coordenadas fictícias para telas normais.
E. Seleção de coordenadas: validar latitude/longitude (incluindo 0,0 e NaN), deduplicar, recusar expiração e coordenadas indisponíveis; permitir copiar `lat, lon` e usar a ação de teleporte que JÁ EXISTE no PGBR, com controles/cooldown/confirmação existentes. Não implementar novo sistema de GPS nem teleporte oculto. Camada Sniper 1/2 pode consumir a lista normalizada somente quando o usuário ativar a função e houver dados válidos/permitidos; não alterar ShundoSniper ou automações sem teste de regressão.
F. Rede robusta: HTTPS, timeouts, cancelamento, tamanho máximo do payload, validação estrita de JSON e timestamp (s/ms/ISO 8601), 429+Retry-After, backoff com jitter, caching apenas dentro do permitido, deduplicação, paginação limitada, circuit breaker por fonte e zero busy polling. Sem segredo de terceiro no APK nem token de usuário em log. Não usar WebView para extrair cookies/JWT nem executar scraping disfarçado.
G. Webhooks: o APK NÃO hospeda receptor público confiável. Integrar webhook por backend/gateway próprio autorizado do projeto, com assinatura HMAC, idempotência, replay protection, filas, retry e autenticação; push para app por um canal adequado ou consulta controlada. Se não existir stream verdadeiro, manter módulo desacoplado e mostrar 'Webhook não disponível', sem simular eventos ao vivo.
H. Filtros e preferências: armazenamento versionado, migração segura e perfil por fonte; presets IV 100, IV 0, PvP, raids, Rocket, XXL/XXS; exportação/importação local de JSON sem segredos. Reaproveitar sprites/cache da biblioteca central, mantendo identidade Dex+forma+shiny e fallback quando faltar imagem. Preservar nomes originais/links de atribuição.
I. Acessibilidade e UX: botões do PGBR coerentes com tema claro/escuro; PT-BR (preferir 'Teleporte' e não 'Teletransportar'); estados de carregamento e erro legíveis; não bloquear thread UI; não alterar layout global ou estilos não relacionados.

VALIDAÇÃO/PROVAS:
- Criar testes unitários do parser, filtro, cursor, paginação, TTL, datas/UTC, sexo/tamanho/forma, 0 IV x unknown, dedup, coordenadas inválidas, timeout/HTTP 401/403/410/429, Retry-After, cancelamento, cotas, isolamento entre contas, e fixtures realistas marcadas como DEMO.
- Testes de integração usam servidor/endpoint de teste que esteja permitido; resultados reais somente com fonte autorizada. Provar UI usando respostas positivas, vazias, expiradas e negadas. Nenhum '25 resultados' deverá ser anunciado como varredura global completa.
- Rodar `tools/check.py`, suíte Java/JVM aplicável, build Android no GitHub Actions (sem Root e Root/Zygisk conforme pipelines disponíveis), inspeção de assinatura/ABI/versionCode; distinguir testes automatizados de teste físico no Android (`device_tested=false` até comprovação).
- Anexar ao PR changelog, antes/depois dos componentes, inventário de arquivos alterados, commit-base, testes realmente executados, falhas, bloqueios, hashes, e caminho de rollback. Atualizar o MESMO `PGBR_ARQUIVO_MESTRE_UNICO.md` na branch/PR, acrescentando o histórico; não reduzir conteúdo anterior. Nunca afirmar APK, release, deploy, merge ou atualização do Drive sem URL de artefato verificável e leitura de volta.
- Se houver build/release realmente produzida, fornecer link DIRETO do APK instalável comprovado, não apenas a página de workflow; distinguir pré-release QA de homologação.

CRITÉRIO FINAL: Entregar implementação que funcione com dados comprovados; quando o provedor não permitir conexão, entregar recurso local funcional e adaptador inativo de forma honesta (não uma busca falsa). Prosseguir sem perguntas repetitivas. Informar claramente o que está FUNCIONANDO, o que está somente TESTADO COM DEMO, o que está BLOQUEADO POR FONTE e o que está EM PR.


============================================================
ADENDO V2 — PGSHARP + IPOGO + GATEWAY PRÓPRIO (23/11/2026)
============================================================
Este adendo COMPLEMENTA integralmente o prompt acima; nenhuma regra anterior foi retirada.

1) Integração multifontes: ao implementar esta fonte, respeitar também `PGSharpSourceAdapter` e `IPogoSourceAdapter` previstos nos prompts 04/05. Não confundir importação de filtros ou catálogos estáticos com um feed remoto ativo. Usar `source_id` verdadeiro (`pokelist`, `iflowgo`, `pogocoordinates`, `pgsharp`, `ipogo`, `pgbr_hundo`, `livepokemap`) e deduplicação por ID/fonte, TTL e referência de observação. Não rotular dados de um provedor como se fossem de outro.
2) ACERVO PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j ; APK/ZIP 1.278.0 (base 0.437.0), scripts PGSStats, configurações PGSData/PGSStats, código próprio em https://github.com/LucianoToscano/PGSharp e mapas de recursos. Os PNGs e layouts proprietários só entram em APK distribuído se os direitos forem verificados. `PgSharpDataImport` já lê JSON convertido e arquiva filtros; NÃO habilita feeds automaticamente.
3) ACERVO iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x ; APK 27.3 e Launcher Android; iPogo iOS 4.7.4; amostras extraídas sem jogo em https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_ . O relatório Android indica adaptação concentrada em `libmain.so` e um hook no `classes2.dex`; o relatório iOS aponta um framework híbrido `Sentry.framework` e IPA parcialmente incompleto. Isso NÃO é backend de dados, API pública nem SDK redistribuível.
4) GATEWAY DO PGBR: projetar serviço próprio separado do APK, com `/v1/capabilities`, `/v1/sources`, `/v1/search`, `/v1/imports`, `/v1/events`, `/v1/webhooks/subscriptions` e `/v1/health`; caminhos são PROPOSTOS para o nosso serviço, NÃO endpoints de fornecedores. Alimentar apenas por (a) APIs/feeds oficiais ou autorizados, (b) importação de dados próprios exportados conscientemente pelo usuário, (c) fontes abertas de metadados estáticos, ou (d) página pública que EXPRESSAMENTE permita coleta automatizada dentro dos limites. Respeitar robots/termos, sem bypass, sem cookies capturados, sem scraping de contas VIP, sem extração de chaves internas. Se a condição não for comprovada, desativar aquele conector remoto e continuar o gateway local.
5) PIPELINE: `ProviderAdapter -> PolicyGate -> Fetch/Import -> SchemaValidator -> Normalizer -> SourceRegistry -> Dedup/TTL Store -> FilterEngine -> API -> WebhookDispatcher -> PGBR Client`. Armazenar `observed_at`, `retrieved_at`, `expires_at`, `source`, `source_url`, `precision`, `quality`, `entitlement`, `coverage`, `is_demo`. Separar `static_reference`/`personal_import`/`live_external`/`simulated_fixture`. Sem atribuir status `live` a dados pessoais antigos.
6) WEBHOOK PRÓPRIO: emitir somente eventos derivados de alterações reais no nosso banco autorizado (`spawn.created/updated/expired`, `raid.*`, `quest.*`, `rocket.*`, `import.completed`, `source.unavailable` conforme fonte); assinatura HMAC-SHA256, `X-Event-Id`, `X-Event-Timestamp`, `X-Event-Signature`, proteção contra replay, idempotência, retries limitados, DLQ, revogação e configuração de destinos. Sem feed real, webhooks de spawn ficam INATIVOS; webhooks de importação/catálogo podem funcionar independentemente. Um scraper de polling, se permitido, não é webhook nativo do fabricante: declarar origem e cadência.
7) PRIORIZAR REAPROVEITAMENTO LOCAL: `PgSharpFeedPolicy`, `PgSharpFeedDialog`, `PgSharpLicenseVault`, `PgSharpDataImport`, `SettingsBackup`, `LiveFeedData`, `FeedChooser`, `LiveFeedFilters`, `ShundoSniper`, `HundoFeedDialog`, `PogoFeedClient`, `IFlowGoFeedClient`, `PokeListFeedClient`. PokeSearch é projeto separado; consultar seus filtros/catálogos e portar código de propriedade comprovada e compatível para PGBR via PR, sem alterar o aplicativo independente.
8) EXECUÇÃO SEM PERGUNTAS REPETIDAS: autorização do proprietário para editar/testar o repositório PGBR já está dada para esta tarefa. Não parar pedindo confirmação sobre branch, leitura, testes ou refatorações reversíveis. Respeitar direitos de terceiros, acesso a dados e dados pessoais; não inventar uma licença externa nem ativar plano pago. Ações de teleporte continuam com controle visível no app, cooldown e proteção de seleção.
9) VERIFICAÇÃO: primeiro testar com fixtures sintéticas e importações consentidas; depois com fonte real permitida (quando disponível). Evidenciar origem e sucesso de cada requisição e evento. Testar isolamento de dados, 401/403/429, quota, expiração, dedup e corte de acesso. Não fazer merge automático em main; branch por tarefa, PR, changelog e adendo no Mestre único. Não anunciar APK novo nem API/webhook em produção sem build/deploy verificado.
10) DOCUMENTAÇÃO COMUM: aplicar também o `00_PROMPT_MESTRE_PGBR_5_FONTES_GATEWAY_2026-11-23.txt`, `04_PROMPT_PGBR_PGSHARP_API_FILTROS_SNIPERS_2026-11-23.txt` e `05_PROMPT_PGBR_IPOGO_API_RADAR_2026-11-23.txt` do mesmo pacote, resolvendo conflitos em favor do HEAD atual do PGBR e mantendo este prompt específico.
~~~~

---

## <a id="secao-03"></a>Seção 03 — PoGoCoordinates + PokeSearch

**Origem integral:** `03_PROMPT_PGBR_POGOCOORDINATES_POKESEARCH_MULTIFONTES_V2_2026-11-23.txt`  
**SHA-256 do texto integrado:** `4b0cf1c38f78ad4bf5d7a14a75defe03d416e84092c6f93df5852361f38f5689`  
**Tamanho:** 24460 bytes

~~~~text
PROMPT 03/05 — PGBR | POGOCOORDINATES + REAPROVEITAMENTO POKESEARCH
MISSÃO: FILTROS REAIS, FEEDS, COORDENADAS, RANK PVP E SNIPERS 1/2

OBJETIVO ESPECÍFICO
Completar o módulo PoGoCoordinates que JÁ existe no PGBR e utilizar, COMO REFERÊNCIA DE UX/MODELO, o trabalho histórico do PokeSearch para melhorar busca, filtros, modos IV/GL/UL e perfis do PGBR. Tudo dentro do PGBR, sem precisar reconstruir o PokeSearch, sem usar dados privados, sem copiar segredos e sem prometer coordenadas que o servidor não liberou. A prioridade é transformar filtros que não buscam ou não refletem o estado em ações reais com resposta observável, com o máximo de funcionamento local independente do fornecedor.

ACERVO E CÓDIGO PARA LER ANTES DE EDITAR
- Central PoGoCoordinates no Drive: https://drive.google.com/drive/folders/19SDhX8DZrUTjMFlinIREi17Z9rItjQ4c
- `01_API_ESPECIFICACOES_SCRAPERS` (catálogo histórico/diff/API), `02_SITE_HTML_CAPTURAS` (snapshots, não backend), `03_FILTROS_IMAGENS_E_EXEMPLOS` (37 capturas), `04_FILTROS_SALVOS_JSON` (3 JSONs), `05_APK_REFERENCIAS`, `06_GITHUB_WEBHOOKS_FONTES`, `07_HISTORICOS_POKESEARCH` (PROMPT_MESTRE_POKESEARCH_LT62_COMPLETO, DESCRICAO_COMPLETA_POKESEARCH_FUNCOES_E_DESIGN_LT62) e auditoria de limitações.
- Entrega demonstrativa no Drive: https://drive.google.com/drive/folders/1__GwIIO9hjDtTeBsRwBUnsRWL3UDF675 (OpenAPI, SDKs e mock; status DEMONSTRAÇÃO, nenhum backend real implantado e webhook ao vivo NÃO disponível). Adaptar schemas e testes, jamais apontar app de produção para servidor mock como se fosse feed real.
- PokeSearch: https://github.com/LucianoToscano/PokeSearch ; usar seus padrões públicos, nomes de filtros, catálogos, layout e componentes próprios permitidos, sem alterar repositório, login, release ou dados de seus usuários. Fonte de UX não implica que haja um endpoint de coordenadas transferível.
- Código PGBR: `PogoFeedDialog.java` (interface grande já existente), `PogoFeedClient.java`, `PogoFeedData.java`, `PogoFeedModes.java`, `PogoFeedIcons.java`, `LiveFeedSession.java`, `FeedChooser.java`, `HundoFeedDialog.java`, `ShundoSniper.java`, `CoordinateClipboard.java`, `PokemonSelectorDialog.java`; ler testes atuais antes de substituí-los.
- Serviço: https://pogocoordinates.com ; rotas do site `/search`, `/map`, `/feeds`, `/nests`, `/shiny-rates`, `/vip`, `/terms`. Plano Free/VIP e cobertura variam; os termos do provedor prevalecem. O catálogo histórico cita `coordinates-api.pokemongopro.com` mas NÃO é autorização de integração.

PONTOS CRÍTICOS DE IDENTIDADE E ACESSO
- A busca histórica `POST /search` pode devolver Pokémon com `reveal_code` SEM latitude/longitude. Não tentar adivinhar, enumerar ou consumir reveal_code em massa; nenhum script que copie JWT de navegador, bypass VIP, scraping de HTML/Next.js ou CORS, brute force, cadência abusiva ou 403/429. Coordenadas exatas só são utilizáveis se a própria fonte as liberar em fluxo permitido; caso contrário mostrar 'Localização não liberada' e opção de abrir a página oficial do resultado.
- `Shiny only`/`background_only` podem depender de vínculo de treinador e consentimento. `shiny_eligible` e taxa shiny não são confirmação de shiny da conta; manter desativado/explicado sem token de treinador válido e integração permitida.
- O PGBR pode manter fichas de consulta, presets e ranking, mesmo sem live coords. Isso não substitui feed vivo. Acesso permitido de terceiros pode variar por recurso/plano; mostrar capabilities reais.

IMPLEMENTAR, APROVEITANDO POKESEARCH
1. REVISÃO DA UI EXISTENTE: mapear cada botão e função de `PogoFeedDialog` ao estado/modelo/requisição/retorno. Criar testes que provem mudanças na consulta em cada clique. Corrigir filtros inertes, estado visual ON falso, perda de seleção ao voltar, scroll, carregamentos duplicados e filtros que resetam outras escolhas. Não descartar os 1.665+ linhas existentes sem auditoria de regressão.
2. MODOS DE BUSCA: `100% IV` (A/D/S 15/15/15), `0% IV` (0/0/0), `IV personalizado` (três faixas independentes 0..15), `IV%` (0..100), `GL` e `UL` (top 1..50 se rank válido), com opção de incluir pré-evoluções se o cálculo/catálogo realmente suportar. GL e UL salvam ranking separadamente. Selecionar modo não deve ativar outros modos simultaneamente. Padrão só pesquisa com os campos que realmente existem.
3. OUTROS FILTROS: espécie/nome/#Dex; múltiplas formas e IDs de forma dinamicamente válidos (acervo histórico cita ~337, não fixar quantidade); sexo Any/Macho/Fêmea/Sem gênero; tamanhos XXS/XS/S/M/L/XL/XXL multisseleção; clima Any/Boosted/Sunny/Rainy/Partly Cloudy/Cloudy/Windy/Snow/Fog conforme suporte; PC 0..9999; nível 1..35 quando compatível; localização por `lat,lon`, raio em km, localidade, salvar local; ordenar por despawn, CP, IV%, nível e distância. Min>Max inválido; campo nulo significa ausência de filtro, não zero. Mostrar badge com número de critérios adicionais verdadeiramente ativos (sem somar valores padrão).
4. PRECISÃO DOS ÍCONES: reusar os PNGs e sprites mapeados na biblioteca PGBR/PokeSearch: estrelas 0/1/2/3 e RawIV, GL, UL, clima, gênero, tamanhos, brilho shiny, Rocket e itens. Verificar identidade e licença; se a imagem não estiver utilizável, usar ícone próprio coerente, sem imagem errada. Seguir o design estudado no PokeSearch para tamanho/contraste sem mudar toda a UI do PGBR. Texto em PT-BR; usar 'Teleporte'.
5. PERFIS: importar os três `saved-filters*.json` do Drive como EXEMPLOS de esquema, ler validação de campos e fazer migração versionada para presets do PGBR; nunca enviar esses arquivos à conta de outro usuário. Botões: aplicar, salvar, renomear, duplicar, excluir, exportar, importar e restaurar padrão; sync visual total entre filtros e resultado. Modo manual não dispara pesquisas sem clique, automático limitado segundo política realmente permitida.
6. FONTES E CAMADAS: PoGoCoordinates oferece páginas públicas de busca/mapa/feeds; API histórica descreve raids, quests, Rocket, clima, Power Spots, Rare Candy, ninhos, feeds prontos/customizados, alertas, catálogos, shiny-rates. Implementar adaptadores por capability somente onde haja fonte realmente acessível e direito de uso; itens sem dados reais devem ficar com estado `não suportado pela fonte/conta`. Não falsificar cobertura ou QR/coords.
7. SNIPER 1 / SNIPER 2: investigar implementação REAL atual do PGBR e PRs paralelas, mapear como recebem coordenadas. Permitir escolher fonte PGBR Hundo / iFlowGo / PokeList / PoGoCoordinates permitida, de forma explícita; buscar -> validar -> listar -> copiar ou encaminhar para mecanismo de teleporte já existente. O Sniper 1 prioriza busca manual com filtro de espécie/IV/CP/TTL/raio; o Sniper 2 oferece presets, lista múltipla de resultados, ordenação, favoritos e filtros PvP. Não declarar automático, live ou integrado se não existir dado. Deixar automação de movimentos/captura separada e sob controle do usuário; preservar cooldown e prevenção de ações acidentais. Não integrar o reveal em massa ao ShundoSniper.
8. REAPROVEITAMENTO POKESEARCH: fazer um mapeamento de quais classes, modelos de busca, formatos de preset, ícones e catálogos são de domínio do próprio projeto; portar lógica de filtro/ranking e UX que seja reutilizável, com autoria e testes, através do contrato comum, mantendo código e release do PokeSearch intactos. Não importar credenciais Google/Discord, banco Firebase, tokens, ID de usuário ou componentes dependentes do runtime do PokeSearch.
9. RESULTADOS: fonte exata, data e TTL, `coordinate_precision`, resultados e quantos foram filtrados, qualidade do IV/PvP, se modo 'global' está em primeira página e limitações por plano. Lista e mapa com dados consistentes; nenhum resultado sem latitude/longitude pode acionar teleporte. Botão copiar coords somente com coordenadas retornadas de forma permitida, status positivo e dado não expirado.
10. TESTES: matriz de cada filtro (servidor vs local), perfis importados, seis modos de IV/PvP, >1 forma, combinações de gênero/tamanho/clima, reset, gl/ul independentes, shiny não verificado, reveal indisponível, 401/403/410/429, contas separadas, TTL, feed vazio, sem rede, regressão com PGBR Hundo, LivePokeMap, PGSharp Feed inativo e seleção de Snipers.

RESULTADO EXIGIDO
PR `feat/pgbr-pogocoordinates-pokesearch-filtros-snipers-20261123` (ajustar nome se ocupada); correções reais nos controles existentes, presets, UI, testes e matriz de disponibilidade. Reaproveitar o PokeSearch como referência/ativos autorizados sem fundir aplicativos e sem depender de licenças não existentes para completar a parte local. Não inventar webhook, feed, coordenadas, login, shiny ou produção operacional.

============================================================
PROTOCOLO COMPARTILHADO — PGBR / FEEDS / SNIPERS — 23-11-2026
============================================================

PAPEL: Você é a IA responsável por DESENVOLVER E TESTAR CÓDIGO no repositório https://github.com/LucianoToscano/PGBR. A tarefa é acrescentar ou reparar um módulo DENTRO do PGBR (Android, PT-BR), não escrever uma carta ao proprietário de um site. Não entregar só planejamento, mock visual ou documentação se o código do PGBR estiver acessível.

MANDATO DE EXECUÇÃO: Inicie com as permissões já disponíveis no repositório e no Drive, sem pedir ao usuário confirmação a cada arquivo, etapa, teste, branch ou PR. Escolha soluções técnicas reversíveis. A autorização do titular do PGBR para trabalhar no PGBR não concede licença, credenciais, quotas VIP ou direitos de acesso a sistemas de terceiros. Programe a experiência e a lógica independente agora; ative chamadas de produção somente por fontes que realmente permitam a integração. Se uma fonte restringir integração, ofereça conector desabilitado com diagnóstico correto, entrada manual, importação de dados próprios, deep link ou outro feed permitido. Não contorne 401/403/429, captcha, assinatura, login, limites/reveal, paywall, TLS, Firebase, tokens privados ou termos de terceiros. Não peça nova autorização do usuário do PGBR para continuar partes locais independentes. Preserve confirmações de ação dentro do aplicativo quando necessárias para evitar teleporte acidental.

PRIMEIRA ETAPA OBRIGATÓRIA:
1. Ler INTEGRALMENTE `PGBR_ARQUIVO_MESTRE_UNICO.md`, `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/BUILDING.md`, `docs/ROOTED.md`, `THIRD_PARTY.md` e documentos de feeds/PRs pertinentes; depois confirmar HEAD de `main`, PRs e branches concorrentes, merges recentes, CI e releases. O Mestre contém snapshots históricos: código e CI atuais prevalecem sobre datas antigas.
2. Aplicar o PROTOCOLO GLOBAL — DESENVOLVIMENTO PARALELO SEM PERDAS: registrar commit-base, criar branch específica, comparar PRs abertas e arquivos afetados, preservar mudanças paralelas e criar PR revisável sem sobrescrever nem fazer merge automático em `main`. Não fazer reset destrutivo, não apagar histórico e não transformar PR de outro chat em branch-base sem auditar.
3. Inventariar classes do PGBR ANTES de criar duplicatas: `FeedChooser`, `LiveFeedDialog`, `LiveFeedClient`, `LiveFeedData`, `LiveFeedFilters`, `LiveFeedFilterDialog`, `PogoFeedDialog`, `PogoFeedClient`, `PogoFeedData`, `PogoFeedModes`, `IFlowGoFeedClient`, `IFlowGoFeedData`, `IFlowGoRequestPolicy`, `PokeListFeedClient`, `PokeListFeedData`, `HundoFeedDialog`, `ShundoSniper`, `PokemonSelectorDialog`, `CoordinateClipboard`, `Control`, `SettingsPanel`, `Pad`, `MapTheme`, `PtBr`, `LiveFeedSession`, `PgSharpFeedPolicy` e testes associados. Verifique os nomes/contratos na HEAD; não presumir implementação só por existir classe ou botão.
4. Preservar a arquitetura e as funcionalidades do PGRazor/PGBR existentes, incluindo controles de GPS/rotas, batalha, VGP+, inventário, sobreposições, Root, sem Root e módulo Zygisk. Não alterar o PokeSearch nem sua assinatura/release; reutilizar no PGBR apenas ideias, componentes e dados comprovadamente reutilizáveis, com créditos e proveniência, sem copiar recursos privados automaticamente.

ACERVO PARA PESQUISA (Drive conectado, confirmar itens/versões por leitura real):
- Banco de Dados Pokémon GO: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- Catálogos e recursos: `00 - Catálogo PGBR e PokeSearch`, `01 - Sprites, imagens, sons e modelos`, `02 - Dados, rankings e leitura de inventário`, `03 - Mapas, radar e fontes de eventos`, `04_APKs_de_Referencia`, `05_Links_Guias_e_Ferramentas`, `98_AUDITORIAS_APIS_WEBHOOKS_E_ENTREGAS`.
- Arquivo mestre de Drive do PGBR: https://drive.google.com/drive/folders/1aAXywzA-wjYhsk9ya4O_4-rgDeLT-DU8
- PokeSearch (consulta técnica, sem alterar): https://github.com/LucianoToscano/PokeSearch
- PGBR original: https://github.com/LucianoToscano/PGBR
- PGRazor upstream: https://github.com/jpitty03/PGRazor
- PokeMiners Game Master de referência: https://github.com/PokeMiners/game_masters
- PokeMiners/recursos e sprites encontrados no acervo: validar origem, versão, formas, licença e identidade dos assets antes de distribuir.

PROJETO DE DADOS COMPARTILHADOS — sem criar outro aplicativo:
A. Reaproveite classes existentes; quando faltar, criar uma interface pequena de adaptador de fontes, sem reescrever o PGBR: `capabilities()`, `search(query,cursor)`, `cancel()`, `sourceStatus()` e `attribution()`. Separar HTTP/credenciais, parse/normalização, persistência TTL, filtros, UI e ação de coordenadas. Não prometer endpoints genéricos que nenhum provedor oferece.
B. Modelo comum: `source_id`, `source_event_id`, `kind` (pokemon/raid/quest/rocket/... real), `species_id`, `form_id`, `lat`, `lon`, `coordinate_precision` (exact/approximate/unavailable), `iv_percent`, `iv_atk`, `iv_def`, `iv_sta`, `cp`, `level`, `gender`, `size`, `weather_boost`, `pvp_rank`, `observed_at_utc`, `expires_at_utc`, `retrieved_at_utc`, `is_demo`, `data_quality`, `coverage`, `source_url`, `server_filters_applied`, `local_filters_applied`, `next_cursor`, `has_more`, `total_known`. Não inventar campos, preencher com null quando ausentes; não confundir IV zero e campo ausente, shiny elegível com shiny garantido, rank calculado com rank de fonte.
C. Consolidar filtros REAIS por espécie/nome/#Dex, forma, IV 100/0/% e A/D/S, CP, nível, tamanho, gênero, clima, PvP GL/UL quando houver dados/algoritmo válido, tipo de evento, centro/raio/local, ordem e TTL. Identificar em cada origem quais filtros são remotos, locais ou não suportados, apresentando isso no app. Um filtro não deve retornar silenciosamente tudo e ser marcado como ativo. Modo Global deve declarar cobertura e paginação, não 'mundo inteiro' quando só há uma página ou hotspots.
D. Interface PT-BR no seletor já existente; estados `carregando`, `dados disponíveis`, `sem resultados reais`, `fora de cobertura`, `não autorizado pelo fornecedor`, `limite atingido`, `fonte indisponível`, `dado expirado`, `apenas demonstração`. Mostrar recebido/exibido/filtrado, nome de origem, hora UTC convertida para local, expiração, qualidade, cobertura e link de fonte. Não gerar coordenadas fictícias para telas normais.
E. Seleção de coordenadas: validar latitude/longitude (incluindo 0,0 e NaN), deduplicar, recusar expiração e coordenadas indisponíveis; permitir copiar `lat, lon` e usar a ação de teleporte que JÁ EXISTE no PGBR, com controles/cooldown/confirmação existentes. Não implementar novo sistema de GPS nem teleporte oculto. Camada Sniper 1/2 pode consumir a lista normalizada somente quando o usuário ativar a função e houver dados válidos/permitidos; não alterar ShundoSniper ou automações sem teste de regressão.
F. Rede robusta: HTTPS, timeouts, cancelamento, tamanho máximo do payload, validação estrita de JSON e timestamp (s/ms/ISO 8601), 429+Retry-After, backoff com jitter, caching apenas dentro do permitido, deduplicação, paginação limitada, circuit breaker por fonte e zero busy polling. Sem segredo de terceiro no APK nem token de usuário em log. Não usar WebView para extrair cookies/JWT nem executar scraping disfarçado.
G. Webhooks: o APK NÃO hospeda receptor público confiável. Integrar webhook por backend/gateway próprio autorizado do projeto, com assinatura HMAC, idempotência, replay protection, filas, retry e autenticação; push para app por um canal adequado ou consulta controlada. Se não existir stream verdadeiro, manter módulo desacoplado e mostrar 'Webhook não disponível', sem simular eventos ao vivo.
H. Filtros e preferências: armazenamento versionado, migração segura e perfil por fonte; presets IV 100, IV 0, PvP, raids, Rocket, XXL/XXS; exportação/importação local de JSON sem segredos. Reaproveitar sprites/cache da biblioteca central, mantendo identidade Dex+forma+shiny e fallback quando faltar imagem. Preservar nomes originais/links de atribuição.
I. Acessibilidade e UX: botões do PGBR coerentes com tema claro/escuro; PT-BR (preferir 'Teleporte' e não 'Teletransportar'); estados de carregamento e erro legíveis; não bloquear thread UI; não alterar layout global ou estilos não relacionados.

VALIDAÇÃO/PROVAS:
- Criar testes unitários do parser, filtro, cursor, paginação, TTL, datas/UTC, sexo/tamanho/forma, 0 IV x unknown, dedup, coordenadas inválidas, timeout/HTTP 401/403/410/429, Retry-After, cancelamento, cotas, isolamento entre contas, e fixtures realistas marcadas como DEMO.
- Testes de integração usam servidor/endpoint de teste que esteja permitido; resultados reais somente com fonte autorizada. Provar UI usando respostas positivas, vazias, expiradas e negadas. Nenhum '25 resultados' deverá ser anunciado como varredura global completa.
- Rodar `tools/check.py`, suíte Java/JVM aplicável, build Android no GitHub Actions (sem Root e Root/Zygisk conforme pipelines disponíveis), inspeção de assinatura/ABI/versionCode; distinguir testes automatizados de teste físico no Android (`device_tested=false` até comprovação).
- Anexar ao PR changelog, antes/depois dos componentes, inventário de arquivos alterados, commit-base, testes realmente executados, falhas, bloqueios, hashes, e caminho de rollback. Atualizar o MESMO `PGBR_ARQUIVO_MESTRE_UNICO.md` na branch/PR, acrescentando o histórico; não reduzir conteúdo anterior. Nunca afirmar APK, release, deploy, merge ou atualização do Drive sem URL de artefato verificável e leitura de volta.
- Se houver build/release realmente produzida, fornecer link DIRETO do APK instalável comprovado, não apenas a página de workflow; distinguir pré-release QA de homologação.

CRITÉRIO FINAL: Entregar implementação que funcione com dados comprovados; quando o provedor não permitir conexão, entregar recurso local funcional e adaptador inativo de forma honesta (não uma busca falsa). Prosseguir sem perguntas repetitivas. Informar claramente o que está FUNCIONANDO, o que está somente TESTADO COM DEMO, o que está BLOQUEADO POR FONTE e o que está EM PR.


============================================================
ADENDO V2 — PGSHARP + IPOGO + GATEWAY PRÓPRIO (23/11/2026)
============================================================
Este adendo COMPLEMENTA integralmente o prompt acima; nenhuma regra anterior foi retirada.

1) Integração multifontes: ao implementar esta fonte, respeitar também `PGSharpSourceAdapter` e `IPogoSourceAdapter` previstos nos prompts 04/05. Não confundir importação de filtros ou catálogos estáticos com um feed remoto ativo. Usar `source_id` verdadeiro (`pokelist`, `iflowgo`, `pogocoordinates`, `pgsharp`, `ipogo`, `pgbr_hundo`, `livepokemap`) e deduplicação por ID/fonte, TTL e referência de observação. Não rotular dados de um provedor como se fossem de outro.
2) ACERVO PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j ; APK/ZIP 1.278.0 (base 0.437.0), scripts PGSStats, configurações PGSData/PGSStats, código próprio em https://github.com/LucianoToscano/PGSharp e mapas de recursos. Os PNGs e layouts proprietários só entram em APK distribuído se os direitos forem verificados. `PgSharpDataImport` já lê JSON convertido e arquiva filtros; NÃO habilita feeds automaticamente.
3) ACERVO iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x ; APK 27.3 e Launcher Android; iPogo iOS 4.7.4; amostras extraídas sem jogo em https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_ . O relatório Android indica adaptação concentrada em `libmain.so` e um hook no `classes2.dex`; o relatório iOS aponta um framework híbrido `Sentry.framework` e IPA parcialmente incompleto. Isso NÃO é backend de dados, API pública nem SDK redistribuível.
4) GATEWAY DO PGBR: projetar serviço próprio separado do APK, com `/v1/capabilities`, `/v1/sources`, `/v1/search`, `/v1/imports`, `/v1/events`, `/v1/webhooks/subscriptions` e `/v1/health`; caminhos são PROPOSTOS para o nosso serviço, NÃO endpoints de fornecedores. Alimentar apenas por (a) APIs/feeds oficiais ou autorizados, (b) importação de dados próprios exportados conscientemente pelo usuário, (c) fontes abertas de metadados estáticos, ou (d) página pública que EXPRESSAMENTE permita coleta automatizada dentro dos limites. Respeitar robots/termos, sem bypass, sem cookies capturados, sem scraping de contas VIP, sem extração de chaves internas. Se a condição não for comprovada, desativar aquele conector remoto e continuar o gateway local.
5) PIPELINE: `ProviderAdapter -> PolicyGate -> Fetch/Import -> SchemaValidator -> Normalizer -> SourceRegistry -> Dedup/TTL Store -> FilterEngine -> API -> WebhookDispatcher -> PGBR Client`. Armazenar `observed_at`, `retrieved_at`, `expires_at`, `source`, `source_url`, `precision`, `quality`, `entitlement`, `coverage`, `is_demo`. Separar `static_reference`/`personal_import`/`live_external`/`simulated_fixture`. Sem atribuir status `live` a dados pessoais antigos.
6) WEBHOOK PRÓPRIO: emitir somente eventos derivados de alterações reais no nosso banco autorizado (`spawn.created/updated/expired`, `raid.*`, `quest.*`, `rocket.*`, `import.completed`, `source.unavailable` conforme fonte); assinatura HMAC-SHA256, `X-Event-Id`, `X-Event-Timestamp`, `X-Event-Signature`, proteção contra replay, idempotência, retries limitados, DLQ, revogação e configuração de destinos. Sem feed real, webhooks de spawn ficam INATIVOS; webhooks de importação/catálogo podem funcionar independentemente. Um scraper de polling, se permitido, não é webhook nativo do fabricante: declarar origem e cadência.
7) PRIORIZAR REAPROVEITAMENTO LOCAL: `PgSharpFeedPolicy`, `PgSharpFeedDialog`, `PgSharpLicenseVault`, `PgSharpDataImport`, `SettingsBackup`, `LiveFeedData`, `FeedChooser`, `LiveFeedFilters`, `ShundoSniper`, `HundoFeedDialog`, `PogoFeedClient`, `IFlowGoFeedClient`, `PokeListFeedClient`. PokeSearch é projeto separado; consultar seus filtros/catálogos e portar código de propriedade comprovada e compatível para PGBR via PR, sem alterar o aplicativo independente.
8) EXECUÇÃO SEM PERGUNTAS REPETIDAS: autorização do proprietário para editar/testar o repositório PGBR já está dada para esta tarefa. Não parar pedindo confirmação sobre branch, leitura, testes ou refatorações reversíveis. Respeitar direitos de terceiros, acesso a dados e dados pessoais; não inventar uma licença externa nem ativar plano pago. Ações de teleporte continuam com controle visível no app, cooldown e proteção de seleção.
9) VERIFICAÇÃO: primeiro testar com fixtures sintéticas e importações consentidas; depois com fonte real permitida (quando disponível). Evidenciar origem e sucesso de cada requisição e evento. Testar isolamento de dados, 401/403/429, quota, expiração, dedup e corte de acesso. Não fazer merge automático em main; branch por tarefa, PR, changelog e adendo no Mestre único. Não anunciar APK novo nem API/webhook em produção sem build/deploy verificado.
10) DOCUMENTAÇÃO COMUM: aplicar também o `00_PROMPT_MESTRE_PGBR_5_FONTES_GATEWAY_2026-11-23.txt`, `04_PROMPT_PGBR_PGSHARP_API_FILTROS_SNIPERS_2026-11-23.txt` e `05_PROMPT_PGBR_IPOGO_API_RADAR_2026-11-23.txt` do mesmo pacote, resolvendo conflitos em favor do HEAD atual do PGBR e mantendo este prompt específico.
~~~~

---

## <a id="secao-04"></a>Seção 04 — PGSharp

**Origem integral:** `04_PROMPT_PGBR_PGSHARP_API_FILTROS_SNIPERS_2026-11-23.txt`  
**SHA-256 do texto integrado:** `a160419b63fa1cbce9071e86baf6aa2b0572f944e007ab6a5cd910485f99939c`  
**Tamanho:** 9502 bytes

~~~~text
PROMPT 04/05 — IMPLEMENTAÇÃO PGBR + PGSHARP
OBJETIVO: transformar o acervo PGSharp no Drive e o código PRÓPRIO existente em filtros, favoritos, presets e Snipers 1/2 do PGBR, com uma API própria de importação e notificações permitidas.

FONTES OBRIGATÓRIAS A CONFERIR:
- Drive PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j
- APKs: https://drive.google.com/drive/folders/1teTVuA_xc_VG51lEWSeebq_n2-YFX4tj ; recorte do plugin 1.278.0 / Pokémon GO 0.437.0: https://drive.google.com/drive/folders/18gejA95KBEaGTt2JZZ5IM3CNyFuo1Rub
- Dados/configurações PGSData e PGSStats: https://drive.google.com/drive/folders/1aTRQQ2Urliub8vIvgSkYeVtQxKCNPCKm ; código de Apps Script PGSStats API em `08_Scripts_e_Automacoes`, se acesso e escopo permitirem.
- Dossiês e relatórios: https://drive.google.com/drive/folders/1Mn--jCnwWqvfcB-gaInco6KrpFyP-XCG ; assets catalogados em https://drive.google.com/drive/folders/1HHNySaQo9w6NU3XuoQX0KBbTUy6ds46O
- Código próprio: https://github.com/LucianoToscano/PGSharp (`CustomShortcutsModel`, `NativeMenuCoordinator`, `LiveFavoritesOps`, `FavoritesOverlay` e testes); código base PGBR: `PgSharpDataImport`, `PgSharpFeedPolicy`, `PgSharpFeedDialog`, `PgSharpLicenseVault`, `SettingsBackup`, `HundoFeedDialog`, `ShundoSniper`, `FeedChooser`.
- Análise de referência (não licença automática): https://github.com/romainthomas/pgsharp. A informação de versões de 2021 é histórica e não comprova APIs atuais.
- Site: https://www.pgsharp.com/ ; versão de acervo 1.278.0, base 0.437.0. Em 23/11/2026 a página pública cita 100 IV Feed como Free e All Coordinates Feed como Standard; a funcionalidade anunciada NÃO é endpoint público de terceiros.

DIAGNÓSTICO COMPROVADO: `PgSharpFeedPolicy.currentConnector() = UNVERIFIED`; `PgSharpDataImport` aceita JSON de migração, mas os filtros ficam ARQUIVADOS, não executados. A própria referência PGSharp pessoal tem componentes de UI/menus sem adaptador nativo real. Não falsear 'conectado' ou `supportsFourIndependentSniperFeeds()`.

IMPLEMENTE EM BRANCH INDEPENDENTE:
1. Inventariar `.dat`, exportações JSON pessoais, PGSStats, PGSData, filtros e rotas. Confirmar versões, hash, campos, proveniência, status de segredo; nunca executar desserialização Java de arquivo não confiável nem colocar credenciais no Git/Drive público. Registrar distinção entre exportação pessoal e recurso de feed.
2. Estender com segurança o importador JSON próprio `PgSharpDataImport`, preservando dados originais em backup e realizando mapeamento explícito para modelo CANÔNICO DE FILTROS: IV%/raw IV, PC/nível, espécie, forma, sexo, tamanho, clima, IV 100/0, Rank PvP GL/UL, reides, pesquisas, locais, raio, TTL, prioridade, ordenação. Garantir que campos desconhecidos permaneçam em `raw_source` sem se perderem ou serem aplicados erroneamente; fornecer relatório convertido/preservado/não suportado.
3. Filtros Sniper 1/2: modelos independentes de presets, busca e lista, seleção de fonte autorizada, critérios AND/OR coerentes, salvar/editar/importar/exportar, botão de aplicação real e contadores. Separar `Sniper I` (busca manual rápida IV 100 e personalizada) e `Sniper II` (combinações de filtro e PvP/raids/quests conforme dados). Se o motor nativo só suportar 2 slots, NÃO anunciar 3/4. Com o código próprio `CustomShortcutsModel`, portar somente o que encaixa na arquitetura PGBR e testar persistência real.
4. Página PGSharp no PGBR: substituir estados confusos por `dados locais importados`, `feed próprio PGBR`, `feed remoto PGSharp não verificado`, `plano Free/Standard no site oficial`; não confundir chave guardada com validação. Não tentar chamar serviço privado encontrado por engenharia reversa para consumir o All Coordinates Feed nem manipular verificação de VIP. Acesso remoto só com SDK/API/contrato efetivamente permitido pelo fornecedor.
5. Criar `PGSharpLocalImportAdapter`, `PGSharpFilterMapping`, `PGSharpFavoritesBridge`, `PGSharpFeedAdapter` (este último DESABILITADO até conexão permitida), camada de capabilities e testes. Reusar `LiveFeedData`, `LiveFeedFilters`, `PokemonSelectorDialog`, `CoordinateClipboard`, sprite catalog validado e UI atual. Não duplicar motor de localização/cooldown.
6. API PRÓPRIA do PGBR para configurações/importação: contratos sugeridos `POST /v1/imports/pgsharp`, `GET /v1/imports/{id}`, `GET /v1/presets?source=pgsharp`, `GET /v1/capabilities?source=pgsharp`, `POST /v1/filter/preview` e `POST /v1/sources/search` somente para provedores efetivamente conectados. A API é NOSSA, não renomeie endpoint privado do PGSharp. Para dados pessoais, preferir execução local; se houver gateway, criptografar dados por conta, opt-in explícito, retenção mínima, autenticação por usuário e exclusão.
7. Webhooks próprios: `import.completed`, `preset.created/updated/deleted`, `source.status_changed` e eventos de spawn APENAS quando chegarem de feed real permitido. HMAC, ID, timestamp, idempotência, retry, revisão e remoção do destino. Nenhuma aparência de webhook oficial PGSharp.
8. Scraper: não é necessário para importação de JSON/.dat. Se houver página pública que expressamente permita coleta automatizada, desenvolver parser com teste de permissão/robots/quotas, user agent identificável, incremental cache/ETag e backoff; usar só metadados públicos (versões, catálogos de features e documentação), nunca painel VIP, login ou feed protegido. Não inferir spawns de screenshots estáticos.
9. Qualidade e UI: 130 registros Pokémon / 14 Raid / 28 Quest do `.dat` público e 54 locais reais + 26 entradas informativas, 18 rotas são contagens históricas no guia Drive — comparar exportação real antes de afirmar números atuais; ignorar coordenadas `0,0` de legendas como destinos. Validar favoritos/rotas, GPX e separação de dados por conta. Testar importação 2 vezes (idempotência), filtros inconsistentes, retrocompatibilidade e ausência de regressões de batalha/GPS/VGP.
10. Entregar código PGBR, testes, PR, migrações, documentação API OpenAPI 3.1, exemplos de importação SANITIZADOS, relatório de filtros suportados, checkpoint do Mestre, CI build (quando possível), evidências de funcionamento e bloqueios reais. Nunca afirmar 'PGSharp API conectada' antes de request de integração permitido testado com resposta real.

CRITÉRIO: ao menos funcionalidades LOCAIS de importação, filtros, presets e API de importação (se houver backend) devem estar operacionais independentemente de licença externa; feed externo é opção condicionada à fonte real.

============================================================
REGRAS GERAIS DO DESENVOLVIMENTO — PGBR
============================================================
- Repositório: https://github.com/LucianoToscano/PGBR
- Consultar `PGBR_ARQUIVO_MESTRE_UNICO.md` INTEGRAL, AGENTS.md, THIRD_PARTY.md, docs/BUILDING.md, docs/ARCHITECTURE.md, docs/ROOTED.md, docs de fontes, branches, PRs abertas, HEAD real, CI e release antes de qualquer modificação.
- Estado verificado em 23/11/2026: README relata LT043 QA publicada, com importação local de dados PGSharp e integração parcial de Rocket/Max/HUD; Snipers/feeds de PRs paralelas NÃO são automaticamente incorporados ao APK LT043. Conferir novamente HEAD e PRs atuais antes de codificar.
- Protocolo GLOBAL SEM PERDAS: branch independente, commit-base, não sobrescrever PR paralela, registrar diffs/testes, PR revisável, não fazer merge/deploy/release em `main` automaticamente, rollback e atualização ADITIVA do arquivo Mestre. Não modificar o repositório PokeSearch sem tarefa separada.
- Não perguntar confirmações repetidas para trabalho no próprio código PGBR ou decisões reversíveis. Não presumir que consentimento do proprietário PGBR conceda autorização de backend/licença VIP/copyright de terceiros. Não copiar credenciais/segredos, contornar autenticação, rate limits, 401/403/429, captcha, assinatura, criptografia nem extrair feeds protegidos de APK. Usar material binário de terceiros apenas como pesquisa/compatibilidade onde permitido.
- Fontes existentes precisam ser consideradas primeiro: FeedChooser, LiveFeedDialog, LiveFeedData, LiveFeedFilters, LiveFeedSession, PogoFeedDialog/Client/Data, IFlowGoFeedClient/Data/Policy, PokeListFeedClient/Data, HundoFeedDialog, ShundoSniper, PgSharpFeedPolicy, PgSharpFeedDialog, PgSharpLicenseVault, PgSharpDataImport, AccountProfiles, SettingsBackup, CoordinateClipboard. Verificar nomes na HEAD atual, não criar duplicatas.
- Testar local/com fixtures e simulação controlada, depois integração real somente se disponível; verificar 401/403/429, Retry-After, cancelamento, TTL, timezone, validação de coordenadas, expiração, modo global/paginação, isolamento de contas, tolerância a falhas, memória e regressão Root/sem Root/Zygisk. Separar `implemented`, `test_passed`, `device_tested`, `provider_connected`, `staging_deployed` e `release_published`; não converter um no outro.
- Filtros e importações são funcionalidades independentes da ativação de feeds externos. Entregar o máximo de código útil que funcione offline/localmente sem fabricar resultados de spawns, IV exatos ou shiny de conta.
- Se build for gerada, entregar link DIRETO de APK verificável, SHA-256 e assinatura, não somente página de workflow. No app usar português do Brasil, especialmente 'Teleporte'.
~~~~

---

## <a id="secao-05"></a>Seção 05 — iPogo

**Origem integral:** `05_PROMPT_PGBR_IPOGO_API_RADAR_2026-11-23.txt`  
**SHA-256 do texto integrado:** `a367f61ca7cf3c633d5e2d99999abf0989fe9e7fb9e85eda3fa33af2cdf64fd5`  
**Tamanho:** 9050 bytes

~~~~text
PROMPT 05/05 — IMPLEMENTAÇÃO PGBR + IPOGO
OBJETIVO: converter estudo do iPogo Android/iOS em requisitos funcionais próprios do PGBR, modelo de radar, listas globais/locais, filtros, notificações e gateway/API própria, sem confundir SDK proprietário do iPogo com uma fonte pública de coordenadas.

ACERVO OBRIGATÓRIO:
- iPogo principal: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x ; Android `ipogo-27.3.apk`, `ipogo-launcher-27.3.apk`, APK clone e iOS `ipogo-4.7.4.ipa`.
- Extrações sem jogo: https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_ ; Android `IPogo-27_3_sem_Pokemon_Go_APK/LEIAME.md` e iOS `IPogo_iOS_sem_Pokemon_Go/LEIAME.md`.
- O relatório Android descreve diferença concentrada em `lib/arm64-v8a/libmain.so`, hook em `classes2.dex` e classes `app.ipogo.*` não disponibilizadas em forma Java legível. Não existe backend ou feed público comprovado nesse recorte.
- O relatório iOS descreve `Sentry.framework` com dados do iPogo, mas o IPA original teve 287 arquivos Unity sem dados e o recorte NÃO é app funcional. Não copiar o framework binário para Android.
- Documentação de funcionalidades da empresa: https://ipogo.app/features.php ; Global/Custom Feed Overlay, Nearby Pokémon, Global List (raids, Pokémon, PokéStops, quests e Rocket), Shiny Scanner, Special Background Notifications, coordenadas, hotspots, rotas. São referências de UX, não uma API de parceiros anunciada.
- PGBR existente: `LiveFeedDialog`, `LiveFeedData`, `LiveFeedClient`, `LiveFeedFilters`, `FeedChooser`, `ShundoSniper`, `HundoFeedDialog`, `PogoFeedDialog`, `IFlowGoFeedClient`, `PokeListFeedClient`, `AccountProfiles`, `CoordinateClipboard`; PokeSearch pode inspirar apresentação e filtros sem ser modificado.

MÓDULOS PRÓPRIOS PARA DESENVOLVER:
1. `IPogoFeatureMatrix` classifica cada recurso: `UX/reference`, `offline/local`, `feed_available_via_other_authorized_source`, `ipogo_connector_unverified`. Distinguir simulador/fixture de aparição real.
2. `IPogoInspiredNearbyRadar`: painel de próximos usando dados já recebidos do PGBR/serviços PERMITIDOS, com espécie, CP/IV se disponíveis, TTL, precisão, botão copiar coordenadas e atalho voluntário de teleporte existente. Não ler processos/credenciais de iPogo nem aumentar raio do cliente de Pokémon GO por hook extraído.
3. `IPogoInspiredGlobalList`: abas Pokémon, Reides, Pesquisas, Rocket/PokéStops somente com fontes que tenham dados. Suporte a `cursor`, `has_more`, cobertura e limites; nunca interpretar primeira página mundial como todos os resultados. Sem fonte, mostrar 'sem feed autorizado' e permitir catálogos/filtros locais.
4. `IPogoInspiredCustomFeed`: editor de perfis salvos (espécies, evolução, IV 100/0, porcentagem e raw IV 0..15, GL/UL, nível, PC, formas, gênero, XXS/XXL, clima, raio, TTL, filtros por evento), com separação server-side/local/unsupported; perfis exportáveis JSON e testes. Títulos PT-BR e sem promessa de Shiny garantido.
5. `IPogoInspiredAlerts`: notificações sobre resultados válidos do nosso repositório (Shiny confirmado por observação vinculada e autorizada, special background confirmado por campo real, Pokémon raros, IV/GL/UL se disponíveis, raids/quest/Rocket). `shiny_eligible` não significa Shiny de uma conta. Nenhuma notificação de spawn real pode sair só de um sprite/catálogo.
6. `IPogoUserImportAdapter`: aceitar somente exportações que o próprio usuário disponibilize legitimamente em JSON/CSV/GPX, validar tamanho/schema/assinatura opcional e deduplicar; compartilhar dados pessoais com backend apenas por opt-in. Não pressupor que APK iPogo exporte coords ou configurações nesse formato sem teste.
7. `IPogoRemoteFeedAdapter`: contrato desabilitado até ser comprovada API/documentação oficial, escopo e fonte atual de dados. A documentação do app não comprova API pública; não extrair endpoint, chave, payload autenticado ou decifrador a partir do binário para imitar o cliente. Não injetar no iPogo, não intercetar tráfego cifrado, não burlar VPN/proteções/tier VIP.
8. API própria: `GET /v1/capabilities?source=ipogo`, `POST /v1/imports/ipogo` (se houver arquivo exportado), `POST /v1/filters/preview`, `GET /v1/sources/status`, `POST /v1/search` (fonte permitida), `POST /v1/webhooks/subscriptions`. O nome iPogo em `source=ipogo` pode marcar ORIGEM DE PERFIL IMPORTADO, mas NUNCA rotular resultado de iFlowGo/PokeList como dados iPogo. Separar `origin_kind=personal_import|third_party_live|static_catalog`.
9. Webhooks próprios: `import.completed`, `filter.updated`, `provider.unavailable` e `spawn.created/updated/expired` apenas para os feeds de terceiros PERMITIDOS recebidos no gateway. Endpoints HTTPS, assinatura HMAC, filtros, idempotência, retries, DLQ e revogação; não alegar que o fabricante iPogo envia webhook.
10. Scraper: só páginas públicas permissivas sobre documentação, versões e catálogos estáticos, verificando termos e robots, baixando de forma identificável e pouco frequente com cache; **nenhum** scraping de conta/VIP, interceptação de comunicação, emulação de login, reversão de protocolo, paywall, token, criptografia ou WAF. Não transformar APK/iOS em uma falsa fonte de dados ao vivo.
11. Comparar o que a interface atual do PGBR já tem. Reusar radar/filtros/mapa/teleporte/cooldown em vez de reescrever. Compatibilidade Android ARM64 Pokémon GO 0.437.0, Root e sem Root, regressões com batalha/VGP/GPS. Nenhum binário do iPogo no APK PGBR; código próprio com testes e imagens licenciadas.
12. Entregáveis: mapa de capacidades verificado, importador local quando aplicável, radar/listas/perfis/alertas funcionais com fontes existentes, contratos API OpenAPI 3.1, gateway e receptor webhook do PGBR (se backend disponível), testes e fixtures sintéticas, PR independente, adendo Mestre, CI/Android QA. Diferenciar claramente parte implementada de integração remota pendente.

CRITÉRIO: iPogo é REFERÊNCIA DE FUNCIONALIDADES até ser comprovada uma fonte pública/permitida; as funções independentes do PGBR e o gateway próprio podem ser implementados sem aguardar uma API do fabricante.

============================================================
REGRAS GERAIS DO DESENVOLVIMENTO — PGBR
============================================================
- Repositório: https://github.com/LucianoToscano/PGBR
- Consultar `PGBR_ARQUIVO_MESTRE_UNICO.md` INTEGRAL, AGENTS.md, THIRD_PARTY.md, docs/BUILDING.md, docs/ARCHITECTURE.md, docs/ROOTED.md, docs de fontes, branches, PRs abertas, HEAD real, CI e release antes de qualquer modificação.
- Estado verificado em 23/11/2026: README relata LT043 QA publicada, com importação local de dados PGSharp e integração parcial de Rocket/Max/HUD; Snipers/feeds de PRs paralelas NÃO são automaticamente incorporados ao APK LT043. Conferir novamente HEAD e PRs atuais antes de codificar.
- Protocolo GLOBAL SEM PERDAS: branch independente, commit-base, não sobrescrever PR paralela, registrar diffs/testes, PR revisável, não fazer merge/deploy/release em `main` automaticamente, rollback e atualização ADITIVA do arquivo Mestre. Não modificar o repositório PokeSearch sem tarefa separada.
- Não perguntar confirmações repetidas para trabalho no próprio código PGBR ou decisões reversíveis. Não presumir que consentimento do proprietário PGBR conceda autorização de backend/licença VIP/copyright de terceiros. Não copiar credenciais/segredos, contornar autenticação, rate limits, 401/403/429, captcha, assinatura, criptografia nem extrair feeds protegidos de APK. Usar material binário de terceiros apenas como pesquisa/compatibilidade onde permitido.
- Fontes existentes precisam ser consideradas primeiro: FeedChooser, LiveFeedDialog, LiveFeedData, LiveFeedFilters, LiveFeedSession, PogoFeedDialog/Client/Data, IFlowGoFeedClient/Data/Policy, PokeListFeedClient/Data, HundoFeedDialog, ShundoSniper, PgSharpFeedPolicy, PgSharpFeedDialog, PgSharpLicenseVault, PgSharpDataImport, AccountProfiles, SettingsBackup, CoordinateClipboard. Verificar nomes na HEAD atual, não criar duplicatas.
- Testar local/com fixtures e simulação controlada, depois integração real somente se disponível; verificar 401/403/429, Retry-After, cancelamento, TTL, timezone, validação de coordenadas, expiração, modo global/paginação, isolamento de contas, tolerância a falhas, memória e regressão Root/sem Root/Zygisk. Separar `implemented`, `test_passed`, `device_tested`, `provider_connected`, `staging_deployed` e `release_published`; não converter um no outro.
- Filtros e importações são funcionalidades independentes da ativação de feeds externos. Entregar o máximo de código útil que funcione offline/localmente sem fabricar resultados de spawns, IV exatos ou shiny de conta.
- Se build for gerada, entregar link DIRETO de APK verificável, SHA-256 e assinatura, não somente página de workflow. No app usar português do Brasil, especialmente 'Teleporte'.
~~~~

---

## <a id="secao-06"></a>Seção 06 — Contrato OpenAPI proposto

**Origem integral:** `06_OPENAPI_PROPOSTA_GATEWAY_PGBR_5_FONTES.yaml`  
**SHA-256 do texto integrado:** `789e7b6da6fdc381166fb2aa9431c0f22283254661e06dfc7789a5a691cda147`  
**Tamanho:** 5921 bytes

~~~~yaml
openapi: 3.1.0
info:
  title: PGBR Data Gateway (contrato proposto; ainda não implementado)
  version: 0.1.0-proposal
  description: >-
    API própria para normalização, importação e distribuição de eventos permitidos.
    Nenhum endpoint deste documento pertence a PGSharp, iPogo, PokeList,
    iFlowGo ou PoGoCoordinates. Não indica que haja feed real.
servers:
  - url: http://localhost:8080
    description: EXEMPLO LOCAL; não é um serviço implantado
security:
  - gatewayBearer: []
paths:
  /v1/health:
    get:
      security: []
      summary: Saúde do nosso gateway
      responses:
        '200': {description: Disponível}
  /v1/sources:
    get:
      summary: Lista de fontes e estados de conexão reais
      responses:
        '200':
          description: Estados e origens
          content:
            application/json:
              schema: {type: array, items: {$ref: '#/components/schemas/Source'}}
  /v1/capabilities:
    get:
      summary: Filtros, planos, acesso, cobertura e capacidades comprovados
      parameters:
        - {name: source, in: query, required: false, schema: {type: string}}
      responses:
        '200': {description: Matriz de capacidades}
  /v1/search:
    post:
      summary: Busca sobre fontes e eventos permitidos e disponíveis
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/SearchRequest'}
      responses:
        '200':
          description: Resultados reais permitidos (vazio quando ausentes)
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SearchResponse'}
        '403': {description: Sem permissão de fonte ou dados}
        '429': {description: Limite respeitado; retornar Retry-After}
  /v1/imports/pgsharp:
    post:
      summary: Importação de configuração pessoal PGSharp convertida e validada
      description: Não é API de busca do fornecedor nem extrai conteúdo de terceiros.
      responses:
        '202': {description: Importação aceita}
        '422': {description: Arquivo inválido}
  /v1/imports/ipogo:
    post:
      summary: Importação de exportação pessoal válida do usuário (quando existir)
      responses:
        '202': {description: Importação aceita}
        '422': {description: Formato não suportado}
  /v1/events:
    get:
      summary: Eventos observados no nosso gateway com cursor e TTL
      responses:
        '200': {description: Página autorizada}
  /v1/webhooks/subscriptions:
    get:
      summary: Listar assinaturas pertencentes ao usuário
      responses:
        '200': {description: Assinaturas}
    post:
      summary: Criar assinatura com URL HTTPS validada e filtros permitidos
      responses:
        '201': {description: Assinatura criada}
        '422': {description: Destino inseguro ou filtros inválidos}
  /v1/webhooks/subscriptions/{subscriptionId}:
    delete:
      summary: Revogar assinatura
      parameters:
        - {name: subscriptionId, in: path, required: true, schema: {type: string}}
      responses:
        '204': {description: Revogada}
components:
  securitySchemes:
    gatewayBearer:
      type: http
      scheme: bearer
      bearerFormat: JWT
  schemas:
    Source:
      type: object
      required: [source_id, status, origin_kind]
      properties:
        source_id: {type: string}
        status: {type: string, enum: [connected, unverified, unavailable, blocked, local_only]}
        origin_kind: {type: string, enum: [live_external, personal_import, static_catalog, demo_fixture]}
        supports_webhook: {type: boolean}
        live_feed_available: {type: boolean}
        last_verified_at: {type: [string, 'null'], format: date-time}
        attribution: {type: [string, 'null']}
    SearchRequest:
      type: object
      required: [sources]
      properties:
        sources: {type: array, items: {type: string}, minItems: 1}
        kind: {type: string, enum: [pokemon, raid, quest, rocket, powerspot]}
        pokemon_ids: {type: array, items: {type: integer, minimum: 1}}
        iv_min: {type: [number, 'null'], minimum: 0, maximum: 100}
        iv_max: {type: [number, 'null'], minimum: 0, maximum: 100}
        latitude: {type: [number, 'null'], minimum: -90, maximum: 90}
        longitude: {type: [number, 'null'], minimum: -180, maximum: 180}
        radius_km: {type: [number, 'null'], minimum: 0}
        cursor: {type: [string, 'null']}
        limit: {type: integer, minimum: 1, maximum: 100, default: 25}
    SearchResponse:
      type: object
      required: [items, has_more, retrieved_at, source_status]
      properties:
        items: {type: array, items: {$ref: '#/components/schemas/Event'}}
        has_more: {type: boolean}
        next_cursor: {type: [string, 'null']}
        retrieved_at: {type: string, format: date-time}
        source_status: {type: array, items: {$ref: '#/components/schemas/Source'}}
    Event:
      type: object
      required: [source_id, event_id, kind, origin_kind, coordinate_precision, is_demo, observed_at, retrieved_at]
      properties:
        source_id: {type: string}
        event_id: {type: string}
        kind: {type: string}
        origin_kind: {type: string, enum: [live_external, personal_import, static_catalog, demo_fixture]}
        pokemon_id: {type: [integer, 'null']}
        latitude: {type: [number, 'null']}
        longitude: {type: [number, 'null']}
        coordinate_precision: {type: string, enum: [exact, approximate, unavailable]}
        iv_percent: {type: [number, 'null'], minimum: 0, maximum: 100}
        observed_at: {type: string, format: date-time}
        retrieved_at: {type: string, format: date-time}
        expires_at: {type: [string, 'null'], format: date-time}
        is_demo: {type: boolean}
        source_url: {type: [string, 'null']}
        data_quality: {type: [string, 'null']}
~~~~

---

