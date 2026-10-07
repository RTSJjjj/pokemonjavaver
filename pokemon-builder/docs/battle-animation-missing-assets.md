# 战斗动画缺失素材记录

由 `generated/battle-animations/` 对照 `Graphics/Animations` 与 `Audio/SE/Anim` 得出(PkmnAnimations.rxdata 引用了、但工程里不存在的文件)。

处理方式照插件原行为,运行时没有造任何替代品:
- 缺图:`AnimatedBitmap`(AnimatedBitmap:249-252)退化为 32x32 透明位图,即不显示。
- 缺音效:Audio_Play / Game_System 的 se_play 对不存在的文件不播放,即静音。

## 缺失图片 (14)

- `PRAS- Assist.png` — #105 Move:ASSIST
- `PRAS- Darkness BG.png (BG/FG)` — #296 Move:FUTURESIGHT
- `PRAS- Extracessory BG.png (BG/FG)` — #629 Move:EXTRASENSORY
- `PRAS- Giga Impact Opp BG.png (BG/FG)` — #1021 OppMove:GIGAIMPACT、#1603 OppMove:HOLYCHARGETACKLE
- `PRAS- PsychicBG.png` — #158 Move:PSYCHIC
- `PRAS- Sinister Arrow Raid BG.png (BG/FG)` — #775 rawr of time xd
- `PRAS- Sketch and Lick.png (BG/FG)` — #126 OppMove:LICK
- `PRAS- White BG.png (BG/FG)` — #1016 Move:DOOMDUMMY、#1017 OppMove:DOOMDUMMY、#1038 Move:LUSTERPURGE、#1118 Move:FIERCEKILLING 等8个
- `PRAS-Dragon Dance BG.png (BG/FG)` — #313 Move:DRAGONDANCE
- `STRAT- GhostGallop.png` — #1399 Move:GHOSTGALLOP、#1400 OppMove:GHOSTGALLOP
- `STRAT- Magnetic Field.png` — #1402 Move:MAGNETICTERRAIN
- `STRAT- Magnetic Terrain BG.png (BG/FG)` — #1402 Move:MAGNETICTERRAIN
- `STRAT- PaleoDrain.png` — #1403 Move:PALEODRAIN
- `uwu.png (BG/FG)` — #1394 Move:FLOWERTRICK

## 缺失音效 (67)

- `Gen 9 - Syrup Bomb` — #1464 Move:SYRUPBOMB、#1465 OppMove:SYRUPBOMB
- `Overheat1.ogg` — #180 Move:OVERHEAT、#324 Move:OVERHEAT、#325 OppMove:OVERHEAT、#1425 Move:BITTERMALICE
- `PRSFX- Baton Pass.wav` — #821 OppMove:BATONPASS
- `PRSFX- Breakneck Blitz1.wav` — #1142 ZMove:BREAKNECKBLITZ
- `PRSFX- Breakneck Blitz2.wav` — #1142 ZMove:BREAKNECKBLITZ
- `PRSFX- Clangorous Soulblaze1.ogg` — #1617 Move:CLANGOROUSSOULBLAZE、#1627 OppMove:CLANGOROUSSOULBLAZE
- `PRSFX- Clangorous Soulblaze2.ogg` — #1617 Move:CLANGOROUSSOULBLAZE、#1627 OppMove:CLANGOROUSSOULBLAZE
- `PRSFX- Gaurdian of Alola1.ogg` — #1619 Move:GUARDIANOFALOLA
- `PRSFX- Gaurdian of Alola2.ogg` — #1619 Move:GUARDIANOFALOLA
- `PRSFX- Genesis Supernova1.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova10.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova11.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova12.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova2.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova3.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova4.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova5.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova6.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova7.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova8.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Genesis Supernova9.ogg` — #1618 Move:GENESISSUPERNOVA
- `PRSFX- Let's Snuggle Forever1.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever10.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever11.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever12.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever2.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever3.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever4.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever5.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever6.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever7.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever8.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- Let's Snuggle Forever9.ogg` — #1620 Move:LETSSNUGGLEFOREVER、#1628 OppMove:LETSSNUGGLEFOREVER
- `PRSFX- MMM1.ogg` — #1458 Move:ETERNALNIGHT、#1459 OppMove:ETERNALNIGHT、#1622 Move:MENACINGMOONRAZEMAELSTROM、#1629 OppMove:MENACINGMOONRAZEMAELSTROM
- `PRSFX- MMM3.ogg` — #1458 Move:ETERNALNIGHT、#1459 OppMove:ETERNALNIGHT、#1622 Move:MENACINGMOONRAZEMAELSTROM、#1629 OppMove:MENACINGMOONRAZEMAELSTROM
- `PRSFX- MMM4.ogg` — #1458 Move:ETERNALNIGHT、#1459 OppMove:ETERNALNIGHT、#1622 Move:MENACINGMOONRAZEMAELSTROM、#1629 OppMove:MENACINGMOONRAZEMAELSTROM
- `PRSFX- Malicious Moonsault1.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- Malicious Moonsault2.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- Malicious Moonsault3.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- Malicious Moonsault4.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- Malicious Moonsault5.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- Malicious Moonsault6.ogg` — #1621 Move:MALICIOUSMOONSAULT
- `PRSFX- S-S7-SS1.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS2.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS3.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS4.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS5.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS6.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- S-S7-SS7.ogg` — #1624 Move:SOULSTEALING7STARSTRIKE
- `PRSFX- SSS1.ogg` — #1457 Move:SUNNOVA、#1458 Move:ETERNALNIGHT、#1459 OppMove:ETERNALNIGHT、#1622 Move:MENACINGMOONRAZEMAELSTROM 等6个
- `PRSFX- Searing Sunraze Smash1.ogg` — #1457 Move:SUNNOVA、#1623 Move:SEARINGSUNRAZESMASH
- `PRSFX- Searing Sunraze Smash2.ogg` — #1457 Move:SUNNOVA、#1623 Move:SEARINGSUNRAZESMASH
- `PRSFX- Searing Sunraze Smash3.ogg` — #1457 Move:SUNNOVA、#1623 Move:SEARINGSUNRAZESMASH
- `PRSFX- Splintered Stormshards1.ogg` — #1625 Move:SPLINTEREDSTORMSHARDS
- `PRSFX- Splintered Stormshards2.ogg` — #1625 Move:SPLINTEREDSTORMSHARDS
- `PRSFX- Splintered Stormshards3.ogg` — #1625 Move:SPLINTEREDSTORMSHARDS
- `PRSFX- Splintered Stormshards4.ogg` — #1625 Move:SPLINTEREDSTORMSHARDS
- `PRSFX- Stoked Sparksurfer1.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer2.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer3.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer4.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer5.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer6.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Stoked Sparksurfer7.ogg` — #1626 Move:STOKEDSPARKSURFER
- `PRSFX- Twinkle Tackle4.wav` — #1174 ZMove:TWINKLETACKLE
- `PRSFX- Twinkle Tackle5.wav` — #1175 OppZMove:TWINKLETACKLE
- `Stone Axe 1` — #1424 Move:STONEAXE
