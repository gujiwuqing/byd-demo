# BYD Launcher UI 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 BYDLauncher 从三栏平铺布局重构为"双区桌面 + 底部 Dock + 弹出面板"架构

**Architecture:** 单 Activity 架构不变。主屏为 LinearLayout（时钟左区 + 车辆状态右区），底部固定 Dock 栏。空调面板和应用抽屉作为叠加层，用 View 动画从底部滑出，覆盖主内容区但不覆盖 Dock。

**Tech Stack:** Android SDK (Java), AndroidX AppCompat, Material Components, RecyclerView

**设计文档:** `docs/superpowers/specs/2026-06-28-launcher-ui-redesign.md`

---

## 文件结构总览

### 创建的文件
| 文件 | 职责 |
|------|------|
| `res/layout/view_home.xml` | 主屏内容：左区时钟 + 右区车辆状态卡片 |
| `res/layout/view_dock.xml` | 底部 Dock 栏：快捷操作 + 应用入口 |
| `res/layout/view_ac_panel.xml` | 空调控制弹出面板 |
| `res/layout/view_app_drawer.xml` | 应用抽屉弹出面板 |
| `res/drawable/bg_dock.xml` | Dock 栏背景 |
| `res/drawable/bg_dock_btn.xml` | Dock 按钮按压态 |
| `res/drawable/bg_panel.xml` | 弹出面板背景（顶部圆角） |
| `res/drawable/bg_drag_handle.xml` | 拖拽条 drawable |

### 修改的文件
| 文件 | 改动 |
|------|------|
| `res/layout/activity_main.xml` | 完全重写为 FrameLayout 叠加层结构 |
| `res/layout/item_app.xml` | 图标增大至 56dp，适配 6 列网格 |
| `res/values/colors.xml` | 更新卡片色值，新增 dock/panel/overlay 颜色 |
| `res/values/dimens.xml` | 更新间距/圆角，新增 dock/panel/时钟尺寸 |
| `res/values/strings.xml` | 新增"所有应用"、"空调控制"等字符串 |
| `res/values/styles.xml` | 重写样式，新增 DockButton/PanelTitle 等 |
| `java/.../MainActivity.java` | 完全重写 UI 逻辑 |
| `java/.../adapter/AppListAdapter.java` | 网格改为 6 列（调用处修改） |

### 删除的文件
| 文件 | 原因 |
|------|------|
| `res/layout/view_vehicle_status.xml` | 合并入 view_home.xml |
| `res/layout/view_ac_control.xml` | 替换为 view_ac_panel.xml |
| `res/layout/view_quick_switch.xml` | 功能移入 Dock 栏 |

### 保持不变的文件
- 所有 `api/` 目录下的 Java 文件
- `model/VehicleStatus.java`
- `AndroidManifest.xml`
- `res/drawable/bg_circle_btn.xml`、`progress_bar_battery.xml`
- `res/mipmap-anydpi-v26/ic_launcher.xml`、`drawable/ic_launcher_foreground.xml`

---

### Task 1: 更新资源文件（colors、dimens、strings）

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 更新 colors.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 主色调 -->
    <color name="bg_primary">#FF0D1117</color>
    <color name="bg_secondary">#FF161B22</color>
    <color name="bg_card">#FF161B22</color>
    <color name="bg_card_pressed">#FF252D3D</color>

    <!-- Dock / 面板 -->
    <color name="bg_dock">#E6161B22</color>
    <color name="bg_panel">#FF1A1F2E</color>
    <color name="bg_overlay">#80000000</color>
    <color name="divider_dock">#FF30363D</color>
    <color name="drag_handle">#FF484F58</color>

    <!-- 强调色 -->
    <color name="accent_blue">#FF00D4FF</color>
    <color name="accent_blue_dim">#8000D4FF</color>
    <color name="accent_green">#FF00E676</color>
    <color name="accent_red">#FFFF5252</color>
    <color name="accent_orange">#FFFFAB40</color>

    <!-- 文字颜色 -->
    <color name="text_primary">#FFF0F6FC</color>
    <color name="text_secondary">#FF8B949E</color>
    <color name="text_tertiary">#FF484F58</color>

    <!-- 边框 -->
    <color name="border_subtle">#FF30363D</color>

    <!-- 功能色 -->
    <color name="battery_high">#FF00E676</color>
    <color name="battery_mid">#FFFFAB40</color>
    <color name="battery_low">#FFFF5252</color>
    <color name="ac_on">#FF00D4FF</color>
    <color name="ac_off">#FF484F58</color>
    <color name="door_open">#FFFF5252</color>
    <color name="door_closed">#FF00E676</color>
    <color name="locked">#FF00E676</color>
    <color name="unlocked">#FFFFAB40</color>
</resources>
```

- [ ] **Step 2: 更新 dimens.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- 页面 -->
    <dimen name="page_padding">24dp</dimen>

    <!-- 卡片 -->
    <dimen name="card_radius">16dp</dimen>
    <dimen name="card_padding">20dp</dimen>
    <dimen name="card_margin">8dp</dimen>

    <!-- 时钟 -->
    <dimen name="text_clock">60sp</dimen>
    <dimen name="text_date">18sp</dimen>

    <!-- 车辆状态 -->
    <dimen name="text_value">28sp</dimen>
    <dimen name="text_label">14sp</dimen>

    <!-- Dock -->
    <dimen name="dock_height">64dp</dimen>
    <dimen name="dock_icon">24sp</dimen>
    <dimen name="dock_text">12sp</dimen>
    <dimen name="dock_btn_padding">8dp</dimen>

    <!-- 面板 -->
    <dimen name="panel_radius">16dp</dimen>
    <dimen name="panel_padding">24dp</dimen>
    <dimen name="drag_handle_width">40dp</dimen>
    <dimen name="drag_handle_height">4dp</dimen>
    <dimen name="text_panel_title">18sp</dimen>
    <dimen name="text_temp">40sp</dimen>

    <!-- 按钮 -->
    <dimen name="btn_radius">12dp</dimen>
    <dimen name="btn_size">48dp</dimen>
    <dimen name="btn_text">14sp</dimen>

    <!-- 应用网格 -->
    <dimen name="app_icon_size">56dp</dimen>
    <dimen name="app_name_size">12sp</dimen>

    <!-- 通用间距 -->
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">24dp</dimen>
</resources>
```

- [ ] **Step 3: 更新 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">BYD Launcher</string>

    <!-- 主屏 -->
    <string name="battery">电量</string>
    <string name="range">续航</string>
    <string name="mileage">里程</string>
    <string name="power">电源</string>
    <string name="outside_temp">室外</string>
    <string name="door_status">车门</string>
    <string name="all_closed">全部关闭</string>
    <string name="locked_text">已锁车</string>
    <string name="unlocked_text">已解锁</string>

    <!-- Dock -->
    <string name="dock_lock">锁车</string>
    <string name="dock_ac">空调</string>
    <string name="dock_cycle">循环</string>
    <string name="dock_trunk">后备箱</string>
    <string name="all_apps">所有应用</string>

    <!-- 空调面板 -->
    <string name="ac_control_title">空调控制</string>
    <string name="wind_level">风量</string>
    <string name="wind_mode">出风</string>
    <string name="cycle_mode">循环</string>
    <string name="ac_mode">模式</string>
    <string name="inner_cycle">内循环</string>
    <string name="outer_cycle">外循环</string>
    <string name="auto_mode">自动</string>
    <string name="manual_mode">手动</string>
    <string name="defrost">除雾</string>
    <string name="face">吹面</string>
    <string name="foot">吹脚</string>

    <string name="no_data">N/A</string>
</resources>
```

- [ ] **Step 4: 提交资源文件更新**

```bash
git add app/src/main/res/values/
git commit -m "refactor: update color/dimen/string resources for new layout"
```

---

### Task 2: 更新样式文件和创建新 drawable 资源

**Files:**
- Modify: `app/src/main/res/values/styles.xml`
- Create: `app/src/main/res/drawable/bg_dock.xml`
- Create: `app/src/main/res/drawable/bg_dock_btn.xml`
- Create: `app/src/main/res/drawable/bg_panel.xml`
- Create: `app/src/main/res/drawable/bg_drag_handle.xml`

- [ ] **Step 1: 重写 styles.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="Theme.MaterialComponents.NoActionBar">
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowBackground">@color/bg_primary</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowTranslucentStatus">true</item>
        <item name="android:windowTranslucentNavigation">true</item>
        <item name="colorPrimary">@color/accent_blue</item>
        <item name="colorPrimaryDark">@color/bg_primary</item>
        <item name="colorAccent">@color/accent_blue</item>
    </style>

    <!-- 车辆状态标签 -->
    <style name="StatusLabel">
        <item name="android:textColor">@color/text_secondary</item>
        <item name="android:textSize">@dimen/text_label</item>
    </style>

    <!-- 车辆状态数值 -->
    <style name="StatusValue">
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">@dimen/text_value</item>
        <item name="android:textStyle">bold</item>
    </style>

    <!-- Dock 按钮 -->
    <style name="DockButton">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">match_parent</item>
        <item name="android:layout_weight">1</item>
        <item name="android:background">@drawable/bg_dock_btn</item>
        <item name="android:gravity">center</item>
        <item name="android:orientation">vertical</item>
        <item name="android:padding">@dimen/dock_btn_padding</item>
    </style>

    <!-- 空调面板控制按钮 -->
    <style name="AcButton">
        <item name="android:layout_height">wrap_content</item>
        <item name="android:background">@drawable/bg_switch_btn</item>
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">@dimen/btn_text</item>
        <item name="android:gravity">center</item>
        <item name="android:paddingTop">10dp</item>
        <item name="android:paddingBottom">10dp</item>
        <item name="android:paddingStart">4dp</item>
        <item name="android:paddingEnd">4dp</item>
    </style>

    <!-- 面板标题 -->
    <style name="PanelTitle">
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">@dimen/text_panel_title</item>
        <item name="android:textStyle">bold</item>
    </style>

    <!-- 面板分区标签 -->
    <style name="PanelSectionLabel">
        <item name="android:textColor">@color/text_secondary</item>
        <item name="android:textSize">@dimen/text_label</item>
        <item name="android:layout_marginBottom">8dp</item>
    </style>
</resources>
```

- [ ] **Step 2: 创建 bg_dock.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_dock" />
    <stroke android:width="1dp" android:color="@color/border_subtle" />
</shape>
```

- [ ] **Step 3: 创建 bg_dock_btn.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_pressed="true">
        <shape android:shape="rectangle">
            <solid android:color="#22FFFFFF" />
            <corners android:radius="@dimen/btn_radius" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@android:color/transparent" />
        </shape>
    </item>
</selector>
```

- [ ] **Step 4: 创建 bg_panel.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_panel" />
    <corners
        android:topLeftRadius="@dimen/panel_radius"
        android:topRightRadius="@dimen/panel_radius" />
    <stroke android:width="1dp" android:color="@color/border_subtle" />
</shape>
```

- [ ] **Step 5: 创建 bg_drag_handle.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/drag_handle" />
    <corners android:radius="2dp" />
    <size
        android:width="@dimen/drag_handle_width"
        android:height="@dimen/drag_handle_height" />
</shape>
```

- [ ] **Step 6: 提交样式和 drawable 文件**

```bash
git add app/src/main/res/values/styles.xml app/src/main/res/drawable/
git commit -m "refactor: add new styles and drawable resources for dock/panel"
```

---

### Task 3: 创建主屏布局 view_home.xml

**Files:**
- Create: `app/src/main/res/layout/view_home.xml`

- [ ] **Step 1: 创建 view_home.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:padding="@dimen/page_padding">

    <!-- 左区：时钟（40%） -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="2"
        android:orientation="vertical"
        android:paddingStart="8dp"
        android:paddingTop="16dp">

        <TextClock
            android:id="@+id/clock_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format24Hour="HH:mm"
            android:format12Hour="hh:mm"
            android:textColor="@color/text_primary"
            android:textSize="@dimen/text_clock"
            android:textStyle="bold"
            android:fontFamily="sans-serif-light"
            android:letterSpacing="0.05" />

        <TextClock
            android:id="@+id/clock_date"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format24Hour="MM月dd日 EEEE"
            android:format12Hour="MM月dd日 EEEE"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/text_date"
            android:layout_marginTop="-4dp" />
    </LinearLayout>

    <!-- 右区：车辆状态卡片（60%） -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="3"
        android:layout_marginStart="@dimen/spacing_lg"
        android:background="@drawable/bg_card"
        android:orientation="vertical"
        android:padding="@dimen/card_padding"
        android:gravity="center_vertical">

        <!-- 第1行：电量 + 续航 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="4dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/battery"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_battery"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    style="@style/StatusValue" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/range"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_range"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    style="@style/StatusValue" />
            </LinearLayout>
        </LinearLayout>

        <!-- 电量进度条 -->
        <ProgressBar
            android:id="@+id/progress_battery"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="match_parent"
            android:layout_height="6dp"
            android:layout_marginBottom="@dimen/spacing_md"
            android:max="100"
            android:progress="0"
            android:progressDrawable="@drawable/progress_bar_battery" />

        <!-- 第2行：里程 + 电源 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="@dimen/spacing_md">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/mileage"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_mileage"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    style="@style/StatusValue" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/power"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_power"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    android:textColor="@color/accent_blue"
                    android:textSize="@dimen/text_value"
                    android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>

        <!-- 第3行：室外温度 + 车门 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="@dimen/spacing_sm">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/outside_temp"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_outside_temp"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    style="@style/StatusValue" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/door_status"
                    style="@style/StatusLabel" />

                <TextView
                    android:id="@+id/tv_door_status"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="--"
                    style="@style/StatusValue" />
            </LinearLayout>
        </LinearLayout>

        <!-- 第4行：锁车状态 -->
        <TextView
            android:id="@+id/tv_lock_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="--"
            android:textSize="@dimen/text_label"
            android:textColor="@color/locked" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/res/layout/view_home.xml
git commit -m "feat: create home layout with clock and vehicle status card"
```

---

### Task 4: 创建底部 Dock 栏 view_dock.xml

**Files:**
- Create: `app/src/main/res/layout/view_dock.xml`

- [ ] **Step 1: 创建 view_dock.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="@dimen/dock_height"
    android:background="@drawable/bg_dock"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="@dimen/spacing_lg"
    android:paddingEnd="@dimen/spacing_lg">

    <!-- 快捷操作区 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="7"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <!-- 锁车 -->
        <LinearLayout
            android:id="@+id/dock_lock"
            style="@style/DockButton">
            <TextView
                android:id="@+id/dock_lock_icon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="🔒"
                android:textSize="@dimen/dock_icon" />
            <TextView
                android:id="@+id/dock_lock_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/dock_lock"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/dock_text" />
        </LinearLayout>

        <!-- 空调 -->
        <LinearLayout
            android:id="@+id/dock_ac"
            style="@style/DockButton">
            <TextView
                android:id="@+id/dock_ac_icon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="❄️"
                android:textSize="@dimen/dock_icon" />
            <TextView
                android:id="@+id/dock_ac_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/dock_ac"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/dock_text" />
        </LinearLayout>

        <!-- 循环 -->
        <LinearLayout
            android:id="@+id/dock_cycle"
            style="@style/DockButton">
            <TextView
                android:id="@+id/dock_cycle_icon"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="🔄"
                android:textSize="@dimen/dock_icon" />
            <TextView
                android:id="@+id/dock_cycle_text"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/dock_cycle"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/dock_text" />
        </LinearLayout>

        <!-- 后备箱 -->
        <LinearLayout
            android:id="@+id/dock_trunk"
            style="@style/DockButton">
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="📦"
                android:textSize="@dimen/dock_icon" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/dock_trunk"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/dock_text" />
        </LinearLayout>
    </LinearLayout>

    <!-- 分隔线 -->
    <View
        android:layout_width="1dp"
        android:layout_height="32dp"
        android:background="@color/divider_dock"
        android:layout_marginStart="@dimen/spacing_sm"
        android:layout_marginEnd="@dimen/spacing_sm" />

    <!-- 所有应用入口 -->
    <LinearLayout
        android:id="@+id/dock_all_apps"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="3"
        android:background="@drawable/bg_dock_btn"
        android:gravity="center"
        android:orientation="vertical"
        android:padding="@dimen/dock_btn_padding">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⬆"
            android:textSize="@dimen/dock_icon"
            android:textColor="@color/text_secondary" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/all_apps"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/dock_text" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/res/layout/view_dock.xml
git commit -m "feat: create bottom dock bar layout"
```

---

### Task 5: 创建空调控制面板 view_ac_panel.xml

**Files:**
- Create: `app/src/main/res/layout/view_ac_panel.xml`

- [ ] **Step 1: 创建 view_ac_panel.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/ac_panel"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    android:background="@drawable/bg_panel"
    android:orientation="vertical"
    android:padding="@dimen/panel_padding"
    android:visibility="gone">

    <!-- 拖拽条 -->
    <View
        android:layout_width="@dimen/drag_handle_width"
        android:layout_height="@dimen/drag_handle_height"
        android:layout_gravity="center_horizontal"
        android:layout_marginBottom="@dimen/spacing_md"
        android:background="@drawable/bg_drag_handle" />

    <!-- 标题行 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="@dimen/spacing_lg">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/ac_control_title"
            style="@style/PanelTitle" />

        <TextView
            android:id="@+id/btn_ac_power"
            android:layout_width="56dp"
            android:layout_height="32dp"
            android:background="@drawable/bg_ac_btn"
            android:gravity="center"
            android:text="OFF"
            android:textColor="@color/ac_off"
            android:textSize="@dimen/btn_text"
            android:textStyle="bold" />
    </LinearLayout>

    <!-- 温度控制区 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:layout_marginBottom="@dimen/spacing_lg">

        <TextView
            android:id="@+id/btn_temp_down"
            android:layout_width="@dimen/btn_size"
            android:layout_height="@dimen/btn_size"
            android:background="@drawable/bg_circle_btn"
            android:gravity="center"
            android:text="−"
            android:textColor="@color/accent_blue"
            android:textSize="22sp" />

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:orientation="vertical"
            android:layout_marginStart="32dp"
            android:layout_marginEnd="32dp">

            <TextView
                android:id="@+id/tv_ac_temp"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="25°C"
                android:textColor="@color/accent_blue"
                android:textSize="@dimen/text_temp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tv_ac_status"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="已关闭"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/text_label" />
        </LinearLayout>

        <TextView
            android:id="@+id/btn_temp_up"
            android:layout_width="@dimen/btn_size"
            android:layout_height="@dimen/btn_size"
            android:background="@drawable/bg_circle_btn"
            android:gravity="center"
            android:text="+"
            android:textColor="@color/accent_blue"
            android:textSize="22sp" />
    </LinearLayout>

    <!-- 风量 -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/wind_level"
        style="@style/PanelSectionLabel" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="@dimen/spacing_md">

        <TextView android:id="@+id/wind_0" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="0" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_1" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="1" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_2" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="2" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_3" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="3" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_4" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="4" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_5" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="5" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_6" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="6" android:layout_marginEnd="3dp" />
        <TextView android:id="@+id/wind_7" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="7" />
    </LinearLayout>

    <!-- 出风 + 循环 + 模式 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- 出风模式 -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="3"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_md">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/wind_mode"
                style="@style/PanelSectionLabel" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content">
                <TextView android:id="@+id/btn_mode_face" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/face" android:layout_marginEnd="3dp" />
                <TextView android:id="@+id/btn_mode_foot" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/foot" android:layout_marginEnd="3dp" />
                <TextView android:id="@+id/btn_mode_defrost" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/defrost" />
            </LinearLayout>
        </LinearLayout>

        <!-- 循环模式 -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="2"
            android:orientation="vertical"
            android:layout_marginEnd="@dimen/spacing_md">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/cycle_mode"
                style="@style/PanelSectionLabel" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content">
                <TextView android:id="@+id/btn_cycle_inner" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/inner_cycle" android:layout_marginEnd="3dp" />
                <TextView android:id="@+id/btn_cycle_outer" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/outer_cycle" />
            </LinearLayout>
        </LinearLayout>

        <!-- 控制模式 -->
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="2"
            android:orientation="vertical">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/ac_mode"
                style="@style/PanelSectionLabel" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content">
                <TextView android:id="@+id/btn_mode_auto" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/auto_mode" android:layout_marginEnd="3dp" />
                <TextView android:id="@+id/btn_mode_manual" style="@style/AcButton" android:layout_width="0dp" android:layout_weight="1" android:text="@string/manual_mode" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/res/layout/view_ac_panel.xml
git commit -m "feat: create AC control slide-up panel layout"
```

---

### Task 6: 创建应用抽屉 view_app_drawer.xml 并更新 item_app.xml

**Files:**
- Create: `app/src/main/res/layout/view_app_drawer.xml`
- Modify: `app/src/main/res/layout/item_app.xml`

- [ ] **Step 1: 创建 view_app_drawer.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/app_drawer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_panel"
    android:orientation="vertical"
    android:padding="@dimen/panel_padding"
    android:visibility="gone">

    <!-- 拖拽条 -->
    <View
        android:layout_width="@dimen/drag_handle_width"
        android:layout_height="@dimen/drag_handle_height"
        android:layout_gravity="center_horizontal"
        android:layout_marginBottom="@dimen/spacing_md"
        android:background="@drawable/bg_drag_handle" />

    <!-- 标题 -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/all_apps"
        android:layout_marginBottom="@dimen/spacing_md"
        style="@style/PanelTitle" />

    <!-- 应用网格 -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/app_grid"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false" />
</LinearLayout>
```

- [ ] **Step 2: 更新 item_app.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_app_item"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="12dp">

    <ImageView
        android:id="@+id/app_icon"
        android:layout_width="@dimen/app_icon_size"
        android:layout_height="@dimen/app_icon_size"
        android:scaleType="fitCenter" />

    <TextView
        android:id="@+id/app_name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:ellipsize="end"
        android:gravity="center"
        android:maxLines="1"
        android:textColor="@color/text_secondary"
        android:textSize="@dimen/app_name_size" />
</LinearLayout>
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/res/layout/view_app_drawer.xml app/src/main/res/layout/item_app.xml
git commit -m "feat: create app drawer layout and update app item size"
```

---

### Task 7: 重写 activity_main.xml 主容器

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: 重写 activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_primary"
    android:orientation="vertical">

    <!-- 主内容区（可被面板覆盖） -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- 主屏：时钟 + 车辆状态 -->
        <include
            layout="@layout/view_home"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <!-- 半透明遮罩层 -->
        <View
            android:id="@+id/overlay_mask"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="@color/bg_overlay"
            android:visibility="gone" />

        <!-- 空调控制面板（底部对齐，半屏） -->
        <include layout="@layout/view_ac_panel" />

        <!-- 应用抽屉（全屏） -->
        <include layout="@layout/view_app_drawer" />
    </FrameLayout>

    <!-- 底部 Dock 栏（始终可见） -->
    <include
        layout="@layout/view_dock"
        android:layout_width="match_parent"
        android:layout_height="@dimen/dock_height" />
</LinearLayout>
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "refactor: rewrite activity_main as layered container with dock"
```

---

### Task 8: 重写 MainActivity.java

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/MainActivity.java`

- [ ] **Step 1: 重写 MainActivity.java 完整代码**

```java
package com.bydlauncher;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bydlauncher.adapter.AppListAdapter;
import com.bydlauncher.api.BydAcApi;
import com.bydlauncher.api.BydVehicleManager;
import com.bydlauncher.model.VehicleStatus;

public class MainActivity extends AppCompatActivity implements BydVehicleManager.VehicleStatusListener {

    private static final int ANIM_DURATION = 300;

    private BydVehicleManager vehicleManager;

    // Home views
    private TextView tvBattery, tvRange, tvMileage, tvPower;
    private TextView tvOutsideTemp, tvDoorStatus, tvLockStatus;
    private ProgressBar progressBattery;

    // Dock views
    private TextView dockLockIcon, dockLockText;
    private TextView dockAcIcon, dockAcText;
    private TextView dockCycleText;

    // Overlay
    private View overlayMask;

    // AC Panel views
    private View acPanel;
    private TextView tvAcTemp, tvAcStatus, btnAcPower;
    private TextView btnTempUp, btnTempDown;
    private TextView[] windLevelBtns;
    private TextView btnModeFace, btnModeFoot, btnModeDefrost;
    private TextView btnCycleInner, btnCycleOuter;
    private TextView btnModeAuto, btnModeManual;

    // App Drawer
    private View appDrawer;

    private int currentAcTemp = 25;
    private boolean acPanelVisible = false;
    private boolean appDrawerVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        vehicleManager = BydVehicleManager.getInstance(this);
        vehicleManager.setListener(this);

        initHomeViews();
        initDock();
        initAcPanel();
        initAppDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        vehicleManager.startPolling();
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

    // ========== Home ==========

    private void initHomeViews() {
        tvBattery = findViewById(R.id.tv_battery);
        tvRange = findViewById(R.id.tv_range);
        tvMileage = findViewById(R.id.tv_mileage);
        tvPower = findViewById(R.id.tv_power);
        tvOutsideTemp = findViewById(R.id.tv_outside_temp);
        tvDoorStatus = findViewById(R.id.tv_door_status);
        tvLockStatus = findViewById(R.id.tv_lock_status);
        progressBattery = findViewById(R.id.progress_battery);
        overlayMask = findViewById(R.id.overlay_mask);

        overlayMask.setOnClickListener(v -> closeAllPanels());
    }

    // ========== Dock ==========

    private void initDock() {
        dockLockIcon = findViewById(R.id.dock_lock_icon);
        dockLockText = findViewById(R.id.dock_lock_text);
        dockAcIcon = findViewById(R.id.dock_ac_icon);
        dockAcText = findViewById(R.id.dock_ac_text);
        dockCycleText = findViewById(R.id.dock_cycle_text);

        findViewById(R.id.dock_lock).setOnClickListener(v ->
                vehicleManager.getBodyworkApi().toggleLock());

        findViewById(R.id.dock_ac).setOnClickListener(v -> toggleAcPanel());

        findViewById(R.id.dock_cycle).setOnClickListener(v ->
                vehicleManager.getAcApi().toggleCycleMode());

        findViewById(R.id.dock_all_apps).setOnClickListener(v -> toggleAppDrawer());
    }

    // ========== AC Panel ==========

    private void initAcPanel() {
        acPanel = findViewById(R.id.ac_panel);
        tvAcTemp = findViewById(R.id.tv_ac_temp);
        tvAcStatus = findViewById(R.id.tv_ac_status);
        btnAcPower = findViewById(R.id.btn_ac_power);
        btnTempUp = findViewById(R.id.btn_temp_up);
        btnTempDown = findViewById(R.id.btn_temp_down);

        windLevelBtns = new TextView[]{
                findViewById(R.id.wind_0), findViewById(R.id.wind_1),
                findViewById(R.id.wind_2), findViewById(R.id.wind_3),
                findViewById(R.id.wind_4), findViewById(R.id.wind_5),
                findViewById(R.id.wind_6), findViewById(R.id.wind_7)
        };

        btnModeFace = findViewById(R.id.btn_mode_face);
        btnModeFoot = findViewById(R.id.btn_mode_foot);
        btnModeDefrost = findViewById(R.id.btn_mode_defrost);
        btnCycleInner = findViewById(R.id.btn_cycle_inner);
        btnCycleOuter = findViewById(R.id.btn_cycle_outer);
        btnModeAuto = findViewById(R.id.btn_mode_auto);
        btnModeManual = findViewById(R.id.btn_mode_manual);

        BydAcApi acApi = vehicleManager.getAcApi();

        btnAcPower.setOnClickListener(v -> acApi.toggle());

        btnTempUp.setOnClickListener(v -> {
            currentAcTemp = Math.min(33, currentAcTemp + 1);
            acApi.setMainTemp(currentAcTemp);
            tvAcTemp.setText(currentAcTemp + "°C");
        });

        btnTempDown.setOnClickListener(v -> {
            currentAcTemp = Math.max(17, currentAcTemp - 1);
            acApi.setMainTemp(currentAcTemp);
            tvAcTemp.setText(currentAcTemp + "°C");
        });

        for (int i = 0; i < windLevelBtns.length; i++) {
            final int level = i;
            windLevelBtns[i].setOnClickListener(v -> {
                acApi.setWindLevel(level);
                updateWindLevelUI(level);
            });
        }

        btnModeFace.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FACE); updateWindModeUI(BydAcApi.WIND_MODE_FACE); });
        btnModeFoot.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FOOT); updateWindModeUI(BydAcApi.WIND_MODE_FOOT); });
        btnModeDefrost.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_DEFROST); updateWindModeUI(BydAcApi.WIND_MODE_DEFROST); });

        btnCycleInner.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_INNER); updateCycleModeUI(BydAcApi.CYCLE_INNER); });
        btnCycleOuter.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_OUTER); updateCycleModeUI(BydAcApi.CYCLE_OUTER); });

        btnModeAuto.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_AUTO); updateControlModeUI(BydAcApi.MODE_AUTO); });
        btnModeManual.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_MANUAL); updateControlModeUI(BydAcApi.MODE_MANUAL); });
    }

    private void toggleAcPanel() {
        if (acPanelVisible) {
            hideAcPanel();
        } else {
            if (appDrawerVisible) hideAppDrawer();
            showAcPanel();
        }
    }

    private void showAcPanel() {
        acPanel.setVisibility(View.VISIBLE);
        acPanel.post(() -> {
            acPanel.setTranslationY(acPanel.getHeight());
            acPanel.animate()
                    .translationY(0)
                    .setDuration(ANIM_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
        showOverlay();
        acPanelVisible = true;
    }

    private void hideAcPanel() {
        acPanel.animate()
                .translationY(acPanel.getHeight())
                .setDuration(ANIM_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> acPanel.setVisibility(View.GONE))
                .start();
        hideOverlay();
        acPanelVisible = false;
    }

    // ========== App Drawer ==========

    private void initAppDrawer() {
        appDrawer = findViewById(R.id.app_drawer);
        RecyclerView appGrid = findViewById(R.id.app_grid);
        appGrid.setLayoutManager(new GridLayoutManager(this, 6));
        appGrid.setAdapter(new AppListAdapter(this));
    }

    private void toggleAppDrawer() {
        if (appDrawerVisible) {
            hideAppDrawer();
        } else {
            if (acPanelVisible) hideAcPanel();
            showAppDrawer();
        }
    }

    private void showAppDrawer() {
        appDrawer.setVisibility(View.VISIBLE);
        appDrawer.post(() -> {
            appDrawer.setTranslationY(appDrawer.getHeight());
            appDrawer.animate()
                    .translationY(0)
                    .setDuration(ANIM_DURATION)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
        showOverlay();
        appDrawerVisible = true;
    }

    private void hideAppDrawer() {
        appDrawer.animate()
                .translationY(appDrawer.getHeight())
                .setDuration(ANIM_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> appDrawer.setVisibility(View.GONE))
                .start();
        hideOverlay();
        appDrawerVisible = false;
    }

    // ========== Overlay ==========

    private void showOverlay() {
        overlayMask.setVisibility(View.VISIBLE);
        overlayMask.setAlpha(0f);
        overlayMask.animate().alpha(1f).setDuration(200).start();
    }

    private void hideOverlay() {
        overlayMask.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> overlayMask.setVisibility(View.GONE))
                .start();
    }

    private void closeAllPanels() {
        if (acPanelVisible) hideAcPanel();
        if (appDrawerVisible) hideAppDrawer();
    }

    // ========== Status Updates ==========

    @Override
    public void onStatusUpdated(VehicleStatus status) {
        runOnUiThread(() -> updateUI(status));
    }

    private void updateUI(VehicleStatus s) {
        // 车辆状态卡片
        tvBattery.setText(s.getBatteryText());
        progressBattery.setProgress(s.getBatteryValue());
        tvRange.setText(s.getEvMileageText());
        tvMileage.setText(s.getTotalMileageText());
        tvPower.setText(s.powerLevelText);
        tvOutsideTemp.setText(s.getOutsideTempText());

        // 车门
        if (s.hasAnyDoorOpen()) {
            StringBuilder doors = new StringBuilder();
            if (s.doorLeftFrontOpen) doors.append("左前 ");
            if (s.doorRightFrontOpen) doors.append("右前 ");
            if (s.doorLeftRearOpen) doors.append("左后 ");
            if (s.doorRightRearOpen) doors.append("右后 ");
            if (s.trunkOpen) doors.append("后备箱 ");
            if (s.hoodOpen) doors.append("引擎盖 ");
            tvDoorStatus.setText(doors.toString().trim());
            tvDoorStatus.setTextColor(ContextCompat.getColor(this, R.color.door_open));
        } else {
            tvDoorStatus.setText(R.string.all_closed);
            tvDoorStatus.setTextColor(ContextCompat.getColor(this, R.color.door_closed));
        }

        // 锁车
        if (s.isLocked) {
            tvLockStatus.setText("🔒 " + getString(R.string.locked_text));
            tvLockStatus.setTextColor(ContextCompat.getColor(this, R.color.locked));
            dockLockIcon.setText("🔒");
            dockLockText.setText(R.string.locked_text);
        } else {
            tvLockStatus.setText("🔓 " + getString(R.string.unlocked_text));
            tvLockStatus.setTextColor(ContextCompat.getColor(this, R.color.unlocked));
            dockLockIcon.setText("🔓");
            dockLockText.setText(R.string.unlocked_text);
        }

        // 空调温度
        if (s.acTemp > 0 && s.acTemp < 100) {
            currentAcTemp = s.acTemp;
            tvAcTemp.setText(s.getAcTempText());
        }

        // 空调状态
        if (s.acOn) {
            btnAcPower.setText("ON");
            btnAcPower.setTextColor(ContextCompat.getColor(this, R.color.ac_on));
            tvAcStatus.setText(s.getAcModeText() + " · " + s.getCycleModeText());
            dockAcIcon.setText("❄️");
            dockAcText.setText("空调 ON");
            dockAcText.setTextColor(ContextCompat.getColor(this, R.color.accent_blue));
        } else {
            btnAcPower.setText("OFF");
            btnAcPower.setTextColor(ContextCompat.getColor(this, R.color.ac_off));
            tvAcStatus.setText("已关闭");
            dockAcIcon.setText("⬜");
            dockAcText.setText("空调");
            dockAcText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // Dock 循环状态
        dockCycleText.setText(s.getCycleModeText());

        // AC 面板按钮状态
        updateWindLevelUI(s.acWindLevel);
        updateWindModeUI(s.acWindMode);
        updateCycleModeUI(s.acCycleMode);
        updateControlModeUI(s.acControlMode);
    }

    // ========== AC UI Helpers ==========

    private void updateWindLevelUI(int level) {
        for (int i = 0; i < windLevelBtns.length; i++) {
            windLevelBtns[i].setSelected(i == level);
            windLevelBtns[i].setTextColor(ContextCompat.getColor(this,
                    i == level ? R.color.accent_blue : R.color.text_secondary));
        }
    }

    private void updateWindModeUI(int mode) {
        btnModeFace.setSelected(mode == BydAcApi.WIND_MODE_FACE);
        btnModeFoot.setSelected(mode == BydAcApi.WIND_MODE_FOOT);
        btnModeDefrost.setSelected(mode == BydAcApi.WIND_MODE_DEFROST);
        btnModeFace.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.WIND_MODE_FACE ? R.color.accent_blue : R.color.text_secondary));
        btnModeFoot.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.WIND_MODE_FOOT ? R.color.accent_blue : R.color.text_secondary));
        btnModeDefrost.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.WIND_MODE_DEFROST ? R.color.accent_blue : R.color.text_secondary));
    }

    private void updateCycleModeUI(int mode) {
        btnCycleInner.setSelected(mode == BydAcApi.CYCLE_INNER);
        btnCycleOuter.setSelected(mode == BydAcApi.CYCLE_OUTER);
        btnCycleInner.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.CYCLE_INNER ? R.color.accent_blue : R.color.text_secondary));
        btnCycleOuter.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.CYCLE_OUTER ? R.color.accent_blue : R.color.text_secondary));
    }

    private void updateControlModeUI(int mode) {
        btnModeAuto.setSelected(mode == BydAcApi.MODE_AUTO);
        btnModeManual.setSelected(mode == BydAcApi.MODE_MANUAL);
        btnModeAuto.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.MODE_AUTO ? R.color.accent_blue : R.color.text_secondary));
        btnModeManual.setTextColor(ContextCompat.getColor(this, mode == BydAcApi.MODE_MANUAL ? R.color.accent_blue : R.color.text_secondary));
    }

    @Override
    public void onBackPressed() {
        if (acPanelVisible || appDrawerVisible) {
            closeAllPanels();
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "refactor: rewrite MainActivity with dock, slide-up panels, and app drawer"
```

---

### Task 9: 删除旧布局文件并清理无用 drawable

**Files:**
- Delete: `app/src/main/res/layout/view_vehicle_status.xml`
- Delete: `app/src/main/res/layout/view_ac_control.xml`
- Delete: `app/src/main/res/layout/view_quick_switch.xml`

- [ ] **Step 1: 删除旧布局文件**

```bash
rm app/src/main/res/layout/view_vehicle_status.xml
rm app/src/main/res/layout/view_ac_control.xml
rm app/src/main/res/layout/view_quick_switch.xml
```

- [ ] **Step 2: 提交最终清理**

```bash
git add -A
git commit -m "cleanup: remove old three-column layout files"
```
