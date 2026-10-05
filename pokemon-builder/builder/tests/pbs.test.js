// Stage 3 / P0: PBS text -> generated/pbs/*.json parser tests.
//
// The real PBS folder is a live project input, so the unit tests use synthetic
// text (every dialect detail: quoted commas, parameterless evolution methods,
// missing trailing fields) and one optional floor test against the reference
// project when it is checked out next to the builder.

import test from "node:test";
import assert from "node:assert/strict";
import { existsSync } from "node:fs";

import {
  buildNatures,
  buildPbsIr,
  parseAbilities,
  parseCsvRecords,
  parseIniSections,
  parseItems,
  parsePokemon,
  parsePokemonForms,
  parseTmCompatibility,
  parseTrainerTypes,
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

  const trainerTypes = parseTrainerTypes("0,POKEMONTRAINER_Red,训练家,60,Battle trainer,,,Male,,\n");
  assert.equal(trainerTypes.POKEMONTRAINER_Red.name, "训练家");
  assert.equal(trainerTypes.POKEMONTRAINER_Red.baseMoney, 60);
  assert.equal(trainerTypes.POKEMONTRAINER_Red.skill, "Battle trainer");
});

test("types.txt and tm.txt keep the type chart and TM compatibility", () => {
  const types = parseTypes(`
[0]
Name = 普通
InternalName = NORMAL
Weaknesses = FIGHTING
Immunities = GHOST
`);
  assert.deepEqual(types.NORMAL.immunities, ["GHOST"]);
  assert.deepEqual(types.NORMAL.weaknesses, ["FIGHTING"]);

  const tm = parseTmCompatibility("[MEGAHORN]\nABSOL,BOUFFALANT\n[BUGBUZZ]\nBUTTERFREE\n");
  assert.deepEqual(tm.MEGAHORN, ["ABSOL", "BOUFFALANT"]);
  assert.deepEqual(tm.BUGBUZZ, ["BUTTERFREE"]);
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
  assert.equal(counts.natures, 25);
  assert.deepEqual(output["pbs/pokemon.json"].species.BULBASAUR.types, ["GRASS", "POISON"]);
  assert.ok(output["pbs/pokemon.json"].species.EEVEE.evolutions.length >= 5);
});
