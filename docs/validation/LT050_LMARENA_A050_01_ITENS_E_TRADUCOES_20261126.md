# LT050 · A-050-01 — itens, ícones e traduções PT-BR (LMArena)

Lote de continuidade do **A-050-01** (issue #320, "PRIORIDADE P0 — feedback físico do proprietário LT049").
Base: `origin/main` = `4ea5bbf` (merge de #351). Branch: `arena/64947e58-pgbr`.

Escopo cumprido: rótulos de item por **ID**, nomes de fruta, tradução de texto que o jogador digita,
e a separação entre *integridade do PNG* e *identidade visual*. **Sem** duplicar #353 (Teleporte, já na
main) nem os módulos offline reservados a outras lanes.

`device_tested=false` · `provider_connected=false` · sem APK novo · sem alegação sobre 0.439.0 · V5 intocado.

---

## 1. O que estava errado, com a prova

### 1.1 Duas frutas trocadas no rótulo da galeria

A galeria de itens (`LocalAuditDialog.galleryLabel`) rotulava **703** como `Fruta Anaba` e **705** como
`Fruta Caxi`. A tabela de itens do protocolo fixa o contrário:

| ID | Enum do protocolo | Nome PT-BR do app |
|---|---|---|
| 701 | `ITEM_RAZZ_BERRY` | Fruta Frambo |
| 702 | `ITEM_BLUK_BERRY` | Fruta Bluk |
| 703 | `ITEM_NANAB_BERRY` | **Fruta Caxí** (banana) |
| 704 | `ITEM_WEPAR_BERRY` | Fruta Wepear |
| 705 | `ITEM_PINAP_BERRY` | **Fruta Abacaxi** (abacaxi) |
| 706 | `ITEM_GOLDEN_RAZZ_BERRY` | Fruta Frambo Dourada |
| 707 | `ITEM_GOLDEN_NANAB_BERRY` | Fruta Caxí Dourada |
| 708 | `ITEM_GOLDEN_PINAP_BERRY` | Fruta Abacaxi Prateada |
| 709 | `ITEM_POFFIN` | Poffin |

Confirmado por três fontes independentes, não por semelhança de imagem:

- `POGOProtos …/Inventory/Item/ItemId.proto` (701 Razz, 702 Bluk, 703 Nanab, 704 Wepear, 705 Pinap);
- tabela pública de itens (`ITEM_GOLDEN_RAZZ_BERRY = 706`, `ITEM_GOLDEN_NANAB_BERRY = 707`,
  `ITEM_GOLDEN_PINAP_BERRY = 708`);
- o próprio dicionário do app já usava `Nanab Berry → Fruta Caxí` e `Pinap Berry → Fruta Abacaxi`.

Os PNGs **concordam**: `item-703` é a penca de bananas e `item-705` é o abacaxi. Ou seja, o texto estava
errado e a imagem estava certa — o inverso do que a suspeita inicial sugeria, e exatamente o motivo de
não se renomear item "pela cara do sprite".

Corrigido também `case 708`, que dizia `Fruta Caxí Prateada` para um abacaxi prateado — mesmo valor errado
que estava gravado em `docs/validation/LT046_ITEM_ICON_EXPANSION.json` (corrigido junto).

### 1.2 Famílias sem rótulo algum

A galeria tem 91 IDs, dos quais 65 tinham nome. Adicionados, por correspondência de ID confirmada:

| Família | IDs rotulados agora |
|---|---|
| Frutas | 702, 704, 707 (além dos 701/703/705/706/708/709 já existentes) |
| MT | 1201 MT de Ataque Rápido, 1202 MT de Ataque Carregado, 1203/1204 Élite |
| Evolução / doce | 1301 Doce Raro |
| Reides / passes | 1401 Passe de Reide, 1402 Passe de Batalha Premium, 1403 Passe de Reide EX, 1404 Pedaço de Estrela, 1406 Medalhão de Equipe, 1408 Passe de Reide Remoto |
| Poké Bolas | 4 Bola Mestra, 5 Bola Premier, 6 Bola Besta (antes em inglês, ao lado de 1/2/3/7 em português) |

Resultado: **83 de 91** IDs com rótulo, contra 65.

### 1.3 "Byte-idêntico" não é identidade visual — o caso `item-1609`

O sprite `item-1609.png` é literalmente a palavra **"TMP"** em vermelho, em um PNG 256×256 válido, e é
**byte-idêntico** à referência `Item_1609.png` do Drive (5 631 bytes, blob `64bd67aa…`). É a demonstração
mais limpa de que integridade de arquivo não diz nada sobre o conteúdo: o arquivo está perfeito e não é
um ícone de item.

Nada foi renomeado por adivinhação. Em vez disso a galeria passou a **declarar a incerteza**:

- `spriteIdentityUnconfirmed(int)` marca 1350, 1351, 1602, 1606, 1607, 1609;
- cada linha da galeria mostra `imagem carregada; identidade visual não confirmada` para esses IDs;
- a linha de inspeção por ID mostra `• identidade visual não confirmada`.

IDs ainda sem nome por falta de referência confiável (**8**): `1350`, `1351`, `1600`, `1601`, `1602`,
`1606`, `1607`, `1609`. Não receberam nome inventado.

### 1.4 Texto do jogador passava pelo tradutor

O tradutor casa por chave exata e, no que sobra, por prefixo. Passar um nome digitado pelo jogador por
ele reescreve o dado:

- `WorldMapDialog` mostrava nome de **local salvo** e de **rota salva** por `label(...)`, que traduz →
  um local chamado "Gym" virava "Ginásio". Agora usa `labelRaw(...)`, que não traduz.
- `GpxRouteDialog` usava `PtBr.tr(entry.name)` no título e `PtBr.tr(current.name)` no campo de nome da
  rota; agora mostra o nome como foi salvo.
- `HotspotsDialog` traduzia `spot.name` — nome próprio de cidade/parque ("PokéPark KANTO", "Auckland").

### 1.5 Resumo de prioridade de fruta meio traduzido

`BerryPriorityPicker` montava o texto como `PtBr.tr(NAMES[i]+" (12)")`. A regra exata só casa a frase
inteira, então **toda** linha com contagem caía nas regras de prefixo. O nome agora vem do ID
(`BerryPriority.displayName`), e o `contentDescription` (TalkBack) é montado com fragmentos traduzidos.
Mesma correção no botão de fruta do encontro (`BerryOverlay`).

### 1.6 Glossário

| Chave | Antes | Depois |
|---|---|---|
| `Use a Golden Razz Berry on bonus catches` | …Frambo dourada… | …Frambo **Dourada**… |
| `Also feed surplus Pinap berries` | …Frutas **Caxi** excedentes | …Frutas **Abacaxi** excedentes |
| `Pinap Berry reserve (0–1000)` | Reserva de Fruta **Caxi** | Reserva de Fruta **Abacaxi** |
| `Nanab Berry reserve (0–1000)` | Reserva de Fruta **Nanab** | Reserva de Fruta **Caxí** |
| `Golden Pinap Berry` | Fruta **Caxí** Dourada | Fruta **Abacaxi** Dourada |
| `PokéStop loaded nearby by the game.` | Poké**Parada** | Poké**parada** |
| `Feeds the gyms you currently defend once…` | "Nanab é usada… Pinap é opcional" | "A Fruta Caxí é usada… A Fruta Abacaxi é opcional" |

`PtBr.java` e `RootPtBr.java` foram regenerados a partir do JSON: **1613/1613**, zero divergência.

---

## 2. Arquivos alterados

```
docs/validation/LT046_ITEM_ICON_EXPANSION.json   rótulo 708 corrigido
docs/validation/LT050_LMARENA_A050_01_…md        este recibo
pgrazor/i18n/pt-BR.json                          6 valores + 6 chaves novas (1613)
pgrazor/src/dev/pgrazor/PtBr.java                regenerado (1613)
pgrazor/root-installer/…/RootPtBr.java           regenerado (1613)
pgrazor/src/dev/pgrazor/BerryPriority.java       displayName(int) por ID
pgrazor/src/dev/pgrazor/BerryPriorityPicker.java display(), summary, rótulos e TalkBack
pgrazor/src/dev/pgrazor/BerryOverlay.java        contentDescription por fragmentos
pgrazor/src/dev/pgrazor/LocalAuditDialog.java     rótulos por ID, notas de identidade, nomes por ID
pgrazor/src/dev/pgrazor/WorldMapDialog.java       labelRaw para dado do jogador
pgrazor/src/dev/pgrazor/GpxRouteDialog.java       nome de rota não traduzido
pgrazor/src/dev/pgrazor/HotspotsDialog.java       nome próprio não traduzido
pgrazor/test/test_item_berry_labels.py            NOVO — 18 testes
pgrazor/test/ItemBerryLabelTest.java              NOVO — executa displayName em JVM
pgrazor/test/test_lt050_visual_labels.py          rótulos fixados por ID (corrige o pin trocado)
```

## 3. Verificação

| Verificação | Resultado |
|---|---|
| Suíte Python do projeto (`unittest discover`) | **650 testes**, 1 erro ambiental (sem `javac` no contêiner), 87 ignorados por exigirem JDK |
| `python3 tools/check.py` | bloqueado: exige JDK 17+ e Android SDK 36, ausentes no contêiner. Roda na CI |
| `test_item_berry_labels` | 18 testes, todos passam (1 skipping por JDK) |
| `PtBr` × `RootPtBr` × JSON | 1613/1613, 0 faltando, 0 sobrando, 0 divergentes |
| Duplicatas no dicionário | 0 |
| Sintaxe Java (`javalang`) | OK em todos os arquivos alterados |
| Rótulos da galeria | 83/91; 8 sem referência confiável, marcados como incertos |

O job **"Suite real check.py + compilacao Java + Root"** da CI é a prova de compilação exigida; este
recibo não afirma compilação local.

## 4. Limites mantidos

- `device_tested=false`; nenhum teste em aparelho.
- Nenhum APK/ZIP novo, nenhuma alegação sobre 0.439.0 ou sobre o APKM original.
- V5, preferências, nomes personalizados, funcionalidades anteriores e proteções existentes intactos:
  as mudanças são rótulo de tela, acessibilidade e não-tradução de dado do jogador.
- Nenhuma ação automática nova no jogo; nenhum módulo offline de outra lane foi tocado.
- Não duplica #353: as quatro frases de Teleporte já vieram da main (`4ea5bbf`).
