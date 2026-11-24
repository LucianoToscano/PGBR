package dev.pgrazor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The exactly-five integrations plus the two existing PGBR feeds. This is the single place that
 * declares what each origin can really do; adapters read it instead of inventing capabilities.
 *
 * <p>Nothing here claims a live connection. Every remote source starts {@code UNVERIFIED}: the
 * provider must answer a real request before an adapter promotes it to {@code CONNECTED}, and a
 * refused request (401/403/429) leaves it {@code BLOCKED} with a message — never a bypass.
 *
 * <p>Pokémod and PGTools are deliberately absent: they are historical references, not integrations.
 */
public final class SourceRegistry {
    private SourceRegistry() {}

    public static final String POKELIST = "pokelist";
    public static final String IFLOWGO = "iflowgo";
    public static final String POGO_COORDINATES = "pogocoordinates";
    public static final String PGSHARP = "pgsharp";
    public static final String IPOGO = "ipogo";
    public static final String PGBR_HUNDO = "pgbr_hundo";
    public static final String LIVE_POKEMAP = "livepokemap";

    private static final Map<String, SourceCapabilities> CAPABILITIES = new LinkedHashMap<>();
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(POKELIST, "PokeList / Moonani");
        LABELS.put(IFLOWGO, "iFlowGo");
        LABELS.put(POGO_COORDINATES, "PoGoCoordinates");
        LABELS.put(PGSHARP, "PGSharp (dados próprios)");
        LABELS.put(IPOGO, "iPogo (dados próprios)");
        LABELS.put(PGBR_HUNDO, "Feed 100% IV do PGBR");
        LABELS.put(LIVE_POKEMAP, "LivePokeMap");

        CAPABILITIES.put(POKELIST, SourceCapabilities.builder(POKELIST)
                .status(SourceCapabilities.Status.UNVERIFIED)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .remoteSearch(true).pagination(false).liveFeed(false).webhook(false)
                .coverage("Consulta por espécie + área; a fonte pode recusar (401/403/429).")
                .attribution("https://moonani.com/PokeList/")
                .note("Contrato observado no APK 1.3.18; servidor pode recusar. O PGBR não contorna a recusa.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.FORM, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.SIZE, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.PVP, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.RADIUS, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.KIND, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                .build());

        CAPABILITIES.put(IFLOWGO, SourceCapabilities.builder(IFLOWGO)
                .status(SourceCapabilities.Status.UNVERIFIED)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .remoteSearch(true).pagination(true).liveFeed(false).webhook(false)
                .coverage("Nearby por raio limitado; busca global por espécie com página declarada.")
                .attribution("https://pokecoords.iflowgo.com")
                .note("25 itens por página não são cobertura mundial; o PGBR mostra página e has_more.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.RADIUS, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.KIND, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.PAGINATION, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                .build());

        CAPABILITIES.put(POGO_COORDINATES, SourceCapabilities.builder(POGO_COORDINATES)
                .status(SourceCapabilities.Status.UNVERIFIED)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .remoteSearch(true).pagination(false).liveFeed(false).webhook(false)
                .precision(SourceEvent.CoordinatePrecision.UNAVAILABLE)
                .coverage("Resultados podem vir com reveal_code em vez de latitude/longitude.")
                .attribution("https://pogocoordinates.com")
                .note("reveal_code NÃO é coordenada. O PGBR não revela, não enumera e não teleporte sem lat/lon liberados.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.FORM, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.SIZE, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.WEATHER, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.PVP, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.RADIUS, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.KIND, SourceCapabilities.Support.REMOTE)
                .build());

        CAPABILITIES.put(PGSHARP, SourceCapabilities.builder(PGSHARP)
                .status(SourceCapabilities.Status.LOCAL_ONLY)
                .originKind(OriginKind.PERSONAL_IMPORT)
                .remoteSearch(false).pagination(false).liveFeed(false).webhook(false)
                .coverage("Somente arquivos convertidos pelo próprio usuário neste aparelho.")
                .attribution("https://www.pgsharp.com/")
                .note("O conector remoto do PGSharp não é verificado: sem API pública documentada. "
                        + "Favoritos, rotas e filtros do arquivo do usuário funcionam localmente.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.SIZE, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.WEATHER, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.PVP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                .build());

        CAPABILITIES.put(IPOGO, SourceCapabilities.builder(IPOGO)
                .status(SourceCapabilities.Status.LOCAL_ONLY)
                .originKind(OriginKind.PERSONAL_IMPORT)
                .remoteSearch(false).pagination(false).liveFeed(false).webhook(false)
                .coverage("Somente exportações pessoais (JSON/CSV/GPX) ou catálogos estáticos.")
                .attribution("https://ipogo.app/features.php")
                .note("O APK/iOS do iPogo não expõe API pública de coordenadas: nenhuma chamada remota é feita.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.GENDER, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.SIZE, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.WEATHER, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.PVP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                .build());

        CAPABILITIES.put(PGBR_HUNDO, SourceCapabilities.builder(PGBR_HUNDO)
                .status(SourceCapabilities.Status.UNVERIFIED)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .remoteSearch(true).pagination(false).liveFeed(true).webhook(false)
                .coverage("Feed coletado pelo projeto (100% IV), atualizado a cada intervalo declarado.")
                .attribution("https://github.com/LucianoToscano/PGBR (tools/hundo-feed)")
                .note("A fonte declara 100% IV; o PGBR não recalcula o encontro nem confirma o spawn.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_ADS, SourceCapabilities.Support.UNSUPPORTED)
                .supports(SourceCapabilities.CP, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.LOCAL)
                .supports(SourceCapabilities.TTL, SourceCapabilities.Support.LOCAL)
                .build());

        CAPABILITIES.put(LIVE_POKEMAP, SourceCapabilities.builder(LIVE_POKEMAP)
                .status(SourceCapabilities.Status.UNVERIFIED)
                .originKind(OriginKind.LIVE_EXTERNAL)
                .remoteSearch(true).pagination(false).liveFeed(false).webhook(false)
                .coverage("Viewport do mapa; login próprio do LivePokeMap (não é credencial de outra fonte).")
                .attribution("https://play.livepokemap.com")
                .note("Serviço externo com login; pode recusar. O PGBR mantém o cadastro separado por conta.")
                .supports(SourceCapabilities.SPECIES, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.IV_PERCENT, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.LEVEL, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.PVP, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.RADIUS, SourceCapabilities.Support.REMOTE)
                .supports(SourceCapabilities.KIND, SourceCapabilities.Support.REMOTE)
                .build());
    }

    public static Map<String, SourceCapabilities> all() {
        return Collections.unmodifiableMap(CAPABILITIES);
    }

    public static SourceCapabilities of(String sourceId) {
        return CAPABILITIES.get(sourceId);
    }

    /** Display label, or the raw id for anything unknown. */
    public static String label(String sourceId) {
        if (sourceId == null) return "";
        String label = LABELS.get(sourceId);
        return label == null ? sourceId : label;
    }

    public static boolean isKnown(String sourceId) { return CAPABILITIES.containsKey(sourceId); }
}
