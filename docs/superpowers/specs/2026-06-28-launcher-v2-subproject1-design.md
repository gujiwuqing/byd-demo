# BYD Launcher V2 全面重构 - 子项目 1：核心框架 + 主屏仪表盘

**日期**: 2026-06-28
**状态**: 已确认
**参考**: Kinex (https://kinex.lexwah.com/)

## 1. 项目背景

BYDLauncher 是一个 Android Launcher 应用，安装到 BYD 12-13 英寸横屏车机上替换原生 Launcher。参考 Kinex 的设计理念进行全面重构。

### 全局项目拆分

| 子项目 | 内容 | 状态 |
|--------|------|------|
| **1（本文档）** | 核心框架 + Status 主屏仪表盘 | 当前 |
| 2 | Controls + Apps + Settings 标签页 | 待定 |
| 3 | Map 标签页 + 高级功能（多布局、盲区摄像头） | 待定 |

### 设计原则（来自 Kinex）

- **Minimal & focused**：大触摸目标，克制的视觉层级，关键信息一目了然
- **Pretty, not flashy**：精致排版和间距，不把屏幕变成灯光秀
- **模块化**：每个功能独立卡片/组件，职责单一

## 2. 整体架构

屏幕分为三层固定结构：

```
┌─────────────────────────────────────────────────────────────────┐
│  顶部状态栏 (32dp)                                              │
│  🔋📶🔵📍            BYD Launcher          ♫ Spotify    14:30  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                     内容区（标签页切换）                          │
│                   FrameLayout + View 显隐                       │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│  底部导航栏 (56dp)                                              │
│  [快捷APP] | [Status][Map][Controls][Apps][Settings] | [空调控制]│
└─────────────────────────────────────────────────────────────────┘
```

- 顶部栏和底部栏始终可见
- 内容区通过底部标签切换，使用 View 显隐（不用 Fragment/ViewPager）
- 子项目 1 只实现 Status 页，其余 4 个标签页显示占位内容

## 3. 顶部状态栏

高度 32dp 的信息条。

- **左侧**：模拟系统图标（🔋📶🔵📍），装饰性，不读取真实系统状态
- **中间**：应用名称 "BYD Launcher"，小字体
- **右侧**：当前媒体源名称 + 时间（HH:mm，实时更新）
- 浅色主题：深色文字 + 半透明白色背景；深色主题：浅色文字 + 半透明深色背景

## 4. Status 标签页（主屏仪表盘）

三列布局：

```
┌──────────┬──────────────────────────┬────────────────────┐
│ 左侧组件  │      中央仪表区           │   右侧卡片          │
│ (20%)    │      (45%)               │   (35%)            │
│          │                          │                    │
│ ┌──────┐ │         ┌───┐            │ ┌────────────────┐ │
│ │空气质量│ │    ┌────│   │────┐      │ │ 音乐播放器卡片  │ │
│ │室外 良 │ │    │    │   │    │      │ │ 封面+歌名+控制  │ │
│ │室内 优 │ │    │  ┌─┴───┴─┐  │      │ └────────────────┘ │
│ └──────┘ │    │  │       │  │      │                    │
│          │    │  │  BYD  │  │      │ ┌────────────────┐ │
│ ┌──────┐ │    │  │       │  │      │ │ 常用APP快捷     │ │
│ │ 日期  │ │    │  └───────┘  │      │ │ [📱][🗺][🎵][+]│ │
│ │ 时间  │ │    └─────────────┘      │ └────────────────┘ │
│ └──────┘ │  37psi           36psi  │                    │
│          │   P R N D    0 km/h     │                    │
│          │   ⚡69% ━━━━━━━░░       │                    │
│          │  42psi           43psi  │                    │
└──────────┴──────────────────────────┴────────────────────┘
```

### 4.1 左侧组件区（20%）

两个独立小卡片，垂直排列：

**空气质量卡片：**
- 标题 "空气质量"，带图标
- 室外空气状态（优/良/中/差）+ 颜色指示（绿/黄/橙/红）
- 室内空气状态 + 颜色指示
- 模拟模式：室外=良（黄），室内=优（绿）

**日期时间卡片：**
- 日期：MM月dd日 EEEE（如 "06月28日 星期六"）
- 时间：HH:mm 大字体
- 使用 TextClock 控件实时更新

### 4.2 中央仪表区（45%）

**车辆 SVG 俯视图（VehicleDiagramView 自定义 View）：**
- 居中显示简化的 BYD 车辆俯视轮廓
- 用 Canvas 在 onDraw 中绘制：车身轮廓（圆角矩形）、4 个车轮、车窗区域
- 车门打开时对应区域边缘变红色高亮
- 后备箱/引擎盖打开时对应端变红色高亮

**四角胎压：**
- 4 个 TextView 分布在车辆图四角
- 显示 "37 PSI" 格式
- 模拟模式：左前 37、右前 36、左后 42、右后 43

**下方遥测数据（水平排列）：**
- 挡位指示：P R N D 四个字母，当前挡位用强调色高亮，其余灰色
- 速度：大字体居中 "0 km/h"
- 电量：百分比 + 水平进度条，高(>60%)=绿色，中(30-60%)=橙色，低(<30%)=红色

### 4.3 右侧卡片区（35%）

两个卡片垂直堆叠：

**音乐播放器卡片（MusicCardView）：**
- 通过 MediaSessionManager API 监听系统正在播放的媒体
- 显示：媒体应用图标 + 名称（如 "Spotify"）、专辑封面缩略图、歌曲名、歌手名
- 控制按钮：上一首 ⏮、暂停/播放 ⏸、下一首 ⏭
- 没有媒体播放时显示 "未在播放" 灰色文字
- 需要 NotificationListenerService 权限读取媒体信息

**常用应用快捷方式卡片：**
- 一排水平排列的应用图标（3-4 个 + 1 个"+"按钮）
- 点击图标启动对应应用
- 点击"+"跳转到应用列表选择要 pin 的应用
- pin 的应用列表保存在 SharedPreferences 中
- 模拟/首次安装默认预置：设置、音乐、地图

## 5. 底部导航栏

高度 56dp，三段式水平布局：

### 5.1 左段：自定义应用快捷方式（15%）

- 2-3 个应用图标，点击直接启动
- 与 Status 页右侧的快捷方式独立（底部栏的是全局快捷，Status 页的是页面内快捷）
- 图标较小（20dp），紧凑排列

### 5.2 中段：标签页切换按钮（55%）

5 个按钮，每个包含图标 + 文字标签：

| 标签 | 文字 | 说明 |
|------|------|------|
| ⊙ | Status | 主屏仪表盘 |
| 🗺 | Map | 地图导航（占位） |
| 🎛 | Controls | 车辆控制（占位） |
| ⊞ | Apps | 应用列表（占位） |
| ⚙ | Settings | 设置（占位） |

- 选中状态：强调色文字 + 背景微亮圆角矩形
- 未选中：灰色文字 + 透明背景
- 切换时内容区 View 显隐切换，无动画（保持即时响应）

### 5.3 右段：空调快捷控制（30%）

- 温度显示：当前温度 "22°"
- 减号/加号按钮：点击 ±1°C
- 风量指示：风扇图标 + 当前等级数字
- 空调关闭时显示 "OFF" 灰色文字
- 所有操作通过 BydAcApi 执行

## 6. 浅色/深色双主题

### 6.1 浅色主题（默认）

| 用途 | 色值 | 说明 |
|------|------|------|
| 背景主色 | #F2F4F7 | 浅灰白 |
| 卡片背景 | #FFFFFF | 纯白 + 2dp elevation 投影 |
| 强调色 | #0A84FF | 蓝色 |
| 文字主色 | #1A1A1A | 近黑 |
| 文字次级 | #6B7280 | 中灰 |
| 边框 | #E5E7EB | 浅灰 |
| 导航栏背景 | #FFFFFF | 白色 |
| 状态栏背景 | #E8EBF0 | 浅灰 |

### 6.2 深色主题

| 用途 | 色值 | 说明 |
|------|------|------|
| 背景主色 | #0D1117 | 深黑 |
| 卡片背景 | #161B22 | 深灰 + 1dp 边框 |
| 强调色 | #00D4FF | 青色 |
| 文字主色 | #F0F6FC | 近白 |
| 文字次级 | #8B949E | 灰色 |
| 边框 | #30363D | 深灰 |
| 导航栏背景 | #161B22 | 深灰 |
| 状态栏背景 | #0D1117 | 深黑 |

### 6.3 技术实现

- 使用 Android 原生 `values/colors.xml` + `values-night/colors.xml` 双套颜色资源
- 通过 `AppCompatDelegate.setDefaultNightMode()` 切换
- `ThemeManager` 单例管理主题状态，持久化到 SharedPreferences
- 默认跟随系统设置
- 卡片背景引用 `?attr/colorSurface` 等主题属性

## 7. 代码架构

### 7.1 Java 类结构

```
com.bydlauncher/
├── MainActivity.java              # 主 Activity：初始化、标签页切换、生命周期
├── theme/
│   └── ThemeManager.java          # 主题管理（切换、持久化、颜色获取）
├── ui/
│   ├── StatusPage.java            # Status 标签页逻辑
│   ├── PlaceholderPage.java       # 占位标签页
│   ├── NavBar.java                # 底部导航栏逻辑
│   ├── TopBar.java                # 顶部状态栏逻辑
│   ├── MusicCardView.java         # 音乐播放器卡片
│   └── VehicleDiagramView.java    # 车辆俯视图自定义 View
├── adapter/
│   └── AppListAdapter.java        # 保留
├── api/                           # 全部保留不变
└── model/
    └── VehicleStatus.java          # 保留不变
```

### 7.2 布局文件

| 文件 | 说明 |
|------|------|
| `activity_main.xml` | 根布局：顶部栏 + FrameLayout 内容区 + 底部栏 |
| `view_top_bar.xml` | 顶部状态栏 |
| `view_nav_bar.xml` | 底部导航栏（三段式） |
| `page_status.xml` | Status 标签页（三列仪表盘） |
| `page_placeholder.xml` | 占位页（居中文字 "即将推出"） |
| `card_music.xml` | 音乐播放器卡片 |
| `card_air_quality.xml` | 空气质量卡片 |
| `card_datetime.xml` | 日期时间卡片 |
| `card_app_shortcuts.xml` | 常用应用快捷方式 |
| `item_app.xml` | 应用图标项 |

### 7.3 资源文件

| 文件 | 说明 |
|------|------|
| `values/colors.xml` | 浅色主题颜色 |
| `values-night/colors.xml` | 深色主题颜色 |
| `values/dimens.xml` | 尺寸 |
| `values/strings.xml` | 字符串 |
| `values/styles.xml` | 样式和主题 |
| `drawable/bg_card.xml` | 卡片背景（引用主题属性） |
| `drawable/bg_nav_bar.xml` | 导航栏背景 |
| `drawable/bg_nav_tab.xml` | 标签按钮 selector |
| `drawable/bg_ac_quick.xml` | 空调快捷按钮 |
| `drawable/bg_circle_btn.xml` | 圆形按钮（保留） |
| `drawable/bg_switch_btn.xml` | 切换按钮（保留） |
| `drawable/bg_ac_btn.xml` | 空调开关按钮（保留） |
| `drawable/progress_bar_battery.xml` | 电量进度条（保留） |

### 7.4 需要的权限

在 AndroidManifest.xml 中新增：
```xml
<uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
```

以及 NotificationListenerService 声明（用于读取媒体播放状态）。

### 7.5 需要删除的旧文件

- `view_home.xml` → 替换为 `page_status.xml`
- `view_dock.xml` → 替换为 `view_nav_bar.xml`
- `view_ac_panel.xml` → 空调控制移至 Controls 页（子项目 2），底部栏保留快捷控制
- `view_app_drawer.xml` → 移至 Apps 页（子项目 2）
- `bg_dock.xml`、`bg_dock_btn.xml`、`bg_panel.xml`、`bg_drag_handle.xml` → 不再需要

### 7.6 保持不变的文件

- 所有 `api/` 目录下的 Java 文件
- `model/VehicleStatus.java`
- `adapter/AppListAdapter.java`
- `AndroidManifest.xml`（仅追加权限声明）
