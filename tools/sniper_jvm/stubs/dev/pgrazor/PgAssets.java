package dev.pgrazor;

import android.content.Context;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Espelho de PgAssets.open(Context, String) — mesma assinatura e mesma exceção do arquivo real.
 *
 * <p>Aqui ele sempre falha, de propósito: o catálogo de nomes não existe no harness. Isso exercita o
 * caminho de "não sei resolver nomes nesta instalação", que precisa degradar para o filtro local em
 * vez de inventar uma espécie.
 */
final class PgAssets {
    private PgAssets() {}

    public static InputStream open(Context context, String name) throws IOException {
        throw new FileNotFoundException("assets indisponíveis no harness: " + name);
    }
}
