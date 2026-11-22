#!/usr/bin/env python3
"""Reconciliador conservador de Pokémon GO, sem rede e sem alterar fontes.

Uso: python tools/pokemon_data/reconciliar.py --pokemon pogo_pkm.json --game-master latest.json --out resultado
Saídas: pokemon_confirmados.json, pokemon_conflitos.json, pokemon_invalidos.json, manifesto_importacao.json.
A saída NUNCA indica que a fonte representa os dados atuais do jogo.
"""
import argparse
import collections
import datetime as dt
import hashlib
import json
from pathlib import Path


def read_json(path):
    raw = path.read_bytes()
    obj = json.loads(raw)
    if not isinstance(obj, list):
        raise ValueError(f'Esperada lista JSON: {path}')
    return obj, {'filename': path.name, 'size_bytes': len(raw), 'sha256': hashlib.sha256(raw).hexdigest()}


def stable(item):
    return json.dumps(item, sort_keys=True, ensure_ascii=False, separators=(',', ':'))


def export_json(path, data):
    temp = path.with_name(path.name + '.tmp')
    temp.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    temp.replace(path)


def reconcile(pokemon_file, game_master_file, out):
    pokemon, pmeta = read_json(pokemon_file)
    gm, gmmeta = read_json(game_master_file)
    if any(not isinstance(x, dict) or not isinstance(x.get('templateId'), str) for x in gm):
        raise ValueError('Game Master: templateId ausente ou inválido')
    tids = [x['templateId'] for x in gm]
    if len(set(tids)) != len(tids):
        raise ValueError('Game Master contém templateId duplicados; interrompendo')
    grouped = collections.defaultdict(list)
    invalid = []
    for index, item in enumerate(pokemon):
        if not isinstance(item, dict) or type(item.get('id')) is not int or not isinstance(item.get('form'), str):
            invalid.append({'source_index': index, 'raw': item})
            continue
        grouped[(item['id'], item['form'])].append({'source_index': index, 'raw': item})
    confirmed = []
    conflicts = []
    for (species_id, form), variants in sorted(grouped.items()):
        representations = {stable(v['raw']) for v in variants}
        provenance = {'source_file': pmeta['filename'], 'sha256': pmeta['sha256'], 'source_indices': [v['source_index'] for v in variants]}
        if len(representations) == 1:
            confirmed.append({'id': species_id, 'form': form, 'data': variants[0]['raw'], 'provenance': provenance})
        else:
            distinct = {}
            for v in variants:
                distinct.setdefault(stable(v['raw']), {'data': v['raw'], 'source_indices': []})['source_indices'].append(v['source_index'])
            conflicts.append({'id': species_id, 'form': form, 'status': 'REVIEW_REQUIRED', 'variants': list(distinct.values()), 'provenance': provenance})
    manifest = {
        'format_version': 1,
        'generated_utc': dt.datetime.now(dt.timezone.utc).isoformat(),
        'policy': 'Never auto-resolve conflicting (id, form); preserve raw sources. No live/release claims.',
        'input': {'pokemon': pmeta, 'game_master': gmmeta},
        'stats': {
            'pokemon_records': len(pokemon), 'pokemon_unique_keys': len(grouped),
            'pokemon_duplicate_groups': sum(len(v) > 1 for v in grouped.values()),
            'pokemon_excess_duplicate_rows': sum(len(v) - 1 for v in grouped.values()),
            'pokemon_conflict_groups': len(conflicts), 'pokemon_confirmed_keys': len(confirmed),
            'pokemon_invalid_rows': len(invalid), 'game_master_templates': len(gm),
        },
        'artifacts': ['pokemon_confirmados.json', 'pokemon_conflitos.json', 'pokemon_invalidos.json'],
        'notes': ['game_master_snapshot_only; timestamp or in-game availability unverified',
                  'sprites and third-party model redistribution requires separate license review',
                  'no sprites, builds, Android app or GitHub release were modified'],
    }
    out.mkdir(parents=True, exist_ok=True)
    export_json(out / 'pokemon_confirmados.json', confirmed)
    export_json(out / 'pokemon_conflitos.json', conflicts)
    export_json(out / 'pokemon_invalidos.json', invalid)
    export_json(out / 'manifesto_importacao.json', manifest)
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pokemon', required=True, type=Path)
    parser.add_argument('--game-master', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    print(json.dumps(reconcile(args.pokemon, args.game_master, args.out)['stats'], indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
