# Auditoria de preservação — PT-BR e GPS pós-LT043

## Método
- Inspecionado `main` e as árvores-base/HEAD dos PRs #101 e #109; somente 3 arquivos de tradução divergiam do merge-base por 1 linha nova de tratamento de erro de backup, inserida e mantida no conteúdo do PR.
- Demais fontes não conflitantes recuperadas sem tocar em `Control.java`, `SettingsPanel.java`, `rocket_combat.cpp`, Root hooks ou `SettingsBackupRules.java`.
- `GameLocationDelivery` passa a checar finitude e faixas antes de entregar fix sintético; invalidade libera callback do provedor original. Sem alteração da política de movimento/autorização.

## Matriz de testes antes de merge
| Tipo | Verificação | Estado inicial |
|---|---|---|
| Conteúdo PT-BR | JSON parse, mapa PtBr/RootPtBr com chaves idênticas e novas mensagens de rollback | Três caminhos reconciliados sem remoção |
| Java/JVM | PtBrDynamicTest, GameLocationDeliveryTest e tools/check.py | A executar em CI |
| Root e NDK | Scripts instalador, compilação Java/Root e C++ | A executar em CI |
| APK | Aplicativo sem Root, Root e Zygisk | Requer workflow QA específico |
| Dispositivo | Teleporte, callbacks reais, tela e GPS, localização durante pausa | Não testado (device_tested=false) |

**Fora deste escopo:** PR #135 Savant/Root, Snipers #115/#118/#121, radar #114 e outras PRs antigas. Não alegar incorporação dessas mudanças nem publicar LT044 com versão LT043.
