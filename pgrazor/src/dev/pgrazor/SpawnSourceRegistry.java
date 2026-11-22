package dev.pgrazor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The catalogue of Pokémon sources the snipers can actually query, with an honest status per source.
 *
 * <p>Only connectors that already exist in PGBR and produce real rows are registered here. Providers
 * that were investigated but cannot be integrated are listed with a {@link Status} explaining why, so
 * the menu never offers a button that pretends to work:
 * <ul>
 *   <li><b>PGSharp / iPogo / Pokémod</b> — proprietary clients. Their feeds are not a public API, and
 *       the analysis in {@code docs} (and the reference packages) only describes their internals. No
 *       endpoint, key or protocol is used.</li>
 *   <li><b>PoGoCoordinates</b> — API exists and is used by {@link PogoFeedDialog}, but coordinates
 *       require the provider's own reveal flow and the per-row cost/limits are the provider's. It is
 *       marked {@link Status#REVEAL_REQUIRED} here instead of being silently treated as free.</li>
 *   <li><b>RocketMap/PokeSearch gateway</b> — needs an operator-authorised HTTPS server; without one
 *       there is nothing to query (no scanner coverage is created by a URL).</li>
 * </ul>
 *
 * <p>Nothing in this class performs I/O.
 */
public final class SpawnSourceRegistry {
    private SpawnSourceRegistry() {}

    public enum Status {
        /** Usable now: the row set arrives with coordinates and the needed attributes. */
        READY,
        /** Usable, but the operator must first configure a URL or a private read key. */
        NEEDS_CONFIGURATION,
        /** Rows arrive without coordinates; the provider's own reveal step is mandatory. */
        REVEAL_REQUIRED,
        /** No public/authorised integration exists; the option is shown disabled with the reason. */
        NOT_INTEGRATED
    }

    /** One catalogue entry. */
    public static final class SourceInfo {
        public final String id, label, detail;
        public final Status status;
        public final SpawnContract.Capabilities capabilities;

        SourceInfo(String id, String label, Status status, String detail,
                   SpawnContract.Capabilities capabilities) {
            this.id = id;
            this.label = label;
            this.status = status;
            this.detail = detail;
            this.capabilities = capabilities;
        }

        public boolean selectable() {
            return status == Status.READY || status == Status.NEEDS_CONFIGURATION;
        }
    }

    /** Stable ids; also used as the {@code source_id} reported on every row. */
    public static final String ID_IFLOWGO = "iflowgo";
    public static final String ID_ROCKETMAP = "rocketmap";
    public static final String ID_POGOCOORDINATES = "pogocoordinates";
    public static final String ID_PGBR_HUNDO = "pgbr-hundo";
    public static final String ID_POKELIST = "pokelist";

    private static final List<SourceInfo> CATALOGUE;

    static {
        List<SourceInfo> list = new ArrayList<>();
        list.add(new SourceInfo(ID_IFLOWGO, "iFlowGo", Status.READY,
                "Coordenadas reais, IV completo (percentual e componentes), nível, PC, gênero e "
                        + "expiração. Aceita pokemon_id no servidor. A busca global lê apenas a "
                        + "primeira página da fonte: não é uma varredura mundial completa.",
                SpawnContract.Capabilities.IFLOWGO));
        list.add(new SourceInfo(ID_ROCKETMAP, "RocketMap / gateway PokeSearch",
                Status.NEEDS_CONFIGURATION,
                "Precisa de um servidor HTTPS autorizado pelo operador. Sem scanner ativo não há "
                        + "cobertura: um endereço sozinho não cria aparições. Campos clássicos do "
                        + "raw_data; nível/gênero só quando o servidor enviar.",
                SpawnContract.Capabilities.ROCKETMAP));
        list.add(new SourceInfo(ID_PGBR_HUNDO, "PGBR 100% IV", Status.READY,
                "Fonte própria do PGBR: apenas linhas declaradas 100% IV, com coordenadas e "
                        + "expiração. Não traz IVs individuais nem nível.",
                SpawnContract.Capabilities.PGBR_HUNDO));
        list.add(new SourceInfo(ID_POGOCOORDINATES, "PoGoCoordinates", Status.REVEAL_REQUIRED,
                "A busca devolve códigos de revelação em vez de coordenadas. Use o painel próprio "
                        + "dessa fonte: a revelação é o fluxo autorizado dela e respeita as cotas "
                        + "do fornecedor.",
                SpawnContract.Capabilities.POGOCOORDINATES));
        list.add(new SourceInfo(ID_POKELIST, "PokeList / Moonani", Status.READY,
                "Para busca global exige uma espécie escolhida. Distância em passos de 1 a 5 km. "
                        + "O fornecedor pode restringir o acesso a qualquer momento.",
                SpawnContract.Capabilities.POKELIST));
        list.add(new SourceInfo("pgsharp", "PGSharp", Status.NOT_INTEGRATED,
                "Cliente proprietário; o feed não é API pública. O PGBR não usa endpoints, chaves "
                        + "nem protocolos dele.", null));
        list.add(new SourceInfo("ipogo", "iPogo", Status.NOT_INTEGRATED,
                "Cliente proprietário; sem contrato de integração autorizado.", null));
        list.add(new SourceInfo("pokemod", "Pokémod", Status.NOT_INTEGRATED,
                "Cliente proprietário; sem contrato de integração autorizado.", null));
        CATALOGUE = Collections.unmodifiableList(list);
    }

    public static List<SourceInfo> all() { return CATALOGUE; }

    /** Selectable sources, in display order. */
    public static List<SourceInfo> selectable() {
        List<SourceInfo> out = new ArrayList<>();
        for (SourceInfo info : CATALOGUE) if (info.selectable()) out.add(info);
        return Collections.unmodifiableList(out);
    }

    public static SourceInfo find(String id) {
        for (SourceInfo info : CATALOGUE) if (info.id.equals(id)) return info;
        return null;
    }

    /** First usable source; the panel's default when nothing was saved yet. */
    public static String defaultSourceId() {
        return ID_IFLOWGO;
    }

    /**
     * Converts whatever a connector returned into the shared contract. Rows keep their own source id
     * so two sources merged in one list can never be confused, and every unknown field stays unknown.
     */
    public static List<SpawnContract.Spawn> normalize(List<LiveFeedData.Entry> entries,
                                                      String sourceId,
                                                      SpawnContract.Capabilities capabilities) {
        if (entries == null) return Collections.emptyList();
        List<SpawnContract.Spawn> out = new ArrayList<>();
        for (LiveFeedData.Entry entry : entries) {
            if (entry == null) continue;
            if (entry.kind != LiveFeedData.Kind.POKEMON && entry.kind != LiveFeedData.Kind.RAID) continue;
            SpawnContract.Spawn spawn = SpawnContract.fromLiveFeed(entry, sourceId, capabilities);
            if (spawn != null) out.add(spawn);
        }
        return out;
    }
}
