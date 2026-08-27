package com.lumavault.app;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
    private PinManager pins;
    private boolean unlocked, launchedPrompt;
    private final Handler lockHandler = new Handler(Looper.getMainLooper());
    private final Runnable delayedLock = () -> { if (unlocked) showGate(); };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(Color.rgb(11,16,32));
        pins = new PinManager(this); showGate();
    }

    private void showGate() {
        unlocked = false; launchedPrompt = false;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL);
        SystemBarInsets.apply(root, dp(28), dp(32), dp(28), dp(16)); root.setBackgroundColor(Color.rgb(11,16,32));
        TextView mark = text("✦", 52, Color.rgb(50,214,160)); mark.setGravity(Gravity.CENTER); root.addView(mark, lp(-1, 74));
        TextView title = text("LUMA VAULT", 28, Color.WHITE); title.setGravity(Gravity.CENTER); title.setLetterSpacing(.14f); root.addView(title, lp(-1, 52));
        TextView sub = text(pins.isConfigured() ? "歡迎回來，解鎖您的私人保險庫" : "建立只屬於您的離線安全空間", 14, Color.rgb(157,169,202));
        sub.setGravity(Gravity.CENTER); root.addView(sub, lp(-1, 58));
        Space gap = new Space(this); root.addView(gap, lp(1, 30));
        EditText pin = new EditText(this); pin.setHint(pins.isConfigured() ? "輸入主密碼" : "設定 6 位以上主密碼"); pin.setHintTextColor(Color.rgb(112,125,158));
        pin.setTextColor(Color.WHITE); pin.setTextSize(17); pin.setSingleLine(); pin.setPadding(dp(18),0,dp(18),0);
        pin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); pin.setBackground(round(Color.rgb(24,33,62), 16)); root.addView(pin, lp(-1, 58));
        EditText confirm = new EditText(this);
        if (!pins.isConfigured()) {
            confirm.setHint("再次輸入主密碼"); confirm.setHintTextColor(Color.rgb(112,125,158)); confirm.setTextColor(Color.WHITE); confirm.setTextSize(17);
            confirm.setSingleLine(); confirm.setPadding(dp(18),0,dp(18),0); confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirm.setBackground(round(Color.rgb(24,33,62),16)); LinearLayout.LayoutParams cp = lp(-1,58); cp.topMargin=dp(12); root.addView(confirm,cp);
        }
        Button go = button(pins.isConfigured() ? "解鎖保險庫" : "建立安全空間"); LinearLayout.LayoutParams bp=lp(-1,58);bp.topMargin=dp(18);root.addView(go,bp);
        if (pins.isConfigured() && pins.biometricEnabled() && canAuthenticate()) {
            TextView or = text("或", 12, Color.rgb(104,118,150)); or.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams op=lp(-1,34);op.topMargin=dp(4);root.addView(or,op);
            Button fingerprint = new Button(this); fingerprint.setText("使用指紋登入"); fingerprint.setTextColor(Color.rgb(50,214,160));
            fingerprint.setTextSize(15); fingerprint.setAllCaps(false); fingerprint.setCompoundDrawablePadding(dp(8));
            GradientDrawable fingerprintBg=round(Color.TRANSPARENT,16);fingerprintBg.setStroke(dp(1),Color.rgb(50,214,160));fingerprint.setBackground(fingerprintBg);
            fingerprint.setOnClickListener(v->{launchedPrompt=true;biometric();});root.addView(fingerprint,lp(-1,54));
        }
        TextView privacy = text("100% 離線  •  AES-256 加密  •  不含廣告", 12, Color.rgb(104,118,150)); privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp=lp(-1,48);pp.topMargin=dp(18);root.addView(privacy,pp); setContentView(root);
        go.setOnClickListener(v -> {
            try {
                String value=pin.getText().toString();
                if (!pins.isConfigured()) {
                    if (value.length()<6) { toast("主密碼至少需要 6 個字元"); return; }
                    if (!value.equals(confirm.getText().toString())) { toast("兩次輸入不一致"); return; }
                    pins.create(value); pins.setBiometric(canAuthenticate()); unlock();
                } else if (pins.verify(value)) unlock(); else { toast("主密碼不正確"); pin.setText(""); }
            } catch (Exception e) { toast("安全驗證失敗"); }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        lockHandler.removeCallbacks(delayedLock);
        if (!unlocked && pins != null && pins.isConfigured() && pins.biometricEnabled() && !launchedPrompt) {
            launchedPrompt = true; new Handler(Looper.getMainLooper()).postDelayed(this::biometric, 250);
        }
    }

    @Override protected void onStop() {
        super.onStop();
        if (unlocked) lockHandler.postDelayed(delayedLock, 120000);
    }

    private void biometric() {
        if (!canAuthenticate()) return;
        new BiometricPrompt.Builder(this).setTitle("解鎖 Luma Vault").setSubtitle("使用生物辨識快速進入")
                .setNegativeButton("使用主密碼",getMainExecutor(),(d,w)->{})
                .build().authenticate(new android.os.CancellationSignal(),getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){unlock();}
                });
    }

    @SuppressWarnings("deprecation")
    private boolean canAuthenticate() {
        if (Build.VERSION.SDK_INT >= 29) {
            BiometricManager bm=(BiometricManager)getSystemService(BIOMETRIC_SERVICE);
            return bm!=null && bm.canAuthenticate()==BiometricManager.BIOMETRIC_SUCCESS;
        }
        FingerprintManager fm=(FingerprintManager)getSystemService(FINGERPRINT_SERVICE);
        return fm!=null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints();
    }

    private void unlock() { unlocked=true; setContentView(new VaultView(this, pins)); }
    void lockNow() { showGate(); }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.rgb(8,31,28));b.setTextSize(16);b.setAllCaps(false);b.setBackground(round(Color.rgb(50,214,160),16));return b;}
    GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w== -1?ViewGroup.LayoutParams.MATCH_PARENT:w,dp(h));}
}
