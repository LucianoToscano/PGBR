"""Resolve org.json para os testes JVM de forma simétrica entre compilação e execução.

Regra única: **tudo o que compila entra no classpath de execução**. Foi a violação dessa regra
que derrubou o CI com `NoClassDefFoundError: org/json/JSONObject`: o teste do matcher compilava
com `-classpath android.jar` e depois executava só com o diretório de saída, onde org.json não
estava. Compilar e executar com dependências diferentes é sempre um defeito do teste, nunca do
código sob teste.

Ordem de preferência (a mesma de quem compila o app):

1. fontes do SDK (`$ANDROID_HOME/sources/android-*/org/json/*.java`) compiladas para o diretório
   de trabalho — é literalmente o parser que roda no aparelho;
2. `android.jar` do SDK — mesma origem, já compilado;
3. espelhos versionados em `tools/sniper_jvm/stubs/org/json` — mesma superfície do Android,
   vigiada por `test_json_stub_android_parity.py`.

Sem nenhuma das três, o resultado é inconclusivo com mensagem explícita: nunca um verde falso.
"""

import os
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ROOT = REPO / 'pgrazor'
STUB_JSON = REPO / 'tools/sniper_jvm/stubs/org/json'

ANDROID_PLATFORMS = ('android-36', 'android-36.1', 'android-35', 'android-34')


def sdk_root():
    return Path(os.environ.get('ANDROID_HOME')
                or os.environ.get('ANDROID_SDK_ROOT')
                or str(Path(os.environ.get('LOCALAPPDATA', str(Path.home()))) / 'Android/Sdk'))


def android_jar():
    """android.jar instalado, ou None."""
    sdk = sdk_root()
    for platform in ANDROID_PLATFORMS:
        candidate = sdk / 'platforms' / platform / 'android.jar'
        if candidate.is_file():
            return candidate
    return None


def sdk_json_sources(work):
    """Copia as fontes reais de org.json do SDK para `work` e devolve os caminhos.

    As fontes do AOSP usam anotações e imports internos do framework (`@UnsupportedAppUsage`,
    `libcore.*`) que o JDK não tem; eles são removidos por regex, como o restante da suíte já faz.
    """
    sdk = sdk_root()
    candidates = sorted((sdk / 'sources').glob('android-*/org/json'), reverse=True)
    if not candidates:
        return []
    written = []
    for original in candidates[0].glob('*.java'):
        text = original.read_text(encoding='utf-8')
        text = re.sub(r'^import (?:static )?(?:android\.|libcore\.).*;\s*$', '', text, flags=re.M)
        text = re.sub(r'@(?:UnsupportedAppUsage|SystemApi|NonNull|Nullable)(?:\([^)]*\))?\s*', '', text)
        target = Path(work) / original.name
        target.write_text(text, encoding='utf-8')
        written.append(target)
    return written


def resolution(work):
    """Descreve como org.json será resolvido.

    Devolve um dicionário com:
      mode       'sdk-sources' | 'android-jar' | 'vendored-stubs' | 'missing'
      sources    arquivos .java a compilar junto (vazio quando vem de um jar)
      classpath  entradas extras de classpath, usadas em COMPILAÇÃO E EXECUÇÃO
      needs_jar  True quando as fontes do PGBR precisam de APIs do Android no classpath

    `needs_jar` é independente de org.json: um teste que só use org.json compila sem android.jar.
    """
    sources = sdk_json_sources(work)
    if sources:
        return {'mode': 'sdk-sources', 'sources': sources, 'classpath': [], 'needs_jar': android_jar() is not None}
    jar = android_jar()
    if jar is not None:
        return {'mode': 'android-jar', 'sources': [], 'classpath': [jar], 'needs_jar': True}
    stubs = sorted(STUB_JSON.glob('*.java'))
    if stubs:
        return {'mode': 'vendored-stubs', 'sources': stubs, 'classpath': [], 'needs_jar': False}
    return {'mode': 'missing', 'sources': [], 'classpath': [], 'needs_jar': False}


def describe(options):
    """Resumo legível para o log do CI."""
    parts = [options['mode']]
    if options['sources']:
        parts.append('%d fonte(s)' % len(options['sources']))
    parts += [str(p) for p in options['classpath']]
    return ' '.join(parts)
