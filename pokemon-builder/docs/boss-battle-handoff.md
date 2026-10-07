# Boss battles (Boss 三打) - handoff

Branch `battle-engine-rewrite`. Sources: `plugin-src/ruby/319_Boss_Battles.rb`, `320_PokeBattle_BOSS.rb`, `321_Boss_reward.rb`
and the sections cited in the code comments. A boss fight is a wild battle `Nv1` (N = min(able party, 3)), so the
triple-battle engine (commit b135ea0) carries it; this work adds the boss layer on top.

## Done (each with tests; core suite: 834 tests, only the 2 `SoundLengthTest` audio-asset failures remain, they fail on a clean checkout too)
| Plugin | Java |
| --- | --- |
| Battler_Initialize:44-45/78-79/366-367 `BOSS_HP_RANK[battleRank]` scales `@totalhp`/`@hp` | `Battler.maxHp()` / constructor |
| PokeBattle_BOSS:157-187 `pbCatchBossPokemon` (confirm, Bag as ball chooser, delete ball, `resetMoves`, `hp=1`, throw with rareness 255, decision 4 / 1) | `Battler.pbCatchBossPokemon`, `Battle.Scene.pbChooseBallFromBag` + `SceneCall.Kind.CHOOSE_BALL`, `BattleScreen` |
| PokeBattle_BattleCommon:68-164 `pbThrowPokeBall` in the engine (the port's `ball()` is the player-command variant) | `Battle.pbThrowPokeBall`, `Battle.CaptureHooks` (implemented by `InteractiveBattlePort.BallHooks`), `pbRemoveFromParty`, `Battler.pbReset`, `Battler.removedFromParty` |
| PokeBattle_Pokemon:448-464 `resetMoves` | `Pokemon.resetMoves` |
| PField_Battles:38/105 + PokeBattle_Battle:68/145 + Battle_ExpAndMoveLearning:8 `noexp` (was recorded but never reached the battle) | `BattlePort.setExpGain`, `Battle.expGain` |
| PokeBattle_Pokemon:466-495 `pbLearnMove` forgets the first move when full (the runtime skipped it) | `EventInterpreter.learnMove` |
| Boss_Battles:5-4367 the 177 `def battleXxx` | `tools/boss-battles/generate.mjs` -> `event/BossBattleData.java` (generated, strict parser: throws on any statement it does not know), run by `EventInterpreter.startBossBattleCondition` as a code-111 script condition (`return decision==N` is the branch) |
| 319:2725-2726 `pkmn.totalhp = pkmn.totalhp * 7` | `Pokemon.totalHpFactor` (reset by the `calcStats` op, like `@totalhp` is rewritten) |

Already present before this batch (checked against every `battleRank` site in the plugin): pbBossBuffPhase, EOR/hit damage
fractions for rank > 2, Mega rules, start-of-battle lines, switch-in prompt, data box `×N`, no PP use for rank < 2.

## Not done / needs a decision
1. **`battleBoss(species,level,rank)` (319:5-31)** calls `changeEVandNature` and `pbRandomIV`; neither is in any exported section
   (also needed by Battle_StartAndEnd:168). Export those sections, then extend the generator. The game's events only call
   `battleIronJugulis` and `battleOverlordflos` (docs/plugin-usage.md), both transcribed.
2. **Script-statement form.** Only the code-111 *condition* form (`if battleXxx`) is wired. A `battleXxx` as a plain 355 script
   statement would need the builder's script-compiler to emit an IR command; the game's event data is not in this repo to check which form is used.
3. **Wild-boss AI** (AI_Move:9 `skill = 20 * battleRank`, :20 `battleRank < 2` branch): bosses with rank >= 2 still use the old
   fallback `defaultAi`. The plugin's real scorer is AI_Move_EffectScores (3,883 lines) - superseded by TASK 2 (CFRU AI port).
4. `pbSEPlay("Battle flee")` (PokeBattle_BOSS:160/178/183) is registered, not played: the engine has no SE channel
   (same note as `BattleSwitchAction`'s header).
5. `pbStorePokemon`'s box messages / Pokedex page after an engine capture are not modelled (the Pokemon joins the party or box via `addToParty`).
6. Not verified on screen: the Bag-as-ball-chooser (`BattleScreen` `CHOOSE_BALL`) was written against the existing `BagView.chooseItem` but has
   no screenshot/probe run (needs the generated PBS, absent in the cloud).

## Cloud build note
`google()` is not reachable from the cloud sandbox, so the root `build.gradle` cannot resolve the Android Gradle Plugin.
The local-only workaround used here (not committed, `git update-index --skip-worktree runtime/build.gradle`): comment out
the `id 'com.android.application' ...` line in the `plugins {}` block. Run with `sh ./gradlew :core:test` (the wrapper is not executable).
