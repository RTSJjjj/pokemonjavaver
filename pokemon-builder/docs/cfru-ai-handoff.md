# CFRU trainer AI port - handoff (phase 1, batch 1)

Source: `Skeli789/Complete-Fire-Red-Upgrade` `src/Battle_AI/` (`ai_master.c`, `ai_negatives.c`, `ai_positives.c`, `ai_util.c`,
`ai_advanced.c`) and `src/damage_calc.c` (`AI_CalcDmg`). Fetch with
`curl https://raw.githubusercontent.com/Skeli789/Complete-Fire-Red-Upgrade/master/src/Battle_AI/<file>` (the GitHub API is blocked in the sandbox,
raw.githubusercontent works). Decisions with the user: one tier for every trainer = `CHECK_BAD_MOVE | CHECK_GOOD_MOVE`
(Negatives + Positives, `SemiSmart` is only for non-smart trainers), no difficulty options, Mega Evolution AI stays the project's own,
wild battles keep `BattleAi.handles` (rank < 2); wild Bosses (rank >= 2) and single-battle trainer battles now use the scorer,
`defaultAi` stays the fallback, doubles are phase 4.

## Files (package `pokemon.runtime.battle`)
- `AiMaster` - `ChooseMoveOrAction_Singles`, score setup (100 / 0), `BattleAI_DoAIProcessing`, `PredictMovesForBanks`.
  Hooked in `Battle.pickMove` after the wild branch.
- `AiCtx` - `AI_THINKING_STRUCT` + the per-decision caches (predictions, strongest move).
- `AiCalc` - the `ai_util.c` / `ai_advanced.c` helpers (AI damage = top roll x 93%, hit chance, KO maths, strongest move, speed/priority order,
  secondary damage, fighting style).
- `AiNegatives` - `AIScript_Negatives`: Gravity/Powder, type-absorbing + blocking Abilities, Terrain, Throat Chop, Heal Block, primal weather,
  `AI_STANDARD_DAMAGE`.
- `AiPositives` - `AIScript_Positives` tail + `DamageMoveViabilityIncrease` (single battle) + `IncreaseViabilityForSlowKOMove`.
- `AiUtil`, `AiSignature` - earlier helpers; `AiSignature` (original-move bonus) is still not called.

## Batch 2 (done)
`AiNegativeEffects`: ai_negatives.c effect cases for Sleep (:760), Absorb/Strength Sap (:782), Explosion (:800, `OKAY_WITH_AI_SUICIDE` is defined in CFRU's
config.h), Dream Eater, Splash, Teleport, all stat-raising effects (:931-1228) and stat-lowering effects (:1240-1325). Function codes are now checked
against the movefx class list (e.g. King's Shield 14B, Spiky Shield 14C, Aurora Veil 167, Sleep Talk 0B4, Snore 011, Laser Focus 15E); batch 1's wrong
guesses were corrected. Any effect without a case runs `AI_STANDARD_DAMAGE`, as in C.

## Batch 3 (done)
`AiNegativeEffects` now covers essentially all of ai_negatives.c:760-3120 that does not need data this runtime lacks (parts 1-4: stat moves, sleep/status
moves, recovery, screens, recoil, protect, hazards, weather, two-turn attacks, baton pass, field effects, terrains, type changers...).

## Batch 4 (done)
`AiPositiveHelpers` (IncreaseStatusViability / IncreaseStatViability, ShouldTryToSetUpStat, GoodIdea/BadIdea to raise or lower a stat, ShouldRecover,
ShouldPhaze, IncreaseSleepViability, GetAmountToRecoverBy, CountUsefulStatChanges) and `AiPositiveEffects` part 1 (ai_positives.c:57-1010): sleep/yawn,
drain, all stat raising/lowering moves, Haze, Roar, recovery, Rest, poison, Mist, Focus Energy, Confuse, Paralyze, Leech Seed, Snore/Sleep Talk, Laser Focus.
`HasUsedMove*` history tests: see Batch 8 (before that they were treated as false).

## Batch 5 (done)
`AiPositiveEffects` part 2 (ai_positives.c:1010-1760): Destiny Bond, Nightmare, Curse, Foresight/Miracle Eye, Perish Song, Swagger/Flatter, Attract, Safeguard,
Rollout, Fury Cutter, Belly Drum, Sun/Rain/Sandstorm, Pursuit, Baton Pass class bonus, and entry hazards (`IncreaseEntryHazardsViability`).

## Batch 6 (done)
`AiPositiveEffects` part 3 (ai_positives.c:1760-2125): Fake Out, Hail, Torment, Will-O-Wisp, Memento, Taunt, Ingrain/Aqua Ring, Magic Coat, Brick Break.

## Phase 2: switching (done, singles)
`AiSwitching` (ai_switching.c): `CalcMostSuitableMonToSwitchInto` (:1980-2539, bench Pokemon are scored with the same `AiCalc` damage code) and these
`ShouldSwitch` reasons, in source order: type-absorb switch (:403), only-bad-moves (:256 + `CalcOnlyBadMovesLeftInMoveset`), Natural Cure/Regenerator (:635),
yawned (:975), asleep (:1086), annoying secondary damage (:1179), avoid death (:1248), low offensive stats (:1550), save sweeper (:1698).
Wiring: `Battle.chooseFor` registers `:SwitchOut` for a trainer's single-battle foe (`AiSwitching.decide`), `Battle.pbAttackPhaseSwitchOpposing`
plays it (Battle_Phase_Attack:50-71) before the moves, and `Battle.defaultChooseNewEnemy` picks the replacement after a faint with the bench scorer.
登记 (not transcribed): `PassOnWish` (:751), `CanStopLockedMove` (:939), `SemiInvulnerableTroll` (:803, never TRUE in the source), `ShouldSwitchIfPerishSong`
(body not in the cached source), `ShouldSwitchIfWonderGuard` (:1323), the pivot hand-off (a fast pivoting move simply declines the switch),
Disguise/Imposter/Trace/Dynamax/Steelsurge on the incoming Pokemon, Wish recovery, `switchingCooldown` (read as "no turn yet"; its setter is outside the cached files),
the player's and the opponent's switches of one round run player first. Doubles are phase 4.

## Phase 3: enemy item use (done, singles)
Not CFRU's `ShouldAIUseItem` (it needs CFRU's item-effect table, which the project's items lack) but the plugin's own item AI, `143_AI_Item.rb`
(`pbEnemyShouldUseItem?` / `pbEnemyItemToUse`), which `pbDefaultChooseEnemyCommand` calls right after the withdraw check (142_PokeBattle_AI.rb:168-170): `AiItems`.
`Battle.foeItems` holds `TrainerData.items` (filled in `InteractiveBattlePort.Session`), `chooseFor` registers `:UseItem` after the switch decision,
`pbAttackPhaseSwitchOpposing` runs the opposing `:UseItem`s (139_Battle_Phase_Attack:72-93).
登记: the item handlers `triggerCanUseInBattle` / `BattleUseOnBattler` (PItem_BattleItemEffects) are not in the plugin source here - "can use" is read as
missing HP / matching status / stat below +6, healing amounts are `ItemUse.healValue`, X items use the stat/stages of the plugin's own `xItems` table, and the per-item
result messages are not shown (only `pbUseItemMessage`). Doubles trainers' item lists (`@items[owner]`) are phase 4.

## Phase 4: doubles move/target choice (partly done)
`AiDoubles` = `ChooseMoveOrAction_Doubles` + `ChooseTarget_Doubles` (ai_master.c:525-846 and the static helpers :412-523): each move is scored against every living target
with the singles scripts (`AiMaster.scoreMoves`, `AiCtx.user/target/foeOf`), then the target is chosen with the KO / most-damage / dangerous-foe rules. `Battle.chooseFor`
uses it for trainer (and rank>=2 boss) foes in double battles and registers the target (`choices[3]`) for single-target moves.
登记: `ai_partner.c` (an ally target is always -1), Z-moves, doubles fighting classes (read from the singles class), `CanKnockOutWithFasterMove` approximated,
doubles branches inside AiNegatives/AiPositives, doubles switching and item use (still singles only).

## Batch 7 (done): side-effect hits and the common utility moves
- Damaging moves that share a status function code in Essentials (Body Slam = 007, Flamethrower = 00A, Thunderbolt, Psychic's stat drop, ...) are CFRU's `EFFECT_*_HIT`:
  Negatives treat them as plain damage (ai_negatives.c:3289; before this they were wrongly penalised like the status move), Positives only run the status checks when the
  side-effect chance is >= 75% (>= 50% for stat drops on a non-damager) and the target has no Substitute (`AiCalc.isSideEffectHit`, `secondaryEffectChance`).
- `AiPositiveMore`: Explosion, Reflect/Light Screen/Aurora Veil (`ShouldSetUpScreens`), Substitute (`ShouldUseSubstitute`), Protect family + Endure (`ShouldProtect`),
  U-Turn/Volt Switch/Parting Shot (`ShouldPivot` on top of the bench scorer, `IncreasePivotViability`), Knock Off.
  登记: team protections, Aegislash, Wish after a pivot, `RecalcStrongestMoveIgnoringMove`, Mimic/Disable/Encore/Spite/Thief/Trick/Psych Up/Wish/Heal Bell/Skill Swap family.

## Batch 8 (done): used-move history
The engine already keeps `Battler.movesUsed` / `lastMoveUsed` / `lastMoveUsedType` and `Battle.lastMoveUsed`, which are CFRU's `BATTLE_HISTORY->usedMoves`, `gLastUsedMoves[]`,
`gLastUsedTypes` and `gNewBS->LastUsedMove` (reset on switch-in like the battle history). `AiCalc.hasUsedMove / hasUsedStatusFunction / hasUsedHitFunction / lastUsedMove / globalLastUsedMove`
read them. Now using them: `BadIdeaToRaise<Stat>Against` (stat-lowering moves the foe has shown, `HasUsedPhazingMoveThatAffects`), Powder/Ion Deluge (Negatives preamble),
the screen and hazard "player will cheese it" -9 checks (Brick Break/Defog/Rapid Spin used), and `AiNegativeHistory` + `AiPositiveMore` for Copycat, Mimic, Disable, Encore, Spite,
Conversion 2, Sketch. 登记: banned-move flags (approximated by function code), `CanLastMoveNotBeEncored`, Mirror Move (`lastTakenMoveFrom`), Counter's `previousMovePredictions` branch,
Imprison clauses, `NoUsableHazardsInMoveset`.

## Batch 9 (done): items, abilities and whole-party cases
`AiAbilityRatings` = `gAbilityRatings[]` (ability_battle_effects.c:42, 263 of 264 entries; PORTALPOWER is not in this project). `AiPositiveItems`: Wish / Heal Bell (`ShouldUseWishAromatherapy`),
Thief/Covet, Trick/Bestow, Skill Swap / Simple Beam / Worry Seed / Gastro Acid / Entrainment, Power/Guard/Heart/Speed Swap, Power/Guard Split, Power Trick, Psych Up, Spectral Thief,
Imprison, Refresh, Mud/Water Sport, Trick Room, Magic/Wonder Room. Negatives for the ability-changing moves use the plugin's own failure rules (`PokeBattle_Move_063..068`) instead of
CFRU's `gSpecialAbilityFlags`, because those decide whether the move really works here.
Still open in Positives: Utility Umbrella / Eject Button / Assault Vest branches of Trick, Role Play ("To do" in CFRU), Psycho Shift status hand-over, Fling, Feint, Embargo/Powder/Throat Chop/Heal Block branches,
Soak/Trick-or-Treat, Topsy-Turvy/Electrify, Fairy Lock, Tailwind, Lucky Chant, Magnet Rise, Camouflage, Secret Power, Smack Down, Bug Bite/Incinerate, Clear Smog-type effects, Gravity, Ion Deluge, Court Change, Defog/Rapid Spin.

## Not yet transcribed (explicit, in order of value)
1. `AIScript_Negatives`: the cases listed as 登记 in `AiNegativeEffects` (Haze/Psych Up GOOD_AI branch, Bide, Roar, Conversion, Knock Off, Skill Swap family, Fling, Instruct, Court Change, Spite/Mimic/Disable/Encore/Sketch which need last-used-move history, Max-move/partner checks) and the ability cases of the preamble (ai_negatives.c:222-324).
2. `AIScript_Positives` per-effect `switch` from ai_positives.c:1760 on, plus Protect (`ShouldProtect`), screens (`ShouldSetUpScreens`), pivots (`ShouldPivot`), Substitute, Taunt, Trick, the secondary-effect HIT cases, Explosion, Mean Look/Trap (`ShouldTrap`), Heal Bell/Wish and the 登记 cases listed in `AiPositiveEffects`.
3. Target-ability cases needing `gStatLoweringMoveEffects` / `gSetStatusMoveEffects` (ai_negatives.c:222-324).
4. `BadIdeaToMakeContactWith`, `BetterToKOLastFoeMon`, `HasUsedMove` history, `usingDesperateMove`, `NoUsableHazardsInMoveset`,
   Focus Sash / Sturdy damage clamps, Parental Bond, `BracketCalc`, critical-hit chance in `AI_CalcDmg`.
5. Doubles (`ChooseMoveOrAction_Doubles`, ai_partner.c) and the doubles half of switching.

## Function-code mappings used (Essentials numbering; not checked against this project's moves.json)
Counter/Mirror Coat/Metal Burst 071-073, Future Sight 111, Explosion 0E0, Recharge 0C2, charge turn 0C3-0CE, OHKO 070, fixed damage 06A-06F,
Protect 0AA/149/14A/168, Roar 0EB, Haze 051, Wish 0D7, Heal Bell 019, Sucker Punch 116, Rapid Spin 110, Defog 049, Pursuit 088, Heart Swap 054, U-Turn/Volt Switch 0EE, Reflect 0A2, Light Screen 0A3, Leech Seed 0DC, healing 0D5/0D6/0D8/114,
Spikes 103-105/153, stat-boost status 01C-03B, Sleep Talk/Snore 0B4/011, Lock-On 0A6. Verify against the generated `moves.json` before relying on them.


## Classification of the untranscribed items (rule: nothing below is silently dropped)
- **Not applicable to this project (will never be ported, ignore):** Z-moves / Z-crystals, Dynamax / Max moves / raid battles, Camomons, Hoopa SOS, FROSTBITE, Steelsurge, Rainbow side effect.
- **Deferred, source is in hand (doable):** `ai_partner.c`, the doubles branches inside the scripts, doubles switching / item lists, `PassOnWish`, `CanStopLockedMove`, `ShouldSwitchIfWonderGuard`,
  Wish / Heal Bell (`ShouldUseWishAromatherapy`), Counter's `previousMovePredictions` branch (the AI must remember last turn's prediction), Mirror Move (`lastTakenMoveFrom`, needs an engine record), Imprison, `NoUsableHazardsInMoveset`.
- **Deferred, needs a source file or table not fetched yet:** `ShouldSwitchIfPerishSong` body, the `switchingCooldown` setter, `gAbilityRatings` (Skill Swap family), `gCopycatBannedMoves` / `gMimicBannedMoves`, `CanLastMoveNotBeEncored`, Trick / Thief item rules (`CanTransferItem`).
- **Item effect handlers:** fully transcribed from `190_PItem_BattleItemEffects.rb` and its helpers in `188_PItem_Items.rb` (nothing inferred any more).
- **Written but simplified (approximation, listed at each site):** doubles fighting classes, `CanKnockOutWithFasterMove`, the 2-hit `CanKnockOutAfterHealing`, Sheer Force / flinch tables in `CalcSecondaryEffectChance`.

## Real-PBS conformance (`AiRealPbsTest`, reads `plugin-src/pbs`)
Checked against the exported PBS: all 239 function codes the AI uses map to moves of the expected kind (no orphan code), the flag letters (a contact, b protect, c magic coat, g thaw, k sound, l powder, n bomb)
match, and every ability / item / move name the scorer compares against exists - except `ASONE` (fixed to `ASONEGHOST`; `ASONEICE` is not a Moxie-type). The test keeps this from drifting.
Real trainer item lists are only FULLRESTORE, MAXPOTION, BURNHEAL, POTION, SUPERPOTION, HYPERPOTION, MAXREVIVE and vitamins (PROTEIN/CARBOS/CALCIUM, ignored by the AI).

## moves.json conformance pass (done)
Against the real `plugin-src/pbs/moves.json` (1010 moves): see `AiRealPbsTest`.
- Every function code the AI uses exists and belongs to the expected move (70 hand-checked pairs).
- Shared codes: Essentials gives a status move and its damaging "hit" variants the same code. `AiCalc.SIDE_EFFECT_CODES` now also covers the self-raising hits (01C Power-Up Punch / Meteor Mash, 01D Steel Wing,
  01F Flame Charge, 020 Charge Beam, 022, 179), sleep hits (003 Relic Song), and the plain-damage trap / Foresight hits (0EF Spirit Shackle..., 0A7 Target Beam): Negatives treat them as plain damage,
  Positives only run the status case when the side-effect chance is >= 75%.
- `move.target()` strings: the Prankster exemption compared against "OpposingSide", the data says "FoeSide" - fixed.
- Accuracy 0 = never misses (handled), power 0 never appears on a damaging category, flag letters match.
- Every move is scored (Negatives, Positives, full pick) against two board states without an exception. This found two engine gaps, fixed: Trump Card read an unwired `@pp`
  (now the PP of the slot used) and Belch an unwired `belched?` (now `Battle.belch` per side/party index as in Ruby; `pbConsumeItem` no longer sets Unburden as a marker).

## Move-effect stubs ("M0 待接线") wired
Reachability was checked by using every real move in 1v1/2v2 (`MoveSmokeRealPbsTest`) and by reading each stub's call sites. Wired now:
- Battler stat values (`baseAttack/Defense/SpAtk/SpDef/Speed` + setters, reset on switch-in): Power Trick, Power Split, Guard Split, Speed Swap, Power Shift, Arceus' raw stats, `pbGetAttackStats/DefenseStats`.
  (The old comment claiming the plugin has no `defense=`/`spdef=` writer was wrong: 109_PokeBattle_Battler.rb:74/88 has `attr_writer`.)
- Mimic (temporary, restored on switch-out and at battle end) and Sketch (permanent) write the move slot; Keldeo's `pbCheckFormOnMovesetChange`.
- `addSideStatUps` / `pbMirrorStatUpsOpposing` (Opportunist, Mirror Herb), `Pokemon.statusCount`, Belch (`Battle.belch`), Trump Card PP.
- Spite, Instruct, Hold-style PP reads: PP and `totalpp` come from the owner's move slot (`Battler.moveTotalPp`, new). Before, `BattleMove.totalpp()` was 0, so Spite/Instruct/Cursed Body always failed.
- Rapid Spin message (`PBMoves_getName`), Aura Wheel's species check, Ally Switch (`pbSwapBattlers`, `@battlers`, owner index), Evolution Ray (`pbGetEvolvedFormData`), Eviolite.
Deliberately left as they are (the Ruby crashes there too, or nothing calls them): `target.pbWeather` (BattleHandlers_Abilities:4581), `pbCanConfuse?/pbCanSleep?(…,self)` (Arceus:3884/3896),
Shadow Pokemon `i.hp` (:593), the "no base default" hooks of the generic move classes, `PBItems_getName`, `pbCalcDamageMultipliers` (not used by `DamageCalc`).
One deviation: `pbItemOpposingStatGainCheck` no longer throws when Mirror Herb triggers (Ruby raises NoMethodError at Battler_AbilityAndItem:338); now that the stat-up tally is wired the line is reachable, so the item just does not trigger.

## 插件缺陷修复（用户裁决：插件本身没写好的地方按明显意图修）

已修（代码里以「插件缺陷已修」标注）：
- Safeguard 的 `user` 未定义（Battler_Statuses:239-243）→ 同步场景下用参数 `target` 作施加方，Infiltrator/Translucent Ghost 无视。
- `pbWeather`（Battler 上调用，RAINBOWARCH）→ `battle.pbWeather()`；Arceus 的 `pbCanConfuse/pbCanSleep` 第三参误传 proc → 按无招式处理。
- `reduceHalfHp`（`i.hp/2` 的 `i` 未定义）→ 自身 HP；Cud Chew 的 `item_to_use` → `thisItem`；Mirror Herb 的 `forcedItem` 误传布尔 → 正常消耗道具。
- Ripen 树果 `pbRecoverHP?` 拼写 → `pbRecoverHP(maxHp/2)`。
- Crafty Shield（SuccessChecks:404）`!move.function == "18E"` → `move.function != "18E"`（块之前永不执行）。
- Sea of Fire 回合末 `@battle.pbCommonAnimation`（`@battle` 为 nil）→ `battle.commonAnimation`。
- 四处 `modifiers[EVA_STAGE] = 0`（Chip Away 类 0A9、1CC 附近两处、1BD 雨天）：变量名未定义且钩子无调用点 → 改落在实际被调用的 `pbCalcAccuracyModifiers`，无视对方闪避等级（1BD 仅雨天）。
- Red Card：`Battle::Scene::USE_ABILITY_SPLASH`（NameError）→ `PokeBattle_SceneConstants`；`user.dynamax?` 本工程无 Dynamax，恒 false；补全 pbRecallAndReplace(随机替换)。
- Fling 技能机 `movedata` 为 nil → 威力 10。
- 修正误记的「无 defense=/spdef= 写入器」注释（实际有 attr_writer）。

未改（意图不明确）：
- CRAFTMIND 的形参错位（proc 本身只有 `next 值`，无从判断本意）。
- `DamageCalcTargetAbility.copy(MOLDBREAKER/PRESSURE/SUPREMEOVERLORD…)`：源条目从未注册，无内容可复制。
- BattleAnimations 的 nil 精灵守卫（不可达）。

## Batch 10: doubles infrastructure and ai_partner.c

Done (tests: `AiPartnerTest`):
- **Doubles fight classes** (`ai_advanced.c:586-775`, `AiCalc.fightingStyle` → `doublesFightingStyle`) used whenever the battle is not a 1v1 (`AiDoublesScore.isDouble`); `classDamager`/`classDoublesAttacker`/… predicates.
- **Doubles killing score** (`ai_util.c:796-1130` `UpdateBestDoubleKillingMoveScore`, `GetDoubleKillingScore`) and `gDoublesDamageViabilityMapping`; the doubles branch of `DamageMoveViabilityIncrease` (`ai_positives.c:2870`) now uses it instead of the singles scoring.
- Doubles cases of `IncreaseStatusViability`, `IncreaseStatViability`, `IncreaseSleepViability`, `IncreaseEntryHazardsViability`, `IncreaseFakeOutViability`, `IncreasePivotViability`, `IncreaseSubstituteViability`, `IncreaseFoeProtectionViability`; new `IncreaseHelpingHand/HealPartner/PsychUp/AllyProtection/TeamProtection/Tailwind/SpeedControl` (the last four are not called yet, see below). `ShouldUseFakeOut` doubles branch.
- **`AIScript_Partner`** (`ai_partner.c`, whole file) in `AiPartner`; `AiPositives.score` hands over when the target is the partner; `AiDoubles.choose` no longer forces the ally's score to −1.
- Negatives: `TARGETING_PARTNER` exemptions and the target-Ability cases that were 登记 before (Justified, Rattled, Steam Engine, Aroma/Sweet/Flower Veil, Contrary, Mirror Armor, Clear Body family, Hyper Cutter, Keen Eye, Big Pecks, Defiant, Competitive, Comatose, Shields Down, Leaf Guard).
- Bug fix found on the way: the AI compared statuses with `"FREEZE"` (never matches); the project's id is `"FROZEN"`.
- Wild Bosses (rank ≥ 2) in 3v1 go through `AiDoubles` (the boss fight is a non-1v1 battle); the "next foe" fallback no longer assumes bank 1 is an opponent.

登记 (source not exported or no equivalent here): `DoesProtectionMoveBlockMove` (built from this engine's protection rules), `gStatLoweringMoveEffects` / `gSetStatusMoveEffects` / `gAromaVeilProtectedMoves` (move_tables.c: lists rebuilt from the real moves.json), `UnfreezingMoveInMoveset` (flag g), `CanKnockOffItem` (holds a transferable item), Ion Deluge's second foe (the C reads `foe1` twice), triples (CFRU has none: sums run over all living foes).

## Batch 11: doubles branches of Positives / Negatives, items

Done (tests: `AiPartnerTest`, `AiRealPbsTest` scores every move):
- Protect family in doubles: `ShouldProtect` doubles branch (PROTECT_FROM_ALLIES / PROTECT_FROM_FOES), Quick Guard / Wide Guard / Crafty Shield / Mat Block / King's Shield / Baneful Bunker / Endure in `AiPositiveMore.protect`.
- Positives: weather moves (+ the speed-ability → Tailwind rule), Will-O-Wisp / poison / paralysis (with `DoubleDamageWithStatusMove…`), Haze / Roar / Clear Smog classes, drain / recover class bonuses, screens, Mist / Safeguard / Lucky Chant, Rapid Spin / Defog, pivot moves, Fake Out, Taunt, Follow Me, Trick Room, Tailwind, Gravity, Ion Deluge, Court Change, Powder, Telekinesis, Throat Chop, Heal Block, Embargo, Soak, Topsy-Turvy, Electrify, Fairy Lock, terrain, Pledge, Quash, Magnet Rise, Flame Burst, Sky Drop, Bug Bite / Incinerate / Smack Down, Feint, multi-hit moves, Fell Stinger (new `AiPositiveField`).
- Negatives: partner-aware avoidance (`AiNegativeDoubles`: same effect / same target / partner weather / terrain / Trick Room-Tailwind), partner-aware ability checks, Wide Guard, Protect, Spikes, Perish Song, Defog, Helping Hand / Follow Me, Explosion, Haze, Howl / Aromatic Mist / Rototiller / Gear Up / Magnetic Flux.
- `ShouldSetUpScreens` considers both foes. Trainers use items in doubles too (`Battle.chooseFor`).

登记: Fling (`gFlingTable` not exported), Camouflage (its C condition can never hold), Z-moves / Dynamax / Max-move variants, Foresight / Miracle Eye partner clause, Pledge combo failure clause, Perish Song partner "same target", `gDoubleDamageOnStatus` / `gAromaVeilProtectedMoves` / `gStatLoweringMoveEffects` / `gSetStatusMoveEffects` (rebuilt from moves.json).

## Batch 12: doubles switching

Done (tests: `AiPartnerTest`): `ai_switching.c` doubles branches - the bench scorer over both foes (`CalcMostSuitableMonToSwitchInto`: type-matchup defence for two foes, faint ⇒ weak-to-move, score cap ×2, hazard remover ×2, thresholds of 3), `FindMonThatAbsorbsOpponentsMove`, `ShouldSwitchIfOnlyBadMovesLeft`, Natural Cure / Regenerator, `ShouldSwitchWhenYawned`, `ShouldSwitchWhileAsleep`, `IsTakingAnnoyingSecondaryDamage`, `ShouldSwitchWhenOffensiveStatsAreLow`; single-only checks (`ShouldSwitchToAvoidDeath`, `ShouldSaveSweeperForLater`, Wonder Guard) are skipped in doubles as in C; also the new singles checks `PassOnWish` and `ShouldSwitchIfWonderGuard`.
Engine fix found on the way: `pbCanSwitchLax?` only rejected the battler's own Pokemon; it now rejects any Pokemon on the field for the side, and the AI skips a Pokemon the partner has already chosen to switch to.
`Battle.chooseFor` / `defaultChooseNewEnemy` now use the switching scorer in double battles.

登记 (see the `AiSwitching` header): `CanStopLockedMove`, `SemiInvulnerableTroll` (no effect in the source), `ShouldSwitchIfPerishSong` (body not in the cached source), pivot hand-off, Disguise / Dynamax / Imposter / Steelsurge / Wish recovery on the incoming Pokemon, two trainers on one side.

## Batch 13 (gap pass after cloning the CFRU repo)
- Real CFRU tables replaced reconstructed ones (setsStatus/confuses, DoesProtectionMoveBlockMove, stat-lowering set).
- Fling (0F7), Perish Song switching, real switchingCooldown counter.
- CFRU defect fixed: `RunAllSemiInvulnerableLockedMoveCalcs`/`CanStopLockedMove` returned FALSE after emitting the switch; port returns the chosen party index.
- Wish recovery on the incoming mon (`GetWishHPRecovery`, battle_util.c:1241) now factored into bench scoring.
- Pivot hand-off done: `FastPivotingMoveInMovesetThatAffects` (0EE/151), `ConfirmAISwitch(.., willPivot)` records `Battle.aiPivotTo`/`aiGoodToPivot` (cleared at end of round / when the foe switches), `ShouldPivot` returns PIVOT_IMMEDIATELY (+9), `AiSwitching.replacement` honours the recorded target.
- Still 登记: Trace/Imposter/Disguise on the incoming mon, Wonder Guard weather/Trick-orb clauses, Counter/Mirror Coat `previousMovePredictions`. N/A: Z-moves, Dynamax, Steelsurge.

## Plan tail (added)
- `pbIsUnlosableItem?` (188_PItem_Items.rb:172) was never ported; now `ItemsUnlosable` (all 140 species rows) is wired into `Battler.unlosableItem`. Project addition: REGIGIGAS/REGISPELL and SAMUROTT/CRAFTMIND (all forms) are unlosable (the original plugin omitted them).
- TODO (AI): big score bonus for Knock Off (0F0) / Thief, Covet (0F1) / Trick, Switcheroo (0F2) / Bug Bite, Pluck (0F4) / Corrosive Gas (201) when the target is a player-owned Pokemon that only obeys while holding its item (117_Battler_UseMove_SuccessChecks.rb:136-160): SEAMONSTER+EOSINORB, GROUNDMONSTER+ULTRAMARINEORB, SKYMONSTER+BLACKGREENORB, HAXORUS form 2+TYRANTCREST. Also score Thief/Covet/Trick (currently 登记).
- TODO (AI): original moves (ids 1231-1232, 1300+) and abilities (453-480) have no dedicated AI scoring; damage is handled by the generic calc.

## Batch 14 (gap clean-up)
Done: obedience-item bonus for Knock Off / Thief / Trick / Corrosive Gas (`AiPositives.stripObedienceItem`, +40); Wonder Guard weather and Trick-orb clauses; `NoUsableHazardsInMoveset`
(CFRU defect: returns FALSE on every path, fixed); Knock Off (`CanKnockOffItem`) in `CalcOnlyBadMovesLeft` and the Knock Off / Corrosive Gas negatives; Skill Swap family negatives (engine failure tests
stand in for the ability ban tables, messages discarded); Offensive-set-up `*_HIT` moves; Flash Fire / Unburden in `AnyUsefulOffensiveStatIsRaised`; `previousMovePredictions` (Counter / Mirror Coat,
faulty-prediction switch rule); Disguise / Flame Veil / Ice Face on the incoming Pokemon (`AiCalc.disguiseDamage`); `BracketCalc` for the deterministic sources (Stall, Mycelium Might, Custap Berry,
Lagging Tail, Full Incense). Trace / Imposter on the incoming Pokemon: not in the CFRU source, nothing to transcribe.
Still open (small): Imprison clauses, the Utility Umbrella / Eject Button / Assault Vest branches of Trick, Role Play, Psycho Shift status hand-over, Sea of Fire / Bad Dreams in
`WillFaintFromSecondaryDamage`, Sheer Force table, confusion in `HighChanceOfBeingImmobilized`, multi-turn lock-in prediction shortcut, Quick Draw / Quick Claw rolls (RNG).
N/A: Z-moves, Dynamax, Steelsurge.
