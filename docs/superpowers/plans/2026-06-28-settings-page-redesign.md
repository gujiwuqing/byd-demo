# 设置页面 UI 重设计 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将设置页面从双列网格布局重构为单列列表式布局，提升视觉质量和操作体验。

**Architecture:** 单列列表式布局，每个设置组用卡片包裹，组内设置项用细线分隔。控件混合使用：分段按钮（多选项）、Toggle Switch（二选一）、右箭头（导航项）。图标使用 emoji 字符（与 NavBar 保持一致）。

**Tech Stack:** Android XML 布局、Java、Shape Drawable、SharedPreferences

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| 修改 | `app/src/main/res/layout/page_settings.xml` | 设置页面布局（完全重写） |
| 修改 | `app/src/main/java/com/bydlauncher/ui/SettingsPage.java` | 设置页面逻辑（更新控件引用 + Toggle Switch） |
| 修改 | `app/src/main/res/values/dimens.xml` | 新增设置页专用尺寸 |
| 修改 | `app/src/main/res/values/strings.xml` | 新增设置页字符串 |
| 创建 | `app/src/main/res/drawable/bg_settings_segment_selected.xml` | 分段按钮选中态背景 |
| 创建 | `app/src/main/res/drawable/bg_settings_segment_unselected.xml` | 分段按钮未选中态背景 |
| 创建 | `app/src/main/res/drawable/bg_settings_switch_track_on.xml` | Toggle Switch 开启态轨道 |
| 创建 | `app/src/main/res/drawable/bg_settings_switch_track_off.xml` | Toggle Switch 关闭态轨道 |
| 创建 | `app/src/main/res/drawable/bg_settings_switch_thumb.xml` | Toggle Switch 圆形滑块 |
| 创建 | `app/src/main/res/drawable/bg_settings_divider.xml` | 设置项分隔线 |

---

### Task 1: 创建 drawable 资源

**Files:**
- Create: `app/src/main/res/drawable/bg_settings_segment_selected.xml`
- Create: `app/src/main/res/drawable/bg_settings_segment_unselected.xml`
- Create: `app/src/main/res/drawable/bg_settings_switch_track_on.xml`
- Create: `app/src/main/res/drawable/bg_settings_switch_track_off.xml`
- Create: `app/src/main/res/drawable/bg_settings_switch_thumb.xml`
- Create: `app/src/main/res/drawable/bg_settings_divider.xml`

- [ ] **Step 1: 创建分段按钮选中态背景**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/accent" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 2: 创建分段按钮未选中态背景**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_surface_variant" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 3: 创建 Toggle Switch 开启态轨道**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/accent" />
    <corners android:radius="12dp" />
    <size android:width="48dp" android:height="24dp" />
</shape>
```

- [ ] **Step 4: 创建 Toggle Switch 关闭态轨道**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/bg_surface_variant" />
    <corners android:radius="12dp" />
    <size android:width="48dp" android:height="24dp" />
</shape>
```

- [ ] **Step 5: 创建 Toggle Switch 圆形滑块**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="@color/text_on_accent" />
    <size android:width="20dp" android:height="20dp" />
</shape>
```

- [ ] **Step 6: 创建分隔线 drawable**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/border" />
    <size android:height="1dp" />
</shape>
```

---

### Task 2: 更新 dimens.xml 和 strings.xml

**Files:**
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 在 dimens.xml 末尾添加设置页专用尺寸**

在 `</resources>` 前插入：

```xml
    <!-- Settings 页 -->
    <dimen name="settings_title_size">28sp</dimen>
    <dimen name="settings_section_title_size">14sp</dimen>
    <dimen name="settings_item_title_size">15sp</dimen>
    <dimen name="settings_item_desc_size">13sp</dimen>
    <dimen name="settings_icon_size">24dp</dimen>
    <dimen name="settings_icon_text_size">20sp</dimen>
    <dimen name="settings_item_min_height">64dp</dimen>
    <dimen name="settings_item_padding_v">12dp</dimen>
    <dimen name="settings_segment_height">32dp</dimen>
    <dimen name="settings_segment_padding_h">14dp</dimen>
    <dimen name="settings_segment_text_size">13sp</dimen>
    <dimen name="settings_switch_track_width">48dp</dimen>
    <dimen name="settings_switch_track_height">24dp</dimen>
    <dimen name="settings_switch_thumb_size">20dp</dimen>
    <dimen name="settings_divider_indent">52dp</dimen>
```

- [ ] **Step 2: 在 strings.xml 中更新设置页字符串**

替换 Settings 页部分（从 `<!-- Settings 页 -->` 开始到 `</resources>` 前）：

```xml
    <!-- Settings 页 -->
    <string name="settings_title">设置</string>
    <string name="settings_display">显示</string>
    <string name="settings_theme">主题模式</string>
    <string name="settings_theme_system">跟随系统</string>
    <string name="settings_theme_light">浅色</string>
    <string name="settings_theme_dark">深色</string>
    <string name="settings_clock_format">时钟格式</string>
    <string name="settings_clock_24h_desc">显示 24 小时制时间</string>
    <string name="settings_clock_24h">24小时制</string>
    <string name="settings_clock_12h">12小时制</string>

    <string name="settings_units">单位</string>
    <string name="settings_temp_unit">温度单位</string>
    <string name="settings_pressure_unit">胎压单位</string>

    <string name="settings_vehicle">车辆</string>
    <string name="settings_car_model">车型</string>
    <string name="settings_car_model_default">宋PLUS DM-i</string>
    <string name="settings_sim_mode">模拟模式</string>
    <string name="settings_sim_mode_desc">非 BYD 设备上自动启用，显示模拟数据</string>
    <string name="settings_sim_on">已启用</string>
    <string name="settings_sim_off">已关闭（真车模式）</string>

    <string name="settings_launcher">桌面</string>
    <string name="settings_default_launcher">设为默认桌面</string>
    <string name="settings_default_launcher_desc">点击后系统会询问选择默认桌面应用</string>

    <string name="settings_about">关于</string>
    <string name="settings_version">版本</string>
    <string name="settings_version_value">1.0.0</string>
    <string name="settings_reference">设计参考</string>
    <string name="settings_reference_value">Kinex Launcher</string>
    <string name="settings_target_device">目标设备</string>
    <string name="settings_target_device_value">BYD DiLink 3.0 · 12-13英寸横屏</string>
```

---

### Task 3: 重写 page_settings.xml 布局

**Files:**
- Modify: `app/src/main/res/layout/page_settings.xml`（完全重写）

- [ ] **Step 1: 重写为单列列表式布局**

完全替换 `page_settings.xml` 内容：

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/page_settings"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:visibility="gone">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/page_padding">

        <!-- 页面标题 -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_title"
            android:textColor="@color/text_primary"
            android:textSize="@dimen/settings_title_size"
            android:textStyle="bold"
            android:layout_marginBottom="@dimen/spacing_xl" />

        <!-- ═══ 显示 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_display"
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

            <!-- 主题模式 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🎨"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_theme"
                        android:textColor="@color/text_primary"
                        android:textSize="@dimen/settings_item_title_size" />
                </LinearLayout>

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">
                    <TextView
                        android:id="@+id/settings_theme_system"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="@string/settings_theme_system"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_theme_light"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="@string/settings_theme_light"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_theme_dark"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="@string/settings_theme_dark"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size" />
                </LinearLayout>
            </LinearLayout>

            <!-- 分隔线 -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@drawable/bg_settings_divider"
                android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 时钟格式（Toggle Switch） -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🕐"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/settings_md">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_clock_format"
                        android:textColor="@color/text_primary"
                        android:textSize="@dimen/settings_item_title_size" />
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_clock_24h_desc"
                        android:textColor="@color/text_tertiary"
                        android:textSize="@dimen/settings_item_desc_size"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <FrameLayout
                    android:id="@+id/settings_clock_switch"
                    android:layout_width="@dimen/settings_switch_track_width"
                    android:layout_height="@dimen/settings_switch_track_height">
                    <View
                        android:id="@+id/settings_clock_track"
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:background="@drawable/bg_settings_switch_track_off" />
                    <View
                        android:id="@+id/settings_clock_thumb"
                        android:layout_width="@dimen/settings_switch_thumb_size"
                        android:layout_height="@dimen/settings_switch_thumb_size"
                        android:layout_gravity="center_vertical|start"
                        android:layout_marginStart="2dp"
                        android:background="@drawable/bg_settings_switch_thumb" />
                </FrameLayout>
            </LinearLayout>
        </LinearLayout>

        <!-- ═══ 单位 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_units"
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

            <!-- 温度单位 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🌡"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_temp_unit"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/settings_item_title_size"
                    android:layout_marginStart="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">
                    <TextView
                        android:id="@+id/settings_temp_c"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:text="°C"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_temp_f"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="16dp"
                        android:paddingEnd="16dp"
                        android:text="°F"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size" />
                </LinearLayout>
            </LinearLayout>

            <!-- 分隔线 -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@drawable/bg_settings_divider"
                android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 胎压单位 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="📊"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_pressure_unit"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/settings_item_title_size"
                    android:layout_marginStart="@dimen/spacing_md" />

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal">
                    <TextView
                        android:id="@+id/settings_psi"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="PSI"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_kpa"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="kPa"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size"
                        android:layout_marginEnd="2dp" />
                    <TextView
                        android:id="@+id/settings_bar"
                        android:layout_width="wrap_content"
                        android:layout_height="@dimen/settings_segment_height"
                        android:background="@drawable/bg_settings_segment_unselected"
                        android:gravity="center"
                        android:paddingStart="@dimen/settings_segment_padding_h"
                        android:paddingEnd="@dimen/settings_segment_padding_h"
                        android:text="bar"
                        android:textColor="@color/text_secondary"
                        android:textSize="@dimen/settings_segment_text_size" />
                </LinearLayout>
            </LinearLayout>
        </LinearLayout>

        <!-- ═══ 车辆 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_vehicle"
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

            <!-- 车型（右箭头） -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🚗"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_car_model"
                    android:textColor="@color/text_primary"
                    android:textSize="@dimen/settings_item_title_size"
                    android:layout_marginStart="@dimen/spacing_md" />

                <TextView
                    android:id="@+id/settings_car_model_value"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/settings_car_model_default"
                    android:textColor="@color/text_secondary"
                    android:textSize="14sp"
                    android:layout_marginEnd="@dimen/spacing_sm" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="›"
                    android:textColor="@color/text_tertiary"
                    android:textSize="20sp" />
            </LinearLayout>

            <!-- 分隔线 -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@drawable/bg_settings_divider"
                android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 模拟模式 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🔧"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_sim_mode"
                        android:textColor="@color/text_primary"
                        android:textSize="@dimen/settings_item_title_size" />
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_sim_mode_desc"
                        android:textColor="@color/text_tertiary"
                        android:textSize="@dimen/settings_item_desc_size"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <TextView
                    android:id="@+id/settings_sim_status"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textSize="14sp" />
            </LinearLayout>

            <!-- 分隔线 -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@drawable/bg_settings_divider"
                android:layout_marginStart="@dimen/settings_divider_indent" />

            <!-- 设为默认桌面（右箭头） -->
            <LinearLayout
                android:id="@+id/settings_set_default"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/settings_item_min_height"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="?android:attr/selectableItemBackground">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="⭐"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:layout_marginStart="@dimen/spacing_md">
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_default_launcher"
                        android:textColor="@color/text_primary"
                        android:textSize="@dimen/settings_item_title_size" />
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_default_launcher_desc"
                        android:textColor="@color/text_tertiary"
                        android:textSize="@dimen/settings_item_desc_size"
                        android:layout_marginTop="2dp" />
                </LinearLayout>

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="›"
                    android:textColor="@color/text_tertiary"
                    android:textSize="20sp" />
            </LinearLayout>
        </LinearLayout>

        <!-- ═══ 关于 ═══ -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_about"
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
            android:padding="@dimen/card_padding"
            android:layout_marginBottom="@dimen/spacing_xl">

            <!-- 应用名称 + 图标 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:layout_marginBottom="@dimen/spacing_md">

                <TextView
                    android:layout_width="@dimen/settings_icon_size"
                    android:layout_height="wrap_content"
                    android:text="🚀"
                    android:textSize="@dimen/settings_icon_text_size"
                    android:gravity="center" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="BYD Launcher V2"
                    android:textColor="@color/text_primary"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginStart="@dimen/spacing_md" />
            </LinearLayout>

            <!-- 版本 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/settings_divider_indent"
                android:layout_marginBottom="@dimen/spacing_sm">
                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_version"
                    android:textColor="@color/text_secondary"
                    android:textSize="14sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/settings_version_value"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />
            </LinearLayout>

            <!-- 设计参考 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/settings_divider_indent"
                android:layout_marginBottom="@dimen/spacing_sm">
                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_reference"
                    android:textColor="@color/text_secondary"
                    android:textSize="14sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/settings_reference_value"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />
            </LinearLayout>

            <!-- 目标设备 -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/settings_divider_indent">
                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="@string/settings_target_device"
                    android:textColor="@color/text_secondary"
                    android:textSize="14sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/settings_target_device_value"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp" />
            </LinearLayout>
        </LinearLayout>

    </LinearLayout>
</ScrollView>
```

---

### Task 4: 重写 SettingsPage.java

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/ui/SettingsPage.java`（完全重写）

- [ ] **Step 1: 重写 SettingsPage.java 支持新布局和 Toggle Switch**

完全替换 `SettingsPage.java` 内容：

```java
package com.bydlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.theme.ThemeManager;

public class SettingsPage {

    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_CLOCK_24H = "clock_24h";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_PRESSURE_UNIT = "pressure_unit";

    public static final int TEMP_C = 0;
    public static final int TEMP_F = 1;
    public static final int PRESSURE_PSI = 0;
    public static final int PRESSURE_KPA = 1;
    public static final int PRESSURE_BAR = 2;

    private final View rootView;
    private final Context context;
    private final ThemeManager themeManager;
    private final SharedPreferences prefs;

    // 主题分段按钮
    private final TextView btnThemeSystem, btnThemeLight, btnThemeDark;
    // 时钟 Toggle Switch
    private final FrameLayout clockSwitch;
    private final View clockTrack, clockThumb;
    // 温度分段按钮
    private final TextView btnTempC, btnTempF;
    // 胎压分段按钮
    private final TextView btnPsi, btnKpa, btnBar;
    // 状态
    private final TextView simStatus;

    public SettingsPage(View rootView, boolean isSimulation) {
        this.rootView = rootView;
        this.context = rootView.getContext();
        this.themeManager = ThemeManager.getInstance(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        btnThemeSystem = rootView.findViewById(R.id.settings_theme_system);
        btnThemeLight = rootView.findViewById(R.id.settings_theme_light);
        btnThemeDark = rootView.findViewById(R.id.settings_theme_dark);

        clockSwitch = rootView.findViewById(R.id.settings_clock_switch);
        clockTrack = rootView.findViewById(R.id.settings_clock_track);
        clockThumb = rootView.findViewById(R.id.settings_clock_thumb);

        btnTempC = rootView.findViewById(R.id.settings_temp_c);
        btnTempF = rootView.findViewById(R.id.settings_temp_f);

        btnPsi = rootView.findViewById(R.id.settings_psi);
        btnKpa = rootView.findViewById(R.id.settings_kpa);
        btnBar = rootView.findViewById(R.id.settings_bar);

        simStatus = rootView.findViewById(R.id.settings_sim_status);

        initThemeButtons();
        initClockSwitch();
        initTempUnitButtons();
        initPressureButtons();
        initSimStatus(isSimulation);
        initDefaultLauncher();
    }

    // ── 主题 ──

    private void initThemeButtons() {
        btnThemeSystem.setOnClickListener(v -> setTheme(ThemeManager.MODE_SYSTEM));
        btnThemeLight.setOnClickListener(v -> setTheme(ThemeManager.MODE_LIGHT));
        btnThemeDark.setOnClickListener(v -> setTheme(ThemeManager.MODE_DARK));
        highlightTheme();
    }

    private void setTheme(int mode) {
        themeManager.setThemeMode(mode);
    }

    private void highlightTheme() {
        int current = themeManager.getThemeMode();
        setSegmentActive(btnThemeSystem, current == ThemeManager.MODE_SYSTEM);
        setSegmentActive(btnThemeLight, current == ThemeManager.MODE_LIGHT);
        setSegmentActive(btnThemeDark, current == ThemeManager.MODE_DARK);
    }

    // ── 时钟 Toggle Switch ──

    private void initClockSwitch() {
        boolean is24h = prefs.getBoolean(KEY_CLOCK_24H, true);
        clockSwitch.setOnClickListener(v -> setClock24h(!isClock24h()));
        updateClockSwitch(is24h);
    }

    private boolean isClock24h() {
        return prefs.getBoolean(KEY_CLOCK_24H, true);
    }

    private void setClock24h(boolean is24h) {
        prefs.edit().putBoolean(KEY_CLOCK_24H, is24h).apply();
        updateClockSwitch(is24h);
    }

    private void updateClockSwitch(boolean isOn) {
        clockTrack.setBackgroundResource(isOn
                ? R.drawable.bg_settings_switch_track_on
                : R.drawable.bg_settings_switch_track_off);

        // 移动 thumb 位置：开启时靠右，关闭时靠左
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockThumb.getLayoutParams();
        if (isOn) {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
            params.setMarginEnd(dpToPx(2));
            params.setMarginStart(0);
        } else {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
            params.setMarginStart(dpToPx(2));
            params.setMarginEnd(0);
        }
        clockThumb.setLayoutParams(params);
    }

    // ── 温度 ──

    private void initTempUnitButtons() {
        int unit = prefs.getInt(KEY_TEMP_UNIT, TEMP_C);
        btnTempC.setOnClickListener(v -> setTempUnit(TEMP_C));
        btnTempF.setOnClickListener(v -> setTempUnit(TEMP_F));
        highlightTempUnit(unit);
    }

    private void setTempUnit(int unit) {
        prefs.edit().putInt(KEY_TEMP_UNIT, unit).apply();
        highlightTempUnit(unit);
    }

    private void highlightTempUnit(int unit) {
        setSegmentActive(btnTempC, unit == TEMP_C);
        setSegmentActive(btnTempF, unit == TEMP_F);
    }

    // ── 胎压 ──

    private void initPressureButtons() {
        int unit = prefs.getInt(KEY_PRESSURE_UNIT, PRESSURE_PSI);
        btnPsi.setOnClickListener(v -> setPressureUnit(PRESSURE_PSI));
        btnKpa.setOnClickListener(v -> setPressureUnit(PRESSURE_KPA));
        btnBar.setOnClickListener(v -> setPressureUnit(PRESSURE_BAR));
        highlightPressure(unit);
    }

    private void setPressureUnit(int unit) {
        prefs.edit().putInt(KEY_PRESSURE_UNIT, unit).apply();
        highlightPressure(unit);
    }

    private void highlightPressure(int unit) {
        setSegmentActive(btnPsi, unit == PRESSURE_PSI);
        setSegmentActive(btnKpa, unit == PRESSURE_KPA);
        setSegmentActive(btnBar, unit == PRESSURE_BAR);
    }

    // ── 模拟模式 ──

    private void initSimStatus(boolean isSimulation) {
        if (isSimulation) {
            simStatus.setText(R.string.settings_sim_on);
            simStatus.setTextColor(ContextCompat.getColor(context, R.color.status_fair));
        } else {
            simStatus.setText(R.string.settings_sim_off);
            simStatus.setTextColor(ContextCompat.getColor(context, R.color.status_good));
        }
    }

    // ── 默认桌面 ──

    private void initDefaultLauncher() {
        rootView.findViewById(R.id.settings_set_default).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    // ── 工具方法 ──

    private void setSegmentActive(TextView btn, boolean active) {
        btn.setBackgroundResource(active
                ? R.drawable.bg_settings_segment_selected
                : R.drawable.bg_settings_segment_unselected);
        btn.setTextColor(ContextCompat.getColor(context,
                active ? R.color.text_on_accent : R.color.text_secondary));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // ── 静态工具方法 ──

    public static boolean isClock24h(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CLOCK_24H, true);
    }

    public static int getTempUnit(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_TEMP_UNIT, TEMP_C);
    }

    public static int getPressureUnit(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_PRESSURE_UNIT, PRESSURE_PSI);
    }
}
```

---

### Task 5: 修复布局中的 dimen 引用错误

**Files:**
- Modify: `app/src/main/res/layout/page_settings.xml`

- [ ] **Step 1: 修复时钟格式描述行的 marginStart 引用**

在 `page_settings.xml` 中找到时钟格式部分的描述 `LinearLayout`，将 `android:layout_marginStart="@dimen/settings_md"` 改为 `android:layout_marginStart="@dimen/spacing_md"`。

这是 Task 3 布局中的一个笔误：`settings_md` 不是有效的 dimen 名称，应该用 `spacing_md`。

---

### Task 6: 验证构建

- [ ] **Step 1: 构建 debug APK**

```bash
cd /Users/feng/work/byd && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 如果构建失败，检查错误信息并修复**

常见问题：
- `R.id.xxx not found` → 检查 `page_settings.xml` 中的 id 是否与 Java 代码一致
- `dimen not found` → 检查 `dimens.xml` 中是否遗漏了某个尺寸
- `drawable not found` → 检查 Task 1 中的 6 个 drawable 文件是否都已创建

---

## 自检清单

- [x] 每个 spec 需求都有对应 task
- [x] 所有 drawable 资源都有创建步骤
- [x] 布局 XML 和 Java 代码中的 ID 一致
- [x] dimen 引用使用现有值（spacing_md 等）或新增值（settings_* 等）
- [x] Toggle Switch 逻辑完整（点击、状态切换、thumb 位置动画）
- [x] 分段按钮选中/未选中态背景切换正确
