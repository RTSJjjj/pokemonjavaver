package pokemon.runtime.battle;

/**
 * Stage 4 / M0: {@code PBEffects} (PBEffects.rb:1-240), transcribed one
 * constant at a time. The Ruby module puts FOUR independent index spaces in one
 * module - battler effects, position effects, side effects and field effects -
 * and several names in different groups share the same number on purpose
 * ({@code AquaRing=0} vs {@code AuroraVeil=0}). Each group therefore gets its
 * own nested class here and its own {@link EffectMap} in the battle state; the
 * numbers must never be merged.
 *
 * <p>Names are kept in the plugin's CamelCase spelling rather than Java's
 * UPPER_SNAKE so that every use site can be diffed against the Ruby line that
 * produced it (the whole point of the port is line-by-line traceability).</p>
 *
 * <h2>Two plugin quirks that are reproduced deliberately</h2>
 * <ol>
 * <li>{@code Tearalament} is declared TWICE in the Ruby module - 166 at
 *     PBEffects.rb:169 and 171 at :173. The second assignment wins, so
 *     {@code PBEffects::Tearalament == 171} and index 166 is never referenced by
 *     any script. {@link Battler#Tearalament} is therefore 171; there is no
 *     constant for 166 because the plugin has none.</li>
 * <li>{@code PokeBattle_ActiveSide#initialize} (PokeBattle_ActiveField.rb:65)
 *     sets {@code @effects[PBEffects::DelusionSong] = 0}. {@code DelusionSong}
 *     is a BATTLER-group constant (172), so the side effect array is written at
 *     index 172. {@link Side#DelusionSong} keeps that value.</li>
 * </ol>
 */
public final class PBEffects {

    private PBEffects() {
    }

    /** {@code PBEffects} battler effects (PBEffects.rb:6-174): {@code battler.effects}. */
    public static final class Battler {
        private Battler() {
        }

        public static final int AquaRing = 0;
        public static final int Attract = 1;
        public static final int BanefulBunker = 2;
        public static final int BeakBlast = 3;
        public static final int Bide = 4;
        public static final int BideDamage = 5;
        public static final int BideTarget = 6;
        public static final int BurnUp = 7;
        public static final int Charge = 8;
        public static final int ChoiceBand = 9;
        public static final int Confusion = 10;
        public static final int Counter = 11;
        public static final int CounterTarget = 12;
        public static final int Curse = 13;
        public static final int Dancer = 14;
        public static final int DefenseCurl = 15;
        public static final int DestinyBond = 16;
        public static final int DestinyBondPrevious = 17;
        public static final int DestinyBondTarget = 18;
        public static final int Disable = 19;
        public static final int DisableMove = 20;
        public static final int Electrify = 21;
        public static final int Embargo = 22;
        public static final int Encore = 23;
        public static final int EncoreMove = 24;
        public static final int Endure = 25;
        public static final int FirstPledge = 26;
        public static final int FlashFire = 27;
        public static final int Flinch = 28;
        public static final int FocusEnergy = 29;
        public static final int FocusPunch = 30;
        public static final int FollowMe = 31;
        public static final int Foresight = 32;
        public static final int FuryCutter = 33;
        public static final int GastroAcid = 34;
        public static final int GemConsumed = 35;
        public static final int Grudge = 36;
        public static final int HealBlock = 37;
        public static final int HelpingHand = 38;
        public static final int HyperBeam = 39;
        public static final int Illusion = 40;
        public static final int Imprison = 41;
        public static final int Ingrain = 42;
        public static final int Instruct = 43;
        public static final int Instructed = 44;
        public static final int KingsShield = 45;
        public static final int LaserFocus = 46;
        public static final int LeechSeed = 47;
        public static final int LockOn = 48;
        public static final int LockOnPos = 49;
        public static final int MagicBounce = 50;
        public static final int MagicCoat = 51;
        public static final int MagnetRise = 52;
        public static final int MeanLook = 53;
        public static final int MeFirst = 54;
        public static final int Metronome = 55;
        public static final int MicleBerry = 56;
        public static final int Minimize = 57;
        public static final int MiracleEye = 58;
        public static final int MirrorCoat = 59;
        public static final int MirrorCoatTarget = 60;
        public static final int MoveNext = 61;
        public static final int MudSport = 62;
        public static final int Nightmare = 63;
        public static final int Outrage = 64;
        public static final int ParentalBond = 65;
        public static final int PerishSong = 66;
        public static final int PerishSongUser = 67;
        public static final int PickupItem = 68;
        public static final int PickupUse = 69;
        /** Battle Palace only (PBEffects.rb:76). */
        public static final int Pinch = 70;
        public static final int Powder = 71;
        public static final int PowerTrick = 72;
        public static final int Prankster = 73;
        public static final int PriorityAbility = 74;
        public static final int PriorityItem = 75;
        public static final int Protect = 76;
        public static final int ProtectRate = 77;
        public static final int Pursuit = 78;
        public static final int Quash = 79;
        public static final int Rage = 80;
        /** Used along with FollowMe (PBEffects.rb:81). */
        public static final int RagePowder = 81;
        public static final int Revenge = 82;
        public static final int Rollout = 83;
        public static final int Roost = 84;
        public static final int ShellTrap = 85;
        public static final int SkyDrop = 86;
        public static final int SlowStart = 87;
        public static final int SmackDown = 88;
        public static final int Snatch = 89;
        public static final int SpikyShield = 90;
        public static final int Spotlight = 91;
        public static final int Stockpile = 92;
        public static final int StockpileDef = 93;
        public static final int StockpileSpDef = 94;
        public static final int Substitute = 95;
        public static final int Taunt = 96;
        public static final int Telekinesis = 97;
        public static final int ThroatChop = 98;
        public static final int Torment = 99;
        public static final int Toxic = 100;
        public static final int Transform = 101;
        public static final int TransformSpecies = 102;
        /** Trapping move (PBEffects.rb:109). */
        public static final int Trapping = 103;
        public static final int TrappingMove = 104;
        public static final int TrappingUser = 105;
        public static final int Truant = 106;
        public static final int TwoTurnAttack = 107;
        public static final int Type3 = 108;
        public static final int Unburden = 109;
        public static final int Uproar = 110;
        public static final int WaterSport = 111;
        public static final int WeightChange = 112;
        public static final int Yawn = 113;
        public static final int GorillaTactics = 114;
        public static final int BallFetch = 115;
        // 116 and 117 are unused in the plugin (PBEffects.rb:121-122 jump 115 -> 118).
        public static final int LashOut = 118;
        public static final int BurningJealousy = 119;
        public static final int NoRetreat = 120;
        public static final int Obstruct = 121;
        public static final int JawLock = 122;
        public static final int JawLockUser = 123;
        public static final int TarShot = 124;
        public static final int Octolock = 125;
        public static final int OctolockUser = 126;
        public static final int BlunderPolicy = 127;
        public static final int SwitchedAlly = 128;
        /** 咒钉 (PBEffects.rb:133). */
        public static final int CurseNail = 129;
        /** 使用过高速旋转 (PBEffects.rb:134). */
        public static final int UsedRapidSpin = 130;
        /** 使用过盘蜷 (PBEffects.rb:135). */
        public static final int UsedCoil = 131;
        /** 断刃鏖杀无法逃走 (PBEffects.rb:136). */
        public static final int FierceKilling = 132;
        /** 断刃鏖杀无法行动 (PBEffects.rb:137). */
        public static final int FierceKilling2 = 133;
        /** 手术 (PBEffects.rb:138). */
        public static final int Operation = 134;
        /** 大将 (PBEffects.rb:139). */
        public static final int SupremeOverlord = 135;
        /** 甘露之蜜 (PBEffects.rb:140). */
        public static final int SupersweetSyrup = 136;
        public static final int CudChew = 137;
        public static final int LoseGrassType = 138;
        public static final int LoseFireType = 139;
        public static final int LoseWaterType = 140;
        public static final int DoubleShock = 141;
        public static final int BambooSword = 142;
        public static final int BoosterEnergy = 143;
        public static final int ParadoxStat = 144;
        public static final int BurningBulwark = 145;
        public static final int Commander = 146;
        public static final int GlaiveRush = 147;
        public static final int SaltCure = 148;
        public static final int Syrupy = 149;
        public static final int SyrupyUser = 150;
        public static final int SuccessiveMove = 151;
        public static final int Purgated = 152;
        public static final int CrimeSelling = 153;
        public static final int Deadline = 154;
        /**
         * {@code VictoryDance} - REASSIGNED by the {@code Arceus} section
         * ({@code Arceus:33 VictoryDance = 303}). Ruby's later assignment wins,
         * so the effective value is 303, not the main section's 155. The main
         * section's readers and writers both go through this constant, so
         * keeping the winning value is what makes them agree.
         */
        public static final int VictoryDance = 303;
        /** {@code StoneAxe} - reassigned by {@code Arceus:31} from 156 to 301 (same reason as {@link #VictoryDance}). */
        public static final int StoneAxe = 301;
        public static final int Obscured = 157;
        public static final int QuarkDrive = 158;
        public static final int DeoxysForm = 159;
        // 160 is unused (PBEffects.rb:163 jumps 159 -> 161).
        public static final int Starlight = 161;
        /** {@code CeaselessEdge} - reassigned by {@code Arceus:32} from 162 to 302 (same reason as {@link #VictoryDance}). */
        public static final int CeaselessEdge = 302;
        public static final int DisappearInAir = 163;
        public static final int StopMagic = 164;
        public static final int DisguseAimAttack = 165;
        /**
         * Declared twice in the plugin - 166 at PBEffects.rb:169 and 171 at
         * :173. The later assignment wins, so this is 171 and nothing uses 166.
         */
        public static final int Tearalament = 171;
        // 167 is unused (PBEffects.rb:170 jumps 166 -> 168).
        public static final int SilkTrap = 168;
        /** 地魔之剑 - 毒系易伤回合数 (PBEffects.rb:171). */
        public static final int PoisonVulnerability = 169;
        /** 海魔之雨 - 冰系易伤回合数 (PBEffects.rb:172). */
        public static final int IceVulnerability = 170;
        public static final int DelusionSong = 172;

        // ------------------------------------------------------------------
        // Constants added by REOPENINGS of `module PBEffects` in later sections
        // (PBEffects.rb:2-233 is only the main definition; the plugin reopens
        // the module twice more). A full-project scan of every
        // `^\s*module\s+PBEffects\b` found exactly these two reopenings, so
        // these are the only constants missing from the main section - and the
        // reason VictoryDance/StoneAxe/CeaselessEdge above had to be renumbered.
        // ------------------------------------------------------------------

        /** {@code Arceus:30 PowerShift = 300} (the plugin's own comment: "Starts from 300 to avoid conflicts with other plugins."). */
        public static final int PowerShift = 300;

        /** {@code 场地:476 CaptureNet = 304}. */
        public static final int CaptureNet = 304;

        /** {@code 场地:477 CaptureNetUser = 305}. */
        public static final int CaptureNetUser = 305;
    }

    /**
     * {@code PBEffects} position effects (PBEffects.rb:178-186):
     * {@code battle.field.positions[i].effects} - one per battle position, not
     * per battler.
     */
    public static final class Position {
        private Position() {
        }

        public static final int FutureSightCounter = 0;
        public static final int FutureSightMove = 1;
        public static final int FutureSightUserIndex = 2;
        public static final int FutureSightUserPartyIndex = 3;
        public static final int HealingWish = 4;
        public static final int LunarDance = 5;
        public static final int Wish = 6;
        public static final int WishAmount = 7;
        public static final int WishMaker = 8;
    }

    /** {@code PBEffects} side effects (PBEffects.rb:191-213): {@code battle.sides[side].effects}. */
    public static final class Side {
        private Side() {
        }

        public static final int AuroraVeil = 0;
        public static final int CraftyShield = 1;
        public static final int EchoedVoiceCounter = 2;
        public static final int EchoedVoiceUsed = 3;
        public static final int LastRoundFainted = 4;
        public static final int LightScreen = 5;
        public static final int LuckyChant = 6;
        public static final int MatBlock = 7;
        public static final int Mist = 8;
        public static final int QuickGuard = 9;
        public static final int Rainbow = 10;
        public static final int Reflect = 11;
        public static final int Round = 12;
        public static final int Safeguard = 13;
        public static final int SeaOfFire = 14;
        public static final int Spikes = 15;
        public static final int StealthRock = 16;
        public static final int StickyWeb = 17;
        public static final int Swamp = 18;
        public static final int Tailwind = 19;
        public static final int ToxicSpikes = 20;
        public static final int WideGuard = 21;
        public static final int StickyWebUser = 22;
        /**
         * {@code PokeBattle_ActiveSide#initialize} writes
         * {@code PBEffects::DelusionSong}, which is the BATTLER-group constant
         * 172 (PokeBattle_ActiveField.rb:65) - reproduced as-is.
         */
        public static final int DelusionSong = 172;
    }

    /** {@code PBEffects} battle-wide effects (PBEffects.rb:219-232): {@code battle.field.effects}. */
    public static final class Field {
        private Field() {
        }

        public static final int AmuletCoin = 0;
        public static final int FairyLock = 1;
        public static final int FusionBolt = 2;
        public static final int FusionFlare = 3;
        public static final int Gravity = 4;
        public static final int HappyHour = 5;
        public static final int IonDeluge = 6;
        public static final int MagicRoom = 7;
        public static final int MudSportField = 8;
        public static final int PayDay = 9;
        public static final int TrickRoom = 10;
        public static final int WaterSportField = 11;
        public static final int WonderRoom = 12;
        public static final int NeutralizingGas = 13;
    }
}
