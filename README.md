> **LT043 integrada e compilada como QA:** `PGBR-v0.00.43-LT.apk`, Root e Zygisk assinados e verificados no [Actions #45277310216](https://github.com/LucianoToscano/PGBR/actions/runs/45277310216). Conteúdo: Rocket QA6, mapa/HUD/IV, sessão Max preparatória e importação PGSharp local. **Publicação de release em validação** no [Actions #45279562497](https://github.com/LucianoToscano/PGBR/actions/runs/45279562497); não divulgar APK direto antes de verificar assets. [Notas detalhadas](docs/releases/v0.00.43-LT.md) · [merge #141](https://github.com/LucianoToscano/PGBR/pull/141) · [pendências #144](https://github.com/LucianoToscano/PGBR/issues/144). `device_tested=false`.

<p align="center"><img src="pgrazor/branding/pgbr-logo.webp" width="92" alt="Logotipo PGBR"></p>

<h1 align="center">PGBR</h1>
> **Arquivo Mestre Único do PGBR (leitura obrigatória):** [PGBR_ARQUIVO_MESTRE_UNICO.md](PGBR_ARQUIVO_MESTRE_UNICO.md). Fonte documental canônica na `main`; esta branch pode ter código mais avançado do que a `main`. Consulte os dois estados antes de afirmar qual versão está pronta.

<p align="center"><strong>PGBR-v0.00.42-LT — pré-release QA publicada do Ataque Inteligente VNext · device_tested=false. LT041 preservada.</strong></p>

> **LT042 publicada como QA:** [APK sem Root — download direto](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.42-LT/PGBR-v0.00.42-LT.apk), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.42-LT/PGBR-v0.00.42-LT-Root.apk), [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.42-LT/PGBR-v0.00.42-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.42-LT/SHA256SUMS.txt). [CI #45088743052](https://github.com/LucianoToscano/PGBR/actions/runs/45088743052) `completed/success`; `device_tested=false`, Ataque Inteligente VNext ainda experimental no jogo.
> **LT041 publicada e preservada:** [APK sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT/PGBR-v0.00.41-LT.apk), [Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT/PGBR-v0.00.41-LT-Root.apk) e [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.41-LT/PGBR-v0.00.41-LT-Root-module.zip). Releases anteriores continuam disponíveis abaixo.
<p align="center">Controles de localização, joystick, captura, rotas e recursos opcionais de automação para Pokémon GO.</p>

> **Status (22/11/2026):** a pré-release LT034 acrescenta Sniper 100 IV manual ao PGBR e painel PGSharp Free/Standard com key criptografada; **a consulta direta a PGSharp e a verificação de licença ainda não estão operacionais**. LT033 consolida a linhagem LT032, Eventos no menu Pokébola, correções de visibilidade, testes nativos sazonais, manutenção e rastreabilidade. A publicação é **pré-lançamento de QA**: PvP, VGP+, Root+Vector, joystick, feeds e privacidade visual ainda exigem ensaio real. Não há integração autenticada a dados privados do PGSharp/Pokémod.

> **LT034:** [notas da nova versão](docs/releases/v0.00.34-LT.md) · [workflow de compilação](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt034-release.yml). A publicação foi verificada em 22/11/2026 e a LT033 permanece como opção anterior.

> **LT035 publicada (QA):** [notas da LT035](docs/releases/v0.00.35-LT.md) · [release oficial](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.35-LT) · [execução de build validada #45018980405](https://github.com/LucianoToscano/PGBR/actions/runs/45018980405). Os três pacotes foram compilados, assinados e verificados no CI. **Auto PvP não homologado em dispositivo (`device_tested=false`)**; não é cópia de funções do APK PGSharp.

> **LT037 publicada como QA:** [release verificada](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.37-LT), [build Actions #45028477340](https://github.com/LucianoToscano/PGBR/actions/runs/45028477340). O instalador é `PGBR-v0.00.37-LT.apk`. Novo modo automático compartilhado do Rocket para PvP tipado (inclusive desafios privados com confirmação), treinamento/NPC reconhecido; sem qualquer ativação em raide/ginásio/Max, selvagem ou modo desconhecido. [Política de alvos e limitações](docs/releases/v0.00.37-LT.md). `device_tested=false`: nenhum golpe real homologado.

> **LT036 publicada e preservada:** [APK direto PGBR-v0.00.36-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT.apk), [Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT-Root.apk) e [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.36-LT/PGBR-v0.00.36-LT-Root-module.zip). A LT036 foi compilada e publicada em QA; sem homologação física. O APK sem Root não substitui Pokémon GO/PGSharp de certificado diferente: [diagnóstico](docs/QA_LT036_TREINAMENTO_INSTALACAO.md).

> **LT038 candidata (menu e temporizadores):** [notas e limitações](docs/releases/v0.00.38-LT.md) · [workflow de compilação](https://github.com/LucianoToscano/PGBR/actions/workflows/pgbr-lt038-release.yml). Instalador `PGBR-v0.00.38-LT.apk` **publicado e verificado** na [release LT038](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.38-LT), após [CI #45039119340](https://github.com/LucianoToscano/PGBR/actions/runs/45039119340) com sucesso. Inclui uma opção Ataque Inteligente persistente, troca 2,3 s e escudo 1,7 s, sem atrasar carregados. **A priorização por eficácia ainda NÃO opera no executor nativo de batalha**; não anunciar como habilitada no jogo. Tela branca ainda sob investigação. Preserva o APK LT037 aprovado pelo usuário para Ágil/Carregado/Escudo.

> **LT039 publicada como QA:** [notas técnicas](docs/releases/v0.00.39-LT.md) · [release GitHub](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.39-LT) · [build verificado #45045923119](https://github.com/LucianoToscano/PGBR/actions/runs/45045923119). PRs #100 e #102 integradas; escudo 0/1.500 (padrão)/3.000 ms/desativado e troca VNext após KO 400/2.000 (padrão)/6.000 ms/desativado. Mantidos 250 ms pós-escudo e ataques imediatos. APK sem Root, Root e ZIP Zygisk assinados e verificados. `device_tested=false` até homologação física. Seleção universal de golpes por eficácia ainda não implementada.

> **LT040 publicada como QA:** [release oficial de pré-teste](https://github.com/LucianoToscano/PGBR/releases/tag/v0.00.40-LT) e [APK sem Root PGBR-v0.00.40-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT.apk), [workflow verificado #45054751331](https://github.com/LucianoToscano/PGBR/actions/runs/45054751331): [notas de versão](docs/releases/v0.00.40-LT.md) · [PR de recuperação de batalhas #107](https://github.com/LucianoToscano/PGBR/pull/107). O assistente compartilhado reutiliza o motor Rocket em sessões comprovadas de Treinamento/PvP/duelo; observa Hub antes das preferências, recupera prova de tipo validada, bloqueia modos desconhecidos/Raid/Max. Escudo 0/1.500/3.000 ms e Troca Nocaute 400/2.000/6.000 ms, **sem opção Desativado**; Ágil/Carregado/Escudo seguem independentes. Cinco checks da PR #107 aprovados; **release LT040 assinada, mas sem homologação física de ataques PvP/duelos**. Ataque Inteligente por eficácia e tela branca continuam pendentes.

## Baixar e validar

- **LT040 (pré-release QA publicada):** [APK sem Root — download direto](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT.apk), [APK Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT-Root.apk), [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/PGBR-v0.00.40-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.40-LT/SHA256SUMS.txt). [CI de publicação](https://github.com/LucianoToscano/PGBR/actions/runs/45054751331) `completed/success`. `device_tested=false`: funcionalidade em PvP/duelo, escolha por eficácia e tela branca exigem teste no celular.

- **LT039 (QA publicada):** [APK direto PGBR-v0.00.39-LT.apk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT.apk), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT-Root.apk), [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/PGBR-v0.00.39-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.39-LT/SHA256SUMS.txt). CI GitHub aprovado, `device_tested=false`.

- **LT038 (QA publicada):** [APK sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT.apk), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT-Root.apk), [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/PGBR-v0.00.38-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.38-LT/SHA256SUMS.txt). Menu inteligente e temporizadores presentes; `device_tested=false`; a prioridade nativa global de golpes **não** está implementada.

- **LT037 (QA publicada):** [APK sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT.apk), [Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT-Root.apk), [Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/PGBR-v0.00.37-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.37-LT/SHA256SUMS.txt). **Auto PvP/Treinamento não homologado em aparelho.**

- **LT035 (QA, publicada):** [APK direto sem Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT.apk), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root.apk), [módulo Zygisk](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/PGBR-v0.00.35-LT-Root-module.zip) e [SHA256SUMS.txt](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.35-LT/SHA256SUMS.txt). **Sem teste físico de PvP; preserve a instalação anterior até validar.**

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
