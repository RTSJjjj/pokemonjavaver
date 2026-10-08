# CFRU trainer AI — final handoff

Branch `battle-engine-rewrite` (module `runtime/core`, package `pokemon.runtime.battle`). Full tests: `cd pokemon-builder/runtime && bash ./gradlew --offline :core:test -q`
— 902 tests, 2 known failures (`SoundLengthTest`, missing generated audio; unrelated).

## Rules of the port
Transcribe faithfully with `source:line` citations; never invent behaviour; `登记` marks anything not transcribed or deliberately different; small commits; no Z-moves / Dynamax /
Steelsurge (the project has none). Commit trailer: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>` + `Claude-Session: <url>`. No PR unless asked.
The CFRU source (needed to check any transcription) is `https://github.com/Skeli789/Complete-Fire-Red-Upgrade`; `git clone --depth 1` works through the proxy.

## What exists (read these files, in this order)
| Area | Files |
|---|---|
| Entry / scoring loop | `AiMaster`, `AiCtx`, `AiBattle`, `AiDoubles` (flow), `AiNegatives*`, `AiPositives*` |
| Doubles | `AiDoublesScore`, `AiPartner` (ai_partner.c), `AiNegativeDoubles`, `AiPositiveField` |
| Switching / items | `AiSwitching` (ai_switching.c: ShouldSwitch*, bench scoring, pivot hand-off, cooldown), `AiItems` |
| Tables / helpers | `AiCalc`, `AiUtil`, `AiAbilityRatings` |
| Tests | `AiPartnerTest` (synthetic PBS), `AiCfruTest`, `AiRealPbsTest`, `BattleTriplesTest` |
Fight classes, `Negatives -> Positives`, partner AI, bench scoring over two foes, `switchingCooldown`, pivot hand-off (`Battle.aiPivotTo` / `aiGoodToPivot`,
`PIVOT_IMMEDIATELY` = +9), previous-move predictions (`Battle.aiPredictionLog`) are all in. Smartest tier for every trainer; wild non-boss battles keep `BattleAi.handles`.

## CFRU defects fixed (kept as deliberate deviations)
- `CanStopLockedMove` / `SemiInvulnerableTroll`: the C emitted the switch and returned FALSE; the port returns the chosen Pokemon.
- `NoUsableHazardsInMoveset`: returned FALSE on every path; now TRUE when no hazard move is usable.
- `HighChanceOfBeingImmobilized`: threshold is `odds <= 50` as in the source (an earlier port used `< 75`).

## Project-specific rules (not in CFRU)
- Absorbing abilities Earth Eater / Ice Absorb (ICEBSORB) / Well-Baked Body, Good as Gold; Burning Bulwark (1CC) and the original Veil (221) join the protect family.
- Bonus +40 (`AiPositives.OBEDIENCE_ITEM_BONUS`) for Knock Off / Thief / Covet / Trick / Corrosive Gas against a player's Pokemon that only obeys with its item:
  SEAMONSTER+EOSINORB, GROUNDMONSTER+ULTRAMARINEORB, SKYMONSTER+BLACKGREENORB, HAXORUS form 2+TYRANTCREST (`AiCalc.obeysOnlyWithItem`). REGISPELL (Regigigas) and
  CRAFTMIND (Samurott) are unlosable (`ItemsUnlosable`, the port of `pbIsUnlosableItem?`, plus these two additions).
- New switch rule `AiSwitching.statusImmuneSwitch`: answer a foe's sleep/poison/paralysis/burn moves with a status-immune bench Pokemon (engine StatusImmunityAbility handlers).
- Original abilities mapped to CFRU categories (see `docs/original-moves-abilities-ai.md`): Clear Body / Keen Eye / Dazzling / Magic Bounce / Moxie / Mummy / Disguise / Protean-Huge Power
  ratings / trapping abilities / weather setters / reborn (= Disguise-like); Rapidash's Flame Explosion +5.
- Engine fixes made along the way: Translucent Ghost ignores protect, Fearless raises Attack from Intimidate, Calamity Aerial (Delta Stream + Speed -1 to everyone else),
  Combat Machine and Angry Body scripts (Java only, no Ruby), Sacred/Abyss Reborn revive, new move effects 220 (Bone Wind) and 221 (Veil), moves.json function codes of 7 original moves.

## Open items (all small or by decision)
- Left as they are by owner decision: MURKYMIST (1D6 does not match its description), ABYSSSHADOW (1B2, SUJINRAKU only), Role Play scoring.
- Not transcribed: Splinters / Bad Thoughts / G-Max residual damage (CFRU-only), multi-turn lock-in prediction shortcut (ai_master.c:1172), Quick Draw / Quick Claw rolls (they use the
  battle RNG), Evaporate (no such ability), Psycho Shift's Frostbite branch, Eject Button branch of Trick (needs Dynamax).
- Original abilities left to run only in the engine (no AI category): Space/Time-Lord style abilities, Ghastly Wail, Netherdrive, Shatter Fist, most terrain setters, priority changers
  beyond the engine hook, status-immune abilities other than the new switch rule.
- Pre-existing failing tests: `SoundLengthTest` x2 (needs `generated/audio/SE/001Cry.ogg`).
- Engine-wide 登记 lists live in `docs/cfru-ai-handoff.md` (batches 1-15) and `docs/original-moves-abilities-ai.md`.

## Opening message for the next session
> This is the pokemonjavaver Essentials -> Java/libGDX port, branch `battle-engine-rewrite`, module `pokemon-builder/runtime/core`. The CFRU trainer AI port (singles + doubles, switching,
> items, partner AI, pivot hand-off) is finished and pushed; read `pokemon-builder/docs/cfru-ai-final-handoff.md` first, then `cfru-ai-handoff.md` and
> `original-moves-abilities-ai.md` for details. Rules: transcribe the source with `file:line` citations, never invent behaviour, mark gaps with `登记`, small commits, push to origin,
> no PR unless asked, no Z-moves/Dynamax. Run the tests with `cd pokemon-builder/runtime && bash ./gradlew --offline :core:test -q` (2 known `SoundLengthTest` failures). Remaining work is
> only the open items listed in that document; ask me before picking any of them up.
