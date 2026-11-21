# PGBR LT028 — PvP Battle Assistant

Status em 21/11/2026: UI, persistência, política de decisões e testes locais implementados; **automatização dentro da batalha ainda não conectada ao jogo**.

## Referências recebidas
- PGSharp 1.277.2 (Pokémon GO 0.435.2) contém as strings `battleAssistant`, `pvpCombatStats`, `rocket.fast`, `rocket.charge` e `rocket.shield` em `libmain.so`.
- O usuário confirma que o PGSharp tem modo de batalha PvP. A página pública de funcionalidades não lista explicitamente todos os seus submenus; isso **não** refuta o recurso. Há demonstração de um submenu Auto Battle na versão 1.264.3 (vídeo de julho/2026).
- As strings por si só não validam o código executável de controle PvP. Nunca redirecionar hooks Rocket para PvP sem validação de estado.

## Implementação LT028
- Três preferências: ataque ágil, carregado, escudo; configuradas e persistidas separadamente de Rocket/Ginásios/Reides.
- `PvpBattleMode` isola bits e exige permissões/runtime operante.
- `PvpBattleController` é política pura, sem hooks ou toques: usa identidade de sessão, modal de escudo, movimento carregado disponível e solicitação de próximo Pokémon; impede acionamento repetitivo e recusa observações antigas.
- Namespace exclusivo de identificação: `MODE_PVP=0x505650` para não colidir com os modos nativos de Ginásios/Reides/Dynamax.
- Na ausência de adaptador validado, `pvpBattleReady=false` e os controles na tela ficam desabilitados.
- `build.py --test-only` inclui os testes unitários. `test_control_java.py` inclui `PvpBattleMode.java` entre as dependências.

## Pendências para ter jogo automático
1. Mapear com segurança nas versões atuais do jogo o estado de combate PvP (incluindo desafio de treinador e GBL), objeto e ciclo de vida.
2. Confirmar métodos/assinaturas corretos para ataque rápido, carregado, escudo e seleção de Pokémon na versão-alvo; não copiar offsets de builds antigos.
3. Criar o adaptador com validação de classe/versão e execução no thread Unity; nenhuma ação deve ser enviada caso os dados estejam incompletos.
4. Implementar gateway PvP independente da ponte Rocket/PvE e adicionar testes de integração; qualquer mudança no ABI exige atualização coordenada do host/nativo e da suíte.
5. Validar no dispositivo, em combate real, antes de marcar como pronto ou publicar como automação funcional.

Fora de escopo: afirmar integração direta à API interna do PGSharp ou uso das mesmas rotinas binárias sem interface oficialmente documentada/verificada.
