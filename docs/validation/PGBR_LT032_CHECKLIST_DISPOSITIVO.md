# PGBR LT032 — verificação funcional em aparelho Android

**Não considerar a LT032 estável com base somente no GitHub Actions.** Se o app não abrir, guardar o APK anterior, coletar logcat e preservar credenciais; nunca anexar dados privados ou localização exata em issues públicas.

| Área | Procedimento de teste | Evidência e bloqueio |
| --- | --- | --- |
| Instalação | Testar assinatura, ABI ARM64, abertura do jogo e reinício após permissões | Sem crash; registrar versão, SHA e erro sanitizado |
| VGP | Ligar botão, confirmar permissões, Pokémon/Poképaradas e estado nativo; observar pelo menos uma tentativa real de giro e uma captura | Registrar status/resultado nativo, não apenas toggle verde; se nenhum evento, coletar Registro de execução |
| VGP coexistência | Desativar e reativar com app em primeiro plano; testar com/sem Rocket e em mudança de conta | Não enviar ações após suspensão nem reutilizar alvo expirado |
| PvP | Testar desafios/GBL com opções ágil, carregado e escudo separadas; comparar com Rocket | Provar se ataques/escudos foram executados; se parou, registrar cenário/UI e `pvpExecutorReady` com valores não sensíveis |
| PvP segundo golpe | Alternar preferência supereficaz; conferir em cenário com dois golpes carregados | **Esperado pendente:** botão de segundo golpe não vinculado ao executor; não rotular funcional |
| Busca Pokémon | iFlowGo, RocketMap, LivePokeMap, PokeList quando autorizado; buscar espécies e tocar coordenada válida | Comprovar fonte verdadeira, expiração, HTTP, filtro de espécie, confirmação; nunca inventar feed PGSharp |
| Interface | Conferir ícones de itens, sprites, overlay, cantos e joystick/Pokébola Eternatus | Registrar screenshots sem dados pessoais e nomes de itens conflitantes |
| Privacidade | Ativar/desativar `*****` no mapa e no perfil Eu, relogar, trocar conta | Somente aprovar se ocultar todas as aparições visíveis, sem editar identidade real |
| Regressões | Abrir rotas GPX, cooldown, salvar/restaurar preferências e modo Root anterior | Sem perda de configuração ou permissões |

Se qualquer teste falhar, **preservar a build LT031 e a release LT023** para comparação. Não sobrescrever dados antigos. Testes devem ser feitos em ambiente controlado e respeitar termos dos serviços utilizados.
