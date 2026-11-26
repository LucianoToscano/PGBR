"""Offline tests; no credentials or network required."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("auto_dev", Path(__file__).resolve().parents[2] / "tools/auto_dev_orchestrator.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class AutoDevTests(unittest.TestCase):
    def row(self, ident="G-TEST-01", agent="gemini"):
        return {"id": ident, "agent": agent, "tasks": ["Offline test"]}

    def test_queue_and_duplicates(self):
        self.assertEqual(m.validate_queue({"version": 1, "batches": [self.row()]}), [self.row()])
        for rows in ([self.row(), self.row()], [self.row("../file")], [self.row(agent="unknown")],
                     [dict(self.row(), tasks=[])], [dict(self.row(), tasks=["x"]*16)]):
            with self.assertRaises(ValueError):
                m.validate_queue({"version": 1, "batches": rows})

    def test_selection_and_reservation(self):
        rows = [self.row("C-TEST-01", "chatgpt"), self.row(), self.row("G-TEST-02"), self.row("A-TEST-01", "arena")]
        self.assertEqual(m.select(rows, set(), set(m.AGENTS)), [rows[1]])
        self.assertEqual(m.select(rows, {"G-TEST-01"}, set(m.AGENTS)), [rows[2]])
        self.assertEqual(m.select(rows, set(), {"chatgpt", "arena"}), [])
        pages = [[{"user": {"login": "github-actions[bot]"}, "body": "<!-- pgbr-auto-dev:v1:G-TEST-01:reserved -->\nreceipt"},
                  {"user": {"login": "other"}, "body": "<!-- pgbr-auto-dev:v1:G-TEST-02:reserved -->"}]]
        self.assertEqual(m.reserved_ids(pages), {"G-TEST-01"})

    def test_free_configuration_requires_all_guards(self):
        env = {"PGBR_AUTO_DEV_MODE": "free", "PGBR_FREE_BILLING_CONFIRMED": "true",
               "PGBR_GITHUB_FREE_BUDGET_CONFIRMED": "true", "PGBR_GEMINI_FREE_KEY": "test-key",
               "PGBR_GEMINI_FREE_MODEL": "gemini-test", "OPENAI_API_KEY": "ignored", "ARENA_API_KEY": "ignored"}
        self.assertEqual(m.free_configuration(env), {"gemini": ("gemini-test", "test-key")})
        for key in ("PGBR_AUTO_DEV_MODE", "PGBR_FREE_BILLING_CONFIRMED", "PGBR_GITHUB_FREE_BUDGET_CONFIRMED",
                    "PGBR_GEMINI_FREE_KEY", "PGBR_GEMINI_FREE_MODEL"):
            invalid = dict(env)
            del invalid[key]
            self.assertEqual(m.free_configuration(invalid), {})
        self.assertEqual(m.free_configuration({"GEMINI_API_KEY": "ordinary-key"}), {})

    def test_existing_key_only_with_explicit_selection(self):
        env = {"PGBR_AUTO_DEV_MODE": "free", "PGBR_FREE_BILLING_CONFIRMED": "true",
               "PGBR_GITHUB_FREE_BUDGET_CONFIRMED": "true", "GEMINI_API_KEY": "legacy-key",
               "PGBR_GEMINI_FREE_MODEL": "gemini-test"}
        self.assertEqual(m.free_configuration(env), {})
        env["PGBR_GEMINI_KEY_SOURCE"] = "existing"
        self.assertEqual(m.free_configuration(env), {"gemini": ("gemini-test", "legacy-key")})
        env["PGBR_FREE_BILLING_CONFIRMED"] = "false"
        self.assertEqual(m.free_configuration(env), {})
        env["PGBR_FREE_BILLING_CONFIRMED"] = "true"
        env["PGBR_GEMINI_KEY_SOURCE"] = "unknown"
        self.assertEqual(m.free_configuration(env), {})

    def test_pause_ignores_untrusted_comments(self):
        def comment(user, ident):
            return {"user": {"login": user}, "body": "<!-- pgbr-auto-dev:v1:" + ident + ":paused -->\nreceipt"}
        pages = [[comment("github-actions[bot]", "G-TEST-01")],
                 [comment("github-actions[bot]", "G-TEST-02"), comment("other", "G-TEST-03")]]
        self.assertEqual(m.paused_batch(pages), "G-TEST-02")
        self.assertIsNone(m.paused_batch([[comment("other", "G-TEST-03")]]))

    def test_provider_payloads(self):
        url, headers, body = m.request_spec("gemini", "gemini-test", "secret", "public task")
        self.assertEqual(url, "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent")
        self.assertNotIn("secret", str(body))
        self.assertEqual(headers["x-goog-api-key"], "secret")
        self.assertIn("public task", str(body))
        self.assertNotIn("tools", body)
        for agent in ("chatgpt", "arena"):
            with self.assertRaises(ValueError):
                m.request_spec(agent, "gemini-test", "key", "task")
        with self.assertRaises(ValueError):
            m.request_spec("gemini", "../bad", "key", "task")

    def test_response_contract(self):
        self.assertEqual(m.response_text("gemini", {"candidates": [{"finishReason": "STOP", "content": {"parts": [
            {"text": "hidden", "thought": True}, {"text": "proposal"}]}}]}), "proposal")
        with self.assertRaises(ValueError):
            m.response_text("gemini", {"candidates": [{"finishReason": "MAX_TOKENS"}]})
        with self.assertRaises(ValueError):
            m.response_text("chatgpt", {})


if __name__ == "__main__":
    unittest.main()
