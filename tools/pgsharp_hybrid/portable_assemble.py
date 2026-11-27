"""Assemble the private candidate locally using a CI-compiled own DEX and official apksig.

No Android SDK required here; the SDK was used in CI. No network or APK upload.
"""
import argparse
import hashlib
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

from hybrid_audit import sha, entry_sha, inventory, dex_inventory
from private_build import PGSHARP_SHA, SOURCE_CERT, PGBR_CERT, LAUNCHER, old_signature, validate_payload
import axml

HERE = Path(__file__).resolve().parent


def check_bundle():
    manifest = HERE/'SHA256SUMS.txt'
    expected = {'pgbr-panel.dex','apksig.jar','PrivateApkSign.java','portable_assemble.py',
                'hybrid_audit.py','private_build.py','axml.py','APKSIG-LICENSE.txt','apksig.pom'}
    seen = set()
    for line in manifest.read_text(encoding='utf-8').splitlines():
        digest, name = line.split(maxsplit=1)
        name = name.strip().lstrip('*')
        if name in seen or name not in expected or sha(HERE/name) != digest:
            raise ValueError('invalid bundle checksum: '+name)
        seen.add(name)
    if seen != expected: raise ValueError('incomplete bundle')


def sign_tool(*args):
    run = subprocess.run(['java','--class-path',str(HERE/'apksig.jar'),
                          str(HERE/'PrivateApkSign.java'),*map(str,args)],
                         check=True,text=True,capture_output=True)
    return run.stdout.strip()


def check_alignment(path):
    with open(path,'rb') as raw, zipfile.ZipFile(path) as z:
        for item in z.infolist():
            if item.compress_type != zipfile.ZIP_STORED or item.is_dir(): continue
            raw.seek(item.header_offset)
            head = raw.read(30)
            if head[:4] != b'PK\x03\x04': raise ValueError('invalid local ZIP header')
            name_len, extra_len = struct.unpack_from('<HH',head,26)
            offset = item.header_offset+30+name_len+extra_len
            align = 16384 if item.filename.endswith('.so') else 4
            if offset % align: raise ValueError('alignment failed: '+item.filename)


def assemble(base, output, keystore, alias):
    check_bundle()
    if sha(base) != PGSHARP_SHA or sign_tool('verify',base) != SOURCE_CERT:
        raise ValueError('not the pinned verified PGSharp source')
    if output.exists() or not output.parent.is_dir(): raise ValueError('use a new local output')
    data=(HERE/'pgbr-panel.dex').read_bytes(); own=validate_payload(data)
    with tempfile.TemporaryDirectory(dir=output.parent) as td:
        unsigned=Path(td)/'unsigned.apk'; signed=Path(td)/'signed.apk'
        with zipfile.ZipFile(base) as original, zipfile.ZipFile(unsigned,'w') as dest:
            names=inventory(original); dexes=dex_inventory(original)
            if set(dexes)!={'classes.dex','classes2.dex','classes3.dex','classes4.dex'}:
                raise ValueError('unexpected host DEX inventory')
            if own & set.union(*dexes.values()) or 'classes5.dex' in names or any(n.startswith('assets/pgrazor/') for n in names):
                raise ValueError('host collision')
            manifest=original.read('AndroidManifest.xml'); info=axml.inspect_manifest(manifest)
            if info['package']!='com.nianticlabs.pokemongo' or info['versionName']!='0.439.0' or info['split'] or not any(a['name']==LAUNCHER for a in info['activities']):
                raise ValueError('unexpected host identity')
            patched=axml.patch_manifest(manifest)
            for item in original.infolist():
                if old_signature(item.filename): continue
                if item.filename=='AndroidManifest.xml': dest.writestr(item,patched); continue
                with original.open(item) as reader,dest.open(item,'w',force_zip64=False) as writer:
                    shutil.copyfileobj(reader,writer)
            dest.writestr('classes5.dex',data,compress_type=zipfile.ZIP_DEFLATED)
        signer=sign_tool('sign',unsigned,signed,keystore,alias)
        if signer in (SOURCE_CERT,PGBR_CERT) or len(signer)!=64 or sign_tool('verify',signed)!=signer:
            raise ValueError('independent verified test signature required')
        check_alignment(signed)
        with zipfile.ZipFile(base) as original, zipfile.ZipFile(signed) as result:
            expected={n for n in inventory(original) if not old_signature(n)}|{'classes5.dex'}
            if {n for n in inventory(result) if not old_signature(n)}!=expected: raise ValueError('unexpected entries')
            if result.read('AndroidManifest.xml')!=patched or result.read('classes5.dex')!=data: raise ValueError('payload mismatch')
            if any(entry_sha(original,n)!=entry_sha(result,n) for n in expected-{'AndroidManifest.xml','classes5.dex'}):
                raise ValueError('host entry changed')
            if result.testzip() is not None: raise ValueError('CRC failure')
        with signed.open('rb') as src,output.open('xb') as dst: shutil.copyfileobj(src,dst)
    print('APK SHA-256:',sha(output))
    print('Verified test signer:',signer)
    print('device_tested=false; provider_connected=false')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--private-test',action='store_true')
    for key in ('base','output','keystore'): parser.add_argument('--'+key,type=Path,required=True)
    parser.add_argument('--alias',required=True)
    args=parser.parse_args()
    if not args.private_test: parser.error('explicit private authorized test required')
    assemble(args.base,args.output,args.keystore,args.alias)
