# PGBR — Opções do ícone flutuante VGP (LT005)

O botão do GO Plus virtual mantém o alvo de toque original de **54 dp** e o texto de estado **ON/OFF**. A arte foi reduzida para **28–30 dp**, centralizada, para ficar visualmente proporcional aos outros botões no mapa.

Em **GO Plus, Poképaradas e itens**, escolha **Ícone do VGP**:
- Original — mantém os ícones conectado/desconectado usados anteriormente.
- Pokébola simples — o desenho azul e rosa fornecido pelo usuário.
- Pokébola brilhante — desenho vermelho com contorno azul; escolha padrão.
- GO Plus — desenho com alça, verde e vermelho.

A escolha fica salva em `vgpIconStyle`; não muda o estado real do VGP, rede, controles de captura nem joystick. Com o VGP desligado, a arte personalizada fica mais discreta e o texto mostra `OFF`.

As imagens `vgp-custom-*.png` são incluídas pelo mesmo empacotador `map_icon_assets()` já utilizado nas versões sem Root e Root.

**Verificação pendente:** inspecionar no Android a legibilidade do desenho após redução e o comportamento ON/OFF em cada estilo. Não modificar PokeSearch ou a branch `main`.
