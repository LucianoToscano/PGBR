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
                           "PoGoCoordinates\nBusca por IV, Shiny, PvP e espécie (limites próprios da fonte)"};
        AlertDialog choose = ChoiceList.attach(new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Busca de Pokémon e rastreadores"))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).create(), labels, which -> {
                if (which == 0) LiveFeedDialog.showPokemonSearch(activity);
                else if (which == 1) LiveFeedDialog.showRaidSearch(activity);
                else if (which == 2) LiveFeedDialog.show(activity);
                else PogoFeedDialog.show(activity);
            });
        DialogLifecycle.bind(activity, choose, null);
        choose.setOnShowListener(d -> MapTheme.styleButtons(choose));
        choose.show();
    }
}
