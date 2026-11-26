# Auditoria Offline P0 — Identidade Visual e Proveniência de Ícones (LT050)

## 1. Diferença Crítica: Integridade PNG vs. Identidade Visual

Validar que um arquivo PNG possui assinatura, `IHDR`, `IDAT`, `IEND` e `CRC32` íntegros (`png_integrity_ok = True`) **não comprova** que a imagem corresponde visualmente ao item exibido ao usuário nem que o ID do enum coincide com o runtime atual (`0.439.0`).

O auditor offline `tools/pgbr_visual_identity_audit/` lê, sem mutação, os 91 arquivos `pgrazor/assets/item-icons/item-*.png`, os 2 ícones nomeados de expansão (`stardust.png` e `daily-adventure-incense.png`), os manifestos `docs/validation/LT046_ITEM_ICON_REFERENCE.json` e `docs/validation/LT046_ITEM_ICON_EXPANSION.json`, `pgrazor/assets/map-icons/provenance.json` e `LocalAuditDialog.java`.

## 2. Achados Objetivos no Repositório (`origin/main`)

1. **Sobreposição e hash obsoleto em `id=401` (`item-401.png` — Incenso)**:
   - Presente simultaneamente em `LT046_ITEM_ICON_REFERENCE.json` (`git_blob_sha1 = f0668003...`, fonte `Item_0401.png`) e em `LT046_ITEM_ICON_EXPANSION.json` (`git_blob_sha1 = 4eada7fb...`, fonte `incense.png`).
   - O arquivo em disco corresponde ao manifesto de expansão; o registro em `REFERENCE` está obsoleto.
2. **26 IDs sem referência semântica nomeada (`numeric_ref_only_unverified_semantic_identity`)**:
   - Comparados apenas contra nomes numéricos opacos (`Item_XXXX.png`) em `LT046_ITEM_ICON_REFERENCE.json`, sem `drive_named_reference`:
     - **Frutas (`701..707`)**: 7 IDs sem referência nomeada (sendo `702`, `704` e `707` sem rótulo explícito em `LocalAuditDialog.java`).
     - **MTs (`1201..1204`)**: 4 IDs sem referência nomeada.
     - **Outros (`1301`, `1350`, `1351`, `1401..1404`, `1406`, `1408`, `1600..1602`, `1606`, `1607`, `1609`)**: 15 IDs sem referência nomeada.
3. **7 IDs com bytes distintos da referência nomeada (`visual_match_distinct_bytes_unverified_runtime`)**:
   - **Poções (`101..104`)**, **Reviver (`201..202`)** e **Ovo da Sorte (`301`)**: bytes em disco diferem da referência nomeada do Drive.
4. **55 IDs de expansão baseados em enum histórico (`expansion_historical_enum_unverified_runtime`)**:
   - Incluem **Poké Bolas (`4..7`)**, **Incensos (`401`, `405..407`)**, **Módulos Atrair (`501..506`)**, **Frutas (`708..709`)** e **Itens de Evolução (`1101..1107`, `1150..1155`)**.
   - O próprio manifesto declara escopo de enum histórico público, **sem verificação no runtime 0.437.0 ou 0.439.0**.
5. **Ícones nomeados ausentes em `pgrazor/assets/map-icons/provenance.json`**:
   - `pgrazor/assets/map-icons/stardust.png` (**Poeira Estelar**) e `pgrazor/assets/map-icons/daily-adventure-incense.png` constam em `LT046_ITEM_ICON_EXPANSION.json["named"]` com `label = null` e não constam em `map-icons/provenance.json`.
6. **Colisões SHA-256 entre IDs distintos**: `0` colisões de bytes idênticos entre os 91 `item-*.png` e 2 nomeados (e o auditor inclui detector contínuo para impedir regressões).

## 3. Limites de Evidência

- Nenhuma imagem foi trocada nem nenhuma correspondência foi inventada.
- `visual_identity_confirmed = False` e `runtime_0439_verified = False` permanecem explícitos até conferência visual e validação contra referência autorizada e teste físico (`device_tested = False`).
