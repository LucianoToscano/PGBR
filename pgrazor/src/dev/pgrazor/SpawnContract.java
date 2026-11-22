package dev.pgrazor;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The single internal Pokémon contract shared by Sniper 1 and Sniper 2.
 *
 * <p>Every provider row is converted into one immutable {@link Spawn}, so both snipers filter and
 * render the same normalised data while keeping their own filter sets. The model is deliberately
 * explicit about what is <em>unknown</em>:
 * <ul>
 *   <li>Absent numbers are {@code -1} (or {@link #UNKNOWN_LEVEL} / {@link #UNKNOWN_RANK}); a
 *       {@code -1} never means {@code 0} and never satisfies a bound.</li>
 *   <li>"100% IV" is never inferred from a percentage that the provider did not send, nor from
 *       {@code cp} or a species name; {@link #perfect()} requires proven 15/15/15.</li>
 *   <li>Shiny is a {@code possibility}, never a guarantee for the account; {@link #shinyConfirmed}
 *       stays false unless a provider states it for that very encounter.</li>
 * </ul>
 *
 * <p>Nothing here performs I/O: this class is pure so the whole chain
 * (provider row -> normalise -> dedupe -> filter -> list) is unit-testable without a device.
 */
public final class SpawnContract {
    private SpawnContract() {}

    /** Marker for "provider did not state this value". */
    public static final int UNKNOWN = -1;
    /** Marker for an unknown level (0 would be a real, very low level). */
    public static final double UNKNOWN_LEVEL = Double.NaN;
    public static final int UNKNOWN_RANK = -1;
    public static final long UNKNOWN_TIME = 0L;

    /** How the coordinates of a row are known. */
    public enum Coordinates {
        /** Latitude/longitude present and validated; teleport is possible after confirmation. */
        AVAILABLE,
        /** Provider returned a row with a reveal code only; no coordinates yet. */
        BLOCKED,
        /** Present but rejected by validation (non-finite, out of range, 0,0). */
        INVALID
    }

    /** What a given source can actually deliver, so the UI never implies more than the feed gives. */
    public static final class Capabilities {
        public final boolean coordinates, ivComponents, ivPercent, cp, level, gender, form, size,
                despawn, shinyPossibility, shinyConfirmed, pvpRanks, serverSideSpecies,
                distanceQuery;
        public final String label;

        public Capabilities(String label, boolean coordinates, boolean ivComponents, boolean ivPercent,
                            boolean cp, boolean level, boolean gender, boolean form, boolean size,
                            boolean despawn, boolean shinyPossibility, boolean shinyConfirmed,
                            boolean pvpRanks, boolean serverSideSpecies, boolean distanceQuery) {
            this.label = label;
            this.coordinates = coordinates;
            this.ivComponents = ivComponents;
            this.ivPercent = ivPercent;
            this.cp = cp;
            this.level = level;
            this.gender = gender;
            this.form = form;
            this.size = size;
            this.despawn = despawn;
            this.shinyPossibility = shinyPossibility;
            this.shinyConfirmed = shinyConfirmed;
            this.pvpRanks = pvpRanks;
            this.serverSideSpecies = serverSideSpecies;
            this.distanceQuery = distanceQuery;
        }

        /** iFlowGo: full spawn stats, server-side pokemon_id, radius query. */
        public static final Capabilities IFLOWGO = new Capabilities("iFlowGo", true, true, true, true,
                true, true, false, false, true, false, false, false, true, true);
        /** RocketMap/PokeSearch gateway: classic raw_data fields; everything else stays unknown. */
        public static final Capabilities ROCKETMAP = new Capabilities("RocketMap/PokeSearch", true,
                true, true, true, true, true, true, false, true, false, false, false, false, true);
        /** PoGoCoordinates: stats via API, coordinates only after a provider-authorised reveal. */
        public static final Capabilities POGOCOORDINATES = new Capabilities("PoGoCoordinates", false,
                true, true, true, true, true, true, true, true, false, true, false, true, false);
        /** PGBR 100% IV feed: hundo rows only, no individual IVs. */
        public static final Capabilities PGBR_HUNDO = new Capabilities("PGBR 100% IV", true, false,
                true, false, true, false, true, false, true, false, false, false, false, false);
        /** PokeList/Moonani: requires one species even for a global request. */
        public static final Capabilities POKELIST = new Capabilities("PokeList", true, true, true,
                true, true, true, true, false, true, false, false, false, true, true);
    /** Licensed Partner API: live status and each field are checked against /v1/capabilities.
     * Shiny is only eligibility and server-side PvP ranks are conditional, never guaranteed. */
    public static final Capabilities POKELIST_PARTNER = new Capabilities(
            "PokeList Partner API", true, true, true, true, true, true, false,
            true, true, true, false, true, true, true);

    }

    /** One normalised observation. Immutable; every optional field uses its own "unknown" marker. */
    public static final class Spawn {
        public final String sourceId, encounterId, pokemonName;
        public final int pokemonId, form, ivAttack, ivDefense, ivStamina, cp, gender, size,
                pvpRank, pvpLeague;
        /** Provider-asserted IV percentage, or {@link #UNKNOWN}. Never derived from components here. */
        public final int ivPercent;
        public final double latitude, longitude, level;
        /** Provider observation time and despawn time in ms; {@link #UNKNOWN_TIME} when absent. */
        public final long observedAt, despawnAt;
        public final Coordinates coordinatesStatus;
        /** Provider reveal code for a row whose coordinates are still blocked; may be null. */
        public final String revealCode;
        /** True only when the provider said shiny for this encounter; never a probability. */
        public final boolean shinyConfirmed;
        /** True when this species can be shiny at all (a species property, not this encounter's). */
        public final boolean shinyPossible;
        public final Capabilities capabilities;
        /** True when PvP rank was computed by PGBR instead of supplied by the provider. */
        public final boolean pvpRankLocal;
        /**
         * The provider entry this row came from, kept so the existing sprite loader
         * ({@link LiveFeedIcons#iconFor}) can draw exactly the asset it draws elsewhere. Never used for
         * filtering or for any value shown as data.
         */
        public final LiveFeedData.Entry live;

        Spawn(String sourceId, String encounterId, int pokemonId, String pokemonName, int form,
              double latitude, double longitude, int ivAttack, int ivDefense, int ivStamina,
              int ivPercent, int cp, double level, int gender, int size, int pvpRank, int pvpLeague,
              boolean pvpRankLocal, long observedAt, long despawnAt, Coordinates coordinatesStatus,
              String revealCode, boolean shinyConfirmed, boolean shinyPossible,
              Capabilities capabilities, LiveFeedData.Entry live) {
            this.sourceId = sourceId;
            this.encounterId = encounterId;
            this.pokemonId = pokemonId;
            this.pokemonName = pokemonName;
            this.form = form;
            this.latitude = latitude;
            this.longitude = longitude;
            this.ivAttack = ivAttack;
            this.ivDefense = ivDefense;
            this.ivStamina = ivStamina;
            this.ivPercent = ivPercent;
            this.cp = cp;
            this.level = level;
            this.gender = gender;
            this.size = size;
            this.pvpRank = pvpRank;
            this.pvpLeague = pvpLeague;
            this.pvpRankLocal = pvpRankLocal;
            this.observedAt = observedAt;
            this.despawnAt = despawnAt;
            this.coordinatesStatus = coordinatesStatus;
            this.revealCode = revealCode;
            this.shinyConfirmed = shinyConfirmed;
            this.shinyPossible = shinyPossible;
            this.capabilities = capabilities;
            this.live = live;
        }

        /** True when the provider proved 15/15/15. A percentage alone is not proof. */
        public boolean perfect() {
            return ivAttack == 15 && ivDefense == 15 && ivStamina == 15;
        }

        /** True only when every individual IV is known, so a "a/d/s" readout is never partial. */
        public boolean hasIvComponents() {
            return ivAttack >= 0 && ivDefense >= 0 && ivStamina >= 0;
        }

        /** True only when the provider itself stated 100%. */
        public boolean providerSaysPerfect() {
            return ivPercent == 100;
        }

        public boolean hasLocation() {
            return coordinatesStatus == Coordinates.AVAILABLE;
        }

        public boolean expired(long now) {
            return despawnAt != UNKNOWN_TIME && despawnAt <= now;
        }

        /** Remaining milliseconds, or {@link #UNKNOWN_TIME} when the provider did not say. */
        public long remainingMs(long now) {
            return despawnAt == UNKNOWN_TIME ? UNKNOWN_TIME : despawnAt - now;
        }

        /**
         * Identity used for de-duplication. Species alone is never enough; two different Pokémon of
         * the same species are distinct spawns.
         */
        public String dedupeKey() {
            if (encounterId != null && !encounterId.isEmpty())
                return sourceId + "|" + pokemonId + "|" + encounterId;
            return sourceId + "|" + pokemonId + "|"
                    + String.format(Locale.US, "%.5f,%.5f", latitude, longitude);
        }

        public double distanceKmFrom(double lat, double lon) {
            if (!hasLocation() || !Double.isFinite(lat) || !Double.isFinite(lon)) return Double.NaN;
            return SpawnContract.distanceKm(lat, lon, latitude, longitude);
        }
    }

    /** Builder used by callers that assemble a spawn from an already-validated provider row. */
    public static final class Builder {
        private String sourceId, encounterId, pokemonName, revealCode;
        private int pokemonId = UNKNOWN, form = UNKNOWN, ivAttack = UNKNOWN, ivDefense = UNKNOWN,
                ivStamina = UNKNOWN, cp = UNKNOWN, gender = UNKNOWN, size = UNKNOWN,
                pvpRank = UNKNOWN_RANK, pvpLeague = UNKNOWN;
        private int ivPercent = UNKNOWN;
        private double latitude = Double.NaN, longitude = Double.NaN;
        private double level = UNKNOWN_LEVEL;
        private long observedAt = UNKNOWN_TIME, despawnAt = UNKNOWN_TIME;
        private Coordinates coordinates = Coordinates.INVALID;
        private boolean shinyConfirmed, shinyPossible;
        private boolean pvpRankLocal;
        private Capabilities capabilities;
        private LiveFeedData.Entry live;

        public Builder(String sourceId, Capabilities capabilities) {
            this.sourceId = sourceId;
            this.capabilities = capabilities;
        }

        public Builder encounter(String id) { this.encounterId = id; return this; }
        public Builder species(int dex, String name) { this.pokemonId = dex; this.pokemonName = name; return this; }
        public Builder form(int value) { this.form = value; return this; }
        public Builder ivs(int atk, int def, int sta) {
            this.ivAttack = atk; this.ivDefense = def; this.ivStamina = sta; return this;
        }
        public Builder ivPercent(int value) { this.ivPercent = value; return this; }
        public Builder cp(int value) { this.cp = value; return this; }
        public Builder level(double value) { this.level = value; return this; }
        public Builder gender(int value) { this.gender = value; return this; }
        public Builder size(int value) { this.size = value; return this; }
        public Builder pvp(int league, int rank, boolean locallyComputed) {
            this.pvpLeague = league; this.pvpRank = rank; this.pvpRankLocal = locallyComputed; return this;
        }
        public Builder times(long observedAt, long despawnAt) {
            this.observedAt = observedAt; this.despawnAt = despawnAt; return this;
        }
        public Builder revealCode(String code) { this.revealCode = code; return this; }
        public Builder shiny(boolean confirmed, boolean possible) {
            this.shinyConfirmed = confirmed; this.shinyPossible = possible; return this;
        }

        /**
         * Validates and stores coordinates. Rejects non-finite values, out-of-range values and the
         * 0,0 "null island" placeholder that several feeds use for "not scanned".
         */
        public Builder location(double lat, double lon) {
            if (validCoordinates(lat, lon)) {
                this.latitude = lat; this.longitude = lon; this.coordinates = Coordinates.AVAILABLE;
            } else {
                this.latitude = Double.NaN; this.longitude = Double.NaN;
                this.coordinates = Coordinates.INVALID;
            }
            return this;
        }

        /** Marks a row whose provider withheld coordinates behind an authorised reveal. */
        public Builder coordinatesBlocked() {
            this.latitude = Double.NaN; this.longitude = Double.NaN;
            this.coordinates = Coordinates.BLOCKED;
            return this;
        }

        /** Species-level shiny availability, used when the provider says nothing per encounter. */
        public Builder shinyPossible(boolean possible) { this.shinyPossible = possible; return this; }

        /** Keeps the originating feed entry for sprite rendering only. */
        public Builder live(LiveFeedData.Entry entry) { this.live = entry; return this; }

        public Spawn build() {
            return new Spawn(sourceId, encounterId, pokemonId, pokemonName == null ? "" : pokemonName,
                    form, latitude, longitude, ivAttack, ivDefense, ivStamina, ivPercent, cp, level,
                    gender, size, pvpRank, pvpLeague, pvpRankLocal, observedAt, despawnAt,
                    coordinates, revealCode, shinyConfirmed, shinyPossible, capabilities, live);
        }
    }

    /** True only for real, usable coordinates; filters out NaN, out-of-range and 0,0. */
    public static boolean validCoordinates(double lat, double lon) {
        if (!Double.isFinite(lat) || !Double.isFinite(lon)) return false;
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return false;
        return !(Math.abs(lat) < 1e-7 && Math.abs(lon) < 1e-7);
    }

    /** Great-circle distance in km (haversine). Returns NaN for invalid input. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (!validCoordinates(lat1, lon1) || !validCoordinates(lat2, lon2)) return Double.NaN;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * De-duplicates a list of rows, keeping the first occurrence of each {@link Spawn#dedupeKey()}.
     * Two Pokémon of the same species at different places or with different encounter ids survive.
     */
    public static List<Spawn> dedupe(List<Spawn> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        Map<String, Spawn> seen = new LinkedHashMap<>();
        for (Spawn row : rows) {
            if (row == null) continue;
            String key = row.dedupeKey();
            if (!seen.containsKey(key)) seen.put(key, row);
        }
        return Collections.unmodifiableList(new ArrayList<>(seen.values()));
    }

    /** Drops expired rows; a row with unknown despawn time is kept (it may still be alive). */
    public static List<Spawn> alive(List<Spawn> rows, long now) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        List<Spawn> out = new ArrayList<>();
        for (Spawn row : rows) if (row != null && !row.expired(now)) out.add(row);
        return Collections.unmodifiableList(out);
    }

    /** How a result list must be ordered. {@code null} values (e.g. distance) sort last. */
    public enum Order { DISTANCE, TIME_LEFT, IV, SPECIES, PVP_RANK }

    /**
     * Stable ordering over normalised rows. Unmeasured values always sort to the end, so a row with
     * unknown distance or unknown IV never displaces a measured one.
     */
    public static List<Spawn> sort(List<Spawn> rows, final Order order, final double originLat,
                                   final double originLon, final long now) {
        if (rows == null || rows.isEmpty() || order == null) return rows;
        List<Spawn> copy = new ArrayList<>(rows);
        Comparator<Spawn> byOrder;
        switch (order) {
            case DISTANCE:
                byOrder = new Comparator<Spawn>() {
                    public int compare(Spawn a, Spawn b) {
                        return compareUnknownLast(a.distanceKmFrom(originLat, originLon),
                                b.distanceKmFrom(originLat, originLon));
                    }
                };
                break;
            case TIME_LEFT:
                byOrder = new Comparator<Spawn>() {
                    public int compare(Spawn a, Spawn b) {
                        long ra = a.remainingMs(now), rb = b.remainingMs(now);
                        if (ra == UNKNOWN_TIME && rb == UNKNOWN_TIME) return 0;
                        if (ra == UNKNOWN_TIME) return 1;
                        if (rb == UNKNOWN_TIME) return -1;
                        return Long.compare(ra, rb);
                    }
                };
                break;
            case IV:
                byOrder = new Comparator<Spawn>() {
                    public int compare(Spawn a, Spawn b) {
                        // Prefer a proven percentage, then proven components; unknown sorts last.
                        return Integer.compare(ivScore(b), ivScore(a));
                    }
                };
                break;
            case PVP_RANK:
                byOrder = new Comparator<Spawn>() {
                    public int compare(Spawn a, Spawn b) {
                        int ra = a.pvpRank <= 0 ? Integer.MAX_VALUE : a.pvpRank;
                        int rb = b.pvpRank <= 0 ? Integer.MAX_VALUE : b.pvpRank;
                        return Integer.compare(ra, rb);
                    }
                };
                break;
            case SPECIES:
            default:
                byOrder = new Comparator<Spawn>() {
                    public int compare(Spawn a, Spawn b) {
                        int byDex = Integer.compare(a.pokemonId, b.pokemonId);
                        if (byDex != 0) return byDex;
                        int byName = clean(a.pokemonName).compareTo(clean(b.pokemonName));
                        if (byName != 0) return byName;
                        return compareUnknownLast(a.distanceKmFrom(originLat, originLon),
                                b.distanceKmFrom(originLat, originLon));
                    }
                };
                break;
        }
        Collections.sort(copy, byOrder);
        return Collections.unmodifiableList(copy);
    }

    /** 0..100 for sorting; unknown IV yields -1 so it always sorts last on a descending compare. */
    static int ivScore(Spawn row) {
        if (row.ivPercent != UNKNOWN) return row.ivPercent;
        if (row.hasIvComponents())
            return (int) Math.round(100.0 * (row.ivAttack + row.ivDefense + row.ivStamina) / 45.0);
        return -1;
    }

    private static int compareUnknownLast(double a, double b) {
        boolean ua = !Double.isFinite(a), ub = !Double.isFinite(b);
        if (ua && ub) return 0;
        if (ua) return 1;
        if (ub) return -1;
        return Double.compare(a, b);
    }

    private static String clean(String value) {
        if (value == null) return "";
        String lower = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return lower.replaceAll("\\p{M}+", "").trim();
    }

    /**
     * Converts an existing {@link LiveFeedData.Entry} (the shape every current provider client
     * already returns) into the shared contract. Unknown values stay unknown; nothing is invented.
     *
     * @param sourceId stable source identifier reported to the user
     * @param capabilities what that source can deliver
     */
    public static Spawn fromLiveFeed(LiveFeedData.Entry entry, String sourceId,
                                     Capabilities capabilities) {
        if (entry == null) return null;
        LiveFeedData.Attrs attrs = entry.attrs == null ? LiveFeedData.Attrs.UNKNOWN : entry.attrs;
        Builder b = new Builder(sourceId, capabilities)
                .encounter(entry.id)
                .species(entry.dexId, entry.name)
                .ivs(attrs.atkIv, attrs.defIv, attrs.staIv)
                .ivPercent(percentFrom(entry.iv))
                .cp(entry.cp)
                .level(entry.level > 0 ? entry.level : UNKNOWN_LEVEL)
                .gender(attrs.gender)
                .size(attrs.size)
                .form(attrs.form)
                .times(UNKNOWN_TIME, entry.expiresAt)
                .live(entry);
        // Every rank the provider sent is kept; the "active" league is only inferred when exactly one
        // rank is present, so an Ultra-only row is never labelled Great League.
        int ranks = 0;
        ranks += attrs.pvpGreatRank > 0 ? 1 : 0;
        ranks += attrs.pvpUltraRank > 0 ? 1 : 0;
        ranks += attrs.pvpLittleRank > 0 ? 1 : 0;
        if (ranks == 1) {
            if (attrs.pvpGreatRank > 0) b.pvp(0, attrs.pvpGreatRank, false);
            else if (attrs.pvpUltraRank > 0) b.pvp(1, attrs.pvpUltraRank, false);
            else b.pvp(2, attrs.pvpLittleRank, false);
        }
        b.location(entry.lat, entry.lon);
        return b.build();
    }

    /** Reconciles the legacy {@code double iv} percentage with the strict {@code int} contract. */
    static int percentFrom(double iv) {
        if (!Double.isFinite(iv) || iv < 0 || iv > 100) return UNKNOWN;
        return (int) Math.round(iv);
    }
}
