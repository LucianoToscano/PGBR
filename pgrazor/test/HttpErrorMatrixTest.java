package dev.pgrazor;

/** Offline contract for the HTTP error matrix. No network: only URL validation is exercised. */
public final class HttpErrorMatrixTest {
    static int checks;
    static void ok(boolean truth, String why) { checks++; if (!truth) throw new AssertionError(why); }

    static void rejects(String url, String why) {
        try { RocketMapFeedClient.validateBaseUrl(url); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("aceitou URL perigosa: " + why + " (" + url + ")");
    }

    static void accepts(String url, String expected) {
        checks++;
        String got = RocketMapFeedClient.validateBaseUrl(url);
        if (!expected.equals(got)) throw new AssertionError("normalização: " + url + " -> " + got);
    }

    public static void main(String[] args) {
        // Credential smuggling and plaintext transport must never reach a request.
        rejects("http://meu-servidor.example", "HTTP sem TLS");
        rejects("https://user:senha@meu-servidor.example", "credencial na URL");
        rejects("https://meu-servidor.example/?token=abc", "query na URL");
        rejects("https://meu-servidor.example/#x", "fragmento na URL");
        rejects("https://meu-servidor.example/../admin", "path traversal");
        rejects("", "vazio");
        rejects(null, "nulo");
        rejects("https://" + "a".repeat(320), "longo demais");
        rejects("meu-servidor.example", "sem esquema");
        rejects("ftp://meu-servidor.example", "esquema não HTTPS");
        accepts("https://meu-servidor.example/", "https://meu-servidor.example");
        accepts(" https://meu-servidor.example/rm ", "https://meu-servidor.example/rm");
        System.out.println("PASS: " + checks + " casos da matriz de validação de servidor HTTPS");
    }
}
