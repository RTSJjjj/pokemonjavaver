package pokemon.runtime.battle.movefx;

import com.badlogic.gdx.utils.Array;

import pokemon.runtime.battle.Battle;
import pokemon.runtime.battle.BattleMove;
import pokemon.runtime.battle.Battler;
import pokemon.runtime.battle.PBEffects;
import pokemon.runtime.battle.PBEnvironment;
import pokemon.runtime.battle.PBStats;
import pokemon.runtime.battle.PBTypeEffectiveness;
import pokemon.runtime.battle.PBTypes;
import pokemon.runtime.battle.PBWeather;
import pokemon.runtime.battle.PendingApi;
import pokemon.runtime.battle.PokeBattle_SceneConstants;
import pokemon.runtime.pokemon.PbsData;
import pokemon.runtime.pokemon.Pokemon;

/**
 * Stage 4 / L2: the function-code classes {@code 0B0}-{@code 0D4} (<b>37</b> of
 * them) of the plugin's {@code Move_Effects_080-0FF.rb} (3922 lines), transcribed
 * one class at a time. The Ruby range is {@code :1327-2607} (the class
 * {@code PokeBattle_Move_0B0} at :1327 through {@code PokeBattle_Move_0D4} at
 * :2540-2601; the next class, {@code PokeBattle_Move_0D5} at :2608, belongs to
 * the {@code 0D5}-{@code 0FF} file).
 *
 * <p>The rest of that Ruby file lives in the sibling files of this package:
 * {@code MoveEffects_080_0AF.java} ({@code 080}-{@code 0AF}),
 * {@code MoveEffects_0D5_0FF.java} ({@code 0D5}-{@code 0FF}) and
 * {@code MoveEffects_100_17F}/{@code _180_1FF}. {@code extends} across the ranges
 * uses the fully qualified nested name.</p>
 *
 * <h2>Shape</h2>
 * Each Ruby {@code class PokeBattle_Move_XXX < PokeBattle_Yyy} becomes a nested
 * {@code public static class PokeBattle_Move_XXX} extending
 * {@link MoveEffectsGeneric}'s matching base (or {@link MoveEffectBase} when the
 * parent is {@code PokeBattle_Move} itself). The classes are deliberately NOT
 * {@code final}: the plugin's classes are never final and the sibling ranges
 * extend across them (e.g. {@code MoveEffects_180_1FF.PokeBattle_Move_193 extends
 * MoveEffects_0B0_0D4.PokeBattle_Move_0C0}). Only the hooks the class really
 * overrides are overridden, each with its
 * {@code // Move_Effects_080-0FF.rb:行号} comment.
 *
 * <h2>Translations used in this file</h2>
 * <ul>
 * <li>Type identity is the internal-name {@code String}; item identity too, so
 *     {@code user.item==0} is {@code user.item.isEmpty()}.</li>
 * <li>{@code getID(PBMoves,:X)} / {@code getConst(PBMoves,:X)} / {@code isConst?(@id,PBMoves,:X)}
 *     go through {@link #moveIdByName(Battle, String, int)} (the PBS
 *     {@code PbsData.Move.id}), and {@code pbGetMoveData(id,MOVE_FUNCTION_CODE)} /
 *     {@code PBMoves.getName(id)} / {@code pbLoadMovesData[id]} through
 *     {@link #moveById(Battle, int)} - all real paths over {@code PbsData}.</li>
 * <li>{@code @battle.choices[i][0]}/{@code [2]} is this runtime's
 *     {@code Battle.choices(i)} array ({@code [0]} action String, {@code [2]} the
 *     registered {@code BattleMove}); see {@link #choiceAction} /
 *     {@link #choiceMove}.</li>
 * <li>{@code user.effects[X]} is {@code user.effects} (real field);
 *     {@code user.stages[s]} is {@code user.stage(s)}.</li>
 * <li>{@code @battle.pbDisplay}/{@code pbDisplayBrief} are the task-13
 *     {@code Battle.display}/{@code displayBrief}; {@code pbShowAbilitySplash} /
 *     {@code pbHideAbilitySplash} likewise.</li>
 * <li>{@code pkmn.able?} (PokeBattle_Pokemon:735-737 {@code !egg? && hp>0}) is
 *     {@link #able(Pokemon)}; {@code pkmn.baseStats[PBStats::ATTACK]} is
 *     {@code pkmn.baseStat(PBStats.ATTACK)}.</li>
 * <li>{@code m.pp}/{@code m.totalpp} (a {@code PokeBattle_Move}'s current/total
 *     PP) live on the owner's {@code Pokemon.MoveSlot} here, so the
 *     {@code eachMove} loops read {@code pokemon.moves} slots; see
 *     {@link #moveSlots(Battler)}.</li>
 * <li><b>Shared-strategy state (task-11 decision 4):</b> the plugin instantiates
 *     one move object per use, while this runtime shares one strategy per
 *     function code, so instance state set outside {@code initialize}
 *     ({@code @npMove}, {@code @sleepTalkMoves}, {@code @assistMoves},
 *     {@code @metronomeMove}, {@code @beatUpList}, {@code @calcBaseDmg},
 *     {@code @damagingTurn}) is per-code here. The {@code initialize} blacklists
 *     are constant, so they are {@code static final} arrays.</li>
 * <li>{@code if NEWEST_BATTLE_MECHANICS} appends (0B5 :1623-1646, 0BC :1962-1971)
 *     are unconditional in this project ({@code Battle.NEWEST_BATTLE_MECHANICS}
 *     is true), so the appended codes are part of the one array, with the
 *     {@code :行号} of both halves.</li>
 * <li><b>Local stubs (this file's rule):</b> a method with no runtime counterpart
 *     becomes a {@code private static} helper that throws
 *     {@code UnsupportedOperationException("M0 待接线: <段:行号>")} - never a
 *     default value. {@code MoveFxPendingApi} is not touched. The only existing
 *     stub reused is {@code PendingApi.setForm} (0CB's {@code user.form=}),
 *     which already has the exact shape.</li>
 * </ul>
 */
public final class MoveEffects_0B0_0D4 {

    private MoveEffects_0B0_0D4() {
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:1327-1479 - move-calling effects (0B0-0B3)
    // ==================================================================

    /**
     * {@code class PokeBattle_Move_0B0 < PokeBattle_Move} (:1327-1367): Me First
     * uses the move the target was about to use this round, with 1.5x power.
     */
    public static class PokeBattle_Move_0B0 extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1333-1348). */
        private static final String[] MOVE_BLACKLIST = {
                "0F1",   // :1334 Covet, Thief
                "002",   // :1336 Struggle
                "014",   // :1337 Chatter
                "158",   // :1338 Belch
                "071",   // :1340 Counter
                "072",   // :1341 Mirror Coat
                "073",   // :1342 Metal Burst
                "0BQ",   // :1344 GRANDEUR
                "115",   // :1345 Focus Punch
                "171",   // :1346 Shell Trap
                "172",   // :1347 Beak Blast
        };

        /** {@code ignoresSubstitute?(user); return true; end} (:1328). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1328
        }

        /** {@code callsAnotherMove?; return true; end} (:1329). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1329
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1351-1360). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (super.pbMoveFailedTargetAlreadyMoved(move, target)) {         // :1352
                return true;
            }
            BattleMove oppMove = choiceMove(battleOf(user), target.index);    // :1353 @battle.choices[target.index][2]
            if (oppMove == null || oppMove.id() <= 0                             // :1354
                    || oppMove.statusMove()                                      // :1355
                    || contains(MOVE_BLACKLIST, oppMove.function())) {           // :1355 @moveBlacklist.include?
                battleOf(user).display("但是失败了！");                        // :1356
                return true;                                                  // :1357
            }
            return false;                                                     // :1359
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1362-1366). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            user.effects.set(PBEffects.Battler.MeFirst, true);                // :1363
            BattleMove oppMove = choiceMove(battleOf(user), target.index);    // :1364
            pbUseMoveSimple(user, oppMove == null ? 0 : oppMove.id());
            user.effects.set(PBEffects.Battler.MeFirst, false);               // :1365
        }
    }

    /** {@code class PokeBattle_Move_0B1 < PokeBattle_Move} (:1375-1380): Magic Coat. */
    public static class PokeBattle_Move_0B1 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1376-1379). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.MagicCoat, true);              // :1377
            battleOf(user).display(user.pbThis() + "裹上了一层魔术外衣！");     // :1378
        }
    }

    /** {@code class PokeBattle_Move_0B2 < PokeBattle_Move} (:1387-1396): Snatch. */
    public static class PokeBattle_Move_0B2 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:1388-1395). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = battleOf(user);
            user.effects.set(PBEffects.Battler.Snatch, 1);                    // :1389
            for (Battler b : battle.eachBattler()) {                          // :1390 @battle.eachBattler
                if (b.effects.intVal(PBEffects.Battler.Snatch)
                        < user.effects.intVal(PBEffects.Battler.Snatch)) {    // :1391
                    continue;
                }
                user.effects.set(PBEffects.Battler.Snatch,                    // :1392
                        b.effects.intVal(PBEffects.Battler.Snatch) + 1);
            }
            battle.display(user.pbThis() + "等待一个目标来抢夺招式！");         // :1394
        }
    }

    /**
     * {@code class PokeBattle_Move_0B3 < PokeBattle_Move} (:1406-1479): Nature
     * Power uses a different move depending on the environment.
     *
     * <p>{@code @npMove} is the chosen move id; {@code getConst(PBMoves,:X) || @npMove}
     * keeps the previous value when the move is absent, so the helper's
     * {@code fallback} of -1 is only used to detect "absent".</p>
     */
    public static class PokeBattle_Move_0B3 extends MoveEffectBase {

        /** {@code @npMove} (:1413, :1416-1470). */
        private int npMove;

        /** {@code callsAnotherMove?; return true; end} (:1407). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1407
        }

        /** {@code pbOnStartUse(user,targets)} (:1409-1473). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = battleOf(user);
            npMove = moveIdByName(battle, "TRIATTACK", 0);                    // :1413 getID(PBMoves,:TRIATTACK)
            switch (battle.field.terrain) {                                   // :1414 @battle.field.terrain
                case PBBattleTerrainsElectric:                                // :1415
                    setIfPresent(battle, "THUNDERBOLT");                      // :1416
                    break;
                case PBBattleTerrainsGrassy:                                  // :1417
                    setIfPresent(battle, "ENERGYBALL");                       // :1418
                    break;
                case PBBattleTerrainsMisty:                                   // :1419
                    setIfPresent(battle, "MOONBLAST");                        // :1420
                    break;
                case PBBattleTerrainsPsychic:                                 // :1421
                    setIfPresent(battle, "PSYCHIC");                          // :1422
                    break;
                default:                                                      // :1423 else
                    switch (battle.environment) {                             // :1424 @battle.environment
                        case PBEnvironment.Grass:                             // :1425
                        case PBEnvironment.TallGrass:                         // :1425
                        case PBEnvironment.Forest:                            // :1426
                        case PBEnvironment.ForestGrass:                       // :1426
                            if (Battle.NEWEST_BATTLE_MECHANICS) {             // :1427
                                setIfPresent(battle, "ENERGYBALL");           // :1428
                            } else {
                                setIfPresent(battle, "SEEDBOMB");             // :1430
                            }
                            break;
                        case PBEnvironment.MovingWater:                       // :1432
                        case PBEnvironment.StillWater:                        // :1432
                        case PBEnvironment.Underwater:                        // :1432
                            setIfPresent(battle, "HYDROPUMP");                // :1433
                            break;
                        case PBEnvironment.Puddle:                            // :1434
                            setIfPresent(battle, "MUDBOMB");                  // :1435
                            break;
                        case PBEnvironment.Cave:                              // :1436
                            if (Battle.NEWEST_BATTLE_MECHANICS) {             // :1437
                                setIfPresent(battle, "POWERGEM");             // :1438
                            } else {
                                setIfPresent(battle, "ROCKSLIDE");            // :1440
                            }
                            break;
                        case PBEnvironment.Rock:                              // :1442
                            if (Battle.NEWEST_BATTLE_MECHANICS) {             // :1443
                                setIfPresent(battle, "EARTHPOWER");           // :1444
                            } else {
                                setIfPresent(battle, "ROCKSLIDE");            // :1446
                            }
                            break;
                        case PBEnvironment.Sand:                              // :1448
                            if (Battle.NEWEST_BATTLE_MECHANICS) {             // :1449
                                setIfPresent(battle, "EARTHPOWER");           // :1450
                            } else {
                                setIfPresent(battle, "EARTHQUAKE");           // :1452
                            }
                            break;
                        case PBEnvironment.Snow:                              // :1455
                        case PBEnvironment.Ice:                               // :1455
                            if (Battle.NEWEST_BATTLE_MECHANICS) {             // :1456
                                setIfPresent(battle, "FROSTBREATH");          // :1457
                            } else {
                                setIfPresent(battle, "ICEBEAM");              // :1459
                            }
                            break;
                        case PBEnvironment.Volcano:                           // :1461
                            setIfPresent(battle, "LAVAPLUME");                // :1462
                            break;
                        case PBEnvironment.Graveyard:                         // :1463
                            setIfPresent(battle, "SHADOWBALL");               // :1464
                            break;
                        case PBEnvironment.Sky:                               // :1465
                            setIfPresent(battle, "AIRSLASH");                 // :1466
                            break;
                        case PBEnvironment.Space:                             // :1467
                            setIfPresent(battle, "DRACOMETEOR");              // :1468
                            break;
                        case PBEnvironment.UltraSpace:                        // :1469
                            setIfPresent(battle, "PSYSHOCK");                 // :1470
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }

        /** {@code getConst(PBMoves,:X) || @npMove} (:1416 etc.): keep the old id when absent. */
        private void setIfPresent(Battle battle, String moveName) {
            int id = moveIdByName(battle, moveName, -1);
            if (id > 0) {
                npMove = id;
            }
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1475-1478). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            battle.display(move.name() + "变成了" + moveNameById(battle, npMove) + "！");   // :1476
            pbUseMoveSimple(user, npMove, target.index);                      // :1477
        }
    }

    /**
     * {@code class PokeBattle_Move_0B4 < PokeBattle_Move} (:1486-1552): Sleep Talk
     * uses a random move the user knows; fails unless the user is asleep.
     */
    public static class PokeBattle_Move_0B4 extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1492-1531). */
        private static final String[] MOVE_BLACKLIST = {
                "0D1",   // :1493 Uproar
                "0D4",   // :1494 Bide
                "002",   // :1496 Struggle
                "014",   // :1497 Chatter
                "158",   // :1498 Belch
                "05C",   // :1500 Mimic
                "05D",   // :1501 Sketch
                "0AE",   // :1503 Mirror Move
                "0AF",   // :1504 Copycat
                "0B0",   // :1505 Me First
                "0B3",   // :1506 Nature Power
                "0B4",   // :1507 Sleep Talk
                "0B5",   // :1508 Assist
                "0B6",   // :1509 Metronome
                "0C3",   // :1511 Razor Wind
                "0C4",   // :1512 Solar Beam, Solar Blade
                "0C5",   // :1513 Freeze Shock
                "0C6",   // :1514 Ice Burn
                "0C7",   // :1515 Sky Attack
                "0C8",   // :1516 Skull Bash
                "0C9",   // :1517 Fly
                "0CA",   // :1518 Dig
                "0CB",   // :1519 Dive
                "0CC",   // :1520 Bounce
                "0CD",   // :1521 Shadow Force
                "0CE",   // :1522 Sky Drop
                "12E",   // :1523 Shadow Half
                "14D",   // :1524 Phantom Force
                "14E",   // :1525 Geomancy
                "0BQ",   // :1527 GRANDEUR
                "115",   // :1528 Focus Punch
                "171",   // :1529 Shell Trap
                "172",   // :1530 Beak Blast
        };

        /** {@code @sleepTalkMoves} (:1535). */
        private final Array<Integer> sleepTalkMoves = new Array<>();

        /** {@code usableWhenAsleep?; return true; end} (:1487). */
        @Override
        public boolean usableWhenAsleep(BattleMove move) {
            return true;                                                     // :1487
        }

        /** {@code callsAnotherMove?; return true; end} (:1488). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1488
        }

        /** {@code pbMoveFailed?(user,targets)} (:1534-1546). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            sleepTalkMoves.clear();                                          // :1535
            Array<Pokemon.MoveSlot> slots = moveSlots(user);                  // :1536 eachMoveWithIndex
            for (int i = 0; i < slots.size; i++) {
                PbsData.Move data = slots.get(i) == null ? null : slots.get(i).move;
                if (data == null) {
                    continue;
                }
                if (contains(MOVE_BLACKLIST, data.function)) {                 // :1537
                    continue;
                }
                if (battleOf(user).canChooseMove(user.index, i) != null) {     // :1538 @battle.pbCanChooseMove?(user.index,i,false,true)
                    continue;
                }
                sleepTalkMoves.add(i);                                       // :1539
            }
            if (!user.asleep() || sleepTalkMoves.size == 0) {                 // :1541
                battleOf(user).display("但是失败了！");                        // :1542
                return true;                                                  // :1543
            }
            return false;                                                     // :1545
        }

        /** {@code pbEffectGeneral(user)} (:1548-1551). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = battleOf(user);
            int choice = sleepTalkMoves.get(battle.pbRandom(sleepTalkMoves.size));   // :1549
            PbsData.Move data = moveSlots(user).get(choice).move;             // :1550 user.moves[choice].id
            Battler opposing = user.pbDirectOpposing(false);
            pbUseMoveSimple(user, data == null ? 0 : data.id,
                    opposing == null ? -1 : opposing.index);                  // :1550
        }
    }

    /**
     * {@code class PokeBattle_Move_0B5 < PokeBattle_Move} (:1559-1673): Assist
     * uses a random move known by any non-user Pokemon in the user's party.
     *
     * <p>The {@code NEWEST_BATTLE_MECHANICS} append (:1623-1646) is folded into the
     * one array below - the constant is true in this project.</p>
     */
    public static class PokeBattle_Move_0B5 extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1565-1622) + the {@code NEWEST_BATTLE_MECHANICS} append (:1625-1645). */
        private static final String[] MOVE_BLACKLIST = {
                "002",   // :1566 Struggle
                "014",   // :1567 Chatter
                "158",   // :1568 Belch
                "05C",   // :1570 Mimic
                "05D",   // :1571 Sketch
                "069",   // :1572 Transform
                "071",   // :1574 Counter
                "072",   // :1575 Mirror Coat
                "073",   // :1576 Metal Burst
                "09C",   // :1578 Helping Hand
                "0AD",   // :1579 Feint
                "0AA",   // :1581 Detect, Protect
                "0AB",   // :1582 Quick Guard
                "0AC",   // :1583 Wide Guard
                "0E8",   // :1584 Endure
                "149",   // :1585 Mat Block
                "14A",   // :1586 Crafty Shield
                "14B",   // :1587 King's Shield
                "14C",   // :1588 Spiky Shield
                "168",   // :1589 Baneful Bunker
                "180",   // :1590 Obstruct
                "0AE",   // :1592 Mirror Move
                "0AF",   // :1593 Copycat
                "0B0",   // :1594 Me First
                "0B4",   // :1596 Sleep Talk
                "0B5",   // :1597 Assist
                "0B6",   // :1598 Metronome
                "0B1",   // :1600 Magic Coat
                "0B2",   // :1601 Snatch
                "117",   // :1602 Follow Me, Rage Powder
                "16A",   // :1603 Spotlight
                "0E6",   // :1605 Grudge
                "0E7",   // :1606 Destiny Bond
                "0EC",   // :1609 Circle Throw, Dragon Tail
                "0F1",   // :1611 Covet, Thief
                "0F2",   // :1612 Switcheroo, Trick
                "0F3",   // :1613 Bestow
                "0BQ",   // :1615 GRANDEUR
                "115",   // :1616 Focus Punch
                "171",   // :1617 Shell Trap
                "172",   // :1618 Beak Blast
                "133",   // :1620 Hold Hands
                "134",   // :1621 Celebrate
                // NEWEST_BATTLE_MECHANICS append (:1623-1646, true in this project)
                "0B3",   // :1626 Nature Power
                "0C3",   // :1628 Razor Wind
                "0C4",   // :1629 Solar Beam, Solar Blade
                "0C5",   // :1630 Freeze Shock
                "0C6",   // :1631 Ice Burn
                "0C7",   // :1632 Sky Attack
                "0C8",   // :1633 Skull Bash
                "0C9",   // :1634 Fly
                "0CA",   // :1635 Dig
                "0CB",   // :1636 Dive
                "0CC",   // :1637 Bounce
                "0CD",   // :1638 Shadow Force
                "0CE",   // :1639 Sky Drop
                "12E",   // :1640 Shadow Half
                "14D",   // :1641 Phantom Force
                "14E",   // :1642 Geomancy
                "0EB",   // :1644 Roar, Whirlwind
        };

        /** {@code @assistMoves} (:1650). */
        private final Array<Integer> assistMoves = new Array<>();

        /** {@code callsAnotherMove?; return true; end} (:1560). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1560
        }

        /** {@code pbMoveFailed?(user,targets)} (:1649-1667). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            assistMoves.clear();                                             // :1650
            Battle battle = battleOf(user);
            // :1652 @battle.pbParty(user.index).each_with_index
            Array<Battler> party = battle.partyOf(user.index);
            for (int i = 0; i < party.size; i++) {
                Battler b = party.get(i);
                Pokemon pkmn = b == null ? null : b.pokemon;
                if (pkmn == null || i == user.pokemonIndex) {                 // :1653
                    continue;
                }
                if (Battle.NEWEST_BATTLE_MECHANICS && pkmn.egg) {             // :1654
                    continue;
                }
                for (int s = 0; s < pkmn.moves.size; s++) {                   // :1655 pkmn.moves.each
                    PbsData.Move data = pkmn.moves.get(s).move;
                    if (data == null || data.id <= 0) {                       // :1656
                        continue;
                    }
                    if (contains(MOVE_BLACKLIST, data.function)) {             // :1657 pbGetMoveData(move.id,MOVE_FUNCTION_CODE)
                        continue;
                    }
                    if ("SHADOW".equals(data.type)) {                          // :1658 isConst?(move.type,PBTypes,:SHADOW)
                        continue;
                    }
                    assistMoves.add(data.id);                                 // :1659
                }
            }
            if (assistMoves.size == 0) {                                      // :1662
                battle.display("但是失败了！");                                // :1663
                return true;                                                  // :1664
            }
            return false;                                                     // :1666
        }

        /** {@code pbEffectGeneral(user)} (:1669-1672). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = battleOf(user);
            int chosen = assistMoves.get(battle.pbRandom(assistMoves.size));   // :1670
            pbUseMoveSimple(user, chosen);                                     // :1671
        }
    }

    /**
     * {@code class PokeBattle_Move_0B6 < PokeBattle_Move} (:1680-1799): Metronome
     * uses a random move that exists.
     */
    public static class PokeBattle_Move_0B6 extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1685-1744). */
        private static final String[] MOVE_BLACKLIST = {
                "011",   // :1686 Snore
                "11D",   // :1687 After You
                "11E",   // :1688 Quash
                "16C",   // :1689 Instruct
                "002",   // :1691 Struggle
                "014",   // :1692 Chatter
                "158",   // :1693 Belch
                "05C",   // :1695 Mimic
                "05D",   // :1696 Sketch
                "069",   // :1697 Transform
                "071",   // :1699 Counter
                "072",   // :1700 Mirror Coat
                "073",   // :1701 Metal Burst
                "09C",   // :1703 Helping Hand
                "0AD",   // :1704 Feint
                "0AA",   // :1706 Detect, Protect
                "0AB",   // :1707 Quick Guard
                "0AC",   // :1708 Wide Guard
                "0E8",   // :1709 Endure
                "149",   // :1710 Mat Block
                "14A",   // :1711 Crafty Shield
                "14B",   // :1712 King's Shield
                "14C",   // :1713 Spiky Shield
                "168",   // :1714 Baneful Bunker
                "180",   // :1715 Obstruct
                "0AE",   // :1717 Mirror Move
                "0AF",   // :1718 Copycat
                "0B0",   // :1719 Me First
                "0B3",   // :1720 Nature Power
                "0B4",   // :1721 Sleep Talk
                "0B5",   // :1722 Assist
                "0B6",   // :1723 Metronome
                "0B1",   // :1725 Magic Coat
                "0B2",   // :1726 Snatch
                "117",   // :1727 Follow Me, Rage Powder
                "16A",   // :1728 Spotlight
                "0E6",   // :1730 Grudge
                "0E7",   // :1731 Destiny Bond
                "0F1",   // :1733 Covet, Thief
                "0F2",   // :1734 Switcheroo, Trick
                "0F3",   // :1735 Bestow
                "0BQ",   // :1737 GRANDEUR
                "115",   // :1738 Focus Punch
                "171",   // :1739 Shell Trap
                "172",   // :1740 Beak Blast
                "133",   // :1742 Hold Hands
                "134",   // :1743 Celebrate
        };

        /** {@code @moveBlacklistSignatures} (:1745-1767) - the moves themselves, not their codes. */
        private static final String[] MOVE_BLACKLIST_SIGNATURES = {
                "SNARL",             // :1746
                "DIAMONDSTORM",      // :1748
                "FLEURCANNON",       // :1749
                "FREEZESHOCK",       // :1750
                "HYPERSPACEFURY",    // :1751
                "HYPERSPACEHOLE",    // :1752
                "ICEBURN",           // :1753
                "LIGHTOFRUIN",       // :1754
                "MINDBLOWN",         // :1755
                "PHOTONGEYSER",      // :1756
                "PLASMAFISTS",       // :1757
                "RELICSONG",         // :1758
                "SECRETSWORD",       // :1759
                "SPECTRALTHIEF",     // :1760
                "STEAMERUPTION",     // :1761
                "TECHNOBLAST",       // :1762
                "THOUSANDARROWS",    // :1763
                "THOUSANDWAVES",     // :1764
                "VCREATE",           // :1765
                "ETERNALFLAME",      // :1766
        };

        /** {@code @metronomeMove} (:1772). */
        private int metronomeMove;

        /** {@code callsAnotherMove?; return true; end} (:1681). */
        @Override
        public boolean callsAnotherMove(BattleMove move) {
            return true;                                                     // :1681
        }

        /** {@code pbMoveFailed?(user,targets)} (:1770-1794). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = battleOf(user);
            metronomeMove = 0;                                               // :1772
            for (int i = 0; i < 1000; i++) {                                 // :1775 1000.times
                int candidate = battle.pbRandom(maxMoveId(battle)) + 1;       // :1776 pbRandom(PBMoves.maxValue)+1
                PbsData.Move data = moveById(battle, candidate);              // :1777 movesData[move]
                if (data == null) {
                    continue;
                }
                if (contains(MOVE_BLACKLIST, data.function)) {                 // :1778
                    continue;
                }
                boolean blacklisted = false;                                   // :1779-1784
                for (String signature : MOVE_BLACKLIST_SIGNATURES) {
                    if (data.id == moveIdByName(battle, signature, -1)) {      // :1781 isConst?(move,PBMoves,m)
                        blacklisted = true;
                        break;
                    }
                }
                if (blacklisted) {
                    continue;                                                  // :1784
                }
                if ("SHADOW".equals(data.type)) {                              // :1785
                    continue;
                }
                metronomeMove = candidate;                                     // :1786
                break;                                                         // :1787
            }
            if (metronomeMove <= 0) {                                         // :1789
                battle.display("但是失败了！");                                // :1790
                return true;                                                  // :1791
            }
            return false;                                                     // :1793
        }

        /** {@code pbEffectGeneral(user)} (:1796-1798). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            pbUseMoveSimple(user, metronomeMove);                             // :1797
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:1806-2010 - disable/denial effects (0B7-0BC)
    // ==================================================================

    /** {@code class PokeBattle_Move_0B7 < PokeBattle_Move} (:1806-1823): Torment. */
    public static class PokeBattle_Move_0B7 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user); return true; end} (:1807). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1807
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1809-1816). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.truthy(PBEffects.Battler.Torment)) {            // :1810
                battleOf(user).display("但是失败了！");                        // :1811
                return true;                                                  // :1812
            }
            return super.pbMoveFailedAromaVeil(move, user, target, true);      // :1814 (Ruby 默认 showMessage=true)
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1818-1822). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Torment, true);              // :1819
            battleOf(user).display(target.pbThis() + "受到了无理取闹！");      // :1820
            target.pbItemStatusCureCheck(0, false);                           // :1821
        }
    }

    /** {@code class PokeBattle_Move_0B8 < PokeBattle_Move} (:1830-1843): Imprison. */
    public static class PokeBattle_Move_0B8 extends MoveEffectBase {

        /** {@code pbMoveFailed?(user,targets)} (:1831-1837). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.truthy(PBEffects.Battler.Imprison)) {             // :1832
                battleOf(user).display("但是失败了！");                        // :1833
                return true;                                                  // :1834
            }
            return false;                                                     // :1836
        }

        /** {@code pbEffectGeneral(user)} (:1839-1842). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.Imprison, true);                // :1840
            battleOf(user).display(user.pbThis() + "封印了目标！");            // :1841
        }
    }

    /** {@code class PokeBattle_Move_0B9 < PokeBattle_Move} (:1850-1880): Disable. */
    public static class PokeBattle_Move_0B9 extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user); return true; end} (:1851). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1851
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1853-1871). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.Disable) > 0) {        // :1854
                battleOf(user).display("但是失败了！");                        // :1855
                return true;                                                  // :1856
            }
            if (super.pbMoveFailedAromaVeil(move, user, target, true)) {        // :1858 (Ruby 默认 showMessage=true)
                return true;
            }
            boolean canDisable = false;                                       // :1859
            for (Pokemon.MoveSlot slot : moveSlots(target)) {                  // :1860 target.eachMove
                if (slot == null || slot.move == null) {
                    continue;
                }
                if (target.lastRegularMoveUsed == null
                        || !target.lastRegularMoveUsed.equals(slot.move.internalName)) {   // :1861 m.id!=target.lastRegularMoveUsed
                    continue;
                }
                if (slot.pp == 0 && slot.maxPp > 0) {                          // :1862 m.pp==0 && m.totalpp>0
                    continue;
                }
                canDisable = true;                                            // :1863
                break;                                                        // :1864
            }
            if (!canDisable) {                                                // :1866
                battleOf(user).display("但是失败了！");                        // :1867
                return true;                                                  // :1868
            }
            return false;                                                     // :1870
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1873-1879). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            target.effects.set(PBEffects.Battler.Disable, 5);                  // :1874
            target.effects.set(PBEffects.Battler.DisableMove,                  // :1875
                    moveIdByName(battle, target.lastRegularMoveUsed, -1));
            battle.display(target.pbThis() + "的"                              // :1876-1877
                    + moveNameByInternalName(battle, target.lastRegularMoveUsed) + "被禁用了！");
            target.pbItemStatusCureCheck(0, false);                            // :1878
        }
    }

    /** {@code class PokeBattle_Move_0BA < PokeBattle_Move} (:1887-1916): Taunt. */
    public static class PokeBattle_Move_0BA extends MoveEffectBase {

        /** {@code ignoresSubstitute?(user); return true; end} (:1888). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1888
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1890-1908). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            if (target.effects.intVal(PBEffects.Battler.Taunt) > 0) {          // :1891
                battle.display("但是失败了！");                                // :1892
                return true;                                                  // :1893
            }
            if (super.pbMoveFailedAromaVeil(move, user, target, true)) {        // :1895 (Ruby 默认 showMessage=true)
                return true;
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && target.hasActiveAbility("OBLIVIOUS")   // :1896-1897
                    && !battle.moldBreaker) {                                  // :1897
                battle.showAbilitySplash(target);                              // :1898
                if (PokeBattle_SceneConstants.USE_ABILITY_SPLASH) {            // :1899
                    battle.display("但是失败了！");                            // :1900
                } else {
                    battle.display("但因" + target.pbThis(true) + "的"
                            + target.abilityName() + "而失败了！");            // :1902-1903
                }
                battle.hideAbilitySplash(target);                              // :1905
                return true;                                                   // :1906
            }
            return false;                                                     // :1908
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1911-1915). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Taunt, 4);                    // :1912
            battleOf(user).display(target.pbThis() + "被挑衅了！");             // :1913
            target.pbItemStatusCureCheck(0, false);                            // :1914
        }
    }

    /** {@code class PokeBattle_Move_0BB < PokeBattle_Move} (:1923-1938): Heal Block. */
    public static class PokeBattle_Move_0BB extends MoveEffectBase {

        /** {@code pbFailsAgainstTarget?(user,target)} (:1924-1931). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            if (target.effects.intVal(PBEffects.Battler.HealBlock) > 0) {       // :1925
                battleOf(user).display("但是失败了！");                        // :1926
                return true;                                                  // :1927
            }
            return super.pbMoveFailedAromaVeil(move, user, target, true);       // :1929 (Ruby 默认 showMessage=true)
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:1933-1937). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.HealBlock, 5);                // :1934
            battleOf(user).display(target.pbThis() + "的回复被阻止了！");       // :1935
            target.pbItemStatusCureCheck(0, false);                            // :1936
        }
    }

    /**
     * {@code class PokeBattle_Move_0BC < PokeBattle_Move} (:1945-2010): Encore.
     *
     * <p>The {@code NEWEST_BATTLE_MECHANICS} append (:1962-1971) is folded into the
     * one array below.</p>
     */
    public static class PokeBattle_Move_0BC extends MoveEffectBase {

        /** {@code @moveBlacklist} (:1950-1971). */
        private static final String[] MOVE_BLACKLIST = {
                "0BC",   // :1951 Encore
                "002",   // :1953 Struggle
                "05C",   // :1955 Mimic
                "05D",   // :1956 Sketch
                "069",   // :1957 Transform
                "0AE",   // :1959 Mirror Move
                // NEWEST_BATTLE_MECHANICS append (:1962-1971, true in this project)
                "0AF",   // :1965 Copycat
                "0B0",   // :1966 Me First
                "0B3",   // :1967 Nature Power
                "0B4",   // :1968 Sleep Talk
                "0B5",   // :1969 Assist
                "0B6",   // :1970 Metronome
        };

        /** {@code ignoresSubstitute?(user); return true; end} (:1946). */
        @Override
        public boolean ignoresSubstitute(BattleMove move, Battler user) {
            return true;                                                     // :1946
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:1975-2002). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            if (target.effects.intVal(PBEffects.Battler.Encore) > 0) {          // :1976
                battle.display("但是失败了！");                                // :1977
                return true;                                                  // :1978
            }
            if (target.lastRegularMoveUsed == null                                // :1980 target.lastRegularMoveUsed<=0
                    || contains(MOVE_BLACKLIST,                                    // :1981 pbGetMoveData(...,MOVE_FUNCTION_CODE)
                    moveFunctionByInternalName(battle, target.lastRegularMoveUsed))) {
                battle.display("但是失败了！");                                // :1982
                return true;                                                  // :1983
            }
            if (target.effects.truthy(PBEffects.Battler.ShellTrap)) {           // :1985
                battle.display("但是失败了！");                                // :1986
                return true;                                                  // :1987
            }
            if (super.pbMoveFailedAromaVeil(move, user, target, true)) {         // :1989 (Ruby 默认 showMessage=true)
                return true;
            }
            boolean canEncore = false;                                        // :1990
            for (Pokemon.MoveSlot slot : moveSlots(target)) {                  // :1991 target.eachMove
                if (slot == null || slot.move == null) {
                    continue;
                }
                if (!slot.move.internalName.equals(target.lastRegularMoveUsed)) {   // :1992 m.id!=target.lastRegularMoveUsed
                    continue;
                }
                if (slot.pp == 0 && slot.maxPp > 0) {                          // :1993 m.pp==0 && m.totalpp>0
                    continue;
                }
                canEncore = true;                                             // :1994
                break;                                                        // :1995
            }
            if (!canEncore) {                                                 // :1997
                battle.display("但是失败了！");                                // :1998
                return true;                                                  // :1999
            }
            return false;                                                     // :2001
        }

        /** {@code pbEffectAgainstTarget(user,target)} (:2004-2009). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.Encore, 4);                    // :2005
            target.effects.set(PBEffects.Battler.EncoreMove,                    // :2006
                    target.lastRegularMoveUsed == null ? -1 : moveIdByName(battleOf(user), target.lastRegularMoveUsed, -1));
            battleOf(user).display(target.pbThis() + "被要求再来一次！");        // :2007
            target.pbItemStatusCureCheck(0, false);                             // :2008
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:2017-2136 - multi-hit moves (0BD-0C2)
    // ==================================================================

    /** {@code class PokeBattle_Move_0BD < PokeBattle_Move} (:2017-2020): hits twice. */
    public static class PokeBattle_Move_0BD extends MoveEffectBase {

        /** {@code multiHitMove?; return true; end} (:2018). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2018
        }

        /** {@code pbNumHits(user,targets); return 2; end} (:2019). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 2;                                                        // :2019
        }
    }

    /**
     * {@code class PokeBattle_Move_0BE < PokeBattle_PoisonMove} (:2027-2030):
     * Twineedle - hits twice, may poison on each hit (the poison body is the
     * base's).
     */
    public static class PokeBattle_Move_0BE extends MoveEffectsGeneric.PokeBattle_PoisonMove {

        /** {@code multiHitMove?; return true; end} (:2028). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2028
        }

        /** {@code pbNumHits(user,targets); return 2; end} (:2029). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 2;                                                        // :2029
        }
    }

    /** {@code class PokeBattle_Move_0BF < PokeBattle_Move} (:2038-2056): Triple Kick. */
    public static class PokeBattle_Move_0BF extends MoveEffectBase {

        /** {@code @calcBaseDmg} (:2047, :2052-2054). */
        private int calcBaseDmg;
        /** {@code @accCheckPerHit} (:2048, :2043). */
        private boolean accCheckPerHit;

        /** {@code multiHitMove?; return true; end} (:2039). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2039
        }

        /** {@code pbNumHits(user,targets); return 3; end} (:2040). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return 3;                                                        // :2040
        }

        /** {@code successCheckPerHit?} (:2042-2044). */
        @Override
        public boolean successCheckPerHit(BattleMove move) {
            return accCheckPerHit;                                           // :2043
        }

        /** {@code pbOnStartUse(user,targets)} (:2046-2049). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            calcBaseDmg = 0;                                                 // :2047
            accCheckPerHit = !user.hasActiveAbility("SKILLLINK")             // :2048
                    && !user.hasActiveItem("LOADEDDICE");
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2051-2055). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (calcBaseDmg == 0) {                                          // :2052 @calcBaseDmg = 0 if !@calcBaseDmg
                calcBaseDmg = 0;
            }
            if (!target.damageState.disguise || !target.damageState.iceface    // :2053
                    || !target.damageState.flameveil) {
                calcBaseDmg += baseDmg;
            }
            return calcBaseDmg;                                              // :2054
        }
    }

    /** {@code class PokeBattle_Move_0C0 < PokeBattle_Move} (:2063-2085): hits 2-5 times. */
    public static class PokeBattle_Move_0C0 extends MoveEffectBase {

        /** {@code hitChances = [2,2,3,3,4,5]} (:2072). */
        private static final int[] HIT_CHANCES = { 2, 2, 3, 3, 4, 5 };

        /** {@code multiHitMove?; return true; end} (:2064). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2064
        }

        /** {@code pbNumHits(user,targets)} (:2066-2076). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = battleOf(user);
            if (moveIdByName(battle, "WATERSHURIKEN", -1) == move.id()         // :2067 isConst?(@id,PBMoves,:WATERSHURIKEN)
                    && user.isSpecies("GRENINJA") && user.form() == 2) {       // :2068
                return 3;                                                     // :2069
            }
            if (user.hasActiveItem("LOADEDDICE")) {                           // :2071
                return 4 + battle.pbRandom(2);                                // :2071
            }
            int r = battle.pbRandom(HIT_CHANCES.length);                       // :2074
            if (user.hasActiveAbility("SKILLLINK")) {                         // :2075
                r = HIT_CHANCES.length - 1;
            }
            return HIT_CHANCES[r];                                            // :2076
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2078-2084). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (moveIdByName(battleOf(user), "WATERSHURIKEN", -1) == move.id()   // :2079
                    && user.isSpecies("GRENINJA") && user.form() == 2) {        // :2080
                return 20;                                                    // :2081
            }
            return super.pbBaseDamage(move, baseDmg, user, target);            // :2083 return super
        }
    }

    /**
     * {@code class PokeBattle_Move_0C1 < PokeBattle_Move} (:2095-2125): Beat Up -
     * one hit per non-user, unfainted, status-free party member.
     */
    public static class PokeBattle_Move_0C1 extends MoveEffectBase {

        /** {@code @beatUpList} (:2113, :2119-2123). */
        private final Array<Integer> beatUpList = new Array<>();

        /** {@code multiHitMove?; return true; end} (:2096). */
        @Override
        public boolean multiHitMove(BattleMove move) {
            return true;                                                     // :2096
        }

        /** {@code pbMoveFailed?(user,targets)} (:2098-2105). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            handleBeatUpList(user);                                          // :2099
            if (beatUpList.size == 0) {                                      // :2100
                battleOf(user).display("但是它失败了！");                      // :2101
                return true;                                                  // :2102
            }
            return false;                                                     // :2104
        }

        /** {@code pbNumHits(user,targets)} (:2107-2109). */
        @Override
        public int pbNumHits(BattleMove move, Battler user, Array<Battler> targets) {
            return beatUpList.size;                                          // :2108
        }

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2111-2116). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            if (beatUpList.size == 0) {                                      // :2112 handle_beatUpList(user) if !@beatUpList
                handleBeatUpList(user);
            }
            if (beatUpList.size == 0) {
                return baseDmg;
            }
            int i = beatUpList.removeIndex(0);                               // :2113 @beatUpList.shift
            Pokemon pkmn = partyPokemon(user, i);                             // :2114 @battle.pbParty(user.index)[i]
            int atk = pkmn == null ? 0 : pkmn.baseStat(PBStats.ATTACK);        // :2114 baseStats[PBStats::ATTACK]
            return 5 + (atk / 10);                                           // :2115
        }

        /** {@code handle_beatUpList(user)} (:2118-2124). */
        private void handleBeatUpList(Battler user) {
            beatUpList.clear();                                              // :2119
            // :2120 @battle.eachInTeamFromBattlerIndex(user.index)
            Battle battle = battleOf(user);
            Array<Battler> party = battle.partyOf(user.index);
            for (int i = 0; i < party.size; i++) {
                Pokemon pkmn = party.get(i) == null ? null : party.get(i).pokemon;
                if (pkmn == null || !able(pkmn)                                // :2121 !pkmn.able?
                        || !pkmn.status.isEmpty()) {                           // :2121 pkmn.status!=PBStatuses::NONE
                    continue;
                }
                beatUpList.add(i);                                           // :2122
            }
        }
    }

    /** {@code class PokeBattle_Move_0C2 < PokeBattle_Move} (:2131-2136): Hyper Beam. */
    public static class PokeBattle_Move_0C2 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2132-2135). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            user.effects.set(PBEffects.Battler.HyperBeam, 2);                  // :2133
            setCurrentMove(user, move.id());                                  // :2134 user.currentMove = @id
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:2143-2401 - two-turn attacks (0C3-0CE)
    // ==================================================================

    /** {@code class PokeBattle_Move_0C3 < PokeBattle_TwoTurnMove} (:2143-2147): Razor Wind. */
    public static class PokeBattle_Move_0C3 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:2144-2146). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "刮起了旋风！");            // :2145
        }
    }

    /** {@code class PokeBattle_Move_0C4 < PokeBattle_TwoTurnMove} (:2155-2187): Solar Beam. */
    public static class PokeBattle_Move_0C4 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbIsChargingTurn?(user)} (:2156-2170). */
        @Override
        public boolean pbIsChargingTurn(BattleMove move, Battler user) {
            boolean ret = super.pbIsChargingTurn(move, user);                 // :2157 ret = super
            if (user.effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0) {   // :2158
                int w = battleOf(user).pbWeather();                            // :2159 w = @battle.pbWeather
                if ((w == PBWeather.Sun || w == PBWeather.HarshSun               // :2161
                        || user.hasActiveAbility("SUPERSUN"))                    // :2161
                        && !user.hasUtilityUmbrella()) {                         // :2162
                    powerHerb = false;                                        // :2163
                    chargingTurn = true;                                      // :2164
                    damagingTurn = true;                                      // :2165
                    return false;                                             // :2166 直接进入伤害回合
                }
            }
            return ret;                                                       // :2169
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:2172-2174). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "吸收了阳光！");             // :2173
        }

        /** {@code pbBaseDamageMultiplier(damageMult,user,target)} (:2176-2186). */
        @Override
        public float pbBaseDamageMultiplier(BattleMove move, float damageMult, Battler user, Battler target) {
            int w = battleOf(user).pbWeather();                                // :2177
            if (w != PBWeather.None && w != PBWeather.Sun && w != PBWeather.HarshSun   // :2179
                    && !user.hasActiveAbility("SUPERSUN")) {                   // :2180
                if (!((w == PBWeather.Rain || w == PBWeather.HeavyRain)        // :2182
                        && user.hasUtilityUmbrella())) {
                    damageMult = Math.round(damageMult / 2.0f);                // :2183 (damageMult/2.0).round
                }
            }
            return damageMult;                                                // :2185
        }
    }

    /** {@code class PokeBattle_Move_0C5 < PokeBattle_Move} (:2194-2199): Freeze Shock (no charge turn). */
    public static class PokeBattle_Move_0C5 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2195-2198). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :2196
                return;
            }
            if (target.pbCanParalyze(user, false, move)) {                     // :2197 pbCanParalyze?(user,false,self)
                target.pbParalyze(user, null);                                 // :2197
            }
        }
    }

    /** {@code class PokeBattle_Move_0C6 < PokeBattle_Move} (:2206-2211): Ice Burn (no charge turn). */
    public static class PokeBattle_Move_0C6 extends MoveEffectBase {

        /** {@code pbAdditionalEffect(user,target)} (:2207-2210). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :2208
                return;
            }
            if (target.pbCanBurn(user, false, move)) {                         // :2209 pbCanBurn?(user,false,self)
                target.pbBurn(user, null);                                     // :2209
            }
        }
    }

    /** {@code class PokeBattle_Move_0C7 < PokeBattle_TwoTurnMove} (:2218-2229): Sky Attack. */
    public static class PokeBattle_Move_0C7 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code flinchingMove?; return true; end} (:2219). */
        @Override
        public boolean flinchingMove(BattleMove move) {
            return true;                                                     // :2219
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:2221-2223). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "被强烈的阳光笼罩！");       // :2222
        }

        /** {@code pbAdditionalEffect(user,target)} (:2225-2228). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :2226
                return;
            }
            target.pbFlinch(user);                                            // :2227 target.pbFlinch(user)
        }
    }

    /** {@code class PokeBattle_Move_0C8 < PokeBattle_TwoTurnMove} (:2237-2247): Skull Bash. */
    public static class PokeBattle_Move_0C8 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:2238-2240). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "将头缩回去了！");           // :2239
        }

        /** {@code pbChargingTurnEffect(user,target)} (:2242-2246). */
        @Override
        public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
            if (user.pbCanRaiseStatStage(PBStats.DEFENSE, user, move, false)) {   // :2243 pbCanRaiseStatStage?(...,self)
                user.pbRaiseStatStage(PBStats.DEFENSE, 1, user);               // :2244
            }
        }
    }

    /** {@code class PokeBattle_Move_0C9 < PokeBattle_TwoTurnMove} (:2255-2261): Fly. */
    public static class PokeBattle_Move_0C9 extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code unusableInGravity?; return true; end} (:2256). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :2256
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:2258-2260). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "飞上了高空！");             // :2259
        }
    }

    /** {@code class PokeBattle_Move_0CA < PokeBattle_TwoTurnMove} (:2269-2273): Dig. */
    public static class PokeBattle_Move_0CA extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:2270-2272). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "钻进了地下!");              // :2271
        }
    }

    /** {@code class PokeBattle_Move_0CB < PokeBattle_TwoTurnMove} (:2281-2291): Dive. */
    public static class PokeBattle_Move_0CB extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:2282-2290). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            Battle battle = battleOf(user);
            battle.display(user.pbThis() + "潜入了水下！");                     // :2283
            if (user.isSpecies("CRAMORANT")                                    // :2284 isConst?(user.species,PBSpecies,:CRAMORANT)
                    && user.hasActiveAbility("GULPMISSILE") && user.form() == 0) {   // :2285
                PendingApi.setForm(user, 2);                                   // :2286 user.form=2
                if (user.hp > user.maxHp() / 2) {                              // :2287 user.hp>(user.totalhp/2)
                    PendingApi.setForm(user, 1);                               // :2287 user.form=1
                }
                // 登记: Move_Effects_080-0FF:2288 @battle.scene.pbChangePokemon(user,user.pokemon)（场景未建模）
            }
        }
    }

    /** {@code class PokeBattle_Move_0CC < PokeBattle_TwoTurnMove} (:2300-2311): Bounce. */
    public static class PokeBattle_Move_0CC extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code unusableInGravity?; return true; end} (:2301). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :2301
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:2303-2305). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "跃上了高空！");             // :2304
        }

        /** {@code pbAdditionalEffect(user,target)} (:2307-2310). */
        @Override
        public void pbAdditionalEffect(BattleMove move, Battler user, Battler target) {
            if (target.damageState.substitute) {                              // :2308
                return;
            }
            if (target.pbCanParalyze(user, false, move)) {                     // :2309
                target.pbParalyze(user, null);                                 // :2309
            }
        }
    }

    /** {@code class PokeBattle_Move_0CD < PokeBattle_TwoTurnMove} (:2319-2334): Shadow Force. */
    public static class PokeBattle_Move_0CD extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code pbChargingTurnMessage(user,targets)} (:2320-2322). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "突然消失了！");             // :2321
        }

        /** {@code pbAttackingTurnEffect(user,target)} (:2324-2333). */
        @Override
        public void pbAttackingTurnEffect(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.BanefulBunker, false);        // :2325
            target.effects.set(PBEffects.Battler.KingsShield, false);          // :2326
            target.effects.set(PBEffects.Battler.Protect, false);              // :2327
            target.effects.set(PBEffects.Battler.SpikyShield, false);          // :2328
            target.pbOwnSide().effects.set(PBEffects.Side.CraftyShield, false);   // :2329
            target.pbOwnSide().effects.set(PBEffects.Side.MatBlock, false);       // :2330
            target.pbOwnSide().effects.set(PBEffects.Side.QuickGuard, false);     // :2331
            target.pbOwnSide().effects.set(PBEffects.Side.WideGuard, false);      // :2332
        }
    }

    /** {@code class PokeBattle_Move_0CE < PokeBattle_TwoTurnMove} (:2344-2401): Sky Drop. */
    public static class PokeBattle_Move_0CE extends MoveEffectsGeneric.PokeBattle_TwoTurnMove {

        /** {@code unusableInGravity?; return true; end} (:2345). */
        @Override
        public boolean unusableInGravity(BattleMove move) {
            return true;                                                     // :2345
        }

        /** {@code pbIsChargingTurn?(user)} (:2347-2354). */
        @Override
        public boolean pbIsChargingTurn(BattleMove move, Battler user) {
            powerHerb = false;                                                // :2350
            chargingTurn = user.effects.intVal(PBEffects.Battler.TwoTurnAttack) == 0;   // :2351
            damagingTurn = user.effects.intVal(PBEffects.Battler.TwoTurnAttack) != 0;   // :2352
            return !damagingTurn;                                             // :2353
        }

        /** {@code pbFailsAgainstTarget?(user,target)} (:2356-2379). */
        @Override
        public boolean pbFailsAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            if (!target.opposes(user)) {                                      // :2357
                battle.display("但是失败了！");                                // :2358
                return true;                                                  // :2359
            }
            if (target.effects.intVal(PBEffects.Battler.Substitute) > 0        // :2361
                    && !ignoresSubstitute(move, user)) {
                battle.display("但是失败了！");                                // :2362
                return true;                                                  // :2363
            }
            if (Battle.NEWEST_BATTLE_MECHANICS && pbWeight(target) >= 2000) {   // :2365 # 200.0kg
                battle.display("但是失败了！");                                // :2366
                return true;                                                  // :2367
            }
            if (target.semiInvulnerable()                                      // :2369
                    || (target.effects.intVal(PBEffects.Battler.SkyDrop) >= 0 && chargingTurn)) {   // :2370
                battle.display("但是失败了！");                                // :2371
                return true;                                                  // :2372
            }
            if (target.effects.intVal(PBEffects.Battler.SkyDrop) != user.index   // :2374
                    && damagingTurn) {
                battle.display("但是失败了！");                                // :2375
                return true;                                                  // :2376
            }
            return false;                                                     // :2378
        }

        /** {@code pbCalcTypeMod(movetype,user,target)} (:2381-2384). */
        @Override
        public int pbCalcTypeMod(BattleMove move, String movetype, Battler user, Battler target) {
            if (target.pbHasType("FLYING")) {                                 // :2382
                return PBTypeEffectiveness.INEFFECTIVE;                       // :2382
            }
            return super.pbCalcTypeMod(move, movetype, user, target);          // :2383 return super
        }

        /** {@code pbChargingTurnMessage(user,targets)} (:2386-2388). */
        @Override
        public void pbChargingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(user.pbThis() + "带着"                         // :2387
                    + targets.get(0).pbThis(true) + "飞上了高空！");
        }

        /** {@code pbAttackingTurnMessage(user,targets)} (:2390-2392). */
        @Override
        public void pbAttackingTurnMessage(BattleMove move, Battler user, Array<Battler> targets) {
            battleOf(user).display(targets.get(0).pbThis() + "逃脱了！");         // :2391
        }

        /** {@code pbChargingTurnEffect(user,target)} (:2394-2396). */
        @Override
        public void pbChargingTurnEffect(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.SkyDrop, user.index);         // :2395
        }

        /** {@code pbAttackingTurnEffect(user,target)} (:2398-2400). */
        @Override
        public void pbAttackingTurnEffect(BattleMove move, Battler user, Battler target) {
            target.effects.set(PBEffects.Battler.SkyDrop, -1);                 // :2399
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:2409-2530 - trapping / locked-in moves (0CF-0D3)
    // ==================================================================

    /** {@code class PokeBattle_Move_0CF < PokeBattle_Move} (:2409-2446): trapping move. */
    public static class PokeBattle_Move_0CF extends MoveEffectBase {

        /** {@code pbEffectAgainstTarget(user,target)} (:2410-2445). */
        @Override
        public void pbEffectAgainstTarget(BattleMove move, Battler user, Battler target) {
            Battle battle = battleOf(user);
            if (target.fainted() || target.damageState.substitute) {           // :2411
                return;
            }
            if (target.effects.intVal(PBEffects.Battler.Trapping) > 0) {       // :2412
                return;
            }
            if (user.hasActiveItem("GRIPCLAW")) {                             // :2414
                target.effects.set(PBEffects.Battler.Trapping,                  // :2415
                        Battle.NEWEST_BATTLE_MECHANICS ? 8 : 6);
            } else {
                target.effects.set(PBEffects.Battler.Trapping,                  // :2417
                        5 + battle.pbRandom(2));
            }
            target.effects.set(PBEffects.Battler.TrappingMove, move.id());      // :2419 @id
            target.effects.set(PBEffects.Battler.TrappingUser, user.index);     // :2420
            String msg = target.pbThis() + "被困在漩涡里了！";                   // :2422
            if (moveIdByName(battle, "BIND", -1) == move.id()) {               // :2423
                msg = target.pbThis() + "被" + user.pbThis(true) + "绑紧了！";   // :2424
            } else if (moveIdByName(battle, "CLAMP", -1) == move.id()) {       // :2425
                msg = user.pbThis() + "夹住了" + target.pbThis(true) + "!";     // :2426
            } else if (moveIdByName(battle, "FIRESPIN", -1) == move.id()) {    // :2427
                msg = target.pbThis() + "陷入了火焰漩涡中！";                   // :2428
            } else if (moveIdByName(battle, "INFESTATION", -1) == move.id()) {   // :2429
                msg = user.pbThis(true) + "侵扰着" + target.pbThis() + "！";     // :2430
            } else if (moveIdByName(battle, "MAGMASTORM", -1) == move.id()) {   // :2431
                msg = target.pbThis() + "被旋转的岩浆困住了！";                 // :2432
            } else if (moveIdByName(battle, "SANDTOMB", -1) == move.id()) {    // :2433
                msg = target.pbThis() + "陷入了流沙中！";                       // :2434
            } else if (moveIdByName(battle, "WHIRLPOOL", -1) == move.id()) {   // :2435
                msg = target.pbThis() + "陷入了漩涡中！";                       // :2436
            } else if (moveIdByName(battle, "SNAPTRAP", -1) == move.id()) {    // :2437
                msg = target.pbThis() + " was caught in the Snap Trap!";       // :2438
            } else if (moveIdByName(battle, "THUNDERCAGE", -1) == move.id()) {   // :2439
                msg = user.pbThis() + " trapped " + target.pbThis(true)        // :2440
                        + " in a Thunder Cage!";
            } else if (moveIdByName(battle, "WRAP", -1) == move.id()) {        // :2441
                msg = user.pbThis(true) + "束缚着" + target.pbThis() + "！";     // :2442
            }
            battle.display(msg);                                              // :2444
        }
    }

    /**
     * {@code class PokeBattle_Move_0D0 < PokeBattle_Move_0CF} (:2455-2462):
     * Whirlpool - doubles power against a diving target.
     */
    public static class PokeBattle_Move_0D0 extends PokeBattle_Move_0CF {

        /** {@code hitsDivingTargets?; return true; end} (:2456). */
        @Override
        public boolean hitsDivingTargets(BattleMove move) {
            return true;                                                     // :2456
        }

        /** {@code pbModifyDamage(damageMult,user,target)} (:2458-2461). */
        @Override
        public float pbModifyDamage(BattleMove move, float damageMult, Battler user, Battler target) {
            if (inTwoTurnAttack(target, "0CB")) {                             // :2459 target.inTwoTurnAttack?("0CB") # Dive
                damageMult *= 2;
            }
            return damageMult;                                                // :2460
        }
    }

    /** {@code class PokeBattle_Move_0D1 < PokeBattle_Move} (:2473-2485): Uproar. */
    public static class PokeBattle_Move_0D1 extends MoveEffectBase {

        /** {@code pbEffectGeneral(user)} (:2474-2484). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            Battle battle = battleOf(user);
            if (user.effects.intVal(PBEffects.Battler.Uproar) > 0) {           // :2475
                return;
            }
            user.effects.set(PBEffects.Battler.Uproar, 3);                     // :2476
            setCurrentMove(user, move.id());                                  // :2477 user.currentMove = @id
            battle.display(user.pbThis() + "制造着噪音！");                     // :2478
            for (Battler b : pbPriority(battle, true)) {                       // :2479 @battle.pbPriority(true)
                if (b.fainted() || !"SLEEP".equals(b.status)) {                // :2480 b.status!=PBStatuses::SLEEP
                    continue;
                }
                if (b.hasActiveAbility("SOUNDPROOF")) {                        // :2481
                    continue;
                }
                b.pbCureStatus();                                             // :2482
            }
        }
    }

    /** {@code class PokeBattle_Move_0D2 < PokeBattle_Move} (:2493-2506): Outrage / Thrash. */
    public static class PokeBattle_Move_0D2 extends MoveEffectBase {

        /** {@code pbEffectAfterAllHits(user,target)} (:2494-2505). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.unaffected                                  // :2495
                    && user.effects.intVal(PBEffects.Battler.Outrage) == 0) {
                user.effects.set(PBEffects.Battler.Outrage, 2 + battleOf(user).pbRandom(2));   // :2496
                setCurrentMove(user, move.id());                               // :2497 user.currentMove = @id
            }
            if (user.effects.intVal(PBEffects.Battler.Outrage) > 0) {           // :2499
                user.effects.decrement(PBEffects.Battler.Outrage);              // :2500 -= 1
                if (user.effects.intVal(PBEffects.Battler.Outrage) == 0          // :2501
                        && user.pbCanConfuse(null, false, null, true)) {        // :2501 user.pbCanConfuseSelf?(false)
                    pbConfuse(user, user.pbThis() + "因为过于疲劳而混乱了！");   // :2502
                }
            }
        }
    }

    /** {@code class PokeBattle_Move_0D3 < PokeBattle_Move} (:2515-2530): Ice Ball / Rollout. */
    public static class PokeBattle_Move_0D3 extends MoveEffectBase {

        /** {@code pbBaseDamage(baseDmg,user,target)} (:2516-2521). */
        @Override
        public int pbBaseDamage(BattleMove move, int baseDmg, Battler user, Battler target) {
            int rollout = user.effects.intVal(PBEffects.Battler.Rollout);
            int shift = 5 - rollout;                                          // :2516
            if (rollout == 0) {                                               // :2517
                shift = 0;                                                    // :2517 For first turn
            }
            if (user.effects.truthy(PBEffects.Battler.DefenseCurl)) {           // :2518
                shift += 1;
            }
            baseDmg *= (1 << shift);                                          // :2519 2**shift
            return baseDmg;                                                   // :2520
        }

        /** {@code pbEffectAfterAllHits(user,target)} (:2523-2529). */
        @Override
        public void pbEffectAfterAllHits(BattleMove move, Battler user, Battler target) {
            if (!target.damageState.unaffected                                  // :2524
                    && user.effects.intVal(PBEffects.Battler.Rollout) == 0) {
                user.effects.set(PBEffects.Battler.Rollout, 5);                 // :2525
                setCurrentMove(user, move.id());                               // :2526 user.currentMove = @id
            }
            if (user.effects.intVal(PBEffects.Battler.Rollout) > 0) {           // :2528
                user.effects.decrement(PBEffects.Battler.Rollout);              // :2528 -= 1
            }
        }
    }

    // ==================================================================
    // Move_Effects_080-0FF.rb:2540-2601 - Bide (0D4)
    // ==================================================================

    /** {@code class PokeBattle_Move_0D4 < PokeBattle_FixedDamageMove} (:2540-2601): Bide. */
    public static class PokeBattle_Move_0D4 extends MoveEffectsGeneric.PokeBattle_FixedDamageMove {

        /** {@code @damagingTurn} (:2565, set by {@code pbOnStartUse}). */
        private boolean damagingTurn;

        /** {@code pbAddTarget(targets,user)} (:2541-2547). */
        @Override
        public void pbAddTarget(BattleMove move, Array<Battler> targets, Battler user) {
            if (user.effects.intVal(PBEffects.Battler.Bide) != 1) {            // :2542 Not the attack turn
                return;
            }
            int idxTarget = user.effects.intVal(PBEffects.Battler.BideTarget);  // :2543
            Battler t = idxTarget >= 0 ? battleOf(user).battlerAt(idxTarget) : null;   // :2543 @battle.battlers[idxTarget]
            if (!battlerPbAddTarget(user, targets, t, move, false)) {                // :2544 user.pbAddTarget(targets,user,t,self,false)
                battlerPbAddTargetRandomFoe(user, targets, move, false);              // :2545
            }
        }

        /** {@code pbMoveFailed?(user,targets)} (:2550-2562). */
        @Override
        public boolean pbMoveFailed(BattleMove move, Battler user, Array<Battler> targets) {
            if (user.effects.intVal(PBEffects.Battler.Bide) != 1) {            // :2551 Not the attack turn
                return false;
            }
            if (user.effects.intVal(PBEffects.Battler.BideDamage) == 0) {       // :2552
                battleOf(user).display("但是失败了！");                        // :2553
                user.effects.set(PBEffects.Battler.Bide, 0);                    // :2554
                return true;                                                  // :2555
            }
            if (targets.size == 0) {                                          // :2556
                battleOf(user).display("但是没有目标……");                     // :2557
                user.effects.set(PBEffects.Battler.Bide, 0);                    // :2558
                return true;                                                  // :2559
            }
            return false;                                                     // :2561
        }

        /** {@code pbOnStartUse(user,targets)} (:2565-2567). */
        @Override
        public void pbOnStartUse(BattleMove move, Battler user, Array<Battler> targets) {
            damagingTurn = user.effects.intVal(PBEffects.Battler.Bide) == 1;    // :2566
        }

        /** {@code pbDisplayUseMessage(user)} (:2569-2577). */
        @Override
        public void pbDisplayUseMessage(BattleMove move, Battler user) {
            if (damagingTurn) {                                               // :2570 Attack turn
                battleOf(user).displayBrief(user.pbThis() + "释放了能量！");    // :2571
            } else if (user.effects.intVal(PBEffects.Battler.Bide) > 1) {       // :2572 Charging turns
                battleOf(user).displayBrief(user.pbThis() + "正在积蓄能量！");   // :2573
            } else {
                super.pbDisplayUseMessage(move, user);                         // :2575 super (Start using Bide)
            }
        }

        /** {@code pbDamagingMove?} (:2579-2581). */
        @Override
        public boolean pbDamagingMove(BattleMove move) {
            if (!damagingTurn) {                                              // :2580
                return false;
            }
            return super.pbDamagingMove(move);                                 // :2581 return super
        }

        /** {@code pbFixedDamage(user,target)} (:2584-2586). */
        @Override
        public int pbFixedDamage(BattleMove move, Battler user, Battler target) {
            return user.effects.intVal(PBEffects.Battler.BideDamage) * 2;       // :2585
        }

        /** {@code pbEffectGeneral(user)} (:2588-2596). */
        @Override
        public void pbEffectGeneral(BattleMove move, Battler user) {
            if (user.effects.intVal(PBEffects.Battler.Bide) == 0) {             // :2589 Starting using Bide
                user.effects.set(PBEffects.Battler.Bide, 3);                    // :2590
                user.effects.set(PBEffects.Battler.BideDamage, 0);              // :2591
                user.effects.set(PBEffects.Battler.BideTarget, -1);             // :2592
                setCurrentMove(user, move.id());                               // :2593 user.currentMove = @id
            }
            user.effects.decrement(PBEffects.Battler.Bide);                     // :2595 -= 1
        }

        /** {@code pbShowAnimation(id,user,targets,hitNum=0,showAnimation=true)} (:2598-2600). */
        @Override
        public void pbShowAnimation(BattleMove move, int id, Battler user, Array<Battler> targets, int hitNum,
                                    boolean showAnimation) {
            int animHitNum = damagingTurn ? hitNum : 1;                        // :2599 hitNum = 1 if !@damagingTurn
            super.pbShowAnimation(move, id, user, targets, animHitNum, showAnimation);   // :2600 super
        }
    }

    // ==================================================================
    // Private helpers
    // ==================================================================

    /** The terrain ids the plugin's {@code PBBattleTerrains} switch uses (kept as named constants for readability). */
    private static final int PBBattleTerrainsElectric = 1;
    private static final int PBBattleTerrainsGrassy = 2;
    private static final int PBBattleTerrainsMisty = 3;
    private static final int PBBattleTerrainsPsychic = 4;

    /** {@code Battler#battle} (PokeBattle_Battler:3) - a real public field in this runtime. */
    private static Battle battleOf(Battler battler) {
        return battler.battle;
    }

    /** {@code @moveBlacklist.include?(code)}. */
    private static boolean contains(String[] codes, String code) {
        if (code == null) {
            return false;
        }
        for (String candidate : codes) {
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** {@code @battle.choices[idxBattler][0]} (Battle.choices) as its action String. */
    private static String choiceAction(Battle battle, int idxBattler) {
        Object[] choices = battle.choices(idxBattler);
        return choices == null || choices.length == 0 || choices[0] == null
                ? "" : String.valueOf(choices[0]);
    }

    /** {@code @battle.choices[idxBattler][2]} (Battle.choices): the registered move. */
    private static BattleMove choiceMove(Battle battle, int idxBattler) {
        Object[] choices = battle.choices(idxBattler);
        return choices == null || choices.length < 3 || !(choices[2] instanceof BattleMove)
                ? null : (BattleMove) choices[2];
    }

    /** {@code Battler#moveSlots} - the four slots, a blank one being {@code null} ({@code id==0}). */
    private static Array<Pokemon.MoveSlot> moveSlots(Battler battler) {
        return battler.pokemon == null ? new Array<>() : battler.pokemon.moves;
    }

    /** {@code @battle.pbParty(idx)[i]} (Battle_StartAndEnd:307-309) as its Pokemon. */
    private static Pokemon partyPokemon(Battler battler, int idxParty) {
        Array<Battler> party = battleOf(battler).partyOf(battler.index);
        if (idxParty < 0 || idxParty >= party.size || party.get(idxParty) == null) {
            return null;
        }
        return party.get(idxParty).pokemon;
    }

    /** {@code pkmn.able?} (PokeBattle_Pokemon:735-737 {@code !egg? && hp>0}). */
    private static boolean able(Pokemon pkmn) {
        return pkmn != null && !pkmn.egg && pkmn.hp > 0;
    }

    /**
     * {@code PBMoves.maxValue} (the highest move id in the PBS table): the runtime
     * has no {@code PBMoves} module, so it is computed over
     * {@code PbsData.moves}.
     */
    private static int maxMoveId(Battle battle) {
        PbsData pbs = battle == null ? null : battle.pbs();
        if (pbs == null) {
            return 0;
        }
        int max = 0;
        for (PbsData.Move data : pbs.moves.values()) {
            if (data != null && data.id > max) {
                max = data.id;
            }
        }
        return max;
    }

    /** {@code pbLoadMovesData[id]} / {@code pbGetMoveData(id,...)}: the move record of a numeric id, or null. */
    private static PbsData.Move moveById(Battle battle, int id) {
        PbsData pbs = battle == null ? null : battle.pbs();
        if (pbs == null || id <= 0) {
            return null;
        }
        for (PbsData.Move data : pbs.moves.values()) {
            if (data != null && data.id == id) {
                return data;
            }
        }
        return null;
    }

    /** {@code getID(PBMoves,:X)}: the PBS id of an internal name, or {@code fallback} when absent. */
    private static int moveIdByName(Battle battle, String internalName, int fallback) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Move data = pbs == null || internalName == null ? null : pbs.move(internalName);
        return data == null ? fallback : data.id;
    }

    /** {@code PBMoves.getName(id)}: the display name of a move id ({@code null} when absent). */
    private static String moveNameById(Battle battle, int id) {
        PbsData.Move data = moveById(battle, id);
        return data == null ? null : data.name;
    }

    /** {@code pbGetMoveData(id,MOVE_FUNCTION_CODE)}: the function code of an internal name ({@code null} when absent). */
    private static String moveFunctionByInternalName(Battle battle, String internalName) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Move data = pbs == null || internalName == null ? null : pbs.move(internalName);
        return data == null ? null : data.function;
    }

    /** {@code PBMoves.getName(id)} for the runtime's internal-name id ({@code null} when absent). */
    private static String moveNameByInternalName(Battle battle, String internalName) {
        PbsData pbs = battle == null ? null : battle.pbs();
        PbsData.Move data = pbs == null || internalName == null ? null : pbs.move(internalName);
        return data == null ? null : data.name;
    }

    /** {@code user.pbUseMoveSimple(moveID)} (PokeBattle_Battler:...). */
    private static void pbUseMoveSimple(Battler user, int moveId) {
        pbUseMoveSimple(user, moveId, -1);
    }

    /** {@code user.pbUseMoveSimple(moveID,targetIndex)} (PokeBattle_Battler:...). */
    private static void pbUseMoveSimple(Battler user, int moveId, int targetIndex) {
        PbsData.Move m = user.battle.pbs().moveById(moveId);
        user.pbUseMoveSimple(m == null ? null : m.internalName, targetIndex);   // Battler_UseMove:152-165
    }

    /** {@code user.currentMove = @id} (PokeBattle_Battler:38 {@code attr_accessor :currentMove}). */
    private static void setCurrentMove(Battler user, int moveId) {
        user.currentMove = moveId;                              // PokeBattle_Battler:38
    }

    /** {@code target.pbFlinch(user)} (Battler_ChangeSelf:...). */
    private static void pbFlinch(Battler target) {
        target.pbFlinch();
    }

    /** {@code user.pbConfuse(msg)} (Battler_Statuses:531-540). */
    private static void pbConfuse(Battler user, String msg) {
        user.pbConfuse(msg);
    }

    /** {@code user.pbAddTarget(targets,user,target,move,showMessages)} (Battler_UseMove_Targeting:...). */
    private static boolean battlerPbAddTarget(Battler user, Array<Battler> targets, Battler target, BattleMove move,
                                       boolean showMessages) {
        return pokemon.runtime.battle.BattlerTargeting.pbAddTarget(user, targets, user, target, move, showMessages, false);   // Battler_UseMove_Targeting:243 (5th arg is nearOnly)
    }

    /** {@code user.pbAddTargetRandomFoe(targets,user,move,showMessages)} (Battler_UseMove_Targeting:...). */
    private static void battlerPbAddTargetRandomFoe(Battler user, Array<Battler> targets, BattleMove move,
                                             boolean showMessages) {
        pokemon.runtime.battle.BattlerTargeting.pbAddTargetRandomFoe(user, targets, user, move, showMessages);   // :262 (4th arg is nearOnly)
    }

    /** {@code target.pbWeight} (PokeBattle_Battler:700-706). */
    private static int pbWeight(Battler target) {
        return target.pbWeight();
    }

    /** {@code Battler#inTwoTurnAttack?(*functionCodes)} (PokeBattle_Battler:718-723). */
    private static boolean inTwoTurnAttack(Battler battler, String... functionCodes) {
        return battler.inTwoTurnAttack(functionCodes);
    }

    /** {@code @battle.pbPriority(ignoringFainted)} (Battle_Phase_Attack:...): battlers in priority order. */
    private static Array<Battler> pbPriority(Battle battle, boolean ignoringFainted) {
        return battle.pbPriority(ignoringFainted);
    }

    /**
     * {@code PokeBattle_Move.pbFromPBMove} (PokeBattle_Move.rb:51-59) for the
     * 37 function-code classes this file holds: the Ruby builds
     * {@code "PokeBattle_Move_#{function}"} and instantiates it on demand, so each
     * code is bound to its no-argument constructor here and
     * {@link MoveEffectRegistry#of} calls it lazily (some constructors still touch
     * not-yet-wired stubs, which must not break the whole table).
     *
     * <p>Generated by {@code __l2register.mjs} - re-run it with {@code --write}
     * after the class set changes.</p>
     */
    static void register() {
        MoveEffectRegistry.register("0B0", PokeBattle_Move_0B0::new);
        MoveEffectRegistry.register("0B1", PokeBattle_Move_0B1::new);
        MoveEffectRegistry.register("0B2", PokeBattle_Move_0B2::new);
        MoveEffectRegistry.register("0B3", PokeBattle_Move_0B3::new);
        MoveEffectRegistry.register("0B4", PokeBattle_Move_0B4::new);
        MoveEffectRegistry.register("0B5", PokeBattle_Move_0B5::new);
        MoveEffectRegistry.register("0B6", PokeBattle_Move_0B6::new);
        MoveEffectRegistry.register("0B7", PokeBattle_Move_0B7::new);
        MoveEffectRegistry.register("0B8", PokeBattle_Move_0B8::new);
        MoveEffectRegistry.register("0B9", PokeBattle_Move_0B9::new);
        MoveEffectRegistry.register("0BA", PokeBattle_Move_0BA::new);
        MoveEffectRegistry.register("0BB", PokeBattle_Move_0BB::new);
        MoveEffectRegistry.register("0BC", PokeBattle_Move_0BC::new);
        MoveEffectRegistry.register("0BD", PokeBattle_Move_0BD::new);
        MoveEffectRegistry.register("0BE", PokeBattle_Move_0BE::new);
        MoveEffectRegistry.register("0BF", PokeBattle_Move_0BF::new);
        MoveEffectRegistry.register("0C0", PokeBattle_Move_0C0::new);
        MoveEffectRegistry.register("0C1", PokeBattle_Move_0C1::new);
        MoveEffectRegistry.register("0C2", PokeBattle_Move_0C2::new);
        MoveEffectRegistry.register("0C3", PokeBattle_Move_0C3::new);
        MoveEffectRegistry.register("0C4", PokeBattle_Move_0C4::new);
        MoveEffectRegistry.register("0C5", PokeBattle_Move_0C5::new);
        MoveEffectRegistry.register("0C6", PokeBattle_Move_0C6::new);
        MoveEffectRegistry.register("0C7", PokeBattle_Move_0C7::new);
        MoveEffectRegistry.register("0C8", PokeBattle_Move_0C8::new);
        MoveEffectRegistry.register("0C9", PokeBattle_Move_0C9::new);
        MoveEffectRegistry.register("0CA", PokeBattle_Move_0CA::new);
        MoveEffectRegistry.register("0CB", PokeBattle_Move_0CB::new);
        MoveEffectRegistry.register("0CC", PokeBattle_Move_0CC::new);
        MoveEffectRegistry.register("0CD", PokeBattle_Move_0CD::new);
        MoveEffectRegistry.register("0CE", PokeBattle_Move_0CE::new);
        MoveEffectRegistry.register("0CF", PokeBattle_Move_0CF::new);
        MoveEffectRegistry.register("0D0", PokeBattle_Move_0D0::new);
        MoveEffectRegistry.register("0D1", PokeBattle_Move_0D1::new);
        MoveEffectRegistry.register("0D2", PokeBattle_Move_0D2::new);
        MoveEffectRegistry.register("0D3", PokeBattle_Move_0D3::new);
        MoveEffectRegistry.register("0D4", PokeBattle_Move_0D4::new);
    }

}
