# Google Navigation SDK for Android - Codelab (Taipei Route Edition)

本專案提供 Google Navigation SDK 在 Android 環境下的實作範例，規劃路線為台北 101 至台北車站商圈（全長約 5.5 公里）。專案適配 Android 14+ (API 34/36)，並實作 Navigation SDK v7.8.0 的架喚與配置規範。

此版本針對官方 [Codelab](https://codelabs.developers.google.com/codelabs/maps-platform/navigation-sdk-101-android?hl=zh-tw#2) 原始範例中的非同步生命週期錯誤、導航初始化問題進行了修正。

---

## 🚨 模擬器環境限制說明

在 Android Studio 中建立 API 34 / API 36 虛擬裝置（Emulator）時，系統映像檔選單中包含帶有 `(16 KB page size)` 後綴的特殊核心機型。

> **注意：**
> Google Navigation SDK 的底層原生動態庫（.so）目前不支援 16KB 記憶體分頁對齊機制。若使用該模擬器，App 將於啟動時發生底層記憶體錯誤（Segment Fault）並閃退。請選擇標準的 **ARM64 / x86_64 (4KB)** 官方預設映像檔。

---

## 📦 專案目錄結構

專案區分為兩個資料夾：

```text
├── /starter      # 初始專案（已更新至 SDK 7.8.0 與相關相依套件，保留空白方法供練習）
└── /solution     # 待開發之特定

```

---

## 🛠️ 開發環境需求

* **Android Studio**: Ladybug 或更高版本
* **Kotlin**: `2.3.0`+
* **Navigation SDK for Android**: `7.8.0`
* **測試環境**: Android 14 (API 34) 實機或標準 4KB 模擬器

---

## 🚀 快速開始與 API Key 設定

1. 登入 Google Cloud Console，確認專案已啟用 **Maps SDK for Android**、**Places API** 以及 **Navigation SDK**，且該帳戶已啟用帳單功能（導航路由要求必須綁定帳單）。
2. 在專案的**根目錄**下，建立名為 `secrets.properties` 的檔案。
3. 在檔案中填入您的 API 金鑰：
```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY

```


4. 點擊 Android Studio 的 **Sync Project with Gradle Files** 進行同步，即可編譯執行。

---

## 💡 技術修正說明

專案在中實作了以下技術修正：

### 1. 修正路點解析錯誤（WAYPOINT_ERROR）

* **問題**：台北車站為地下站體共構空間。若僅傳入 Places ID 字串，汽車導航算法可能因無法對齊地面車道而傳回 `WAYPOINT_ERROR`。
* **解法**：實作 v7.4.0 引入的 Enhanced Location Context 規範，在建立 `Waypoint` 時採取 **Place ID + LatLng 座標（25.045892, 121.515164）雙重綁定**。優先利用經緯度引導至地面車道。本專案同時保留純經緯度備援代碼，以利在未開通 Places API 的環境下測試。

### 2. 修正相機初始化朝北不跟隨問題

* **問題**：設定隨車頭轉動視角（`TOP_DOWN_HEADING_UP`）後，在模擬行車啟動的瞬間，相機常被系統初始化重置為正北朝上。
* **解法**：
1. 呼叫 `mNavigator?.startGuidance()` 正式啟動導航引導狀態。
2. 使用 `Handler` 延遲 **1500 毫秒**，避開初始化時的視圖重組。
3. 隨後呼叫 `followMyLocation(CameraPerspective.TOP_DOWN_HEADING_UP)` 以鎖定車頭方位角。


### 3. 設定地圖固定縮放層級與跟隨鎖定

* **問題**：若呼叫 `googleMap.moveCamera()` 調整視野，會被系統視為使用者手勢干涉而解除相機的自動跟隨狀態。
* **解法**：移除 `moveCamera` 呼叫，直接設定地圖原生的縮放限制 `setMaxZoomPreference(14.5f)`。當導航引擎將鏡頭拉近時會受到此物理限制，使畫面維持在 14.5f 的視野，且相機跟隨鎖定狀態不會失效。

### 4. 音訊 API 重構與生命週期綁定

* **優化**：依據新版 SDK 規範，廢棄舊式整數常數音訊方法，改用 `AudioGuidanceSettings.builder()` 架構。並確保 `NavigationView` 的 `onCreate()` 在 Activity 生命週期的第一時間執行，避免非同步載入引發的 `ApiIllegalStateException`。

---

## 🎮 執行流程

* **導航前**：App 啟動後，地圖以台北 101 為中心固定（Zoom = 16f），藍色定位點保持靜止，屏蔽實體裝置 GPS，下方顯示「開始模擬導航」按鈕。
* **導航中**：點擊按鈕後按鈕隱藏。經過 1.5 秒緩衝後，地圖調整至 14.5f 縮放層級，並自動旋轉使車頭朝上。虛擬定位車輛沿規劃路線前進，地圖隨車輛移動動態平移與旋轉。
