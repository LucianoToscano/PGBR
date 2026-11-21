<p align="center"><img src="pgrazor/branding/pgrazor.svg" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
> **Arquivo Mestre Único do PGBR (leitura obrigatória):** [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Ele centraliza a versão auditada mais recente, decisões, pendências e regras de validação. A versão de código nesta branch pode estar desatualizada em relação às branches de integração.

<p align="center"><strong>PGBR-v0.00.01-LT · Edição brasileira para Android</strong></p>
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status:** versão experimental PT-BR. A compilação automatizada passou nos testes; o funcionamento em aparelhos Android e a cobertura integral da tradução ainda precisam ser validados.

## Baixar o PGBR

Acesse **[Compilações do PGBR no GitHub Actions](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-build-full-ptbr-candidate.yml)**, abra a execução mais recente com ✓ e, em **Artifacts**, baixe o pacote `PGBR-v0.00.01-LT-APKs`.

O pacote ZIP contém:
- **`PGBR-v0.00.01-LT.apk`** — instalador principal para Android ARM64, sem Root.
- **`PGBR-v0.00.01-LT-Root.apk`** — instalador para aparelhos com Root.
- **`PGBR-v0.00.01-LT-Root-module.zip`** — módulo Root para Magisk, KernelSU ou APatch.
- Comprovantes de compilação e arquivo `SHA256SUMS.txt`.

É necessário entrar no GitHub para baixar artefatos, que expiram conforme o prazo do workflow. O APK é assinado com a chave própria do PGBR e **não atualiza diretamente** instalações assinadas por outras chaves. Não desinstale sua versão anterior sem salvar os dados necessários.

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
