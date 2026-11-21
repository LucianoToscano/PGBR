package dev.pgrazor;

/**
 * Read-only VGP troubleshooting summary. No server, account, hook or automation calls.
 * Input values come from Control's existing status snapshot; the report never starts VGP.
 */
final class VgpHealthReport {
    private VgpHealthReport() {}

    static String build(boolean overlay, boolean enabled, int options,
                        boolean stopOnCooldown, long cooldownMs, String runtimeStatus) {
        String status = runtimeStatus == null ? "" : runtimeStatus.trim();
        if (status.length() > 320) status = status.substring(0, 320) + "…";
        StringBuilder report = new StringBuilder();
        report.append("VGP: ").append(!overlay ? "botão oculto"
                : enabled ? "ativada (sem confirmação de ações)" : "desligada").append('\n');
        report.append("Captura de Pokémon: ").append((options & 1) != 0 ? "selecionada" : "desmarcada").append('\n');
        report.append("Giro de Poképaradas: ").append((options & 2) != 0 ? "selecionado" : "desmarcado").append('\n');
        report.append("Pausa durante cooldown: ").append(stopOnCooldown ? "ativada" : "desativada").append('\n');
        if (cooldownMs > 0) {
            report.append("Cooldown local estimado: ").append((cooldownMs + 999) / 1000)
                    .append(" s (não é confirmação do servidor)").append('\n');
        } else {
            report.append("Cooldown local estimado: liberado (não confirma ações no servidor)").append('\n');
        }
        report.append('\n').append("Diagnóstico atual: ")
                .append(status.isEmpty() ? "indisponível" : status).append('\n').append('\n');
        report.append("Próxima verificação: ");
        if (!overlay) {
            report.append("ative 'VGP overlay' e depois inicie VGP+.");
        } else if (!enabled) {
            if (stopOnCooldown && cooldownMs > 0) {
                report.append("aguarde o cooldown estimado terminar; depois inicie VGP+ manualmente.");
            } else {
                report.append("inicie VGP+ manualmente e confira se a execução foi aceita.");
            }
        } else if ((options & 3) == 0) {
            report.append("marque captura e/ou giro nas opções da VGP.");
        } else if ((options & 1) == 0) {
            report.append("para capturar, marque 'Nearby Pokémon'.");
        } else if ((options & 2) == 0) {
            report.append("para girar, marque 'Nearby PokéStops'.");
        } else if (status.toLowerCase(java.util.Locale.ROOT).contains("aguardando")) {
            report.append("verifique a condição descrita acima; o botão ligado não comprova funcionamento.");
        } else {
            report.append("se não houver captura/giro, registre um diagnóstico de interação "
                    + "em Contas e backup > Diagnósticos.");
        }
        report.append('\n').append('\n')
                .append("Este painel não executa captura, giro, teleportes ou mudanças na conta.");
        return report.toString();
    }
}
