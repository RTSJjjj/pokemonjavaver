package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.*;

/** {@code pbStartTrade}'s first half (227_PScreen_Trading:192-218): the Pokemon the player is going to get. */
public final class TradeModel {
    private TradeModel() { }

    /**
     * Builds {@code yourPokemon}: an offered Pokemon object keeps its moves; a species name makes a new Pokemon of the
     * traded one's level. Either way the foreign trainer is its OT with a fresh id, it gets the nickname, the traded
     * obtain mode and its first moves, and the player has now seen and owned the species.
     */
    public static Pokemon prepare(TrainerState trainer, Pokemon mine, Object newpoke, String nickname,
                                  String trainerName, PbsData data, java.util.Random random) {
        Pokemon yours;
        boolean resetMoves = true;
        if (newpoke instanceof Pokemon) {                                   // :197-203
            yours = (Pokemon) newpoke;
            resetMoves = false;
        } else if (newpoke instanceof String && data != null && data.species((String) newpoke) != null) {   // :204-209
            yours = pokemon.runtime.pokemon.WildGenerator.pbNewPkmn(data, data.species((String) newpoke), mine.level, null, 0, random);   // :204-209 PokeBattle_Pokemon.new
        } else {
            return null;                                                    // :206 the species does not exist
        }
        int foreignId = random.nextInt();                                   // :195 opponent.setForeignID($Trainer)
        yours.trainerID = foreignId;
        yours.publicID = foreignId & 0xFFFF;
        yours.originalTrainer = trainerName;                                // :198-200 ot, otgender
        yours.otGender = 0;                                                 // trainerGender default 0
        yours.name = nickname;                                              // :212
        yours.obtainMode = 2;                                               // :213 traded
        if (resetMoves) {
            yours.resetMoves(data);                                         // :214
        }
        yours.recordFirstMoves();                                           // :215
        trainer.registerOwned(yours);                                       // :216-218
        return yours;
    }
}
