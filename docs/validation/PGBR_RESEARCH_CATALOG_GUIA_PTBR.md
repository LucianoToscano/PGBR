# Guia PT-BR — Catálogo Offline de Pesquisas e Recompensas (G-049-10 / EVT-02)

## 1. Escopo e Limites de Evidência

O pacote `tools/pgbr_research_catalog/` fornece um catálogo **100% offline** para consulta manual de pesquisas e recompensas nas categorias `campo`, `temporaria`, `especial` e `colecao`.

Limites verificados no repositório local (`inspect_local_research_sources`):
- `pgrazor/src/dev/pgrazor/ResearchPreferences.java` (formato `2`, categorias `i`/`e`/`v`, limite `MAX_TEMPLATES = 300`) armazena apenas tarefas de pesquisa de campo observadas em execução pelo próprio usuário — **não contém base embarcada de pesquisas reais**.
- `pgrazor/assets/map-icons/pokemon-names.json` contém apenas nomes públicos de espécies embarcados, sem tabelas de tarefas ou recompensas.
- **Ausência declarada**: não há dataset público autorizado embarcado de pesquisas neste repositório (`authorized_embedded_research_dataset_present = False`). Nenhum registro real de pesquisa ativa é inventado ou inferido.

## 2. Separação entre Pesquisa Documentada e Disponibilidade Atual

Toda avaliação via `assess_availability(entry, now_utc)` mantém `live_availability_confirmed = False` em modo offline. Uma entrada pode estar:
- `sem_janela_declarada`
- `antes_da_janela`
- `dentro_da_janela_declarada`
- `expirada_por_janela`

Mesmo quando `dentro_da_janela_declarada`, o catálogo informa explicitamente que o modo offline não confirma rotação ativa no jogo.

## 3. Modelo, Condições, Recompensas e Proveniência

- **IDs estáveis**: padrão `^[A-Z0-9][A-Z0-9_.-]{1,95}$`, com rejeição de duplicatas e erro explícito para IDs desconhecidos.
- **Condições separadas de rótulos**: `ResearchCondition(code, declared_label_ptbr, target_count, parameters)` separa o código estruturado do texto traduzido em PT-BR.
- **Recompensas tipadas**: `especie`, `item`, `poeira_estelar`, `xp`, `doce`, `mega_energia` e `desconhecido` (com `is_unknown=True` explícito quando a fonte não documenta a recompensa).
- **Proveniência obrigatória**: `source_name`, `source_type`, `revision`, `valid_from_utc`, `valid_until_utc` (ISO-8601 com fuso explícito), `is_synthetic`.

## 4. Checklist Manual e Reaproveitamento de `ManualChecklist` (0006)

`ResearchManualChecklist` gerencia estados manuais (`pendente`, `em_progresso`, `concluido`, `ignorado`) sem acessar conta ou runtime do jogo. Quando o módulo `tools.pgbr_data_tools.events_planner.ManualChecklist` (lote `0006`) está presente no ambiente, o checklist delega diretamente a ele; caso contrário, utiliza fallback local compatível com o mesmo contrato.

## 5. Importação/Exportação Segura e Sanitização

- Limite rígido de tamanho (`MAX_EXPORT_BYTES = 1_000_000` bytes) e de registros (`MAX_CATALOG_ENTRIES = 1000`).
- Leitura/escrita local com `O_BINARY` (compatibilidade Windows/Linux), `O_NOFOLLOW`, rejeição de symlinks e arquivos não regulares.
- Sanitização recursiva de chaves sensíveis (`token`, `email`, `lat`, `lon`, `trainer_name`, etc.) **e** de valores em texto livre (e-mails, tokens/JWT, coordenadas GPS e caminhos de host `/Users/...`, `/home/...`, `C:\Users\...`).

## 6. Exemplo Sintético de Uso (Python)

```python
from datetime import datetime, timezone
from tools.pgbr_research_catalog import (
    ResearchManualChecklist,
    assess_availability,
    build_synthetic_example_catalog,
)

catalog = build_synthetic_example_catalog()
matches = catalog.search(category="temporária", query="jogadas otimas")
assessment = assess_availability(matches[0], now_utc=datetime(2026, 6, 1, 12, 0, tzinfo=timezone.utc))
assert assessment.live_availability_confirmed is False

checklist = ResearchManualChecklist(catalog)
checklist.track("SYNTH_FIELD_001", window_key="semana-exemplo")
checklist.confirm_state("SYNTH_FIELD_001", "concluido")
```
