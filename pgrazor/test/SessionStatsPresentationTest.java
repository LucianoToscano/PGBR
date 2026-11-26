package dev.pgrazor;
final class SessionStatsPresentationTest {
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args){
        double[] values={6000,60,90,12,1.25,3,500,200};
        SessionStatsHistory.Change partial=new SessionStatsHistory.Change(values,30*60_000L,1795000000000L);
        String summary=SessionStatsPresentation.describe(partial,SessionStatsHistory.HOUR_MS);
        check(summary.contains("30 min observados"),"coverage");
        check(summary.contains("período parcial"),"partial");
        check(summary.contains("Poeira +500 / −200"),"spent");
        check(summary.contains("estimativa pelo saldo"),"estimate");
        check(summary.contains("1,25 km"),"PT-BR decimal");
        check(summary.contains("XP/h estimado: 12.000"),"rate denominator");
        check(summary.contains("outros aparelhos"),"scope");
        String exported=SessionStatsPresentation.exportSummary(partial,SessionStatsHistory.HOUR_MS);
        check(exported.contains("(1 h)"),"window");
        check(!exported.contains("1795000000000"),"no timestamp");
        check(SessionStatsPresentation.exportSummary(null,SessionStatsHistory.HOUR_MS)==null,"no sample");
        SessionStatsHistory.Change zero=new SessionStatsHistory.Change(values,0,1);
        check(SessionStatsPresentation.exportSummary(zero,SessionStatsHistory.HOUR_MS)==null,"no coverage export");
        check(!SessionStatsPresentation.describe(zero,SessionStatsHistory.HOUR_MS).contains("XP/h"),"no zero division");
        check(SessionStatsPresentation.describe(new SessionStatsHistory.Change(values,SessionStatsHistory.DAY_MS,1),SessionStatsHistory.DAY_MS).contains("período completo"),"full day");
        values[0]=Double.NaN;
        check(SessionStatsPresentation.describe(partial,SessionStatsHistory.HOUR_MS).contains("indisponível"),"nonfinite rejected");
        System.out.println("PASS: SessionStatsPresentation");
    }
}
