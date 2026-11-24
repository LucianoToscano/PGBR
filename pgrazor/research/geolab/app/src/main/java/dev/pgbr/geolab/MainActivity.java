package dev.pgbr.geolab;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.text.InputType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.ArrayList;
public final class MainActivity extends Activity {
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private TextView result;
    private Button start, load;
    private EditText fileId, token;
    private volatile FilterEngine.Rule rule=new FilterEngine.Rule("READY",80);
    @Override public void onCreate(Bundle b){super.onCreate(b);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(24,24,24,24);
        TextView heading=new TextView(this);heading.setText("PGBR GeoLab — laboratório isolado");heading.setTextSize(22);layout.addView(heading);
        fileId=new EditText(this);fileId.setHint("ID de configuração JSON no Drive (opcional)");layout.addView(fileId);
        token=new EditText(this);token.setHint("Token OAuth temporário");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);token.setSaveEnabled(false);layout.addView(token);
        load=new Button(this);load.setText("Carregar regra autorizada");layout.addView(load);
        start=new Button(this);start.setText("Executar teste sintético");layout.addView(start);
        result=new TextView(this);result.setText("Regra inicial: READY e valor >= 80. Nenhuma conexão com o jogo.");layout.addView(result);setContentView(layout);
        load.setOnClickListener(v->{String id=fileId.getText().toString().trim(), t=token.getText().toString().trim();token.setText("");busy(true);work.execute(()->{try{rule=DriveConfig.load(id,t);show("Regra carregada: "+rule.status+" "+rule.junction+" valor "+rule.comparison+" "+rule.minValue);}catch(Exception e){show("Configuração rejeitada; regra anterior preservada.");}});});
        start.setOnClickListener(v->{busy(true);work.execute(()->{try{show(runFixture());}catch(Exception|LinkageError e){show("Falha no laboratório: "+e.getClass().getSimpleName());}});});
    }
    private void busy(boolean b){start.setEnabled(!b);load.setEnabled(!b);}
    private void show(String s){runOnUiThread(()->{if(!isDestroyed()){result.setText(s);busy(false);}});}
    private String runFixture() throws Exception {
        StringBuilder json=new StringBuilder("[");for(Entity e:new MeshGrid(50)){if(json.length()>1)json.append(',');json.append(e.json());}json.append(']');
        long t=System.nanoTime();ArrayList<Entity> matches=new ArrayList<>();
        try(FilterEngine engine=new FilterEngine(rule,2,8)){
            int n=engine.submit(JsonParser.entities(NativePipe.transfer(json.toString().getBytes(StandardCharsets.UTF_8))),matches::add).get(10,TimeUnit.SECONDS);
            long ms=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-t);
            try(LocalSimulator sim=new LocalSimulator()){
                ActionDispatcher dispatch=new ActionDispatcher(sim.port(),3);int sent=0;
                for(Entity e:matches){if(sent==3)break;if(dispatch.dispatch(e))sent++;}
                return "Entidades: 2500\nCorrespondências: "+n+"\nTransporte, parsing e filtro: "+ms+" ms\nPOSTs simulados aceitos: "+sim.accepted();
            }
        }
    }
    @Override protected void onDestroy(){work.shutdownNow();super.onDestroy();}
}
