# PGBR — Guia de Auditoria Offline de Datasets Texto/JSON (DATA-01 / DATA-02)

Escopo: `tools/pgbr_dataset_audit/` (Despachos `G-049-08` e `G-049-09` / LT049).
Finalidade: inventariar e auditar datasets de texto/JSON **embarcados autorizados** no repositório PGBR de forma **estritamente somente leitura**, sem alterar arquivos, sem duplicar a auditoria de imagens (`tools/pgbr_asset_audit/`, PR #340) e sem tocar no runtime Android ou na correção V5 da tela branca (`BAT-01`).

## Garantias de Robustez, Segurança e Privacidade (G-049-09)

1. **Segurança de leitura e limites**:
   - Recusa raiz simbólica (`ValueError`), diretórios/arquivos simbólicos e arquivos não regulares (`os.lstat` / `S_ISREG`).
   - Leitura binária limitada (`O_BINARY` no Windows + teto `max_file_bytes + 1`, padrão `4 MiB`); arquivos acima do limite não sofrem hashing ilimitado (`sha256=None`, `hash_status="indisponivel_oversized"`, `partial_coverage=True`).
   - Erros de E/S (`OSError`, permissão negada, arquivo removido) são contabilizados em `inaccessible_count` sem derrubar o inventário nem expor caminhos absolutos do host.
2. **Parsing estrito de JSON e CSV/TSV**:
   - JSON rejeita `NaN`, `Infinity`, `-Infinity` e inteiros acima do limite de dígitos (`max_int_digits=300`); captura `RecursionError` e aplica limites iterativos de profundidade (`max_json_depth=64`) e nós (`max_json_nodes=200000`).
   - CSV/TSV utiliza parsing incremental com `strict=True` e limites explícitos de colunas (`256`), linhas (`50000`), tamanho de campo (`4096`) e consistência de colunas por linha.
3. **Privacidade estrita no manifesto**:
   - **Nunca** copia chaves arbitrárias, valores de IDs, nomes de colunas, strings livres de origem/revisão/licença/esquema, mensagens de exceção ou caminhos inválidos para o manifesto.
   - Emite apenas contagens, categorias controladas e valores comprovadamente públicos (ex.: lista allowlist SPDX).
   - Descarta URLs com credenciais (`user:pass@`), queries (`?...`), fragmentos, esquemas não-HTTPS ou caminhos absolutos do host (`documented_source_status="descartada_insegura"`).
   - Exclui arquivos locais de usuário, backups, sessões, perfis e segredos (`excluded_sensitive_count`).
   - Valida referências esperadas exigindo caminhos relativos POSIX limpos e rejeitando `..`/traversal.
4. **Esquema, validade temporal e cobertura parcial**:
   - Separa `declared_schema` (presença + chave declarada) de `inferred_schema` (tipo raiz + contagens).
   - Datas sem offset de fuso horário **não** têm UTC inferido: recebem `validity_status="validade_ambigua_sem_fuso"`.
   - Limites de inspeção de IDs (`max_id_records`) sinalizam explicitamente `id_inspection.truncated=True` e `partial_coverage=True`.
   - Presença de `NOTICE.txt`/licença é marcada como `explicita_nao_validada` com `license_validated=False`.
   - Não afirma integração, homologação física (`device_tested=false`) ou conexão com provedores externos.
