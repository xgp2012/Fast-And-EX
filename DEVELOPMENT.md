# EXClient 开发文档（完整记录，供后续维护参考）

本文件记录 EXClient 项目的全部开发细节、约定、踩坑与本次会话所做的改动，目标是让下一位接手的 AI 无需重新探索即可继续工作。

---

## 1. 项目身份与来源（务必先读）

- **EXClient** 是一个 Minecraft 1.8.9 中文 PVP 客户端。
- 它是 **[HowXu/Chocolate](https://github.com/HowXu/Chocolate)** 的**二次开发（fork）**，原作者为 HowXu，仅用于学习研究，版权归原作者。README 顶部已注明此声明。
- 底层基于 **Tritium-X**（来自 [ImFl0wow/Tritium-Open](https://github.com/ImFl0wow/Tritium-Open)）。
- 仓库当前位置：`https://github.com/xgp2012/Fast-And-EX`（remote `origin`）。
- 感谢来源（README 末尾）：Tritium-Open 源码、DragonWingsMod（龙翼）、OPPO OPPOSans 字体。
- 根目录有 `LICENSE` 文件（原始 Chocolate 许可证），再分发前请先确认其条款。

## 2. 源码三部分结构（最重要：避免改错地方）

`src/main/java` 下分三部分，权限和归属完全不同：

1. **`cn/howxu/**`** —— 本项目自有代码（`gui`/`module`/`render`/`ELog`）。**这是主要工作区，可自由编辑。**
2. **`tritium/**` 与 `me/imflowow/**`** —— 导入的 Tritium-X 基础代码。在本仓库内是普通目录（**不是子模块**），可编辑，但应视为“基础”，改动即与上游 Tritium-X 分叉，合并上游时需手动处理。
3. **`net/**` 与 `javax/**`** —— **git 子模块**（外部仓库），**禁止编辑**：
   - `src/main/java/net` → `https://github.com/HowXuVSCode/Chocolate_mcp_src`（反混淆后的 Minecraft 源码）
   - `src/main/java/javax` → `https://github.com/HowXuVSCode/Chocolate_javax_src`（反混淆后的 javax 源码）
   - 这两个子模块的 URL 在 `.gitmodules` 中，是外部仓库真名，**不要改**。
   - **关键约束**：CI 在构建时会按记录的 commit 重新拉取子模块原版。即便本地改了 `net/...` 里的文件，提交到 superproject 的 gitlink 指向的也是本地才有的 commit，CI `git submodule update` 会失败或拉回原版。**因此任何想改 MC 源码（如 `Minecraft.java`）的需求，都应改为“fork 子模块”方案（见第 9 节）。**

资源目录：`src/main/java/resources/` 含 `assets/`（如 `tritium/icons/logo_white.png`）、`log4j2.xml`、`pack.png`、`META-INF`。

## 3. 环境与工具链（硬性要求）

- **JDK 8  ONLY**。CI 用 Zulu JDK 8 + Gradle 8.5。新版本 JDK 会破坏 1.8.9 客户端（字节码/模块系统不兼容）。
- Gradle Wrapper 固定 **8.5**（`gradle/wrapper/gradle-wrapper.properties`）。
- 自定义 Gradle 插件 **`chocolate_gradle`**：`com.github.howxu:chocolate_gradle:v1.4`（来自 jitpack）。它提供 `runClient` / `getRuntimeResources` / `buildArch` 等任务，**不是标准 `application` 插件**，不要用普通 Java 任务替代。
- `build.gradle.kts` 里 `tasks.withType<JavaCompile> { options.encoding = "UTF-8" }`：源码/注释是中文，必须 UTF-8。
- `group = "cn.howxu.exclient"`，`version = "2.2"`（见 `build.gradle.kts`）。
- 本机 Git 使用 **Git Credential Manager**（`credential.helper=manager`）+ **schannel** SSL 后端（`http.sslbackend=schannel`），即走 Windows 证书库。**这与 JDK 的 cacerts 无关**——本地 JDK 的 SSL 证书问题不会影响 `git` 拉取（见第 6、10 节）。
- 本机没有安装 `gh` CLI；推送一律用 `git` CLI（凭据管理器已缓存 xgp2012 账号）。

## 4. 子模块初始化（构建前置）

没有子模块，编译直接失败。命令：

```bash
git submodule update --init --recursive
# 或首次克隆：git clone --recursive <repo>
```

CI 的 `release.yml` 也会手动 `git submodule init` / `git submodule update`。

## 5. 开发/构建/运行命令（顺序很重要）

```bash
git submodule update --init --recursive   # 1. 先拉子模块
./gradlew check                            # 2. 拉依赖（必须最先跑）
./gradlew getRuntimeResources              # 3. 拉运行时资源/动态库（运行前必须）
./gradlew runClient                        # 运行客户端
./gradlew build                            # 单个 jar -> build/libs
./gradlew buildArch                        # 发布包 -> build/cache/*.zip（文件名由插件决定）
```

- `getNativesResources` 已包含在 `getRuntimeResources` 中，一般无需单独跑。
- Windows 上脚本是 `gradlew.bat`。

## 6. 依赖仓库的坑（重要，已修）

`build.gradle.kts` 的 `repositories` 当前顺序（**必须保持**）：

```
mavenCentral()
maven { url = "https://libraries.minecraft.net/" }   // Mojang 官方库，稳定，含 netty/icu4j/codecjorbis/oshi 等
maven { url = "https://nexus.velocitypowered.com/repository/maven-public/" } // tv.twitch 等少数库在这；该源不稳定(偶发 522)，放最后兜底
```

**原因**：`nexus.velocitypowered.com` 会返回 **522**（Cloudflare 连不上源站）。Gradle 对仓库返回 **非 404 错误（如 522）会直接中止解析、不再尝试后续仓库**。所以 velocitypowered 必须放在 `libraries.minecraft.net` **之后**，否则那 4 个 MC 依赖会因前者 522 而整体解析失败（`compileJava` 报 `Could not resolve com.mojang:netty` 等）。

- `libraries.minecraft.net` 已验证包含：`com.mojang:netty:1.8.8`、`com.ibm.icu:icu4j-core-mojang:51.2`、`com.paulscode:codecjorbis:20101023`、`oshi-project:oshi-core:1.1`。
- `tv.twitch` 相关库似乎只在 velocitypowered，故它作最后兜底。若该源长期挂掉且 tv.twitch 成为必需，需另找镜像。

## 7. CI / 自动发布

- 工作流文件：`.github/workflows/release.yml`。
- **触发条件**：push 打了 `v*` 标签（如 `v2.2`）。
- 运行环境：`windows-latest`，`actions/setup-java` 用 **Zulu JDK 8**，`gradle/gradle-build-action@v3`。
- 步骤：checkout → JDK → Gradle → `git submodule init/update` → `./gradlew.bat check` → `./gradlew.bat buildArch` → `softprops/action-gh-release@v2`。
- 发布权限：`permissions: contents: write`，使用 `secrets.GITHUB_TOKEN`（**不要用原来的 `secrets.TOKEN`**，fork 上没有该 secret 会失败）。
- 上传文件：`files: ./build/cache/*.zip`（用通配，避免插件产出文件名不确定导致找不到文件）。
- **如何触发一次发布**：打 `v*` 标签并推送即可。若仓库的 **Actions 功能被关闭**，标签推送不会触发运行——先到仓库 Settings → Actions 开启。重触发方法（本会话一直用这个）：
  ```bash
  git push origin --delete v2.2
  git tag -d v2.2
  git tag v2.2
  git push origin v2.2
  ```
- 仓库没有 `gh`，全部用 `git` CLI 推送。

## 8. 本次会话已做的改动（按顺序）

1. **性能 - 事件总线** `tritium/api/utils/event/api/EventManager.java`
   - 原 `call()` 每次派发都用 `Method.invoke` 反射调用订阅者；高频事件（`Render2D/Render3D/Tick`）逐帧触发，反射有开销。
   - 改为：注册时把 `Method` 转成 `MethodHandle` 缓存（`asType` 成 `(Object)Object`），派发走 `handle.invoke((Object) argument)`。
   - **回退保护**：若 `MethodHandles.lookup().unreflect` 因访问权限失败（JDK 8 下其他包的 private/包级处理器），`handle` 为 null，自动回退到原反射路径，行为不变。
   - 说明：公开处理器走 MethodHandle 提速；私有/外部包处理器仍走反射。要全覆盖需特权 lookup（JDK 8 较脆弱的 hack），未采用。

2. **性能 - 龙翼颜色** `cn/howxu/module/Wings.java`
   - `getColors()` 原来每次渲染龙翼都 `new float[3]`，改为复用 `cachedColors` 缓冲，消除每帧分配。

3. **崩溃修复 - 启动画面 NPE** `tritium/api/utils/render/special/SplashRender.java`
   - 崩溃栈：`EntityRenderer.updateShaderGroupSize`(line 439) → `mc.renderGlobal.createBindEntityOutlineFbs(...)` 时 `renderGlobal` 为 null。
   - 调用链：`SplashRender.drawSplashScreen` → `mc.updateDisplay()` → `checkWindowResize()` → `resize()` → `updateFramebufferSize()` → `updateShaderGroupSize()`。启动早期 `renderGlobal` 尚未初始化，且本机开 OptiFine、支持 shader，故该分支被执行 → NPE。
   - 修复：`drawSplashScreen` 末尾把 `mc.updateDisplay()` 换成 `Display.update()`（启动画面只需交换缓冲上屏，不需走 resize 检查）。
   - 与本次其他性能改动无关，是 Tritium 基础代码的潜在 bug。

4. **重命名为 EXClient**（可见名）
   - `settings.gradle.kts`：`rootProject.name = "EXClient"`。
   - `Chocolate.json`：`"id": "EXClient"`（启动器显示的版本名）。
   - `me/imflowow/tritium/client/ui/mainmenu/MainMenu.java`：主菜单标题 `TitleText = "EXClient"`。
   - `me/imflowow/tritium/client/ui/clickgui/components/Window.java`：ClickGUI 窗口标题 `drawString("EXClient", ...)`。
   - `me/imflowow/tritium/core/Tritium.java`（两处）与 `ClientListener.java`：客户端数据目录 `chocolate` → `exclient`（即 `.minecraft/exclient`）。
   - `README.md`：标题、版本文件夹名、启动版本名、章节标题、克隆地址改为 `xgp2012/Fast-And-EX`；并新增二次开发声明（指向 HowXu/Chocolate）。
   - `AGENTS.md`：项目描述改为 EXClient。
   - `build.gradle.kts`：`group = "cn.howxu.exclient"`。
   - `.github/workflows/release.yml`：上传路径改为 `./build/cache/*.zip`。

5. **窗口标题重命名（CI 安全做法）** `me/imflowow/tritium/core/ClientListener.java`
   - 游戏窗口标题在 `net` 子模块的 `Minecraft.java`（`Display.setTitle("Chocolate 1.8.9")`）里，属于外部子模块，不能改、CI 也不会生效。
   - 改为在**本项目自有可编辑代码**里覆盖：新增 `onTickRename(TickEvent)` 处理器，在第一个 client tick 时 `Display.setTitle("EXClient 1.8.9")` 一次（`titleRenamed` 布尔守卫）。
   - 效果：启动后窗口标题即显示 EXClient（加载瞬间可能闪一下 "Chocolate"，之后被覆盖）。无需动子模块，CI 生效。

**刻意保留未改的**：
- `chocolate_gradle` 插件名及其仓库链接（`Chocolate_Gradle`）——这是依赖项，不是本项目名。
- `.gitmodules` 中子模块 URL 的 `Chocolate_mcp_src` / `Chocolate_javax_src` —— 外部仓库真名，必须保留。
- `Minecraft.java` 里的 `"Chocolate 1.8.9"` 原始字符串 —— 子模块内，未改（见第 9 节真正的改名方案）。

## 9. 已知限制与后续可做（给下一位 AI）

- **窗口标题真正改名**：当前是“覆盖”方案。若要彻底改掉 `net` 子模块里的字符串，需 fork 子模块：
  1. 在 GitHub 新建仓库（如 `xgp2012/Chocolate_mcp_src`），把 `Chocolate_mcp_src` 的内容（含本次要改的那一行）推上去。
  2. 本地在子模块内改 `Minecraft.java` 的 `Display.setTitle("EXClient 1.8.9")`，于子模块内 `git commit`。
  3. 改 `.gitmodules` 里 `src/main/java/net` 的 url 为新 fork 地址；`git submodule sync`。
  4. 提交 superproject（gitlink 指向新 commit），并把 fork 推到 GitHub。
  5. 之后 CI `git submodule update` 会拉到 fork 的新 commit，标题即原生 EXClient。
  - 注意：改 `.gitmodules` 指向尚不存在的仓库会让 CI 暂时失败，直到 fork 推上去。

- **`group` 改动风险**：`group = "cn.howxu.exclient"` 若被 `chocolate_gradle` 插件用于计算某些输出路径，构建可能异常；若 CI 报相关错误，先回退 group 或排查插件。

- **没有测试**：仓库无任何 `src/test`、无 JUnit。不要引入测试命令或假设有测试套件。验证改动只能靠构建 + 手动运行客户端。

- **javax 子模块缺失会导致编译失败**：`compileJava` 需要 `javax` 源码。务必先 `git submodule update --init`。

- **本地 JDK SSL 问题（仅本地构建，非 CI）**：若本机用老旧 JDK 8，`gradlew` 下载 Gradle 分发包或拉依赖时可能 `PKIX path building failed`/`522`。解决：用较新的 Zulu/Adoptium JDK 8；或 `set JAVA_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`；或导入代理 CA 到 `cacerts`。`git` 本身不受影响（schannel）。

- **性能优化方向（未做，需先 profile）**：建议用 VisualVM/JFR 抓一帧的 CPU 与分配再动手。事件总线反射提速仅对公开处理器有效；其余可看实体/粒子剔除、渲染分配等。

## 10. 快速接手清单

1. 拉代码 + `git submodule update --init --recursive`。
2. 装 Zulu JDK 8，确认 `gradlew -version` 用 JDK 8。
3. `./gradlew check` → `./gradlew getRuntimeResources` → `./gradlew runClient` 验证可跑。
4. 改代码优先在 `cn/howxu/**`；动 `tritium/`/`me.imflowow/` 视为改基础；**绝不**改 `net/`/`javax/`（子模块）。
5. 想发布：提交后打 `v*` 标签并推送（注意先开启仓库 Actions）。
6. 遇到依赖解析失败先想 velocitypowered 522 与仓库顺序（第 6 节）。
