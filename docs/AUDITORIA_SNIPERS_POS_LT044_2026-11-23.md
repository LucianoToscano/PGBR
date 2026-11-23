# Auditoria de regressões — Sniper 1/2 após LT044

## Linhagens confrontadas
- Atual PGBR LT044 candidata (`release/lt044-ptbr-gps-qa-20261123`).
- PR #121 `audit/pgbr-regressions-20261122-2020`, baseada em `b355def7f6693b4d7f88c802b6ea40df018f0490` da PR #115.
- PR #118 (PokeList Partner) depende da origem PR #115 e ainda não foi incorporada nem homologada.

## Inventário de conflitos reais
- 118 arquivos na PR #121: 109 sem colisão de blob, 9 conflitantes com main LT044.
- Java IFlowGoFeedClient: conservar paginação do novo sniper e políticas atuais `nearbyRadiusKm` e `globalSpeciesQuery` para cada página; não enviar espécie inválida.
- Java LiveFeedDialog: conservar novos slots/filtros do Sniper e bloqueio atual de falso Global de Reides/ID Dex inválido; manter default de raio distinto entre Pokémon e Raid.
- SettingsBackupRules: adicionar validação de campo obrigatório com `root.has` sem descartar catálogo 200/100, escopo PGSharp ou consentimento de Teleporte.
- Testes de wiring: manter contrato remoto da política atual em vez de procurar literal `pokemon_id=` no cliente.
- Histórico: não substituir README, CHANGELOG, docs ROOTED ou Mestre pelos snapshots LT040/LT041 da origem.

## Gates para promoção
- CI de código `tools/check.py`, compilação Java/Root, C++ host e ARM64 antes de merge.
- QA dos Snipers em JVM com casos de zero resultados, 401/403/429, filtros, erros e URLs autorizadas.
- APKs assinados novos somente com tag nova, preservando LT043/LT044 e hashes. Teste real no Android é exigido para declarar localização/teleporte funcional.
- Exigir credenciais ou fonte de coordenadas autorizada; não fabricar endpoints privados, acesso a PGSharp, PokeList Partner nem resultados reais.

**Status:** branch de auditoria, não incluída em release; `device_tested=false`.
