package dev.pgrazor;
public final class JoystickDisplayPolicyTest {
    public static void main(String[] args) {
        for(int bits=0;bits<128;bits++){
            boolean preferred=(bits&1)!=0,hidden=(bits&2)!=0,settings=(bits&4)!=0,
                menu=(bits&8)!=0,encounter=(bits&16)!=0,focused=(bits&32)!=0,black=(bits&64)!=0;
            boolean expected=bits==33;
            if(JoystickDisplayPolicy.visible(preferred,hidden,settings,menu,encounter,focused,black)!=expected)
                throw new AssertionError("visibility flags "+bits);
        }
    }
}
