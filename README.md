# 重生之满级暴击 · 开发手册

> Slogan：重生归零，暴击满级，每一次重来都是更强的自己
> 开发者：云麓盒子　|　引擎：LibGDX 1.12.1 (Java)　|　平台：Android 优先，Windows 复用同一 core

## 一、工程结构

```
E:\重生之满级暴击\
├── core/            平台无关主逻辑（Android / Windows 共用）
│   └── src/main/java/com/yunluhezi/maxcrit/
│       ├── core/        MaxCritGame 主类、PlatformBridge 平台桥
│       ├── asset/       GameAssets 资源管理
│       ├── config/      ConfigHub + SkillDef/MonsterDef/EquipDef/TalentDef/StageDef
│       ├── save/        SaveManager（CRC 校验存档）、MetaProgress（养成→面板换算）
│       ├── battle/      BattleWorld 战斗核心
│       │   ├── entity/  玩家 / 怪物 / 子弹 / 经验球 / 召唤物
│       │   ├── wave/    WaveManager 波次与难度膨胀
│       │   ├── skill/   SkillSystem 三选一抽取
│       │   ├── damage/  DamageCalculator 满级暴击乘区
│       │   └── pool/    Pools 对象池
│       ├── ui/          Hud、VirtualJoystick、SkillChoiceDialog
│       ├── screen/      Menu / Game / Talent / Bag
│       └── util/        Ui（白纹理/圆形/坐标换算）、Colors
├── android/         Android 启动器（applicationId: com.yunluhezi.maxcrit）
├── desktop/         Windows 启动器（lwjgl3）
├── assets/          运行时资源
│   ├── textures/icons/  41 个图标/立绘/特效（512px PNG8）
│   ├── textures/ui/     79 张 UI 成品底板（1280×720 JPG）
│   ├── ui/font/         deskhan.fnt + deskhan.png（中文 BitmapFont）
│   ├── config/          skill / monster / equip / talent 四张 JSON 表
│   └── sound/           音效（放入 wav 即可自动加载）
├── libs/            本地离线依赖（gdx 1.12.1 全家桶）
├── tools/
│   └── build_assets.py  资源管线：切图 / 缩放 / 生成中文位图字体
└── 方案.txt / 音效方案.txt
```

## 二、构建命令

```bash
cd E:/重生之满级暴击
export JAVA_HOME="D:/TOOL/jdk"
export ANDROID_HOME="D:/Android/Sdk"
export ANDROID_SDK_ROOT="D:/Android/Sdk"

# Android APK（debug）
E:/tool/gradle-8.9/bin/gradle.bat :android:assembleDebug
# 产物：android/build/outputs/apk/debug/android-debug.apk

# Windows 桌面版
E:/tool/gradle-8.9/bin/gradle.bat :desktop:run

# 资源重建（改了 sucai/ 之后执行）
"C:/Users/19513/.workbuddy/binaries/python/versions/3.13.12/python.exe" tools/build_assets.py
```

> 工程路径含中文，AGP 默认会拒绝；`gradle.properties` 里已设 `android.overridePathCheck=true`。

## 三、核心机制

### 1. 满级暴击乘区（battle/damage/DamageCalculator.java）

```
最终伤害 = 基础攻击
         × 流派倍率 × 神装套装系数 × 符文增幅 × 回溯印记加成 × 天赋/局内被动
         × 暴击判定系数 × 满级超额暴击系数
```

- 暴击判定：`roll(0~100) < 暴击率`（已扣除目标暴击抗性）
- 暴击回溯：命中后回血 `基础攻击×2%`，缩减技能 CD 0.15s
- 满级暴击：暴击率 ≥ 85% **且** 爆伤加成 ≥ 150% 时，暴击命中额外触发范围冲击波（2.5 倍伤害）
- 阈值常量在 `DamageCalculator.OVER_CRIT_CRIT_RATE` / `OVER_CRIT_DAMAGE_BONUS`

### 2. 回溯印记重生（save/MetaProgress.java）

每枚印记永久 +1.5% 暴击率、+4% 爆伤。无论通关失败都发放，是「每一局都更强」的货币。

### 3. 养成闭环

| 系统 | 位置 | 说明 |
|---|---|---|
| 回溯印记 | SaveManager.echoMarks | 暴击率/爆伤永久加成 |
| 神装 6 部位 | weapon/helmet/cape/amulet/boots/ring | 2/4/6 件激活套装（裂隙/虚空/破晓） |
| 暴击符文 | SaveManager.runes | 每枚 +1.5% 暴击率、+5% 爆伤 |
| 觉醒天赋树 | 瞬杀/永续/神佑 三系 9 节点 | 消耗天赋点，前置节点解锁 |

### 4. 战斗循环

摇杆走位 → 自动武器按「距离优先」锁敌 → 击杀掉经验球 → 满经验弹出**三选一**（稀有度影响权重）→ 波次推进（小怪→精英→BOSS）→ BOSS 掉**暴击突破晶** → 结算发放回溯印记。

## 四、素材说明

`sucai/` 下 121 张源图分类：

- **95 张 2848×1600**：已渲染的完整 UI 成品稿（含中文文案）。按「底板 + 动态数据层」使用：图做背景，数值/列表/按钮用 Scene2D 动态绘制。
- **26 张 2048×2048**：图标、装备图、技能图、怪物立绘、场景、Logo。

`tools/build_assets.py` 负责：UI 图缩到 1280×720 转 JPG、图标缩到 512 转 PNG8、生成中文 BitmapFont（黑体，覆盖项目全部用字）。

**缺失素材（当前用程序化替代）**：怪物行走帧、子弹、经验球、粒子贴图、音效。现有实现用立绘 + 缩放旋转 + 程序化圆形贴图表现战斗。

## 五、音效

`音效方案.txt` 已列全清单（Kenney Sci-Fi Sounds / Kenney RPG Audio，CC0 可商用）。把 wav 放进 `assets/sound/`，`GameAssets` 会自动扫描加载：

- `sfx_hit_normal.wav` / `sfx_critical_hit.wav` / `sfx_enemy_death.wav` / `sfx_boss_death.wav`
- `sfx_pickup_exp.wav` / `sfx_pickup_crystal.wav` / `sfx_levelup.wav`
- `sfx_ui_click.wav` / `sfx_ui_confirm.wav` / `sfx_ui_cancel.wav`
- `bgm_menu.ogg` / `bgm_void_gap.ogg` / `bgm_boss_theme.ogg`

调用：`game.assets.play("sfx_critical_hit", 0.8f)`、`game.assets.playMusic("bgm_menu", 0.5f)`。

## 六、已知限制

- 无尽爬塔与联机模式尚未实现（`network` 包预留空目录），GameScreen 目前只有裂隙闯关。
- 符文系统只有接口与 UI 提示，镶嵌逻辑未做。
- 屏幕外怪物休眠只做了标记位，实际剔除逻辑在 `BattleWorld.cleanup()` 里未启用。
- UI 底板未做九宫格切分，当前整图拉伸，移动端高分屏下边缘可能略糊。
