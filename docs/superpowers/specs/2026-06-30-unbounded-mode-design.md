# 迪UI 无界模式设计文档

**日期**: 2026-06-30
**目标**: 实现类似嘟嘟桌面PRO"无界模式"的沉浸式车机桌面体验
**目标设备**: BYD DiLink 3.0/4.0 车机，Android 9，12-13 英寸横屏

---

## 需求总结

| 项目 | 决定 |
|------|------|
| 底层地图 | 用户自选导航应用，Intent 启动 |
| 模式切换 | 设置页切换（标准/无界）+ 边缘滑动 |
| 插件卡片 | 聚合卡片（速度+胎压+电量+温度）+ 音乐卡片 + 时钟卡片 |
| 卡片布局 | 左侧栏竖排，右侧地图/壁纸区域 |
| 卡片背景 | 毛玻璃效果（静态模拟，零性能开销） |
| 手势 | 三指下滑隐藏卡片、三指上滑恢复、三指侧滑交换位置 |
| 延伸布局 | 底部上拖切换回标准模式、顶部下拖切回无界 |
| 应用配置 | 导航/音乐/视频/电话槽位，用户从已安装应用列表选择 |

---

## 方案对比（保留备选）

### 方案 A：Activity 内嵌模式（当前选定）

在现有 `activity_main.xml` 的 `content_frame` 中新增 `page_unbounded` 布局，与现有 4 个标签页同级。无界模式下隐藏 TopBar + NavBar + 所有标签页，全屏显示 `page_unbounded`。

```
标准模式: TopBar + [page_status | page_controls | page_apps | page_settings] + NavBar
无界模式: [page_unbounded (全屏)] — 自带精简 NavBar
```

- **优点**: 改动最小，复用现有 Activity 架构，卡片直接用 View，不需要 WindowManager
- **缺点**: 地图不是真正嵌入，通过 Intent 启动外部导航应用
- **性能**: 最轻量，不额外创建 Window
- **风险**: 低。与现有架构完全兼容

### 方案 B：独立 Activity 模式（备选）

无界模式用一个新的 `UnboundedActivity`，从 MainActivity 跳转。

- **优点**: 代码隔离干净，无界模式逻辑不影响主 Activity
- **缺点**: Activity 切换有延迟（200-500ms），VehicleManager 状态传递复杂，作为 Launcher 多 Activity 体验不好，按 Home 键可能回到错误的 Activity
- **性能**: 中等，多一个 Activity 实例
- **适用场景**: 如果方案 A 导致 MainActivity 代码过于臃肿，可考虑迁移到此方案

### 方案 C：WindowManager 悬浮窗模式（备选）

用 `SYSTEM_ALERT_WINDOW` 创建系统级悬浮窗，把卡片以 WindowManager 方式叠在高德地图上。

- **优点**: 真正的全局悬浮，可以叠在任何 APP 上（包括高德全屏导航时），最接近嘟嘟桌面的体验
- **缺点**: 实现复杂，触摸事件传递困难（悬浮窗会拦截地图触摸），车机权限问题多（部分 BYD 固件限制悬浮窗），与 Launcher 身份冲突（Launcher 应该在后台，悬浮窗又在前台）
- **性能**: 较高，WindowManager 持续渲染
- **适用场景**: 如果需要在高德导航过程中始终显示车况卡片，且车机固件允许悬浮窗

### 方案选择建议

**首选 A**。如果后续需要"导航时也显示车况"的需求，可以单独用方案 C 做一个轻量悬浮 Widget（只显示速度+电量），与方案 A 共存。

---

## 模块一：无界模式布局架构

### 整体结构

```
┌─────────────────────────────────────────────────────────────┐
│                    page_unbounded (全屏)                      │
│                                                              │
│  ┌──────────┐                                               │
│  │ 时钟卡片  │              地图/壁纸区域                      │
│  │ HH:MM    │              （透明背景，点击启动导航应用）       │
│  │ 日期星期  │                                               │
│  ├──────────┤                                               │
│  │ 聚合卡片  │                                               │
│  │ 速度 68  │                                               │
│  │ 胎压 250 │                                               │
│  │ 电量 89% │                                               │
│  │ 温度 26° │                                               │
│  ├──────────┤                                 ┌────────────┐│
│  │ 音乐卡片  │                                 │ 精简NavBar  ││
│  │ 🎵 歌名  │                                 │ ❄ 🏠 ⚙    ││
│  │ ◀ ▶ ⏸   │                                 └────────────┘│
│  └──────────┘                                               │
│  ┌──────────────────────────────────────────────────────────┐│
│  │          底部拖拽区域 (20dp, 透明)                         ││
│  └──────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 关键参数

- **左侧卡片栏**: 宽度 260dp，上下内边距 12dp，卡片间距 8dp
- **精简 NavBar**: 右下角固定，宽 160dp，高 44dp，包含空调快捷/回标准模式/设置 3 个按钮
- **底部拖拽区**: 全宽 20dp，透明，检测上滑手势切换到标准模式
- **地图/壁纸区**: 右侧剩余空间，显示壁纸或纯色背景，底部半透明提示"点击打开导航"

### 与现有架构的关系

```
activity_main.xml
  ├─ LinearLayout id="standard_container"（标准模式容器，需要给现有 LinearLayout 加此 id）
  │   ├─ view_top_bar
  │   ├─ content_frame
  │   │   ├─ page_status
  │   │   ├─ page_controls
  │   │   ├─ page_apps
  │   │   └─ page_settings
  │   └─ view_nav_bar
  ├─ page_unbounded（无界模式，新增，默认 GONE）
  ├─ overlay_mask
  ├─ panel_window_control
  └─ panel_seat_control
```

模式切换时：
- 标准 → 无界：隐藏 LinearLayout（标准容器），显示 page_unbounded
- 无界 → 标准：隐藏 page_unbounded，显示 LinearLayout

---

## 模块二：毛玻璃卡片效果

### 实现方案：静态模拟（零性能开销）

考虑车机性能有限，不用 RenderScript 实时高斯模糊，改用多层 Drawable 静态模拟磨砂玻璃效果：

```xml
<!-- bg_card_glass.xml -->
<layer-list>
    <!-- 底层：深色半透明 -->
    <item><shape><solid color="#CC1A1A2E" /><corners radius="12dp" /></shape></item>
    <!-- 顶层：亮边框 -->
    <item><shape><stroke width="1dp" color="#15FFFFFF" /><corners radius="12dp" /></shape></item>
</layer-list>
```

效果：深色磨砂玻璃质感，比纯半透明 `bg_card`（#1AFFFFFF）更有层次感。

### 壁纸模糊（可选增强）

如果用户设置了自定义壁纸，在模式切换时一次性计算高斯模糊：
- `GlassBlurHelper.blurBitmap(wallpaper, radius=25)` — 使用 `ScriptIntrinsicBlur`
- 缓存 `blurredBitmap`，作为 `page_unbounded` 的背景
- 仅在壁纸变更时重新计算，运行时零开销

### 文件

- 新增 `res/drawable/bg_card_glass.xml`
- 新增 `GlassBlurHelper.java`（可选，壁纸模糊工具）

---

## 模块三：聚合卡片

### 布局

```
┌─────────────────────┐
│  ⚡ 68 km/h    D    │  速度+挡位
├─────────────────────┤
│  🔋 89%    90km EV  │  电量+纯电续航
│  ⛽ 37%    21.0L    │  油量+油量值
├─────────────────────┤
│  FL 250  FR 252     │  胎压 2x2
│  RL 250  RR 250     │
├─────────────────────┤
│  车外 26°  车内 24° │  温度
│  功率 12.5kW        │  实时功率
└─────────────────────┘
```

### 实现

- 新增 `AggregateCardView.java` — 自定义 View 类
  - `updateStatus(VehicleStatus s)` — 接收车辆数据更新
  - 内部持有所有 TextView 引用，直接更新文本
- 新增 `view_card_aggregate.xml` — 聚合卡片布局
- 宽度 260dp，背景 `bg_card_glass`
- 数据源：与 StatusPage 共享 `BydVehicleManager.onStatusUpdated()` 回调

---

## 模块四：音乐卡片 + 时钟卡片

### 音乐卡片（精简版）

```
┌─────────────────────┐
│  🎵 歌曲名称        │
│  歌手名              │
│  ◀◀  ▶⏸  ▶▶       │
└─────────────────────┘
```

- 新增 `view_card_music_mini.xml` — 精简布局（去掉专辑封面）
- `MusicCardView.java` 增加 `initMini()` 方法适配精简布局
- 点击歌名区域：通过 `AppSlotManager` 启动用户选择的音乐应用

### 时钟卡片

```
┌─────────────────────┐
│       14:32          │
│   06月30日 星期一     │
└─────────────────────┘
```

- 直接在 `page_unbounded.xml` 中内联 `TextClock`
- 背景 `bg_card_glass`
- 读取 `SettingsPage` 的时钟格式偏好（24h/12h）
- 无需新增 Java 类

---

## 模块五：三指手势系统

### 手势定义

| 手势 | 触发条件 | 效果 |
|------|----------|------|
| 三指下滑 | 3 触摸点同时下移 >100dp | 隐藏所有卡片，只留时钟（移到右上角）→ 纯壁纸模式 |
| 三指上滑 | 3 触摸点同时上移 >100dp | 恢复显示所有卡片 |
| 三指左/右滑 | 3 触摸点同时横移 >100dp | 交换左侧卡片顺序（聚合↔音乐↔时钟轮换） |

### 实现

新增 `ThreeFingerGestureDetector.java`:

```java
public class ThreeFingerGestureDetector {
    public interface Callback {
        void onSwipeDown();   // 隐藏卡片
        void onSwipeUp();     // 恢复卡片
        void onSwipeLeft();   // 交换卡片顺序
        void onSwipeRight();  // 交换卡片顺序
    }
    
    // 在 onTouchEvent 中检测:
    // ACTION_POINTER_DOWN → 计数触摸点
    // 3 点时记录起始位置
    // ACTION_MOVE → 计算 3 点平均位移
    // 位移 >100dp 且方向一致 → 触发回调
}
```

### 卡片动画

- 隐藏：向左平移 260dp + alpha 1→0（300ms, DecelerateInterpolator）
- 恢复：从左侧滑入 + alpha 0→1（300ms, DecelerateInterpolator）
- 时钟在纯壁纸模式下 LayoutParams 移到右上角

---

## 模块六：延伸布局切换

### 交互方式

```
无界模式 → 底部上拖 >80dp → 切换到标准模式
标准模式 → 设置页选择"无界模式" 或 从顶部下拖 >80dp → 切换到无界模式
```

不是拖拽跟手，是边缘滑动触发（类似 Android 手势导航）。

### 切换动画

- 无界 → 标准：page_unbounded 向上滑出 + 渐隐，LinearLayout（标准容器）从下方滑入 + 渐显（300ms）
- 标准 → 无界：反向

### 状态持久化

- `SharedPreferences` key: `layout_mode`，值: `"standard"` / `"unbounded"`
- App 启动时读取，直接进入对应模式
- 设置页同步显示当前模式

### 实现

- `page_unbounded.xml` 底部 20dp 透明触摸区域
- `MainActivity.java` 新增 `switchToUnbounded()` / `switchToStandard()` — 控制 View 显隐 + 动画
- `UnboundedPage.java` 底部边缘滑动检测
- `SettingsPage.java` 新增"布局模式"设置项

---

## 模块七：导航/音乐应用自定义

### 应用槽位

| 槽位 | SharedPreferences key | 用途 |
|------|----------------------|------|
| 导航应用 | `app_nav` | 无界模式地图区域 + 标准模式地图快捷入口 |
| 音乐应用 | `app_music` | 音乐卡片点击 + 标准模式音乐快捷入口 |
| 视频应用 | `app_video` | 标准模式视频快捷入口 |
| 电话应用 | `app_phone` | 标准模式电话快捷入口 |

### 选择交互

设置页点击槽位 → 弹出 AppPickerDialog → 列出所有已安装应用（图标+名称） → 顶部搜索框过滤 → 用户选择 → 保存包名。

**不依赖预设包名**。扫描 `PackageManager.getInstalledApplications()` 列出所有非系统应用。首次安装时尝试匹配几个常见包名作为推荐（在列表顶部标"推荐"），但匹配不到也不影响。

### 状态处理

| 场景 | 行为 |
|------|------|
| 首次使用，未配置 | 地图区域显示"点击设置导航应用"，引导去设置页 |
| 已配置但应用被卸载 | 检测到包名无效，提示"应用已卸载，请重新选择" |
| 已配置且正常 | 点击直接启动该应用 |

### 实现

- 新增 `AppSlotManager.java` — 管理 4 个槽位（读取/保存/启动/检测有效性）
- 新增 `AppPickerDialog.java` — 应用选择对话框
- `SettingsPage.java` + `page_settings.xml` — 新增"应用配置"区域
- `UnboundedPage.java` — 地图区域点击通过 AppSlotManager 启动
- `MusicCardView.java` — 点击通过 AppSlotManager 启动
- `StatusPage.java` — pip_map/pip_music 等快捷入口也改为使用 AppSlotManager

---

## 新增文件汇总

| 文件路径 | 职责 |
|----------|------|
| `ui/UnboundedPage.java` | 无界模式页面控制器（卡片管理/手势/地图区域/边缘滑动） |
| `ui/AggregateCardView.java` | 聚合卡片（速度+胎压+电量+温度） |
| `ui/ThreeFingerGestureDetector.java` | 三指手势检测器 |
| `ui/AppSlotManager.java` | 应用槽位管理（导航/音乐/视频/电话） |
| `ui/AppPickerDialog.java` | 应用选择对话框 |
| `ui/GlassBlurHelper.java` | 壁纸高斯模糊工具（可选） |
| `res/layout/page_unbounded.xml` | 无界模式全屏布局 |
| `res/layout/view_card_aggregate.xml` | 聚合卡片布局 |
| `res/layout/view_card_music_mini.xml` | 精简音乐卡片布局 |
| `res/layout/view_mini_navbar.xml` | 精简导航栏布局 |
| `res/drawable/bg_card_glass.xml` | 毛玻璃卡片背景 |

## 修改文件汇总

| 文件路径 | 改动概要 |
|----------|----------|
| `res/layout/activity_main.xml` | 根 FrameLayout 中新增 page_unbounded（默认 GONE） |
| `res/layout/page_settings.xml` | 新增"布局模式"和"应用配置"设置项 |
| `MainActivity.java` | switchToUnbounded()/switchToStandard() 模式切换 + 动画，onCreate 读取模式 |
| `ui/SettingsPage.java` | 布局模式切换 + 应用配置 UI |
| `ui/MusicCardView.java` | 增加 initMini() 精简模式，点击通过 AppSlotManager 启动 |
| `ui/StatusPage.java` | pip 快捷入口改为使用 AppSlotManager |
