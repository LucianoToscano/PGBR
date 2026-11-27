package dev.pgrazor;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Alternate private panel; no access to PGSharp objects, credentials or native routines. */
public final class LogActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);scroll.addView(root);
        TextView heading=new TextView(this);heading.setText("PGBR - teste de coexistencia PGSharp");heading.setTextSize(20);root.addView(heading);
        TextView hint=new TextView(this);hint.setText("O menu PGSharp permanece original. Este painel testa apenas a leitura local do PGBR. Nenhuma consulta ao feed PGSharp e realizada por ele.");root.addView(hint);
        EditText input=new EditText(this);input.setHint(PgSharpHundoBridge.SCHEMA);input.setMinLines(4);input.setSaveEnabled(false);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PgSharpHundoBridge.MAX_INPUT_CHARS)});root.addView(input);
        Button check=new Button(this);check.setText("Validar exportacao 100% IV");root.addView(check);
        TextView report=new TextView(this);root.addView(report);
        check.setOnClickListener(v -> {
            if (isFinishing() || isDestroyed()) return;
            String raw=input.getText().toString();check.setEnabled(false);
            worker.execute(() -> {
                String text;
                try {
                    PgSharpHundoBridge.Report r=PgSharpHundoBridge.inspect(()->decode(raw),System.currentTimeMillis());
                    StringBuilder s=new StringBuilder("Dados locais; origem nao autenticada. Aceitos: "+r.rows.size()+" | Rejeitados: "+r.rejected+" | Duplicatas: "+r.duplicates);
                    int shown=0;long now=System.currentTimeMillis();
                    for(PgSharpHundoBridge.Hundo h:r.rows){if(h.expiresMs<=now || shown>=50)continue;shown++;s.append("\n#").append(h.dex).append(" | IV 15/15/15 | ").append(h.lat).append(", ").append(h.lon);}
                    text=s.toString();
                } catch(Exception failure){text="Amostra invalida. Nenhum resultado carregado.";}
                final String result=text;
                runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()){report.setText(result);check.setEnabled(true);}});
            });
        });
        Button close=new Button(this);close.setText("Voltar ao PGSharp");close.setOnClickListener(v->finish());root.addView(close);setContentView(scroll);
    }
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}
    private static PgSharpHundoBridge.Batch decode(String raw) throws Exception {
        if(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>PgSharpHundoBridge.MAX_INPUT_CHARS)throw new IllegalArgumentException();
        JSONObject doc=new JSONObject(raw);if(doc.optBoolean("is_demo",false))throw new IllegalArgumentException();
        JSONArray items=doc.getJSONArray("items");if(items.length()>PgSharpHundoBridge.MAX_ROWS)throw new IllegalArgumentException();
        List<PgSharpHundoBridge.Row> rows=new ArrayList<>();
        for(int i=0;i<items.length();i++){
            JSONObject p=items.optJSONObject(i);if(p==null||p.optBoolean("is_demo",false)){rows.add(null);continue;}
            rows.add(new PgSharpHundoBridge.Row(p.opt("category"),p.opt("coordinate_precision"),p.opt("attack_iv"),p.opt("defense_iv"),p.opt("stamina_iv"),p.opt("iv_percent"),p.opt("dex"),p.opt("form"),p.opt("lat"),p.opt("lon"),p.opt("expires_ms"),p.opt("encounter_id")));
        }
        return new PgSharpHundoBridge.Batch(doc.opt("schema"),doc.opt("provider"),doc.opt("coordinate_precision"),doc.opt("fetched_ms"),rows);
    }
}
