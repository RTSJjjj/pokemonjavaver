// Stage 3 / P0: PBS/*.txt -> generated/pbs/*.json for the Pokemon domain.
//
// The project keeps its species/move/item/ability/type/trainer data in the
// Essentials PBS text format, which mixes two dialects:
//   * INI style:   [SECTION] headers with "Key = Value" lines (pokemon.txt,
//                  pokemonforms.txt, types.txt, tm.txt, metadata.txt)
//   * CSV style:   one record per line, quoted descriptions may contain commas
//                  (moves.txt, items.txt, abilities.txt, trainertypes.txt)
// The runtime never parses the text files; the Builder normalizes them once
// into generated/pbs/, exactly like the map/rxdata pipeline.

import fs from "node:fs";
import path from "node:path";

/** Reads a PBS file or null when it (or the whole PBS/ folder) is missing. */
function readPbs(projectPath, name) {
  const file = path.join(projectPath, "PBS", name);
  try {
    return fs.readFileSync(file, "utf8");
  } catch {
    return null;
  }
}

/** Drops blank lines and full-line "#" comments. */
function contentLines(text) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line !== "" && !line.startsWith("#"));
}

/** P0: INI dialect -> [{ header, fields: Map<lowerKey,string>, raw: [...] }]. */
export function parseIniSections(text) {
  const sections = [];
  let current = null;
  for (const line of contentLines(text)) {
    const header = /^\[(.+)\]$/.exec(line);
    if (header) {
      current = { header: header[1].trim(), fields: new Map(), order: [] };
      sections.push(current);
      continue;
    }
    if (!current) continue;
    const keyValue = /^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/.exec(line);
    if (!keyValue) continue;
    const key = keyValue[1].toLowerCase();
    const value = keyValue[2].trim();
    if (!current.fields.has(key)) current.order.push(key);
    current.fields.set(key, value);
  }
  return sections;
}

/** P0: CSV dialect -> array of field arrays (quote aware, per line). */
export function parseCsvRecords(text) {
  const records = [];
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (line === "" || line.startsWith("#")) continue;
    const fields = [];
    let field = "";
    let quoted = false;
    for (let i = 0; i < line.length; i++) {
      const ch = line[i];
      if (quoted) {
        if (ch === '"') {
          if (line[i + 1] === '"') {
            field += '"';
            i++;
          } else {
            quoted = false;
          }
        } else {
          field += ch;
        }
      } else if (ch === '"') {
        quoted = true;
      } else if (ch === ",") {
        fields.push(field.trim());
        field = "";
      } else {
        field += ch;
      }
    }
    fields.push(field.trim());
    records.push(fields);
  }
  return records;
}

const list = (value) => (value ? value.split(",").map((x) => x.trim()).filter(Boolean) : []);
const numList = (value) => list(value).map(Number);
const intOrNull = (value) => {
  const n = Number(value);
  return value !== undefined && value !== "" && Number.isFinite(n) ? Math.trunc(n) : null;
};
const floatOrNull = (value) => {
  const n = Number(value);
  return value !== undefined && value !== "" && Number.isFinite(n) ? n : null;
};
const at = (record, index) => (record[index] === undefined ? "" : record[index]);

/** Moves learnset "1,TACKLE,3,GROWL" -> [{ level, move }]. */
function parseMoveset(value) {
  const tokens = list(value);
  const moves = [];
  for (let i = 0; i + 1 < tokens.length; i += 2) {
    const level = Number(tokens[i]);
    if (Number.isFinite(level)) moves.push({ level, move: tokens[i + 1] });
  }
  return moves;
}

/** Methods are CamelCase (Level, HappinessDay); species/parameters are UPPER. */
const isMethodToken = (token) => /^[A-Z][a-z]/.test(token);

/**
 * Evolutions "IVYSAUR,Level,16,CLEFABLE,Item,MOONSTONE" -> triples.
 *
 * Some methods take no parameter (Trade, HappinessDay, HappinessNight), and
 * both a parameter and the next species are ALL-UPPER, so one token of
 * lookahead decides: after the method, "X Y" means X is a species (the method
 * is parameterless) when Y is another method, otherwise X is the parameter.
 */
function parseEvolutions(value) {
  const tokens = list(value);
  const evolutions = [];
  let i = 0;
  while (i < tokens.length) {
    const species = tokens[i++];
    if (i >= tokens.length) {
      evolutions.push({ species, method: "@raw", parameter: "" });
      break;
    }
    const method = tokens[i++];
    let parameter = null;
    if (i < tokens.length) {
      const next = tokens[i];
      const after = tokens[i + 1];
      const nextIsParameter = !isMethodToken(next)
        && (i + 1 >= tokens.length || !isMethodToken(after));
      if (nextIsParameter) {
        parameter = next;
        i++;
      }
    }
    evolutions.push({ species, method, parameter });
  }
  return evolutions;
}

/** pokemon.txt -> species keyed by INTERNALNAME (plus the dex id). */
export function parsePokemon(text) {
  const species = {};
  const byId = {};
  for (const section of parseIniSections(text)) {
    const id = intOrNull(section.header);
    if (id === null) continue;
    const f = (key) => section.fields.get(key);
    const internalName = f("internalname");
    if (!internalName) continue;
    const entry = {
      id,
      internalName,
      name: f("name") || internalName,
      types: [f("type1"), f("type2")].filter(Boolean),
      baseStats: numList(f("basestats")),
      genderRate: f("genderrate") || "Unknown",
      growthRate: f("growthrate") || "Medium",
      baseExp: intOrNull(f("baseexp")) || 0,
      effortPoints: numList(f("effortpoints")),
      rareness: intOrNull(f("rareness")) || 0,
      happiness: intOrNull(f("happiness")) || 0,
      abilities: list(f("abilities")),
      hiddenAbility: f("hiddenability") || null,
      moves: parseMoveset(f("moves")),
      eggMoves: list(f("eggmoves")),
      compatibility: list(f("compatibility")),
      stepsToHatch: intOrNull(f("stepstohatch")) || 0,
      height: floatOrNull(f("height")),
      weight: floatOrNull(f("weight")),
      color: f("color") || null,
      shape: intOrNull(f("shape")),
      habitat: f("habitat") || null,
      kind: f("kind") || null,
      pokedex: f("pokedex") || null,
      regionalNumbers: numList(f("regionalnumbers")),
      evolutions: parseEvolutions(f("evolutions")),
      wildItems: {
        common: f("wilditemcommon") || null,
        uncommon: f("wilditemuncommon") || null,
        rare: f("wilditemrare") || null,
      },
      formName: f("formname") || null,
      incense: f("incense") || null,
      battler: {
        playerX: intOrNull(f("battlerplayerx")),
        playerY: intOrNull(f("battlerplayery")),
        enemyX: intOrNull(f("battlerenemyx")),
        enemyY: intOrNull(f("battlerenemyy")),
        shadowX: intOrNull(f("battlershadowx")),
        shadowSize: intOrNull(f("battlershadowsize")),
      },
    };
    species[internalName] = entry;
    byId[String(id)] = internalName;
  }
  return { species, byId };
}

/**
 * pokemonforms.txt -> alternate forms keyed by SPECIES_FORM (the Essentials
 * internal naming used by tm.txt and the scripts). Only the keys a form may
 * override are carried; absent ones inherit the base species.
 */
export function parsePokemonForms(text) {
  const forms = {};
  for (const section of parseIniSections(text)) {
    const parts = section.header.split(",").map((x) => x.trim());
    const species = parts[0];
    const form = Number(parts[1]);
    if (!species || !Number.isFinite(form)) continue;
    const f = (key) => section.fields.get(key);
    const entry = { species, form, key: `${species}_${form}`, formName: f("formname") || null };
    if (f("basestats")) entry.baseStats = numList(f("basestats"));
    if (f("type1")) entry.types = [f("type1"), f("type2")].filter(Boolean);
    if (f("abilities")) entry.abilities = list(f("abilities"));
    if (f("hiddenability")) entry.hiddenAbility = f("hiddenability");
    if (f("height")) entry.height = floatOrNull(f("height"));
    if (f("weight")) entry.weight = floatOrNull(f("weight"));
    if (f("color")) entry.color = f("color");
    if (f("pokedex")) entry.pokedex = f("pokedex");
    if (f("megastone")) entry.megaStone = f("megastone");
    if (f("unmegaform")) entry.unmegaForm = intOrNull(f("unmegaform"));
    if (f("moves")) entry.moves = parseMoveset(f("moves"));
    if (f("evolutions")) entry.evolutions = parseEvolutions(f("evolutions"));
    entry.battler = {
      playerY: intOrNull(f("battlerplayery")),
      enemyY: intOrNull(f("battlerenemyy")),
    };
    forms[entry.key] = entry;
  }
  return forms;
}

/** moves.txt CSV -> moves keyed by INTERNALNAME. */
export function parseMoves(text) {
  const moves = {};
  for (const record of parseCsvRecords(text)) {
    const id = intOrNull(at(record, 0));
    const internalName = at(record, 1);
    if (id === null || !internalName) continue;
    moves[internalName] = {
      id,
      internalName,
      name: at(record, 2) || internalName,
      function: at(record, 3) || null,
      power: intOrNull(at(record, 4)) || 0,
      type: at(record, 5) || null,
      category: at(record, 6) || null,
      accuracy: intOrNull(at(record, 7)) || 0,
      pp: intOrNull(at(record, 8)) || 0,
      effectChance: intOrNull(at(record, 9)) || 0,
      target: at(record, 10) || null,
      priority: intOrNull(at(record, 11)) || 0,
      flags: at(record, 12) || "",
      description: at(record, 13) || "",
    };
  }
  return moves;
}

/** items.txt CSV -> items keyed by INTERNALNAME (pocket/price/uses). */
export function parseItems(text) {
  const items = {};
  for (const record of parseCsvRecords(text)) {
    const id = intOrNull(at(record, 0));
    const internalName = at(record, 1);
    if (id === null || !internalName) continue;
    items[internalName] = {
      id,
      internalName,
      name: at(record, 2) || internalName,
      namePlural: at(record, 3) || at(record, 2) || internalName,
      pocket: intOrNull(at(record, 4)) || 0,
      price: intOrNull(at(record, 5)) || 0,
      description: at(record, 6) || "",
      fieldUse: intOrNull(at(record, 7)),
      battleUse: intOrNull(at(record, 8)),
      extra: record.slice(9).filter((x) => x !== ""),
    };
  }
  return items;
}

/** abilities.txt CSV -> abilities keyed by INTERNALNAME. */
export function parseAbilities(text) {
  const abilities = {};
  for (const record of parseCsvRecords(text)) {
    const id = intOrNull(at(record, 0));
    const internalName = at(record, 1);
    if (id === null || !internalName) continue;
    abilities[internalName] = {
      id,
      internalName,
      name: at(record, 2) || internalName,
      description: at(record, 3) || "",
    };
  }
  return abilities;
}

/** types.txt INI -> type chart keyed by INTERNALNAME. */
export function parseTypes(text) {
  const types = {};
  for (const section of parseIniSections(text)) {
    const id = intOrNull(section.header);
    const internalName = section.fields.get("internalname");
    if (id === null || !internalName) continue;
    types[internalName] = {
      id,
      internalName,
      name: section.fields.get("name") || internalName,
      weaknesses: list(section.fields.get("weaknesses")),
      resistances: list(section.fields.get("resistances")),
      immunities: list(section.fields.get("immunities")),
    };
  }
  return types;
}

/** trainertypes.txt CSV -> trainer classes keyed by INTERNALNAME. */
export function parseTrainerTypes(text) {
  const trainerTypes = {};
  for (const record of parseCsvRecords(text)) {
    const id = intOrNull(at(record, 0));
    const internalName = at(record, 1);
    if (id === null || !internalName) continue;
    trainerTypes[internalName] = {
      id,
      internalName,
      name: at(record, 2) || internalName,
      baseMoney: intOrNull(at(record, 3)) || 0,
      skill: at(record, 4) || null,
      fields: record.slice(5),
    };
  }
  return trainerTypes;
}

// tm.txt is "[MOVE]" followed by a raw CSV line, which the INI reader would
// treat as an unknown key. Parse it line by line instead.
export function parseTmCompatibility(text) {
  const compatibility = {};
  let move = null;
  for (const line of contentLines(text)) {
    const header = /^\[(.+)\]$/.exec(line);
    if (header) {
      move = header[1].trim();
      compatibility[move] = [];
      continue;
    }
    if (move) compatibility[move] = list(line);
  }
  return compatibility;
}

/**
 * Natures live in the Essentials scripts, not in PBS (the project has no
 * natures.txt). The 25 standard natures with their raised/lowered stat are
 * emitted as data so the runtime owns one source of truth.
 */
export function buildNatures() {
  const stats = ["ATTACK", "DEFENSE", "SPEED", "SPATK", "SPDEF"];
  const table = [
    ["Hardy", null, null], ["Lonely", "ATTACK", "DEFENSE"], ["Brave", "ATTACK", "SPEED"],
    ["Adamant", "ATTACK", "SPATK"], ["Naughty", "ATTACK", "SPDEF"],
    ["Bold", "DEFENSE", "ATTACK"], ["Docile", null, null], ["Relaxed", "DEFENSE", "SPEED"],
    ["Impish", "DEFENSE", "SPATK"], ["Lax", "DEFENSE", "SPDEF"],
    ["Timid", "SPEED", "ATTACK"], ["Hasty", "SPEED", "DEFENSE"], ["Serious", null, null],
    ["Jolly", "SPEED", "SPATK"], ["Naive", "SPEED", "SPDEF"],
    ["Modest", "SPATK", "ATTACK"], ["Mild", "SPATK", "DEFENSE"], ["Quiet", "SPATK", "SPEED"],
    ["Bashful", null, null], ["Rash", "SPATK", "SPDEF"],
    ["Calm", "SPDEF", "ATTACK"], ["Gentle", "SPDEF", "DEFENSE"], ["Sassy", "SPDEF", "SPEED"],
    ["Careful", "SPDEF", "SPATK"], ["Quirky", null, null],
  ];
  return table.map(([name, up, down], id) => ({
    id,
    internalName: name.toUpperCase(),
    name,
    statUp: up,
    statDown: down,
  }));
}

/** True when any PBS file exists (a project may not use PBS at all). */
export function hasPbs(projectPath) {
  try {
    return fs.statSync(path.join(projectPath, "PBS")).isDirectory();
  } catch {
    return false;
  }
}

/**
 * Builds every generated/pbs/*.json document. Always returns all keys so the
 * converter can register a stable output list; a missing source file yields an
 * empty document (projects without PBS stay valid).
 */
export function buildPbsIr(projectPath) {
  const read = (name) => {
    const text = readPbs(projectPath, name);
    return text === null ? "" : text;
  };
  const pokemon = parsePokemon(read("pokemon.txt"));
  const forms = parsePokemonForms(read("pokemonforms.txt"));
  const moves = parseMoves(read("moves.txt"));
  const items = parseItems(read("items.txt"));
  const abilities = parseAbilities(read("abilities.txt"));
  const types = parseTypes(read("types.txt"));
  const trainerTypes = parseTrainerTypes(read("trainertypes.txt"));
  const natures = buildNatures();
  const tm = parseTmCompatibility(read("tm.txt"));

  const counts = {
    species: Object.keys(pokemon.species).length,
    forms: Object.keys(forms).length,
    moves: Object.keys(moves).length,
    items: Object.keys(items).length,
    abilities: Object.keys(abilities).length,
    types: Object.keys(types).length,
    trainerTypes: Object.keys(trainerTypes).length,
    natures: natures.length,
    tmMoves: Object.keys(tm).length,
  };

  const output = {
    "pbs/index.json": { format: "pokemon-builder/pbs/1", kind: "pbsIndex", counts,
      files: {
        pokemon: "pbs/pokemon.json", forms: "pbs/pokemonforms.json", moves: "pbs/moves.json",
        items: "pbs/items.json", abilities: "pbs/abilities.json", types: "pbs/types.json",
        trainerTypes: "pbs/trainertypes.json", natures: "pbs/natures.json", tm: "pbs/tm.json",
      } },
    "pbs/pokemon.json": { format: "pokemon-builder/pbs/1", kind: "pbsPokemon",
      total: counts.species, byId: pokemon.byId, species: pokemon.species },
    "pbs/pokemonforms.json": { format: "pokemon-builder/pbs/1", kind: "pbsPokemonForms",
      total: counts.forms, forms },
    "pbs/moves.json": { format: "pokemon-builder/pbs/1", kind: "pbsMoves", total: counts.moves, moves },
    "pbs/items.json": { format: "pokemon-builder/pbs/1", kind: "pbsItems", total: counts.items, items },
    "pbs/abilities.json": { format: "pokemon-builder/pbs/1", kind: "pbsAbilities", total: counts.abilities, abilities },
    "pbs/types.json": { format: "pokemon-builder/pbs/1", kind: "pbsTypes", total: counts.types, types },
    "pbs/trainertypes.json": { format: "pokemon-builder/pbs/1", kind: "pbsTrainerTypes",
      total: counts.trainerTypes, trainerTypes },
    "pbs/natures.json": { format: "pokemon-builder/pbs/1", kind: "pbsNatures", total: counts.natures, natures },
    "pbs/tm.json": { format: "pokemon-builder/pbs/1", kind: "pbsTm", total: counts.tmMoves, compatibility: tm },
  };
  return { output, counts };
}
