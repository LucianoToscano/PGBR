package dev.pgrazor;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/**
 * The full-screen PGBR settings body: a home screen of icon rows, one per category, each opening
 * a page that holds that category's switches, checkboxes, status captions and groups, plus their
 * construction and per-frame refresh. Extracted from {@link Pad}, which keeps overlay
 * attachment/lifecycle, the launcher/quick menu and the joystick. This panel never touches those
 * overlays directly; the few cross-cutting actions it triggers go through {@link Host} so Pad's
 * internals stay private. Settings keys and the {@link Control} API are unchanged.
 */
final class SettingsPanel {
    /** Narrow callback surface back into Pad: everything the settings body needs that lives with the overlays/joystick. */
    interface Host {
        /** Full refresh (overlays + this panel); matches Pad's existing single refresh entry point. */
        void refresh();
        /** Re-theme overlays and this panel after a dark/light toggle. */
        void applyTheme();
        /** Release the location joystick, optionally preserving an in-progress walk. */
        void releaseJoystick(boolean preserveWalk);
        /** Persist the joystick-visible choice and reflect it on the overlay Pad owns. */
        void setJoystickVisible(boolean visible);
        /** Open the shared World map (also used by the quick menu). */
        void openMap();
        /** Open the coordinates & speed editor (tied to engine/joystick state Pad owns). */
        void editLocation();
    }

    private final Activity activity;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final java.util.List<AlertDialog> itemDialogs = new java.util.ArrayList<>();
    private final LinearLayout panel, body, home;
    private final ScrollView bodyScroll;
    private final TextView coords;
    private View closeIcon, themeIcon, backIcon;
    private TextView title, versionLabel;
    private Switch darkSwitch;
    // Category navigation: one home row and one page per category; current is -1 on the home screen.
    private final java.util.List<LinearLayout> pages=new java.util.ArrayList<>(), homeRows=new java.util.ArrayList<>();
    private final java.util.List<String> pageTitles=new java.util.ArrayList<>();
    private final java.util.List<TextView> pageSummaries=new java.util.ArrayList<>(), homeNames=new java.util.ArrayList<>();
    private final java.util.List<MenuIcon> homeIcons=new java.util.ArrayList<>();
    private int current=-1, homeScroll;
    private EditText settingsSearch;
    private TextView sessionStatsSummary;
    private Button sessionStatsToggle;
    private boolean sessionStats24h;
    private LinearLayout settingsSearchResults;
    private SettingsSearch settingsSearchIndex;
    private final java.util.List<View> settingsSearchTargets=new java.util.ArrayList<>();
    private Switch incubatorSwitch;
    private Button incubatorPriority;
    private Button uiScale;
    private TextView incubatorStatus;
    // Primary action and cycle/dialog rows.
    private Button mapTap, throwGrade, throwVariety, storageIv, ivCatchMode, ivOwnedMode, itemSettings, cameraSample, interactionCapture, transferKeepList, transferIvLimit;
    // Evolve-before-transfer rows: enable switch, stages cycle, selection-mode cycle, selected-species picker.
    private Switch evolveTransferSwitch;
    private Button evolveTransferStages, evolveTransferMode, evolveTransferList;
    // Boolean settings use proper switches.
    private Switch idleWalkSwitch, locationSwitch, vgpSwitch, joystickSwitch, cooldownSwitch, localTimeSwitch, encounterCounterSwitch, encounterOverlaySwitch, spawnSwitch, autoTransferSwitch,
        quickSpinSwitch, berrySwitch, quickSummarySwitch, curveSwitch, alwaysHitSwitch, arSwitch, transferOverlaySwitch, catchChanceSwitch, autoBallSwitch, autoThrowSwitch, autoThrowMapSwitch, skipCutscenesSwitch, routeGiftSwitch,
        batteryResSwitch, batteryFpsSwitch, batteryQualitySwitch;
    private Button autoBallTarget, berryPriority;
    private Button vgpIconStyleButton, vgpStartStopButton, joystickStyleButton, eternatusIntensityButton, eternatusSpeedButton;
    private BerryPriorityPicker berryPicker;
    private TextView quickSummaryLabel, vgpStatus, cutsceneStatus, healStatus, smartBattleStatus;
    private Switch healSwitch, healReviveSwitch, smartBattleSwitch, smartAttackSwitch, gymRepeatSwitch;
    private Button healWhen, healThreshold, healItems, shieldWaitButton, faintedSwapWaitButton;
    private static final String[] SHIELD_WAIT_LABELS={
        "Imediato — 0 ms","1,5 segundo (padrão)","3 segundos"
    };
    private static final String[] FAINTED_SWAP_LABELS={
        "Original VNext — 400 ms","2 segundos (padrão)","6 segundos"
    };
    private final CheckBox[] vgpChoices=new CheckBox[3];
    // Status / caption lines (muted, readiness driven).
    private TextView state, steeringStatus, spawnStatus, quickSummaryStatus, transferStatus, quickSpinStatus,
        buddyStatus, giftStatus, itemStatus, rocketStatus, battleStatus, quickBallStatus, ivStatus, cameraStatus, interactionStatus;
    // Multi-option groups keep checkboxes / radios.
    private final CheckBox[] transferProtection=new CheckBox[8];
    private final Switch[] pvpKeep=new Switch[3];
    private final Button[] pvpRank=new Button[3];
    private final CheckBox[] giftChoices=new CheckBox[3];
    private final CheckBox[] quickMenuChoices=new CheckBox[QuickMenu.CUSTOMIZABLE.length];
    private final CheckBox[] rocketChoices=new CheckBox[3];
    private final CheckBox[] pvpBattleChoices=new CheckBox[3];
    private TextView pvpBattleStatus;
    private final CheckBox[] battleChoices=new CheckBox[10];
    private Runnable buddyRefresh=()->{},smartRefresh=()->{};
    // Groups gated by backend readiness (catch-summary support, ball-animation support), refreshed without
    // recreation, and the per-page notes shown while some of them are hidden.
    private final java.util.List<View> summaryGated=new java.util.ArrayList<>(), ballGated=new java.util.ArrayList<>();
    private final java.util.List<TextView> summaryNotes=new java.util.ArrayList<>(), ballNotes=new java.util.ArrayList<>();
    private LinearLayout cameraRoot, interactionRoot;
    private final java.util.List<TextView> sectionHeads=new java.util.ArrayList<>();
    private final java.util.List<TextView> captions=new java.util.ArrayList<>();

    private int dp(float x){return SettingsUi.dp(activity,x);}
    private int ink(){return SettingsUi.ink();}
    private int muted(){return SettingsUi.muted();}
    private int surface(){return SettingsUi.surface();}
    private int accent(){return SettingsUi.accent();}
    private GradientDrawable rounded(int color,int radius){return SettingsUi.rounded(activity,color,radius);}
    private void styleTree(View view){SettingsUi.styleTree(activity,view);}
    private Context dialogContext(){return SettingsUi.dialogContext(activity);}
    private TextView text(String label,int size,int color){return SettingsUi.text(activity,label,size,color);}
    private Button button(String label){return SettingsUi.button(activity,label);}

    /** A collapsible group. Content is built directly into the returned container, so no post-hoc index moves. */
    private LinearLayout section(LinearLayout owner,String label,boolean open){
        LinearLayout group=new LinearLayout(activity);group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head=new LinearLayout(activity);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);head.setMinimumHeight(dp(46));head.setClickable(true);head.setFocusable(true);
        TextView title=new TextView(activity);title.setText(dev.pgrazor.PtBr.tr(label));title.setTextSize(14);head.addView(title,new LinearLayout.LayoutParams(0,-2,1f));
        TextView chev=new TextView(activity);chev.setTextSize(15);chev.setGravity(Gravity.CENTER);chev.setText(dev.pgrazor.PtBr.tr(open?"–":"+"));head.addView(chev,new LinearLayout.LayoutParams(dp(32),-2));
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(6),0,dp(2),dp(8));content.setVisibility(open?View.VISIBLE:View.GONE);
        head.setOnClickListener(v->{boolean o=content.getVisibility()!=View.VISIBLE;content.setVisibility(o?View.VISIBLE:View.GONE);chev.setText(dev.pgrazor.PtBr.tr(o?"–":"+"));});
        group.addView(head);group.addView(content);owner.addView(group);
        sectionHeads.add(title);sectionHeads.add(chev);
        return content;
    }
    private Switch switchRow(LinearLayout owner,String label){
        LinearLayout r=new LinearLayout(activity);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setMinimumHeight(dp(44));
        TextView t=new TextView(activity);t.setText(dev.pgrazor.PtBr.tr(label));t.setTextSize(14);r.addView(t,new LinearLayout.LayoutParams(0,-2,1f));
        Switch sw=new Switch(activity);sw.setContentDescription(dev.pgrazor.PtBr.tr(label));sw.setClickable(true);r.addView(sw,new LinearLayout.LayoutParams(-2,-2));
        owner.addView(r);return sw;
    }
    private TextView caption(LinearLayout owner,String s){TextView t=new TextView(activity);t.setText(dev.pgrazor.PtBr.tr(s));t.setTextSize(13);owner.addView(t);captions.add(t);return t;}

    /** Shows the destination theme: golden sun in dark mode, indigo moon in light mode. */
    private final class ThemeIcon extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ThemeIcon(Context c){super(c);setFocusable(true);setClickable(true);setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3345e1ba),null,rounded(0xffffffff,24)));}
        protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f;
            if(Control.darkTheme()){
                paint.setColor(0xffffcf5c);paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,dp(5),paint);paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine(x+(float)Math.cos(a)*dp(8),y+(float)Math.sin(a)*dp(8),x+(float)Math.cos(a)*dp(11),y+(float)Math.sin(a)*dp(11),paint);}
            }else{
                paint.setColor(0xff5355b9);paint.setStyle(Paint.Style.FILL);
                Path moon=new Path();moon.addCircle(x,y,dp(10),Path.Direction.CW);
                Path cut=new Path();cut.addCircle(x+dp(6),y-dp(4),dp(9),Path.Direction.CW);moon.op(cut,Path.Op.DIFFERENCE);c.drawPath(moon,paint);
            }
        }
    }
    /** A clean close affix: a small X drawn inside a 48dp hitbox. */
    private final class CloseIcon extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        CloseIcon(Context c){super(c);setContentDescription(dev.pgrazor.PtBr.tr("Close PGBR settings"));setFocusable(true);setClickable(true);
            setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3345e1ba),null,rounded(0xffffffff,24)));}
        protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=dp(8);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(dp(2));paint.setColor(muted());c.drawLine(cx-r,cy-r,cx+r,cy+r,paint);c.drawLine(cx-r,cy+r,cx+r,cy-r,paint);}
    }

    /** A drawn back chevron inside a 48dp hitbox; shown only while a category page is open. */
    private final class BackIcon extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        BackIcon(Context c){super(c);setContentDescription(dev.pgrazor.PtBr.tr("Back to all settings"));setFocusable(true);setClickable(true);
            setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3345e1ba),null,rounded(0xffffffff,24)));}
        protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=dp(7);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeWidth(dp(2));paint.setColor(accent());
            Path p=new Path();p.moveTo(cx+r/2,cy-r);p.lineTo(cx-r/2,cy);p.lineTo(cx+r/2,cy+r);c.drawPath(p,paint);}
    }

    /**
     * Adds a category: an icon row on the home screen and the page it opens. Content is built
     * directly into the returned page container.
     */
    private LinearLayout page(int icon,String label){
        final int index=pages.size();
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(60));
        row.setPadding(dp(12),dp(8),dp(6),dp(8));row.setClickable(true);row.setFocusable(true);
        MenuIcon glyph=new MenuIcon(activity,icon);glyph.accent=true;row.addView(glyph,new LinearLayout.LayoutParams(dp(Control.menuIconSizeDp()+4),dp(Control.menuIconSizeDp()+4)));
        LinearLayout texts=new LinearLayout(activity);texts.setOrientation(LinearLayout.VERTICAL);texts.setPadding(dp(10),0,0,0);
        TextView name=text(label,15,ink());texts.addView(name);
        TextView summary=text("",12,muted());summary.setMaxLines(2);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);texts.addView(summary);
        row.addView(texts,new LinearLayout.LayoutParams(0,-2,1f));
        TextView chevron=text("›",22,muted());chevron.setGravity(Gravity.CENTER);row.addView(chevron,new LinearLayout.LayoutParams(dp(28),-2));
        LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.bottomMargin=dp(6);home.addView(row,rowParams);
        row.setOnClickListener(v->open(index));
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setVisibility(View.GONE);body.addView(content);
        pages.add(content);pageTitles.add(label);homeRows.add(row);homeNames.add(name);homeIcons.add(glyph);pageSummaries.add(summary);
        captions.add(summary);captions.add(chevron);
        return content;
    }
    /** A headed, always-open group inside a page. Returns its content container; the parent is the group root. */
    private LinearLayout group(LinearLayout owner,String label){
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
        TextView head=text(label,13,accent());LinearLayout.LayoutParams headParams=new LinearLayout.LayoutParams(-1,-2);headParams.topMargin=dp(14);headParams.bottomMargin=dp(2);root.addView(head,headParams);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);root.addView(content);
        owner.addView(root);sectionHeads.add(head);
        return content;
    }
    private LinearLayout plain(LinearLayout owner){LinearLayout v=new LinearLayout(activity);v.setOrientation(LinearLayout.VERTICAL);owner.addView(v);return v;}
    private Button launch(LinearLayout owner,String label,Runnable action){Button b=button(label);owner.addView(b);b.setOnClickListener(v->action.run());return b;}
    /** Shown at the top of a page while some of its groups are hidden because the game has not reported support. */
    private void unavailableNote(LinearLayout owner,boolean needsBall){
        TextView note=caption(owner,"Some settings here are not available yet.");
        (needsBall?ballNotes:summaryNotes).add(note);
    }
    /** Creates a compact, accent-insensitive index of real controls, including nested settings. */
    private void indexSettingsOnce(){
        if(settingsSearchIndex!=null)return;
        settingsSearchIndex=new SettingsSearch();
        settingsSearchTargets.clear();
        for(int i=0;i<pages.size();i++){
            String category=pageTitles.get(i);
            settingsSearchTargets.add(pages.get(i));
            settingsSearchIndex.add(category,"Categorias",category,i);
            indexSettingControls(pages.get(i),category,i);
        }
    }
    private void indexSettingControls(View view,String category,int pageIndex){
        if(view instanceof TextView){
            TextView textView=(TextView)view;
            boolean candidate=view instanceof Button||view instanceof CheckBox||view instanceof Switch;
            // A switch-row label belongs to its horizontal control row and is a valid search target.
            if(!candidate && view.getParent() instanceof LinearLayout){
                LinearLayout parent=(LinearLayout)view.getParent();
                candidate=parent.getOrientation()==LinearLayout.HORIZONTAL&&parent.getChildCount()>1;
            }
            if(candidate){
                String label=SettingsSearch.cleanName(textView.getText().toString());
                if(label.length()>2 && settingsSearchTargets.size()<1000) {
                    settingsSearchTargets.add(view);
                    settingsSearchIndex.add(label,"Opções",category,pageIndex);
                }
            }
        }
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)indexSettingControls(group.getChildAt(i),category,pageIndex);
        }
    }
    private void openSearchTarget(SettingsSearch.Entry entry){
        if(entry.id<0||entry.id>=settingsSearchTargets.size()||entry.page>=pages.size())return;
        open(entry.page);
        View target=settingsSearchTargets.get(entry.id),page=pages.get(entry.page);
        // A setting may belong to a collapsed section. Reveal its ancestors but do not toggle the setting.
        for(View walk=target;walk!=page&&walk!=null;){
            if(walk.getVisibility()==View.GONE)walk.setVisibility(View.VISIBLE);
            if(!(walk.getParent() instanceof View))break;
            walk=(View)walk.getParent();
        }
        bodyScroll.post(()->{
            int y=page.getTop();
            for(View walk=target;walk!=page&&walk!=null;){
                y+=walk.getTop();
                if(!(walk.getParent() instanceof View))break;
                walk=(View)walk.getParent();
            }
            bodyScroll.smoothScrollTo(0,Math.max(0,y-dp(54)));
        });
    }
    /** Search by name, aliases, group and category; tap any result to open and scroll to its control. */
    private void filterSettings(String raw){
        String query=SettingsSearch.normalize(raw);
        if(settingsSearchResults==null)return;
        settingsSearchResults.removeAllViews();
        settingsSearchResults.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);
        if(query.isEmpty()){
            for(LinearLayout row:homeRows)row.setVisibility(View.VISIBLE);
            return;
        }
        indexSettingsOnce();
        java.util.List<SettingsSearch.Entry> matches=settingsSearchIndex.find(query);
        java.util.HashSet<Integer> shownPages=new java.util.HashSet<>();
        int visible=0;
        for(SettingsSearch.Entry match:matches){
            shownPages.add(match.page);
            if(visible++>=14)continue; // Avoid an unbounded dynamic result list on small phones.
            Button result=button(match.name+"  ·  "+match.category);
            result.setAllCaps(false);
            result.setContentDescription("Abrir configuração "+match.name);
            result.setOnClickListener(v->openSearchTarget(match));
            settingsSearchResults.addView(result,new LinearLayout.LayoutParams(-1,dp(46)));
        }
        for(int i=0;i<homeRows.size();i++)
            homeRows.get(i).setVisibility(shownPages.contains(i)?View.VISIBLE:View.GONE);
        if(matches.isEmpty()){
            TextView empty=text("Nenhuma configuração encontrada.",13,muted());
            settingsSearchResults.addView(empty);
        }
    }
    /** Shows the home screen (index -1) or one category page, keeping the home scroll position for the way back. */
    private void open(int index){
        if(current<0&&index>=0)homeScroll=bodyScroll.getScrollY();
        current=index;refreshHome();
        home.setVisibility(index<0?View.VISIBLE:View.GONE);
        for(int i=0;i<pages.size();i++)pages.get(i).setVisibility(i==index?View.VISIBLE:View.GONE);
        title.setText(dev.pgrazor.PtBr.tr(index<0?EncounterCounter.title():pageTitles.get(index)));
        backIcon.setVisibility(index<0?View.GONE:View.VISIBLE);
        // The theme toggle and version make room for the longer page titles; theme is also under Display & Performance.
        themeIcon.setVisibility(index<0?View.VISIBLE:View.GONE);versionLabel.setVisibility(index<0?View.VISIBLE:View.GONE);
        final int y=index<0?homeScroll:0;bodyScroll.post(()->bodyScroll.scrollTo(0,y));
    }

    SettingsPanel(Activity activity, Host host) {
        this.activity = activity; this.host = host;
        panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(12), dp(8), dp(12), dp(12));
        panel.setBackgroundColor(surface()); panel.setElevation(dp(12));
        // Full-screen settings header: back (on a category page), title, theme and close controls.
        LinearLayout header = new LinearLayout(activity); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL);
        backIcon = new BackIcon(activity); backIcon.setVisibility(View.GONE); header.addView(backIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        backIcon.setOnClickListener(v -> open(-1));
        title = text("PGBR", 19, accent()); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setPadding(dp(4), dp(8), 0, dp(8)); sectionHeads.add(title);
        title.setSingleLine(true); title.setEllipsize(android.text.TextUtils.TruncateAt.END); title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1f));
        themeIcon = new ThemeIcon(activity); header.addView(themeIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        themeIcon.setOnClickListener(v -> {Control.setDarkTheme(!Control.darkTheme());host.applyTheme();});
        versionLabel = text("PGBR-" + BuildVersion.NAME, 12, muted());
        versionLabel.setSingleLine(true); versionLabel.setGravity(Gravity.CENTER_VERTICAL);
        versionLabel.setPadding(dp(4), 0, dp(8), 0);
        versionLabel.setContentDescription(dev.pgrazor.PtBr.tr("PGBR version " + BuildVersion.NAME));
        captions.add(versionLabel);
        header.addView(versionLabel, new LinearLayout.LayoutParams(-2, dp(48)));
        closeIcon = new CloseIcon(activity); header.addView(closeIcon, new LinearLayout.LayoutParams(dp(48), dp(48)));
        panel.addView(header);
        View gap = new View(activity); panel.addView(gap, new LinearLayout.LayoutParams(-1, dp(8)));
        body = new LinearLayout(activity); body.setOrientation(LinearLayout.VERTICAL);
        bodyScroll = new ScrollView(activity); bodyScroll.setFillViewport(false); bodyScroll.setVerticalScrollBarEnabled(false);
        bodyScroll.addView(body, new ScrollView.LayoutParams(-1,-2));
        panel.addView(bodyScroll, new LinearLayout.LayoutParams(-1,0,1));
        TextView discord = text("Join our Discord", 14, accent());
        discord.setGravity(Gravity.CENTER); discord.setMinimumHeight(dp(48));
        discord.setContentDescription(dev.pgrazor.PtBr.tr("Join the PGBR Discord community"));
        discord.setClickable(true); discord.setFocusable(true); sectionHeads.add(discord);
        panel.addView(discord, new LinearLayout.LayoutParams(-1,dp(48)));
        // Links externos da comunidade ficam ocultos até decisão editorial do PGBR.
        discord.setVisibility(View.GONE);
        discord.setOnClickListener(v -> {
            try { activity.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://discord.gg/9QqfkDSQX3"))); }
            catch (android.content.ActivityNotFoundException unavailable) {
                android.widget.Toast.makeText(activity, dev.pgrazor.PtBr.tr("No app available to open Discord links"), android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Home screen: the always-visible location status, then one icon row per category.
        home = new LinearLayout(activity); home.setOrientation(LinearLayout.VERTICAL); body.addView(home);
        coords = text("", 16, ink()); coords.setPadding(dp(4), dp(6), dp(4), dp(10)); home.addView(coords);
        // Session stats from read-only lifetime counters; accounts are kept in separate local histories.
        LinearLayout sessionCard=new LinearLayout(activity);sessionCard.setOrientation(LinearLayout.VERTICAL);
        sessionCard.setPadding(dp(12),dp(10),dp(12),dp(10));
        sessionCard.setBackground(rounded(Control.darkTheme()?Color.rgb(28,39,53):Color.rgb(232,241,247),12));
        TextView statsLabel=text("Estatísticas de jogo · últimas 1 hora / 24 horas",14,accent());
        sessionCard.addView(statsLabel);
        sessionStatsSummary=text("Aguardando primeira leitura da sessão…",12,ink());
        sessionStatsSummary.setPadding(0,dp(7),0,dp(7));sessionCard.addView(sessionStatsSummary);
        sessionStatsToggle=button("Ver últimas 24 horas");sessionCard.addView(sessionStatsToggle);
        sessionStatsToggle.setOnClickListener(v->{
            sessionStats24h=!sessionStats24h;refreshSessionStats();
        });
        LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2);
        cardParams.bottomMargin=dp(10);home.addView(sessionCard,cardParams);
        settingsSearch = new EditText(activity);
        settingsSearch.setSingleLine(true);
        settingsSearch.setTextSize(15);
        settingsSearch.setHint(dev.pgrazor.PtBr.tr("Buscar configurações, ferramentas e opções"));
        settingsSearch.setContentDescription(dev.pgrazor.PtBr.tr("Buscar nas configurações do PGBR"));
        settingsSearch.setInputType(InputType.TYPE_CLASS_TEXT);
        settingsSearch.setPadding(dp(12),dp(10),dp(12),dp(10));
        home.addView(settingsSearch,new LinearLayout.LayoutParams(-1,dp(52)));
        settingsSearchResults=new LinearLayout(activity);
        settingsSearchResults.setOrientation(LinearLayout.VERTICAL);
        settingsSearchResults.setVisibility(View.GONE);
        home.addView(settingsSearchResults,new LinearLayout.LayoutParams(-1,-2));
        settingsSearch.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){filterSettings(s.toString());}
            public void afterTextChanged(android.text.Editable editable){}
        });
        buildLocationPage();
        buildMapIndicatorsPage();
        buildCatchingPage();
        buildPokemonPage();
        buildGoPlusPage();
        buildBattlesPage();
        buildSocialPage();
        buildAutomationPage();
        buildEventsPage();
        buildDisplayPage();
        buildAccountsPage();   // last: its Diagnostics group collects status lines created by the pages above
        filterSettings("");

        // Closing the full-screen settings panel simply returns to the game (or the quick menu on the next tap).
        closeIcon.setOnClickListener(v -> panel.setVisibility(View.GONE));
        // The settings panel is a full-screen modal now; it consumes taps so the game behind it stays inert.
        panel.setClickable(true);
    }

    private static String eternityIntensityLabel(int intensity){
        return intensity>=2?"Épica":"Suave";
    }
    private static String eternitySpeedLabel(int speed){
        return speed==0?"Lenta":speed==2?"Rápida":"Normal";
    }
    private void showEternatusFxChoice(boolean intensityChoice){
        String[] choices=intensityChoice
            ? new String[]{"Suave • menos brilho","Épica • vórtice, fumaça e iluminação fortes"}
            : new String[]{"Lenta","Normal","Rápida"};
        int selected=intensityChoice?Control.eternatusFxIntensity()-1:Control.eternatusFxSpeed();
        new AlertDialog.Builder(dialogContext())
            .setTitle(intensityChoice?"Intensidade da tempestade":"Velocidade do vórtice")
            .setSingleChoiceItems(choices,selected,(dialog,which)->{
                dialog.dismiss();
                if(intensityChoice){
                    Control.setEternatusFxIntensity(which+1);
                    if(eternatusIntensityButton!=null)
                        eternatusIntensityButton.setText("Intensidade da tempestade: "+
                            eternityIntensityLabel(Control.eternatusFxIntensity()));
                }else{
                    Control.setEternatusFxSpeed(which);
                    if(eternatusSpeedButton!=null)
                        eternatusSpeedButton.setText("Velocidade do vórtice: "+
                            eternitySpeedLabel(Control.eternatusFxSpeed()));
                }
                host.refresh();
            }).setNegativeButton("Cancelar",null).show();
    }

    private static String joystickStyleName(int style){
        switch(style){
            case 1:return "Eternatus • Tempestade viva";
            case 2:return "HUD translúcido • Setas";
            case 3:return "Imagem personalizada";
            case 4:return "Eternatus • Vórtice anterior";
            default:return "Joystick original";
        }
    }
    private void showJoystickStyles(){
        Context ui=dialogContext();
        LinearLayout content=new LinearLayout(ui);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(8),dp(12),dp(8));
        ScrollView scroll=new ScrollView(ui);scroll.addView(content);
        final AlertDialog[] chosen={null};
        for(int style=0;style<=4;style++){
            final int selection=style;
            LinearLayout option=new LinearLayout(ui);
            option.setOrientation(LinearLayout.HORIZONTAL);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(dp(8),dp(6),dp(8),dp(6));
            option.setBackground(SettingsUi.rounded(activity,style==Control.joystickStyle()?0x3041d9cc:0x138c92a0,dp(14)));
            LinearLayout.LayoutParams optionParams=new LinearLayout.LayoutParams(-1,dp(78));
            optionParams.bottomMargin=dp(6);
            content.addView(option,optionParams);
            FrameLayout preview=new FrameLayout(ui);
            option.addView(preview,new LinearLayout.LayoutParams(dp(60),dp(60)));
            if(style==1||style==2||style==4){
                String name=style==2?"hud":"eternatus";
                String[] layers = style == 1 ? (Control.joystickEternatusEffects()
                    ? new String[]{"clean", "storm", "thumb"}
                    : new String[]{"clean", "thumb"})
                    : style == 4 && Control.joystickEternatusEffects()
                        ? new String[]{"vortex", "rim", "thumb"}
                        : new String[]{"base", "thumb"};
                for(String layer : layers){
                    String file=style==1
                        ? "pgrazor/map-icons/joystick-eternatus-morph-"+layer+".webp"
                        : "pgrazor/map-icons/joystick-"+name+"-"+layer+".webp";
                    try(java.io.InputStream image=PgAssets.open(activity,file)){
                        Bitmap bitmap=BitmapFactory.decodeStream(image);
                        if(bitmap==null)continue;
                        ImageView thumbnail=new ImageView(ui);
                        thumbnail.setImageBitmap(bitmap);
                        if(style==1 && layer.equals("storm"))thumbnail.setAlpha(.65f);
                        thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        int size = dp(layer.equals("thumb") ? (style == 2 ? 20 : 22) : 60);
                        FrameLayout.LayoutParams artParams=new FrameLayout.LayoutParams(size,size,Gravity.CENTER);
                        preview.addView(thumbnail,artParams);
                    }catch(Exception ignored){}
                }
            }else{
                TextView glyph=new TextView(ui);
                glyph.setText(dev.pgrazor.PtBr.tr(style==0?"◉":"▣"));
                glyph.setTextSize(30);glyph.setGravity(Gravity.CENTER);
                glyph.setTextColor(SettingsUi.accent());
                preview.addView(glyph,new FrameLayout.LayoutParams(-1,-1));
            }
            TextView name=new TextView(ui);
            name.setText(dev.pgrazor.PtBr.tr(joystickStyleName(style)));
            name.setTextSize(14);
            name.setTextColor(SettingsUi.ink());
            name.setPadding(dp(8),0,0,0);
            option.addView(name,new LinearLayout.LayoutParams(0,-2,1f));
            RadioButton radio=new RadioButton(ui);
            radio.setChecked(Control.joystickStyle()==style);
            radio.setClickable(false);radio.setFocusable(false);
            option.addView(radio);
            option.setClickable(true);option.setFocusable(true);
            option.setContentDescription(dev.pgrazor.PtBr.tr(joystickStyleName(style)));
            option.setOnClickListener(v->{
                if(chosen[0]!=null)chosen[0].dismiss();
                if(selection==3){JoystickImage.select(activity);return;}
                host.releaseJoystick(true);
                Control.setJoystickStyle(selection);
                if(joystickStyleButton!=null)
                    joystickStyleButton.setText(dev.pgrazor.PtBr.tr("Estilo do joystick: "+joystickStyleName(selection)));
                host.refresh();
            });
        }
        chosen[0]=new AlertDialog.Builder(ui)
            .setTitle(dev.pgrazor.PtBr.tr("Escolher estilo do joystick"))
            .setView(scroll)
            .setNegativeButton(dev.pgrazor.PtBr.tr("Fechar"),null)
            .create();
        chosen[0].setOnShowListener(dialog->MapTheme.styleButtons(chosen[0]));
        chosen[0].show();
    }

    private void configureHudIndicator(String name,int bit){
        final Context ctx=dialogContext();
        LinearLayout body=new LinearLayout(ctx);body.setOrientation(LinearLayout.VERTICAL);
        int padding=dp(16);body.setPadding(padding,dp(6),padding,dp(6));
        TextView desc=new TextView(ctx);
        desc.setText("Fundo desta legenda: "+Control.hudBackgroundOpacity(bit)+"%");
        desc.setTextSize(14);body.addView(desc);
        SeekBar opacity=new SeekBar(ctx);opacity.setMax(100);
        opacity.setProgress(Control.hudBackgroundOpacity(bit));body.addView(opacity);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int percent,boolean user){
                if(!user)return;
                Control.setHudBackgroundOpacity(bit,percent);
                desc.setText("Fundo desta legenda: "+percent+"%");host.refresh();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        if (bit == Control.HUD_COOLDOWN) {
            int[] sizes = {0, 11, 13, 15, 18, 20};
            String[] names = {"Automático", "11 sp", "13 sp", "15 sp", "18 sp", "20 sp"};
            Button size = button("Tamanho do contador · " + (Control.cooldownTextSp() == 0
                ? names[0] : Control.cooldownTextSp() + " sp"));
            body.addView(size);
            size.setOnClickListener(v -> new AlertDialog.Builder(ctx).setTitle("Tamanho do contador")
                .setSingleChoiceItems(names, java.util.Arrays.binarySearch(sizes, Control.cooldownTextSp()), (dialog, index) -> {
                    Control.setCooldownTextSp(sizes[index]);
                    size.setText("Tamanho do contador · " + names[index]);
                    host.refresh();
                    dialog.dismiss();
                }).setNegativeButton("Cancelar", null).show());
        }
        Button color=button("Cor: "+Control.HUD_PALETTE_NAMES[Control.hudPaletteIndex(bit)]);
        body.addView(color);
        color.setOnClickListener(v->{
            new AlertDialog.Builder(ctx).setTitle("Cor: "+name)
                .setSingleChoiceItems(Control.HUD_PALETTE_NAMES,Control.hudPaletteIndex(bit),(dialog,choice)->{
                    Control.setHudPaletteIndex(bit,choice);
                    color.setText("Cor: "+Control.HUD_PALETTE_NAMES[choice]);
                    host.refresh();dialog.dismiss();
                }).setNegativeButton("Cancelar",null).show();
        });
        AlertDialog dialog=new AlertDialog.Builder(ctx).setTitle(name)
            .setView(body).setPositiveButton("Concluído",null).create();
        dialog.setOnShowListener(d->MapTheme.styleButtons(dialog));
        dialog.show();
    }

    private void buildLocationPage() {
        LinearLayout loc = page(MenuIcon.TELEPORT, "Location & Map");
        locationSwitch=switchRow(loc,"Location spoofing");locationSwitch.setChecked(Control.desiredEnabled());
        locationSwitch.setOnCheckedChangeListener((v,checked)->{if(checked==Control.desiredEnabled())return;host.releaseJoystick(true);Control.setLocationSpoofing(checked);host.refresh();});
        steeringStatus = caption(loc, "Joystick follows the camera");
        launch(loc, "Coordinates & speed", host::editLocation);
        launch(loc, "Velocidades rápidas: 0 / 9,3 / 18,6 / 30 / 60 km/h",()->{
            final double[] speeds={0,9.3,18.6,30,60};
            final String[] labels={"0,0 km/h · Parado","9,3 km/h","18,6 km/h","30 km/h","60 km/h"};
            int current=0;
            for(int i=0;i<speeds.length;i++)if(Math.abs(Control.engine.speedKmh-speeds[i])<0.001)current=i;
            new AlertDialog.Builder(dialogContext()).setTitle("Velocidade de movimento")
                .setSingleChoiceItems(labels,current,(d,index)->{
                    Control.engine.speed(speeds[index]);Control.saveState();host.refresh();d.dismiss();
                }).setNegativeButton("Cancelar",null).show();
        });
        launch(loc, "World map", host::openMap);
        joystickSwitch = switchRow(loc, "Show joystick"); joystickSwitch.setChecked(Control.joystickVisible());
        joystickSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.joystickVisible())return; host.setJoystickVisible(checked); });
        LinearLayout joystickArt=section(loc,"Aparência do joystick",false);
        TextView joystickSizeLabel=new TextView(activity);joystickSizeLabel.setTextSize(13);
        joystickSizeLabel.setText(dev.pgrazor.PtBr.tr("Tamanho do joystick: "+Control.joystickSizeDp()+" dp"));joystickArt.addView(joystickSizeLabel);
        SeekBar joystickSize=new SeekBar(activity);joystickSize.setMax(132);joystickSize.setProgress(Control.joystickSizeDp()-88);
        joystickArt.addView(joystickSize);
        joystickSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){
                if(user){Control.setJoystickSizeDp(88+progress);joystickSizeLabel.setText(dev.pgrazor.PtBr.tr("Tamanho do joystick: "+Control.joystickSizeDp()+" dp"));host.refresh();}
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        TextView joystickOpacityLabel=new TextView(activity);joystickOpacityLabel.setTextSize(13);
        joystickOpacityLabel.setText(dev.pgrazor.PtBr.tr("Opacidade do joystick: "+Control.joystickOpacity()+"%"));joystickArt.addView(joystickOpacityLabel);
        SeekBar joystickOpacity=new SeekBar(activity);joystickOpacity.setMax(90);joystickOpacity.setProgress(Control.joystickOpacity()-10);
        joystickArt.addView(joystickOpacity);
        joystickOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){
                if(user){Control.setJoystickOpacity(10+progress);joystickOpacityLabel.setText(dev.pgrazor.PtBr.tr("Opacidade do joystick: "+Control.joystickOpacity()+"%"));host.refresh();}
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        caption(joystickArt,"A transparência controla quanto do mapa aparece sob o joystick.");
        joystickStyleButton=launch(joystickArt,
            "Estilo do joystick: "+joystickStyleName(Control.joystickStyle()),this::showJoystickStyles);
        Switch neonEffects=switchRow(joystickArt,"Animação Eternatus · limpo ↔ tempestade");
        neonEffects.setChecked(Control.joystickEternatusEffects());
        neonEffects.setOnCheckedChangeListener((v,on)->{
            Control.setJoystickEternatusEffects(on);host.refresh();
        });
        caption(joystickArt,"Ciclo automático: forma limpa → tempestade iluminada → vórtice girando → forma limpa. A Pokébola Eternatus acompanha o mesmo ciclo.");
        eternatusIntensityButton=launch(joystickArt,
            "Intensidade da tempestade: "+eternityIntensityLabel(Control.eternatusFxIntensity()),
            ()->showEternatusFxChoice(true));
        eternatusSpeedButton=launch(joystickArt,
            "Velocidade do vórtice: "+eternitySpeedLabel(Control.eternatusFxSpeed()),
            ()->showEternatusFxChoice(false));
        caption(joystickArt,"A tempestade gira em duas camadas com fumaça e iluminação. Desative a animação para manter o visual limpo; o vórtice anterior continua disponível.");
        launch(joystickArt,"Escolher imagem do joystick no celular",()->{
            if(Control.engine.routeProgress().active){
                android.widget.Toast.makeText(activity,dev.pgrazor.PtBr.tr("Pause ou encerre a rota antes de abrir o seletor de imagens do Android."),android.widget.Toast.LENGTH_LONG).show();
            }else JoystickImage.select(activity);
        });
        launch(joystickArt,"Restaurar imagem original do joystick",()->{JoystickImage.reset(activity);host.refresh();});
        idleWalkSwitch = switchRow(loc, "Idle walking");idleWalkSwitch.setChecked(Control.idleWalking());
        idleWalkSwitch.setOnCheckedChangeListener((v,checked)->{if(checked!=Control.idleWalking())Control.setIdleWalking(checked);});
        caption(loc, "When idle, start walking in a circle. Speed 0 pauses it.");

        LinearLayout map = group(loc, "Game map");
        mapTap = button("Game map tap: Do nothing"); map.addView(mapTap);
        mapTap.setOnClickListener(v -> { Control.setMapTapMode((Control.mapTapMode()+1)%4); host.refresh(); });
        spawnSwitch = switchRow(map, "Spawn booster (80 m)"); spawnSwitch.setChecked(Control.spawnBooster());
        spawnSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.spawnBooster())return; Control.setSpawnBooster(spawnSwitch.isChecked()); });
        spawnStatus = caption(map, "");
        caption(map, "Can cause lag in busy areas with many nearby Pokémon. Turn it off if the game stutters.");


    }

    // Keep the original four stored format IDs stable across updates and imported profiles.
    private static final int[] CLOCK_FORMAT_IDS = {4, 5, 2, 1, 3, 0, 6};
    private static final String[] CLOCK_EXAMPLES = {
        "Só hora · 14:17", "Hora e data · 14:17 · 20/11/26", "Com fuso · 14:17 GMT-03:00",
        "Com bandeira · 🇧🇷 14:17 - 20/11/26", "Com ícones · 🕒 14:17 🗓 20/11/26",
        "Bandeira e ícones · 🇧🇷 🕒 14:17 🗓 20/11/26",
        "Bandeira + cidade · 🇧🇷 Fortaleza • 14:17 · 20/11/26"
    };
    private static final String[] COOLDOWN_STYLES = {"Discreto", "Chamativo"};
    private Button clockFormatButton, cooldownStyleButton;

    private int selectedClockChoice() {
        for (int i = 0; i < CLOCK_FORMAT_IDS.length; i++)
            if (CLOCK_FORMAT_IDS[i] == Control.localTimeFormat()) return i;
        return 0;
    }

    private void buildMapIndicatorsPage() {
        LinearLayout page = page(MenuIcon.MAP, "Hora, cooldown e legendas");
        LinearLayout clock = group(page, "Hora local");
        localTimeSwitch = switchRow(clock, "Mostrar hora no mapa");
        localTimeSwitch.setOnCheckedChangeListener((v, on) -> {
            if (on == (Control.localTimeOverlay() && Control.hudLabelVisible(Control.HUD_LOCALTIME))) return;
            if (on) Control.setHudLabelVisible(Control.HUD_LOCALTIME, true);
            Control.setLocalTimeOverlay(on);
            host.refresh();
        });
        clockFormatButton = launch(clock, "Formato da hora", () -> new AlertDialog.Builder(dialogContext())
            .setTitle("Formato da hora local").setSingleChoiceItems(CLOCK_EXAMPLES, selectedClockChoice(), (dialog, index) -> {
                Control.setLocalTimeFormat(CLOCK_FORMAT_IDS[index]);
                dialog.dismiss();
                host.refresh();
            }).setNegativeButton("Cancelar", null).show());
        launch(clock, "Cor e fundo da hora", () -> configureHudIndicator("Hora local", Control.HUD_LOCALTIME));

        LinearLayout cooldown = group(page, "Cooldown");
        cooldownSwitch = switchRow(cooldown, "Mostrar tempo de espera no mapa");
        cooldownSwitch.setOnCheckedChangeListener((v, on) -> {
            if (on == (Control.cooldownOverlay() && Control.hudLabelVisible(Control.HUD_COOLDOWN))) return;
            if (on) Control.setHudLabelVisible(Control.HUD_COOLDOWN, true);
            Control.setCooldownOverlay(on);
            host.refresh();
        });
        cooldownStyleButton = launch(cooldown, "Estilo do contador", () -> new AlertDialog.Builder(dialogContext())
            .setTitle("Estilo do cooldown").setSingleChoiceItems(COOLDOWN_STYLES, Control.cooldownEmphasis() ? 1 : 0,
                (dialog, index) -> {
                    Control.setCooldownEmphasis(index == 1);
                    dialog.dismiss();
                    host.refresh();
                }).setNegativeButton("Cancelar", null).show());
        caption(cooldown, "A contagem usa a cor escolhida. Ao chegar a 0:00:00, números e contorno ficam verdes automaticamente. Sem localização, o contador mostra —, nunca um verde falso.");
        launch(cooldown, "Tamanho, cor e fundo", () -> configureHudIndicator("Cooldown", Control.HUD_COOLDOWN));
        launch(cooldown, "Histórico de cooldown", () -> Cooldown.show(activity));

        LinearLayout hud=section(page,"Outras legendas do mapa",false);
        caption(hud,"Cada legenda tem seu próprio controle. Desativar uma legenda não desliga o recurso nem oculta seus ícones de ação. Alertas de Shiny continuam funcionando.");
        final int[] captionBits={Control.HUD_VGP,Control.HUD_BUDDY,Control.HUD_AUTOMATION,
            Control.HUD_GYM,Control.HUD_CATCH,Control.HUD_ENCOUNTERS,Control.HUD_SHINY};
        final String[] captionNames={"Status VGP+","Energia do companheiro","Status das automações",
            "Status de Gym Slotter e frutas","Chance de captura","Contador de encontros",
            "Aviso de Shiny encontrado"};
        final Switch[] captionSwitches=new Switch[captionBits.length];
        for(int i=0;i<captionBits.length;i++){
            final int bit=captionBits[i];
            Switch item=switchRow(hud,"Exibir: "+captionNames[i]);captionSwitches[i]=item;
            item.setChecked(Control.hudLabelVisible(bit));
            item.setOnCheckedChangeListener((v,on)->{
                if(Control.hudLabelVisible(bit)!=on){Control.setHudLabelVisible(bit,on);host.refresh();}
            });
        }
        launch(hud,"Mostrar todas as legendas do mapa",()->{
            Control.setHudLabelsVisible(true);
            for(Switch item:captionSwitches)item.setChecked(true);
            host.refresh();
        });
        launch(hud,"Ocultar todas as legendas do mapa",()->{
            Control.setHudLabelsVisible(false);
            for(Switch item:captionSwitches)item.setChecked(false);
            host.refresh();
        });
        TextView fontLabel=new TextView(activity);fontLabel.setTextSize(13);
        fontLabel.setText(dev.pgrazor.PtBr.tr("Tamanho das legendas: "+Control.hudLabelTextSp()+" sp"));hud.addView(fontLabel);
        SeekBar fontSlider=new SeekBar(activity);fontSlider.setMax(5);
        fontSlider.setProgress(Control.hudLabelTextSp()-9);hud.addView(fontSlider);
        fontSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                if(!fromUser)return;
                Control.setHudLabelTextSp(progress+9);
                fontLabel.setText(dev.pgrazor.PtBr.tr("Tamanho das legendas: "+Control.hudLabelTextSp()+" sp"));host.refresh();
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
        launch(hud,"Personalizar cor e fundo de cada legenda",()->{
            new AlertDialog.Builder(dialogContext()).setTitle("Selecionar indicador")
                .setItems(captionNames,(d,index)->configureHudIndicator(captionNames[index],captionBits[index]))
                .setNegativeButton("Cancelar",null).show();
        });
        caption(hud,"As cores e a transparência de cada legenda são salvas separadamente. Com fundo 0%, apenas o símbolo ou número permanece visível.");

    }

    private void buildCatchingPage() {
        LinearLayout page = page(MenuIcon.CATCH, "Catching");
        unavailableNote(page, true);

        // ----- Quick catch (gated by catch-summary support) -----
        LinearLayout quick = group(page, "Quick catch"); summaryGated.add((View)quick.getParent());
        LinearLayout sumRow = new LinearLayout(activity); sumRow.setOrientation(LinearLayout.HORIZONTAL); sumRow.setGravity(Gravity.CENTER_VERTICAL); sumRow.setMinimumHeight(dp(44));
        quickSummaryLabel = new TextView(activity); quickSummaryLabel.setTextSize(14); sumRow.addView(quickSummaryLabel, new LinearLayout.LayoutParams(0,-2,1f));
        quickSummarySwitch = new Switch(activity); quickSummarySwitch.setContentDescription(dev.pgrazor.PtBr.tr("Quick catch")); sumRow.addView(quickSummarySwitch, new LinearLayout.LayoutParams(-2,-2)); quick.addView(sumRow);
        quickSummarySwitch.setChecked(Control.quickSummaryEnabled());
        quickSummarySwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.quickSummaryEnabled())return; Control.setQuickSummaryEnabled(quickSummarySwitch.isChecked()); host.refresh(); });
        quickSummaryStatus = caption(quick, "");

        // ----- Throws, ball choice and auto throw (gated by ball-animation support) -----
        LinearLayout throwC = group(page, "Throws"); ballGated.add((View)throwC.getParent());
        throwGrade = button("Throw rating: OFF"); throwC.addView(throwGrade);
        throwGrade.setOnClickListener(v -> { Control.setThrowGrade((Control.throwRating()+1)%4); host.refresh(); });
        throwVariety = button("Throw variety: Off"); throwC.addView(throwVariety);
        throwVariety.setOnClickListener(v -> { Control.setThrowVariety((Control.throwVariety()+1)%4); host.refresh(); });
        caption(throwC, "Throw variety picks a rating at random for each throw, with even odds. All includes a plain throw. It replaces the throw rating while on.");
        curveSwitch = switchRow(throwC, "Always curveball"); curveSwitch.setChecked(Control.throwCurve());
        curveSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.throwCurve())return; Control.setThrowCurve(curveSwitch.isChecked()); host.refresh(); });
        alwaysHitSwitch = switchRow(throwC, "Always hit"); alwaysHitSwitch.setChecked(Control.throwAlwaysHit());
        alwaysHitSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.throwAlwaysHit())return; Control.setThrowAlwaysHit(alwaysHitSwitch.isChecked()); host.refresh(); });
        arSwitch = switchRow(throwC, "AR+ throw bonus"); arSwitch.setChecked(Control.throwAr());
        arSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.throwAr())return; Control.setThrowAr(arSwitch.isChecked()); host.refresh(); });
        caption(throwC, "Wild encounters. Combines with throw rating and curveball.");

        LinearLayout ballC = group(page, "Catch chance & ball choice"); ballGated.add((View)ballC.getParent());
        catchChanceSwitch = switchRow(ballC, "Show catch chance"); catchChanceSwitch.setChecked(Control.catchChanceShown());
        catchChanceSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.catchChanceShown())return; Control.setCatchChanceShown(checked); host.refresh(); });
        autoBallSwitch = switchRow(ballC, "Auto ball"); autoBallSwitch.setChecked(Control.autoBall());
        autoBallSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.autoBall())return; Control.setAutoBall(checked); host.refresh(); });
        autoBallTarget = button("Auto ball target: "+Control.autoBallTarget()+"%"); ballC.addView(autoBallTarget);
        autoBallTarget.setOnClickListener(v -> {
            int[] targets=Control.AUTO_BALL_TARGETS;int next=targets[0];
            for(int i=0;i<targets.length;i++)if(targets[i]==Control.autoBallTarget()){next=targets[(i+1)%targets.length];break;}
            Control.setAutoBallTarget(next); host.refresh(); });
        caption(ballC, "Manual encounters. The chance uses the server's catch rate for each ball with your berry, throw rating and curveball; the server's rate already includes your type medal bonus. Auto ball picks the cheapest Poké, Great or Ultra Ball that reaches the target, when the encounter opens and after each escape; a ball you pick by hand is kept until the next escape. It never uses a Master Ball.");

        LinearLayout autoC = group(page, "Auto throw"); ballGated.add((View)autoC.getParent());
        autoThrowSwitch = switchRow(autoC, "Auto throw"); autoThrowSwitch.setChecked(Control.autoThrow());
        autoThrowSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.autoThrow())return; Control.setAutoThrow(autoThrowSwitch.isChecked()); host.refresh(); });
        autoThrowMapSwitch = switchRow(autoC, "Catch on map (tap Pokémon)");
        autoThrowMapSwitch.setChecked(Control.autoThrowOnMap());
        autoThrowMapSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.autoThrowOnMap())return; Control.setAutoThrowOnMap(checked); host.refresh(); });
        quickBallStatus = caption(autoC, "");

        LinearLayout berryC = group(page, "Berries"); summaryGated.add((View)berryC.getParent());
        berrySwitch = switchRow(berryC, "Catch berries & overlay"); berrySwitch.setChecked(Control.encounterBerryEnabled());
        berrySwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.encounterBerryEnabled())return; Control.setEncounterBerryEnabled(berrySwitch.isChecked()); host.refresh(); });
        berryPicker = new BerryPriorityPicker(activity, host::refresh);
        berryPriority = button("Berry priority: "+BerryPriorityPicker.summary()); berryC.addView(berryPriority);
        berryPriority.setOnClickListener(v -> berryPicker.show());
    }

    private void buildPokemonPage() {
        LinearLayout page = page(MenuIcon.BOX, "Pokémon & Transfer");
        unavailableNote(page, false);

        LinearLayout searchTools = group(page, "Busca inteligente");
        caption(searchTools, "Monte buscas para a coleção do Pokémon GO sem modificar sua conta. "
                +"Termos dependentes de idioma podem precisar de tradução.");
        launch(searchTools, "Gerador de buscas da coleção", () -> PokemonInventorySearchDialog.show(activity));

        // ----- Transfer on catch (gated by catch-summary support) -----
        LinearLayout transfer = plain(page); summaryGated.add(transfer);
        LinearLayout txC = group(transfer, "Transfer on catch");
        autoTransferSwitch = switchRow(txC, "Transferir automaticamente novas capturas"); autoTransferSwitch.setChecked(Control.autoTransferEnabled());
        autoTransferSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.autoTransferEnabled())return; Control.setAutoTransferEnabled(autoTransferSwitch.isChecked()); host.refresh(); });
        transferIvLimit=button("");txC.addView(transferIvLimit);
        transferIvLimit.setOnClickListener(v->{
            // Compatibility: the previously selected 101 ("protect only") remains opt-in.
            // New choices 0..100 are used by the existing packed native transfer contract.
            LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);
            int pad=dp(16);box.setPadding(pad,pad,pad,pad);
            TextView help=text("Limite de IV para manter as novas capturas (0–100%). 0% mantém todos; 100% mantém somente IV 100%. Favoritos e proteções continuam valendo.",13,muted());
            box.addView(help);
            android.widget.SeekBar slider=new android.widget.SeekBar(activity);
            slider.setMax(100);slider.setProgress(Math.min(100,Control.transferIvKeepPercent()));
            box.addView(slider,new LinearLayout.LayoutParams(-1,dp(52)));
            EditText typed=new EditText(activity);
            typed.setSingleLine(true);typed.setInputType(InputType.TYPE_CLASS_NUMBER);
            typed.setText(String.valueOf(slider.getProgress()));
            typed.setSelectAllOnFocus(true);typed.setContentDescription(dev.pgrazor.PtBr.tr("Limite de IV de 0 a 100 por cento"));
            box.addView(typed);
            slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(android.widget.SeekBar bar,int value,boolean byUser){
                    if(byUser&&!typed.getText().toString().equals(String.valueOf(value)))typed.setText(String.valueOf(value));
                }
                public void onStartTrackingTouch(android.widget.SeekBar bar){}
                public void onStopTrackingTouch(android.widget.SeekBar bar){}
            });
            typed.addTextChangedListener(new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int start,int count,int after){}
                public void onTextChanged(CharSequence s,int start,int before,int count){
                    try{int value=Integer.parseInt(s.toString());
                        if(value>=0&&value<=100&&value!=slider.getProgress())slider.setProgress(value);
                    }catch(NumberFormatException ignored){}
                }
                public void afterTextChanged(android.text.Editable s){}
            });
            AlertDialog chooser=new AlertDialog.Builder(dialogContext())
                .setTitle(dev.pgrazor.PtBr.tr("Manter Pokémon a partir deste IV"))
                .setView(box).setNegativeButton(dev.pgrazor.PtBr.tr("Cancelar"),null)
                .setNeutralButton(dev.pgrazor.PtBr.tr("Somente protegidos"),null)
                .setPositiveButton(dev.pgrazor.PtBr.tr("Aplicar"),null).create();
            chooser.setOnShowListener(d->{
                chooser.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2->{
                    int value;
                    try{value=Integer.parseInt(typed.getText().toString());}
                    catch(NumberFormatException bad){typed.setError(dev.pgrazor.PtBr.tr("Digite um valor de 0 a 100"));return;}
                    if(value<0||value>100){typed.setError(dev.pgrazor.PtBr.tr("Digite um valor de 0 a 100"));return;}
                    Control.setTransferIvKeepPercent(value);host.refresh();chooser.dismiss();
                });
                chooser.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v2->
                    new AlertDialog.Builder(dialogContext()).setTitle("Confirmar regra irreversível")
                        .setMessage("Este modo poderá transferir até Pokémon com IV 100%! Somente favoritos, espécies selecionadas e Pokémon protegidos serão preservados. Não é possível desfazer transferências.")
                        .setNegativeButton("Cancelar",null)
                        .setPositiveButton("Confirmar",(prompt,which)->{
                            Control.setTransferIvKeepPercent(101);host.refresh();chooser.dismiss();
                        }).show());
            });
            chooser.show();
        });
        caption(txC,"As transferências são permanentes e só se aplicam a novas capturas. Favoritos e Pokémon com dados desconhecidos são preservados.");
        transferStatus = caption(txC, "");

        LinearLayout keepC = group(transfer, "Always keep");
        // Indexed by protection bit; costume (bit 7) is listed next to event.
        String[] protections={"Keep legendary / mythical / Ultra Beasts","Keep shiny Pokemon","Keep event Pokemon","Keep background Pokemon","Keep XXS","Keep XXL","Keep 0% IV (0/0/0)","Keep costume Pokemon"};
        for(int i:new int[]{0,1,2,7,3,4,5,6}){final int bit=1<<i;CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(protections[i]));box.setChecked((Control.transferProtect()&bit)!=0);transferProtection[i]=box;keepC.addView(box);
            box.setOnClickListener(v->Control.setTransferProtect(box.isChecked()?Control.transferProtect()|bit:Control.transferProtect()&~bit));}
        transferKeepList = button("Custom keep list"); keepC.addView(transferKeepList);
        transferKeepList.setOnClickListener(v -> TransferKeepDialog.show(activity, host::refresh));
        transferOverlaySwitch = switchRow(keepC, "Keep-all overlay button"); transferOverlaySwitch.setChecked(Control.transferOverlayEnabled());
        transferOverlaySwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.transferOverlayEnabled())return; Control.setTransferOverlayEnabled(transferOverlaySwitch.isChecked()); host.refresh(); });
        caption(keepC, "Kept species are never auto-transferred, across all forms. The floating button holds every catch (Keep All) or follows the rules above.");

        LinearLayout pvpC = group(transfer, "Keep for PvP");
        // One row per league: its switch, then the rank it keeps (tap to cycle).
        for(int i=0;i<3;i++){final int league=i;
            LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(44));
            TextView label=new TextView(activity);label.setText(dev.pgrazor.PtBr.tr("Keep "+Control.PVP_LEAGUES[i]+" PvP IVs"));label.setTextSize(14);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
            Button rank=button("Top "+Control.pvpRank(i));pvpRank[i]=rank;row.addView(rank,new LinearLayout.LayoutParams(-2,-2));
            Switch sw=new Switch(activity);sw.setContentDescription(dev.pgrazor.PtBr.tr("Keep "+Control.PVP_LEAGUES[i]+" PvP IVs"));sw.setChecked(Control.pvpKeep(i));pvpKeep[i]=sw;
            LinearLayout.LayoutParams swParams=new LinearLayout.LayoutParams(-2,-2);swParams.leftMargin=dp(8);row.addView(sw,swParams);
            sw.setOnCheckedChangeListener((v,checked)->{if(checked==Control.pvpKeep(league))return;Control.setPvpKeep(league,checked);host.refresh();});
            rank.setOnClickListener(v->{
                int[] ranks=Control.PVP_RANKS;int next=ranks[0];
                for(int j=0;j<ranks.length;j++)if(ranks[j]==Control.pvpRank(league)){next=ranks[(j+1)%ranks.length];break;}
                Control.setPvpRank(league,next);host.refresh();});
            pvpC.addView(row);}
        caption(pvpC, "O limite geral de IV (inclusive 100%) não elimina as proteções selecionadas. Se o Pokémon for Top 1 ou estiver dentro do Top configurado em qualquer liga ativada, ele será mantido mesmo abaixo de 100% IV. A checagem inclui evoluções, limites de PC e a lista de "+Control.PVP_META+" espécies relevantes por liga.");

        LinearLayout evolveC = group(transfer, "Evolve before transferring");
        evolveTransferSwitch = switchRow(evolveC, "Evolve before transferring");
        evolveTransferSwitch.setChecked(Control.evolveTransferEnabled());
        evolveTransferSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.evolveTransferEnabled())return; Control.setEvolveTransferEnabled(evolveTransferSwitch.isChecked()); host.refresh(); });
        evolveTransferStages = button("Evolve: once"); evolveC.addView(evolveTransferStages);
        evolveTransferStages.setOnClickListener(v -> { Control.setEvolveTransferStages(Control.evolveTransferStages()==2?1:2); host.refresh(); });
        evolveTransferMode = button("Applies to: current transfer rules"); evolveC.addView(evolveTransferMode);
        evolveTransferMode.setOnClickListener(v -> { Control.setEvolveTransferMode(Control.evolveTransferMode()==1?0:1); host.refresh(); });
        evolveTransferList = button("Selected species"); evolveC.addView(evolveTransferList);
        evolveTransferList.setOnClickListener(v -> TransferKeepDialog.show(activity, "Selected species", "species selected", Control::evolveTransferSpecies, Control::setEvolveTransferSpecies, host::refresh));
        caption(evolveC, "New catches only; requires Transfer above. Uses candy for ordinary evolutions, without items or special conditions. Keep rules always apply. If evolution is unavailable, transfer normally. Selected species mode keeps every species outside its list.");

        // ----- IV display: the encounter, Pokemon details and storage list IV overlays -----
        LinearLayout pokemonSearch=group(page,"Busca e rastreamento de Pokémon");
        launch(pokemonSearch,"Filtro de Pokémon e resultados · estilo PGSharp",()->LiveFeedDialog.showPokemonSearch(activity));
        launch(pokemonSearch,"Filtro de Reides e resultados · estilo PGSharp",()->LiveFeedDialog.showRaidSearch(activity));
        launch(pokemonSearch,"Buscar por IV, Shiny, PvP e espécie",()->PogoFeedDialog.show(activity));
        launch(pokemonSearch,"Radar de Pokémon e locais próximos",()->LiveFeedDialog.show(activity));
        caption(pokemonSearch,"As fontes usam seus próprios serviços e filtros. Resultados globais e determinados dados podem exigir conta ou acesso específico.");
        LinearLayout encounters = section(page, "Encounter counter", false);
        encounterCounterSwitch = switchRow(encounters, "Count encounters"); encounterCounterSwitch.setChecked(Control.encounterCounter());
        encounterCounterSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.encounterCounter())return; Control.setEncounterCounter(encounterCounterSwitch.isChecked()); host.refresh(); });
        encounterOverlaySwitch = switchRow(encounters, "Show the count on the map"); encounterOverlaySwitch.setChecked(Control.encounterOverlay());
        encounterOverlaySwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.encounterOverlay())return; Control.setEncounterOverlay(encounterOverlaySwitch.isChecked()); host.refresh(); });
        caption(encounters, "Wild Pokémon this account has encountered in the last 24 hours on this device while PGBR was running, shown next to the PGBR title. Encounters made elsewhere, and lure, incense, raid and research encounters, are not counted.");

        LinearLayout iv = group(page, "IV display");
        // One button per screen; each tap moves to the next format, with Off between the last and first.
        ivCatchMode = button(""); iv.addView(ivCatchMode);
        ivCatchMode.setOnClickListener(v -> { Control.setIvCatchMode(Control.nextIvMode(Control.ivCatchMode())); host.refresh(); });
        ivOwnedMode = button(""); iv.addView(ivOwnedMode);
        ivOwnedMode.setOnClickListener(v -> { Control.setIvOwnedMode(Control.nextIvMode(Control.ivOwnedMode())); host.refresh(); });
        storageIv = button(""); iv.addView(storageIv);
        storageIv.setOnClickListener(v -> { Control.setStorageIvMode(Control.nextIvMode(Control.storageIvMode())); host.refresh(); });
        ivStatus = caption(iv, "");

        LinearLayout tools = group(page, "Tools");
        launch(tools, "Mass Evolve", () -> BatchEvolutionSettings.show(activity));
    }

    private void buildGoPlusPage() {
        LinearLayout page = page(MenuIcon.BAG, "Go Plus, Stops & Items");
        unavailableNote(page, false);

        LinearLayout vgpC=group(page,"Virtual Go Plus");
        vgpSwitch=switchRow(vgpC,"Exibir botão VGP+ (não inicia)");vgpSwitch.setChecked(Control.vgpOverlay());
        vgpSwitch.setOnCheckedChangeListener((b,checked)->{if(checked!=Control.vgpOverlay())Control.setVgpOverlay(checked);host.refresh();});
        caption(vgpC,"Exibir o botão não inicia a automação. Use Iniciar VGP+ abaixo "
                + "ou toque no ícone flutuante no mapa e confirme. "
                + "Sem temporizador e sem pausa por cooldown estimado.");
        vgpStartStopButton=launch(vgpC,"Iniciar VGP+",()->{
            if(Control.vgpEnabled()){
                Control.setVgpEnabled(false);host.refresh();return;
            }
            if(!Control.vgpOverlay()){
                new AlertDialog.Builder(dialogContext())
                    .setTitle(dev.pgrazor.PtBr.tr("Botão VGP+ oculto"))
                    .setMessage(dev.pgrazor.PtBr.tr("Ative Exibir botão VGP+ antes de iniciar a automação."))
                    .setPositiveButton(dev.pgrazor.PtBr.tr("Entendi"),null).show();
                return;
            }
            Runnable start=()->{Control.setVgpEnabled(true);host.refresh();};
            if(Control.vgpTapProtection()){
                new AlertDialog.Builder(dialogContext())
                    .setTitle(dev.pgrazor.PtBr.tr("Iniciar Virtual GO Plus?"))
                    .setMessage(dev.pgrazor.PtBr.tr("A VGP poderá tentar capturar Pokémon e girar Poképaradas conforme as opções marcadas. A ativação não garante prontidão do mecanismo nativo."))
                    .setPositiveButton(dev.pgrazor.PtBr.tr("Iniciar"),(d,which)->start.run())
                    .setNegativeButton(dev.pgrazor.PtBr.tr("Cancelar"),null).show();
            }else start.run();
        });
        String[] vgpLabels={"Nearby Pokémon","Nearby PokéStops","Push notifications"};
        for(int i=0;i<3;i++){final int bit=1<<i;CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(vgpLabels[i]));box.setChecked((Control.vgpOptions()&bit)!=0);vgpChoices[i]=box;vgpC.addView(box);
            box.setOnClickListener(v->{Control.setVgpOptions(box.isChecked()?Control.vgpOptions()|bit:Control.vgpOptions()&~bit);
                if(bit==4&&box.isChecked()&&Build.VERSION.SDK_INT>=33&&activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7301);
            });
        }
        Switch accidentalTap=switchRow(vgpC,"Evitar toque acidental no VGP+");
        accidentalTap.setChecked(Control.vgpTapProtection());
        accidentalTap.setOnCheckedChangeListener((button,checked)->Control.setVgpTapProtection(checked));
        caption(vgpC,"A confirmação evita ativação acidental. Desligar continua com um toque.");
        vgpIconStyleButton=launch(vgpC,"Ícone do VGP",this::showVgpAppearance);
        launch(vgpC,"Diagnóstico VGP+ (somente leitura)",()->{
            String report=VgpHealthReport.build(Control.vgpOverlay(),
                Control.vgpEnabled(),Control.vgpOptions(),Control.vgpDiagnosticStatus());
            new AlertDialog.Builder(dialogContext())
                .setTitle(dev.pgrazor.PtBr.tr("Diagnóstico Virtual GO Plus"))
                .setMessage(report)
                .setPositiveButton(dev.pgrazor.PtBr.tr("Fechar"),null)
                .show();
        });
        vgpStatus=caption(vgpC,Control.vgpStatus());
        smartRefresh=SmartCatchSettings.populate(activity,vgpC);

        LinearLayout stops = group(page, "PokéStops"); summaryGated.add((View)stops.getParent());
        quickSpinSwitch = switchRow(stops, "Quick spin"); quickSpinSwitch.setChecked(Control.quickSpinEnabled());
        quickSpinSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.quickSpinEnabled())return; Control.setQuickSpinEnabled(quickSpinSwitch.isChecked()); });
        quickSpinStatus = caption(stops, "");
        LinearLayout mateo=group(page,"Rotas");summaryGated.add((View)mateo.getParent());
        routeGiftSwitch=switchRow(mateo,"Trocar presentes com Mateo");
        routeGiftSwitch.setChecked(Control.routeGiftExchange());
        routeGiftSwitch.setOnCheckedChangeListener((v,checked)->{
            if(checked!=Control.routeGiftExchange())Control.setRouteGiftExchange(checked);
        });
        caption(mateo,"Opcional: durante uma Rota, troca o presente com Mateo usando os comandos normais do jogo. Não negocia Pokémon e não ativa outras automações.");

        // ----- Inventory (opens its modal directly) -----
        LinearLayout itemsC = group(page, "Items"); summaryGated.add((View)itemsC.getParent());
        itemSettings = button("Auto-transfer items"); itemsC.addView(itemSettings); itemSettings.setOnClickListener(v -> showItemSettings());
        itemStatus = caption(itemsC, "");
    }

    private void buildBattlesPage() {
        LinearLayout page = page(MenuIcon.BATTLE, "Battles");
        unavailableNote(page, false);
        LinearLayout battleC = plain(page); summaryGated.add(battleC);
        LinearLayout all = group(battleC, "All battles");
        buildSmartBattle(all);
        buildSmartAttack(all);
        buildHealing(all);

        // ----- Team Rocket -----
        LinearLayout rocketC = group(battleC, "Team Rocket");
        String[] rocketLabels={"Auto quick attack","Auto charged attack","Auto shield"};
        for(int i=0;i<3;i++){final int bit=1<<i;CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(rocketLabels[i]));box.setChecked((Control.rocketOptions()&bit)!=0);rocketChoices[i]=box;rocketC.addView(box);
            box.setOnClickListener(v->{
                Control.setRocketOptions(box.isChecked()?Control.rocketOptions()|bit:Control.rocketOptions()&~bit);
                refreshRocket();
                refreshPvpBattles();
            });}
        caption(rocketC, "Ataques e escudos são compartilhados com PvP. A execução automática Rocket só funciona quando o controlador Rocket está pronto.");
        rocketStatus = caption(rocketC, "");

        // PvP has the same three action flags as Rocket, with its own runtime gate.
        LinearLayout pvpBattleC=group(battleC,"PvP · Liga de Batalha GO");
        String[] pvpBattleLabels={"Ataque ágil automático","Ataque carregado automático","Escudo automático"};
        for(int i=0;i<3;i++){
            final int option=1<<i;
            CheckBox box=new CheckBox(activity);
            box.setText(pvpBattleLabels[i]);
            box.setChecked((Control.pvpBattleOptions()&option)!=0);
            pvpBattleChoices[i]=box;pvpBattleC.addView(box);
            box.setOnClickListener(v->{
                Control.setPvpBattleOptions(
                    PvpBattleMode.update(Control.pvpBattleOptions(),option,box.isChecked()));
                refreshPvpBattles();
                refreshRocket();
            });
        }
        caption(pvpBattleC,"Ataque ágil, carregado e escudo são compartilhados com Rocket. O executor VNext PvP é experimental; a escolha supereficaz do segundo golpe ainda não está integrada.");
        pvpBattleStatus=caption(pvpBattleC,"");

        LinearLayout gymC = group(battleC, "Gyms");
        launch(gymC, "Gym Slotter & Berry Feeder", () -> GymAutomationSettings.show(activity));
        battleChoice(gymC,2,"Auto quick attack");
        battleChoice(gymC,3,"Auto charged attack");
        battleChoice(gymC,8,"Auto dodge");
        gymRepeatSwitch = switchRow(gymC, "Battle until defeated"); gymRepeatSwitch.setChecked(Control.gymRepeatEnabled());
        gymRepeatSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.gymRepeatEnabled())return; Control.setGymRepeatEnabled(checked); });
        caption(gymC, "After you start a gym battle, automatically starts the next full round from the same gym screen until defeated. Uses your attack settings and waits for Smart healing when set to After battle or Both. Turn off or leave manually to stop.");
        launch(gymC, "Remote Gym Feeding", () -> AutomationPanel.showRemoteFeed(activity));

        LinearLayout raidC = group(battleC, "Raids");
        battleChoice(raidC,0,"Auto quick attack");
        battleChoice(raidC,1,"Auto charged attack");
        battleChoice(raidC,7,"Auto dodge");
        launch(raidC, "Auto-Raid", () -> AutoRaidDialog.show(activity));

        LinearLayout maxC = group(battleC, "Dynamax");
        battleChoice(maxC,4,"Auto quick attack");
        battleChoice(maxC,5,"Auto charged attack");
        battleChoice(maxC,6,"Auto Max attack");
        battleChoice(maxC,9,"Auto dodge");
        caption(maxC,"Automatically moves toward nearby Max energy orbs during battle while PGBR is active.");
        battleStatus = caption(battleC, "");
    }

    private void buildSmartBattle(LinearLayout owner) {
        smartBattleSwitch = switchRow(owner, "Smart battling"); smartBattleSwitch.setChecked(Control.smartBattle());
        smartBattleSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.smartBattle())return; Control.setSmartBattle(checked); refreshSmartBattle(); });
        caption(owner, "Escolhe a equipe para Ginásios, Raides e Rocket e troca automaticamente em Ginásios e Rocket. Os demais modos exigem suporte próprio validado.");
        smartBattleStatus = caption(owner, "");
        refreshSmartBattle();
    }

    private void refreshSmartBattle() {
        if(smartBattleSwitch==null)return;
        smartBattleSwitch.setChecked(Control.smartBattle());
        String status=Control.smartBattleStatus(); smartBattleStatus.setText(dev.pgrazor.PtBr.tr(status)); smartBattleStatus.setVisibility(status.isEmpty()?View.GONE:View.VISIBLE);
    }

    private void buildSmartAttack(LinearLayout owner){
        smartAttackSwitch=switchRow(owner,"Smart attacking");
        smartAttackSwitch.setChecked(Control.smartAttack());
        smartAttackSwitch.setOnCheckedChangeListener((v,on)->{
            if(on!=Control.smartAttack())Control.setSmartAttack(on);
        });
        caption(owner,"Um único controle para todos os modos: Super eficaz > Neutro > Pouco eficaz. "+
            "A preferência é salva, mas a seleção nativa dos botões ainda não está integrada "+
            "nem validada para todos os modos. Ataques carregados continuam instantâneos.");
        shieldWaitButton=launch(owner,"Espera do Escudo",()->
            new AlertDialog.Builder(dialogContext())
                .setTitle("Espera do Escudo Inteligente")
                .setSingleChoiceItems(SHIELD_WAIT_LABELS,Control.smartShieldTiming(),(dialog,index)->{
                    Control.setSmartShieldTiming(index);dialog.dismiss();refreshBattleTimings();
                }).setNegativeButton("Cancelar",null).show());
        faintedSwapWaitButton=launch(owner,"Troca após Nocaute",()->
            new AlertDialog.Builder(dialogContext())
                .setTitle("Troca Inteligente após Nocaute")
                .setSingleChoiceItems(FAINTED_SWAP_LABELS,Control.smartFaintedSwapTiming(),(dialog,index)->{
                    Control.setSmartFaintedSwapTiming(index);dialog.dismiss();refreshBattleTimings();
                }).setNegativeButton("Cancelar",null).show());
        caption(owner,"Escolhas manuais têm prioridade. Para desligar o uso automático, desmarque Ágil, Carregado e/ou Escudo nas opções de batalha. "+
            "Escudo: 250 ms de intervalo técnico após usar; troca: até 8 s com 1 s entre tentativas. "+
            "As preferências só atuam em fluxos nativos reconhecidos, sem atrasar golpes carregados.");
        refreshBattleTimings();
    }

    private void refreshBattleTimings(){
        if(shieldWaitButton!=null)shieldWaitButton.setText(dev.pgrazor.PtBr.tr(
            "Espera do Escudo: "+SHIELD_WAIT_LABELS[Control.smartShieldTiming()]));
        if(faintedSwapWaitButton!=null)faintedSwapWaitButton.setText(dev.pgrazor.PtBr.tr(
            "Troca após Nocaute: "+FAINTED_SWAP_LABELS[Control.smartFaintedSwapTiming()]));
    }

    private void buildHealing(LinearLayout owner) {
        healSwitch = switchRow(owner, "Smart healing"); healSwitch.setChecked(Control.healEnabled());
        healSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.healEnabled())return; Control.setHealEnabled(checked); refreshHealing(); });
        caption(owner,"Usa itens após batalhas ou no mapa quando permitido; poções e revives não são usados durante combate.");
        healWhen = button(""); owner.addView(healWhen);
        healWhen.setOnClickListener(v -> { Control.setHealWhen((Control.healWhen()+1)%3); refreshHealing(); });
        healThreshold = button(""); owner.addView(healThreshold);
        healThreshold.setOnClickListener(v -> { Control.cycleHealThreshold(); refreshHealing(); });
        healItems = button(""); owner.addView(healItems);
        healItems.setOnClickListener(v -> { Control.setHealFull(!Control.healFull()); refreshHealing(); });
        healReviveSwitch = switchRow(owner, "Revive fainted Pokémon"); healReviveSwitch.setChecked(Control.healRevive());
        healReviveSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.healRevive())return; Control.setHealRevive(checked); });
        healStatus = caption(owner, "");
        refreshHealing();
    }

    private void refreshHealing() {
        if(healSwitch==null)return;
        healSwitch.setChecked(Control.healEnabled()); healReviveSwitch.setChecked(Control.healRevive());
        healWhen.setText(dev.pgrazor.PtBr.tr("Heal: "+Control.HEAL_WHEN_LABELS[Control.healWhen()]));
        healThreshold.setText(dev.pgrazor.PtBr.tr(Control.healThreshold()>=100?"Heal when: Any HP missing":"Heal when: Below "+Control.healThreshold()+"% HP"));
        healItems.setText(dev.pgrazor.PtBr.tr(Control.healFull()?"Items: Always heal fully":"Items: Smart (saves big potions)"));
        boolean on=Control.healEnabled();
        SettingsUi.enabled(healWhen,on); SettingsUi.enabled(healThreshold,on); SettingsUi.enabled(healItems,on); SettingsUi.enabled(healReviveSwitch,on);
        String status=Control.healStatus(); healStatus.setText(dev.pgrazor.PtBr.tr(status)); healStatus.setVisibility(status.isEmpty()?View.GONE:View.VISIBLE);
    }

    private void battleChoice(LinearLayout owner,int index,String label) {
        // Keep the persisted/native option bits unchanged when moving controls between groups.
        final int bit=1<<index;
        CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(label));
        box.setChecked((Control.battleOptions()&bit)!=0);battleChoices[index]=box;owner.addView(box);
        box.setOnClickListener(v->Control.setBattleOptions(box.isChecked()?Control.battleOptions()|bit:Control.battleOptions()&~bit));
    }

    private void buildSocialPage() {
        LinearLayout page = page(MenuIcon.HEART, "Buddy, Eggs & Friends");
        unavailableNote(page, false);

        LinearLayout buddyC=group(page,"Buddy Emotion Engine"); summaryGated.add((View)buddyC.getParent());
        buddyRefresh=BuddyEmotionSettings.populate(activity,buddyC);
        buddyStatus=caption(buddyC,"");

        LinearLayout eggsC = group(page, "Eggs");
        incubatorSwitch = switchRow(eggsC, "Auto incubate");
        incubatorSwitch.setChecked(Control.autoIncubate());
        incubatorSwitch.setOnCheckedChangeListener((v, checked) -> {
            if (checked != Control.autoIncubate()) Control.setAutoIncubate(checked);
        });
        incubatorPriority = button("Priority: shortest distance"); eggsC.addView(incubatorPriority);
        incubatorPriority.setOnClickListener(v -> { Control.setIncubateLongest(!Control.incubateLongest()); host.refresh(); });
        caption(eggsC, "Unlimited incubator only");
        incubatorStatus = caption(eggsC, Control.incubatorStatus());

        LinearLayout giftC = group(page, "Friend gifts"); summaryGated.add((View)giftC.getParent());
        String[] giftLabels={"Auto open gifts","Auto send gifts","Add an owned sticker"};
        for(int i=0;i<3;i++){final int bit=1<<i;CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(giftLabels[i]));box.setChecked((Control.giftOptions()&bit)!=0);giftChoices[i]=box;giftC.addView(box);box.setOnClickListener(v->Control.setGiftOptions(box.isChecked()?Control.giftOptions()|bit:Control.giftOptions()&~bit));}
        giftStatus = caption(giftC, "");
        launch(giftC, "Send & Open Gifts", () -> GiftBatchDialog.show(activity));
    }

    private void buildAutomationPage() {
        // One-shot manual tasks; mirrors the quick-menu Automation hub, plus Research.
        LinearLayout page = page(MenuIcon.AUTOMATION, "Automation");
        caption(page, "Each task runs when you start it.");
        launch(page, "Shundo Sniper (Experimental)", () -> ShundoSniper.show(activity));
        launch(page, "Shiny Scanner (Experimental)", () -> ShinyScanner.show(activity));
        launch(page, "Auto-Raid", () -> AutoRaidDialog.show(activity));
        launch(page, "Daily Incense Session", () -> AutomationPanel.showIncense(activity));
        launch(page, "Remote Gym Feeding", () -> AutomationPanel.showRemoteFeed(activity));
        launch(page, "Send & Open Gifts", () -> GiftBatchDialog.show(activity));
        launch(page, "Mass Evolve", () -> BatchEvolutionSettings.show(activity));
        LinearLayout research = group(page, "Field research");
        launch(research, "Research", () -> ResearchSettings.show(activity));
        LinearLayout gpxResearch = group(page, "GPX e missões");
        caption(gpxResearch, "Importe uma rota GPX, escolha as pesquisas que deseja manter e configure o arremesso Excelente no VGP+. A caminhada por GPX, o giro de Poképaradas e as pesquisas ainda são funções independentes.");
        launch(gpxResearch, "Importar GPX / caminhada automática", () -> AutoWalkDialog.show(activity));
        launch(gpxResearch, "Selecionar missões desejadas", () -> ResearchSettings.show(activity));
        launch(gpxResearch, "Configurar arremesso Excelente", () -> SmartCatchSettings.show(activity));
    }

    private void buildEventsPage() {
        LinearLayout page = page(MenuIcon.EVENTS, "Events");
        caption(page, "Settings for limited-time events.");
        Switch eventShortcut=switchRow(page,"Mostrar Eventos no Menu Pokébola");
        eventShortcut.setChecked(Control.quickMenuVisible(QuickMenuVisibilityPolicy.EVENTS_BIT));
        eventShortcut.setOnCheckedChangeListener((button,checked)->{
            if(checked==Control.quickMenuVisible(QuickMenuVisibilityPolicy.EVENTS_BIT))return;
            Control.setQuickMenuVisible(QuickMenuVisibilityPolicy.EVENTS_BIT,checked);
            // Keep the Display > Quick menu entries checkbox synchronized, if built.
            if(quickMenuChoices[9]!=null)quickMenuChoices[9].setChecked(checked);
            host.refresh();
        });
        launch(page,"Abrir seletor de Eventos",()->
            dev.pgrazor.events.EventPanel.showSelector(activity,Control.darkTheme()));
        caption(page,"Halloween: coletor só ativa quando o runtime informa disponibilidade; "
            +"GO Wild: Safari Balls apenas para Mighty, com VGP+ ou Auto Ball ativado. "
            +"Rota Eternatus: exige GPX e confirmação real das pesquisas.");
        launch(page, "Halloween 2026", () -> dev.pgrazor.events.EventPanel.showHalloween(activity, Control.darkTheme()));
        launch(page, "Go Wild November 2026", () -> dev.pgrazor.events.EventPanel.showGoWild(activity, Control.darkTheme()));
        launch(page, "Evento Rota Doces Eternatus", () -> dev.pgrazor.events.EventPanel.showEternatus(activity, Control.darkTheme()));
    }

    private interface VisualChange{void set(int value);}
    private void visualSlider(LinearLayout owner,String title,int min,int max,int initial,VisualChange change){
        TextView label=new TextView(activity);label.setTextSize(13);
        label.setText(dev.pgrazor.PtBr.tr(title+": "+initial+(title.contains("Opacidade")?"%":" dp")));
        owner.addView(label);
        SeekBar slider=new SeekBar(activity);slider.setMax(max-min);slider.setProgress(initial-min);
        owner.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                if(!fromUser)return;int value=min+progress;change.set(value);
                label.setText(dev.pgrazor.PtBr.tr(title+": "+value+(title.contains("Opacidade")?"%":" dp")));
            }
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });
    }
    private void buildDisplayPage() {
        LinearLayout page = page(MenuIcon.SETTINGS, "Display & Performance");
        darkSwitch = switchRow(page, "Dark theme"); darkSwitch.setChecked(Control.darkTheme());
        darkSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.darkTheme())return; Control.setDarkTheme(checked); host.applyTheme(); });
        uiScale=button("UI scale: "+Control.uiScalePercent()+"%");page.addView(uiScale);
        uiScale.setOnClickListener(v->{final int[] values={100,75,50,25};String[] labels={"100%","75%","50%","25%"};int selected=0;for(int i=0;i<values.length;i++)if(values[i]==Control.uiScalePercent())selected=i;
            new AlertDialog.Builder(dialogContext()).setTitle(dev.pgrazor.PtBr.tr("Overlay scale")).setSingleChoiceItems(labels,selected,(dialog,which)->{host.releaseJoystick(true);Control.setUiScalePercent(values[which]);host.refresh();dialog.dismiss();}).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).show();});
        skipCutscenesSwitch = switchRow(page, "Skip cutscenes"); skipCutscenesSwitch.setChecked(Control.skipCutscenes());
        skipCutscenesSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked==Control.skipCutscenes())return; Control.setSkipCutscenes(checked); });
        cutsceneStatus = caption(page, "");  // shown only when the native module reports an error

        // The game UI hook is a best-effort compatibility probe, not an attested privacy shield.
        LinearLayout privacy=group(page,"Privacidade do treinador");
        caption(privacy,"Oculta o nome nos menus do PGBR e tenta substituir somente o texto "
            +"do próprio treinador por ***** em componentes de exibição do Unity. "
            +"A cobertura do mapa e do perfil Eu ainda precisa ser confirmada no aparelho. "
            +"Não considere esta opção uma proteção garantida para lives e gravações.");
        final Switch hideTrainer=switchRow(privacy,"Ocultar nome visualmente");
        final TextView preview=caption(privacy,"");
        final TextView gamePrivacy=caption(privacy,"");
        Runnable updatePrivacy=()->{
            hideTrainer.setChecked(Control.trainerPrivacyMode()!=0);
            preview.setText(dev.pgrazor.PtBr.tr(
                "Exibição no PGBR: "+(Control.trainerPrivacyMode()==1?"*****":
                    Control.trainerPrivacyMode()==2?Control.trainerAlias():"Nome original")));
            int status=Control.trainerPrivacyDisplayStatus();
            int sinks=status & 0x0F;
            String detail=status<0?"Módulo visual indisponível":
                (status & 0x10)==0?"Aguardando confirmação da identidade do treinador":
                sinks==0?"Nenhum componente de texto Unity validado":
                sinks+" componente(s) de texto Unity interceptado(s)";
            int nativeStreamer=status<0?0:(status>>>8)&3;
            String streamer=nativeStreamer==3
                ?"metadados do serviço e seus dois indicadores reconhecidos, sem ativação"
                :nativeStreamer==2
                ?"classe localizada, porém assinatura dos indicadores ainda incompatível"
                :nativeStreamer==1
                ?"classe não encontrada nesta versão do jogo"
                :"aguardando leitura de compatibilidade";
            gamePrivacy.setText(dev.pgrazor.PtBr.tr(
                "Jogo: "+detail+". Proteção em tela não verificada. "
                +"Modo Streamer nativo: "+streamer+". "
                +"A detecção do serviço não comprova ocultação do nome do treinador."));
        };
        hideTrainer.setOnCheckedChangeListener((button,checked)->{
            if(checked==(Control.trainerPrivacyMode()!=0))return;
            Control.setTrainerPrivacyMode(checked?1:0);
            updatePrivacy.run();host.refresh();
        });
        launch(privacy,"Nome exibido nos menus do PGBR",()->{
            String[] options={"Nome original","Ocultar nome no PGBR","Mostrar apelido no PGBR"};
            new AlertDialog.Builder(dialogContext()).setTitle("Nome exibido no PGBR")
                .setSingleChoiceItems(options,Control.trainerPrivacyMode(),(d,choice)->{
                    Control.setTrainerPrivacyMode(choice);updatePrivacy.run();host.refresh();d.dismiss();
                }).setNegativeButton("Cancelar",null).show();
        });
        launch(privacy,"Definir meu apelido",()->{
            EditText name=new EditText(dialogContext());
            name.setSingleLine(true);
            name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
            name.setText(Control.trainerAlias());
            name.setHint("PGBR Treinador");
            AlertDialog aliasDialog=new AlertDialog.Builder(dialogContext()).setTitle("Apelido local")
                .setMessage("De 2 a 24 caracteres: letras, números, espaços, ponto, traço ou sublinhado.")
                .setView(name).setNegativeButton("Cancelar",null)
                .setPositiveButton("Salvar",null).create();
            aliasDialog.setOnShowListener(d->aliasDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v->{
                    if(!Control.setCustomTrainerAlias(name.getText().toString())){
                        name.setError(dev.pgrazor.PtBr.tr("Use de 2 a 24 caracteres válidos."));return;
                    }
                    updatePrivacy.run();host.refresh();aliasDialog.dismiss();
                }));
            aliasDialog.show();
        });
        launch(privacy,"Gerar apelido PGBR aleatório",()->{
            Control.regenerateTrainerAlias();Control.setTrainerPrivacyMode(2);
            updatePrivacy.run();host.refresh();
        });
        updatePrivacy.run();
        LinearLayout battery = group(page, "Battery usage");
        caption(battery, "Lower graphics to save battery and keep the phone cooler. Each turns back to the game's own setting when switched off.");
        batteryResSwitch = switchRow(battery, "Lower resolution"); batteryResSwitch.setChecked(Control.batteryLowRes());
        batteryResSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked!=Control.batteryLowRes())Control.setBatteryLowRes(checked); });
        caption(battery, "Draws the map and Pokémon at half resolution. Menus and text stay sharp.");
        batteryFpsSwitch = switchRow(battery, "Limit to 20 FPS"); batteryFpsSwitch.setChecked(Control.batteryFps20());
        batteryFpsSwitch.setOnCheckedChangeListener((v,checked) -> { if(checked!=Control.batteryFps20())Control.setBatteryFps20(checked); });
        batteryQualitySwitch = switchRow(battery, "Low graphics quality"); batteryQualitySwitch.setChecked(Control.batteryLowQuality());
        batteryQualitySwitch.setOnCheckedChangeListener((v,checked) -> { if(checked!=Control.batteryLowQuality())Control.setBatteryLowQuality(checked); });
        caption(battery, "Turns off shadows, fog, water reflections, grass in encounters, HDR and anti-aliasing, and shows less far detail.");

        LinearLayout appearance=group(page,"Personalização dos ícones e fundos");
        visualSlider(appearance,"Tamanho dos ícones dos menus",16,40,Control.menuIconSizeDp(),value->{
            Control.setMenuIconSizeDp(value);host.refresh();});
        launch(appearance,"Ícone do menu principal: PGBR / PGEterna / Personalizado",()->{
            String[] labels={"PGBR (azul pulsante)","PGEterna (efeitos Eternatus)","Imagem escolhida no celular"};
            new AlertDialog.Builder(dialogContext()).setTitle("Ícone do menu principal")
                .setSingleChoiceItems(labels,Control.menuLauncherIconStyle(),(dialog,which)->{
                    dialog.dismiss();
                    if(which==2){
                        OverlayArtwork.select(activity,"launcher",()->{Control.setMenuLauncherIconStyle(2);host.refresh();});
                    }else{Control.setMenuLauncherIconStyle(which);host.refresh();}
                }).setNegativeButton("Cancelar",null).show();
        });
        visualSlider(appearance,"Tamanho do botão principal",28,96,Control.menuLauncherSizeDp(),value->{
            Control.setMenuLauncherSizeDp(value);host.refresh();});
        visualSlider(appearance,"Opacidade do ícone principal",10,100,Control.menuLauncherOpacity(),value->{
            Control.setMenuLauncherOpacity(value);host.refresh();});
        visualSlider(appearance,"Opacidade do fundo principal",0,100,Control.menuLauncherBgOpacity(),value->{
            Control.setMenuLauncherBgOpacity(value);host.refresh();});
        visualSlider(appearance,"Tamanho do ícone de ginásios",24,110,Control.gymIconSizeDp(),value->{
            Control.setGymIconSizeDp(value);host.refresh();});
        visualSlider(appearance,"Opacidade do ícone de ginásios",10,100,Control.gymIconOpacity(),value->{
            Control.setGymIconOpacity(value);host.refresh();});
        visualSlider(appearance,"Opacidade do fundo de ginásios",0,100,Control.gymBackgroundOpacity(),value->{
            Control.setGymBackgroundOpacity(value);host.refresh();});
        launch(appearance,"Exibir ou ocultar ícone do ginásio e frutas",()->GymAutomationSettings.show(activity));
        launch(appearance,"Escolher símbolo do ginásio no celular",()->OverlayArtwork.select(activity,"gym",host::refresh));
        launch(appearance,"Escolher fruta do botão no celular",()->OverlayArtwork.select(activity,"berry",host::refresh));
        launch(appearance,"Restaurar símbolos originais do ginásio e fruta",()->{
            OverlayArtwork.reset(activity,"gym",()->OverlayArtwork.reset(activity,"berry",host::refresh));
        });
        visualSlider(appearance,"Opacidade dos fundos flutuantes",0,100,Control.overlayBackgroundOpacity(),value->{
            Control.setOverlayBackgroundOpacity(value);host.applyTheme();host.refresh();});
        caption(appearance,"0% deixa os fundos flutuantes transparentes. Janelas de confirmação permanecem legíveis para evitar ações acidentais.");
        LinearLayout menu = group(page,"Quick menu entries");
        caption(menu,"Choose which entries appear. This does not turn features on or off. Settings stays visible.");
        for(int i=0;i<QuickMenu.CUSTOMIZABLE.length;i++){
            final int bit=1<<i;
            CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr(QuickMenu.CUSTOMIZABLE[i]));
            box.setChecked(Control.quickMenuVisible(bit));quickMenuChoices[i]=box;menu.addView(box);
            box.setOnClickListener(v->{Control.setQuickMenuVisible(bit,box.isChecked());host.refresh();});
        }
    }

    private void buildAccountsPage() {
        LinearLayout page = page(MenuIcon.ACCOUNTS, "Accounts & Backup");
        launch(page, "Accounts", () -> AccountsDialog.show(activity));
        launch(page, "Export / import settings", () -> {
            if(PgAssets.rooted())new SettingsBackup(activity).show();
            else activity.startActivity(new Intent(activity,LogActivity.class).putExtra("settingsBackup",true));
        });
        launch(page, "Run log / export", () -> LogActivity.open(activity));
        launch(page, "Verificar atualizações do PGBR", () -> UpdateChecker.check(activity));
        Switch autoUpdateNotice=switchRow(page,"Avisar automaticamente sobre novas versões");
        autoUpdateNotice.setChecked(UpdateChecker.automaticEnabled(activity));
        autoUpdateNotice.setOnCheckedChangeListener((button,on)->UpdateChecker.setAutomaticEnabled(activity,on));
        caption(page,"O aviso aparece automaticamente ao abrir o PGBR quando há uma versão pública mais recente. Baixar e instalar exigem confirmação.");

        // ----- Diagnostics (status plumbing kept out of everyday settings) -----
        LinearLayout diag = section(page, "Diagnostics", false);
        state = caption(diag, Control.connection);
        for(TextView status:new TextView[]{steeringStatus,spawnStatus,quickSummaryStatus,transferStatus,quickSpinStatus,buddyStatus,giftStatus,itemStatus,rocketStatus,battleStatus,quickBallStatus}){
            ((ViewGroup)status.getParent()).removeView(status);diag.addView(status);
        }
        cameraRoot = new LinearLayout(activity); cameraRoot.setOrientation(LinearLayout.VERTICAL); diag.addView(cameraRoot);
        cameraStatus = caption(cameraRoot, ""); cameraStatus.setMaxLines(3); cameraStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cameraSample = button("Sample camera (10s)"); cameraRoot.addView(cameraSample);
        cameraSample.setOnClickListener(v -> { if (Control.requestCameraSample()) { host.releaseJoystick(false); host.refresh(); } });
        interactionRoot = new LinearLayout(activity); interactionRoot.setOrientation(LinearLayout.VERTICAL); diag.addView(interactionRoot);
        interactionStatus = caption(interactionRoot, ""); interactionStatus.setMaxLines(3); interactionStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        interactionCapture = button("Capture catch/spin (60s)"); interactionRoot.addView(interactionCapture);
        interactionCapture.setOnClickListener(v -> {
            if (Control.interactionDiagnosticRunning()) Control.requestInteractionStop();
            else if (Control.requestInteractionTrace()) host.releaseJoystick(false);
            host.refresh();
        });
    }

    /** Home-row summary: the features that are on, or a plain "All off". Arguments alternate name, state. */
    private static String active(Object... pairs){
        StringBuilder out=new StringBuilder();
        for(int i=0;i+1<pairs.length;i+=2)if(Boolean.TRUE.equals(pairs[i+1])){if(out.length()>0)out.append(" · ");out.append(pairs[i]);}
        return out.length()==0?"All off":out.toString();
    }
    private void summary(int index,String value){TextView view=pageSummaries.get(index);if(!value.contentEquals(view.getText()))view.setText(dev.pgrazor.PtBr.tr(value));}
    private void refreshSessionStats(){
        if(sessionStatsSummary==null)return;
        long window=sessionStats24h?SessionStatsHistory.DAY_MS:SessionStatsHistory.HOUR_MS;
        SessionStatsHistory.Change changes=SessionStats.change(window);
        sessionStatsToggle.setText(sessionStats24h?"Ver última hora":"Ver últimas 24 horas");
        if(changes==null){
            sessionStatsSummary.setText("Aguardando estatísticas do jogo…");
            return;
        }
        java.util.Locale pt=new java.util.Locale("pt","BR");
        sessionStatsSummary.setText(String.format(pt,
            "XP +%,d · Poeira +%,d\nCapturas %,d · Encontros %,d\nPoképaradas %,d · Caminhada %.2f km · Ovos %,d",
            Math.round(changes.get(SessionStatsHistory.XP)),
            Math.round(changes.get(SessionStatsHistory.DUST)),
            Math.round(changes.get(SessionStatsHistory.CAUGHT)),
            Math.round(changes.get(SessionStatsHistory.SEEN)),
            Math.round(changes.get(SessionStatsHistory.STOPS)),
            changes.get(SessionStatsHistory.KM),
            Math.round(changes.get(SessionStatsHistory.HATCHED))));
    }
    private void refreshHome(){
        if(current>=0)return;
        refreshSessionStats();
        summary(0,(Control.desiredEnabled()?"Spoofing on":"Spoofing off")+String.format(Locale.US," · %.1f km/h",Control.engine.speedKmh));
        summary(1,"Relógio local, tempo de espera e legendas do mapa");
        summary(2,active("Quick catch",Control.quickSummaryEnabled(),"Auto throw",Control.autoThrow(),"Auto ball",Control.autoBall(),"Curveball",Control.throwCurve(),"Always hit",Control.throwAlwaysHit(),"Berries",Control.encounterBerryEnabled()));
        summary(3,active("Auto-transfer",Control.autoTransferEnabled(),"Evolve first",Control.autoTransferEnabled()&&Control.evolveTransferEnabled(),"IVs shown",Control.ivCatchMode()!=0||Control.ivOwnedMode()!=0||Control.storageIvMode()!=0));
        summary(4,active("Go Plus overlay",Control.vgpOverlay(),"Quick spin",Control.quickSpinEnabled(),"Item cleanup",Control.itemTransferEnabled()));
        summary(5,active("Smart battling",Control.smartBattle(),"Smart healing",Control.healEnabled(),"Auto attacks",Control.battleOptions()!=0||Control.rocketOptions()!=0,"Gym repeat",Control.gymRepeatEnabled()));
        summary(6,active("Buddy engine",BuddyEmotionSettings.enabled(),"Auto incubate",Control.autoIncubate(),"Open gifts",(Control.giftOptions()&1)!=0,"Send gifts",(Control.giftOptions()&2)!=0));
        summary(7,AutoRaid.running()?"Auto-Raid running":ShundoSniper.active()?"Shundo Sniper running":ShinyScanner.enabled()?"Shiny Scanner on":"Shundo, shiny scan, raids, incense, gym feeding, gifts, evolve, research");
        summary(8,"Halloween · Go Wild");
        summary(9,"UI "+Control.uiScalePercent()+"% · "+(Control.darkTheme()?"Dark":"Light")+" theme"+(Control.skipCutscenes()?" · Skip cutscenes":""));
        summary(10,"Accounts, export / import, run log, diagnostics");
    }

    /** The root panel view; Pad owns attachment to the window and insets wiring. */
    View view(){return panel;}
    boolean isVisible(){return panel.getVisibility()==View.VISIBLE;}
    /** Bring the full-screen panel up; Pad applies current insets immediately after. */
    void show(){homeScroll=0;open(-1);panel.setVisibility(View.VISIBLE);panel.bringToFront();panel.requestApplyInsets();}
    /** Hidden state matches the original startup and the close affix: returns to the game underneath. */
    void hide(){panel.setVisibility(View.GONE);}
    /** Release this panel's dialog polling when its owning activity panel is detached. */
    void close(){
        if(berryPicker!=null)berryPicker.close();
        for(AlertDialog dialog:new java.util.ArrayList<>(itemDialogs))dialog.dismiss();
        itemDialogs.clear();handler.removeCallbacksAndMessages(null);
    }

    /** Pads the full-screen settings panel inside the status bar, navigation bar, and IME. */
    void applyInsets(WindowInsets insets) {
        if(insets==null)return;
        int top,bottom,left,right;
        if(Build.VERSION.SDK_INT>=30){
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());
            android.graphics.Insets safe=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            top=Math.max(bars.top,safe.top);bottom=Math.max(bars.bottom,safe.bottom);left=Math.max(bars.left,safe.left);right=Math.max(bars.right,safe.right);
        }else{
            top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
            left=insets.getSystemWindowInsetLeft();right=insets.getSystemWindowInsetRight();
        }
        if(top==0){int id=activity.getResources().getIdentifier("status_bar_height","dimen","android");top=id!=0?activity.getResources().getDimensionPixelSize(id):dp(28);}
        panel.setPadding(dp(12)+left,dp(8)+top,dp(12)+right,dp(12)+bottom);
    }

    void applyTheme(){
        panel.setBackgroundColor(surface());styleTree(panel);
        for(TextView head:sectionHeads){head.setTextColor(accent());head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);}
        for(TextView c:captions)c.setTextColor(muted());
        if(coords!=null)coords.setTextColor(ink());
        for(LinearLayout row:homeRows)MapTheme.setButtonBackground(row);
        for(TextView name:homeNames)name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        for(MenuIcon icon:homeIcons){
            ViewGroup.LayoutParams lp=icon.getLayoutParams();
            int size=dp(Control.menuIconSizeDp()+4);
            if(lp!=null&&(lp.width!=size||lp.height!=size)){lp.width=size;lp.height=size;icon.setLayoutParams(lp);}
            icon.invalidate();
        }
        if(backIcon!=null)backIcon.invalidate();
        if(closeIcon!=null)closeIcon.invalidate();
        if(themeIcon!=null){themeIcon.setContentDescription(dev.pgrazor.PtBr.tr(Control.darkTheme()?"Switch to light theme":"Switch to dark theme"));themeIcon.invalidate();}
    }

    private void showItemSettings(){
        Control.requestItemCatalog();
        LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(8),dp(16),dp(8));
        CheckBox enabled=new CheckBox(activity);enabled.setText(dev.pgrazor.PtBr.tr("Enable automatic item transfer"));enabled.setChecked(Control.itemTransferEnabled());form.addView(enabled);
        TextView explanation=new TextView(activity);explanation.setText(dev.pgrazor.PtBr.tr("Check items to trim and set the maximum to keep. Saving applies to your current bag and future items. A limit of 0 removes all of that item. Unchecked items are kept."));form.addView(explanation);
        java.util.Map<Integer,Integer> limits=new java.util.HashMap<>();java.util.Set<Integer> selected=new java.util.HashSet<>();
        for(String row:Control.itemLimits().split(";"))try{String[] b=row.split(":");limits.put(Integer.parseInt(b[0]),Integer.parseInt(b[1]));}catch(RuntimeException ignored){}
        for(String row:Control.itemRules().split(";"))try{String[] b=row.split(":");int id=Integer.parseInt(b[0]);selected.add(id);limits.put(id,Integer.parseInt(b[1]));}catch(RuntimeException ignored){}
        LinearLayout rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(activity);scroll.addView(rows);form.addView(scroll,new LinearLayout.LayoutParams(-1,dp(380)));
        java.util.Map<Integer,CheckBox> boxes=new java.util.LinkedHashMap<>();java.util.Map<Integer,EditText> inputs=new java.util.LinkedHashMap<>();
        Runnable populate=()->{for(String row:Control.itemCatalog().split("\n")){String[] b=row.split("\\|",-1);if(b.length!=3)continue;int id;try{id=Integer.parseInt(b[0]);}catch(NumberFormatException invalid){continue;}if(boxes.containsKey(id)){boxes.get(id).setText(dev.pgrazor.PtBr.tr((dev.pgrazor.PtBr.tr(b[1])+" ("+b[2]+" na bolsa)")));continue;}
            LinearLayout line=new LinearLayout(activity);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);
            ItemBadge itemArt=new ItemBadge(activity,id);
            LinearLayout.LayoutParams artLayout=new LinearLayout.LayoutParams(dp(26),dp(26));
            artLayout.rightMargin=dp(4);line.addView(itemArt,artLayout);
            CheckBox box=new CheckBox(activity);box.setText(dev.pgrazor.PtBr.tr((dev.pgrazor.PtBr.tr(b[1])+" ("+b[2]+" na bolsa)")));box.setChecked(selected.contains(id));line.addView(box,new LinearLayout.LayoutParams(0,-2,1));
            itemArt.setAlpha(box.isChecked()?1f:0.70f);
            EditText cap=new EditText(dialogContext());cap.setInputType(InputType.TYPE_CLASS_NUMBER);cap.setSingleLine(true);cap.setHint(dev.pgrazor.PtBr.tr("Max"));cap.setText(dev.pgrazor.PtBr.tr(String.valueOf(limits.containsKey(id)?limits.get(id):100)));cap.setContentDescription(dev.pgrazor.PtBr.tr((dev.pgrazor.PtBr.tr("Maximum ")+dev.pgrazor.PtBr.tr(b[1])+dev.pgrazor.PtBr.tr(" to keep"))));SettingsUi.enabled(cap,enabled.isChecked()&&box.isChecked());box.setOnCheckedChangeListener((button,checked)->{SettingsUi.enabled(cap,enabled.isChecked()&&checked);itemArt.setAlpha(checked?1f:0.70f);});line.addView(cap,new LinearLayout.LayoutParams(dp(72),-2));rows.addView(line);styleTree(line);box.setTextSize(12);boxes.put(id,box);inputs.put(id,cap);
        }};populate.run();
        Runnable dependencies=()->{for(java.util.Map.Entry<Integer,CheckBox> entry:boxes.entrySet()){SettingsUi.enabled(entry.getValue(),enabled.isChecked());SettingsUi.enabled(inputs.get(entry.getKey()),enabled.isChecked()&&entry.getValue().isChecked());}};
        enabled.setOnCheckedChangeListener((v,on)->dependencies.run());dependencies.run();
        TextView status=new TextView(activity);status.setText(dev.pgrazor.PtBr.tr(Control.itemStatus()));form.addView(status);
        AlertDialog dialog=new AlertDialog.Builder(dialogContext()).setTitle(dev.pgrazor.PtBr.tr("Auto-transfer inventory items")).setView(form).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Save & apply"),null).create();
        final Runnable[] poll=new Runnable[1];poll[0]=()->{if(!dialog.isShowing())return;Control.requestItemCatalog();populate.run();dependencies.run();status.setText(dev.pgrazor.PtBr.tr(Control.itemStatus()));handler.postDelayed(poll[0],1000);};
        itemDialogs.add(dialog);
        dialog.setOnDismissListener(d->{handler.removeCallbacks(poll[0]);itemDialogs.remove(dialog);});dialog.show();styleTree(form);handler.post(poll[0]);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{StringBuilder rules=new StringBuilder(),all=new StringBuilder();for(java.util.Map.Entry<Integer,EditText> e:inputs.entrySet()){int cap;try{cap=Integer.parseInt(e.getValue().getText().toString().trim());if(cap<0||cap>100000)throw new NumberFormatException();}catch(NumberFormatException invalid){e.getValue().setError(dev.pgrazor.PtBr.tr("Enter 0 to 100000"));return;}String rule=e.getKey()+":"+cap+";";all.append(rule);if(boxes.get(e.getKey()).isChecked())rules.append(rule);}
            Control.saveItemRules(enabled.isChecked(),rules.toString(),all.toString());dialog.dismiss();host.refresh();});
    }

    void refresh() {
        refreshHome();
        if(joystickStyleButton!=null){
            String desired="Estilo do joystick: "+joystickStyleName(Control.joystickStyle());
            if(!desired.contentEquals(joystickStyleButton.getText()))
                joystickStyleButton.setText(dev.pgrazor.PtBr.tr(desired));
        }
        for(MenuIcon icon:homeIcons){
            ViewGroup.LayoutParams lp=icon.getLayoutParams();int size=dp(Control.menuIconSizeDp()+4);
            if(lp!=null&&(lp.width!=size||lp.height!=size)){lp.width=size;lp.height=size;icon.setLayoutParams(lp);}
        }
        darkSwitch.setChecked(Control.darkTheme());
        refreshLocationStatus();
        refreshDiagnostics();
        refreshSummaryVisibility();
        refreshTransfer();
        refreshVgpStatus();
        refreshIncubation();
        refreshVgpOptions();
        refreshBuddy();
        refreshGifts();
        refreshBattles();
        refreshRocket();
        refreshPvpBattles();
        refreshItems();
        refreshQuickCatch();
        refreshLocationOptions();
        refreshThrows();
        skipCutscenesSwitch.setChecked(Control.skipCutscenes());cutsceneStatus.setText(dev.pgrazor.PtBr.tr(Control.cutsceneSkipStatus()));
        batteryResSwitch.setChecked(Control.batteryLowRes());batteryFpsSwitch.setChecked(Control.batteryFps20());batteryQualitySwitch.setChecked(Control.batteryLowQuality());
        cutsceneStatus.setVisibility(Control.cutsceneSkipStatus().isEmpty()?View.GONE:View.VISIBLE);
        refreshHealing();
        refreshSmartBattle();
        if(smartAttackSwitch!=null)smartAttackSwitch.setChecked(Control.smartAttack());
        refreshBattleTimings();
        refreshDependencies();
    }

    private void refreshDependencies(){
        for(CheckBox box:vgpChoices)SettingsUi.enabled(box,Control.vgpOverlay());
        smartRefresh.run();
        for(CheckBox box:transferProtection)SettingsUi.enabled(box,Control.autoTransferEnabled());
        SettingsUi.enabled(transferKeepList,Control.autoTransferEnabled());
        SettingsUi.enabled(transferIvLimit,Control.autoTransferEnabled());
        SettingsUi.enabled((View)transferOverlaySwitch.getParent(),Control.autoTransferEnabled());
        // Evolve before transferring needs the auto-transfer master; its options need the feature on, and
        // the selected-species list only matters in "Selected species only" mode.
        SettingsUi.enabled((View)evolveTransferSwitch.getParent(),Control.autoTransferEnabled());
        boolean evolveOn=Control.autoTransferEnabled()&&Control.evolveTransferEnabled();
        SettingsUi.enabled(evolveTransferStages,evolveOn);
        SettingsUi.enabled(evolveTransferMode,evolveOn);
        SettingsUi.enabled(evolveTransferList,evolveOn&&Control.evolveTransferMode()==1);
        SettingsUi.enabled(incubatorPriority,Control.autoIncubate()&&Control.incubatorAvailable());
        SettingsUi.enabled(giftChoices[2],(Control.giftOptions()&2)!=0);
        SettingsUi.enabled((View)joystickSwitch.getParent(),Control.desiredEnabled());
        SettingsUi.enabled(mapTap,Control.desiredEnabled()&&Control.quickBallConfigured());
    }

    private void refreshLocationStatus() {
        if(uiScale!=null)uiScale.setText(dev.pgrazor.PtBr.tr("UI scale: "+Control.uiScalePercent()+"%"));
        coords.setText(dev.pgrazor.PtBr.tr((Control.desiredEnabled()?"":"Saved spoof location\n")+String.format(Locale.US, "%.6f, %.6f\n%.1f km/h", Control.engine.latitude, Control.engine.longitude, Control.engine.speedKmh)));
        MotionEngine.RouteProgress route=Control.engine.routeProgress();
        locationSwitch.setChecked(Control.desiredEnabled());
        String movement=!Control.desiredEnabled()?"DEVICE LOCATION":Control.engine.enabled ? (Control.engine.walking ? "WALKING" : "ON") : "WAITING FOR ADAPTER";
        if (route.total>1 && route.active) movement=String.format(Locale.US,"ROUTE %d/%d  ·  %.0f m left",route.visited+1,route.total,route.remainingMeters);
        else if (route.total>1 && route.visited==route.total) movement="ON  ·  route complete";
        state.setText(dev.pgrazor.PtBr.tr(Control.connection + "\n" + movement + "  ·  coordinate reads " + Control.latitudeReads + "/" + Control.longitudeReads));
        steeringStatus.setText(dev.pgrazor.PtBr.tr(Control.cameraSteeringConfigured() ? Control.cameraSteeringStatus() : "Joystick follows the camera"));
    }

    private void refreshDiagnostics() {
        cameraRoot.setVisibility(Control.cameraDiagnosticsConfigured() ? View.VISIBLE : View.GONE);
        cameraStatus.setText(dev.pgrazor.PtBr.tr(Control.cameraDiagnosticStatus()));
        cameraSample.setText(dev.pgrazor.PtBr.tr(Control.cameraDiagnosticRunning() ? "Sampling camera..." : "Sample camera (10s)"));
        cameraSample.setEnabled(Control.cameraDiagnosticsReady() && !Control.cameraSamplePending());
        interactionRoot.setVisibility(Control.interactionDiagnosticsConfigured() ? View.VISIBLE : View.GONE);
        interactionStatus.setText(dev.pgrazor.PtBr.tr(Control.interactionDiagnosticStatus()));
        interactionCapture.setText(dev.pgrazor.PtBr.tr(Control.interactionDiagnosticRunning() ? "Stop capture" : "Capture catch/spin (60s)"));
        interactionCapture.setEnabled(Control.interactionDiagnosticRunning() || (Control.interactionDiagnosticsReady() && !Control.interactionTracePending()));
    }

    private void refreshSummaryVisibility() {
        boolean summary = Control.quickSummaryConfigured(), ball = Control.quickBallConfigured();
        for(View root:summaryGated)root.setVisibility(summary ? View.VISIBLE : View.GONE);
        for(View root:ballGated)root.setVisibility(ball ? View.VISIBLE : View.GONE);
        for(TextView note:summaryNotes)note.setVisibility(summary ? View.GONE : View.VISIBLE);
        for(TextView note:ballNotes)note.setVisibility(summary && ball ? View.GONE : View.VISIBLE);
    }

    private void refreshTransfer() {
        quickSummaryStatus.setText(dev.pgrazor.PtBr.tr(Control.quickSummaryStatus()));
        autoTransferSwitch.setChecked(Control.autoTransferEnabled());
        if(transferIvLimit!=null)transferIvLimit.setText(dev.pgrazor.PtBr.tr(Control.transferIvKeepPercent()==101?
            "IV: manter apenas protegidos / marcados":
            "IV: manter "+Control.transferIvKeepPercent()+"% ou mais"));
        for(int i=0;i<transferProtection.length;i++)transferProtection[i].setChecked((Control.transferProtect()&(1<<i))!=0);
        for(int i=0;i<3;i++){pvpKeep[i].setChecked(Control.pvpKeep(i));pvpRank[i].setText(dev.pgrazor.PtBr.tr("Top "+Control.pvpRank(i)));pvpRank[i].setEnabled(Control.pvpKeep(i));}
        if(transferKeepList!=null){String kept=Control.transferKeepSpecies();int n=kept.isEmpty()?0:kept.split(",").length;transferKeepList.setText(dev.pgrazor.PtBr.tr(n>0?"Custom keep list ("+n+")":"Custom keep list"));}
        if(transferOverlaySwitch!=null){transferOverlaySwitch.setChecked(Control.transferOverlayEnabled());transferOverlaySwitch.setEnabled(Control.autoTransferEnabled());transferOverlaySwitch.setAlpha(1f);}
        if(evolveTransferSwitch!=null)evolveTransferSwitch.setChecked(Control.evolveTransferEnabled());
        if(evolveTransferStages!=null)evolveTransferStages.setText(dev.pgrazor.PtBr.tr("Evolve: "+(Control.evolveTransferStages()==2?"all available stages":"once")));
        if(evolveTransferMode!=null)evolveTransferMode.setText(dev.pgrazor.PtBr.tr("Applies to: "+(Control.evolveTransferMode()==1?"selected species only":"current transfer rules")));
        if(evolveTransferList!=null){String sel=Control.evolveTransferSpecies();int n=sel.isEmpty()?0:sel.split(",").length;evolveTransferList.setText(dev.pgrazor.PtBr.tr(n>0?"Selected species ("+n+")":"Selected species"));}
        transferStatus.setText(dev.pgrazor.PtBr.tr(Control.transferStatus()));
    }

    private void refreshVgpStatus() {
        vgpSwitch.setChecked(Control.vgpOverlay());
        if(vgpStartStopButton!=null){
            vgpStartStopButton.setText(dev.pgrazor.PtBr.tr(
                Control.vgpEnabled() ? "Parar VGP+" : "Iniciar VGP+"));
            vgpStartStopButton.setEnabled(Control.vgpEnabled() || Control.vgpOverlay());
        }
        vgpStatus.setText(dev.pgrazor.PtBr.tr(Control.vgpDiagnosticStatus()));
    }

    private void refreshIncubation() {
        incubatorSwitch.setChecked(Control.autoIncubate());
        incubatorSwitch.setEnabled(Control.incubatorAvailable());
        incubatorPriority.setText(dev.pgrazor.PtBr.tr("Priority: " + (Control.incubateLongest() ? "longest" : "shortest") + " distance"));
        incubatorStatus.setText(dev.pgrazor.PtBr.tr(Control.autoIncubate() ? Control.incubatorStatus() : "Off"));
    }

    private void showVgpAppearance(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16),dp(10),dp(16),dp(8));
        TextView hint=new TextView(activity);
        hint.setText("Escolha o ícone. A transparência do fundo é ajustada separadamente; "
                + "0% deixa apenas o desenho, sem o círculo cinza.");
        hint.setTextSize(13);content.addView(hint);
        LinearLayout previews=new LinearLayout(activity);
        previews.setGravity(Gravity.CENTER);previews.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(previews);
        // There are exactly TWO icon designs. The old third option reused the
        // identical vgp-hd-states.webp asset and differed only by background opacity.
        final ImageView[] choices=new ImageView[2];
        final LinearLayout[] tiles=new LinearLayout[2];
        final String[] files={"vgp-ring.png","vgp-hd-states.webp"};
        final String[] labels={"LED","GO Plus HD"};
        for(int i=0;i<choices.length;i++){
            final int style=i;
            LinearLayout tile=new LinearLayout(activity);tiles[i]=tile;
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            ImageView image=new ImageView(activity);choices[i]=image;
            try(java.io.InputStream in=PgAssets.open(activity,"pgrazor/map-icons/"+files[i])){
                Bitmap bitmap=BitmapFactory.decodeStream(in);
                if(i==1&&bitmap!=null&&bitmap.getHeight()==bitmap.getWidth()*3)
                    bitmap=Bitmap.createBitmap(bitmap,0,2*bitmap.getWidth(),bitmap.getWidth(),bitmap.getWidth());
                image.setImageBitmap(bitmap);
            }catch(java.io.IOException|RuntimeException ignored){}
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(8),dp(8),dp(8),dp(8));
            tile.addView(image,new LinearLayout.LayoutParams(-1,dp(80)));
            TextView label=new TextView(activity);
            label.setText(labels[i]);label.setTextSize(13);label.setGravity(Gravity.CENTER);
            tile.addView(label,new LinearLayout.LayoutParams(-1,dp(26)));
            previews.addView(tile,new LinearLayout.LayoutParams(0,dp(112),1f));
            tile.setContentDescription(labels[i]);
            tile.setClickable(true);tile.setFocusable(true);
            tile.setOnClickListener(v->{
                Control.setVgpIconStyle(style);
                for(int j=0;j<tiles.length;j++)
                    tiles[j].setBackgroundColor(j==style?0x3340BDAA:Color.TRANSPARENT);
                host.refresh();
                refreshVgpOptions();
            });
        }
        // Legacy style=2 was the same HD asset with background forcibly hidden.
        // Preserve its transparent appearance until the user selects a new style.
        int current=Control.vgpIconStyle()==0?0:1;
        tiles[current].setBackgroundColor(0x3340BDAA);
        visualSlider(content,"Tamanho do VGP",28,72,Control.vgpIconSize(),value->{
            Control.setVgpIconSize(value);host.refresh();});
        visualSlider(content,"Opacidade do ícone VGP",10,100,Control.vgpIconOpacity(),value->{
            Control.setVgpIconOpacity(value);host.refresh();});
        visualSlider(content,"Fundo cinza (0% = transparente)",0,100,
            Control.vgpIconStyle()==2?0:Control.vgpBackgroundOpacity(),value->{
                // Convert legacy backgroundless style into the same HD icon
                // with an independent alpha control, instead of duplicating an icon.
                if(Control.vgpIconStyle()==2)Control.setVgpIconStyle(1);
                Control.setVgpBackgroundOpacity(value);host.refresh();refreshVgpOptions();
            });
        launch(content,"Escolher imagem VGP no celular",()->OverlayArtwork.select(activity,"vgp",()->{
            Control.setVgpIconStyle(3);host.refresh();refreshVgpOptions();
        }));
        launch(content,"Restaurar imagem original VGP",()->OverlayArtwork.reset(activity,"vgp",()->{
            Control.setVgpIconStyle(1);host.refresh();refreshVgpOptions();
        }));
        new AlertDialog.Builder(dialogContext()).setTitle("Aparência do VGP")
            .setView(content).setPositiveButton("Concluir",null).show();
    }
    private void refreshVgpOptions() {
        if(vgpIconStyleButton!=null){
            String[] choices={"LED","GO Plus HD","GO Plus HD","Imagem personalizada"};
            vgpIconStyleButton.setText(dev.pgrazor.PtBr.tr("Ícone do VGP: "+choices[Control.vgpIconStyle()]));
        }
        for(int i=0;i<3;i++)vgpChoices[i].setChecked((Control.vgpOptions()&(1<<i))!=0);
    }

    private void refreshBuddy() {buddyRefresh.run();buddyStatus.setText(dev.pgrazor.PtBr.tr(BuddyEmotionSettings.status()));}

    private void refreshGifts() {
        for(int i=0;i<3;i++)giftChoices[i].setChecked((Control.giftOptions()&(1<<i))!=0);giftStatus.setText(dev.pgrazor.PtBr.tr(Control.giftStatus()));
    }

    private void refreshBattles() {
        for(int i=0;i<10;i++)battleChoices[i].setChecked((Control.battleOptions()&(1<<i))!=0);battleStatus.setText(dev.pgrazor.PtBr.tr(Control.battleStatus()));
        gymRepeatSwitch.setChecked(Control.gymRepeatEnabled());
    }

    private void refreshRocket() {
        for(int i=0;i<3;i++)rocketChoices[i].setChecked((Control.rocketOptions()&(1<<i))!=0);rocketStatus.setText(dev.pgrazor.PtBr.tr(Control.rocketStatus()));
    }

    private void refreshPvpBattles() {
        // Preferences must remain editable even without a PvP executor.
        // This does NOT bypass Control.pvpBattleAllowed() or claim the runtime works.
        for(int i=0;i<3;i++){
            pvpBattleChoices[i].setChecked((Control.pvpBattleOptions()&(1<<i))!=0);
            pvpBattleChoices[i].setEnabled(true);
        }
        if(Control.pvpBattleReady()){
            pvpBattleStatus.setText(Control.pvpBattleStatus());
        }else if((Control.pvpBattleOptions()&PvpBattleMode.ACTIONS)!=0){
            pvpBattleStatus.setText("Opções Rocket/PvP sincronizadas: Ágil/carregado/escudo compartilhados no Rocket, PvP tipado (incluindo desafios privados) e treinamento/NPC reconhecido. Sem ataque em reide, ginásio, Max ou modo desconhecido; não homologado em aparelho.");
        }else{
            pvpBattleStatus.setText("Você pode configurar as opções, mas a automação PvP ainda não está disponível nesta versão.");
        }
        pvpBattleStatus.setVisibility(View.VISIBLE);
    }

    private void refreshItems() {
        itemSettings.setText(dev.pgrazor.PtBr.tr("Auto-transfer items: "+(Control.itemTransferEnabled()?"ON":"OFF")));itemStatus.setText(dev.pgrazor.PtBr.tr(Control.itemStatus()));
    }

    private void refreshQuickCatch() {
        berrySwitch.setChecked(Control.encounterBerryEnabled());
        berryPriority.setText(dev.pgrazor.PtBr.tr("Berry priority: "+BerryPriorityPicker.summary()));
        berryPicker.refresh();
        quickSpinSwitch.setChecked(Control.quickSpinEnabled());quickSpinStatus.setText(dev.pgrazor.PtBr.tr(Control.quickSpinStatus()));
        routeGiftSwitch.setChecked(Control.routeGiftExchange());
        quickSummaryLabel.setText(dev.pgrazor.PtBr.tr(Control.quickSummaryAutoDismiss() ? "Quick catch" : "Faster catch summary"));
        quickSummarySwitch.setChecked(Control.quickSummaryEnabled());
        quickSummarySwitch.setEnabled(Control.quickSummaryConfigured());
        quickBallStatus.setText(dev.pgrazor.PtBr.tr(Control.quickBallStatus()));
    }

    private void refreshLocationOptions() {
        for(int i=0;i<quickMenuChoices.length;i++)quickMenuChoices[i].setChecked(Control.quickMenuVisible(1<<i));
        idleWalkSwitch.setChecked(Control.idleWalking());idleWalkSwitch.setEnabled(Control.desiredEnabled());idleWalkSwitch.setAlpha(Control.desiredEnabled()?1f:.45f);
        mapTap.setText(dev.pgrazor.PtBr.tr("Game map tap: "+new String[]{"Do nothing","Walk","Teleport","Walk / double tap teleport"}[Control.mapTapMode()]));
        mapTap.setEnabled(Control.quickBallConfigured());
        spawnSwitch.setChecked(Control.spawnBooster());spawnSwitch.setEnabled(Control.spawnAllowed());spawnStatus.setText(dev.pgrazor.PtBr.tr(Control.spawnStatus()));
        localTimeSwitch.setChecked(Control.localTimeOverlay() && Control.hudLabelVisible(Control.HUD_LOCALTIME));
        cooldownSwitch.setChecked(Control.cooldownOverlay() && Control.hudLabelVisible(Control.HUD_COOLDOWN));
        String clockLabel = "Formato da hora · " + CLOCK_EXAMPLES[selectedClockChoice()];
        if (!clockLabel.contentEquals(clockFormatButton.getText())) clockFormatButton.setText(clockLabel);
        String cooldownLabel = "Estilo do contador · " + COOLDOWN_STYLES[Control.cooldownEmphasis() ? 1 : 0];
        if (!cooldownLabel.contentEquals(cooldownStyleButton.getText())) cooldownStyleButton.setText(cooldownLabel);
        encounterCounterSwitch.setChecked(Control.encounterCounter());encounterOverlaySwitch.setChecked(Control.encounterOverlay());
        SettingsUi.enabled(encounterOverlaySwitch,Control.encounterCounter());
        if(current<0){String home=EncounterCounter.title();if(!home.contentEquals(title.getText()))title.setText(dev.pgrazor.PtBr.tr(home));}
    }

    // Indexed by IV mode: 0 off, 1 level and attack/defense/stamina, 2 level and percent, 3 all three.
    private static final String[] IV_MODES={"Off","Lv · 15/14/12","Lv · IV","Lv · IV · 15/14/12","Lv · IV · 15/14/12 · PvP"};

    private void refreshThrows() {
        throwGrade.setText(dev.pgrazor.PtBr.tr("Throw rating: "+new String[]{"OFF","Nice","Great","Excellent"}[Control.throwRating()]));
        throwGrade.setEnabled(Control.quickBallConfigured()&&Control.throwVariety()==0);
        throwVariety.setText(dev.pgrazor.PtBr.tr("Throw variety: "+Control.THROW_VARIETY_LABELS[Control.throwVariety()]));
        throwVariety.setEnabled(Control.quickBallConfigured());
        curveSwitch.setChecked(Control.throwCurve());curveSwitch.setEnabled(Control.quickBallConfigured());
        alwaysHitSwitch.setChecked(Control.throwAlwaysHit());alwaysHitSwitch.setEnabled(Control.throwAlwaysHitReady() || Control.throwAlwaysHit());
        arSwitch.setChecked(Control.throwAr());arSwitch.setEnabled(Control.throwArReady());
        catchChanceSwitch.setChecked(Control.catchChanceShown());autoBallSwitch.setChecked(Control.autoBall());
        autoBallTarget.setText(dev.pgrazor.PtBr.tr("Auto ball target: "+Control.autoBallTarget()+"%"));autoBallTarget.setEnabled(Control.autoBall());
        autoThrowSwitch.setChecked(Control.autoThrow());
        autoThrowMapSwitch.setChecked(Control.autoThrowOnMap());
        autoThrowMapSwitch.setEnabled(Control.autoThrow());
        ivCatchMode.setText(dev.pgrazor.PtBr.tr("Encounter: "+IV_MODES[Control.ivCatchMode()]));ivCatchMode.setEnabled(Control.quickBallConfigured());
        ivOwnedMode.setText(dev.pgrazor.PtBr.tr("Pokémon stats screen: "+IV_MODES[Control.ivOwnedMode()]));ivOwnedMode.setEnabled(Control.quickBallConfigured());
        storageIv.setText(dev.pgrazor.PtBr.tr("Pokémon storage list: "+IV_MODES[Control.storageIvMode()]));
        String ivText=Control.ivDisplayStatus(), storageText=Control.storageIvStatus();
        ivStatus.setText(dev.pgrazor.PtBr.tr(ivText.isEmpty()?storageText:storageText.isEmpty()?ivText:ivText+"\n"+storageText));
    }
}
