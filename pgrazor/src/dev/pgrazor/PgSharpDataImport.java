package dev.pgrazor;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Imports a user-converted PGSharp data snapshot. Never deserializes a Java .dat file,
 * contacts PGSharp servers, changes battle options or executes a teleport.
 * Raw filter definitions are retained verbatim for future, explicit provider mapping;
 * they are NOT represented as live-feed compatible or enabled automatically.
 */
final class PgSharpDataImport {
    static final String FORMAT = "PGBR PGSharp migration";
    static final String SCOPE = "pgrazor-pgsharp-data-v1";
    static final String KEY = "migration_json";
    private PgSharpDataImport() {}

    static final class Report {
        final int favoritesAdded, favoritesSkipped, routesAdded, routesSkipped;
        final int pokemonFilters, questFilters, raidFilters;
        Report(int fa, int fs, int ra, int rs, int pf, int qf, int rf) {
            favoritesAdded=fa; favoritesSkipped=fs; routesAdded=ra; routesSkipped=rs;
            pokemonFilters=pf; questFilters=qf; raidFilters=rf;
        }
        String summary() {
            return "Importação concluída. " + favoritesAdded + " favoritos e "
                + routesAdded + " rotas adicionados; " + favoritesSkipped
                + " favoritos e " + routesSkipped + " rotas já existentes. "
                + pokemonFilters + " filtros Pokémon, " + questFilters
                + " filtros de pesquisas e " + raidFilters
                + " filtros de reides preservados para compatibilização. "
                + "Nenhum filtro de servidor, teleporte ou automação foi ativado.";
        }
    }

    private static void check(boolean condition, String error) throws JSONException {
        if(!condition) throw new JSONException(error);
    }
    private static JSONArray bounded(JSONObject root, String name, int max) throws JSONException {
        JSONArray array=root.getJSONArray(name);
        check(array.length()<=max, "Muitos registros em " + name);
        for(int i=0;i<array.length();i++)check(array.optJSONObject(i)!=null,
            "Registro inválido em " + name + ": " + (i+1));
        return array;
    }
    static void validate(JSONObject root) throws JSONException {
        check(root!=null && FORMAT.equals(root.optString("format")), "Arquivo não é uma migração PGSharp do PGBR");
        check(root.optInt("version",-1)==1, "Versão de migração incompatível");
        JSONArray favorites=bounded(root,"favorites",200);
        JSONArray routes=bounded(root,"routes",100);
        JSONArray pokemon=bounded(root,"pokemonFilters",500);
        JSONArray quests=bounded(root,"questFilters",200);
        JSONArray raids=bounded(root,"raidFilters",100);
        for(int i=0;i<favorites.length();i++){
            JSONObject f=favorites.getJSONObject(i);
            check(f.has("name") && f.has("lat") && f.has("lng"),"Favorito incompleto");
            check(f.get("lat") instanceof Number && f.get("lng") instanceof Number,
                "Coordenadas inválidas em favorito");
            double lat=f.getDouble("lat"), lon=f.getDouble("lng");
            check(Double.isFinite(lat)&&Math.abs(lat)<=90&&Double.isFinite(lon)&&Math.abs(lon)<=180,
                "Coordenadas fora de intervalo");
        }
        for(int i=0;i<routes.length();i++){
            JSONObject route=routes.getJSONObject(i);
            check(route.has("name"),"Rota sem nome");
            JSONArray points=route.getJSONArray("points");
            check(points.length()>0 && points.length()<=GpxCodec.MAX_POINTS,"Rota sem pontos ou excessiva");
            for(int j=0;j<points.length();j++){
                JSONArray row=points.getJSONArray(j);
                check(row.length()>=2 && row.get(0) instanceof Number && row.get(1) instanceof Number,
                    "Ponto de rota inválido");
                double lat=row.getDouble(0),lon=row.getDouble(1);
                check(Double.isFinite(lat)&&Math.abs(lat)<=90&&Double.isFinite(lon)&&Math.abs(lon)<=180,
                    "Ponto de rota fora de intervalo");
            }
        }
        for(JSONArray list:new JSONArray[]{pokemon,quests,raids})
            for(int i=0;i<list.length();i++)
                check(list.getJSONObject(i).has("name"),"Filtro sem nome");
    }

    static JSONObject stored(Context context) {
        String raw=AccountProfiles.preferences(context,SCOPE).getString(KEY,null);
        if(raw==null)return null;
        try { JSONObject result=new JSONObject(raw); validate(result); return result; }
        catch(Exception ignored){ return null; }
    }
    static String storedSummary(Context context) {
        JSONObject d=stored(context);
        if(d==null)return "Nenhum pacote PGSharp importado para esta conta.";
        return "Dados PGSharp preservados neste perfil:\n"
            + d.optJSONArray("favorites").length() + " favoritos originais\n"
            + d.optJSONArray("routes").length() + " rotas originais\n"
            + d.optJSONArray("pokemonFilters").length() + " filtros de Pokémon\n"
            + d.optJSONArray("questFilters").length() + " filtros de pesquisas\n"
            + d.optJSONArray("raidFilters").length() + " filtros de reides\n\n"
            + "Os filtros PGSharp originais foram arquivados, mas não são consultas ativas: "
            + "os serviços de dados e seus campos precisam ser compatibilizados separadamente.";
    }

    /** Validate and preflight the complete request before touching any profile storage. */
    static synchronized Report importData(Context context, JSONObject root) throws Exception {
        validate(root);
        SavedPlaces places=new SavedPlaces(context);
        SavedPlaces.Snapshot before=places.read();
        if(before.damaged)throw new IOException("Há favoritos danificados. Exporte um backup antes de importar.");
        Set<String> names=new HashSet<>(), coordinates=new HashSet<>();
        for(SavedPlaces.Entry existing:before.entries){
            names.add(existing.name.toLowerCase(Locale.ROOT));
            coordinates.add(coordKey(existing.latitude,existing.longitude));
        }
        JSONArray sourceFavorites=root.getJSONArray("favorites");
        List<SavedPlaces.Entry> toAdd=new ArrayList<>();
        int skippedFavorites=0;
        for(int i=0;i<sourceFavorites.length();i++){
            JSONObject f=sourceFavorites.getJSONObject(i);
            String name=f.getString("name").trim();
            SavedPlaces.Entry entry=new SavedPlaces.Entry(name,f.getDouble("lat"),f.getDouble("lng"),"Geral");
            String key=name.toLowerCase(Locale.ROOT), loc=coordKey(entry.latitude,entry.longitude);
            if(!names.add(key) || !coordinates.add(loc)){skippedFavorites++;continue;}
            toAdd.add(entry);
        }
        if(before.entries.size()+toAdd.size()>SavedPlaces.LIMIT)
            throw new IOException("Limite de favoritos atingido. Remova alguns antes de importar.");

        GpxStore routeStore=new GpxStore(context);
        List<GpxStore.Entry> oldRoutes=routeStore.list();
        Set<String> routeNames=new HashSet<>();
        for(GpxStore.Entry entry:oldRoutes)routeNames.add(entry.name.toLowerCase(Locale.ROOT));
        JSONArray sourceRoutes=root.getJSONArray("routes");
        List<GpxRoute> toSave=new ArrayList<>();
        int skippedRoutes=0;
        for(int i=0;i<sourceRoutes.length();i++){
            JSONObject route=sourceRoutes.getJSONObject(i);
            JSONArray points=route.getJSONArray("points");
            double[][] coords=new double[points.length()][2];
            for(int j=0;j<points.length();j++){
                JSONArray row=points.getJSONArray(j);
                coords[j][0]=row.getDouble(0); coords[j][1]=row.getDouble(1);
            }
            GpxRoute entry=new GpxRoute(route.getString("name"),coords);
            if(!routeNames.add(entry.name.toLowerCase(Locale.ROOT))){skippedRoutes++;continue;}
            toSave.add(entry);
        }
        if(oldRoutes.size()+toSave.size()>GpxStore.MAX_FILES)
            throw new IOException("Limite de rotas atingido. Remova algumas antes de importar.");

        SharedPreferences archive=AccountProfiles.preferences(context,SCOPE);
        String previousArchive=archive.getString(KEY,null);
        List<String> insertedRouteIds=new ArrayList<>();
        boolean touchedFavorites=false,touchedArchive=false;
        try {
            for(GpxRoute route:toSave)insertedRouteIds.add(routeStore.save(route).id);
            if(!toAdd.isEmpty()){
                touchedFavorites=true;
                places.merge(toAdd);
            }
            touchedArchive=true;
            if(!archive.edit().putString(KEY,root.toString()).commit())
                throw new IOException("Falha ao preservar os filtros PGSharp");
        }catch(Exception error){
            if(touchedArchive)try {
                SharedPreferences.Editor editor=archive.edit();
                if(previousArchive==null)editor.remove(KEY);else editor.putString(KEY,previousArchive);
                if(!editor.commit())throw new IOException("Rollback do arquivo PGSharp falhou");
            }catch(Exception rollback){error.addSuppressed(rollback);}
            if(touchedFavorites)try{places.restoreSnapshot(before);}
                catch(Exception rollback){error.addSuppressed(rollback);}
            for(int i=insertedRouteIds.size()-1;i>=0;i--)try{routeStore.delete(insertedRouteIds.get(i));}
                catch(Exception rollback){error.addSuppressed(rollback);}
            throw error;
        }
        return new Report(toAdd.size(),skippedFavorites,toSave.size(),skippedRoutes,
            root.getJSONArray("pokemonFilters").length(),
            root.getJSONArray("questFilters").length(),
            root.getJSONArray("raidFilters").length());
    }
    private static String coordKey(double lat,double lon) {
        return Double.doubleToLongBits(lat)+":"+Double.doubleToLongBits(lon);
    }
}
