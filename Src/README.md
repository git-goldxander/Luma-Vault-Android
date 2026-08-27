# Luma Vault

Luma Vault 是一款完全離線的 Android 個人密碼保險庫，介面與功能由零開始重新設計。

## 功能

- 主密碼保護（PBKDF2-HMAC-SHA256，120,000 次迭代）
- Android Keystore 管理的 AES-256-GCM 加密資料庫
- 生物辨識快速解鎖（可在安全中心開關）
- 搜尋、分類、收藏與弱密碼篩選
- 密碼強度評分、重複密碼偵測與安全儀表板
- 12–36 字元的高強度密碼產生器
- 帳號、密碼與網站一鍵複製，60 秒後清除剪貼簿
- 進入背景 2 分鐘後自動上鎖
- 完全離線，無網路權限、無廣告、無追蹤

## 建置

需要 JDK 17 與 Android SDK 35。在 `Src` 目錄執行：

```powershell
.\gradlew.bat assembleRelease
```

最低支援 Android 9（API 28）。正式散佈時請使用自己的長期簽署金鑰。
