# PGBR LT022 — resultado do teste real de feed e contrato de parceiros

## Validação online executada — 21/11/2026

- Fonte analisada: **PokeList / Moonani**, endpoint que aparece na análise estática do APK PokeList v1.3.18 entregue ao projeto.
- Foi executada **uma única consulta GET de leitura**, com `type=pokemon`, `mons=25`, raio restrito a 1 km e coordenadas públicas de referência. Nenhuma credencial ou dado do usuário foi enviado.
- Ambiente de teste: GitHub Actions (Ubuntu), runner de CI.
- Resultado: **HTTP 403**, antes de fornecer qualquer JSON. Os três testes sintéticos locais passaram, mas **nenhuma coordenada de aparição real pôde ser observada**.
- Evidência: [GitHub Actions — consulta controlada](https://github.com/LucianoToscano/PGBR/actions/runs/44866750137), etapa "Única consulta pública do endpoint PokeList".
- Interpretação: **acesso recusado a partir do runner**. Não sabemos se o fornecedor exige sessão, licença de backend, acesso de rede aprovado ou aplica alguma outra restrição. Não é evidência de indisponibilidade global.
- Não foi feita tentativa de contorno, mudança de identidade do cliente, repetição ou consulta em massa.

## Correções incorporadas à base LT022

1. A busca Pokémon passa a iniciar em **LivePokeMap** quando não existir preferência salva. Esse fornecedor já tem um conector de login próprio, embora seus resultados ainda dependam do acesso e cobertura do usuário.
2. Em resposta HTTP **401/403 da PokeList**, não existe mais *polling* automático. Os resultados anteriores são descartados, a interface informa que não há aparições confirmadas e uma nova consulta só pode ser acionada manualmente.
3. O HTTP **429** continua obedecendo ao tempo de espera indicado; o usuário não desarma esse limite com o botão de atualizar.
4. A troca de fornecedor, raio ou filtro cancela requisições obsoletas. Respostas atrasadas **não podem** misturar Pokémon de fontes distintas nem sobrescrever uma busca mais recente.
5. O resultado exibido sempre mantém o **crédito explícito de origem**; um dado do LivePokeMap nunca é rotulado como PGSharp, iPogo ou Pokémod.

## Integração de parceiros — contrato técnico mínimo

Para habilitar qualquer um dos conectores **PGSharp / iPogo / Pokémod / PGTools / PokeList**, precisamos de dados fornecidos legitimamente pelo serviço ou por exportação autorizada que inclua, por item:

| Campo | Obrigatório | Finalidade |
|---|---|---|
| `source` | Sim | Identificar fornecedor real do dado e mostrar crédito |
| `spawn_id` | Sim | Deduplicação dentro da fonte |
| `pokemon_id` / `dex_id` | Sim | Nome e sprite no PGBR |
| `latitude`, `longitude` | Sim | Localizações reais, com validade geográfica |
| `observed_at` | Sim | Identificar a idade do levantamento |
| `expires_at` | Sim, para teleporte de aparições selvagens | Evitar teleportar para spawns antigos |
| `iv`, `level`, `cp`, `form`, `gender`, `size` | Opcionais | Filtros somente quando a fonte confirmou esses dados |

O formato será normalizado para `LiveFeedData.Entry`, que já suporta nome, coordenadas, CP, IV, expiração, atributos e sprite por ID da Pokédex.

Antes de ativar cada feed remoto, verificar: endpoint oficial ou SDK fornecido pelo parceiro, escopo permitido da autorização, método de autenticação, limites de requisição, contrato JSON real, data/hora e timezone de expiração, acesso de acordo com a licença, crédito exibido e resposta ao erro 401/403/429. A especificação nunca deve embutir senhas/chaves do fornecedor no APK ou registrar tokens.

### Situação por provedor

- **LivePokeMap** — conector já implementado; depende do login e de resultados reais do próprio serviço.
- **PokeList / Moonani** — conector experimental, **HTTP 403 no teste GitHub em 21/11/2026**, acesso não validado. Não repetir automaticamente após recusa.
- **PoGoCoordinates** — conector próprio preservado com limites da fonte; não usado como substituto para dados de outro provedor.
- **PGSharp** — feed e filtros documentados publicamente no produto, mas sem endpoint de integração de aparições comprovado: https://www.pgsharp.com/features/
- **iPogo** — Global/Custom Feed documentado, mas sem endpoint de integração de aparições comprovado: https://ipogo.app/features.php
- **Pokémod** — produtos Atlas/Aegis documentados: https://pokemod.dev/mapping ; a documentação encontrada não estabelece uma API de spawns para consumo do PGBR.
- **PGTools** — endpoints encontrados por análise estática não comprovam acesso a resultados de aparição.

**Importante:** compra/licença de um APK, autorização de análise técnica e permissão de acesso ao backend de aparições são assuntos diferentes; nenhuma autorização de API/SDK pode ser inferida exclusivamente de arquivos decompilados.

## Homologação real pendente

- Confirmar com os provedores o acesso autorizado (ou exportação real) para alimentação da busca multi-fonte.
- Testar pelo menos uma resposta contendo espécie, coordenadas e expiração, sem expor dados privados nos logs.
- Comparar os resultados com o mapa em um aparelho, em tempo real.
- Não anunciar como fonte operacional antes dessas três confirmações.
