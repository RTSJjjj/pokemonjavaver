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

import { marshalLoad } from "../../builder/src/marshal.js";

/**
 * Data/berry_plants.dat: the per-item berry growth table
 * {@code [hoursPerStage, dryingPerHour, minYield, maxYield]} indexed by item id
 * (PField_BerryPlants#pbGetBerryPlantData). Missing entries fall back to
 * [3,15,2,5] at runtime, so an unreadable file simply yields an empty table.
 */
function buildBerryPlants(projectPath) {
  const file = path.join(projectPath, "Data", "berry_plants.dat");
  if (!fs.existsSync(file)) return {};
  let data;
  try {
    data = marshalLoad(fs.readFileSync(file));
  } catch {
    return {};
  }
  const byId = {};
  if (Array.isArray(data)) {
    data.forEach((value, id) => {
      if (value) byId[id] = value;
    });
  }
  return byId;
}

/**
 * The marshal reader hands strings back as raw bytes (latin1), so the UTF-8
 * payload of the project's Chinese texts has to be re-decoded. Already valid
 * text (ASCII, or a future UTF-8 reader) is returned unchanged.
 */
function marshalText(value) {
  if (value === null || value === undefined) return "";
  const text = String(value);
  const decoded = Buffer.from(text, "latin1").toString("utf8");
  return decoded.includes("\uFFFD") ? text : decoded;
}

/**
 * Data/town_map.dat + PBS/townmap.txt: the region map behind pbShowMap
 * (PScreen_RegionMap). The .dat is {@code sections[region] = [nil, filename,
 * points]} (Compiler_PBS#pbCompileTownMap:92); a point is {@code [x, y,
 * placeName, placeDescription, healMapId, healX, healY, switchId]} from the
 * CSV schema "uussUUUU" (Compiler_PBS:57). Region names are not in the .dat -
 * the compiler puts them into MessageTypes::RegionNames from the txt "Name="
 * lines (Compiler_PBS:80,93), so the names are read from the same text file.
 */
function buildTownMap(projectPath) {
  const regions = [];
  const datFile = path.join(projectPath, "Data", "town_map.dat");
  if (fs.existsSync(datFile)) {
    let data = null;
    try {
      data = marshalLoad(fs.readFileSync(datFile));
    } catch {
      data = null;
    }
    if (Array.isArray(data)) {
      for (const section of data) {
        if (!Array.isArray(section)) {
          regions.push(null);
          continue;
        }
        const points = Array.isArray(section[2]) ? section[2] : [];
        regions.push({
          filename: marshalText(section[1]),
          points: points.filter(Array.isArray).map((point) => ({
            x: Number(point[0]) || 0,
            y: Number(point[1]) || 0,
            name: marshalText(point[2]),
            description: marshalText(point[3]),
            healMapId: point[4] === null || point[4] === undefined ? null : Number(point[4]),
            healX: point[5] === null || point[5] === undefined ? null : Number(point[5]),
            healY: point[6] === null || point[6] === undefined ? null : Number(point[6]),
            switchId: point[7] === null || point[7] === undefined ? null : Number(point[7]),
          })),
        });
      }
    }
  }
  const names = [];
  const text = readPbs(projectPath, "townmap.txt");
  if (text !== null) {
    let current = -1;
    for (const raw of text.split(/\r?\n/)) {
      const line = raw.trim();
      const header = /^\[(\d+)\]$/.exec(line);
      if (header) {
        current = Number(header[1]);
        continue;
      }
      const name = /^Name\s*=\s*(.*)$/.exec(line);
      if (name && current >= 0) names[current] = name[1].trim();
    }
  }
  const merged = regions.map((region, index) => region === null
    ? null
    : { name: names[index] || "", filename: region.filename, points: region.points });
  return { regions: merged, total: merged.filter(Boolean).length };
}

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

// Compiler:368-376 csvBoolean! (schema letter "b", Compiler:542-543) is the
// plugin's own boolean reader: it tests the loose regexes below, in this order,
// and raises on anything else. "1", "true", "yes", "y" -> true; "0", "false",
// "no", "n" -> false. The builder must never abort a whole build over one
// malformed flag, so an unrecognised token keeps the documented default
// (false) instead of raising.
const CSV_BOOLEAN_TRUE = /^1|[Tt][Rr][Uu][Ee]|[Yy][Ee][Ss]|[Yy]$/;
const CSV_BOOLEAN_FALSE = /^0|[Ff][Aa][Ll][Ss][Ee]|[Nn][Oo]|[Nn]$/;
const booleanField = (value) => {
  if (value === undefined || value === null) return false;
  const field = String(value);
  if (CSV_BOOLEAN_TRUE.test(field)) return true;
  if (CSV_BOOLEAN_FALSE.test(field)) return false;
  return false;
};

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
    if (f("megamove")) entry.megaMove = f("megamove");
    if (f("megamessage")) entry.megaMessage = intOrNull(f("megamessage"));   // 0 = default message, 1 = Rayquaza's
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
      // Compiler_PBS:551-573 "vnssuusuuUN": column 9 is ITEM_TYPE
      // (PItem_Items:12), the field pbIsPokeBall? / pbIsBerry? / pbIsMail?
      // read; column 10 is the machine's move (TM/HM).
      type: intOrNull(at(record, 9)),
      machine: at(record, 10) || null,
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
      // Compiler_PBS:340-350: types[id] = [id, Name, InternalName,
      // IsPseudoType, IsSpecialType, Weaknesses, Resistances, Immunities]
      // ("IsPseudoType" => [3,"b"], "IsSpecialType" => [4,"b"]), both optional
      // booleans that default to false (Compiler_PBS:365).
      // Compiler_PBS:397/420-422 collect the pseudo types: every id with
      // IsPseudoType=true plus every id missing from types.txt (the compiler's
      // types.compact! drops the nil slots, so a gap in the numbering is a
      // pseudo type too). Compiler_PBS:424 collects the special types from
      // IsSpecialType only. PBTypes.isPseudoType?/isSpecialType?
      // (PBTypes_Extra:34-40) read those two lists.
      pseudoType: booleanField(section.fields.get("ispseudotype")),
      specialType: booleanField(section.fields.get("isspecialtype")),
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
      // The CSV schema is "unsUSSSeUS" (Compiler_PBS:1374): 4 = battle BGM
      // (pbGetTrainerBattleBGM:627), 5 = victory ME (pbGetTrainerVictoryME:676),
      // 6 = intro ME (pbPlayTrainerIntroME:612), 7 = gender.
      battleBgm: at(record, 4) || null,
      victoryMe: at(record, 5) || null,
      introMe: at(record, 6) || null,
      fields: record.slice(5),
    };
  }
  return trainerTypes;
}

/** EncounterTypes::Names order (PField_Encounters). */
export const ENCOUNTER_METHODS = [
  "Land", "Cave", "Water", "RockSmash", "OldRod", "GoodRod", "SuperRod",
  "HeadbuttLow", "HeadbuttHigh", "LandMorning", "LandDay", "LandNight", "BugContest",
];
/** EncounterTypes::EnctypeDensities (defaults when the map has no entry). */
const ENCOUNTER_DENSITIES = [25, 10, 10, 0, 0, 0, 0, 0, 0, 25, 25, 25, 25];
/** EncounterTypes::EnctypeCompileDens: 1=Land, 2=Cave, 3=Water, 0=own. */
const ENCOUNTER_COMPILE_DENS = [1, 2, 3, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1];

/**
 * P2: encounters.txt -> { byMap, total }. Every map is
 * {@code { id, name, densities, methods }} where densities is keyed by method
 * name (the file only stores Land/Cave/Water; the others follow
 * EnctypeCompileDens / EnctypeDensities) and methods maps a method name to
 * [{ species, min, max }].
 */
export function parseEncounters(text) {
  const byMap = {};
  let current = null;
  let method = null;
  for (const rawLine of (text || "").split(/\r?\n/)) {
    const line = rawLine.replace(/^\uFEFF/, "").trim();
    if (line === "" || line.startsWith("#")) continue;
    const header = /^(\d+)(?:\s*#\s*(.*))?$/.exec(line);
    if (header) {
      const id = Number(header[1]);
      current = { id, name: header[2] ? header[2].trim() : "", densities: null, methods: {} };
      byMap[id] = current;
      method = null;
      continue;
    }
    if (!current) continue;
    const density = /^(\d+)\s*,\s*(\d+)\s*,\s*(\d+)$/.exec(line);
    if (density && current.densities === null) {
      current.densities = {
        Land: Number(density[1]), Cave: Number(density[2]), Water: Number(density[3]),
      };
      continue;
    }
    if (ENCOUNTER_METHODS.includes(line)) {
      method = line;
      current.methods[method] = [];
      continue;
    }
    if (method) {
      const parts = line.split(",").map((part) => part.trim());
      const species = parts[0];
      const min = Number(parts[1]);
      if (species && Number.isFinite(min)) {
        const max = parts.length > 2 ? Number(parts[2]) : min;
        current.methods[method].push({
          species, min, max: Number.isFinite(max) ? max : min,
        });
      }
    }
  }
  const maps = {};
  for (const [key, map] of Object.entries(byMap)) {
    const base = map.densities
      || { Land: ENCOUNTER_DENSITIES[0], Cave: ENCOUNTER_DENSITIES[1], Water: ENCOUNTER_DENSITIES[2] };
    const densities = {};
    ENCOUNTER_METHODS.forEach((name, index) => {
      const compile = ENCOUNTER_COMPILE_DENS[index];
      densities[name] = compile === 1 ? base.Land
        : compile === 2 ? base.Cave
          : compile === 3 ? base.Water : ENCOUNTER_DENSITIES[index];
    });
    maps[key] = { id: map.id, name: map.name, densities, methods: map.methods };
  }
  return { byMap: maps, total: Object.keys(maps).length };
}

/**
 * P2: trainers.txt -> { byKey, order, total }. A section is
 * {@code [TYPE,Name(,version)]}; "Pokemon = SPECIES,level" starts a party
 * member whose indented keys (Moves/Item/IV/EV/Ability/Nature/Form/Shiny/
 * Ball/Gender/Happiness/SuperShiny) adjust it.
 */
export function parseTrainers(text) {
  const byKey = {};
  const order = [];
  let current = null;
  let pokemon = null;
  for (const rawLine of (text || "").split(/\r?\n/)) {
    const line = rawLine.replace(/^\uFEFF/, "");
    const trimmed = line.trim();
    if (trimmed === "" || trimmed.startsWith("#")) continue;
    const header = /^\[(.+)\]$/.exec(trimmed);
    if (header) {
      const parts = header[1].split(",").map((part) => part.trim());
      const type = parts[0];
      const name = parts[1] || "";
      const version = parts.length > 2 ? Number(parts[2]) : 0;
      const key = `${type},${name},${version}`;
      current = { key, type, name, version, loseText: null, items: [], party: [] };
      byKey[key] = current;
      order.push(key);
      pokemon = null;
      continue;
    }
    if (!current) continue;
    const keyValue = /^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/.exec(trimmed);
    if (!keyValue) continue;
    const key = keyValue[1];
    const value = keyValue[2].trim();
    if (key === "Pokemon") {
      pokemon = { species: value.split(",")[0].trim(), level: 1, moves: [], ivs: null, evs: null };
      const level = intOrNull(value.split(",")[1]);
      if (level !== null) pokemon.level = level;
      current.party.push(pokemon);
      continue;
    }
    const indented = /^\s/.test(line);
    if (!indented) {
      if (key === "LoseText") current.loseText = value.replace(/^"|"$/g, "");
      else if (key === "Items") current.items = list(value);
      continue;
    }
    if (!pokemon) continue;
    applyTrainerPokemonField(pokemon, key, value);
  }
  return { byKey, order, total: order.length };
}

function applyTrainerPokemonField(pokemon, key, value) {
  switch (key) {
    case "Moves": pokemon.moves = list(value); break;
    case "Item": pokemon.item = value; break;
    case "IV": pokemon.ivs = numList(value); break;
    case "EV": pokemon.evs = numList(value); break;
    case "Ability": pokemon.ability = value; break;
    case "Nature": pokemon.nature = value; break;
    case "Form": pokemon.form = intOrNull(value); break;
    case "Shiny": pokemon.shiny = value === "true"; break;
    case "SuperShiny": pokemon.superShiny = value === "true"; break;
    case "Ball": pokemon.ball = value; break;
    case "Gender": pokemon.gender = value; break;
    case "Happiness": pokemon.happiness = intOrNull(value); break;
    default: break;
  }
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

/**
 * PBS/metadata.txt (Misc_Data:36-115, {@code PokemonMetadata}): the "[000]"
 * global section plus one section per map.
 *
 * <p>Only the keys this runtime models are kept, and they are kept as the raw
 * strings the file holds, because {@code pbStringToAudioFile} (Audio_Play:1-14)
 * parses "file:volume:pitch" at play time and RGSS resolves the extension
 * itself: the four lookups of PSystem_FileUtilities:546-699
 * ({@code pbGetWildBattleBGM} / {@code pbGetWildVictoryME} /
 * {@code pbGetTrainerBattleBGM} / {@code pbGetTrainerVictoryME}) plus
 * {@code pbGetWildCaptureME} (:585), and the five per-map fields the map IR
 * already consumed (Outdoor / SnapEdges / MapPosition / BattleBack).</p>
 *
 * <p>The other keys of {@code NonGlobalTypes} (HealingSpot, Weather, DiveMap,
 * DarkMap, SafariMap, Dungeon, MapSize, Environment, ShowArea, Bicycle,
 * BicycleAlways) and of {@code GlobalTypes} (Home, SurfBGM, BicycleBGM,
 * PlayerA-H) are not modelled yet.</p>
 *
 * @returns {{ global: object, maps: Map<number, object> }} map id 0 is the
 *   global section ({@code pbGetMetadata(0,...)}), never a map record.
 */
export function parseMetadata(projectPath) {
  const global = {};
  const maps = new Map();
  const text = readPbs(projectPath, "metadata.txt");
  if (text === null) return { global, maps };
  // The same key names mean different metadata indices in the two dialects:
  // "WildBattleBGM" is MetadataWildBattleBGM (2) under "[000]" and
  // MetadataMapWildBattleBGM (14) under a map section (Misc_Data:74-115).
  const audioKeys = ["wildBattleBGM", "trainerBattleBGM", "wildVictoryME",
    "trainerVictoryME", "wildCaptureME"];
  for (const section of parseIniSections(text)) {
    if (!/^\d+$/.test(section.header)) continue;
    const id = Number(section.header);
    const isGlobal = id === 0;
    const record = isGlobal ? global : maps.get(id) || {};
    for (const key of audioKeys) {
      const value = section.fields.get(key.toLowerCase());
      if (value) record[key] = value;
    }
    if (!isGlobal) {
      const flag = (key) => {
        const value = section.fields.get(key);
        return value === undefined ? undefined
          : value.toLowerCase() === "true" || value === "1" || value.toLowerCase() === "yes";
      };
      const outdoor = flag("outdoor");
      if (outdoor !== undefined) record.outdoor = outdoor;
      const snapEdges = flag("snapedges");
      if (snapEdges !== undefined) record.snapEdges = snapEdges;
      const position = section.fields.get("mapposition");
      if (position !== undefined) {
        const parts = position.split(",").map((value) => Number(value.trim()));
        if (parts.length >= 3 && parts.every((value) => Number.isInteger(value) && value >= 0)) {
          record.mapPosition = parts.slice(0, 3);
          // MetadataMapPosition's first field is the region of the region map.
          record.region = parts[0];
        }
      }
      const battleBack = section.fields.get("battleback");
      if (battleBack) record.battleBack = battleBack;
      // MetadataEnvironment (NonGlobalTypes, Misc_Data:113): the battle's
      // environment, which pbGetEnvironment (PField_Battles:194-217) starts
      // from, and which pbPrepareBattle:181-183 reads to make Dusk Balls work
      // in caves.
      const environment = section.fields.get("environment");
      if (environment) record.environment = environment;
      // A section that only carries keys this runtime does not model yet stays
      // out of the file, so "total" counts the maps that actually say
      // something.
      if (Object.keys(record).length > 0) maps.set(id, record);
    }
  }
  return { global, maps };
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
  const encounters = parseEncounters(read("encounters.txt"));
  const trainers = parseTrainers(read("trainers.txt"));

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
    encounters: encounters.total,
    trainers: trainers.total,
  };

  const berryPlants = buildBerryPlants(projectPath);
  const townMap = buildTownMap(projectPath);
  const metadata = parseMetadata(projectPath);

  counts.townMapRegions = townMap.total;
  counts.metadataMaps = metadata.maps.size;

  const output = {
    "pbs/index.json": { format: "pokemon-builder/pbs/1", kind: "pbsIndex", counts,
      files: {
        pokemon: "pbs/pokemon.json", forms: "pbs/pokemonforms.json", moves: "pbs/moves.json",
        items: "pbs/items.json", abilities: "pbs/abilities.json", types: "pbs/types.json",
        trainerTypes: "pbs/trainertypes.json", natures: "pbs/natures.json", tm: "pbs/tm.json",
        encounters: "pbs/encounters.json", trainers: "pbs/trainers.json",
        berryPlants: "pbs/berryplants.json", townMap: "pbs/townmap.json",
        metadata: "pbs/metadata.json",
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
    "pbs/encounters.json": { format: "pokemon-builder/pbs/1", kind: "pbsEncounters",
      total: counts.encounters, byMap: encounters.byMap },
    "pbs/trainers.json": { format: "pokemon-builder/pbs/1", kind: "pbsTrainers",
      total: counts.trainers, order: trainers.order, trainers: trainers.byKey },
    "pbs/berryplants.json": { format: "pokemon-builder/pbs/1", kind: "pbsBerryPlants",
      total: Object.keys(berryPlants).length, byId: berryPlants },
    "pbs/townmap.json": { format: "pokemon-builder/pbs/1", kind: "pbsTownMap",
      total: townMap.total, regions: townMap.regions },
    "pbs/metadata.json": { format: "pokemon-builder/pbs/1", kind: "pbsMetadata",
      total: metadata.maps.size,
      // "[000]": the global section (pbGetMetadata(0,...)).
      global: metadata.global,
      // Map id -> record; a key is absent when the file does not define it,
      // which is exactly when pbGetMetadata returns nil.
      maps: Object.fromEntries([...metadata.maps.entries()].sort((a, b) => a[0] - b[0])),
    },
  };
  return { output, counts };
}
