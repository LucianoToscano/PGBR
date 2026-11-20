# Modo anônimo — protótipo visual do nome de treinador

A função é opcional e começa **desativada**.

Em **Exibição e desempenho → Privacidade do treinador**, habilite **Proteger nome no mapa (visual)**. O PGBR cria um apelido aleatório local no formato `PGBR_Treinador0000` e exibe uma tarja opaca no canto inferior do mapa; arraste-a até encobrir inteiramente o nome real do treinador. **Gerar novo apelido** troca os quatro dígitos. Desativar a opção remove a tarja e deixa o HUD original à vista.

O apelido e a preferência são salvos no arquivo de configurações da conta; o gerador não lê nem grava a identidade do jogador. Nenhuma chamada da rede, `PlayerService`, perfil ou operação de conta foi modificada. A tarja deliberadamente não é ocultada por **Ocultar sobreposições**, pois isso revelaria o nome sob ela.

## Limitações e validação pendente

Isto **não** é uma alteração do nome dentro da interface Unity. É uma máscara Android posicional e, portanto, pode não cobrir corretamente em outras resoluções ou após atualizações visuais; o usuário deve alinhar a tarja. A tela de perfil, lista de amigos, Reides e outras superfícies podem revelar o nome real. Não afirmar que é equivalente ao Incognito do Pokemod, nem que protege vídeos ou capturas sem inspeção.

Para ampliar a outras telas, criar integração dedicada com o caminho de **renderização** do nome, nunca substituindo os identificadores do jogador nem alterando as respostas da rede, e testar no Android. Não oferecer um recurso que mude o nome real da conta.

A branch principal `main` e o PokeSearch foram preservados.
