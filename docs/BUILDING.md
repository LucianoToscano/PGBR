# Como compilar o PGBR

As compilações locais usam a versão exibida nas configurações a partir de `pgrazor/VERSION`. As compilações de lançamento a substituem por `PGRAZOR_VERSION`, definida pela tag do fluxo de trabalho. Essa é a versão do PGBR, independente da versão do jogo original.

Para o módulo Zygisk e o instalador Root separados, consulte [PGBR Root](ROOTED.md). As duas versões compartilham os controles Java e o núcleo nativo do jogo.

## Requisitos

O conjunto de ferramentas para empacotar o APK atualmente oferece suporte ao Windows. Instale Python 3.12+, Node.js 22+, JDK 17+ e Android SDK. Os scripts Python de compilação usam a biblioteca padrão; a compilação nativa também exige Git, CMake 4.4.4 e Ninja 1.13.2.

Os pacotes padrão do SDK são `platforms;android-36.1` e `build-tools;36.1.0`. O CI utiliza API 36 / Build Tools 36.0.0. O núcleo do jogo exige NDK `27.3.13750724`, e a integração ART exige NDK `29.0.14206865`. Instale ambos, pois atendem a exigências diferentes de compilação.

## Preparação após obter uma cópia nova do repositório

Na raiz do repositório, com Python 3.12 selecionado como `python` e `sdkmanager` disponível no `PATH`:

```powershell
$env:ANDROID_SDK_ROOT = 'C:\Android\Sdk'
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
$env:PGRAZOR_ANDROID_PLATFORM = 'android-36'
$env:PGRAZOR_BUILD_TOOLS = '36.0.0'
python -m pip install "cmake==4.4.4" "ninja==1.13.2"
sdkmanager "platforms;android-36" "build-tools;36.0.0" "ndk;27.3.13750724" "ndk;29.0.14206865"
$env:ANDROID_NDK_HOME = Join-Path $env:ANDROID_SDK_ROOT 'ndk/27.3.13750724'
$env:PGRAZOR_ART_NDK = Join-Path $env:ANDROID_SDK_ROOT 'ndk/29.0.14206865'
$env:PGRAZOR_NATIVE_DEPS = Join-Path (Get-Location) 'pgrazor/build/native/deps'
python -B tools/prepare_native.py --download
```

Execute cada comando com sucesso antes de continuar. No Windows, se houver várias versões do Python, use `py -3.12` no lugar de `python` em todos os comandos. A opção explícita `--download` permite baixar as fontes fixadas de LSPlant, DexBuilder, parallel_hashmap, xz-embedded e Dobby (com SHA verificado). Apenas os submódulos Git necessários são inicializados. Árvores de dependências modificadas ou em versões incorretas são rejeitadas; a preparação não as redefine.

Mantenha essas variáveis de ambiente nas compilações seguintes. `PGRAZOR_NATIVE_DEPS` define o cache das dependências; `--cache PATH` também é aceito pela preparação e pelo `art_build.py`. Sem substituição, uma cópia nova usa `pgrazor/build/native/deps`. Cópias de desenvolvimento existentes podem reutilizar o cache validado `.artifacts/native-migration`. Para localizar o NDK ART, também são aceitos `ANDROID_SDK_ROOT`/`ANDROID_HOME` e o caminho local alternativo; as compilações do jogo usam `ANDROID_NDK_HOME`.

Para conferir os pré-requisitos posteriormente, sem baixar nem compilar:

```powershell
python -B tools/prepare_native.py
python -B pgrazor/art_build.py --check
python -B tools/check.py
```

Os dois primeiros comandos conferem os commits e vínculos Git das dependências, a integridade do código-fonte, as versões fixadas do NDK e as ferramentas de compilação. `tools/check.py` executa as verificações do repositório. As compilações ART mantêm objetos CMake, bibliotecas estáticas, mapas de ligação, bibliotecas não reduzidas e comprovantes de origem em `pgrazor/build/art`; preserve essas saídas e as fontes das dependências registradas para permitir uma nova ligação.

## Compilação local do aplicativo completo

Forneça seu próprio APK/APKM/APKS ARM64 original, sem modificações e assinado pela Niantic. A entrada compatível é Pokémon GO 0.437.0, código de versão 2026110902. O fluxo de lançamento também fixa seu SHA-256:

```text
5be1ff372d8a3aa1c9ce9f4014b3825119086feaf71aae6b0538636acafebdd3
```

Guarde o original na pasta `pgrazor/input/`, ignorada pelo Git. Na raiz do repositório:

```powershell
python -B pgrazor/nonroot.py --apk pgrazor/input/game.apkm
python -B pgrazor/release_apk.py --run pgrazor/build/nonroot/RUN-ID --output pgrazor/build/release
```

Use a pasta de execução informada pelo primeiro comando. O segundo combina o componente de configuração ARM64 que contém apenas bibliotecas, remove a exigência de instalação dividida, define o nome instalado como Pokémon GO (o menu dentro do jogo continua PGBR), alinha e assina **PGBR-v0.00.03-LT.apk** e grava os checksums. Componentes de recursos ou código são rejeitados, em vez de serem descartados silenciosamente. Nenhum desses comandos instala algo no celular.

O empacotamento em APK único é recente e ainda exige um teste de instalação e abertura em um celular desconectado. Os pacotes divididos anteriores continuam disponíveis na pasta de execução como opção de retorno. Preserve uma instalação já testada até verificar a nova versão.

## Execução em celulares e MuMu

A compilação padrão seleciona o núcleo de produção compilado, incluindo os componentes habituais. Ela reúne bibliotecas nativas principais e ART, controles e integrações Java e chamadas DEX reescritas. A auditoria de recursos de produção rejeita Frida e JavaScript do PGBR. As bibliotecas do jogo continuam ARM64; o MuMu traduz essas bibliotecas enquanto seu processo Java Android roda em x86-64.

As integrações compiladas Java/ART controlam localização e ciclo de vida. O transporte nativo tipado `transport52` troca configurações e eventos, inclusive entre os espaços de nomes do hospedeiro e do processo traduzido no MuMu. O ponto de renderização validado orienta o agendamento Unity nativo e a coordenação do hospedeiro. Informações desatualizadas revogam a permissão temporária de execução dos recursos; não é necessário serviço Root nem controlador externo.

O código executável traduzido é verificado pelos segmentos executáveis ELF ARM e pelas assinaturas de instruções. O posicionamento das interceptações, a duração das referências gerenciadas e a publicação ART possuem testes nativos específicos. As verificações ao vivo atuais do núcleo exclusivamente compilado foram feitas no MuMu de pesquisa; testes anteriores em celulares com ambientes de execução antigos não validam essa migração no Android físico. Consulte o comprovante de migração mais recente para conhecer a cobertura exata dos recursos.

Frida está disponível apenas ao selecionar explicitamente `--research-runtime`, para testes compatíveis de inspeção, cena, interação e câmera. Scripts de pesquisa mantidos, a integração fixada e o provisionamento Gadget dão suporte a essas compilações, mas ficam fora dos APKs normais de produção. Consulte `nonroot.py --help` para opções compatíveis de pesquisa. As informações de lançamento registram as arquiteturas nativas incluídas separadamente da arquitetura ARM64 do jogo.

## Arquivos compactados de código-fonte e ferramentas

`python -B tools/source_bundle.py` compacta os arquivos rastreados presentes na árvore de trabalho, ignorando os que foram apagados. O comando falha de forma explícita se fontes novas autorizadas do projeto forem ficar de fora. Revise esses arquivos antes de adicioná-los ao Git ou use explicitamente `python -B tools/source_bundle.py --include-working-tree` para gerar um pacote local.

A opção explícita adiciona apenas fontes não ignoradas em `pgrazor/native`, `pgrazor/art`, `pgrazor/src`, `pgrazor/test`, `tools/dex_bridge` e nos módulos nomeados de compilação nativa/ART/esquema. Ela não coleta notas nem transferências arbitrárias fora do Git. Os dois modos rejeitam caminhos selecionados de credenciais e arquivos gerados, além de links simbólicos e junções, inclusive nas pastas ancestrais. A opção não adiciona nem envia arquivos ao Git; revise o pacote antes de distribuí-lo. Arquivos de entrada, chaves, evidências do aparelho e histórico de retenção continuam locais.

## Assinatura

O desenvolvimento local cria uma chave ignorada pelo Git em `build/pgrazor-development.jks`, com o alias `pgrazor`. Essa chave usa senha de desenvolvimento; configure um armazenamento de chaves exclusivo, com senha forte, para lançamentos. Compilações de pré-lançamento não migram automaticamente configurações nem identificadores de versões anteriores.

Para configurar a assinatura, defina `PGRAZOR_KEYSTORE`, `PGRAZOR_KEY_ALIAS`, `PGRAZOR_STORE_PASSWORD` e `PGRAZOR_KEY_PASSWORD`. As senhas são passadas às ferramentas de assinatura do Android por referências a variáveis de ambiente, nunca por valores na linha de comando. Uma chave ou senha configurada ausente interrompe a compilação. **Reutilize o mesmo certificado nas atualizações.**

A saída `pgrazor-preview.apk` do `build.py` é apenas uma demonstração dos controles, **não é o jogo completo**. Não a publique como uma versão funcional do PGBR.

## GitHub Actions: lançamentos sem Root e com Root

Escreva notas objetivas seguindo as [regras de lançamento](releases/README.md) e o [modelo](releases/TEMPLATE.md). GitHub e Discord reutilizam as mesmas notas. Inclua links de download e de lançamento específicos da versão; o rodapé compartilhado mantém referências ao apoio opcional e ao repositório. Não inclua verificações nem alterações exclusivas do site nas notas públicas ou no histórico de mudanças. Links promocionais permanecem sujeitos à decisão de publicação do PGBR.

Configure estes segredos no GitHub Actions:

| Segredo | Valor |
| --- | --- |
| `PGRAZOR_INPUT_URL` | URL HTTPS do APKM original compatível ou URL da API de um arquivo de lançamento deste repositório |
| `PGRAZOR_KEYSTORE_BASE64` | Armazenamento de chaves de lançamento codificado em Base64 |
| `PGRAZOR_KEY_ALIAS` | Alias da assinatura; por exemplo, `pgrazor` para o PGBR |
| `PGRAZOR_STORE_PASSWORD` | Senha do armazenamento de chaves |
| `PGRAZOR_KEY_PASSWORD` | Senha da chave privada |

Para uma entrada privada do GitHub, utilize a URL `https://api.github.com/repos/OWNER/REPO/releases/assets/ID`. O fluxo fornece um token com permissões limitadas; a autorização é removida quando o GitHub redireciona ao servidor de download assinado.

Acesse **Actions → Android release (non-root and rooted) → Run workflow**, informe uma tag como `v0.00.03-LT` e deixe `publish` desligado para um candidato de testes. Baixe **PGBR-android** da execução concluída. Ele contém **PGBR-v0.00.03-LT.apk** (sem Root), **PGBR-v0.00.03-LT-Root.apk** (instalador Root) e **PGBR-v0.00.03-LT-Root-module.zip** (módulo Root manual). É necessário acesso de leitura ao repositório para baixar [artefatos de execução](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

Depois dos testes no aparelho, execute com `publish` ligado para criar um lançamento com os três downloads, checksums e comprovantes de compilação. Tags e lançamentos existentes não são substituídos. O fluxo instala os NDKs e ferramentas fixados e prepara as dependências nativas antes dos testes e empacotamento. A preparação PowerShell escreve os caminhos em `GITHUB_ENV`, preservando os mesmos compiladores e cache nas etapas seguintes. A execução é manual, não roda para solicitações de incorporação e exige notas da versão em `docs/releases/TAG.md`. O arquivo `docs/releases/footer.md` é acrescentado automaticamente, incluindo a referência opcional de apoio. O CI de fontes não envia segredos nem arquivos do jogo.

O envio e a publicação conferem os três downloads com checksums e comprovantes, verificam a correspondência das versões do jogo e do lançamento e confirmam que o instalador Root contém exatamente o módulo ZIP publicado. O CI de fontes também compila o instalador Root sem o jogo nem segredos de assinatura. A chave permanece nos segredos do Actions; não a substitua entre versões.

## Avisos de lançamento no Discord

Crie um webhook de entrada no canal Discord `1562582290536726598`, em **Editar canal → Integrações → Webhooks**. Salve a URL como segredo **DISCORD_RELEASE_WEBHOOK** no GitHub Actions. Não publique a URL no repositório.

O fluxo **Discord release announcement** envia um aviso de lançamentos publicados (inclusive pré-lançamentos) com notas da versão e links do GitHub, APK sem Root, instalador Root e módulo ZIP, além do apoio opcional quando configurado. Antes do envio, ele confirma o canal do webhook. Rascunhos não são anunciados. O fluxo de APK chama esse processo diretamente porque lançamentos criados com `GITHUB_TOKEN` não acionam automaticamente outro fluxo de eventos de lançamento.

Para mencionar um cargo de assinantes do lançamento, defina a **variável** `DISCORD_RELEASE_ROLE_ID` com o ID numérico do cargo (Modo desenvolvedor do Discord → botão direito no cargo → Copiar ID). Confirme que o Discord permite ao webhook mencionar esse cargo. Apenas esse cargo poderá ser mencionado; menções a outros cargos, pessoas e `everyone` ficam desativadas. Deixe a variável vazia para não mencionar ninguém. Isso vale para pré-lançamentos e avisos manuais.

Sem o segredo, a etapa de aviso registra um alerta e não envia nada. Para anunciar uma versão já publicada, execute manualmente **Discord release announcement** com a tag. Execuções manuais e repetidas podem gerar avisos duplicados; confira o canal antes. O fluxo publica uma mensagem no canal, sem republicá-la automaticamente para seguidores.

## Instalação e atualizações

Instale o APK único pelo instalador do Android ou execute `adb install -r PGBR-v0.00.03-LT.apk` no computador. **Não desinstale uma versão PGBR funcional só para contornar erro de assinatura**: use uma compilação assinada com a chave já existente. O jogo oficial não pode ser substituído por uma compilação com assinatura diferente sem desinstalação. A remoção pode causar perda dos dados locais.

O SDK mínimo é Android 9 (API 28), mas as interceptações nativas do jogo são compatíveis apenas com a versão ARM64 fixada. Não há garantia de compatibilidade com emuladores, futuras versões ou todos os aparelhos.

## Retenção de arquivos

`cleanup.ps1` remove APKs gerados obsoletos reconhecidos após o empacotamento, preservando registros de diagnóstico, chaves, entradas e dependências de execução. Copie `build-retention.example.json` para `build-retention.json` (ignorado pelo Git) numa cópia nova; o processo de empacotamento também inicializa esse arquivo, quando ausente, e fixa o primeiro candidato concluído (não um retorno já testado). Fixe as versões instaladas e uma alternativa de retorno testada antes de substituí-las. Nunca publique o histórico de retenção específico do aparelho.

### Arquivo original do jogo hospedado

O fluxo de lançamento lê `PGRAZOR_INPUT_URL`; `tools/private_input.py` compara o checksum com o perfil selecionado em `pgrazor/native/schema/game/` antes de empacotar. A entrada atual é mantida como arquivo de um lançamento `build-inputs` em **rascunho não publicado**. Preserve esse estado; pacotes originais do jogo não devem ser publicados como arquivos de lançamento.

Para atualizar o jogo, envie o novo pacote original com nome versionado, confirme seu SHA-256 e atualize `PGRAZOR_INPUT_URL` para a URL do arquivo na API do GitHub. O fluxo autentica o download com o token do repositório. O GitHub limita arquivos de rascunho a chamadas com permissão de escrita; por isso, a compilação acionada manualmente usa `contents: write`, e a obtenção do código não mantém esse token nas credenciais Git. Atualize juntos o checksum fixado e as integrações nativas de compatibilidade antes de executar a publicação.

## Atualizações da versão do jogo

Consulte [GAME_UPDATES.md](GAME_UPDATES.md) para o perfil de versão, as verificações das integrações geradas
e o procedimento de comparação das assinaturas antigas e novas.
