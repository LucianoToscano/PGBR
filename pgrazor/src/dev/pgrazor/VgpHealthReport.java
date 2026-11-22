package dev.pgrazor;

/** Read-only, PT-BR VGP troubleshooting summary. Never enables an action. */
final class VgpHealthReport {
    private VgpHealthReport() {}

    static String build(boolean overlay, boolean enabled, int options, String runtimeStatus) {
        String status=runtimeStatus==null?"":runtimeStatus.trim().replace('\n',' ').replace('\r',' ');
        if(status.length()>320)status=status.substring(0,320)+"…";
        StringBuilder r=new StringBuilder();
        r.append("Botão VGP+: ").append(overlay?"visível":"oculto").append('\n');
        r.append("Automação: ").append(!overlay||!enabled?"desligada":
            "início solicitado (ações não confirmadas)").append('\n');
        r.append("Captura: ").append((options&1)!=0?"habilitada":"desabilitada").append('\n');
        r.append("Giro: ").append((options&2)!=0?"habilitado":"desabilitado").append('\n');
        r.append("Estado: ").append(status.isEmpty()?"indisponível":status).append('\n');
        r.append("\nPróxima verificação: ");
        if(!overlay)r.append("ative Exibir botão VGP+; isso ainda não inicia a automação.");
        else if(!enabled)r.append("use Iniciar VGP+ e confirme a operação.");
        else if((options&3)==0)r.append("habilite captura e/ou giro nas opções.");
        else if((options&1)==0)r.append("habilite Captura de Pokémon para capturar.");
        else if((options&2)==0)r.append("habilite Giro de Poképaradas para girar.");
        else r.append("se não houver ações, consulte o Registro de execução e a prontidão nativa.");
        r.append("\n\nBotão visível ou ligado não confirma captura/giro. "
               +"Este relatório não toca no jogo, não envia coordenadas nem altera sua conta.");
        return r.toString();
    }
}
