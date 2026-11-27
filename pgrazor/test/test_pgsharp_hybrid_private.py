"""Private hybrid preflight tests. No third-party APK needed in CI."""
import os
import shutil
import struct
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / 'tools/pgsharp_hybrid'
# Support local execution of the checked source outside the repository.
if not TOOLS.is_dir():
    TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))
import hybrid_audit as audit
import private_build as build


def dex_fixture(names):
    """Minimal header/tables for the descriptor reader, NOT executable DEX."""
    n = len(names); so = 112; to = so + 4*n; co = to + 4*n
    data = bytearray(co + 32*n)
    data[:8] = b'dex\n035\0'
    for offset, value in [(36,112),(40,0x12345678),(56,n),(60,so),(64,n),(68,to),(96,n),(100,co)]:
        struct.pack_into('<I', data, offset, value)
    for i, name in enumerate(names):
        struct.pack_into('<I', data, so+4*i, len(data))
        struct.pack_into('<I', data, to+4*i, i)
        struct.pack_into('<I', data, co+32*i, i)
        data.extend(bytes([len(name)])+name+b'\0')
    struct.pack_into('<I', data, 32, len(data))
    return bytes(data)


class HybridPrivateTests(unittest.TestCase):
    def test_allowlist(self):
        names = [p+b';' for p in build.OWN]
        self.assertEqual(build.validate_payload(dex_fixture(names)),set(names))
        for invalid in (names[:-1], names+[b'Ldev/pgrazor/NativeCore;'], names+[b'Lthird/Party;'], names+[names[0]]):
            with self.assertRaises(ValueError):
                build.validate_payload(dex_fixture(invalid))

    def test_wrong_input_hash_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'input.apk';p.write_bytes(b'not an APK')
            with self.assertRaisesRegex(ValueError,'audited private pair'):
                audit.audit(p,p)

    def test_signature_exclusion_is_narrow(self):
        for name in ('META-INF/HELA.RSA','META-INF/HELA.SF','META-INF/MANIFEST.MF','stamp-cert-sha256'):
            self.assertTrue(build.old_signature(name))
        for name in ('resources.arsc','META-INF/androidx.version','lib/arm64-v8a/libmain.so','assets/pgrazor/mode.json'):
            self.assertFalse(build.old_signature(name))

    def test_compile_payload_when_android_sdk_present(self):
        sdk=Path(os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME') or '/nonexistent-sdk')
        if not (sdk/'platforms/android-36/android.jar').is_file() or not shutil.which('javac'):
            self.skipTest('Android API 36 SDK or JDK unavailable')
        build.tool(sdk/'build-tools/36.0.0','d8')
        with tempfile.TemporaryDirectory() as d:
            payload=Path(d)/'payload.dex';build.compile_payload(sdk,payload)
            self.assertTrue(build.validate_payload(payload.read_bytes()))


if __name__ == '__main__':
    unittest.main()
