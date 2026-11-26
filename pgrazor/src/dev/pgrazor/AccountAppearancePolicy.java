package dev.pgrazor;

import java.util.Properties;

/** Allowlist for explicit cross-profile appearance copies; no identity or game preferences. */
final class AccountAppearancePolicy {
    private AccountAppearancePolicy(){}
    static Properties select(Properties source){
        Properties out=new Properties();
        if(source==null)return out;
        bool(source,out,"darkTheme");
        number(source,out,"uiScalePercent",25,100,true);
        number(source,out,"joystickSizeDp",88,220,false);
        number(source,out,"joystickOpacity",10,100,false);
        bool(source,out,"joystickEternatusEffects");
        number(source,out,"eternatusFxIntensity",1,2,false);
        number(source,out,"eternatusFxSpeed",0,2,false);
        number(source,out,"menuIconSizeDp",16,40,false);
        number(source,out,"gymIconSizeDp",24,110,false);
        number(source,out,"gymIconOpacity",10,100,false);
        number(source,out,"overlayBackgroundOpacity",0,100,false);
        number(source,out,"menuLauncherSizeDp",28,96,false);
        number(source,out,"menuLauncherOpacity",10,100,false);
        number(source,out,"menuLauncherBgOpacity",0,100,false);
        number(source,out,"gymBackgroundOpacity",0,100,false);
        number(source,out,"vgpIconOpacity",10,100,false);
        number(source,out,"vgpBackgroundOpacity",0,100,false);
        return out;
    }
    private static void bool(Properties from,Properties to,String key){
        String v=from.getProperty(key);
        if("true".equals(v)||"false".equals(v))to.setProperty(key,v);
    }
    private static void number(Properties from,Properties to,String key,int low,int high,boolean scale){
        String v=from.getProperty(key);
        if(v==null||!v.matches("[0-9]{1,3}"))return;
        int n=Integer.parseInt(v);
        if(n>=low&&n<=high&&(!scale||n%25==0))to.setProperty(key,Integer.toString(n));
    }
}
