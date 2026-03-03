# ♟ 中国象棋 - 在线人机对战

一款纯前端中国象棋游戏，支持 AI 人机对战、5级难度、悔棋、提示、语音播报、进度保存，界面对老人友好。

🎮 **在线体验**: [https://decli.github.io/Chinesechess/](https://decli.github.io/Chinesechess/)

![中国象棋截图](https://img.shields.io/badge/中国象棋-在线对战-red?style=for-the-badge)

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 🤖 **AI 对战** | Alpha-Beta 剪枝算法，Web Worker 中运行不阻塞 UI |
| ⭐ **5级难度** | 入门 → 初级 → 中级 → 高级 → 大师 |
| ↩️ **悔棋** | AI 模式自动回退两步（一个完整回合） |
| 💡 **提示** | 紫色箭头显示推荐走法 |
| 🔊 **语音播报** | 每步棋用中文语音播报（如"红方，马八进七"） |
| 💾 **保存/加载** | 3个存档槽位 + 自动保存（关闭页面再打开自动恢复） |
| 📝 **走法记录** | 标准中文棋谱记法实时记录 |
| 👴 **老人友好** | 大字体(18-22px)、大按钮(52px)、高对比度暗色主题 |
| 📱 **响应式** | 桌面横排布局，移动端自适应竖排 |
| 🔄 **执子切换** | 可选执红先行或执黑后行 |

## 🎯 象棋规则

完整实现中国象棋全部规则：
- 7种棋子走法（帅/将、仕/士、相/象、马、车、炮、兵/卒）
- 蹩马腿、塞象眼检测
- 将帅不能面对面（会面检测）
- 将军、将杀、困毙判定
- 走后不能被将的合法性验证

## 🛠 技术架构

```
纯静态 HTML + CSS + JavaScript，无需后端
```

- **渲染**: HTML5 Canvas 木纹风格棋盘
- **AI**: Minimax + Alpha-Beta 剪枝 + 位置价值表，运行在 Web Worker
- **存储**: localStorage
- **语音**: Web Speech API (浏览器原生)
- **部署**: GitHub Pages / Cloudflare Pages

## 📁 项目结构

```
├── index.html          # 主页面
├── css/
│   └── style.css       # 暗色主题样式
├── js/
│   ├── rules.js        # 规则引擎
│   ├── game.js         # 棋局状态管理
│   ├── board.js        # Canvas 棋盘渲染
│   ├── ai-worker.js    # AI Web Worker
│   ├── ai.js           # AI 接口封装
│   ├── storage.js      # 本地存储
│   └── main.js         # 主控制器
└── README.md
```

## 🚀 本地运行

```bash
# 克隆仓库
git clone https://github.com/decli/Chinesechess.git
cd Chinesechess

# 任选其一启动本地服务器
npx -y http-server ./ -p 8080
# 或
python -m http.server 8080

# 访问 http://localhost:8080
```

## 📄 License

MIT License
