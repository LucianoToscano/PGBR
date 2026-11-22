package dev.pgrazor;

import android.app.Activity;

/**
 * Espelho do contrato REAL de WorldMapDialog (pgrazor/src/dev/pgrazor/WorldMapDialog.java).
 *
 * <p>Este stub antes declarava `Callback { void apply(...) }`, um contrato que NÃO existe no código
 * real. Com ele o painel compilava aqui e quebrava no build Android de verdade ("Cannot return a void
 * result" em DestinationListener). Um stub que inventa assinatura não é prova de nada: o contrato
 * precisa ser o do arquivo real, incluindo o retorno boolean.
 */
public final class WorldMapDialog {
    private WorldMapDialog() {}

    /** Retorne true somente depois de aplicar o destino; false mantém o seletor aberto. */
    public interface DestinationListener {
        boolean onDestination(double latitude, double longitude);
    }

    public interface RouteListener {
        boolean onRoute(double[][] waypoints, boolean loop);
    }

    public static android.app.AlertDialog show(Activity activity, double latitude, double longitude,
                                              DestinationListener listener) {
        return null;
    }

    public static android.app.AlertDialog show(Activity activity, double latitude, double longitude,
                                              DestinationListener listener, DestinationListener walk) {
        return null;
    }

    public static android.app.AlertDialog show(Activity activity, double latitude, double longitude,
                                              DestinationListener listener, DestinationListener walk,
                                              RouteListener route) {
        return null;
    }
}
