package dev.pgrazor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Independent, clearly labeled offline demonstration entries for the PGBR
 * Pokémon and Raid search dialogs. Not a map scanner, backend, encounter,
 * PGSharp feed, or proof that a Pokémon exists at these coordinates.
 */
final class DemoLiveFeedData {
    private DemoLiveFeedData() {}

    static LiveFeedData.Snapshot sample(double centerLat, double centerLon,
                                        long now, boolean raids) {
        if (!Double.isFinite(centerLat) || Math.abs(centerLat)>89.9
                || !Double.isFinite(centerLon) || Math.abs(centerLon)>179.9) {
            centerLat=-23.550520;
            centerLon=-46.633308;
        }
        List<LiveFeedData.Entry> entries=new ArrayList<>();
        if (raids) {
            int[] bosses={150,25,384,249};
            int[] tiers={5,3,5,5};
            for (int i=0;i<bosses.length;i++) {
                double lat=centerLat+0.0015*(i-1.5);
                double lon=centerLon+0.0016*(i%2==0?1:-1);
                long end=now+(15+i*8)*60000L;
                long hatch=i==3?now+4*60000L:now-60000L;
                boolean bossLive=i!=3;
                entries.add(new LiveFeedData.Entry(
                    "demo:raid:"+i,LiveFeedData.Kind.RAID,
                    "DEMO · Ginásio Fictício "+(i+1),
                    "DADOS FICTÍCIOS · Reide de exemplo · Nv "+tiers[i],
                    lat,lon,bossLive?bosses[i]:-1,-1,tiers[i],-1,end,false,
                    1+i%3,hatch,bossLive));
            }
        } else {
            int[] species={25,133,150,443,669,1,4,7};
            String[] names={"Pikachu","Eevee","Mewtwo","Gible","Flabébé",
                    "Bulbasaur","Charmander","Squirtle"};
            int[] cp={850,920,2315,418,580,430,920,650};
            int[] level={30,24,40,18,30,14,29,25};
            int[] atk={15,12,14,-1,15,0,15,15};
            int[] def={15,12,13,-1,15,0,14,15};
            int[] sta={15,15,14,-1,15,0,15,15};
            for (int i=0;i<species.length;i++) {
                double lat=centerLat+0.0012*(i%4-1.5);
                double lon=centerLon+0.0014*(i<4?1:-1);
                double iv=atk[i]<0?-1:100.0*(atk[i]+def[i]+sta[i])/45.0;
                LiveFeedData.Attrs attributes=new LiveFeedData.Attrs(
                    -1,-1,-1,i==4?5:-1,atk[i],def[i],sta[i],
                    i==1?1:-1,i==6?5:-1,-1);
                String details="DADOS FICTÍCIOS · PC "+cp[i]+" · Nv "+level[i]
                        +(iv>=0?" · "+String.format(Locale.US,"%.1f%% IV",iv)
                             :" · IV desconhecido");
                entries.add(new LiveFeedData.Entry(
                    "demo:pokemon:"+i,LiveFeedData.Kind.POKEMON,names[i],
                    details,lat,lon,species[i],cp[i],level[i],iv,
                    now+(10+i*4)*60000L,iv>=0,0,0,false,false,attributes));
            }
        }
        return new LiveFeedData.Snapshot(entries,now,60,
            "MODO DEMONSTRAÇÃO OFFLINE — dados FICTÍCIOS. "
          + "Nenhuma aparição foi consultada. Não use estas coordenadas.");
    }
}
