package dev.pgrazor;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local import: JSON/CSV/GPX/coordinates, duplicates, per-row rejection reasons, GPX export. */
public final class LocalEventImportTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final long NOW = 1_800_000_000_000L;

    public static void main(String[] args) throws Exception {
        // --- canonical JSON -------------------------------------------------------------
        String canonical = "{\"format\":\"PGBR events\",\"version\":1,\"events\":["
                + "{\"source_id\":\"import\",\"event_id\":\"e1\",\"kind\":\"pokemon\",\"origin_kind\":\"personal_import\","
                + "\"latitude\":-23.55,\"longitude\":-46.63,\"pokemon_id\":25,\"iv_percent\":100.0,\"cp\":500,\"level\":20},"
                + "{\"source_id\":\"import\",\"event_id\":\"e1\",\"kind\":\"pokemon\",\"latitude\":-23.55,\"longitude\":-46.63}"
                + ",{\"latitude\":0,\"longitude\":0}]}";
        LocalEventImport.Report json = LocalEventImport.parse(canonical, NOW);
        check(json.imported == 1, "Um evento válido, foi " + json.imported);
        check(json.duplicates == 1, "Duplicata recusada: " + json.duplicates);
        check(json.rejected == 1, "0,0 rejeitado: " + json.rejected);
        check(json.rejections.get(0).reason.contains("coordenadas"), "Motivo da rejeição: "
                + json.rejections.get(0).reason);
        check(json.events.get(0).originKind == OriginKind.PERSONAL_IMPORT,
                "Importação pessoal não vira feed ao vivo");
        check(json.events.get(0).dataQuality == SourceEvent.QUALITY_IMPORTED, "Qualidade importada");

        check(LocalEventImport.parse("\uFEFF" + canonical, NOW).imported == 1,
                "BOM UTF-8 no início não impede importação");
        check(LocalEventImport.parse(canonical.replace("\"version\":1", "\"version\":99"), NOW).imported == 0,
                "Versão futura do formato canônico não é interpretada como v1");
        String oversized = new String(new char[LocalEventImport.MAX_BYTES / 2]).replace('\0', '\u20AC');
        check(LocalEventImport.parse(oversized, NOW).rejections.get(0).reason.contains("2 MB"),
                "Limite de bytes considera caracteres UTF-8 multibyte");
        String scopedNames = "<gpx><wpt lat='1' lon='2'><name>Meu ponto</name></wpt>"
                + "<trk><name>Nome da trilha</name><trkseg><trkpt lat='3' lon='4'/></trkseg></trk></gpx>";
        LocalEventImport.Report scoped = LocalEventImport.parse(scopedNames, NOW);
        check(scoped.imported == 2 && "Meu ponto".equals(scoped.events.get(0).name),
                "Nome da trilha não sobrescreve nome do waypoint anterior");
        check("Ponto 2".equals(scoped.events.get(1).name), "Ponto sem nome mantém fallback");

        // --- PokeList-like JSON with aliases --------------------------------------------
        String pokeList = "{\"type\":\"pokemon\",\"pokemons\":[{\"id\":\"s1\",\"number\":25,\"name\":\"Pikachu\","
                + "\"latitude\":-23.550520,\"longitude\":-46.633310,\"cp\":500,\"level\":25,\"iv\":100,"
                + "\"attack\":15,\"defense\":15,\"hp\":15,\"endTime\":" + ((NOW + 120_000) / 1000L) + "},"
                + "{\"id\":\"s2\",\"number\":25,\"latitude\":95.0,\"longitude\":10.0}]}";
        LocalEventImport.Report aliased = LocalEventImport.parse(pokeList, NOW);
        check(aliased.imported == 1, "Alias resolvido: " + aliased.imported);
        check(aliased.rejected == 1, "Coordenada inválida rejeitada");
        SourceEvent row = aliased.events.get(0);
        check(row.pokemonId == 25 && row.ivPercent == 100.0 && row.ivAtk == 15,
                "Campos do PokeList mapeados");
        check(row.expiresAt == NOW + 120_000, "endTime em segundos convertido para ms");
        check(aliased.summary().contains("Importados: 1"), "Resumo em PT-BR: " + aliased.summary());

        // --- expired rows are rejected, not imported -------------------------------------
        String expired = "{\"events\":[{\"latitude\":-23.55,\"longitude\":-46.63,\"pokemon_id\":25,"
                + "\"expires_at\":" + (NOW - 5000) + "}]}";
        LocalEventImport.Report expiredReport = LocalEventImport.parse(expired, NOW);
        check(expiredReport.imported == 1, "Linha expirada é importada, mas fica expirada");
        check(expiredReport.events.get(0).expired(NOW), "Registro expirado não vira alvo");
        check(!expiredReport.events.get(0).isActionable(NOW), "Registro expirado não é acionável");

        // --- CSV --------------------------------------------------------------------------
        String csv = "name,lat,lon,pokemon_id,cp,level,iv,attack,defense,hp,expires_at\n"
                + "Pikachu,-23.550520,-46.633310,25,500,25,100,15,15,15," + (NOW + 300_000) + "\n"
                + "Invalid,0,0,25,10,1,0,0,0,0,\n"
                + "NoCoords,,,26,10,1,0,0,0,0,\n"
                + "Pikachu,-23.550520,-46.633310,25,500,25,100,15,15,15," + (NOW + 300_000) + "\n";
        LocalEventImport.Report csvReport = LocalEventImport.parse(csv, NOW);
        check(csvReport.format == LocalEventImport.Format.CSV, "Formato CSV detectado");
        check(csvReport.imported == 1, "Uma linha CSV válida: " + csvReport.imported);
        check(csvReport.duplicates == 1, "Linha CSV duplicada recusada");
        check(csvReport.rejected == 2, "Duas linhas CSV inválidas: " + csvReport.rejected);
        check(csvReport.events.get(0).name.equals("Pikachu"), "Nome do CSV preservado");

        // --- GPX (waypoints with names) ---------------------------------------------------
        String gpx = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<gpx version=\"1.1\" creator=\"test\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n"
                + "<wpt lat=\"-23.550520\" lon=\"-46.633310\"><name>Pokestop A</name></wpt>\n"
                + "<wpt lat=\"0\" lon=\"0\"><name>Legenda</name></wpt>\n"
                + "<trkpt lat=\"-23.560000\" lon=\"-46.640000\"><name>Trilha</name></trkpt>\n"
                + "</gpx>";
        LocalEventImport.Report gpxReport = LocalEventImport.parse(gpx, NOW);
        check(gpxReport.format == LocalEventImport.Format.GPX, "Formato GPX detectado");
        check(gpxReport.imported == 2, "Dois pontos GPX válidos: " + gpxReport.imported);
        check(gpxReport.rejected == 1, "Ponto 0,0 do GPX rejeitado");
        check(gpxReport.events.get(0).kind == SourceEvent.Kind.ROUTE, "Ponto GPX não é spawn");
        check(gpxReport.events.get(0).name.equals("Pokestop A"), "Nome do waypoint preservado");

        // GPX is a real document, not any XML carrying a wpt-like element.
        check(LocalEventImport.parse("<html><wpt lat='1' lon='2'/></html>", NOW).imported == 0,
                "Non-GPX root rejected");
        check(LocalEventImport.parse("<gpx xmlns='urn:other'><wpt lat='1' lon='2'/></gpx>", NOW).imported == 0,
                "Unknown GPX namespace rejected");
        LocalEventImport.Report prefixed = LocalEventImport.parse(
                "<g:gpx xmlns:g='http://www.topografix.com/GPX/1/1'><g:wpt lat='1' lon='2'><g:name>Nome</g:name></g:wpt></g:gpx>", NOW);
        check(prefixed.imported == 1 && "Nome".equals(prefixed.events.get(0).name), "Prefixed GPX supported");
        LocalEventImport.Report extensions = LocalEventImport.parse(
                "<gpx><extensions><wpt lat='1' lon='2'/></extensions><wpt lat='3' lon='4'/></gpx>", NOW);
        check(extensions.imported == 1 && extensions.events.get(0).lat == 3.0, "Nested extension is not a GPX point");
        check(LocalEventImport.parse("<!DOCTYPE gpx><gpx><wpt lat='1' lon='2'/></gpx>", NOW).imported == 0,
                "DTD rejected");
        StringBuilder crowded = new StringBuilder("<gpx>");
        for (int i = 0; i <= LocalEventImport.MAX_ROWS; i++) crowded.append("<wpt lat='1' lon='2'/>");
        LocalEventImport.Report capped = LocalEventImport.parse(crowded.append("</gpx>").toString(), NOW);
        check(capped.imported == LocalEventImport.MAX_ROWS && capped.note.contains("primeiros"),
                "Point cap is reported rather than hidden");
        String deep = "<gpx>" + "<x>".repeat(130) + "</x>".repeat(130) + "</gpx>";
        check(!LocalEventImport.parse(deep, NOW).rejections.isEmpty(), "Deep structure rejected");

        // --- typed / clipboard coordinates -----------------------------------------------
        LocalEventImport.Report coords = LocalEventImport.parse(
                "-23.550520, -46.633310\n-23.560000,-46.640000\n0,0\nabc,def\n-23.550520, -46.633310\n", NOW);
        check(coords.format == LocalEventImport.Format.COORDINATES, "Coordenadas detectadas");
        check(coords.imported == 2, "Duas coordenadas válidas: " + coords.imported);
        check(coords.duplicates == 1, "Coordenada duplicada recusada");
        check(coords.rejected == 2, "0,0 e texto rejeitados: " + coords.rejected);
        check(coords.events.get(0).hasCoordinates(), "Coordenada importada tem lat/lon");

        // --- unknown / empty input --------------------------------------------------------
        check(LocalEventImport.parse("", NOW).imported == 0, "Entrada vazia não importa nada");
        check(LocalEventImport.parse("isto não é um formato", NOW).imported == 0, "Texto solto é rejeitado");
        LocalEventImport.Report malformed = LocalEventImport.parse("{\"events\":[{\"latitude\":\"x\"}]}", NOW);
        check(malformed.imported == 0, "JSON inválido não importa");
        check(!malformed.rejections.isEmpty(), "JSON inválido explica o motivo");

        // --- the PGBR 100% IV feed shape --------------------------------------------------
        JSONObject hundoRoot = new JSONObject().put("v", 1).put("fetched_ms", NOW)
                .put("hundos", new JSONArray()
                        .put(new JSONArray().put("123").put(25).put(0).put(-23.55).put(-46.63)
                                .put(NOW + 300_000).put(500).put(20))
                        .put(new JSONArray().put("124").put(26).put(0).put(91.0).put(10.0)
                                .put(NOW + 300_000).put(100).put(5)));
        LocalEventImport.Report hundo = LocalEventImport.parse(hundoRoot.toString(), NOW);
        check(hundo.imported == 1, "Linha do feed 100% IV inválida rejeitada: " + hundo.imported);
        check(hundo.events.get(0).ivPercent == 100.0, "Feed declara 100% IV");

        check(hundo.events.get(0).expiresAt == NOW + 300_000, "TTL em milissegundos preservado");
        LocalEventImport.Report precision = LocalEventImport.parse(new JSONObject().put("events", new JSONArray()
                .put(new JSONObject().put("id", "approx").put("lat", -23.5).put("lon", -46.6).put("coordinate_precision", "approximate"))
                .put(new JSONObject().put("id", "demo").put("lat", -23.5).put("lon", -46.6).put("origin_kind", "demo_fixture"))).toString(), NOW);
        check(precision.imported == 1 && precision.rejected == 1 && !precision.events.get(0).isActionable(NOW),
                "Importação preserva precisão e recusa demo");

        // --- GPX export only writes valid points ------------------------------------------
        List<SourceEvent> toExport = java.util.Arrays.asList(
                SourceEvent.builder("import", "ok", SourceEvent.Kind.ROUTE).coordinates(-23.55, -46.63).name("A").build(),
                SourceEvent.builder("import", "nocoord", SourceEvent.Kind.ROUTE).build(),
                SourceEvent.builder("import", "bad", SourceEvent.Kind.ROUTE).coordinates(95.0, 10.0).build());
        String exported = LocalEventImport.toGpx(toExport);
        check(exported.contains("lat=\"-23.550000\""), "GPX exportado contém o ponto válido");
        check(!exported.contains("nocoord") && !exported.contains("95.0"),
                "GPX exportado não contém pontos inválidos");
        check(exported.startsWith("<?xml") && exported.contains("<gpx"), "GPX exportado é um documento GPX");

        // --- the store dedupes and prunes ---------------------------------------------------
        LocalEventStore store = new LocalEventStore();
        check(store.addAll(coords.events) == 2, "Store adiciona os registros novos");
        check(store.addAll(coords.events) == 0, "Segunda importação é idempotente");
        check(store.size() == 2, "Store guarda 2 registros");
        String saved = store.toJson();
        LocalEventStore reopened = new LocalEventStore();
        check(reopened.mergeJson(saved, NOW) == 2, "Store recarrega do JSON");
        check(reopened.size() == 2, "Store recarregada com 2 registros");
        check(fails(() -> reopened.mergeJson("{\"format\":\"outro\"}", NOW)),
                "Formato desconhecido é recusado");
        JSONObject damaged = new JSONObject(saved);
        damaged.getJSONArray("events").put(new JSONObject().put("source_id", "import"))
                .put(SourceEvent.builder("import", "after", SourceEvent.Kind.ROUTE).coordinates(-23.5, -46.6).build().toJson());
        LocalEventStore recovered = new LocalEventStore();
        check(recovered.mergeJson(damaged.toString(), NOW) == 3, "Registro inválido não impede recuperação posterior");
        check(!recovered.add(SourceEvent.builder("import", "", SourceEvent.Kind.ROUTE).build()), "Identidade vazia recusada");
        check(fails(() -> recovered.mergeJson(new JSONObject(saved).put("version", 99).toString(), NOW)),
                "Versão futura não é interpretada como v1");
        SourceEvent dying = SourceEvent.builder("import", "dying", SourceEvent.Kind.POKEMON)
                .coordinates(-23.55, -46.63).expiresAt(NOW + 1000).build();
        store.add(dying);
        check(store.prune(NOW + 5000) == 1, "Registros expirados são removidos");

        System.out.println("LocalEventImportTest: OK");
    }

    private interface Call { void run() throws Exception; }

    private static boolean fails(Call call) {
        try { call.run(); return false; }
        catch (Exception expected) { return true; }
    }
}
