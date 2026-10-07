package pokemon.runtime.audio;

import pokemon.runtime.pokemon.PbsData;

/**
 * The battle BGM / victory ME lookups of {@code PSystem_FileUtilities:546-699},
 * which every battle start and every battle win goes through:
 *
 * <ul>
 *   <li>{@code pbGetWildBattleBGM} (:546) - played by
 *       {@code pbBattleAnimation} (PField_Visuals:35-36) when
 *       {@code pbWildBattle} (PField_Battles:329) does not pass one;</li>
 *   <li>{@code pbGetWildVictoryME} (:565) - {@code pbWildBattleSuccess}
 *       (PokeBattle_Scene:340) via {@code pbBGMPlay};</li>
 *   <li>{@code pbGetWildCaptureME} (:585) - {@code pbThrowSuccess}
 *       (Scene_Animations:360) via {@code pbMEPlay};</li>
 *   <li>{@code pbGetTrainerBattleBGM} (:618) - {@code pbTrainerBattle}
 *       (PField_Battles:503);</li>
 *   <li>{@code pbGetTrainerVictoryME} (:668) - {@code pbTrainerBattleSuccess}
 *       (PokeBattle_Scene:347).</li>
 * </ul>
 *
 * <p>Every one of them checks the same four sources in the same order:
 * {@code $PokemonGlobal.nextBattleBGM} / {@code nextBattleME} (set by
 * {@code pbBattleAnimation}'s parameters, a roaming Pokemon
 * (PField_RoamingPokemon:203) or a script), then the trainer type's own
 * column, then the map's metadata, then the global "[000]" metadata, and
 * finally the plugin's hard-coded default. The returned names are the raw
 * strings - {@code pbStringToAudioFile} (Audio_Play:1) parses
 * "file:volume:pitch" only when the file is actually played.</p>
 */
public final class BattleMusic {

    /** pbGetWildBattleBGM:562. */
    private static final String DEFAULT_WILD_BGM = "Battle wild";
    /** pbGetTrainerBattleBGM:644 / pbGetTrainerBattleBGMFromType:664. */
    private static final String DEFAULT_TRAINER_BGM = "Battle trainer";
    /** pbGetWildVictoryME:580, pbGetTrainerVictoryME:696. */
    private static final String DEFAULT_VICTORY_ME = "Battle victory";
    /** pbGetWildCaptureME:600. */
    private static final String DEFAULT_CAPTURE_ME = "Battle capture success";

    private BattleMusic() {
    }

    /**
     * An {@code RPG::AudioFile}: the name plus the volume / pitch the string
     * carried. {@code pbStringToAudioFile} (Audio_Play:1-14) accepts
     * "file", "file:volume" and "file:volume:pitch" and defaults to 100/100.
     */
    public static final class Track {
        public final String name;
        public final int volume;
        public final int pitch;

        Track(String name, int volume, int pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }

        /** pbResolveAudioFile / pbBGMPlay: an empty name plays nothing. */
        public boolean playable() {
            return name != null && !name.isEmpty();
        }

        @Override
        public String toString() {
            return name + ":" + volume + ":" + pitch;
        }
    }

    /** pbStringToAudioFile (Audio_Play:1-14). */
    public static Track resolve(String value) {
        if (value == null) {
            return null;
        }
        java.util.regex.Matcher both =
                java.util.regex.Pattern.compile("^(.*):\\s*(\\d+)\\s*:\\s*(\\d+)\\s*$").matcher(value);
        if (both.matches()) {
            return new Track(both.group(1), Integer.parseInt(both.group(2)),
                    Integer.parseInt(both.group(3)));
        }
        java.util.regex.Matcher volume =
                java.util.regex.Pattern.compile("^(.*):\\s*(\\d+)\\s*$").matcher(value);
        if (volume.matches()) {
            return new Track(volume.group(1), Integer.parseInt(volume.group(2)), 100);
        }
        return new Track(value, 100, 100);
    }

    /**
     * pbGetWildBattleBGM (:546-563). {@code _wildParty} is unused by the
     * plugin, and {@code nextBattleBGM} wins over every other source.
     */
    public static Track wildBattleBgm(PbsData pbs, int mapId, String nextBattleBgm) {
        if (notEmpty(nextBattleBgm)) {
            return resolve(nextBattleBgm);
        }
        return resolve(firstNonEmpty(mapString(pbs, mapId, Bgm.WILD_BATTLE),
                globalString(pbs, Bgm.WILD_BATTLE), DEFAULT_WILD_BGM));
    }

    /** pbGetWildVictoryME (:565-583); the plugin also prefixes "../../Audio/ME/". */
    public static Track wildVictoryMe(PbsData pbs, int mapId, String nextBattleMe) {
        if (notEmpty(nextBattleMe)) {
            return resolve(nextBattleMe);
        }
        return resolve(firstNonEmpty(mapString(pbs, mapId, Bgm.WILD_VICTORY),
                globalString(pbs, Bgm.WILD_VICTORY), DEFAULT_VICTORY_ME));
    }

    /** pbGetWildCaptureME (:585-603). */
    public static Track wildCaptureMe(PbsData pbs, int mapId, String nextCaptureMe) {
        if (notEmpty(nextCaptureMe)) {
            return resolve(nextCaptureMe);
        }
        return resolve(firstNonEmpty(mapString(pbs, mapId, Bgm.WILD_CAPTURE),
                globalString(pbs, Bgm.WILD_CAPTURE), DEFAULT_CAPTURE_ME));
    }

    /**
     * pbGetTrainerBattleBGM (:618-646): the trainer type's own battle BGM
     * (column 4, {@code pbGetTrainerTypeData(trainer)[4]}) beats the map and
     * global metadata. An array of trainers keeps the last one that has one,
     * like the plugin's {@code trainerarray.each}.
     */
    public static Track trainerBattleBgm(PbsData pbs, int mapId, String nextBattleBgm,
            PbsData.TrainerData... trainers) {
        if (notEmpty(nextBattleBgm)) {
            return resolve(nextBattleBgm);
        }
        String typeMusic = null;
        for (PbsData.TrainerData trainer : trainers) {
            PbsData.TrainerType type = trainerType(pbs, trainer);
            if (type != null && notEmpty(type.battleBgm)) {
                typeMusic = type.battleBgm;
            }
        }
        return resolve(firstNonEmpty(typeMusic,
                mapString(pbs, mapId, Bgm.TRAINER_BATTLE),
                globalString(pbs, Bgm.TRAINER_BATTLE), DEFAULT_TRAINER_BGM));
    }

    /** pbGetTrainerVictoryME (:668-699). */
    public static Track trainerVictoryMe(PbsData pbs, int mapId, String nextBattleMe,
            PbsData.TrainerData... trainers) {
        if (notEmpty(nextBattleMe)) {
            return resolve(nextBattleMe);
        }
        String typeMusic = null;
        for (PbsData.TrainerData trainer : trainers) {
            PbsData.TrainerType type = trainerType(pbs, trainer);
            if (type != null && notEmpty(type.victoryMe)) {
                typeMusic = type.victoryMe;
            }
        }
        return resolve(firstNonEmpty(typeMusic,
                mapString(pbs, mapId, Bgm.TRAINER_VICTORY),
                globalString(pbs, Bgm.TRAINER_VICTORY), DEFAULT_VICTORY_ME));
    }

    /** The five metadata keys this resolver reads (Misc_Data:74-115). */
    private enum Bgm { WILD_BATTLE, TRAINER_BATTLE, WILD_VICTORY, TRAINER_VICTORY, WILD_CAPTURE }

    private static String mapString(PbsData pbs, int mapId, Bgm key) {
        PbsData.Metadata record = pbs == null ? null : pbs.mapMetadata(mapId);
        return record == null ? null : field(record, key);
    }

    private static String globalString(PbsData pbs, Bgm key) {
        return pbs == null ? null : field(pbs.globalMetadata(), key);
    }

    private static String field(PbsData.Metadata record, Bgm key) {
        if (record == null) {
            return null;
        }
        switch (key) {
            case WILD_BATTLE: return record.wildBattleBGM;
            case TRAINER_BATTLE: return record.trainerBattleBGM;
            case WILD_VICTORY: return record.wildVictoryME;
            case TRAINER_VICTORY: return record.trainerVictoryME;
            default: return record.wildCaptureME;
        }
    }

    private static PbsData.TrainerType trainerType(PbsData pbs, PbsData.TrainerData trainer) {
        return pbs == null || trainer == null || trainer.type == null
                ? null : pbs.trainerTypes.get(trainer.type);
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /** {@code ret = pbStringToAudioFile(x) if music && music!=""}: an empty value falls through. */
    private static String firstNonEmpty(String... candidates) {
        for (String candidate : candidates) {
            if (notEmpty(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
