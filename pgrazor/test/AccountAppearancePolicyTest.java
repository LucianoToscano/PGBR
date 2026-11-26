package dev.pgrazor;
import java.util.Properties;
public final class AccountAppearancePolicyTest {
    public static void main(String[] args){
        Properties p=new Properties();
        p.setProperty("darkTheme","false");p.setProperty("uiScalePercent","75");
        p.setProperty("joystickSizeDp","220");p.setProperty("joystickOpacity","101");
        p.setProperty("vgpIconOpacity","9");p.setProperty("eternatusFxSpeed","2");
        p.setProperty("trainerAlias","private");p.setProperty("trainerPrivacyMode","2");
        p.setProperty("locationSpoofing","true");p.setProperty("joystickVisible","false");
        p.setProperty("apiKey","secret");p.setProperty("vgpOptions","7");
        p.setProperty("joystickStyle","3");p.setProperty("route","private");
        Properties out=AccountAppearancePolicy.select(p);
        if(out.size()!=4||!"false".equals(out.getProperty("darkTheme"))||
            !"75".equals(out.getProperty("uiScalePercent"))||
            !"220".equals(out.getProperty("joystickSizeDp"))||
            !"2".equals(out.getProperty("eternatusFxSpeed")))throw new AssertionError("allowlist");
        if(p.size()!=14)throw new AssertionError("source changed");
        p.setProperty("uiScalePercent","80");p.setProperty("darkTheme","invalid");
        if(AccountAppearancePolicy.select(p).size()!=2)throw new AssertionError("invalid values");
        if(!AccountAppearancePolicy.select(null).isEmpty())throw new AssertionError("null");
    }
}
