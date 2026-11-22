<p align="center"><img src="pgrazor/branding/pgbr-logo.webp" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
> **Arquivo Mestre Único do PGBR (leitura obrigatória):** [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Fonte documental canônica na `main`; esta branch pode ter código mais avançado do que a `main`. Consulte os dois estados antes de afirmar qual versão está pronta.

<p align="center"><strong>PGBR-v0.00.32-LT (candidato de teste) · Edição brasileira para Android</strong></p>
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status (21/11/2026):** LT032 é a **branch de integração de teste**. Preserva a linhagem LT023–LT031, junta as correções posteriores do pipeline LT027 e não representa homologação em aparelho. **PvP, VGP, feeds e privacidade visual exigem ensaio real**. Não há integração direta autenticada de dados PGSharp/Pokémod.

## Baixar e validar

- **APK de teste LT032:** `PGBR-v0.00.32-LT.apk`; procurar o artefato de 30 dias do [workflow LT032](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt032-consolidado-apk.yml) depois de uma execução bem-sucedida.
- **Última release pública com instaladores Root e sem Root:** [LT023](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.23-LT).
- **Pontos de teste para homologação:** [checklist LT032](docs/validation/PGBR_LT032_CHECKLIST_DISPOSITIVO.md).
- **Registro da integração:** [notas LT032](docs/releases/v0.00.32-LT.md).

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
