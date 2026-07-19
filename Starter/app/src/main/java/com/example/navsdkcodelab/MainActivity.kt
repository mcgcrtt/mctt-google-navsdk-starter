package com.example.navsdkcodelab

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.contract.ActivityResultContracts

// Google 地圖核心與導航元件導入
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.navigation.AudioGuidanceSettings

class MainActivity : AppCompatActivity() {

    private lateinit var navView: NavigationView
    private var mNavigator: Navigator? = null

    private var arrivalListener: Navigator.ArrivalListener? = null
    private var routeChangedListener: Navigator.RouteChangedListener? = null

    private val splashScreenDelayMillis = 1000L
    // 📍 完美替換：台北 101 (Taipei 101) 精確經緯度起點座標
    private val startLocation = LatLng(25.033964, 121.564468)
    // 🎯 終點物理座標：新光三越台北站前店前方馬路 (LatLng 備援方案)
    private val destinationLocation = LatLng(25.045892, 121.515164)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 立刻載入佈局並初始化 navView，確保全程跟上 Activity 原生生命週期
        setContentView(R.layout.activity_main)
        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigation_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 權限與延時用來控制「導航流程的啟動」
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.any { !checkPermissionGranted(it) }) {
            val permissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionResults ->
                if (permissionResults.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                    startNavigationFlow()
                } else {
                    showToast("需要定位權限才能進行導航")
                    finish()
                }
            }
            permissionsLauncher.launch(permissions)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                startNavigationFlow()
            }, splashScreenDelayMillis)
        }
    }

    private fun startNavigationFlow() {
        initializeNavigationApi()
    }

    @SuppressLint("MissingPermission")
    private fun initializeNavigationApi() {
        // 📍 階段一：地圖一開起來，立刻「單純」將相機定格到倫敦起點（這不需要導航引擎，100% 安全）
        navView.getMapAsync { googleMap ->
            googleMap.moveCamera(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(startLocation, 16f)
            )
            // ⚠️ 注意：原本這裡的 googleMap.followMyLocation(...) 已經被移走了！
        }

        // 呼叫 Google 導航核心引擎
        NavigationApi.getNavigator(
            this,
            object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(navigator: Navigator) {
                    mNavigator = navigator
                    navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)

                    // 1. 將導航器的虛擬定位定在倫敦起點
                    navigator.simulator?.setUserLocation(startLocation)

                    registerNavigationListeners()

                    // ⚡ 關鍵修正：當引擎 100% 準備就緒後，此時才安全地要求地圖開啟 3D 視角跟隨定位！
                    navView.getMapAsync { googleMap ->
                        googleMap.followMyLocation(com.google.android.gms.maps.GoogleMap.CameraPerspective.TILTED)
                    }

                    // 🎯 階段二：綁定按鈕點擊事件
                    val startButton = findViewById<Button>(R.id.btn_start_navigation)
                    startButton.setOnClickListener {
                        showToast("正在規劃路線...")
                        navigateToPlace(destinationLocation)

                        // 點擊後讓按鈕消失，完整釋放導航視野
                        startButton.visibility = android.view.View.GONE
                    }
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
                            showToast("Error loading Navigation API: Your API key is invalid or not authorized.")
                        }
                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
                            showToast("Error loading Navigation API: User did not accept Terms.")
                        }
                        else -> showToast("Error loading Navigation API: $errorCode")
                    }
                }
            }
        )
    }

    /**
     * 建立導航至特定 Place ID 的目的地方法
     */
    @SuppressLint("MissingPermission")
    private fun navigateToPlace(destinationLatLng: LatLng) {
        val waypoint: Waypoint? = try {
            // ⚡ 終極防錯：純經緯度建置，不需要 Places API 權限，100% 秒速解算成功！
            Waypoint.builder()
                .setLatLng(destinationLatLng.latitude, destinationLatLng.longitude)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("路點建立失敗")
            return
        }

        // 隱藏頂部 ActionBar 以最大化導航畫面
        supportActionBar?.hide()

        // ⚡ 使用 2026 現代化全新 Builder 設定語音播報，完美取代過時的整數常數舊寫法
        val audioSettings = AudioGuidanceSettings.builder()
            .setGuidanceMode(AudioGuidanceSettings.GuidanceMode.VOICE_ALERTS_AND_GUIDANCE)
            .build()
        mNavigator?.setAudioGuidanceSettings(audioSettings)

        // 請求導航路線
        val pendingRoute = mNavigator?.setDestination(waypoint)

        // 監聽路線計算結果
        pendingRoute?.setOnResultListener { code ->
            when (code) {
                com.google.android.libraries.navigation.Navigator.RouteStatus.OK -> {
                    showToast("路線規劃成功，啟動模擬導航！")

                    // ⚡ 終極修正 1：必須呼叫這行！正式啟動導航引導狀態機，相機轉動限制才會被全面解鎖！
                    mNavigator?.startGuidance()

                    // 2. 啟動模擬行車
                    mNavigator?.simulator?.simulateLocationsAlongExistingRoute(
                        SimulationOptions().speedMultiplier(10f)
                    )

                    // ⚡ 終極修正 2：延遲 1.5 秒，等導航面板跟模擬器徹底安穩下來後，再強行將視角鎖死
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        navView.getMapAsync { googleMap ->
                            // 強制將地圖最高放大層級鎖定在 14.5f 的全景鳥瞰高度
                            googleMap.setMaxZoomPreference(14.5f)

                            googleMap.followMyLocation(
                                com.google.android.gms.maps.GoogleMap.CameraPerspective.TOP_DOWN_HEADING_UP
                            )
                        }
                    }, 1500)

                }
                com.google.android.libraries.navigation.Navigator.RouteStatus.ROUTE_CANCELED -> {
                    showToast("導航已被取消")
                }
                com.google.android.libraries.navigation.Navigator.RouteStatus.NO_ROUTE_FOUND -> {
                    showToast("找不到對應路線，請檢查虛擬起點設定。")
                }
                com.google.android.libraries.navigation.Navigator.RouteStatus.NETWORK_ERROR -> {
                    showToast("網路連線錯誤，無法計算路線")
                }
                else -> showToast("引導啟動錯誤，狀態: $code")
            }
        }
    }

    private fun registerNavigationListeners() {
        mNavigator?.let { navigator ->
            arrivalListener = Navigator.ArrivalListener {
                showToast("使用者已抵達目的地！")
                mNavigator?.clearDestinations()
            }
            navigator.addArrivalListener(arrivalListener)

            routeChangedListener = Navigator.RouteChangedListener {
                showToast("路線已重新規劃（Rerouted）")
            }
            navigator.addRouteChangedListener(routeChangedListener)
        }
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED

    private fun showToast(errorMessage: String) {
        // 💡 改為 LENGTH_SHORT，解決 Toast 連續跳出時嚴重的體感延遲排隊問題
        Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::navView.isInitialized) navView.onSaveInstanceState(outState)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::navView.isInitialized) navView.onTrimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::navView.isInitialized) navView.onConfigurationChanged(newConfig)
    }

    override fun onStart() {
        super.onStart()
        if (::navView.isInitialized) navView.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (::navView.isInitialized) navView.onResume()
    }

    override fun onPause() {
        if (::navView.isInitialized) navView.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (::navView.isInitialized) navView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::navView.isInitialized) navView.onDestroy()
        mNavigator?.let { navigator ->
            if (arrivalListener != null) navigator.removeArrivalListener(arrivalListener)
            if (routeChangedListener != null) navigator.removeRouteChangedListener(routeChangedListener)
            navigator.simulator?.unsetUserLocation()
            navigator.cleanup()
        }
        super.onDestroy()
    }
}