package dev.pgbr.geolab;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.util.*;
/** Strict, bounded parser for the flat GeoLab fixture schema; no arbitrary JSON execution. */
public final class JsonParser {
    private JsonParser(){}
    private static final Set<String> ENTITY_KEYS=Collections.unmodifiableSet(new HashSet<>(Arrays.asList("id","lat","lon","status","value")));
    private static final java.util.regex.Pattern NUMBER=java.util.regex.Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");
    public static List<Entity> entities(byte[] data) {
        Cursor c=new Cursor(data); List<Entity> result=new ArrayList<>();
        c.take('[');
        if(!c.peek(']')) do {
            if(result.size()>=10000) throw c.bad();
            Map<String,String> m=c.object();
            if(!m.keySet().equals(ENTITY_KEYS)) throw c.bad();
            result.add(new Entity(c.str(m,"id"),c.num(m,"lat"),c.num(m,"lon"),c.str(m,"status"),c.num(m,"value")));
        } while(c.optional(','));
        c.take(']'); c.end(); return Collections.unmodifiableList(result);
    }
    public static FilterEngine.Rule rule(byte[] data) {
        Cursor c=new Cursor(data); Map<String,String> m=c.object(); c.end();
        if(!m.containsKey("status") || !m.containsKey("minValue") || !Arrays.asList("status","minValue","comparison","junction").containsAll(m.keySet())) throw c.bad();
        try {
            return new FilterEngine.Rule(c.str(m,"status"),c.num(m,"minValue"),
                m.containsKey("comparison")?FilterEngine.Rule.Comparison.valueOf(c.str(m,"comparison")):FilterEngine.Rule.Comparison.GE,
                m.containsKey("junction")?FilterEngine.Rule.Junction.valueOf(c.str(m,"junction")):FilterEngine.Rule.Junction.AND);
        } catch(IllegalArgumentException invalid){throw c.bad();}
    }
    private static final class Cursor {
        final String s; int p;
        Cursor(byte[] bytes){
            if(bytes==null || bytes.length>1024*1024) throw new IllegalArgumentException("Invalid payload size");
            try {s=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
            catch(CharacterCodingException e){throw new IllegalArgumentException("Invalid UTF-8",e);}
        }
        IllegalArgumentException bad(){return new IllegalArgumentException("Invalid fixture JSON at "+p);}
        void ws(){while(p<s.length() && " \n\r\t".indexOf(s.charAt(p))>=0)p++;}
        boolean peek(char ch){ws();return p<s.length()&&s.charAt(p)==ch;}
        boolean optional(char ch){if(peek(ch)){p++;return true;}return false;}
        void take(char ch){if(!optional(ch))throw bad();}
        void end(){ws();if(p!=s.length())throw bad();}
        String string(){take('"');int start=p;while(p<s.length() && s.charAt(p)!='"'){
            char ch=s.charAt(p++); if(ch<32||ch=='\\'||p-start>64)throw bad();
        } if(p==s.length())throw bad();return s.substring(start,p++);}
        String number(){ws();int start=p;while(p<s.length()&&"-+0123456789.eE".indexOf(s.charAt(p))>=0){p++;if(p-start>64)throw bad();}String v=s.substring(start,p);if(!NUMBER.matcher(v).matches())throw bad();return v;}
        Map<String,String> object(){Map<String,String> m=new HashMap<>();take('{');if(!peek('}'))do{
            String key=string();take(':');String value=peek('"')?"s"+string():"n"+number();
            if(m.put(key,value)!=null || m.size()>8)throw bad();
        }while(optional(','));take('}');return m;}
        String str(Map<String,String> m,String k){String v=m.get(k);if(v==null||!v.startsWith("s"))throw bad();return v.substring(1);}
        double num(Map<String,String> m,String k){String v=m.get(k);if(v==null||!v.startsWith("n"))throw bad();double n=Double.parseDouble(v.substring(1));if(!Double.isFinite(n))throw bad();return n;}
    }
}
