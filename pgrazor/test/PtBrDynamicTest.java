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
        eq("Pokestop spin","Giro de Poképarada");
        eq("PokéStop spin","Giro de Poképarada");
        eq("Encounter: Nickit · EVENTO","Encontro: Nickit · EVENTO");
        eq("Stardust · EVENTO","Poeira Estelar · EVENTO");
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
        eq("Razz Berry","Fruta Frambo");
        eq("Nanab Berry","Fruta Caxí");
        eq("Troy Disk Glacial","Módulo Atrair Glacial");
        eq("Gen5 Evolution Stone","Pedra de Unova");
        eq("Move Reroll Elite Fast Attack","MT de Ataque Ágil de Elite");
        eq("Priority: longest distance","Prioridade: maior distância");
        eq("Priority: shortest distance","Prioridade: menor distância");
        eq("Starting 7 km egg","Iniciando ovo de 7 km");
        eq("Ready: 35 recyclable item types","Pronto: 35 tipos de itens recicláveis");
        eq("No field research seen yet. Tasks appear here as the game gives them to you while PGBR runs.","Nenhuma Pesquisa de Campo detectada ainda. As tarefas aparecerão aqui conforme o jogo as disponibilizar enquanto o PGBR estiver ativo.");
        eq("This session: claimed ","Nesta sessão: resgatadas ");
        eq(", removed ",", removidas ");
        eq(", caught ",", capturadas ");
        eq("Feeding priority: ","Prioridade de frutas: ");
        eq("Search places or coordinates","Buscar lugares ou coordenadas");
        eq("Power Spots","Pontos de Poder");
        eq("Search name or #dex","Buscar nome ou número da Pokédex");
        eq("Shiny Scanner","Scanner de Brilhantes");
        eq("Scan for shinies","Procurar Pokémon Brilhantes");
        eq("Stop route?","Interromper a rota?");
        eq("Stop route","Interromper rota");
        eq("Nearby here","Próximos daqui");
        eq("Nearby layers unavailable","Camadas de locais próximos indisponíveis");
        eq("No live results in range.","Nenhum resultado disponível no alcance selecionado.");
        eq("Encounter limit: 25","Limite de encontros: 25");
        eq("UI scale: 100%","Escala da interface: 100%");
        eq("Filter presets","Predefinições de filtros");
        eq("Saved places could not be read.","Não foi possível ler os locais salvos.");
        eq("Berry priority","Prioridade de frutas");
        eq("PGBR · current run","PGBR · execução atual");
        eq("Refresh log","Atualizar registro");
        eq("Back to controls","Voltar aos controles");
        eq("Go to coordinates","Ir para coordenadas");
        eq("Choose up to 3. The next choice starts a new list.","Escolha até três opções. A próxima escolha inicia uma nova lista.");
        System.out.println("PGBR: testes de textos dinamicos aprovados.");
    }
}
