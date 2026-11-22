# Sniper 1 e Sniper 2 — dois filtros independentes no PGBR (22/11/2026)

## Pedido

Separar e usar no PGBR **a ferramenta de filtros Snipe 1 e Snipe 2** que o cliente PGSharp oferece,
conforme o material de referência do proprietário (vídeo 12 "PgSharp Sniper 1 e Sniper 2", vídeo 21
"PgSharp Sniper Liberado" e as capturas de configuração de filtros).

## O que foi feito

Dois slots de filtro **independentes** sobre a fonte 100% IV que o PGBR **já** lê
(`ShundoSniper.FEED_URL`), com uma requisição servindo os dois e cada slot filtrando por conta própria:

| Peça | Arquivo | Papel |
|---|---|---|
| Modelo puro | `pgrazor/src/dev/pgrazor/SnipeFilters.java` | Filtro imutável de UM slot: ligado/desligado, espécies, IV mín/máx, nível mín/máx, forma exata, tempo mínimo restante. Sem Android: roda em JVM. |
| Editor | `pgrazor/src/dev/pgrazor/SnipeFilterDialog.java` | Edita um slot, valida o texto, grava **somente a chave daquele slot**. |
| Painel | `pgrazor/src/dev/pgrazor/SnipeSlotsDialog.java` | Mostra Sniper 1 e Sniper 2 lado a lado, cada um com sua lista, contagem e resumo. |
| Menu | `pgrazor/src/dev/pgrazor/FeedChooser.java` | Nova opção de índice **6**; os índices 0–5 permanecem exatamente como estavam. |
| Dado | `pgrazor/src/dev/pgrazor/HundoFeedData.java` | Passa a expor `iv` (`FEED_CLAIMED_IV = 100`) na linha decodificada. Nenhuma validação anterior mudou. |
| Testes | `pgrazor/test/SnipeFiltersTest.java`, `pgrazor/test/test_snipe_slots_contract.py` | Comportamento do filtro em JVM e contratos estruturais. |

## Independência entre os slots

- Chaves separadas em `pgrazor-snipe-filters`: `sniper1` e `sniper2`.
- O editor de um slot grava **apenas** a sua chave; o painel lê cada chave separadamente.
- Um valor ausente, corrompido ou de formato estranho derruba **só aquele** slot para o padrão
  (`SnipeFilters.containsAnyKey` + `catch (JSONException)`), nunca copia o outro slot.
- `withEnabled` devolve cópia: ligar/desligar um slot não altera o outro.

## Regras de honestidade dos dados

- "100% IV" é **declaração da fonte**, herdada do esquema `{"v":1,"hundos":[...]}`; **não** é IV lido
  de um encontro no jogo. O valor agora fica explícito em `HundoFeedData.Entry.iv`.
- Valor que a fonte **não** informou (`iv = -1`, nível `NaN`, forma `-1`) **nunca** satisfaz um limite:
  o filtro fecha. Não existe "completar" IV, nível, CP ou forma.
- Padrões são amplos/desligados: slot novo não esconde nada porque não recebeu ordem para esconder.
- Nada de spawn inventado, resultado expirado apresentado como vivo, ou contagem que não veio da fonte.

## Limites operacionais preservados

- **Nenhuma** chamada a `f.pgsharp.com`, login, key, protocolo ou APK do PGSharp. Sem engenharia
  reversa nova: a pasta do Drive (`libmain.so` 1.278.0, `patches/`) é referência do que o cliente faz,
  não foi copiada nem portada.
- **Uma** requisição automática ao abrir o painel, mais uma por toque em Atualizar, com **mínimo de
  30 s** entre toques; HTTP 401/403/429 bloqueiam por 5 min; corpo limitado a 8 MB; sem autorretry e
  sem polling.
- Atualização de contagem/expiração a cada 10 s é **local**, sem rede (o tick só redesenha).
- Selecionar um alvo exige snapshot fresco e alvo vivo; teleporte só via `TeleportDialog.confirm` +
  `Control.canEnable()`, com confirmação explícita. Nada de captura automática.

## Como testar

```bash
python -B -m unittest discover -s pgrazor/test -p 'test_snipe_slots_contract.py' -v
python -B -m unittest discover -s pgrazor/test -p 'test_live_feed_java.py' -v   # exige JDK + fontes org.json do SDK
```

`test_live_feed_java.py` agora compila e executa também `SnipeFiltersTest`. O contrato do leitor manual
anterior (`test_hundo_manual_ui_contract.py`) continua passando, incluindo a exigência de `which == 4`.

## Estado factual

- **Implementado em fonte**, com testes estruturais executados localmente e aprovados.
- **Não executado nesta sessão:** compilação Java completa, `tools/check.py`, build Android, APK,
  instalação e uso em aparelho. O sandbox desta sessão tinha apenas JRE (sem `javac`) e sem SDK Android.
- **`device_tested=false`.** Nenhuma release, tag ou APK foi gerada por este trabalho.
- O filtro de IV só tem efeito real quando a fonte declara o IV; para uma fonte sem IV, use espécie,
  nível, forma e tempo restante. Isso é limitação de dado, não bug de interface.
