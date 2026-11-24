package dev.pgrazor;

import java.util.*;
import java.util.regex.*;
import org.json.JSONObject;
import org.json.JSONException;

/** Summarizes player-supplied, filtered diagnostics. No game or account access. */
final class LocalDiagnosticSummary {
    private LocalDiagnosticSummary(){}
    static final int MAX_CHARS=2100000,MAX_LINES=30000,MAX_DISPLAY_ICONS=200;
    static final String HEADER="PGBR — diagnóstico filtrado; mensagens livres e identificadores omitidos.";
    private static final Pattern LINE=Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|[+-]\\d{2}:\\d{2}) \\[([a-z-]{1,40})\\] (\\{.*\\})$");
    private static final Pattern FOOTER=Pattern.compile("^Registros omitidos: ([0-9]{1,8})$");
    private static final Set<String> BATTLE_REASONS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("actor-unknown","actor-refresh","stale-effect","unknown-effect","unverified-slots","unavailable-pair","only-one-ready","effectiveness","known-super","off")));
    private static final Set<String> OTHER=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("runtime-gates","state-activation","panel-closed","disconnected","heartbeat-expired","route","game-routes","vgp","smart-battle","auto-raid","export-start","export-complete","export-error","log-recovery")));
    static final class Summary {
        final Map<Integer,Integer> transfers,icons;
        final Map<String,Integer> battle;
        final int omitted,records,unclassified,other;
        Summary(Map<Integer,Integer> transfers,Map<Integer,Integer> icons,Map<String,Integer> battle,int omitted,int records,int unclassified,int other){
            this.transfers=Collections.unmodifiableMap(new TreeMap<>(transfers));
            this.icons=Collections.unmodifiableMap(new TreeMap<>(icons));
            this.battle=Collections.unmodifiableMap(new TreeMap<>(battle));
            this.omitted=omitted;this.records=records;this.unclassified=unclassified;this.other=other;
        }
        String describe(){
            StringBuilder out=new StringBuilder("Registros no diagnóstico filtrado: "+records+"\nOmitidos na exportação: "+omitted+"\nOutros eventos: "+other+"\nRegistros sem classificação suficiente: "+unclassified+"\n");
            out.append("\nMotivos de preservação (ocorrências, não Pokémon únicos):\n");
            if(transfers.isEmpty())out.append("Nenhum motivo classificado.\n");
            for(Map.Entry<Integer,Integer> e:transfers.entrySet())out.append(e.getKey()).append(" — ").append(LocalAuditPolicy.reason(e.getKey())).append(": ").append(e.getValue()).append('\n');
            out.append("\nMotivos observados de combate:\n");
            if(battle.isEmpty())out.append("Nenhum motivo classificado.\n");
            for(Map.Entry<String,Integer> e:battle.entrySet())out.append(e.getKey()).append(": ").append(e.getValue()).append(" — ").append(LocalAuditPolicy.battleReason(e.getKey())).append('\n');
            out.append("\nÍcones ausentes ou com erro (ID de catálogo):\n");
            if(icons.isEmpty())out.append("Nenhum ID confirmado.\n");
            List<Map.Entry<Integer,Integer>> ranked=new ArrayList<>(icons.entrySet());
            ranked.sort((a,b)->{int n=Integer.compare(b.getValue(),a.getValue());return n!=0?n:Integer.compare(a.getKey(),b.getKey());});
            for(int i=0;i<Math.min(ranked.size(),MAX_DISPLAY_ICONS);i++){Map.Entry<Integer,Integer> e=ranked.get(i);out.append(e.getKey()).append(": ").append(e.getValue()).append('\n');}
            if(ranked.size()>MAX_DISPLAY_ICONS)out.append("Mais ").append(ranked.size()-MAX_DISPLAY_ICONS).append(" IDs distintos não exibidos. Total de IDs: ").append(ranked.size()).append('\n');
            return out.append("\nContagens não confirmam ação executada, causa individual, ausência de falhas ou funcionamento no aparelho. A exportação pode omitir campos e conter contexto repetido.").toString();
        }
    }
    static Summary parse(String raw){
        if(raw==null||raw.length()>MAX_CHARS)throw new IllegalArgumentException("Diagnóstico acima do limite.");
        String[] lines=raw.split("\\r\\n|\\n|\\r",-1);
        if(lines.length>MAX_LINES)throw new IllegalArgumentException("Excesso de registros.");
        int end=lines.length-1;while(end>0&&lines[end].trim().isEmpty())end--;
        if(end<1||!HEADER.equals(lines[0]))throw new IllegalArgumentException("Use apenas o arquivo diagnostico.txt filtrado.");
        Matcher footer=FOOTER.matcher(lines[end]);if(!footer.matches())throw new IllegalArgumentException("Diagnóstico incompleto: rodapé ausente.");
        int omitted=Integer.parseInt(footer.group(1)),records=0,unknown=0,other=0;
        Map<Integer,Integer> transfers=new TreeMap<>(),icons=new TreeMap<>();Map<String,Integer> battle=new TreeMap<>();
        for(int i=1;i<end;i++){
            records++;
            Matcher row=LINE.matcher(lines[i]);
            if(!row.matches()||row.group(2).length()>4096){unknown++;continue;}
            try{
                JSONObject data=new JSONObject(row.group(2));String kind=row.group(1);
                if("transfer-kept".equals(kind)){
                    Integer n=number(data.opt("reasonCode"),0,20);if(n==null)unknown++;else increment(transfers,n);
                }else if("battle-action".equals(kind)){
                    Object reason=data.opt("reason");if(!(reason instanceof String)||!BATTLE_REASONS.contains(reason))unknown++;else increment(battle,(String)reason);
                }else if("item-icon-missing".equals(kind)||"item-icon-error".equals(kind)){
                    Integer n=number(data.opt("itemId"),1,999999);if(n==null)unknown++;else increment(icons,n);
                }else if(OTHER.contains(kind))other++;else unknown++;
            }catch(JSONException e){unknown++;}
        }
        return new Summary(transfers,icons,battle,omitted,records,unknown,other);
    }
    private static Integer number(Object v,int min,int max){
        if(!(v instanceof Number))return null;
        double x=((Number)v).doubleValue();
        return Double.isFinite(x)&&x==Math.rint(x)&&x>=min&&x<=max?(int)x:null;
    }
    private static <K> void increment(Map<K,Integer> map,K key){map.put(key,map.getOrDefault(key,0)+1);}
}
