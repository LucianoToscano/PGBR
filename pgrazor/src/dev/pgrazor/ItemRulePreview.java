package dev.pgrazor;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;

/** Read-only description of existing item limits. It cannot apply rules or modify the bag. */
final class ItemRulePreview {
    private ItemRulePreview() {}
    static String describe(Map<Integer,Integer> limits, Set<Integer> checked, String catalog) {
        Map<Integer,String[]> rows = new HashMap<>();
        if(catalog != null && catalog.length() <= 100000) {
            for(String row:catalog.split("\n")) {
                String[] fields=row.split("\\|",-1);
                if(fields.length!=3)continue;
                try{
                    int id=Integer.parseInt(fields[0]), count=Integer.parseInt(fields[2]);
                    if(id>0 && count>=0) rows.put(id,fields);
                }catch(NumberFormatException ignored){}
            }
        }
        StringBuilder out=new StringBuilder("Estimativa pelo catálogo disponível (pode estar desatualizado). Confirme os limites antes de salvar. Itens não marcados serão mantidos.\n");
        int zeros=0,missing=0,shown=0;
        for(int id:new TreeSet<>(checked)){
            Integer cap=limits.get(id);
            if(cap==null)continue;
            if(cap==0)zeros++;
            String[] row=rows.get(id);
            if(row==null)missing++;
            if(shown++>=80)continue;
            out.append('\n').append(row==null?"Item #"+id:PtBrLabel.clean(row[1],id)).append(": limite ").append(cap);
            if(row==null)out.append(" • quantidade não disponível");
            else{
                int count=Integer.parseInt(row[2]);
                out.append(" • catálogo ").append(count).append(" • excedente estimado ").append(Math.max(0,count-cap));
            }
            if(cap==0)out.append(" • REMOVE TODOS");
        }
        if(shown==0)out.append("\nNenhum item marcado.");
        if(shown>80)out.append("\nMais ").append(shown-80).append(" regras não exibidas.");
        if(missing>0)out.append("\nItens sem quantidade disponível: ").append(missing).append(". Revise antes de confirmar.");
        if(zeros>0)out.append("\nATENÇÃO: ").append(zeros).append(" limite(s) zero. A regra existente poderá remover toda a quantidade desses itens.");
        return out.toString();
    }
    private static final class PtBrLabel {
        static String clean(String value,int id){
            String name=value.replaceAll("[\\p{Cntrl}\\p{Cf}]"," ").trim();
            if(name.isEmpty())return "Item #"+id;
            return name.length()>100?name.substring(0,100):name;
        }
    }
}
