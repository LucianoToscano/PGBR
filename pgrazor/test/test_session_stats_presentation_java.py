"""Pure Java session summary and sanitized export."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from test_control_java import java_tools

ROOT=Path(__file__).resolve().parents[1]

class SessionStatsPresentationTests(unittest.TestCase):
    def test_summary_and_export(self):
        compiler,runtime=java_tools()
        with tempfile.TemporaryDirectory(prefix="pgbr-stats-summary-") as work:
            paths=[ROOT/"src/dev/pgrazor/SessionStatsHistory.java",ROOT/"src/dev/pgrazor/SessionStatsPresentation.java",ROOT/"test/SessionStatsPresentationTest.java"]
            result=subprocess.run([str(compiler),"-encoding","UTF-8","-d",work,*map(str,paths)],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
            result=subprocess.run([str(runtime),"-cp",work,"dev.pgrazor.SessionStatsPresentationTest"],capture_output=True,text=True,timeout=30)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
