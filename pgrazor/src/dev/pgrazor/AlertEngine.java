package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local alerts: a preset fires when a row that was actually imported or received matches its
 * criteria. There is no push channel in the app, so this is evaluated over the store/feed the user
 * already has — and a preset with no criteria never fires, because "match everything" would be an
 * invented alert.
 *
 * <p>Shiny is only reported when the origin observed it ({@code shiny_observed=true}). A catalogue
 * entry, a shiny-eligible flag or a sprite never produces an alert.
 */
public final class AlertEngine {
    private AlertEngine() {}

    public static final class Alert {
        public final String presetId;
        public final String presetName;
        public final SourceEvent event;
        public final String reason;

        Alert(String presetId, String presetName, SourceEvent event, String reason) {
            this.presetId = presetId;
            this.presetName = presetName;
            this.event = event;
            this.reason = reason;
        }
    }

    public static List<Alert> evaluate(List<PresetCodec.Preset> presets, List<SourceEvent> events, long now) {
        List<Alert> alerts = new ArrayList<>();
        if (presets == null || events == null || events.isEmpty()) return Collections.emptyList();
        Set<String> seen = new HashSet<>();
        for (PresetCodec.Preset preset : presets) {
            if (preset == null || !preset.query.isActive()) continue; // no criteria => no alert
            EventFilterEngine.Outcome outcome = EventFilterEngine.apply(preset.query, events, now);
            for (SourceEvent event : outcome.kept) {
                if (event == null || event.expired(now) || event.isDemo) continue;
                if (!seen.add(preset.id + "|" + event.sourceId + "|" + event.eventId)) continue;
                alerts.add(new Alert(preset.id, preset.name, event, reasonFor(preset.query, event)));
            }
        }
        return Collections.unmodifiableList(alerts);
    }

    private static String reasonFor(SourceQuery query, SourceEvent event) {
        StringBuilder sb = new StringBuilder();
        List<String> criteria = query.activeCriteria();
        if (!criteria.isEmpty()) sb.append("Critérios: ").append(join(criteria));
        if (event.shinyObserved == Boolean.TRUE)
            sb.append(sb.length() > 0 ? " · " : "").append("Shiny observado pela fonte.");
        else if (event.name.toLowerCase(java.util.Locale.ROOT).contains("shiny"))
            sb.append(sb.length() > 0 ? " · " : "").append("Nome menciona shiny sem observação confirmada.");
        sb.append(sb.length() > 0 ? " · " : "").append("Fonte: ").append(event.sourceLabel());
        return sb.toString();
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
