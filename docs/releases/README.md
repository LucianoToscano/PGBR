# Formato das notas de lançamento

Use este formato em todos os lançamentos, inclusive experimentais. Copie [TEMPLATE.md](TEMPLATE.md) para `docs/releases/vVERSION.md` e substitua os marcadores.

- **Novos recursos:** explique cada funcionalidade em uma ou duas frases curtas.
- **Melhorias e correções:** use itens curtos descrevendo o resultado. Agrupe pequenas correções e ajustes de interface quando fizer sentido; omita seções vazias.
- Não inclua seções de verificação, contagens de testes, detalhes de compilação ou alterações restritas ao site. Guarde verificações e diagnósticos nos comprovantes internos. Mantenha `CHANGELOG.md` conciso e sem alterações exclusivas do site.
- Evite alegações de sucesso sem comprovação. Acrescente uma limitação breve somente quando for necessária para compreender um recurso.
- Inclua os links específicos para APK sem Root, instalador Root, ZIP do módulo e lançamento no GitHub. Mantenha os links de apoio opcional e repositório em [footer.md](footer.md), sujeitos à decisão de exibição pública.

O fluxo de lançamento publica as notas versionadas seguidas de `footer.md`. O Discord usa o mesmo conteúdo da versão no GitHub; escreva apenas uma vez, sem uma versão paralela e extensa para Discord. O Discord também adiciona campos de download para os três artefatos e vincula o título ao lançamento GitHub.

Na publicação manual, acrescente o rodapé somente uma vez, como no fluxo automático. Revise as notas e os links após renderizados antes de publicar. Não envie novamente um anúncio antigo do Discord apenas para alterar sua formatação.
