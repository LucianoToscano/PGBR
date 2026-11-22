"""Compile production VGP diagnostics and run its JVM fixture."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT=Path(__file__).resolve().parents[1]

class VgpHealthReportTests(unittest.TestCase):
    def test_read_only_report(self):
        javac,java=java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-vgp-health-") as out:
            sources=[ROOT/"src/dev/pgrazor/VgpHealthReport.java",
                     ROOT/"test/VgpHealthReportTest.java"]
            compilation=subprocess.run([str(javac),"-encoding","UTF-8","-d",out,
                         *map(str,sources)],capture_output=True,text=True,timeout=30)
            self.assertEqual(0,compilation.returncode,compilation.stderr+compilation.stdout)
            result=subprocess.run([str(java),"-cp",out,"dev.pgrazor.VgpHealthReportTest"],
                            capture_output=True,text=True,timeout=30)
            self.assertEqual(0,result.returncode,result.stderr+result.stdout)

if __name__=="__main__":unittest.main()
