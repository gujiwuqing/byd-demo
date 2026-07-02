# 迪UI 完整性重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对迪UI(BYD 车机 Launcher)全部页面做完整性重构 —— 补矢量图标/插图/数据可视化,整合零散信息,统一深色玻璃质感。

**Architecture:** 延续现有单 Activity + XML 布局 + Java 自定义 View 架构(方案 A)。新增设计 token、矢量资源包、可视化 View 组件;页面级重构 Status/TopBar/Controls/NavBar/Settings/Apps。不引入 Compose,不动 API 层。

**Tech Stack:** Android (Java)、XML drawable/layout、Canvas 自定义 View、ViewBinding/findViewById。API 26+。

**验证策略:** 本项目无测试套件,且 CLAUDE.md 禁止自动运行构建命令。每个任务完成后**由用户手动** `./gradlew assembleDebug` 编译 + ADB 装车机验证。commit 步骤由 agent 执行(git 允许)。每个任务结束有 commit 步骤。

**对应 spec:** `docs/superpowers/specs/2026-07-02-ui-refactor-design.md`

---

## 文件结构

### 新增
- `app/src/main/res/drawable-anydpi-v26/ic_*.xml`(17 个矢量图标)
- `app/src/main/res/drawable/bg_ambient_glass.xml`(氛围背景)
- `app/src/main/java/com/diui/launcher/ui/GaugeView.java`(环形进度)
- `app/src/main/java/com/diui/launcher/ui/WindLevelView.java`(风量条)

### 修改
- `app/src/main/res/values/colors.xml`、`dimens.xml`、`styles.xml`(补 token)
- `app/src/main/java/com/diui/launcher/ui/VehicleDiagramView.java`(扩展胎压)
- `app/src/main/res/layout/page_status.xml` + `ui/StatusPage.java`
- `app/src/main/res/layout/view_top_bar.xml` + `ui/TopBar.java` + `MainActivity.java`
- `app/src/main/res/layout/page_controls.xml` + `ui/ControlsPage.java`
- `app/src/main/res/layout/view_nav_bar.xml`
- `app/src/main/res/layout/page_settings.xml` + `page_apps.xml`

---

## 阶段 P1:基础设施(设计 token + 矢量图标包 + 可视化组件)

### Task 1: 补充设计 token

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/styles.xml`

- [ ] **Step 1: 在 `colors.xml` 末尾 `</resources>` 前追加语义色**

```xml
    <!-- P1 重构 token -->
    <color name="surface_glass">#1E0E1A2A</color>
    <color name="surface_glass_highlight">#33FFFFFF</color>
    <color name="accent_cool">#FF00C8F0</color>
    <color name="accent_warn">#FFFF7A00</color>
    <color name="gauge_track">#14FFFFFF</color>
    <color name="gauge_progress">#FF00C8F0</color>
    <color name="vehicle_body_stroke">#FF3D4A5A</color>
```
(注:`vehicle_body`/`vehicle_window`/`vehicle_wheel` 等若已存在则不重复;仅追加缺失项。)

- [ ] **Step 2: 在 `dimens.xml` 末尾 `</resources>` 前追加 token**

```xml
    <!-- P1 重构 token -->
    <dimen name="radius_card_lg">16dp</dimen>
    <dimen name="radius_card_sm">12dp</dimen>
    <dimen name="spacing_token_4">4dp</dimen>
    <dimen name="spacing_token_8">8dp</dimen>
    <dimen name="spacing_token_12">12dp</dimen>
    <dimen name="spacing_token_16">16dp</dimen>
    <dimen name="spacing_token_24">24dp</dimen>
    <dimen name="gauge_stroke">6dp</dimen>
```

- [ ] **Step 3: 在 `styles.xml` 末尾 `</resources>` 前追加复用样式**

```xml
    <!-- P1 重构复用样式 -->
    <style name="Card.Glass" parent="">
        <item name="android:background">@drawable/bg_card_glass</item>
        <item name="android:padding">@dimen/card_padding</item>
    </style>
    <style name="DataLabel.Primary">
        <item name="android:textColor">@color/text_primary</item>
        <item name="android:textSize">@dimen/data_value_size</item>
        <item name="android:textStyle">bold</item>
    </style>
    <style name="DataLabel.Secondary">
        <item name="android:textColor">@color/text_secondary</item>
        <item name="android:textSize">@dimen/data_label_size</item>
    </style>
```

- [ ] **Step 4: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/dimens.xml app/src/main/res/values/styles.xml
git commit -m "refactor(ui): 补充 P1 设计 token 语义色/间距/样式"
```

---

### Task 2: 新增氛围背景 drawable

**Files:**
- Create: `app/src/main/res/drawable/bg_ambient_glass.xml`

- [ ] **Step 1: 创建文件**

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 底色 -->
    <item android:drawable="@color/bg_primary" />
    <!-- 顶部径向高光,模拟玻璃环境光 -->
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:type="radial"
                android:gradientRadius="720"
                android:centerX="0.5"
                android:centerY="0.0">
                <item android:offset="0.0" android:color="#22264A6E" />
                <item android:offset="1.0" android:color="#00000000" />
            </gradient>
        </shape>
    </item>
</layer-list>
```

- [ ] **Step 2: 应用到 `activity_main.xml` 根容器**

将根 `FrameLayout` 的背景:
```xml
android:background="@color/bg_primary"
```
改为:
```xml
android:background="@drawable/bg_ambient_glass"
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/bg_ambient_glass.xml app/src/main/res/layout/activity_main.xml
git commit -m "feat(ui): 新增深色玻璃氛围背景 bg_ambient_glass 并应用到根容器"
```

---

### Task 3: 新增 17 个矢量图标(替换 emoji)

**Files:**
- Create: `app/src/main/res/drawable-anydpi-v26/ic_tab_home.xml`、`ic_ac.xml`、`ic_apps.xml`、`ic_settings.xml`、`ic_cycle.xml`、`ic_window.xml`、`ic_seat.xml`、`ic_wind.xml`、`ic_battery.xml`、`ic_signal.xml`、`ic_bluetooth.xml`、`ic_media.xml`、`ic_map.xml`、`ic_music.xml`、`ic_video.xml`、`ic_phone.xml`、`ic_more.xml`、`ic_plus.xml`、`ic_minus.xml`

所有图标统一 24x24 viewport,`tint` 由引用处 `app:tint` 控制。用简洁几何 path 自绘。

- [ ] **Step 1: 创建 `ic_tab_home.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M12,3 L3,11 L5,11 L5,21 L10,21 L10,15 L14,15 L14,21 L19,21 L19,11 L21,11 Z" />
</vector>
```

- [ ] **Step 2: 创建 `ic_ac.xml`(雪花)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M12,2 L12,22 M2,12 L22,12 M5,5 L19,19 M19,5 L5,19" />
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M9,4 L12,6 L15,4 M9,20 L12,18 L15,20 M4,9 L6,12 L4,15 M20,9 L18,12 L20,15" />
</vector>
```

- [ ] **Step 3: 创建 `ic_apps.xml`(圆点网格)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M5,5h4v4h-4zM10,5h4v4h-4zM15,5h4v4h-4zM5,10h4v4h-4zM10,10h4v4h-4zM15,10h4v4h-4zM5,15h4v4h-4zM10,15h4v4h-4zM15,15h4v4h-4z" />
</vector>
```

- [ ] **Step 4: 创建 `ic_settings.xml`(齿轮)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M12,8a4,4 0 1,0 0,8a4,4 0 1,0 0,-8 M12,1 L13.5,4 L16.5,3 L17,6 L20,6.5 L19,9.5 L22,12 L19,14.5 L20,17.5 L17,18 L16.5,21 L13.5,20 L12,23 L10.5,20 L7.5,21 L7,18 L4,17.5 L5,14.5 L2,12 L5,9.5 L4,6.5 L7,6 L7.5,3 L10.5,4 Z" />
</vector>
```

- [ ] **Step 5: 创建 `ic_cycle.xml`(循环箭头)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M4,12a8,8 0 0,1 14,-5 M20,12a8,8 0 0,1 -14,5" />
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M18,2 L18,7 L13,7 Z M6,22 L6,17 L11,17 Z" />
</vector>
```

- [ ] **Step 6: 创建 `ic_window.xml`(车窗)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M3,5 L21,5 L21,19 L3,19 Z M3,12 L21,12 M12,5 L12,19" />
</vector>
```

- [ ] **Step 7: 创建 `ic_seat.xml`(座椅)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M7,4 C7,3 8,2 9,2 L15,2 C16,2 17,3 17,4 L17,12 L19,18 L17,18 L16,14 L8,14 L7,18 L5,18 L7,12 Z M8,16 L16,16 L16,20 L8,20 Z" />
</vector>
```

- [ ] **Step 8: 创建 `ic_wind.xml`(风线)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M3,8 L14,8 C16,8 17,7 17,5 M3,12 L18,12 C20,12 21,13 21,15 M3,16 L12,16 C14,16 15,17 15,19" />
</vector>
```

- [ ] **Step 9: 创建 `ic_battery.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M2,7 L20,7 L20,17 L2,17 Z" />
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M20,10 L23,10 L23,14 L20,14 Z M4,9 L10,9 L10,15 L4,15 Z" />
</vector>
```

- [ ] **Step 10: 创建 `ic_signal.xml`(信号)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M3,18 L5,18 L5,21 L3,21 Z M8,15 L10,15 L10,21 L8,21 Z M13,11 L15,11 L15,21 L13,21 Z M18,6 L20,6 L20,21 L18,21 Z" />
</vector>
```

- [ ] **Step 11: 创建 `ic_bluetooth.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="1.8"
        android:pathData="M7,7 L17,17 L12,22 L12,2 L17,7 L7,17" />
</vector>
```

- [ ] **Step 12: 创建 `ic_media.xml`(音符)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M10,3 L18,2 L18,16 C18,18 16,20 14,20 C12,20 10,18 10,16 C10,14 12,13 14,13 C15,13 16,13 17,14 L17,6 L10,7 Z" />
</vector>
```

- [ ] **Step 13: 创建 `ic_map.xml`(地图标记)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M12,2 C7,2 3,6 3,11 C3,17 12,22 12,22 C12,22 21,17 21,11 C21,6 17,2 12,2 Z M12,14 A3,3 0 1,1 12,8 A3,3 0 1,1 12,14 Z" />
</vector>
```

- [ ] **Step 14: 创建 `ic_music.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M9,17 A2,2 0 1,1 7,15 A2,2 0 1,1 9,17 L9,5 L19,3 L19,14 A2,2 0 1,1 17,12 A2,2 0 1,1 19,14 L19,3" />
</vector>
```
(注:与 `ic_media` 同为音符,但 `ic_music` 用于"音乐"快捷入口,可单独保留以区分语义;若需复用,`ic_music.xml` 内容可改为 `@drawable/ic_media` 的 bitmap 引用。此处保持独立几何。)

- [ ] **Step 15: 创建 `ic_video.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M3,6 L17,6 L17,18 L3,18 Z M17,10 L22,7 L22,17 L17,14 Z" />
</vector>
```

- [ ] **Step 16: 创建 `ic_phone.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M6,2 L18,2 L18,22 L6,22 Z M10,18 L14,18 L14,20 L10,20 Z" />
</vector>
```

- [ ] **Step 17: 创建 `ic_more.xml`(三圆点)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M6,10a2,2 0 1,0 0,4a2,2 0 1,0 0,-4 M12,10a2,2 0 1,0 0,4a2,2 0 1,0 0,-4 M18,10a2,2 0 1,0 0,4a2,2 0 1,0 0,-4" />
</vector>
```

- [ ] **Step 18: 创建 `ic_plus.xml` 与 `ic_minus.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="2.2"
        android:pathData="M12,4 L12,20 M4,12 L20,12" />
</vector>
```
`ic_minus.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="2.2"
        android:pathData="M4,12 L20,12" />
</vector>
```

- [ ] **Step 19: 用户手动构建验证** —— `./gradlew assembleDebug` 通过(图标资源无引用错误)。

- [ ] **Step 20: Commit**

```bash
git add app/src/main/res/drawable-anydpi-v26/
git commit -m "feat(ui): 新增 17 个矢量图标替换 emoji(tab/ac/apps/settings/cycle/window/seat/wind/battery/signal/bluetooth/media/map/music/video/phone/more/plus/minus)"
```

---

### Task 4: 扩展 VehicleDiagramView 支持胎压/胎温

**Files:**
- Modify: `app/src/main/java/com/diui/launcher/ui/VehicleDiagramView.java`

- [ ] **Step 1: 在类字段区(现有 `doorLF...hood` 字段之后)新增胎压字段与文字 Paint**

```java
    // 胎压/胎温数据(单位:kPa / ℃),-1 表示不可用
    private int tireFLP = -1, tireFLT = -1;
    private int tireFRP = -1, tireFRT = -1;
    private int tireRLP = -1, tireRLT = -1;
    private int tireRRP = -1, tireRRT = -1;
    private boolean tireWarnFL, tireWarnFR, tireWarnRL, tireWarnRR;

    private final Paint tireTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tireTempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tireWarnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
```

- [ ] **Step 2: 在 `initPaints()` 末尾追加胎压文字 Paint 初始化**

```java
        tireTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        tireTextPaint.setTextSize(28f);
        tireTextPaint.setTextAlign(Paint.Align.CENTER);
        tireTextPaint.setFakeBoldText(true);

        tireTempPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_tertiary));
        tireTempPaint.setTextSize(18f);
        tireTempPaint.setTextAlign(Paint.Align.CENTER);

        tireWarnPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent_warn));
        tireWarnPaint.setStyle(Paint.Style.STROKE);
        tireWarnPaint.setStrokeWidth(3f);
```

- [ ] **Step 3: 新增 `setTireData` 方法(放在 `setDoorStates` 之后)**

```java
    public void setTireData(int flP, int flT, int frP, int frT,
                            int rlP, int rlT, int rrP, int rrT) {
        this.tireFLP = flP; this.tireFLT = flT;
        this.tireFRP = frP; this.tireFRT = frT;
        this.tireRLP = rlP; this.tireRLT = rlT;
        this.tireRRP = rrP; this.tireRRT = rrT;
        // 告警:压力 < 200 或 > 320,或温度 > 90
        this.tireWarnFL = isTireAbnormal(flP, flT);
        this.tireWarnFR = isTireAbnormal(frP, frT);
        this.tireWarnRL = isTireAbnormal(rlP, rlT);
        this.tireWarnRR = isTireAbnormal(rrP, rrT);
        invalidate();
    }

    private boolean isTireAbnormal(int p, int t) {
        if (p < 0) return false;
        return p < 200 || p > 320 || (t > 90 && t >= 0);
    }
```

- [ ] **Step 4: 在 `onDraw` 末尾(`}` 之前)追加胎压四角外置绘制**

```java
        // ===== 胎压/胎温 四角外置 =====
        float tireTextOffset = carW * 0.18f;
        // 左前(车头左上角外侧)
        drawTire(canvas, left - tireTextOffset, top + carH * 0.15f, tireFLP, tireFLT, tireWarnFL);
        // 右前
        drawTire(canvas, right + tireTextOffset, top + carH * 0.15f, tireFRP, tireFRT, tireWarnFR);
        // 左后
        drawTire(canvas, left - tireTextOffset, bottom - carH * 0.15f, tireRLP, tireRLT, tireWarnRL);
        // 右后
        drawTire(canvas, right + tireTextOffset, bottom - carH * 0.15f, tireRRP, tireRRT, tireWarnRR);
```

- [ ] **Step 5: 新增 `drawTire` 辅助方法**

```java
    private void drawTire(Canvas canvas, float cx, float cy, int pressure, int temp, boolean warn) {
        if (pressure < 0) return;
        Paint pPaint = warn ? tireWarnPaint : tireTextPaint;
        // 告警时文字用 warn 色
        if (warn) {
            tireTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent_warn));
        } else {
            tireTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }
        canvas.drawText(String.valueOf(pressure), cx, cy, tireTextPaint);
        canvas.drawText(temp >= 0 ? (temp + "°") : "--", cx, cy + 22f, tireTempPaint);
        // 恢复文字色,避免影响后续
        tireTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_primary));
    }
```

- [ ] **Step 6: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/VehicleDiagramView.java
git commit -m "feat(ui): VehicleDiagramView 扩展胎压/胎温四角外置展示与告警高亮"
```

---

### Task 5: 新增 GaugeView 环形进度组件

**Files:**
- Create: `app/src/main/java/com/diui/launcher/ui/GaugeView.java`

- [ ] **Step 1: 创建文件**

```java
package com.diui.launcher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;

/**
 * 环形进度 View,用于数据可视化(电量/油量/能耗等)。
 * setProgress(value, max) 设定进度;setColorByRange 自动按阈值变色。
 */
public class GaugeView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress = 0f;
    private float max = 100f;

    public GaugeView(Context context) { super(context); init(); }
    public GaugeView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.gauge_track));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(12f);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.gauge_progress));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(12f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float value, float max) {
        this.progress = Math.max(0f, Math.min(value, max));
        this.max = max;
        // 按比例变色:<30% 红,<60% 黄,否则绿
        float ratio = max > 0 ? progress / max : 0f;
        int color;
        if (ratio < 0.3f) color = ContextCompat.getColor(getContext(), R.color.battery_low);
        else if (ratio < 0.6f) color = ContextCompat.getColor(getContext(), R.color.battery_mid);
        else color = ContextCompat.getColor(getContext(), R.color.battery_high);
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = 12f;
        RectF rect = new RectF(pad, pad, getWidth() - pad, getHeight() - pad);
        canvas.drawArc(rect, -90, 360, false, trackPaint);
        float sweep = max > 0 ? 360f * (progress / max) : 0f;
        canvas.drawArc(rect, -90, sweep, false, progressPaint);
    }
}
```

- [ ] **Step 2: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/GaugeView.java
git commit -m "feat(ui): 新增 GaugeView 环形进度可视化组件"
```

---

### Task 6: 新增 WindLevelView 风量条组件

**Files:**
- Create: `app/src/main/java/com/diui/launcher/ui/WindLevelView.java`

- [ ] **Step 1: 创建文件**

```java
package com.diui.launcher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;

/**
 * 风量等级条(0-7),选中级别高亮。替代 8 个文字方块。
 */
public class WindLevelView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int level = 0;
    private final int maxLevel = 7;

    public WindLevelView(Context context) { super(context); init(); }
    public WindLevelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public WindLevelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, maxLevel));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float gap = 6f;
        float barW = (w - gap * (maxLevel)) / (maxLevel + 1);
        for (int i = 0; i <= maxLevel; i++) {
            float left = i * (barW + gap);
            float barH = h * ((i + 1) / (float) (maxLevel + 1));
            barPaint.setColor(ContextCompat.getColor(getContext(),
                    i <= level ? R.color.accent : R.color.gauge_track));
            canvas.drawRect(left, h - barH, left + barW, h, barPaint);
        }
    }
}
```

- [ ] **Step 2: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/WindLevelView.java
git commit -m "feat(ui): 新增 WindLevelView 风量条组件替代文字方块"
```

---

## 阶段 P2:Status 页重构

### Task 7: 重写 page_status.xml

**Files:**
- Modify: `app/src/main/res/layout/page_status.xml`(整体重写)

- [ ] **Step 1: 用如下内容整体替换 `page_status.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_status"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_primary"
    android:orientation="horizontal"
    android:paddingStart="@dimen/page_padding_h"
    android:paddingEnd="@dimen/page_padding_h"
    android:paddingTop="@dimen/page_padding_v"
    android:paddingBottom="@dimen/page_padding_v">

    <!-- ===== 左侧 35%:驾驶核心(速度 + 车图) ===== -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="35"
        android:orientation="vertical"
        android:layout_marginEnd="@dimen/spacing_sm">

        <!-- 速度表 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:background="@drawable/bg_card_accent"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="@dimen/card_padding"
            android:layout_marginBottom="@dimen/spacing_sm">

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginBottom="2dp">
                <TextView android:id="@+id/gear_p" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="P" android:textSize="@dimen/gear_text_size" android:textColor="@color/accent" android:textStyle="bold" android:layout_marginEnd="14dp" />
                <TextView android:id="@+id/gear_r" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="R" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="14dp" />
                <TextView android:id="@+id/gear_n" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="N" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="14dp" />
                <TextView android:id="@+id/gear_d" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="D" android:textSize="@dimen/gear_text_size" android:textColor="@color/gear_inactive" android:textStyle="bold" />
            </LinearLayout>

            <TextView
                android:id="@+id/tv_speed"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0"
                android:textColor="@color/text_primary"
                android:textSize="@dimen/speed_size"
                android:textStyle="bold"
                android:fontFamily="sans-serif-thin"
                android:includeFontPadding="false"
                android:lineSpacingMultiplier="0.85" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="km/h"
                android:textColor="@color/text_tertiary"
                android:textSize="11sp"
                android:layout_marginBottom="@dimen/spacing_sm" />

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:gravity="center">
                <TextView android:id="@+id/tv_outside_temp" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="38°C" android:textColor="@color/text_secondary" android:textSize="12sp" />
                <View android:layout_width="1dp" android:layout_height="12dp" android:background="@color/divider" android:layout_marginStart="10dp" android:layout_marginEnd="10dp" />
                <TextView android:id="@+id/tv_power_kw" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="0.0 kW" android:textColor="@color/accent" android:textSize="12sp" android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>

        <!-- 车辆俯视图(整合胎压 + 车门) -->
        <com.diui.launcher.ui.VehicleDiagramView
            android:id="@+id/vehicle_diagram"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1.3"
            android:background="@drawable/bg_card" />
    </LinearLayout>

    <!-- ===== 右侧 65%:媒体 + 行程 ===== -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="65"
        android:orientation="vertical">

        <!-- 上:时钟 + app 快捷 -->
        <LinearLayout
            android:id="@+id/pip_default"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:background="@drawable/bg_card"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="@dimen/spacing_xl"
            android:layout_marginBottom="@dimen/spacing_sm">

            <TextClock
                android:id="@+id/pip_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:format24Hour="HH:mm"
                android:format12Hour="hh:mm"
                android:textColor="@color/text_primary"
                android:textSize="@dimen/pip_clock_size"
                android:fontFamily="sans-serif-thin"
                android:textStyle="bold"
                android:layout_marginBottom="2dp" />

            <TextClock
                android:id="@+id/pip_date"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:format24Hour="MM月dd日  EEEE"
                android:format12Hour="MM月dd日  EEEE"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/pip_date_size"
                android:layout_marginBottom="@dimen/spacing_xl" />

            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center">

                <LinearLayout android:id="@+id/pip_map"
                    android:layout_width="@dimen/pip_app_btn_w" android:layout_height="@dimen/pip_app_btn_h"
                    android:background="@drawable/bg_pip_app" android:gravity="center" android:orientation="vertical"
                    android:padding="8dp" android:layout_marginEnd="8dp">
                    <ImageView android:layout_width="@dimen/pip_app_icon_size" android:layout_height="@dimen/pip_app_icon_size"
                        android:src="@drawable/ic_map" android:tint="@color/text_secondary" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="地图" android:textColor="@color/text_secondary" android:textSize="11sp" android:layout_marginTop="4dp" />
                </LinearLayout>

                <LinearLayout android:id="@+id/pip_music"
                    android:layout_width="@dimen/pip_app_btn_w" android:layout_height="@dimen/pip_app_btn_h"
                    android:background="@drawable/bg_pip_app" android:gravity="center" android:orientation="vertical"
                    android:padding="8dp" android:layout_marginEnd="8dp">
                    <ImageView android:layout_width="@dimen/pip_app_icon_size" android:layout_height="@dimen/pip_app_icon_size"
                        android:src="@drawable/ic_music" android:tint="@color/text_secondary" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="音乐" android:textColor="@color/text_secondary" android:textSize="11sp" android:layout_marginTop="4dp" />
                </LinearLayout>

                <LinearLayout android:id="@+id/pip_video"
                    android:layout_width="@dimen/pip_app_btn_w" android:layout_height="@dimen/pip_app_btn_h"
                    android:background="@drawable/bg_pip_app" android:gravity="center" android:orientation="vertical"
                    android:padding="8dp" android:layout_marginEnd="8dp">
                    <ImageView android:layout_width="@dimen/pip_app_icon_size" android:layout_height="@dimen/pip_app_icon_size"
                        android:src="@drawable/ic_video" android:tint="@color/text_secondary" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="视频" android:textColor="@color/text_secondary" android:textSize="11sp" android:layout_marginTop="4dp" />
                </LinearLayout>

                <LinearLayout android:id="@+id/pip_phone"
                    android:layout_width="@dimen/pip_app_btn_w" android:layout_height="@dimen/pip_app_btn_h"
                    android:background="@drawable/bg_pip_app" android:gravity="center" android:orientation="vertical"
                    android:padding="8dp" android:layout_marginEnd="8dp">
                    <ImageView android:layout_width="@dimen/pip_app_icon_size" android:layout_height="@dimen/pip_app_icon_size"
                        android:src="@drawable/ic_phone" android:tint="@color/text_secondary" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="电话" android:textColor="@color/text_secondary" android:textSize="11sp" android:layout_marginTop="4dp" />
                </LinearLayout>

                <LinearLayout android:id="@+id/pip_more_apps"
                    android:layout_width="@dimen/pip_app_btn_w" android:layout_height="@dimen/pip_app_btn_h"
                    android:background="@drawable/bg_pip_app" android:gravity="center" android:orientation="vertical"
                    android:padding="8dp">
                    <ImageView android:layout_width="@dimen/pip_app_icon_size" android:layout_height="@dimen/pip_app_icon_size"
                        android:src="@drawable/ic_more" android:tint="@color/text_secondary" />
                    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="更多" android:textColor="@color/text_secondary" android:textSize="11sp" android:layout_marginTop="4dp" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>

        <!-- 下:行程卡(能耗 + 里程整合) -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card"
            android:orientation="vertical"
            android:padding="@dimen/card_padding_sm">

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="行程" style="@style/DataLabel" android:layout_marginEnd="@dimen/spacing_md" />
                <TextView android:id="@+id/tv_trip_distance" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="0.0km" android:textColor="@color/text_primary" android:textSize="14sp" android:textStyle="bold" />
                <TextView android:id="@+id/tv_trip_time" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text=" · 00:00" android:textColor="@color/text_secondary" android:textSize="12sp" />
                <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
                <TextView android:id="@+id/tv_elec_consumption" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="16.0度" android:textColor="@color/accent" android:textSize="13sp" android:textStyle="bold" />
            </LinearLayout>

            <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@color/divider" android:layout_marginTop="@dimen/spacing_sm" android:layout_marginBottom="@dimen/spacing_sm" />

            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center_vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="总里程" android:textColor="@color/text_tertiary" android:textSize="11sp" />
                <TextView android:id="@+id/tv_total_mileage" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="73465km" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginStart="4dp" android:layout_marginEnd="@dimen/spacing_md" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="HEV" android:textColor="@color/accent" android:textSize="11sp" />
                <TextView android:id="@+id/tv_hev_mileage" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="18563km" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginStart="4dp" />
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

(注:已删除原 `tv_battery/tv_ev_range/tv_fuel/tv_fuel_amount/tv_total_range/progress_battery` 等能源 TextView —— 移至 TopBar;已删除胎压 2x2 网格 —— 并入车图;已删除 `tv_smart_charge/tv_recovery_mode/tv_fuel_consumption/tv_tire_*` —— 低频或已整合。`pip_area` FrameLayout 移除,改为直接 LinearLayout。)

- [ ] **Step 2: 用户手动构建验证** —— `./gradlew assembleDebug`。预期:编译会因 `StatusPage.java` 仍引用已删除的 View ID 而失败 → 进入 Task 8 修复。

- [ ] **Step 3: Commit(布局先提交,Java 修复在同一阶段后续 commit)**

```bash
git add app/src/main/res/layout/page_status.xml
git commit -m "refactor(ui): 重写 page_status,车图整合胎压/车门,删零散能源迷你卡"
```

---

### Task 8: 修复 StatusPage.java 适配新布局

**Files:**
- Modify: `app/src/main/java/com/diui/launcher/ui/StatusPage.java`

- [ ] **Step 1: 先读取现有 `StatusPage.java` 完整内容,确认字段与 `findViewById` 调用清单**

```bash
cat app/src/main/java/com/diui/launcher/ui/StatusPage.java
```

- [ ] **Step 2: 删除已不存在的字段与 `findViewById`**

删除对以下 ID 的字段声明与绑定:`tv_battery`、`tv_ev_range`、`progress_battery`、`tv_fuel`、`tv_fuel_amount`、`tv_total_range`、`tv_fuel_consumption`、`tv_smart_charge`、`tv_recovery_mode`、`tv_tire_fl_pressure`、`tv_tire_fr_pressure`、`tv_tire_rl_pressure`、`tv_tire_rr_pressure`、`tv_tire_fl_temp`、`tv_tire_fr_temp`、`tv_tire_rl_temp`、`tv_tire_rr_temp`。
保留(`page_status.xml` 新布局仍含这些 ID):`tv_speed`、`gear_p/r/n/d`、`tv_outside_temp`、`tv_power_kw`、`tv_trip_distance`、`tv_trip_time`、`tv_elec_consumption`、`tv_total_mileage`、`tv_hev_mileage`、`pip_map/pip_music/pip_video/pip_phone/pip_more_apps`(`initPipArea()` 不动)。

- [ ] **Step 3: 新增 `VehicleDiagramView` 字段与绑定**

在字段区新增:
```java
    private VehicleDiagramView vehicleDiagram;
```
在 `findViewById` 区新增:
```java
        vehicleDiagram = rootView.findViewById(R.id.vehicle_diagram);
```
添加 import:
```java
import com.diui.launcher.ui.VehicleDiagramView;
```
(若 `StatusPage` 已在 `com.diui.launcher.ui` 包内,无需 import。)

- [ ] **Step 4: 在 `updateStatus(VehicleStatus s)` 方法中,删除对已删字段的 setText 调用,新增车图胎压/车门更新**

删除方法体内:电量块(`tvBattery/progressBattery/tvEvRange`)、油量块(`tvFuel/tvFuelAmount`)、总续航(`tvTotalRange`)、`tvFuelConsumption`、胎压胎温块(8 个 tire* setText)、能量模式(`tvSmartCharge/tvRecoveryMode`)。
保留:速度/挡位/功率/温度、`tvElecConsumption`、行程/里程。
在方法末尾追加:

```java
        // 车图:胎压 + 车门(VehicleStatus 字段为 public,直接访问)
        if (vehicleDiagram != null) {
            vehicleDiagram.setTireData(
                    s.tirePressureFL, s.tireTempFL,
                    s.tirePressureFR, s.tireTempFR,
                    s.tirePressureRL, s.tireTempRL,
                    s.tirePressureRR, s.tireTempRR);
            vehicleDiagram.setDoorStates(
                    s.doorLeftFrontOpen, s.doorRightFrontOpen,
                    s.doorLeftRearOpen, s.doorRightRearOpen,
                    s.trunkOpen, s.hoodOpen);
        }
```
(`VehicleStatus` 字段已确认为 public:`tirePressureFL/FR/RL/RR`、`tireTempFL/FR/RL/RR`、`doorLeftFrontOpen`、`doorRightFrontOpen`、`doorLeftRearOpen`、`doorRightRearOpen`、`trunkOpen`、`hoodOpen`。`StatusPage` 与 `VehicleDiagramView` 同在 `com.diui.launcher.ui` 包,无需 import。)

- [ ] **Step 5: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。装车验证车图显示胎压/车门、行程卡显示。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/StatusPage.java
git commit -m "refactor(ui): StatusPage 适配新布局,车图绑定胎压/车门,删能源 TextView"
```

---

## 阶段 P3:TopBar 能源常驻栏

### Task 9: 重写 view_top_bar.xml

**Files:**
- Modify: `app/src/main/res/layout/view_top_bar.xml`(整体重写)

- [ ] **Step 1: 用如下内容整体替换**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/top_bar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_bar_height"
    android:background="@drawable/bg_top_bar"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingStart="@dimen/top_bar_padding_h"
    android:paddingEnd="@dimen/top_bar_padding_h">

    <!-- 左:电量 + 纯电续航 -->
    <ImageView
        android:layout_width="@dimen/top_bar_icon" android:layout_height="@dimen/top_bar_icon"
        android:src="@drawable/ic_battery" android:tint="@color/accent" />
    <TextView android:id="@+id/top_battery" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="89%" android:textColor="@color/text_primary" android:textSize="@dimen/top_bar_text" android:textStyle="bold" android:layout_marginStart="3dp" />
    <TextView android:id="@+id/top_ev_range" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="90km" android:textColor="@color/text_secondary" android:textSize="@dimen/top_bar_text" android:layout_marginStart="3dp" android:layout_marginEnd="@dimen/spacing_md" />

    <!-- 油量 + 油量L -->
    <ImageView
        android:layout_width="@dimen/top_bar_icon" android:layout_height="@dimen/top_bar_icon"
        android:src="@drawable/ic_battery" android:tint="@color/status_fair" />
    <TextView android:id="@+id/top_fuel" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="37%" android:textColor="@color/text_primary" android:textSize="@dimen/top_bar_text" android:textStyle="bold" android:layout_marginStart="3dp" />
    <TextView android:id="@+id/top_fuel_amount" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="21L" android:textColor="@color/text_secondary" android:textSize="@dimen/top_bar_text" android:layout_marginStart="3dp" android:layout_marginEnd="@dimen/spacing_md" />

    <!-- 总续航 -->
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="总续航" android:textColor="@color/text_tertiary" android:textSize="@dimen/top_bar_text" />
    <TextView android:id="@+id/top_total_range" android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="396km" android:textColor="@color/accent" android:textSize="@dimen/top_bar_text" android:textStyle="bold" android:layout_marginStart="3dp" />

    <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />

    <!-- 右:标题 + 时间 -->
    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="迪UI" android:textSize="11sp" android:textColor="@color/accent" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_md" />
    <TextClock
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:format24Hour="HH:mm" android:format12Hour="hh:mm"
        android:textSize="@dimen/top_bar_text" android:textColor="@color/text_secondary" android:textStyle="bold" />
</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/view_top_bar.xml
git commit -m "refactor(ui): TopBar 改为能源常驻栏(电量+纯电续航+油量+总续航+时间)"
```

---

### Task 10: TopBar.java 绑定能源数据

**Files:**
- Modify: `app/src/main/java/com/diui/launcher/ui/TopBar.java`
- Modify: `app/src/main/java/com/diui/launcher/MainActivity.java`

- [ ] **Step 1: 读取现有 `TopBar.java` 与 `MainActivity.java`,确认 TopBar 构造与轮询回调结构**

```bash
cat app/src/main/java/com/diui/launcher/ui/TopBar.java
grep -n "topBar\|TopBar\|update" app/src/main/java/com/diui/launcher/MainActivity.java
```

- [ ] **Step 2: 重写 `TopBar.java` —— 删除 `mediaSource`(新布局已无 `top_media_source` ID,否则 `updateMediaSource` 会 NPE),改为能源字段**

整体替换 `TopBar.java` 内容为:

```java
package com.diui.launcher.ui;

import android.view.View;
import android.widget.TextView;

import com.diui.launcher.R;
import com.diui.launcher.model.VehicleStatus;

public class TopBar {

    private final TextView tvBattery, tvEvRange, tvFuel, tvFuelAmount, tvTotalRange;

    public TopBar(View rootView) {
        tvBattery = rootView.findViewById(R.id.top_battery);
        tvEvRange = rootView.findViewById(R.id.top_ev_range);
        tvFuel = rootView.findViewById(R.id.top_fuel);
        tvFuelAmount = rootView.findViewById(R.id.top_fuel_amount);
        tvTotalRange = rootView.findViewById(R.id.top_total_range);
    }

    public void update(VehicleStatus s) {
        if (s == null) return;
        if (tvBattery != null) tvBattery.setText(s.getBatteryText());
        if (tvEvRange != null) tvEvRange.setText(s.getEvMileageText());
        if (tvFuel != null) tvFuel.setText(s.fuelPercent >= 0 ? s.fuelPercent + "%" : "N/A");
        if (tvFuelAmount != null) tvFuelAmount.setText(s.getFuelText());
        if (tvTotalRange != null) tvTotalRange.setText(s.getTotalRangeText());
    }
}
```
(注:`VehicleStatus` 已有现成 text 方法:`getBatteryText()` → "89%"、`getEvMileageText()` → "90km"(`evMileage`)、`getFuelText()` → "21.0L"(`fuelAmount`)、`getTotalRangeText()` → "396km";`fuelPercent` 为 public int 字段。)

- [ ] **Step 3: 在 `MainActivity.java` 把原 `topBar.updateMediaSource("")` 改为 `topBar.update(status)`**

现有 `MainActivity` 第 524 行附近:
```java
            topBar.updateMediaSource("");
```
改为:
```java
            topBar.update(status);
```
(`topBar` 字段已在 `MainActivity` 第 51 行声明、第 112 行初始化为 `new TopBar(findViewById(R.id.top_bar))`,无需新增声明。轮询回调位于第 520 行 `statusPage.updateStatus(status)` 同一代码块,`status` 变量在作用域内。)

- [ ] **Step 4: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。装车验证 TopBar 实时显示能源。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/TopBar.java app/src/main/java/com/diui/launcher/MainActivity.java
git commit -m "feat(ui): TopBar 绑定 VehicleStatus 能源数据并接入轮询"
```

---

## 阶段 P4:Controls 页图形化

### Task 11: 重写 page_controls.xml(图形化,去车身控制)

**Files:**
- Modify: `app/src/main/res/layout/page_controls.xml`(整体重写)

- [ ] **Step 1: 用如下内容整体替换**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_controls"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="@dimen/page_padding"
    android:visibility="gone"
    android:scrollbars="none">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_card"
        android:orientation="vertical"
        android:padding="@dimen/card_padding">

        <!-- 标题 + 开关 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:layout_marginBottom="@dimen/spacing_lg">
            <ImageView android:layout_width="20dp" android:layout_height="20dp"
                android:src="@drawable/ic_ac" android:tint="@color/accent" android:layout_marginEnd="8dp" />
            <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:text="空调控制" android:textColor="@color/text_primary" android:textSize="18sp" android:textStyle="bold" />
            <TextView android:id="@+id/ctrl_ac_power" android:layout_width="56dp" android:layout_height="32dp"
                android:background="@drawable/bg_ac_btn" android:gravity="center"
                android:text="OFF" android:textColor="@color/text_secondary" android:textSize="14sp" android:textStyle="bold" />
        </LinearLayout>

        <!-- 温度双区 + 中央风量 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:layout_marginBottom="@dimen/spacing_lg">

            <!-- 左区温度 -->
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:orientation="vertical" android:gravity="center">
                <TextView android:id="@+id/ctrl_ac_temp" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="25°C" android:textColor="@color/accent" android:textSize="32sp" android:textStyle="bold" />
                <TextView android:id="@+id/ctrl_ac_status" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="已关闭" android:textColor="@color/text_secondary" android:textSize="12sp" />
                <LinearLayout android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="6dp">
                    <TextView android:id="@+id/ctrl_temp_down" android:layout_width="36dp" android:layout_height="36dp"
                        android:background="@drawable/bg_ac_quick" android:gravity="center"
                        android:text="−" android:textColor="@color/accent" android:textSize="22sp" android:layout_marginEnd="8dp" />
                    <TextView android:id="@+id/ctrl_temp_up" android:layout_width="36dp" android:layout_height="36dp"
                        android:background="@drawable/bg_ac_quick" android:gravity="center"
                        android:text="+" android:textColor="@color/accent" android:textSize="22sp" />
                </LinearLayout>
            </LinearLayout>

            <!-- 中央风量条 -->
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
                android:orientation="vertical" android:gravity="center">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="风量" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginBottom="6dp" />
                <com.diui.launcher.ui.WindLevelView
                    android:id="@+id/ctrl_wind_view"
                    android:layout_width="120dp" android:layout_height="48dp" />
                <TextView android:id="@+id/ctrl_wind_label" android:layout_width="wrap_content" android:layout_height="wrap_content"
                    android:text="0 / 7" android:textColor="@color/text_primary" android:textSize="13sp" android:textStyle="bold" android:layout_marginTop="4dp" />
            </LinearLayout>
        </LinearLayout>

        <!-- 出风 + 循环 + 模式 -->
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content">
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="3" android:orientation="vertical" android:layout_marginEnd="@dimen/spacing_md">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="出风" android:textColor="@color/text_secondary" android:textSize="13sp" android:layout_marginBottom="@dimen/spacing_sm" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content">
                    <TextView android:id="@+id/ctrl_mode_face" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="吹面" android:textColor="@color/text_primary" android:textSize="13sp" android:layout_marginEnd="3dp" />
                    <TextView android:id="@+id/ctrl_mode_foot" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="吹脚" android:textColor="@color/text_primary" android:textSize="13sp" android:layout_marginEnd="3dp" />
                    <TextView android:id="@+id/ctrl_mode_defrost" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="除雾" android:textColor="@color/text_primary" android:textSize="13sp" />
                </LinearLayout>
            </LinearLayout>
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="2" android:orientation="vertical" android:layout_marginEnd="@dimen/spacing_md">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="循环" android:textColor="@color/text_secondary" android:textSize="13sp" android:layout_marginBottom="@dimen/spacing_sm" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content">
                    <TextView android:id="@+id/ctrl_cycle_inner" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="内循环" android:textColor="@color/text_primary" android:textSize="13sp" android:layout_marginEnd="3dp" />
                    <TextView android:id="@+id/ctrl_cycle_outer" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="外循环" android:textColor="@color/text_primary" android:textSize="13sp" />
                </LinearLayout>
            </LinearLayout>
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="2" android:orientation="vertical">
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="模式" android:textColor="@color/text_secondary" android:textSize="13sp" android:layout_marginBottom="@dimen/spacing_sm" />
                <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content">
                    <TextView android:id="@+id/ctrl_auto" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="自动" android:textColor="@color/text_primary" android:textSize="13sp" android:layout_marginEnd="3dp" />
                    <TextView android:id="@+id/ctrl_manual" android:layout_width="0dp" android:layout_weight="1" android:layout_height="36dp" android:background="@drawable/bg_switch_btn" android:gravity="center" android:text="手动" android:textColor="@color/text_primary" android:textSize="13sp" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

(注:删除了原 0-7 八个 `ctrl_wind_*` 文字方块,改为 `WindLevelView` + 标签;删除了原 `ctrl_wind_0..7` ID —— `ControlsPage.java` 需在 Task 12 适配。出风/循环/模式按钮 ID 保留不变,API 绑定无需改。)

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/page_controls.xml
git commit -m "refactor(ui): Controls 页图形化,风量改 WindLevelView,去车身控制区"
```

---

### Task 12: 修复 ControlsPage.java 适配 WindLevelView

**Files:**
- Modify: `app/src/main/java/com/diui/launcher/ui/ControlsPage.java`

- [ ] **Step 1: 读取现有 `ControlsPage.java` 确认 wind 相关代码块**

```bash
cat app/src/main/java/com/diui/launcher/ui/ControlsPage.java
```

- [ ] **Step 2: 将 `windBtns` 数组及其绑定替换为 `WindLevelView`**

删除字段:
```java
    private final TextView[] windBtns;   // 删除
```
删除构造中 `windBtns = new TextView[]{...}` 整块。
新增字段与绑定:
```java
    private WindLevelView windView;
    private TextView windLabel;
```
构造中:
```java
        windView = rootView.findViewById(R.id.ctrl_wind_view);
        windLabel = rootView.findViewById(R.id.ctrl_wind_label);
```
删除原 `for (int i=0; i<windBtns.length; i++) { windBtns[i].setOnClickListener(...) }` 整块(风量改为通过下方面板控制;若需保留点击风量条增减,可加):
```java
        if (windView != null) {
            windView.setOnClickListener(v -> {
                int next = (currentWindLevel + 1) % 8;
                acApi.setWindLevel(next);
            });
        }
```

- [ ] **Step 3: 修改 `highlightWindLevel(int level)` 方法**

删除原遍历 `windBtns` 设置 selected/textColor 的逻辑,改为:
```java
    private void highlightWindLevel(int level) {
        currentWindLevel = level;
        if (windView != null) windView.setLevel(level);
        if (windLabel != null) windLabel.setText(level + " / 7");
    }
```
新增字段:
```java
    private int currentWindLevel = 0;
```

- [ ] **Step 4: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。装车验证风量条显示与点击循环。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/diui/launcher/ui/ControlsPage.java
git commit -m "refactor(ui): ControlsPage 改用 WindLevelView,删 windBtns 数组"
```

---

## 阶段 P5:NavBar 图标升级

### Task 13: view_nav_bar.xml 替换 emoji 为矢量图标

**Files:**
- Modify: `app/src/main/res/layout/view_nav_bar.xml`

- [ ] **Step 1: 将 4 个 tab 的 `TextView` emoji 替换为 `ImageView` 矢量图标**

`tab_status` 内的:
```xml
<TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="🏠" android:textSize="@dimen/nav_tab_icon" />
```
替换为:
```xml
<ImageView android:layout_width="@dimen/nav_tab_icon" android:layout_height="@dimen/nav_tab_icon"
    android:src="@drawable/ic_tab_home" android:tint="@color/nav_text" />
```
`tab_controls` 的 `❄` → `ic_ac`;`tab_apps` 的 `⊞` → `ic_apps`;`tab_settings` 的 `⚙` → `ic_settings`。tint 均为 `@color/nav_text`。

- [ ] **Step 2: AC 快捷区图标替换**

`nav_ac_power` 的 `android:text="❄ AC"` 改为:
```xml
android:text="AC"
```
并在其前方新增(若布局允许,或用 drawableStart):
```xml
android:drawableStart="@drawable/ic_ac"
android:drawableTint="@color/accent"
android:paddingStart="6dp"
```
`nav_ac_wind_icon` 的 `android:text="🌀"` TextView 替换为:
```xml
<ImageView android:layout_width="14dp" android:layout_height="14dp"
    android:src="@drawable/ic_wind" android:tint="@color/text_secondary"
    android:layout_marginEnd="3dp" />
```
(注意:`nav_ac_wind_icon` 原 ID 是 TextView —— 若 `NavBar.java` 通过 ID 引用它,需保持 ID 在新 ImageView 上:`android:id="@+id/nav_ac_wind_icon"`。若 NavBar 未引用该 ID,可直接替换。实施时 `grep nav_ac_wind_icon app/src/main/java` 确认。)

- [ ] **Step 3: 右侧功能按钮 emoji 替换**

`nav_btn_cycle` 的 `🔄` → `ic_cycle`;`nav_btn_window` 的 `🪟` → `ic_window`;`nav_btn_seat` 的 `💺` → `ic_seat`。均替换为 ImageView,tint `@color/nav_text`。

- [ ] **Step 4: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。装车验证 NavBar 图标显示、选中态变色。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/view_nav_bar.xml
git commit -m "refactor(ui): NavBar 全量 emoji 替换为矢量图标"
```

---

## 阶段 P6:Settings / Apps

### Task 14: page_settings.xml 分组卡片 + 图标

**Files:**
- Modify: `app/src/main/res/layout/page_settings.xml`(局部修改)

- [ ] **Step 1: 读取现有 `page_settings.xml` 确认三大分组结构与现有 emoji/硬编码图标位置**

```bash
grep -n "显示\|车辆\|系统\|emoji\|text=\"⚙\|text=\"🏠" app/src/main/res/layout/page_settings.xml
```

- [ ] **Step 2: 在每个分组标题 TextView 旁加分组图标**

"显示" 分组标题 TextView 前加:
```xml
<ImageView android:layout_width="16dp" android:layout_height="16dp"
    android:src="@drawable/ic_settings" android:tint="@color/text_secondary"
    android:layout_marginEnd="6dp" />
```
"车辆" 分组用 `ic_cycle`;"系统" 分组用 `ic_settings`。(若无现成分组图标,统一用 `ic_settings` 也可;此处不新增图标以控制范围。)
注意:需把分组标题 TextView 与 ImageView 放进一个 horizontal LinearLayout。

- [ ] **Step 3: 行内 emoji/硬编码图标替换为矢量**

逐处将 `android:text="🏠"` 等替换为 `ImageView` + `ic_*.xml`(模式同 Task 13)。

- [ ] **Step 4: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/page_settings.xml
git commit -m "refactor(ui): Settings 分组卡片图标化,替换 emoji"
```

---

### Task 15: page_apps.xml 标题图标化

**Files:**
- Modify: `app/src/main/res/layout/page_apps.xml`

- [ ] **Step 1: 标题"所有应用"前加图标**

将:
```xml
<TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
    android:text="所有应用" ... />
```
改为包在 horizontal LinearLayout 内,前置:
```xml
<ImageView android:layout_width="18dp" android:layout_height="18dp"
    android:src="@drawable/ic_apps" android:tint="@color/accent" android:layout_marginEnd="8dp" />
```

- [ ] **Step 2: 用户手动构建验证** —— `./gradlew assembleDebug` 通过。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/page_apps.xml
git commit -m "refactor(ui): Apps 页标题图标化"
```

---

## 完成准则

- 全部 emoji 在 NavBar/TopBar/Status/Controls/Settings/Apps 被矢量图标替换。
- Status 页胎压并入车图(四角外置 + 告警),零散迷你卡整合为速度表/车图/行程卡。
- TopBar 常驻显示电量+纯电续航+油量+总续航+时间。
- Controls 页风量图形化,无车身控制区。
- 每阶段独立 commit,`./gradlew assembleDebug` 编译通过,装车验证。
- API 层、权限、`VehicleStatus` 字段未改。
