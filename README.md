<p align="center"><img src="pgrazor/branding/pgbr-logo.webp" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
<p align="center"><strong>PGBR-v0.00.14-LT · Edição brasileira para Android</strong></p>
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status:** LT014 em validação. A tempestade Eternatus foi corrigida no código; a máscara do nome no Unity é experimental e **não foi comprovada no mapa/perfil Eu**. Não usar essa opção como proteção de privacidade durante lives.

## Baixar o PGBR

A [LT013 permanece disponível no pré-lançamento anterior](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.13-LT) enquanto a LT014 é validada. Não confundir os pacotes.

Acompanhe as **[compilações completas no GitHub Actions](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml)**. Somente após uma execução LT014 bem-sucedida, baixe o artefato `PGBR-v0.00.14-LT-APKs`, que deve conter:
- `PGBR-v0.00.14-LT.apk` — instalador sem Root.
- `PGBR-v0.00.14-LT-Root.apk` — instalador com Root.
- `PGBR-v0.00.14-LT-Root-module.zip` — módulo para Magisk/KernelSU/APatch.
- Comprovantes de compilação e `SHA256SUMS.txt`.

Os artefatos exigem login no GitHub e expiram conforme prazo do workflow. A assinatura do PGBR não necessariamente corresponde à de instalações anteriores. Não desinstale o aplicativo sem preservar seus dados.

## Recursos

O PGBR mantém os recursos da base de código: joystick, teletransporte, caminhada automática, rotas GPX, mapa, filtros, ferramentas de captura, gestão de contas e configurações visuais. Alguns recursos dependem da versão exata do jogo, de serviços externos ou de testes no aparelho.

A interface do aplicativo e do instalador Root está sendo traduzida para **português brasileiro**. Consulte a [auditoria de tradução](docs/PGBR_AUDITORIA_PTBR_LT001_20261120.md); novas telas e mensagens ainda podem conter termos em inglês.

## Desenvolvimento na nuvem

O repositório é **[LucianoToscano/PGBR](https://github.com/LucianoToscano/PGBR)**.

- **`main`**: código público principal e documentação do PGBR.
- **`feature/pt-br-traducao`**: desenvolvimento, testes e compilações PT-BR.
- **[GitHub Actions](https://github.com/LucianoToscano/PGBR/actions)**: compila, testa e assina os instaladores. Não é necessário um computador local para cada atualização.

O pacote original do jogo é mantido fora do controle de versão, como entrada temporária da compilação. As senhas e chaves de assinatura ficam nos Secrets do GitHub Actions; nunca devem ser publicadas no repositório.

## Informações importantes

O PGBR é um projeto experimental independente. O tempo de espera (*cooldown*) é uma estimativa local, e nenhuma alteração de cliente garante resultados no servidor ou proteção contra medidas aplicadas pelo publicador de Pokémon GO.

## Documentação

- [Instalação, assinatura e compilação](docs/BUILDING.md)
- [Compatibilidade e arquitetura](docs/ARCHITECTURE.md)
- [Instalador Root](docs/ROOTED.md)
- [Atualizações do jogo](docs/GAME_UPDATES.md)
- [Auditoria PT-BR](docs/PGBR_AUDITORIA_PTBR_LT001_20261120.md)
- [Licenças e dependências de terceiros](THIRD_PARTY.md)

## Créditos e licença

O PGBR é uma adaptação independente do código aberto do projeto original **[PGRazor](https://github.com/jpitty03/PGRazor)**, disponibilizado sob licença MIT. A atribuição ao autor e os avisos de licença de terceiros são preservados conforme os respectivos termos. Pokémon e Pokémon GO pertencem aos seus titulares. O PGBR não é afiliado à Niantic, à The Pokémon Company nem aos publicadores do jogo.
