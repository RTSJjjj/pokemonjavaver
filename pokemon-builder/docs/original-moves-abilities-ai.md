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

## Needs your decision / source (reported, not guessed)
Move data vs. effect class mismatches (the effect class that actually runs is the one in the function column):
| move | function | what the class does | description says |
|---|---|---|---|
| LIGHTABSORPTION 光能吸收 | 1C9 | doubles power if an ally fainted (power 0 status -> nothing happens) | heal half HP |
| MURKYMIST 晦暗之雾 | 1D6 | Grave-digger power +50/fainted ally (status, power 0 -> nothing) | smoke on our side |
| VEIL 纱幕 | 1CC | Burning Bulwark (protect + burn on contact) | protect, lower accuracy vs special |
| BONEWIND 蚀骨风 | 04E | Captivate (Sp. Atk -2 vs opposite gender) | lower Def and Sp. Def |
| ECSTASYPALM 销魂掌 | 043 | Defense -1 chance | flinch chance |
| SOULCRUSH 命断魂破 | 150 | Fell Stinger (+Atk on KO) | recoil |
| NIGHTUNENDING 永夜未央 | 210 | frostbite + bonus damage | lowers Attack |
| SOULREND 通幽绝杀 | 211 | burn + bonus damage | double damage if statused |
| ABYSSSHADOW 深渊暗影 | 1B2 | SUJINRAKU only | generic |
Please confirm whether the moves.json function codes or the descriptions are right; the AI follows the function code.

Abilities that are in `abilities.json` but have **no script at all** in `103_BattleHandlers_Abilities.rb` / the Java engine (they do nothing in battle, so the AI
cannot classify them): **COMBATMACHINE 战斗机器, ANGRYBODY 狂暴身躯** (no hook, no inline check). If their code lives in a file I do not have, please upload it.
Also with only inline (non-hook) logic: UNSEENFIST, ARMORTAIL, GUARDDOG, LINGERINGAROMA, MINDSEYE, *OFRUIN, SAVAGECEREMONY, NOBLESTRIKE, FLAMEVEIL, SHATTERFIST,
FEARLESS, CALAMITYAERIAL, SPOVERLORD, DRAGONSOULCRY, CLEARHEART, TRANSLUCENTGHOST, LIBERO.

## Not classified yet (no CFRU category to attach them to; AI treats them as 0-rated, like an unlisted ability in `gAbilityRatings`)
Weather/terrain setters (BESTOWEDRAIN, STORMEYE, RAINBOWARCH, ORICHALCUMPULSE, ETERNALFLAME, CALAMITY*, BUGLURESURGE, COLDSURGE, ROSE*, HADRONENGINE...),
priority changers (TRAPTRICK, FAIRYDANCE, SOUNDSTRIDE, ARMORTAIL, QUICKDRAW), reborn abilities (SACREDREBORN, ABYSSREBORN), trapping (DSOVERLORD, CONFESSIONLIST),
status immunity (PLAYFULHEART, RAINCURTAIN, PURIFYINGSALT), damage-multiplier abilities. CFRU's AI has per-ability code only for the ~130 abilities listed in
`AiPositiveHelpers`/`AiNegatives`/`AiSwitching`; the rest relies on the engine's real damage calc, which already includes these abilities' multipliers.
Say which of these groups you want mapped (each needs a decision on what the AI should *do* with it, e.g. "treat BESTOWEDRAIN like DRIZZLE for partner weather checks").
