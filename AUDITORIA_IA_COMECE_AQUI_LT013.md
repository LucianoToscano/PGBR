# PGBR LT013 — Entrada para auditoria por outra IA

**Escopo:** revisão incremental PGBR v0.00.13-LT, baseada na LT012, com código completo versionado, guiado pelas regras `AGENTS.md`.

## Questão principal
Como validar que o cooldown muda a verde somente quando `0:00:00`, que o relógio opcional mostra a cidade da coordenada sem travar o mapa, e que a rota Eternatus percorre GPX longo com margem segura de 3s, opção sem VGP e retomada por índice, sem inventar resgates ou perder recursos do PGBR?

## Código para auditar primeiro
- `CooldownVisual.java`, `Pad.java`, `Cooldown.java`, `SettingsPanel.java`: transição de cor, contorno, modos Discreto/Chamativo e estados sem histórico.
- `LocalTimeCityLookup.java`, `LocalTimeOverlay.java`, `LocalTimeClock.java`, `TimeZoneLookup.java`: geocodificação assíncrona, cancelamento, cidade obsoleta, relógio local, permissões e fallback.
- `EternatusRouteRunner.java`, `GpxRoute.java`, `GpxCodec.java`, `events/EventPanel.java`: 1/550 e 450/550, início por posição, cooldown+3000 ms e callbacks, paradas sem resgate, limpeza e restaurar ajustes.
- `Control.java`, `SettingsBackupRules.java`: preferências `eternatusWithoutVgp` e `eternatusTimedAdvance`; importação/exportação.
- `pgrazor/native/quick_spin.cpp`, `pgrazor/native/map_auto_throw_controller.inc`, `pgrazor/native/research.cpp`: o modo sem VGP somente usa recursos existentes. **Quick Spin depende da interação com a PokéParada e map throw depende do toque em um Pokémon. Não afirmar giro/captura totalmente autônomos.**
- `pgrazor/test/CooldownVisualTest.java`, `EternatusRouteRunnerTest.java`, `test_eternatus_route_java.py`, `LocalTimeClockTest.java`, `SettingsBackupRulesTest.java`.
- `docs/releases/v0.00.13-LT.md`: comportamento, garantia e limitações.

## Evidências exigidas
Relatar separadamente implementado, testes JVM, compilação Android, homologação em aparelho e release publicada. Incluir arquivo:linha, causa raiz, diff e riscos. Nunca contar doces de stop por tempo. Não modificar a identidade do treinador na Unity sem hook verificado. Não mudar o PokeSearch. Não expor segredos, APKM privado ou chave da assinatura.

## Links e fontes
- Repositório: https://github.com/LucianoToscano/PGBR
- Biblioteca de efeitos: https://drive.google.com/drive/folders/1TPgVIGAWkY2Ez1VS3bHAitv9Y_NpczbK
- Evento 2026: https://pokemongo.com/en/news/dynamax-trails-eternamax-eternatus-2026
