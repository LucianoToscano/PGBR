# PGBR GeoLab v0.1.1 (pesquisa isolada)
Este subprojeto Gradle é um laboratório de transporte JNI, JSON estrito, malha sintética, predicados com fila limitada, callbacks e POST local. Não integra o runtime de produção, não intercepta tráfego do jogo, não usa Zygisk e não altera o GPS. O dispatcher aceita apenas fixtures e o simulador escuta exclusivamente loopback. DriveConfig lê regras JSON via HTTPS e OAuth explícito, sem persistir token; configuração remota não pode alterar destino nem fornecer comandos.
## Teste
Na pasta deste projeto: bash tests/run-host.sh (Linux, JDK 17, g++). A partir da raiz: bash pgrazor/research/geolab/tests/run-host.sh.
## Android
Gradle 8.11.1, AGP 8.10.0, SDK 36, NDK 27.3.13750724, CMake 3.22.1. Execute gradle --no-daemon :app:assembleDebug nesta pasta. APK separado dev.pgbr.geolab; não substitui o PGBR oficial. CI próprio verifica assinatura debug e SHA-256. Não há publicação automática.
## Evidência
O pacote local anterior v0.1.1 passou em 24/11/2026: geração+filtro 629307/s (20000), JNI+JSON+filtro 98424/s (10000, 788891 bytes). São medições host, não Android. O código importado deve passar CI do próprio HEAD antes de qualquer aprovação. Drive real e dispositivo não homologados.

## Correção de CI — 24/11/2026
O run 45339773695 falhou com código 127 (`sdkmanager: command not found`) antes da compilação Android. O workflow passa a preparar o SDK com a mesma ação fixada por SHA da main, instalar e verificar os componentes antes de `assembleDebug`. Não altera fonte do laboratório ou APK oficial. Novo CI a confirmar; homologação física continua pendente.

- Segunda revisão: run 45345176318/job 138277893433 encontrou o sdkmanager, mas a configuração padrão da ação solicitou o pacote legado `tools`, indisponível. O parâmetro `packages: platform-tools` replica exatamente os jobs aprovados da main; SDK, NDK e CMake continuam obrigatórios, com as mesmas versões e verificações. CI novo pendente.
