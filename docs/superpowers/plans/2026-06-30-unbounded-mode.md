# 迪UI 无界模式 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现无界模式——全屏地图/壁纸底层 + 左侧毛玻璃悬浮卡片（聚合车况+音乐+时钟）+ 三指手势 + 应用自定义。

**Architecture:** 在现有单 Activity 的 FrameLayout 中新增 page_unbounded（默认 GONE），通过 switchToUnbounded/switchToStandard 控制两套布局的显隐切换。卡片是 Activity 内的 View（非系统悬浮窗），三指手势通过自定义 GestureDetector 检测。

**Tech Stack:** Android Java, targetSdk 28, 自定义 View, GestureDetector, SharedPreferences, PackageManager

**注意:** 本项目无测试套件，验证靠构建 + 安装到车机/模拟器手动测试。每个 Task 完成后 commit。

---

## 文件结构

### 新增文件

| 文件路径 | 职责 |
|----------|------|
| `app/src/main/res/drawable/bg_card_glass.xml` | 毛玻璃卡片背景 Drawable |
| `app/src/main/res/layout/page_unbounded.xml` | 无界模式全屏布局 |
| `app/src/main/res/layout/view_card_aggregate.xml` | 聚合卡片布局 |
| `app/src/main/res/layout/view_card_music_mini.xml` | 精简音乐卡片布局 |
| `app/src/main/res/layout/view_mini_navbar.xml` | 精简导航栏布局 |
| `app/src/main/java/com/bydlauncher/ui/AggregateCardView.java` | 聚合卡片 View |
| `app/src/main/java/com/bydlauncher/ui/UnboundedPage.java` | 无界模式页面控制器 |
| `app/src/main/java/com/bydlauncher/ui/ThreeFingerGestureDetector.java` | 三指手势检测器 |
| `app/src/main/java/com/bydlauncher/ui/AppSlotManager.java` | 应用槽位管理 |
| `app/src/main/java/com/bydlauncher/ui/AppPickerDialog.java` | 应用选择对话框 |

### 修改文件

| 文件路径 | 改动概要 |
|----------|----------|
| `app/src/main/res/layout/activity_main.xml` | 给标准容器加 id，新增 page_unbounded include |
| `app/src/main/res/layout/page_settings.xml` | 新增布局模式+应用配置设置项 |
| `app/src/main/res/values/strings.xml` | 新增无界模式相关字符串 |
| `app/src/main/java/com/bydlauncher/MainActivity.java` | 模式切换逻辑+动画+UnboundedPage 集成 |
| `app/src/main/java/com/bydlauncher/ui/SettingsPage.java` | 布局模式切换+应用配置 UI |
| `app/src/main/java/com/bydlauncher/ui/StatusPage.java` | pip 快捷入口改用 AppSlotManager |

---

## Task 1: 毛玻璃背景 + 字符串资源

**Files:**
- Create: `app/src/main/res/drawable/bg_card_glass.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 创建 bg_card_glass.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#CC1A1A2E" />
            <corners android:radius="@dimen/card_radius" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <stroke android:width="1dp" android:color="#20FFFFFF" />
            <corners android:radius="@dimen/card_radius" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step 2: 在 strings.xml 中添加无界模式相关字符串**

在 `</resources>` 之前添加：

```xml
    <!-- 无界模式 -->
    <string name="unbounded_tap_nav">点击打开导航</string>
    <string name="unbounded_no_nav">未设置导航应用，请在设置中配置</string>
    <string name="unbounded_app_uninstalled">应用已卸载，请重新选择</string>
    <string name="settings_layout_mode">布局模式</string>
    <string name="settings_layout_standard">标准</string>
    <string name="settings_layout_unbounded">无界</string>
    <string name="settings_app_config">应用配置</string>
    <string name="settings_app_nav">导航应用</string>
    <string name="settings_app_music">音乐应用</string>
    <string name="settings_app_video">视频应用</string>
    <string name="settings_app_phone">电话应用</string>
    <string name="settings_app_not_set">未设置</string>
    <string name="settings_app_pick_title">选择应用</string>
    <string name="settings_app_search_hint">搜索应用…</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/bg_card_glass.xml app/src/main/res/values/strings.xml
git commit -m "feat: add glass card drawable and unbounded mode strings"
```

---

## Task 2: 聚合卡片布局 + View

**Files:**
- Create: `app/src/main/res/layout/view_card_aggregate.xml`
- Create: `app/src/main/java/com/bydlauncher/ui/AggregateCardView.java`

- [ ] **Step 1: 创建 view_card_aggregate.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="260dp"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card_glass"
    android:orientation="vertical"
    android:padding="@dimen/card_padding_sm">

    <!-- 速度+挡位 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:layout_marginBottom="6dp">
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⚡"
            android:textSize="14sp" />
        <TextView
            android:id="@+id/agg_speed"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0"
            android:textColor="@color/text_primary"
            android:textSize="28sp"
            android:textStyle="bold"
            android:layout_marginStart="4dp" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text=" km/h"
            android:textColor="@color/text_tertiary"
            android:textSize="11sp" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView
            android:id="@+id/agg_gear"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="P"
            android:textColor="@color/accent"
            android:textSize="18sp"
            android:textStyle="bold" />
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@color/divider" android:layout_marginBottom="6dp" />

    <!-- 电量+油量 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="2dp">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="🔋 " android:textSize="12sp" />
        <TextView android:id="@+id/agg_battery" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="89%" android:textColor="@color/accent" android:textSize="13sp" android:textStyle="bold" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView android:id="@+id/agg_ev_range" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="90km EV" android:textColor="@color/text_secondary" android:textSize="11sp" />
    </LinearLayout>
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="6dp">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="⛽ " android:textSize="12sp" />
        <TextView android:id="@+id/agg_fuel" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="37%" android:textColor="@color/status_fair" android:textSize="13sp" android:textStyle="bold" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView android:id="@+id/agg_fuel_amount" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="21.0L" android:textColor="@color/text_secondary" android:textSize="11sp" />
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@color/divider" android:layout_marginBottom="6dp" />

    <!-- 胎压 2x2 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="2dp">
        <TextView android:id="@+id/agg_tire_fl" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="FL 250" android:textColor="@color/text_primary" android:textSize="12sp" android:gravity="center" />
        <TextView android:id="@+id/agg_tire_fr" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="FR 252" android:textColor="@color/text_primary" android:textSize="12sp" android:gravity="center" />
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="6dp">
        <TextView android:id="@+id/agg_tire_rl" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="RL 250" android:textColor="@color/text_primary" android:textSize="12sp" android:gravity="center" />
        <TextView android:id="@+id/agg_tire_rr" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="RR 250" android:textColor="@color/text_primary" android:textSize="12sp" android:gravity="center" />
    </LinearLayout>

    <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@color/divider" android:layout_marginBottom="6dp" />

    <!-- 温度+功率 -->
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginBottom="2dp">
        <TextView android:id="@+id/agg_outside_temp" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="车外 26°" android:textColor="@color/text_secondary" android:textSize="12sp" />
        <View android:layout_width="0dp" android:layout_height="0dp" android:layout_weight="1" />
        <TextView android:id="@+id/agg_power" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="0.0kW" android:textColor="@color/accent" android:textSize="12sp" android:textStyle="bold" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 创建 AggregateCardView.java**

```java
package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import com.bydlauncher.R;
import com.bydlauncher.model.VehicleStatus;

public class AggregateCardView {

    private final View rootView;
    private final TextView speed, gear, battery, evRange, fuel, fuelAmount;
    private final TextView tireFL, tireFR, tireRL, tireRR;
    private final TextView outsideTemp, power;

    public AggregateCardView(View rootView) {
        this.rootView = rootView;
        speed = rootView.findViewById(R.id.agg_speed);
        gear = rootView.findViewById(R.id.agg_gear);
        battery = rootView.findViewById(R.id.agg_battery);
        evRange = rootView.findViewById(R.id.agg_ev_range);
        fuel = rootView.findViewById(R.id.agg_fuel);
        fuelAmount = rootView.findViewById(R.id.agg_fuel_amount);
        tireFL = rootView.findViewById(R.id.agg_tire_fl);
        tireFR = rootView.findViewById(R.id.agg_tire_fr);
        tireRL = rootView.findViewById(R.id.agg_tire_rl);
        tireRR = rootView.findViewById(R.id.agg_tire_rr);
        outsideTemp = rootView.findViewById(R.id.agg_outside_temp);
        power = rootView.findViewById(R.id.agg_power);
    }

    public void updateStatus(VehicleStatus s) {
        speed.setText(s.getSpeedText());
        gear.setText(s.getGearText());
        battery.setText(s.getBatteryText());
        evRange.setText(s.getEvMileageText());
        if (s.fuelPercent >= 0) fuel.setText(s.fuelPercent + "%");
        fuelAmount.setText(s.getFuelText());

        if (s.tirePressureFL >= 0) {
            tireFL.setText("FL " + s.tirePressureFL);
            tireFR.setText("FR " + s.tirePressureFR);
            tireRL.setText("RL " + s.tirePressureRL);
            tireRR.setText("RR " + s.tirePressureRR);
        }

        outsideTemp.setText(s.getOutsideTempText());
        power.setText(s.getPowerKwText());
    }

    public View getView() { return rootView; }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/view_card_aggregate.xml app/src/main/java/com/bydlauncher/ui/AggregateCardView.java
git commit -m "feat: add AggregateCardView with speed/tire/battery/temp"
```

---

## Task 3: 精简音乐卡片 + 精简导航栏布局

**Files:**
- Create: `app/src/main/res/layout/view_card_music_mini.xml`
- Create: `app/src/main/res/layout/view_mini_navbar.xml`

- [ ] **Step 1: 创建 view_card_music_mini.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/music_card_mini"
    android:layout_width="260dp"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_card_glass"
    android:orientation="vertical"
    android:padding="@dimen/card_padding_sm">

    <TextView
        android:id="@+id/music_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/not_playing"
        android:textColor="@color/text_primary"
        android:textSize="13sp"
        android:textStyle="bold"
        android:maxLines="1"
        android:ellipsize="end" />

    <TextView
        android:id="@+id/music_artist"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/text_secondary"
        android:textSize="11sp"
        android:maxLines="1"
        android:ellipsize="end"
        android:layout_marginBottom="6dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center">

        <ImageView
            android:id="@+id/music_prev"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@android:drawable/ic_media_previous"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_xl" />

        <ImageView
            android:id="@+id/music_play_pause"
            android:layout_width="28dp"
            android:layout_height="28dp"
            android:src="@android:drawable/ic_media_play"
            android:scaleType="fitCenter"
            android:layout_marginEnd="@dimen/spacing_xl" />

        <ImageView
            android:id="@+id/music_next"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@android:drawable/ic_media_next"
            android:scaleType="fitCenter" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: 创建 view_mini_navbar.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/mini_navbar"
    android:layout_width="160dp"
    android:layout_height="44dp"
    android:layout_gravity="bottom|end"
    android:layout_marginEnd="16dp"
    android:layout_marginBottom="16dp"
    android:background="@drawable/bg_card_glass"
    android:gravity="center"
    android:orientation="horizontal"
    android:paddingStart="12dp"
    android:paddingEnd="12dp">

    <TextView
        android:id="@+id/mini_nav_ac"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:text="❄"
        android:textSize="18sp"
        android:gravity="center" />

    <View android:layout_width="1dp" android:layout_height="20dp" android:background="@color/divider" />

    <TextView
        android:id="@+id/mini_nav_home"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:text="⬚"
        android:textSize="18sp"
        android:gravity="center" />

    <View android:layout_width="1dp" android:layout_height="20dp" android:background="@color/divider" />

    <TextView
        android:id="@+id/mini_nav_settings"
        android:layout_width="0dp"
        android:layout_weight="1"
        android:layout_height="wrap_content"
        android:text="⚙"
        android:textSize="18sp"
        android:gravity="center" />
</LinearLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/view_card_music_mini.xml app/src/main/res/layout/view_mini_navbar.xml
git commit -m "feat: add mini music card and mini navbar layouts"
```

---

## Task 4: page_unbounded 全屏布局

**Files:**
- Create: `app/src/main/res/layout/page_unbounded.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: 创建 page_unbounded.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_unbounded"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_primary"
    android:visibility="gone">

    <!-- 地图/壁纸区域（全屏，点击启动导航） -->
    <FrameLayout
        android:id="@+id/unbounded_map_area"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <!-- 底部提示文字 -->
        <TextView
            android:id="@+id/unbounded_map_hint"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|center_horizontal"
            android:layout_marginBottom="60dp"
            android:text="@string/unbounded_tap_nav"
            android:textColor="@color/text_tertiary"
            android:textSize="13sp"
            android:background="@drawable/bg_card_glass"
            android:paddingStart="16dp"
            android:paddingEnd="16dp"
            android:paddingTop="6dp"
            android:paddingBottom="6dp" />
    </FrameLayout>

    <!-- 左侧卡片栏 -->
    <LinearLayout
        android:id="@+id/unbounded_card_column"
        android:layout_width="260dp"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:paddingStart="12dp"
        android:paddingTop="12dp"
        android:paddingBottom="12dp">

        <!-- 时钟卡片 -->
        <LinearLayout
            android:id="@+id/unbounded_clock_card"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card_glass"
            android:orientation="vertical"
            android:gravity="center"
            android:padding="@dimen/card_padding_sm"
            android:layout_marginBottom="8dp">

            <TextClock
                android:id="@+id/unbounded_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:format24Hour="HH:mm"
                android:format12Hour="hh:mm"
                android:textColor="@color/text_primary"
                android:textSize="36sp"
                android:fontFamily="sans-serif-thin"
                android:textStyle="bold" />

            <TextClock
                android:id="@+id/unbounded_date"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:format24Hour="MM月dd日  EEEE"
                android:format12Hour="MM月dd日  EEEE"
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />
        </LinearLayout>

        <!-- 聚合卡片 -->
        <include
            layout="@layout/view_card_aggregate"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp" />

        <!-- 音乐卡片 -->
        <include
            layout="@layout/view_card_music_mini"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </LinearLayout>

    <!-- 精简导航栏 -->
    <include layout="@layout/view_mini_navbar" />

    <!-- 底部拖拽区域（触发切换到标准模式） -->
    <View
        android:id="@+id/unbounded_drag_zone"
        android:layout_width="match_parent"
        android:layout_height="20dp"
        android:layout_gravity="bottom" />
</FrameLayout>
```

- [ ] **Step 2: 修改 activity_main.xml — 给标准容器加 id + 添加 page_unbounded**

在 `activity_main.xml` 中，给现有的第一个 LinearLayout 加上 `android:id="@+id/standard_container"`：

将：
```xml
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">
```
改为：
```xml
    <LinearLayout
        android:id="@+id/standard_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">
```

在 `</LinearLayout>` (标准容器结束) 和 `<!-- 全屏遮罩层 -->` 之间插入：

```xml
    <!-- 无界模式（默认隐藏） -->
    <include layout="@layout/page_unbounded" />
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/page_unbounded.xml app/src/main/res/layout/activity_main.xml
git commit -m "feat: add page_unbounded layout and integrate into activity_main"
```

---

## Task 5: ThreeFingerGestureDetector

**Files:**
- Create: `app/src/main/java/com/bydlauncher/ui/ThreeFingerGestureDetector.java`

- [ ] **Step 1: 创建 ThreeFingerGestureDetector.java**

```java
package com.bydlauncher.ui;

import android.view.MotionEvent;

public class ThreeFingerGestureDetector {

    public interface Callback {
        void onSwipeDown();
        void onSwipeUp();
        void onSwipeLeft();
        void onSwipeRight();
    }

    private static final float THRESHOLD_DP = 100f;

    private final Callback callback;
    private final float threshold;
    private boolean tracking = false;
    private float startX, startY;
    private boolean consumed = false;

    public ThreeFingerGestureDetector(Callback callback, float density) {
        this.callback = callback;
        this.threshold = THRESHOLD_DP * density;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 3 && !tracking) {
                    tracking = true;
                    consumed = false;
                    startX = avgX(event);
                    startY = avgY(event);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (tracking && pointerCount >= 3 && !consumed) {
                    float dx = avgX(event) - startX;
                    float dy = avgY(event) - startY;

                    if (Math.abs(dy) > threshold && Math.abs(dy) > Math.abs(dx)) {
                        consumed = true;
                        if (dy > 0) callback.onSwipeDown();
                        else callback.onSwipeUp();
                        return true;
                    }
                    if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy)) {
                        consumed = true;
                        if (dx > 0) callback.onSwipeRight();
                        else callback.onSwipeLeft();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                consumed = false;
                break;
        }
        return false;
    }

    private float avgX(MotionEvent e) {
        float sum = 0;
        int count = Math.min(e.getPointerCount(), 3);
        for (int i = 0; i < count; i++) sum += e.getX(i);
        return sum / count;
    }

    private float avgY(MotionEvent e) {
        float sum = 0;
        int count = Math.min(e.getPointerCount(), 3);
        for (int i = 0; i < count; i++) sum += e.getY(i);
        return sum / count;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bydlauncher/ui/ThreeFingerGestureDetector.java
git commit -m "feat: add ThreeFingerGestureDetector for 3-finger swipe detection"
```

---

## Task 6: AppSlotManager + AppPickerDialog

**Files:**
- Create: `app/src/main/java/com/bydlauncher/ui/AppSlotManager.java`
- Create: `app/src/main/java/com/bydlauncher/ui/AppPickerDialog.java`

- [ ] **Step 1: 创建 AppSlotManager.java**

```java
package com.bydlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import com.bydlauncher.R;

import java.util.ArrayList;
import java.util.List;

public class AppSlotManager {

    private static final String TAG = "AppSlotManager";
    private static final String PREFS_NAME = "app_slots";

    public static final String SLOT_NAV = "app_nav";
    public static final String SLOT_MUSIC = "app_music";
    public static final String SLOT_VIDEO = "app_video";
    public static final String SLOT_PHONE = "app_phone";

    private final Context context;
    private final SharedPreferences prefs;

    public AppSlotManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getPackageName(String slot) {
        return prefs.getString(slot, null);
    }

    public void setPackageName(String slot, String packageName) {
        prefs.edit().putString(slot, packageName).apply();
    }

    public boolean isConfigured(String slot) {
        String pkg = getPackageName(slot);
        return pkg != null && isInstalled(pkg);
    }

    public boolean isInstalled(String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String getAppLabel(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) return context.getString(R.string.settings_app_not_set);
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return context.getString(R.string.unbounded_app_uninstalled);
        }
    }

    public Drawable getAppIcon(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) return null;
        try {
            return context.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean launch(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) {
            Toast.makeText(context, R.string.unbounded_no_nav, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isInstalled(pkg)) {
            Toast.makeText(context, R.string.unbounded_app_uninstalled, Toast.LENGTH_SHORT).show();
            prefs.edit().remove(slot).apply();
            return false;
        }
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static class AppInfo {
        public final String packageName;
        public final String label;
        public final Drawable icon;

        public AppInfo(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    public List<AppInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<AppInfo> result = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            if (app.packageName.equals(context.getPackageName())) continue;
            String label = pm.getApplicationLabel(app).toString();
            Drawable icon = pm.getApplicationIcon(app);
            result.add(new AppInfo(app.packageName, label, icon));
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }
}
```

- [ ] **Step 2: 创建 AppPickerDialog.java**

```java
package com.bydlauncher.ui;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.bydlauncher.R;

import java.util.ArrayList;
import java.util.List;

public class AppPickerDialog {

    public interface OnAppSelected {
        void onSelected(String packageName, String label);
    }

    public static void show(Context context, AppSlotManager slotManager, OnAppSelected callback) {
        List<AppSlotManager.AppInfo> allApps = slotManager.getInstalledApps();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), 0);

        EditText search = new EditText(context);
        search.setHint(R.string.settings_app_search_hint);
        search.setTextSize(14);
        search.setSingleLine(true);
        container.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        container.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 400)));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                .setTitle(R.string.settings_app_pick_title)
                .setView(container)
                .setNegativeButton(R.string.perm_btn_cancel, null)
                .create();

        Runnable refreshList = () -> {
            listContainer.removeAllViews();
            String query = search.getText().toString().toLowerCase();
            for (AppSlotManager.AppInfo app : allApps) {
                if (!query.isEmpty() && !app.label.toLowerCase().contains(query)
                        && !app.packageName.toLowerCase().contains(query)) {
                    continue;
                }
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(context, 8), 0, dp(context, 8));
                row.setClickable(true);
                row.setFocusable(true);

                ImageView icon = new ImageView(context);
                icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)));
                icon.setImageDrawable(app.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(icon);

                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                textParams.setMarginStart(dp(context, 12));
                textCol.setLayoutParams(textParams);

                TextView labelTv = new TextView(context);
                labelTv.setText(app.label);
                labelTv.setTextSize(14);
                labelTv.setTextColor(context.getResources().getColor(R.color.text_primary));
                textCol.addView(labelTv);

                TextView pkgTv = new TextView(context);
                pkgTv.setText(app.packageName);
                pkgTv.setTextSize(10);
                pkgTv.setTextColor(context.getResources().getColor(R.color.text_tertiary));
                pkgTv.setSingleLine(true);
                textCol.addView(pkgTv);

                row.addView(textCol);

                row.setOnClickListener(v -> {
                    callback.onSelected(app.packageName, app.label);
                    dialog.dismiss();
                });

                listContainer.addView(row);
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { refreshList.run(); }
        });

        refreshList.run();

        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);
        }
        dialog.show();
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/ui/AppSlotManager.java app/src/main/java/com/bydlauncher/ui/AppPickerDialog.java
git commit -m "feat: add AppSlotManager and AppPickerDialog for user-configurable app slots"
```

---

## Task 7: UnboundedPage 页面控制器

**Files:**
- Create: `app/src/main/java/com/bydlauncher/ui/UnboundedPage.java`

- [ ] **Step 1: 创建 UnboundedPage.java**

```java
package com.bydlauncher.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bydlauncher.R;
import com.bydlauncher.model.VehicleStatus;

public class UnboundedPage {

    public interface ModeSwitch {
        void switchToStandard();
    }

    private final View rootView;
    private final Context context;
    private final AggregateCardView aggregateCard;
    private final MusicCardView musicCard;
    private final LinearLayout cardColumn;
    private final View clockCard;
    private final View mapArea;
    private final TextView mapHint;
    private final View dragZone;
    private final ThreeFingerGestureDetector gestureDetector;

    private ModeSwitch modeSwitch;
    private AppSlotManager appSlotManager;
    private boolean cardsVisible = true;

    // 卡片顺序管理（0=时钟, 1=聚合, 2=音乐）
    private int[] cardOrder = {0, 1, 2};
    private View[] cards;

    public UnboundedPage(View rootView) {
        this.rootView = rootView;
        this.context = rootView.getContext();

        cardColumn = rootView.findViewById(R.id.unbounded_card_column);
        clockCard = rootView.findViewById(R.id.unbounded_clock_card);
        mapArea = rootView.findViewById(R.id.unbounded_map_area);
        mapHint = rootView.findViewById(R.id.unbounded_map_hint);
        dragZone = rootView.findViewById(R.id.unbounded_drag_zone);

        // 聚合卡片
        View aggView = rootView.findViewById(R.id.agg_speed).getRootView()
                .findViewById(R.id.agg_speed);
        // 找到 view_card_aggregate 的根 View
        View aggRoot = findAncestorWithBackground(rootView.findViewById(R.id.agg_speed));
        aggregateCard = new AggregateCardView(aggRoot);

        // 音乐卡片
        View musicRoot = rootView.findViewById(R.id.music_card_mini);
        musicCard = new MusicCardView(musicRoot);

        cards = new View[]{clockCard, aggRoot, musicRoot};

        // 手势检测
        gestureDetector = new ThreeFingerGestureDetector(new ThreeFingerGestureDetector.Callback() {
            @Override
            public void onSwipeDown() { hideCards(); }
            @Override
            public void onSwipeUp() { showCards(); }
            @Override
            public void onSwipeLeft() { rotateCards(-1); }
            @Override
            public void onSwipeRight() { rotateCards(1); }
        }, context.getResources().getDisplayMetrics().density);

        // 触摸事件
        rootView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // 地图区域点击
        mapArea.setOnClickListener(v -> {
            if (appSlotManager != null) {
                appSlotManager.launch(AppSlotManager.SLOT_NAV);
            }
        });

        // 底部拖拽区域
        setupDragZone();

        // 精简导航栏
        setupMiniNavbar();
    }

    private View findAncestorWithBackground(View view) {
        View current = view;
        while (current.getParent() instanceof View) {
            View parent = (View) current.getParent();
            if (parent.getId() == R.id.unbounded_card_column || parent == rootView) return current;
            current = parent;
        }
        return view;
    }

    public void setModeSwitch(ModeSwitch modeSwitch) {
        this.modeSwitch = modeSwitch;
    }

    public void setAppSlotManager(AppSlotManager appSlotManager) {
        this.appSlotManager = appSlotManager;
        updateMapHint();
    }

    private void updateMapHint() {
        if (appSlotManager == null || !appSlotManager.isConfigured(AppSlotManager.SLOT_NAV)) {
            mapHint.setText(R.string.unbounded_no_nav);
        } else {
            mapHint.setText(R.string.unbounded_tap_nav);
        }
    }

    public void updateStatus(VehicleStatus status) {
        aggregateCard.updateStatus(status);
    }

    public void refreshMusic() {
        musicCard.refreshMediaState();
    }

    // ========== 卡片显隐 ==========

    private void hideCards() {
        if (!cardsVisible) return;
        cardsVisible = false;
        cardColumn.animate()
                .translationX(-cardColumn.getWidth())
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> cardColumn.setVisibility(View.GONE))
                .start();
    }

    private void showCards() {
        if (cardsVisible) return;
        cardsVisible = true;
        cardColumn.setVisibility(View.VISIBLE);
        cardColumn.setTranslationX(-cardColumn.getWidth());
        cardColumn.setAlpha(0f);
        cardColumn.animate()
                .translationX(0)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // ========== 卡片顺序轮换 ==========

    private void rotateCards(int direction) {
        if (direction > 0) {
            int last = cardOrder[cardOrder.length - 1];
            System.arraycopy(cardOrder, 0, cardOrder, 1, cardOrder.length - 1);
            cardOrder[0] = last;
        } else {
            int first = cardOrder[0];
            System.arraycopy(cardOrder, 1, cardOrder, 0, cardOrder.length - 1);
            cardOrder[cardOrder.length - 1] = first;
        }
        reorderCards();
    }

    private void reorderCards() {
        cardColumn.removeAllViews();
        for (int idx : cardOrder) {
            View card = cards[idx];
            if (card.getParent() != null) {
                ((android.view.ViewGroup) card.getParent()).removeView(card);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
            cardColumn.addView(card, params);
        }
    }

    // ========== 底部拖拽 ==========

    private void setupDragZone() {
        dragZone.setOnTouchListener(new View.OnTouchListener() {
            private float startY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dy = startY - event.getRawY();
                        float threshold = 80 * context.getResources().getDisplayMetrics().density;
                        if (dy > threshold) {
                            if (modeSwitch != null) modeSwitch.switchToStandard();
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    // ========== 精简导航栏 ==========

    private void setupMiniNavbar() {
        View navAc = rootView.findViewById(R.id.mini_nav_ac);
        View navHome = rootView.findViewById(R.id.mini_nav_home);
        View navSettings = rootView.findViewById(R.id.mini_nav_settings);

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (modeSwitch != null) modeSwitch.switchToStandard();
            });
        }
    }

    public View getView() { return rootView; }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bydlauncher/ui/UnboundedPage.java
git commit -m "feat: add UnboundedPage controller with gesture, cards, map launch, drag zone"
```

---

## Task 8: MainActivity 模式切换集成

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/MainActivity.java`

- [ ] **Step 1: 添加字段和导入**

在 MainActivity.java 中添加字段（在 `private int currentTab = 0;` 之后）：

```java
    private View standardContainer;
    private View unboundedContainer;
    private UnboundedPage unboundedPage;
    private AppSlotManager appSlotManager;
    private boolean isUnboundedMode = false;
```

添加 import：
```java
import com.bydlauncher.ui.UnboundedPage;
import com.bydlauncher.ui.AppSlotManager;
```

- [ ] **Step 2: 在 onCreate 中初始化无界模式**

在 `setContentView(R.layout.activity_main);` 之后、环境检测代码之前添加：

```java
        standardContainer = findViewById(R.id.standard_container);
        unboundedContainer = findViewById(R.id.page_unbounded);
        appSlotManager = new AppSlotManager(this);
```

在 `settingsPage.setOnSimModeChangedListener(this::reinitializeWithSimMode);` 之后添加：

```java
        // 无界模式初始化
        if (unboundedContainer != null) {
            unboundedPage = new UnboundedPage(unboundedContainer);
            unboundedPage.setModeSwitch(this::switchToStandard);
            unboundedPage.setAppSlotManager(appSlotManager);
        }

        // 读取上次的布局模式
        String layoutMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("layout_mode", "standard");
        if ("unbounded".equals(layoutMode) && unboundedContainer != null) {
            switchToUnboundedImmediate();
        }
```

- [ ] **Step 3: 添加模式切换方法**

在 `onBackPressed()` 之前添加：

```java
    public void switchToUnbounded() {
        if (isUnboundedMode) return;
        isUnboundedMode = true;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString("layout_mode", "unbounded").apply();

        unboundedContainer.setVisibility(View.VISIBLE);
        unboundedContainer.setAlpha(0f);
        unboundedContainer.animate().alpha(1f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        standardContainer.animate().alpha(0f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> standardContainer.setVisibility(View.GONE))
                .start();
    }

    public void switchToStandard() {
        if (!isUnboundedMode) return;
        isUnboundedMode = false;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString("layout_mode", "standard").apply();

        standardContainer.setVisibility(View.VISIBLE);
        standardContainer.setAlpha(0f);
        standardContainer.animate().alpha(1f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        unboundedContainer.animate().alpha(0f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> unboundedContainer.setVisibility(View.GONE))
                .start();
    }

    private void switchToUnboundedImmediate() {
        isUnboundedMode = true;
        standardContainer.setVisibility(View.GONE);
        unboundedContainer.setVisibility(View.VISIBLE);
    }
```

- [ ] **Step 4: 在 onStatusUpdated 中同步更新无界模式**

在 `onStatusUpdated` 方法的 `runOnUiThread` lambda 中，`WindowPanelController.updateFromStatus(...)` 之后添加：

```java
            if (unboundedPage != null && isUnboundedMode) {
                unboundedPage.updateStatus(status);
                unboundedPage.refreshMusic();
            }
```

- [ ] **Step 5: 修改 onBackPressed — 无界模式下按返回切回标准**

替换现有的 `onBackPressed()` 方法：

```java
    @Override
    public void onBackPressed() {
        if (isUnboundedMode) {
            switchToStandard();
        } else if (currentTab != 0) {
            navBar.selectTab(0);
        }
    }
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "feat: integrate unbounded mode switching into MainActivity"
```

---

## Task 9: SettingsPage 布局模式 + 应用配置

**Files:**
- Modify: `app/src/main/res/layout/page_settings.xml`
- Modify: `app/src/main/java/com/bydlauncher/ui/SettingsPage.java`

- [ ] **Step 1: 在 page_settings.xml 中添加布局模式设置**

在 `<!-- ═══ 车辆 ═══ -->` 之前插入：

```xml
        <!-- ═══ 布局 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_layout_mode"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/settings_section_title_size"
            android:textAllCaps="true"
            android:letterSpacing="0.08"
            android:layout_marginBottom="@dimen/spacing_md" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card"
            android:orientation="vertical"
            android:paddingStart="@dimen/card_padding"
            android:paddingEnd="@dimen/card_padding"
            android:layout_marginBottom="@dimen/spacing_xl">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🖼"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_layout_mode"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/settings_item_title_size"
                    android:layout_marginStart="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">
                    <TextView
                        android:id="@+id/settings_layout_standard"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="@string/settings_layout_standard"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_layout_unbounded"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="@string/settings_layout_unbounded"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>

        <!-- ═══ 应用配置 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_app_config"
            android:textColor="@color/text_secondary"
            android:textSize="@dimen/settings_section_title_size"
            android:textAllCaps="true"
            android:letterSpacing="0.08"
            android:layout_marginBottom="@dimen/spacing_md" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_card"
            android:orientation="vertical"
            android:paddingStart="@dimen/card_padding"
            android:paddingEnd="@dimen/card_padding"
            android:layout_marginBottom="@dimen/spacing_xl">

            <!-- 导航应用 -->
            <LinearLayout
                android:id="@+id/settings_slot_nav"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="?android:attr/selectableItemBackground">
                <TextView android:layout_width="@dimen/settings_icon_size" android:layout_height="wrap_content" android:text="🗺" android:textSize="@dimen/settings_icon_text_size" android:gravity="center" />
                <TextView android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="@string/settings_app_nav" android:textColor="@color/text_primary" android:textSize="@dimen/settings_item_title_size" android:layout_marginStart="@dimen/spacing_md" />
                <TextView android:id="@+id/settings_slot_nav_value" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/settings_app_not_set" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginEnd="@dimen/spacing_sm" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="@color/text_tertiary" android:textSize="20sp" />
            </LinearLayout>

            <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@drawable/bg_settings_divider" android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 音乐应用 -->
            <LinearLayout
                android:id="@+id/settings_slot_music"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="?android:attr/selectableItemBackground">
                <TextView android:layout_width="@dimen/settings_icon_size" android:layout_height="wrap_content" android:text="🎵" android:textSize="@dimen/settings_icon_text_size" android:gravity="center" />
                <TextView android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="@string/settings_app_music" android:textColor="@color/text_primary" android:textSize="@dimen/settings_item_title_size" android:layout_marginStart="@dimen/spacing_md" />
                <TextView android:id="@+id/settings_slot_music_value" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/settings_app_not_set" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginEnd="@dimen/spacing_sm" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="@color/text_tertiary" android:textSize="20sp" />
            </LinearLayout>

            <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@drawable/bg_settings_divider" android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 视频应用 -->
            <LinearLayout
                android:id="@+id/settings_slot_video"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="?android:attr/selectableItemBackground">
                <TextView android:layout_width="@dimen/settings_icon_size" android:layout_height="wrap_content" android:text="📺" android:textSize="@dimen/settings_icon_text_size" android:gravity="center" />
                <TextView android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="@string/settings_app_video" android:textColor="@color/text_primary" android:textSize="@dimen/settings_item_title_size" android:layout_marginStart="@dimen/spacing_md" />
                <TextView android:id="@+id/settings_slot_video_value" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/settings_app_not_set" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginEnd="@dimen/spacing_sm" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="@color/text_tertiary" android:textSize="20sp" />
            </LinearLayout>

            <View android:layout_width="match_parent" android:layout_height="1dp" android:background="@drawable/bg_settings_divider" android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 电话应用 -->
            <LinearLayout
                android:id="@+id/settings_slot_phone"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="?android:attr/selectableItemBackground">
                <TextView android:layout_width="@dimen/settings_icon_size" android:layout_height="wrap_content" android:text="📞" android:textSize="@dimen/settings_icon_text_size" android:gravity="center" />
                <TextView android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:text="@string/settings_app_phone" android:textColor="@color/text_primary" android:textSize="@dimen/settings_item_title_size" android:layout_marginStart="@dimen/spacing_md" />
                <TextView android:id="@+id/settings_slot_phone_value" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="@string/settings_app_not_set" android:textColor="@color/text_secondary" android:textSize="12sp" android:layout_marginEnd="@dimen/spacing_sm" />
                <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="›" android:textColor="@color/text_tertiary" android:textSize="20sp" />
            </LinearLayout>
        </LinearLayout>
```

- [ ] **Step 2: 修改 SettingsPage.java — 添加布局模式和应用配置**

添加 import：
```java
import com.bydlauncher.ui.AppSlotManager;
import com.bydlauncher.ui.AppPickerDialog;
```

在构造函数的 `initApiProbe();` 之后添加：
```java
        initLayoutMode();
        initAppSlots();
```

添加字段（在现有字段声明区域）：
```java
    private AppSlotManager appSlotManager;
    private TextView btnLayoutStandard, btnLayoutUnbounded;

    public interface OnLayoutModeChangedListener {
        void onLayoutModeChanged(boolean isUnbounded);
    }
    private OnLayoutModeChangedListener layoutModeChangedListener;
```

添加 setter（在 `setOnAdbAuthorizeListener` 附近）：
```java
    public void setOnLayoutModeChangedListener(OnLayoutModeChangedListener listener) {
        this.layoutModeChangedListener = listener;
    }

    public void setAppSlotManager(AppSlotManager manager) {
        this.appSlotManager = manager;
        refreshSlotLabels();
    }
```

添加方法：
```java
    private void initLayoutMode() {
        btnLayoutStandard = rootView.findViewById(R.id.settings_layout_standard);
        btnLayoutUnbounded = rootView.findViewById(R.id.settings_layout_unbounded);
        if (btnLayoutStandard == null || btnLayoutUnbounded == null) return;

        String mode = prefs.getString("layout_mode", "standard");
        highlightLayoutMode("unbounded".equals(mode));

        btnLayoutStandard.setOnClickListener(v -> {
            prefs.edit().putString("layout_mode", "standard").apply();
            highlightLayoutMode(false);
            if (layoutModeChangedListener != null) layoutModeChangedListener.onLayoutModeChanged(false);
        });
        btnLayoutUnbounded.setOnClickListener(v -> {
            prefs.edit().putString("layout_mode", "unbounded").apply();
            highlightLayoutMode(true);
            if (layoutModeChangedListener != null) layoutModeChangedListener.onLayoutModeChanged(true);
        });
    }

    private void highlightLayoutMode(boolean isUnbounded) {
        if (btnLayoutStandard == null) return;
        setSegmentActive(btnLayoutStandard, !isUnbounded);
        setSegmentActive(btnLayoutUnbounded, isUnbounded);
    }

    private void initAppSlots() {
        setupSlot(R.id.settings_slot_nav, R.id.settings_slot_nav_value, AppSlotManager.SLOT_NAV);
        setupSlot(R.id.settings_slot_music, R.id.settings_slot_music_value, AppSlotManager.SLOT_MUSIC);
        setupSlot(R.id.settings_slot_video, R.id.settings_slot_video_value, AppSlotManager.SLOT_VIDEO);
        setupSlot(R.id.settings_slot_phone, R.id.settings_slot_phone_value, AppSlotManager.SLOT_PHONE);
    }

    private void setupSlot(int rowId, int valueId, String slot) {
        View row = rootView.findViewById(rowId);
        TextView value = rootView.findViewById(valueId);
        if (row == null || value == null) return;

        row.setOnClickListener(v -> {
            if (appSlotManager == null) return;
            AppPickerDialog.show(context, appSlotManager, (packageName, label) -> {
                appSlotManager.setPackageName(slot, packageName);
                value.setText(label);
            });
        });
    }

    private void refreshSlotLabels() {
        if (appSlotManager == null) return;
        updateSlotLabel(R.id.settings_slot_nav_value, AppSlotManager.SLOT_NAV);
        updateSlotLabel(R.id.settings_slot_music_value, AppSlotManager.SLOT_MUSIC);
        updateSlotLabel(R.id.settings_slot_video_value, AppSlotManager.SLOT_VIDEO);
        updateSlotLabel(R.id.settings_slot_phone_value, AppSlotManager.SLOT_PHONE);
    }

    private void updateSlotLabel(int viewId, String slot) {
        TextView tv = rootView.findViewById(viewId);
        if (tv != null && appSlotManager != null) {
            tv.setText(appSlotManager.getAppLabel(slot));
        }
    }
```

- [ ] **Step 3: 在 MainActivity 中连接 SettingsPage 的布局模式回调**

在 `settingsPage.setOnSimModeChangedListener(this::reinitializeWithSimMode);` 之后添加：

```java
        settingsPage.setOnLayoutModeChangedListener(isUnbounded -> {
            if (isUnbounded) switchToUnbounded();
            else switchToStandard();
        });
        settingsPage.setAppSlotManager(appSlotManager);
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/page_settings.xml app/src/main/java/com/bydlauncher/ui/SettingsPage.java app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "feat: add layout mode switch and app slot config to settings"
```

---

## Task 10: StatusPage pip 快捷入口改用 AppSlotManager

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/ui/StatusPage.java`

- [ ] **Step 1: 修改 StatusPage 使用 AppSlotManager**

添加字段和 setter：

```java
    private AppSlotManager appSlotManager;

    public void setAppSlotManager(AppSlotManager manager) {
        this.appSlotManager = manager;
    }
```

修改 `initPipArea()` 方法，将硬编码的包名替换为 AppSlotManager：

```java
    private void initPipArea() {
        rootView.findViewById(R.id.pip_map).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_NAV)) {
                appSlotManager.launch(AppSlotManager.SLOT_NAV);
            } else {
                launchApp("com.autonavi.minimap", "高德地图");
            }
        });
        rootView.findViewById(R.id.pip_music).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_MUSIC)) {
                appSlotManager.launch(AppSlotManager.SLOT_MUSIC);
            } else {
                launchApp("com.tencent.qqmusic", "QQ音乐");
            }
        });
        rootView.findViewById(R.id.pip_video).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_VIDEO)) {
                appSlotManager.launch(AppSlotManager.SLOT_VIDEO);
            } else {
                launchApp("com.tencent.qqlive", "腾讯视频");
            }
        });
        rootView.findViewById(R.id.pip_phone).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_PHONE)) {
                appSlotManager.launch(AppSlotManager.SLOT_PHONE);
            } else {
                launchApp("com.android.dialer", "电话");
            }
        });
    }
```

在 MainActivity 的 `statusPage = new StatusPage(pageStatusView);` 之后添加：

```java
        statusPage.setAppSlotManager(appSlotManager);
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bydlauncher/ui/StatusPage.java app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "feat: StatusPage pip shortcuts use AppSlotManager with fallback"
```

---

## Task 11: 最终检查

- [ ] **Step 1: 确认所有新增文件存在**

```bash
ls -la app/src/main/res/drawable/bg_card_glass.xml
ls -la app/src/main/res/layout/page_unbounded.xml
ls -la app/src/main/res/layout/view_card_aggregate.xml
ls -la app/src/main/res/layout/view_card_music_mini.xml
ls -la app/src/main/res/layout/view_mini_navbar.xml
ls -la app/src/main/java/com/bydlauncher/ui/AggregateCardView.java
ls -la app/src/main/java/com/bydlauncher/ui/UnboundedPage.java
ls -la app/src/main/java/com/bydlauncher/ui/ThreeFingerGestureDetector.java
ls -la app/src/main/java/com/bydlauncher/ui/AppSlotManager.java
ls -la app/src/main/java/com/bydlauncher/ui/AppPickerDialog.java
```

- [ ] **Step 2: 确认 git 状态干净**

```bash
git status
git log --oneline -12
```
