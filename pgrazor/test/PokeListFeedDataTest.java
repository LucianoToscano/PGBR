package dev.pgrazor;

import org.json.JSONArray;
import org.json.JSONObject;

/** Offline contract tests for the unverified PokeList response schema. */
public final class PokeListFeedDataTest {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        long now=1_800_000_000_000L;
        JSONObject payload=new JSONObject().put("type","pokemon");
        JSONArray list=new JSONArray();
        list.put(new JSONObject().put("id","spawn-a").put("number",25)
                .put("name","Pikachu").put("latitude",-23.550520).put("longitude",-46.633310)
                .put("cp",500).put("level",25).put("iv",100)
                .put("attack",15).put("defense",15).put("hp",15)
                .put("endTime",(now+120000)/1000L));
        // Duplicate stable ID is rejected.
        list.put(new JSONObject().put("id","spawn-a").put("number",25)
                .put("latitude",-23.550521).put("longitude",-46.633311));
        // Invalid coordinates and invalid species are rejected.
        list.put(new JSONObject().put("id","spawn-b").put("number",100)
                .put("latitude",100).put("longitude",-46.63));
        list.put(new JSONObject().put("id","spawn-c").put("number",2000)
                .put("latitude",-23.55).put("longitude",-46.63));
        // Clearly expired spawn cannot be suggested as live.
        list.put(new JSONObject().put("id","spawn-d").put("number",150)
                .put("latitude",-23.55).put("longitude",-46.63)
                .put("endTime",(now-5000)/1000L));
        payload.put("pokemons",list);
        LiveFeedData.Snapshot snap=PokeListFeedData.parse(payload,now);
        check(snap.entries.size()==1,"Only valid, fresh, unique spawn should survive");
        LiveFeedData.Entry e=snap.entries.get(0);
        check(e.kind==LiveFeedData.Kind.POKEMON,"Correct result kind");
        check(e.dexId==25 && e.iv==100 && e.cp==500,"Exact confirmed stats");
        check(e.attrs.atkIv==15 && e.attrs.defIv==15 && e.attrs.staIv==15,"IV components");
        check(e.expiresAt>now && e.expiresAt<now+180000,"Epoch seconds converted to ms");
        check(e.id.startsWith("pokelist:"),"Origin namespace prevents cross-provider collisions");
        check(snap.note.contains("PokeList / Moonani"),"Visible provider credit");
        try {
            PokeListFeedData.parse(new JSONObject().put("type","pokemon"),now);
            throw new AssertionError("Missing results array must be an error, not fake empty feed");
        }catch(org.json.JSONException expected){}
        try {
            PokeListFeedData.parse(new JSONObject().put("type","raid").put("pokemons",new JSONArray()),now);
            throw new AssertionError("Unexpected feed type must be rejected");
        }catch(org.json.JSONException expected){}
        System.out.println("PASS: source-tagged PokeList defensive parser");
    }
}
