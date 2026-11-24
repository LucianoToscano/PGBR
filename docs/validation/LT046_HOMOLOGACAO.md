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

## Casos adicionais para a sessão única de homologação

Todos pendentes; verificar apenas em ambiente autorizado. Registrar observação, resultado esperado/real e evidência sanitizada. Não usar a coleção valiosa para ensaios destrutivos.

| Caso | Critério de aceitação | Root | Sem Root |
|---|---|---|---|
| Etiquetas e tempo de carregamento | Medir início/fim, timeout e recuperação; não confundir catálogo manual com carregamento real | Pendente | Pendente |
| Alteração de filtros/evento | Validar positivos, negativos, desconhecidos e reavaliação, sem perda de coleção | Pendente | Pendente |
| Proteções | Verificar cada critério isolado e combinações contraditórias; ausência de evidência não autoriza ação | Pendente | Pendente |
| PvP e lifecycle | Observar estado após abrir/fechar menus, mudança de tela e suspensão; registrar falhas e tela branca | Pendente | Pendente |
| Diagnóstico de ataques | Registrar actor-unknown, um/dois carregados e discrepâncias de eficácia sem inferir execução | Pendente | Pendente |
| Max, frutas e MT | Distinguir preparação manual de recurso funcional; conferir disponibilidade e alternativas sem gasto de teste | Pendente | Pendente |
| Fortalecimento | Conferir apresentação de IV/nível antes e depois de alteração confirmada | Pendente | Pendente |
| GPX multissegmento | Nomes longos mantêm sufixos distintos, limite de 80 caracteres e Unicode válido; progresso preservado | Pendente | Pendente |
| Ícones adicionais | Confirmar ID e identidade visual, sem adivinhar por semelhança | Pendente | Pendente |

## Evidências de integração acumuladas (24/11/2026)

Base consolidada ed3684a7d24f98d3154b8cd001ea7f78cf0ed730; os HEADs exatos dos PRs tiveram CI aprovado. Não substitui a homologação física.

| PR | Escopo validado | Workflows |
|---|---|---|
| #166 | BOM e cancelamento no diagnóstico; distância GPX | 45356941370, 45356941320 |
| #167 | Sufixos e Unicode GPX; matriz QA | 45357357962, 45357357907 |
| #168 | Envelope do cofre local e limites de leitura | 45368218108, 45368218049 |
| #169 | Parser e critérios de proteção locais | 45370546529, 45370546494 |
| #170 | Prévia em lote em worker cancelável | 45371840759, 45371840647 |
| #171 | Tipos e nomes de novos arquivos de migração | 45372949115, 45372949324, 45372949127 |
| #172 | Nomes XML e tamanho de exportação GPX | 45373080938, 45373081108 |

Complementar a sessão física com: fechar/reabrir cofre durante consulta/salvamento/remoção, edição bloqueada enquanto ocupado, confirmação protegida, chave não exibida nem enviada; fechar/reabrir a prévia em lote durante avaliação; exportação GPX excessiva deve falhar sem publicar arquivo inválido. Nenhuma dessas observações foi executada no aparelho.

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
