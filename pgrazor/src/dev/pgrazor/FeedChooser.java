package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;

/** The quick menu's Feeds entry: pick which live feed service to open (each keeps its own sign-in). */
final class FeedChooser {
    private FeedChooser() {}

    static void show(Activity activity) {
        String[] labels = {
                           "Filtro Pokémon · estilo PGSharp\nEspécie, IV, nível, PvP e coordenadas reais por fonte",
                           "Filtro Reides · estilo PGSharp\nChefe, tipo, equipe e tempo restante com teleporte confirmado",
                           "Radar LivePokeMap\nPokémon, Poképaradas, Ginásios, Reides e Pontos de Poder",
                           "PoGoCoordinates\nBusca por IV, Shiny, PvP e espécie (limites próprios da fonte)",
                           "Sniper 100% IV · PGBR\nConsulta manual ao feed existente; toque para coordenadas e confirmação",
                           "PGSharp · 100% IV grátis / licença\nOrigem separada; key opcional para filtros Standard",
                           "Radar PGBR · 5 fontes\nPokémon, Reides, Pesquisas, Rocket, Power Spots, Ninhos e Rotas com origem e TTL reais",
                           "Sniper 1 · busca rápida\nEspécie, IV 100/0/faixa, PC, TTL e raio; copiar, mapa e Teleporte com confirmação",
                           "Sniper 2 · busca avançada\nA/D/S, PvP GL/UL, forma, sexo, tamanho, E/OU, presets e favoritos",
                           "Importar dados locais\nJSON, CSV, GPX ou coordenadas coladas; importação pessoal identificada"};
        AlertDialog choose = ChoiceList.attach(new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Busca de Pokémon e rastreadores"))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).create(), labels, which -> {
                if (which == 0) LiveFeedDialog.showPokemonSearch(activity);
                else if (which == 1) LiveFeedDialog.showRaidSearch(activity);
                else if (which == 2) LiveFeedDialog.show(activity);
                else if (which == 3) PogoFeedDialog.show(activity);
                else if (which == 4) HundoFeedDialog.show(activity);
                else if (which == 5) PgSharpFeedDialog.show(activity);
                else if (which == 6) SourceRadarDialog.show(activity);
                else if (which == 7) SniperDialog.showQuick(activity);
                else if (which == 8) SniperDialog.showAdvanced(activity);
                else if (which == 9) LocalImportDialog.show(activity, null);
            });
        DialogLifecycle.bind(activity, choose, null);
        choose.setOnShowListener(d -> MapTheme.styleButtons(choose));
        choose.show();
    }
}
