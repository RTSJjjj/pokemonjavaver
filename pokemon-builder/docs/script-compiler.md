# Script Compiler（事件脚本 → IR）

> 对应任务书 §4、§21–§25、§58–§59、§78–§79、§84。实现：`builder/src/script-compiler.js`，
> 事件分析在 `tools/event-analyzer/`；运行端消费在 `runtime/core/.../event/ScriptIr.java`。

## 1. 为什么需要它

RMXP 事件里的脚本框（命令 355/655）是 Ruby。运行时不带 Ruby（§84），因此 Builder 在
**构建阶段**把每个脚本块编译成 JSON IR，运行时只按块 id 执行命令。

```text
Data/MapXXX.rxdata 事件 355/655
        │  event-analyzer：合并 355+655 续行、分类（Essentials API / 插件 / 简单表达式 / 未知）
        ▼
generated/scripts/blocks.json      （每块：id、位置、category、apis、calls、rubySource）
        │  compileBlocks()：语句拆分 → 逐条匹配 HANDLERS → SEQUENCE/CONDITION
        ▼
generated/scripts/ir.json          （运行时读；CONDITION 块不产出 IR 条目）
build/reports/script-coverage.json
build/reports/java-handler-required.json
docs/unsupported-scripts.md
```

## 2. 块与位置

- 块 id：`map<mapId>/event<eventId>/page<page>/cmd<commandIndex>`（公共事件为
  `commonEvent<id>/page<p>/cmd<i>`）。
- 位置对象（报告与失败信息用）：`{mapId, eventId, page, commandIndex, file}`；
  §59 要求未支持脚本必须带此位置失败。
- 一个块可以包含多条语句（同一脚本框多行）；编译器按顶层语句逐条处理，成功时
  合成一条 `SEQUENCE`。

## 3. 命令覆盖（HANDLERS，节选）

| Ruby 写法 | IR |
|---|---|
| `pbItemBall(:POTION, 3)` / `pbReceiveItem(...)` | `GIVE_ITEM {item, amount}` |
| `pbDeleteItem(...)` | `REMOVE_ITEM {item, amount}` |
| `pbSetSelfSwitch(event, "A", true[, mapId])` | `SET_SELF_SWITCH {eventId, channel, value[, mapId]}` |
| `setTempSwitchOn/Off("A")` | `SET_TEMP_SWITCH {channel, value}` |
| `pbSmashThisEvent` | `ERASE_EVENT` |
| `activateQuest / advanceQuestToStage / completeQuest` | `ACTIVATE_QUEST / ADVANCE_QUEST_TO_STAGE / COMPLETE_QUEST` |
| `pbGetKeyItem(...)` / `pbToggleFollowingPokemon` / `pbPokemonFollow` | `GIVE_KEY_ITEM / TOGGLE_FOLLOWING_POKEMON / POKEMON_FOLLOW` |
| `pbBridgeOn/Off` | `SET_BRIDGE {on}` |
| `pbSEPlay(name)` | `PLAY_SE {file}`（缺音频只告警） |
| `pbWait(frames)` | `WAIT {seconds = frames/40}` |
| `pbCaveEntrance / pbCaveExit` | `CAVE_ENTRANCE {exiting}`（等动画时长） |
| `pbCryFile(N)` + `pbSEPlay(cry) if cry` 惯用式 | `PLAY_CRY {species}`（两句合并） |
| `pbExclaim(get_character(n))` | `EXCLAIM {character, animationId}` |
| `pbPushThisBoulder` | `PUSH_BOULDER` |
| `toggle_liefeng_switches` | `TOGGLE_PLATE_SWITCHES` |
| `pbTrainerIntro/End` | `TRAINER_INTRO / TRAINER_END`（战斗域占位，阶段 3） |
| 简单条件表达式 `$game_switches[n] == true` 等 | `CONDITION {expression}`（条件分支内联，不进 IR） |

参数解析：`:SYMBOL`、整数、`"字符串"`、`true/false/nil`、数组、`_INTL("a","b")`
（拼成纯文本）；无法静态求值的参数保留为 `{script: "..."}`。

## 4. 三类结果与策略

- `TRANSLATED`：产出 IR，运行时执行。
- `JAVA_HANDLER_REQUIRED`：命中宝可梦域/战斗域（阶段 3），如实登记到
  `build/reports/java-handler-required.json`，**不算失败**。
- `UNSUPPORTED`：无法解释的 Ruby（不在任何域清单里）。**发布策略**（§24/§59）：
  `evaluateReleasePolicy(problems)` 在存在 unsupported 时返回 `exitCode: 1`；
  仅显式开发开关（`allowUnsupported`，`build-data`/`build-pc` 开发构建使用）放行，
  且必须把块位置与 Ruby 原文写入报告。

当前真实工程（L6 消化 `pbNoticePlayer`/`pbSave` 后，2026-10-04）：5441 块，
**translated 4052 / javaHandlerRequired 891 / unsupported 498**，有效覆盖 **90.8 %**
（L6 前为 3712 / 891 / 838，84.6 %）。

## 5. 运行时消费

- `ScriptIr.load(file)` 读 `ir.json`；`EventInterpreter.attachScriptIr(...)` 注入。
- 事件命中脚本命令（355/655/509）时按 `scriptBlockId` 取 IR 执行：
  - `SEQUENCE` 逐步播放，前一步等待（如 `SHOW_TEXT`）结束后继续；
  - 所有命令可无图形执行（`ScriptIrExecutionTest`/fixture 覆盖）。
- **R15 修复**：RMXP 把 355 与其后的 655 续行合并为一段脚本；块内每条命令都带
  同一 `scriptBlockId`，解释器现在在 355 执行后跳过同 id 的 655 续行（此前会执行
  两次，fixture golden 抓到 `GIVE_ITEM` 发 6 个药）。

## 6. 测试

- `builder/tests/script-compiler.test.js`：单块/多语句/条件/参数解析/发布策略。
- `builder/tests/fixture-project.test.js`（R15）：`pbItemBall(:POTION,3)` →
  `{GIVE_ITEM,POTION,3}`；未知 Ruby 必须带 Map/Event/Page/Command 位置且发布失败。
- `runtime/core/.../ScriptIrExecutionTest`：IR 命令的真实执行（含等待、门控）。
- 重建：`builder.bat build-data <project>` 会重写 `generated/scripts` 并立即重编译 IR
  （数据重建不会孤立 IR）。
