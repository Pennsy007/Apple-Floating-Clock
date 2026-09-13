<div align="center">

# 🍎 苹果灵动岛毛玻璃悬浮时钟 (Apple Floating Clock)

**基于 Apple 最新 UI 设计语言（iOS 18 / VisionOS）打造的殿堂级美学悬浮时钟**  
*高精原子钟毫秒级授时 · 极致温润毛玻璃 · 0 生硬描边 · 物理阻尼弹性交互 · 超低功耗设计*

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java%2017-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-007AFF)](https://github.com/Pennsy007/Apple-Floating-Clock/releases/tag/v1.0.0)

<p align="center">
  <img src="https://fastly.jsdelivr.net/gh/Pennsy007/Apple-Floating-Clock@main/docs/images/preview_desktop.png" alt="桌面悬浮效果" width="320" style="border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.2);" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="https://fastly.jsdelivr.net/gh/Pennsy007/Apple-Floating-Clock@main/docs/images/preview_settings.png" alt="控制中心设置" width="320" style="border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.2);" />
</p>

</div>

---

## 📖 项目简介 (Overview)

市面上的悬浮时钟普遍存在**外观粗糙生硬、黑灰边框发脏、字体抖动、占用大内存以及疯狂耗电**等通病。

**Apple Floating Clock** 是一款专为追求极致审美的用户、数码发烧友与抢购抢票玩家量身定制的 Android 悬浮时钟小挂件。我们深度复刻了苹果最新 UI（iOS 18 灵动岛与 VisionOS 空间玻璃）的材质精髓，让悬浮窗不仅仅是一个实用的抢购计时器，更是悬浮在手机壁纸与应用上方的一枚**晶莹温润的小艺术品挂件**。

---

## ✨ 核心特色 (Features)

### 🎨 1. 苹果最新 UI 极致美学
- **两端正半圆跑道胶囊（Apple Squircle Pill）**：基于物理连续曲率渲染，杜绝任何变形或生硬切角。
- **纯正毛玻璃材质（Ultra-Thin Frosted Glass）**：
  - **0 生硬描边（Zero Harsh Borders）**：彻底剔除传统粗暴的黑线与生硬 Stroke；
  - **顶面漫反射天光泛光（Top Ambient Sheen）**：仅在胶囊上沿保留自然的微折射柔光，下半部无缝融入背景；
  - **72% 黄金通透比**：背景壁纸通透呈现，不突兀、不抢戏。
- **5 款自然仿生主题材质**：
  - 🌑 **暗夜星曜 (Midnight Starlight)** - 沉稳深色半透，经典灵动岛风范（默认推荐）
  - ❄️ **晨露冰晶 (Glacier Morning Dew)** - 冰晶晶透高亮，适合浅色或高亮桌面
  - 🌲 **冷杉林雾 (Forest Pine Mist)** - 苍翠自然薄雾，与绿植山川壁纸绝配
  - 🌅 **落日暖霞 (Sunset Amber Glow)** - 晚霞琥珀暖金，温馨护眼
  - 🌌 **暮夜星辰 (Twilight Aurora)** - 暮光梦幻极光，科技感十足

### ⏱️ 2. 北京时间高精原子授时 (毫秒级零延迟)
- **NTP 授时服务器自动对齐**：内置国家授时中心/阿里云高精授时池，自动计算网络往返往复延迟，动态补偿系统偏差。
- **等宽纯净数字排版（`tnum`）**：全字号开启 Tabular Numbers，秒数与毫秒高速流转时整体版面纹丝不动、绝不左右抖动。
- **毫秒按需开启**：日常是纯粹的时分秒桌面挂件，抢购/秒杀时一键开启 `.SSS` 毫秒高刷流水流转。

### 🤏 3. iOS 级物理阻尼与流体交互
- **双指捏合无级缩放**：任意双指在胶囊上捏合/张开，实时平滑缩放尺寸（0.6x ~ 1.8x）。
- **双击快速切换毫秒**：双击胶囊自带 iOS 物理弹性回弹微动效，一秒切换时钟精细度。
- **边缘阻尼弹簧磁吸**：松手后平滑惯性吸附至屏幕边缘，避免遮挡主要内容。
- **顶部迷你贴边模式**：一键开启极简灵动岛顶栏挂件模式。

### 🍃 4. 极致超低功耗架构
- **分级能耗调度**：未开启毫秒时，底层采用高精秒对齐休眠调度，每秒仅被系统唤醒一次更新，CPU 占用趋近于 0%；
- **按需垂直同步（120Hz VSync）**：仅在开启毫秒模式时激活 Choreographer 硬件垂直同步，拒绝空转浪费电量；
- **息屏绝对静默**：监听广播在手机灭屏时完全挂起绘制，亮屏秒级自恢复。

### 🛡️ 5. 安全合规与纯粹隐私
- **零敏感高危权限**：不索取通讯录、定位、相机、麦克风、存储等任何隐私权限；
- **全套正式 Release 签名**：完整配置 V1 + V2 + V3 签名体系，主流安卓手机管家检测绿标通过，放心安装。

---

## 📲 下载与安装 (Download)

1. 前往本项目的 [Releases](https://github.com) 页面下载最新版 `app-release.apk`。
2. 安装后首次打开，按提示授予**悬浮窗显示权限**。
3. 点击 **“开启悬浮时钟”**，即可开始体验。

---

## 🛠️ 本地编译与构建 (Build from Source)

本项目采用现代标准 Android 技术栈开发：

- **开发语言**：Java 17
- **构建工具**：Gradle 8.7 (Kotlin DSL)
- **Compile SDK**：Android 14 (API 34)
- **Min SDK**：Android 8.0 (API 26)

### 构建步骤：
```bash
# 1. 克隆代码仓库
git clone https://github.com/Pennsy007/Apple-Floating-Clock.git
cd Apple-Floating-Clock

# 2. 编译 Debug 版本
./gradlew assembleDebug

# 3. 编译 Release 版本
./gradlew assembleRelease
```
编译产物位于：`app/build/outputs/apk/release/app-release.apk`。

---

## ☕ 请作者喝杯咖啡 (Sponsor)

**Apple Floating Clock** 是一个完全免费、纯粹无广告、无内购、无任何数据收集的开源个人项目。

如果这个悬浮时钟在你的**抢购秒杀、考研备考、时间管理**中助你一臂之力，或者你喜欢这份精雕细琢的**苹果设计美学**，欢迎请作者喝杯咖啡 ☕ 犒劳一下！

你的每一份支持与认可，都是作者持续打磨细节、跟进系统适配与维护开源项目的最大动力！❤️

<div align="center">

<img src="https://fastly.jsdelivr.net/gh/Pennsy007/Apple-Floating-Clock@main/docs/images/sponsor.jpg" alt="微信赞赏码" width="280" style="border-radius: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.15);" />

<br/>

**七月初八 (喜)** · 微信扫码赞赏支持

*（感谢所有给予支持与建议的朋友们，祝大家生活愉快，心想事成！）*

</div>

---

## 🤝 贡献与反馈 (Contributing)

欢迎提交 Issue 和 Pull Request！  
- 如果发现任何机型适配问题或 Bug，请提交 [Issue](https://github.com/Pennsy007/Apple-Floating-Clock/issues)；
- 如果你有更好的视觉设计点子或功能建议，非常欢迎提交 PR 共同完善！

---

## 📄 开源许可证 (License)

本项目基于 [MIT License](LICENSE) 协议开源。可自由修改、分发与使用，请保留原作者版权声明。
