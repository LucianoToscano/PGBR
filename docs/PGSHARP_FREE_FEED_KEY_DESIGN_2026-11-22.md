# PGBR — Acesso PGSharp gratuito e licença opcional (22/11/2026)

## Requisito
A edição Free do PGSharp inclui **100 IV Feed** e a Standard inclui **All Coordinates Feed**, conforme [site oficial](https://www.pgsharp.com/) e [descrição dos Quick Snipers](https://www.pgsharp.com/features/). O proprietário já documentou sua declaração de autorização no Guia Mestre PokeSearch (LT364/LT365); não exigir repetição.

## Evidência técnica
Plugin PGSharp 1.278.0: serviço de Pokémon em https://f.pgsharp.com/peanuts; métodos Dalvik de consulta Ll/a/a/n6/q4;->d(k5/b) e leitura de resultados Ll/a/a/n6/q4;->e(k5/b); chaves FASTSNIPE, FASTSNIPE2, FEEDS_BASIC, FEEDS_ADVANCED e post.auth.lic. **Ainda não há especificação completa e validada de HTTP, autenticidade, resposta ou autenticação externa com key.** Uma URL estática não equivale a uma API pronta.

## Implementado na PR #73
- Nova sexta opção independente no menu de buscas, sem alterar índices 0–4 nem as fontes anteriores.
- PgSharpFeedPolicy.java: separação entre Free 100% IV e Standard; chave guardada localmente NÃO equivale a key verificada pelo provedor; ambas as modalidades exigem conector de rede validado.
- PgSharpFeedDialog.java: interface de configuração, estados transparentes e acesso à fonte 100% atual do PGBR com seu nome verdadeiro. Não transmite key nem consulta serviço PGSharp nesta etapa.
- PgSharpLicenseVault.java: armazenamento opt-in de key com AES-256/GCM em AndroidKeyStore; arquivo fora do backup Android e exclusão local; entrada mascarada/protegida e sem logs de segredos.
- Testes JVM, regras estáticas e CI Android associados.

Base: PR #72, commit c3430532a363ca0704e713c6a4a2c7e492c301d6. Branch: feat/pgsharp-free-key-tier-20261122. PR dependente: https://github.com/LucianoToscano/PGBR/pull/73

## Pendências (não confundir com implementação)
1. Confirmar os requisitos reais do serviço gratuito e validar o contrato de obtenção dos mesmos resultados 100% IV do PGSharp.
2. Implementar transporte verificável sem supor headers, método HTTP ou esquema JSON.
3. Implementar validação da key Standard no sistema do fornecedor com tratamento de limite, expiração e revogação.
4. Normalizar spawns por espécie, IV, coordenadas, horário/despawn, fonte e estado; recusar resultados expirados e nunca criar aparições simuladas.
5. Homologar no Android físico, sem apagar versões existentes e com confirmação explícita de teleporte.

**Status factual:** UI, política e cofre de key em código; consultas externas e login PGSharp não implementados. O Sniper 100% IV anterior do PGBR mantém sua fonte FaustusFoundry. Nenhum APK instalável novo foi publicado.

## 25/11/2026 — Requisito de provedor opcional com licença pessoal

- O proprietário declara possuir autorização para a integração e uma licença própria para teste; cada usuário deve adquirir e cadastrar a sua credencial no aparelho. O PGBR não comercializa, compartilha ou incorpora chaves. Não considerar essa declaração documentação de um contrato técnico do provedor.
- Escopo: somente filtros e resultados de pesquisa PGSharp, com interoperabilidade de Sniper 1/2 e Radar; preservar demais módulos. Preferir SDK/interface autorizada independente. Não embutir APK completo, não presumir que partes DEX/.so constituam módulo isolável, não inventar requisições, não transmitir credenciais a terceiros.
- PR #302 integrada em fe142f7d0bc5f4acf11357f9c2b2edc64c8bd11d, CI 45571206249/45571206201/45571206130 aprovado: atalho de importação local e indicação dos critérios não convertidos. #303 acrescenta validação dos campos numéricos e aliases CSV; CI a confirmar no HEAD final.
- A documentação pública https://www.pgsharp.com/features/ e https://www.pgsharp.com/ confirma o recurso comercial, não especifica API externa, autenticação ou schema. Permanecem necessários contrato autorizado, proveniência dos componentes e testes do serviço. Credencial pessoal fica no cofre local já existente; nenhuma consulta ao serviço nem APK novo comprovados.
