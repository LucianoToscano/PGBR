# Auditoria detalhada de integração LT043 — 23/11/2026

Base main LT042 f78d0e2, Rocket/Max/HUD PRs #136/#140 (HEAD 77cc0772) e importação local PGSharp PRs #129/#138 (HEAD c046a1f5). Histórico preservado via commit com dois pais.

## Áreas cobertas
| Área | Inclusão revisada | Verificação ainda necessária |
|---|---|---|
| Rocket | Recuperação QA6, escolha conservadora de carregados quando metadata inválida | Testes reais de KO, troca, escudo, tipos, Charge2 |
| Batalha Max | Sessão voluntária, gate Max, limpeza de sessão | Android e mudança de conta/background |
| Mapas e HUD | Duplo toque confirmado, painel IV e badge | Testar mapa e Pokédex em aparelho |
| Dados PGSharp | Importação JSON pessoal e rotas, 26 legendas 0,0 excluídas dos destinos | Backup/restauração, sem feed Sniper presumido |
| Persistência | Mescladas whitelist de mapTapTeleportNoAsk e categoria pgrazor-pgsharp-data-v1 | Todos os testes Java de backup e perfil |
| Outros paralelos | #135, #115/#121/#118, #114, #109, #101 mantidos sem alteração | Integração posterior, rebase e CI individual |

## Não declarar homologação antes dos testes
Esta integração não inclui automaticamente Snipers conectados nem recursos de outras PRs antigas; não altera LT042 e não contém o arquivo pessoal .dat. device_tested=false. A conclusão e os hashes do CI serão registrados no Mestre após verificação.
