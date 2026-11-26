"""Contrato do org.json usado no harness JVM.

O app é escrito contra o org.json do Android. Um espelho que diverge (por exemplo, lançar em
`get()` de chave ausente, ou devolver Map/List crus no parse) faz o teste reprovar código correto
— foi exatamente a causa de "No value for species" e "species must be a list." no CI. Cada
asserção abaixo cita o comportamento documentado do Android.
"""

import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
STUB = ROOT.parent / 'tools/sniper_jvm/stubs/org/json'


class JsonStubParityTest(unittest.TestCase):
    def read(self, name):
        return (STUB / name).read_text(encoding='utf-8')

    def test_missing_key_throws_like_android(self):
        """Android: get() de chave ausente lança "No value for <chave>".

        O espelho precisa ser RÍGIDO aqui: um get() permissivo esconde o defeito real de
        SnipeFilters.fromJson, que chamava get("species") sem has() e quebrava todo slot salvo
        só com {"enabled":true}. Quem quer tolerância usa has()/opt().
        """
        body = self.read('JSONObject.java')
        get_body = body.split('public Object get(String key)')[1]
        get_body = get_body.split('{', 1)[1].split('}', 1)[0]
        code = '\n'.join(line for line in get_body.splitlines()
                         if not line.strip().startswith('//'))
        self.assertIn('throw new JSONException("No value for " + key)', code)

    def test_parse_converts_nested_objects_and_arrays(self):
        """Android: o parse devolve instâncias de JSONObject/JSONArray, nunca Map/List crus."""
        self.assertIn('static Object convert(Object raw)', self.read('Json.java'))
        self.assertIn('Json.convert(new Json(text).parseValue())', self.read('JSONObject.java'))

    def test_null_marker_and_is_null_exist(self):
        """Android: JSONObject.NULL é um marcador; opt() dele devolve null e isNull() é verdadeiro."""
        body = self.read('JSONObject.java')
        self.assertIn('public static final Object NULL', body)
        self.assertIn('public boolean isNull(String key)', body)

    def test_null_marker_serializes_as_bare_null(self):
        """Android: JSONObject.NULL vira `null` sem aspas — não a STRING "null".

        O espelho gravava send\"null\" e a releitura devolvia o TEXTO "null". Um backup com
        name/id nulos passava como válido no JVM e falhava no aparelho: foi a causa real de
        "SettingsBackupRulesTest: Accepted invalid backup" (campo nulo virava nome "null").
        """
        write = self.read('Json.java').split('private static String writeValue(Object value)')[1]
        write = '\n'.join(write.splitlines()[:6])  # só o começo do método, não o arquivo todo
        self.assertIn('value == JSONObject.NULL', write)

    def test_android_types_used_by_the_app_are_present(self):
        """Tipos que o app usa de verdade precisam existir, senão o teste mede o espelho, não o app."""
        self.assertTrue((STUB / 'JSONTokener.java').exists(), 'Android tem JSONTokener')
        array = self.read('JSONArray.java')
        for method in ('getInt(int index)', 'optDouble(int index, double fallback)',
                       'optJSONArray(int index)', 'public JSONArray(String text)'):
            self.assertIn(method, array, 'JSONArray precisa de ' + method)


if __name__ == '__main__':
    unittest.main()
