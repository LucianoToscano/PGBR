"""Rótulos de fruta e nomes de jogador: contratos extraídos do código real.

Duas classes de defeito que o relato físico de 26/11 descreve como "traduções erradas":

1. Um nome de fruta exibido na tela vinha de um rótulo em inglês passado pelo dicionário. Isso
   funciona enquanto o texto é exatamente a chave; assim que a mesma frase é montada com uma
   contagem — o que o seletor de prioridade faz — a tradução só chega pela regra de prefixo, e
   uma prioridade salva pode aparecer meio traduzida.
2. Nomes digitados ou importados pelo jogador (nome de local, de rota) eram passados pelo
   tradutor. Um local chamado "Route" virava "Rota" e um chamado "Gym" virava "Ginásio": o
   aplicativo renomeava o dado do usuário na tela.

Os testes abaixo não simulam Android: leem o código-fonte e o dicionário reais.
"""
import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

from test_control_java import java_tools

ROOT = Path(__file__).resolve().parents[1]
GLOSSARY = ROOT / 'i18n' / 'pt-BR.json'


def entries():
    return json.loads(GLOSSARY.read_text(encoding='utf-8'))['translations']


class ItemBerryLabelTests(unittest.TestCase):
    def test_berry_names_come_from_the_id(self):
        """Executa BerryPriority.displayName num JVM real, junto do dicionário."""
        compiler, runtime = java_tools()
        with tempfile.TemporaryDirectory(prefix='pgrazor-berry-labels-') as directory:
            work = Path(directory)
            sources = [ROOT / 'src/dev/pgrazor/BerryPriority.java', ROOT / 'test/ItemBerryLabelTest.java']
            compiled = subprocess.run([str(compiler), '-encoding', 'UTF-8', '-d', str(work), *map(str, sources)],
                                      capture_output=True, text=True, timeout=60)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(runtime), '-cp', str(work), 'ItemBerryLabelTest'],
                                    capture_output=True, text=True, timeout=60)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('PASS: berry labels', result.stdout)

    def test_berry_dictionary_values_match_the_java_names(self):
        """As cinco frutas têm exatamente o mesmo nome no dicionário e no código."""
        tr = entries()
        expected = {
            701: 'Razz Berry',
            703: 'Nanab Berry',
            705: 'Pinap Berry',
            706: 'Golden Razz Berry',
            707: 'Silver Pinap Berry',
        }
        java = (ROOT / 'src/dev/pgrazor/BerryPriority.java').read_text(encoding='utf-8')
        for key in expected.values():
            with self.subTest(berry=key):
                self.assertIn(key, tr, 'fruta ausente do dicionário: ' + key)
        # O pareamento ID -> nome segue o sprite, não a ordem em que os rótulos aparecem:
        # item-703 é a penca de bananas e item-705 é o abacaxi.
        by_id = {701: 'Fruta Frambo', 703: 'Fruta Caxí', 705: 'Fruta Abacaxi',
                 706: 'Fruta Frambo Dourada', 707: 'Fruta Abacaxi Prateada'}
        for id_, name in by_id.items():
            with self.subTest(id=id_):
                self.assertEqual(name, tr[expected[id_]])
        for name in ('Fruta Frambo', 'Fruta Abacaxi', 'Fruta Caxí',
                     'Fruta Frambo Dourada', 'Fruta Abacaxi Prateada'):
            with self.subTest(name=name):
                self.assertIn('"%s"' % name, java, 'nome não vem do código: ' + name)
                self.assertIn(name, tr.values(), 'nome sem par no dicionário: ' + name)

    def test_screens_no_longer_translate_a_composed_berry_line(self):
        """O seletor e o botão de fruta não passam mais a frase montada pelo tradutor."""
        picker = (ROOT / 'src/dev/pgrazor/BerryPriorityPicker.java').read_text(encoding='utf-8')
        self.assertIn('BerryPriority.displayName', picker)
        self.assertNotIn('PtBr.tr(NAMES[i]', picker, 'rótulo inglês ainda traduzido em tempo de tela')
        overlay = (ROOT / 'src/dev/pgrazor/BerryOverlay.java').read_text(encoding='utf-8')
        self.assertIn('BerryPriorityPicker.display(index(id))', overlay)
        self.assertNotIn('"Catch berry: "+NAMES[index(id)]', overlay)
        # O nome inglês puro continua existindo (é a chave do dicionário), mas não chega à tela.
        self.assertIn('static final String[] NAMES=', picker)

    def test_berry_fragments_exist_for_the_overlay(self):
        tr = entries()
        for key in ('Catch berry: ', 'available', 'Tap to choose berry priorities.', ', priority '):
            with self.subTest(key=key):
                self.assertIn(key, tr)
                self.assertNotEqual(tr[key], key)


class GalleryLabelByIdTests(unittest.TestCase):
    """A galeria de itens rotula pelo ID, e o par ID→fruta segue a tabela do protocolo."""

    def gallery(self):
        source = (ROOT / 'src/dev/pgrazor/LocalAuditDialog.java').read_text(encoding='utf-8')
        panel = source.split(' static String galleryLabel(int id){', 1)[1].split(' static void tagReview(Activity a){', 1)[0]
        labels = {int(i): t for i, t in re.findall(r'case (\d+):return "([^"]*)"', panel)}
        ids = [int(x) for x in re.search(r'GALLERY_IDS=\{([0-9,]+)\}', source).group(1).split(',')]
        return source, ids, labels

    def test_each_berry_id_carries_the_right_name(self):
        _, _, labels = self.gallery()
        self.assertEqual({
            701: 'Fruta Frambo', 702: 'Fruta Bluk', 703: 'Fruta Caxí', 704: 'Fruta Wepear',
            705: 'Fruta Abacaxi', 706: 'Fruta Frambo Dourada', 707: 'Fruta Caxí Dourada',
            708: 'Fruta Abacaxi Prateada', 709: 'Poffin',
        }, {i: labels[i] for i in range(701, 710)})

    def test_nanab_and_pinap_are_not_swapped(self):
        """703 é a banana (Nanab) e 705 o abacaxi (Pinap); trocar os dois é o defeito que existia."""
        _, _, labels = self.gallery()
        self.assertEqual('Fruta Caxí', labels[703], '703 é Nanab (banana)')
        self.assertEqual('Fruta Abacaxi', labels[705], '705 é Pinap (abacaxi)')
        self.assertNotIn('Anaba', ''.join(labels.values()), 'grafia "Anaba" não existe')
        self.assertNotIn('Caxi', ''.join(labels.values()), '"Caxí" leva acento')

    def test_every_label_is_portuguese_and_accented(self):
        _, _, labels = self.gallery()
        for id_, label in labels.items():
            with self.subTest(id=id_):
                self.assertTrue(label.strip(), 'rótulo vazio')
                for english in ('Berry', 'Ball', 'Potion', 'Incubator', 'Raid Pass', 'TM', 'Camera'):
                    self.assertNotIn(english, label, 'inglês no rótulo %d: %s' % (id_, label))
        # Nomes que perderam acento no código.
        for wrong, right in (('Caxi', 'Caxí'), ('Caxí Dourada', 'Caxí Dourada')):
            self.assertNotIn(wrong, labels.values())
        self.assertEqual('Fruta Caxí Dourada', labels[707])

    def test_confirmed_families_are_labelled(self):
        """Módulos, MT, evolução, reides: o que a tabela de itens confirma tem rótulo."""
        _, _, labels = self.gallery()
        for id_ in (501, 502, 503, 504, 505, 506, 1101, 1102, 1103, 1104, 1105, 1106, 1107,
                    1201, 1202, 1203, 1204, 1301):
            with self.subTest(id=id_):
                self.assertIn(id_, labels)
        self.assertIn('MT', labels[1201], 'MT de ataque rápido')

    def test_unconfirmed_sprites_are_marked_and_not_named(self):
        """O PNG que decodifica não prova identidade visual: o placeholder TMP é sinalizado."""
        source, ids, labels = self.gallery()
        block = re.search(r'static boolean spriteIdentityUnconfirmed\(int id\)\{(.*?)\}', source, re.S).group(1)
        flagged = {int(x) for x in re.findall(r'id==(\d+)', block)}
        self.assertIn(1609, flagged, 'item-1609 é o placeholder "TMP"')
        self.assertIn(1350, flagged)
        for id_ in flagged:
            with self.subTest(id=id_):
                self.assertNotIn(id_, labels, 'não invente nome para sprite sem referência: %d' % id_)
        self.assertIn('identidade visual não confirmada', source)

    def test_placeholder_sprite_is_documented_not_named(self):
        """O 1609 continua embarcado, mas o teste registra que ele não é um sprite de item."""
        _, ids, labels = self.gallery()
        self.assertIn(1609, ids)
        self.assertNotIn(1609, labels)


class PlayerNameIsNotTranslatedTests(unittest.TestCase):
    def test_saved_names_are_shown_verbatim(self):
        """Nome de local e de rota são dados do jogador: exibidos como foram salvos."""
        map_ = (ROOT / 'src/dev/pgrazor/WorldMapDialog.java').read_text(encoding='utf-8')
        self.assertIn('labelRaw(ui,entry.name,17)', map_)
        self.assertIn('labelRaw(ui,name,17)', map_)
        self.assertNotIn('label(ui,entry.name,17)', map_)
        self.assertNotIn('label(ui,name,17)', map_)
        self.assertIn('private static TextView labelRaw(', map_)
        gpx = (ROOT / 'src/dev/pgrazor/GpxRouteDialog.java').read_text(encoding='utf-8')
        self.assertIn('setTitle(entry.name)', gpx)
        self.assertNotIn('PtBr.tr(entry.name)', gpx)
        self.assertIn('name.setText(current.name);', gpx)
        self.assertNotIn('PtBr.tr(current.name)', gpx)

    def test_hotspot_proper_nouns_are_not_translated(self):
        """Nome de cidade/parque é nome próprio: não passa pelo tradutor."""
        source = (ROOT / 'src/dev/pgrazor/HotspotsDialog.java').read_text(encoding='utf-8')
        self.assertIn('setTitle(spot.name)', source)
        self.assertNotIn('PtBr.tr(spot.name)', source)

    def test_no_screen_translates_a_stored_entity_name(self):
        """Nenhum ponto do código traduz o nome de uma entidade salva."""
        offenders = []
        for source in sorted((ROOT / 'src/dev/pgrazor').glob('*.java')):
            text = source.read_text(encoding='utf-8')
            for match in re.finditer(r'PtBr\.tr\(\s*(?:entry|place|route|current)\.name\s*\)', text):
                offenders.append('%s: %s' % (source.name, match.group(0)))
        self.assertEqual([], offenders, 'nome do jogador passou pelo tradutor: %s' % offenders)


class FixedBerryDictionaryTests(unittest.TestCase):
    def test_no_mixed_spelling_of_the_pinap_berry(self):
        """A fruta Pinap aparece como 'Abacaxi' em todo o dicionário, nunca como 'Caxi'."""
        tr = entries()
        wrong = [(k, v) for k, v in tr.items() if re.search(r'Pinap', k) and re.search(r'\bCaxi\b', v)]
        self.assertEqual([], wrong, 'Pinap traduzida como Caxí: %s' % wrong[:3])

    def test_golden_berry_is_capitalised_like_the_gallery(self):
        """'Dourada' com maiúscula, igual ao rótulo da galeria de itens."""
        tr = entries()
        self.assertIn('Frambo Dourada', tr['Use a Golden Razz Berry on bonus catches'])
        self.assertIn('Frambo Dourada', tr['Golden Razz Berry'])

    def test_pokestop_spelling_is_consistent(self):
        """Poképarada numa só grafia: a minúscula usada nos outros nove rótulos."""
        tr = entries()
        wrong = [(k, v) for k, v in tr.items() if 'PokéParada' in v]
        self.assertEqual([], wrong, 'grafia divergente: %s' % wrong)

    def test_dictionary_has_no_duplicate_key(self):
        """Chave repetida no JSON: a última vence silenciosamente, mascarando uma correção."""
        text = GLOSSARY.read_text(encoding='utf-8')
        keys = re.findall(r'^    ("(?:[^"\\]|\\.)*"):', text, flags=re.M)
        duplicates = sorted({k for k in keys if keys.count(k) > 1})
        self.assertEqual([], duplicates, 'chaves repetidas: %s' % duplicates)

    def test_both_uis_carry_the_same_entries(self):
        """PtBr e RootPtBr têm exatamente as chaves do dicionário, com os mesmos valores."""
        tr = entries()
        pair = re.compile(r'm\.put\(("(?:[^"\\]|\\.)*"), ("(?:[^"\\]|\\.)*")\);')
        for relative in ('src/dev/pgrazor/PtBr.java',
                         'root-installer/src/dev/pgrazor/root/RootPtBr.java'):
            with self.subTest(source=relative):
                text = (ROOT / relative).read_text(encoding='utf-8')
                have = {json.loads(a): json.loads(b) for a, b in pair.findall(text)}
                self.assertEqual([], [k for k in tr if k not in have], 'faltando em ' + relative)
                self.assertEqual([], [k for k in have if k not in tr], 'sobrando em ' + relative)
                self.assertEqual([], [k for k in have if tr.get(k) != have[k]], 'divergente em ' + relative)
        # Nenhuma chave pode guardar um escape duplo (\\n), que nunca casa com o texto real.
        for relative in ('src/dev/pgrazor/PtBr.java',
                         'root-installer/src/dev/pgrazor/root/RootPtBr.java'):
            text = (ROOT / relative).read_text(encoding='utf-8')
            self.assertNotIn('\\\\\\\\n', text, 'chave com escape duplicado em ' + relative)


if __name__ == '__main__':
    unittest.main()
