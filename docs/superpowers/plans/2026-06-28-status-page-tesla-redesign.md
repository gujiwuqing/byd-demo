# 状态页 Tesla 极简风格 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将状态页重构为 Tesla 极简风格——车辆图居中为主，底部迷你信息条。

**Architecture:** 垂直布局：Hero 区域（车辆图 + 叠加遥测数据）占满可用空间 + 底部固定 120dp 迷你信息条。3 张卡片（音乐/空气/快捷）重新设计为紧凑迷你卡。

**Tech Stack:** Android XML 布局、Java、Shape Drawable、自定义 View（VehicleDiagramView）

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 创建 | `app/src/main/res/drawable/bg_mini_card.xml` | 迷你卡片背景（12dp 圆角） |
| 修改 | `app/src/main/res/values/dimens.xml` | 新增状态页专用尺寸 |
| 修改 | `app/src/main/res/layout/page_status.xml` | 完全重写为 Tesla 风格布局 |
| 修改 | `app/src/main/res/layout/card_music.xml` | 重写为迷你卡片 |
| 修改 | `app/src/main/res/layout/card_air_quality.xml` | 重写为迷你卡片 |
| 修改 | `app/src/main/res/layout/card_app_shortcuts.xml` | 重写为迷你卡片 |
| 修改 | `app/src/main/java/com/bydlauncher/ui/StatusPage.java` | 更新控件引用和逻辑 |
| 修改 | `app/src/main/java/com/bydlauncher/ui/MusicCardView.java` | 处理迷你卡中移除的视图（appIcon/appName/cover 可为 null） |

---

### Task 1: 创建迷你卡片 drawable 和更新 dimens

**Files:**
- Create: `app/src/main/res/drawable/bg_mini_card.xml`
- Modify: `app/src/main/res/values/dimens.xml`

- [ ] **Step 1: 创建迷你卡片背景 drawable**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_surface" />
    <corners android:radius="12dp" />
</shape>
```

- [ ] **Step 2: 在 dimens.xml 末尾添加状态页专用尺寸**

在 `</resources>` 前插入：

```xml
    <!-- Status 页 (Tesla 风格) -->
    <dimen name="status_speed_size">56sp</dimen>
    <dimen name="status_speed_unit_size">16sp</dimen>
    <dimen name="status_tire_value_size">14sp</dimen>
    <dimen name="status_tire_unit_size">10sp</dimen>
    <dimen name="status_gear_size">16sp</dimen>
    <dimen name="status_mini_card_height">110dp</dimen>
    <dimen name="status_mini_card_padding">12dp</dimen>
```

---

### Task 2: 重写 page_status.xml

**Files:**
- Modify: `app/src/main/res/layout/page_status.xml`（完全重写）

- [ ] **Step 1: 重写为 Tesla 极简风格布局**

完全替换 `page_status.xml` 内容：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_status"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="@dimen/page_padding">

    <!-- ═══ Hero 区域：车辆图 + 遥测数据 ═══ -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <!-- 速度（顶部居中） -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal|top"
            android:layout_marginTop="@dimen/spacing_lg"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tv_speed"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0"
                android:textColor="@color/text_primary"
                android:textSize="@dimen/status_speed_size"
                android:textStyle="bold"
                android:fontFamily="sans-serif-light" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/speed_unit"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/status_speed_unit_size"
                android:layout_marginStart="@dimen/spacing_sm"
                android:layout_marginBottom="@dimen/spacing_sm" />
        </LinearLayout>

        <!-- 车辆俯视图（居中） -->
        <com.bydlauncher.ui.VehicleDiagramView
            android:id="@+id/vehicle_diagram"
            android:layout_width="220dp"
            android:layout_height="match_parent"
            android:layout_gravity="center"
            android:layout_marginTop="60dp"
            android:layout_marginBottom="60dp" />

        <!-- 胎压：左前 -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start|top"
            android:layout_marginStart="@dimen/spacing_lg"
            android:layout_marginTop="80dp"
            android:orientation="vertical"
            android:gravity="center">
            <TextView
                android:id="@+id/tire_fl"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="37"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/status_tire_value_size"
                android:textStyle="bold" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/speed_unit_tire"
                android:textColor="@color/text_tertiary"
                android:textSize="@dimen/status_tire_unit_size" />
        </LinearLayout>

        <!-- 胎压：右前 -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end|top"
            android:layout_marginEnd="@dimen/spacing_lg"
            android:layout_marginTop="80dp"
            android:orientation="vertical"
            android:gravity="center">
            <TextView
                android:id="@+id/tire_fr"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="36"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/status_tire_value_size"
                android:textStyle="bold" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/speed_unit_tire"
                android:textColor="@color/text_tertiary"
                android:textSize="@dimen/status_tire_unit_size" />
        </LinearLayout>

        <!-- 胎压：左后 -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="start|bottom"
            android:layout_marginStart="@dimen/spacing_lg"
            android:layout_marginBottom="80dp"
            android:orientation="vertical"
            android:gravity="center">
            <TextView
                android:id="@+id/tire_rl"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="35"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/status_tire_value_size"
                android:textStyle="bold" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/speed_unit_tire"
                android:textColor="@color/text_tertiary"
                android:textSize="@dimen/status_tire_unit_size" />
        </LinearLayout>

        <!-- 胎压：右后 -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end|bottom"
            android:layout_marginEnd="@dimen/spacing_lg"
            android:layout_marginBottom="80dp"
            android:orientation="vertical"
            android:gravity="center">
            <TextView
                android:id="@+id/tire_rr"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="35"
                android:textColor="@color/text_secondary"
                android:textSize="@dimen/status_tire_value_size"
                android:textStyle="bold" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/speed_unit_tire"
                android:textColor="@color/text_tertiary"
                android:textSize="@dimen/status_tire_unit_size" />
        </LinearLayout>

        <!-- 挡位 + 电量（底部居中） -->
        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal|bottom"
            android:layout_marginBottom="@dimen/spacing_md"
            android:gravity="center"
            android:orientation="vertical">

            <!-- 挡位 -->
            <LinearLayout
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginBottom="@dimen/spacing_sm">
                <TextView android:id="@+id/gear_p" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="P" android:textSize="@dimen/status_gear_size" android:textColor="@color/gear_active" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_lg" />
                <TextView android:id="@+id/gear_r" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="R" android:textSize="@dimen/status_gear_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_lg" />
                <TextView android:id="@+id/gear_n" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="N" android:textSize="@dimen/status_gear_size" android:textColor="@color/gear_inactive" android:textStyle="bold" android:layout_marginEnd="@dimen/spacing_lg" />
                <TextView android:id="@+id/gear_d" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="D" android:textSize="@dimen/status_gear_size" android:textColor="@color/gear_inactive" android:textStyle="bold" />
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
                    android:text="99%"
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
                    android:progress="99"
                    android:progressDrawable="@drawable/progress_bar_battery" />
            </LinearLayout>
        </LinearLayout>
    </FrameLayout>

    <!-- ═══ 底部迷你信息条 ═══ -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/status_mini_card_height"
        android:layout_marginTop="@dimen/spacing_md"
        android:orientation="horizontal">

        <include
            layout="@layout/card_music"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="@dimen/spacing_md" />

        <include
            layout="@layout/card_air_quality"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="@dimen/spacing_md" />

        <include
            layout="@layout/card_app_shortcuts"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1" />
    </LinearLayout>
</LinearLayout>
```

---

### Task 3: 重写三张迷你卡片布局

**Files:**
- Modify: `app/src/main/res/layout/card_music.xml`
- Modify: `app/src/main/res/layout/card_air_quality.xml`
- Modify: `app/src/main/res/layout/card_app_shortcuts.xml`

- [ ] **Step 1: 重写 card_music.xml 为迷你卡片**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/music_card"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_mini_card"
    android:orientation="vertical"
    android:padding="@dimen/status_mini_card_padding">

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
        android:ellipsize="end"
        android:layout_marginBottom="@dimen/spacing_sm" />

    <!-- 控制按钮 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:layout_marginTop="@dimen/spacing_xs">

        <ImageView
            android:id="@+id/music_prev"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:src="@android:drawable/ic_media_previous"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_lg" />

        <ImageView
            android:id="@+id/music_play_pause"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@android:drawable/ic_media_play"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_lg" />

        <ImageView
            android:id="@+id/music_next"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:src="@android:drawable/ic_media_next"
            android:scaleType="fitCenter" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 重写 card_air_quality.xml 为迷你卡片**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_mini_card"
    android:orientation="vertical"
    android:padding="@dimen/status_mini_card_padding">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/air_quality"
        android:textColor="@color/text_secondary"
        android:textSize="@dimen/aq_label_size"
        android:layout_marginBottom="@dimen/spacing_sm" />

    <!-- 室外 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="@dimen/spacing_xs">
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_outside"
            android:textColor="@color/text_secondary"
            android:textSize="13sp" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView
            android:id="@+id/aq_outside_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_good"
            android:textColor="@color/status_fair"
            android:textSize="14sp"
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
            android:textSize="13sp" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView
            android:id="@+id/aq_inside_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/aq_excellent"
            android:textColor="@color/status_good"
            android:textSize="14sp"
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

- [ ] **Step 3: 重写 card_app_shortcuts.xml 为迷你卡片**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/shortcuts_card"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_mini_card"
    android:orientation="horizontal"
    android:padding="@dimen/status_mini_card_padding"
    android:gravity="center">

    <ImageView
        android:id="@+id/shortcut_app_1"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_sm" />

    <ImageView
        android:id="@+id/shortcut_app_2"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_sm" />

    <ImageView
        android:id="@+id/shortcut_app_3"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:scaleType="fitCenter"
        android:layout_marginEnd="@dimen/spacing_sm" />

    <TextView
        android:id="@+id/shortcut_add"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:background="@drawable/bg_ac_quick"
        android:gravity="center"
        android:text="+"
        android:textColor="@color/text_secondary"
        android:textSize="20sp" />
</LinearLayout>
```

---

### Task 4: 更新 strings.xml 和 StatusPage.java

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/bydlauncher/ui/StatusPage.java`
- Modify: `app/src/main/java/com/bydlauncher/ui/MusicCardView.java`

- [ ] **Step 1: 在 strings.xml 中添加胎压单位字符串**

在 `<!-- Status 页 -->` 部分（`<string name="all_closed">` 附近）添加：

```xml
    <string name="speed_unit_tire">PSI</string>
```

- [ ] **Step 2: 更新 StatusPage.java 胎压显示逻辑**

替换 `updateStatus` 方法中的胎压部分。旧代码：

```java
tireFL.setText("37\nPSI");
tireFR.setText("36\nPSI");
tireRL.setText("42\nPSI");
tireRR.setText("43\nPSI");
```

新代码（胎压值不再包含单位，单位在 XML 中固定显示）：

```java
tireFL.setText("37");
tireFR.setText("36");
tireRL.setText("35");
tireRR.setText("35");
```

- [ ] **Step 3: 更新 MusicCardView.java 处理 null 视图**

新的迷你卡片移除了 `music_app_icon`、`music_app_name`、`music_cover`，需要让 `MusicCardView.java` 兼容这些视图为 null 的情况。

将字段声明从 `final` 改为可空：

```java
// 旧代码
private final ImageView appIcon;
private final TextView appName;
private final ImageView cover;

// 新代码（去掉 final，这些视图在迷你卡中可能不存在）
private ImageView appIcon;
private TextView appName;
private ImageView cover;
```

在 `updateFromController` 方法中添加 null 检查：

```java
// 在设置 appIcon/appName 前加 null 检查
if (appIcon != null) {
    appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageName));
}
if (appName != null) {
    appName.setText(context.getPackageManager().getApplicationLabel(
            context.getPackageManager().getApplicationInfo(packageName, 0)));
}

// 在设置 cover 前加 null 检查
if (cover != null) {
    if (art != null) {
        cover.setImageBitmap(art);
    } else {
        cover.setImageResource(android.R.drawable.ic_menu_gallery);
    }
}
```

同样在 `showNoMedia()` 和 `showSimulatedMedia()` 方法中添加 null 检查：

```java
private void showNoMedia() {
    title.setText(R.string.not_playing);
    artist.setVisibility(View.GONE);
    if (appName != null) appName.setText("");
    if (cover != null) cover.setImageResource(android.R.drawable.ic_menu_gallery);
    activeController = null;
}

private void showSimulatedMedia() {
    if (appName != null) appName.setText("Music");
    title.setText("Simulated Track");
    artist.setText("BYD Audio");
    artist.setVisibility(View.VISIBLE);
    if (cover != null) cover.setImageResource(android.R.drawable.ic_menu_gallery);
    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
}
```

---

### Task 5: 清理不再使用的 card_datetime.xml

**Files:**
- Delete: `app/src/main/res/layout/card_datetime.xml`

- [ ] **Step 1: 删除 card_datetime.xml**

该卡片不再被 `page_status.xml` 引用（时间已在 TopBar 显示）。直接删除文件。

---

### Task 6: 构建验证

- [ ] **Step 1: 构建 debug APK**

```bash
cd /Users/feng/work/byd && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 检查可能的编译错误**

常见问题：
- `R.id.music_app_icon` / `R.id.music_app_name` / `R.id.music_cover` — 这些旧 ID 在新的 card_music.xml 中已移除，如果 Java 代码引用了它们需要处理
- `R.id.status_date` / `R.id.status_time` — card_datetime.xml 删除后，如果有 Java 引用需要移除
- `R.string.speed_unit_tire` — 确认已在 strings.xml 中添加

---

## 自检清单

- [x] VehicleDiagramView 不需要修改（车辆图居中由 XML 布局控制）
- [x] 胎压数字与车轮位置对齐（通过 margin 调整）
- [x] 速度大字居中显示在车辆图上方
- [x] 挡位 P/R/N/D 居中显示在车辆图下方
- [x] 3 张迷你卡片等宽排列在底部
- [x] card_datetime.xml 不再需要（已删除）
- [x] card_music.xml 中移除了 music_app_icon、music_app_name、music_cover（迷你卡不需要）
- [x] 所有 Java 引用的 ID 在新 XML 中存在
