package dev.pgrazor;

public final class UpdateVersionTest {
    private static void test(boolean value){if(!value)throw new AssertionError("Updater version check failed");}
    public static void main(String[] args){
        test(UpdateVersion.valid("v0.00.05-LT"));
        test(UpdateVersion.isNewer("v0.00.05-LT","v0.00.04-LT"));
        test(UpdateVersion.isNewer("v1.0.0","v0.99.99"));
        test(!UpdateVersion.isNewer("v0.00.04-LT","v0.00.04-LT"));
        test(!UpdateVersion.isNewer("v0.00.03-LT","v0.00.04-LT"));
        test(!UpdateVersion.isNewer("not-a-version","v0.00.04-LT"));
        test(!UpdateVersion.isNewer("v0.00.05-LT","preview"));
        System.out.println("PGBR: testes de versao aprovados");
    }
}
