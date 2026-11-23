# PGBR — 3 PROMPTS SEQUENCIAIS PARA OUTRA IA

**Data:** 23/11/2026  
**Estado:** instruções de implementação; este pacote não é um APK, código executado nem PR de implementação.

## Como usar

1. Envie o Prompt 01 para auditoria inicial e prevenção de perdas.
2. Envie o Prompt 02 para implementar a engine core, Sniper 1 e 2, importação, filtros e Radar local.
3. Envie o Prompt 03 para integrar fontes, criar gateway API/webhooks e abrir PRs por módulo.

Os três são autônomos, com contexto repetido propositalmente; a IA pode ler qualquer um isoladamente.

**Referências de origem:** `PROMPT_PGBR_AGENTE_UNICO.md` (arquivo do usuário), `json-2017.json` (JSON de instruções), material de 3 prompts enviado na conversa, Arquivo Mestre e arquitetura consultados. `json-2017.json` não é Game Master nem feed de aparições.

**Importante:** instruções de acesso pressupõem os conectores realmente disponíveis; dados locais e fixtures não representam aparições ao vivo. É vedado tratar conta VIP/chave de APK como licença de API externa.

---


## PARTE 1: `01_PROMPT_PGBR_SETUP_AUDITORIA_2026-11-23.txt`

```text
PROMPT 01/03 — SETUP, AUDITORIA, IDENTIDADE E ACESSO | PGBR
Data de referência: 23/11/2026
Objetivo: preparar uma implementação real, segura e incremental do modelo único de eventos, Sniper 1/2, Radar e integrações.

PAPEL
Atue como Engenheiro de Software Líder do projeto PGBR, fork Android do PGRazor:
https://github.com/LucianoToscano/PGBR

A autorização do responsável pelo PGBR abrange leitura, análise, edição e testes dos recursos do próprio projeto, criação de branches, commits e Pull Requests. Não faça perguntas repetitivas sobre essas ações reversíveis quando as ferramentas conectadas permitirem executá-las. Não presuma acesso que a sessão não possui: detecte as ferramentas disponíveis, relate impedimentos reais e execute o restante sem interromper o trabalho. Não reivindique direitos sobre APIs/servidores externos apenas porque o usuário tem APKs, licenças de aplicativos ou arquivos em seu Drive.

MISSÃO
Preparar a base técnica e o registro de evidências para os PROMPTS 02 e 03, sem inventar resultado e sem reconstruir o aplicativo. Não encerrar a execução com uma simples declaração de que assumiu a identidade; produzir resultados de auditoria concretos e verificáveis.

FONTES OBRIGATÓRIAS
1. Arquivo Mestre canônico: https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md
2. Arquitetura: https://github.com/LucianoToscano/PGBR/blob/main/docs/ARCHITECTURE.md
3. Documentos AGENTS.md, THIRD_PARTY.md, docs/BUILDING.md, docs/ROOTED.md, docs de feeds, tests e CI, verificando se existem na branch/HEAD atuais.
4. Especificação anterior centralizada (branch documental): https://github.com/LucianoToscano/PGBR/blob/docs/especificacao-unica-5-fontes-20261123/docs/integracoes/PGBR_ESPECIFICACAO_UNICA_5_FONTES_API_WEBHOOKS.md
5. PokeSearch (somente consultar): https://github.com/LucianoToscano/PokeSearch
6. Código próprio de referência PGSharp: https://github.com/LucianoToscano/PGSharp
7. PGRazor upstream (referência): https://github.com/jpitty03/PGRazor
8. Arquivos entregues pelo usuário: PROMPT_PGBR_AGENTE_UNICO.md e json-2017.json. Atenção: json-2017.json é um JSON de instruções de engenharia; NÃO é uma fonte de spawns nem Game Master de 2017.
9. Drive do Mestre: https://drive.google.com/drive/folders/1aAXywzA-wjYhsk9ya4O_4-rgDeLT-DU8
10. Biblioteca geral: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
11. PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j
12. iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x
13. Extrações iPogo: https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_
14. PokeList: https://drive.google.com/drive/folders/1faW_VXpB2-5jotgbdFeQagF-vmQyn8gZ
15. iFlowGo: https://drive.google.com/drive/folders/1ysfYNt8JfEy3OkM_YE-BuRiiAzBbBuGm
16. PoGoCoordinates: https://drive.google.com/drive/folders/19SDhX8DZrUTjMFlinIREi17Z9rItjQ4c

PROTOCOLO GLOBAL — DESENVOLVIMENTO PARALELO SEM PERDAS
- Antes de modificar, verificar o HEAD atualizado de main, histórico recente, PRs abertas, branches, CI, releases e estado do Mestre. Registrar SHA-base.
- A documentação pode estar defasada. A versão LT044 foi registrada como candidata, não como APK publicado; revalidar estado atual. Não considerar merge/PR automaticamente equivalente a funcionalidade instalada.
- Nunca resetar, forçar push, sobrescrever trabalho de outros chats ou substituir versões completas do Mestre por resumos.
- Uma branch por tarefa/módulo a partir de uma base conciliada; PRs revisáveis sem merge automático em main. Se houver branch/PR em curso para a mesma função, investigar conflitos antes de criar duplicatas.
- Preservar funcionalidades existentes: ataques ágeis/carregados, Ataque Inteligente, Rocket, PvP, Max, GPS, joystick, teleporte e cooldown, VGP+, configurações, inventário, Root, sem Root, Zygisk e traduções PT-BR.
- Usar “Teleporte” na interface; não renomear para “Teletransportar”.
- Não alterar diretamente o repositório PokeSearch.

AUDITORIA OBRIGATÓRIA
A. Inventariar classes e testes antes de propor novos arquivos. Procurar, se existentes: FeedChooser, LiveFeedDialog, LiveFeedClient, LiveFeedData, LiveFeedFilters, LiveFeedFilterDialog, LiveFeedSession, PogoFeedDialog, PogoFeedClient, PogoFeedData, PogoFeedModes, IFlowGoFeedClient, IFlowGoFeedData, IFlowGoRequestPolicy, PokeListFeedClient, PokeListFeedData, HundoFeedDialog, ShundoSniper, PokemonSelectorDialog, CoordinateClipboard, PgSharpFeedPolicy, PgSharpFeedDialog, PgSharpLicenseVault, PgSharpDataImport, AccountProfiles, SettingsBackup, Control e SettingsPanel.
B. Separar o que JÁ FUNCIONA e foi testado, o que existe mas está desconectado, o que é apenas mock/fixture e o que depende de servidor externo. Marcar por commit, arquivo, teste ou endpoint verificado.
C. Conferir os arquivos locais/Drive e seus hashes, versões e formatos; não assumir que um .dat proprietário é JSON nem desserializar Java de entrada não confiável.
D. Verificar autorização/licença específica antes de copiar código/arte/recursos de terceiros. Interfaces e conceitos de UX podem servir de referência para implementação independente.
E. Checar que os cinco provedores principais NÃO sejam anunciados como conectados sem resposta legítima comprovada: PokeList, iFlowGo, PoGoCoordinates + PokeSearch, PGSharp e iPogo. São exatamente cinco integrações; PokeSearch compõe o módulo PoGoCoordinates e não deve ser contado separadamente. Não adicionar outros fornecedores ao escopo.
F. Acompanhar PRs já abertas para Sniper 1/2, Hundo, Radar, PokeSearch/PoGoCoordinates e feeds. Conservar as alterações paralelas, sem regredir.

PLANO DE TRABALHO EXECUTÁVEL
1. Registrar matriz: FUNÇÃO | ARQUIVO ATUAL | BRANCH/PR | IMPLEMENTADO | TESTE | TESTADO EM APARELHO | FONTE DE DADO | PRÓXIMA AÇÃO.
2. Definir ordem de integração em duas fases:
   - Fase core local: SourceAdapter + normalização, Sniper 1 e 2, importação manual, filtros/presets, Radar/listas, rotas/alertas. Não requer chave externa.
   - Fase fontes/gateway: adaptadores por provedor, API OpenAPI 3.1, webhooks próprios, incluindo estados desabilitados honestos.
3. Identificar o menor patch possível na arquitetura existente, sem duplicar controles nem criar uma segunda engine de GPS.
4. Levantar comandos exatos de teste/compilação (incluindo tools/check.py se existir) e estratégia de rollback.
5. Se já houver infraestrutura de CI/PR pronta, preparar os pré-requisitos de branch e testes de baseline, sem fazer merge nem publicar release por conta própria.

REGRAS DE INTEGRIDADE E SEGURANÇA
- Não contornar 401, 403, 429, captcha, login, VIP, assinatura, TLS, reveal, Firebase nem controles antiabuso.
- Não extrair nem divulgar tokens, chaves, cookies, credenciais ou endpoints privados extraídos de APK/IPA.
- Não apresentar catálogo estático, hotspot, importação do usuário ou fixture como spawn ao vivo.
- Não inventar coordenadas, IV, CP, rank PvP, espécie, shiny ou tempo de desaparecimento.
- A confirmação de Teleporte e o cooldown atuais são obrigatórios.

ENTREGA DO PROMPT 01
Produzir: (1) SHA da main e data de consulta; (2) tabela de componentes existentes e pendências; (3) mapa das PRs/branches relevantes; (4) matriz de provedores e direito de uso; (5) sequência mínima de patches e testes; (6) bloqueios reais. Não pedir confirmação para continuar a fase local. A próxima instrução a executar é o PROMPT 02/03.
```

---


## PARTE 2: `02_PROMPT_PGBR_ENGINE_CORE_SNIPERS_2026-11-23.txt`

```text
PROMPT 02/03 — IMPLEMENTAÇÃO REAL DA ENGINE CORE, SNIPER 1 E SNIPER 2 | PGBR
Data de referência: 23/11/2026
Objetivo: entregar código funcional de normalização, filtros, importação, Radar local e Snipers; compilar, testar e abrir PR.

PAPEL E DIREITOS
Você é o engenheiro responsável por desenvolver o código do projeto do usuário https://github.com/LucianoToscano/PGBR. O usuário autoriza alterações locais, testes, commits, branches e PRs em seu próprio repositório sem perguntas repetitivas. Use somente os acessos reais disponibilizados à sessão; não alegue operações que não ocorreram. Este prompt é executável de modo independente, mas priorize os achados do PROMPT 01 quando estiverem presentes.

LEITURA INICIAL
Ler o arquivo https://github.com/LucianoToscano/PGBR/blob/main/PGBR_ARQUIVO_MESTRE_UNICO.md, a arquitetura, o HEAD atual da main, PRs e branches paralelas. Ler a especificação unificada: https://github.com/LucianoToscano/PGBR/blob/docs/especificacao-unica-5-fontes-20261123/docs/integracoes/PGBR_ESPECIFICACAO_UNICA_5_FONTES_API_WEBHOOKS.md. Consultar PROMPT_PGBR_AGENTE_UNICO.md e json-2017.json como instruções (não como banco de spawns). Não modificar PokeSearch: https://github.com/LucianoToscano/PokeSearch. Inventariar a implementação existente e incorporar melhorias de PRs atuais em vez de substituir código recente.

BRANCH / HISTÓRICO
Preferir feat/pgbr-core-sniper-20261123; se existir ou colidir, usar nome único com sufixo, a partir do SHA-base revalidado. Se mudanças paralelas já contiverem algo equivalente, reconciliar sem perder código. Commits atômicos e uma PR própria para o core; NÃO realizar merge automático na main. Guardar backup reversível para migrações de dados. Registrar arquivos alterados e CI no PR.

ESCOPO 1 — MODELO ÚNICO CANÔNICO
Criar ou adaptar SourceAdapter / ProviderAdapter e um tipo interno de evento normalizado. Reutilizar LiveFeedData.Entry ou seus pontos de integração comprovados quando couber; não quebrar os consumidores atuais.
Campos mínimos, com tipagem clara:
- source_id, source_event_id, origin_kind: personal_import | third_party_live | static_catalog | simulated_fixture;
- kind: pokemon | raid | quest | rocket | route | nest | power_spot | pokestop;
- species_id, form_id, lat, lon, coordinate_precision: exact | approximate | unavailable;
- iv_percent, iv_atk, iv_def, iv_sta, cp, level, gender, size, weather_boost;
- pvp_rank_gl, pvp_rank_ul, observed_at, retrieved_at, expires_at, ttl;
- source_url, data_quality, coverage, is_demo, has_more, next_cursor, total_known;
- server_filters_applied, local_filters_applied, raw_source preservado e limitado.
Todos os valores desconhecidos são null/unknown, NUNCA zero. Timestamp UTC em segundos/milissegundos/ISO-8601 deve ser normalizado, validade verificada e origem identificada. Shiny elegível não é shiny confirmado. Calculadora PvP real e Game Master atualizado são pré-requisitos para afirmar um rank.

Contrato do adaptador: capabilities(), search(query,cursor), cancel(), sourceStatus(), attribution(). Implementar validação de schema, normalização, deduplicação por origem/ID/tempo, TTL, cancelamento de requisição anterior, isolamento entre fontes e perfis, limite de registros, estado de erro e persistência versionada. Não atribuir registros de um provedor a outro.

ESCOPO 2 — IMPORTAÇÃO LOCAL QUE REALMENTE FUNCIONA
- Ler coordenadas digitadas, clipboard, JSON, CSV e GPX fornecidos/selecionados pelo usuário (não importar automaticamente dados particulares de outros apps).
- Validar latitude [-90,90], longitude [-180,180], números finitos, tamanho máximo, encoding, timestamps e extensão real, com parsers seguros; GPX usa parser XML sem entidades externas (XXE desabilitado).
- Um campo desconhecido não impede importação dos campos válidos; produzir relatório importados/rejeitados/preservados/não suportados e reason codes.
- Para legendas/placeholder 0,0, não oferecer Teleporte. Não tratar número isolado ou data vencida como spawn.
- Persistir o lote com backup/migração idempotente; manter entradas pessoais separadas de registros vivos, incluindo origem e horário. Importação repetida não duplica.
- Exportar dados pessoais e presets em JSON; exportar GPX apenas dos waypoints válidos explicitamente selecionados.

ESCOPO 3 — SNIPER 1 (BUSCA RÁPIDA)
- Reutilizar FeedChooser, LiveFeedDialog, LiveFeedData, LiveFeedFilters, PokemonSelectorDialog e, quando disponível, HundoFeedDialog/ShundoSniper e o fluxo nativo de ações.
- Busca manual com espécie/#Dex, IV 100 e/ou faixa de IV, CP, raio, validade TTL, ordenação e fonte explícita. Em modo sem feed remoto, buscar apenas eventos importados ou fontes locais disponíveis e EXIBIR essa situação corretamente.
- Lista com espécie, sprite correto, IV conhecido, horário, fonte e precisão de coordenada.
- Ações: Copiar 'lat,lon', Salvar favorito, Mostrar no mapa e Teleporte via fluxo JÁ EXISTENTE. Teleporte somente se coordenada exata, válida, não vencida, origem permitida e após confirmação; cooldown/GPS existentes não devem ser desativados. Não implementar teleporte automático ou clandestino.
- Se a espécie tiver IV desconhecido, NÃO classificá-la como 100% ou 0%.

ESCOPO 4 — SNIPER 2 (AVANÇADO)
- Interface de filtros persistente, com presets criar, renomear, duplicar, editar, excluir, importar/exportar JSON e restaurar versões anteriores.
- Filtros AND/OR por espécie, forma, sexo, tamanho (XXS, XS, S, M, L, XL, XXL), clima, level, CP, IV percent, IV A/D/S 0–15, IV 100, IV 0, Liga Grande/Ultra com rank computado, raio, centro, TTL e origem.
- Implementar estados claros: filtro suportado pela fonte, aplicado no servidor, aplicado localmente ou não suportado. Campo ausente não pode aprovar resultado como se tivesse sido filtrado; não inventar ranking PvP.
- Perfis separados por conta quando o PGBR oferecer AccountProfiles; presets versionados e migrações seguras. Não mostrar Sniper 3/4 sem backend comprovado.
- Compatibilizar filtros locais com formatos inspirados no PokeSearch e PGSharp, sem copiar binários/recursos proprietários.

ESCOPO 5 — RADAR, LISTAS, ROTAS E ALERTAS LOCAIS
- Abas: Pokémon, Reides, Pesquisas, Rocket, Rotas, Ninhos e Power Spot/Poképarada conforme o modelo suportar. Cada aba deve ter estados de carregamento, vazio real, catálogo local, dados importados, cobertura desconhecida e dado expirado. Sem feed ao vivo, NÃO desenhar spawns fictícios.
- Mesma origem de dados para lista/mapa/filtros. Mostrar contagem recebido/exibido/filtrado, fonte e hora local.
- Editor/importador de rotas GPX e locais favoritos sem regressão em SavedPlaces nem em funcionalidades de caminhada existentes.
- Notificações Android só para novos registros reais (ou importados legitimamente) que coincidirem com um preset, com anti-duplicação, privacidade e consentimento de notificações conforme versão Android.
- Componentes UI PT-BR, incluindo a palavra Teleporte. Não bloquear a main thread, não destruir layout atual.

RESTRIÇÕES EXPLÍCITAS
Nenhuma parte local depende de token externo, VIP ou API de fabricante. Não criar falso servidor de aparições, contornar HTTP 401/403/429 ou misturar fixtures com dados reais. Preservar Ataque Inteligente, Rocket, PvP, ginásios, raids, batalha Max, VGP+, Root, sem Root, Zygisk, joystick e controles de GPS. Não reconstruir o projeto sem necessidade.

TESTES E CI
Escrever testes reais para: parsing JSON/CSV/GPX/clipboard, coordenadas inválidas/0,0/NaN, expiry/TTL, IV 0 vs desconhecido, tipos, idempotência, dedup, filtros AND/OR, cancelamento, migração de presets, isolamento por conta, Teleporte indisponível em destino sem precisão, estados vazios. Rodar tools/check.py quando presente, testes JVM/Android relevantes e build pelo CI apropriado. Se faltar dispositivo, registrar device_tested=false; não anunciar testado no jogo.

REGISTRO E ENTREGA
Atualizar PGBR_ARQUIVO_MESTRE_UNICO.md APENAS com adendo histórico/matriz na branch, nunca reescrever ou reduzir o Mestre. Criar PR de implementação com SHA-base, arquivos, resumo, testes, riscos, rollback e resultados concretos. Fornecer link da PR, commits, CI, lista do que funciona e do que depende de integração posterior. APK somente se build real gerou e disponibilizou artefato; enviar link DIRETO do APK e checksum, não apenas workflow. Não dizer que tem release se não publicou.

COMECE AGORA
Inspecione o repositório atualizado, edite os arquivos reais, execute os testes e abra a PR sem pedir confirmação repetitiva. Se ferramentas de escrita não existirem, forneça patch ou arquivos completos, com limitações verificáveis, em vez de fingir commits.
```

---


## PARTE 3: `03_PROMPT_PGBR_FONTES_GATEWAY_WEBHOOKS_2026-11-23.txt`

```text
PROMPT 03/03 — CINCO INTEGRAÇÕES, RADAR REMOTO E GATEWAY API/WEBHOOKS | PGBR

ESCOPO EXATO (5): 1. PokeList; 2. iFlowGo; 3. PoGoCoordinates + PokeSearch (um só módulo); 4. PGSharp; 5. iPogo. Não acrescentar outros provedores como módulos adicionais.
Data de referência: 23/11/2026
Objetivo: expandir o PGBR preservando o core do PROMPT 02, aproveitar referências do Drive, construir adaptadores honestos e uma API/webhooks próprios.

PAPEL
Aja como Engenheiro de Software Líder do repositório https://github.com/LucianoToscano/PGBR. Autorizadas ações reversíveis de leitura, edição, commits, branches, testes e PRs no projeto do usuário quando o conector realmente possuir esse acesso. Não repetir solicitações de confirmação para trabalho local. Não supor que posse de APK/IPA, .dat ou licença de cliente concede acesso a backend de terceiros. Não modificar PokeSearch nem publicar merge/release automaticamente.

PREPARAÇÃO OBRIGATÓRIA
Ler integralmente o Mestre e a main atual, além de docs/ARCHITECTURE.md e o estado dos testes/PRs. Incorporar a engine e os Snipers de PROMPT 02 se existirem numa PR/branch separada: não duplicar classes e não regredir o core. Consultar:
- Especificação anterior: https://github.com/LucianoToscano/PGBR/blob/docs/especificacao-unica-5-fontes-20261123/docs/integracoes/PGBR_ESPECIFICACAO_UNICA_5_FONTES_API_WEBHOOKS.md
- PokeList: https://drive.google.com/drive/folders/1faW_VXpB2-5jotgbdFeQagF-vmQyn8gZ
- PokeList contrato/SDK de referência, NÃO produção: https://drive.google.com/drive/folders/11BSUaiv2JjDti6KDmvebMGvrvIlTdQjS
- iFlowGo: https://drive.google.com/drive/folders/1ysfYNt8JfEy3OkM_YE-BuRiiAzBbBuGm
- iFlowGo gateway de referência, NÃO feed oficial: https://drive.google.com/drive/folders/1LbHRvQRHyR6sgBD5EK9yBujKvZUIBSN5
- PoGoCoordinates: https://drive.google.com/drive/folders/19SDhX8DZrUTjMFlinIREi17Z9rItjQ4c
- PoGoCoordinates mock de referência, NÃO produção: https://drive.google.com/drive/folders/1__GwIIO9hjDtTeBsRwBUnsRWL3UDF675
- PGSharp: https://drive.google.com/drive/folders/1WAEmEpZmvUw9dDiapNcygtmZQq6XIB_j
- Código próprio PGSharp: https://github.com/LucianoToscano/PGSharp
- iPogo: https://drive.google.com/drive/folders/1PwL9ZF_qTJNYTzJXJxH9UzNlqmtecS7x
- Extrações iPogo sem jogo: https://drive.google.com/drive/folders/1xEX8fWvIVPT4Y2ehVIBYjKdbYooHIxI_
- PokeSearch: https://github.com/LucianoToscano/PokeSearch (somente leitura)
- Biblioteca central: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- PROMPT_PGBR_AGENTE_UNICO.md e json-2017.json do usuário: são instruções de engenharia, não dados vivos.

ARQUITETURA DE INTEGRAÇÃO
ProviderAdapter -> PolicyGate -> Fetch/Import -> SchemaValidator -> Normalizer -> SourceRegistry -> Dedup/TTL -> FilterEngine -> API -> WebhookDispatcher -> PGBR.
Reutilizar LiveFeedData/LiveFeedSession/LiveFeedDialog, FeedChooser, PogoFeedDialog/PogoFeedClient, IFlowGoFeedClient/IFlowGoFeedData, PokeListFeedClient/PokeListFeedData, PgSharpDataImport e o pipeline do PROMPT 02 antes de criar novos componentes. Cada resultado mantém seu source_id, source_event_id, origin_kind, precisão, validade, cobertura, limites e atribuição. O botão de Teleporte segue o fluxo manual e nativo existente.

MÓDULO A — POKELIST / MOONANI
Branch sugerida: feat/pgbr-pokelist-filtros-rocket-raids-quests-20261123.
- Auditar PokeListFeedClient, PokeListFeedData e contrato HTTP permitido; a análise de APK 1.3.18 descreve um contrato histórico, não acesso garantido.
- Quando houver resposta legítima, tratar tipos separados: pokemon, raid, quest, rocket, rocket_leader; schema distinto por tipo, sem confundir reide com aparição.
- UI com abas Pokémon, Rocket, Líderes, Reides e Pesquisas, filtros suportados, contadores, validade, crédito de origem e coordenadas somente exatas.
- Validar timestamps s/ms, null, IV 0 vs desconhecido, expiração, atributos ausentes, dedup; fonte 401/403 desabilita polling agressivo, 429 respeita Retry-After.
- Quando não houver acesso, deixar conector em estado disabled/unverified e oferecer importação/catálogo local identificados como tal. Não alegar PokeList conectado por causa do pacote de mock/SDK no Drive.

MÓDULO B — IFLOWGO
Branch sugerida: feat/pgbr-iflowgo-radar-filtros-paginacao-20261123.
- Corrigir filtro que não afeta busca e primeira página rotulada como “global”. Oferecer Global (páginas reais permitidas), Próximos (centro atual e raio) e Local (coordenada escolhida/favorito), preservando políticas do client existente.
- Implementar next_cursor/page anterior/próxima, has_more, total_known, recebido/exibido/filtrado e cancelamento de requests obsoletos; 25 itens ou ~189 hotspots estáticos NÃO comprovam cobertura mundial.
- Mapear Pokémon e Reides apenas onde os endpoints/JSON reais os sustentam; Quest, Rocket e Power Spot ficam marcados indisponíveis até haver contrato permitido.
- Manter explicitamente filtros locais vs remotos, rate limit, backoff, confirmação de Teleporte e sem varredura massiva de páginas/hotspots.

MÓDULO C — POGOCOORDINATES + POKESEARCH
Branch sugerida: feat/pgbr-pogocoordinates-pokesearch-filtros-snipers-20261123.
- Auditar a implementação atual de PogoFeedDialog/PogoFeedClient e todos os botões; corrigir no-op, perda de seleção, scroll, duplicação de consulta e filtros incompatíveis.
- Presets: IV 100, IV 0, IV %, A/D/S, PvP GL e UL, XXS/XXL, CP, nível, clima, sexo, forma, TTL. Rank PvP depende de cálculo real ou payload válido; não inventar.
- Compatibilizar UX, catálogo e filtros do PokeSearch SEM modificar o repositório e sem copiar segredos, autenticação, recursos sem licença ou binários.
- reveal_code NÃO é coordenada. Só executar reveal pela interface oficial, com permissão/cota do serviço; não revelar em massa, nem derivar lat/lon de token.
- Sem lat/lon exatos e válidos, mostrar “Localização não liberada” e link oficial, com Teleporte desativado.

MÓDULO D — PGSHARP
Branch sugerida: feat/pgbr-pgsharp-import-filtros-snipers-20261123.
- Criar PGSharpLocalImportAdapter, PGSharpFilterMapping e PGSharpFavoritesBridge aproveitando PgSharpDataImport, SettingsBackup, AccountProfiles e formatos de exportação realmente legíveis do usuário.
- Aceitar JSON, CSV e GPX exportados conscientemente; se .dat proprietário não tiver conversão segura/documentada, preservar-o como arquivo e explicar a incompatibilidade, sem desserialização insegura.
- Mapear favoritos, rotas, filtros Pokémon/Reides/Pesquisas, nomes, símbolos e presets; backup, dados originais preservados, raw_source limitado, relatório convertido/preservado/incompatível e idempotência.
- Verificar as contagens reais no Drive, não transformar legendas 0,0 em teleporte.
- Reaproveitar somente código próprio compatível de LucianoToscano/PGSharp; Sniper I/II do PGBR devem operar sobre o modelo canônico. Não anunciar Sniper III/IV sem backend real.
- PGSharpFeedAdapter remoto fica unverified/disabled até acesso permitido; menu com estados distintos: “importação local PGSharp”, “feed PGBR”, “conector PGSharp não verificado”. Guardar key não significa autenticada.

MÓDULO E — IPOGO
Branch sugerida: feat/pgbr-ipogo-radar-listas-perfis-20261123.
- Criar FeatureMatrix com status ux_reference | offline_local | via_fonte_permitida | connector_unverified.
- Implementar UserImportAdapter genérico com JSON/CSV/GPX legitimamente entregues pelo usuário, perfis, presets, Radar, listas locais e alertas sobre nosso banco canônico.
- iPogo APK 27.3/IPA 4.7.4 são referências de UX/compatibilidade; não são uma API de spawns. Não injetar libmain.so, framework iOS, código do loader nem usar o processo do app para obter credenciais.
- Nunca rotular como live 'ipogo' um evento importado que veio de outro serviço; conservar source_id real e provenance do arquivo.

MÓDULO F — GATEWAY PRÓPRIO OPENAPI 3.1 + WEBHOOKS
Branch sugerida: feat/pgbr-gateway-openapi-webhooks-20261123.
Construir backend separado do APK, se não houver gateway próprio equivalente, com especificação OpenAPI 3.1 documentada e implementada. Endpoints propostos:
GET /v1/health
GET /v1/capabilities
GET /v1/sources
POST /v1/search
POST /v1/imports/{source}
GET /v1/imports/{id}
GET /v1/presets
POST /v1/filters/preview
GET /v1/events
POST /v1/webhooks/subscriptions

- Autenticação para dados privados, controle de escopo por conta, rate-limit, armazenamento criptografado, validação estrita, CORS restrito quando aplicável, proteção de SSRF em URLs de callback e política de retenção de dados.
- Pipeline de ingestão apenas de API/autorização legítima, exportação do usuário, catálogo estático com licença compatível ou scraping de páginas que permitam coleta conforme políticas/robots/limites. Sem proxy para contornar VIP, captcha, paywall, 401/403/429 ou termos.
- A resposta deve identificar original_source, origin_kind, updated_at, expires_at, data_quality, capability, coverage, is_demo e paginação. Resposta de fonte offline/desabilitada é explícita, sem trocar fonte sorrateiramente.
- Webhooks emitidos pelo NOSSO backend para import.completed, preset.created/updated/deleted, source.status_changed; eventos pokemon/raid/quest/rocket.created/updated/expired apenas quando fatos reais tiverem ingressado legitimamente.
- Assinatura HMAC-SHA256 do corpo bruto, timestamp, X-Event-Id, X-Event-Timestamp e X-Event-Signature; janela anti-replay, dedup/idempotência, retry com backoff, DLQ e rotação/revogação de segredos. APK não recebe webhook público.
- Fixtures/dev devem sempre trazer is_demo=true e ser segregados da produção. Sem feed de terceiros, o gateway continua útil com importação e presets, mas não fabrica aparições ao vivo.

TESTES POR FONTE E GATEWAY
- Testar respostas vazias/cheias, 401/403/410/429/5xx, limites de tempo, cancelamento, tipos mistos, dias/timezones, s/ms/ISO timestamps, IV 0, IV null, CP e shiny desconhecidos, coordenadas inexatas, TTL, cursor de paginação, dedup e idempotência.
- Testar segurança HMAC (body adulterado, timestamp velho, event_id duplicado, assinatura inválida), receptor falhando, retry, DLQ, SSRF e limites de payload.
- Validar contra fixtures marcadas demo ou servidores de teste autorizados. Não reportar teste de prod sem execução real autorizada. Regressão para PGBR Hundo, LivePokeMap, PokeSearch UX, Snipers 1/2, Android e Root/Zygisk.

GOVERNANÇA DE BRANCHES E RESULTADOS
Criar branches e PRs SEPARADAS por módulo (A..F), sem merge automático. Se a mesma mudança já existe numa PR de outro chat, revisar e complementar, sem reescrever. Registrar no Arquivo Mestre (adendo preservando histórico): FUNÇÃO | FONTE | EVIDÊNCIA/URL | IMPLEMENTADO | TESTADO | DEVICE_TESTED | PROVIDER_CONNECTED | STAGING_DEPLOYED | RELEASE_PUBLISHED | PR/COMMIT | BLOQUEIO.

Entregar URLs das PRs criadas, commit-base, arquivos alterados, testes executados/resultados, instruções de deploy, rollback, SHA e links de builds somente se existirem. Listar separadamente:
1. FUNCIONANDO COM DADOS REAIS/LOCAIS
2. TESTADO SOMENTE COM FIXTURES
3. BLOQUEADO POR FONTE EXTERNA
4. EM PR

AÇÃO
Inicie examinando a main e as PRs atuais. Implemente módulos com acesso e testes disponíveis, preservando o core do PROMPT 02. Não interrompa para pedir permissão repetida para tarefas locais. Se um provedor não fornecer acesso, publique a capacidade local e mantenha o remoto disabled, com status honesto — nunca simule coordenadas ao vivo nem declare uma API externa conectada.
```

---
