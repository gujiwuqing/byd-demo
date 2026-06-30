# 迪UI 精致空调面板设计文档

**日期**: 2026-06-30
**目标**: 为无界模式设计并实现精致的空调控制面板，参考嘟嘟桌面/Kinex 仪表盘风格
**触发方式**: 无界模式右下角 ❄ 按钮点击

---

## 整体布局

弹窗居中显示，宽度 480dp，高度 wrap_content，背景 `bg_card_glass`（深色毛玻璃）。

```
┌─────────────────────────────────┐
│  ❄ 空调控制              [ × ]  │  ← 标题行
├─────────────────────────────────┤
│      ╭──────────────╮           │
│    ╱   ● ● ● ● ● ●   ╲         │  ← 圆弧刻度（180° 半圆，17~33°C）
│   │        22°C        │        │  ← 中心温度大字
│   │      [❄ ON]        │        │  ← 开关按钮
│    ╲                 ╱          │
│      [  −  ]  [  +  ]           │  ← 温度调节按钮
├─────────────────────────────────┤
│  🌬 ① ② ③ ④ ⑤ ⑥ ⑦ 💨        │  ← 风量选择（7级点击）
├─────────────────────────────────┤
│  风向  [吹脸↑] [吹脚↓] [除霜≋]  │
│  循环  [内↺]   [外↻]            │
│  模式  [自动A]  [手动M]          │
└─────────────────────────────────┘
```

---

## 模块一：ArcTempView（圆弧温度表盘）

自定义 View，Canvas 绘制。

### 绘制层次

1. **背景圆弧**：180° 半圆（9点 → 3点），灰色 `#33FFFFFF`，线宽 8dp
2. **进度圆弧**：从起点到当前温度对应角度，颜色 `#00C8F0`，线宽 10dp
3. **刻度点**：17 个点（对应 17~33°C），均匀分布在圆弧上，半径 3dp，颜色 `#55FFFFFF`
4. **当前温度点**：较大亮点，半径 6dp，颜色白色，外发光（shadowLayer radius=8dp, color=#4400C8F0）
5. **中心温度大字**：32sp bold，颜色 `#F0F4FF`
6. **中心 °C 副标题**：12sp，颜色 `#00C8F0`

### 参数

| 参数 | 值 |
|------|-----|
| 圆弧半径 | 120dp |
| 扫描角度 | 180°（startAngle=180°） |
| 背景弧线宽 | 8dp |
| 进度弧线宽 | 10dp |
| 普通刻度点半径 | 3dp |
| 当前温度点半径 | 6dp |

### 温度范围映射

```
17°C → startAngle=180°（左端）
33°C → startAngle+180°=360°（右端）
当前温度 T → 进度角度 = (T - 17) / 16 * 180°
```

### API

```java
class ArcTempView extends View {
    void setTemp(int temp);       // 17~33，更新进度和中心文字，触发 invalidate()
    void setAcOn(boolean on);     // false 时圆弧变灰，中心文字变暗
}
```

---

## 模块二：风量选择器

7 个等权重的圆点，横向 LinearLayout，两侧标签。

### 样式

| 状态 | 尺寸 | 颜色 |
|------|------|------|
| 未选中 | 8dp 圆点 | `#33FFFFFF` |
| 选中 | 12dp 圆点 | `#00C8F0`，外发光 |

```
[🌬 弱]  ○ ○ ● ○ ○ ○ ○  [强 💨]
          1 2 3 4 5 6 7
```

点击任一圆点 → 选中高亮 → 调用 `acApi.setWindLevel(level)`。

---

## 模块三：风向 / 循环 / 模式控制行

三行，每行带文字标签按钮。

```
风向   [↑ 吹脸]  [↓ 吹脚]  [≋ 除霜]
循环   [↺ 内循环]  [↻ 外循环]
模式   [A 自动]   [M 手动]
```

### 按钮样式

- **未选中**：背景 `bg_card_glass`，文字 `text_secondary`，圆角 8dp，高度 36dp
- **选中**：背景透明 + `accent` 色描边（1dp），文字 `accent` 色

---

## 新增 / 修改文件

### 新增文件

| 文件 | 职责 |
|------|------|
| `app/src/main/java/com/bydlauncher/ui/ArcTempView.java` | 圆弧温度自定义 View |
| `app/src/main/java/com/bydlauncher/ui/AcPanelDialog.java` | 空调面板弹窗控制器 |
| `app/src/main/res/layout/dialog_ac_panel.xml` | 面板布局 |
| `app/src/main/res/drawable/bg_btn_ac_selected.xml` | 选中状态按钮背景（accent 描边） |

### 修改文件

| 文件 | 改动 |
|------|------|
| `app/src/main/java/com/bydlauncher/ui/UnboundedPage.java` | `showAcPopup()` 替换为 `AcPanelDialog.show()` |

### AcPanelDialog 公共接口

```java
public class AcPanelDialog {
    public static void show(Context context, BydAcApi acApi, VehicleStatus currentStatus);
}
```

`currentStatus` 用于初始化各控件的当前状态（温度、风量、风向、循环、模式）。

---

## 数据流

```
UnboundedPage.showAcPopup()
  → AcPanelDialog.show(context, acApi, currentStatus)
    → 初始化 ArcTempView.setTemp(currentStatus.acTemp)
    → 初始化 ArcTempView.setAcOn(currentStatus.acOn)
    → 初始化风量、风向、循环、模式高亮
    → 用户交互 → 直接调用 acApi 对应方法
    → acApi 返回后更新 View 状态
```

---

## 视觉规格

| 元素 | 规格 |
|------|------|
| 弹窗宽度 | 480dp |
| 弹窗背景 | `bg_card_glass`（`#CC1A1A2E` + 1dp 白色描边） |
| 圆弧颜色 | `#00C8F0`（accent） |
| 温度大字 | 32sp，bold，`#F0F4FF` |
| 控制按钮高度 | 36dp |
| 选中按钮描边 | 1dp，`#00C8F0` |
| 各区间距 | 12dp |
