package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Account-scoped persistence for the local event store and the presets. Everything is plain JSON in
 * app-private preferences (already namespaced per account by {@link AccountProfiles}), so an import
 * never leaks into another account and a backup export never carries another account's rows.
 *
 * <p>No credentials, tokens or provider sessions are stored here: only the user's own imported rows
 * and filter presets.
 */
final class SourceDataStore {
    private SourceDataStore() {}

    static final String SCOPE = "pgrazor-source-data-v1";
    static final String KEY_EVENTS = "events";
    static final String KEY_PRESETS = "presets";

    static LocalEventStore events(Context context, long now) {
        LocalEventStore store = new LocalEventStore();
        SharedPreferences preferences = AccountProfiles.preferences(context, SCOPE);
        String raw = preferences.getString(KEY_EVENTS, null);
        if (raw == null || raw.trim().isEmpty()) return store;
        try {
            store.mergeJson(raw, now);
        } catch (JSONException ignored) {
            // A damaged store starts empty instead of crashing the radar; the raw value is kept so a
            // later export can still recover it.
        }
        return store;
    }

    static void saveEvents(Context context, LocalEventStore store) {
        if (store == null) return;
        AccountProfiles.preferences(context, SCOPE)
                .edit().putString(KEY_EVENTS, store.toJson()).apply();
    }

    static List<PresetCodec.Preset> presets(Context context) {
        SharedPreferences preferences = AccountProfiles.preferences(context, SCOPE);
        String raw = preferences.getString(KEY_PRESETS, null);
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        try {
            return new ArrayList<>(PresetCodec.importPresets(raw));
        } catch (JSONException invalid) {
            return new ArrayList<>();
        }
    }

    static void savePresets(Context context, List<PresetCodec.Preset> presets) {
        try {
            AccountProfiles.preferences(context, SCOPE)
                    .edit().putString(KEY_PRESETS, PresetCodec.export(presets)).apply();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Adds a preset, replacing an existing one with the same id, and persists. */
    static List<PresetCodec.Preset> upsertPreset(Context context, PresetCodec.Preset preset) {
        List<PresetCodec.Preset> current = presets(context);
        List<PresetCodec.Preset> next = new ArrayList<>();
        for (PresetCodec.Preset existing : current)
            if (!existing.id.equals(preset.id)) next.add(existing);
        next.add(preset);
        List<PresetCodec.Preset> deduped = new ArrayList<>(PresetCodec.dedupe(next));
        savePresets(context, deduped);
        return deduped;
    }

    static void deletePreset(Context context, String presetId) {
        List<PresetCodec.Preset> next = new ArrayList<>();
        for (PresetCodec.Preset existing : presets(context))
            if (!existing.id.equals(presetId)) next.add(existing);
        savePresets(context, next);
    }
}
