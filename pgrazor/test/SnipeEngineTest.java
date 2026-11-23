import dev.pgrazor.LiveFeedClient;
import dev.pgrazor.LiveFeedData;
import dev.pgrazor.SpawnContract;
import dev.pgrazor.SpawnFilters;
import dev.pgrazor.SpawnProfiles;
import dev.pgrazor.SpawnSearch;
import dev.pgrazor.SpawnSourceRegistry;
import dev.pgrazor.SpeciesGroups;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contract tests for the sniper engine: normalisation, honest unknowns, filtering, ordering,
 * de-duplication, query lifecycle and per-slot independence.
 *
 * <p>Every Pokémon below is <strong>test data</strong> written by hand for these assertions. None of it
 * was obtained from a provider, and none of it proves that any source works.
 */
public final class SnipeEngineTest {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        try {
            unknownsStayUnknown();
            hundredPercentNeedsProof();
            zeroIsNotUnknown();
            invalidCoordinatesRejected();
            expiryAndDedupe();
            orderingPutsUnknownLast();
            ivAndComponentFilters();
            cpLevelGenderFormFilters();
            pvpAndShinyFilters();
            distanceAndCoordinatesFilters();
            engineStates();
            engineFailures();
            profilesAreIndependent();
            groupsAndGuards();
            speciesSetParsing();
            nameAndDexSearch();
        } catch (Throwable failure) {
            failures.add("exceção inesperada: " + failure);
            failure.printStackTrace();
        }
        System.out.println("SnipeEngineTest: " + checks + " verificações, " + failures.size() + " falha(s)");
        for (String failure : failures) System.out.println("  FALHA " + failure);
        if (!failures.isEmpty()) System.exit(1);
    }

    // ------------------------------------------------------------------ fixtures (test data only)

    /** Monotonic fixture seed so two test rows never share an encounter id by accident. */
    private static int seed;

    private static LiveFeedData.Entry entry(int dex, double lat, double lon, int atk, int def, int sta,
                                            int cp, double level, int gender, int form, long despawnInMs) {
        long now = System.currentTimeMillis();
        LiveFeedData.Attrs attrs = new LiveFeedData.Attrs(form, -1, gender, -1, atk, def, sta, -1, -1, -1);
        double ivPercent = atk < 0 || def < 0 || sta < 0 ? -1
                : 100.0 * (atk + def + sta) / 45.0;
        return new LiveFeedData.Entry("enc-" + dex + "-" + (++seed),
                LiveFeedData.Kind.POKEMON, "TESTE #" + dex, "", lat, lon, dex, cp, level, ivPercent,
                now + despawnInMs, true, 0, 0L, false, false, attrs);
    }

    private static SpawnContract.Spawn spawn(int dex, double lat, double lon, int atk, int def, int sta,
                                             int cp, double level, int gender, int form,
                                             long despawnInMs) {
        return SpawnContract.fromLiveFeed(entry(dex, lat, lon, atk, def, sta, cp, level, gender, form,
                despawnInMs), SpawnSourceRegistry.ID_IFLOWGO, SpawnContract.Capabilities.IFLOWGO);
    }

    /** 0,0 = Natal/RN reference point used only as an origin for distance assertions. */
    private static final double ORIGIN_LAT = -5.7945, ORIGIN_LON = -35.2110;

    // ------------------------------------------------------------------ assertions

    private static void unknownsStayUnknown() {
        LiveFeedData.Entry bare = new LiveFeedData.Entry("x1", LiveFeedData.Kind.POKEMON, "#25", "",
                ORIGIN_LAT, ORIGIN_LON, 25, -1, -1, -1, System.currentTimeMillis() + 60_000, false);
        SpawnContract.Spawn s = SpawnContract.fromLiveFeed(bare, SpawnSourceRegistry.ID_IFLOWGO,
                SpawnContract.Capabilities.IFLOWGO);
        check("entrada sem stats gera linha", s != null);
        check("IV ausente continua desconhecido", s.ivPercent == SpawnContract.UNKNOWN);
        check("componentes ausentes continuam desconhecidos", !s.hasIvComponents());
        check("PC ausente não vira 0", s.cp == SpawnContract.UNKNOWN);
        check("nível ausente não vira 0", Double.isNaN(s.level));
        check("nível ausente não passa em filtro de nível",
                !new SpawnFilters.Builder().level(1, 50).build().accepts(s, now(), ORIGIN_LAT, ORIGIN_LON));
        check("gênero ausente não passa em filtro de gênero",
                !new SpawnFilters.Builder().gender(SpawnFilters.GENDER_MALE).build()
                        .accepts(s, now(), ORIGIN_LAT, ORIGIN_LON));
        check("sem filtro a linha aparece", SpawnFilters.defaults().accepts(s, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void hundredPercentNeedsProof() {
        SpawnContract.Spawn proven = spawn(149, ORIGIN_LAT, ORIGIN_LON, 15, 15, 15, 3000, 30, 1, 0, 120_000);
        check("15/15/15 é 100%", proven.perfect() && proven.providerSaysPerfect());

        // A provider that merely sends the percentage (no components) is still not proof of 15/15/15.
        LiveFeedData.Entry percentOnly = new LiveFeedData.Entry("p1", LiveFeedData.Kind.POKEMON, "#6", "",
                ORIGIN_LAT, ORIGIN_LON, 6, 2000, 25, 100.0, System.currentTimeMillis() + 90_000, true);
        SpawnContract.Spawn claimed = SpawnContract.fromLiveFeed(percentOnly, "teste",
                SpawnContract.Capabilities.IFLOWGO);
        check("percentual 100 da fonte é preservado", claimed.ivPercent == 100);
        check("percentual 100 não prova 15/15/15", !claimed.perfect());
        check("linha com apenas percentual entra em 100% porque a fonte afirmou",
                new SpawnFilters.Builder().perfectOnly(true).build()
                        .accepts(claimed, now(), ORIGIN_LAT, ORIGIN_LON));

        SpawnContract.Spawn strongCp = spawn(150, ORIGIN_LAT, ORIGIN_LON, 12, 13, 14, 4000, 40, 1, 0, 90_000);
        check("PC alto não vira 100%", !strongCp.perfect() && !strongCp.providerSaysPerfect());
        check("PC alto não passa em 100%", !new SpawnFilters.Builder().perfectOnly(true).build()
                .accepts(strongCp, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void zeroIsNotUnknown() {
        SpawnContract.Spawn zero = spawn(1, ORIGIN_LAT, ORIGIN_LON, 0, 0, 0, 10, 1, 2, 0, 120_000);
        check("0/0/0 legítimo é conhecido", zero.hasIvComponents() && zero.ivPercent == 0);
        check("filtro 0% aceita 0/0/0", new SpawnFilters.Builder().zeroOnly(true).build()
                .accepts(zero, now(), ORIGIN_LAT, ORIGIN_LON));
        check("filtro 0% recusa 15/15/15", !new SpawnFilters.Builder().zeroOnly(true).build()
                .accepts(spawn(1, ORIGIN_LAT, ORIGIN_LON, 15, 15, 15, 10, 1, 1, 0, 120_000),
                        now(), ORIGIN_LAT, ORIGIN_LON));
        check("PC 10 real não é tratado como ausente", zero.cp == 10);
    }

    private static void invalidCoordinatesRejected() {
        check("0,0 recusado", !SpawnContract.validCoordinates(0, 0));
        check("NaN recusado", !SpawnContract.validCoordinates(Double.NaN, -35.2));
        check("latitude fora de faixa recusada", !SpawnContract.validCoordinates(-91, 0.5));
        check("longitude fora de faixa recusada", !SpawnContract.validCoordinates(5, 181));
        check("coordenada real aceita", SpawnContract.validCoordinates(ORIGIN_LAT, ORIGIN_LON));

        LiveFeedData.Entry nullIsland = new LiveFeedData.Entry("bad", LiveFeedData.Kind.POKEMON, "#25",
                "", 0, 0, 25, 500, 20, 50, System.currentTimeMillis() + 60_000, true);
        SpawnContract.Spawn s = SpawnContract.fromLiveFeed(nullIsland, "teste",
                SpawnContract.Capabilities.IFLOWGO);
        check("linha em 0,0 fica sem localização", !s.hasLocation());
        check("linha em 0,0 tem status INVALID", s.coordinatesStatus == SpawnContract.Coordinates.INVALID);
        check("linha em 0,0 não tem distância", Double.isNaN(s.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON)));
        check("filtro de coordenadas válidas remove 0,0",
                !new SpawnFilters.Builder().coordinatesOnly(true).build()
                        .accepts(s, now(), ORIGIN_LAT, ORIGIN_LON));
        check("filtro de distância remove linha sem local",
                !new SpawnFilters.Builder().distance(100, 25).build()
                        .accepts(s, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void expiryAndDedupe() {
        long now = now();
        SpawnContract.Spawn expired = spawn(25, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, -1_000);
        check("linha vencida é detectada", expired.expired(now));
        check("filtro padrão descarta vencida", !SpawnFilters.defaults().accepts(expired, now, ORIGIN_LAT, ORIGIN_LON));
        check("alive remove vencidas", SpawnContract.alive(Collections.singletonList(expired), now).isEmpty());

        SpawnContract.Spawn noClock = new LiveFeedDataEntryStub().build();
        check("despawn desconhecido não é tratado como vencido", !noClock.expired(now));

        SpawnContract.Spawn a = spawn(25, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        SpawnContract.Spawn b = spawn(25, ORIGIN_LAT + 0.01, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        check("duas aparições da mesma espécie sobrevivem", SpawnContract.dedupe(Arrays.asList(a, b)).size() == 2);
        check("mesma aparição (mesmo id) é deduplicada",
                SpawnContract.dedupe(Arrays.asList(a, a)).size() == 1);

        LiveFeedData.Entry one = entry(25, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        LiveFeedData.Entry two = new LiveFeedData.Entry("enc-25-same-point", LiveFeedData.Kind.POKEMON,
                "#25", "", ORIGIN_LAT, ORIGIN_LON, 25, 500, 20, 66.7,
                System.currentTimeMillis() + 60_000, true);
        List<SpawnContract.Spawn> rows = new ArrayList<>();
        rows.add(SpawnContract.fromLiveFeed(one, "teste", SpawnContract.Capabilities.IFLOWGO));
        rows.add(SpawnContract.fromLiveFeed(two, "teste", SpawnContract.Capabilities.IFLOWGO));
        check("ids de encontro diferentes com mesmo ponto não colapsam", SpawnContract.dedupe(rows).size() == 2);
    }

    /** Tiny helper: a row with no despawn time at all, to prove unknown time is not "expired". */
    private static final class LiveFeedDataEntryStub {
        SpawnContract.Spawn build() {
            return new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                    .species(25, "Pikachu").location(ORIGIN_LAT, ORIGIN_LON).build();
        }
    }

    private static void orderingPutsUnknownLast() {
        long now = now();
        SpawnContract.Spawn near = spawn(1, ORIGIN_LAT + 0.01, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        SpawnContract.Spawn far = spawn(2, ORIGIN_LAT + 1.0, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        SpawnContract.Spawn noWhere = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(3, "Teste").build();
        List<SpawnContract.Spawn> sorted = SpawnContract.sort(Arrays.asList(noWhere, far, near),
                SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("ordem por distância põe o mais perto primeiro", sorted.get(0) == near && sorted.get(1) == far);
        check("linha sem local vai para o fim", sorted.get(2) == noWhere);

        SpawnContract.Spawn perfect = spawn(4, ORIGIN_LAT, ORIGIN_LON, 15, 15, 15, 900, 25, 1, 0, 60_000);
        SpawnContract.Spawn unknownIv = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(5, "Teste").location(ORIGIN_LAT, ORIGIN_LON)
                .times(0, now + 60_000).build();
        List<SpawnContract.Spawn> byIv = SpawnContract.sort(Arrays.asList(unknownIv, perfect),
                SpawnContract.Order.IV, ORIGIN_LAT, ORIGIN_LON, now);
        check("ordem por IV põe o comprovado antes do desconhecido", byIv.get(0) == perfect);

        SpawnContract.Spawn quick = spawn(6, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 30_000);
        List<SpawnContract.Spawn> byTime = SpawnContract.sort(Arrays.asList(perfect, quick),
                SpawnContract.Order.TIME_LEFT, ORIGIN_LAT, ORIGIN_LON, now);
        check("ordem por tempo restante põe o mais urgente primeiro", byTime.get(0) == quick);

        List<SpawnContract.Spawn> bySpecies = SpawnContract.sort(Arrays.asList(far, near),
                SpawnContract.Order.SPECIES, ORIGIN_LAT, ORIGIN_LON, now);
        check("ordem por espécie segue a Pokédex", bySpecies.get(0).pokemonId == 1);
    }

    private static void ivAndComponentFilters() {
        SpawnContract.Spawn row = spawn(25, ORIGIN_LAT, ORIGIN_LON, 14, 15, 15, 800, 25, 1, 0, 120_000);
        check("IV 98 passa em mínimo 95", new SpawnFilters.Builder().iv(95, SpawnFilters.UNSET).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("IV 98 não passa em 100", !new SpawnFilters.Builder().iv(100, SpawnFilters.UNSET).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("IV 98 não passa em máximo 90", !new SpawnFilters.Builder().iv(SpawnFilters.UNSET, 90).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("ataque 14 não passa em ataque 15", !new SpawnFilters.Builder().attack(15, 15).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("defesa 15 passa em defesa 15", new SpawnFilters.Builder().defense(15, 15).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("stamina 15 passa em stamina 15", new SpawnFilters.Builder().stamina(15, 15).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("faixa invertida é recusada", throwsOn(() -> new SpawnFilters.Builder().iv(90, 80).build()));
        check("IV 101 é recusado", throwsOn(() -> new SpawnFilters.Builder().iv(0, 101).build()));
        check("componente 16 é recusado", throwsOn(() -> new SpawnFilters.Builder().attack(0, 16).build()));

        SpawnContract.Spawn unknown = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(25, "Pikachu").location(ORIGIN_LAT, ORIGIN_LON)
                .times(0, now() + 60_000).build();
        check("IV desconhecido não passa em faixa 0-100",
                !new SpawnFilters.Builder().iv(0, 100).build().accepts(unknown, now(), ORIGIN_LAT, ORIGIN_LON));
        check("requireKnownIv remove desconhecido", !new SpawnFilters.Builder().requireKnownIv(true).build()
                .accepts(unknown, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void cpLevelGenderFormFilters() {
        SpawnContract.Spawn row = spawn(143, ORIGIN_LAT, ORIGIN_LON, 10, 12, 13, 1900, 28.5, 2, 7, 120_000);
        check("PC 1900 passa em 1500-2000", new SpawnFilters.Builder().cp(1500, 2000).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("PC 1900 não passa em 2000-3000", !new SpawnFilters.Builder().cp(2000, 3000).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("nível 28.5 passa em 25-30", new SpawnFilters.Builder().level(25, 30).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("nível 28.5 não passa em 30-40", !new SpawnFilters.Builder().level(30, 40).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("gênero feminino aceito", new SpawnFilters.Builder().gender(SpawnFilters.GENDER_FEMALE).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("gênero masculino recusado", !new SpawnFilters.Builder().gender(SpawnFilters.GENDER_MALE).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("forma 7 aceita", new SpawnFilters.Builder().form(7).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("forma 3 recusada", !new SpawnFilters.Builder().form(3).build()
                .accepts(row, now(), ORIGIN_LAT, ORIGIN_LON));
        check("forma desconhecida não passa em forma exigida",
                !new SpawnFilters.Builder().form(1).build().accepts(
                        spawn(143, ORIGIN_LAT, ORIGIN_LON, 10, 12, 13, 1900, 28.5, 2, -1, 120_000),
                        now(), ORIGIN_LAT, ORIGIN_LON));
        check("nível 51 recusado", throwsOn(() -> new SpawnFilters.Builder().level(1, 51).build()));

        SpawnContract.Spawn xxs = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(19, "Rattata").location(ORIGIN_LAT, ORIGIN_LON).size(1)
                .times(0, now() + 60_000).build();
        check("XXS aceito", new SpawnFilters.Builder().size(SpawnFilters.SIZE_XXS).build()
                .accepts(xxs, now(), ORIGIN_LAT, ORIGIN_LON));
        check("XXL recusa XXS", !new SpawnFilters.Builder().size(SpawnFilters.SIZE_XXL).build()
                .accepts(xxs, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void pvpAndShinyFilters() {
        SpawnContract.Spawn great = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(9, "Blastoise").location(ORIGIN_LAT, ORIGIN_LON)
                .pvp(SpawnFilters.LEAGUE_GREAT, 42, false).times(0, now() + 60_000).build();
        check("rank 42 passa em top 100", new SpawnFilters.Builder().league(SpawnFilters.LEAGUE_GREAT, 100)
                .build().accepts(great, now(), ORIGIN_LAT, ORIGIN_LON));
        check("rank 42 não passa em top 20", !new SpawnFilters.Builder().league(SpawnFilters.LEAGUE_GREAT, 20)
                .build().accepts(great, now(), ORIGIN_LAT, ORIGIN_LON));
        check("líder de Grande Liga não passa em Ultra",
                !new SpawnFilters.Builder().league(SpawnFilters.LEAGUE_ULTRA, 100).build()
                        .accepts(great, now(), ORIGIN_LAT, ORIGIN_LON));
        check("linha sem rank não passa em filtro PvP",
                !new SpawnFilters.Builder().league(SpawnFilters.LEAGUE_GREAT, 100).build()
                        .accepts(spawn(9, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 1500, 25, 1, 0, 60_000),
                                now(), ORIGIN_LAT, ORIGIN_LON));
        check("rank local é marcado como calculado",
                new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                        .species(9, "Blastoise").location(ORIGIN_LAT, ORIGIN_LON)
                        .pvp(0, 5, true).build().pvpRankLocal);
        check("rank do fornecedor não é marcado como local", !great.pvpRankLocal);
        check("rank 0 recusado", throwsOn(() -> new SpawnFilters.Builder()
                .league(SpawnFilters.LEAGUE_GREAT, 0).build()));

        SpawnContract.Spawn confirmed = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(25, "Pikachu").location(ORIGIN_LAT, ORIGIN_LON)
                .shiny(true, true).times(0, now() + 60_000).build();
        SpawnContract.Spawn possible = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(25, "Pikachu").location(ORIGIN_LAT, ORIGIN_LON)
                .shiny(false, true).times(0, now() + 60_000).build();
        check("shiny confirmado passa em confirmado", new SpawnFilters.Builder().shinyConfirmedOnly(true)
                .build().accepts(confirmed, now(), ORIGIN_LAT, ORIGIN_LON));
        check("shiny possível não é confirmado", !new SpawnFilters.Builder().shinyConfirmedOnly(true)
                .build().accepts(possible, now(), ORIGIN_LAT, ORIGIN_LON));
        check("shiny possível passa em possível", new SpawnFilters.Builder().shinyPossibleOnly(true)
                .build().accepts(possible, now(), ORIGIN_LAT, ORIGIN_LON));
    }

    private static void distanceAndCoordinatesFilters() {
        // ~1.1 km north of the origin.
        SpawnContract.Spawn close = spawn(1, ORIGIN_LAT + 0.01, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        // ~111 km north.
        SpawnContract.Spawn far = spawn(2, ORIGIN_LAT + 1.0, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        check("distância do ponto próximo < 2 km", close.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON) < 2);
        check("distância do ponto distante ~111 km",
                Math.abs(far.distanceKmFrom(ORIGIN_LAT, ORIGIN_LON) - 111.2) < 1.0);
        check("limite de 5 km aceita o próximo", new SpawnFilters.Builder().distance(5, 25).build()
                .accepts(close, now(), ORIGIN_LAT, ORIGIN_LON));
        check("limite de 5 km recusa o distante", !new SpawnFilters.Builder().distance(5, 25).build()
                .accepts(far, now(), ORIGIN_LAT, ORIGIN_LON));
        check("limite de teleporte abaixo de 1 km recusado", throwsOn(() -> new SpawnFilters.Builder()
                .distance(0.5, 25).build()));
        check("marcador UNSET em distância significa sem limite", !throwsOn(() -> new SpawnFilters.Builder()
                .distance(SpawnFilters.UNSET, 25).build()));
        check("raio acima do máximo recusado", throwsOn(() -> new SpawnFilters.Builder()
                .distance(10, SpawnFilters.MAX_RADIUS_KM + 1).build()));
    }

    // ------------------------------------------------------------------ engine

    /** A source that returns fixed rows and reports its coverage. */
    private static final class FixtureSource implements SpawnSearch.Source {
        private final List<SpawnContract.Spawn> rows;
        private final Exception failure;
        private final SpawnSearch.Coverage coverage;
        private int fetches;
        private int cancels;

        FixtureSource(List<SpawnContract.Spawn> rows, Exception failure, SpawnSearch.Coverage coverage) {
            this.rows = rows;
            this.failure = failure;
            this.coverage = coverage;
        }

        @Override public String id() { return "fixture"; }
        @Override public SpawnContract.Capabilities capabilities() { return SpawnContract.Capabilities.IFLOWGO; }
        @Override public List<SpawnContract.Spawn> fetch(SpawnSearch.Request request) throws Exception {
            fetches++;
            if (failure != null) throw failure;
            return rows;
        }
        @Override public SpawnSearch.Coverage coverage() { return coverage; }
        @Override public void cancel() { cancels++; }
    }

    private static SpawnSearch.Request request(boolean global) {
        return new SpawnSearch.Request("fixture", -1, global, ORIGIN_LAT, ORIGIN_LON, 25, false);
    }

    private static void engineStates() {
        long now = now();
        List<SpawnContract.Spawn> rows = Arrays.asList(
                spawn(25, ORIGIN_LAT, ORIGIN_LON, 15, 15, 15, 800, 25, 1, 0, 120_000),
                spawn(6, ORIGIN_LAT + 0.02, ORIGIN_LON, 10, 10, 10, 2000, 30, 1, 0, 120_000));

        SpawnSearch.Result ok = SpawnSearch.run(new FixtureSource(rows, null, SpawnSearch.Coverage.FIRST_PAGE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("estado OK com resultados", ok.state == SpawnSearch.State.OK && ok.count() == 2);
        check("contagem de recebidos correta", ok.received == 2);
        check("nada filtrado sem filtro", ok.droppedByFilter == 0);
        check("cobertura de primeira página sinalizada", ok.partialCoverage()
                && ok.coverage == SpawnSearch.Coverage.FIRST_PAGE);
        check("mensagem de OK nega explicitamente a busca mundial completa",
                ok.message.contains("não é uma busca mundial completa")
                        && ok.message.contains("primeira página"));

        // Cobertura parcial por paginação: nunca dizer "2 de 2" quando a fonte informa mais páginas.
        SpawnSearch.Result partial = SpawnSearch.run(
                new FixtureSource(rows, null, SpawnSearch.Coverage.PARTIAL_PAGES),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("páginas parciais marcam cobertura parcial", partial.partialCoverage());
        check("mensagem de páginas parciais avisa que há mais resultados",
                partial.message.contains("parcial") && partial.message.contains("mais"));
        check("páginas parciais não afirmam cobertura completa",
                !partial.message.contains("todos os registros enviados"));
        // Refiltragem local: mudar IV/CP/PvP NÃO pode gerar nova requisição nem perder linhas.
        SpawnSearch.Result initial = SpawnSearch.run(
                new FixtureSource(rows, null, SpawnSearch.Coverage.FULL_RESPONSE),
                request(false), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("consulta inicial guarda as linhas recebidas", initial.receivedRows.size() == 2);
        SpawnFilters onlyPerfect = new SpawnFilters.Builder().iv(100, 100).build();
        SpawnSearch.Result refiltered = SpawnSearch.refilter(initial,
                (spawn, at) -> onlyPerfect.accepts(spawn, at, ORIGIN_LAT, ORIGIN_LON),
                SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("refiltro mantém o total recebido", refiltered.received == initial.received);
        check("refiltro aplica o filtro novo", refiltered.count() == 1);
        check("refiltro informa que não houve nova requisição",
                refiltered.message.contains("sem nova requisição"));
        // Refiltrar de novo (3ª vez) não pode acumular perdas: parte sempre do que a fonte mandou.
        SpawnSearch.Result again = SpawnSearch.refilter(refiltered, null,
                SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("refiltro sucessivo não perde linhas", again.received == initial.received);
        check("remover o filtro traz as linhas de volta", again.count() == 2);
        check("refiltro sem consulta anterior falha explicitamente",
                SpawnSearch.refilter(null, null, SpawnContract.Order.DISTANCE,
                        ORIGIN_LAT, ORIGIN_LON, now).state == SpawnSearch.State.INVALID_REQUEST);

        SpawnSearch.Result full = SpawnSearch.run(
                new FixtureSource(rows, null, SpawnSearch.Coverage.FULL_RESPONSE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("cobertura completa não é marcada como parcial", !full.partialCoverage());
        check("cobertura completa descreve todos os registros",
                full.message.contains("todos os registros"));

        SpawnSearch.Result empty = SpawnSearch.run(
                new FixtureSource(Collections.<SpawnContract.Spawn>emptyList(), null,
                        SpawnSearch.Coverage.FIRST_PAGE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("fonte vazia é SOURCE_EMPTY, não ALL_FILTERED_OUT", empty.state == SpawnSearch.State.SOURCE_EMPTY);
        check("fonte vazia não diz ausência mundial", empty.message.contains("não significa"));

        SpawnFilters impossible = new SpawnFilters.Builder().cp(9999, 10000).build();
        SpawnSearch.Result filtered = SpawnSearch.run(
                new FixtureSource(rows, null, SpawnSearch.Coverage.AREA), request(false),
                (spawn, at) -> impossible.accepts(spawn, at, ORIGIN_LAT, ORIGIN_LON),
                SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("tudo filtrado é ALL_FILTERED_OUT", filtered.state == SpawnSearch.State.ALL_FILTERED_OUT);
        check("tudo filtrado mantém a contagem de recebidos", filtered.received == 2 && filtered.count() == 0);
        check("mensagem de filtro cita os filtros", filtered.message.contains("filtros"));

        SpawnContract.Spawn expired = spawn(25, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, -5_000);
        SpawnSearch.Result allExpired = SpawnSearch.run(
                new FixtureSource(Collections.singletonList(expired), null, SpawnSearch.Coverage.AREA),
                request(false), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("linha vencida não aparece", allExpired.count() == 0 && allExpired.expired == 1);
        check("linha vencida é ALL_FILTERED_OUT", allExpired.state == SpawnSearch.State.ALL_FILTERED_OUT);

        List<SpawnContract.Spawn> duplicates = Arrays.asList(rows.get(0), rows.get(0));
        SpawnSearch.Result deduped = SpawnSearch.run(
                new FixtureSource(duplicates, null, SpawnSearch.Coverage.AREA), request(false), null,
                SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("duplicata contabilizada", deduped.duplicates == 1 && deduped.count() == 1);

        SpawnContract.Spawn blocked = new SpawnContract.Builder("pogo", SpawnContract.Capabilities.POGOCOORDINATES)
                .species(150, "Mewtwo").coordinatesBlocked().times(0, now + 60_000)
                .revealCode("abc").build();
        SpawnSearch.Result blockedResult = SpawnSearch.run(
                new FixtureSource(Collections.singletonList(blocked), null, SpawnSearch.Coverage.FULL_RESPONSE),
                new SpawnSearch.Request("pogo", -1, true, ORIGIN_LAT, ORIGIN_LON, 25, false),
                (spawn, at) -> spawn.hasLocation(), SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("coordenada bloqueada não é listada", blockedResult.count() == 0);
        check("coordenada bloqueada tem estado próprio",
                blockedResult.state == SpawnSearch.State.ALL_FILTERED_OUT);

        SpawnSearch.Result invalid = SpawnSearch.run(null, null, null, null, 0, 0, now);
        check("consulta nula é INVALID_REQUEST", invalid.state == SpawnSearch.State.INVALID_REQUEST);
    }

    private static void engineFailures() {
        long now = now();
        SpawnSearch.Result denied = SpawnSearch.run(new FixtureSource(null,
                        new LiveFeedClient.FeedException("recusado", 403, 0), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("403 é ACCESS_DENIED", denied.state == SpawnSearch.State.ACCESS_DENIED);
        check("403 não sugere contornar", denied.message.contains("não contorna"));

        SpawnSearch.Result unauth = SpawnSearch.run(new FixtureSource(null,
                        new LiveFeedClient.FeedException("login necessário", 401, 0), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("401 é ACCESS_DENIED", unauth.state == SpawnSearch.State.ACCESS_DENIED);

        SpawnSearch.Result limited = SpawnSearch.run(new FixtureSource(null,
                        new LiveFeedClient.FeedException("limite", 429, 120_000), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("429 é RATE_LIMITED", limited.state == SpawnSearch.State.RATE_LIMITED);
        check("429 preserva Retry-After", limited.retryAfterMs == 120_000);

        SpawnSearch.Result server = SpawnSearch.run(new FixtureSource(null,
                        new LiveFeedClient.FeedException("falhou", 503, 0), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("503 é SOURCE_ERROR", server.state == SpawnSearch.State.SOURCE_ERROR);

        SpawnSearch.Result offline = SpawnSearch.run(new FixtureSource(null,
                        new java.net.UnknownHostException("sem dns"), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("falha de rede é NETWORK_ERROR", offline.state == SpawnSearch.State.NETWORK_ERROR);

        SpawnSearch.Result cancelled = SpawnSearch.run(new FixtureSource(null,
                        new LiveFeedClient.FeedException("Busca iFlowGo cancelada."), SpawnSearch.Coverage.NONE),
                request(true), null, SpawnContract.Order.DISTANCE, ORIGIN_LAT, ORIGIN_LON, now);
        check("cancelamento não é erro", cancelled.state == SpawnSearch.State.CANCELLED);
    }

    private static void profilesAreIndependent() {
        SpawnProfiles one = SpawnProfiles.defaults(SpawnProfiles.SLOT_ONE);
        SpawnProfiles two = SpawnProfiles.defaults(SpawnProfiles.SLOT_TWO);
        check("Sniper 1 começa em 100%", one.filters().perfectOnly());
        check("Sniper 2 começa sem filtro", !two.filters().perfectOnly());

        SpawnProfiles edited = one.withFilters(new SpawnFilters.Builder().cp(500, 1500).build())
                .withSource(SpawnSourceRegistry.ID_POKELIST).withOrder(SpawnContract.Order.IV)
                .withAutoRefresh(120).saveProfile("PvP Great", new SpawnFilters.Builder()
                        .league(SpawnFilters.LEAGUE_GREAT, 100).build());
        check("perfil salvo aparece", edited.profileNames().contains("PvP Great"));
        check("filtro ativo guardado", edited.filters().minCp() == 500);
        check("fonte guardada", edited.sourceId().equals(SpawnSourceRegistry.ID_POKELIST));
        check("ordem guardada", edited.order() == SpawnContract.Order.IV);
        check("auto-refresh guardado", edited.autoRefreshSeconds() == 120);

        SpawnProfiles restored = SpawnProfiles.fromJson(SpawnProfiles.SLOT_ONE,
                edited.toJson());
        check("perfil sobrevive ao JSON", restored.profileNames().contains("PvP Great"));
        check("aplicar perfil muda o filtro ativo",
                restored.applyProfile("PvP Great").filters().league() == SpawnFilters.LEAGUE_GREAT);
        check("limpar filtros remove filtros ativos", restored.clearFilters().filters().minCp() == SpawnFilters.UNSET);
        check("limpar filtros preserva perfis", restored.clearFilters().profileNames().contains("PvP Great"));
        check("perfil excluído some", restored.deleteProfile("PvP Great").profileNames().isEmpty());

        check("slot 2 não herda o filtro do slot 1",
                !SpawnProfiles.defaults(SpawnProfiles.SLOT_TWO).filters().perfectOnly());
        check("chaves de slot diferentes",
                !SpawnProfiles.SLOT_ONE.equals(SpawnProfiles.SLOT_TWO));

        SpawnProfiles bad = SpawnProfiles.fromJson(SpawnProfiles.SLOT_TWO, null);
        check("JSON nulo cai no padrão do slot 2", !bad.filters().perfectOnly());
        check("auto-refresh fora das opções vira desligado",
                SpawnProfiles.fromJson(SpawnProfiles.SLOT_ONE, edited.toJson())
                        .withAutoRefresh(37).autoRefreshSeconds() == SpawnProfiles.AUTO_OFF);
        check("nome vazio não cria perfil", edited.saveProfile("   ", SpawnFilters.defaults()) == edited);
    }

    private static void groupsAndGuards() {
        check("Mewtwo é lendário", SpeciesGroups.legendary(150));
        check("Mew é mítico", SpeciesGroups.mythical(151));
        check("Pikachu não é lendário", !SpeciesGroups.legendary(25));
        check("Rayquaza é lendário", SpeciesGroups.legendary(384));
        check("Zacian é lendário", SpeciesGroups.legendary(888));
        check("grupo lendário aceita lendário",
                SpeciesGroups.matchesAny(150, true, false, false, false, false));
        check("grupo lendário recusa comum",
                !SpeciesGroups.matchesAny(25, true, false, false, false, false));
        check("grupo mítico aceita mítico",
                SpeciesGroups.matchesAny(151, false, true, false, false, false));

        check("regional recusado honestamente", throwsOn(() -> new SpawnFilters.Builder()
                .groups(false, false, true, false, false).build()));
        check("raro recusado honestamente", throwsOn(() -> new SpawnFilters.Builder()
                .groups(false, false, false, true, false).build()));
        check("evento recusado honestamente", throwsOn(() -> new SpawnFilters.Builder()
                .groups(false, false, false, false, true).build()));
        check("lendário permitido", !throwsOn(() -> new SpawnFilters.Builder()
                .groups(true, false, false, false, false).build()));

        check("fonte desconhecida não é selecionável", !SpawnSourceRegistry.find("pgsharp").selectable());
        check("fonte desconhecida explica o motivo",
                SpawnSourceRegistry.find("pgsharp").detail.contains("não é API pública"));
        check("PoGoCoordinates exige revelação",
                SpawnSourceRegistry.find(SpawnSourceRegistry.ID_POGOCOORDINATES).status
                        == SpawnSourceRegistry.Status.REVEAL_REQUIRED);
        check("iFlowGo é utilizável",
                SpawnSourceRegistry.find(SpawnSourceRegistry.ID_IFLOWGO).status
                        == SpawnSourceRegistry.Status.READY);
        check("catálogo não oferece PoGoCoordinates como pronta",
                !SpawnSourceRegistry.selectable().contains(
                        SpawnSourceRegistry.find(SpawnSourceRegistry.ID_POGOCOORDINATES)));
        check("fonte inválida cai no padrão",
                SpawnProfiles.defaults(SpawnProfiles.SLOT_ONE).withSource("nao-existe")
                        .sourceId().equals(SpawnSourceRegistry.defaultSourceId()));
    }

    private static void speciesSetParsing() {
        SpawnSearch.SpeciesSet set = SpawnSearch.SpeciesSet.parse("25, #6;150 texto 1026 0");
        check("parse ignora texto e fora de faixa", set.size() == 3 && set.contains(25)
                && set.contains(6) && set.contains(150));
        check("parse não inventa espécie única", set.single() == -1);
        check("conjunto com uma espécie devolve essa espécie",
                SpawnSearch.SpeciesSet.parse("25").single() == 25);
        check("conjunto vazio significa todas", SpawnSearch.SpeciesSet.parse("").isEmpty());
        check("espécie única vira filtro remoto",
                new SpawnFilters.Builder().species(Arrays.asList(25)).build().remoteSpecies() == 25);
        check("várias espécies não viram filtro remoto",
                new SpawnFilters.Builder().species(Arrays.asList(25, 26)).build().remoteSpecies() == -1);
    }

    /**
     * Name/dex matching used by the panel is {@code PokemonFeedSearch} (package-private, exercised by
     * {@code PokemonFeedSearchTest}). Here only the contract-level view is asserted: a row keeps the
     * dex and the display name, and a name is never invented from the id.
     */
    private static void nameAndDexSearch() {
        SpawnContract.Spawn named = spawn(25, ORIGIN_LAT, ORIGIN_LON, 10, 10, 10, 500, 20, 1, 0, 60_000);
        check("nome da fonte é preservado quando existe", named.pokemonName.contains("25"));
        check("dex é preservado", named.pokemonId == 25);
        SpawnContract.Spawn unnamed = new SpawnContract.Builder("teste", SpawnContract.Capabilities.IFLOWGO)
                .species(6, null).location(ORIGIN_LAT, ORIGIN_LON).build();
        check("nome ausente não vira texto inventado", unnamed.pokemonName.isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private static long now() { return System.currentTimeMillis(); }

    private interface RunnableThatThrows { void run() throws Exception; }

    private static boolean throwsOn(RunnableThatThrows action) {
        try {
            action.run();
            return false;
        } catch (Exception expected) {
            return true;
        }
    }

    private static void check(String label, boolean condition) {
        checks++;
        if (!condition) failures.add(label);
    }
}
