package dev.pgrazor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** A failed pending save must never make reload replace live choices with an older disk snapshot. */
public final class ControlSettingsReloadTest {
    public static void main(String[] args) throws Exception {
        android.app.Application app=new android.app.Application() {
            public java.io.File getFilesDir(){return new java.io.File(args[0]);}
        };
        Method load=Control.class.getDeclaredMethod("loadSettings",android.app.Application.class);
        if(Control.gymRepeatEnabled())throw new AssertionError("Gym repeat should default off");
        if(Control.localTimeOverlay())throw new AssertionError("Local clock should default off");
        if(!Control.vgpTapProtection())throw new AssertionError("VGP anti-tap should default ON");
        load.setAccessible(true); load.invoke(null,app);
        if(Control.joystickStyle()!=1 || Control.menuLauncherIconStyle()!=1)
            throw new AssertionError("New profiles must default to Eternatus joystick and Pokeball");
        Control.setGymRepeatEnabled(true);
        Control.setLocalTimeOverlay(true);
        if(!"true".equals(Control.exportSettings().getProperty("localTimeOverlay")))
            throw new AssertionError("Export missed local clock toggle");
        if(!"true".equals(Control.exportSettings().getProperty("gymRepeatEnabled")))
            throw new AssertionError("Export missed gym repeat toggle");
        Control.setBattleOptions(111); Control.setBattleOptions(981);
        if(!"981".equals(Control.exportSettings().getProperty("battleOptions")))
            throw new AssertionError("Export missed latest queued choice");
        Control.setAutoBerry(705); Control.setBattleOptions(2);
        java.util.Properties incoming=new java.util.Properties();
        Control.setLocalTimeFormat(3);
        Control.setCooldownTextSp(11);
        incoming.setProperty("localTimeFormat","5");
        incoming.setProperty("cooldownEmphasis","true");
        incoming.setProperty("cooldownTextSp","18");
        incoming.setProperty("localTimeOverlay","false"); incoming.setProperty("vgpTapProtection","false");
        incoming.setProperty("idleWalking","false"); incoming.setProperty("battleOptions","5"); incoming.setProperty("autoBerry","701");
        // Explicitly selected classic visuals must survive the new Eternatus defaults.
        incoming.setProperty("joystickStyle","0"); incoming.setProperty("menuLauncherIconStyle","0"); incoming.setProperty("gymRepeatEnabled","false");
        Control.importSettings(app,incoming,9.3); Control.flushSettings(5000);
        if(Control.localTimeFormat()!=5 || Control.cooldownTextSp()!=18 || !Control.cooldownEmphasis())
            throw new AssertionError("Imported clock and cooldown appearance did not load");
        if(Control.idleWalking())throw new AssertionError("Idle walking import did not apply false");
        if(Control.gymRepeatEnabled())throw new AssertionError("Gym repeat import did not apply false");
        if(Control.localTimeOverlay())throw new AssertionError("Local clock import did not apply false");
        if(Control.vgpTapProtection() || !"false".equals(Control.exportSettings().getProperty("vgpTapProtection")))
            throw new AssertionError("VGP protection import/export not applied");
        if(Control.joystickStyle()!=0 || Control.menuLauncherIconStyle()!=0)
            throw new AssertionError("Explicit classic joystick/Pokeball styles must remain user-controlled");
        if(Control.battleOptions()!=5 || Control.autoBerry()!=701 || Control.vgpEnabled())
            throw new AssertionError("Import ordering, legacy berry migration or VGP OFF changed");
        if(!"5".equals(Control.exportSettings().getProperty("battleOptions")))
            throw new AssertionError("A queued snapshot overwrote imported choices");
        // Evolve before transferring: a corrupt persisted mode must fail safe and disable the feature on reload.
        Control.setAutoTransferEnabled(true);
        Control.setEvolveTransferEnabled(true); Control.setEvolveTransferMode(1); Control.setEvolveTransferStages(2); Control.setEvolveTransferSpecies("1,4,25");
        if(!Control.evolveTransferEnabled()||Control.evolveTransferMode()!=1||Control.evolveTransferStages()!=2||!Control.evolveTransferSpecies().equals("1,4,25"))
            throw new AssertionError("Evolve-before-transfer choices not applied before import");
        java.util.Properties corruptEvolve=new java.util.Properties(); corruptEvolve.setProperty("evolveTransferMode","9");
        Control.importSettings(app,corruptEvolve,9.3); Control.flushSettings(5000);
        if(Control.autoTransferEnabled()||Control.evolveTransferEnabled()||Control.evolveTransferMode()!=0||Control.evolveTransferStages()!=2||!Control.evolveTransferSpecies().equals("1,4,25"))
            throw new AssertionError("Corrupt evolve-transfer mode did not fail safe to disabled on reload");
        // Backups from before costume had its own option: "event / costume" on keeps both, off keeps neither.
        java.util.Properties oldProtect=new java.util.Properties(); oldProtect.setProperty("transferProtect","127");
        Control.importSettings(app,oldProtect,9.3); Control.flushSettings(5000);
        if(Control.transferProtect()!=255)throw new AssertionError("Old event/costume choice did not keep costumes");
        oldProtect.setProperty("transferProtect","123");
        Control.importSettings(app,oldProtect,9.3); Control.flushSettings(5000);
        if(Control.transferProtect()!=123)throw new AssertionError("Old event/costume off turned costume keeping on");
        // A current backup keeps the two choices apart, and the choice survives a reload.
        oldProtect.setProperty("transferProtect","4"); oldProtect.setProperty("transferProtectSplit","1");
        Control.importSettings(app,oldProtect,9.3); Control.flushSettings(5000);
        load.invoke(null,app);
        if(Control.transferProtect()!=4)throw new AssertionError("Event-only protection was not preserved");
        Control.setDarkTheme(false); Control.flushSettings(5000);
        Field writer=Control.class.getDeclaredField("settingsWriter"); writer.setAccessible(true);
        Object original=writer.get(null);
        writer.set(null,new SettingsWriter(Runnable::run,p->{throw new IOException("disk unavailable");},e->{}));
        try {
            Control.setDarkTheme(true);
            load.invoke(null,app);
            if(!Control.darkTheme())throw new AssertionError("Failed save allowed stale settings reload");
        } finally { writer.set(null,original); }
        System.out.println("PASS: export/import ordering, migration, evolve-transfer corrupt-config fail-safe and failed-save reload isolation");
    }
}
