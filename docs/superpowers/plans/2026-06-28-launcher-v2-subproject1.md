# BYD Launcher V2 子项目 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 BYDLauncher 从简易双区桌面重构为参考 Kinex 设计的多标签仪表盘界面，包含 Status 主屏（SVG 车辆俯视图+遥测数据+音乐卡片）、三段式底部导航栏、顶部状态栏和浅色/深色双主题系统。

**Architecture:** 单 Activity + 多 View 页面切换架构。底部导航栏切换 5 个标签页（子项目 1 只实现 Status 和占位页）。VehicleDiagramView 用 Canvas 绘制车辆俯视图，MusicCardView 通过 MediaSession 获取媒体信息。ThemeManager 管理浅色/深色切换。

**Tech Stack:** Android SDK (Java 17), AndroidX AppCompat, Material Components, RecyclerView, MediaSession API

**设计文档:** `docs/superpowers/specs/2026-06-28-launcher-v2-subproject1-design.md`

---

## 文件结构总览

### 创建的文件

| 文件 | 职责 |
|------|------|
| `java/.../theme/ThemeManager.java` | 主题管理单例 |
| `java/.../ui/StatusPage.java` | Status 标签页逻辑 |
| `java/.../ui/PlaceholderPage.java` | 占位标签页 |
| `java/.../ui/NavBar.java` | 底部导航栏逻辑 |
| `java/.../ui/TopBar.java` | 顶部状态栏逻辑 |
| `java/.../ui/MusicCardView.java` | 音乐播放器卡片 |
| `java/.../ui/VehicleDiagramView.java` | 车辆俯视图自定义 View |
| `res/layout/activity_main.xml` | 根布局（重写） |
| `res/layout/view_top_bar.xml` | 顶部状态栏 |
| `res/layout/view_nav_bar.xml` | 底部导航栏 |
| `res/layout/page_status.xml` | Status 标签页 |
| `res/layout/page_placeholder.xml` | 占位标签页 |
| `res/layout/card_music.xml` | 音乐卡片 |
| `res/layout/card_air_quality.xml` | 空气质量卡片 |
| `res/layout/card_datetime.xml` | 日期时间卡片 |
| `res/layout/card_app_shortcuts.xml` | 应用快捷方式卡片 |
| `res/values/colors.xml` | 浅色主题颜色（重写） |
| `res/values-night/colors.xml` | 深色主题颜色（新建） |
| `res/drawable/bg_nav_bar.xml` | 导航栏背景 |
| `res/drawable/bg_nav_tab.xml` | 标签按钮 selector |
| `res/drawable/bg_nav_tab_active.xml` | 选中标签背景 |
| `res/drawable/bg_top_bar.xml` | 顶部状态栏背景 |
| `res/drawable/bg_ac_quick.xml` | 空调快捷按钮 |

### 修改的文件

| 文件 | 改动 |
|------|------|
| `java/.../MainActivity.java` | 完全重写 |
| `res/values/dimens.xml` | 重写 |
| `res/values/strings.xml` | 重写 |
| `res/values/styles.xml` | 重写 |
| `res/layout/item_app.xml` | 微调 |
| `res/drawable/bg_card.xml` | 重写为主题感知 |
| `AndroidManifest.xml` | 追加权限 |

### 删除的文件

| 文件 | 原因 |
|------|------|
| `res/layout/view_home.xml` | 替换为 page_status.xml |
| `res/layout/view_dock.xml` | 替换为 view_nav_bar.xml |
| `res/layout/view_ac_panel.xml` | 移至子项目 2 |
| `res/layout/view_app_drawer.xml` | 移至子项目 2 |
| `res/drawable/bg_dock.xml` | 不再需要 |
| `res/drawable/bg_dock_btn.xml` | 不再需要 |
| `res/drawable/bg_panel.xml` | 不再需要 |
| `res/drawable/bg_drag_handle.xml` | 不再需要 |

### 保持不变的文件

- 所有 `api/` 目录下的 Java 文件
- `model/VehicleStatus.java`
- `adapter/AppListAdapter.java`
- `res/drawable/bg_circle_btn.xml`、`bg_switch_btn.xml`、`bg_ac_btn.xml`、`progress_bar_battery.xml`、`bg_app_item.xml`、`ic_launcher_foreground.xml`
- `res/mipmap-anydpi-v26/ic_launcher.xml`

---

### Task 1: 删除旧文件 + 更新资源基础

**Files:**
- Delete: `res/layout/view_home.xml`, `res/layout/view_dock.xml`, `res/layout/view_ac_panel.xml`, `res/layout/view_app_drawer.xml`
- Delete: `res/drawable/bg_dock.xml`, `res/drawable/bg_dock_btn.xml`, `res/drawable/bg_panel.xml`, `res/drawable/bg_drag_handle.xml`
- Modify: `res/values/colors.xml`
- Create: `res/values-night/colors.xml`
- Modify: `res/values/dimens.xml`
- Modify: `res/values/strings.xml`

- [ ] **Step 1: 删除旧布局和 drawable 文件**

```bash
cd /Users/feng/work/byd/app/src/main/res
rm -f layout/view_home.xml layout/view_dock.xml layout/view_ac_panel.xml layout/view_app_drawer.xml
rm -f drawable/bg_dock.xml drawable/bg_dock_btn.xml drawable/bg_panel.xml drawable/bg_drag_handle.xml
```

- [ ] **Step 2: 写入浅色主题 colors.xml**

写入 `/Users/feng/work/byd/app/src/main/res/values/colors.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 浅色主题 -->
    <color name="bg_primary">#FFF2F4F7</color>
    <color name="bg_surface">#FFFFFFFF</color>
    <color name="bg_surface_variant">#FFF8F9FA</color>
    <color name="bg_nav_bar">#FFFFFFFF</color>
    <color name="bg_top_bar">#FFE8EBF0</color>
    <color name="bg_overlay">#33000000</color>

    <color name="accent">#FF0A84FF</color>
    <color name="accent_dim">#330A84FF</color>

    <color name="text_primary">#FF1A1A1A</color>
    <color name="text_secondary">#FF6B7280</color>
    <color name="text_tertiary">#FF9CA3AF</color>
    <color name="text_on_accent">#FFFFFFFF</color>

    <color name="border">#FFE5E7EB</color>
    <color name="divider">#FFE5E7EB</color>
    <color name="icon_tint">#FF6B7280</color>

    <!-- 功能色（主题无关） -->
    <color name="status_good">#FF34C759</color>
    <color name="status_fair">#FFFFCC00</color>
    <color name="status_poor">#FFFF9500</color>
    <color name="status_bad">#FFFF3B30</color>
    <color name="battery_high">#FF34C759</color>
    <color name="battery_mid">#FFFF9500</color>
    <color name="battery_low">#FFFF3B30</color>
    <color name="door_open">#FFFF3B30</color>
    <color name="door_closed">#FF34C759</color>
    <color name="gear_active">#FF0A84FF</color>
    <color name="gear_inactive">#FFD1D5DB</color>

    <!-- 车辆图 -->
    <color name="vehicle_body">#FFD1D5DB</color>
    <color name="vehicle_body_stroke">#FF9CA3AF</color>
    <color name="vehicle_window">#FFE5E7EB</color>
    <color name="vehicle_wheel">#FF6B7280</color>
</resources>
```

- [ ] **Step 3: 创建深色主题 colors.xml**

创建目录并写入 `/Users/feng/work/byd/app/src/main/res/values-night/colors.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 深色主题 -->
    <color name="bg_primary">#FF0D1117</color>
    <color name="bg_surface">#FF161B22</color>
    <color name="bg_surface_variant">#FF1C2333</color>
    <color name="bg_nav_bar">#FF161B22</color>
    <color name="bg_top_bar">#FF0D1117</color>
    <color name="bg_overlay">#80000000</color>

    <color name="accent">#FF00D4FF</color>
    <color name="accent_dim">#3300D4FF</color>

    <color name="text_primary">#FFF0F6FC</color>
    <color name="text_secondary">#FF8B949E</color>
    <color name="text_tertiary">#FF484F58</color>
    <color name="text_on_accent">#FF000000</color>

    <color name="border">#FF30363D</color>
    <color name="divider">#FF30363D</color>
    <color name="icon_tint">#FF8B949E</color>

    <!-- 车辆图 -->
    <color name="vehicle_body">#FF30363D</color>
    <color name="vehicle_body_stroke">#FF484F58</color>
    <color name="vehicle_window">#FF1C2333</color>
    <color name="vehicle_wheel">#FF8B949E</color>
</resources>
```

- [ ] **Step 4: 写入 dimens.xml**

写入 `/Users/feng/work/byd/app/src/main/res/values/dimens.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 顶部栏 -->
    <dimen name="top_bar_height">32dp</dimen>
    <dimen name="top_bar_text">12sp</dimen>
    <dimen name="top_bar_icon">14sp</dimen>
    <dimen name="top_bar_padding_h">16dp</dimen>

    <!-- 底部导航栏 -->
    <dimen name="nav_bar_height">56dp</dimen>
    <dimen name="nav_tab_icon">18sp</dimen>
    <dimen name="nav_tab_text">11sp</dimen>
    <dimen name="nav_tab_padding">6dp</dimen>
    <dimen name="nav_shortcut_icon">20dp</dimen>
    <dimen name="nav_ac_text">14sp</dimen>
    <dimen name="nav_ac_btn">28dp</dimen>

    <!-- 页面内容 -->
    <dimen name="page_padding">16dp</dimen>
    <dimen name="card_radius">14dp</dimen>
    <dimen name="card_padding">16dp</dimen>
    <dimen name="card_elevation">2dp</dimen>

    <!-- Status 页 -->
    <dimen name="clock_time_size">36sp</dimen>
    <dimen name="clock_date_size">14sp</dimen>
    <dimen name="speed_size">48sp</dimen>
    <dimen name="battery_text_size">22sp</dimen>
    <dimen name="gear_text_size">16sp</dimen>
    <dimen name="tire_text_size">12sp</dimen>
    <dimen name="tire_unit_size">9sp</dimen>

    <!-- 音乐卡片 -->
    <dimen name="music_cover_size">56dp</dimen>
    <dimen name="music_title_size">15sp</dimen>
    <dimen name="music_artist_size">13sp</dimen>
    <dimen name="music_control_size">24dp</dimen>
    <dimen name="music_control_play_size">32dp</dimen>

    <!-- 空气质量卡片 -->
    <dimen name="aq_label_size">13sp</dimen>
    <dimen name="aq_value_size">13sp</dimen>

    <!-- 应用快捷方式 -->
    <dimen name="shortcut_icon_size">40dp</dimen>
    <dimen name="shortcut_add_size">40dp</dimen>

    <!-- 通用间距 -->
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">12dp</dimen>
    <dimen name="spacing_lg">16dp</dimen>
    <dimen name="spacing_xl">24dp</dimen>
</resources>
```

- [ ] **Step 5: 写入 strings.xml**

写入 `/Users/feng/work/byd/app/src/main/res/values/strings.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">BYD Launcher</string>

    <!-- 顶部栏 -->
    <string name="no_media">未在播放</string>

    <!-- 底部导航 -->
    <string name="tab_status">Status</string>
    <string name="tab_map">Map</string>
    <string name="tab_controls">Controls</string>
    <string name="tab_apps">Apps</string>
    <string name="tab_settings">Settings</string>
    <string name="ac_off_label">OFF</string>

    <!-- Status 页 -->
    <string name="air_quality">空气质量</string>
    <string name="aq_outside">室外</string>
    <string name="aq_inside">室内</string>
    <string name="aq_excellent">优</string>
    <string name="aq_good">良</string>
    <string name="aq_fair">中</string>
    <string name="aq_poor">差</string>
    <string name="speed_unit">km/h</string>
    <string name="tire_unit">PSI</string>
    <string name="battery_format">%d%%</string>

    <!-- 音乐卡片 -->
    <string name="not_playing">未在播放</string>
    <string name="music_unknown_title">未知歌曲</string>
    <string name="music_unknown_artist">未知歌手</string>

    <!-- 快捷方式 -->
    <string name="add_shortcut">添加</string>

    <!-- 占位页 -->
    <string name="coming_soon">即将推出</string>

    <!-- 车门 -->
    <string name="all_closed">全部关闭</string>
    <string name="locked_text">已锁车</string>
    <string name="unlocked_text">已解锁</string>
</resources>
```

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "refactor: clean old layout files, set up dual-theme color resources"
```

---

### Task 2: 更新样式 + 创建新 drawable 资源

**Files:**
- Modify: `res/values/styles.xml`
- Modify: `res/drawable/bg_card.xml`
- Create: `res/drawable/bg_nav_bar.xml`
- Create: `res/drawable/bg_nav_tab.xml`
- Create: `res/drawable/bg_nav_tab_active.xml`
- Create: `res/drawable/bg_top_bar.xml`
- Create: `res/drawable/bg_ac_quick.xml`

- [ ] **Step 1: 重写 styles.xml**

写入 `/Users/feng/work/byd/app/src/main/res/values/styles.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowBackground">@color/bg_primary</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
        <item name="android:windowTranslucentNavigation">true</item>
        <item name="colorPrimary">@color/accent</item>
        <item name="colorAccent">@color/accent</item>
    </style>

    <style name="CardStyle">
        <item name="android:background">@drawable/bg_card</item>
        <item name="android:padding">@dimen/card_padding</item>
    </style>

    <style name="NavTabText">
        <item name="android:textSize">@dimen/nav_tab_text</item>
        <item name="android:textColor">@color/text_secondary</item>
    </style>

    <style name="NavTabIcon">
        <item name="android:textSize">@dimen/nav_tab_icon</item>
    </style>

    <style name="AcQuickButton">
        <item name="android:layout_width">@dimen/nav_ac_btn</item>
        <item name="android:layout_height">@dimen/nav_ac_btn</item>
        <item name="android:background">@drawable/bg_ac_quick</item>
        <item name="android:gravity">center</item>
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">16sp</item>
    </style>
</resources>
```

- [ ] **Step 2: 重写 bg_card.xml（主题感知）**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_card.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_surface" />
    <corners android:radius="@dimen/card_radius" />
    <stroke android:width="0dp" android:color="@color/border" />
</shape>
```

- [ ] **Step 3: 创建 bg_nav_bar.xml**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_nav_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_nav_bar" />
    <stroke android:width="1dp" android:color="@color/border" />
</shape>
```

- [ ] **Step 4: 创建 bg_nav_tab_active.xml**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_nav_tab_active.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/accent_dim" />
    <corners android:radius="10dp" />
</shape>
```

- [ ] **Step 5: 创建 bg_nav_tab.xml**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_nav_tab.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true" android:drawable="@drawable/bg_nav_tab_active" />
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/accent_dim" />
            <corners android:radius="10dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@android:color/transparent" />
        </shape>
    </item>
</selector>
```

- [ ] **Step 6: 创建 bg_top_bar.xml**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_top_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_top_bar" />
</shape>
```

- [ ] **Step 7: 创建 bg_ac_quick.xml**

写入 `/Users/feng/work/byd/app/src/main/res/drawable/bg_ac_quick.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="oval">
            <solid android:color="@color/accent_dim" />
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/bg_surface_variant" />
        </shape>
    </item>
</selector>
```

- [ ] **Step 8: 提交**

```bash
git add -A && git commit -m "refactor: update styles and drawable resources for V2 theme"
```

---

### Task 3: 创建布局文件 — 顶部栏 + 底部导航栏

**Files:**
- Create: `res/layout/view_top_bar.xml`
- Create: `res/layout/view_nav_bar.xml`

- [ ] **Step 1: 创建 view_top_bar.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/view_top_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/top_bar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_bar_height"
    android:background="@drawable/bg_top_bar"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="@dimen/top_bar_padding_h"
    android:paddingEnd="@dimen/top_bar_padding_h">

    <!-- 左侧系统图标 -->
    <TextView
        android:id="@+id/top_sys_icons"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="🔋 📶 🔵 📍"
        android:textSize="@dimen/top_bar_icon"
        android:textColor="@color/text_secondary" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- 中间应用名 -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textSize="@dimen/top_bar_text"
        android:textColor="@color/text_tertiary" />

    <View
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- 右侧媒体 + 时间 -->
    <TextView
        android:id="@+id/top_media_source"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="♫"
        android:textSize="@dimen/top_bar_text"
        android:textColor="@color/text_secondary"
        android:layout_marginEnd="@dimen/spacing_sm" />

    <TextClock
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format24Hour="HH:mm"
        android:format12Hour="hh:mm"
        android:textSize="@dimen/top_bar_text"
        android:textColor="@color/text_secondary"
        android:textStyle="bold" />
</LinearLayout>
```

- [ ] **Step 2: 创建 view_nav_bar.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/view_nav_bar.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_bar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/nav_bar_height"
    android:background="@drawable/bg_nav_bar"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="@dimen/spacing_md"
    android:paddingEnd="@dimen/spacing_md">

    <!-- 左段：应用快捷方式 -->
    <LinearLayout
        android:id="@+id/nav_shortcuts"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="15"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingEnd="@dimen/spacing_sm">

        <ImageView
            android:id="@+id/nav_shortcut_1"
            android:layout_width="@dimen/nav_shortcut_icon"
            android:layout_height="@dimen/nav_shortcut_icon"
            android:layout_marginEnd="@dimen/spacing_sm"
            android:scaleType="fitCenter"
            android:src="@android:drawable/ic_menu_compass" />

        <ImageView
            android:id="@+id/nav_shortcut_2"
            android:layout_width="@dimen/nav_shortcut_icon"
            android:layout_height="@dimen/nav_shortcut_icon"
            android:layout_marginEnd="@dimen/spacing_sm"
            android:scaleType="fitCenter"
            android:src="@android:drawable/ic_menu_call" />
    </LinearLayout>

    <!-- 分隔线 -->
    <View
        android:layout_width="1dp"
        android:layout_height="28dp"
        android:background="@color/divider" />

    <!-- 中段：标签页按钮 -->
    <LinearLayout
        android:id="@+id/nav_tabs"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="55"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingStart="@dimen/spacing_sm"
        android:paddingEnd="@dimen/spacing_sm">

        <LinearLayout
            android:id="@+id/tab_status"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_nav_tab"
            android:gravity="center"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_xs">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="⊙" style="@style/NavTabIcon" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/tab_status" style="@style/NavTabText" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/tab_map"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_nav_tab"
            android:gravity="center"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_xs">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="🗺" style="@style/NavTabIcon" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/tab_map" style="@style/NavTabText" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/tab_controls"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_nav_tab"
            android:gravity="center"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_xs">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="🎛" style="@style/NavTabIcon" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/tab_controls" style="@style/NavTabText" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/tab_apps"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_nav_tab"
            android:gravity="center"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_xs">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="⊞" style="@style/NavTabIcon" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/tab_apps" style="@style/NavTabText" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/tab_settings"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/bg_nav_tab"
            android:gravity="center"
            android:orientation="vertical">
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="⚙" style="@style/NavTabIcon" />
            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/tab_settings" style="@style/NavTabText" />
        </LinearLayout>
    </LinearLayout>

    <!-- 分隔线 -->
    <View
        android:layout_width="1dp"
        android:layout_height="28dp"
        android:background="@color/divider" />

    <!-- 右段：空调快捷控制 -->
    <LinearLayout
        android:id="@+id/nav_ac_section"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="30"
        android:gravity="center"
        android:orientation="horizontal"
        android:paddingStart="@dimen/spacing_sm">

        <TextView
            android:id="@+id/nav_ac_temp"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="25°"
            android:textColor="@color/text_primary"
            android:textSize="@dimen/nav_ac_text"
            android:textStyle="bold"
            android:layout_marginEnd="@dimen/spacing_sm" />

        <TextView
            android:id="@+id/nav_ac_down"
            style="@style/AcQuickButton"
            android:text="−"
            android:layout_marginEnd="@dimen/spacing_xs" />

        <TextView
            android:id="@+id/nav_ac_up"
            style="@style/AcQuickButton"
            android:text="+"
            android:layout_marginEnd="@dimen/spacing_md" />

        <TextView
            android:id="@+id/nav_ac_wind_icon"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🌀"
            android:textSize="14sp"
            android:layout_marginEnd="@dimen/spacing_xs" />

        <TextView
            android:id="@+id/nav_ac_wind_level"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="3"
            android:textColor="@color/text_primary"
            android:textSize="@dimen/nav_ac_text" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: create top bar and nav bar layouts"
```

---

### Task 4: 创建布局文件 — Status 页面卡片

**Files:**
- Create: `res/layout/card_air_quality.xml`
- Create: `res/layout/card_datetime.xml`
- Create: `res/layout/card_music.xml`
- Create: `res/layout/card_app_shortcuts.xml`

- [ ] **Step 1: 创建 card_air_quality.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/card_air_quality.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card"
    android:orientation="vertical"
    android:padding="@dimen/card_padding">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/air_quality"
        android:textColor="@color/text_secondary"
        android:textSize="@dimen/aq_label_size"
        android:drawableStart="@android:drawable/ic_menu_compass"
        android:drawablePadding="@dimen/spacing_xs"
        android:layout_marginBottom="@dimen/spacing_md" />

    <!-- 室外 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="@dimen/spacing_sm">
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_outside"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/aq_label_size" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView
            android:id="@+id/aq_outside_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_good"
            android:textColor="@color/status_fair"
            android:textSize="@dimen/aq_value_size"
            android:textStyle="bold" />
        <TextView
            android:id="@+id/aq_outside_dot"
            android:layout_width="8dp"
            android:layout_height="8dp"
            android:layout_marginStart="@dimen/spacing_xs"
            android:background="@color/status_fair" />
    </LinearLayout>

    <!-- 室内 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical">
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_inside"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/aq_label_size" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView
            android:id="@+id/aq_inside_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_excellent"
            android:textColor="@color/status_good"
            android:textSize="@dimen/aq_value_size"
            android:textStyle="bold" />
        <TextView
            android:id="@+id/aq_inside_dot"
            android:layout_width="8dp"
            android:layout_height="8dp"
            android:layout_marginStart="@dimen/spacing_xs"
            android:background="@color/status_good" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 创建 card_datetime.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/card_datetime.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card"
    android:orientation="vertical"
    android:padding="@dimen/card_padding"
    android:gravity="center">

    <TextClock
        android:id="@+id/status_date"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format24Hour="MM月dd日 EEEE"
        android:format12Hour="MM月dd日 EEEE"
        android:textColor="@color/text_secondary"
        android:textSize="@dimen/clock_date_size" />

    <TextClock
        android:id="@+id/status_time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:format24Hour="HH:mm"
        android:format12Hour="hh:mm"
        android:textColor="@color/text_primary"
        android:textSize="@dimen/clock_time_size"
        android:textStyle="bold"
        android:fontFamily="sans-serif-light" />
</LinearLayout>
```

- [ ] **Step 3: 创建 card_music.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/card_music.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/music_card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card"
    android:orientation="vertical"
    android:padding="@dimen/card_padding">

    <!-- 顶部：应用图标 + 来源名 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="@dimen/spacing_md">

        <ImageView
            android:id="@+id/music_app_icon"
            android:layout_width="16dp"
            android:layout_height="16dp"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_xs" />

        <TextView
            android:id="@+id/music_app_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/not_playing"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/aq_label_size" />
    </LinearLayout>

    <!-- 中间：封面 + 歌曲信息 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="@dimen/spacing_md">

        <ImageView
            android:id="@+id/music_cover"
            android:layout_width="@dimen/music_cover_size"
            android:layout_height="@dimen/music_cover_size"
            android:scaleType="centerCrop"
            android:background="@color/bg_surface_variant"
            android:layout_marginEnd="@dimen/spacing_md" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/music_title"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/not_playing"
                android:textColor="@color/text_primary"
                android:textSize="@dimen/music_title_size"
                android:textStyle="bold"
                android:maxLines="1"
                android:ellipsize="end" />

            <TextView
                android:id="@+id/music_artist"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/music_artist_size"
                android:maxLines="1"
                android:ellipsize="end" />
        </LinearLayout>
    </LinearLayout>

    <!-- 控制按钮 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center">

        <ImageView
            android:id="@+id/music_prev"
            android:layout_width="@dimen/music_control_size"
            android:layout_height="@dimen/music_control_size"
            android:src="@android:drawable/ic_media_previous"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_xl" />

        <ImageView
            android:id="@+id/music_play_pause"
            android:layout_width="@dimen/music_control_play_size"
            android:layout_height="@dimen/music_control_play_size"
            android:src="@android:drawable/ic_media_play"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_xl" />

        <ImageView
            android:id="@+id/music_next"
            android:layout_width="@dimen/music_control_size"
            android:layout_height="@dimen/music_control_size"
            android:src="@android:drawable/ic_media_next"
            android:scaleType="fitCenter" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 4: 创建 card_app_shortcuts.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/card_app_shortcuts.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/shortcuts_card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card"
    android:orientation="horizontal"
    android:padding="@dimen/card_padding"
    android:gravity="center">

    <ImageView
        android:id="@+id/shortcut_app_1"
        android:layout_width="@dimen/shortcut_icon_size"
        android:layout_height="@dimen/shortcut_icon_size"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_md" />

    <ImageView
        android:id="@+id/shortcut_app_2"
        android:layout_width="@dimen/shortcut_icon_size"
        android:layout_height="@dimen/shortcut_icon_size"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_md" />

    <ImageView
        android:id="@+id/shortcut_app_3"
        android:layout_width="@dimen/shortcut_icon_size"
        android:layout_height="@dimen/shortcut_icon_size"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_md" />

    <!-- 添加按钮 -->
    <TextView
        android:id="@+id/shortcut_add"
        android:layout_width="@dimen/shortcut_add_size"
        android:layout_height="@dimen/shortcut_add_size"
        android:background="@drawable/bg_ac_quick"
        android:gravity="center"
        android:text="+"
        android:textColor="@color/text_secondary"
        android:textSize="20sp" />
</LinearLayout>
```

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat: create status page card layouts (air quality, datetime, music, shortcuts)"
```

---

### Task 5: 创建 Status 页面布局 + 占位页 + 主容器

**Files:**
- Create: `res/layout/page_status.xml`
- Create: `res/layout/page_placeholder.xml`
- Modify: `res/layout/activity_main.xml`

- [ ] **Step 1: 创建 page_status.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/page_status.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_status"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:padding="@dimen/page_padding">

    <!-- 左侧 Widget 列 (20%) -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="20"
        android:orientation="vertical"
        android:layout_marginEnd="@dimen/spacing_md">

        <include
            layout="@layout/card_air_quality"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/spacing_md" />

        <include
            layout="@layout/card_datetime"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </LinearLayout>

    <!-- 中央仪表区 (45%) -->
    <FrameLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="45"
        android:layout_marginEnd="@dimen/spacing_md">

        <!-- 车辆俯视图（自定义 View） -->
        <com.bydlauncher.ui.VehicleDiagramView
            android:id="@+id/vehicle_diagram"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_gravity="center" />

        <!-- 四角胎压 -->
        <TextView
            android:id="@+id/tire_fl"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start|top"
            android:layout_marginStart="8dp"
            android:layout_marginTop="16dp"
            android:text="37"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/tire_text_size" />

        <TextView
            android:id="@+id/tire_fr"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end|top"
            android:layout_marginEnd="8dp"
            android:layout_marginTop="16dp"
            android:text="36"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/tire_text_size" />

        <TextView
            android:id="@+id/tire_rl"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start|bottom"
            android:layout_marginStart="8dp"
            android:layout_marginBottom="60dp"
            android:text="42"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/tire_text_size" />

        <TextView
            android:id="@+id/tire_rr"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end|bottom"
            android:layout_marginEnd="8dp"
            android:layout_marginBottom="60dp"
            android:text="43"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/tire_text_size" />

        <!-- 底部遥测数据 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom"
            android:gravity="center"
            android:orientation="vertical"
            android:padding="@dimen/spacing_sm">

            <!-- 挡位指示 -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="@dimen/spacing_xs">
                <TextView android:id="@+id/gear_p" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="P" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_md" />
                <TextView android:id="@+id/gear_r" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="R" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_md" />
                <TextView android:id="@+id/gear_n" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="N" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_md" />
                <TextView android:id="@+id/gear_d" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="D" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" />
            </LinearLayout>

            <!-- 速度 -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="baseline"
                android:orientation="horizontal">
                <TextView
                    android:id="@+id/tv_speed"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/speed_size"
                    android:textStyle="bold"
                    android:fontFamily="sans-serif-light" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/speed_unit"
                    android:textColor="@color/text_secondary"
                    android:textSize="@dimen/gear_text_size"
                    android:layout_marginStart="@dimen/spacing_xs" />
            </LinearLayout>

            <!-- 电量 -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="⚡"
                    android:textSize="14sp" />
                <TextView
                    android:id="@+id/tv_battery"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="69%"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/battery_text_size"
                    android:textStyle="bold"
                    android:layout_marginStart="@dimen/spacing_xs" />
                <ProgressBar
                    android:id="@+id/progress_battery"
                    style="?android:attr/progressBarStyleHorizontal"
                    android:layout_width="80dp"
                    android:layout_height="6dp"
                    android:layout_marginStart="@dimen/spacing_sm"
                    android:max="100"
                    android:progress="69"
                    android:progressDrawable="@drawable/progress_bar_battery" />
            </LinearLayout>
        </LinearLayout>
    </FrameLayout>

    <!-- 右侧卡片列 (35%) -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="35"
        android:orientation="vertical">

        <include
            layout="@layout/card_music"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="@dimen/spacing_md" />

        <include
            layout="@layout/card_app_shortcuts"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 创建 page_placeholder.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/page_placeholder.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/placeholder_text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="@string/coming_soon"
        android:textColor="@color/text_tertiary"
        android:textSize="20sp" />
</FrameLayout>
```

- [ ] **Step 3: 重写 activity_main.xml**

写入 `/Users/feng/work/byd/app/src/main/res/layout/activity_main.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_primary"
    android:orientation="vertical">

    <!-- 顶部状态栏 -->
    <include
        layout="@layout/view_top_bar"
        android:layout_width="match_parent"
        android:layout_height="@dimen/top_bar_height" />

    <!-- 内容区（标签页切换） -->
    <FrameLayout
        android:id="@+id/content_frame"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- Status 页 -->
        <include layout="@layout/page_status" />

        <!-- 占位页（Map/Controls/Apps/Settings 共用） -->
        <include
            layout="@layout/page_placeholder"
            android:id="@+id/page_placeholder"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone" />
    </FrameLayout>

    <!-- 底部导航栏 -->
    <include
        layout="@layout/view_nav_bar"
        android:layout_width="match_parent"
        android:layout_height="@dimen/nav_bar_height" />
</LinearLayout>
```

- [ ] **Step 4: 提交**

```bash
git add -A && git commit -m "feat: create status page, placeholder page, and main activity layout"
```

---

### Task 6: 创建 ThemeManager.java

**Files:**
- Create: `java/com/bydlauncher/theme/ThemeManager.java`

- [ ] **Step 1: 创建 ThemeManager.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/theme/ThemeManager.java`：

```java
package com.bydlauncher.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static ThemeManager instance;
    private final SharedPreferences prefs;

    private ThemeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    public void applyTheme() {
        int mode = getThemeMode();
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        applyTheme();
    }

    public boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: create ThemeManager for light/dark theme switching"
```

---

### Task 7: 创建 VehicleDiagramView.java

**Files:**
- Create: `java/com/bydlauncher/ui/VehicleDiagramView.java`

- [ ] **Step 1: 创建 VehicleDiagramView.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/VehicleDiagramView.java`：

```java
package com.bydlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;

public class VehicleDiagramView extends View {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doorHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean doorLF, doorRF, doorLR, doorRR, trunk, hood;

    public VehicleDiagramView(Context context) {
        super(context);
        initPaints();
    }

    public VehicleDiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public VehicleDiagramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        bodyPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_body));
        bodyPaint.setStyle(Paint.Style.FILL);

        bodyStrokePaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_body_stroke));
        bodyStrokePaint.setStyle(Paint.Style.STROKE);
        bodyStrokePaint.setStrokeWidth(3f);

        windowPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_window));
        windowPaint.setStyle(Paint.Style.FILL);

        wheelPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_wheel));
        wheelPaint.setStyle(Paint.Style.FILL);

        doorHighlightPaint.setColor(ContextCompat.getColor(getContext(), R.color.door_open));
        doorHighlightPaint.setStyle(Paint.Style.STROKE);
        doorHighlightPaint.setStrokeWidth(4f);
    }

    public void setDoorStates(boolean lf, boolean rf, boolean lr, boolean rr, boolean trk, boolean hd) {
        this.doorLF = lf;
        this.doorRF = rf;
        this.doorLR = lr;
        this.doorRR = rr;
        this.trunk = trk;
        this.hood = hd;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        float carW = w * 0.45f;
        float carH = h * 0.75f;
        float left = cx - carW / 2f;
        float top = cy - carH / 2f;
        float right = cx + carW / 2f;
        float bottom = cy + carH / 2f;

        float radius = carW * 0.2f;

        // 车身
        RectF bodyRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(bodyRect, radius, radius, bodyPaint);
        canvas.drawRoundRect(bodyRect, radius, radius, bodyStrokePaint);

        // 车窗区域
        float winMargin = carW * 0.12f;
        float winTop = top + carH * 0.18f;
        float winBottom = top + carH * 0.45f;
        RectF windowRect = new RectF(left + winMargin, winTop, right - winMargin, winBottom);
        canvas.drawRoundRect(windowRect, radius * 0.6f, radius * 0.6f, windowPaint);

        // 后窗
        float rWinTop = top + carH * 0.6f;
        float rWinBottom = top + carH * 0.78f;
        RectF rearWindowRect = new RectF(left + winMargin, rWinTop, right - winMargin, rWinBottom);
        canvas.drawRoundRect(rearWindowRect, radius * 0.6f, radius * 0.6f, windowPaint);

        // 4 个车轮
        float wheelW = carW * 0.12f;
        float wheelH = carH * 0.1f;
        float wheelOffset = carW * 0.08f;

        // 左前轮
        canvas.drawRoundRect(new RectF(left - wheelOffset, top + carH * 0.15f, left - wheelOffset + wheelW, top + carH * 0.15f + wheelH), 4, 4, wheelPaint);
        // 右前轮
        canvas.drawRoundRect(new RectF(right + wheelOffset - wheelW, top + carH * 0.15f, right + wheelOffset, top + carH * 0.15f + wheelH), 4, 4, wheelPaint);
        // 左后轮
        canvas.drawRoundRect(new RectF(left - wheelOffset, bottom - carH * 0.15f - wheelH, left - wheelOffset + wheelW, bottom - carH * 0.15f), 4, 4, wheelPaint);
        // 右后轮
        canvas.drawRoundRect(new RectF(right + wheelOffset - wheelW, bottom - carH * 0.15f - wheelH, right + wheelOffset, bottom - carH * 0.15f), 4, 4, wheelPaint);

        // 车门高亮
        float doorStrokeInset = 4f;
        if (doorLF) {
            canvas.drawLine(left + doorStrokeInset, top + carH * 0.2f, left + doorStrokeInset, cy, doorHighlightPaint);
        }
        if (doorRF) {
            canvas.drawLine(right - doorStrokeInset, top + carH * 0.2f, right - doorStrokeInset, cy, doorHighlightPaint);
        }
        if (doorLR) {
            canvas.drawLine(left + doorStrokeInset, cy, left + doorStrokeInset, bottom - carH * 0.2f, doorHighlightPaint);
        }
        if (doorRR) {
            canvas.drawLine(right - doorStrokeInset, cy, right - doorStrokeInset, bottom - carH * 0.2f, doorHighlightPaint);
        }
        if (hood) {
            canvas.drawLine(left + carW * 0.2f, top + doorStrokeInset, right - carW * 0.2f, top + doorStrokeInset, doorHighlightPaint);
        }
        if (trunk) {
            canvas.drawLine(left + carW * 0.2f, bottom - doorStrokeInset, right - carW * 0.2f, bottom - doorStrokeInset, doorHighlightPaint);
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: create VehicleDiagramView with Canvas-based car top-view"
```

---

### Task 8: 创建 MusicCardView.java

**Files:**
- Create: `java/com/bydlauncher/ui/MusicCardView.java`

- [ ] **Step 1: 创建 MusicCardView.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/MusicCardView.java`：

```java
package com.bydlauncher.ui;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bydlauncher.R;

import java.util.List;

public class MusicCardView {

    private static final String TAG = "MusicCardView";

    private final Context context;
    private final View rootView;

    private final ImageView appIcon;
    private final TextView appName;
    private final ImageView cover;
    private final TextView title;
    private final TextView artist;
    private final ImageView btnPrev;
    private final ImageView btnPlayPause;
    private final ImageView btnNext;

    private MediaController activeController;

    public MusicCardView(View rootView) {
        this.context = rootView.getContext();
        this.rootView = rootView;

        appIcon = rootView.findViewById(R.id.music_app_icon);
        appName = rootView.findViewById(R.id.music_app_name);
        cover = rootView.findViewById(R.id.music_cover);
        title = rootView.findViewById(R.id.music_title);
        artist = rootView.findViewById(R.id.music_artist);
        btnPrev = rootView.findViewById(R.id.music_prev);
        btnPlayPause = rootView.findViewById(R.id.music_play_pause);
        btnNext = rootView.findViewById(R.id.music_next);

        btnPrev.setOnClickListener(v -> sendMediaCommand(PlaybackState.ACTION_SKIP_TO_PREVIOUS));
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> sendMediaCommand(PlaybackState.ACTION_SKIP_TO_NEXT));

        showNoMedia();
    }

    public void refreshMediaState() {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) {
                showNoMedia();
                return;
            }

            List<MediaController> controllers = msm.getActiveSessions(null);
            if (controllers.isEmpty()) {
                showNoMedia();
                return;
            }

            activeController = controllers.get(0);
            updateFromController(activeController);
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to access media sessions, showing simulation", e);
            showSimulatedMedia();
        } catch (Exception e) {
            Log.w(TAG, "Failed to get media sessions", e);
            showNoMedia();
        }
    }

    private void updateFromController(MediaController controller) {
        MediaMetadata metadata = controller.getMetadata();
        if (metadata == null) {
            showNoMedia();
            return;
        }

        String titleText = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        String artistText = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);

        if (titleText == null) titleText = context.getString(R.string.music_unknown_title);
        if (artistText == null) artistText = context.getString(R.string.music_unknown_artist);

        title.setText(titleText);
        artist.setText(artistText);
        artist.setVisibility(View.VISIBLE);

        if (art != null) {
            cover.setImageBitmap(art);
        } else {
            cover.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        String packageName = controller.getPackageName();
        try {
            appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
            appName.setText(context.getPackageManager().getApplicationLabel(
                    context.getPackageManager().getApplicationInfo(packageName, 0)));
        } catch (Exception e) {
            appName.setText(packageName);
        }

        PlaybackState state = controller.getPlaybackState();
        boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        btnPlayPause.setImageResource(playing
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
    }

    private void togglePlayPause() {
        if (activeController == null) return;
        PlaybackState state = activeController.getPlaybackState();
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
            activeController.getTransportControls().pause();
        } else if (activeController != null) {
            activeController.getTransportControls().play();
        }
    }

    private void sendMediaCommand(long action) {
        if (activeController == null) return;
        if (action == PlaybackState.ACTION_SKIP_TO_PREVIOUS) {
            activeController.getTransportControls().skipToPrevious();
        } else if (action == PlaybackState.ACTION_SKIP_TO_NEXT) {
            activeController.getTransportControls().skipToNext();
        }
    }

    private void showNoMedia() {
        title.setText(R.string.not_playing);
        artist.setVisibility(View.GONE);
        appName.setText("");
        cover.setImageResource(android.R.drawable.ic_menu_gallery);
        activeController = null;
    }

    private void showSimulatedMedia() {
        appName.setText("Music");
        title.setText("Simulated Track");
        artist.setText("BYD Audio");
        artist.setVisibility(View.VISIBLE);
        cover.setImageResource(android.R.drawable.ic_menu_gallery);
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
    }

    public String getMediaSourceName() {
        if (activeController != null) {
            try {
                return context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(activeController.getPackageName(), 0)).toString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: create MusicCardView with MediaSession integration"
```

---

### Task 9: 创建 UI 辅助类（TopBar、NavBar、StatusPage、PlaceholderPage）

**Files:**
- Create: `java/com/bydlauncher/ui/TopBar.java`
- Create: `java/com/bydlauncher/ui/NavBar.java`
- Create: `java/com/bydlauncher/ui/StatusPage.java`
- Create: `java/com/bydlauncher/ui/PlaceholderPage.java`

- [ ] **Step 1: 创建 TopBar.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/TopBar.java`：

```java
package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import com.bydlauncher.R;

public class TopBar {

    private final TextView mediaSource;

    public TopBar(View rootView) {
        mediaSource = rootView.findViewById(R.id.top_media_source);
    }

    public void updateMediaSource(String sourceName) {
        if (sourceName != null && !sourceName.isEmpty()) {
            mediaSource.setText("♫ " + sourceName);
        } else {
            mediaSource.setText("♫");
        }
    }
}
```

- [ ] **Step 2: 创建 NavBar.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/NavBar.java`：

```java
package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydAcApi;

public class NavBar {

    public interface TabListener {
        void onTabSelected(int tabIndex);
    }

    private final View rootView;
    private final View[] tabs;
    private final TextView acTemp;
    private final TextView acWindLevel;
    private int currentTab = 0;
    private TabListener listener;

    private int currentAcTemp = 25;

    public NavBar(View rootView, BydAcApi acApi) {
        this.rootView = rootView;

        tabs = new View[]{
                rootView.findViewById(R.id.tab_status),
                rootView.findViewById(R.id.tab_map),
                rootView.findViewById(R.id.tab_controls),
                rootView.findViewById(R.id.tab_apps),
                rootView.findViewById(R.id.tab_settings)
        };

        acTemp = rootView.findViewById(R.id.nav_ac_temp);
        acWindLevel = rootView.findViewById(R.id.nav_ac_wind_level);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> selectTab(index));
        }

        rootView.findViewById(R.id.nav_ac_down).setOnClickListener(v -> {
            currentAcTemp = Math.max(17, currentAcTemp - 1);
            acApi.setMainTemp(currentAcTemp);
            acTemp.setText(currentAcTemp + "°");
        });

        rootView.findViewById(R.id.nav_ac_up).setOnClickListener(v -> {
            currentAcTemp = Math.min(33, currentAcTemp + 1);
            acApi.setMainTemp(currentAcTemp);
            acTemp.setText(currentAcTemp + "°");
        });

        selectTab(0);
    }

    public void setTabListener(TabListener listener) {
        this.listener = listener;
    }

    public void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setSelected(i == index);
        }
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    public void updateAcState(boolean acOn, int temp, int windLevel) {
        if (acOn) {
            currentAcTemp = temp;
            acTemp.setText(temp + "°");
            acWindLevel.setText(String.valueOf(windLevel));
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.VISIBLE);
            acWindLevel.setVisibility(View.VISIBLE);
        } else {
            acTemp.setText(rootView.getContext().getString(R.string.ac_off_label));
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.GONE);
            acWindLevel.setVisibility(View.GONE);
        }
    }
}
```

- [ ] **Step 3: 创建 StatusPage.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/StatusPage.java`：

```java
package com.bydlauncher.ui;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.model.VehicleStatus;

public class StatusPage {

    private final View rootView;

    private final VehicleDiagramView vehicleDiagram;
    private final TextView tireFL, tireFR, tireRL, tireRR;
    private final TextView[] gears;
    private final TextView tvSpeed, tvBattery;
    private final ProgressBar progressBattery;

    public StatusPage(View rootView) {
        this.rootView = rootView;

        vehicleDiagram = rootView.findViewById(R.id.vehicle_diagram);
        tireFL = rootView.findViewById(R.id.tire_fl);
        tireFR = rootView.findViewById(R.id.tire_fr);
        tireRL = rootView.findViewById(R.id.tire_rl);
        tireRR = rootView.findViewById(R.id.tire_rr);

        gears = new TextView[]{
                rootView.findViewById(R.id.gear_p),
                rootView.findViewById(R.id.gear_r),
                rootView.findViewById(R.id.gear_n),
                rootView.findViewById(R.id.gear_d)
        };

        tvSpeed = rootView.findViewById(R.id.tv_speed);
        tvBattery = rootView.findViewById(R.id.tv_battery);
        progressBattery = rootView.findViewById(R.id.progress_battery);
    }

    public void updateStatus(VehicleStatus s) {
        // 车辆图
        vehicleDiagram.setDoorStates(
                s.doorLeftFrontOpen, s.doorRightFrontOpen,
                s.doorLeftRearOpen, s.doorRightRearOpen,
                s.trunkOpen, s.hoodOpen);

        // 胎压（模拟数据）
        tireFL.setText("37\nPSI");
        tireFR.setText("36\nPSI");
        tireRL.setText("42\nPSI");
        tireRR.setText("43\nPSI");

        // 挡位（模拟：P 挡）
        int gearIndex = 0;
        for (int i = 0; i < gears.length; i++) {
            gears[i].setTextColor(ContextCompat.getColor(rootView.getContext(),
                    i == gearIndex ? R.color.gear_active : R.color.gear_inactive));
        }

        // 速度（模拟）
        tvSpeed.setText("0");

        // 电量
        int batteryVal = s.getBatteryValue();
        tvBattery.setText(s.getBatteryText());
        progressBattery.setProgress(batteryVal);
    }
}
```

- [ ] **Step 4: 创建 PlaceholderPage.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/ui/PlaceholderPage.java`：

```java
package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import com.bydlauncher.R;

public class PlaceholderPage {

    private final View rootView;
    private final TextView text;

    private static final String[] TAB_NAMES = {"Map", "Controls", "Apps", "Settings"};

    public PlaceholderPage(View rootView) {
        this.rootView = rootView;
        this.text = rootView.findViewById(R.id.placeholder_text);
    }

    public View getView() {
        return rootView;
    }

    public void showForTab(int tabIndex) {
        int nameIndex = tabIndex - 1;
        if (nameIndex >= 0 && nameIndex < TAB_NAMES.length) {
            text.setText(TAB_NAMES[nameIndex] + "\n" + rootView.getContext().getString(R.string.coming_soon));
        }
        rootView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        rootView.setVisibility(View.GONE);
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add -A && git commit -m "feat: create TopBar, NavBar, StatusPage, PlaceholderPage UI classes"
```

---

### Task 10: 重写 MainActivity.java + 更新 AndroidManifest.xml

**Files:**
- Modify: `java/com/bydlauncher/MainActivity.java`
- Modify: `AndroidManifest.xml`

- [ ] **Step 1: 更新 AndroidManifest.xml 追加权限**

在 `/Users/feng/work/byd/app/src/main/AndroidManifest.xml` 的 `<uses-permission>` 块末尾追加：

```xml
    <uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
```

- [ ] **Step 2: 重写 MainActivity.java**

写入 `/Users/feng/work/byd/app/src/main/java/com/bydlauncher/MainActivity.java`：

```java
package com.bydlauncher;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bydlauncher.api.BydVehicleManager;
import com.bydlauncher.model.VehicleStatus;
import com.bydlauncher.theme.ThemeManager;
import com.bydlauncher.ui.MusicCardView;
import com.bydlauncher.ui.NavBar;
import com.bydlauncher.ui.PlaceholderPage;
import com.bydlauncher.ui.StatusPage;
import com.bydlauncher.ui.TopBar;

public class MainActivity extends AppCompatActivity
        implements BydVehicleManager.VehicleStatusListener, NavBar.TabListener {

    private BydVehicleManager vehicleManager;

    private TopBar topBar;
    private NavBar navBar;
    private StatusPage statusPage;
    private PlaceholderPage placeholderPage;
    private MusicCardView musicCard;

    private View pageStatusView;
    private View pagePlaceholderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        vehicleManager = BydVehicleManager.getInstance(this);
        vehicleManager.setListener(this);

        pageStatusView = findViewById(R.id.page_status);
        pagePlaceholderView = findViewById(R.id.page_placeholder);

        topBar = new TopBar(findViewById(R.id.top_bar));
        navBar = new NavBar(findViewById(R.id.nav_bar), vehicleManager.getAcApi());
        navBar.setTabListener(this);

        statusPage = new StatusPage(pageStatusView);
        placeholderPage = new PlaceholderPage(pagePlaceholderView);
        musicCard = new MusicCardView(findViewById(R.id.music_card));
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        vehicleManager.startPolling();
        musicCard.refreshMediaState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        vehicleManager.stopPolling();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onTabSelected(int tabIndex) {
        if (tabIndex == 0) {
            pageStatusView.setVisibility(View.VISIBLE);
            placeholderPage.hide();
        } else {
            pageStatusView.setVisibility(View.GONE);
            placeholderPage.showForTab(tabIndex);
        }
    }

    @Override
    public void onStatusUpdated(VehicleStatus status) {
        runOnUiThread(() -> {
            statusPage.updateStatus(status);

            navBar.updateAcState(status.acOn, status.acTemp, status.acWindLevel);

            musicCard.refreshMediaState();
            topBar.updateMediaSource(musicCard.getMediaSourceName());
        });
    }

    @Override
    public void onBackPressed() {
        // Launcher 不响应返回键
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: rewrite MainActivity with tab navigation, status page, and media integration"
```

---

### Task 11: 清理旧文件 + 最终验证

**Files:**
- Delete: 已无需额外删除（Task 1 已处理）
- Verify: 所有 ID 引用一致性

- [ ] **Step 1: 验证所有布局 ID 引用**

```bash
cd /Users/feng/work/byd
echo "=== IDs in layouts ===" && grep -roh 'android:id="@+id/[^"]*"' app/src/main/res/layout/ | sort -u
echo "=== IDs in Java ===" && grep -roh 'R\.id\.[a-zA-Z_]*' app/src/main/java/ | sort -u
```

确认 Java 中引用的所有 ID 都在布局文件中定义。

- [ ] **Step 2: 验证文件结构完整**

```bash
find app/src/main -type f | sort
```

确认无遗漏文件。

- [ ] **Step 3: 最终提交**

```bash
git add -A && git commit -m "chore: V2 sub-project 1 complete - Kinex-inspired dashboard launcher"
```
