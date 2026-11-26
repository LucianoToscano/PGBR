# PGBR — Guia de Auditoria Offline de Datasets Texto/JSON (DATA-01 / DATA-02)

Escopo: `tools/pgbr_dataset_audit/` (Despacho `G-049-08` / LT049).
Finalidade: inventariar e auditar datasets de texto/JSON já embarcados no repositório PGBR de forma **estritamente somente leitura**, sem alterar arquivos, sem duplicar a auditoria de imagens (`tools/pgbr_asset_audit/`, PR #340) e sem tocar no runtime Android ou na correção V5 da tela branca (`BAT-01`).

## Garantias e Limites

1. **Segurança de leitura**:
   - Recusa raiz de auditoria simbólica (`ValueError`) e arquivos não regulares (`os.lstat` / `S_ISREG`: FIFOs, sockets, dispositivos).
   - Leitura limitada a `max_file_bytes + 1` (padrão `4 MiB`); arquivos acima do limite não passam por hashing ilimitado e recebem `sha256=None`, `hash_status="indisponivel_oversized"`.
2. **Separação de esquema e metadados**:
   - **Esquema declarado** (`declared_schema`): preenchido apenas quando o JSON declara explicitamente `$schema`, `schema_version`, `schema`, `format_version` ou `version`.
   - **Esquema inferido** (`inferred_schema`): estrutura observada (`root_type`, contagem e amostra ordenada de chaves), sempre separada do esquema declarado.
   - **Fonte, revisão, data e validade**: registradas apenas quando documentadas no arquivo; caso contrário, marcadas como `"desconhecida"`.
   - **Licença e atribuição**: presença de campo `license`/`attribution` ou arquivo `NOTICE.txt`/`LICENSE*` recebe `status="explicita_nao_validada"` e `license_validated=False` (presença documental não equivale a validação jurídica).
3. **Integridade e privacidade**:
   - Detecta chaves JSON duplicadas e colisões de IDs por namespace sem sobrescrever nem modificar nenhum arquivo.
   - O manifesto determinístico usa exclusivamente caminhos POSIX relativos à raiz do repositório, sem caminhos absolutos da máquina local nem credenciais.
   - Não afirma integração, homologação física (`device_tested=false`) ou conexão com provedores externos (`provider_connected=false`).
