package dev.pgrazor;

import android.app.Activity;
import android.app.AlertDialog;

/** The quick menu's Feeds entry: pick which live feed service to open (each keeps its own sign-in). */
final class FeedChooser {
    private FeedChooser() {}

    static void show(Activity activity) {
        String[] labels = {"LivePokeMap\nPokémon próximos, Poképaradas, Ginásios, Reides e Pontos de Poder",
                           "PoGoCoordinates\nBusca por IV, Shiny, rankings PvP, tamanho e espécie (conta Discord necessária)"};
        AlertDialog choose = ChoiceList.attach(new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr("Busca de Pokémon e rastreadores"))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"), null).create(), labels, which -> {
                if (which == 0) LiveFeedDialog.show(activity); else PogoFeedDialog.show(activity);
            });
        DialogLifecycle.bind(activity, choose, null);
        choose.setOnShowListener(d -> MapTheme.styleButtons(choose));
        choose.show();
    }
}
