"""Offline tests for the proposal dispatcher; no external services or credentials."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("auto_dev", Path(__file__).resolve().parents[2] / "tools/auto_dev_orchestrator.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class AutoDevTests(unittest.TestCase):
    def row(self, ident="C-TEST-01", agent="chatgpt"):
        return {"id": ident, "agent": agent, "tasks": ["Offline test"]}

    def test_queue_and_duplicates(self):
        self.assertEqual(m.validate_queue({"version": 1, "batches": [self.row()]}), [self.row()])
        for rows in ([self.row(), self.row()], [self.row("../file")], [self.row(agent="unknown")],
                     [dict(self.row(), tasks=[])], [dict(self.row(), tasks=["x"]*16)]):
            with self.assertRaises(ValueError):
                m.validate_queue({"version": 1, "batches": rows})

    def test_selection_and_reservation(self):
        rows = [self.row(), self.row("C-TEST-02"), self.row("G-TEST-01", "gemini"), self.row("A-TEST-01", "arena")]
        self.assertEqual(m.select(rows, {"C-TEST-01"}, {"chatgpt", "gemini"}), rows[1:3])
        self.assertEqual(m.select(rows, set(), set()), [])
        self.assertEqual(m.select(rows, set(), set(m.AGENTS)), [rows[0], rows[2], rows[3]])
        pages = [[{"user": {"login": "github-actions[bot]"}, "body": "<!-- pgbr-auto-dev:v1:C-TEST-01:reserved -->\nreceipt"},
                  {"user": {"login": "other"}, "body": "<!-- pgbr-auto-dev:v1:G-TEST-01:reserved -->"}]]
        self.assertEqual(m.reserved_ids(pages), {"C-TEST-01"})

    def test_provider_payloads(self):
        for agent, expected in (("chatgpt", "https://api.openai.com/v1/responses"),
                                ("gemini", "https://generativelanguage.googleapis.com/v1beta/models/test:generateContent"),
                                ("arena", "https://api.arena.ai/v1/chat/completions")):
            url, headers, body = m.request_spec(agent, "test", "secret", "public task")
            self.assertEqual(url, expected)
            self.assertNotIn("secret", str(body))
            self.assertIn("secret", str(headers))
            self.assertIn("public task", str(body))
        with self.assertRaises(ValueError):
            m.request_spec("gemini", "../bad", "key", "task")

    def test_response_contracts(self):
        self.assertEqual(m.response_text("chatgpt", {"status": "completed", "output": [
            {"type": "message", "content": [{"type": "output_text", "text": "proposal"}]}]}), "proposal")
        self.assertEqual(m.response_text("gemini", {"candidates": [{"finishReason": "STOP", "content": {"parts": [
            {"text": "hidden", "thought": True}, {"text": "proposal"}]}}]}), "proposal")
        self.assertEqual(m.response_text("arena", {"choices": [{"finish_reason": "stop", "message": {"content": "proposal"}}]}), "proposal")
        with self.assertRaises(ValueError):
            m.response_text("chatgpt", {"status": "incomplete"})
        with self.assertRaises(ValueError):
            m.response_text("arena", {"choices": [{"finish_reason": "length", "message": {"content": "partial"}}]})


if __name__ == "__main__":
    unittest.main()
