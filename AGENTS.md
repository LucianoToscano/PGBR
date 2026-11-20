# AGENTS.md — PGBR (instruções para IA auditora)

Este arquivo acompanha exclusivamente a **cópia de auditoria** do PGBR v0.00.09-LT. Antes de propor mudanças, leia `AUDITORIA_IA_COMECE_AQUI.md`, `README.md`, `docs/BUILDING.md`, `docs/ARCHITECTURE.md`, `docs/ROOTED.md` e `THIRD_PARTY.md`.

**Regra mestre:** enriquecer e nunca empobrecer — mantenha tudo que já funciona. Faça mudanças mínimas, justificadas por evidência e testadas, e preserve traduções PT-BR, acessibilidade e configurações existentes. Não toque no projeto PokeSearch.

**Limites:** sem acesso neste ZIP ao APKM privado do Pokémon GO, à chave de assinatura ou a dados pessoais; não gere nem solicite segredos; não alegue compilação/instalação que você não executou. Não altere identidade/autenticação do jogador, nem use fontes privadas da biblioteca externa sem checar direitos.

**Saída obrigatória:** relatório por problema com arquivo:linha, causa raiz, correção, testes executados, testes não executados, risco e status. Separar resultados estáticos, testes de unidade, compilação Android, homologação em aparelho e publicação.

**Prioridades:** evitar toque acidental na VGP com opção; sprites completos do catálogo; ocultar nome só na renderização (sem alterar identidade real); evento Eternatus automático com confirmação verdadeira de pesquisas e cooldown; elementos Eternatus transparentes/epic; legibilidade de HUD/tempo/cooldown/menus; compatibilidade Root/sem Root. Veja questões de aceitação detalhadas em `AUDITORIA_IA_COMECE_AQUI.md`.

**Publicação:** as versões oficiais são criadas apenas pela CI após verificações. Não expor `.jks`, `.apkm` privado, tokens, credenciais ou logs com dados pessoais. Não reescrever a release existente nem forçar commits na `main` sem autorização.
