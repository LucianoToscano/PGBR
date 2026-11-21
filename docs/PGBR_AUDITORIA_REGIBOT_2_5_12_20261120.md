# PGBR — auditoria Regibot 2.5.12 (APK decompilado)

**Origem privada:** ZIP fornecido pelo responsável; `regibot-2.5.12.apk_Decompiler.com.zip`, SHA256 `d5cb0675190693e2c40db5eacea277c741234314fecb40a9e6d552338e5259de`.

**Limite:** análise estática; o APK, o injetor e os scripts NÃO foram executados. `ANDROID-RECONSTRUCTION.txt` informa 141 métodos não reconstruídos e 104 erros de decompilação. Configuração em arquivo não prova funcionamento no aparelho, nem acesso aos servidores privados. Nenhum binário ou credencial é incluído neste commit.

## Resumo do pacote

- `AndroidManifest.xml`: `com.regitools.regibot`, `versionName=2.5.12`, `versionCode=73`, `minSdkVersion=26` e `targetSdkVersion=36`.
- ZIP: 7.679 entradas; 3.241 Java decompilados, 3.241 Smali, 917 XML, 187 PNG.
- Cinco esquemas sob `resources/assets/config_schema/`: **419 campos** em **59 seções** (`regiplay` 76, `grunt` 95, `catchemall` 127, `raid` 79, `sniper` 42); **275 campos** com ativação condicional `enabledWhen`. Tipos: 216 booleanos, 92 inteiros, 50 notas, 27 JSON, 15 choices, 11 multi, 6 strings, 2 floats. 14 seletores de espécies e 13 tabelas.
- Game data: `pokemon.json` 1.250 registros, 1.025 Dex IDs únicos; `pokemon_forms.json` 1.473 IDs de formas; `move_names.json` 534 nomes; `pvp_species.json` 1.045 registros, `pvp_meta.json` top 250 para GL/UL/Master (fonte declarada PvPoke, data 13/10/2026); `stamp_rallies_builtin.json` 9 listas.
- Nativo: `regi_inject_pro` 22.982.696 bytes e `regi_run` 4.328 bytes, ELF ARM64; `libregibot-security.so`/ `libregiqv.so`. Scripts `regibot_watchdog.sh` e `attach_sampler.sh`. Pontes Frida Java/IL2CPP empacotadas, não um SDK estável com contrato PGBR.

## Descobertas para o PGBR

| Recurso | Prova estática | Caminho PGBR |
|---|---|---|
| **Private mode** | `settings_private_mode_desc`: oculta nome na própria tela para screenshots/streaming, preservando o nome visto por terceiros. `switchPrivateMode`, `editPrivateName`; apelido default `Regibot`, até 15 caracteres, aplicado no próximo início | Investigar máscara de apresentação `*****`/vazio/alias no mapa e perfil **Eu**; hooks reais e restauração precisam de QA |
| **Filtros IV** | `AdvancedIvFilterActivity`, `AdvancedCatchFilterActivity`, `SavedFilterSetsActivity` e configuração por IV exato | `PogoFeedFilters`, `PokemonInventorySearchBuilder`, perfis editáveis e critérios compostos |
| **Metas PvP/Pokédex** | `PokedexActivity`, `PvpRankListActivity` e strings `Skip ranks I already have` GL/UL | Bridge PvPPokeGo, inventário importado quando autorizado, sem confundir PvP rank da espécie e do IV |
| **Stamp Rally e checkpoints** | nove listas + progresso parcial e `Resume` por conta nos recursos de strings | Evoluir `EternatusRouteRunner` com persistência N/total e retomada segura |
| **Histórico** | `CooldownTrackerActivity`, `DailyAccountDataActivity`, `WeeklyLimitsActivity` | painel por conta 24h/7 dias com métricas locais e dados verificados |
| **Inventário protegido** | `regiplay`, `grunt`, `catchemall`, `raid` e `sniper` definem regras de manter shinies, hundos/nundos, trajes, fundos e espécies | edição e prévia de exclusões antes de transferências e reciclagem |
| **Alerts/webhooks** | `WebhookConfigActivity`: Shiny/Hundo/Nundo, storage full, cooldown, start/stop, recovery | notificações opt-in, nomes e coordenadas desativados por padrão |
| **Root/remote health** | `BotService`, `RemoteKeepAliveService`, scripts de watchdog/sampler | diagnóstico não destrutivo em `RuntimeHealth`; sem importar relaunch/oom adjustment sem teste |

### Privacidade — delimitação

O texto do Regibot confirma uma *configuração pretendida* de substituição visual; **não demonstra** em quais componentes Unity isso acontece. O item `action_hide_names` em `AccountManagerActivity` corresponde à lista de contas, não deve ser confundido com a máscara do nome no jogo. Não usar a descrição da UI para afirmar que a proteção PGBR já funciona em lives. Preservar dados reais de identidade/login/PlayerService/RPC.

### Cuidados com Root, serviços e compatibilidade

O `regibot_watchdog.sh` usa período de 10s, `oom_score_adj=-700`, `renice=-5` e reabre `MainActivity` quando a sessão está marcada. O `attach_sampler.sh` observa `/proc`, threads e carregamento de `libunity.so`/`libil2cpp.so`. Isso é útil como referência de diagnóstico, mas sua política agressiva pode competir com o bootstrap do PGBR. O manifesto declara `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, overlay e instalação de pacotes. O módulo de controle remoto requer escopo de permissões, pareamento, revogação e testes de privacidade; não habilitar por padrão.

O manifesto descreve versão do **Regibot**, não a versão base de Pokémon GO suportada por seu injector. O fluxo LT015 PGBR é 0.437.0; a compatibilidade binária não está demonstrada. Fonte PvPoke exige verificar licença/upstream antes de distribuir modificações e datasets.

## Aplicação recomendada

1. **P0 — Private mode**: comparar o mecanismo visual no runtime compatível; testar mapa + Eu, desligar, troca de conta e captura de tela.
2. **P0 — Busca unificada**: adotar o padrão de esquema para filtros por espécie/forma, IV exato, tamanho, gênero, PvP e presets, com migração das preferências atuais.
3. **P0 — Checkpoint**: progresso N/total do evento Eternatus por conta; recuperação e log de evidência de ações, sem inventar recompensas.
4. **P1 — Painel inteligente**: cooldown, dados diários e limites 7d locais por conta.
5. **P1 — Segurança de inventário**: regras de manter, revisão antes de atos irreversíveis, reservas de bolas/poções e telemetria.
6. **P2 — Comunidade / controle remoto**: somente com infraestrutura própria e autorização explícita, minimização de dados e controle de acesso.

**Status desta auditoria:** registro de evidências e recomendações; **nenhum módulo Regibot foi integrado ou ativado**. Relaciona-se ao épico #24, filtros #20 e privacidade #16.
