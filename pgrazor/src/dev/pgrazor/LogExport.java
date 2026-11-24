package dev.pgrazor;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.security.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;
import org.json.JSONException;

/** Verify exports through the document URI; its backing folder is provider-owned. */
public final class LogExport {
    public static void save(Context context,Uri uri,File file) throws IOException {
        save(context,uri,file,false);
    }
    static byte[] settingsJson(Properties preferences,double speedKmh) throws IOException {
        return settingsJson(preferences,speedKmh,new JSONObject());
    }
    private static byte[] settingsJson(Properties preferences,double speedKmh,JSONObject groups) throws IOException {
        try {
            JSONObject settings=new JSONObject();
            for(String key:new TreeSet<>(preferences.stringPropertyNames()))settings.put(key,preferences.getProperty(key));
            JSONObject root=new JSONObject();
            root.put("format","PGBR diagnostic settings");root.put("version",1);
            root.put("appVersion",BuildVersion.NAME);
            root.put("capturedAtEpochMs",System.currentTimeMillis());
            root.put("speedKmh",speedKmh);root.put("settings",settings);
            root.put("preferences",groups);
            return root.toString().getBytes(StandardCharsets.UTF_8);
        } catch(JSONException error) { throw new IOException("Cannot capture settings",error); }
    }
    private static final String[] SAFE_SETTINGS={"smartAttack","smartBattle","smartCatchEnabled","smartCatchPremium","smartCatchShinyOnly","smartCatchRareCustom","smartCatchManualBerry","smartCatchRareMask","smartCatchRareIvMin","smartCatchRareBerry","smartCatchTagBerry","transferProtect","transferProtectSplit","transferKeepIv","rocketOptions","pvpBattleOptions","smartShieldTiming","smartFaintedSwapTiming"};
    static Properties shareSettings(Properties original){
        Properties clean=new Properties();
        for(String key:SAFE_SETTINGS){
            String v=original.getProperty(key);
            if(v!=null&&v.matches("true|false|[0-9]{1,5}"))clean.setProperty(key,v);
        }
        return clean;
    }
    private static final java.util.regex.Pattern LOG_LINE=java.util.regex.Pattern.compile("^(?:\\[retained-context\\] )?(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2})) \\[([a-z-]{1,40})\\] (.*)$");
    private static final java.util.Set<String> SAFE_KINDS=new java.util.HashSet<>(java.util.Arrays.asList("transfer-kept","battle-action","runtime-gates","state-activation","panel-closed","disconnected","heartbeat-expired","route","game-routes","vgp","smart-battle","auto-raid","export-start","export-complete","export-error","log-recovery","item-icon-missing","item-icon-error"));
    private static final String[] SAFE_BOOLS={"smartAttackEnabled","classifierBound","chargeSeen","charge2Seen","chargeReady","charge2Ready","pvpExecutorReady","pvpObserved","vsSeekerPvPProbeReady","pvpTypeProbeReady"};
    private static final String[] SAFE_ENUMS={"smartAttackMode","actorIdentity","decision","reason","chargeTier","charge2Tier","mode","pvpScene","actorProbe"};
    private static final java.util.Set<String> SAFE_VALUES=new java.util.HashSet<>(java.util.Arrays.asList("rocket","pvp","training","unknown","not-observed","known","verified","hold","selected","select","off","on","super","neutral","normal","resisted","actor-unknown","actor-refresh","stale-effect","unknown-effect","unverified-slots","unavailable-pair","only-one-ready","effectiveness","known-super","hub","preparation","candidate","training-entry","scope-ended","vnext-closed","battle-ended","excluded-pve-scope"));
    static String displayLog(byte[] bytes,boolean privateMode)throws IOException{
        return new String(privateMode?shareLog(new ByteArrayInputStream(bytes)):bytes,StandardCharsets.UTF_8);
    }
    static byte[] shareLog(InputStream input)throws IOException{
        // Read bounded bytes, then discard all non-allowlisted free text.
        ByteArrayOutputStream raw=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=input.read(buffer))!=-1){if(raw.size()+n>2097152)throw new IOException("Log acima do limite de compartilhamento.");raw.write(buffer,0,n);}
        StringBuilder out=new StringBuilder("PGBR — diagnóstico filtrado; mensagens livres e identificadores omitidos.\n");
        int omitted=0;
        for(String line:new String(raw.toByteArray(),StandardCharsets.UTF_8).split("\\r?\\n")){
            java.util.regex.Matcher m=LOG_LINE.matcher(line);
            if(!m.matches()||!SAFE_KINDS.contains(m.group(2))){omitted++;continue;}
            JSONObject safe=new JSONObject();
            try{
                // Item icon messages use a fixed leading catalog ID, never a player identifier.
                // Discard the rest, including any path or exception message.
                if("item-icon-missing".equals(m.group(2))||"item-icon-error".equals(m.group(2))){
                    java.util.regex.Matcher id=java.util.regex.Pattern.compile("^id=([1-9][0-9]{0,5})(?:$| .*)").matcher(m.group(3));
                    if(id.matches())safe.put("itemId",Integer.parseInt(id.group(1)));
                }else{
                JSONObject data=new JSONObject(m.group(3));
                for(String key:SAFE_BOOLS){Object v=data.opt(key);if(v instanceof Boolean)safe.put(key,v);}
                for(String key:SAFE_ENUMS){Object v=data.opt(key);if(v instanceof String&&SAFE_VALUES.contains(v))safe.put(key,v);}
                for(String key:new String[]{"reasonCode","chargeCount","species","cp"}){
                    Object v=data.opt(key);if(v instanceof Number){double x=((Number)v).doubleValue();if(Double.isFinite(x)&&x==Math.rint(x)&&x>=0&&x<=100000)safe.put(key,(int)x);}
                }
                }
            }catch(JSONException ignored){}
            out.append(m.group(1)).append(" [").append(m.group(2)).append("] ").append(safe.toString()).append('\n');
        }
        out.append("Registros omitidos: ").append(omitted).append('\n');
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static JSONObject preferenceGroups(Context context)throws IOException {
        String[] scopes={"pgrazor-map","pgrazor-overlays","pgrazor-livefeed","pgrazor-pogofeed"};
        String[][] keys={{"Stops","Gyms","Raids"},
            {"cooldown.fx","cooldown.fy","vgp.fx","vgp.fy","transfer.fx","transfer.fy","gym-feed.fx","gym-feed.fy","local-time.fx","local-time.fy"},
            {"category","radius","minIv","myShinies","filters_v1"},
            {"feedMode","radiusKm","refreshIntervalIndex","filters_v1"}};
        try {
            JSONObject groups=new JSONObject();
            for(int i=0;i<scopes.length;i++) {
                Map<String,?> all=AccountProfiles.preferences(context,scopes[i]).getAll();
                JSONObject group=new JSONObject();
                for(String key:keys[i])if(all.containsKey(key))group.put(key,all.get(key));
                groups.put(scopes[i],group);
            }
            JSONObject events=new JSONObject();
            events.put("halloween_2026.box_collector",dev.pgrazor.events.EventSettings.boxCollectorRequested(context));
            events.put("go_wild_november_2026.vgp_plus_safari",dev.pgrazor.events.EventSettings.wildVgpSafari(context));
            events.put("go_wild_november_2026.auto_ball_safari",dev.pgrazor.events.EventSettings.wildAutoBallSafari(context));
            groups.put("pgrazor-events-v1",events);
            return groups;
        } catch(JSONException error) { throw new IOException("Cannot capture preferences",error); }
    }
    static void bundle(InputStream log,String logName,byte[] settings,OutputStream destination) throws IOException {
        // The caller owns the provider stream; close the compressor without closing that stream.
        try(ZipOutputStream zip=new ZipOutputStream(new FilterOutputStream(destination) {
            @Override public void close()throws IOException{flush();}
        })) {
            zip.putNextEntry(new ZipEntry(logName));
            byte[] buffer=new byte[8192];int n;
            while((n=log.read(buffer))!=-1)zip.write(buffer,0,n);
            zip.closeEntry();zip.putNextEntry(new ZipEntry("settings.json"));
            zip.write(settings);zip.closeEntry();zip.finish();zip.flush();
        }
    }
    public static void save(Context context,Uri uri,File file,boolean includeSettings) throws IOException {
        if(uri==null || file==null) throw new IOException("No log or document selected");
        if(!includeSettings&&Control.trainerPrivacyMode()!=0)throw new IOException("TXT bruto bloqueado no modo de privacidade.");
        try {
            byte[] settings=includeSettings?settingsJson(shareSettings(Control.diagnosticSettings()),Control.engine.speedKmh,new JSONObject()):null;
            MessageDigest written=MessageDigest.getInstance("SHA-256");
            MessageDigest checked=MessageDigest.getInstance("SHA-256");
            long count=0,verified=0;
            Control.event("export-start","document-provider");
            try(InputStream in=new FileInputStream(file); OutputStream out=context.getContentResolver().openOutputStream(uri,"wt")) {
                if(out==null) throw new IOException("Document provider returned no output stream");
                // Digest the actual archive bytes, including the ZIP directory, before read-back verification.
                CountingOutputStream counted=new CountingOutputStream(new DigestOutputStream(out,written));
                if(includeSettings)bundle(new ByteArrayInputStream(shareLog(in)),"diagnostico.txt",settings,counted);
                else {
                    byte[] buffer=new byte[8192]; int n;
                    while((n=in.read(buffer))!=-1)counted.write(buffer,0,n);
                }
                count=counted.count;
                out.flush();
            }
            try(InputStream in=context.getContentResolver().openInputStream(uri)) {
                if(in==null) throw new IOException("Document provider returned no verification stream");
                byte[] buffer=new byte[8192]; int n;
                while((n=in.read(buffer))!=-1) { checked.update(buffer,0,n); verified+=n; }
            }
            if(count!=verified || !Arrays.equals(written.digest(),checked.digest())) throw new IOException("Saved document verification failed");
            Control.event("export-complete","verifiedBytes="+count);
        } catch(NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
        catch(IOException error) { Control.event("export-error","io-failure"); throw error; }
    }

    /**
     * PGBR Root has no document picker (the run log is shown as a dialog, not a private activity),
     * so save a verified copy into the game's own app-visible external files directory instead. The
     * same two-pass digest check guards the written file; the destination path is returned so the
     * dialog can tell the user where to find it. No new storage permission is required.
     */
    public static File copyToExternal(Context context,File file) throws IOException {
        return copyToExternal(context,file,false);
    }
    public static File copyToExternal(Context context,File file,boolean includeSettings) throws IOException {
        if(file==null) throw new IOException("No log is available");
        if(!includeSettings&&Control.trainerPrivacyMode()!=0)throw new IOException("TXT bruto bloqueado no modo de privacidade.");
        File dir=context.getExternalFilesDir("pgrazor-logs");
        if(dir==null) dir=new File(context.getFilesDir(),"pgrazor-logs/exports");
        if(!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create export folder");
        // A unique temporary file avoids collisions with parallel exports. Never delete a previous copy.
        File temp=File.createTempFile("pgbr-diagnostic-", ".tmp",dir);
        File dest=new File(dir,temp.getName().replaceFirst("\\.tmp$",includeSettings?".zip":".txt"));
        try {
            byte[] settings=includeSettings?settingsJson(shareSettings(Control.diagnosticSettings()),Control.engine.speedKmh,new JSONObject()):null;
            MessageDigest written=MessageDigest.getInstance("SHA-256");
            MessageDigest checked=MessageDigest.getInstance("SHA-256");
            long count=0;
            Control.event("export-start","local-copy");
            try(InputStream in=new FileInputStream(file); OutputStream out=new FileOutputStream(temp)) {
                CountingOutputStream counted=new CountingOutputStream(new DigestOutputStream(out,written));
                if(includeSettings)bundle(new ByteArrayInputStream(shareLog(in)),"diagnostico.txt",settings,counted);
                else {
                    byte[] buffer=new byte[8192]; int n;
                    while((n=in.read(buffer))!=-1)counted.write(buffer,0,n);
                }
                count=counted.count;
                out.flush();
            }
            verifyAndPublish(temp,dest,count,written.digest(),checked);
            Control.event("export-complete","verifiedBytes="+count);
            return dest;
        } catch(NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
        catch(IOException error) { Control.event("export-error","io-failure"); throw error; }
        finally{if(temp.exists())temp.delete();}
    }
    /** Verify before exposing the final name; never remove an existing export. */
    static void verifyAndPublish(File temp,File dest,long count,byte[] digest,MessageDigest checked)throws IOException{
        if(temp.equals(dest)||dest.exists()||!temp.getCanonicalFile().getParentFile().equals(dest.getCanonicalFile().getParentFile()))throw new IOException("Export destination conflict");
        long verified=0;checked.reset();
        try(InputStream in=new FileInputStream(temp)){
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){checked.update(buffer,0,n);verified+=n;}
        }
        if(count!=verified||!MessageDigest.isEqual(digest,checked.digest()))throw new IOException("Saved copy verification failed");
        if(dest.exists()||!temp.renameTo(dest))throw new IOException("Could not finish writing the export");
    }
    private static final class CountingOutputStream extends FilterOutputStream {
        long count;
        CountingOutputStream(OutputStream out){super(out);}
        @Override public void write(int value)throws IOException{out.write(value);count++;}
        @Override public void write(byte[] bytes,int offset,int length)throws IOException{out.write(bytes,offset,length);count+=length;}
    }
}
