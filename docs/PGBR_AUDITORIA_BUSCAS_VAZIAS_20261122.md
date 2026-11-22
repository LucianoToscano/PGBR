# Auditoria das buscas vazias — PGBR + PokeSearch (22/11/2026)

Escopo: PROMPT MESTRE — corrigir buscas, radares, filtros, Sniper 1/2, coordenadas e as
**listas de resultado que apareciam vazias**. Tudo abaixo foi reproduzido no código, corrigido,
coberto por teste e reexecutado. Nada de fonte real foi consultado: o sandbox não alcança
nenhum provedor (`curl` → `http=000`), então toda verificação é de contrato/offline.

## 1. Defeitos corrigidos no PGBR (branch `arena/75dcdb70-pgbr`, PR #115)

| # | Sintoma | Arquivo / método | Causa | Correção | Teste |
|---|---------|------------------|-------|----------|-------|
| 1 | Aba **Ginásios** e **Paradas** sempre vazia em servidor RocketMap saudável | `RocketMapFeedData.parse` | só lia `gyms` **com** `raid`; ignorava ginásio sem reide e todo `pokestops` | `readPlainGyms` / `readPokestops` (`Kind.GYM` / `Kind.STOP`), isca futura sinalizada, `0,0` recusado | `RocketMapFeedDataTest` (+7) |
| 2 | Mesmo servidor: nenhuma parada retornada | `RocketMapFeedClient` | query pedia `pokestops=false` | `pokestops=true` | `test_rocketmap_wiring.py` |
| 3 | Em modo reide, Ginásios/Paradas vazios | `LiveFeedDialog.rebuildVisible` | laço descartava toda linha não-`RAID` **antes** do switch de categoria | só linhas `RAID` passam pelo `raidFilters` | `test_live_feed_java.py` |
| 4 | Busca global do iFlowGo presa na página 1 | `IFlowGoFeedClient.fetchGlobal` | URL fixa `page=1`; `total` do servidor nunca era lido | até `GLOBAL_PAGES` páginas, continua só quando `total` prova que existe, dedupe por id, nota "páginas N de M" e aviso de resultados além da página; falha após a 1ª página preserva as linhas ("busca parcial") | `IFlowGoFeedDataTest` (+8) |
| 5 | `/raw_data` com `disappear_time` em segundos → lista vazia | `RocketMapFeedData.unixMillis` | só aceitava 13 dígitos; segundos viravam `0` → toda linha descartada como expirada | 10¹²..10¹³ = ms; 10⁹..10¹⁰ = s; string numérica aceita; resto continua desconhecido | `RocketMapFeedDataTest` (+10) |
| 6 | Busca por **nome** pode esconder a própria resposta | `PokemonFeedSearch` / `LiveFeedDialog` | requisição resolvia nome → dex, lista comparava o rótulo `#25` | `resolveDex()` única usada nos dois lados; ambíguo nunca escolhe sozinho | `PokemonFeedSearchTest` (18) |
| 7 | Aba vazia sem explicação (iFlowGo/PokeList/demo não têm ginásio/parada) | `LiveFeedDialog` | lista vazia genérica | `sourcePublishes`/`requestedLayer`/`emptyReason`: a lista diz **por que** está vazia | `test_feed_layer_capabilities.py` (6) |
| 8 | "Nenhum resultado" único para todas as causas | `LiveFeedDialog.rebuildVisible` | sem instrumentação | contadores por causa (expirados, categoria, espécie, IV/nível/PvP, filtro de reide, raio, coordenada) → `Descartados: …`; zerados a cada passagem | `test_feed_layer_capabilities.py` |
| 9 | Controle de raio ignorado no modo demo | `DemoLiveFeedData.sample` | fixture fixo | sobrecarga com `radiusKm`, nota "N de M exemplos dentro de X km"; teleporte continua bloqueado | `DemoLiveFeedDataTest` (+7) |
| 10 | Matriz HTTP nunca executada | `tools/sniper_jvm/compile_engine.py` | só compilava o teste | harness exige `PASS:` da matriz além do `0 falha` do motor | `HttpErrorMatrixTest` (12) |
| 11 | **Sniper 1/2 não abria** quando o slot salvo tinha só `{"enabled":true}` | `SnipeFilters.fromJson` | `o.get("species")` sem `has()`; `org.json` do Android **lança** `No value for species` | `get` guardado por `has()` | `SnipeFiltersTest` (+3) |
| 12 | Backup de configurações dava exceção crua sem `version`/`speedKmh` | `SettingsBackupRules.validate` | mesmo padrão `get()` sem `has()` | campos opcionais com fallback validado | `SettingsBackupRulesTest` |
| 13 | **CI vermelho** em 4 workflows: `NoClassDefFoundError: org/json/JSONObject` | `test_pokemon_feed_search_java.py` | compilava com `-classpath android.jar` e executava só com o diretório de saída | `org_json_support.py` + classpath de execução igual ao de compilação | `test_runtime_classpath_contract.py` (3) |

## 2. Evidência de execução

```
cd pgrazor/test && python3 -m unittest discover -p "test_*.py"
Ran 329 tests — OK (skipped=64)              # sandbox sem JDK
                                              # com JDK: 328 OK + 1 erro só de dependência
                                              # fixada (dexlib2 ausente; o CI a baixa antes)

ECJ_JAR=tools/sniper_jvm/ecj.jar python3 tools/sniper_jvm/compile_engine.py
SnipeEngineTest: 160 verificações, 0 falha(s)
PASS: 12 casos da matriz de validação de servidor HTTPS
OK: motor + matriz HTTP + painel compilados e suíte verde contra os stubs.

# reprodução do defeito 13 (prova de que o teste pegava o defeito)
compilação com a dependência:            rc 0
forma antiga (runtime sem dependência):  rc 1  NoClassDefFoundError: org/json/JSONObject
forma nova (mesmo classpath):            rc 0  PASS: 18 verificações
```

Commits: `a1e0af4` (camadas+busca+vazio), `844b59f` (paginação), `677a82e` (matriz HTTP),
`227a9d1` (diagnóstico de descartes), `648d3bd` (epoch em segundos), `7b840a5` (defeitos 11/12),
`09eb28e` (defeito 13).

### 2.1 Situação do CI — commit `09eb28e` (todos verdes)

| Workflow | Estado |
|---|---|
| PGBR sniper engine (JVM) — `45072235918` | success |
| PGBR - QA Sniper 1 e 2 (filtros separados) — `45072235946` | success |
| PGBR - QA Sniper 100% IV manual — `45072235980` | success |
| PGBR Radar Filters and Authorized Webhook QA — `45072235934` | success (2 jobs) |
| PGBR - QA PGSharp free and optional key — `45072235917` | success |
| PGBR - Arquivo Mestre e limpeza segura — `45072235971` | success |
| PGBR - verificacao de codigo em PRs e main — `45072235950` | success (4 jobs, inclui o `check.py` real e a compilação Java) |

Nenhum resultado de commit anterior foi usado como comprovação do commit novo: a tabela acima é
do run disparado por `09eb28e`.

## 3. Bloqueios e limites honestos

- **Nenhuma consulta real a provedor**: rede de saída do sandbox bloqueada. "Fonte retorna dados
  reais com evidência" **não** pode ser afirmado por mim; o que está provado é o contrato de
  parsing/URL/erro e a lista de motivos de vazio.
- **Sem build Android / sem APK**: `device_tested=false`. O harness compila contra stubs
  (`tools/sniper_jvm/stubs`), o que não substitui `android.jar`. Nesta sessão o JDK do sandbox e
  o `ecj.jar` em cache desapareceram; a compilação Java local foi restaurada baixando o ECJ
  3.38.0 de um blob público (Maven e releases do GitHub estão bloqueados) e compilando com
  `-source 21 -target 21`. O emulador de `javac` aponta para esse ECJ.
- **O binário `aar`/APK não foi gerado aqui**: nenhuma build Android rodou neste ambiente;
  não existe link de APK para este commit.
- **Cobertura por fonte**: iFlowGo não tem camada de ginásio/parada (agora declarado na UI);
  PokeList só Pokémon; demo só exemplos fictícios.
- **PokeSearch**: push/PR negado (`403` — sem permissão de escrita para o token desta sessão).
  O patch verificado está em `docs/patches/pokesearch-gateway-viewport-20261122.patch`
  (32 testes node, 0 falha, aplicável em `feat/demo-frontend-live-feed-20261122`).

## 4. Próximos passos

1. Homologar em aparelho as abas Ginásios/Paradas e a busca global paginada contra um servidor
   RocketMap autorizado e um iFlowGo autorizado (registrar `device_tested` e evidência).
2. Ligar `LiveFeedDialog` → `SpawnSearch` (motor nativo) para o Sniper consumir as mesmas fontes.
3. Aplicar o patch do PokeSearch quando houver permissão de escrita no repositório.

**Não** declaro integração "pronta" nem operação ao vivo confirmada: o que está verificado é
software testado offline, com limites explícitos acima.
