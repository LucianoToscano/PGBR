package dev.pgrazor;

public final class CandyQuestPolicyTest {
    private static void check(boolean result){if(!result)throw new AssertionError("Candy quest policy");}
    public static void main(String[] args){
        check(CandyQuestPolicy.selectionRequired("Candy",false,0,0));
        check(!CandyQuestPolicy.selectionRequired("Candy",true,0,0));
        check(!CandyQuestPolicy.selectionRequired("Candy",false,1,0));
        check(!CandyQuestPolicy.selectionRequired("Candy",false,0,1));
        check(!CandyQuestPolicy.selectionRequired("Pokemon",false,0,0));
        check(!CandyQuestPolicy.selectionRequired(null,false,0,0));
        System.out.println("PASS Candy quest selection/locality policy");
    }
}
