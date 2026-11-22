package dev.pgrazor;

import java.util.Random;

public final class TrainerAliasPolicyTest {
    public static void main(String[] args){
        Random r=new Random(12345);
        for(int i=0;i<250;i++){
            String alias=TrainerAliasPolicy.generate(r);
            if(!TrainerAliasPolicy.valid(alias)||alias.length()!=18)throw new AssertionError(alias);
        }
        if(TrainerAliasPolicy.valid(null)||TrainerAliasPolicy.valid("PGBR_Treinador")||
           TrainerAliasPolicy.valid("PGBR_Treinador000A"))throw new AssertionError("Invalid alias accepted");
        for(String alias:new String[]{"PGBR Treinador","Treinador LT","João-42","PGEterna_2026"})
            if(!TrainerAliasPolicy.validCustom(alias))throw new AssertionError("Alias valid rejected: "+alias);
        for(String alias:new String[]{"","x"," a","a ","a  b","<script>","Joe\\n42","🚀","aaaaaaaaaaaaaaaaaaaaaaaaa"})
            if(TrainerAliasPolicy.validCustom(alias))throw new AssertionError("Unsafe alias accepted: "+alias);
        if(TrainerAliasPolicy.validCustom(null))throw new AssertionError("Null alias accepted");
        System.out.println("PGBR: gerador de apelidos visuais aprovado");
    }
}
