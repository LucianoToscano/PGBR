package dev.pgrazor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

public final class LogExportTest {
    public static void main(String[] args)throws Exception {
        Properties settings=new Properties();
        settings.setProperty("smartCatchEnabled","true");
        settings.setProperty("escaped","quotes\" and newline\nUnicode \u2603");
        byte[] json=LogExport.settingsJson(settings,9.5);
        String line=new String(json,StandardCharsets.UTF_8);
        if(line.contains("\n")||line.contains("\r"))throw new AssertionError("Not single line");
        JSONObject parsed=new JSONObject(line);
        if(!parsed.getJSONObject("settings").getString("escaped").equals(settings.getProperty("escaped")))throw new AssertionError("JSON escaping");
        if(parsed.getDouble("speedKmh")!=9.5)throw new AssertionError("Speed");
        byte[] log=new byte[25000];new Random(42).nextBytes(log);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        LogExport.bundle(new ByteArrayInputStream(log),"session.txt",json,bytes);
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            for(String name:new String[]{"session.txt","settings.json"}) {
                ZipEntry entry=zip.getNextEntry();
                if(entry==null||!name.equals(entry.getName()))throw new AssertionError("Entry name");
                ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;
                while((n=zip.read(buffer))!=-1)data.write(buffer,0,n);
                if(!Arrays.equals(data.toByteArray(),name.endsWith(".txt")?log:json))throw new AssertionError("Archive content");
            }
            if(zip.getNextEntry()!=null)throw new AssertionError("Unexpected entry");
        }
        settings.setProperty("apiKey","secret-example");
        settings.setProperty("trainerName","private-example");
        settings.setProperty("smartAttack","true");
        Properties safeSettings=LogExport.shareSettings(settings);
        if(!"true".equals(safeSettings.getProperty("smartAttack"))||safeSettings.containsKey("apiKey")||safeSettings.containsKey("trainerName")||safeSettings.containsKey("escaped"))throw new AssertionError("Settings privacy");
        String raw="2026-11-24T01:10:00.000-03:00 [transfer-kept] {\"reasonCode\":12,\"id\":\"private-example\",\"species\":25}\n"
            +"2026-11-24T01:10:01.000-03:00 [battle-action] {\"smartAttackEnabled\":true,\"reason\":\"actor-unknown\",\"token\":\"secret-example\"}\n"
            +"2026-11-24T01:10:02.000-03:00 [login] secret-example\n"
            +"2026-11-24T01:10:03.000-03:00 [item-icon-missing] id=708\n"
            +"2026-11-24T01:10:04.000-03:00 [item-icon-error] id=1401 asset=private-example cause=secret-example\n"
            +"2026-11-24T01:10:05.000-03:00 [item-icon-error] id=999999999 private-example\n";
        String safe=new String(LogExport.shareLog(new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8))),StandardCharsets.UTF_8);
        if(safe.contains("secret-example")||safe.contains("private-example")||!safe.contains("\"reasonCode\":12")||!safe.contains("actor-unknown"))throw new AssertionError("Log privacy");
        if(!safe.contains("\"itemId\":708")||!safe.contains("\"itemId\":1401")||safe.contains("999999999"))throw new AssertionError("Icon diagnostic privacy");
        try{LocalDiagnosticSummary.parse("\n".repeat(LocalDiagnosticSummary.MAX_LINES));throw new AssertionError("Line preflight");}catch(IllegalArgumentException expected){if(!expected.getMessage().equals("Excesso de registros."))throw expected;}
        LocalDiagnosticSummary.Summary summary=LocalDiagnosticSummary.parse(safe);
        LocalDiagnosticSummary.Summary crlf=LocalDiagnosticSummary.parse(safe.replace("\n","\r\n"));
        if(crlf.records!=summary.records||crlf.omitted!=summary.omitted||!crlf.icons.equals(summary.icons))throw new AssertionError("CRLF summary");
        if(summary.records!=5||summary.omitted!=1||summary.unclassified!=1||summary.transfers.get(12)!=1||summary.battle.get("actor-unknown")!=1||summary.icons.get(708)!=1||summary.icons.get(1401)!=1)throw new AssertionError("Filtered diagnostic summary");
        if(summary.describe().contains("private-example")||summary.describe().contains("secret-example")||!summary.describe().contains("não Pokémon únicos"))throw new AssertionError("Summary privacy or caveat");
        try{LocalDiagnosticSummary.parse(safe.substring(0,safe.indexOf("Registros omitidos:")));throw new AssertionError("Truncated diagnostic");}catch(IllegalArgumentException expected){}
        try{LocalDiagnosticSummary.parse(raw);throw new AssertionError("Raw diagnostic accepted");}catch(IllegalArgumentException expected){}
        String invalid=LocalDiagnosticSummary.HEADER+"\n2026-11-24T01:10:00.000-03:00 [transfer-kept] {\"reasonCode\":\"12\"}\n"
          +"2026-11-24T01:10:00.000-03:00 [battle-action] {\"reason\":\"secret-example\"}\nRegistros omitidos: 0\n";
        LocalDiagnosticSummary.Summary bad=LocalDiagnosticSummary.parse(invalid);
        if(bad.unclassified!=2||!bad.transfers.isEmpty()||!bad.battle.isEmpty()||bad.describe().contains("secret-example"))throw new AssertionError("Untrusted values");
        StringBuilder many=new StringBuilder(LocalDiagnosticSummary.HEADER).append('\n');
        for(int i=1;i<=201;i++)many.append("2026-11-24T01:10:00.000-03:00 [item-icon-error] {\"itemId\":").append(i).append("}\n");
        many.append("Registros omitidos: 0\n");
        LocalDiagnosticSummary.Summary limited=LocalDiagnosticSummary.parse(many.toString());
        if(limited.icons.size()!=201||!limited.describe().contains("Mais 1 IDs distintos não exibidos"))throw new AssertionError("Summary display limit");
        // Test new import limits without mutating profile storage or old archives.
        JSONObject migration=new JSONObject();
        migration.put("format",PgSharpDataImport.FORMAT).put("version",1);
        for(String k:new String[]{"favorites","pokemonFilters","questFilters","raidFilters"})migration.put(k,new org.json.JSONArray());
        org.json.JSONArray routes=new org.json.JSONArray(),points=new org.json.JSONArray();
        org.json.JSONArray point=new org.json.JSONArray().put(1).put(2);
        for(int i=0;i<GpxCodec.MAX_POINTS;i++)points.put(point);
        for(int i=0;i<GpxCodec.MAX_TOTAL_POINTS/GpxCodec.MAX_POINTS;i++)routes.put(new JSONObject().put("name","R"+i).put("points",points));
        migration.put("routes",routes);
        PgSharpDataImport.validateNewImport(migration);
        routes.put(new JSONObject().put("name","Extra").put("points",new org.json.JSONArray().put(point)));
        PgSharpDataImport.validate(migration);
        try{PgSharpDataImport.validateNewImport(migration);throw new AssertionError("Aggregate migration limit");}catch(org.json.JSONException expected){
            if(!expected.getMessage().contains("Total de pontos"))throw expected;
        }
        java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("pgbr-export-test");
        File tmp=dir.resolve("current.tmp").toFile(),old=dir.resolve("old.zip").toFile(),dest=dir.resolve("current.zip").toFile();
        try{
            java.nio.file.Files.write(old.toPath(),new byte[]{7});
            java.nio.file.Files.write(tmp.toPath(),log);
            byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(log);
            java.security.MessageDigest verify=java.security.MessageDigest.getInstance("SHA-256");
            try{LogExport.verifyAndPublish(tmp,old,log.length,hash,verify);throw new AssertionError("Overwrite");}catch(IOException expected){}
            try{LogExport.verifyAndPublish(tmp,dest,log.length+1,hash,verify);throw new AssertionError("Invalid length");}catch(IOException expected){}
            byte[] wrong=hash.clone();wrong[0]^=1;
            try{LogExport.verifyAndPublish(tmp,dest,log.length,wrong,verify);throw new AssertionError("Invalid digest");}catch(IOException expected){}
            if(dest.exists()||!tmp.exists()||java.nio.file.Files.readAllBytes(old.toPath())[0]!=7)throw new AssertionError("Previous export preservation");
            LogExport.verifyAndPublish(tmp,dest,log.length,hash,verify);
            if(tmp.exists()||!Arrays.equals(log,java.nio.file.Files.readAllBytes(dest.toPath())))throw new AssertionError("Verified publication");
        }finally{tmp.delete();old.delete();dest.delete();java.nio.file.Files.deleteIfExists(dir);}
        System.out.println("PASS compact JSON escaping and ZIP log/settings byte-exact round trip");
    }
}
