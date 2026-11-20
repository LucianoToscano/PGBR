# PGBR — inventário de links externos e política de ocultação

Atualizado em 20/11/2026 para a branch `feature/pt-br-traducao`.

## Links de divulgação ocultos

| Serviço | Endereço identificado | Ocultação |
| --- | --- | --- |
| Discord do projeto de origem | https://discord.gg/9QqfkDSQX3 | Botão do rodapé do aplicativo oculto em `SettingsPanel.java`; não exibir até escolha do administrador. |
| Repositório PGRazor original | https://github.com/jpitty03/PGRazor | Links promocionais removidos da navegação pública do site; **a atribuição MIT permanece** no README e nos créditos. |
| Ko-fi do projeto de origem | https://ko-fi.com/pgrazor | Removido do rodapé padrão das notas de lançamento e das mensagens de anúncio pelo Discord. |

As URLs de divulgação são mantidas **apenas neste inventário** e onde os créditos legais/históricos são necessários. Não restaurar botões públicos sem autorização.

## Serviços operacionais externos (manter ativos)

| Serviço | Endereço / domínio | Utilização |
| --- | --- | --- |
| Feed de Pokémon 100% | https://feed.faustusfoundry.com/pgrazor/hundos.json | Fonte de dados do caçador de Shundo. **Não renomear o caminho `/pgrazor/`** sem atualizar o servidor. |
| LivePokeMap | https://play.livepokemap.com | Consulta de aparições, exige login/assinatura própria. |
| PoGoCoordinates | https://pogocoordinates.com e https://coordinates-api.pokemongopro.com | Busca e liberação de coordenadas de Pokémon. |
| Discord OAuth (PoGoCoordinates) | https://discord.com/login | Login na fonte PoGoCoordinates; não confundir com convite público do projeto original. |
| Google OAuth | https://accounts.google.com/o/oauth2/v2/auth e https://oauth2.googleapis.com/token | Autenticação de contas. |
| Photon / Komoot | https://photon.komoot.io/api/ | Pesquisa de endereços no mapa. |
| OpenStreetMap | https://tile.openstreetmap.org/ | Imagens do mapa; manter atribuição em https://www.openstreetmap.org/copyright. |

Esses endereços **não são botões promocionais**. Desativá-los pode interromper autenticação, busca, feeds ou mapas; permanecem configurados nos clientes correspondentes.

## Infraestrutura do próprio PGBR (manter acessível)

- Repositório: https://github.com/LucianoToscano/PGBR
- Compilação e artefatos: https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml
- Licenças de terceiros: https://github.com/LucianoToscano/PGBR/blob/main/THIRD_PARTY.md

## Fontes e licenças de desenvolvimento (referências, não divulgação no app)

- Frida: https://github.com/frida/frida
- Dobby: https://github.com/jmpews/Dobby
- LSPlant: https://github.com/LSPosed/LSPlant
- DexBuilder: https://github.com/LSPosed/DexBuilder
- XZ Embedded: https://github.com/tukaani-project/xz-embedded
- PokeMiners: https://github.com/PokeMiners/pogo_assets
- PvPoke: https://github.com/pvpoke/pvpoke
- PokeAPI sprites: https://github.com/PokeAPI/sprites
- Guava: https://github.com/google/guava
- Smali: https://github.com/JesusFreke/smali

## Regras de segurança

1. Não traduzir nem renomear pacotes internos `dev.pgrazor`, diretórios `pgrazor/`, segredos `PGRAZOR_*`, símbolos nativos ou o identificador do módulo Root sem migração específica.
2. Manter os créditos legais ao PGRazor original, apesar de ocultar seus links promocionais.
3. Manter as integrações e links de autenticação em funcionamento. Ocultação se aplica a divulgação pública, não a recursos.
4. Não publicar capturas de tela antigas como se fossem da versão atual do PGBR.
5. Revalidar as telas e compilações antes de declarar tradução/branding integral concluídos.

> Este é o inventário de destinos encontrados nos componentes de interface, documentação principal e integrações inspecionadas nesta rodada; outros caminhos gerados dinamicamente ou documentos históricos podem conter URLs adicionais.
