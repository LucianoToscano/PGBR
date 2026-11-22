# Auditoria de integração de batalha e cura — 22/11/2026

Base: `main` b355def7f6693b4d7f88c802b6ea40df018f0490, incluindo [PR #116](https://github.com/LucianoToscano/PGBR/pull/116). Esta revisão é parcial e **não implementa Ataque Inteligente no jogo**; não é evidência de comportamento em Android.

## Cadeia de batalha: evidências e lacunas

* `SettingsPanel.java` coloca Batalha, Ataque e Cura Inteligente nessa ordem; escudo/troca ficam na seção de ataque. A preferência de ataque é salva, mas `smart_attack_decision.h` só é incluído pelo seu teste, não pelo executor. O texto de UI deixa explícita a ausência de integração e não promete uma classificação simples de tipos.
* `rocket_combat.cpp` escolhe primeiro candidato pronto/visível/interagível no VNext, ou primeiro/segundo no Legacy. A ordem de captura VNext não prova identidade ou posição do golpe. `battles.cpp` tem executor próprio para reide/ginásio/Max e também procura o primeiro carregado. Não houve alteração de executor, cadência, escudo, troca ou escopo nesta revisão.
* `smart_battle.cpp` lê tabelas do Game Master em execução, Pokémon/golpes armazenados e algumas observações de combate; isso é mais que um catálogo estático. Porém é lógica de equipe/troca, não uma fonte demonstrada de IDs dos dois botões, energia, prontidão e adversário **da mesma sessão** para esses executores. A tabela `MoveSettingsProto` usada ali não demonstra dados específicos PvP; sua busca de espécie pode usar fallback da forma zero, inadequado como prova de forma exata. Não é seguro conectá-la diretamente como se esses requisitos já estivessem resolvidos. O snapshot PvPoke em `schema/pvp` é versionado, mas é destinado a IV/ranking e não é estado ao vivo nem catálogo completo de batalha.
* Para concluir #90: identificar e validar no cliente suportado um provedor autorizado com modalidade, sessão/geração, identidades/forma exatas, parâmetros específicos PvP/PvE, energia e vínculo inequívoco de cada move ID ao botão vivo; expirar e revalidar antes da ação em KO/troca/transição; integrar separadamente os executores e confirmar ações quando houver sinal confiável. Não supor ordem de eventos, offsets ou classes. PvE/Max exige investigação e testes próprios. Testes simulados não provam aceitação no jogo.
* O motor puro estima potência/energia em PvP e potência por tempo de ciclo em PvE. Não é dano real ou garantia da melhor jogada; neutro forte pode superar super eficaz fraco. Ainda faltam comportamento integrado, teste por modo e homologação física.

## Cura e risco corrigido

Antes, `pgr_heal_tick` conferia mapa/configuração antes de consultas gerenciadas; `heal_next` só revalidava configuração quando recebia callback de permissão do ginásio. Uma mudança durante `GetPokemon` ou `GetItemCount` poderia levar o fluxo normal a enviar item com estado antigo. Agora ele consulta novamente configuração e mapa após as consultas e imediatamente antes de enviar; recusa preferência desligada, snapshot inválido, conta inativa/não pronta, saída do mapa e mudanças de preferências. A permissão específica entre rodadas de ginásio continua exigindo callback e configuração compatível, sem permitir cura periódica off-map. O recheck reduz essa janela, mas não estabelece transação atômica com o jogo ou com a thread que publica configurações.

A fixture `heal_runtime_test.cpp` executa o próprio runtime com relógio, storage, transport e promessas simulados, incluindo mudanças durante as duas consultas, restauração posterior, limite de itens, revives, confirmação, falta de permissão e ginásio. Agora faz parte do gate host. Não demonstra consumo real nem resolve eventuais falhas de metadados, simultaneidade posterior à última checagem ou confirmação ambígua em aparelho.

## Tela branca e homologação

Não há captura/log reproduzível suficiente nesta revisão para atribuir a tela branca a um componente ou declarar correção. O minijogo de partículas é separado da seleção de golpe. Reproduzir com versão exata, modo, configurações, sequência e logs preservados, comparar automação ligada/desligada e apurar antes de alterar lógica.

O gate host não é build completo nem teste físico. A publicação de três pacotes e hashes fica condicionada à integração completa, CI da revisão final e homologação real separada por modo e por instalação Root/sem Root; nenhum APK, release, hash ou teste de dispositivo novo é reivindicado aqui.
