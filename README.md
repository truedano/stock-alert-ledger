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

1. **估值雷達：台美前十大權值股雙軌估值、互動式基準切換與自選搜尋估值（v1.12.0+, v1.13.0+, v1.14.0+）**
   - **台美前十大權值股自動校正**：即時依市值排序抓取台股與美股前十大權值股（自動排除雙重股別重複佔位與美股掛牌台股 ADR），台股自動對齊證交所官方中文名稱與 PE/PB，並拉取過去 5 年季度 PE / PB 序列自動定錨 P20（便宜）、P50（合理）、P80（昂貴）倍數區間。
   - **雙軌估值與金融股 P/B 保護**：一般股預設採「預估 PE × 歷史分位」並計算隱含獲利成長率；金融股與波克夏 (`BRK-B`) 自動切換為「每股淨值 × 歷史 P/B 分位」為主導模型。
   - **互動式估值基準切換（v1.13.0+）**：點擊單張股票卡片四宮格中的 **「預估 PE」**、**「過去 PE」** 或 **「淨值比 P/B」**，即可即時切換評估基準，同步更新該卡的位階標籤、折溢價幅度、估值河流圖與三欄價格，並可點擊狀態標籤一鍵重置回系統預設主導基準。
   - **自選與搜尋任意個股估值（v1.14.0+）**：新增 **「⭐ 自選與搜尋」** 分頁，自動帶入自選監控清單非指數標的，並支援即時搜尋任一台股（含上市 `.TW`、上櫃 `.TWO`）或美股代號與中文名稱加入估值；自動推斷 `TWD` / `USD` 幣別、補齊證交所最新基本面，內建單檔 5 年歷史分位快取（6 小時）加速更新，並針對缺乏有效盈餘／淨值數據的虧損股或 ETF 顯示警語提示。
   - **一鍵設定目標價監控**：底部「設便宜價 / 設合理價」按鈕會自動依當前選取的評估基準帶入圓整目標價，一鍵加入監控提醒。

2. **三大總經籌碼指標細項面板與歷史追蹤（v1.12.0+）**
   - 深度升級美股 CNN 恐懼與貪婪指數、台股全市場融資維持率與期交所台指期法人／散戶籌碼看板，提供展開細項面板與歷史追蹤走勢檢視。

3. **期交所台指期盤後籌碼抓取升級、智慧快取與台股配色顯示（v1.11.0+）**
   - **期交所官網盤後 CSV 優先與 OpenAPI 雙重備援**：改為優先抓取台灣期貨交易所官網 MS950/Big5 盤後 CSV（含小台/微台全市場未平倉與三大法人未平倉），精準計算外資大台未平倉日增減口數；若最新交易日盤後法人資料尚未公布則自動回退前一交易日以對齊未平倉分母，官網異常時自動降級回 TAIFEX OpenAPI。
   - **30 分鐘快取與獨立響應式更新**：內建 30 分鐘記憶體快取與手動強制刷新支援，並透過 `StateFlow` 響應式資料流與融資維持率條件解耦，確保進入 App、手動刷新及定期更新流程皆能獨立同步籌碼資料且避免重複請求。
   - **台股紅多綠空視覺升級與盤後標示**：外資淨未平倉與小台/微台散戶多空比改採台股「紅多綠空」獨立著色，介面同步顯示外資未平倉日增減口數、資料日盤後標示與約 15:15 盤後公布時機說明，籌碼解讀更直覺。

2. **台指期大台法人留倉與散戶多空比籌碼看板、全螢幕警報中心與自選分類標籤（v1.10.0+）**
   - **期交所台指期法人未平倉與散戶多空比指標**：即時爬取台灣期貨交易所 Open API（三大法人期貨未平倉部位與每日市場行情），精準計算外資淨未平倉（大台 OI）以及小台/微台散戶淨留倉與「散戶多空比」逆向籌碼指標；並於宏觀情緒看板中提供摺疊子卡與指標判讀指引，快速判別外資動向與散戶軋空/殺多氛圍。
   - **全螢幕警報歷史中心 (Alert History Center)**：將歷史紀錄升級為獨立全螢幕頁面（整合頂部 TopAppBar、返回導航與滑動列表），支援標的 FilterChip 即時過濾、清空歷史記錄二次確認對話框，大幅提升長列表閱讀與管理體驗。
   - **自選監控清單類別與狀態快速篩選列**：自選股主頁面頂部新增分類標籤（全部、台股、美股、指數）與狀態標籤（已觸發、監控中、已停用），支援即時橫向滑動與快速聚焦篩選目標標的。
   - **桌面小工具點擊直達與自動買入導流**：點擊桌面微型監控 Widget 項目時直接定位跳轉至 App 內對應監控規則卡片並高亮藍框視覺引導；若點擊之標的已處於觸發狀態，自動彈出快速記帳買入對話框，實現桌面盯盤至快速下單記帳無縫串接。

2. **台美股代號/名稱即時搜尋與指數交易標的快速對照（v1.9.0+）**
   - **關鍵字即時搜尋與代號自動補齊**：新增監控規則或買入記帳時，輸入代號或中文名稱即刻自 TWSE/TPEx 清單與 Yahoo Finance 檢索匹配標的，支援點擊一鍵帶入代號、名稱與當前股價，告別手動背代號與輸入錯誤。
   - **大盤指數觸發替代標的映射**：收到大盤指數（如加權指數 `^TWII`、標普 500 `^GSPC` 等）警報訊號時，記帳對話框自動推薦對應可交易 ETF 標的（如 0050、SPY、VOO）或整合現有庫存持股，並支援一鍵重抓即時價格與自訂替代標的。
   - **通知列快速動作捷徑**：股價警報推播直接提供「快速記帳」與「暫停監控」（測試通知提供「關閉通知」）快捷按鈕，無須切換 App 即可在通知列直接處理警報與暫停特定監控規則。
   - **網路請求指數退避重試 (Exponential Backoff)**：強化外部行情與證交所融資爬蟲的容錯機制，遭遇伺服器暫時性異常時自動重試，確保報價不中斷。

2. **自選股 K 線技術圖表與融資維持率副圖（v1.8.0+）**
   - **K 線展開與買點可視化**：自選股卡片支援一鍵展開高精細互動式 K 線圖，直觀查看歷史走勢，並於主圖直接繪製警報設定之買點防線（如 MA 均線、波段回撤價位或目標價位）；支援 K 線歷史快取持久化與副圖序列計算最佳化（v1.12.1+）。
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
   - **盤中高頻前景常駐監控與自動節能（v1.2.0+, v1.3.0+, v1.12.1+）**：支援啟動專屬 Foreground Service，自選 15 秒、30 秒、1 分鐘或 5 分鐘即時輪詢股價與比對條件；結合台美股開休市智慧感知（含台股指數開盤時段判斷），休市期間自動降頻節能，並在非開盤時段智慧跳過 WorkManager 背景輪詢，大幅降低電量與網路消耗；強化前景服務與桌面小工具生命週期保護，App 關閉時自動停止釋放資源。
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
   - **匯入還原與安全保護**：支援選取備份檔完整覆蓋還原，內建備份格式檢核、資料庫事務保護與主鍵安全重配，並收斂 `FileProvider` 授權路徑確保分享檔案安全性（v1.12.0+, v1.12.1+）。

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

請前往 [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest) 下載最新版本的 `stockAlertLedger-vX.Y.Z.apk` 進行安裝，或於 App 內點擊檢查更新自動下載安裝。

---

<br>

## English

**Stock Alert & Ledger** is a native Android application built for Taiwan (TW) and US stock investors. It integrates **real-time technical indicator monitoring**, **market sentiment & macro indicators**, **high-priority notifications**, **one-tap quick buy ledger tracking**, and **complete offline backup & restore**. No accounts or subscriptions needed; all your portfolio data and rules are stored strictly on your local device.

### ✨ Key Features

1. **Valuation Radar: Top 10 TW/US Large-Cap Dual-Track Valuation, Interactive Basis Switching & Custom Stock Search (v1.12.0+, v1.13.0+, v1.14.0+)**
   - **Auto-Calibrated Top 10 Market-Cap Leaders**: Dynamically fetches top 10 Taiwan and US equities by market cap (deduplicating dual share classes and Taiwan ADRs), aligns TWSE official Chinese names, and pulls 5-year quarterly PE/PB series to auto-anchor P20 (Cheap), P50 (Fair), and P80 (Expensive) bands.
   - **Dual-Track PE / P/B Valuation**: Defaults to Forward PE × historical percentiles with implied EPS growth for standard equities, while automatically switching financial stocks and `BRK-B` to P/B-priority valuation.
   - **Interactive Valuation Basis Switching (v1.13.0+)**: Tap **Forward PE**, **Trailing PE**, or **P/B** directly on any stock card to dynamically switch the evaluation basis—updating the tier badge, margin of safety, valuation river bar, and three-column target prices in place, with one-tap reset to the default primary method.
   - **Custom Watchlist & Arbitrary Stock Search Valuation (v1.14.0+)**: Adds a **"Custom & Search"** tab that automatically merges non-index watchlist holdings and supports real-time search for any Taiwan (`.TW` / `.TWO`) or US ticker/name to compute 5-year historical PE/PB bands on demand. Automatically infers `TWD`/`USD` currency, enriches TWSE fundamentals, caches per-stock historical bands (6h TTL), and flags loss-making stocks or ETFs lacking valid fundamentals.
   - **One-Tap Price Alert Creation**: "Set Cheap Price" and "Set Fair Price" buttons automatically use the currently selected valuation basis to create target price alerts.

2. **Macro & Chip Indicator Detail Panels & Historical Tracking (v1.12.0+)**
   - Upgraded detail drill-down panels and historical trend tracking for the US CNN Fear & Greed Index, TWSE market-wide margin maintenance rate, and TAIFEX futures institutional/retail positioning.

3. **Upgraded TAIFEX Post-Market Futures Chip Scraper, Smart Caching & Taiwan Color Styling (v1.11.0+)**
   - **TAIFEX Official CSV Priority with OpenAPI Fallback**: Prioritizes Taiwan Futures Exchange (TAIFEX) official MS950/Big5 post-market CSV reports (Mini-TX/Micro-TX market total OI and institutional OI) to calculate daily changes in Foreign Institutional TX net OI. Automatically falls back one trading day if today's institutional report is not yet published so the OI denominator stays aligned, and degrades gracefully to TAIFEX OpenAPI if official CSV endpoints fail.
   - **30-Minute Memory Cache & Independent Reactive Updates**: Introduces a 30-minute in-memory cache with force-refresh support, exposed via reactive `StateFlow` and decoupled from margin maintenance conditions so futures chip data updates independently on app launch, manual refresh, and periodic real-data syncs without redundant requests.
   - **Taiwan Red-Bullish / Green-Bearish Visuals & Settlement Timestamp**: Applies Taiwan market color conventions (red bullish / green bearish) separately to Foreign net OI and Mini-TX/Micro-TX retail long/short ratios, alongside daily Foreign OI delta, settlement date labels, and post-market (~15:15) publication notes.

2. **Taiwan Index Futures Institutional OI & Retail Sentiment Gauges, Full-Screen Alert History & Watchlist Filter Tabs (v1.10.0+)**
   - **TAIFEX Institutional Futures OI & Retail Long/Short Ratio**: Fetches real-time open interest and daily market reports from Taiwan Futures Exchange (TAIFEX) Open API, calculating Foreign Institutional net open contracts (TX OI) and Mini-TX / Micro-TX retail net positions & "Retail Long/Short Ratio" sentiment indicators; integrated into the macro sentiment banner with interactive explainers.
   - **Full-Screen Alert History Center**: Upgrades alert history from a basic dialog into a dedicated full-screen page featuring TopAppBar navigation, symbol filter chips, and clear confirmation dialog for fluid log management.
   - **Watchlist Category & Status Quick-Filter Chips**: Adds quick filter chips to the watchlist (All, Taiwan, US, Indices) along with rule status (Triggered, Monitoring, Disabled) for rapid target filtering and clutter-free organization.
   - **Widget Direct Rule Navigation & Auto Buy Trigger**: Tapping any rule row on the home screen AppWidget directly opens the App, highlights the target rule with visual focus, and automatically launches the Quick-Buy dialog if the rule is already triggered.

2. **Real-time Ticker Search, Index Proxy Target Mapping & Notification Quick Actions (v1.9.0+)**
   - **Real-Time Ticker & Symbol Autocomplete**: Search Taiwan and US stocks by symbol or company name with instant auto-suggestions from TWSE/TPEx and Yahoo Finance; pre-fills ticker, name, and live price with a single tap.
   - **Market Index Signal Proxy Target Mapping**: When market index alerts (e.g., `^TWII`, `^GSPC`) are triggered, the Quick-Buy ledger automatically recommends tradable proxy ETFs (e.g., 0050, SPY, VOO) or existing portfolio holdings, with one-tap live price lookup.
   - **Notification Action Buttons**: Push notifications include direct action shortcuts ("Quick Buy" and "Pause Alert"), allowing users to act on or pause alerts straight from the system notification shade.
   - **Network Request Resilience**: Integrated exponential backoff retry mechanisms for Yahoo Finance and TWSE market data fetching to ensure uninterrupted quotes.

2. **Interactive Candlestick Charts & Margin Maintenance Rate Indicators (v1.8.0+)**
   - **Expandable K-Line Chart & Strategy Visualizer**: One-tap expansion of high-resolution candlestick charts on watchlist cards with visible alert target price lines directly overlaid on the chart (MA support, peak drawdown targets, price thresholds), backed by persistent K-line caching and optimized sub-chart series calculation (v1.12.1+).
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
   - **Intraday High-Frequency Foreground Service & Auto Power-Saving (v1.2.0+, v1.3.0+, v1.12.1+)**: Dedicated Foreground Service supporting customizable polling intervals (15s, 30s, 1m, 5m). Incorporates market session awareness (including Taiwan index trading hours) to automatically throttle down polling during non-trading hours and skip WorkManager cycles when markets are closed, significantly reducing battery and data consumption; hardens Foreground Service and AppWidget lifecycle protection so resources are cleanly released when the app task is dismissed.
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
   - **Import / Restore & Security Hardening**: One-click restore with backup validation, atomic database transactions, primary key conflict prevention, and tightened `FileProvider` sharing paths (v1.12.0+, v1.12.1+).

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

Download the latest signed release APK (`stockAlertLedger-vX.Y.Z.apk`) from [GitHub Releases](https://github.com/truedano/stock-alert-ledger/releases/latest), or check for updates directly inside the app.

---

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
