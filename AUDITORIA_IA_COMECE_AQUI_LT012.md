# PGBR LT012 — Ponto de entrada para outra IA auditora

**Versão:** PGBR v0.00.12-LT. **Origem:** base LT011 (tempestade), complementação seletiva PR #9 (ícones, histórico, privacidade). **Idioma:** PT-BR.

## Questão para solucionar
Como garantir que o PGBR v0.00.12-LT apresente efeitos realmente visíveis no joystick e na Pokébola, tenha o evento Eternatus confiável sem resgates/avanços falsos, sprites de itens reais sem distorção, ícones de histórico legíveis e privacidade somente visual, sem regressões em modo Root/sem Root, e publicar com provas de teste?

## Comece investigando
- `pgrazor/src/dev/pgrazor/EternatusRouteRunner.java` — início, preflight, callbacks, cooldown, rota, skip manual, mensagens nativas.
- `EternatusMission.java`, `EternatusProgress.java`, `ResearchSettings.java`, `pgrazor/native/research.cpp` e `pgrazor/native/research_policy.h` — identidade de pesquisas e deduplicação. Confirme questId e recompensa com registros reais do jogo. Não invente automação de paradas sem missão.
- `Pad.java`, `EternatusVisualEffects.java`, `EternatusFxTiming.java`, `EternatusMorph.java`, `SettingsPanel.java` — atmosfera, efeitos visíveis em 48dp, clipping, FPS, caches, capacidade de escolher o joystick anterior.
- `ItemBadge.java`, `ItemSpriteFit.java`, `Cooldown.java`, `PtBr.java` — proporção e identidade dos sprites, botões brancos/ícones verde-escuros, PT-BR.
- `Control.java`, `AccountsDialog.java`, `TrainerAliasPolicy.java`, `pgrazor/diagnostics/il2cpp-survey.js`, `pgrazor/native/schema/game/0.437.0.json` — ocultação **PGBR-only** confirmada; Unity mapa e perfil **pendentes** sem ponto de renderização validado.
- `pgrazor/test/test_item_sprite_fit_java.py`, `test_eternatus_route_java.py`, `test_eternatus_fx_java.py`, `pgrazor/test/SettingsBackupRulesTest.java` e demais testes.
- `docs/PGBR_LT011_CORRECOES_PARCIAIS.md` e `docs/releases/v0.00.12-LT.md`.

## Proveniência de sprites
Biblioteca Drive: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK

Efeitos de vórtice/fumaça importados: `pgrazor/assets/map-icons/eternatus-fx-*.png`. Pasta de origem: `05 - Efeitos e animações / PokeMiners - Efeitos visuais`. Conferir autorizações/licenças. Catálogo de itens traz 37 sprites com IDs rastreados, mas **não** cobre todos os itens. Não criar mapeamentos sem evidências.

## Relatório solicitado
Para cada problema, indicar **status real**, arquivo:linha, reprodução, causa raiz, correção, teste executado, teste não executado e risco de regressão. Separar explicitamente **implementado**, **testado em JVM**, **compilado**, **homologado no aparelho**, **publicado**. O pacote ZIP contém todos os arquivos versionados, não inclui secrets, APKM privado, `.git` nem a biblioteca original completa.
