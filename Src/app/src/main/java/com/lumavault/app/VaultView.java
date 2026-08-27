package com.lumavault.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

final class VaultView extends LinearLayout implements BackupManager.Host {
    private final MainActivity activity;
    private final PinManager pins;
    private final SecureStore store;
    private final ArrayList<VaultItem> items = new ArrayList<>();
    private final LinearLayout list;
    private final EditText search;
    private String filter = "全部";
    private final int ink=Color.rgb(11,16,32), panel=Color.rgb(21,29,54), mint=Color.rgb(50,214,160), muted=Color.rgb(148,160,190);

    VaultView(MainActivity context, PinManager pins) {
        super(context); activity=context; this.pins=pins; store=new SecureStore(context);
        setOrientation(VERTICAL); setBackgroundColor(ink); SystemBarInsets.apply(this,0,0,0,0);
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(20),dp(12),dp(12),dp(4));
        LinearLayout brand=new LinearLayout(context);brand.setOrientation(VERTICAL);
        TextView eyebrow=txt("LUMA VAULT",11,mint);eyebrow.setLetterSpacing(.16f);brand.addView(eyebrow,lp(-1,22));
        brand.addView(txt("我的保險庫",27,Color.WHITE),lp(-1,42));header.addView(brand,new LayoutParams(0,dp(68),1));
        Button help=ghost("說明");help.setOnClickListener(v->help());header.addView(help,lp(66,42));
        Button shield=ghost("安全中心");shield.setOnClickListener(v->security());LayoutParams hp=lp(92,42);hp.leftMargin=dp(6);header.addView(shield,hp);addView(header,lp(-1,84));

        search=new EditText(context);search.setHint("搜尋帳號、網站或分類…");search.setHintTextColor(Color.rgb(103,117,151));search.setTextColor(Color.WHITE);
        search.setTextSize(15);search.setSingleLine();search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);search.setPadding(dp(18),0,dp(18),0);search.setBackground(round(panel,16));
        LayoutParams sp=lp(-1,52);sp.setMargins(dp(20),0,dp(20),dp(12));addView(search,sp);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){render();}public void afterTextChanged(android.text.Editable e){}});

        HorizontalScrollView scroll=new HorizontalScrollView(context);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips=new LinearLayout(context);chips.setPadding(dp(16),0,dp(16),0);
        for(String f:new String[]{"全部","收藏","弱密碼","社群","工作","金融"}){Button b=chip(f);b.setOnClickListener(v->{filter=f;render();});chips.addView(b,lp(82,40));}
        scroll.addView(chips);addView(scroll,lp(-1,48));

        ScrollView body=new ScrollView(context);list=new LinearLayout(context);list.setOrientation(VERTICAL);list.setPadding(dp(20),dp(8),dp(20),dp(100));body.addView(list);addView(body,new LayoutParams(-1,0,1));
        LinearLayout bar=new LinearLayout(context);bar.setPadding(dp(20),dp(8),dp(20),dp(14));bar.setGravity(Gravity.CENTER);
        Button generate=ghost("產生密碼");generate.setOnClickListener(v->generator(null));bar.addView(generate,new LayoutParams(0,dp(54),1));
        Button add=primary("＋ 新增項目");add.setOnClickListener(v->edit(null));LayoutParams ap=new LayoutParams(0,dp(54),1.25f);ap.leftMargin=dp(10);bar.addView(add,ap);addView(bar,lp(-1,76));
        try{items.addAll(store.load());}catch(Exception e){activity.toast("無法開啟加密資料");}render();
    }

    private void render(){
        list.removeAllViews(); String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int strong=0;for(VaultItem i:items)if(i.strength()>=70)strong++;
        LinearLayout summary=new LinearLayout(activity);summary.setPadding(dp(16),dp(12),dp(16),dp(12));summary.setGravity(Gravity.CENTER_VERTICAL);summary.setBackground(round(Color.rgb(17,45,54),18));
        LinearLayout stext=new LinearLayout(activity);stext.setOrientation(VERTICAL);stext.addView(txt(items.size()+" 個安全項目",17,Color.WHITE),lp(-1,30));
        stext.addView(txt(items.isEmpty()?"新增第一組密碼開始使用":"安全評分  "+score()+" / 100",12,Color.rgb(143,204,187)),lp(-1,24));summary.addView(stext,new LayoutParams(0,dp(56),1));
        TextView ring=txt(items.isEmpty()?"—":String.valueOf(score()),24,mint);ring.setGravity(Gravity.CENTER);ring.setBackground(stroke(Color.rgb(23,66,67),mint,32,2));summary.addView(ring,lp(58,58));list.addView(summary,lp(-1,82));
        TextView label=txt("項目",14,muted);LayoutParams labelp=lp(-1,40);labelp.topMargin=dp(10);list.addView(label,labelp);
        int shown=0;
        ArrayList<VaultItem> sorted=new ArrayList<>(items);sorted.sort((a,b)->Boolean.compare(b.favorite,a.favorite));
        for(VaultItem item:sorted){
            boolean match=q.isEmpty()||(item.title+item.username+item.website+item.category).toLowerCase(Locale.ROOT).contains(q);
            boolean filtered=filter.equals("全部")||(filter.equals("收藏")&&item.favorite)||(filter.equals("弱密碼")&&item.strength()<55)||filter.equals(item.category);
            if(match&&filtered){list.addView(card(item),lp(-1,104));shown++;}
        }
        if(shown==0){TextView empty=txt(items.isEmpty()?"尚無項目\n點選「新增項目」建立第一筆安全資料":"找不到符合條件的項目",15,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(48),0,0);list.addView(empty,lp(-1,150));}
    }

    private View card(VaultItem item){
        LinearLayout c=new LinearLayout(activity);c.setPadding(dp(15),dp(12),dp(8),dp(12));c.setGravity(Gravity.CENTER_VERTICAL);c.setBackground(round(panel,16));
        TextView avatar=txt(item.title.isEmpty()?"?":item.title.substring(0,1).toUpperCase(Locale.ROOT),20,Color.WHITE);avatar.setGravity(Gravity.CENTER);avatar.setBackground(round(colorFor(item.category),13));c.addView(avatar,lp(48,48));
        LinearLayout info=new LinearLayout(activity);info.setOrientation(VERTICAL);info.setPadding(dp(12),0,0,0);info.addView(txt((item.favorite?"★  ":"")+item.title,16,Color.WHITE),lp(-1,28));
        info.addView(txt(item.username.isEmpty()?item.category:item.username,13,muted),lp(-1,24));c.addView(info,new LayoutParams(0,dp(58),1));
        TextView strength=txt(item.strength()>=70?"安全":item.strength()>=50?"普通":"偏弱",11,item.strength()>=70?mint:Color.rgb(255,176,91));strength.setGravity(Gravity.CENTER);c.addView(strength,lp(48,36));
        c.setOnClickListener(v->detail(item));LayoutParams p=lp(-1,104);p.bottomMargin=dp(9);c.setLayoutParams(p);return c;
    }

    private void detail(VaultItem item){
        LinearLayout box=dialogBox();box.addView(dialogTitle(item.title));
        fieldLine(box,"使用者名稱",item.username,false,item);fieldLine(box,"密碼","••••••••••••",true,item);
        if(!item.website.isEmpty())fieldLine(box,"網站",item.website,false,item);
        if(!item.notes.isEmpty()){box.addView(txt("備註",12,muted),lp(-1,24));box.addView(txt(item.notes,14,Color.WHITE),lp(-1,48));}
        box.addView(txt("強度  "+item.strength()+" / 100  •  "+item.category,12,item.strength()>=70?mint:Color.rgb(255,176,91)),lp(-1,36));
        AlertDialog d=new AlertDialog.Builder(activity).setView(box).setNegativeButton("關閉",null).setNeutralButton(item.favorite?"取消收藏":"收藏",null).setPositiveButton("編輯",null).create();
        d.setOnShowListener(x->{d.getButton(-1).setOnClickListener(v->{d.dismiss();edit(item);});d.getButton(-3).setOnClickListener(v->{item.favorite=!item.favorite;save();d.dismiss();render();});});d.show();
    }

    private void fieldLine(LinearLayout box,String label,String value,boolean secret,VaultItem item){
        box.addView(txt(label,12,muted),lp(-1,22));LinearLayout line=new LinearLayout(activity);line.setGravity(Gravity.CENTER_VERTICAL);TextView val=txt(value,15,Color.WHITE);line.addView(val,new LayoutParams(0,dp(40),1));
        Button copy=ghost("複製");copy.setOnClickListener(v->copy(secret?item.password:value));line.addView(copy,lp(68,36));box.addView(line,lp(-1,44));
    }

    private void edit(VaultItem item){
        boolean fresh=item==null;LinearLayout box=dialogBox();box.addView(dialogTitle(fresh?"新增安全項目":"編輯項目"));
        EditText title=input("名稱，例如：Google",fresh?"":item.title,false);EditText user=input("帳號 / Email",fresh?"":item.username,false);
        EditText pass=input("密碼",fresh?"":item.password,true);EditText site=input("網站（選填）",fresh?"":item.website,false);
        Spinner cat=new Spinner(activity);String[] cats={"社群","工作","金融","購物","娛樂","其他"};cat.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,cats));if(!fresh)cat.setSelection(Math.max(0,Arrays.asList(cats).indexOf(item.category)));
        EditText notes=input("備註（選填）",fresh?"":item.notes,false);box.addView(title);box.addView(user);box.addView(pass);
        Button gen=ghost("為我產生高強度密碼");gen.setOnClickListener(v->generator(pass));box.addView(gen,lp(-1,42));box.addView(site);box.addView(cat,lp(-1,48));box.addView(notes);
        AlertDialog.Builder builder=new AlertDialog.Builder(activity).setView(box).setNegativeButton("取消",null).setPositiveButton("儲存",null);
        if(!fresh)builder.setNeutralButton("刪除",null);
        AlertDialog d=builder.create();d.setOnShowListener(x->{
            d.getButton(-1).setOnClickListener(v->{if(title.getText().toString().trim().isEmpty()||pass.getText().toString().isEmpty()){activity.toast("請填寫名稱與密碼");return;}
                VaultItem target=fresh?new VaultItem("","","","","其他",""):item;target.title=title.getText().toString().trim();target.username=user.getText().toString().trim();target.password=pass.getText().toString();target.website=site.getText().toString().trim();target.category=(String)cat.getSelectedItem();target.notes=notes.getText().toString().trim();target.updatedAt=System.currentTimeMillis();if(fresh)items.add(target);save();d.dismiss();render();});
            if(!fresh)d.getButton(-3).setOnClickListener(v->new AlertDialog.Builder(activity).setTitle("刪除項目？").setMessage("此動作無法復原。").setNegativeButton("取消",null).setPositiveButton("刪除",(confirmDialog,w)->{items.remove(item);save();d.dismiss();render();}).show());
        });d.show();
    }

    private void generator(EditText destination){
        LinearLayout box=dialogBox();box.addView(dialogTitle("智慧密碼產生器"));SeekBar length=new SeekBar(activity);length.setMax(24);length.setProgress(8);TextView preview=txt("",18,mint);preview.setGravity(Gravity.CENTER);preview.setBackground(round(panel,12));
        TextView count=txt("長度：20",13,muted);box.addView(count,lp(-1,34));box.addView(length,lp(-1,46));box.addView(preview,lp(-1,64));
        final String[] value={makePassword(20)};preview.setText(value[0]);length.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean u){int n=p+12;count.setText("長度："+n);value[0]=makePassword(n);preview.setText(value[0]);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        new AlertDialog.Builder(activity).setView(box).setNegativeButton("取消",null).setNeutralButton("重新產生",(d,w)->generator(destination)).setPositiveButton(destination==null?"複製":"使用此密碼",(d,w)->{if(destination==null)copy(value[0]);else destination.setText(value[0]);}).show();
    }

    private void security(){
        LinearLayout box=dialogBox();box.addView(dialogTitle("安全中心"));int weak=0,reused=0;HashSet<String> seen=new HashSet<>();for(VaultItem i:items){if(i.strength()<55)weak++;if(!seen.add(i.password))reused++;}
        box.addView(metric("整體安全評分",score()+" / 100",mint));box.addView(metric("弱密碼",weak+" 組",weak==0?mint:Color.rgb(255,176,91)));box.addView(metric("重複密碼",reused+" 組",reused==0?mint:Color.rgb(255,176,91)));
        Switch bio=new Switch(activity);bio.setText("使用生物辨識快速解鎖");bio.setTextColor(Color.WHITE);bio.setChecked(pins.biometricEnabled());bio.setOnCheckedChangeListener((b,on)->pins.setBiometric(on));box.addView(bio,lp(-1,58));
        TextView note=txt("資料僅儲存在此裝置，採 Android Keystore 與 AES-256-GCM 加密。",12,muted);note.setPadding(0,dp(8),0,0);box.addView(note,lp(-1,64));
        new AlertDialog.Builder(activity).setView(box).setNegativeButton("立即上鎖",(d,w)->activity.lockNow()).setPositiveButton("完成",null).show();
    }

    private void help(){
        LinearLayout box=dialogBox();box.addView(dialogTitle("說明與更新"));
        box.addView(metric("目前版本","v"+BuildConfig.VERSION_NAME,mint));
        TextView guide=txt("Luma Vault 將帳號資料加密保存在此裝置。您可以搜尋、分類、收藏項目，並使用安全中心檢查弱密碼。\n\n更新檢查只會向 GitHub 讀取最新版本資訊，不會上傳保險庫或任何密碼。",13,Color.rgb(55,65,90));
        guide.setPadding(0,dp(8),0,dp(10));box.addView(guide,lp(-1,128));
        Button update=primary("檢查 GitHub 更新");box.addView(update,lp(-1,52));
        Button data=ghost("資料備份與手機轉移");LayoutParams dataParams=lp(-1,52);dataParams.topMargin=dp(8);box.addView(data,dataParams);
        AlertDialog d=new AlertDialog.Builder(activity).setView(box).setNegativeButton("關閉",null).create();
        update.setOnClickListener(v->{d.dismiss();UpdateChecker.check(activity);});d.show();
        data.setOnClickListener(v->{d.dismiss();activity.openDataManager(this);});
    }

    @Override public ArrayList<VaultItem> snapshot(){return new ArrayList<>(items);}
    @Override public void applyImport(ArrayList<VaultItem> imported,boolean replace){
        if(replace)items.clear();
        LinkedHashMap<String,VaultItem> merged=new LinkedHashMap<>();for(VaultItem item:items)merged.put(item.id,item);for(VaultItem item:imported)merged.put(item.id,item);
        items.clear();items.addAll(merged.values());save();render();
    }
    @Override public void deleteAfterTransfer(){items.clear();save();render();activity.toast("舊手機的保險庫資料已刪除");}

    private View metric(String a,String b,int color){LinearLayout line=new LinearLayout(activity);line.setGravity(Gravity.CENTER_VERTICAL);line.addView(txt(a,14,muted),new LayoutParams(0,dp(46),1));TextView val=txt(b,16,color);val.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);line.addView(val,lp(100,46));return line;}
    private int score(){if(items.isEmpty())return 0;int sum=0;HashSet<String>s=new HashSet<>();for(VaultItem i:items){sum+=i.strength();if(!s.add(i.password))sum-=20;}return Math.max(0,Math.min(100,sum/items.size()));}
    private void save(){try{store.save(items);}catch(Exception e){activity.toast("儲存失敗");}}
    private void copy(String value){ClipboardManager cm=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Luma Vault",value));activity.toast("已複製，60 秒後自動清除");new Handler(Looper.getMainLooper()).postDelayed(()->{if(cm.hasPrimaryClip())cm.clearPrimaryClip();},60000);}
    private String makePassword(int n){String chars="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*+-=?";SecureRandom r=new SecureRandom();StringBuilder s=new StringBuilder();s.append("Aq7!");while(s.length()<n)s.append(chars.charAt(r.nextInt(chars.length())));char[] a=s.toString().toCharArray();for(int i=a.length-1;i>0;i--){int j=r.nextInt(i+1);char t=a[i];a[i]=a[j];a[j]=t;}return new String(a);}

    private LinearLayout dialogBox(){LinearLayout b=new LinearLayout(activity);b.setOrientation(VERTICAL);b.setPadding(dp(24),dp(12),dp(24),dp(8));return b;}
    private TextView dialogTitle(String s){TextView t=txt(s,22,Color.rgb(20,29,55));t.setPadding(0,dp(4),0,dp(8));return t;}
    private EditText input(String hint,String value,boolean password){EditText e=new EditText(activity);e.setHint(hint);e.setText(value);e.setSingleLine();e.setTextSize(15);e.setPadding(dp(12),0,dp(12),0);e.setBackground(round(Color.rgb(237,240,248),10));if(password)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);LayoutParams p=lp(-1,50);p.bottomMargin=dp(8);e.setLayoutParams(p);return e;}
    private Button primary(String s){Button b=new Button(activity);b.setText(s);b.setTextColor(Color.rgb(5,38,31));b.setTextSize(15);b.setAllCaps(false);b.setBackground(round(mint,15));return b;}
    private Button ghost(String s){Button b=new Button(activity);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setAllCaps(false);b.setBackground(stroke(panel,Color.rgb(62,76,112),14,1));return b;}
    private Button chip(String s){Button b=ghost(s);b.setTextSize(12);return b;}
    private TextView txt(String s,int size,int color){TextView t=new TextView(activity);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private int colorFor(String s){int[] c={Color.rgb(82,102,220),Color.rgb(224,105,127),Color.rgb(33,153,176),Color.rgb(197,128,54),Color.rgb(129,90,210),Color.rgb(74,91,125)};return c[Math.abs(s.hashCode())%c.length];}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private GradientDrawable stroke(int color,int line,int r,int width){GradientDrawable g=round(color,r);g.setStroke(dp(width),line);return g;}
    private LayoutParams lp(int w,int h){return new LayoutParams(w==-1?ViewGroup.LayoutParams.MATCH_PARENT:dp(w),h==-1?ViewGroup.LayoutParams.MATCH_PARENT:dp(h));}
    private int dp(int v){return activity.dp(v);}
}
