"""Private, opt-in PGSharp-base coexistence test. Never fetches or publishes APKs."""
import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

from hybrid_audit import PGSHARP_SHA, sha, inventory, dex_inventory, dex_classes, entry_sha

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
SOURCE_CERT = 'e601d7e0383f0cc1e0f0a4243394e42cafba6e7cedf1af377f9052c3a00a46a4'
PGBR_CERT = '7be443fec5c9d15f7c8eadb47e1c21da4c4319341a6282f74285049ee53d96e7'
LAUNCHER = 'com.nianticproject.holoholo.libholoholo.unity.UnityMainActivity'
OWN = (b'Ldev/pgrazor/BootstrapProvider', b'Ldev/pgrazor/LogActivity', b'Ldev/pgrazor/PgSharpHundoBridge')


def run(*args, capture=False):
    return subprocess.run([str(a) for a in args], check=True, text=True, capture_output=capture)


def tool(folder, name):
    for suffix in ('', '.exe', '.bat'):
        p = folder / (name + suffix)
        if p.is_file():
            return p
    raise ValueError('Android build tool missing: ' + name)


def signer(apksigner, apk):
    result = run(apksigner, 'verify', '--print-certs', apk, capture=True)
    digests = re.findall(r'Signer #\d+ certificate SHA-256 digest:\s*([a-fA-F0-9]{64})', result.stdout)
    if len(digests) != 1:
        raise ValueError('expected one verified APK signer')
    return digests[0].lower()


def validate_payload(data):
    classes = dex_classes(data)
    if not all(any(c == p+b';' or c.startswith(p+b'$') for p in OWN) for c in classes):
        raise ValueError('unexpected payload class')
    if not all(p+b';' in classes for p in OWN):
        raise ValueError('missing own payload class')
    return classes


def compile_payload(sdk, output):
    jar = sdk / 'platforms/android-36/android.jar'
    tools = sdk / 'build-tools/36.0.0'
    if not jar.is_file():
        raise ValueError('Android API 36 platform missing')
    if output.exists():
        raise ValueError('payload output already exists')
    with tempfile.TemporaryDirectory() as td:
        temp = Path(td); classes = temp/'classes'; classes.mkdir(); dex = temp/'dex'; dex.mkdir()
        src = HERE/'java/dev/pgrazor'
        bridge = ROOT/'pgrazor/src/dev/pgrazor/PgSharpHundoBridge.java'
        run('javac', '-encoding','UTF-8','-source','8','-target','8','-classpath',jar,'-d',classes,
            bridge,src/'BootstrapProvider.java',src/'LogActivity.java')
        # Match the ordinary Android build: compile against the SDK as a
        # classpath, then supply a JAR to D8 for desugaring. This avoids
        # replacing javac's JDK lambda metafactory with Android stubs.
        payload_jar = temp/'payload.jar'
        with zipfile.ZipFile(payload_jar, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
            for item in sorted(classes.rglob('*.class')):
                archive.write(item, item.relative_to(classes).as_posix())
        run(tool(tools,'d8'), '--min-api','28','--lib',jar,'--output',dex,payload_jar)
        files = list(dex.glob('*.dex'))
        if len(files) != 1 or files[0].name != 'classes.dex':
            raise ValueError('expected a single minimal DEX')
        data = files[0].read_bytes(); validate_payload(data)
        output.parent.mkdir(parents=True,exist_ok=True)
        with output.open('xb') as out:
            out.write(data)


def old_signature(name):
    n = name.upper()
    return n == 'STAMP-CERT-SHA256' or (n.startswith('META-INF/') and
            (n.endswith(('.SF','.RSA','.DSA','.EC')) or n == 'META-INF/MANIFEST.MF'))


def assemble_private(args):
    # Separate experiment: never call or relax nonroot.py's clean-game guard.
    if not args.private_test:
        raise ValueError('explicit --private-test is required')
    if sha(args.base) != PGSHARP_SHA:
        raise ValueError('unexpected PGSharp input; do not substitute another build')
    if args.output.exists() or not args.output.parent.is_dir():
        raise ValueError('use a new output path in an existing directory')
    if not args.keystore.is_file() or not all(os.environ.get(n) for n in ('PGBR_HYBRID_STORE_PASS','PGBR_HYBRID_KEY_PASS')):
        raise ValueError('local dedicated test signing key and password environment required')
    sdk = args.sdk; tools = sdk/'build-tools/36.0.0'; apksigner = tool(tools,'apksigner')
    if signer(apksigner,args.base) != SOURCE_CERT:
        raise ValueError('PGSharp signer differs from the reviewed public certificate')
    sys.path.insert(0,str(ROOT/'pgrazor'))
    import axml
    with tempfile.TemporaryDirectory(dir=args.output.parent) as td:
        temp=Path(td); payload=temp/'payload.dex'; compile_payload(sdk,payload)
        data=payload.read_bytes(); own=validate_payload(data)
        unsigned=temp/'unsigned.apk'; aligned=temp/'aligned.apk'; signed=temp/'signed.apk'
        with zipfile.ZipFile(args.base) as original, zipfile.ZipFile(unsigned,'w') as output:
            names=inventory(original); dexes=dex_inventory(original)
            if set(dexes) != {'classes.dex','classes2.dex','classes3.dex','classes4.dex'}:
                raise ValueError('unexpected original DEX inventory')
            if own & set.union(*dexes.values()) or 'classes5.dex' in names or any(n.startswith('assets/pgrazor/') for n in names):
                raise ValueError('class or package collision')
            manifest=original.read('AndroidManifest.xml'); info=axml.inspect_manifest(manifest)
            if info['package']!='com.nianticlabs.pokemongo' or info['versionName']!='0.439.0' or info['split'] or not any(a['name']==LAUNCHER for a in info['activities']):
                raise ValueError('unexpected host identity')
            patched=axml.patch_manifest(manifest)
            for item in original.infolist():
                if old_signature(item.filename): continue
                if item.filename=='AndroidManifest.xml': output.writestr(item,patched); continue
                with original.open(item) as reader,output.open(item,'w',force_zip64=False) as writer:
                    shutil.copyfileobj(reader,writer)
            output.writestr('classes5.dex',data,compress_type=zipfile.ZIP_DEFLATED)
        run(tool(tools,'zipalign'),'-P','16','-f','4',unsigned,aligned)
        run(apksigner,'sign','--ks',args.keystore,'--ks-key-alias',args.alias,'--ks-pass','env:PGBR_HYBRID_STORE_PASS',
            '--key-pass','env:PGBR_HYBRID_KEY_PASS','--out',signed,aligned)
        if signer(apksigner,signed) in (SOURCE_CERT,PGBR_CERT):
            raise ValueError('use a dedicated test signing certificate, not the original or stable key')
        run(tool(tools,'zipalign'),'-c','-P','16','4',signed)
        with zipfile.ZipFile(args.base) as original, zipfile.ZipFile(signed) as result:
            original_names=inventory(original); result_names=inventory(result)
            expected={n for n in original_names if not old_signature(n)}|{'classes5.dex'}
            if {n for n in result_names if not old_signature(n)} != expected:
                raise ValueError('unexpected archive changes')
            if result.read('AndroidManifest.xml') != patched or result.read('classes5.dex') != data:
                raise ValueError('manifest or payload differs')
            if any(entry_sha(original,n)!=entry_sha(result,n) for n in expected-{'AndroidManifest.xml','classes5.dex'}):
                raise ValueError('original PGSharp entry changed')
            if result.testzip() is not None: raise ValueError('CRC failure')
        with signed.open('rb') as src,args.output.open('xb') as dst:
            shutil.copyfileobj(src,dst)
    print('Private coexistence candidate SHA-256:',sha(args.output))
    print('device_tested=false; provider_connected=false; no PGSharp data integration claimed')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk',type=Path,required=True)
    sub=parser.add_subparsers(dest='action',required=True)
    compile_cmd=sub.add_parser('compile-payload');compile_cmd.add_argument('--output',type=Path,required=True)
    build=sub.add_parser('assemble-private');build.add_argument('--private-test',action='store_true')
    for n in ('base','output','keystore'): build.add_argument('--'+n,type=Path,required=True)
    build.add_argument('--alias',required=True)
    args=parser.parse_args()
    if args.action=='compile-payload': compile_payload(args.sdk,args.output)
    else: assemble_private(args)


if __name__=='__main__':
    main()
