package com.lumavault.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PinManager {
    private final SharedPreferences prefs;
    PinManager(Context c) { prefs = c.getSharedPreferences("luma_security", Context.MODE_PRIVATE); }
    boolean isConfigured() { return prefs.contains("pin_hash"); }
    boolean biometricEnabled() { return prefs.getBoolean("biometric", true); }
    void setBiometric(boolean enabled) { prefs.edit().putBoolean("biometric", enabled).apply(); }
    void create(String pin) throws Exception {
        byte[] salt = new byte[24]; new SecureRandom().nextBytes(salt);
        prefs.edit().putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pin_hash", hash(pin, salt)).apply();
    }
    boolean verify(String pin) throws Exception {
        byte[] salt = Base64.decode(prefs.getString("pin_salt", ""), Base64.NO_WRAP);
        return constantTime(prefs.getString("pin_hash", ""), hash(pin, salt));
    }
    private String hash(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
        return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), Base64.NO_WRAP);
    }
    private boolean constantTime(String a, String b) {
        if (a.length() != b.length()) return false; int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i); return diff == 0;
    }
}
