# Matriz de homologação LT046 — candidata

Base: LT045 publicada (merge #163, 205ac2105a45dac4821c26b9ff73e66c4097e76a). PR #164. Não confundir aprovação de CI com homologação física. Nenhum teste abaixo marcado como executado sem evidência.

## Android: execução manual em ambiente autorizado

Registrar versão/commit e hash do pacote; modelo, Android, variante, data e resultado. Não incluir nome real, credenciais, conta, coordenadas ou identificadores em logs compartilhados.

| Caso | Resultado esperado | Root | Sem Root |
|---|---|---|---|
| Instalação/abertura | Interface acessível, versão correta, sem crash | Pendente | Pendente |
| Tema claro/escuro | Texto e controles legíveis, ícones originais | Pendente | Pendente |
| Telas pequenas/rotação | Diálogos roláveis e botões acessíveis | Pendente | Pendente |
| Prévia individual/lote | Dados ausentes preservam por incerteza; não transfere | Pendente | Pendente |
| Importação inválida | Linha e motivo; colagem grande rejeitada sem truncar | Pendente | Pendente |
| Etiquetas | Editar observação invalida completude; desconhecido não vira falso | Pendente | Pendente |
| Frutas e preparação Max | Dados manuais, sem gasto de itens ou ações | Pendente | Pendente |
| Simulador PvP/PvE | Resultado inválido ao editar; uma ou duas opções, sem execução | Pendente | Pendente |
| Diagnóstico | Exportação preserva anterior, ZIP filtrado sem dados pessoais; resumo não prova execução | Pendente | Pendente |
| GPX | Desenho e progresso manual, sem alterar localização; limite e erro claros | Pendente | Pendente |
| Ícones | Comparação visual por ID de catálogo e fallback legível | Pendente | Pendente |
| Privacidade | Conferir nas telas autorizadas se a máscara cobre o nome e se é reversível; não presumir proteção em live | Pendente | Pendente |
| Desempenho | Medir memória/bateria com protocolo reproduzível, não inferir de limites de cache | Pendente | Pendente |

## Bloqueios separados

- Comportamento observado PvP/VGP+, telas externas e modo streamer: evidências reais autorizadas; não declarar resolução com simulador, metadados ou testes host.
- Fontes externas, gateway e IA: contratos, consentimento, armazenamento seguro e testes isolados. Não incluir credenciais no repositório.
- GeoLab: artefato de pesquisa separado, não faz parte da distribuição oficial; teste físico pendente.

## Entrega

1. Confirmar HEAD e base, reconciliar PRs paralelas, preservar histórico e artefatos existentes.
2. Validar os workflows aplicáveis ao HEAD exato, sem contornar proteções.
3. Preparar VERSION, changelog e documentação da release candidata; não anunciar publicação antes de existir.
4. Somente em entrega autorizada, verificar build, assinatura, SHA-256 e links reais de cada variante; manter status QA se aparelho não homologado.
5. Registrar evidências e impedimentos no Mestre canônico.
