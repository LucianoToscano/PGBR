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
        System.out.println("PGBR: gerador de apelidos visuais aprovado");
    }
}
