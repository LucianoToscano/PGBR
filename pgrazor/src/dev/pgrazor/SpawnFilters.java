package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The full filter set for one Sniper panel, evaluated against the shared {@link SpawnContract}.
 *
 * <p>Everything here is a local filter over what the provider already sent, except the optional
 * single species that a provider can narrow server-side (see {@link #remoteSpecies()}). The rules are
 * strict about missing data on purpose:
 * <ul>
 *   <li>A bound that is set is <em>never</em> satisfied by an unknown value. Setting "level 30-40"
 *       excludes rows whose level the provider did not state, instead of guessing.</li>
 *   <li>"100% IV" requires proven 15/15/15 or a provider-asserted 100%; a bare species or CP never
 *       promotes a row to perfect.</li>
 *   <li>Shiny is availability ("can be shiny"), never a guarantee; {@code shinyConfirmedOnly} only
 *       keeps rows where the provider stated shiny for that encounter.</li>
 * </ul>
 *
 * <p>Immutable and UI-free, so Sniper 1 and Sniper 2 can each hold their own instance and be tested
 * on a JVM without a device.
 */
public final class SpawnFilters {
    public static final int IV_MIN = 0, IV_MAX = 100;
    public static final int COMPONENT_MIN = 0, COMPONENT_MAX = 15;
    public static final int LEVEL_MIN = 1, LEVEL_MAX = 50;
    public static final int CP_MIN = 10, CP_MAX = 10000;
    public static final int MAX_RADIUS_KM = 500;
    public static final int MAX_DISTANCE_KM = 20000;
    public static final int LEAGUE_ANY = -1, LEAGUE_GREAT = 0, LEAGUE_ULTRA = 1, LEAGUE_LITTLE = 2;
    public static final int SIZE_ANY = -1, SIZE_XXS = 1, SIZE_XXL = 2;
    public static final int GENDER_ANY = -1, GENDER_MALE = 1, GENDER_FEMALE = 2, GENDER_GENDERLESS = 3;
    /** Marker used by every optional bound: the filter is not applied. */
    public static final int UNSET = -1;

    private final boolean showPerfectOnly, showZeroOnly, shinyPossibleOnly, shinyConfirmedOnly;
    private final boolean requireCoordinatesOnly, requireKnownIv;
    private final int minIv, maxIv, minAtk, maxAtk, minDef, maxDef, minSta, maxSta;
    private final int minCp, maxCp, minLevel, maxLevel, gender, form, size, league, maxRank;
    private final double maxDistanceKm, radiusKm;
    private final List<Integer> species;
    private final boolean legendaryOnly, mythicalOnly, regionalOnly, rareOnly, eventOnly;

    private SpawnFilters(Builder b) {
        this.showPerfectOnly = b.showPerfectOnly;
        this.showZeroOnly = b.showZeroOnly;
        this.shinyPossibleOnly = b.shinyPossibleOnly;
        this.shinyConfirmedOnly = b.shinyConfirmedOnly;
        this.requireCoordinatesOnly = b.requireCoordinatesOnly;
        this.requireKnownIv = b.requireKnownIv;
        this.minIv = b.minIv; this.maxIv = b.maxIv;
        this.minAtk = b.minAtk; this.maxAtk = b.maxAtk;
        this.minDef = b.minDef; this.maxDef = b.maxDef;
        this.minSta = b.minSta; this.maxSta = b.maxSta;
        this.minCp = b.minCp; this.maxCp = b.maxCp;
        this.minLevel = b.minLevel; this.maxLevel = b.maxLevel;
        this.gender = b.gender; this.form = b.form; this.size = b.size;
        this.league = b.league; this.maxRank = b.maxRank;
        this.maxDistanceKm = b.maxDistanceKm;
        this.radiusKm = b.radiusKm;
        this.species = Collections.unmodifiableList(new ArrayList<>(b.species));
        this.legendaryOnly = b.legendaryOnly;
        this.mythicalOnly = b.mythicalOnly;
        this.regionalOnly = b.regionalOnly;
        this.rareOnly = b.rareOnly;
        this.eventOnly = b.eventOnly;
    }

    /** Broad defaults: nothing is hidden that the user did not ask to hide. */
    public static SpawnFilters defaults() {
        return new Builder().build();
    }

    /** Mutable builder; every setter is validated and returns this builder. */
    public static final class Builder {
        private boolean showPerfectOnly, showZeroOnly, shinyPossibleOnly, shinyConfirmedOnly,
                requireCoordinatesOnly, requireKnownIv;
        private boolean legendaryOnly, mythicalOnly, regionalOnly, rareOnly, eventOnly;
        private int minIv = UNSET, maxIv = UNSET, minAtk = UNSET, maxAtk = UNSET, minDef = UNSET,
                maxDef = UNSET, minSta = UNSET, maxSta = UNSET, minCp = UNSET, maxCp = UNSET,
                minLevel = UNSET, maxLevel = UNSET;
        private int gender = GENDER_ANY, form = UNSET, size = SIZE_ANY, league = LEAGUE_ANY,
                maxRank = UNSET;
        private double maxDistanceKm = UNSET, radiusKm = 25;
        private List<Integer> species = new ArrayList<>();

        public Builder perfectOnly(boolean value) { this.showPerfectOnly = value; return this; }
        public Builder zeroOnly(boolean value) { this.showZeroOnly = value; return this; }
        public Builder requireKnownIv(boolean value) { this.requireKnownIv = value; return this; }
        public Builder shinyPossibleOnly(boolean value) { this.shinyPossibleOnly = value; return this; }
        public Builder shinyConfirmedOnly(boolean value) { this.shinyConfirmedOnly = value; return this; }
        public Builder coordinatesOnly(boolean value) { this.requireCoordinatesOnly = value; return this; }
        public Builder groups(boolean legendary, boolean mythical, boolean regional, boolean rare,
                              boolean event) {
            this.legendaryOnly = legendary; this.mythicalOnly = mythical; this.regionalOnly = regional;
            this.rareOnly = rare; this.eventOnly = event;
            return this;
        }
        public Builder iv(int min, int max) { this.minIv = min; this.maxIv = max; return this; }
        public Builder attack(int min, int max) { this.minAtk = min; this.maxAtk = max; return this; }
        public Builder defense(int min, int max) { this.minDef = min; this.maxDef = max; return this; }
        public Builder stamina(int min, int max) { this.minSta = min; this.maxSta = max; return this; }
        public Builder cp(int min, int max) { this.minCp = min; this.maxCp = max; return this; }
        public Builder level(int min, int max) { this.minLevel = min; this.maxLevel = max; return this; }
        public Builder gender(int value) { this.gender = value; return this; }
        public Builder form(int value) { this.form = value; return this; }
        public Builder size(int value) { this.size = value; return this; }
        public Builder league(int value, int rank) { this.league = value; this.maxRank = rank; return this; }
        public Builder distance(double maxDistanceKm, double radiusKm) {
            this.maxDistanceKm = maxDistanceKm;
            this.radiusKm = radiusKm;
            return this;
        }
        public Builder species(List<Integer> ids) {
            this.species = ids == null ? new ArrayList<Integer>() : new ArrayList<>(ids);
            return this;
        }

        /**
         * Builds the immutable set.
         *
         * @throws IllegalArgumentException on an inverted or out-of-domain range; the editor shows the
         *     message and keeps the dialog open instead of silently accepting bad bounds.
         */
        public SpawnFilters build() {
            check(minIv, maxIv, IV_MIN, IV_MAX, "IV");
            check(minAtk, maxAtk, COMPONENT_MIN, COMPONENT_MAX, "Ataque");
            check(minDef, maxDef, COMPONENT_MIN, COMPONENT_MAX, "Defesa");
            check(minSta, maxSta, COMPONENT_MIN, COMPONENT_MAX, "Stamina");
            check(minCp, maxCp, CP_MIN, CP_MAX, "PC");
            check(minLevel, maxLevel, LEVEL_MIN, LEVEL_MAX, "Nível");
            if (maxRank != UNSET && (maxRank < 1 || maxRank > 4096))
                throw new IllegalArgumentException("Ranking PvP deve ficar entre 1 e 4096.");
            if (league != LEAGUE_ANY && league != LEAGUE_GREAT && league != LEAGUE_ULTRA
                    && league != LEAGUE_LITTLE)
                throw new IllegalArgumentException("Liga PvP desconhecida.");
            if (radiusKm < 1 || radiusKm > MAX_RADIUS_KM)
                throw new IllegalArgumentException("Raio deve ficar entre 1 e " + MAX_RADIUS_KM + " km.");
            if (maxDistanceKm != UNSET && (maxDistanceKm < 1 || maxDistanceKm > MAX_DISTANCE_KM))
                throw new IllegalArgumentException("Distância máxima de teleporte inválida.");
            for (Integer id : species) {
                if (id == null || id < 1 || id > 1025)
                    throw new IllegalArgumentException("Espécie fora da Pokédex: " + id + ".");
            }
            // Refuse to pretend: these groups have no validated definition in this repository.
            if (regionalOnly)
                throw new IllegalArgumentException("Filtro de regionais indisponível: formas "
                        + "regionais não têm lista verificada por espécie neste repositório.");
            if (rareOnly)
                throw new IllegalArgumentException("Filtro de raros indisponível: não há "
                        + "definição verificável de raridade por espécie.");
            if (eventOnly)
                throw new IllegalArgumentException("Filtro de evento indisponível: a "
                        + "disponibilidade em eventos muda por evento e por fonte.");
            return new SpawnFilters(this);
        }

        private static void check(int min, int max, int floor, int ceiling, String label) {
            if (min != UNSET && (min < floor || min > ceiling))
                throw new IllegalArgumentException(label + " mínimo fora de " + floor + ".." + ceiling + ".");
            if (max != UNSET && (max < floor || max > ceiling))
                throw new IllegalArgumentException(label + " máximo fora de " + floor + ".." + ceiling + ".");
            if (min != UNSET && max != UNSET && min > max)
                throw new IllegalArgumentException(label + " mínimo não pode exceder o máximo.");
        }
    }

    /** True when the row may be listed. Every bound is strict about unknown values. */
    public boolean accepts(SpawnContract.Spawn s, long now, double originLat, double originLon) {
        if (s == null) return false;
        if (s.expired(now)) return false;
        if (requireCoordinatesOnly && !s.hasLocation()) return false;

        if (!species.isEmpty() && !species.contains(s.pokemonId)) return false;
        if (legendaryOnly || mythicalOnly || regionalOnly || rareOnly || eventOnly) {
            if (!SpeciesGroups.matchesAny(s.pokemonId, legendaryOnly, mythicalOnly, regionalOnly,
                    rareOnly, eventOnly)) return false;
        }

        if (showPerfectOnly && !s.perfect() && !s.providerSaysPerfect()) return false;
        if (showZeroOnly && !(s.hasIvComponents() && s.ivAttack == 0 && s.ivDefense == 0
                && s.ivStamina == 0)) return false;

        if (minIv != UNSET || maxIv != UNSET) {
            int pct = percentOf(s);
            if (pct == SpawnContract.UNKNOWN) return false;      // unknown never satisfies a bound
            if (minIv != UNSET && pct < minIv) return false;
            if (maxIv != UNSET && pct > maxIv) return false;
        } else if (requireKnownIv && !s.hasIvComponents() && s.ivPercent == SpawnContract.UNKNOWN) {
            return false;
        }
        if (!componentOk(s.ivAttack, minAtk, maxAtk)) return false;
        if (!componentOk(s.ivDefense, minDef, maxDef)) return false;
        if (!componentOk(s.ivStamina, minSta, maxSta)) return false;

        if (minCp != UNSET || maxCp != UNSET) {
            if (s.cp == SpawnContract.UNKNOWN) return false;
            if (minCp != UNSET && s.cp < minCp) return false;
            if (maxCp != UNSET && s.cp > maxCp) return false;
        }
        if (minLevel != UNSET || maxLevel != UNSET) {
            if (!Double.isFinite(s.level)) return false;          // unknown level never satisfies
            if (minLevel != UNSET && s.level < minLevel) return false;
            if (maxLevel != UNSET && s.level > maxLevel) return false;
        }
        if (gender != GENDER_ANY) {
            if (s.gender == SpawnContract.UNKNOWN) return false;
            if (!genderMatches(s.gender)) return false;
        }
        if (form != UNSET) {
            if (s.form == SpawnContract.UNKNOWN) return false;
            if (s.form != form) return false;
        }
        if (size != SIZE_ANY) {
            if (s.size == SpawnContract.UNKNOWN) return false;
            if (size == SIZE_XXS && s.size != 1) return false;
            if (size == SIZE_XXL && s.size != 2) return false;
        }
        if (league != LEAGUE_ANY) {
            if (s.pvpRank <= 0 || s.pvpLeague != league) return false;
            if (maxRank != UNSET && s.pvpRank > maxRank) return false;
        }
        if (shinyPossibleOnly && !s.shinyPossible) return false;
        if (shinyConfirmedOnly && !s.shinyConfirmed) return false;

        if (maxDistanceKm != UNSET) {
            double km = s.distanceKmFrom(originLat, originLon);
            if (!Double.isFinite(km)) return false;               // no location, no distance bound
            if (km > maxDistanceKm) return false;
        }
        return true;
    }

    private boolean componentOk(int value, int min, int max) {
        if (min == UNSET && max == UNSET) return true;
        if (value == SpawnContract.UNKNOWN) return false;
        if (min != UNSET && value < min) return false;
        if (max != UNSET && value > max) return false;
        return true;
    }

    private boolean genderMatches(int rowGender) {
        if (gender == GENDER_MALE) return rowGender == 1;
        if (gender == GENDER_FEMALE) return rowGender == 2;
        if (gender == GENDER_GENDERLESS) return rowGender == 3;
        return true;
    }

    /** Percentage from the contract: provider percentage first, then proven components. */
    static int percentOf(SpawnContract.Spawn s) {
        if (s.ivPercent != SpawnContract.UNKNOWN) return s.ivPercent;
        if (s.hasIvComponents())
            return (int) Math.round(100.0 * (s.ivAttack + s.ivDefense + s.ivStamina) / 45.0);
        return SpawnContract.UNKNOWN;
    }

    /**
     * The single species this panel may send to a provider as a server-side filter, or -1.
     * Multi-species selection is never collapsed into a fabricated single id.
     */
    public int remoteSpecies() {
        return species.size() == 1 ? species.get(0) : -1;
    }

    /** True when any bound needs a value the provider might not send, so the UI can warn. */
    public boolean needsOptionalFields() {
        return minIv != UNSET || maxIv != UNSET || minCp != UNSET || maxCp != UNSET
                || minLevel != UNSET || maxLevel != UNSET || gender != GENDER_ANY
                || form != UNSET || size != SIZE_ANY || league != LEAGUE_ANY
                || minAtk != UNSET || maxAtk != UNSET || minDef != UNSET || maxDef != UNSET
                || minSta != UNSET || maxSta != UNSET || shinyConfirmedOnly;
    }

    /** One-line summary for the panel header. */
    public String summary() {
        List<String> parts = new ArrayList<>();
        if (!species.isEmpty()) parts.add(species.size() == 1 ? "1 espécie" : species.size() + " espécies");
        if (showPerfectOnly) parts.add("100% IV");
        if (showZeroOnly) parts.add("0% IV");
        if (minIv != UNSET || maxIv != UNSET) parts.add("IV " + bound(minIv, maxIv));
        if (minCp != UNSET || maxCp != UNSET) parts.add("PC " + bound(minCp, maxCp));
        if (minLevel != UNSET || maxLevel != UNSET) parts.add("Nv " + bound(minLevel, maxLevel));
        if (minAtk != UNSET || minDef != UNSET || minSta != UNSET) parts.add("IVs individuais");
        if (gender != GENDER_ANY) parts.add("gênero");
        if (form != UNSET) parts.add("forma " + form);
        if (size != SIZE_ANY) parts.add(size == SIZE_XXS ? "XXS" : "XXL");
        if (league != LEAGUE_ANY) parts.add("PvP " + leagueName(league)
                + (maxRank != UNSET ? " top " + maxRank : ""));
        if (shinyPossibleOnly) parts.add("shiny possível");
        if (shinyConfirmedOnly) parts.add("shiny confirmado");
        if (legendaryOnly) parts.add("lendários");
        if (mythicalOnly) parts.add("míticos");
        if (regionalOnly) parts.add("regionais");
        if (rareOnly) parts.add("raros");
        if (eventOnly) parts.add("evento");
        if (maxDistanceKm != UNSET) parts.add("até " + (int) maxDistanceKm + " km");
        return parts.isEmpty() ? "sem filtros" : join(parts);
    }

    private static String bound(int min, int max) {
        if (min != UNSET && max != UNSET) return min + "–" + max;
        if (min != UNSET) return "≥ " + min;
        return "≤ " + max;
    }

    public static String leagueName(int league) {
        if (league == LEAGUE_GREAT) return "Grande Liga";
        if (league == LEAGUE_ULTRA) return "Ultra Liga";
        if (league == LEAGUE_LITTLE) return "Pequena Copa";
        return "qualquer";
    }

    private static String join(List<String> parts) {
        StringBuilder s = new StringBuilder();
        for (String part : parts) {
            if (s.length() > 0) s.append(" · ");
            s.append(part);
        }
        return s.toString();
    }

    // ------------------------------------------------------------------ persistence

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            JSONArray ids = new JSONArray();
            for (Integer id : species) ids.put(id.intValue());
            o.put("species", ids);
            o.put("perfect_only", showPerfectOnly);
            o.put("zero_only", showZeroOnly);
            o.put("require_known_iv", requireKnownIv);
            o.put("coordinates_only", requireCoordinatesOnly);
            o.put("shiny_possible_only", shinyPossibleOnly);
            o.put("shiny_confirmed_only", shinyConfirmedOnly);
            put(o, "min_iv", minIv); put(o, "max_iv", maxIv);
            put(o, "min_attack", minAtk); put(o, "max_attack", maxAtk);
            put(o, "min_defense", minDef); put(o, "max_defense", maxDef);
            put(o, "min_stamina", minSta); put(o, "max_stamina", maxSta);
            put(o, "min_cp", minCp); put(o, "max_cp", maxCp);
            put(o, "min_level", minLevel); put(o, "max_level", maxLevel);
            o.put("gender", gender); o.put("form", form); o.put("size", size);
            o.put("league", league); put(o, "max_rank", maxRank);
            o.put("max_distance_km", maxDistanceKm);
            o.put("radius_km", radiusKm);
            o.put("legendary", legendaryOnly); o.put("mythical", mythicalOnly);
            o.put("regional", regionalOnly); o.put("rare", rareOnly); o.put("event", eventOnly);
            return o;
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void put(JSONObject o, String key, int value) throws JSONException {
        if (value != UNSET) o.put(key, value);
    }

    /** Rebuilds a set from stored JSON; unknown keys are ignored and validation still applies. */
    public static SpawnFilters fromJson(JSONObject o) throws JSONException {
        if (o == null) return defaults();
        Builder b = new Builder();
        b.species(intList(o.optJSONArray("species")));
        b.perfectOnly(o.optBoolean("perfect_only", false));
        b.zeroOnly(o.optBoolean("zero_only", false));
        b.requireKnownIv(o.optBoolean("require_known_iv", false));
        b.coordinatesOnly(o.optBoolean("coordinates_only", false));
        b.shinyPossibleOnly(o.optBoolean("shiny_possible_only", false));
        b.shinyConfirmedOnly(o.optBoolean("shiny_confirmed_only", false));
        b.groups(o.optBoolean("legendary", false), o.optBoolean("mythical", false),
                o.optBoolean("regional", false), o.optBoolean("rare", false),
                o.optBoolean("event", false));
        b.iv(o.optInt("min_iv", UNSET), o.optInt("max_iv", UNSET));
        b.attack(o.optInt("min_attack", UNSET), o.optInt("max_attack", UNSET));
        b.defense(o.optInt("min_defense", UNSET), o.optInt("max_defense", UNSET));
        b.stamina(o.optInt("min_stamina", UNSET), o.optInt("max_stamina", UNSET));
        b.cp(o.optInt("min_cp", UNSET), o.optInt("max_cp", UNSET));
        b.level(o.optInt("min_level", UNSET), o.optInt("max_level", UNSET));
        b.gender(o.optInt("gender", GENDER_ANY));
        b.form(o.optInt("form", UNSET));
        b.size(o.optInt("size", SIZE_ANY));
        b.league(o.optInt("league", LEAGUE_ANY), o.optInt("max_rank", UNSET));
        b.distance(o.optDouble("max_distance_km", UNSET),
                o.optDouble("radius_km", 25));
        try {
            return b.build();
        } catch (IllegalArgumentException invalid) {
            throw new JSONException(invalid.getMessage());
        }
    }

    private static List<Integer> intList(JSONArray array) {
        List<Integer> out = new ArrayList<>();
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) {
            int value = array.optInt(i, -1);
            if (value >= 1 && value <= 1025 && !out.contains(value)) out.add(value);
        }
        return out;
    }

    // accessors used by the panel and tests
    public List<Integer> species() { return species; }
    public boolean perfectOnly() { return showPerfectOnly; }
    public boolean zeroOnly() { return showZeroOnly; }
    public boolean shinyConfirmedOnly() { return shinyConfirmedOnly; }
    public boolean shinyPossibleOnly() { return shinyPossibleOnly; }
    public boolean requireKnownIv() { return requireKnownIv; }
    public boolean coordinatesOnly() { return requireCoordinatesOnly; }
    public boolean legendaryOnly() { return legendaryOnly; }
    public boolean mythicalOnly() { return mythicalOnly; }
    public int minAttack() { return minAtk; }
    public int maxAttack() { return maxAtk; }
    public int minDefense() { return minDef; }
    public int maxDefense() { return maxDef; }
    public int minStamina() { return minSta; }
    public int maxStamina() { return maxSta; }
    public int minIv() { return minIv; }
    public int maxIv() { return maxIv; }
    public int minCp() { return minCp; }
    public int maxCp() { return maxCp; }
    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public int gender() { return gender; }
    public int form() { return form; }
    public int size() { return size; }
    public int league() { return league; }
    public int maxRank() { return maxRank; }
    public double maxDistanceKm() { return maxDistanceKm; }
    public double radiusKm() { return radiusKm; }
}
