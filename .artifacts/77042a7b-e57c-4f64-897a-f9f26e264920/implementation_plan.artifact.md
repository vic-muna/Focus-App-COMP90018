# Focus App 獎勵機制基礎架構實作計畫

本計畫旨在建立 Focus App 的核心獎勵邏輯與資料持久化層。我們將使用現代 Android 開發最佳實踐（DataStore + ViewModel + Compose）。

## 使用者評論與回饋要求

> [!IMPORTANT]
> **關於影片檔 (Animation Video)**:
> 請將您的影片檔案放入專案的以下路徑：
> `app/src/main/res/raw/`
> 如果 `raw` 資料夾不存在，請手動建立它。檔名建議使用小寫英文字母與底線（例如：`focus_progress_bg.mp4`）。在 Android 中，我們通常使用 `ExoPlayer` (Media3) 來播放 `res/raw` 中的影片作為背景。

## 待解決問題
- [ ] 您預計的第一個里程碑是「累計 10 小時」嗎？是否需要更小的初始里程碑（如 1 小時）以便測試？
- [ ] 獎勵解鎖後，是否需要彈出視窗提示使用者？

## 擬議變更

---

### 1. 依賴項配置 (Dependencies)

我們需要添加 `DataStore` 用於儲存專注時間，以及 `ViewModel` 用於管理狀態。

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/app2/gradle/libs.versions.toml)
添加 DataStore 和 ViewModel 的依賴項定義。

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/user/AndroidStudioProjects/app2/app/build.gradle.kts)
應用上述依賴項。

---

### 2. 資料模型與持久化 (Data Layer)

#### [NEW] `RewardItem.kt`
定義獎勵項目的資料結構，包含 `id`, `name`, `type` (Sound/Theme), `threshold` (所需小時數)。

#### [NEW] `FocusPreferences.kt`
使用 `DataStore` 封裝資料讀寫，儲存：
- `total_focus_seconds`: 累計專注秒數。
- `unlocked_reward_ids`: 已解鎖的獎勵 ID 列表。

---

### 3. 業務邏輯層 (Logic Layer)

#### [NEW] `FocusViewModel.kt`
- 負責倒數計時邏輯。
- 當專注完成時，呼叫 `FocusPreferences` 更新總時間。
- 自動計算哪些獎勵應被解鎖。

---

### 4. 資源準備

#### [NEW] `res/raw` 資料夾
用於存放您的透明背景 Progressing 影片檔。

## 驗證計畫

### 自動化測試
- 撰寫單元測試驗證 `total_focus_seconds` 增加時，獎勵解鎖邏輯是否正確觸發。

### 手動驗證
- 模擬完成一次專注任務，確認累計時間有正確儲存。
- 手動調整累計時間至里程碑（如 10 小時），確認解鎖狀態變更。
