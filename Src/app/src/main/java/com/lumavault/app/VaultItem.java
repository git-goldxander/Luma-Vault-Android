package com.lumavault.app;

import org.json.JSONObject;

final class VaultItem {
    String id, title, username, password, website, category, notes;
    boolean favorite;
    long updatedAt;

    VaultItem(String title, String username, String password, String website, String category, String notes) {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = title; this.username = username; this.password = password;
        this.website = website; this.category = category; this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    JSONObject toJson() throws Exception {
        return new JSONObject().put("id", id).put("title", title).put("username", username)
                .put("password", password).put("website", website).put("category", category)
                .put("notes", notes).put("favorite", favorite).put("updatedAt", updatedAt);
    }

    static VaultItem fromJson(JSONObject o) {
        VaultItem item = new VaultItem(o.optString("title"), o.optString("username"), o.optString("password"),
                o.optString("website"), o.optString("category", "其他"), o.optString("notes"));
        item.id = o.optString("id", item.id); item.favorite = o.optBoolean("favorite");
        item.updatedAt = o.optLong("updatedAt", System.currentTimeMillis()); return item;
    }

    int strength() {
        int s = Math.min(40, password.length() * 3);
        if (password.matches(".*[A-Z].*")) s += 15;
        if (password.matches(".*[a-z].*")) s += 15;
        if (password.matches(".*[0-9].*")) s += 15;
        if (password.matches(".*[^A-Za-z0-9].*")) s += 15;
        return Math.min(100, s);
    }
}
