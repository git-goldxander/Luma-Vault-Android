package com.lumavault.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import org.json.JSONArray;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStore {
    private static final String ALIAS = "luma_vault_aes_2026";
    private final File file;
    SecureStore(Context c) { file = new File(c.getFilesDir(), "luma_vault.bin"); }

    ArrayList<VaultItem> load() throws Exception {
        ArrayList<VaultItem> result = new ArrayList<>();
        if (!file.exists()) return result;
        byte[] packed;
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n); packed = out.toByteArray();
        }
        ByteBuffer buf = ByteBuffer.wrap(packed); int ivLen = buf.getInt();
        if (ivLen < 12 || ivLen > 32 || buf.remaining() <= ivLen) throw new SecurityException("資料格式錯誤");
        byte[] iv = new byte[ivLen]; buf.get(iv); byte[] encrypted = new byte[buf.remaining()]; buf.get(encrypted);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        JSONArray json = new JSONArray(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
        for (int i = 0; i < json.length(); i++) result.add(VaultItem.fromJson(json.getJSONObject(i)));
        return result;
    }

    void save(List<VaultItem> items) throws Exception {
        JSONArray json = new JSONArray(); for (VaultItem item : items) json.put(item.toJson());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(json.toString().getBytes(StandardCharsets.UTF_8)); byte[] iv = cipher.getIV();
        ByteBuffer packed = ByteBuffer.allocate(4 + iv.length + encrypted.length).putInt(iv.length).put(iv).put(encrypted);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(packed.array()); out.getFD().sync(); }
        if (file.exists() && !file.delete()) throw new IOException("無法更新保險庫");
        if (!tmp.renameTo(file)) throw new IOException("無法儲存保險庫");
    }

    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return gen.generateKey();
    }
}
