package com.lumavault.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateChecker {
    private static final String LATEST_RELEASE = "https://api.github.com/repos/git-goldxander/Luma-Vault-Android/releases/latest";

    static void check(MainActivity activity) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMessage("正在向 GitHub 檢查最新版本…");
        progress.setCancelable(false);
        progress.show();
        new Thread(() -> {
            try {
                Release release = fetch();
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    if (isNewer(release.version, BuildConfig.VERSION_NAME)) showUpdate(activity, release);
                    else new AlertDialog.Builder(activity).setTitle("已是最新版本")
                            .setMessage("目前版本：v" + BuildConfig.VERSION_NAME + "\nGitHub 最新版本：v" + release.version)
                            .setPositiveButton("確定", null).show();
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    progress.dismiss();
                    new AlertDialog.Builder(activity).setTitle("無法檢查更新")
                            .setMessage("請確認網路連線後再試一次。\n\n保險庫資料不會在更新檢查時上傳。")
                            .setPositiveButton("確定", null).show();
                });
            }
        }, "luma-update-check").start();
    }

    private static Release fetch() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "Luma-Vault-Android/" + BuildConfig.VERSION_NAME);
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("GitHub response " + connection.getResponseCode());
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject json = new JSONObject(body.toString()); String tag = json.getString("tag_name");
            String version = tag.startsWith("v") ? tag.substring(1) : tag;
            String download = json.optString("html_url", "https://github.com/git-goldxander/Luma-Vault-Android/releases/latest");
            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) for (int i=0;i<assets.length();i++) {
                JSONObject asset=assets.getJSONObject(i); String name=asset.optString("name").toLowerCase();
                if (name.endsWith(".apk")) { download=asset.getString("browser_download_url"); break; }
            }
            return new Release(version, json.optString("body"), download);
        } finally { connection.disconnect(); }
    }

    private static void showUpdate(MainActivity activity, Release release) {
        String notes=release.notes.trim(); if (notes.length()>700) notes=notes.substring(0,700)+"…";
        String message="目前版本：v"+BuildConfig.VERSION_NAME+"\n最新版本：v"+release.version;
        if (!notes.isEmpty()) message += "\n\n更新內容：\n" + notes;
        message += "\n\n確定要更新嗎？確認後將開啟 GitHub 官方 APK 下載。";
        new AlertDialog.Builder(activity).setTitle("發現新版本")
                .setMessage(message).setNegativeButton("稍後再說",null)
                .setPositiveButton("確定更新",(dialog,which)->{
                    try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(release.downloadUrl))); }
                    catch (Exception e) { activity.toast("無法開啟下載連結"); }
                }).show();
    }

    static boolean isNewer(String remote, String current) {
        String[] a=remote.split("[.-]"), b=current.split("[.-]"); int count=Math.max(a.length,b.length);
        for(int i=0;i<count;i++){int x=i<a.length?number(a[i]):0,y=i<b.length?number(b[i]):0;if(x!=y)return x>y;}
        return false;
    }
    private static int number(String value){try{return Integer.parseInt(value.replaceAll("[^0-9]", ""));}catch(Exception e){return 0;}}
    private static final class Release { final String version,notes,downloadUrl; Release(String v,String n,String d){version=v;notes=n;downloadUrl=d;} }
}
