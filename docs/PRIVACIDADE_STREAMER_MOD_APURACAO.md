# Investigação: Modo Streamer do Pokémon GO e privacidade do nome no PGBR

## Evidência analisada
Arquivo fornecido pelo usuário: `arquivos_apk_pgsharp.zip` (contendo a extração parcial do cliente **Pokémon GO 0.435.2**, identificada em `LEIAME.txt`). O arquivo contém `global-metadata.dat` IL2CPP v39, DEX, recursos e manifesto, **sem `libil2cpp.so`**. Os metadados mostram os identificadores:

- Namespace: `Niantic.Holoholo.StreamerMode`
- `StreamerModeService`, `IStreamerModeService`
- `get_IsActive`, `get_IsHiddenInfoPanelEnabled`, `UpdateStreamerState`
- `OnToggleStreamerMode` (configuração)
- `StreamerOverlayGuiController`, `StreamerModeSettingsProto`, `StreamerModeEnabled`

**Esses nomes não demonstram que o modo oculta o nome do jogador no mapa e no perfil Eu**; `IsHiddenInfoPanelEnabled` pode ser apenas um painel de informações. Outros identificadores como `HideNameplate` e `HomeTrainerName` pertencem a contextos distintos, não são prova de proteção do treinador no mapa.

## Aplicação no PGBR LT014

- Adicionada descoberta **somente leitura** no `trainer_privacy_ui.cpp`, usando a própria camada de resolução de metadados já validada no runtime IL2CPP da versão executada.
- A descoberta não supõe offsets, RVAs ou prólogos da versão 0.435.2. Somente testa a existência da classe e valida assinaturas dos getters de estado. **Não chama getters, não ativa o modo do jogo, não altera opções, RPC, serviços de identidade ou servidor**.
- O painel de privacidade exibe três estados possíveis do diagnóstico (classe não encontrada, classe encontrada mas assinatura incompatível, ou metadados de classe + getters reconhecidos), além de aguardando leitura. **O estado não significa que a identidade esteja visualmente protegida.**
- A máscara visual experimental de `*****` permanece independente do modo nativo e não é considerada proteção total.

## Verificação orientada no painel

O PGBR agora oferece **Verificar privacidade antes da live** na seção Privacidade.
A ação apresenta uma verificação manual: ativar máscara visual, reabrir mapa, conferir
perfil Eu, desligar e verificar restauração, repetir em conta alternativa se disponível.
O fluxo não lê a tela nem marca a privacidade como validada automaticamente.

A detecção de metadados do serviço StreamerMode foi limitada a uma inspeção por
processo após bootstrap validado, evitando buscar a mesma classe em cada frame de jogo.
O diagnóstico continua independente da máscara e não alterna estado do serviço.

**Limitação:** o gancho de texto existente intercepta setters Unity UI/TMP apenas quando
o valor coincide exatamente com o nome autenticado. Conteúdo já exibido, superfícies de
renderização diferentes e outros contextos podem continuar mostrando o nome real.
Por isso, o resultado de testes de código ou de compilação não certifica privacidade
durante lives.

## Critérios ainda não cumpridos

1. Inspecionar a implementação correspondente ao `libil2cpp.so` **da mesma versão que o APK PGBR distribui** (a entrada de compilação LT014 é 0.437.0, não 0.435.2).
2. Comprovar em testes no aparelho o nome no mapa e no perfil Eu, inclusive textos já renderizados, retorno ao original ao desligar e troca de conta.
3. Não interferir em autenticação, ID, PlayerService, RPC ou qualquer atualização de identidade do jogo.
4. Não oferecer a opção como proteção para live até todos os testes visuais passarem.

Os binários privados fornecidos pelo usuário não foram incluídos no controle de versão.
