package com.lumavault.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

final class BackupCodec {
    private static final int MAGIC=0x4C564231, VERSION=1, ITERATIONS=210000, MAX_BYTES=25*1024*1024;
    private static final byte[] AAD="LumaVault-Portable-Backup-v1".getBytes(StandardCharsets.UTF_8);

    static void write(OutputStream destination, List<VaultItem> items, String password, boolean transfer) throws Exception {
        JSONObject root=new JSONObject().put("schema",1).put("createdAt",System.currentTimeMillis())
                .put("kind",transfer?"transfer":"backup");
        JSONArray array=new JSONArray();for(VaultItem item:items)array.put(item.toJson());root.put("items",array);
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)){gzip.write(root.toString().getBytes(StandardCharsets.UTF_8));}
        SecureRandom random=new SecureRandom();byte[] salt=new byte[24],iv=new byte[12];random.nextBytes(salt);random.nextBytes(iv);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt,ITERATIONS),new GCMParameterSpec(128,iv));cipher.updateAAD(AAD);
        byte[] encrypted=cipher.doFinal(compressed.toByteArray());
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(destination))){
            out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(ITERATIONS);out.writeInt(salt.length);out.writeInt(iv.length);out.writeInt(encrypted.length);
            out.write(salt);out.write(iv);out.write(encrypted);out.flush();
        }
    }

    static ArrayList<VaultItem> read(InputStream source, String password) throws Exception {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(source))){
            if(in.readInt()!=MAGIC)throw new SecurityException("不是 Luma Vault 備份檔");
            if(in.readInt()!=VERSION)throw new SecurityException("不支援的備份版本");int iterations=in.readInt();
            int saltLength=in.readInt(),ivLength=in.readInt(),cipherLength=in.readInt();
            if(iterations<100000||iterations>1000000||saltLength<16||saltLength>64||ivLength<12||ivLength>32||cipherLength<16||cipherLength>MAX_BYTES)throw new SecurityException("備份檔格式錯誤");
            byte[] salt=new byte[saltLength],iv=new byte[ivLength],encrypted=new byte[cipherLength];in.readFully(salt);in.readFully(iv);in.readFully(encrypted);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,salt,iterations),new GCMParameterSpec(128,iv));cipher.updateAAD(AAD);
            byte[] compressed=cipher.doFinal(encrypted);ByteArrayOutputStream plain=new ByteArrayOutputStream();
            try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(compressed))){byte[] buffer=new byte[4096];int count,total=0;while((count=gzip.read(buffer))!=-1){total+=count;if(total>MAX_BYTES)throw new SecurityException("備份資料過大");plain.write(buffer,0,count);}}
            JSONObject root=new JSONObject(plain.toString(StandardCharsets.UTF_8.name()));if(root.optInt("schema")!=1)throw new SecurityException("不支援的資料格式");
            JSONArray array=root.getJSONArray("items");ArrayList<VaultItem> result=new ArrayList<>();for(int i=0;i<array.length();i++)result.add(VaultItem.fromJson(array.getJSONObject(i)));return result;
        }
    }

    private static SecretKeySpec key(String password,byte[] salt,int iterations)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);byte[] bytes=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();return new SecretKeySpec(bytes,"AES");
    }
}
