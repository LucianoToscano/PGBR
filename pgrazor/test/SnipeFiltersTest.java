package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Offline, synthetic-only verification of the two independent Snipe slots.
 *
 * Every fixture is built in code; no feed response, account, credential or network access is used.
 * "100% IV" here is the synthetic row's own claim, exactly like the real schema's.
 */
public final class SnipeFiltersTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void reject(JSONObject payload, String what) {
        boolean failed = false;
        try {
            SnipeFilters.fromJson(payload);
        } catch (JSONException expected) {
            failed = true;
        }
        check(failed, "Must reject " + what);
    }

    /** Minimal synthetic target so the filter can be exercised without a feed. */
    private static SnipeFilters.Target target(final int dex, final int form, final int iv,
                                              final double level, final long despawnMs) {
        return new SnipeFilters.Target() {
            public int dex() { return dex; }
            public int form() { return form; }
            public int claimedIv() { return iv; }
            public double level() { return level; }
            public long despawnMs() { return despawnMs; }
        };
    }

    private static JSONObject json(String text) throws JSONException {
        return new JSONObject(text);
    }

    public static void main(String[] args) throws Exception {
        long now = 1_800_000_000_000L;
        long alive = now + 300_000L;

        // ---------------------------------------------------------------- broad defaults
        SnipeFilters off = SnipeFilters.defaults();
        check(!off.enabled(), "a new slot must start disabled");
        check(!off.ivBounded() && !off.levelBounded() && off.form() == SnipeFilters.FORM_ANY,
                "a new slot must not invent bounds");
        check(!off.matches(target(25, 0, 100, 20, alive), now),
                "a disabled slot must list nothing");
        check(off.select(null, now).isEmpty(), "null rows never throw and never match");

        SnipeFilters on = SnipeFilters.fromJson(json("{\"enabled\":true}"));
        check(on.enabled(), "enabled must round-trip");
        check(on.matches(target(25, 0, 100, 20, alive), now),
                "an enabled slot with no bound must not hide a valid target");
        check(!on.matches(target(25, 0, 100, 20, now - 1), now), "expired rows never match");
        check(!on.matches(target(25, 0, 100, 20, now), now), "a row dying exactly now never matches");

        // ---------------------------------------------------------------- per-slot independence
        SnipeFilters slotOne = SnipeFilters.fromJson(json(
                "{\"enabled\":true,\"species\":[25],\"min_iv\":98,\"max_iv\":100}"));
        SnipeFilters slotTwo = SnipeFilters.fromJson(json(
                "{\"enabled\":true,\"species\":[1,4],\"min_iv\":0,\"max_iv\":100}"));
        check(slotOne.matches(target(25, 0, 100, 20, alive), now), "slot 1 keeps its own species");
        check(!slotOne.matches(target(4, 0, 100, 20, alive), now), "slot 1 must ignore slot 2 species");
        check(slotTwo.matches(target(4, 0, 80, 20, alive), now), "slot 2 keeps its own IV range");
        check(!slotTwo.matches(target(25, 0, 100, 20, alive), now), "slot 2 must ignore slot 1 species");
        check(!slotOne.toJson().toString().equals(slotTwo.toJson().toString()),
                "the two slots must not serialise to the same state");
        check(slotOne.withEnabled(false).enabled() == false && slotOne.enabled(),
                "withEnabled must not mutate the original slot");

        // ---------------------------------------------------------------- round-trip
        JSONObject restored = slotOne.toJson();
        SnipeFilters again = SnipeFilters.fromJson(restored);
        check(again.enabled() == slotOne.enabled()
                        && again.species().equals(slotOne.species())
                        && again.minIv() == slotOne.minIv()
                        && again.maxIv() == slotOne.maxIv()
                        && again.form() == slotOne.form()
                        && again.minRemainingSeconds() == slotOne.minRemainingSeconds(),
                "toJson/fromJson must round-trip every field");
        check(SnipeFilters.containsAnyKey(restored), "a saved slot must be recognised");
        check(!SnipeFilters.containsAnyKey(json("{\"other\":1}")),
                "a foreign payload must not be read as a slot");
        check(SnipeFilters.fromJson(json("{\"enabled\":true,\"future_key\":9}")).enabled(),
                "unknown keys must be ignored");

        // ---------------------------------------------------------------- unknown values fail closed
        SnipeFilters bounded = SnipeFilters.fromJson(json(
                "{\"enabled\":true,\"min_iv\":90,\"min_level\":30}"));
        check(!bounded.matches(target(25, 0, SnipeFilters.IV_UNKNOWN, 20, alive), now),
                "an unstated IV must never satisfy an IV bound");
        check(!bounded.matches(target(25, 0, 100, Double.NaN, alive), now),
                "an unstated level must never satisfy a level bound");
        check(bounded.matches(target(25, 0, 100, 35, alive), now), "stated values inside the bounds match");
        check(!bounded.matches(target(25, 0, 100, 29.5, alive), now), "level below minimum is rejected");

        SnipeFilters exactForm = SnipeFilters.fromJson(json("{\"enabled\":true,\"form\":3}"));
        check(!exactForm.matches(target(25, -1, 100, 20, alive), now),
                "an unstated form must never satisfy an exact form");
        check(!exactForm.matches(target(25, 4, 100, 20, alive), now), "a different form is rejected");
        check(exactForm.matches(target(25, 3, 100, 20, alive), now), "the exact form matches");

        // ---------------------------------------------------------------- remaining time
        SnipeFilters patient = SnipeFilters.fromJson(json("{\"enabled\":true,\"min_remaining_s\":120}"));
        check(!patient.matches(target(25, 0, 100, 20, now + 60_000L), now),
                "a target dying too soon must be skipped");
        check(patient.matches(target(25, 0, 100, 20, now + 180_000L), now),
                "a target with enough time must match");

        // ---------------------------------------------------------------- editor text
        SnipeFilters fromText = SnipeFilters.fromEditor(true, "25, #6; 150", "98", "100", "", "", "", "");
        check(fromText.species().size() == 3 && fromText.species().contains(25)
                        && fromText.species().contains(6) && fromText.species().contains(150),
                "editor species text must accept commas, semicolons and #");
        check(fromText.ivBounded() && fromText.minIv() == 98, "editor IV bounds must be kept");
        check(!fromText.levelBounded(), "an empty level field must not create a level bound");
        check(fromText.matches(target(25, 0, 100, 5, alive), now),
                "an empty level field must not hide low levels");

        SnipeFilters duplicateText = SnipeFilters.fromEditor(true, "25,25,25", "", "", "", "", "", "");
        check(duplicateText.species().size() == 1, "duplicate species must be collapsed");

        // Each row: {species, minIv, maxIv, minLevel, maxLevel, form, remainingSeconds}.
        for (String[] bad : new String[][]{
                {null, "99", "100", null, null, null, null},     // min_iv > max_iv
                {"0", null, null, null, null, null, null},        // species out of range
                {"1026", null, null, null, null, null, null},     // species out of range
                {"abc", null, null, null, null, null, null},      // species not a number
                {null, "101", null, null, null, null, null},      // IV out of range
                {null, "-5", null, null, null, null, null},       // negative IV
                {null, null, null, "0", null, null, null},        // level below 1
                {null, null, null, "51", null, null, null},       // level out of range
                {null, null, null, "10", "5", null, null},        // min_level > max_level
                {null, "9.5", null, null, null, null, null},      // fractional IV
                {null, null, null, null, null, null, "-1"}}) {    // negative seconds
            boolean failed = false;
            try {
                SnipeFilters.fromEditor(true, bad[0], bad[1], bad[2], bad[3], bad[4], bad[5], bad[6]);
            } catch (JSONException expected) {
                failed = true;
            }
            check(failed, "editor must reject " + java.util.Arrays.toString(bad));
        }

        // ---------------------------------------------------------------- stored payload guards
        reject(json("{\"enabled\":\"yes\"}"), "a non-boolean enabled flag");
        reject(json("{\"min_iv\":\"90\"}"), "a string IV bound");
        reject(json("{\"min_level\":20,\"max_level\":10}"), "inverted level bounds");
        reject(json("{\"species\":\"25\"}"), "species that is not a list");
        reject(json("{\"species\":[0]}"), "species below the dex range");
        reject(json("{\"species\":[1026]}"), "species above the dex range");
        reject(json("{\"species\":[25.5]}"), "a fractional species id");
        StringBuilder many = new StringBuilder("{\"species\":[");
        for (int i = 1; i <= SnipeFilters.MAX_SPECIES + 1; i++) {
            if (i > 1) many.append(',');
            many.append(i);
        }
        reject(json(many.append("]}").toString()), "too many species");
        reject(json("{\"min_remaining_s\":999999}"), "an absurd remaining-time floor");

        // ---------------------------------------------------------------- feed rows
        JSONObject feed = json("{\"v\":1,\"fetched_ms\":" + (now - 1000)
                + ",\"interval_s\":30,\"hundos\":["
                + "[\"111\",25,0,-6.2,-35.2," + alive + ",900,33.5],"
                + "[\"112\",4,0,-6.2,-35.2," + alive + ",700,20.0],"
                + "[\"113\",1,0,-6.2,-35.2," + alive + "]]}");
        HundoFeedData.Snapshot snapshot = HundoFeedData.parse(feed, now);
        check(snapshot.entries.size() == 3, "the synthetic feed must decode three rows");
        check(snapshot.entries.get(0).iv == HundoFeedData.FEED_CLAIMED_IV,
                "the schema's 100% IV claim must be carried, not invented per row");
        check(snapshot.entries.get(2).cp == -1 && Double.isNaN(snapshot.entries.get(2).level),
                "missing CP/level must stay unknown");

        List<HundoFeedData.Entry> rows = new ArrayList<>(snapshot.entries);
        List<HundoFeedData.Entry> one = slotOne.select(rows, now);
        List<HundoFeedData.Entry> two = slotTwo.select(rows, now);
        check(one.size() == 1 && one.get(0).dex == 25, "slot 1 must select only its own species");
        check(two.size() == 2, "slot 2 must select only its own species");
        check(slotOne.select(rows, now).size() == 1, "selecting twice must not consume rows");
        check(SnipeFilters.defaults().select(rows, now).isEmpty(), "a disabled slot selects nothing");

        SnipeFilters levelSlot = SnipeFilters.fromJson(json(
                "{\"enabled\":true,\"min_level\":30,\"max_level\":40}"));
        List<HundoFeedData.Entry> levels = levelSlot.select(rows, now);
        check(levels.size() == 1 && levels.get(0).dex == 25,
                "a level bound must drop rows whose level the provider did not state");

        // ---------------------------------------------------------------- labels
        check(off.summary().contains("Desligado"), "a disabled slot must say so");
        check(slotOne.summary().contains("IV 98–100") && slotOne.summary().contains("1 espécie"),
                "the summary must describe the active bounds");
        check(SnipeFilters.fromJson(json("{\"enabled\":true}")).summary()
                        .contains("todas as espécies"),
                "an empty species list must read as all species");

        System.out.println("SnipeFiltersTest: OK");
    }
}
