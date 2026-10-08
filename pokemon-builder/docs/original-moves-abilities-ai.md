# Original (project) moves and abilities vs. the CFRU AI port

Scope: the 40 original moves (ids 1231-1232, 1300+) and the abilities the CFRU AI does not know (129: Gen 9 + project originals;
the project originals are 236/259-312 are Gen 9, 400-480 mostly originals). Engine behaviour is the Java transcription of the plugin
(`BattleHandlers_Abilities.rb` etc.); the AI only has to know *categories*.

## What was classified into the existing AI flow (done)
- **Absorbing abilities** (same `MoveImmunityTargetAbility` hooks as Volt Absorb / Flash Fire): EARTHEATER (Ground, heal), ICEBSORB (Ice, heal),
  WELLBAKEDBODY (Fire, +2 Def) added to the target-ability -20 check (`AiNegatives`), the absorb-switch list (`AiSwitching`), `AiPositiveMore.absorbs`;
  EARTHEATER / ICEBSORB also count as "heal partner" abilities in `AiPartner`. GOODASGOLD (immune to status moves): -10 for status moves.
- **Protect family**: function 1CC (Burning Bulwark and the original **Veil**) is `PokeBattle_ProtectMove`; added to `AiCalc.PROTECT`, the Protect cases of
  `AiNegativeEffects` / `AiPositiveMore` / `AiPartner`.
- **Glow Dance (1C6)**: damage + Sp. Def +1 on the user -> side-effect-hit class with a one-stage Sp. Def raise (analogue of 033).
- **Abyss Shadow (1B2)** only works for SUJINRAKU (`pbMoveFailed`): the AI never selects it for anyone else.
- Moves using vanilla codes (013 confuse, 00F flinch, 043/046/047/04E stat drops, 0A9, 070 OHKO, 0EF, 0C0, 0CF, 074, 121, 147, 1B6 flinch...) already go through
  the existing code tables; their damage comes from the engine calc.

## Move function codes fixed in moves.json (user confirmed: descriptions are right)
| move | was | now | note |
|---|---|---|---|
| LIGHTABSORPTION | 1C9 | 0D5 (recover half) | |
| ECSTASYPALM | 043 | 00F (flinch) | effectChance 30 already |
| SOULCRUSH | 150 | 0FB (recoil 1/3) | ASSUMPTION: fraction not in the description; 0FB = Brave Bird (also 120 power) |
| NIGHTUNENDING | 210 | 042 (Attack -1) | effectChance 0 -> 100 (a 0 chance never fires) |
| SOULREND | 211 | 07F (Hex, x2 on status) | |
| BONEWIND | 04E | 220 (new: Def and Sp. Def -1) | new class `PokeBattle_Move_220`, a thin `TargetMultiStatDownMove` |
| VEIL | 1CC | 221 (new protect) | new class `PokeBattle_Move_221` + `PBEffects.Battler.VeilGuard`; blocks like Protect, a blocked special move lowers the attacker's accuracy by 1 (ASSUMPTION: 1 stage) |
Still open: **MURKYMIST** (1D6 is a damaging move; "our side wrapped in smoke, power rises with fainted allies" has no matching code - tell me the intended effect) and **ABYSSSHADOW** (1B2 is SUJINRAKU-only by design; kept).

## Abilities with no script: added
- COMBATMACHINE = Neuroforce (user x1.25 on super effective) + Filter (target x0.75), handler copies.
- ANGRYBODY = Fairy immunity + Attack +1 (Sap Sipper pattern).
(Java only; the Ruby plugin source was not edited.)

## Official Gen 8/9 abilities now classified into the AI
- Unseen Fist, Piercing Drill, Translucent Ghost (contact moves ignore the protect family): `AiPartner.ignoresProtect`; engine `unseenFist` now also covers Translucent Ghost.
- Translucent Ghost / Noble Strike / Eternal Star: Clear Body family in all stat-lowering checks (they share the engine's StatLossImmunity copy).
- Armor Tail: Dazzling family (priority moves). Clear Heart: Magic Bounce family. Mind's Eye: Keen Eye family (+ Scrappy for Ghost).
- Fearless and Dragon Soul Cry: Moxie family (`AiCalc.isMoxie`). Fearless is also a Guard Dog (Intimidate raises Attack): engine `pbLowerStatStageByAbility` and the Intimidate switch-in check.
- Guard Dog: unchanged in the AI (Intimidate/phaze immunity are engine side; the CFRU AI has no Intimidate or Suction Cups scoring to attach to).
- Swords/Tablets/Vessel/Beads of Ruin: permanent field stat drops, already in the engine damage calc; the AI uses that calc, so nothing to map.

## Not classified yet (for you)
LIBERO, SAVAGECEREMONY, RAPIDASH, SHATTERFIST, FLAMEVEIL, CALAMITYAERIAL, SPOVERLORD, LINGERINGAROMA, GHASTLYWAIL, NETHERDRIVE (script exists, no category decided);
weather/terrain setters (BESTOWEDRAIN, STORMEYE, RAINBOWARCH, ORICHALCUMPULSE, ETERNALFLAME, CALAMITY*, BUGLURESURGE, COLDSURGE, ROSE*, HADRONENGINE...), priority changers
(TRAPTRICK, FAIRYDANCE, SOUNDSTRIDE, QUICKDRAW), reborn (SACREDREBORN, ABYSSREBORN), trapping (DSOVERLORD, CONFESSIONLIST), status immunity (PLAYFULHEART, RAINCURTAIN, PURIFYINGSALT).
