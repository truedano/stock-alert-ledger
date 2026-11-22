# 股價監控與記帳助理 (Stock Alert & Ledger)

[![Android CI](https://github.com/truedano/stock-alert-ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/truedano/stock-alert-ledger/actions/workflows/ci.yml)
[![Build & Release APK](https://github.com/truedano/stock-alert-ledger/actions/workflows/release.yml/badge.svg)](https://github.com/truedano/stock-alert-ledger/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/truedano/stock-alert-ledger)](https://github.com/truedano/stock-alert-ledger/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

[English](#english) | [繁體中文](#繁體中文)

---

## 繁體中文

**股價監控與記帳助理** 是一款專為台股（TW）與美股（US）投資人打造的 Android 本地端工具。結合了**即時技術指標條件監控**、**市場宏觀情緒看板**、**高優先級通知提醒**、**一鍵快速買入成交記帳**與**完整的離線備份/還原**功能。無須註冊帳號、無第三方伺服器蒐集資料，所有資產資料與規則均妥善儲存在手機本地。

### ✨ 核心功能

1. **自選股 K 線技術圖表與融資維持率副圖（v1.8.0+）**
   - **K 線展開與買點可視化**：自選股卡片支援一鍵展開高精細互動式 K 線圖，直觀查看歷史走勢，並於主圖直接繪製警報設定之買點防線（如 MA 均線、波段回撤價位或目標價位）。
   - **推估融資維持率副圖與 130% 追繳防線**：台股標的自動呈現歷史推估融資維持率副圖，標示 130% 融資斷頭警戒紅線與即時維持率標籤，並於主圖同步繪製追繳臨界價位防線，精準避開或把握籌碼斷頭賣壓。
   - **多週期靈活切換**：內建快捷週期切換晶片（1 個月、3 個月、6 個月、1 年），流暢縮放與檢視中長期位階。
   - **卡片排版最佳化**：獨立「預定買入金額與預估股數」專屬資訊橫幅，解決手機窄螢幕操作按鈕與金額擠壓換行問題。

2. **桌面微型監控 Widget (AppWidget) 與即時連動（v1.7.0+）**
   - **桌面即時盯盤小工具**：無須打開 App 即可在手機桌面隨時查看自選標的最新現價、漲跌幅、觸發警報狀態與主要監控條件。
   - **一鍵手動與自動同步整理**：提供小工具快速更新按鈕，並與 App 內部資料庫與輪詢狀態即時雙向連動。

2. **複合多條件警報 (CompositeCondition) 與進階二次確認過濾器（v1.7.0+）**
   - **複合多條件規則 (AND 組合)**：支援多項技術指標條件同時成立時才觸發（例如：「月線 20MA 觸及」且「波段高點回撤 -15%」或「RSI < 30」超賣同時符合），大幅提高右側交易策略勝率。
   - **進階二次確認過濾器 (Confirmation Filters)**：支援成交量爆量確認（當日成交量突破 20 日均量倍數門檻，如 1.5x / 2x）與跌破/突破確認日數（連續 N 日確認有效突破/跌破，防止假突破/假跌破騙線）。

3. **警報歷史紀錄日誌與快捷過濾 (Alert History Log)（v1.7.0+）**
   - **完整觸發歷史日誌**：記錄每一次警報觸發的詳細時間、標的代號、觸發時現價與符合的策略條件內容。
   - **快速標的篩選晶片 (FilterChip)**：在警報歷史對話框中提供標的快捷晶片，一鍵單獨檢視特定自選股或指數的歷史觸發歷程，並支援一鍵清除過期紀錄。

4. **大盤指數支援與自選常駐快捷記帳（v1.7.0+）**
   - **大盤與重要指數監控**：支援台股加權指數 (`^TWII`)、櫃買指數 (`^TWOII`)、道瓊工業指數 (`^DJI`)、那斯達克 (`^IXIC`)、標普 500 (`^GSPC`)、費城半導體 (`^SOX`)，並於新增自選股時提供熱門大盤快捷標籤。
   - **常駐快速買入按鈕**：自選股卡片常駐「記帳」操作捷徑，無須等待警報觸發即可隨時點擊快速記帳。

5. **台美雙市場報價與全方位策略條件監控（v1.4.0+, v1.5.0+, v1.6.0+）**
   - 支援台股代號（如 `2330.TW`、`0050.TW`）與美股代號（如 `AAPL`、`NVDA`）。
   - **台美市場宏觀指標與市場氛圍看板（v1.6.0+）**：
     - **美股 CNN 恐懼與貪婪指數 (Fear & Greed Index)**：即時連線 CNN 抓取多維度市場情緒指數（0-100分），劃分「極度恐慌、恐慌、中立、貪婪、極度貪婪」狀態並提供歷史走勢圖與情緒解讀。
     - **台股證交所全市場融資維持率**：即時爬取 TWSE 整戶擔保維持率，支援設定恐慌維持率門檻（如 150%）搭配反彈點數（如反彈 20 點）觸發黃金抄底警示，並提供出場安全停泊警戒模式。
     - **個股信用交易與推估維持率看板（v1.6.0+）**：台股個股即時顯示最新融資融券增減張數、餘額，以及個股推估融資維持率與警戒標籤。
     - **指標說明彈窗**：提供宏觀溫度計卡片指標說明彈窗，解析數值意義與實務投資操作參考建議。
   - **絕對目標價警示（v1.5.0+）**：支援設定精準目標價買進或停利提醒，具備「跌破/觸及」或「突破/觸及」方向判定、容許偏差範圍，以及觸發後自動停用規則機制；提供現價快捷百分比換算晶片輔助輸入。
   - **多元均線監控**：短中長期均線（週線 5MA、雙週線 10MA、月線 20MA、季線 60MA、半年線 120MA、年線 240MA，以及長週期 3年線 720MA、5年線 1200MA、10年線 2400MA，支援最高 3000 天自訂週期）。
   - **波段高點回撤抄底（Peak Drawdown）**：支援自訂回溯天數（如 60 日、120 日）與自波段最高價下跌百分比（如回撤 -10%、-20%），精準捕捉右側抄底或逢低佈局買點。
   - **RSI 技術指標警示**：計算自訂週期 RSI（預設 14 日），支援「跌破超賣區 (RSI < 30)」恐慌撿便宜或「突破超買區 (RSI > 70)」獲利調節提醒。
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

7. **應用程式自動更新與 CI/CD 自動化**
   - 整合 GitHub Releases API，可於 App 內一鍵檢查並下載安裝最新版本 APK。
   - 整合 GitHub Actions 自動化 CI/CD：每次推送自動執行單元測試驗證，發布 release tag 自動完成 APK 編譯、簽章與發布。

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
- **CI/CD**：GitHub Actions（自動化測試與 GitHub Release 簽章發布）

---

### 📲 安裝與下載

請前往 [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest) 下載最新版本的 `app-release.apk` 進行安裝。

---

<br>

## English

**Stock Alert & Ledger** is a native Android application built for Taiwan (TW) and US stock investors. It integrates **real-time technical indicator monitoring**, **market sentiment & macro indicators**, **high-priority notifications**, **one-tap quick buy ledger tracking**, and **complete offline backup & restore**. No accounts or subscriptions needed; all your portfolio data and rules are stored strictly on your local device.

### ✨ Key Features

1. **Interactive Candlestick Charts & Margin Maintenance Rate Indicators (v1.8.0+)**
   - **Expandable K-Line Chart & Strategy Visualizer**: One-tap expansion of high-resolution candlestick charts on watchlist cards with visible alert target price lines directly overlaid on the chart (MA support, peak drawdown targets, price thresholds).
   - **Estimated Margin Maintenance Sub-Chart & 130% Liquidation Line**: Real-time margin keep rate sub-chart for Taiwan equities with a 130% margin-call red boundary and current keep rate badge, mapping margin risk directly onto the main price chart.
   - **Flexible Timeframe Selector**: Quick time-range filter chips (1M / 3M / 6M / 1Y) for seamless multi-period trend and cycle analysis.
   - **Refined Card Layout**: Dedicated clean banner for planned purchase budget and estimated shares, preventing button clutter and line truncation on narrow mobile screens.

2. **AppWidget Micro-Monitor & Live Sync (v1.7.0+)**
   - **Home Screen AppWidget**: Glance at real-time stock prices, intraday changes, alert trigger status, and key monitor rules directly on your home screen without opening the app.
   - **Manual & Automated Sync**: Integrated widget refresh controls with instant bidirectional synchronization to local database state.

2. **Composite Multi-Condition Alerts & Secondary Confirmation Filters (v1.7.0+)**
   - **Composite Multi-Condition Rules (AND Logic)**: Triggers only when multiple conditions are met simultaneously (e.g., "20MA Touch" AND "Peak Drawdown -15%" or "RSI < 30"), boosting strategy precision for bottom-fishing.
   - **Secondary Confirmation Filters**: Filters false breakouts and whipsaws with volume spike confirmation (e.g., 1.5x / 2.0x 20-day average volume) and persistent breakout/breakdown day counts.

3. **Alert Trigger History Log & Target Filter Chips (v1.7.0+)**
   - **Complete Trigger History**: Automatically records timestamp, symbol, market price at trigger, and satisfied rule conditions.
   - **Quick FilterChips**: Filter history records by individual stock or index ticker with one tap; supports clearing obsolete logs.

4. **Market Index Tracking & Persistent Ledger Actions (v1.7.0+)**
   - **Major Market Benchmark Indices**: Full support for TWSE (`^TWII`), TPEx (`^TWOII`), Dow Jones (`^DJI`), Nasdaq (`^IXIC`), S&P 500 (`^GSPC`), and Philadelphia Semiconductor (`^SOX`) with quick-select chips.
   - **Persistent Quick Buy Action**: Added permanent "Record Buy" button on stock cards for immediate bookkeeping at any time.

5. **Dual Market & Multi-Strategy Alerting (v1.4.0+, v1.5.0+, v1.6.0+)**
   - Supports Taiwan stocks (`2330.TW`, `0050.TW`) and US equities (`AAPL`, `NVDA`, `TSLA`).
   - **Macro Market Sentiment Dashboard (v1.6.0+)**:
     - **US CNN Fear & Greed Index**: Real-time integration tracking sentiment (0-100), classified across Extreme Fear, Fear, Neutral, Greed, and Extreme Greed with contextual descriptions.
     - **TWSE Overall Margin Maintenance Rate**: Live tracking of Taiwan Stock Exchange credit margin keep rates, triggering buy signals when rebounding from panic levels or alerting safe exits.
     - **Individual Stock Margin Data & Estimated Maintenance Rate (v1.6.0+)**: Live individual stock margin balance, daily changes, and estimated maintenance health status badges.
     - **Indicator Guide Dialog**: In-depth explanations for market gauges with actionable investment insights.
   - **Absolute Price Target Alerts (v1.5.0+)**: Precision price target monitoring with "drop below or touch" or "break above or touch" directions, tolerance margins, and auto-disable upon trigger; includes quick percentage offset assist chips based on current price.
   - **Moving Average Monitoring**: short, medium, and long-term MAs (5MA, 10MA, 20MA, 60MA, 120MA, 240MA, plus extended 3-year 720MA, 5-year 1200MA, and 10-year 2400MA; custom cycles up to 3000 days).
   - **Peak Drawdown Bottom-Fishing**: Tracks drawdown percentage (e.g., -10%, -20%) from recent swing highs within customizable lookback days to catch dip-buying opportunities.
   - **RSI Technical Indicator**: Customizable RSI period (default 14 days), alerting on oversold zones (RSI < 30) for bargain hunting or overbought levels (RSI > 70) for profit taking.
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

7. **In-App Auto Updater & Automated CI/CD**
   - Seamlessly checks GitHub Releases for new APK versions and facilitates in-app updates.
   - Automated CI/CD pipeline via GitHub Actions for continuous unit test verification and tagged APK release builds.

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
- **CI/CD**: GitHub Actions (Automated unit tests & signed release publishing)

---

### 📲 Download & Installation

Download the latest signed release APK from [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest).

---

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
