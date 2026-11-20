# PGBR — ajustes de ícones e privacidade (trabalho parcial pós-LT010)

Base: `feature/eternatus-morph-tempestade-lt010`. Mudanças são incrementais e não alteram fluxo de captura, regras de descarte, identidade de conta, VGP, jogo ou assets de Eternatus.

## Implementado em código

- Histórico de cooldown: botões de copiar, mapa e excluir têm fundo branco e os **mesmos desenhos** recoloridos em verde-escuro. O comportamento funcional permanece.
- Sprites de item: `ItemSpriteFit.centeredRect` mantém proporção sem esticar a imagem para um quadrado; teste portátil de 203×79, 238×161, 172×196 e 256×256.
- Decodificação: só marca como faltante após `FileNotFoundException` em todas as alternativas; uma falha de leitura/decodificação gera `Control.event("item-icon-error", ...)` e não bloqueia para sempre nova tentativa.
- Faltantes: registra `item-icon-missing` com ID exato e usa um indicador numérico neutro em lugar de um símbolo que poderia representar outro item. **Não cria nem adivinha IDs ou imagens.**
- Tradução de nomes de catálogo observados: Golden Pinap Berry, Kings Rock, Up Grade, Gen4 Evolution Stone, Purified Gem e Rare Candy XL.
- Nos menus do próprio PGBR, modo 1 usa `*****` e modo 2 com apelido inválido também oculta em vez de revelar o nome real.

## Ainda pendente

1. Os 37 PNGs existentes não cobrem todos os IDs do catálogo. É necessário comparar IDs obtidos de `Control.itemCatalog()` com sprites licenciados/canônicos. Capturas de tela, sozinhas, não identificam os IDs.
2. Ocultação **do nome dentro do jogo (Unity: mapa / perfil Eu)** não foi implementada. Requer confirmação dos pontos de renderização para a versão 0.437.0. O PGBR não deve anunciar essa proteção como pronta nem comprometer a conta.
3. Compilação Android, testes do conjunto todo, homologação de capturas e publicação de APK ainda não foram feitos nesta branch. A aprovação final requer instalação em Android com e sem Root.
4. A versão LT010 continua intacta; o presente patch não muda `VERSION`, assinatura nem release. A aprovação não é automática.

## Gate

- Executar `python -m unittest discover -s test -p 'test_item_sprite_fit_java.py'` a partir de `pgrazor/`.
- Rodar pipeline completo `tools/check.py`, build dos dois instaladores e verificação de hashes.
- Capturar imagens da janela de itens e do histórico no dispositivo; verificar escala, cores, rolamento, ícones ausentes e contraste.
- Conferir que itens sem checkbox marcada nunca são transferidos e que limites persistem.
