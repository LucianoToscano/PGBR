# PGBR — Verificação do contador de tempo de espera

## Como funciona
O botão `Est. 0:00:00` exibe o tempo estimado a partir da distância entre ações registradas e a localização consultada; não é um cronômetro disparado por toda ação. Uma ação na posição atual pode manter zero. Aproximadamente 1 km de distância gera 1 minuto, e viagens longas chegam ao limite de 2 horas.

## Riscos identificados
O código descarta eventos se o aplicativo estiver em segundo plano, a integração não estiver pronta, a conexão tiver expirado ou o encontro não for elegível. O callback nativo que envia uma identificação vazia ao encerrar um encontro apenas limpa o contexto e não deve ser eliminado. Contagem de encontros não equivale a arremessos ou giros.

## Diagnóstico da revisão
- `cooldown-action-recorded`: evento registrado com sucesso; aparece no histórico da conta.
- `cooldown-action-skipped`: evento recebido e ignorado por `background`, `backend-not-ready`, `adapter-lease-expired`, `encounter-ineligible` ou `location-unavailable`. O registro inclui apenas a ação e a causa, sem coordenadas.
- A tela de histórico informa a quantidade e a última ação registrada.
- A tradução das opções de registro manual será exibida sem mudar os identificadores internos persistidos.
- O algoritmo e os bloqueios de segurança não foram afrouxados.

## Verificação em aparelho
Execute um giro ou arremesso válido; abra o histórico e veja se surge uma ação recente. Sem registro, exporte o log do aplicativo e procure as entradas `cooldown-action-skipped` / `cooldown-action-recorded`. Se houver registro, consulte o tempo para um destino distante antes de executar uma nova ação. Só o teste real em aparelho confirma a chegada dos eventos nativos.

Nada foi alterado no PokeSearch ou na branch `main`.
