# LT048 — P0: roteiro de homologação física (tela branca, Auto ATK, Supereficaz)

Status: **código integrado + CI verde; `device_tested=false`.**
Merge: PR #283 → `4e74063c51f325498a0f8e2caf75794e1ec8de3a`.
Nenhum item abaixo pode ser declarado concluído apenas por CI. Este documento separa o
que já foi provado por código/teste do que só um aparelho pode provar.

## 1. Tela branca no ataque carregado (issues #192, PR #275 consolidada)

O que já está provado (sem Android):

- O coletor do minijogo não escreve mais `Transform.localScale` em nenhum caminho
  (`pgrazor/native/rocket_minigame.cpp`).
- Geometria só é ampliada quando a classe do colisor é exatamente `SphereCollider`
  (`radius`) ou `BoxCollider` (`size`), com limites e recusa de NaN/infinito
  (`pgrazor/native/rocket_minigame_shape.h`, `rocket_minigame_shape_test.cpp`).
- Geometria desconhecida/inválida não produz snapshot: sem escala, sem `SetActive`.

Roteiro no aparelho (Rocket primeiro; depois os modos derivados):

1. Rocket: iniciar batalha, usar o ataque carregado nas três faixas (Boa, Ótima,
   Excelente) e confirmar que a tela **nunca** fica branca em nenhuma delas.
2. Repetir com o auxiliar desligado e ligado, para separar o efeito do recurso.
3. Observar se o clarão aparece também ao **cancelar** o minijogo, ao sair da batalha
   e ao abrir/fechar o menu durante a batalha.
4. Repetir em Liga de Batalha GO, Treinamento com líder NPC, duelo entre amigos e
   batalhas consecutivas (sem reiniciar o app entre elas).
5. Se o branco persistir: capturar logcat do PGBR no instante do carregado, informar a
   faixa (Boa/Ótima/Excelente), o modo e se o ícone de estado do auxiliar está ligado.
   Isso indica um segundo caminho visual, que **não** é o fallback removido.

## 1b. Camada branca no carregado (fix visual de 25/11/2026)

O mecanismo do carregado continua o da PR #297 (`1b1286bc`). O que mudou é **apenas** a aparência: os renderers do objeto de toque são desligados (`Renderer.enabled=false`) enquanto o coletor o amplia 1000×, e cada flag original é reposta ao sair.

Aceite no aparelho:

1. Boa, Ótima e Excelente: a batalha permanece **visível**; nenhuma camada leitosa cobre a tela.
2. O carregado continua **funcionando** (o mesmo número de toques/arrastos de antes).
3. Ao sair do carregado, a cena volta ao normal (nada fica invisível depois).
4. Se ainda houver branco, verificar no diagnóstico `miniWhiteHideBound` e o último `miniStage` com prefixo `white-layer-`:
   - `white-layer-unbound` → o runtime não expôs `Renderer.set_enabled`/`GetComponentsInChildren`;
   - `white-layer-renderers-absent` → nenhum renderer na subárvore do colisor (hipótese: segundo objeto branco);
   - `white-layer-write-failed` → exceção gerenciada ao desligar (o objeto é restaurado e a coleta segue).
5. Reportar aparelho/Android, versão do PGBR, SHA do APK e o `miniStage`, se houver falha.

## 2. Auto ATK para de funcionar após a batalha (issue #105)

O que já está provado (sem Android):

- Corrida de leitura do transporte não revoga mais a política do usuário
  (`pgrazor/native/transport_option_policy.h`): tolerância de 1000 ms, falha fechada
  sem relógio, revogação apenas em persistência.
- Lacuna temporária de verificação não destrói o escopo verificado
  (`pgrazor/native/pvp_executor_policy.h`): `SUSPEND` mantém o escopo ancorado e força
  máscara efetiva 0 (nenhuma ação), `LEAVE` só em fim autoritativo.
- A interface mostra "em espera (motivo)" sem derrubar a prontidão (`Control.java`).

Roteiro no aparelho:

1. Batalha (treino e PvP) com as três ações ligadas; terminar a batalha.
2. Abrir e fechar Configurações/menu durante a batalha e fora dela.
3. Entrar em **nova** batalha sem reiniciar o app: Ágil/Carregado/Escudo devem voltar a
   atuar.
4. Alternar uma ação ON/OFF e ON de novo **durante** a mesma luta: deve retomar sem
   exigir nova entrada.
5. Instalar a versão anterior e repetir para confirmar a diferença (regressão
   reproduzida antes / ausente depois).
6. Se falhar, coletar o Registro de execução e procurar: `pvpScene`,
   `pvpExecutorReady`, `pvpExecutorSuspended`, `pvpCandidateDenied`,
   `battlePolicyRevoked` (nomes e motivos apenas; sem dados pessoais).

## 3. Supereficaz / Ataque Inteligente (issue #90)

O que já está provado (sem Android):

- Tabela 18×18 idêntica nas duas camadas e igual à tabela canônica; Fantasma → Aço
  agora é 0,625× (era neutro). Dado ausente ou tipo inválido nunca resulta em SUPER.

Roteiro no aparelho:

1. Batalha de treino com dois carregados conhecidos (tipos diferentes) e adversário de
   tipo duplo.
2. Confirmar a leitura exibida pelo diagnóstico (`smartAttackEnabled`,
   `classifierBound`, `chargeTier`, `charge2Tier`, `decision`, `reason`) e comparar com
   o que o jogo mostra.
3. Caso adversário com Aço (ex.: Registeel, Melmetal): o golpe Fantasma **não** deve ser
   preferido sobre um golpe neutro ou supereficaz.
4. Confirmar que entrada manual continua funcionando com o recurso ligado.

## 4. PT-BR (release notes)

Revisar visualmente: EventPanel (aplicabilidade das opções), ShundoOverlay (texto de
tela escura), mapa (erro de cache), tela de logs e a mensagem dinâmica "Synthetic
location consumer" em PreviewActivity.

## Regra de registro

Para encerrar qualquer item acima é obrigatório anexar: aparelho/versão Android, versão
do PGBR, SHA do APK de teste, cenário, resultado observado e captura/log sanitizado.
Sem isso permanece `device_tested=false`.
