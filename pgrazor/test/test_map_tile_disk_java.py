"""Host-side regression for atomic map cache replacement and failure preservation."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT=Path(__file__).resolve().parents[1]
HARNESS=r"""
package dev.pgrazor;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
public class MapTileDiskStoreHarness {
    static void check(boolean condition) { if(!condition) throw new AssertionError(); }
    static void clean(Path dir) throws IOException {
        try(java.util.stream.Stream<Path> files=Files.list(dir)) {
            check(files.noneMatch(p->p.getFileName().toString().endsWith(".part")));
        }
    }
    public static void main(String[] args) throws Exception {
        Path dir=Paths.get(args[0]), target=dir.resolve("1-0-0.png");
        byte[] old={1,2,3}, next={4,5,6};
        MapTileDiskStore.replace(target.toFile(),old);
        check(Arrays.equals(old,Files.readAllBytes(target)));
        try {
            MapTileDiskStore.replace(target.toFile(),next,(tmp,dst)-> {
                check(Arrays.equals(next,Files.readAllBytes(tmp)));
                check(Arrays.equals(old,Files.readAllBytes(dst)));
                throw new IOException("injected atomic move failure");
            });
            throw new AssertionError("failure swallowed");
        } catch(IOException expected) {}
        check(Arrays.equals(old,Files.readAllBytes(target)));
        clean(dir);
        MapTileDiskStore.replace(target.toFile(),next);
        check(Arrays.equals(next,Files.readAllBytes(target)));
        clean(dir);
    }
}
"""

class MapTileDiskTests(unittest.TestCase):
    def test_atomic_replace_and_failed_move_preserves_old(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory() as tmp:
            directory=Path(tmp)
            source=directory/"MapTileDiskStoreHarness.java"
            source.write_text(HARNESS,encoding="utf-8")
            cache=directory/"cache"
            cache.mkdir()
            subprocess.run([str(compiler),"-encoding","UTF-8","-d",tmp,
                str(ROOT/"src/dev/pgrazor/MapTileDiskStore.java"),str(source)],check=True,timeout=30)
            subprocess.run([str(runtime),"-cp",tmp,"dev.pgrazor.MapTileDiskStoreHarness",str(cache)],check=True,timeout=30)

    def test_cache_integration(self):
        cache=(ROOT/"src/dev/pgrazor/MapTileCache.java").read_text(encoding="utf-8")
        self.assertEqual(cache.count("synchronized (DISK_LOCK)"),3)
        self.assertIn("MapTileDiskStore.replace(file, bytes)",cache)
        self.assertNotIn("if (file.exists()) file.delete()",cache)
        self.assertIn("bytes.length > TILE_BYTE_LIMIT",cache)
        self.assertIn("file.isFile() &&",cache)

if __name__=="__main__":
    unittest.main()
