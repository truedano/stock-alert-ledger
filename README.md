# 股價監控與記帳助理 (Stock Alert & Ledger)

[![Android CI & Release Build](https://github.com/truedano/stock-alert-ledger/actions/workflows/android-release.yml/badge.svg)](https://github.com/truedano/stock-alert-ledger/actions)
[![Latest Release](https://img.shields.io/github/v/release/truedano/stock-alert-ledger)](https://github.com/truedano/stock-alert-ledger/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[English](#english) | [繁體中文](#繁體中文)

---

## 繁體中文

**股價監控與記帳助理** 是一款專為台股（TW）與美股（US）投資人打造的 Android 本地端工具。結合了**即時技術指標條件監控**、**高優先級通知提醒**、**一鍵快速買入成交記帳**與**完整的離線備份/還原**功能。無須註冊帳號、無第三方伺服器蒐集資料，所有資產資料與規則均妥善儲存在手機本地。

### ✨ 核心功能

1. **台美雙市場報價與技術指標監控**
   - 支援台股代號（如 `2330.TW`、`0050.TW`）與美股代號（如 `AAPL`、`NVDA`）。
   - 提供**均線監控**（短中長期均線：週線 5MA、雙週線 10MA、月線 20MA、季線 60MA、半年線 120MA、年線 240MA，以及長週期 3年線 720MA、5年線 1200MA、10年線 2400MA，支援最高 3000 天自訂週期）。
   - 觸發模式支援「跌破或觸及」以及「接近指定百分比範圍內（例如 ±1%）」。

2. **背景排程與盤中高頻監控**
   - 透過 Android WorkManager 定期在背景檢查股價。
   - **盤中高頻前景常駐監控與自動節能（v1.2.0+, v1.3.0+）**：支援啟動專屬 Foreground Service，自選 15 秒、30 秒、1 分鐘或 5 分鐘即時輪詢股價與比對條件；結合台美股開休市智慧感知，休市期間自動降頻節能，並在非開盤時段智慧跳過 WorkManager 背景輪詢，大幅降低電量與網路消耗；App 關閉時自動停止釋放資源。
   - 達到條件時即刻發出高優先級通知，避免錯過買入佈局時機。
   - 智慧冷卻機制：同一條件在當日觸發後自動標註，避免重複騷擾。

3. **一鍵快速記帳 (Quick Buy)**
   - 點擊通知或自選卡片，自動帶入當前股價、預計買進金額與計算後的預估股數。
   - 支援零股（小數點股數）與整股運算。
   - 自動將觸發條件（例如「月線 20MA」）標註於買入紀錄中，方便回溯交易策略。

4. **投資組合與成本分析**
   - 依股票、觸發標籤等多維度過濾成交記錄。
   - 即時計算持股均價、總投入成本、當前市值與帳面未實現損益。

5. **沙盒模擬市場 (Simulator)**
   - 提供模擬環境，可自由拖曳調整模擬股價與觸發測試通知，方便在非交易時段驗證警報條件與介面。

6. **資料管理與安全備份（v1.1.0+）**
   - **儲存備份**：一鍵將所有自選監控、成交記帳與設定導出為標準 JSON 檔至本地空間。
   - **傳送分享**：一鍵上傳備份至 **Google 雲端硬碟 (Google Drive)**、Gmail、LINE 等，換機備份無痛搞定。
   - **匯入還原**：支援選取備份檔完整覆蓋還原，內建資料庫事務保護與主鍵安全重配。

7. **應用程式自動更新**
   - 整合 GitHub Releases API，可於 App 內一鍵檢查並下載安裝最新版本 APK。

---

### 🛠 技術棧與架構

- **UI 框架**：Jetpack Compose (Material Design 3)
- **架構模式**：Clean Architecture + MVVM + Unidirectional Data Flow (MVI 風格)
- **非同步處理**：Kotlin Coroutines & StateFlow
- **本地持久化**：
  - Room (SQLite) 資料庫，支援 TypeConverters 與 JSON 序列化
  - Jetpack DataStore (Preferences)
- **背景任務**：Android Jetpack WorkManager
- **資料序列化**：Kotlinx Serialization & Moshi
- **網路通訊**：OkHttp3 + Retrofit2
- **依賴注入**：純 Kotlin AppContainer 架構（輕量化無反射）

---

### 📲 安裝與下載

請前往 [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest) 下載最新版本的 `app-release.apk` 進行安裝。

---

<br>

## English

**Stock Alert & Ledger** is a native Android application built for Taiwan (TW) and US stock investors. It integrates **real-time technical indicator monitoring**, **high-priority notifications**, **one-tap quick buy ledger tracking**, and **complete offline backup & restore**. No accounts or subscriptions needed; all your portfolio data and rules are stored strictly on your local device.

### ✨ Key Features

1. **Dual Market & Indicator Alerting**
   - Supports Taiwan stocks (`2330.TW`, `0050.TW`) and US equities (`AAPL`, `NVDA`, `TSLA`).
   - Built-in **Moving Average Monitoring** (short, medium, and long-term: 5MA, 10MA, 20MA, 60MA, 120MA, 240MA, plus extended 3-year 720MA, 5-year 1200MA, and 10-year 2400MA; custom cycles up to 3000 days).
   - Alert triggers: "Drop below or touch" and "Within ±X% tolerance".

2. **Background Monitoring & Intraday High-Frequency Alerts**
   - Powered by Android WorkManager for periodic background market checking.
   - **Intraday High-Frequency Foreground Service & Auto Power-Saving (v1.2.0+, v1.3.0+)**: Dedicated Foreground Service supporting customizable polling intervals (15s, 30s, 1m, 5m). Incorporates market session awareness to automatically throttle down polling during non-trading hours and skip WorkManager cycles when markets are closed, significantly reducing battery and data consumption; automatically terminates and cleans up when the app task is dismissed.
   - High-priority push notifications ensure you never miss buying opportunities.
   - Smart daily cooldown avoids duplicate alert spam on the same day.

3. **One-Tap Quick Buy & Transaction Ledger**
   - Tap notifications or stock cards to launch the Quick-Buy dialog prefilled with live price, planned budget, and estimated shares.
   - Supports both whole shares and fractional shares.
   - Automatically tags purchase records with the alert reason (e.g., "20MA Touch") for post-trade review.

4. **Portfolio & Cost Ledger**
   - Filter transactions by symbol or strategy trigger tags.
   - Calculates average cost, total invested capital, current market value, and unrealized P&L in real time.

5. **Sandbox Market Simulator**
   - Interactive slider to test price alerts and simulate push notifications during non-trading hours.

6. **Data Management & Backup / Restore (v1.1.0+)**
   - **Export / Save**: Export all rules, ledger records, and preferences into a clean, structured JSON file.
   - **Share to Cloud**: Directly upload backups to **Google Drive**, send via Gmail, or share to other devices.
   - **Import / Restore**: One-click restore with atomic database transactions and primary key conflict prevention.

7. **In-App Auto Updater**
   - Seamlessly checks GitHub Releases for new APK versions and facilitates in-app updates.

---

### 🛠 Tech Stack & Architecture

- **UI**: Jetpack Compose (Material Design 3, Dark Financial Theme)
- **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow
- **Concurrency**: Kotlin Coroutines & StateFlow
- **Storage**:
  - Android Room (SQLite) with TypeConverters and schema versioning
  - Jetpack DataStore (Preferences)
- **Background Scheduler**: Android Jetpack WorkManager
- **Serialization**: Kotlinx Serialization & Moshi
- **Networking**: OkHttp3 + Retrofit2
- **DI**: Lightweight manual AppContainer DI

---

### 📲 Download & Installation

Download the latest signed release APK from [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest).

---

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
