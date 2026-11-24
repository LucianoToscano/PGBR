# LT048 — matriz incremental de homologação

Complementa [LT046](LT046_HOMOLOGACAO.md). Usar ambiente autorizado e dados sintéticos ou cópia de teste. Registrar HEAD, hash, variante, Android, tamanho, fonte, resultado e evidência sanitizada. Todos os casos físicos pendentes.

| ID | Cenário | Aceite | Root | Sem Root |
|---|---|---|---|---|
| QA48-01 | Filtros, rotação, fonte grande | Mapa útil, rolagem e atribuição legível | Pendente | Pendente |
| QA48-02 | Pan, pinch, cancelamento, reabertura | Gestos e estado coerentes | Pendente | Pendente |
| QA48-03 | Cache de teste, falha de gravação | Entrada anterior preservada | Pendente | Pendente |
| QA48-04 | GPX Unicode multissegmento | Rotas e ordem preservadas | Pendente | Pendente |
| QA48-05 | GPX estranho/corrompido/link externo | Rejeição sem afetar outras rotas | Pendente | Pendente |
| QA48-06 | Estatísticas sintéticas, restauração | Interpolação e cobertura corretas | Pendente | Pendente |
| QA48-07 | Índice sintético inválido | Erro sem reinicializar dados | Pendente | Pendente |
| QA48-08 | Tema e acessibilidade | Contraste, foco e dimensões legíveis | Pendente | Pendente |
| QA48-09 | Clarão Rocket, depois PvP/treino | Evidência visual sem inferir causalidade | Pendente | Pendente |
| QA48-10 | Identidade do pacote | Versão, SHA, assinatura e proveniência conferidos | Pendente | Pendente |
| QA48-11 | Diagnóstico filtrado denso | Resumo abre arquivo, omissões contabilizadas, sem texto privado | Pendente | Pendente |
| QA48-12 | Dois diálogos de Locais | Alterações locais concorrentes preservadas | Pendente | Pendente |
| QA48-13 | Origem de Locais corrompida acima do limite | Erro claro e origem intacta | Pendente | Pendente |

CI do SHA exato, compilação autorizada e recibos de integridade antes de entrega. Manter `device_tested=false` até evidência física. Registrar no MESTRE.md.
