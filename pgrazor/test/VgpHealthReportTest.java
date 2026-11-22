package dev.pgrazor;
/** JVM-only assertions for the VGP diagnostic. */
public final class VgpHealthReportTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        String hidden=VgpHealthReport.build(false,false,3,null);
        check(hidden.contains("oculto"),"overlay hidden");
        check(hidden.contains("desligada"),"hidden cannot mean enabled");
        String visible=VgpHealthReport.build(true,false,3,"");
        check(visible.contains("use Iniciar VGP+"),"visible should require explicit start");
        String noOptions=VgpHealthReport.build(true,true,0,"VGP pronta");
        check(noOptions.contains("habilite captura e/ou giro"),"no options");
        String running=VgpHealthReport.build(true,true,3,"Aguardando runtime");
        check(running.contains("ações não confirmadas"),"no false success");
        check(running.contains("Registro de execução"),"next step");
        check(!running.contains("sucesso garantido"),"must not guarantee");
        String oversized=VgpHealthReport.build(true,true,3,new String(new char[1200]).replace('\0','A'));
        check(oversized.length()<900,"bounded report");
        check(!oversized.contains("Coordenadas:"),"no coordinates");
        System.out.println("PASS: VGP read-only health report");
    }
}
