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
Every `HasUsedMove*` history test is treated as false (no used-move history).

## Not yet transcribed (explicit, in order of value)
1. `AIScript_Negatives`: the cases listed as 登记 in `AiNegativeEffects` (Haze/Psych Up GOOD_AI branch, Bide, Roar, Conversion, Knock Off, Skill Swap family, Fling, Instruct, Court Change, Spite/Mimic/Disable/Encore/Sketch which need last-used-move history, Max-move/partner checks) and the ability cases of the preamble (ai_negatives.c:222-324).
2. `AIScript_Positives` per-effect `switch` from ai_positives.c:1010 on (Protect, hazards, weather, screens, Baton Pass/pivots, Taunt, Trick, Substitute, secondary-effect HIT cases, Explosion, ...) and the 登记 cases listed in `AiPositiveEffects`.
3. Target-ability cases needing `gStatLoweringMoveEffects` / `gSetStatusMoveEffects` (ai_negatives.c:222-324).
4. `BadIdeaToMakeContactWith`, `BetterToKOLastFoeMon`, `HasUsedMove` history, `usingDesperateMove`, `NoUsableHazardsInMoveset`,
   Focus Sash / Sturdy damage clamps, Parental Bond, `BracketCalc`, critical-hit chance in `AI_CalcDmg`.
5. The signature-move bonus (+6, `AiSignature`), switching (ai_switching.c), item use, doubles (`ChooseMoveOrAction_Doubles`, ai_partner.c).

## Function-code mappings used (Essentials numbering; not checked against this project's moves.json)
Counter/Mirror Coat/Metal Burst 071-073, Future Sight 111, Explosion 0E0, Recharge 0C2, charge turn 0C3-0CE, OHKO 070, fixed damage 06A-06F,
Protect 0AA/149/14A/168, Roar 0EB, Haze 051, Wish 0D7, Heal Bell 019, Reflect 0A2, Light Screen 0A3, Leech Seed 0DC, healing 0D5/0D6/0D8/114,
Spikes 103-105/153, stat-boost status 01C-03B, Sleep Talk/Snore 0B4/011, Lock-On 0A6. Verify against the generated `moves.json` before relying on them.
