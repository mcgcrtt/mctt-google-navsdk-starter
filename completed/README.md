# 實作 Google Navigation SDK for Android 模擬導航

## 1. 簡介
在本 Codelab 中，您將學習如何使用 Google Navigation SDK for Android 建置一個簡單的導航應用程式。本教學以台北市為整合背景，實作從**台北 101** 前往**台北車站商圈**（全程約 5.5 公里）的車輛模擬導航。

### 您將建置的內容
* 整合 Navigation SDK v7.8.0 的 Android 應用程式。
* 處理複雜地下共構站體所導致的路由錯誤。
* 實作動態隨車頭轉動（Heading-Up）且固定縮放層級的相機追蹤邏輯。

### 您將學到的內容
* 如何正確配置 Navigation SDK 的生命週期以防止非同步崩潰。
* 如何使用經緯度路點避開 `WAYPOINT_ERROR`。
* 如何透過狀態機解鎖相機旋轉限制，並實作延遲防蓋台機制。
* 如何在不干涉自動跟隨狀態的情況下限制地圖縮放邊界。

---

## 2. 設定開發環境與模擬器限制

### 2.1 模擬器記憶體分頁限制
在 Android Studio 中建立 API 34 或 API 36 的虛擬裝置（Emulator）時，系統映像檔（System Image）選單中包含帶有 `(16 KB page size)` 後綴的核心機型。

> ⚠️ **重要警告：**
> 請勿選擇帶有 `16 KB page size` 的模擬器。Navigation SDK 的底層原生原生庫（.so）目前僅支援標準的 4KB 分頁機制。若誤用 16KB 分頁模擬器，應用程式將在啟動時直接閃退（Segment Fault）。請選擇標準的 **ARM64 / x86_64** 官方預設映像檔。

### 2.2 開發環境需求
* Android Studio Ladybug 或更高版本。
* Kotlin `2.3.0`+。
* Navigation SDK for Android `7.8.0`。

---

## 3. 配置秘密金鑰 (secrets.properties)

本專案區分為兩個獨立的 Android 專案目錄：`/starter`（練習版）與 `/completed`（完成版）。API 金鑰必須配置在您所要執行的專案根目錄下。

1. 登入 Google Cloud Console，確認您的帳戶已啟用帳單功能，並開通 **Maps SDK for Android** 以及 **Navigation SDK**。
2. 根據您要執行的專案，在該專案的目錄下建立名為 `secrets.properties` 的檔案：
   * 練習版路徑：`[專案根目錄]/starter/secrets.properties`
   * 完成版路徑：`[專案根目錄]/completed/secrets.properties`
3. 在 `secrets.properties` 中加入您的 API 金鑰：
   ```properties
   MAPS_API_KEY=YOUR_ACTUAL_GOOGLE_MAPS_API_KEY


4. 在 Android Studio 中開啟對應資料夾，點擊 **Sync Project with Gradle Files** 進行同步。

---

## 4. 對齊視圖生命週期與地圖初始化

`NavigationView` 內部包含封裝的 C++ 狀態機，對於調用順序有嚴格限制。如果將 `onCreate` 移至非同步回呼或延遲掛載，會觸發 `ApiIllegalStateException`。

打開 `MainActivity.kt`，確保 `setContentView` 與 `navView.onCreate` 在 Activity 生命週期的第一時間執行：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.addInterface(...) // 依專案既有基礎配置
    super.onCreate(savedInstanceState)
    
    // 1. 確保視圖與 SDK 生命週期同步初始化
    setContentView(R.layout.activity_main)
    navView = findViewById(R.id.navigation_view)
    navView.onCreate(savedInstanceState)

    // 2. 初始化地圖配置並設定縮放限制
    navView.getMapAsync { googleMap ->
        // 限制地圖的最大與最小縮放層級，用以替代會中斷追蹤的 moveCamera
        googleMap.setMaxZoomPreference(14.5f)
        googleMap.setMinZoomPreference(10.0f)
    }
}

```

---

## 5. 要求導航路由與路點處理

台北車站屬於地下多鐵共構空間，若直接使用 Place ID 字串請求汽車導航，路由演算法常因對齊到地底下的軌道層節點而傳回 `WAYPOINT_ERROR`。本步驟改用台北車站站前地面層的經緯度座標來配置 `Waypoint`。

請在 `MainActivity.kt` 中加入常數定義，並實作 `navigateToPlace` 方法：

```kotlin
// 定義起終點經緯度
private val startLocation = LatLng(25.033964, 121.564468)       // 台北 101
private val destinationLocation = LatLng(25.045892, 121.515164) // 台北車站站前地面馬路

private fun navigateToPlace(destinationLatLng: LatLng) {
    val waypoint: Waypoint? = try {
        // 使用純經緯度建置，精確引導路由伺服器至地面實體車道
        Waypoint.builder()
            .setLatLng(destinationLatLng.latitude, destinationLatLng.longitude)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }

    // 配置音訊引導設定
    val audioSettings = AudioGuidanceSettings.builder()
        .setGuidanceMode(AudioGuidanceSettings.GuidanceMode.VOICE_ALERTS_AND_GUIDANCE)
        .build()
    mNavigator?.setAudioGuidanceSettings(audioSettings)

    // 送出路由請求並監聽結果
    val pendingRoute = mNavigator?.setDestination(waypoint)
    pendingRoute?.setOnResultListener { code ->
        when (code) {
            com.google.android.libraries.navigation.Navigator.RouteStatus.OK -> {
                // 路由成功，準備啟動導航引導與模擬
                startSimulation()
            }
            com.google.android.libraries.navigation.Navigator.RouteStatus.WAYPOINT_ERROR -> {
                Log.e("NavCodelab", "路點解析錯誤 (WAYPOINT_ERROR)")
            }
            else -> Log.e("NavCodelab", "其他路由錯誤，代碼: $code")
        }
    }
}

```

---

## 6. 解鎖與啟用隨車頭轉動視角

當取得成功路由後，必須依序處理兩個相機限制以防跟隨失效：

1. **解除朝北限制**：必須先調用 `startGuidance()` 讓系統切換至導航引導狀態，否則地圖相機將被鎖定在正北朝上。
2. **防初始化蓋台**：導航與模擬啟動時底層會重組視圖，立即設定相機轉動會被重置覆蓋。必須使用 `Handler` 延遲注入設定。

請在 `MainActivity.kt` 中實作以下導航啟動與相機設定邏輯：

```kotlin
private fun startSimulation() {
    // 1. 開啟導航狀態機，解鎖相機方位角轉動限制
    mNavigator?.startGuidance()

    // 2. 啟動模擬行車（設定為 5 倍行駛速度）
    mNavigator?.simulator?.simulateLocationsAlongExistingRoute(
        SimulationOptions().speedMultiplier(5f)
    )

    // 3. 延遲注入隨車頭追蹤相機
    applyCameraTrackingWithDelay()
}

private fun applyCameraTrackingWithDelay() {
    // 延遲 1500 毫秒以避開導航初始化時的視圖重組波峰
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        navView.getMapAsync { googleMap ->
            // 🎯 設定 2D 隨車頭轉動視角 (Heading Up)
            // 由於前述已設定 setMaxZoomPreference(14.5f)，
            // 相機會維持在 14.5f 視野且自動追蹤狀態不會因 moveCamera 而被解除。
            googleMap.followMyLocation(
                com.google.android.gms.maps.GoogleMap.CameraPerspective.TOP_DOWN_HEADING_UP
            )
        }
    }, 1500)
}

```

---

## 7. 驗證應用程式

完成程式碼修改後，將應用程式編譯並部署至實機或標準 4KB 模擬器中：

1. **初始狀態**：應用程式啟動後，相機自動空投並定格在台北 101 周邊，藍色定位點保持靜止。
2. **啟動導航**：點擊「開始模擬導航」按鈕，系統成功計算路由。經過 1.5 秒延遲後，地圖會自動旋轉，將定位點調整為車頭垂直朝上，縮放層級調整至 14.5f。
3. **動態追蹤**：虛擬車輛沿著信義路與忠孝東路移動時，地圖會即時平移跟隨；當車輛在路口轉彎時，地圖會順暢旋轉以維持車頭朝上的鳥瞰視野。

