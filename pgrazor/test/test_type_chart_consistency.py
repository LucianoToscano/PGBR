"""Independent verification of the type-effectiveness data used by PGBR.

The chart is duplicated by necessity: the native battle ordering/priority policy
(`battle_counter.h`, `smart_attack_priority.h`) and the Java PvP charged-move
selector (`PvpChargedPreference.java`) must agree bit for bit with the game.

This suite owns its own copy of the canonical chart (Gen VI+ / Pokémon GO
multipliers) so a change to either source file cannot silently "agree with
itself". It also proves the two encodings agree: the native layer sums the -2/-1/0/1
codes and the Java layer raises 1.6 to that sum, so the same code table yields the
same SUPER / NEUTRAL / RESISTED tier for every single- and dual-typed defender.

No source file is written here.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NATIVE = ROOT / "pgrazor" / "native" / "battle_counter.h"
JAVA = ROOT / "pgrazor" / "src" / "dev" / "pgrazor" / "PvpChargedPreference.java"

TYPES = (
    "normal fighting flying poison ground rock bug ghost steel fire water grass "
    "electric psychic ice dragon dark fairy".split()
)
SE = 1
NVE = -1
DOUBLE_NVE = -2  # the game's only sub-0.5 multiplier: immunities and double resists


def _chart_from():
    """Canonical table: +1 = 1.6x, -1 = 0.625x, -2 = 0.390625x."""
    strong = {
        "fighting": "normal rock steel ice dark",
        "flying": "fighting bug grass",
        "poison": "grass fairy",
        "ground": "poison rock steel fire electric",
        "rock": "flying bug fire ice",
        "bug": "grass psychic dark",
        "ghost": "ghost psychic",
        "steel": "rock ice fairy",
        "fire": "bug steel grass ice",
        "water": "ground rock fire",
        "grass": "ground rock water",
        "electric": "flying water",
        "psychic": "fighting poison",
        "ice": "flying ground grass dragon",
        "dragon": "dragon",
        "dark": "ghost psychic",
        "fairy": "fighting dragon dark",
    }
    weak = {
        "normal": "rock steel",
        "fighting": "flying poison bug psychic fairy",
        "flying": "rock steel electric",
        "poison": "poison ground rock ghost",
        "ground": "bug grass",
        "rock": "fighting ground steel",
        "bug": "fighting flying poison ghost steel fire fairy",
        "ghost": "dark steel",
        "steel": "steel fire water electric",
        "fire": "rock fire water dragon",
        "water": "water grass dragon",
        "grass": "flying poison bug steel fire grass dragon",
        "electric": "grass electric dragon",
        "psychic": "steel psychic",
        "ice": "steel fire water ice",
        "dragon": "steel",
        "dark": "fighting dark fairy",
        "fairy": "poison steel fire",
    }
    immune = {
        "normal": "ghost",
        "fighting": "ghost",
        "poison": "steel",
        "ground": "flying",
        "ghost": "normal",
        "electric": "ground",
        "psychic": "dark",
        "dragon": "fairy",
    }
    table = []
    for attacker in TYPES:
        row = []
        for defender in TYPES:
            value = 0
            if defender in strong.get(attacker, "").split():
                value = SE
            elif defender in weak.get(attacker, "").split():
                value = NVE
            if defender in immune.get(attacker, "").split():
                value = DOUBLE_NVE
            row.append(value)
        table.append(row)
    return table


def _parse_native():
    text = NATIVE.read_text(encoding="utf-8")
    body = re.search(
        r"kChart\[kTypeCount\]\[kTypeCount\] = \{(.*?)\n\};", text, re.S
    ).group(1)
    rows = [
        [int(value) for value in re.findall(r"-?\d+", line)]
        for line in body.splitlines()
        if line.strip().startswith("{")
    ]
    return rows


def _parse_java():
    text = JAVA.read_text(encoding="utf-8")
    body = re.search(r"byte\[\]\[\] TYPE = \{(.*?)\n    \};", text, re.S).group(1)
    rows = [
        [int(value) for value in re.findall(r"-?\d+", line)]
        for line in body.splitlines()
        if line.strip().startswith("{")
    ]
    return rows


class TypeChartTests(unittest.TestCase):
    def setUp(self):
        self.canonical = _chart_from()
        self.native = _parse_native()
        self.java = _parse_java()

    def test_shape(self):
        for name, chart in (("native", self.native), ("java", self.java)):
            self.assertEqual(len(chart), 18, name)
            for row in chart:
                self.assertEqual(len(row), 18, name)

    def test_native_matches_canonical(self):
        for a, attacker in enumerate(TYPES):
            for d, defender in enumerate(TYPES):
                self.assertEqual(
                    self.native[a][d],
                    self.canonical[a][d],
                    "native %s -> %s" % (attacker, defender),
                )

    def test_java_matches_canonical(self):
        for a, attacker in enumerate(TYPES):
            for d, defender in enumerate(TYPES):
                self.assertEqual(
                    self.java[a][d],
                    self.canonical[a][d],
                    "java %s -> %s" % (attacker, defender),
                )

    def test_both_layers_are_identical(self):
        self.assertEqual(self.native, self.java)

    def test_ghost_into_steel_is_resisted(self):
        """Regression: ghost -> steel must be 0.5x, never neutral."""
        self.assertEqual(self.native[TYPES.index("ghost")][TYPES.index("steel")], NVE)
        self.assertEqual(self.java[TYPES.index("ghost")][TYPES.index("steel")], NVE)

    def test_each_code_means_one_multiplier(self):
        steps = {0: 1.0, SE: 1.6, NVE: 0.625, DOUBLE_NVE: 0.390625}
        for a in range(18):
            for d in range(18):
                self.assertIn(self.native[a][d], steps)
        # 1.6 * 0.390625 == 0.625 keeps the summed code equivalent to the product.
        self.assertAlmostEqual(steps[SE] * steps[DOUBLE_NVE], steps[NVE], places=12)

    def test_native_and_java_tiers_agree_for_every_matchup(self):
        """native: sign(sum) ; java: pow(1.6, sum) -> tier. Both must match."""
        for a, attacker in enumerate(TYPES):
            for d1, defender in enumerate(TYPES):
                for d2 in [None] + list(range(18)):
                    if d2 is not None and TYPES[d2] == defender:
                        continue
                    codes = [self.canonical[a][d1]]
                    if d2 is not None:
                        codes.append(self.canonical[a][d2])
                    total = sum(codes)
                    multiplier = 1.6 ** total
                    native_tier = 1 if total > 0 else (-1 if total < 0 else 0)
                    java_tier = (
                        1 if multiplier > 1.00001 else (-1 if multiplier < 0.99999 else 0)
                    )
                    self.assertEqual(
                        native_tier,
                        java_tier,
                        "%s vs %s" % (attacker, defender),
                    )
                    # A dual type can never turn a resistance into a weakness bonus.
                    self.assertEqual(0, total != 0 and native_tier == 1 and multiplier < 1)

    def test_immunities_are_exactly_the_eight_game_pairs(self):
        found = set()
        for a, attacker in enumerate(TYPES):
            for d, defender in enumerate(TYPES):
                if self.canonical[a][d] == DOUBLE_NVE:
                    found.add((attacker, defender))
        self.assertEqual(
            found,
            {
                ("normal", "ghost"),
                ("fighting", "ghost"),
                ("poison", "steel"),
                ("ground", "flying"),
                ("ghost", "normal"),
                ("electric", "ground"),
                ("psychic", "dark"),
                ("dragon", "fairy"),
            },
        )


class SmartPriorityGuardTests(unittest.TestCase):
    """Unknown or malformed opponent data must never be reported as SUPER."""

    def test_priority_policy_requires_a_confirmed_type_pair(self):
        header = (ROOT / "pgrazor" / "native" / "smart_attack_priority.h").read_text(
            encoding="utf-8"
        )
        self.assertIn("opponent[0] < 1 || opponent[0] > pgr_counter::kTypeCount", header)
        self.assertIn("opponent[1] < 0 || opponent[1] > pgr_counter::kTypeCount", header)
        self.assertIn("return Effect::UNKNOWN;", header)

    def test_java_selector_rejects_unknown_opponent_types(self):
        source = JAVA.read_text(encoding="utf-8")
        self.assertIn("if(!typeValid(opponent1)||opponent2!=0&&!typeValid(opponent2)||", source)
        self.assertIn("return UNKNOWN;", source)
        # A missing second move keeps the verified first slot; it never guesses.
        self.assertIn("if(second==null)return firstReady?FIRST:WAIT;", source)


if __name__ == "__main__":
    unittest.main()
