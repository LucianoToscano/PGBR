package dev.pgrazor;

/** Plain JVM tests for the read-only, safe PT-BR VGP troubleshooting report. */
public final class VgpHealthReportTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static String report(boolean overlay, boolean enabled, int options,
                                 boolean guard, long cooldown, String status) {
        return VgpHealthReport.build(overlay, enabled, options, guard, cooldown, status);
    }
    public static void main(String[] args) {
        String hidden = report(false, false, 3, false, 0, null);
        check(hidden.contains("botão oculto"), "Hidden VGP is not reported running");
        check(hidden.contains("ative 'VGP overlay'"), "Show next step for hidden overlay");
        check(hidden.contains("indisponível"), "Missing status must be explicit");

        String unstarted = report(true, false, 3, false, 0, "");
        check(unstarted.contains("desligada"), "Visible overlay must not imply enabled");
        check(unstarted.contains("inicie VGP+ manualmente"), "Manual action made clear");

        String cooldown = report(true, false, 3, true, 3101, "VGP pausada");
        check(cooldown.contains("4 s"), "Cooldown milliseconds round up, not down");
        check(cooldown.contains("depois inicie VGP+ manualmente"), "Never promise auto resume");
        check(cooldown.contains("não é confirmação do servidor"), "Cooldown is estimated");

        String noOptions = report(true, true, 0, false, 0, "VGP habilitada para ações");
        check(noOptions.contains("marque captura e/ou giro"), "Missing both VGP actions");

        String onlySpin = report(true, true, 2, false, 0, "VGP habilitada");
        check(onlySpin.contains("Captura de Pokémon: desmarcada"), "Capture-off reason");
        check(onlySpin.contains("para capturar"), "Missing capture step");

        String onlyCatch = report(true, true, 1, false, 0, "VGP habilitada");
        check(onlyCatch.contains("Giro de Poképaradas: desmarcado"), "Spin-off reason");
        check(onlyCatch.contains("para girar"), "Missing spin step");

        String waiting = report(true, true, 3, false, 0, "VGP aguardando: suporte nativo");
        check(waiting.contains("não comprova funcionamento"), "Do not falsely claim VGP works");

        String ready = report(true, true, 3, false, 0, "VGP habilitada para ações");
        check(ready.contains("registre um diagnóstico"), "Troubleshooting path exposed");
        check(!ready.contains("sucesso garantido"), "No invented successes");
        check(!ready.contains("Coordenadas:"), "Report contains no coordinates");

        String oversized = report(true, true, 3, false, 0, new String(new char[1000]).replace('\0', 'A'));
        check(oversized.length() < 1100, "Native status should be bounded");

        System.out.println("PASS: VGP read-only diagnostic, cooldown, option readiness and false-success guard");
    }
}
