package pokemon.runtime.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pokemon.runtime.pokemon.PbsData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * R12: the battle BGM / victory ME lookups of PSystem_FileUtilities:546-699.
 * Every one of them walks the same source order - nextBattleBGM / nextBattleME,
 * the trainer type's own column, the map metadata, the global "[000]" metadata,
 * the plugin's default - so the tests pin each step of that chain.
 */
class BattleMusicTest {

    @TempDir
    Path tempDir;

    private PbsData pbs;

    private static void write(Path root, String name, String json) throws Exception {
        Path file = root.resolve("pbs").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() throws Exception {
        write(tempDir, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
        // [000]: the four global values the reference project defines.
        // [2]: a map that overrides its wild battle BGM and victory ME only.
        write(tempDir, "metadata.json", "{"
                + "\"kind\":\"pbsMetadata\",\"total\":1,"
                + "\"global\":{\"wildBattleBGM\":\"Battle wild.mid\","
                + "\"trainerBattleBGM\":\"Battle trainer.mid\","
                + "\"wildVictoryME\":\"Battle victory wild.ogg\","
                + "\"trainerVictoryME\":\"Battle victory trainer.ogg\"},"
                + "\"maps\":{\"2\":{\"wildBattleBGM\":\"Route 1\","
                + "\"wildVictoryME\":\"Route 1 victory\",\"outdoor\":true}}}");
        write(tempDir, "trainertypes.json", "{\"total\":2,\"trainerTypes\":{"
                + "\"POKEMONTRAINER_Red\":{\"id\":0,\"internalName\":\"POKEMONTRAINER_Red\","
                + "\"name\":\"训练家\",\"baseMoney\":60,\"battleBgm\":\"Battle trainer\"},"
                + "\"LEADER_BUG\":{\"id\":85,\"internalName\":\"LEADER_BUG\",\"name\":\"道馆馆主\","
                + "\"baseMoney\":200,\"battleBgm\":\"Battle Gym Leader\","
                + "\"victoryMe\":\"Battle victory leader\"}}}");
        pbs = PbsData.parse(tempDir.toFile());
    }

    private static PbsData.TrainerData trainer(String type) {
        PbsData.TrainerData data = new PbsData.TrainerData();
        data.type = type;
        return data;
    }

    @Test
    @DisplayName("pbGetMetadata separates the global [000] section from the map records")
    void readsMetadata() {
        assertEquals("Battle wild.mid", pbs.globalMetadata().wildBattleBGM);
        assertEquals("Battle trainer.mid", pbs.globalMetadata().trainerBattleBGM);
        assertEquals("Battle victory wild.ogg", pbs.globalMetadata().wildVictoryME);
        assertEquals("Battle victory trainer.ogg", pbs.globalMetadata().trainerVictoryME);
        assertNull(pbs.globalMetadata().wildCaptureME, "an absent key is nil, not empty");

        assertEquals("Route 1", pbs.mapMetadata(2).wildBattleBGM);
        assertNull(pbs.mapMetadata(2).trainerBattleBGM);
        assertNull(pbs.mapMetadata(3), "a map without a section has no record");
        assertNull(pbs.mapMetadata(0), "map id 0 is the global section, not a map");
    }

    @Test
    @DisplayName("pbGetWildBattleBGM: map metadata, then global, then \"Battle wild\"")
    void wildBattleBgm() {
        assertEquals("Route 1", BattleMusic.wildBattleBgm(pbs, 2, null).name,
                "the map's own WildBattleBGM wins");
        assertEquals("Battle wild.mid", BattleMusic.wildBattleBgm(pbs, 6, null).name);
        assertEquals("Battle wild", BattleMusic.wildBattleBgm(empty(), 6, null).name,
                "pbGetWildBattleBGM:562 without any metadata");
        assertEquals("Battle roaming", BattleMusic.wildBattleBgm(pbs, 6, "Battle roaming").name,
                "nextBattleBGM (PField_Field:883) beats every metadata source");
    }

    @Test
    @DisplayName("pbGetWildVictoryME: the map's ME, then the global one, then \"Battle victory\"")
    void wildVictoryMe() {
        assertEquals("Route 1 victory", BattleMusic.wildVictoryMe(pbs, 2, null).name);
        assertEquals("Battle victory wild.ogg", BattleMusic.wildVictoryMe(pbs, 6, null).name);
        assertEquals("Battle victory", BattleMusic.wildVictoryMe(empty(), 6, null).name);
        assertEquals("Roaming ME", BattleMusic.wildVictoryMe(pbs, 6, "Roaming ME").name);
    }

    @Test
    @DisplayName("pbGetWildCaptureME falls back to \"Battle capture success\" (:600)")
    void wildCaptureMe() {
        assertEquals("Battle capture success", BattleMusic.wildCaptureMe(pbs, 6, null).name);
        assertEquals("Catch jingle", BattleMusic.wildCaptureMe(pbs, 6, "Catch jingle").name);
    }

    @Test
    @DisplayName("pbGetTrainerBattleBGM: the trainer type's column 4 beats the metadata")
    void trainerBattleBgm() {
        assertEquals("Battle Gym Leader",
                BattleMusic.trainerBattleBgm(pbs, 6, null, trainer("LEADER_BUG")).name);
        assertEquals("Battle trainer",
                BattleMusic.trainerBattleBgm(pbs, 6, null, trainer("POKEMONTRAINER_Red")).name);
        assertEquals("Battle trainer.mid",
                BattleMusic.trainerBattleBgm(pbs, 6, null, trainer("UNKNOWN_TYPE")).name,
                "an unknown type falls through to the global metadata");
        assertEquals("Battle trainer",
                BattleMusic.trainerBattleBgm(empty(), 6, null, trainer("UNKNOWN_TYPE")).name,
                "pbGetTrainerBattleBGM:644 without any metadata");
        assertEquals("Boss theme",
                BattleMusic.trainerBattleBgm(pbs, 6, "Boss theme", trainer("LEADER_BUG")).name);
    }

    @Test
    @DisplayName("pbGetTrainerVictoryME: the trainer type's column 5, then the metadata")
    void trainerVictoryMe() {
        assertEquals("Battle victory leader",
                BattleMusic.trainerVictoryMe(pbs, 6, null, trainer("LEADER_BUG")).name);
        assertEquals("Battle victory trainer.ogg",
                BattleMusic.trainerVictoryMe(pbs, 6, null, trainer("POKEMONTRAINER_Red")).name,
                "no victory ME on the type: the global one is used");
        assertEquals("Battle victory",
                BattleMusic.trainerVictoryMe(empty(), 6, null, trainer("POKEMONTRAINER_Red")).name);
        assertEquals("League ME",
                BattleMusic.trainerVictoryMe(pbs, 6, "League ME", trainer("LEADER_BUG")).name);
    }

    @Test
    @DisplayName("pbStringToAudioFile parses file, file:volume and file:volume:pitch")
    void resolvesAudioFileStrings() {
        BattleMusic.Track plain = BattleMusic.resolve("Battle wild");
        assertEquals("Battle wild", plain.name);
        assertEquals(100, plain.volume);
        assertEquals(100, plain.pitch);

        BattleMusic.Track volume = BattleMusic.resolve("Battle wild: 80");
        assertEquals("Battle wild", volume.name);
        assertEquals(80, volume.volume);
        assertEquals(100, volume.pitch);

        BattleMusic.Track both = BattleMusic.resolve("Battle wild:80:120");
        assertEquals(80, both.volume);
        assertEquals(120, both.pitch);

        // The plugin passes the parsed AudioFile to pbBGMPlay.
        assertEquals("Route 1", BattleMusic.wildBattleBgm(pbs, 2, null).name);
        PbsData withVolume = parseWithGlobalWildBgm("Battle wild:70:100");
        BattleMusic.Track track = BattleMusic.wildBattleBgm(withVolume, 6, null);
        assertEquals("Battle wild", track.name);
        assertEquals(70, track.volume);
    }

    @Test
    @DisplayName("an empty metadata value is nil in Ruby terms, so it falls through")
    void emptyValuesFallThrough() {
        PbsData blank = parseWithGlobalWildBgm("");
        assertEquals("Battle wild", BattleMusic.wildBattleBgm(blank, 6, null).name);
    }

    private PbsData parseWithGlobalWildBgm(String value) {
        try {
            Path root = Files.createTempDirectory("battle-music");
            try {
                write(root, "index.json", "{\"format\":\"pokemon-builder/pbs/1\",\"kind\":\"pbsIndex\"}");
                write(root, "metadata.json", "{\"global\":{\"wildBattleBGM\":\"" + value + "\"},"
                        + "\"maps\":{}}");
                return PbsData.parse(root.toFile());
            } finally {
                deleteTree(root.toFile());
            }
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    /** PbsData with no PBS documents at all: every lookup uses its default. */
    private PbsData empty() {
        return PbsData.parse(new File(tempDir.toFile(), "missing"));
    }

    private static void deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        file.delete();
    }
}
