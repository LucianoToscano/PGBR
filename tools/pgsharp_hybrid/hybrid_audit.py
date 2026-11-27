"""Offline compatibility audit. No APK modification or provider traffic."""
import hashlib
import json
import re
import struct
import sys
import zipfile
from pathlib import Path

PGSHARP_SHA = '77650a0783117fafed993f327f220ae5eb784efbf18d033c7d809262ed244f34'
PGBR_SHA = 'a705f220589017fe2134c42b9fc3242e04311e8b365b41c8c4407a5a4ec2ad3b'
GAME = 'lib/arm64-v8a/'
METADATA = 'assets/bin/Data/Managed/Metadata/global-metadata.dat'
DEX = re.compile(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex\Z')


def sha(path):
    with open(path, 'rb') as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()


def entry_sha(z, name):
    with z.open(name) as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()


def inventory(z):
    names = z.namelist()
    if len(set(names)) != len(names):
        raise ValueError('duplicate archive path')
    return set(names)


def dex_classes(data):
    def u32(pos):
        return struct.unpack_from('<I', data, pos)[0]
    if len(data) < 112 or not data.startswith(b'dex\n') or u32(32) != len(data) or u32(40) != 0x12345678:
        raise ValueError('invalid DEX header')
    ns, so, nt, to, nc, co = (u32(i) for i in (56,60,64,68,96,100))
    if any(off + count * stride > len(data) for off,count,stride in ((so,ns,4),(to,nt,4),(co,nc,32))):
        raise ValueError('invalid DEX tables')
    classes = set()
    for i in range(nc):
        ti = u32(co + i*32)
        if ti >= nt:
            raise ValueError('invalid type index')
        si = u32(to + ti*4)
        if si >= ns:
            raise ValueError('invalid string index')
        pos = u32(so + si*4)
        for _ in range(5):
            if pos >= len(data):
                raise ValueError('invalid string offset')
            value = data[pos]; pos += 1
            if not value & 128:
                break
        else:
            raise ValueError('invalid DEX string length')
        end = data.find(b'\0', pos)
        if end < 0:
            raise ValueError('unterminated descriptor')
        desc = data[pos:end]
        if not (desc.startswith(b'L') and desc.endswith(b';')) or desc in classes:
            raise ValueError('invalid or duplicate class descriptor')
        classes.add(desc)
    return classes


def dex_inventory(z):
    result = {}; seen = set()
    for name in sorted(n for n in inventory(z) if DEX.fullmatch(n)):
        classes = dex_classes(z.read(name))
        if seen & classes:
            raise ValueError('duplicate class across DEX files')
        result[name] = classes; seen |= classes
    if not result:
        raise ValueError('no DEX')
    return result


def audit(pgsharp, pgbr):
    if sha(pgsharp) != PGSHARP_SHA or sha(pgbr) != PGBR_SHA:
        raise ValueError('input does not match the audited private pair')
    with zipfile.ZipFile(pgsharp) as g, zipfile.ZipFile(pgbr) as b:
        gn, bn = inventory(g), inventory(b)
        common = gn & bn
        different = sorted(n for n in common if (g.getinfo(n).file_size,g.getinfo(n).CRC) != (b.getinfo(n).file_size,b.getinfo(n).CRC))
        gd, bd = dex_inventory(g), dex_inventory(b)
        gc, bc = set.union(*gd.values()), set.union(*bd.values())
        own = bd.get('classes5.dex', set())
        native_shared = sorted(n for n in common if n.startswith(GAME) and n.endswith('.so'))
        native_different = [n for n in native_shared if entry_sha(g,n) != entry_sha(b,n)]
        core = [GAME+'libil2cpp.so', GAME+'libunity.so', METADATA]
        return {'pgsharp_entries':len(gn),'pgbr_entries':len(bn),'shared_paths':len(common),
                'same_size_crc':len(common)-len(different),'different_size_crc':len(different),
                'shared_class_definitions':len(gc & bc),'pgbr_added_classes':len(own),
                'pgbr_added_class_collisions':len(own & gc),'pgbr_added_classes_all_owned_namespace':all(c.startswith(b'Ldev/pgrazor/') for c in own),
                'native_shared':len(native_shared),'native_different':native_different,
                'core_equal_sha256':{n:entry_sha(g,n)==entry_sha(b,n) for n in core},
                'resources_equal_sha256':entry_sha(g,'resources.arsc')==entry_sha(b,'resources.arsc'),
                'pgbr_native_assets_excluded':sorted(n for n in bn-gn if n.startswith('assets/pgrazor/') and n.endswith('.so')),
                'device_tested':False,'provider_connected':False}


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('Usage: hybrid_audit.py PGSHARP.apk PGBR-LT049-R2.apk')
    print(json.dumps(audit(Path(sys.argv[1]),Path(sys.argv[2])),indent=2))
