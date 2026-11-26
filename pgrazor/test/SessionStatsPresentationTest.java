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
        check(!SessionStatsPresentation.describe(partial,SessionStatsHistory.HOUR_MS).contains("indisponível"),"snapshot independent of caller array");
        check(SessionStatsPresentation.describe(new SessionStatsHistory.Change(values,60_000,1),SessionStatsHistory.HOUR_MS).contains("indisponível"),"nonfinite rejected");
        SessionStatsHistory history=new SessionStatsHistory();
        long now=1_795_000_000_000L;
        history.record(now,new double[]{100,1,1,1,1,1,-1});
        history.record(now+60_000,new double[]{200,2,2,2,2,2,-1});
        check(!history.change(SessionStatsHistory.HOUR_MS).dustObserved,"unknown balance is not zero gain");
        check(SessionStatsPresentation.describe(history.change(SessionStatsHistory.HOUR_MS),SessionStatsHistory.HOUR_MS).contains("Poeira: dados insuficientes"),"unknown dust visible");
        history.record(now+120_000,new double[]{300,3,3,3,3,3,100});
        check(!history.change(SessionStatsHistory.HOUR_MS).dustObserved,"unknown starting balance remains unknown");
        history.record(now+600_000,new double[]{400,4,4,4,4,4,100});
        history.record(now+900_000,new double[]{500,5,5,5,5,5,100});
        check(history.change(300_000).dustObserved,"known baseline with zero gain");
        check(SessionStatsPresentation.describe(history.change(300_000),300_000).contains("Poeira +0 / −0"),"true zero distinguished");
        check(SessionStatsPresentation.describe(new SessionStatsHistory.Change(new double[8],119_999,1),SessionStatsHistory.HOUR_MS).contains("1 min observados"),"coverage never rounded upward");
        System.out.println("PASS: SessionStatsPresentation");
    }
}
