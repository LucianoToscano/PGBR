# PGBR — comparativo verificável de privacidade do nome (20/11/2026)

## Escopo e evidências

### PGSharp: inspeção de APK

- Artefato analisado localmente: `pgs1.277.2_0.435.2_rhGEK(arm64m).apk`, aproximadamente 211 MB.
- Biblioteca principal: `lib/arm64-v8a/libmain.so`, ELF ARM64, 38.958.432 bytes, stripped.
- A análise de strings da biblioteca identificou `hideTrainerName` e `aHIDEPLAYER` na zona de dados. Uma inspeção das referências ao endereço de `hideTrainerName` mostrou chamadas nativas que leem a chave de configuração.
- Isso **comprova a presença de uma chave de configuração nativa**, mas não prova onde a interface é interceptada. Não afirmar que a implementação usa `PlayerService.get_PlayerName`, `UnityEngine.UI.Text`, ou `TextMeshPro` sem examinar os hooks e a execução.
- Documentação oficial: https://www.pgsharp.com/features/ — a função `Hide Trainer Name` oculta o nome na tela principal e na seção `Me`.

### Pokemod

- Busca no Google Drive por `Pokemod` não localizou um APK com esse nome; foram encontrados documentos de referência. Não ocorreu análise de binário Pokemod.
- A descrição oficial https://pokemod.dev/ informa que `Incognito Mode` substitui o nome por um genérico na maioria das telas.
- O Pokemod usa o Pokémon GO oficial e um módulo/anexação em Android com root, segundo o site. Essa arquitetura não deve ser confundida com a do APK sem root.

### iPogo

- Busca no Google Drive por `iPogo` não localizou um APK com esse nome. A URL de download oficial do Android foi identificada, mas o binário não ficou disponível no ambiente de análise. Não ocorreu análise binária do iPogo.
- A documentação oficial https://ipogo.app/features.php descreve `Incognito Mode`, que mostra `*****` no lugar de informações pessoais em mapa, perfil e outras telas.

## O que foi confirmado sobre o PGBR

- O PGBR lê `PlayerService.get_PlayerName` para vincular e identificar a sessão da conta. **Esse getter não deve ser substituído por apelido genérico:** isso arriscaria a identificação, troca e persistência das contas.
- A tarja Android introduzida anteriormente em `Pad.java` não alterava a renderização Unity. Foi **retirada**; o controle visual enganoso foi substituído por um aviso de que a função está em validação.
- As preferências antigas `trainerAlias` e `trainerAliasEnabled` são preservadas para futura migração, mas sem exibir tarja. Não apagar dados do usuário.

## Projeto da solução correta (a implementar, não aprovada)

Modos mutuamente exclusivos: `Original`, `Ocultar na tela`, `Exibir apelido fictício` (por exemplo `PGBR_Treinador4827`). Tudo desativado por padrão. O nome real e o ID da conta devem permanecer imutáveis.

Arquitetura desejada: substituir **somente o conteúdo dos widgets de renderização** do treinador, depois de a conta e a lógica do jogo terem recebido seus valores originais; nada de interceptar identidade, dados serializados, RPC ou contas. Precisamos descobrir e validar os componentes exatos de mapa e perfil na versão suportada, usando metadados verificados e uma rotina segura de reversão. Em caso de falha de resolução, não mascarar nada e registrar o erro.

## Testes exigidos para considerar o recurso pronto

1. Mapa principal: original, ocultar e apelido; alternar sem reiniciar se suportado.
2. Perfil `Eu`, lista de amigos e telas em que o nome apareça; registrar explicitamente as superfícies cobertas e não cobertas.
3. Login, troca de conta, reinício e retorno do plano de fundo: a conta original e a autenticação não podem mudar.
4. Capturas de tela e vídeos: nenhum nome real visível nas telas declaradas como protegidas.
5. Falha de metadados / atualização de versão do jogo: manutenção da integridade da conta e desativação segura do recurso.
6. Compilação, testes automatizados e homologação no dispositivo.

## Status

**Comparação parcial dos APKs:** análise estática efetiva do APK PGSharp; descrição funcional oficial para Pokemod e iPogo. Não houve descompilação ou teste dos APKs desses dois últimos. A função de substituição real **não foi implementada** no PGBR; não apresentá-la como concluída.

O PokeSearch, a assinatura e a branch `main` devem continuar intactos.

## Pista adicional: texto "The Pokemod Group" (20/11/2026)

O usuário observou que o modo Incognito do Pokemod exibe **"The Pokemod Group"** onde normalmente apareceria o nickname do treinador. Essa observação é relevante como **string-alvo de busca na renderização**. O fornecedor se apresenta oficialmente com esse mesmo nome, o que é consistente, mas não identifica um método técnico específico.

**Importante distinguir ocorrências:** a auditoria estática anterior do APK Pokemod 14.2.1, registrada no repositório PokeSearch em `docs/LT359_AUDITORIA_POKEMOD_MULTICLIENTE_20261115.md`, comprovou `CN=The Pokemod Group` no certificado `META-INF/CERT.RSA`. Uma correspondência dessa string no certificado **não comprova** que ela seja usada no código de substituição de nick.

A mesma auditoria estudou `classes.dex`, `classes2.dex`, bibliotecas `.so` e `assets/pokemod-injector`, mas **não identificou** o hook de Incognito Mode nem disponibilizou código-fonte desse mecanismo. O APK local daquela auditoria não está disponível na sessão atual; seus resultados anteriores são evidências de estrutura, não inspeção binária nova.

Próxima investigação, quando houver acesso ao artefato original: localizar ocorrências da frase em recursos, DEX e módulo ARM64; separar texto de metadados de assinatura, log e material publicitário de referências executáveis; examinar chamadas que alterem **somente** componentes de texto de UI Unity, sem modificar `PlayerService.get_PlayerName` nem os valores de identidade persistidos. Testar mapa/perfil separadamente antes de declarar suporte.

Fonte adicional: documentação do fornecedor `https://pokemod.dev/` (Incognito Mode: substituição por nome genérico na maioria das telas).
