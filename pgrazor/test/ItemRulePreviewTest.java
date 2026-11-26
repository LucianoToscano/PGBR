package dev.pgrazor;
import java.util.Map;
import java.util.Set;
final class ItemRulePreviewTest {
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args){
        Map<Integer,Integer> limits=Map.of(1,10,2,0,3,30,4,10);
        String preview=ItemRulePreview.describe(limits,Set.of(1,2,4),"1|Pokebola|25\n2|Bola especial|3\n3|Nao marcado|100");
        check(preview.contains("excedente estimado 15"),"positive surplus");
        check(preview.contains("REMOVE TODOS"),"explicit zero");
        check(preview.contains("limite(s) zero"),"zero aggregate");
        check(preview.contains("Item #4")&&preview.contains("quantidade não disponível"),"missing row preserved");
        check(!preview.contains("Nao marcado"),"unselected item excluded");
        check(preview.contains("pode estar desatualizado"),"cache caveat");
        check(ItemRulePreview.describe(Map.of(1,100),Set.of(1),"1|A|2").contains("excedente estimado 0"),"no negative surplus");
        check(ItemRulePreview.describe(Map.of(1,0),Set.of(1),"1|A|-1").contains("quantidade não disponível"),"invalid count unknown");
        check(ItemRulePreview.describe(Map.of(),Set.of(),"").contains("Nenhum item marcado"),"empty selection");
        check(ItemRulePreview.describe(Map.of(1,5),Set.of(1),"1|A|bad").contains("quantidade não disponível"),"invalid count");
        check(ItemRulePreview.describe(Map.of(1,5),Set.of(1),null).contains("quantidade não disponível"),"null catalog");
        check(limits.size()==4,"preview does not mutate rules");
        System.out.println("PASS: ItemRulePreview");
    }
}
