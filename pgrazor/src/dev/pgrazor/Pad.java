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

/** An in-activity panel: no root or system-overlay permission needed. */
public final class Pad {
    final Activity activity;
    private final FrameLayout parent;
    // Every on-screen overlay lives in this full-screen layer so "Hide overlay" can hide them all at once;
    // the three-finger watcher brings them back.
    private final FrameLayout overlays;
    private View unhideWatcher;
    private boolean overlaysHidden;
    private AlertDialog hideOverlaysDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Stick stick;
    // The full-screen settings body (switches, captions, sections) owns its own construction/refresh.
    private final SettingsPanel settings;
    private View launcher;
    private VgpToggle vgpToggle;
    private int appliedUiScale;
    private int appliedWindowWidth,appliedWindowHeight;
    private final Rect appliedWindowBounds=new Rect();
    private LinearLayout joystickOverlay;
    private BerryOverlay encounterBerry;
    private Button cooldownTimer;
    // Small draggable overlay showing the local wall-clock time at the spoofed coordinates (not device time).
    private LocalTimeOverlay localTime;
    private TextView vgpActivity, buddyEmotionStatus, catchChance;
    private OverlayDrag catchChanceDrag;
    private OverlayDrag buddyEmotionDrag;
    private GymFeedOverlay gymFeedOverlay;
    private ShundoOverlay shundoOverlay;
    // Shiny Scanner: the latest shiny found, shown on the map until it expires or is tapped away; drag to move it.
    private TextView shinyBadge,shinyMark;private OverlayDrag shinyDrag;private AlertDialog shinyPopup;
    // Encounter counter badge (optional): this account's encounters in the last 24 hours; drag to move it.
    private TextView encounterBadge;private OverlayDrag encounterDrag;
    // Shundo Sniper's black screen: above the game, below PGBR's overlays. Its hint fades after a few s.
    private FrameLayout blackScreen;private TextView blackHint;private boolean blackShown;
    private TextView gymAutomationStatus;
    private TextView automationStatus;
    private OverlayDrag automationDrag;
    private OverlayDrag gymFeedDrag, shundoDrag;
    private VgpMarker vgpMarker;
    private MapCatchBadge mapCatchBadge;
    // Draggable, position-persisting handles for the cooldown and VGP overlays.
    private OverlayDrag cooldownDrag, vgpDrag;
    private OverlayDrag localTimeDrag;
    private int lastVgpHitboxPx;
    private int lastHudAppearanceKey=Integer.MIN_VALUE;
    private QuickMenu quickMenu;
    // Draggable, position-persisting Keep All / transfer-rules overlay button.
    private TransferToggle transferToggle;
    private OverlayDrag transferDrag;
    private GameRoutesOverlay gameRoutesOverlay;
    private OverlayDrag gameRoutesDrag;

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

    /** Re-theme the overlays Pad owns, then the settings body. Invoked on dark/light toggle. */
    private void applyTheme(){
        settings.applyTheme();
        if(launcher!=null)launcher.invalidate();
        if(joystickOverlay!=null){joystickOverlay.setBackground(null);stick.setAlpha(Control.joystickOpacity()/100f);styleTree(joystickOverlay);stick.invalidate();}
        if(encounterBerry!=null)encounterBerry.invalidate();
        lastHudAppearanceKey=Integer.MIN_VALUE;
        applyHudAppearance();
        if(localTime!=null)localTime.applyTheme();
        // Status pill backgrounds depend only on the theme; refresh() must not reallocate them every tick.





        if(transferToggle!=null)transferToggle.invalidate();
        if(gameRoutesOverlay!=null)gameRoutesOverlay.invalidate();
        if(quickMenu!=null)quickMenu.applyTheme();
    }
    private void draggable(View handle,View target,boolean clickable){
        handle.setOnTouchListener(new View.OnTouchListener(){float downX,downY,originX,originY;boolean moved;
            public boolean onTouch(View v,MotionEvent e){int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();originX=target.getX();originY=target.getY();moved=false;return true;}
                if(a==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-downX,dy=e.getRawY()-downY;if(Math.hypot(dx,dy)>dp(6))moved=true;if(moved){Rect safe=UiViewport.bounds(parent);target.setX(UiViewportPolicy.clamp(originX+dx,safe.left,safe.right,OverlayDrag.visualWidth(target)));target.setY(UiViewportPolicy.clamp(originY+dy,safe.top,safe.bottom,OverlayDrag.visualHeight(target)));}return true;}
                if(a==MotionEvent.ACTION_UP){if(!moved&&clickable)v.performClick();return true;}return true;}
        });
    }
    /** Floating launcher: classic PGBR blue pulse or PGEterna matching the themed joystick. */
    private final class PgbrIcon extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Paint shine=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap classic,eterna;
        private Bitmap personal;
        private int artworkVersion=-1;
        PgbrIcon(Context context){
            super(context);
            classic=read("pgrazor/branding/pgbr-logo.webp");
            eterna=read("pgrazor/map-icons/joystick-eternatus-thumb.webp");
            setContentDescription(PtBr.tr("Abrir o menu principal PGBR. Arraste para reposicionar."));
            setFocusable(true);setClickable(true);
        }
        private Bitmap read(String path){
            try(java.io.InputStream in=PgAssets.open(getContext(),path)){return BitmapFactory.decodeStream(in);}
            catch(Exception ignored){return null;}
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            int version=OverlayArtwork.version();
            if(artworkVersion!=version){artworkVersion=version;personal=OverlayArtwork.load(getContext(),"launcher");}
            int style=Control.menuLauncherIconStyle();
            Bitmap logo=style==1?eterna:style==2?personal:classic;
            if(logo==null)logo=classic;
            float sz=Math.min(getWidth(),getHeight()),cx=getWidth()*.5f,cy=getHeight()*.5f;
            long now=SystemClock.uptimeMillis();
            float pulse=(float)(.5+.5*Math.sin(now*(style==1?.0035:.002)));
            int background=Control.menuLauncherBgOpacity();
            if(background>0){
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(Math.round(background*2.55f),219,224,231));
                canvas.drawCircle(cx,cy,sz*.49f,paint);
            }
            shine.setShader(null);shine.setStyle(Paint.Style.STROKE);shine.setStrokeCap(Paint.Cap.ROUND);
            if(style==1&&Control.joystickEternatusEffects()){
                shine.setStrokeWidth(dp(1.8f));
                shine.setColor(Color.argb(Math.round(50+85*pulse),15,220,255));
                canvas.drawArc(cx-sz*.48f,cy-sz*.48f,cx+sz*.48f,cy+sz*.48f,(now*.075f)%360,65,false,shine);
                shine.setColor(Color.argb(Math.round(50+80*pulse),255,39,195));
                canvas.drawArc(cx-sz*.46f,cy-sz*.46f,cx+sz*.46f,cy+sz*.46f,-(now*.060f)%360,56,false,shine);
            }else if(style==0){
                shine.setStrokeWidth(dp(2));
                shine.setColor(Color.argb(Math.round(28+85*pulse),25,171,255));
                canvas.drawCircle(cx,cy,sz*.455f,shine);
            }
            if(logo!=null){
                paint.setColor(Color.WHITE);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);
                float margin=style==1?sz*.09f:sz*.015f;
                canvas.drawBitmap(logo,null,new RectF(cx-sz*.5f+margin,cy-sz*.5f+margin,cx+sz*.5f-margin,cy+sz*.5f-margin),paint);
            }else{
                paint.setColor(SettingsUi.accent());canvas.drawCircle(cx,cy,sz*.4f,paint);
            }
            if(isShown()&&!closed)postInvalidateDelayed(style==1?90:150);
        }
    }

    private long lastTick = SystemClock.elapsedRealtime(), lastLog, lastPositionCheck;
    private String lastPosition = "";
    private boolean closed, lastEnabled;
    private AlertDialog stopRouteDialog;
    public Pad(Activity host) {
        activity = host;
        parent = (FrameLayout)host.getWindow().getDecorView();
        // Build the settings body first (not yet attached); it calls back through the narrow host
        // interface for the few overlay/joystick/engine actions Pad still owns.
        settings = new SettingsPanel(host, new SettingsPanel.Host(){
            public void refresh(){ Pad.this.refresh(); }
            public void applyTheme(){ Pad.this.applyTheme(); }
            public void releaseJoystick(boolean preserveWalk){ stick.release(preserveWalk); }
            public void setJoystickVisible(boolean visible){ stick.release(true); Control.setJoystickVisible(visible); joystickOverlay.setVisibility(visible?View.VISIBLE:View.GONE); }
            public void openMap(){ openWorldMap(); }
            public void editLocation(){ edit(); }
        });

        stick = new Stick(host);
        joystickOverlay = new LinearLayout(host); joystickOverlay.setOrientation(LinearLayout.VERTICAL);
        TextView grip = text("• • •", 12, muted()); grip.setGravity(Gravity.CENTER); joystickOverlay.addView(grip, new LinearLayout.LayoutParams(-1, dp(28)));
        joystickOverlay.addView(stick, new LinearLayout.LayoutParams(dp(Control.joystickSizeDp()), dp(Control.joystickSizeDp()))); draggable(grip, joystickOverlay, false);

        // The settings panel is a full-screen modal; it consumes taps so the game behind it stays inert.
        parent.addView(settings.view(), new FrameLayout.LayoutParams(-1, -1));
        overlays=new FrameLayout(host);parent.addView(overlays,new FrameLayout.LayoutParams(-1,-1));
        blackScreen=new FrameLayout(host);blackScreen.setBackgroundColor(Color.BLACK);blackScreen.setVisibility(View.GONE);
        blackHint=text("Black screen saves battery while Shundo Sniper runs.\nTap anywhere to show the game.",13,Color.rgb(140,140,140));blackHint.setGravity(Gravity.CENTER);
        blackScreen.addView(blackHint,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));
        blackScreen.setOnClickListener(v->{ShundoSniper.setDark(false);refresh();});
        parent.addView(blackScreen,parent.indexOfChild(overlays),new FrameLayout.LayoutParams(-1,-1));

        FrameLayout.LayoutParams joyLayout=new FrameLayout.LayoutParams(dp(Control.joystickSizeDp()),-2,Gravity.LEFT|Gravity.BOTTOM);joyLayout.leftMargin=dp(16);joyLayout.bottomMargin=dp(96);overlays.addView(joystickOverlay,joyLayout);

        // Compact catching-berry overlay; the popup chooses owned berries and Off.
        encounterBerry=new BerryOverlay(activity,this::refresh);
        FrameLayout.LayoutParams berryLayout=new FrameLayout.LayoutParams(dp(90),dp(50),Gravity.RIGHT|Gravity.CENTER_VERTICAL);berryLayout.rightMargin=dp(10);overlays.addView(encounterBerry,berryLayout);encounterBerry.setVisibility(View.GONE);

        cooldownTimer=button("Histórico  --:--");cooldownTimer.setTextSize(11);cooldownTimer.setAllCaps(false);cooldownTimer.setTextColor(accent());FrameLayout.LayoutParams timerLayout=new FrameLayout.LayoutParams(dp(150),dp(38),Gravity.TOP|Gravity.RIGHT);timerLayout.topMargin=dp(44);timerLayout.rightMargin=dp(12);overlays.addView(cooldownTimer,timerLayout);cooldownTimer.setOnClickListener(v->Cooldown.show(activity));cooldownDrag=OverlayDrag.attach(overlays,cooldownTimer,"cooldown");

        // Small local-time overlay: the wall-clock time at the spoofed coordinates, formatted offline by the view.
        localTime=new LocalTimeOverlay(host);FrameLayout.LayoutParams localTimeLayout=new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,dp(32),Gravity.TOP|Gravity.RIGHT);localTimeLayout.rightMargin=dp(12);localTimeLayout.topMargin=dp(94);overlays.addView(localTime,localTimeLayout);localTime.setVisibility(View.GONE);localTimeDrag=OverlayDrag.attach(overlays,localTime,"local-time");

        // Window attachment: the panel pads itself inside the system bars/IME as insets arrive.
        View panelView=settings.view();
        panelView.setOnApplyWindowInsetsListener((view,insets) -> {settings.applyInsets(insets);return insets;});
        panelView.requestApplyInsets();

        vgpToggle=new VgpToggle(host);FrameLayout.LayoutParams vgpButtonLayout=new FrameLayout.LayoutParams(dp(Math.max(48,Control.vgpIconSize())),dp(Math.max(48,Control.vgpIconSize())),Gravity.TOP|Gravity.LEFT);vgpButtonLayout.leftMargin=dp(12);vgpButtonLayout.topMargin=dp(156);overlays.addView(vgpToggle,vgpButtonLayout);vgpToggle.setOnClickListener(v->{if(EternatusRouteRunner.active())EternatusRouteRunner.stop("Controle manual do VGP+ assumido.");Control.setVgpEnabled(!Control.vgpEnabled());refresh();});
        vgpActivity=text("VGP",11,ink());vgpActivity.setPadding(dp(4),dp(2),dp(4),dp(2));vgpActivity.setMaxLines(1);vgpActivity.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams vgpLayout=new FrameLayout.LayoutParams(dp(140),-2,Gravity.TOP|Gravity.LEFT);vgpLayout.leftMargin=dp(12);vgpLayout.topMargin=dp(216);overlays.addView(vgpActivity,vgpLayout);vgpActivity.setVisibility(View.GONE);
        buddyEmotionStatus=text("EP: aguardando",11,ink());buddyEmotionStatus.setPadding(dp(4),dp(2),dp(4),dp(2));buddyEmotionStatus.setMaxLines(1);buddyEmotionStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams buddyEpLayout=new FrameLayout.LayoutParams(dp(155),-2,Gravity.TOP|Gravity.LEFT);buddyEpLayout.leftMargin=dp(12);buddyEpLayout.topMargin=dp(265);overlays.addView(buddyEmotionStatus,buddyEpLayout);buddyEmotionStatus.setVisibility(View.GONE);
        buddyEmotionStatus.setOnClickListener(v->BuddyEmotionSettings.show(activity));buddyEmotionDrag=OverlayDrag.attach(overlays,buddyEmotionStatus,"buddy-emotion");
        gymFeedOverlay=new GymFeedOverlay(activity);FrameLayout.LayoutParams gymFeedLayout=new FrameLayout.LayoutParams(dp(Control.gymIconSizeDp()),dp(Control.gymIconSizeDp()),Gravity.TOP|Gravity.LEFT);gymFeedLayout.leftMargin=dp(12);gymFeedLayout.topMargin=dp(375);overlays.addView(gymFeedOverlay,gymFeedLayout);
        gymAutomationStatus=text("Off",11,ink());gymAutomationStatus.setPadding(dp(6),dp(3),dp(6),dp(3));gymAutomationStatus.setMaxLines(1);gymAutomationStatus.setMaxWidth(dp(165));gymAutomationStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
        overlays.addView(gymAutomationStatus,new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT));
        gymFeedDrag=OverlayDrag.attach(overlays,gymFeedOverlay,gymAutomationStatus,"gym-feed");
        automationStatus=text("",11,ink());automationStatus.setPadding(dp(8),dp(5),dp(8),dp(5));automationStatus.setMaxLines(3);
        FrameLayout.LayoutParams automationLayout=new FrameLayout.LayoutParams(dp(220),-2,Gravity.TOP|Gravity.LEFT);automationLayout.leftMargin=dp(12);automationLayout.topMargin=dp(440);
        overlays.addView(automationStatus,automationLayout);automationStatus.setVisibility(View.GONE);
        automationStatus.setOnClickListener(v->{if(EternatusRouteRunner.active())dev.pgrazor.events.EventPanel.showEternatus(activity,Control.darkTheme());else AutomationPanel.show(activity);});automationDrag=OverlayDrag.attach(overlays,automationStatus,"automation");
        // Catch chance for the open encounter, under the game's nameplate; drag to move it.
        catchChance=text("",12,ink());catchChance.setPadding(dp(10),dp(6),dp(10),dp(6));catchChance.setMaxLines(2);catchChance.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams catchLayout=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);catchLayout.topMargin=dp(400);
        overlays.addView(catchChance,catchLayout);catchChance.setVisibility(View.GONE);catchChanceDrag=OverlayDrag.attach(overlays,catchChance,"catch-chance");
        // The toggle is the drag handle; the status pill follows it as one coherent unit. Tap still flips VGP.
        vgpDrag=OverlayDrag.attach(overlays,vgpToggle,vgpActivity,"vgp");
        vgpActivity.setOnClickListener(v->new AlertDialog.Builder(MapTheme.wrap(activity))
            .setTitle(dev.pgrazor.PtBr.tr("Status do VGP"))
            .setMessage(dev.pgrazor.PtBr.tr(Control.vgpStatus()))
            .setPositiveButton(dev.pgrazor.PtBr.tr("Fechar"),null).show());
        mapCatchBadge=new MapCatchBadge(host);FrameLayout.LayoutParams mapCatchLayout=new FrameLayout.LayoutParams(dp(54),dp(54),Gravity.TOP|Gravity.LEFT);mapCatchLayout.leftMargin=dp(72);mapCatchLayout.topMargin=dp(100);overlays.addView(mapCatchBadge,mapCatchLayout);mapCatchBadge.setVisibility(View.GONE);
        vgpMarker=new VgpMarker(host);overlays.addView(vgpMarker,new FrameLayout.LayoutParams(dp(38),dp(46)));vgpMarker.setVisibility(View.GONE);
        // Compact Keep All / transfer-rules overlay; tap toggles Keep All, drag repositions (persists transfer.fx/fy).
        // Shundo Sniper card: status, counts and Start/Stop; drag to move it.
        shundoOverlay=new ShundoOverlay(activity);FrameLayout.LayoutParams shundoLayout=new FrameLayout.LayoutParams(dp(290),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);shundoLayout.topMargin=dp(96);
        overlays.addView(shundoOverlay,shundoLayout);shundoOverlay.setVisibility(View.GONE);shundoDrag=OverlayDrag.attach(overlays,shundoOverlay,"shundo");
        EncounterCounter.prepare(activity);encounterBadge=text("",11,ink());encounterBadge.setPadding(dp(4),dp(2),dp(4),dp(2));encounterBadge.setSingleLine(true);
        FrameLayout.LayoutParams encounterLayout=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);encounterLayout.rightMargin=dp(12);encounterLayout.topMargin=dp(186);
        overlays.addView(encounterBadge,encounterLayout);encounterBadge.setVisibility(View.GONE);encounterDrag=OverlayDrag.attach(overlays,encounterBadge,"encounters");
        shinyMark=text("✨",19,Color.WHITE);shinyMark.setGravity(Gravity.CENTER);shinyMark.setShadowLayer(dp(4),0,0,Color.BLACK);shinyMark.setClickable(false);shinyMark.setFocusable(false);
        shinyMark.setContentDescription(dev.pgrazor.PtBr.tr("Shiny Pokémon here"));overlays.addView(shinyMark,new FrameLayout.LayoutParams(dp(32),dp(32)));shinyMark.setVisibility(View.GONE);
        ShinyScanner.prepare(activity);shinyBadge=text("",12,ink());shinyBadge.setPadding(dp(4),dp(2),dp(4),dp(2));shinyBadge.setMaxLines(2);
        FrameLayout.LayoutParams shinyLayout=new FrameLayout.LayoutParams(dp(250),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);shinyLayout.topMargin=dp(150);
        overlays.addView(shinyBadge,shinyLayout);shinyBadge.setVisibility(View.GONE);shinyBadge.setOnClickListener(v->{ShinyScanner.showFound();refresh();});shinyDrag=OverlayDrag.attach(overlays,shinyBadge,"shiny");
        transferToggle=new TransferToggle(host);FrameLayout.LayoutParams transferLayout=new FrameLayout.LayoutParams(dp(54),dp(54),Gravity.TOP|Gravity.LEFT);transferLayout.leftMargin=dp(12);transferLayout.topMargin=dp(300);overlays.addView(transferToggle,transferLayout);transferToggle.setOnClickListener(v->{Control.setTransferKeepAll(!Control.transferKeepAll());refresh();});
        transferDrag=OverlayDrag.attach(overlays,transferToggle,"transfer");transferToggle.setVisibility(View.GONE);
        gameRoutesOverlay=new GameRoutesOverlay(activity);
        FrameLayout.LayoutParams routesLayout=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.RIGHT);
        routesLayout.rightMargin=dp(14);routesLayout.topMargin=dp(100);overlays.addView(gameRoutesOverlay,routesLayout);
        gameRoutesOverlay.setVisibility(View.GONE);gameRoutesDrag=OverlayDrag.attach(overlays,gameRoutesOverlay,"game-routes");
        launcher=new PgbrIcon(host);FrameLayout.LayoutParams iconLayout=new FrameLayout.LayoutParams(dp(Control.menuLauncherSizeDp()),dp(Control.menuLauncherSizeDp()),Gravity.TOP|Gravity.LEFT);iconLayout.leftMargin=dp(12);iconLayout.topMargin=dp(100);overlays.addView(launcher,iconLayout);
        launcher.setContentDescription(dev.pgrazor.PtBr.tr("Open PGBR quick menu. Drag to reposition."));
        // O botão da marca PGBR abre o menu rápido; Configurações abre o painel completo.
        quickMenu=new QuickMenu(host,parent,launcher,new QuickMenu.Listener(){
            public void onMap(){openWorldMap();}
            public double onCycleWalkSpeed(){double s=Control.cycleWalkSpeed();refresh();return s;}
            public void onAutoWalk(){stick.release(true);AutoWalkDialog.show(activity);}
            public void onGameRoutes(){stick.release(true);GameRoutesDialog.show(activity);}
            public void onFeeds(){stick.release(true);FeedChooser.show(activity);}
            public void onFavorites(){QuickLocationDialogs.showFavorites(activity,Pad.this::quickTeleport);}
            public void onTeleport(){stick.release(true);QuickLocationDialogs.showTeleport(activity,Pad.this::quickTeleport,Pad.this::quickWalk);}
            public void onHotspots(){stick.release(true);HotspotsDialog.showQuick(activity,Pad.this::quickTeleport,Pad.this::quickWalk);}
            public void onAccounts(){stick.release(true);AccountsDialog.show(activity);}
            public void onSettings(){openSettings();}
            public void onHideOverlays(){confirmHideOverlays();}
        });
        launcher.setOnClickListener(v->{if(quickMenu!=null)quickMenu.toggle();});draggable(launcher,launcher,true);

        settings.hide();joystickOverlay.setVisibility(Control.joystickVisible()?View.VISIBLE:View.GONE);applyTheme();
        unhideWatcher=new ThreeFingerWatcher(host);parent.addView(unhideWatcher,new FrameLayout.LayoutParams(-1,-1));
        refresh();parent.post(this::applyMapScale); handler.post(tick);
    }
    /** Decode a bundled map-icon once; returns null (never throws, never hits the network) if unavailable. */
    private Bitmap decodeMapIcon(String name){
        try(java.io.InputStream in=PgAssets.open(activity, "pgrazor/map-icons/"+name)){return BitmapFactory.decodeStream(in);}
        catch(java.io.IOException|RuntimeException ignored){return null;}
    }
    private Bitmap decodePreferredMapIcon(String preferred,String legacy){
        Bitmap bitmap=decodeMapIcon(preferred);
        return bitmap!=null?bitmap:decodeMapIcon(legacy);
    }
    private final class VgpToggle extends View {
        private final Paint art=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private final Paint glow=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF dst=new RectF();
        private final Bitmap ledOff=trimAlpha(decodeMapIcon("vgp-led-off.png"));
        private final Bitmap ring=trimAlpha(decodeMapIcon("vgp-ring.png"));
        // HD atlas: rows 0=OFF gray, 1=ERROR orange, 2=ON blue.
        // Load the user's full-quality transparent art once, not 48px thumbnail assets.
        private final Bitmap atlas=decodeMapIcon("vgp-hd-states.webp");
        private Bitmap trimAlpha(Bitmap bitmap){
            if(bitmap==null)return null;
            final int w=bitmap.getWidth(),h=bitmap.getHeight();
            if(w<2||h<2)return bitmap;
            final int[] pixels=new int[w*h];
            bitmap.getPixels(pixels,0,w,0,0,w,h);
            int left=w,top=h,right=-1,bottom=-1;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                if((pixels[y*w+x]>>>24)<16)continue;
                if(x<left)left=x;if(x>right)right=x;
                if(y<top)top=y;if(y>bottom)bottom=y;
            }
            if(right<left||bottom<top)return bitmap;
            if(left==0&&top==0&&right==w-1&&bottom==h-1)return bitmap;
            return Bitmap.createBitmap(bitmap,left,top,right-left+1,bottom-top+1);
        }
        private Bitmap hdState(int row,String fallback){
            if(atlas!=null&&atlas.getHeight()==atlas.getWidth()*3){
                try{return trimAlpha(Bitmap.createBitmap(atlas,0,atlas.getWidth()*row,atlas.getWidth(),atlas.getWidth()));}
                catch(RuntimeException ignored){}
            }
            return trimAlpha(decodeMapIcon(fallback));
        }
        private final Bitmap deviceOff=hdState(0,"vgp-device-error.png");
        private final Bitmap deviceError=hdState(1,"vgp-device-on.png");
        private final Bitmap deviceOn=hdState(2,"vgp-device-off.png");
        private Bitmap personal;
        private int artworkVersion=-1;
        VgpToggle(Context context){super(context);setFocusable(true);setClickable(true);}
        private boolean failed(){
            String status=Control.vgpStatus().toLowerCase(java.util.Locale.ROOT);
            return status.contains("error")||status.contains("erro")||status.contains("falha")||
                status.contains("failed")||status.contains("declined")||status.contains("refused");
        }
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            boolean on=Control.vgpEnabled(),error=!on&&failed();
            int style=Control.vgpIconStyle();
            if(artworkVersion!=OverlayArtwork.version()){
                artworkVersion=OverlayArtwork.version();
                personal=trimAlpha(OverlayArtwork.load(getContext(),"vgp"));
            }
            Bitmap icon=style==0?(on?ring:ledOff):style==3&&personal!=null?personal:
                (on?deviceOn:error?deviceError:deviceOff);
            float centerX=getWidth()/2f,centerY=getHeight()/2f;
            float density=getResources().getDisplayMetrics().density;
            float size=Math.min(Math.min(getWidth(),getHeight()),Control.vgpIconSize()*density);
            if(icon!=null){
                if(getParent() instanceof View){
                    View container=(View)getParent();
                    if(getX()>=(container.getWidth()-getWidth()-dp(3)))
                        centerX=getWidth()-size/2f;
                }
                if((style==1||style==3)&&Control.vgpBackgroundOpacity()>0){
                    glow.setStyle(Paint.Style.FILL);glow.setShader(null);
                    glow.setColor(Color.argb(Math.round(Control.vgpBackgroundOpacity()*2.55f),218,225,231));
                    canvas.drawCircle(centerX,centerY,size*.50f,glow);
                }
                float ratio=Math.min(size/icon.getWidth(),size/icon.getHeight());
                float w=icon.getWidth()*ratio,h=icon.getHeight()*ratio;
                dst.set(centerX-w/2,centerY-h/2,centerX+w/2,centerY+h/2);
                art.setAlpha(Math.round(Control.vgpIconOpacity()*2.55f));
                canvas.drawBitmap(icon,null,dst,art);
            }
            if(on&&getVisibility()==View.VISIBLE){
                float phase=(android.os.SystemClock.uptimeMillis()%1200)/1200f;
                float pulse=(float)(0.5+0.5*Math.sin(phase*2*Math.PI));
                if(style==0){
                    // Green LED and two short signal arcs. Do not cover the supplied transparent sprite.
                    glow.setStyle(Paint.Style.FILL);
                    glow.setColor(android.graphics.Color.argb(Math.round((90+150*pulse)*Control.vgpIconOpacity()/100f),0,245,90));
                    canvas.drawCircle(centerX,centerY,size*0.105f,glow);
                    glow.setStyle(Paint.Style.STROKE);glow.setStrokeWidth(Math.max(1.5f,density*1.5f));
                    glow.setColor(android.graphics.Color.argb(Math.round((70+170*pulse)*Control.vgpIconOpacity()/100f),230,255,230));
                    canvas.drawArc(centerX+size*.24f,centerY-size*.47f,centerX+size*.50f,centerY-size*.21f,275,75,false,glow);
                    canvas.drawArc(centerX+size*.33f,centerY-size*.54f,centerX+size*.61f,centerY-size*.15f,275,75,false,glow);
                }else{
                    glow.setStyle(Paint.Style.FILL);
                    glow.setColor(android.graphics.Color.argb(Math.round((45+150*pulse)*Control.vgpIconOpacity()/100f),15,168,255));
                    canvas.drawCircle(centerX,centerY,size*.07f,glow);
                }
                postInvalidateDelayed(80);
            }
        }
    }
    private final class VgpMarker extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        VgpMarker(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/38f,getHeight()/46f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(19,18,17,paint);
            paint.setColor(Control.vgpTargetSpin()?Color.rgb(42,170,240):Color.rgb(235,68,85));canvas.drawCircle(19,18,14,paint);
            paint.setColor(Color.WHITE);
            if(Control.vgpTargetSpin()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);canvas.drawCircle(19,18,8,paint);canvas.drawLine(19,5,19,10,paint);canvas.drawLine(19,26,19,31,paint);canvas.drawLine(6,18,11,18,paint);canvas.drawLine(27,18,32,18,paint);}
            else{paint.setStrokeWidth(3);canvas.drawLine(5,18,33,18,paint);canvas.drawCircle(19,18,5,paint);paint.setColor(Color.rgb(50,55,65));canvas.drawCircle(19,18,2.5f,paint);}
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);Path arrow=new Path();arrow.moveTo(13,36);arrow.lineTo(25,36);arrow.lineTo(19,44);arrow.close();canvas.drawPath(arrow,paint);canvas.restore();
        }
    }
    /**
     * Compact overlay for the transfer policy with three clearly distinct states: KEEP (Keep All on,
     * manual catches are kept), XFER (Keep All off and the auto-transfer master on, so the rules run),
     * and OFF (auto-transfer master off, so nothing transfers regardless). Tap toggles Keep All only;
     * it never re-enables the master.
     */
    private final class TransferToggle extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TransferToggle(Context c){super(c);setFocusable(true);setClickable(true);}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);
            boolean keep=Control.transferKeepAll(),master=Control.autoTransferEnabled();
            int tint=keep?Color.rgb(33,188,125):(master?Color.rgb(230,150,40):muted());
            canvas.save();canvas.scale(getWidth()/54f,getHeight()/54f);
            paint.setStyle(Paint.Style.FILL);paint.setColor((surface()&0x00ffffff)|(Math.round(Control.overlayBackgroundOpacity()*255f/100f)<<24));canvas.drawRoundRect(1,1,53,53,14,14,paint);
            // Shield outline frames the state glyph.
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.4f);paint.setColor(tint);paint.setStrokeCap(Paint.Cap.ROUND);
            Path shield=new Path();shield.moveTo(27,7);shield.lineTo(40,12);shield.lineTo(40,25);shield.cubicTo(40,34,34,39,27,42);shield.cubicTo(20,39,14,34,14,25);shield.lineTo(14,12);shield.close();canvas.drawPath(shield,paint);
            if(keep){canvas.drawLine(21,24,25,29,paint);canvas.drawLine(25,29,34,18,paint);} // check
            else if(master){canvas.drawLine(19,24,33,24,paint);Path a=new Path();a.moveTo(29,19);a.lineTo(35,24);a.lineTo(29,29);canvas.drawPath(a,paint);} // transfer-out arrow
            else{canvas.drawLine(21,24,33,24,paint);} // dash: master off
            paint.setStyle(Paint.Style.FILL);paint.setColor(tint);paint.setTextSize(10);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(keep?"KEEP":(master?"XFER":"OFF"),27,51,paint);
            canvas.restore();}
    }
    private String transferToggleDescription(){
        boolean keep=Control.transferKeepAll(),master=Control.autoTransferEnabled();
        String state=keep?"Keep All on; manual catches are kept":(master?"Normal transfer rules apply":"Auto transfer is off");
        return "Manual encounter transfer: "+state+". Tap to "+(keep?"use normal transfer rules":"keep manual catches")+". Drag to reposition.";
    }

    private void applyMapScale(){
        if(launcher==null||launcher.getWidth()==0||parent.getWidth()==0)return;
        Rect safe=UiViewport.bounds(parent);
        int percent=Control.uiScalePercent();
        if(appliedUiScale==percent&&appliedWindowWidth==parent.getWidth()&&appliedWindowHeight==parent.getHeight()&&appliedWindowBounds.equals(safe))return;
        Rect previousSafe=appliedWindowBounds.isEmpty()?safe:new Rect(appliedWindowBounds);
        appliedWindowBounds.set(safe);appliedWindowWidth=parent.getWidth();appliedWindowHeight=parent.getHeight();
        appliedUiScale=percent;float scale=percent/100f;
        for(View view:new View[]{launcher,joystickOverlay,cooldownTimer,localTime,vgpToggle,vgpActivity,vgpMarker,mapCatchBadge,transferToggle,gameRoutesOverlay,gymFeedOverlay,gymAutomationStatus,shundoOverlay,shinyBadge,shinyMark,encounterBadge}){
            if(view==null)continue;
            float x=view.getX(),y=view.getY();
            int oldWidth=OverlayDrag.visualWidth(view),oldHeight=OverlayDrag.visualHeight(view);
            float fitted=UiViewportPolicy.fit(scale,view.getWidth(),view.getHeight(),safe.width(),safe.height());
            view.setPivotX(0);view.setPivotY(0);view.setScaleX(fitted);view.setScaleY(fitted);
            view.setX(UiViewportPolicy.resizePosition(x,previousSafe.left,previousSafe.right,oldWidth,safe.left,safe.right,OverlayDrag.visualWidth(view)));
            view.setY(UiViewportPolicy.resizePosition(y,previousSafe.top,previousSafe.bottom,oldHeight,safe.top,safe.bottom,OverlayDrag.visualHeight(view)));
        }
        if(cooldownDrag!=null)cooldownDrag.refreshBounds();if(localTimeDrag!=null)localTimeDrag.refreshBounds();if(vgpDrag!=null)vgpDrag.refreshBounds();
        if(transferDrag!=null)transferDrag.refreshBounds();
        if(gameRoutesDrag!=null)gameRoutesDrag.refreshBounds();
        if(gymFeedDrag!=null)gymFeedDrag.refreshBounds();
        if(encounterBerry!=null)encounterBerry.applyUiScale(scale);
        if(quickMenu!=null)quickMenu.applyScale(scale);
    }
    private int launcherSize=-1,launcherOpacity=-1,launcherStyle=-1;
    private void refreshLauncherAppearance(){
        if(launcher==null)return;
        int size=Control.menuLauncherSizeDp(),opacity=Control.menuLauncherOpacity();
        if(launcherSize!=size){
            launcherSize=size;ViewGroup.LayoutParams lp=launcher.getLayoutParams();
            if(lp!=null){lp.width=dp(size);lp.height=dp(size);launcher.setLayoutParams(lp);}
        }
        if(launcherOpacity!=opacity){launcherOpacity=opacity;launcher.setAlpha(opacity/100f);}
        if(launcherStyle!=Control.menuLauncherIconStyle()){
            launcherStyle=Control.menuLauncherIconStyle();launcher.invalidate();
        }
        if(launcher instanceof PgbrIcon&&((PgbrIcon)launcher).artworkVersion!=OverlayArtwork.version())
            launcher.invalidate();
    }
    private int lastJoystickSize=-1,lastJoystickOpacity=-1,lastJoystickStyle=-1;
    private void refreshJoystickAppearance(){
        if(stick==null||joystickOverlay==null)return;
        int size=Control.joystickSizeDp(),opacity=Control.joystickOpacity();
        int style=Control.joystickStyle();
        if(lastJoystickStyle!=style){lastJoystickStyle=style;stick.invalidate();}
        if(style==3&&stick.imageGeneration!=JoystickImage.generation())stick.invalidate();
        if(size!=lastJoystickSize){
            lastJoystickSize=size;
            ViewGroup.LayoutParams current=stick.getLayoutParams();
            if(current!=null){current.width=dp(size);current.height=dp(size);stick.setLayoutParams(current);}
            ViewGroup.LayoutParams outer=joystickOverlay.getLayoutParams();
            if(outer!=null){outer.width=dp(size);joystickOverlay.setLayoutParams(outer);}
        }
        if(opacity!=lastJoystickOpacity){
            lastJoystickOpacity=opacity;stick.setAlpha(opacity/100f);stick.invalidate();
        }
    }
    private int lastGymIconSize=-1;
    private void refreshGymAppearance(){
        if(gymFeedOverlay==null)return;
        int iconSize=Control.gymIconSizeDp();
        if(lastGymIconSize==iconSize)return;
        lastGymIconSize=iconSize;
        ViewGroup.LayoutParams layout=gymFeedOverlay.getLayoutParams();
        if(layout!=null){layout.width=dp(iconSize);layout.height=dp(iconSize);gymFeedOverlay.setLayoutParams(layout);}
        if(gymFeedDrag!=null)gymFeedDrag.refreshBounds();
    }
    private void refreshVgpHitbox(){
        if(vgpToggle==null)return;
        int size=dp(Math.max(48,Control.vgpIconSize()));
        if(size==lastVgpHitboxPx)return;
        lastVgpHitboxPx=size;
        ViewGroup.LayoutParams layout=vgpToggle.getLayoutParams();
        if(layout!=null&&(layout.width!=size||layout.height!=size)){
            layout.width=size;layout.height=size;
            vgpToggle.setLayoutParams(layout);
            if(vgpDrag!=null)vgpDrag.refreshBounds();
        }
    }
    private void applyHudAppearance(){
        int opacity=Control.hudLabelOpacity()*Control.overlayBackgroundOpacity()/100,font=Control.hudLabelTextSp();
        int key=(opacity*100+font)*2+(Control.darkTheme()?1:0);
        if(key==lastHudAppearanceKey)return;
        lastHudAppearanceKey=key;
        int alpha=Math.round(opacity*255f/100f);
        int bg=(surface()&0x00ffffff)|(alpha<<24);
        for(TextView label:new TextView[]{vgpActivity,buddyEmotionStatus,automationStatus,gymAutomationStatus,catchChance,encounterBadge,shinyBadge}){
            if(label==null)continue;
            label.setTextSize(font);
            label.setPadding(dp(4),dp(2),dp(4),dp(2));
            label.setBackground(alpha==0?null:rounded(bg,7));
            label.setShadowLayer(dp(1.5f),0,dp(.5f),Control.darkTheme()?Color.BLACK:Color.WHITE);
        }
        if(cooldownTimer!=null){
            cooldownTimer.setTextSize(font);
            cooldownTimer.setBackground(alpha==0?null:rounded(bg,7));
            cooldownTimer.setShadowLayer(dp(1.5f),0,dp(.5f),Control.darkTheme()?Color.BLACK:Color.WHITE);
        }
    }
    private static String compactBuddyStatus(String full){
        if(full==null||full.isEmpty())return "EP: aguardando";
        String first=full.split("\\n",2)[0];
        if(first.contains(" | Maintenance"))first=first.substring(0,first.indexOf(" | Maintenance"));
        if(first.contains(" | Manutenção"))first=first.substring(0,first.indexOf(" | Manutenção"));
        String second=full.contains("\n")?full.substring(full.indexOf('\n')+1):"";
        if(second.toLowerCase(java.util.Locale.ROOT).contains("paused")||
            second.toLowerCase(java.util.Locale.ROOT).contains("pausad"))return first+" · Pausado";
        return first.length()>44?first.substring(0,44):first;
    }
    private void refresh() {
        applyMapScale();
        if(gymFeedOverlay!=null)gymFeedOverlay.refresh(!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing()));
        boolean black=ShundoSniper.dark()&&!settings.isVisible();
        if(blackScreen!=null&&black!=blackShown){
            blackShown=black;blackScreen.setVisibility(black?View.VISIBLE:View.GONE);
            if(black){blackHint.setVisibility(View.VISIBLE);handler.postDelayed(()->{if(blackHint!=null)blackHint.setVisibility(View.GONE);},4000);}
        }
        applyHudAppearance();
        refreshVgpHitbox();
        refreshLauncherAppearance();
        refreshJoystickAppearance();
        refreshGymAppearance();
        if(encounterBadge!=null){String count=!Control.encounterOverlay()||settings.isVisible()||(quickMenu!=null&&quickMenu.isShowing())?"":EncounterCounter.label();
            encounterBadge.setVisibility(count.isEmpty()||!Control.hudLabelsVisible()?View.GONE:View.VISIBLE);
            if(!count.isEmpty()){setTextIfChanged(encounterBadge,count);encounterBadge.setTextColor(ink());if(encounterDrag!=null)encounterDrag.refreshBounds();}}
        if(shinyMark!=null){boolean mark=ShinyScanner.targetVisible()&&!settings.isVisible();shinyMark.setVisibility(mark?View.VISIBLE:View.GONE);
            if(mark){// The point is the top of the Pokémon; the symbol sits just above it.
            shinyMark.setX(ShinyScanner.targetX()*parent.getWidth()-dp(16));shinyMark.setY(ShinyScanner.targetY()*parent.getHeight()-dp(34));}}
        {String[] found=ShinyScanner.takePopup();if(found!=null&&!closed&&!activity.isFinishing()&&!activity.isDestroyed()){
            if(shinyPopup!=null&&shinyPopup.isShowing())shinyPopup.dismiss();
            AlertDialog.Builder popup=new AlertDialog.Builder(MapTheme.wrap(activity)).setTitle(dev.pgrazor.PtBr.tr(found[0])).setMessage(dev.pgrazor.PtBr.tr(found[1]));
            // A found shiny: Catch it throws for you, Dismiss lets it go, Close keeps it (the badge reopens this).
            if("found".equals(found[2]))popup.setPositiveButton(dev.pgrazor.PtBr.tr("Catch it"),(d,w)->{ShinyScanner.requestCatch();refresh();}).setNeutralButton(dev.pgrazor.PtBr.tr("Dismiss"),(d,w)->{ShinyScanner.dismissFound();refresh();}).setNegativeButton(dev.pgrazor.PtBr.tr("Close"),null);
            else popup.setPositiveButton(dev.pgrazor.PtBr.tr("OK"),null);
            shinyPopup=popup.create();
            final AlertDialog shown=shinyPopup;shown.setCanceledOnTouchOutside(false);DialogLifecycle.bind(activity,shown,null);shown.setOnShowListener(d->MapTheme.styleButtons(shown));shown.show();}}
        if(shinyBadge!=null){String found=settings.isVisible()||(quickMenu!=null&&quickMenu.isShowing())?"":ShinyScanner.foundLabel();
            shinyBadge.setVisibility(found.isEmpty()?View.GONE:View.VISIBLE);
            if(!found.isEmpty()){setTextIfChanged(shinyBadge,found);shinyBadge.setContentDescription(dev.pgrazor.PtBr.tr(found+". Tap for options."));shinyBadge.setTextColor(ink());if(shinyDrag!=null)shinyDrag.refreshBounds();}}
        if(shundoOverlay!=null){shundoOverlay.refresh(!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing()));if(shundoDrag!=null)shundoDrag.refreshBounds();}
        if(gymAutomationStatus!=null){gymAutomationStatus.setVisibility(Control.hudLabelsVisible()?gymFeedOverlay.getVisibility():View.GONE);setTextIfChanged(gymAutomationStatus,GymAutomationSettings.overlayStatus());gymAutomationStatus.setTextColor(ink());gymFeedDrag.refreshBounds();}
        if(vgpToggle!=null){vgpToggle.setVisibility(Control.vgpOverlayVisible()&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);vgpToggle.setContentDescription(dev.pgrazor.PtBr.tr("Virtual Go Plus "+(Control.vgpEnabled()?"on; tap to turn off":"off; tap to turn on")+". "+Control.vgpStatus()));vgpToggle.invalidate();}

        // Pair the status line with the toggle so the cooldown shutoff reason is visible even when VGP is off.
        if(buddyEmotionStatus!=null){buddyEmotionStatus.setVisibility(Control.hudLabelsVisible()&&BuddyEmotionSettings.overlayEnabled()&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);setTextIfChanged(buddyEmotionStatus,compactBuddyStatus(BuddyEmotionSettings.status()));buddyEmotionStatus.setTextColor(ink());}
        if(catchChance!=null){catchChance.setVisibility(Control.hudLabelsVisible()&&CatchChance.visible()&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);setTextIfChanged(catchChance,CatchChance.text());catchChance.setTextColor(ink());}
        if(automationStatus!=null){automationStatus.setVisibility(Control.hudLabelsVisible()&&(EternatusRouteRunner.active()||OneShotAutomation.overlayVisible()||AutoRaid.running())&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);setTextIfChanged(automationStatus,EternatusRouteRunner.active()?"Eternatus "+EternatusRouteRunner.currentStop()+"/"+EternatusRouteRunner.totalStops()+" · "+EternatusRouteRunner.status():AutoRaid.running()?"Auto-Raid | "+AutoRaid.status():OneShotAutomation.overlayStatus());automationStatus.setTextColor(ink());automationDrag.refreshBounds();}
        if(vgpActivity!=null){vgpActivity.setVisibility(Control.hudLabelsVisible()&&Control.vgpOverlayVisible()&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);setTextIfChanged(vgpActivity,Control.vgpEnabled()?"VGP · Ligado":"VGP · Desligado");vgpActivity.setContentDescription(dev.pgrazor.PtBr.tr("Status completo do VGP: "+Control.vgpStatus()));vgpActivity.setTextColor(ink());}
        if(mapCatchBadge!=null)mapCatchBadge.refresh(!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())&&!Control.catchScreenVisible());
        if(vgpMarker!=null){boolean show=Control.vgpTargetVisible()&&!settings.isVisible();vgpMarker.setVisibility(show?View.VISIBLE:View.GONE);if(show){vgpMarker.setX(Control.vgpTargetX()*parent.getWidth()-dp(19)*vgpMarker.getScaleX());vgpMarker.setY(Control.vgpTargetY()*parent.getHeight()-dp(36)-dp(46)*vgpMarker.getScaleY());vgpMarker.invalidate();}}
        if(transferToggle!=null){transferToggle.setVisibility(Control.transferOverlayVisible()&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing())?View.VISIBLE:View.GONE);transferToggle.setContentDescription(dev.pgrazor.PtBr.tr(transferToggleDescription()));transferToggle.invalidate();}

        String routeReward=Control.takeGameRoutesRewardNotice();
        if(!routeReward.isEmpty()&&!overlaysHidden)Toast.makeText(activity,routeReward,Toast.LENGTH_LONG).show();
        if(gameRoutesOverlay!=null)gameRoutesOverlay.refresh(!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing()));
        if(cooldownTimer!=null){cooldownTimer.setVisibility(Control.hudLabelsVisible()&&Control.cooldownOverlay()?View.VISIBLE:View.GONE);setTextIfChanged(cooldownTimer,"☰  "+Cooldown.label());cooldownTimer.setTextColor(Cooldown.remaining()>0?Color.rgb(255,186,95):accent());}
        // The view combines this Pad-side gate with Control.localTimeOverlay()/desiredEnabled() and its own throttle;
        // a false gate (settings open, quick menu up, or overlays hidden) stops its clock work entirely.
        if(localTime!=null){localTime.refresh(!overlaysHidden&&!settings.isVisible()&&!(quickMenu!=null&&quickMenu.isShowing()));if(localTimeDrag!=null&&localTime.getVisibility()==View.VISIBLE)localTimeDrag.refreshBounds();}
        if (lastEnabled != Control.engine.enabled) { lastEnabled = Control.engine.enabled; stick.invalidate(); }
        if(encounterBerry!=null)encounterBerry.refresh();
        if(quickMenu!=null)quickMenu.onRefresh();
        // The hidden panel is GONE; openSettings() refreshes it immediately after show().
        if(settings.isVisible())settings.refresh();
    }
    /** TextView.setText requests a layout pass even for identical text; this tick runs every 100 ms. */
    private static void setTextIfChanged(TextView view,CharSequence value){
        if(!android.text.TextUtils.equals(view.getText(),value))view.setText(dev.pgrazor.PtBr.tr(value));
    }
    private final Runnable tick = new Runnable() {
        public void run() {
            if (closed) return;
            Control.checkLease();
            long now = SystemClock.elapsedRealtime();
            stick.applyMotion(now);
            boolean wasWalking = Control.engine.walking;
            MotionEngine.RouteProgress before=Control.engine.routeProgress();
            Control.stepMotion((now-lastTick)/1000.0,stick.pointer>=0); lastTick = now;
            Control.tickGameRoutes();
            MotionEngine.RouteProgress after=Control.engine.routeProgress();
            if (before.total>1 && after.total==before.total && after.visited>before.visited)
                Control.event("route-progress", "visited=" + after.visited + "/" + after.total);
            if (wasWalking && !Control.engine.walking && Control.engine.enabled) {
                Control.event(before.total>1 ? "route-completed" : "walk-arrived", String.format(Locale.US,"lat=%.7f lon=%.7f; holding destination",Control.engine.latitude,Control.engine.longitude));
                Control.saveState();
            }
            Control.checkpoint(now);
            refresh();
            if (now-lastPositionCheck >= 5000 && Control.engine.enabled) {
                lastPositionCheck = now;
                // Sample movement every five seconds, stationary once a minute; preserve room for failure evidence.
                String where = String.format(Locale.US, "lat=%.7f lon=%.7f speed=%.3f walking=%s idleWalking=%s", Control.engine.latitude, Control.engine.longitude, Control.engine.metersPerSecond(), Control.engine.walking, Control.engine.idleWalkingActive());
                if (!where.equals(lastPosition) || now-lastLog >= 60000) {
                    lastLog = now; lastPosition = where;
                    Control.event("position", where + String.format(Locale.US, " reads=%d/%d", Control.latitudeReads, Control.longitudeReads));
                }
            }
            handler.postDelayed(this, 100);
        }
    };
    private EditText input(String value, String hint) {
        EditText e = new EditText(dialogContext()); e.setSingleLine(true); e.setHint(dev.pgrazor.PtBr.tr(hint)); e.setText(dev.pgrazor.PtBr.tr(value));
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); return e;
    }
    private void edit() {
        if (Control.engine.walking) Control.event("walk-cancelled", "Coordinates editor opened");
        stick.release(); refresh();
        LinearLayout form = new LinearLayout(activity); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(24),dp(8),dp(24),dp(8));
        EditText lat = input(Double.toString(Control.engine.latitude), "Latitude (-90 to 90)");
        EditText lon = input(Double.toString(Control.engine.longitude), "Longitude (-180 to 180)");
        EditText speed = input(Double.toString(Control.engine.speedKmh), "Speed (0.5–30 km/h)");
        for (EditText field : new EditText[]{lat, lon, speed}) form.addView(field);
        AlertDialog dialog = new AlertDialog.Builder(dialogContext()).setTitle(dev.pgrazor.PtBr.tr("Coordinates & speed")).setView(form).setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null).setPositiveButton(dev.pgrazor.PtBr.tr("Apply"),null).create();
        final AlertDialog[] confirmation = {null};
        DialogLifecycle.bind(activity,dialog,()->{if(confirmation[0]!=null)confirmation[0].dismiss();});
        dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
            try {
                // Validate all fields before committing any state.
                MotionEngine check = new MotionEngine(); check.position(Double.parseDouble(lat.getText().toString().trim()), Double.parseDouble(lon.getText().toString().trim())); check.speed(Double.parseDouble(speed.getText().toString().trim()));
                confirmation[0]=TeleportDialog.confirm(activity,check.latitude,check.longitude,(latitude,longitude)->{
                    if(!quickTeleport(latitude,longitude))return false;
                    Control.engine.speed(check.speedKmh); Control.saveState();
                    Control.event("coordinates-set", latitude + "," + longitude + " speed=" + check.speedKmh);
                    refresh(); dialog.dismiss(); return true;
                });
            } catch (IllegalArgumentException error) { speed.setError("Enter valid coordinates and speed (0.5–30)."); }
        })); dialog.show();styleTree(form);
    }
    /** "Hide overlay": confirm first, naming the three-finger tap that shows the overlays again. */
    void confirmHideOverlays() {
        if(closed||overlaysHidden||activity.isFinishing()||activity.isDestroyed()||hideOverlaysDialog!=null)return;
        AlertDialog dialog=new AlertDialog.Builder(dialogContext())
            .setTitle(dev.pgrazor.PtBr.tr("Hide overlay?"))
            .setMessage(dev.pgrazor.PtBr.tr("All PGBR buttons, status bars, messages and in-game IV text will be hidden so the game looks stock. To show them again, tap the screen with three fingers at once."))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Cancel"),null)
            .setPositiveButton(dev.pgrazor.PtBr.tr("Hide"),(d,w)->setOverlaysHidden(true)).create();
        hideOverlaysDialog=dialog;
        DialogLifecycle.bind(activity,dialog,()->{if(hideOverlaysDialog==dialog)hideOverlaysDialog=null;});
        dialog.setOnShowListener(d->MapTheme.styleButtons(dialog));
        dialog.show();
    }
    private void setOverlaysHidden(boolean hidden){
        if(closed||overlaysHidden==hidden)return;
        overlaysHidden=hidden;Control.setOverlaysHidden(hidden);
        if(hidden){stick.release(true);if(quickMenu!=null&&quickMenu.isShowing())quickMenu.hide();}
        overlays.setVisibility(hidden?View.GONE:View.VISIBLE);
        Control.event("overlays",hidden?"hidden":"shown");
        if(!hidden){Toast.makeText(activity,"PGBR overlay shown",Toast.LENGTH_SHORT).show();refresh();}
    }
    /**
     * Sees every finger that touches down anywhere on screen and never takes the touch, so the game
     * and the overlays behave as before. The decor view splits multi-finger touches, so each finger
     * arrives here as its own down event. While the overlay is hidden, three fingers landing within
     * 250 ms at least 24dp apart show it again; one finger tapping the same spot quickly does not.
     */
    private final class ThreeFingerWatcher extends View {
        private final long[] times=new long[3];
        private final float[] xs=new float[3],ys=new float[3];
        private int next;
        ThreeFingerWatcher(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override public boolean dispatchTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            if(!overlaysHidden||(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_POINTER_DOWN))return false;
            if(e.getPointerCount()>=3){unhide();return false;}
            int index=e.getActionIndex();
            times[next]=e.getEventTime();xs[next]=e.getX(index);ys[next]=e.getY(index);next=(next+1)%3;
            long newest=e.getEventTime();float apart=dp(24);
            boolean together=true;
            for(int i=0;i<3&&together;i++){
                together=times[i]!=0&&newest-times[i]<=250;
                for(int j=i+1;j<3&&together;j++)together=Math.hypot(xs[i]-xs[j],ys[i]-ys[j])>=apart;
            }
            if(together)unhide();
            return false;
        }
        private void unhide(){java.util.Arrays.fill(times,0);post(()->setOverlaysHidden(false));}
    }
    /** A ground tap must never silently replace a planned route. */
    void confirmStopRoute() {
        if(closed||activity.isFinishing()||activity.isDestroyed()||stopRouteDialog!=null)return;
        final long token=Control.engine.activeRouteToken();if(token==0)return;
        AlertDialog dialog=new AlertDialog.Builder(dialogContext())
            .setTitle(dev.pgrazor.PtBr.tr("Stop route?"))
            .setMessage(dev.pgrazor.PtBr.tr("A route or Autowalk is running. Do you want to stop it?"))
            .setNegativeButton(dev.pgrazor.PtBr.tr("Keep walking"),null)
            .setPositiveButton(dev.pgrazor.PtBr.tr("Stop route"),(d,w)->{
                if(!closed&&Control.engine.stopRoute(token)){
                    Control.saveState();Control.event("route-stopped","Confirmed after map tap");refresh();
                }
            }).create();
        stopRouteDialog=dialog;
        DialogLifecycle.bind(activity,dialog,()->{if(stopRouteDialog==dialog)stopRouteDialog=null;});
        dialog.setOnShowListener(d->MapTheme.styleButtons(dialog));
        dialog.show();
    }

    /** Shared World map handler for both the settings list and the quick menu Map row. */
    private void openWorldMap() {
        if(quickMenu!=null)quickMenu.hide();
        stick.release(true);
        Control.event("map-open", "Browsing does not apply a destination; current walk=" + Control.engine.walking);
        WorldMapDialog.show(activity, Control.engine.latitude, Control.engine.longitude, (lat, lon) -> {
            if (!Control.canEnable()) { Toast.makeText(activity,"Location adapter is not ready",Toast.LENGTH_SHORT).show(); return false; }
            try {
                stick.release(); Control.teleport(lat, lon);
                Control.event("map-teleport", String.format(Locale.US,"lat=%.7f lon=%.7f",lat,lon));
                refresh(); return true;
            } catch (IllegalArgumentException error) { Toast.makeText(activity,"Select valid coordinates",Toast.LENGTH_SHORT).show(); return false; }
        }, (lat, lon) -> {
            if (!Control.canEnable()) { Toast.makeText(activity,"Location adapter is not ready",Toast.LENGTH_SHORT).show(); return false; }
            try {
                stick.release(); Control.walkTo(lat, lon);
                Control.event("walk-started", String.format(Locale.US,"targetLat=%.7f targetLon=%.7f speedKmh=%.2f",lat,lon,Control.engine.speedKmh));
                if (!Control.engine.walking) Control.event("walk-arrived", "Already at selected destination");
                refresh(); return true;
            } catch (IllegalArgumentException error) { Toast.makeText(activity,"Select valid coordinates",Toast.LENGTH_SHORT).show(); return false; }
        }, (waypoints, loop) -> {
            if (!Control.canEnable()) { Toast.makeText(activity,"Location adapter is not ready",Toast.LENGTH_SHORT).show(); return false; }
            try {
                stick.release(true); Control.walkRoute(waypoints, loop);
                MotionEngine.RouteProgress progress=Control.engine.routeProgress();
                Control.event("route-started", String.format(Locale.US,"stops=%d visited=%d remainingMeters=%.2f speedKmh=%.2f loop=%s",progress.total,progress.visited,progress.remainingMeters,Control.engine.speedKmh,loop));
                refresh(); return true;
            } catch (IllegalArgumentException error) { Toast.makeText(activity,error.getMessage(),Toast.LENGTH_SHORT).show(); return false; }
        });
    }
    /** Opens the existing settings panel full screen; the game runtime is left untouched. */
    private void openSettings() {
        if(quickMenu!=null)quickMenu.hide();
        stick.release(true);
        settings.show(); settings.applyInsets(parent.getRootWindowInsets());
        Control.event("settings-open", "Full-screen panel; runtime unaffected");
        refresh();
    }
    /** Shared quick teleport for the Favorites and Teleport modals; validates readiness. */
    private boolean quickTeleport(double latitude, double longitude) {
        if (!Control.canEnable()) { Toast.makeText(activity,"Location adapter is not ready",Toast.LENGTH_SHORT).show(); return false; }
        try {
            stick.release(); Control.teleport(latitude, longitude);
            Control.event("quick-teleport", String.format(Locale.US,"lat=%.7f lon=%.7f",latitude,longitude));
            refresh(); return true;
        } catch (IllegalArgumentException error) { Toast.makeText(activity,"Enter valid coordinates",Toast.LENGTH_SHORT).show(); return false; }
    }
    void confirmMapTeleport(double latitude,double longitude,int mode,int epoch) {
        TeleportDialog.confirm(activity,latitude,longitude,(lat,lon)->{
            if(closed||!Control.mapTapAllowed()||mode!=Control.mapTapMode()||epoch!=Control.mapTapEpoch()||Control.engine.activeRouteToken()!=0) {
                Toast.makeText(activity,"Map-tap settings changed; tap the map again.",Toast.LENGTH_SHORT).show();return true;
            }
            if(!quickTeleport(lat,lon))return false;
            Control.event("map-tap-applied",String.format(Locale.US,"mode=%d lat=%.7f lon=%.7f",mode,lat,lon));return true;
        });
    }
    private boolean quickWalk(double latitude,double longitude) {
        if(!Control.canEnable()){Toast.makeText(activity,"Location adapter is not ready",Toast.LENGTH_SHORT).show();return false;}
        try {
            stick.release();Control.walkTo(latitude,longitude);
            Control.event("quick-walk",String.format(Locale.US,"lat=%.7f lon=%.7f",latitude,longitude));
            refresh();return true;
        } catch(IllegalArgumentException error){Toast.makeText(activity,"Select valid coordinates",Toast.LENGTH_SHORT).show();return false;}
    }
    public void close() {
        if (closed) return;
        closed = true;if(stopRouteDialog!=null)stopRouteDialog.dismiss(); stick.release(true); Control.suspendForBackground(); handler.removeCallbacks(tick);
        if(quickMenu!=null){quickMenu.destroy();quickMenu=null;}
        if(cooldownDrag!=null){cooldownDrag.detach();cooldownDrag=null;}if(vgpDrag!=null){vgpDrag.detach();vgpDrag=null;}if(transferDrag!=null){transferDrag.detach();transferDrag=null;}if(gameRoutesDrag!=null){gameRoutesDrag.detach();gameRoutesDrag=null;}
        if(localTimeDrag!=null){localTimeDrag.detach();localTimeDrag=null;}if(localTime!=null){localTime.close();overlays.removeView(localTime);localTime=null;}
        if(buddyEmotionDrag!=null){buddyEmotionDrag.detach();buddyEmotionDrag=null;}
        if(buddyEmotionStatus!=null)overlays.removeView(buddyEmotionStatus);
        if(automationStatus!=null){overlays.removeView(automationStatus);automationStatus=null;}
        if(catchChanceDrag!=null){catchChanceDrag.detach();catchChanceDrag=null;}if(catchChance!=null){overlays.removeView(catchChance);catchChance=null;}
        if(gymFeedDrag!=null){gymFeedDrag.detach();gymFeedDrag=null;}
        EncounterCounter.flush();if(encounterDrag!=null){encounterDrag.detach();encounterDrag=null;}if(encounterBadge!=null){overlays.removeView(encounterBadge);encounterBadge=null;}
        if(shinyPopup!=null){shinyPopup.dismiss();shinyPopup=null;}if(shinyMark!=null){overlays.removeView(shinyMark);shinyMark=null;}
        if(shinyDrag!=null){shinyDrag.detach();shinyDrag=null;}if(shinyBadge!=null){overlays.removeView(shinyBadge);shinyBadge=null;}
        if(shundoDrag!=null){shundoDrag.detach();shundoDrag=null;}if(shundoOverlay!=null){shundoOverlay.close();overlays.removeView(shundoOverlay);shundoOverlay=null;}
        if(gymAutomationStatus!=null){overlays.removeView(gymAutomationStatus);gymAutomationStatus=null;}
        if(gymFeedOverlay!=null){gymFeedOverlay.close();overlays.removeView(gymFeedOverlay);gymFeedOverlay=null;}
        settings.close();if(hideOverlaysDialog!=null)hideOverlaysDialog.dismiss();
        parent.removeView(settings.view());overlays.removeView(launcher);overlays.removeView(joystickOverlay);if(encounterBerry!=null){encounterBerry.close();overlays.removeView(encounterBerry);}if(cooldownTimer!=null)overlays.removeView(cooldownTimer);if(vgpToggle!=null)overlays.removeView(vgpToggle);if(vgpActivity!=null)overlays.removeView(vgpActivity);if(vgpMarker!=null)overlays.removeView(vgpMarker);if(mapCatchBadge!=null){overlays.removeView(mapCatchBadge);mapCatchBadge=null;}if(transferToggle!=null)overlays.removeView(transferToggle);if(gameRoutesOverlay!=null){gameRoutesOverlay.close();overlays.removeView(gameRoutesOverlay);}
        parent.removeView(overlays);if(unhideWatcher!=null)parent.removeView(unhideWatcher);
        if(blackScreen!=null){blackScreen.setOnClickListener(null);parent.removeView(blackScreen);blackScreen=null;blackHint=null;}
        ShundoSniper.setDark(false);  // leaving the game ends the black screen; it is not brought back on return
        settings.view().setOnApplyWindowInsetsListener(null);
        Control.event("panel-closed", "Runtime suspended on activity change; desiredEnabled=" + Control.desiredEnabled() + "; movement stopped");
    }
    private final class Stick extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        private Bitmap customImage, builtinBase, builtinThumb;
        private int imageGeneration=-1,loadedStyle=-1;
        private final Paint effect=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path spark=new Path();
        float x, y;
        int pointer = -1;
        long cameraGesture=-1;
        boolean gestureCamera;
        Stick(Context c) { super(c); setContentDescription("Location joystick: drag to move, release to stop"); }
        void release() { release(false); }
        void release(boolean preserveWalk) { x=y=0; pointer=-1; cameraGesture=-1; if (!preserveWalk || !Control.engine.walking) Control.engine.stop(); Control.saveState(); invalidate(); }
        void applyMotion(long now) {
            if (pointer<0) return;
            if (!Control.engine.enabled || !Control.canEnable() || gestureCamera!=Control.cameraRelative()) { release(true); return; }
            boolean wasWalking=Control.engine.walking;
            if (gestureCamera) {
                if (!Control.applyCameraInput(cameraGesture,x,-y,now)) {
                    release(true);
                    Control.event("joystick-released", "Camera unavailable or changed; a new drag is required");
                    return;
                }
            } else Control.engine.input(x,-y);
            if (wasWalking) Control.event("walk-cancelled", "Manual joystick input");
        }
        private Bitmap art(String name){
            try(java.io.InputStream in=PgAssets.open(getContext(),"pgrazor/map-icons/"+name)){
                return BitmapFactory.decodeStream(in);
            }catch(Exception missing){
                return null;  // Never interrupt steering when a decorative asset is unavailable.
            }
        }
        private void loadArtIfNeeded(){
            int style=Control.joystickStyle();
            if(style==loadedStyle)return;
            loadedStyle=style;
            builtinBase=builtinThumb=null;
            if(style==1||style==2){
                String variant=style==1?"eternatus":"hud";
                builtinBase=art("joystick-"+variant+"-base.webp");
                builtinThumb=art("joystick-"+variant+"-thumb.webp");
                if(builtinBase==null||builtinThumb==null)
                    Control.event("joystick-art","Could not load bundled "+variant+" layers; original control remains active");
            }
        }
        private void drawEternatusEffects(Canvas canvas,float cx,float cy,float r,float knobX,float knobY,float knobR){
            long now=SystemClock.uptimeMillis();
            float phase=(now%2000L)/2000f;
            float pulse=(float)(.55+.45*Math.sin(2*Math.PI*phase));
            effect.setShader(null);
            effect.setStyle(Paint.Style.STROKE);
            effect.setStrokeCap(Paint.Cap.ROUND);
            effect.setStrokeJoin(Paint.Join.ROUND);
            // Slow cyan/fuchsia scans travel around the frame, without full-screen invalidation.
            for(int i=0;i<4;i++){
                float angle=(now*.035f+i*91f)%360f;
                float rr=r*(i%2==0?.90f:.86f);
                effect.setStrokeWidth(dp(i%2==0?1.7f:1.25f));
                effect.setColor(i%2==0?Color.argb(105,15,220,255):Color.argb(95,255,22,197));
                canvas.drawArc(cx-rr,cy-rr,cx+rr,cy+rr,angle,33,false,effect);
            }
            // Four small lightning forks rotate around the ring. They are purely decorative.
            for(int i=0;i<4;i++){
                double a=(now*.00057+i*Math.PI/2);
                float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
                float tx=-sa,ty=ca;
                float px=cx+ca*r*.78f,py=cy+sa*r*.78f;
                spark.reset();
                spark.moveTo(px,py);
                spark.lineTo(px+ca*r*.065f+tx*r*.031f,py+sa*r*.065f+ty*r*.031f);
                spark.lineTo(px+ca*r*.10f-tx*r*.023f,py+sa*r*.10f-ty*r*.023f);
                spark.lineTo(px+ca*r*.14f,py+sa*r*.14f);
                effect.setStrokeWidth(dp(1.2f));
                effect.setColor(i%2==0?Color.argb(65,27,218,255):Color.argb(72,255,41,189));
                canvas.drawPath(spark,effect);
            }
            // Knob corona also follows the thumb rather than remaining fixed in the background.
            effect.setStrokeWidth(dp(2.3f));
            effect.setColor(Color.argb(Math.round(55+70*pulse),255,27,217));
            canvas.drawCircle(knobX,knobY,knobR*.98f,effect);
            effect.setStrokeWidth(dp(1.2f));
            effect.setColor(Color.argb(Math.round(80+85*pulse),26,230,255));
            canvas.drawArc(knobX-knobR*1.04f,knobY-knobR*1.04f,
                knobX+knobR*1.04f,knobY+knobR*1.04f,(now*.09f)%360f,68,false,effect);
            effect.setStyle(Paint.Style.FILL);
        }
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            float radius=Math.max(dp(8),Math.min(cx,cy)-dp(12));
            loadArtIfNeeded();
            int style=Control.joystickStyle();
            if((style==1||style==2)&&builtinBase!=null&&builtinThumb!=null){
                float outer=Math.max(dp(16),Math.min(cx,cy)-dp(2));
                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha(255);
                paint.setColor(Color.WHITE);
                canvas.drawBitmap(builtinBase,null,new RectF(cx-outer,cy-outer,cx+outer,cy+outer),paint);
                float thumbR=outer*(style==1?.355f:.319f);
                float travel=Math.max(0,outer-thumbR-dp(4));
                float knobX=cx+x*travel,knobY=cy+y*travel;
                if(style==1&&Control.joystickEternatusEffects())
                    drawEternatusEffects(canvas,cx,cy,outer,knobX,knobY,thumbR);
                // Only the circular hub moves; the arrows, shield and outer artwork remain fixed.
                canvas.drawBitmap(builtinThumb,null,
                    new RectF(knobX-thumbR,knobY-thumbR,knobX+thumbR,knobY+thumbR),paint);
                if(style==1&&Control.joystickEternatusEffects()&&isShown()&&!closed)
                    postInvalidateDelayed(80); // bounded ~12.5 FPS, no extra worker or timer
                return;
            }
            if(style==3&&imageGeneration!=JoystickImage.generation()){
                imageGeneration=JoystickImage.generation();
                customImage=JoystickImage.load(getContext());
            }
            if(style==3&&customImage!=null){
                paint.setAlpha(255);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);
                canvas.drawBitmap(customImage,null,new RectF(cx-radius,cy-radius,cx+radius,cy+radius),paint);
            }else{
                paint.setColor(Control.darkTheme()?0xff202e3e:0xffe1e9ef);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx,cy,radius,paint);
            }
            paint.setColor(Color.rgb(72,102,125));paint.setStrokeWidth(dp(1));
            canvas.drawLine(cx-radius,cy,cx+radius,cy,paint);
            canvas.drawLine(cx,cy-radius,cx,cy+radius,paint);
            paint.setColor(Control.engine.enabled?accent():muted());
            canvas.drawCircle(cx+x*radius*.65f,cy+y*radius*.65f,dp(22),paint);
        }
        public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked();
            if (action==MotionEvent.ACTION_DOWN) {
                if(EternatusRouteRunner.active())EternatusRouteRunner.stop("Movimento manual pelo joystick.");
                getParent().requestDisallowInterceptTouchEvent(true);
                // A deliberate manual takeover cancels even when a fresh camera
                // heading is unavailable. Passive camera loss preserves a walk.
                if (Control.engine.walking) {
                    Control.engine.stop(); Control.saveState();
                    Control.event("walk-cancelled", "Manual joystick touch");
                }
                pointer=e.getPointerId(0); gestureCamera=Control.cameraRelative();
                cameraGesture=gestureCamera ? Control.beginCameraGesture() : -1;
            }
            if (action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL ||
                (action==MotionEvent.ACTION_POINTER_UP && e.getPointerId(e.getActionIndex())==pointer)) {
                getParent().requestDisallowInterceptTouchEvent(false);
                release(pointer<0); Control.event("joystick", "released"); return true;
            }
            int index=e.findPointerIndex(pointer); if(index<0) return true;
            float radius=Math.min(getWidth(),getHeight())/2f-dp(12);
            x=(e.getX(index)-getWidth()/2f)/radius; y=(e.getY(index)-getHeight()/2f)/radius;
            float length=(float)Math.max(1,Math.hypot(x,y)); x/=length; y/=length;
            if(Math.hypot(x,y)<0.08) x=y=0;
            applyMotion(SystemClock.elapsedRealtime()); invalidate(); return true;
        }
    }
}
