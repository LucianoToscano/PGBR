# Recuperação do PNG VGP OFF original

Fonte única: `pgrazor/assets/map-icons/vgp-hd-states.webp`, atlas de três estados 192×576: OFF cinza, ERROR laranja e ON azul.

O PNG fallback `vgp-device-off.png` está truncado, mas o jogo usa prioritariamente o atlas íntegro. O script `tools/repair_vgp_fallback.py` reproduz os pixels RGBA da primeira célula sem recriar arte nem alterar efeitos. `--check` compara pixel a pixel; o teste `test_map_icons_integrity.py` valida CRC, IEND e IDAT/zlib de todos os ícones do mapa, sem dependências de Python adicionais.

Um workflow com permissão limitada de gravação na branch isolada atualiza **somente** o arquivo PNG reparado, depois de executar verificações. O commit do bot não inicia um novo CI automaticamente; revisar o arquivo no PR e compilar novamente o APK Root/sem Root.

Essa correção de arquivo gráfico não homologa a captura, o giro, nem os estados do VGP+ em Android real.
