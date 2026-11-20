package dev.pgrazor;

public final class PtBrDynamicTest {
    private static void eq(String english, String ptbr) {
        String app=PtBr.tr(english);
        String root=dev.pgrazor.root.RootPtBr.tr(english);
        if(!ptbr.equals(app))throw new AssertionError(english+" -> app: "+app);
        if(!ptbr.equals(root))throw new AssertionError(english+" -> root: "+root);
    }
    public static void main(String[] args) {
        eq("VGP  |  App left foreground; tap VGP to start","VGP  |  O aplicativo saiu do primeiro plano; toque no VGP para iniciar");
        eq("Waiting for game readiness","Aguardando o jogo ficar pronto");
        eq("Ready: Quick Catch","Pronto: Captura rápida");
        eq("Ready: new catches only; IV below 90%","Pronto: apenas capturas novas; IV abaixo de 90%");
        eq("EP: 13 | Maintenance ~18m 16s","EP: 13 | Manutenção ~18m 16s");
        eq("Camera heading 1°; up follows the view","Direção da câmera 1°; o topo acompanha a visualização");
        eq("No selected berry above reserve (50 each)","Nenhuma fruta selecionada acima da reserva (50 por fruta)");
        eq("Standard berry: Razz","Fruta padrão: Frambo");
        eq("VGP+ auto ball target: 95%","Alvo da bola automática do VGP+: 95%");
        eq("Catch speed: Normal (about 1.5 s)","Velocidade de captura: Normal (aprox. 1,5 s)");
        eq("High CP threshold (0–10000): 1500","Limite de PC alto (0–10000): 1500");
        eq("Catch only: All species","Capturar somente: Todas as espécies");
        System.out.println("PGBR: testes de textos dinamicos aprovados.");
    }
}
