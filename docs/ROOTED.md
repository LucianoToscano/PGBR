# PGBR Root — instalação e funcionamento

> **Referência publicada:** LT045 é uma pré-release QA assinada, **não homologada em Android físico** (`device_tested=false`). A instalação exige a versão exata de Pokémon GO e configurações compatíveis. Não substitua sua instalação estável antes de validar. Consulte [notas LT045](releases/v0.00.45-LT.md), [instalador Root](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.45-LT/PGBR-v0.00.45-LT-Root.apk), [módulo](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.45-LT/PGBR-v0.00.45-LT-Root-module.zip) e [SHA256SUMS](https://github.com/LucianoToscano/PGBR/releases/download/v0.00.45-LT/SHA256SUMS.txt). A LT046 permanece em desenvolvimento; as referências LT040 abaixo são históricas.

PGBR Root é um instalador separado (`dev.pgrazor.root`) para o Pokémon GO oficial, assinado pela Niantic. Ele inclui um módulo Zygisk e o instala por Magisk 27+, KernelSU ou APatch. KernelSU e APatch também exigem um provedor Zygisk. O APK modificado `PGBR-v0.00.40-LT.apk` continua sendo a versão sem Root.

As versões com e sem Root são mantidas juntas na `main` e produzidas pelo mesmo fluxo de lançamento. O suporte Root ainda é experimental: a instalação de interceptações nativas pode falhar em alguns aparelhos Samsung. Instalar o módulo não altera o APK do jogo nem seu certificado de assinatura.

Se a sobreposição funcionar, mas os recursos nativos continuarem indisponíveis, examine `native-bootstrap-pending` e `native-runtime-health` no Registro de execução. Quando o símbolo padrão de carregamento do Android não está disponível, o carregador procura bibliotecas de sistema já carregadas; ele não inicia outra cópia do jogo. Os motivos `symbol-unavailable` ou `namespace-open-null` exigem um registro específico do aparelho. Se a inicialização chegar a funcionar, mas `native-runtime-start` registrar `scheduler-install` / `invoke-hook` com `invokeRc=-1`, os recursos nativos não iniciaram; **Verificar novamente** no instalador apenas consulta o estado, sem tentar instalar novamente as interceptações. Se o Caçador de Shundo indicar espera por conta, abra **Trocar conta**; a restauração de conta pendente mantém a automação pausada até o jogo reiniciar e confirmar a conta.

## Instalação

1. Instale e abra `PGBR-v0.00.40-LT-Root.apk`.
2. Confira o estado e conceda acesso Root ao instalador no gerenciador Root.
3. Confirme que a versão do jogo oficial corresponde à versão compatível indicada pelo instalador.
4. Toque em **Instalar**, confira o resultado e reinicie quando solicitado.
5. Abra Pokémon GO. O painel PGBR aparece quando o módulo inicia corretamente.

Para desligar, desative o módulo `pgrazor` no gerenciador Root e reinicie. Desinstalar apenas o APK do instalador não remove o módulo.

Para instalar manualmente, instale `PGBR-v0.00.40-LT-Root-module.zip` pelo gerenciador de módulos Root e reinicie. Use a versão oficial do jogo compatível com o lançamento. Você pode também instalar `PGBR-v0.00.40-LT-Root.apk` para consultar o estado e reiniciar automaticamente nas trocas de conta; não é necessário reinstalar o módulo pelo APK. Sem o instalador, as trocas de conta exigem fechar e reabrir manualmente o jogo. `PGBR-v0.00.40-LT.apk` é o jogo modificado sem Root, não o aplicativo auxiliar Root.

O ambiente exige a versão exata do jogo e o certificado oficial de assinatura. Uma versão mais nova não é aceita automaticamente só porque as classes Java ainda coincidem: os recursos nativos também dependem da compilação compatível. Estado Root e configuração Zygisk não comprovam, isoladamente, que a injeção funcionou; o Registro de execução dentro do jogo detalha a inicialização.

## Compilação

Use SDK, JDK, NDKs fixados e configuração de assinatura descritos em [BUILDING.md](BUILDING.md). Na raiz do repositório:

```powershell
py -3.12 -B pgrazor/rooted.py pgrazor/input/game.apkm --download --output dist/release
```

`--skip-build` reutiliza o DEX somente após validar as assinaturas das fontes. `--module-only` produz apenas o ZIP do módulo para desenvolvimento, sem instalador. O APK/APKM original precisa ter o certificado confiável do jogo; as integrações são verificadas antes do empacotamento.

Cada execução fica em `pgrazor/build/rooted/<run>/` e registra em `report.json` os hashes do módulo, conteúdo, DEX e instalador, a versão do jogo e as arquiteturas. O lançamento inclui `PGBR-v0.00.40-LT-Root.apk`, `PGBR-v0.00.40-LT-Root-module.zip`, `root-build-info.json` e as verificações correspondentes em `SHA256SUMS.txt`. O fluxo envia os dois APKs e o módulo ZIP como artefatos e, quando solicitado, como arquivos de lançamento. A publicação é uma opção separada.

`cleanup.ps1 -RootedOnly` mostra uma prévia da limpeza Root; acrescente `-Apply` para executar. O empacotamento chama essa limpeza após gerar o relatório. Arquivos temporários `input-*.apk` podem ser descartados depois de validar um módulo completo. As três pastas de execução mais recentes e as entradas `pinnedRootedRuns` em `build-retention.json` protegem as saídas do módulo e instalador. Mantenha fixados o candidato instalado e uma opção de retorno já testada. A limpeza preserva relatórios, entradas originais, código e chaves, criando um comprovante em `build/cleanup/`.

## Inicialização e ligação nativa

O carregador Zygisk seleciona apenas o processo principal do jogo, lê o DEX e os recursos antes da especialização e inicia o ambiente Java com o UID do jogo. O ambiente valida o destino, instala as integrações Java e chama a inicialização compartilhada do PGBR. Uma instalação parcial das integrações permanece inativa e encaminha as chamadas originais.

No MuMu, as interceptações ART usam a ABI real x86_64 do processo, obtida em `/proc/self/exe`. A execução do jogo usa o núcleo ARM64 traduzido. A carga nativa Root aguarda a mensagem de renderização Unity para que o jogo estabeleça primeiro o espaço de nomes da biblioteca traduzida.

A inicialização nativa tenta primeiro localizar bibliotecas já carregadas com `dlopen(..., RTLD_NOLOAD)`. Se a separação de espaços de nomes Android impedir essa busca, identifica uma única imagem carregada correspondente e usa o carregador Android com o espaço de nomes dela. O processo preserva `RTLD_NOLOAD`, nunca força o carregamento de outro ambiente IL2CPP e deixa carregadores incompatíveis indisponíveis. O mecanismo segue a seleção de espaço de nomes pelo endereço do chamador descrita no [linker dinâmico do Android](https://android.googlesource.com/platform/bionic/+/master/linker/linker.cpp) e em [libdl](https://android.googlesource.com/platform/bionic/+/master/libdl/libdl.cpp).

A inicialização nativa é confirmada por `native-bootstrap-ready` e depois `native-scheduler-ready`. Esses marcadores comprovam a integração e o início do agendador; cada recurso ainda exige testes de funcionamento. Mudanças compartilhadas também precisam de teste regressivo em um aparelho com APK modificado antes de um lançamento público.

Os testes nativos independentes podem ser reproduzidos sobre um núcleo revisado:

```powershell
py -3.12 -B pgrazor/native_harness.py runtime_bootstrap runtime_bootstrap_namespace --serial DEVICE_SERIAL --adb PATH_TO_ADB
```

Esses testes não controlam o jogo. Seus comprovantes registram o hash da biblioteca realmente usada e o resultado.

## Interface Root

O Registro de execução abre dentro do jogo e salva o trecho atual na pasta de arquivos externos do jogo. **Salvar registro + configurações (.zip)** inclui esse registro e uma cópia `settings.json`, em linha única, das preferências; também existe exportação de texto simples. A cópia das configurações exclui tokens de acesso, dados de conta e locais salvos. Os registros incluem motivos de falhas das interceptações, estado da conta e VGP+, saúde da execução nativa, detalhes do aparelho e sinais passivos de Root. Root oculto pode não ser detectado; a sessão também identifica explicitamente o ambiente Root.

O backup das configurações também usa a pasta de arquivos externos, no arquivo `PGBR-settings.json`; copie-o para outro local antes de desinstalar o jogo. A importação lê o mesmo caminho e solicita confirmação antes de substituir as configurações.

As trocas de conta ficam enfileiradas de modo persistente e são aplicadas antes de o Unity iniciar no próximo processo. O instalador oferece um auxiliar de reinício em primeiro plano que reabre o jogo depois de encerrar o processo antigo. Instalações somente do módulo exigem fechar e reabrir o jogo manualmente. O auxiliar aceita apenas solicitações originadas do pacote oficial do jogo e não executa operações Root nem manipula dados da conta. O APK oficial não precisa incluir atividades do PGBR no manifesto.

A API Zygisk utilizada é a v5, cuja [tabela oficial de compatibilidade](https://github.com/topjohnwu/zygisk-module-sample#api) exige Magisk 27 ou mais recente. Versões anteriores do Magisk são rejeitadas pelo instalador. KernelSU/APatch exigem um provedor compatível com a API v5.

## Atualização PGBR LT032 — cadeia de hooks ARM64 (PR upstream #11, 22/11/2026)

Fonte: https://github.com/jpitty03/PGRazor/pull/11. O runtime Root ARM64 pode falhar com SIGILL no ART quando outro motor (por exemplo Vector) sobrescreve um desvio de entrada de 12 bytes por um de 16 bytes. A compilação *privada do ART*, em `pgrazor/art/CMakeLists.txt`, agora preserva uma janela mínima de 16 bytes antes de retornar à função original. O checkout fixado do Dobby **não** é alterado, nem os hooks do motor de gameplay. Se o código fixado do Dobby não tiver mais a assinatura esperada, o CMake falha explicitamente, em vez de compilar uma correção não validada.

Para validar em um Android ARM64 de laboratório, construa a biblioteca ART e habilite a fixture `-DPGRAZOR_ART_CHAIN_TEST=ON` no diretório CMake gerado; execute o binário `pgrazor_art_chain_test` como processo independente, fora do jogo. Essa fixture é somente de teste e não integra o APK/ZIP de produção.

**Limites:** port adaptado em fonte no PGBR, ainda sem CI de compilação ART, sem aparelho validado; não garante compatibilidade com todas as ordens de carregamento, ROMs, mecanismos de hook ou x86_64. Não publicar como crash resolvido até aferição de startup do módulo Root e versão exata de Pokémon GO.
