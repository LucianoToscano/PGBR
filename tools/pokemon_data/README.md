# Reconciliador de dados Pokémon GO — PGBR / PokeSearch / PvPPokeGo / PGSharp

Ferramenta **offline**, conservadora e independente de Android. Não integra automaticamente os apps, o cliente PGSharp ou a aplicação comercial Pokémon GO.

## Uso

```bash
python tools/pokemon_data/reconciliar.py --pokemon /caminho/pogo_pkm.json --game-master /caminho/latest.json --out /caminho/saida
python -m unittest discover -s tools/pokemon_data -p 'test_*.py'
```

A origem é lida integralmente e não alterada. O processador exige listas JSON, identificadores `templateId` únicos no Game Master e chaves `(id,form)` nos registros Pokémon. Registros inválidos são preservados em arquivo separado; pares divergentes retêm todas as versões, índices e hash da fonte, com status `REVIEW_REQUIRED`. Não fazer merge automático de diferenças como `baseDefense` ou `shadow_released`. A saída inclui 4 arquivos JSON (aprovados, conflitos, inválidos, manifesto). Snapshot original não é duplicado: guarde `latest.json` e `pogo_pkm.json` separadamente em acervo de origem imutável.

**Resultado local em 22/11/2026 com os dois arquivos entregues neste chat:** 1.755 registros, 1.451 chaves, 303 grupos duplicados, 304 linhas excedentes, 24 chaves divergentes, 1.427 confirmadas, zero inválidas e 18.812 templates Game Master. Isso não valida fontes externas ao vivo, licenças ou gameplay, e não substitui testes CI/Android. As saídas contêm atributos de Pokémon, não imagens ou dados privados de contas.

Referências, SHA-256, fontes e plano de integração: [documento de consolidação](../../docs/CONSOLIDACAO_CHAT_BIBLIOTECA_VISUAL_DADOS_20261122.md).
