# PGBR — auditoria de fontes de aparições Pokémon (21/11/2026)

Esta auditoria distingue **ter um feed no aplicativo** de **ter um endpoint de resultados reutilizável**. Nenhum serviço foi acessado com credenciais ou chaves de terceiros. Não copiar assinaturas, tokens, chaves, certificados, bypass de autenticação ou dados privados.

| Fonte | Material analisado | Evidência de feed | Endpoint de Pokémon real utilizável? | Estado LT021 |
|---|---|---|---|---|
| **LivePokeMap** | Conector existente `LiveFeedClient.java` | Mapa autenticado, respostas `/api/map/viewport` com coordenadas | Implementação já existente (depende de conta/acesso e cobertura) | Integrado à barra de busca |
| **PokeList / Moonani** | APK decompilado v1.3.18 e `API_PokeList_spec.md` fornecidos | `GET https://moonani.com/PokeList/api.v2.php` com `type=pokemon`, lat/lng/distance e JSON `pokemons[]`, incluindo latitude/longitude | **Candidato, ainda não validado em rede ou quanto aos direitos de uso** | Conector experimental; erro real se não responder/negado; 60s mínimo, sem bypass |
| **PoGoCoordinates** | Conector `PogoFeedClient.java` | Busca /search + revelação individual de coordenadas, sujeita ao limite do serviço | Implementação existente | Mantido como feed separado, sem mascarar resultados |
| **PGSharp** | `arquivos_apk_pgsharp.zip` (DEX e metadata); documentação pública de feeds | Função Feed no produto | Não foi identificada API de terceiros documentada e testável neste material | Não conectado; aguarda contrato |
| **iPogo** | `ipogo-27.3.apk_Decompiler.com.zip` e launcher; documentação pública | Global/Custom Feed no produto; lógica grande parte em Unity/native | Não foi identificada API pública de feed na camada Java inspecionada | Não conectado; aguarda contrato |
| **Pokémod** | `Pokemod_Aegis_Public...`, `Pokemod_Atlas_Public...`, `Pokemod-v14.2.1...` | Recursos próprios do produto | `discovery.pokemod.dev/atlas/agent/aegis` aparece, mas é **discovery de agente**, não evidência de API de aparições | Não conectado; aguarda contrato |
| **PGTools** | `pgtools-6.1.1...` decompilado | Recursos da plataforma | `api-v2.pgtools.net` e `/notifications/push` são vistos, **não demonstram feed de aparições acessível** | Não conectado; aguarda contrato |

## Projeto de integração

- **Contrato visual único:** filtro de nome/#Dex, filtros IV/nível/XXS/XXL/PvP onde houver dados reais, seleção de fornecedor, resultados com sprite por número da Pokédex e crédito claro; expiração e distância mostradas apenas quando conhecidas.
- **Resultados garantidos pela origem:** nunca inventar coordenadas, nunca converter lista de Pokédex (metadados estáticos) em Pokémon realmente aparecendo agora e nunca atribuir dados de um fornecedor a outro.
- **Segurança de API:** autenticação separada por parceiro, armazenamento seguro quando permitido, limites e Retry-After respeitados, cache/TTL, cap de respostas, cancelamento e ausência de logs de tokens.
- **Integrações futuras PGSharp/iPogo/Pokémod/PGTools:** exigir documentação de endpoint de spawn, método de acesso e autorização apropriada (ou SDK parceiro), formato do resultado, limites e termos de crédito. O fato de um aplicativo ter sido adquirido ou ter um APK decompilado não prova que seu backend aceite consumo externo.
- **Restrições reais:** assinatura/licença do aplicativo não concede, por si, permissão de requisitar dados do backend; esse ponto precisa ser confirmado com os parceiros. PokeList ainda requer homologação do contrato `distance`, `endTime` e de respostas reais.

A LT021 entrega **interface e contrato de normalização** para LivePokeMap e PokeList. Não afirma integração efetiva de feeds privados de PGSharp/iPogo/Pokémod/PGTools.
