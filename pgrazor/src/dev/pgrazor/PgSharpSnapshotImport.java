package dev.pgrazor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Imports an explicit PGS-owned snapshot; not a PGSharp wire protocol or authenticated origin. */
final class PgSharpSnapshotImport {
    private PgSharpSnapshotImport() {}

    static final class Result {
        final List<SourceEvent> events;
        final int rejected, duplicates;
        final boolean invalid;
        Result(List<SourceEvent> events, int rejected, int duplicates, boolean invalid) {
            this.events = Collections.unmodifiableList(new ArrayList<>(events));
            this.rejected = rejected;
            this.duplicates = duplicates;
            this.invalid = invalid;
        }
        String summary() {
            if (invalid) return "Snapshot 100% IV inválido, vencido ou incompatível. Nenhum registro carregado.";
            return "PGSharp declarado pelo usuário (origem não autenticada). Importados: "
                    + events.size() + " | rejeitados: " + rejected + " | duplicados: " + duplicates
                    + ". Dados locais, não feed ao vivo.";
        }
    }

    /** Null means this is not the explicit snapshot schema: use the ordinary importer. */
    static Result tryParse(String text, long now) {
        if (text == null || !text.contains(PgSharpHundoBridge.SCHEMA)) return null;
        String raw = text.startsWith("\uFEFF") ? text.substring(1) : text;
        if (!raw.trim().startsWith("{")) return null;
        if (raw.length() > PgSharpHundoBridge.MAX_INPUT_CHARS
                || raw.getBytes(StandardCharsets.UTF_8).length > PgSharpHundoBridge.MAX_INPUT_CHARS)
            return invalid();
        try {
            JSONObject doc = new JSONObject(raw);
            if (!PgSharpHundoBridge.SCHEMA.equals(doc.opt("schema"))) return null;
            if (doc.optBoolean("is_demo", false)) return invalid();
            JSONArray items = doc.getJSONArray("items");
            if (items.length() > LocalEventStore.MAX_EVENTS) return invalid();
            List<PgSharpHundoBridge.Row> rows = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject p = items.optJSONObject(i);
                if (p == null || p.optBoolean("is_demo", false)) {
                    rows.add(null);
                    continue;
                }
                rows.add(new PgSharpHundoBridge.Row(p.opt("category"), p.opt("coordinate_precision"),
                        p.opt("attack_iv"), p.opt("defense_iv"), p.opt("stamina_iv"), p.opt("iv_percent"),
                        p.opt("dex"), p.opt("form"), p.opt("lat"), p.opt("lon"), p.opt("expires_ms"),
                        p.opt("encounter_id")));
            }
            PgSharpHundoBridge.Report checked = PgSharpHundoBridge.validate(
                    new PgSharpHundoBridge.Batch(doc.opt("schema"), doc.opt("provider"),
                            doc.opt("coordinate_precision"), doc.opt("fetched_ms"), rows), now);
            List<SourceEvent> events = new ArrayList<>();
            for (PgSharpHundoBridge.Hundo h : checked.rows) {
                events.add(SourceEvent.builder(SourceRegistry.PGSHARP, "snapshot:" + h.key(), SourceEvent.Kind.POKEMON)
                        .originKind(OriginKind.PERSONAL_IMPORT)
                        .dataQuality(SourceEvent.QUALITY_IMPORTED)
                        .precision(SourceEvent.CoordinatePrecision.EXACT)
                        .pokemonId(h.dex).formId(h.form)
                        .coordinates(h.lat, h.lon)
                        .ivPercent(100.0).ivAtk(15).ivDef(15).ivSta(15)
                        .observedAt(checked.fetchedMs).retrievedAt(now).expiresAt(h.expiresMs)
                        .note("Snapshot pessoal PGS; origem PGSharp declarada pelo usuário, não autenticada.")
                        .build());
            }
            return new Result(events, checked.rejected, checked.duplicates, false);
        } catch (Exception malformed) {
            return invalid();
        }
    }

    private static Result invalid() {
        return new Result(Collections.<SourceEvent>emptyList(), 0, 0, true);
    }
}
