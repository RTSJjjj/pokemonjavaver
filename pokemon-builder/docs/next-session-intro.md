# 开场白（复制到新会话第一条消息）

> 用法：新会话第一条消息直接粘贴下面代码块里的内容。
> 维护：每轮收尾时更新「现状 / 已完成 / 下一步」三段，铁律只在用户加严时改。
> 本轮（B1–B8-A 收尾）已把交接细节单独写成 `docs/stage3-battle-handoff.md`，下面第 1 条指向它。

```
继续 Pokémon Essentials → Java 运行时（pokemon-builder/runtime）的插件复刻，当前做**战斗侧**。
工程根 E:\仓库\范例\929，所有改动未提交，就在工作树里。

【先读文档，按顺序】
1. docs/stage3-battle-handoff.md        —— 【本轮总交接】铁律 / 环境命令 / 8 个 bug 对照表 / 文件所有权 /
                                            6 件待用户拍板事项 / 下一批建议 / 12 条踩坑记录 / 证据目录
2. docs/stage3-wild-battle-audit.md     —— 工作单：§18 用户实测 bug 清单、§19 批次计划、
                                            §20.1-20.11 本轮每个批次的逐行依据 + 27 条"还没啃"清单
3. docs/stage3-berry-port-handoff.md    —— 历史总交接（项目级铁律/环境/早期工作单）
4. docs/stage3-battle-entry-handoff.md  —— 战斗入场过渡（已验收）
5. docs/battle-ui-fixes-plan.md         —— 战斗界面实施细节（历史）

【铁律（用户反复强调，违反会被骂）】
1. 一切以本工程插件为准。源码在 Data/Scripts.rxdata，用 readScriptsSection(projectPath,{withSources:true})
   现场导出；段名精确匹配（含尾空格）。每条实现都要能指到插件行号；文案照抄 _INTL。
2. 【最高优先级】必须逐行转译 Ruby 原文：
   - 一行行对着插件 Ruby 写，不是"看着像"；一次转不完就分批次，时间充足，不许赶。
   - 禁止一切自己造/自己编：自创控制流、自创动画、自创数值、自创文案、自创"等价近似"、自创"可接受简化"。
   - 每处改动都要在该行注释里写明对应 段名:行号；写不出行号就不许写。
   - 无法转译（缺素材/缺数据/插件本身有问题/需先做别的子系统）→ 立刻停止该批次并报告用户，
     不许绕过、不许造替代品、不许默默降级。
   - 插件引用的资源（图/音/数据）必须先确认工程里真实存在再写代码；找不到就立刻停止报告，
     禁止自己造一个资源，禁止留空/注释掉当没看见。
   - 交付按批次报告：本批 段名:行号区间 + 未转的行与原因 + 定点截图证据。
3. 一次只做一个系统（一批），用户验收后再下一个。
4. 老虎机（pbSlotMachine）、挖矿（pbMiningGame）已决定跳过。
5. 验证优先用像素统计/测试，不要一次读很多图。

【环境（详见总交接 §2）】
- subst X: "E:\仓库\范例\929\pokemon-builder"（丢了重建）
- 编译+测试：cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :core:test :lwjgl3:compileJava --console=plain'
  验收一律再跑一次 --rerun-tasks（队友/并发会留下瞬时红，例如 "Unable to delete ... test-results"）。
- 截图探针模式：省略(野生) | trainer | switch | trainer2 | faint | fight-status[|STATUS] | fight-0pp | damage
  cmd /c 'X: >nul && cd X:\runtime && .\gradlew.bat :lwjgl3:captureMenu "-PcaptureArgs=X:\generated|X:\runtime\lwjgl3\<out>|<模式>" --console=plain'
  定点采样用 advanceUntilBattle(predicate, maxFrames)；新增模式**只加分支**，别改现有模式。
- 像素对比工具：python pokemon-builder/runtime/lwjgl3/pixel-diff.py stats|diff ...
  （Pillow 注意：diff 结果要先 convert("RGB") 再看 getbbox()，否则 RGB 差异会被 alpha 掩掉）
- 探针开关：JAVA_TOOL_OPTIONS=-Dpokemon.daynight.hour=19（钉昼夜）、-Dpokemon.capture.sprites=1（dump 精灵表）、
  POKEMON_CAPTURE_PERF=1（性能计数器；**-D 形式传不到 JavaExec**，必须用环境变量）。
- 插件分段导出：工程根 `node __sections.mjs "段名"`、`node __find.mjs "正则"`（每行左侧数字=Ruby 行号）。
- 验证用 Python：C:\Users\Administrator\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe

【现状（本轮收尾基线）】
- core **563 个 @Test 全绿**（本轮从 509 → 563）；builder 295 全绿（未动）。
- 用户实测的 8 个战斗 bug **全部修完**（总交接 §3 有逐条对照表）。
- 探针 7 模式全部 MENU CAPTURE OK；AI 接线做过"接线前 vs 接线后"受控像素回归：**非战斗帧零违规**。

【已完成（战斗侧，均已验收；逐条依据见工作单 §20）】
- B1 入场与派出；B2 换人子系统（含对手换人、玩家倒下换人、Switch 风格询问、"换人占一回合"）；
- B3 倒下演出；B4 升级窗口（两个窗口各自等一次确定键）；B5 结束黑屏 + 战后 16 帧淡入；
- §5.2 双方出招都播报 + 我方倒下提示；§8.1 数据盒状态图标/名字左移/×rank/???/shiny/mega/primal/icon_own；
- §8.2 招式菜单固定 4 槽 + PP 三档配色（修掉"0PP 整格消失、读数错位"）；
- B7 受击闪烁（4 次）+ 未命中/无效/会心/效果/连击文案；
- B8-A 野生 AI = AI_Move:6-155 的"可用招式按分加权随机"（训练家战与野生 BOSS 仍是旧兜底）；
- 引擎侧：命中/闪避公式按 Move_Usage_Calculations:127-142 重写、状态无法行动的文案全套、
  能力升降文案全套、数据盒 batch 颜色泄漏（渲染 bug）修复。

【下一步（建议顺序，等用户点头）】
1. **B8 回合事件有序化**：把 roundMessages/hitEvents/faintEvents 三张并行列表合成一条有序
   RoundEvent{kind: MESSAGE|HIT|FAINT} 事件流，修掉"播报落在受击闪烁之前 + 双方闪烁被合并成一次"两个偏差；
   涉及 Battle.java + BattleScreen.java + InteractiveBattlePort.java 三处，**必须串行做**。
2. 天气/场地子系统（Battle_StartAndEnd:327-352、Battle_Phase_EndOfRound:35-147）。
3. PBEffects 表 → 异常状态扩展（Battler_Statuses）→ 特性子系统（最大一块）。
4. **训练家 AI 建议不要先做**：完整转译 ≈5,065 行，其中 4,176 行与特性/道具/PBEffects/天气不可分割，
   只做骨架会让 pbGetMoveScore 退化成"只有基础分"（明确降级）。

【需要用户决定（不许自行绕过；详见总交接 §6）】
1. 插件 bug 等裁决：Audio_Utilities:1075-1077 少跳 channels 字节 → 倒下动画 delay 2 帧（照抄 A）vs 21 帧（作者本意 B，改 1 行）。
2. 插件点名的 Audio/SE/Exp full、Pkmn level up 本工程不存在（原版也是静音）——现状：照插件点名请求，不挪用别的文件。
3. AI_Move:92-105（只写无人读的全局变量）未转；若要字面 100% 需用户显式下令。
4. 代欧奇希斯形态（AI_Move:133-145）依赖 PBEffects::DeoxysForm + Scene_Commands:149-172 的 T 键 UI，未转。
5. 四招满时的遗忘流程需要概要画面的遗忘模式（Scene_Commands:482-490），未转。
6. Safari 系统未做（pbInSafari? 分支）、SHADOW 属性未建模（Scene_Commands:7「呼唤」）、
   搭档治疗 + $game_player.straighten（PField_Battles:642-649/657）。

【如果用户要求再开队友（本轮做法，效果很好）】
- spawn_teammate **没有 model 参数**，队友一律由 harness 分配 deepseek-v4.1-flash；
  workflow 的 model:"qwen3.8" 覆盖会直接失败（模型未接入）——"qwen3.8/kimi2.7-code"只是角色名。
- 协作纪律：① 队友先做读书笔记（导出 Ruby、列精确行号、核对资源、列出待改 Java 位置）→ 发 Lead 等 GO；
  ② 按文件切写权限，禁改文件只能发"接口请求"；③ **队友自述不作为验收依据**，Lead 必须用独立方法复核
  （模板比对/像素统计/分帧状态机日志/自己重跑测试）；④ 每批报 段名:行号 / 未转的行与原因 / 证据 / 不确定项。

【工作方式】
- 开工先做"读书笔记"：把该批 Ruby 完整导出读一遍、列出精确行号、核对资源是否存在；缺资源立刻停下报告。
- 然后一行行转译，每行带 段名:行号 注释；能测的写测试，界面/演出用探针定点截图做像素统计。
- 每批结束汇报：转了哪些函数（行号区间）、哪些行没转及原因、证据（测试数/像素统计）。
```
