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

## Not yet transcribed (explicit, in order of value)
1. `AIScript_Negatives` per-effect `switch (moveEffect)` (ai_negatives.c:664-3244) - until then a status move is never penalised.
2. `AIScript_Positives` per-effect `switch` (ai_positives.c:55-2722): set-up moves, status, hazards, healing, Protect...
3. Target-ability cases needing `gStatLoweringMoveEffects` / `gSetStatusMoveEffects` (ai_negatives.c:222-324).
4. `BadIdeaToMakeContactWith`, `BetterToKOLastFoeMon`, `HasUsedMove` history, `usingDesperateMove`, `NoUsableHazardsInMoveset`,
   Focus Sash / Sturdy damage clamps, Parental Bond, `BracketCalc`, critical-hit chance in `AI_CalcDmg`.
5. The signature-move bonus (+6, `AiSignature`), switching (ai_switching.c), item use, doubles (`ChooseMoveOrAction_Doubles`, ai_partner.c).

## Function-code mappings used (Essentials numbering; not checked against this project's moves.json)
Counter/Mirror Coat/Metal Burst 071-073, Future Sight 111, Explosion 0E0, Recharge 0C2, charge turn 0C3-0CE, OHKO 070, fixed damage 06A-06F,
Protect 0AA/149/14A/168, Roar 0EB, Haze 051, Wish 0D7, Heal Bell 019, Reflect 0A2, Light Screen 0A3, Leech Seed 0DC, healing 0D5/0D6/0D8/114,
Spikes 103-105/153, stat-boost status 01C-03B, Sleep Talk/Snore 0D1/0D2, Lock-On 0A6. Verify against the generated `moves.json` before relying on them.
