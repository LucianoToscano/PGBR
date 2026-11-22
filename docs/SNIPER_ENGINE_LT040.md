# Sniper 1 e Sniper 2 nativos — motor, filtros e teleporte

Complemento de [`SNIPE_SLOTS_LT040.md`](SNIPE_SLOTS_LT040.md). Aquele documento criou dois conjuntos de
filtros sobre o feed 100% IV; este entrega o **motor de busca compartilhado** com filtros completos,
fontes selecionáveis e teleporte integrado, tudo dentro do PGBR.

## 1. Cadeia de dados

```
fonte (HTTP)  →  cliente existente (IFlowGo/RocketMap/PokeList/Hundo)
              →  SpawnSourceRegistry.normalize
              →  SpawnContract.Spawn          (desconhecido continua desconhecido)
              →  SpawnSearch.run              (dedupe → expiração → filtro do slot → ordem)
              →  SpawnSearch.Result           (recebidos / filtrados / exibidos + Estado)
              →  SniperPanel                  (lista, detalhe, copiar, mapa, teleporte)
```

`SpawnSearch` não faz I/O: recebe um `Source`. Isso permite exercitar a cadeia inteira com fixtures e
trocar de fornecedor sem tocar na interface.

## 2. Estados que a interface sabe diferenciar

| Estado | Quando | O que o usuário lê |
| --- | --- | --- |
| `OK` | há resultado | «N recebidos; M removidos por filtro; cobertura: …» |
| `SOURCE_EMPTY` | a fonte respondeu zero | «zero registros para esta consulta; isso **não** significa ausência mundial» |
| `ALL_FILTERED_OUT` | havia linhas, os filtros removeram todas | «ajuste ou limpe os filtros» + contagem recebida |
| `ACCESS_DENIED` | 401/403 | «a fonte recusou; o PGBR não contorna» |
| `RATE_LIMITED` | 429 | «aguardando o intervalo informado pela fonte» (`Retry-After`) |
| `SOURCE_ERROR` / `NETWORK_ERROR` | 5xx, JSON inválido, sem rede | falha explícita, nenhum dado inventado |
| `COORDINATES_BLOCKED` | linhas sem coordenada revelada | fluxo de revelação do próprio fornecedor |
| `CANCELLED` | consulta substituída | não é erro |
| `STALE` | snapshot velho | teleporte bloqueado até atualizar |

## 3. Filtros

- **Espécie**: multi-seleção 1–1025. Filtro no servidor **somente** quando há exatamente uma espécie
  (iFlowGo `pokemon_id`, PokeList `dexId`); várias espécies nunca são colapsadas em uma.
- **IV**: 100%/*zero*/faixa 0–100 e ataque/defesa/stamina 0–15. «100%» exige 15/15/15 ou 100% declarado
  pela fonte; PC alto não promove ninguém a perfeito.
- **PC, nível (1–50), gênero, forma, XXS/XXL**: limite definido nunca é satisfeito por valor ausente.
- **PvP**: Grande Liga / Ultra Liga / Pequena Copa + rank máximo. Rank local é marcado «(calculado)».
- **Shiny**: «possível» é propriedade da espécie; «confirmado» só quando a fonte afirma para aquele
  encontro. Nunca se confundem.
- **Distância**: distância máxima de teleporte (haversine) e raio da consulta (1–500 km).
- **Grupos**: lendários e míticos usam a tabela curada de `SpeciesGroups`; regionais, raros e evento são
  **recusados** com mensagem até existir lista validada.
- **Perfis**: até 20 por slot, persistidos em `pgrazor-snipe-v2` sob a chave do slot (`slot1`/`slot2`).

## 4. Fontes

| id | Situação | O que entrega |
| --- | --- | --- |
| `iflowgo` | pronto | coordenadas, IV completo, nível, PC, gênero, expiração; `pokemon_id` no servidor; global = **primeira página** |
| `pgbr-hundo` | pronto | feed próprio 100% IV (baixado na hora, cache 30 s) |
| `pokelist` | pronto | exige espécie para busca global; raio 1–5 km |
| `rocketmap` | precisa configuração | servidor HTTPS autorizado pelo operador; só área |
| `pogocoordinates` | revelação obrigatória | usa o painel próprio da fonte e as cotas dela |
| `pgsharp`, `ipogo`, `pokemod` | não integrados | nenhum endpoint, chave ou protocolo é usado |

## 5. Teleporte

1. Toque na linha → detalhe com valores reais, fonte, distância e tempo restante.
2. «Teleporte» revalida coordenada, expiração e frescor.
3. `TeleportDialog.confirm` mostra destino e **cooldown de chegada** (`Cooldown.remainingAt`).
4. Só depois do «Teleporte» confirmado: `Control.canEnable()` → `Control.teleport(lat, lon)`.
5. A mensagem é «Teleporte solicitado; confirme a posição no jogo» — nunca «teleportado com sucesso».

## 6. Como verificar

```bash
# contrato estático (rápido, sem JDK)
python3 -m unittest pgrazor.test.test_sniper_panel_contract

# motor compilado + 148 verificações + painel compilado (precisa de JDK e de um ECJ jar)
ECJ_JAR=caminho/para/ecj.jar python3 tools/sniper_jvm/compile_engine.py
```

O verificador imprime «SKIPPED» e devolve 77 quando não há JDK/ECJ: nesse caso **nada foi compilado** e
nenhum resultado deve ser alegado.

## 7. Pendências assumidas

- Build Android/APK e homologação em aparelho: **não executados**.
- Consulta real a fornecedor: **não executada** (egress bloqueado no ambiente de desenvolvimento).
- Busca global do iFlowGo limitada à primeira página do endpoint observado.
- Regionais/raros/evento aguardando lista verificável.
