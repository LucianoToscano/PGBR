"""Behavioural tests for the PGBR-owned data gateway.

The gateway is the project's own service (health, capabilities, sources, search, imports, presets,
events and signed webhooks). These tests assert the contracts the specification demands: honest
empty states, null for absent values, provenance separation, no fixture reaching production, HMAC
signatures with anti-replay, retry/DLQ, and refusal to emit spawn webhooks from personal imports.
"""
from pathlib import Path
import json
import sys
import time
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "gateway"))

import pgbr_gateway as gw  # noqa: E402

AUTH = {"Authorization": "Bearer t", "X-Forwarded-For": "test"}


def row(event_id, lat=-23.55, lon=-46.63, **extra):
    payload = {"id": event_id, "pokemon_id": 25, "latitude": lat, "longitude": lon}
    payload.update(extra)
    return payload


class GatewayAuthTests(unittest.TestCase):
    def setUp(self):
        self.gateway = gw.Gateway(token="t", secret="s" * 32)

    def test_health_needs_no_token(self):
        status, body, _ = self.gateway.handle("GET", "/v1/health", {}, {}, {})
        self.assertEqual(status, 200)
        self.assertEqual(body["status"], "ok")
        self.assertEqual(body["sources_connected"], [])

    def test_authenticated_routes_require_token(self):
        status, body, _ = self.gateway.handle("GET", "/v1/sources", {}, {}, {})
        self.assertEqual(status, 401)
        self.assertEqual(body["error"]["code"], "unauthorized")

    def test_wrong_token_is_refused(self):
        headers = {"Authorization": "Bearer nope", "X-Forwarded-For": "test"}
        status, body, _ = self.gateway.handle("GET", "/v1/sources", {}, {}, headers)
        self.assertEqual(status, 401)
        self.assertEqual(body["error"]["code"], "unauthorized")

    def test_without_configured_token_every_authenticated_call_fails_closed(self):
        gateway = gw.Gateway(token="", secret="")
        status, body, _ = gateway.handle("GET", "/v1/sources", {}, {}, AUTH)
        self.assertEqual(status, 503)
        self.assertEqual(body["error"]["code"], "not_configured")

    def test_rate_limit_returns_retry_after(self):
        gateway = gw.Gateway(token="t", secret="s" * 32, rate_limit=2)
        first = gateway.handle("GET", "/v1/sources", {}, {}, AUTH)[0]
        second = gateway.handle("GET", "/v1/sources", {}, {}, AUTH)[0]
        third = gateway.handle("GET", "/v1/sources", {}, {}, AUTH)
        self.assertEqual([first, second], [200, 200])
        self.assertEqual(third[0], 429)
        self.assertEqual(third[2]["Retry-After"], "60")
        self.assertEqual(third[1]["error"]["code"], "rate_limited")

    def test_rate_limit_never_exceeds_the_window_when_the_clock_steps_back(self):
        limiter = gw.RateLimiter(limit=1, window_seconds=60)
        allowed, _ = limiter.check("ip", now=1_000.0)
        self.assertTrue(allowed)
        # A wall clock that steps backwards must not advertise a wait longer than the window.
        for now in (1_000.0, 999.9, 990.0):
            allowed, retry = limiter.check("ip", now=now)
            self.assertFalse(allowed)
            self.assertGreaterEqual(retry, 1)
            self.assertLessEqual(retry, 60, "Retry-After acima da janela: %d" % retry)


class GatewaySourceTests(unittest.TestCase):
    def setUp(self):
        self.gateway = gw.Gateway(token="t", secret="s" * 32)

    def test_exactly_seven_sources_and_five_integrations(self):
        body = self.gateway.handle("GET", "/v1/sources", {}, {}, AUTH)[1]
        self.assertEqual([entry["source_id"] for entry in body["sources"]],
                         ["pokelist", "iflowgo", "pogocoordinates", "pgsharp", "ipogo",
                          "pgbr_hundo", "livepokemap"])
        capabilities = self.gateway.handle("GET", "/v1/capabilities", {}, {}, AUTH)[1]
        self.assertEqual(capabilities["integrations"],
                         ["pokelist", "iflowgo", "pogocoordinates", "pgsharp", "ipogo"])
        self.assertEqual(capabilities["not_integrations"], ["pokemond", "pgtools"])

    def test_no_source_claims_to_be_connected(self):
        body = self.gateway.handle("GET", "/v1/sources", {}, {}, AUTH)[1]
        for entry in body["sources"]:
            self.assertIn(entry["status"], ("unverified", "local_only"), entry)
            self.assertIsNone(entry["last_verified_at"])

    def test_declared_coverage_is_honest_per_source(self):
        sources = {entry["source_id"]: entry
                   for entry in self.gateway.handle("GET", "/v1/sources", {}, {}, AUTH)[1]["sources"]}
        self.assertIn("reveal_code", sources["pogocoordinates"]["coverage"])
        self.assertFalse(sources["pgsharp"]["supports_remote_search"])
        self.assertFalse(sources["ipogo"]["supports_remote_search"])
        self.assertIn("API pública", sources["ipogo"]["note"])

    def test_unknown_capability_source_is_404(self):
        status, body, _ = self.gateway.handle("GET", "/v1/capabilities", {"source": ["nope"]}, {},
                                              AUTH)
        self.assertEqual(status, 404)
        self.assertEqual(body["error"]["code"], "unknown_source")

    def test_capabilities_reports_remote_collection_disabled(self):
        body = self.gateway.handle("GET", "/v1/capabilities", {}, {}, AUTH)[1]
        self.assertFalse(body["remote_collection"])
        self.assertIn("permissão explícita", body["remote_collection_reason"])


class GatewayImportTests(unittest.TestCase):
    def setUp(self):
        self.gateway = gw.Gateway(token="t", secret="s" * 32)
        self.headers = dict(AUTH)

    def test_import_reports_imported_duplicates_rejected_with_reason(self):
        payload = {"events": [row("a", iv_percent=100, cp=500, level=20),
                              row("a", iv_percent=100),
                              row("b", lat=0, lon=0),
                              row("c", lat=91, lon=10)]}
        status, report, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {}, payload,
                                                self.headers)
        self.assertEqual(status, 202)
        self.assertEqual(report["imported"], 1)
        self.assertEqual(report["duplicates"], 1)
        self.assertEqual([item["reason"] for item in report["rejected"]],
                         ["coordenadas inválidas ou ausentes (0,0 não é coordenada)"] * 2)
        self.assertEqual(report["origin_kind"], "personal_import")
        self.assertIn("não é feed ao vivo", report["note"])

    def test_import_keeps_zero_iv_as_a_real_observation(self):
        payload = {"events": [row("zero", iv_percent=0, iv_atk=0, iv_def=0, iv_sta=0)]}
        self.gateway.handle("POST", "/v1/imports/pgsharp", {}, payload, self.headers)
        status, search, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "iv_min": 0, "iv_max": 0}, self.headers)
        self.assertEqual(status, 200)
        self.assertEqual(len(search["items"]), 1)
        self.assertEqual(search["items"][0]["iv_percent"], 0.0)
        self.assertEqual(search["items"][0]["iv_atk"], 0)

    def test_import_never_attributes_other_sources_to_pgsharp(self):
        payload = {"events": [row("x", source_id="iflowgo", iv_percent=100)]}
        status, report, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {}, payload,
                                                self.headers)
        self.assertEqual(status, 202)
        events = self.gateway.handle("GET", "/v1/events", {"source": ["pgsharp"]}, {},
                                     self.headers)[1]["items"]
        self.assertEqual([event["source_id"] for event in events], ["pgsharp"])
        status, body, _ = self.gateway.handle("GET", "/v1/events", {"source": ["iflowgo"]}, {},
                                              self.headers)
        self.assertEqual((status, body["items"]), (200, []))
        self.assertEqual(report["imported"], 1)

    def test_import_accepts_explicit_unavailable_precision(self):
        payload = {"events": [{"id": "route", "name": "Rota 1", "coordinate_precision": "unavailable"}]}
        status, report, _ = self.gateway.handle("POST", "/v1/imports/ipogo", {}, payload,
                                                self.headers)
        self.assertEqual(status, 202)
        self.assertEqual(report["imported"], 1)

    def test_import_rejects_demo_fixtures(self):
        payload = {"events": [row("demo", origin_kind="demo_fixture", iv_percent=100)]}
        status, report, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {}, payload,
                                                self.headers)
        self.assertEqual(status, 422)
        self.assertEqual(report["imported"], 0)
        self.assertIn("testes automatizados", report["rejected"][0]["reason"])

    def test_import_rejects_android_demo_alias(self):
        status, report, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {},
            {"events": [row("demo", origin_kind="simulated_fixture")]}, self.headers)
        self.assertEqual(status, 422)
        self.assertEqual(report["imported"], 0)

    def test_normalisation_preserves_precision_and_boolean_evidence(self):
        for precision in ("unavailable", "unknown"):
            result = gw.normalise(row("unavailable", coordinate_precision=precision,
                                      shiny_observed="false", weather_boost="false"))
            self.assertEqual(result["coordinate_precision"], "unavailable")
            self.assertIsNone(result["shiny_observed"])
            self.assertIsNone(result["weather_boost"])
        self.assertTrue(gw.normalise(row("demo", origin_kind="simulated_fixture"))["is_demo"])
        self.assertIs(gw.normalise(row("verified", shiny_observed=False))["shiny_observed"], False)

    def test_import_rejects_oversized_payload_and_bad_shape(self):
        status, body, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": {}},
                                              self.headers)
        self.assertEqual((status, body["error"]["code"]), (422, "invalid_events"))
        big = {"events": [row(str(i)) for i in range(gw.MAX_EVENTS + 1)]}
        status, body, _ = self.gateway.handle("POST", "/v1/imports/pgsharp", {}, big, self.headers)
        self.assertEqual((status, body["error"]["code"]), (422, "too_many_events"))

    def test_import_is_queryable_by_report_id(self):
        self.gateway.handle("POST", "/v1/imports/ipogo", {}, {"events": [row("one")]},
                            self.headers)
        status, report, _ = self.gateway.handle("GET", "/v1/imports/imp-1", {}, {}, self.headers)
        self.assertEqual(status, 200)
        self.assertEqual(report["imported"], 1)
        status, body, _ = self.gateway.handle("GET", "/v1/imports/imp-404", {}, {}, self.headers)
        self.assertEqual(status, 404)

    def test_expired_rows_are_not_imported(self):
        payload = {"events": [row("old", expires_at=int(time.time() * 1000) - 1000)]}
        gateway = gw.Gateway(token="t", secret="s" * 32)
        status, report, _ = gateway.handle("POST", "/v1/imports/pgsharp", {}, payload, self.headers)
        self.assertEqual(status, 422)
        self.assertEqual(report["expired"], 1)
        self.assertEqual(report["imported"], 0)


class GatewaySearchTests(unittest.TestCase):
    def setUp(self):
        self.gateway = gw.Gateway(token="t", secret="s" * 32)
        self.headers = dict(AUTH)
        self.gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": [
            row("hundo", iv_percent=100, iv_atk=15, iv_def=15, iv_sta=15, cp=2000, level=35,
                gender="1", size="5", pvp_great_rank=1),
            row("zero", iv_percent=0, iv_atk=0, iv_def=0, iv_sta=0, cp=10, level=1),
            row("mid", iv_percent=50, cp=300, level=20),
        ]}, self.headers)

    def test_search_requires_at_least_one_source(self):
        for payload in ({}, {"sources": []}, {"sources": ["nope"]}):
            status, body, _ = self.gateway.handle("POST", "/v1/search", {}, payload, self.headers)
            self.assertEqual(status, 422, payload)
            self.assertIn(body["error"]["code"], ("invalid_sources", "unknown_source"))

    def test_search_returns_real_rows_with_provenance(self):
        status, body, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "iv_min": 90}, self.headers)
        self.assertEqual(status, 200)
        self.assertEqual([item["event_id"] for item in body["items"]], ["hundo"])
        item = body["items"][0]
        self.assertEqual(item["origin_kind"], "personal_import")
        self.assertEqual(item["data_quality"], "imported")
        self.assertEqual(item["gender"], "MALE")
        self.assertEqual(item["size"], "XL")
        self.assertEqual(item["pvp_great_rank"], 1)
        self.assertFalse(item["is_demo"])
        self.assertIn("iv_min", body["filters_applied"])
        self.assertEqual(body["received"], 3)

    def test_search_with_no_match_is_an_honest_empty_result(self):
        status, body, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["iflowgo"], "iv_min": 100, "iv_max": 100}, self.headers)
        self.assertEqual(status, 200)
        self.assertEqual(body["items"], [])
        self.assertEqual(body["has_more"], False)
        self.assertIn("Nenhum resultado confirmado", body["note"])

    def test_search_rejects_invalid_ranges_instead_of_widening(self):
        cases = [({"sources": ["pgsharp"], "iv_min": 101}, "invalid_range"),
                 ({"sources": ["pgsharp"], "iv_min": 90, "iv_max": 10}, "invalid_range"),
                 ({"sources": ["pgsharp"], "atk_min": 16}, "invalid_range"),
                 ({"sources": ["pgsharp"], "limit": 0}, "invalid_limit"),
                 ({"sources": ["pgsharp"], "limit": 101}, "invalid_limit"),
                 ({"sources": ["pgsharp"], "kind": "spawn"}, "invalid_kind"),
                 ({"sources": ["pgsharp"], "latitude": 95, "longitude": 10}, "invalid_coordinates"),
                 ({"sources": ["pgsharp"], "pokemon_ids": [0]}, "invalid_pokemon_ids")]
        for payload, code in cases:
            status, body, _ = self.gateway.handle("POST", "/v1/search", {}, payload, self.headers)
            self.assertEqual((status, body["error"]["code"]), (422, code), payload)

    def test_search_reports_filters_the_data_cannot_serve(self):
        gateway = gw.Gateway(token="t", secret="s" * 32)
        gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": [
            row("no-ads", iv_percent=50, cp=300)]}, self.headers)
        status, body, _ = gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "atk_min": 10}, self.headers)
        self.assertEqual(status, 200)
        # The imported rows carry no A/D/S: the gateway says so instead of silently passing them.
        self.assertEqual(body["items"], [])
        self.assertEqual(body["unsupported_filters"], ["atk_min"])
        status, body, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "atk_min": 10}, self.headers)
        self.assertEqual(status, 200)
        # Rows that do carry A/D/S are really filtered, so the filter is supported here.
        self.assertEqual([item["event_id"] for item in body["items"]], ["hundo"])
        self.assertEqual(body["unsupported_filters"], [])

    def test_search_applies_real_radius(self):
        self.gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": [
            row("near", lat=-23.55, lon=-46.63), row("far", lat=-22.90, lon=-43.20)]},
            self.headers)
        status, body, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "latitude": -23.55, "longitude": -46.63, "radius_km": 5,
            "pokemon_ids": [25]}, self.headers)
        self.assertEqual(status, 200)
        self.assertIn("near", [item["event_id"] for item in body["items"]])
        self.assertNotIn("far", [item["event_id"] for item in body["items"]])

    def test_search_paginates_with_limit(self):
        status, body, _ = self.gateway.handle("POST", "/v1/search", {}, {
            "sources": ["pgsharp"], "limit": 1}, self.headers)
        self.assertEqual(status, 200)
        self.assertEqual(len(body["items"]), 1)
        self.assertTrue(body["has_more"])

    def test_events_endpoint_filters_by_source_and_validates_limit(self):
        body = self.gateway.handle("GET", "/v1/events", {"source": ["pgsharp"]}, {},
                                   self.headers)[1]
        self.assertEqual({item["source_id"] for item in body["items"]}, {"pgsharp"})
        status, error, _ = self.gateway.handle("GET", "/v1/events", {"limit": ["0"]}, {},
                                               self.headers)
        self.assertEqual((status, error["error"]["code"]), (422, "invalid_limit"))
        status, error, _ = self.gateway.handle("GET", "/v1/events", {"source": ["nope"]}, {},
                                               self.headers)
        self.assertEqual(status, 404)


class GatewayPresetTests(unittest.TestCase):
    def setUp(self):
        self.gateway = gw.Gateway(token="t", secret="s" * 32)
        self.headers = dict(AUTH)

    def test_preset_create_list_delete(self):
        payload = {"name": "Hundo local", "query": {"iv_min": 100, "iv_max": 100},
                   "favorite": True}
        status, preset, _ = self.gateway.handle("POST", "/v1/presets", {}, payload, self.headers)
        self.assertEqual(status, 201)
        self.assertEqual(preset["query"], {"iv_min": 100.0, "iv_max": 100.0, "cp_min": None,
                                          "cp_max": None, "level_min": None, "level_max": None})
        self.assertTrue(preset["favorite"])
        listed = self.gateway.handle("GET", "/v1/presets", {}, {}, self.headers)[1]["presets"]
        self.assertEqual([item["id"] for item in listed], [preset["id"]])
        status, _, _ = self.gateway.handle("DELETE", "/v1/presets/%s" % preset["id"], {}, {},
                                           self.headers)
        self.assertEqual(status, 204)
        status, _, _ = self.gateway.handle("DELETE", "/v1/presets/%s" % preset["id"], {}, {},
                                           self.headers)
        self.assertEqual(status, 404)

    def test_preset_validation_rejects_bad_ranges_and_missing_name(self):
        for payload, code in (({"query": {}}, "invalid_preset"),
                              ({"name": "x", "query": {"iv_min": 200}}, "invalid_preset"),
                              ({"name": "x", "query": {"iv_min": 90, "iv_max": 10}},
                               "invalid_preset")):
            status, body, _ = self.gateway.handle("POST", "/v1/presets", {}, payload, self.headers)
            self.assertEqual((status, body["error"]["code"]), (422, code), payload)


class WebhookSignatureTests(unittest.TestCase):
    def setUp(self):
        self.dispatcher = gw.WebhookDispatcher("topsecret")

    def test_signature_is_hmac_sha256_over_id_timestamp_body(self):
        digest = self.dispatcher.sign("evt-1", 1700000000, '{"a":1}')
        self.assertEqual(len(digest), 64)
        expected = gw.hmac.new(b"topsecret", b"evt-1.1700000000.{}", gw.hashlib.sha256).hexdigest()
        self.assertNotEqual(digest, expected)
        self.assertTrue(self.dispatcher.verify("evt-1", 1700000000, '{"a":1}', digest, now=1700000000)[0])

    def test_tampered_body_or_signature_is_rejected(self):
        digest = self.dispatcher.sign("evt-1", 1700000000, '{"a":1}')
        self.assertFalse(self.dispatcher.verify("evt-1", 1700000000, '{"a":2}', digest,
                                                now=1700000000)[0])
        self.assertFalse(self.dispatcher.verify("evt-1", 1700000000, '{"a":1}', digest[:-1] + "0",
                                                now=1700000000)[0])

    def test_replay_is_rejected_and_window_is_enforced(self):
        digest = self.dispatcher.sign("evt-1", 1700000000, '{"a":1}')
        self.assertTrue(self.dispatcher.verify("evt-1", 1700000000, '{"a":1}', digest,
                                               now=1700000000)[0])
        accepted, reason = self.dispatcher.verify("evt-1", 1700000000, '{"a":1}', digest,
                                                 now=1700000000)
        self.assertFalse(accepted)
        self.assertIn("replay", reason)
        accepted, reason = self.dispatcher.verify("evt-2", 1700000000, '{"a":1}',
                                                  self.dispatcher.sign("evt-2", 1700000000,
                                                                       '{"a":1}'),
                                                  now=1700000000 + 301)
        self.assertFalse(accepted)
        self.assertIn("timestamp", reason)

    def test_missing_secret_fails_closed(self):
        accepted, reason = gw.WebhookDispatcher("").verify("evt", 1, "{}", "x", now=1)
        self.assertFalse(accepted)
        self.assertIn("segredo", reason)


class WebhookDeliveryTests(unittest.TestCase):
    def setUp(self):
        self.calls = []
        self.dispatcher = gw.WebhookDispatcher("topsecret", delivery=self._deliver,
                                               clock=lambda: 1700000000.0, sleeper=lambda s: None)
        self.dispatcher.create_subscription({"url": "https://example.com/hook",
                                             "events": ["import.completed"]})

    def _deliver(self, url, body, headers):
        self.calls.append((url, body, dict(headers)))
        return self.status

    def test_delivery_sends_signed_headers(self):
        self.status = 200
        result = self.dispatcher.dispatch({"id": "import-1", "type": "import.completed"})
        self.assertEqual(result, {"idempotent": False, "delivered": 1, "failed": 0})
        url, body, headers = self.calls[0]
        self.assertEqual(url, "https://example.com/hook")
        self.assertEqual(headers["X-Event-Id"], "import-1")
        self.assertEqual(headers["X-Event-Timestamp"], "1700000000")
        self.assertEqual(headers["X-Event-Signature"], self.dispatcher.sign("import-1", 1700000000,
                                                                            body))

    def test_idempotent_redelivery_does_not_post_twice(self):
        self.status = 200
        event = {"id": "import-1", "type": "import.completed"}
        self.dispatcher.dispatch(event)
        self.dispatcher.dispatch(event)
        self.assertEqual(len(self.calls), 1)

    def test_retry_then_success_then_dlq(self):
        self.status = 500
        attempts = []

        def flaky(url, body, headers):
            attempts.append(1)
            return 200 if len(attempts) == 3 else 500

        dispatcher = gw.WebhookDispatcher("topsecret", delivery=flaky, clock=lambda: 1700000000.0,
                                          sleeper=lambda s: None)
        dispatcher.create_subscription({"url": "https://example.com/hook",
                                        "events": ["import.completed"]})
        result = dispatcher.dispatch({"id": "import-2", "type": "import.completed"})
        self.assertEqual((result["delivered"], result["failed"]), (1, 0))
        self.assertEqual(len(attempts), 3)

        always_down = gw.WebhookDispatcher("topsecret", delivery=lambda u, b, h: 500,
                                           clock=lambda: 1700000000.0, sleeper=lambda s: None)
        always_down.create_subscription({"url": "https://example.com/hook",
                                         "events": ["import.completed"]})
        result = always_down.dispatch({"id": "import-3", "type": "import.completed"})
        self.assertEqual((result["delivered"], result["failed"]), (0, 1))
        queue = always_down.dead_letter_queue()
        self.assertEqual(len(queue), 1)
        self.assertEqual(queue[0]["status"], 500)
        self.assertEqual(queue[0]["attempts"], gw.WEBHOOK_MAX_ATTEMPTS)
        self.assertEqual(queue[0]["event"]["id"], "import-3")

    def test_refused_destination_is_not_retried_aggressively(self):
        attempts = []
        refused = gw.WebhookDispatcher("topsecret", delivery=lambda u, b, h: (attempts.append(1) or 403),
                                       clock=lambda: 1700000000.0, sleeper=lambda s: None)
        refused.create_subscription({"url": "https://example.com/hook",
                                     "events": ["import.completed"]})
        result = refused.dispatch({"id": "import-4", "type": "import.completed"})
        self.assertEqual(len(attempts), 1)
        self.assertEqual(result["failed"], 1)

    def test_subscription_filters_by_event_type_and_iv(self):
        self.status = 200
        self.dispatcher.create_subscription({"url": "https://example.com/iv",
                                             "events": ["spawn.created"],
                                             "filters": {"min_iv": 90}})
        self.dispatcher.dispatch({"id": "spawn-1", "type": "spawn.created", "iv_percent": 50})
        self.assertEqual(self.calls, [])  # below min_iv, and the other subscription wants imports
        self.dispatcher.dispatch({"id": "spawn-2", "type": "spawn.created", "iv_percent": 95})
        self.assertEqual(len(self.calls), 1)

    def test_revoked_subscription_receives_nothing(self):
        self.status = 200
        self.assertTrue(self.dispatcher.revoke("sub-1"))
        self.dispatcher.dispatch({"id": "import-9", "type": "import.completed"})
        self.assertEqual(self.calls, [])

    def test_subscription_validation_refuses_http_and_localhost(self):
        for url in ("http://example.com/hook", "https://localhost/hook",
                    "https://127.0.0.1/hook", "https://user:pass@example.com/hook", "", None):
            with self.assertRaises(gw.GatewayError) as caught:
                self.dispatcher.create_subscription({"url": url})
            self.assertEqual(caught.exception.status, 422, url)
        with self.assertRaises(gw.GatewayError):
            self.dispatcher.create_subscription({"url": "https://example.com/hook", "events": []})
        with self.assertRaises(gw.GatewayError):
            self.dispatcher.create_subscription({"url": "https://example.com/hook",
                                                 "events": ["spawn.fabricated"]})


class WebhookEmissionTests(unittest.TestCase):
    """Spawn/raid/quest/rocket webhooks may only come from real live-external facts."""

    def setUp(self):
        self.calls = []
        self.deliver = lambda url, body, headers: (self.calls.append(json.loads(body)) or 200)
        self.gateway = gw.Gateway(token="t", secret="s" * 32,
                                  dispatcher=gw.WebhookDispatcher("topsecret", delivery=self.deliver))
        self.gateway.webhooks.create_subscription({
            "url": "https://example.com/hook",
            "events": ["import.completed", "preset.created", "preset.deleted",
                       "source.status_changed", "spawn.created", "raid.created"]})
        self.headers = dict(AUTH)

    def test_import_emits_import_completed_only(self):
        self.gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": [row("a")]}, self.headers)
        self.assertEqual([event["type"] for event in self.calls], ["import.completed"])
        self.assertEqual(self.calls[0]["source_id"], "pgsharp")

    def test_personal_import_never_emits_spawn_webhooks(self):
        self.gateway.handle("POST", "/v1/imports/pgsharp", {}, {"events": [row("a")]}, self.headers)
        with self.assertRaises(gw.GatewayError) as caught:
            self.gateway.emit_live_event({"id": "spawn-x", "type": "spawn.created",
                                          "origin_kind": "personal_import", "source_id": "pgsharp"})
        self.assertEqual(caught.exception.status, 422)
        self.assertEqual([event["type"] for event in self.calls], ["import.completed"])

    def test_preset_changes_emit_webhooks(self):
        self.gateway.handle("POST", "/v1/presets", {}, {"name": "Hundo",
                                                        "query": {"iv_min": 100}}, self.headers)
        self.assertEqual([event["type"] for event in self.calls], ["preset.created"])
        self.calls.clear()
        self.gateway.handle("DELETE", "/v1/presets/preset-1", {}, {}, self.headers)
        self.assertEqual([event["type"] for event in self.calls], ["preset.deleted"])


class OpenApiDocumentTests(unittest.TestCase):
    def test_declares_required_paths_and_no_sixth_source(self):
        document = gw.openapi_document()
        self.assertEqual(document["openapi"], "3.1.0")
        paths = document["paths"]
        for required in ("/v1/health", "/v1/capabilities", "/v1/sources", "/v1/search",
                         "/v1/imports/pgsharp", "/v1/imports/ipogo", "/v1/events",
                         "/v1/webhooks/subscriptions"):
            self.assertIn(required, paths, required)
        self.assertNotIn("/v1/imports/pokemod", paths)
        self.assertEqual(document["components"]["securitySchemes"]["bearerAuth"]["scheme"], "bearer")
        search = paths["/v1/search"]["post"]
        self.assertEqual(search["responses"]["200"]["description"],
                         "Resultados reais (vazio se ausentes)")
        self.assertIn("422", search["responses"])
        self.assertIn("429", search["responses"])

    def test_health_has_no_security_requirement(self):
        document = gw.openapi_document()
        self.assertEqual(document["paths"]["/v1/health"]["get"]["security"], [])


class SelftestTests(unittest.TestCase):
    def test_selftest_passes(self):
        self.assertEqual(gw.selftest(), 0)


if __name__ == "__main__":
    unittest.main()
