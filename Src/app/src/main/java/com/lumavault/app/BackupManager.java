package com.lumavault.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

final class BackupManager {
    interface Host {
        ArrayList<VaultItem> snapshot();
        void applyImport(ArrayList<VaultItem> imported, boolean replace);
        void deleteAfterTransfer();
    }

    static final int CREATE_BACKUP=7101, OPEN_BACKUP=7102;
    private final MainActivity activity; private final Host host;
    private String pendingPassword, prefilledTransferCode; private boolean awaitingRetention;

    BackupManager(MainActivity activity, Host host){this.activity=activity;this.host=host;}

    void showMenu(){
        String[] actions={"匯出加密備份","匯入備份／接收轉移","轉移到新手機（QR Code）"};
        new AlertDialog.Builder(activity).setTitle("資料備份與轉移").setItems(actions,(d,which)->{
            if(which==0)askExportPassword();else if(which==1)startImportWithCode(null);else startTransfer();
        }).setNegativeButton("關閉",null).show();
    }

    void startImportWithCode(String code){
        prefilledTransferCode=code;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        activity.startActivityForResult(intent,OPEN_BACKUP);
    }

    boolean onActivityResult(int requestCode,int resultCode,Intent data){
        if(resultCode!=Activity.RESULT_OK||data==null||data.getData()==null){if(requestCode==CREATE_BACKUP)pendingPassword=null;if(requestCode==OPEN_BACKUP)prefilledTransferCode=null;return requestCode==CREATE_BACKUP||requestCode==OPEN_BACKUP;}
        if(requestCode==CREATE_BACKUP){writeBackup(data.getData());return true;}
        if(requestCode==OPEN_BACKUP){askImportPassword(data.getData());return true;}return false;
    }

    void onResume(){if(awaitingRetention){awaitingRetention=false;new Handler(Looper.getMainLooper()).postDelayed(this::askRetention,350);}}

    private void askExportPassword(){
        passwordDialog("設定備份密碼","備份密碼至少 8 個字元，匯入時必須再次輸入。",true,null,password->{
            pendingPassword=password;String date=new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date());
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"Luma_Vault-backup-"+date+".lvault");
            activity.startActivityForResult(intent,CREATE_BACKUP);
        });
    }

    private void writeBackup(Uri uri){run("正在建立加密備份…",()->{
        try(OutputStream out=activity.getContentResolver().openOutputStream(uri,"w")){if(out==null)throw new IOException("無法開啟檔案");BackupCodec.write(out,host.snapshot(),pendingPassword,false);}
        pendingPassword=null;activity.runOnUiThread(()->message("備份完成","加密備份已儲存。請妥善保管備份密碼；忘記後無法還原。"));
    });}

    private void askImportPassword(Uri uri){
        String hint=prefilledTransferCode==null?"輸入備份密碼":"QR Code 已帶入一次性轉移碼";
        passwordDialog("解鎖匯入檔",hint,false,prefilledTransferCode,password->{prefilledTransferCode=null;readBackup(uri,password);});
    }

    private void readBackup(Uri uri,String password){run("正在驗證與解密…",()->{
        ArrayList<VaultItem> imported;try(InputStream in=activity.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("無法開啟檔案");imported=BackupCodec.read(in,password);}
        activity.runOnUiThread(()->new AlertDialog.Builder(activity).setTitle("找到 "+imported.size()+" 個項目")
                .setMessage("選擇「合併」會保留目前資料並更新同一筆項目；選擇「取代」會先清除目前保險庫。")
                .setNegativeButton("取消",null).setNeutralButton("合併",(d,w)->{host.applyImport(imported,false);message("匯入完成","資料已安全合併。");})
                .setPositiveButton("取代",(d,w)->confirmReplace(imported)).show());
    });}

    private void confirmReplace(ArrayList<VaultItem> imported){new AlertDialog.Builder(activity).setTitle("確定取代目前資料？")
            .setMessage("目前保險庫會被匯入檔完整取代，此操作無法復原。")
            .setNegativeButton("取消",null).setPositiveButton("確定取代",(d,w)->{host.applyImport(imported,true);message("匯入完成","保險庫已由備份資料取代。");}).show();}

    private void startTransfer(){
        String code=transferCode();run("正在建立手機轉移包…",()->{
            File dir=new File(activity.getCacheDir(),"transfers");if(!dir.exists()&&!dir.mkdirs())throw new IOException("無法建立轉移暫存區");
            File file=new File(dir,"Luma_Vault-phone-transfer.lvault");try(FileOutputStream out=new FileOutputStream(file)){BackupCodec.write(out,host.snapshot(),code,true);}
            Bitmap qr=qr("lumavault://transfer?code="+code,640);activity.runOnUiThread(()->showTransferQr(file,code,qr));
        });
    }

    private void showTransferQr(File file,String code,Bitmap qr){
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(8),dp(24),0);box.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView note=text("在新手機安裝 Luma Vault，掃描此 QR Code，再選擇收到的轉移檔。\nQR Code 只包含一次性解密碼，不含任何明文帳號。",13,Color.DKGRAY);note.setGravity(Gravity.CENTER);box.addView(note,new LinearLayout.LayoutParams(-1,dp(76)));
        ImageView image=new ImageView(activity);image.setImageBitmap(qr);image.setAdjustViewBounds(true);box.addView(image,new LinearLayout.LayoutParams(dp(260),dp(260)));
        TextView codeView=text(code,19,Color.rgb(17,25,54));codeView.setGravity(Gravity.CENTER);codeView.setLetterSpacing(.08f);box.addView(codeView,new LinearLayout.LayoutParams(-1,dp(52)));
        new AlertDialog.Builder(activity).setTitle("手機轉移 QR Code").setView(box).setNegativeButton("關閉",null)
                .setNeutralButton("複製轉移碼",(d,w)->copy(code)).setPositiveButton("分享轉移檔",(d,w)->share(file)).show();
    }

    private void share(File file){
        Uri uri=FileProvider.getUriForFile(activity,"com.lumavault.app.files",file);Intent send=new Intent(Intent.ACTION_SEND).setType("application/octet-stream")
                .putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,"Luma Vault 手機轉移檔").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        awaitingRetention=true;activity.startActivity(Intent.createChooser(send,"將加密轉移檔傳到新手機"));
    }

    private void askRetention(){new AlertDialog.Builder(activity).setTitle("新手機已匯入成功嗎？")
            .setMessage("若尚未確認，請選擇保留。刪除後無法從舊手機復原。")
            .setNegativeButton("保留舊手機資料",(d,w)->cleanupTransferFiles()).setPositiveButton("刪除舊手機資料",(d,w)->new AlertDialog.Builder(activity)
                    .setTitle("最後確認刪除").setMessage("確定新手機已能開啟所有密碼？")
                    .setNegativeButton("取消",null).setPositiveButton("永久刪除",(d2,w2)->{cleanupTransferFiles();host.deleteAfterTransfer();}).show()).show();}

    private void passwordDialog(String title,String message,boolean confirm,String preset,PasswordCallback callback){
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),0,dp(24),0);
        TextView info=text(message,13,Color.DKGRAY);box.addView(info,new LinearLayout.LayoutParams(-1,dp(52)));
        EditText first=input(preset==null?"密碼或轉移碼":null,preset);box.addView(first,new LinearLayout.LayoutParams(-1,dp(52)));
        EditText second=null;if(confirm){second=input("再次輸入備份密碼",null);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.topMargin=dp(8);box.addView(second,p);}
        EditText finalSecond=second;AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setView(box).setNegativeButton("取消",null).setPositiveButton("繼續",null).create();
        dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{String value=first.getText().toString();if(confirm&&value.length()<8){activity.toast("備份密碼至少需要 8 個字元");return;}if(confirm&&!value.equals(finalSecond.getText().toString())){activity.toast("兩次密碼不一致");return;}if(value.isEmpty()){activity.toast("請輸入密碼或轉移碼");return;}dialog.dismiss();callback.accept(value);}));dialog.show();
    }

    private EditText input(String hint,String value){EditText e=new EditText(activity);e.setHint(hint);if(value!=null)e.setText(value);e.setSingleLine();e.setTextSize(16);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);return e;}
    private void run(String label,Task task){ProgressDialog p=new ProgressDialog(activity);p.setMessage(label);p.setCancelable(false);p.show();new Thread(()->{try{task.run();activity.runOnUiThread(p::dismiss);}catch(Exception e){activity.runOnUiThread(()->{p.dismiss();message("操作失敗",e instanceof SecurityException?e.getMessage():"請確認檔案、密碼與儲存空間後再試一次。");});}},"luma-backup").start();}
    private Bitmap qr(String content,int size)throws Exception{BitMatrix matrix=new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,size,size);Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565);for(int y=0;y<size;y++)for(int x=0;x<size;x++)bitmap.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);return bitmap;}
    private String transferCode(){String alphabet="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";SecureRandom r=new SecureRandom();StringBuilder s=new StringBuilder();for(int i=0;i<16;i++){if(i>0&&i%4==0)s.append('-');s.append(alphabet.charAt(r.nextInt(alphabet.length())));}return s.toString();}
    private void copy(String value){android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("Luma Vault transfer code",value));activity.toast("轉移碼已複製，60 秒後清除");new Handler(Looper.getMainLooper()).postDelayed(()->{if(clipboard.hasPrimaryClip())clipboard.clearPrimaryClip();},60000);}
    private void cleanupTransferFiles(){File dir=new File(activity.getCacheDir(),"transfers");File[] files=dir.listFiles();if(files!=null)for(File file:files)if(file.isFile()&&file.getName().startsWith("Luma_Vault-"))file.delete();}
    private void message(String title,String body){new AlertDialog.Builder(activity).setTitle(title).setMessage(body).setPositiveButton("確定",null).show();}
    private TextView text(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private int dp(int value){return activity.dp(value);}
    private interface Task{void run()throws Exception;}private interface PasswordCallback{void accept(String password);}
}
