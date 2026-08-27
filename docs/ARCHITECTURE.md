# Architecture & Security

## Components

- `MainActivity`：初次設定、主密碼驗證、生物辨識與自動上鎖。
- `VaultView`：保險庫首頁、搜尋篩選、編輯、密碼產生器及安全中心。
- `PinManager`：主密碼驗證值的衍生與比對。
- `SecureStore`：加密資料讀寫與 Android Keystore 金鑰管理。
- `VaultItem`：密碼項目資料模型與強度計算。

## Data flow

```text
Master password ──PBKDF2──> verification hash
                              │
Android Keystore ──AES key──> AES-256-GCM ──> encrypted local vault
```

主密碼本身不會寫入磁碟，保險庫資料不會離開裝置。網路功能與保險庫完全分離，只在使用者點選「檢查 GitHub 更新」時讀取公開 Release JSON。

## Storage guarantees

- 每次加密使用新的 GCM IV。
- 加密資料包含認證標籤，可偵測內容遭竄改。
- 寫入先完成暫存檔並同步，再取代正式保險庫檔案。
- Android cloud backup、full backup 與 device transfer 均停用。

## Update checks

`UpdateChecker` 使用 HTTPS 呼叫 GitHub 的 latest release endpoint，讀取 `tag_name`、release body 與 APK 的 `browser_download_url`。檢查在背景執行；只有使用者確認更新後，才以系統瀏覽器開啟 GitHub APK 下載連結。

## Adaptive system bars

`SystemBarInsets` 在 Android 15（API 35）全面螢幕強制生效時讀取 `systemBars` 與 `displayCutout` 的實際安全範圍，將狀態列、瀏海、手勢列或三鍵導覽列的寬高動態加入根畫面 padding。舊版 Android 繼續使用系統原有的非全面螢幕配置，避免重複留白。

## Portable backup and phone transfer

`BackupCodec` 將版本化 JSON 先以 GZIP 壓縮，再使用隨機 24-byte salt、PBKDF2-HMAC-SHA256（210,000 次）衍生 256-bit 金鑰，最後以 AES-256-GCM 認證加密。備份密碼或一次性轉移碼不會寫入檔案。

`BackupManager` 使用 Android Storage Access Framework 讓使用者決定備份儲存與匯入位置，不要求廣泛儲存權限。轉移檔透過限制在 App cache 目錄的 `FileProvider` 唯讀分享；QR Code 只包含一次性轉移碼的 `lumavault://transfer` URI，不包含保險庫內容。舊手機刪除動作必須在分享完成後由使用者二次確認。
