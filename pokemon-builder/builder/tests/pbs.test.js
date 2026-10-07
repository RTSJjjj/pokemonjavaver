// Stage 3 / P0: PBS text -> generated/pbs/*.json parser tests.
//
// The real PBS folder is a live project input, so the unit tests use synthetic
// text (every dialect detail: quoted commas, parameterless evolution methods,
// missing trailing fields) and one optional floor test against the reference
// project when it is checked out next to the builder.

import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdtempSync, mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";
import os from "node:os";

import {
  buildNatures,
  buildPbsIr,
  parseAbilities,
  parseCsvRecords,
  parseEncounters,
  parseIniSections,
  parseItems,
  parseMetadata,
  parsePokemon,
  parsePokemonForms,
  parseTmCompatibility,
  parseTrainerTypes,
  parseTrainers,
  parseTypes,
  parseMoves,
} from "../../tools/data-converter/pbs.js";

test("INI sections keep headers, keys and comments out", () => {
  const sections = parseIniSections(`
# a comment
[1]
Name = 妙蛙种子
BaseStats = 45,49,49,45,65,65

[VENUSAUR,1]
FormName = 超级妙蛙花
`);
  assert.equal(sections.length, 2);
  assert.equal(sections[0].header, "1");
  assert.equal(sections[0].fields.get("name"), "妙蛙种子");
  assert.equal(sections[1].header, "VENUSAUR,1");
  assert.equal(sections[1].fields.get("formname"), "超级妙蛙花");
});

test("CSV records keep commas inside quotes and tolerate missing fields", () => {
  const records = parseCsvRecords(`
# comment
1,MEGAHORN,超级角击,000,120,BUG,Physical,85,10,0,NearOther,0,abef,"角很硬, 很华丽"
2,SPLASH,水溅跃,000,0,NORMAL,Status,0,40,0,User,0,d,"什么都不做"
`);
  assert.equal(records.length, 2);
  assert.equal(records[0][13], "角很硬, 很华丽");
  assert.equal(records[1][1], "SPLASH");
});

test("pokemon.txt normalizes the species model (moves, items, evolutions)", () => {
  const { species, byId } = parsePokemon(`
[1]
Name = 妙蛙种子
InternalName = BULBASAUR
Type1 = GRASS
Type2 = POISON
BaseStats = 45,49,49,45,65,65
GenderRate = Female50Percent
GrowthRate = Parabolic
BaseEXP = 64
EffortPoints = 0,0,0,0,1,0
Rareness = 45
Happiness = 50
Abilities = OVERGROW
HiddenAbility = CHLOROPHYLL
Moves = 1,TACKLE,3,VINEWHIP
EggMoves = SKULLBASH,CHARM
Compatibility = Monster,Grass
StepsToHatch = 5355
Height = 0.7
Weight = 6.9
Color = Green
Shape = 8
Kind = 种子
Evolutions = IVYSAUR,Level,16
WildItemCommon = LEFTOVERS
`);
  assert.equal(byId["1"], "BULBASAUR");
  const bulb = species.BULBASAUR;
  assert.deepEqual(bulb.types, ["GRASS", "POISON"]);
  assert.deepEqual(bulb.baseStats, [45, 49, 49, 45, 65, 65]);
  assert.deepEqual(bulb.abilities, ["OVERGROW"]);
  assert.equal(bulb.hiddenAbility, "CHLOROPHYLL");
  assert.deepEqual(bulb.moves, [{ level: 1, move: "TACKLE" }, { level: 3, move: "VINEWHIP" }]);
  assert.deepEqual(bulb.evolutions, [{ species: "IVYSAUR", method: "Level", parameter: "16" }]);
  assert.equal(bulb.wildItems.common, "LEFTOVERS");
  assert.equal(bulb.kind, "种子");
});

test("parameterless evolution methods do not swallow the next species (L6-style lookahead)", () => {
  const { species } = parsePokemon(`
[133]
InternalName = EEVEE
Evolutions = ESPEON,HappinessDay,UMBREON,HappinessNight
`);
  assert.deepEqual(species.EEVEE.evolutions, [
    { species: "ESPEON", method: "HappinessDay", parameter: null },
    { species: "UMBREON", method: "HappinessNight", parameter: null },
  ]);
});

test("pokemonforms.txt keys alternate forms by SPECIES_FORM", () => {
  const forms = parsePokemonForms(`
[VENUSAUR,1]
FormName = 超级妙蛙花
BaseStats = 80,100,123,80,122,120
Abilities = THICKFAT
MegaStone = VENUSAURITE
UnmegaForm = 0
Height = 2.4
`);
  assert.deepEqual(Object.keys(forms), ["VENUSAUR_1"]);
  assert.equal(forms.VENUSAUR_1.species, "VENUSAUR");
  assert.equal(forms.VENUSAUR_1.form, 1);
  assert.equal(forms.VENUSAUR_1.megaStone, "VENUSAURITE");
  assert.deepEqual(forms.VENUSAUR_1.baseStats, [80, 100, 123, 80, 122, 120]);
});

test("moves/items/abilities/trainertypes normalize their CSV columns", () => {
  const moves = parseMoves('1,MEGAHORN,超级角击,000,120,BUG,Physical,85,10,0,NearOther,0,abef,"角很硬"\n');
  assert.equal(moves.MEGAHORN.power, 120);
  assert.equal(moves.MEGAHORN.type, "BUG");
  assert.equal(moves.MEGAHORN.category, "Physical");
  assert.equal(moves.MEGAHORN.flags, "abef");

  const items = parseItems('1,REPEL,除虫喷雾,除虫喷雾,1,400,"喷雾",2,0,0,\n');
  assert.equal(items.REPEL.pocket, 1);
  assert.equal(items.REPEL.price, 400);
  assert.equal(items.REPEL.fieldUse, 2);

  const abilities = parseAbilities("1,STENCH,恶臭,释放臭气\n");
  assert.equal(abilities.STENCH.name, "恶臭");

  const trainerTypes = parseTrainerTypes(
    "0,POKEMONTRAINER_Red,训练家,60,Battle trainer,,,Male,,\n"
    + "85,LEADER_BUG,道馆馆主,200,Battle Gym Leader,Battle victory leader,,Female,,\n");
  assert.equal(trainerTypes.POKEMONTRAINER_Red.name, "训练家");
  assert.equal(trainerTypes.POKEMONTRAINER_Red.baseMoney, 60);
  // Compiler_PBS:1374 "unsUSSSeUS": 4 = battle BGM (pbGetTrainerBattleBGM:627),
  // 5 = victory ME (pbGetTrainerVictoryME:676), 6 = intro ME.
  assert.equal(trainerTypes.POKEMONTRAINER_Red.battleBgm, "Battle trainer");
  assert.equal(trainerTypes.POKEMONTRAINER_Red.victoryMe, null);
  assert.equal(trainerTypes.LEADER_BUG.victoryMe, "Battle victory leader");
});

test("types.txt and tm.txt keep the type chart and TM compatibility", () => {
  const types = parseTypes(`
[0]
Name = 普通
InternalName = NORMAL
Weaknesses = FIGHTING
Immunities = GHOST
[9]
Name = 无
InternalName = QMARKS
IsPseudoType = true
[10]
Name = 火
InternalName = FIRE
IsSpecialType = 1
`);
  assert.deepEqual(types.NORMAL.immunities, ["GHOST"]);
  assert.deepEqual(types.NORMAL.weaknesses, ["FIGHTING"]);
  // Compiler_PBS:345-346 map IsPseudoType -> [3,"b"] and IsSpecialType ->
  // [4,"b"]; PBTypes_Extra:34-40 read them back as the pseudo/special lists.
  assert.equal(types.NORMAL.pseudoType, false, "an absent flag defaults to false");
  assert.equal(types.NORMAL.specialType, false, "an absent flag defaults to false");
  assert.equal(types.QMARKS.pseudoType, true);
  assert.equal(types.QMARKS.specialType, false);
  assert.equal(types.FIRE.pseudoType, false);
  assert.equal(types.FIRE.specialType, true, 'Compiler:370 accepts "1" for a "b" field');

  const tm = parseTmCompatibility("[MEGAHORN]\nABSOL,BOUFFALANT\n[BUGBUZZ]\nBUTTERFREE\n");
  assert.deepEqual(tm.MEGAHORN, ["ABSOL", "BOUFFALANT"]);
  assert.deepEqual(tm.BUGBUZZ, ["BUTTERFREE"]);
});

/**
 * PBS/metadata.txt of the reference project: the "[000]" global section plus
 * one section per map (Misc_Data:36-115). The battle BGM / ME live in both,
 * with different metadata indices, so the parser must keep them apart.
 */
test("metadata.txt keeps the global [000] section and the per-map records", () => {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-metadata-"));
  try {
    mkdirSync(path.join(root, "PBS"), { recursive: true });
    writeFileSync(path.join(root, "PBS", "metadata.txt"), [
      "# See the documentation on the wiki",
      "[000]",
      "PlayerA = POKEMONTRAINER_Red,trchar000",
      "TrainerVictoryME = Battle victory trainer.ogg",
      "WildVictoryME = Battle victory wild.ogg",
      "TrainerBattleBGM = Battle trainer.mid",
      "WildBattleBGM = Battle wild.mid",
      "[002]",
      "# 茶月镇",
      "BattleBack = field",
      "MapPosition = 0,22,9",
      "Outdoor = true",
      "WildBattleBGM = Route 1",
      "[003]",
      "MapPosition = 0,22,9",
      "[004]",
      "Dungeon = true",
    ].join("\n"), "utf8");

    const { global, maps } = parseMetadata(root);
    assert.deepEqual(global, {
      wildBattleBGM: "Battle wild.mid",
      trainerBattleBGM: "Battle trainer.mid",
      wildVictoryME: "Battle victory wild.ogg",
      trainerVictoryME: "Battle victory trainer.ogg",
    }, "map id 0 is the global section, never a map record");
    assert.equal(maps.has(0), false);
    assert.deepEqual(maps.get(2), {
      wildBattleBGM: "Route 1",
      outdoor: true,
      mapPosition: [0, 22, 9],
      region: 0,
      battleBack: "field",
    });
    assert.deepEqual(maps.get(3), { mapPosition: [0, 22, 9], region: 0 });
    assert.equal(maps.has(4), false, "a section with no modelled key stays out");
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test("metadata.txt tolerates a missing PBS folder", () => {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-nometa-"));
  try {
    const { global, maps } = parseMetadata(root);
    assert.deepEqual(global, {});
    assert.equal(maps.size, 0, "no metadata.txt means no map is outdoor");
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test("the nature table is the 25 standard natures", () => {
  const natures = buildNatures();
  assert.equal(natures.length, 25);
  assert.deepEqual(
    { up: natures[1].statUp, down: natures[1].statDown, name: natures[1].name },
    { up: "ATTACK", down: "DEFENSE", name: "Lonely" },
  );
  assert.equal(natures[0].statUp, null, "Hardy is neutral");
});

test("the real reference project PBS parses when it is available", (t) => {
  if (!existsSync("E:/仓库/范例/929/PBS/pokemon.txt")) {
    t.skip("reference project not available in this environment");
    return;
  }
  const { counts, output } = buildPbsIr("E:/仓库/范例/929");
  // Floors, not exact counts: the game author adds species/forms over time.
  assert.ok(counts.species >= 1000, "species floor: " + counts.species);
  assert.ok(counts.forms >= 500, "forms floor: " + counts.forms);
  assert.ok(counts.moves >= 900, "moves floor: " + counts.moves);
  assert.ok(counts.items >= 900, "items floor: " + counts.items);
  assert.ok(counts.abilities >= 300, "abilities floor: " + counts.abilities);
  assert.ok(counts.types >= 18, "types floor: " + counts.types);
  // types.txt IsPseudoType / IsSpecialType (Compiler_PBS:345-346, :397/:424):
  // the reference project has exactly one pseudo type ([9] QMARKS, line 64)
  // and 12 special types.
  const typeChart = output["pbs/types.json"].types;
  assert.equal(typeChart.QMARKS.pseudoType, true);
  assert.equal(typeChart.QMARKS.specialType, false);
  assert.equal(typeChart.STELLAR.pseudoType, false);
  assert.equal(typeChart.STELLAR.specialType, false);
  assert.equal(Object.values(typeChart).filter((type) => type.pseudoType).length, 1);
  assert.equal(Object.values(typeChart).filter((type) => type.specialType).length, 12);
  assert.equal(counts.natures, 25);
  assert.ok(counts.encounters >= 100, "encounters floor: " + counts.encounters);
  assert.ok(counts.trainers >= 300, "trainers floor: " + counts.trainers);
  assert.deepEqual(output["pbs/pokemon.json"].species.BULBASAUR.types, ["GRASS", "POISON"]);
  assert.ok(output["pbs/pokemon.json"].species.EEVEE.evolutions.length >= 5);
  // R12: the battle BGM / victory ME of PBS/metadata.txt reach the runtime
  // through generated/pbs/metadata.json (PSystem_FileUtilities:546-699).
  const metadata = output["pbs/metadata.json"];
  assert.equal(metadata.kind, "pbsMetadata");
  assert.equal(metadata.global.wildBattleBGM, "Battle wild.mid");
  assert.equal(metadata.global.trainerBattleBGM, "Battle trainer.mid");
  assert.equal(metadata.global.wildVictoryME, "Battle victory wild.ogg");
  assert.equal(metadata.global.trainerVictoryME, "Battle victory trainer.ogg");
  assert.equal(metadata.total, counts.metadataMaps);
  assert.ok(counts.metadataMaps >= 100, "metadata maps floor: " + counts.metadataMaps);
  assert.equal(metadata.maps["2"].battleBack, "field");
  assert.equal(metadata.maps["2"].outdoor, true);
  assert.equal(output["pbs/index.json"].files.metadata, "pbs/metadata.json");
});

test("P2: encounters.txt maps density triples and per-method tables", () => {
  const text = [
    "# header",
    "010 # 1号道路",
    "15,8,8",
    "Land",
    "    FIDOUGH,17,22",
    "    PIKACHU,17",
    "Water",
    "    MAGIKARP,10,12",
    "OldRod",
    "    MAGIKARP,5",
  ].join("\n");
  const { byMap, total } = parseEncounters(text);
  assert.equal(total, 1);
  const map = byMap["10"];
  assert.equal(map.id, 10);
  assert.equal(map.name, "1号道路");
  assert.equal(map.densities.Land, 15);
  assert.equal(map.densities.Cave, 8);
  assert.equal(map.densities.Water, 8);
  assert.equal(map.densities.LandNight, 15, "LandNight follows the Land compile density");
  assert.deepEqual(map.methods.Land, [
    { species: "FIDOUGH", min: 17, max: 22 },
    { species: "PIKACHU", min: 17, max: 17 },
  ]);
  assert.deepEqual(map.methods.Water, [{ species: "MAGIKARP", min: 10, max: 12 }]);
  assert.deepEqual(map.methods.OldRod, [{ species: "MAGIKARP", min: 5, max: 5 }]);
});

test("P2: trainers.txt sections carry the party and its per-Pokemon fields", () => {
  const text = [
    "[NNANXIAO,南晓]",
    'LoseText = "果然还是你比较厉害啊"',
    "Items = FULLRESTORE,MAXPOTION",
    "Pokemon = SYLVEON,38",
    "    Gender = female",
    "    Moves = MOONBLAST,CALMMIND",
    "    Ability = 2",
    "    Item = LEFTOVERS",
    "    IV = 25,15,25,31,25,31",
    "    Nature = BOLD",
    "Pokemon = LUNAROSA,36",
    "    Shiny = true",
    "[NNANXIAO,南晓,1]",
    "Pokemon = BLISSEY,48",
  ].join("\n");
  const { byKey, order, total } = parseTrainers(text);
  assert.equal(total, 2);
  assert.deepEqual(order, ["NNANXIAO,南晓,0", "NNANXIAO,南晓,1"]);
  const trainer = byKey["NNANXIAO,南晓,0"];
  assert.equal(trainer.type, "NNANXIAO");
  assert.equal(trainer.name, "南晓");
  assert.equal(trainer.loseText, "果然还是你比较厉害啊");
  assert.deepEqual(trainer.items, ["FULLRESTORE", "MAXPOTION"]);
  assert.equal(trainer.party.length, 2);
  assert.deepEqual(trainer.party[0].moves, ["MOONBLAST", "CALMMIND"]);
  assert.equal(trainer.party[0].ability, "2");
  assert.equal(trainer.party[0].item, "LEFTOVERS");
  assert.equal(trainer.party[0].nature, "BOLD");
  assert.equal(trainer.party[0].gender, "female");
  assert.deepEqual(trainer.party[0].ivs, [25, 15, 25, 31, 25, 31]);
  assert.equal(trainer.party[1].level, 36);
  assert.equal(trainer.party[1].shiny, true, "the indented Shiny belongs to the second Pokemon");
  assert.equal(byKey["NNANXIAO,南晓,1"].version, 1);
});

