package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Small app-private destination library. Call persistence methods off the UI thread. */
public final class SavedPlaces {
    public static final int LIMIT = 200;
    public static final int NAME_LIMIT = 100;
    public static final String[] CATEGORIES=Entry.CATEGORIES;
    private static final int JSON_LIMIT = 64 * 1024;
    private final SharedPreferences preferences;

    public static final class Entry {
        // Isolated from the JSON-backed outer class so desktop Java validation remains independent of Android.
        public static final String[] CATEGORIES={"Geral","Top","Farm","Missões","Rotas","Eventos","Outros"};
        public final String id, name, category;
        public final double latitude, longitude;
        public final long createdAtMs;
        public Entry(String name,double latitude,double longitude){
            this(name,latitude,longitude,"Geral");
        }
        public Entry(String name,double latitude,double longitude,String category){
            this(UUID.randomUUID().toString(),name,latitude,longitude,category,System.currentTimeMillis());
        }
        private Entry(String id,String name,double latitude,double longitude,
                      String category,long createdAtMs){
            if(id==null||id.isEmpty()||id.length()>64)throw new IllegalArgumentException("ID inválido");
            if(name==null||name.trim().isEmpty())throw new IllegalArgumentException("Informe o nome do local");
            name=name.trim();
            if(name.length()>NAME_LIMIT)throw new IllegalArgumentException("Nome com até 100 caracteres");
            for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i)))
                throw new IllegalArgumentException("Nome com caractere inválido");
            if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||latitude < -90
                ||latitude>90||longitude < -180||longitude>180)
                throw new IllegalArgumentException("Coordenadas inválidas");
            boolean accepted=false;
            for(String permitted:CATEGORIES)if(permitted.equals(category)){accepted=true;break;}
            if(!accepted)category="Geral"; // keep older/foreign records, never silently drop locations
            this.id=id;this.name=name;this.latitude=latitude;this.longitude=longitude;
            this.category=category;this.createdAtMs=Math.max(0,createdAtMs);
        }
    }
    public static final class Snapshot {
        public final List<Entry> entries;
        public final boolean damaged;
        Snapshot(List<Entry> entries, boolean damaged) { this.entries = Collections.unmodifiableList(entries); this.damaged = damaged; }
    }

    public SavedPlaces(Context context) { preferences = AccountProfiles.preferences(context.getApplicationContext(),"pgrazor-saved-places-v1"); }

    public synchronized Snapshot read() {
        List<Entry> entries = new ArrayList<>();
        boolean damaged = false;
        try {
            String raw = preferences.getString("places", "[]");
            if (raw == null || raw.length() > JSON_LIMIT) return new Snapshot(entries, true);
            JSONArray array = new JSONArray(raw);
            Set<String> names = new HashSet<>(), ids = new HashSet<>();
            if (array.length() > LIMIT) damaged = true;
            // Inspect at most LIMIT records even when a corrupt document advertises a larger collection.
            for (int i = 0; i < Math.min(array.length(), LIMIT); i++) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    Entry entry = new Entry(object.getString("id"), object.getString("name"), object.getDouble("latitude"), object.getDouble("longitude"),
                        object.optString("category","Geral"),object.optLong("createdAtMs",0));
                    String normalized = entry.name.toLowerCase(Locale.ROOT);
                    if (ids.contains(entry.id) || names.contains(normalized)) { damaged = true; continue; }
                    ids.add(entry.id); names.add(normalized); entries.add(entry);
                } catch (JSONException | IllegalArgumentException e) { damaged = true; }
            }
        } catch (JSONException | ClassCastException e) { damaged = true; }
        return new Snapshot(entries, damaged);
    }

    public synchronized void add(Entry entry) throws IOException {
        List<Entry> entries = new ArrayList<>(read().entries);
        if (entries.size() >= LIMIT) throw new IllegalArgumentException("Você atingiu o limite de 200 favoritos. Exclua um antes de adicionar outro.");
        for (Entry existing : entries) if (existing.name.equalsIgnoreCase(entry.name)) throw new IllegalArgumentException("A place with this name already exists. Choose another name.");
        entries.add(entry);
        write(entries);
    }

    /** Import an all-or-nothing list, retaining existing entries and skipping repeated names (distinct labels may share a location). */
    synchronized int merge(List<Entry> additions) throws IOException {
        Snapshot snapshot=read();
        if(snapshot.damaged)throw new IOException("Existing favorites are damaged.");
        List<Entry> merged=new ArrayList<>(snapshot.entries);
        Set<String> names=new HashSet<>();
        for(Entry e:merged)names.add(e.name.toLowerCase(Locale.ROOT));
        int added=0;
        for(Entry e:additions) {
            if(e==null)throw new IOException("Invalid imported favorite");
            String n=e.name.toLowerCase(Locale.ROOT);
            if(names.contains(n))continue;
            merged.add(e);names.add(n);added++;
        }
        if(merged.size()>LIMIT)throw new IOException("Too many saved places");
        if(added>0)write(merged);
        return added;
    }

    /** Roll back an importer without deleting favorites that existed before the import. */
    synchronized void restoreSnapshot(Snapshot snapshot) throws IOException {
        if(snapshot==null||snapshot.damaged)throw new IOException("Invalid favorites rollback");
        write(new ArrayList<>(snapshot.entries));
    }

    public synchronized boolean remove(String id) throws IOException {
        List<Entry> entries = new ArrayList<>(read().entries);
        boolean removed = false;
        for (int i = entries.size() - 1; i >= 0; i--) if (entries.get(i).id.equals(id)) { entries.remove(i); removed = true; }
        if (removed) write(entries);
        return removed;
    }

    private void write(List<Entry> entries) throws IOException {
        JSONArray array = new JSONArray();
        try {
            for (Entry entry : entries) {
                JSONObject object = new JSONObject();
                object.put("id", entry.id); object.put("name", entry.name); object.put("latitude", entry.latitude); object.put("longitude", entry.longitude);
                object.put("category",entry.category);object.put("createdAtMs",entry.createdAtMs);
                array.put(object);
            }
        } catch (JSONException e) { throw new IOException("Could not encode saved places.", e); }
        String encoded = array.toString();
        if (encoded.length() > JSON_LIMIT) throw new IOException("Saved places exceed the storage limit.");
        SharedPreferences.Editor editor = preferences.edit();
        // Preserve one bounded copy of a damaged document before replacing it with salvaged valid entries.
        if (read().damaged) {
            try {
                String raw = preferences.getString("places", "[]");
                if (raw != null && raw.length() <= JSON_LIMIT) editor.putString("recovered-from", raw);
            } catch (ClassCastException ignored) { }
        }
        if (!editor.putString("places", encoded).commit()) throw new IOException("Could not save places to device storage. Please retry.");
    }
}
