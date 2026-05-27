LibGDX国际象棋项目AI提示词汇总
（单页打印优化版）

一、项目基础信息
开发者：呼伦贝尔学院24级计算机专业学生Intreturn0
项目：LibGDX+Gradle国际象棋游戏
开发环境：Windows 10 + IntelliJ IDEA Ultimate + PowerShell
项目结构：core(核心逻辑) + lwjgl3(桌面启动)
核心版本：JDK 11、LibGDX 1.12.1
代码仓库：https://github.com/Intreturn/LibGDX-NoClassToday.git
版本控制：Git + GitHub

二、核心问题解决指令
1. 配置与构建排错
- settings.gradle：设置仓库模式为PREFER_SETTINGS，添加国内镜像，声明include(":core", ":lwjgl3")
- 根目录build.gradle：删除所有Eclipse相关配置，定义全局变量gdxVersion=1.12.1，适配IDEA环境
- core模块build.gradle：添加核心依赖api "com.badlogicgames.gdx:gdx:$gdxVersion"，统一Java编译版本为11
- lwjgl3模块build.gradle：添加lwjgl3后端依赖，删除Eclipse代码，配置application插件及主类路径
- 终端构建命令：.\gradlew clean build --refresh-dependencies（解决gradlew无法识别、依赖下载失败问题）

2. 运行与验证标准
- 启动类完整路径：D:\LibGDX\projects\lwjgl3\src\main\java\com\intreturn\noclasstoday\lwjgl3\Lwjgl3Launcher.java
- 验证标准：
  1. 游戏窗口标题显示为"Chess Game"
  2. 窗口分辨率为800×600像素
  3. 8×8国际象棋棋盘网格渲染正常

3. 跨AI协作规范
- 问题诊断需提供：文件原始代码、终端完整报错信息、IDEA界面截图
- 输出要求：结构化总结，便于同步至DeepSeek等其他AI助手

三、项目进度与开发计划
已完成工作
1. 所有配置文件修正完毕，无构建报错
2. 依赖下载成功，LibGDX相关类不再爆红
3. 终端构建输出"BUILD SUCCESSFUL"
4. 游戏窗口正常启动，棋盘网格渲染完成

待开发功能
1. Unicode棋子绘制与初始摆放
2. 鼠标点击交互功能
3. 走棋规则引擎开发
4. 双人对弈核心功能
5. 项目打包部署与功能扩展

四、跨AI协作速查
AI锚点（根据实际进度3选1，复制使用）
说明：选择最匹配的版本，新对话开头粘贴即可一键同步完整上下文

情况1：棋盘为空，未绘制任何棋子
{"project":"LibGDX国际象棋","language":"Java","jdk":"11","framework":"LibGDX1.12.1","build_tool":"Gradle","ide":"IDEA","vcs":"Git+GitHub","repo":"https://github.com/Intreturn/LibGDX-NoClassToday.git","status":"棋盘网格渲染成功，尚未实现任何棋子的绘制和初始摆放","next":["Unicode棋子绘制与初始摆放","鼠标点击交互","走子规则引擎","双人对弈"],"ai_team":{"deepseek":"架构/理论/方案/评审","doubao":"本地操作/调试/排错/构建","copilot":"单文件代码生成"}}

情况2：有棋子，使用英文字母临时表示
{"project":"LibGDX国际象棋","language":"Java","jdk":"11","framework":"LibGDX1.12.1","build_tool":"Gradle","ide":"IDEA","vcs":"Git+GitHub","repo":"https://github.com/Intreturn/LibGDX-NoClassToday.git","status":"棋盘和棋子（临时使用英文字母表示）均已渲染，下一步需替换为Unicode符号","next":["Unicode棋子符号替换","鼠标点击交互","走子规则引擎","双人对弈"],"ai_team":{"deepseek":"架构/理论/方案/评审","doubao":"本地操作/调试/排错/构建","copilot":"单文件代码生成"}}

情况3：棋子已渲染，但存在显示bug
{"project":"LibGDX国际象棋","language":"Java","jdk":"11","framework":"LibGDX1.12.1","build_tool":"Gradle","ide":"IDEA","vcs":"Git+GitHub","repo":"https://github.com/Intreturn/LibGDX-NoClassToday.git","status":"棋盘渲染成功，棋子已渲染但存在显示不全/乱码问题，需完善Unicode支持","next":["修复Unicode棋子显示bug","鼠标点击交互","走子规则引擎","双人对弈"],"ai_team":{"deepseek":"架构/理论/方案/评审","doubao":"本地操作/调试/排错/构建","copilot":"单文件代码生成"}}

常用指令模板
- 概念讲解：模板「XX的原理是什么？」，示例「碰撞的本质是什么？」
- 方案设计：模板「我该怎么做XX？」，示例「怎么让方块响应鼠标点击？」
- 问题诊断：模板「报错信息是XX，可能是什么原因？」，示例「exit value 1怎么排查？」
- 代码评审：模板「这段代码有什么问题？」，示例「评估Copilot生成的棋盘代码」
- 进度总结：模板「总结当前项目进度」，示例「整理已完成的所有事项」
- 跨AI同步：模板「把XX内容整理成发给XX的格式」，示例「把下一步计划发给豆包」
- 工具分析：模板「XX工具和XX工具的区别是什么？」，示例「Copilot和豆包怎么分工？」
- 学习路线：模板「学习XX需要掌握哪些内容？」，示例「用Java做游戏需要学什么？」
- 毕设评估：模板「这个项目能当毕设吗？」，示例「国际象棋项目够毕设水平吗？」
- 知识验证：模板「我的理解对吗？（附上思考）」，示例「移动的本质是循环，对吗？」

五、使用说明
1. 恢复上下文：复制对应进度的AI锚点JSON，粘贴到新对话开头即可
2. 下达指令：从常用指令模板库选择对应模板，填入具体问题内容
3. 同步给豆包：在需要同步的内容前添加前缀「把下面内容整理成发给豆包的格式：」
4. 动态更新：随项目进展及时更新AI锚点、指令模板及进度条目

Word格式快速优化
1. 选中所有一级标题（一、二、三...），设置为黑体12号加粗
2. 选中所有二级标题（1. 2. 3...），设置为黑体11号加粗
3. 选中所有JSON代码，设置字体为Consolas，字号9号
4. 页边距设为上下左右1.5厘米，行间距设为1.15倍