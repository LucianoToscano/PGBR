# PGBR LT026 — integração opcional de servidores de radar

## Evidências dos repositórios enviados (21/11/2026)

| Repositório | Função comprovada no código | Situação para o PGBR |
|---|---|---|
| istornz/iPokeGo (MIT) | Cliente iOS para servidores RocketMap e Pogom; seleção de servidores, favoritos, distância e avisos | Referência de interface e gerenciamento de fontes |
| RocketMap/RocketMap (AGPL-3.0) | Servidor Flask `GET /raw_data`, com `pokemons` e `gyms`, incluindo objeto aninhado `raid` | Compatibilidade por parser independente, sujeito a configurar servidor próprio/autorizado |
| brentschooley/pokespotter (MIT) | Biblioteca Node antiga baseada na API não oficial de 2016, exige usuário/senha Pokémon GO | NÃO usar credenciais pessoais nem prometer dados 2026 |
| poke4map/pgmap (AGPL-3.0) | Implementação antiga Python 2.7, mapa Flask e scanners | Evitar duplicação com RocketMap |
| PokemonGoMap/PokemonGo-Map | README orienta migrar para RocketMap | Não é fonte independente |
| brendenriggs/GottaSlackEmAll | README informa projeto descontinuado após API deixar de funcionar | Referência de alertas históricos, não fornecedor atual |
| jfberry/PoracleNG (ISC) | Webhooks do Golbat → processor Go → MySQL → alertas Discord/Telegram e APIs | Arquitetura futura para serviço PGBR/PokeSearch **com scanner próprio conectado** |
| iam4x/pokemongo-webspoof (arquivado) | App macOS/iOS de localização; README declara "NOT SAFE TO USE ANYMORE" | Não integra feed de Pokémon |

### PoracleNG e TILESERVER.md

`TILESERVER.md` descreve **dados de renderização de mapas estáticos para alertas já detectados**, não uma API pública de Pokémon mundiais. Ex.: `latitude`, `longitude`, `pokemon_id`, `form`, `shinyPossible`, e para Reides `pokemon_id`, `level` e `teamId`. *shinyPossible não confirma encontro shiny para uma conta.* Para resultados atuais é necessário um scanner (Golbat, conforme README PoracleNG) e uma fonte de dados autorizada.

### Contrato RocketMap efetivamente lido

Em `pogom/app.py`, há `GET /raw_data`; a resposta inclui:
- `pokemons: [{pokemon_id, encounter_id, latitude, longitude, disappear_time, individual_attack, individual_defense, individual_stamina, cp, ...}]`
- `gyms: { <gym_id>: {latitude, longitude, name, team_id, raid:{pokemon_id, level, start, end, cp, ...}} }`

`CustomJSONEncoder` converte `datetime` para timestamp Unix **milissegundos**. Nosso parser ignora aparições ou Reides sem expiração confirmada ou já expiradas. O Ovo de Reide não é apresentado como chefe confirmado antes de `start`.

**Atenção ao acesso:** o RocketMap antigo implementa regras de bloqueio em `pogom/app.py` (incluindo `no_referrer`) e pode devolver HTTP 403 a clientes externos. O PGBR **não tenta contornar essa proteção**. Quem administra o próprio servidor deve publicar uma API autorizada para o aplicativo, com proteção de acesso apropriada. Uma URL sem scanner ativo pode responder com zero resultados.

### Implementação isolada na LT026

- Quarta fonte opcional do filtro Pokémon e terceira fonte do filtro Reides: `RocketMap (meu servidor)`.
- Configuração manual de URL HTTPS base, sem login do Pokémon GO, sem URL pública embutida e sem seguir redirecionamentos.
- Busca local por viewport até 25 km, campos de IV/CP/expiração quando disponíveis, deduplicação e identificação da fonte.
- HTTP 401/403 e 429 respeitados, sem repetição automática do acesso negado; intervalo mínimo de 60 segundos para o conector.
- Seleções iFlowGo, LivePokeMap, PokeList e PoGoCoordinates seguem independentes.
- RocketMap não informa no contrato atual os rankings PvP, tamanhos XXS/XXL, backgrounds nem Shiny individual; esses filtros não podem prometer dados.
- O teleporte usa o fluxo existente de confirmação; a inclusão desta fonte não injeta coordenadas automáticas.

## Não concluído

1. URL de um RocketMap efetivamente operacional e autorizado — **não fornecida**.
2. API de scanner Golbat/PoracleNG para coordenadas em tempo real — requer servidor e operador; `TILESERVER.md` não serve como API de feed.
3. Verificação no APK real do celular — compilação de controles e testes offline não equivalem a instalar/testar a distribuição.

Este módulo não contém código copiado do RocketMap; é uma adaptação Java independente de seu esquema documentado. A licença AGPL do repositório original deve ser respeitada caso alguém copie/modifique ou distribua o próprio servidor RocketMap.
