<div align="center">
  <img src="icon/luma_vault_icon.png" width="128" alt="Luma Vault icon">
  <h1>Luma Vault</h1>
  <p><strong>漂亮、離線、安全的 Android 個人密碼保險庫</strong></p>

  [![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
  [![API](https://img.shields.io/badge/API-28%2B-5865F2)](https://developer.android.com/about/versions/pie)
  [![License](https://img.shields.io/badge/License-MIT-32D6A0.svg)](LICENSE)
  [![Release](https://img.shields.io/badge/release-v1.2.0-32D6A0)](https://github.com/git-goldxander/Luma-Vault-Android/releases/latest)
</div>

Luma Vault 是一款以離線保險庫為核心的 Android 密碼管理 App。它不含廣告與追蹤程式，所有帳號資料都以 Android Keystore 管理的金鑰加密後保存在裝置內；網路僅在使用者主動檢查 GitHub 更新時使用。

## ✨ 主要功能

| 功能 | 說明 |
| --- | --- |
| 🔐 加密保險庫 | AES-256-GCM 加密，金鑰由 Android Keystore 管理 |
| 🧠 安全中心 | 密碼強度評分、弱密碼與重複密碼檢查 |
| 🪄 密碼產生器 | 產生 12–36 字元的高強度隨機密碼 |
| 🔎 快速整理 | 搜尋、六種分類、收藏及弱密碼篩選 |
| 👆 雙重登入方式 | 開啟 App 時可自由選擇主密碼或指紋登入 |
| 📋 安全複製 | 複製帳號或密碼，60 秒後自動清除剪貼簿 |
| ⏱️ 自動上鎖 | App 進入背景兩分鐘後自動鎖定 |
| 🔄 安全更新檢查 | 手動比對 GitHub 最新版本，確認後才開啟 APK 下載 |
| 📱 全螢幕相容 | 動態避開狀態列、瀏海、手勢列與三鍵導覽列 |
| 💾 加密備份 | 匯出／匯入可攜式 `.lvault` 備份，可選合併或取代 |
| 📲 QR 手機轉移 | 一次性 QR 轉移碼、系統分享，以及轉移後保留／刪除選項 |

## 🎨 設計

介面採用深靛藍、薄荷綠與卡片式資訊層級，針對單手操作設計。保險庫首頁能直接搜尋、切換分類、檢視安全分數，以及快速新增或產生密碼。

## 📦 安裝

1. 前往 [Releases](https://github.com/git-goldxander/Luma-Vault-Android/releases/latest)。
2. 下載最新的 `Luma_Vault-v1.2.0.apk`。
3. 在 Android 裝置上開啟 APK；若系統提示，允許目前的檔案管理器安裝未知來源 App。
4. 第一次啟動時建立至少 6 個字元的主密碼。

> [!IMPORTANT]
> 忘記主密碼後無法還原保險庫；移除 App 也會刪除裝置上的加密資料。請妥善保存重要帳號的其他備份。

## 🛠️ 從原始碼建置

需求：JDK 17、Android SDK 35。

```powershell
cd Src
.\gradlew.bat assembleRelease
```

Linux 或 macOS：

```bash
cd Src
./gradlew assembleRelease
```

未簽署的 APK 會產生在 `Src/app/build/outputs/apk/release/`。正式發布請使用自己的長期簽署金鑰。

## 🗂️ 專案結構

```text
Luma_Vault/
├── .github/workflows/   # GitHub Actions Android CI
├── Src/                 # Android Gradle 專案與完整原始碼
├── icon/                # App 圖示素材
├── docs/                # 架構與安全設計文件
├── CHANGELOG.md         # 版本更新記錄
├── SECURITY.md          # 安全問題回報方式
└── LICENSE              # MIT License
```

## 🔒 安全設計

- 主密碼驗證值使用隨機 24-byte salt 與 PBKDF2-HMAC-SHA256（120,000 次迭代）。
- 保險庫使用 AES-256-GCM 認證加密，每次儲存都產生新的 IV。
- 寫入採暫存檔同步落盤後再取代正式檔案，降低資料損壞風險。
- Android 備份與裝置轉移已停用；網路只用於手動讀取 GitHub Release 公開資訊。
- 可攜式備份使用獨立密碼、PBKDF2-HMAC-SHA256 與 AES-256-GCM，不匯出 Android Keystore 金鑰。

更多細節請參閱 [架構文件](docs/ARCHITECTURE.md)。

## 🤝 參與貢獻

歡迎提出 Issue 或 Pull Request。開始前請先閱讀 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 📄 授權

本專案採用 [MIT License](LICENSE)。
