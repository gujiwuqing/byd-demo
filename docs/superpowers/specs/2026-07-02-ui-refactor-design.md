# 迪UI 完整性重构设计

- 日期:2026-07-02
- 范围:全部页面(UI 完整性重构)
- 痛点:① 缺图片(全靠 emoji + XML shape,无真实图标/插图);② 内容零碎(Status 页左侧 35% 塞 7 个迷你卡,胎压 2x2 网格脱离车辆空间语境)
- 架构方案:方案 A —— 延续 XML + 自定义 View,新增设计 token + 矢量资源包 + 可视化 View 组件,页面级重构。不引入 Compose,不新增依赖。

## 决策摘要

| 维度 | 决策 |
|---|---|
| 视觉风格 | 深色玻璃增强(保持 `bg_card_glass` 质感,补光晕/氛围背景/环形图) |
| 胎压布局 | 四角外置(数字在车图四角,引线指向轮子,可读性优先) |
| 重构范围 | 全面重做所有页面,分阶段落地 |
| 图片补充 | 车辆图升级 + 背景/插画 + 数据可视化图;功能图标作为视觉统一配套项一并替换 emoji |
| 能源信息分工 | TopBar 独占(电量+纯电续航+油量+油量L+总续航+时间);Status 页不再重复能源,放大时钟/媒体/行程 |
| 空调控制 | 保留现有真实 API 绑定,仅 UI 图形化;删除 Controls 页下方车身控制区 |
| Settings | 只做分组卡片 + 图标,不动 `SettingsPage.java` 逻辑 |

## 1. 基础设施层

### 1.1 设计 token 收敛

- `values/colors.xml`:新增语义色 `surface_glass`、`surface_glass_highlight`、`accent_cool`(冷色,空调)、`accent_warn`(胎压/能耗告警)、`gauge_track`、`gauge_progress`。
- `values-night/colors.xml`:对应夜间色值(深色为主)。
- `values/dimens.xml`:统一间距阶梯 `spacing_token_4/8/12/16/24`;圆角 `radius_card_lg=16dp`、`radius_card_sm=12dp`。
- `values/styles.xml`:抽出复用样式 `Card.Glass`、`DataLabel.Primary`、`DataLabel.Secondary`。

### 1.2 矢量资源包(替换全部 emoji)

新建 `drawable-anydpi-v26/`(API 26+ 车机满足),`<vector>` 自绘,`app:tint` 绑主题色:

| emoji | 新矢量 | 用途 |
|---|---|---|
| 🏠 | `ic_tab_home` | 主页 tab |
| ❄ | `ic_ac`(雪花) | 空调 |
| ⊞ | `ic_apps`(圆点网格) | 应用 tab |
| ⚙ | `ic_settings`(齿轮) | 设置 tab |
| 🔄 | `ic_cycle`(循环箭头) | 内/外循环 |
| 🪟 | `ic_window` | 车窗 |
| 💺 | `ic_seat` | 座椅 |
| 🌀 | `ic_wind`(风线) | 风量 |
| 🔋 | `ic_battery` | 电量 |
| 📶 | `ic_signal` | 信号 |
| 🔵 | `ic_bluetooth` | 蓝牙 |
| ♫ | `ic_media` | 媒体 |
| 🗺 | `ic_map` | 地图 |
| 🎵 | `ic_music` | 音乐 |
| 📺 | `ic_video` | 视频 |
| 📞 | `ic_phone` | 电话 |
| ⊞(更多) | `ic_more`(三圆点) | 更多 |
| +/− | `ic_plus` / `ic_minus` | 加减 |

选中态 tint `accent`;日夜模式自动变色。

### 1.3 可视化 View 组件库

- **`VehicleDiagramView` 扩展**(已有 `setDoorStates`):新增 `setTireData(flP,flT,frP,frT,rlP,rlT,rrP,rrT)` 与 `setTireWarning(mask)`。四轮外侧绘制"压力+温度"文字(四角外置),异常轮红色 + 轮廓加粗。一张车图同时承载车门 + 胎压。
- **`GaugeView` 新增**:Canvas 环形进度,`setProgress(value,max)`、`setColorByRange()`(低值黄/红)。用于 TopBar 之外的能源可视化场景(如 Controls 能耗提示)及未来扩展;TopBar 用文字 + 小图标。
- **`ArcTempView`**:已有,复用到 Controls 页弧形温度。
- **背景氛围**:`bg_ambient_glass.xml`(径向渐变 + shape),根容器背景,深色玻璃质感来源。

## 2. Status 页重构

左侧 35% 驾驶核心 / 右侧 65% 媒体 + 行程。能源移至 TopBar。

```
┌──────────────┬──────────────────────────────────────┐
│  速度表       │            14:28                     │
│  P  R  N  D  │         07月02日 周三                │
│    38 km/h   │                                      │
│ 38° 12.4kW   │   [🗺][🎵][📺][📞][⊞]                │
├──────────────┤   (可放 MusicCardView 媒体卡)        │
│ 250 31° 252 31°                                  │
│   ╲       ╱  ├──────────────────────────────────────┤
│   ┌─────┐    │  行程 0.0km · 00:00 · 能耗 16.0度   │
│   │ 车身 │    │  总 73465km · HEV 18563km           │
│   └─────┘    │                                      │
│   ╱       ╲  │                                      │
│ 250 30° 250 33°                                  │
└──────────────┴──────────────────────────────────────┘
```

### 关键整合

- **胎压 → 并入车图**:删 `page_status.xml` 底部"胎压 2x2"整块;`StatusPage.java` 改调 `vehicleDiagram.setTireData(...)`。一张车图同时显示车门开闭 + 四轮胎压胎温 + 异常红警。
- **电量/油量/续航 → 移至 TopBar**(见第 3 节),Status 页右侧删除环形图与相关 TextView。
- **速度表保留**:左侧核心,PRND + 大字速度 + 功率/温度。可选加细环形底纹增强仪表感(P2 视情况)。
- **能耗/行程/里程 → 单卡**:合并原"能耗+智保""行程+里程"两张迷你卡为右侧底部"行程卡";移除 `tv_smart_charge`/`tv_recovery_mode` 等低频字段(不展示,数据层不动)。
- **pip 快捷 → 矢量图标**:地图/音乐/视频/电话/更多换 `ic_*.xml`,背景圆角玻璃卡。

### 数据流

不变。`BydVehicleManager` 2 秒轮询 → `VehicleStatus` → `StatusPage.update()`。`StatusPage` 增持 `VehicleDiagramView` 引用,update 时调 `setTireData/setDoorStates`。无新 API、无新权限。

## 3. TopBar 能源常驻栏

```
┌─────────────────────────────────────────────────────┐
│ 🔋89% 90km  ⛁37% 21L  总396km      迪UI   14:28  │
└─────────────────────────────────────────────────────┘
   电量+纯电续航  油量+油量L  总续航   标题  时间
```

- 左:电量(图标+百分比+纯电剩余 km,`evRange`)+ 油量(图标+百分比+油量 L)+ 总续航。跨所有 tab 常驻。
- 右:标题"迪UI" + 时间(`TextClock`)。
- `TopBar.java` 增加从 `VehicleStatus` 取能源数据并更新;`MainActivity` 在轮询回调里调 `topBar.update(status)`。
- 系统状态图标(信号/蓝牙)本期不接真实系统广播,仅作占位或移除(默认占位矢量图标)。

## 4. Controls 页图形化

```
┌─────────────────────────────────────────────────────┐
│  ❄ 空调控制                              [ AC OFF ] │
├──────────────┬──────────────────┬───────────────────┤
│   左区温度    │    中央风量       │    右区温度       │
│   ◠◠ 25°C   │     3 / 7         │   25°C ◠◝        │
│    −   +    │   ▮▮▮▯▯▯▯        │    −   +         │
├──────────────┴──────────────────┴───────────────────┤
│  出风模式                循环           自动/手动      │
│  [吹面][吹脚][除雾]    [内][外]      [自动][手动]    │
└─────────────────────────────────────────────────────┘
```

- **保留现有真实 API 绑定**:`acApi.toggle/setTemp/setWindLevel/setWindMode/setCycleMode/setControlMode` 调用不变,仅 UI 图形化。
- 温度:复用 `ArcTempView` 弧形,左右双区,加减按钮换 `ic_plus/ic_minus`。
- 风量:0-7 文字方块 → 条形可视化(`WindLevelView` 或复用 `GaugeView`),选中级别高亮。
- 出风/循环/模式:按钮改图标 + 文字,选中态 `bg_btn_ac_selected`(已有)。
- **删除下方车身控制区**:车身控制由 NavBar 车窗/座椅面板 + Status 页车图车门承载。

## 5. NavBar / Settings / Apps

### 5.1 NavBar

- 三段式结构保留(导航 / AC 快捷 / 功能)。
- 4 tab 图标全换矢量,选中态 tint `accent` + `bg_nav_tab_active`(已有)。
- AC 快捷 `❄ AC`/`🌀` 换 `ic_ac`/`ic_wind`,温度数字保留。
- 右侧 `🔄🪟💺` 换 `ic_cycle/window/seat`。
- 背景 `bg_nav_bar` 加顶部 1px 高光分隔。

### 5.2 Settings(1233 行,只做结构 + 图标)

- 按"显示 / 车辆 / 系统"三大分组卡片化,每组顶部分组图标 `ic_display/ic_vehicle/ic_system`。
- 开关、segment 控件保留现有实现,仅图标换矢量。
- 不拆分文件、不动 `SettingsPage.java` 逻辑,仅重排 `page_settings.xml` + 引用 token。

### 5.3 Apps

- `page_apps.xml` 现状 OK(RecyclerView),标题图标化,网格间距用 token,应用图标用真实 app icon(已从 PackageManager 取)。
- pip 快捷与 Apps 页共用 `ic_*.xml` 矢量占位。

## 6. 分阶段实施

| 阶段 | 内容 | 风险 |
|---|---|---|
| P1 基础设施 | 设计 token + 矢量图标包(全量替换 emoji)+ `GaugeView`/`VehicleDiagramView` 扩展 + `bg_ambient_glass` | 低,纯资源 + 新 View |
| P2 Status 页 | 车图整合胎压/车门、行程单卡、删零散迷你卡、右侧去能源 | 中,改 `StatusPage.java` + `page_status.xml` |
| P3 TopBar | 改能源常驻栏(电量+纯电续航+油量+总续航+时间) | 低,改 `TopBar.java` + `view_top_bar.xml` |
| P4 Controls 页 | 图形化空调面板,去车身控制 | 中,改 `ControlsPage.java` UI + `page_controls.xml` |
| P5 NavBar | 图标 + 质感升级 | 低 |
| P6 Settings/Apps | 分组卡片 + 图标 | 低 |

每阶段独立提交、独立验证。

## 7. 验证策略

- 无测试套件(CLAUDE.md 明确)。每阶段 `./gradlew assembleDebug` 编译通过 + ADB 装车机手动验证。
- 关键 View(`VehicleDiagramView`/`GaugeView`/`WindLevelView`)`onDraw` 用确定数据绘制,可脱离车机用模拟数据预览。
- 不改 API 层、不改权限、不改数据模型 `VehicleStatus` 字段(仅调整 UI 消费的字段集合)。

## 8. 不做的事(YAGNI)

- 不引入 Jetpack Compose。
- 不接系统电量/蓝牙广播(本期)。
- 不动 `BydVehicleManager`/各 `Byd*Api`/`DiplusClient`/`AutoserviceClient` 等 API 层。
- 不新增应用功能,仅重构既有信息的展示与控制 UI。
