<p align="center"><img src="pgrazor/branding/pgbr-logo.webp" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
> **Arquivo Mestre Único do PGBR (leitura obrigatória):** [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Fonte documental canônica na `main`; esta branch pode ter código mais avançado do que a `main`. Consulte os dois estados antes de afirmar qual versão está pronta.

<p align="center"><strong>PGBR-v0.00.35-LT (candidato a pré-release QA; publicação pendente) · Edição brasileira para Android</strong></p>
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status (22/11/2026):** a pré-release LT034 acrescenta Sniper 100 IV manual ao PGBR e painel PGSharp Free/Standard com key criptografada; **a consulta direta a PGSharp e a verificação de licença ainda não estão operacionais**. LT033 consolida a linhagem LT032, Eventos no menu Pokébola, correções de visibilidade, testes nativos sazonais, manutenção e rastreabilidade. A publicação é **pré-lançamento de QA**: PvP, VGP+, Root+Vector, joystick, feeds e privacidade visual ainda exigem ensaio real. Não há integração autenticada a dados privados do PGSharp/Pokémod.

> **LT034:** [notas da nova versão](docs/releases/v0.00.34-LT.md) · [workflow de compilação](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt034-release.yml). A publicação foi verificada em 22/11/2026 e a LT033 permanece como opção anterior.

> **LT035 candidato PvP:** [notas de QA](docs/releases/v0.00.35-LT.md) · [workflow de build e publicação](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt035-release.yml). O arquivo `PGBR-v0.00.35-LT.apk` **ainda não está publicado nesta revisão do código**. A LT034 permanece a opção de download realmente verificada; não anunciar PvP homologado antes do teste no aparelho.

## Baixar e validar

- **LT034 publicada:** [PGBR-v0.00.34-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT.apk), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT-Root.apk) e [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/PGBR-v0.00.34-LT-Root-module.zip). [Hashes verificados](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.34-LT/SHA256SUMS.txt). **Sem homologação em dispositivo.**
- **APK sem Root LT033:** [download da pré-release de QA](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.33-LT/PGBR-v0.00.33-LT.apk) (somente após publicação confirmada). As variantes Root e Zygisk estão no [mesmo lançamento](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.33-LT). [Workflow de build LT033](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt033-release.yml).
- **Versão pública histórica preservada:** [LT023](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.23-LT). A LT033 não a substitui como versão fisicamente homologada.
- **Pontos de teste para homologação:** [matriz Android Root e não Root](docs/QA_LT032_ANDROID_FINAL.md) (aplicável à LT033, sem aprovação física presumida).
- **Registro da integração:** [notas LT033](docs/releases/v0.00.33-LT.md), com histórico LT032 preservado.

Mantenha a versão antiga instalada e faça backup das configurações antes de testar. APK de teste compilado não equivale a versão funcional homologada ou autorizada por terceiros.

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

- [Arquivo Mestre Único (leitura obrigatória)](PGBR_ARQUIVO_MESTRE_UNICO.md)
- [Inventário e limpeza conservadora](docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md)
- [Instalação, assinatura e compilação](docs/BUILDING.md)
- [Compatibilidade e arquitetura](docs/ARCHITECTURE.md)
- [Instalador Root](docs/ROOTED.md)
- [Atualizações do jogo](docs/GAME_UPDATES.md)
- [Auditoria PT-BR](docs/PGBR_AUDITORIA_PTBR_LT001_20261120.md)
- [Licenças e dependências de terceiros](THIRD_PARTY.md)

## Créditos e licença

O PGBR é uma adaptação independente do código aberto do projeto original **[PGRazor](https://github.com/jpitty03/PGRazor)**, disponibilizado sob licença MIT. A atribuição ao autor e os avisos de licença de terceiros são preservados conforme os respectivos termos. Pokémon e Pokémon GO pertencem aos seus titulares. O PGBR não é afiliado à Niantic, à The Pokémon Company nem aos publicadores do jogo.


### Verificação de código e origem do financiamento

O [CI de código](.github/workflows/pgbr-codigo-ci.yml) executa verificações Python, Java, Node/bridge e fixtures C++ host/NDK. Esses checks não provam execução real de Pokémon GO: instalação, login, PvP, VGP, joystick e Root requerem testes físicos. Um PR verde não constitui release nem APK homologado.

A entrada [`ko_fi: pgrazor`](.github/FUNDING.yml) pertence ao projeto upstream **PGRazor** e não representa arrecadação do PGBR. Créditos e licenças MIT são preservados.
