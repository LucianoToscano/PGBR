# PGBR — integração opt-in PokeList Partner API v1 (22/11/2026)

**Estado:** código candidato para revisão na branch `feat/pokelist-partner-api-snipers-20261122`, derivada da PR #115 de Sniper 1/2. **Sem merge, APK, homologação Android ou feed real.** O objetivo não é substituir `PokeListFeedClient` legado nem afetar Radar, batalhas, GPS, PokeSearch ou outras PRs.

## Fontes e escopo

- Entrega do titular: https://drive.google.com/drive/folders/11BSUaiv2JjDti6KDmvebMGvrvIlTdQjS
- Contrato OpenAPI 3.1: https://drive.google.com/file/d/1QGfd9jcA9kX6yMSLEJKamGav54OlbOoY/view
- Referência da API: https://drive.google.com/file/d/1JQJpjoO_mi8dVEpd--7kwQ76FRX-gHVf/view
- Dependências externas B1–B10: https://drive.google.com/file/d/1fjLmv6UBokym9Zx0O2T96kDP5E8ESBiB/view
- Código de referência tem 91 testes reportados pelo pacote do titular; estes testes NÃO validam o cliente Android nem uma API de produção.

## Funcionamento no aplicativo

1. Abrir Busca de Pokémon → Sniper 1 ou Sniper 2 → Fonte → **PokeList Partner API v1**.
2. Configurar a **origem HTTPS** (não `/v1/...`) e a chave de parceiro `plp_<id>.<secret>`. Não há endpoint nem segredo embutido.
3. Apenas a origem HTTPS é guardada nas preferências privadas. A chave fica em memória durante a sessão e é apagada ao encerrar o processo ou pressionar **Apagar chave**. Não usar a chave pessoal de Pokémon GO, PGSharp ou outro provedor.
4. O cliente consulta `GET /v1/capabilities` antes de buscar. Se licença não for aprovada ou `live_provider_feed` não estiver configurado, falha sem listar fixtures como spawns reais.
5. O cliente usa `POST /v1/search/pokemon` com centro/raio ou busca global autorizada, espécie e filtros declarados `server_side`. Recebe até três páginas de 100 registros; indica **parcial** se restarem páginas.
6. Aplica os filtros locais por slot sobre os resultados recebidos, mantendo deduplicação, estados de erro, expiração, origem e ordenação da PR #115. A nova busca reaplica filtros remotos quando editados.
7. Ações Copiar/Mapa/Teleporte usam o fluxo manual existente da PR #115, nunca automação de movimento.

### Segurança e fidelidade

- HTTPS obrigatório, URL sem credenciais/query/fragmentos ou caminhos extras, redirecionamentos desativados, limite de 2 MB por resposta, timeouts, cancelamento real, limites de página, `Retry-After`, erros 401/403/429/5xx explícitos.
- Rejeita spawns expirados, observações temporalmente inválidas e fontes que anunciam `demo`, `fixture`, `synthetic` ou `sintet`.
- Campos `null` continuam desconhecidos. `quality=estimated` não fornece IV confirmado nem coordenada apta a Teleporte; `coordinate_precision` diferente de `exact` bloqueia Teleporte. Shiny `eligible` significa **possível**, nunca confirmado para a conta.
- PvP server-side é usado apenas se `/v1/capabilities` declarar suporte, sem prometer cálculo PvP no backend. Uma linha com ranks Great e Ultra simultâneos ainda não cabe no modelo `Spawn` de uma liga; nenhum rank é inventado.
- A busca global abrange apenas os eventos realmente ingeridos e permitidos pela chave; não significa rastrear todo o planeta.
- `POST /internal/v1/events:batch` e `X-Internal-Key` são exclusivos da infraestrutura do titular; **não entram no APK**.

## Arquivos deste trabalho

- `PokeListPartnerClient.java`: capability gate, contrato de rede, paginação, parsing com qualidade e filtros remotos condicionais
- `SpawnSourceRegistry.java`, `SpawnContract.java`, `SpawnConnectors.java`, `SpawnSearch.java`: registro e ponte isolada, contrato de request opcional
- `SniperPanel.java`: formulário opt-in, chave apenas em memória, ligação por slot
- `PokeListPartnerClientTest.java`, `tools/sniper_jvm/compile_engine.py`: verificador da semântica dos dados e falha fechada
- `PGBR_ARQUIVO_MESTRE_UNICO.md`: registro incremental, sem redução do histórico

## O que permanece bloqueado

B1: acesso ao backend e ingestão real; B2: aprovação escrita do titular para licenciamento; B3: política de coordenadas/cache; B4: staging HTTPS + chave de testes; B5: migração PostgreSQL testada; B6: decisão Firebase; B7: catálogo licenciado completo; B8: cobertura efetiva dos campos opcionais; B9: autorização/escopos/quotas do parceiro; B10: retenção e logs.

**Webhooks e alertas push:** o pacote fornece contrato e receptores **server-side**. A entrega de push ao Android requer gateway próprio autenticado, política de notificação e licença B2/B3/B9; não embutir segredo HMAC do webhook nem expor um servidor HTTP no aparelho. Não há endpoint real para validá-los.

**Validação exigida para habilitar produção:** CI de contrato Java + compilação Android, staging autorizado, teste real de 401/403/429/410/422 e paginação, limpeza de dados ao expirar, testes em aparelho de busca/teleporte com consentimento; `device_tested=false` até então.
