# Recuperação do PNG VGP OFF original

Fonte única: `pgrazor/assets/map-icons/vgp-hd-states.webp`, atlas de três estados 192×576: OFF cinza, ERROR laranja e ON azul.

O PNG fallback `vgp-device-off.png` está truncado, mas o jogo usa prioritariamente o atlas íntegro. O script `tools/repair_vgp_fallback.py` reproduz os pixels RGBA da primeira célula sem recriar arte nem alterar efeitos. `--check` compara pixel a pixel; o teste `test_map_icons_integrity.py` valida CRC, IEND e IDAT/zlib de todos os ícones do mapa, sem dependências de Python adicionais.

O reparo inicial foi concluído uma vez na branch isolada pelo workflow #45000246867, com o commit binário `646876a3c06f87173c40604c88f444a0f3850575`. **A automação de gravação foi desativada**: o workflow versionado agora tem exclusivamente `contents: read` e apenas compara o PNG ao atlas e valida os arquivos. Qualquer novo reparo exige ação explícita e novo PR. Recompilar os APKs Root/sem Root após esta alteração e verificar os novos hashes.

Essa correção de arquivo gráfico não homologa a captura, o giro, nem os estados do VGP+ em Android real.
