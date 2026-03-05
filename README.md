# 中国象棋 Android App

一款经典中国象棋 Android 应用，支持人机对战和双人对战，界面对老人友好，适配大屏平板（14寸）。

## 功能特性

| 功能 | 说明 |
|------|------|
| **AI 对战** | Alpha-Beta 剪枝 + 迭代加深搜索，带时间限制，不会卡死 |
| **三级难度** | 初级（休闲）、中级（默认）、高级（挑战） |
| **双人对战** | 支持两人在同一设备上对弈 |
| **悔棋** | 每局最多5次悔棋，AI模式自动回退两步 |
| **走法提示** | 一键获取AI推荐走法，橙色高亮显示 |
| **落子音效** | 走子、吃子、将军、胜利各有不同音效 |
| **AI语音播报** | 机器人走棋时播报幽默语音（使用TTS） |
| **全屏模式** | 隐藏系统栏，棋盘上下撑满屏幕 |
| **老人友好** | 大字体、大按钮、高对比度、简洁界面 |
| **合法走子** | 自动显示可走位置，防止误操作 |

## 象棋规则

完整实现中国象棋全部规则：
- 7种棋子走法（帅/将、仕/士、相/象、马、车、炮、兵/卒）
- 蹩马腿、塞象眼检测
- 将帅不能面对面（飞将检测）
- 将军、将杀、困毙判定
- 走后不能自将的合法性验证

## AI 算法

- **搜索算法**: Alpha-Beta 剪枝 + 迭代加深
- **时间控制**: 每个难度有独立的搜索深度和时间限制，保证不会卡死
  - 初级：搜索2层，限时1秒
  - 中级：搜索4层，限时3秒
  - 高级：搜索6层，限时5秒
- **走法排序**: MVV-LVA（最有价值受害者-最低价值攻击者）+ 将军优先
- **评估函数**: 棋子价值 + 位置价值表 + 机动性评估

## 技术架构

- **语言**: Kotlin
- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 15 (API 35)
- **UI框架**: 自定义 View + ViewBinding
- **AI计算**: Kotlin 协程在后台线程执行，不阻塞UI
- **音效**: SoundPool + TextToSpeech
- **构建**: Gradle 8.9 + AGP 8.7.0

## 项目结构

```
app/src/main/
├── java/com/chinesechess/game/
│   ├── engine/              # 棋局引擎
│   │   ├── ChessConstants.kt   # 常量定义
│   │   ├── ChessBoard.kt       # 棋盘状态管理
│   │   ├── Move.kt             # 走法数据类
│   │   ├── MoveValidator.kt    # 走法验证
│   │   └── MoveGenerator.kt    # 走法生成
│   ├── ai/                  # AI引擎
│   │   └── ChessAI.kt          # Alpha-Beta搜索
│   ├── ui/                  # 界面
│   │   ├── MainActivity.kt     # 主菜单
│   │   ├── GameActivity.kt     # 游戏界面
│   │   └── ChessBoardView.kt   # 棋盘绘制
│   └── audio/               # 音效
│       ├── SoundManager.kt     # 音效管理
│       └── SoundGenerator.kt   # 音效生成
├── res/
│   ├── layout/              # 布局文件
│   ├── raw/                 # 音效文件
│   ├── values/              # 主题和字符串
│   └── drawable/            # 图标
└── AndroidManifest.xml
```

## 构建运行

```bash
# 克隆仓库
git clone https://github.com/decli/chinesechess02.git
cd chinesechess02

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# APK 输出位置
# Debug: app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release-unsigned.apk
```

## 系统要求

- Android 8.0 (API 26) 及以上
- 适配 Android 15
- 优化适配 14 寸平板

## License

MIT License
