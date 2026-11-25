"""Static startup diagnostics contract; not a device smoke test."""
from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "src/dev/pgrazor/BootstrapProvider.java"

class StartupStageDiagnostics(unittest.TestCase):
    def test_provider_preserves_failure(self):
        source = SOURCE.read_text(encoding="utf-8")
        provider = source[source.index("public boolean onCreate()"):source.index("static boolean start(Context context)")]
        self.assertLess(provider.index("startup-stage=provider-enter"), provider.index("start(getContext())"))
        self.assertLess(provider.index("start(getContext())"), provider.index("startup-stage=provider-return"))
        self.assertIn("startup-stage=provider-failed type=", provider)
        self.assertIn("throw failure;", provider)

    def test_recovery_and_control_order(self):
        source = SOURCE.read_text(encoding="utf-8")
        stages = ["startup-stage=account-recovery-enter", "AccountSessions.recover(context)",
                  "startup-stage=account-recovery-return", "startup-stage=control-begin-enter",
                  "Control.begin(app)", "startup-stage=control-begin-return",
                  "startup-stage=loader-enter", "load(context);", "startup-stage=loader-return"]
        cursor = source.index("static boolean start(Context context)")
        for stage in stages:
            cursor = source.index(stage, cursor) + len(stage)
        self.assertIn('throw new IllegalStateException("Saved account session could not be restored; game startup stopped",failure)', source)

if __name__ == "__main__":
    unittest.main()
