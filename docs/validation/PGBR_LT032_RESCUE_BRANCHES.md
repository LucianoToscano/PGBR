# Resgate dos diagnósticos de branches antigas (LT032)

Investigação em 21/11/2026: a branch `feature/lt017-vgp-diagnostico-rapido-20261120` possui um `VgpHealthReport.java` que não entrou na LT031. A versão original dependia de `stopOnCooldown`, incompatível com a VGP atual (modo manual sem cooldown estimado). Sua função segura foi reimplementada como relatório local, read-only, acionado por botão em `SettingsPanel`. O painel não inicia automação, não envia dados ou coordenadas e não dá garantia de captura/giro. O teste JVM independente acompanha o código.

A branch antiga `investigation/lt025-feed-upstreams-20261121` também contém `tools/feed_provider_probe_lt025.py` e um workflow de diagnóstico de fontes; **não** deve ser tratado como integração de API PGSharp/Pokémod, pois não fornece acesso autorizado a seus servidores. Mantida no histórico para eventual uso sem copiar contratos não validados.

Estado: branch de revisão não integrada na compilação LT032 original; executar suíte e homologação antes de promovê-la.
