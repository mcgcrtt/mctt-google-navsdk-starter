# Google Navigation SDK for Android - Codelab (Taipei Route Edition)

本專案提供 Google Navigation SDK 在 Android 環境下的實作範例，並實作 Navigation SDK v7.8.0 的架構與配置規範。


---

## 📦 專案目錄結構

專案區分為兩個獨立的 Android 專案資料夾：

`/Starter`
初始專案（來源為：https://github.com/googlemaps-samples/codelab-navigation-101-android-kotlin.git）

`/completed`
此版本針對官方 [Codelab](https://codelabs.developers.google.com/codelabs/maps-platform/navigation-sdk-101-android?hl=zh-tw#0) 原始範例中的非同步生命週期錯誤、複雜地下站體引發的路點解析錯誤（WAYPOINT_ERROR），以及導航初始化時相機方位角重置問題進行了調整與修正。

---

## 🚀 快速開始與 API 金鑰設定

由於 `/Starter` 與 `/completed` 為兩個獨立的 Gradle 專案，API 金鑰必須配置在對應專案的目錄下。請依循以下步驟操作：

1. 登入 Google Cloud Console，確認專案已啟用 **Maps SDK for Android** 以及 **Navigation SDK**，且該帳戶已啟用帳單功能（導航路由要求必須綁定帳單）。
2. 根據您欲開啟與執行的專案，在該專案的**根目錄**下建立名為 `secrets.properties` 的檔案：
* 若欲執行練習版，請建立於：`專案根目錄/Starter/secrets.properties`
* 若欲執行完成版，請建立於：`專案根目錄/completed/secrets.properties`


3. 在建立的 `secrets.properties` 檔案中填入您的 API 金鑰（此檔案已透過各層級 `.gitignore` 排除，不會被提交至 GitHub）：
```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY

```

4. 使用 Android Studio 開啟對應的資料夾（`/Starter` 或 `/completed`）。
5. 點擊工具列的 **Sync Project with Gradle Files** 進行同步，完成後即可編譯執行。

---


## 🏢 技術支援與專案維護

本專案由 **思想科技 Master Concept** 編譯維護。

**思想科技 Master Concept** 為亞太地區領先的雲端技術顧問，同時也是 Google Cloud 菁英合作夥伴（Google Cloud Premier Partner）。我們深耕雲端產業與地圖智慧逾十數年，提供企業最專業的 **Google Maps Platform** 地理圖資解決方案（Location Analytics）、路線最佳化計算，以及 Navigation SDK 在企業端 App 的深度整合技術指導。

* **官方網站**: [https://masterconcept.ai/zh-hant/](https://masterconcept.ai/zh-hant/)
* **Google Maps Platform 解決方案**: [探索更多企業地圖應用](https://masterconcept.ai/zh-hant/partners/google-cloud/google-maps-platform/)
* **聯絡我們**: 如需進一步的 API 整合、商用圖資技術支援或客製化雲端轉型方案，歡迎透過官網與我們的技術專家聯繫。

## 📜 授權條款 (License)

本專案採用 **Apache License 2.0** 條款開源釋出。您可以自由複製、修改與商用本專案之程式碼，惟須保留原始之版權與作者聲明。詳情請參閱專案根目錄下的 `LICENSE` 檔案。


> *© 2026 Master Concept. All Rights Reserved. Google Maps Platform and Navigation SDK are trademarks of Google LLC.*

