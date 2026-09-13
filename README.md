<div align="center">

# ⏱️ 灵动毛玻璃悬浮时钟 (Floating Clock)

**极简通透 · 高精毫秒级原子授时 · 自然质感跑道胶囊 · 低功耗原生 Android 实现**  
*毫秒超低抖动刷新 · 0 生硬黑边描边 · 物理阻尼弹性交互 · 息屏完全静默*

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

市面上的悬浮时钟普遍存在**外观粗糙生硬、黑灰边框发脏遮挡屏幕、字体频繁抖动、占用大内存以及疯狂耗电**等痛点。

**Floating Clock** 是一款专为**抢购秒杀、考研备考、时间管理与日常桌面装扮**打造的轻量级 Android 悬浮时钟小挂件。我们采用严格的两头正半圆跑道胶囊（Squircle Pill Shape）与通透的微晶毛玻璃材质，剔除粗暴的黑框描边，让悬浮窗不仅是一个精准可靠的毫秒计时器，更是一枚**晶莹温润、不挡屏幕内容的小挂件**。

---

## ✨ 核心特色 (Features)

### 🎨 1. 晶莹通透毛玻璃质感
- **两端正半圆跑道胶囊（Pill Shape）**：严格保持两头饱满的正半圆曲率，避免变形或生硬切角。
- **纯净毛玻璃材质（Frosted Glass）**：
  - **0 生硬描边**：彻底剔除传统粗暴的黑边与生硬线条；
  - **顶面漫反射天光泛光（Top Ambient Sheen）**：仅在胶囊顶部上沿保留自然的微折射柔光，下半部通透融入壁纸与界面；
  - **黄金通透比**：背景界面与壁纸通透可见，不突兀、不抢戏。
- **5 款自然质感主题配色**：
  - 🌑 **暗夜星曜** - 沉稳深色半透质感（经典默认）
  - ❄️ **晨露冰晶** - 冰晶透亮高光，适合浅色或高亮桌面
  - 🌲 **冷杉林雾** - 苍翠自然薄雾，与绿植山川壁纸绝配
  - 🌅 **落日暖霞** - 晚霞琥珀暖金，温馨护眼
  - 🌌 **暮夜星辰** - 暮光幻彩微蓝，质感细腻

### ⏱️ 2. 高精毫秒级原子授时 (低延迟防抖)
- **NTP 授时服务器自动对齐**：支持国家授时中心/阿里云高精授时池，自动计算网络往返往复延迟，动态补偿系统偏差。
- **等宽数字排版（`tnum`）**：全字号开启 Tabular Numbers，秒数与毫秒高速流转时整体版面纹丝不动、绝不左右抖动。
- **毫秒按需开启**：日常是清爽的时分秒挂件，抢购秒杀时一键开启 `.SSS` 毫秒高刷流转。

### 🤏 3. 物理阻尼手势交互
- **双指捏合无级缩放**：任意双指在胶囊上捏合/张开，实时平滑缩放尺寸（0.6x ~ 1.8x）。
- **双击快速切换毫秒**：双击胶囊自带物理弹性回弹微动效，一秒切换时钟精细度。
- **边缘阻尼弹簧磁吸**：拖拽松手后平滑惯性吸附至屏幕边缘，避免遮挡主要操作区。
- **顶部迷你贴边模式**：一键开启极简顶栏挂件模式。

### 🍃 4. 极致超低功耗原生架构
- **分级能耗调度**：未开启毫秒时，底层采用高精秒对齐休眠调度，每秒仅被系统唤醒一次更新，CPU 占用趋近于 0%；
- **按需垂直同步（120Hz VSync）**：仅在开启毫秒模式时激活 Choreographer 硬件垂直同步，拒绝无效空转；
- **息屏绝对静默**：监听广播在手机灭屏时完全挂起绘制，亮屏秒级自恢复。

### 🛡️ 5. 安全合规与纯粹隐私
- **零敏感高危权限**：不索取通讯录、定位、相机、麦克风、存储等任何隐私权限；
- **全套正式 Release 签名**：完整配置 V1 + V2 + V3 签名体系，主流安卓手机管家检测绿标通过，放心安装。

---

## 📲 下载与安装 (Download)

1. 前往本项目的 [Releases](https://github.com/Pennsy007/Apple-Floating-Clock/releases) 页面下载最新版 `app-release.apk`。
2. 安装后首次打开，按系统提示授予**悬浮窗显示权限**。
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

**Floating Clock** 是一个完全免费、纯粹无广告、无内购、无任何数据收集的开源个人项目。

如果这个悬浮时钟在你的**抢购秒杀、考研备考、时间管理**中助你一臂之力，或者你喜欢这份清爽实用的设计，欢迎请作者喝杯咖啡 ☕ 犒劳一下！

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
