package pokemon.runtime.pokemon;

/** 225_PScreen_EggHatching:192-211 {@code pbHatch}: what hatching changes on the Pokemon (the scene is {@code HatchSceneView}). */
public final class EggHatching {
    private EggHatching() {
    }

    /**
     * Everything before {@code pbHatchAnimation}: the species name, the trainer as OT, happiness 120, the time and the map,
     * obtainMode 1, seen/owned and the first moves. 登记: {@code pbSeenForm} is {@link TrainerState#registerOwned}.
     */
    public static void pbHatch(Pokemon pokemon, TrainerState trainer, int mapId, long nowSeconds) {
        pokemon.name = pokemon.species == null ? pokemon.name : pokemon.species.name;   // :193-194
        pokemon.setTrainerID(trainer.id);                                // :195
        pokemon.originalTrainer = trainer.name;                          // :196
        pokemon.happiness = 120;                                         // :197
        pokemon.timeEggHatched = nowSeconds;                             // :198
        pokemon.obtainMode = 1;                                          // :199 hatched from egg
        pokemon.hatchedMap = mapId;                                      // :200
        trainer.registerOwned(pokemon);                                  // :201-203
        pokemon.recordFirstMoves();                                      // :204
    }
}
