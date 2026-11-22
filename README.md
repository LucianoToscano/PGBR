<p align="center"><img src="pgrazor/branding/pgrazor.svg" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
> **Arquivo Mestre Único do PGBR (leitura obrigatória):** [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Fonte documental canônica na `main`; esta branch pode ter código mais avançado do que a `main`. Consulte os dois estados antes de afirmar qual versão está pronta.

<p align="center"><strong>PGBR · Edição brasileira experimental para Android</strong></p>

> **Contexto:** esta `main` preserva fontes e instruções históricas da LT001. A última release pública identificada é a **v0.00.23-LT (pré-lançamento)**; a **LT032** é um candidato de desenvolvimento em PRs separados, com CI concluído e sem homologação em aparelho. Consulte sempre o [Arquivo Mestre Único](PGBR_ARQUIVO_MESTRE_UNICO.md) e as evidências atuais no GitHub antes de anunciar uma versão.
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status:** versão experimental PT-BR. A compilação automatizada passou nos testes; o funcionamento em aparelhos Android e a cobertura integral da tradução ainda precisam ser validados.

## Baixar o PGBR

### Downloads diretos da última release pública identificada — v0.00.23-LT

- [**APK completo sem Root**](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.23-LT/PGBR-v0.00.23-LT.apk)
- [**Instalador Root**](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.23-LT/PGBR-v0.00.23-LT-Root.apk)
- [**Módulo Root/Zygisk (ZIP)**](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.23-LT/PGBR-v0.00.23-LT-Root-module.zip)
- [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.23-LT/SHA256SUMS.txt) · [Página da release](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.23-LT)

**LT032:** builds e artefatos temporários nos [Actions sem Root](https://github.com/LucianoToscano/PGBR/actions/runs/44958257380) e [Actions Root/Zygisk](https://github.com/LucianoToscano/PGBR/actions/runs/44958259611); **não há URL permanente de release LT032 confirmada**. Não invente download direto. Um workflow verde não equivale a instalação ou homologação física.

### Histórico de download — LT001 (não é a versão mais recente)

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

- **`main`**: fonte canônica do Arquivo Mestre e código histórico do projeto; não contém necessariamente os recursos LT032.
- **`integracao/lt032-recuperacao-20261121`**: candidato mais avançado auditado, preservado em [PR #40](https://github.com/LucianoToscano/PGBR/pull/40). Variantes Root e diagnóstico VGP estão em [PR #41](https://github.com/LucianoToscano/PGBR/pull/41) e [PR #42](https://github.com/LucianoToscano/PGBR/pull/42).
- **`feature/pt-br-traducao`**: desenvolvimento, testes e compilações PT-BR.
- **[GitHub Actions](https://github.com/LucianoToscano/PGBR/actions)**: compila, testa e assina os instaladores. Não é necessário um computador local para cada atualização.

O pacote original do jogo é mantido fora do controle de versão, como entrada temporária da compilação. As senhas e chaves de assinatura ficam nos Secrets do GitHub Actions; nunca devem ser publicadas no repositório.

## Informações importantes

O PGBR é um projeto experimental independente. O tempo de espera (*cooldown*) é uma estimativa local, e nenhuma alteração de cliente garante resultados no servidor ou proteção contra medidas aplicadas pelo publicador de Pokémon GO.

## Documentação

- [**Arquivo Mestre Único (consulta obrigatória)**](PGBR_ARQUIVO_MESTRE_UNICO.md)
- [Inventário e limpeza conservadora](docs/maintenance/PGBR_AUDITORIA_CONSERVADORA_20261121.md)
- [Instalação, assinatura e compilação](docs/BUILDING.md)
- [Compatibilidade e arquitetura](docs/ARCHITECTURE.md)
- [Instalador Root](docs/ROOTED.md)
- [Atualizações do jogo](docs/GAME_UPDATES.md)
- [Auditoria PT-BR](docs/PGBR_AUDITORIA_PTBR_LT001_20261120.md)
- [Licenças e dependências de terceiros](THIRD_PARTY.md)

## Créditos e licença

O PGBR é uma adaptação independente do código aberto do projeto original **[PGRazor](https://github.com/jpitty03/PGRazor)**, disponibilizado sob licença MIT. A atribuição ao autor e os avisos de licença de terceiros são preservados conforme os respectivos termos. Pokémon e Pokémon GO pertencem aos seus titulares. O PGBR não é afiliado à Niantic, à The Pokémon Company nem aos publicadores do jogo.
