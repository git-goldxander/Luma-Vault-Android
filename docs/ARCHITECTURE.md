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

主密碼本身不會寫入磁碟。保險庫資料不會離開裝置，App 也未要求網路權限。

## Storage guarantees

- 每次加密使用新的 GCM IV。
- 加密資料包含認證標籤，可偵測內容遭竄改。
- 寫入先完成暫存檔並同步，再取代正式保險庫檔案。
- Android cloud backup、full backup 與 device transfer 均停用。
