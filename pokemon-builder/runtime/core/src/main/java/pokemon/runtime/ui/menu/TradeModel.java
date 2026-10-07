package pokemon.runtime.ui.menu;

import pokemon.runtime.pokemon.*;

/** NPC trades preserve the offered object's IVs, form and shininess. */
public final class TradeModel {
    private TradeModel() { }
    public static boolean eligible(Pokemon pokemon, String wanted) {
        return pokemon != null && !pokemon.egg && pokemon.species != null
                && (wanted == null || wanted.equals(pokemon.species.internalName));
    }
    public static boolean trade(TrainerState trainer, int index, Pokemon offered, String nickname,
                                String trainerName, PbsData data) {
        Pokemon mine = trainer.party.get(index);
        if (!eligible(mine, null) || offered == null || offered.egg || offered == mine) return false;
        offered.name = nickname == null || nickname.isEmpty() ? offered.name : nickname;
        offered.originalTrainer = trainerName;
        trainer.party.members().set(index, offered);
        trainer.registerOwned(offered);
        if (offered.species != null && data != null) for (PbsData.Evolution evolution : offered.species.evolutions) {
            boolean matches = "Trade".equals(evolution.method)
                    || "TradeMale".equals(evolution.method) && offered.gender == PokemonStats.MALE
                    || "TradeFemale".equals(evolution.method) && offered.gender == PokemonStats.FEMALE
                    || "TradeItem".equals(evolution.method) && evolution.parameter.equals(offered.item)
                    || "TradeSpecies".equals(evolution.method) && evolution.parameter.equals(mine.species.internalName);
            if (matches && !"EVERSTONE".equals(offered.item) && data.species(evolution.species) != null) {
                PokemonGrowth.evolve(offered, data.species(evolution.species));
                if ("TradeItem".equals(evolution.method)) offered.item = null;
                trainer.registerOwned(offered); break;
            }
        }
        return true;
    }
}
