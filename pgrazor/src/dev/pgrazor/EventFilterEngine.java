package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The single filtering implementation used by the radar tabs, both Snipers and the presets. It is
 * pure: no I/O, no clock of its own, no Android types.
 *
 * <p>Three properties the UI depends on:
 * <ol>
 *   <li><b>Honest counts</b> — received, expired, demo, invalid coordinates and filtered are
 *       reported separately, so "25 recebidos" can never be presented as "varredura global".</li>
 *   <li><b>Unsupported filters are named</b> — when the origin returned no field for an active
 *       filter, the filter still fails closed (no rows) and the outcome says which filter the
 *       source cannot satisfy.</li>
 *   <li><b>Fail-closed coordinates</b> — a radius filter without a centre, or a row without
 *       coordinates, never silently widens the search.</li>
 * </ol>
 */
public final class EventFilterEngine {
    private EventFilterEngine() {}

    public static final class Outcome {
        public final List<SourceEvent> kept;
        public final int received, expired, demoDropped, invalidCoordinates, filteredOut;
        public final List<String> appliedLocally;
        public final List<String> unsupported;
        public final Map<String, Integer> perSource;
        public final List<String> criteria;

        Outcome(List<SourceEvent> kept, int received, int expired, int demoDropped,
                int invalidCoordinates, int filteredOut, List<String> appliedLocally,
                List<String> unsupported, Map<String, Integer> perSource, List<String> criteria) {
            this.kept = Collections.unmodifiableList(new ArrayList<>(kept));
            this.received = received;
            this.expired = expired;
            this.demoDropped = demoDropped;
            this.invalidCoordinates = invalidCoordinates;
            this.filteredOut = filteredOut;
            this.appliedLocally = Collections.unmodifiableList(new ArrayList<>(appliedLocally));
            this.unsupported = Collections.unmodifiableList(new ArrayList<>(unsupported));
            this.perSource = Collections.unmodifiableMap(new LinkedHashMap<>(perSource));
            this.criteria = Collections.unmodifiableList(new ArrayList<>(criteria));
        }

        public boolean isEmpty() { return kept.isEmpty(); }

        /** One-line, countable summary; never claims more coverage than the rows prove. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Recebidos ").append(received);
            if (demoDropped > 0) sb.append(" · ").append(demoDropped).append(" demo ignorados");
            if (invalidCoordinates > 0) sb.append(" · ").append(invalidCoordinates).append(" sem coordenada válida");
            if (expired > 0) sb.append(" · ").append(expired).append(" expirados");
            if (filteredOut > 0) sb.append(" · ").append(filteredOut).append(" filtrados");
            sb.append(" · exibidos ").append(kept.size());
            return sb.toString();
        }
    }

    /** Applies the query to already-normalised events. */
    public static Outcome apply(SourceQuery query, List<SourceEvent> events, long now) {
        SourceQuery q = query == null ? SourceQuery.builder().build() : query;
        List<SourceEvent> safe = events == null ? Collections.<SourceEvent>emptyList() : events;
        List<String> criteria = q.activeCriteria();
        List<String> activeKeys = activeKeys(q);

        // Which active filters does the payload actually carry a field for?
        Set<String> present = new LinkedHashSet<>();
        for (SourceEvent event : safe) {
            for (String key : activeKeys) if (fieldPresent(event, key)) present.add(key);
        }
        List<String> unsupported = new ArrayList<>();
        List<String> appliedLocally = new ArrayList<>();
        for (String key : activeKeys) {
            if (present.contains(key)) appliedLocally.add(label(key));
            else unsupported.add(label(key));
        }

        List<SourceEvent> kept = new ArrayList<>();
        Map<String, Integer> perSource = new LinkedHashMap<>();
        int expired = 0, demo = 0, invalid = 0, filtered = 0;
        for (SourceEvent event : safe) {
            if (event == null) continue;
            if (event.isDemo && !q.includeDemo) { demo++; continue; }
            if (!q.includeExpired && event.expired(now)) { expired++; continue; }
            boolean needsCoordinates = q.radiusKm != null && q.hasCenter();
            if (needsCoordinates && !event.hasCoordinates()) { invalid++; continue; }
            if (!matches(q, event, now, activeKeys)) { filtered++; continue; }
            kept.add(event);
            Integer count = perSource.get(event.sourceId);
            perSource.put(event.sourceId, count == null ? 1 : count + 1);
        }

        sort(q, kept, now);
        if (kept.size() > q.limit) kept = new ArrayList<>(kept.subList(0, q.limit));
        return new Outcome(kept, safe.size(), expired, demo, invalid, filtered, appliedLocally,
                unsupported, perSource, criteria);
    }

    // --- matching --------------------------------------------------------------------------------

    private static boolean matches(SourceQuery q, SourceEvent e, long now, List<String> keys) {
        int matched = 0;
        for (String key : keys) {
            if (criterionMatches(q, e, now, key)) matched++;
            else if (q.andLogic) return false;
        }
        return !q.andLogic ? matched > 0 : true;
    }

    private static boolean criterionMatches(SourceQuery q, SourceEvent e, long now, String key) {
        switch (key) {
            case "kind": return q.kinds.contains(e.kind);
            case "origin": return q.originKinds.contains(e.originKind);
            case "species": return e.pokemonId != null && q.pokemonIds.contains(e.pokemonId);
            case "form": return e.formId != null && q.formIds.contains(e.formId);
            case "iv_percent":
                return e.ivPercent != null
                        && (q.ivMin == null || e.ivPercent >= q.ivMin)
                        && (q.ivMax == null || e.ivPercent <= q.ivMax);
            case "atk": return e.ivAtk != null
                    && (q.atkMin == null || e.ivAtk >= q.atkMin)
                    && (q.atkMax == null || e.ivAtk <= q.atkMax);
            case "def": return e.ivDef != null
                    && (q.defMin == null || e.ivDef >= q.defMin)
                    && (q.defMax == null || e.ivDef <= q.defMax);
            case "sta": return e.ivSta != null
                    && (q.staMin == null || e.ivSta >= q.staMin)
                    && (q.staMax == null || e.ivSta <= q.staMax);
            case "cp": return e.cp != null
                    && (q.cpMin == null || e.cp >= q.cpMin)
                    && (q.cpMax == null || e.cp <= q.cpMax);
            case "level": return e.level != null
                    && (q.levelMin == null || e.level >= q.levelMin)
                    && (q.levelMax == null || e.level <= q.levelMax);
            case "gender": return q.gender != null && q.gender.equals(e.gender);
            case "size": return e.size != null && q.sizes.contains(e.size);
            case "weather": return q.weatherBoost != null && q.weatherBoost.equals(e.weatherBoost);
            case "pvp": {
                if (q.pvpLeague == null) return false;
                Integer rank = SourceQuery.LEAGUE_ULTRA.equals(q.pvpLeague) ? e.pvpUltraRank : e.pvpGreatRank;
                return rank != null && (q.pvpMaxRank == null || rank <= q.pvpMaxRank);
            }
            case "radius": {
                if (!q.hasCenter() || !e.hasCoordinates()) return false;
                double km = e.distanceKm(q.centerLat, q.centerLon);
                return !Double.isNaN(km) && km <= q.radiusKm;
            }
            case "ttl": return e.expiresAt > 0 && e.remainingMs(now) >= q.minTtlSeconds * 1000L;
            case "expires_within": return e.expiresAt > 0
                    && e.remainingMs(now) <= q.maxRemainingSeconds * 1000L;
            case "text": {
                String needle = q.text.toLowerCase(java.util.Locale.ROOT);
                if (needle.isEmpty()) return false;
                if (e.name != null && e.name.toLowerCase(java.util.Locale.ROOT).contains(needle)) return true;
                return e.pokemonId != null && ("#" + e.pokemonId).equals(needle);
            }
            default: return true; // unknown criterion never blocks
        }
    }

    /** Keys of the criteria that are active, in a stable order. */
    private static List<String> activeKeys(SourceQuery q) {
        List<String> keys = new ArrayList<>();
        if (!q.kinds.isEmpty()) keys.add("kind");
        if (!q.originKinds.isEmpty()) keys.add("origin");
        if (!q.pokemonIds.isEmpty()) keys.add("species");
        if (!q.formIds.isEmpty()) keys.add("form");
        if (q.ivMin != null || q.ivMax != null) keys.add("iv_percent");
        if (q.atkMin != null || q.atkMax != null) keys.add("atk");
        if (q.defMin != null || q.defMax != null) keys.add("def");
        if (q.staMin != null || q.staMax != null) keys.add("sta");
        if (q.cpMin != null || q.cpMax != null) keys.add("cp");
        if (q.levelMin != null || q.levelMax != null) keys.add("level");
        if (q.gender != null) keys.add("gender");
        if (!q.sizes.isEmpty()) keys.add("size");
        if (q.weatherBoost != null) keys.add("weather");
        if (q.pvpLeague != null) keys.add("pvp");
        if (q.radiusKm != null && q.hasCenter()) keys.add("radius");
        if (q.minTtlSeconds != null) keys.add("ttl");
        if (q.maxRemainingSeconds != null) keys.add("expires_within");
        if (!q.text.isEmpty()) keys.add("text");
        return keys;
    }

    private static boolean fieldPresent(SourceEvent e, String key) {
        switch (key) {
            case "kind": return true;
            case "origin": return true;
            case "species": return e.pokemonId != null;
            case "form": return e.formId != null;
            case "iv_percent": return e.ivPercent != null;
            case "atk": return e.ivAtk != null;
            case "def": return e.ivDef != null;
            case "sta": return e.ivSta != null;
            case "cp": return e.cp != null;
            case "level": return e.level != null;
            case "gender": return e.gender != null;
            case "size": return e.size != null;
            case "weather": return e.weatherBoost != null;
            case "pvp": return e.pvpGreatRank != null || e.pvpUltraRank != null;
            case "radius": return e.hasCoordinates();
            case "ttl": return e.expiresAt > 0;
            case "expires_within": return e.expiresAt > 0;
            case "text": return e.pokemonId != null || (e.name != null && !e.name.isEmpty());
            default: return true;
        }
    }

    private static String label(String key) {
        switch (key) {
            case "kind": return "tipo de evento";
            case "origin": return "origem dos dados";
            case "species": return "espécies";
            case "form": return "formas";
            case "iv_percent": return "IV %";
            case "atk": return "IV ataque";
            case "def": return "IV defesa";
            case "sta": return "IV stamina";
            case "cp": return "PC";
            case "level": return "nível";
            case "gender": return "sexo";
            case "size": return "tamanho";
            case "weather": return "clima";
            case "pvp": return "rank PvP";
            case "radius": return "raio";
            case "ttl": return "TTL mínimo";
            case "expires_within": return "expiração máxima";
            case "text": return "busca por texto";
            default: return key;
        }
    }

    // --- ordering --------------------------------------------------------------------------------

    private static void sort(SourceQuery q, List<SourceEvent> kept, long now) {
        final boolean byDistance = q.sort == SourceQuery.Sort.DISTANCE && q.hasCenter();
        Comparator<SourceEvent> comparator;
        switch (q.sort) {
            case IV_DESC:
                comparator = Comparator.comparing((SourceEvent e) -> e.ivPercent,
                        Comparator.nullsLast(Comparator.<Double>reverseOrder()));
                break;
            case CP_DESC:
                comparator = Comparator.comparing((SourceEvent e) -> e.cp,
                        Comparator.nullsLast(Comparator.<Integer>reverseOrder()));
                break;
            case LEVEL_DESC:
                comparator = Comparator.comparing((SourceEvent e) -> e.level,
                        Comparator.nullsLast(Comparator.<Integer>reverseOrder()));
                break;
            case EXPIRES_ASC:
                comparator = Comparator.comparingLong(e -> e.expiresAt <= 0 ? Long.MAX_VALUE : e.expiresAt);
                break;
            case NEWEST:
                comparator = Comparator.comparingLong(e -> -e.retrievedAt);
                break;
            default:
                comparator = byDistance ? Comparator.comparingDouble(
                        e -> e.hasCoordinates() ? e.distanceKm(q.centerLat, q.centerLon) : Double.MAX_VALUE)
                        : Comparator.comparingLong(e -> e.retrievedAt <= 0 ? Long.MAX_VALUE : e.expiresAt);
                break;
        }
        kept.sort(comparator);
    }
}
