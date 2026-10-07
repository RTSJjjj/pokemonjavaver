// Essentials script -> IR compiler (project3 sections 4, 21-23, 78, 79).
//
// The Builder already parsed every event script block (generated/scripts/
// blocks.json carries category, apis, calls and the Ruby source). This module
// turns those blocks into Debug IR: the runtime only ever sees commands like
// {"command":"GIVE_ITEM","item":"ORANBERRY","amount":1}, never Ruby.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

export const IR_VERSION = "pokemon-builder/script-ir/1";

/** Splits an argument list on top level commas (keeps [] and "..." intact). */
export function splitArguments(text) {
  const parts = [];
  let depth = 0;
  let quote = null;
  let current = "";
  for (const char of text) {
    if (quote) {
      current += char;
      if (char === quote) quote = null;
      continue;
    }
    if (char === '"' || char === "'") {
      quote = char;
      current += char;
      continue;
    }
    if (char === "[" || char === "(") depth++;
    if (char === "]" || char === ")") depth--;
    if (char === "," && depth === 0) {
      parts.push(current.trim());
      current = "";
      continue;
    }
    current += char;
  }
  if (current.trim().length > 0) parts.push(current.trim());
  return parts;
}

/** Ruby literal -> JSON value; anything else stays as an explicit SCRIPT arg. */
export function parseValue(text) {
  const value = text.trim();
  if (value.startsWith(":")) return value.slice(1);
  // R8: Essentials constants (PBItems::TOWNMAP) resolve to the internal name;
  // the runtime looks the id up in its own PBS data.
  const constant = /^(?:PBItems|PBSpecies|PBTypes|PBAbilities|PBMoves|PBTrainers|PBStatuses)::([A-Za-z_]\w*)$/.exec(value);
  if (constant) return constant[1];
  if (/^-?\d+$/.test(value)) return Number(value);
  if (value === "true" || value === "false") return value === "true";
  if (value === "nil") return null;
  if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
    return value.slice(1, -1);
  }
  if (value.startsWith("[") && value.endsWith("]")) {
    return splitArguments(value.slice(1, -1)).map(parseValue);
  }
  // _INTL("a", "b") / _I("a") wrap user facing text; join the literal parts so
  // SHOW_TEXT receives plain text instead of a script fragment.
  const intl = value.match(/^_I(?:NTL)?\((.*)\)$/s);
  if (intl) {
    const parts = splitArguments(intl[1]).map(parseValue);
    const literal = parts.filter((part) => typeof part === "string");
    if (literal.length > 0) {
      return literal.join("");
    }
  }
  return { script: value };
}

/** Arguments of a single-call block, already parsed (legacy helper). */
export function callArguments(block) {
  const statements = callStatements(block);
  if (statements.length !== 1) return null;
  return statementArguments(statements[0]);
}

/**
 * R6.19: splits a block into top level statements. A block may hold several
 * API calls (quest chains, several self switches); line breaks inside
 * parentheses, brackets or strings belong to the same statement.
 */
export function callStatements(block) {
  const source = (block.rubySource || "").trim();
  const statements = [];
  let current = "";
  let depth = 0;
  let quote = null;
  for (let i = 0; i < source.length; i++) {
    const ch = source[i];
    if (quote) {
      current += ch;
      if (ch === "\\") {
        current += source[++i] || "";
      } else if (ch === quote) {
        quote = null;
      }
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      current += ch;
      continue;
    }
    if (ch === "#") { // comment: drop it up to the end of the line
      while (i < source.length && source[i] !== "\n") i++;
      continue;
    }
    if (ch === "(" || ch === "[" || ch === "{") {
      depth++;
    } else if (ch === ")" || ch === "]" || ch === "}") {
      depth--;
    }
    if (ch === "\n" && depth <= 0) {
      if (current.trim()) statements.push(current.trim());
      current = "";
      continue;
    }
    current += ch;
  }
  if (current.trim()) statements.push(current.trim());
  return statements;
}

/** Call name of one statement, or null when it is not a bare call. */
function statementName(statement) {
  const open = statement.indexOf("(");
  const head = (open < 0 ? statement : statement.slice(0, open)).trim();
  return /^[A-Za-z_]\w*[!?]?$/.test(head) ? head : null;
}

/** Parsed arguments of one call statement, or null when it is not a call. */
function statementArguments(statement) {
  if (statementName(statement) === null) return null;
  const open = statement.indexOf("(");
  if (open < 0) return [];
  if (!statement.endsWith(")")) return null;
  if (!closesAtEnd(statement, open)) return null;
  return splitArguments(statement.slice(open + 1, -1)).map(parseValue);
}

/** True when the parenthesis opened at {@code open} closes on the last character. */
function closesAtEnd(source, open) {
  let depth = 0;
  let quote = null;
  for (let i = open; i < source.length; i++) {
    const ch = source[i];
    if (quote) {
      if (ch === "\\") {
        i++;
      } else if (ch === quote) {
        quote = null;
      }
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      continue;
    }
    if (ch === "(") {
      depth++;
    } else if (ch === ")") {
      depth--;
      if (depth === 0) return i === source.length - 1;
    }
  }
  return false;
}

/**
 * R6.19: true when a handler's IR still carries an unresolved Ruby expression.
 * {@code parseValue} marks those as {@code {script: "..."}}; emitting them
 * would hand the runtime a value it cannot read (and older builds crashed on
 * {@code ir.getBoolean} for such objects), so the block is reported instead.
 */
export function containsScriptPayload(value) {
  if (value === null || typeof value !== "object") return false;
  if (typeof value.script === "string") return true;
  if (Array.isArray(value)) return value.some(containsScriptPayload);
  return Object.values(value).some(containsScriptPayload);
}

/**
 * Essentials API -> IR handler (project3 section 23). Only APIs that exist in
 * the stage 1 audit are listed; everything else is reported instead of guessed.
 */
/** `:Quest1` / "Quest1" / Quest1 -> "Quest1" (script args arrive as literals). */
function questName(value) {
  if (value === null || value === undefined) return "";
  const text = String(value).trim();
  return text.startsWith(":") ? text.slice(1) : text;
}

/**
 * R6.30: character addressing used by pbExclaim and friends. A literal number
 * passes through; Essentials' {@code get_character(N)} becomes N. Anything
 * else (a variable, an array) is handed back untouched - the IR then carries
 * an unresolved {@code {script:...}} payload and the block is reported instead
 * of guessed.
 */
function characterReference(value) {
  if (typeof value === "number") return value;
  if (value && typeof value === "object" && typeof value.script === "string") {
    const match = value.script.trim().match(/^get_character\(\s*(-?\d+)\s*\)$/);
    if (match) return Number(match[1]);
  }
  return value;
}

/**
 * R6.30: the project plays cries with a fixed two-statement pair
 * ({@code cry = pbCryFile(251)}, {@code pbSEPlay(cry) if cry}). A lone
 * assignment is not a sound by itself, so only the pair is rewritten into a
 * synthetic {@code pbCryFile(251)} call that the normal statement handler
 * turns into one PLAY_CRY step. The pair may appear several times and
 * anywhere in the block (self switch first, two cries in a row, ...).
 */
function rewriteCryStatements(statements) {
  const assignment = /^([A-Za-z_]\w*)\s*=\s*pbCryFile\(\s*((?::[A-Za-z_]\w*)|-?\d+)\s*\)$/;
  const rewritten = [];
  for (let i = 0; i < statements.length; i++) {
    const match = statements[i].match(assignment);
    if (match) {
      const variable = match[1];
      const play = new RegExp(`^pbSEPlay\\(\\s*${variable}\\s*\\)\\s+if\\s+${variable}$`);
      if (i + 1 < statements.length && play.test(statements[i + 1])) {
        rewritten.push(`pbCryFile(${match[2]})`);
        i++;
        continue;
      }
    }
    rewritten.push(statements[i]);
  }
  return rewritten;
}

/**
 * P1: {@code $PokemonBag.pbStoreItem(...)} / {@code $PokemonBag.pbDeleteItem(...)}
 * are the global {@code pbStoreItem} / {@code pbDeleteItem} (PItem_Bag delegates
 * to {@code $PokemonBag}), so the receiver can be dropped before translation.
 */
function rewriteGlobalReceivers(statements) {
  return statements.map((statement) =>
    statement
      .replace(/^\s*\$PokemonBag\.(pbStoreItem|pbDeleteItem)\s*\(/, "$1(")
      // R8: the boss reward module calls (Boss_reward:80-346).
      .replace(/^\s*BossRewards\.(pokemon_reward|blissey|gholdengo_money)\s*$/, "$1()"));
}

/** The statements of one block, after the project rewrites (R6.30 / P1). */
function blockStatements(block) {
  return rewriteGlobalReceivers(rewriteCryStatements(callStatements(block)));
}

/**
 * PField_Battles:359-361: pbWildBattle's optional arguments are battle rules -
 * {@code setBattleRule("outcomeVar",n) if outcomeVar!=1},
 * {@code setBattleRule("cannotRun") if !canRun},
 * {@code setBattleRule("canLose") if canLose}. Only the arguments the script
 * actually wrote reach the IR; the runtime keeps the defaults (outcome variable
 * 1, can run, cannot lose).
 */
function addWildBattleOptions(command, args, from) {
  // setBattleRule("outcomeVar",outcomeVar) if outcomeVar!=1 (:359): the default
  // 1 is not written as a rule, so it stays out of the IR too.
  if (args.length > from && args[from] !== null && args[from] !== undefined) {
    const outcomeVar = Number(args[from]);
    if (Number.isFinite(outcomeVar) && outcomeVar !== 1) command.outcomeVar = outcomeVar;
  }
  // setBattleRule("cannotRun") if !canRun (:360): only a false canRun is a rule.
  if (args.length > from + 1 && args[from + 1] === false) {
    command.canRun = false;
  }
  // setBattleRule("canLose") if canLose (:361).
  if (args.length > from + 2 && args[from + 2] === true) {
    command.canLose = true;
  }
  return command;
}

export const HANDLERS = {
  pbItemBall(args) {
    return { command: "GIVE_ITEM", item: args[0], amount: args.length > 1 ? args[1] : 1 };
  },
  pbReceiveItem(args) {
    return { command: "GIVE_ITEM", item: args[0], amount: args.length > 1 ? args[1] : 1 };
  },
  pbDeleteItem(args) {
    return { command: "REMOVE_ITEM", item: args[0], amount: args.length > 1 ? args[1] : 1 };
  },
  pbSetSelfSwitch(args) {
    const ir = {
      command: "SET_SELF_SWITCH",
      eventId: args[0],
      channel: args[1],
      value: args.length > 2 ? args[2] : true,
    };
    // pbSetSelfSwitch(event, channel, value, mapid = -1)
    if (args.length > 3) {
      ir.mapId = args[3];
    }
    return ir;
  },
  setTempSwitchOn(args) {
    return { command: "SET_TEMP_SWITCH", channel: args[0], value: true };
  },
  setTempSwitchOff(args) {
    return { command: "SET_TEMP_SWITCH", channel: args[0], value: false };
  },
  pbTrainerIntro(args) {
    return { command: "TRAINER_INTRO", trainerType: args[0] };
  },
  pbTrainerEnd() {
    return { command: "TRAINER_END" };
  },
  // ---- player graphics (the intro's gender selector) ----
  /** The BWGenderSelector plugin: picks a gender and applies pbChangePlayer. */
  pbGenderSelector() {
    return { command: "GENDER_SELECTOR" };
  },
  pbPokeCenterPC() { return { command: "OPEN_PC" }; },
  pbChoosePokemonForTrade(args) {
    if (!Number.isInteger(args[0]) || args[0] < 1 || !Number.isInteger(args[1]) || args[1] < 1 || typeof args[2] !== "string")
      throw new Error("Trade selection requires variable ids and a species");
    return { command: "CHOOSE_TRADE", variable: args[0], nameVariable: args[1], wanted: args[2] };
  },
  pbStartTrade(args) {
    const value = arg => {
      if (!arg || typeof arg !== "object" || !arg.script) return arg;
      const variable = /^pbGet\(\s*(\d+)\s*\)$/.exec(arg.script);
      if (variable) return { variable: Number(variable[1]) };
      if (/^[a-z_]\w*$/.test(arg.script)) return { local: arg.script };
      throw new Error("Unsupported trade argument: " + arg.script);
    };
    if (args.length < 4 || typeof args[2] !== "string" || typeof args[3] !== "string") throw new Error("Trade requires literal nickname and trainer name");
    return { command: "START_TRADE", index: value(args[0]), offered: value(args[1]), nickname: args[2], trainerName: args[3] };
  },
  /** pbChangePlayer(id) sets the walking graphic from metadata PlayerA+id. */
  pbChangePlayer(args) {
    return { command: "CHANGE_PLAYER", playerId: args[0] };
  },
  // ---- P2: wild / trainer battles ----
  /**
   * pbWildBattle(species, level, outcomeVar=1, canRun=true, canLose=false)
   * (PField_Battles:351-368): the three optional arguments become battle rules
   * (setBattleRule) and are consumed when the battle starts; the decision lands
   * in the outcome variable (variable 1 by default).
   */
  pbWildBattle(args) {
    const command = {
      command: "WILD_BATTLE",
      species: args[0],
      level: args.length > 1 ? args[1] : 5,
    };
    addWildBattleOptions(command, args, 2);
    return command;
  },
  /** pbFreeWildBattle(species, level) is a wild battle from a species. */
  pbFreeWildBattle(args) {
    const command = {
      command: "WILD_BATTLE",
      species: args[0],
      level: args.length > 1 ? args[1] : 5,
    };
    addWildBattleOptions(command, args, 2);
    return command;
  },
  /** pbTrainerBattle(type, name[, version]) loads trainers.txt and battles. */
  pbTrainerBattle(args) {
    return {
      command: "TRAINER_BATTLE",
      trainerType: args[0],
      trainerName: args.length > 1 ? args[1] : "",
      version: args.length > 2 ? args[2] : 0,
      partner: false,
    };
  },
  /**
   * pbDoubleTrainerBattle(type1, name1, party1, speech1, type2, name2, party2=0, speech2=nil, canLose=false,
   * outcomeVar=1) (PField_Battles:582-596): two opposing trainers; setBattleRule("double") is part of the call.
   */
  pbDoubleTrainerBattle(args) {
    const command = {
      command: "TRAINER_BATTLE",
      trainerType: args[0],
      trainerName: args.length > 1 ? args[1] : "",
      version: args.length > 2 ? args[2] : 0,
      partner: false,
      second: {
        trainerType: args[4],
        trainerName: args.length > 5 ? args[5] : "",
        version: args.length > 6 ? args[6] : 0,
      },
      double: true,
    };
    if (args.length > 8 && args[8] === true) command.canLose = true;
    if (args.length > 9) command.outcomeVar = args[9];
    return command;
  },

  /**
   * pbTripleTrainerBattle(type1, name1, party1, speech1, type2, name2, party2, speech2, type3, name3, party3=0,
   * speech3=nil, canLose=false, outcomeVar=1) (PField_Battles:598-614): three opposing trainers and
   * setBattleRule("triple").
   */
  pbTripleTrainerBattle(args) {
    const command = {
      command: "TRAINER_BATTLE",
      trainerType: args[0],
      trainerName: args.length > 1 ? args[1] : "",
      version: args.length > 2 ? args[2] : 0,
      partner: false,
      second: {
        trainerType: args[4],
        trainerName: args.length > 5 ? args[5] : "",
        version: args.length > 6 ? args[6] : 0,
      },
      third: {
        trainerType: args[8],
        trainerName: args.length > 9 ? args[9] : "",
        version: args.length > 10 ? args[10] : 0,
      },
      triple: true,
    };
    if (args.length > 12 && args[12] === true) command.canLose = true;
    if (args.length > 13) command.outcomeVar = args[13];
    return command;
  },

  /**
   * PField_Battles:60-77: value-taking rules consume the next argument
   * (terrain/weather/environment/environ/backdrop/battleback/base/outcome/
   * outcomevar); everything else is a flag.
   */
  setBattleRule(args) {
    const valueRules = new Set(["terrain", "weather", "environment", "environ",
      "backdrop", "battleback", "base", "outcome", "outcomevar"]);
    const rules = [];
    for (let i = 0; i < args.length; i++) {
      const rule = args[i];
      if (typeof rule !== "string") throw new Error("setBattleRule expects literal rule names");
      if (valueRules.has(rule.toLowerCase())) {
        if (i + 1 >= args.length) throw new Error("setBattleRule " + rule + " needs a value");
        rules.push({ rule, value: args[++i] });
      } else {
        rules.push({ rule });
      }
    }
    return { command: "BATTLE_RULE", rules };
  },
  pbBridgeOn() {
    return { command: "SET_BRIDGE", on: true };
  },
  pbBridgeOff() {
    return { command: "SET_BRIDGE", on: false };
  },
  pbSmashThisEvent() {
    return { command: "ERASE_EVENT" };
  },
  // ---- plugin batch 1: Quests, key items, following Pokemon (R6.12+1) ----
  activateQuest(args) {
    return { command: "ACTIVATE_QUEST", quest: questName(args[0]) };
  },
  advanceQuestToStage(args) {
    return {
      command: "ADVANCE_QUEST_TO_STAGE",
      quest: questName(args[0]),
      stage: args.length > 1 ? args[1] : null,
    };
  },
  completeQuest(args) {
    return { command: "COMPLETE_QUEST", quest: questName(args[0]) };
  },
  pbGetKeyItem(args) {
    return {
      command: "GIVE_KEY_ITEM",
      item: args[0],
      amount: args.length > 1 ? args[1] : 1,
    };
  },
  pbToggleFollowingPokemon(args) {
    return { command: "TOGGLE_FOLLOWING_POKEMON", forced: args.length > 0 ? args[0] : null };
  },
  pbPokemonFollow(args) {
    return { command: "POKEMON_FOLLOW", target: args.length > 0 ? args[0] : null };
  },
  pbPokemonMart(args) {
    const items = Array.isArray(args[0]) ? args[0] : args;
    return { command: "OPEN_MART", items };
  },
  // ---- R8: the boss reward system (Boss_reward:3-348) ----
  boss_reward(args) {
    return { command: "BOSS_REWARD", rank: args.length > 0 ? args[0] : 5 };
  },
  /**
   * R8: the single-call form {@code pbAddPokemon(:TURTWIG,5)}
   * (PSystem_PokemonUtilities:67-83); the Pokemon construction scripts pass a
   * local instead and are handled by the P0c dialect.
   */
  pbAddPokemon(args) {
    if (typeof args[0] !== "string" || typeof args[1] !== "number") {
      throw new Error("pbAddPokemon needs a species literal and a level here");
    }
    return { command: "GIVE_POKEMON", species: args[0], level: args[1] };
  },
  pokemon_reward() {
    return { command: "BOSS_POKEMON_REWARD" };
  },
  blissey() {
    return { command: "BOSS_BLISSEY" };
  },
  gholdengo_money() {
    return { command: "BOSS_GHOLDENGO_MONEY" };
  },
  // ---- R8: partner trainers (PField_Field:1399-1440) ----
  pbRegisterPartner(args) {
    return {
      command: "REGISTER_PARTNER",
      trainerType: args[0],
      name: args.length > 1 ? args[1] : "",
      partyId: args.length > 2 ? args[2] : 0,
    };
  },
  pbDeregisterPartner() {
    return { command: "DEREGISTER_PARTNER" };
  },
  pbCut() {
    return { command: "FIELD_MOVE", move: "CUT" };
  },
  pbRockSmash() {
    return { command: "FIELD_MOVE", move: "ROCK_SMASH" };
  },
  pbMessage(args) {
    return { command: "SHOW_TEXT", text: args[0] };
  },
  pbShowMap(args) {
    // PScreen_RegionMap:431: pbShowMap(region=-1, wallmap=true).
    return {
      command: "SHOW_MAP",
      region: args.length > 0 ? args[0] : -1,
      wallmap: args.length > 1 ? args[1] : true,
    };
  },
  // ---- handler batch 1: field audio / waits / cave transitions (R6.26) ----
  pbSEPlay(args) {
    return {
      command: "PLAY_SE",
      name: args[0],
      volume: args.length > 1 ? args[1] : 100,
      pitch: args.length > 2 ? args[2] : 100,
    };
  },
  pbWait(args) {
    // pbWait counts frames at the project's 40 fps (0168.rb).
    return { command: "WAIT", frames: args[0] };
  },
  pbCaveEntrance() {
    return { command: "CAVE_ENTRANCE", exiting: false };
  },
  pbCaveExit() {
    return { command: "CAVE_ENTRANCE", exiting: true };
  },
  // ---- handler batch 2: cries / exclaim / boulders / floating plates (R6.30) ----
  pbCryFile(args) {
    // Numeric species ids resolve to the numeric cry files; a symbol is only
    // resolvable by name at runtime (the project's files are numeric, so a
    // symbol cry stays silent until the species data lands in stage 3).
    return { command: "PLAY_CRY", species: args[0] };
  },
  pbExclaim(args) {
    return {
      command: "EXCLAIM",
      character: characterReference(args[0]),
      animationId: args.length > 1 ? args[1] : 3, // EXCLAMATION_ANIMATION_ID
      tinting: args.length > 2 ? args[2] : false,
    };
  },
  /**
   * L6: {@code pbNoticePlayer(event)} - the event notices the player: an
   * exclamation when they are not already facing each other, the player turns
   * toward the event and the event walks up to the player. The facing check
   * needs both characters, so it is evaluated by the runtime port.
   */
  pbNoticePlayer(args) {
    return {
      command: "NOTICE_PLAYER",
      character: characterReference(args[0]),
    };
  },
  /**
   * L6: {@code pbSave} - the project's quiet save (PScreen_Save writes the
   * single save file with no UI). The runtime maps it to the quick slot, the
   * same one the F5/F9 shortcuts use.
   */
  pbSave() {
    return { command: "SAVE_GAME" };
  },
  pbPushThisBoulder() {
    return { command: "PUSH_BOULDER" };
  },
  /** P2: a RockSmash encounter (no probability roll; the smashed rock decides). */
  pbRockSmashRandomEncounter() {
    return { command: "ROCK_SMASH_ENCOUNTER" };
  },
  toggle_liefeng_switches() {
    return { command: "TOGGLE_PLATE_SWITCHES" };
  },
  // ---- P1: party / bag / PokeCenter data handlers ----
  /**
   * P1: {@code pbSetPokemonCenter} stores the current map + player position as
   * the respawn point ({@code $PokemonGlobal.pokecenter*}); the runtime reads
   * the live position, so the IR carries no arguments.
   */
  pbSetPokemonCenter() {
    return { command: "SET_POKEMON_CENTER" };
  },
  /**
   * P1: {@code pbStoreItem(item, qty=1)} is the silent bag add
   * ({@code $PokemonBag.pbStoreItem}); it shares GIVE_ITEM with pbItemBall.
   */
  pbStoreItem(args) {
    return { command: "GIVE_ITEM", item: args[0], amount: args.length > 1 ? args[1] : 1 };
  },
  /**
   * P1: {@code myAddEgg(species, text="")} creates a level 1 egg, names it and
   * adds it to the party (or the PC when the party is full).
   */
  myAddEgg(args) {
    return {
      command: "ADD_EGG",
      species: args[0],
      text: args.length > 1 ? args[1] : null,
    };
  },
  // ---- P3: berry plants (PField_BerryPlants:313-590) ----
  /**
   * {@code pbBerryPlant} plays the whole plot interaction (plant / water /
   * harvest); the runtime's BERRY_PLANT stepper carries every message and
   * number from the plugin.
   */
  pbBerryPlant() {
    return { command: "BERRY_PLANT" };
  },
  /**
   * {@code pbPickBerry(berry,qty=1)}: a preset ripe plot, picked through the
   * same stepper (BERRY_PICK) which resets the soil and sets self switch A
   * (PField_BerryPlants:555-590).
   */
  pbPickBerry(args) {
    return {
      command: "BERRY_PICK",
      berry: args[0],
      qty: args.length > 1 ? args[1] : 1,
    };
  },
};

/** Simple conditions the runtime can already answer (R6.1). */
const SIMPLE_CONDITION = /^\$game_(switches|variables)\[\d+\][^;]*$/;

/**
 * Essentials APIs that need the Pokemon runtime or its UI (stage 3 domain:
 * party, bag, berry planting, battles, map/PC screens). They are not "unknown":
 * project3 section 25 maps them to JAVA_HANDLER_REQUIRED so R8 can emit stubs.
 */
const DOMAIN_APIS = new Set([
  "pbGenPkmn", "pbAddPokemon", "pbAddPokemonSilent", "pbChoosePokemon", "pbChoosePokemonForTrade",
  "pbTrainerBattle", "pbDoubleTrainerBattle", "pbTripleTrainerBattle", "pbWildBattle", "pbFreeWildBattle", "pbTrainerIntro",
  "setBattleRule", "pbStartTrade",
  "pbBerryPlant", "pbPickBerry", "pbStoreItem", "pbGetKeyItem", "pbDeleteItem",
  "pbPokeCenterPC", "pbShowMap", "pbSetPokemonCenter",
  "pbToggleFollowingPokemon", "pbRegisterPartner", "pbDeregisterPartner",
  "pbSet", "push", "myAddEgg", "pbCrystalWarp",
]);

/**
 * P0c: methods of one Pokemon in the construction scripts
 * ({@code p = pbGenPkmn(...); p.makeShiny; p.setAbility(1); p.calcStats}).
 */
const POKEMON_METHODS = new Set([
  "makeShiny", "makeSuperShiny", "makeFemale", "makeMale", "calcStats",
  "setAbility", "setItem", "setNature", "pbLearnMove", "pbRecordFirstMoves",
  "giveRibbon",
]);
/** P0c: assignable Pokemon fields ({@code p.iv = [...]}, {@code p.form = 1}). */
const POKEMON_PROPERTIES = new Set([
  "iv", "ev", "form", "ot", "name", "battleRank", "shiny", "item", "nature",
  "happiness", "stepsToHatch",
  // R8: the gift scripts also set the trainer id, the OT gender and the ball.
  "trainerID", "otgender", "ballused",
]);

/**
 * P0c: a value used by the Pokemon construction scripts. Literals keep the
 * {@link parseValue} shape; a few Ruby expressions become typed IR values the
 * runtime resolves without a general script translator:
 * {@code pbGet(n)} / {@code $game_variables[n]} -> {@code {variable:n}},
 * {@code $Trainer.pokemonCount} -> {@code {trainerPokemonCount:true}},
 * a local Pokemon or one of its fields -> {@code {local:"p",property:"iv"}}.
 */
function parseScriptValue(text, locals) {
  const value = String(text).trim();
  const variable = /^(?:pbGet\(\s*(-?\d+)\s*\)|\$game_variables\[\s*(-?\d+)\s*\])$/.exec(value);
  if (variable) {
    return { variable: Number(variable[1] !== undefined ? variable[1] : variable[2]) };
  }
  if (/^\$Trainer\.pokemonCount$/.test(value)) {
    return { trainerPokemonCount: true };
  }
  // PokeBattle_Trainer:177/192: the dex counters used by pbSet.
  const trainerStat = /^\$Trainer\.(pokedexSeen|pokedexOwned)$/.exec(value);
  if (trainerStat) {
    return { trainerStat: trainerStat[1] };
  }
  const localProperty = /^([A-Za-z_]\w*)\.([A-Za-z_]\w*)$/.exec(value);
  if (localProperty && locals.get(localProperty[1]) === "pokemon") {
    return { local: localProperty[1], property: localProperty[2] };
  }
  if (locals.get(value) === "pokemon") {
    return { local: value };
  }
  if (/^[A-Za-z_]\w*$/.test(value) && locals.has(value)) {
    // P0d: a plain value local (`count = $Trainer.pokemonCount`) or the loop
    // variable of a compiled `for`; the runtime reads it back by name.
    return { local: value };
  }
  return parseValue(value);
}

/**
 * P0c: translates one statement of a Pokemon construction script, or null when
 * the statement is not part of that dialect. {@code {todo}} means "this is a
 * stage 3 statement we cannot translate yet" (battle/trade terminators, loops),
 * which keeps the whole block in the JAVA_HANDLER_REQUIRED bucket.
 */
function pokemonStatement(statement, locals) {
  const text = statement.trim();
  let match = /^([A-Za-z_]\w*)\s*=\s*pbGenPkmn\(\s*:([A-Za-z_]\w*)\s*,\s*(-?\d+)\s*\)$/.exec(text);
  if (!match) {
    match = /^([A-Za-z_]\w*)\s*=\s*PokeBattle_Pokemon\.new\(\s*:([A-Za-z_]\w*)\s*,\s*(-?\d+)\s*(?:,\s*[^)]*)?\)$/.exec(text);
  }
  if (match) {
    locals.set(match[1], "pokemon");
    return { ir: { command: "POKEMON_CREATE", local: match[1], species: match[2], level: Number(match[3]) } };
  }
  match = /^([A-Za-z_]\w*)\.([A-Za-z_]\w*)\s*(?:\((.*)\))?$/s.exec(text);
  if (match && POKEMON_METHODS.has(match[2]) && locals.get(match[1]) === "pokemon") {
    const raw = match[3] === undefined || match[3].trim() === "" ? [] : splitArguments(match[3]);
    const args = raw.map((argument) => parseScriptValue(argument, locals));
    if (args.some(containsScriptPayload)) {
      return { todo: true };
    }
    return { ir: { command: "POKEMON_CALL", local: match[1], action: match[2], args } };
  }
  match = /^([A-Za-z_]\w*)\.([A-Za-z_]\w*)\s*=\s*(.+)$/s.exec(text);
  if (match && POKEMON_PROPERTIES.has(match[2]) && locals.get(match[1]) === "pokemon") {
    const value = parseScriptValue(match[3], locals);
    if (containsScriptPayload(value)) {
      return { todo: true };
    }
    return { ir: { command: "POKEMON_SET", local: match[1], property: match[2], value } };
  }
  match = /^([A-Za-z_]\w*)\s*=\s*(.+)$/s.exec(text);
  if (match) {
    const value = parseScriptValue(match[2], locals);
    if (containsScriptPayload(value)) {
      return { todo: true };
    }
    locals.set(match[1], "value");
    return { ir: { command: "LOCAL_SET", local: match[1], value } };
  }
  match = /^pbAddPokemon(Silent)?\(\s*([A-Za-z_]\w*)\s*(?:,\s*[^)]*)?\)$/.exec(text);
  if (match && locals.get(match[2]) === "pokemon") {
    return { ir: { command: "PARTY_ADD", local: match[2], silent: Boolean(match[1]) } };
  }
  const freeWild = /^pbFreeWildBattle\(\s*([A-Za-z_]\w*)\s*((?:,\s*[^,()]+)*)\)$/.exec(text);
  if (freeWild && locals.get(freeWild[1]) === "pokemon") {
    // FreeWildBattle:2-8: pbFreeWildBattle(pkmn, outcomeVar=1, canRun=true,
    // canLose=false) - the same three optional arguments as pbWildBattle.
    const command = { command: "FREE_WILD_BATTLE", local: freeWild[1] };
    const extra = (freeWild[2] || "").split(",").slice(1)
      .map((arg) => arg.trim()).filter((arg) => arg !== "")
      .map((arg) => {
        if (arg === "true") return true;
        if (arg === "false") return false;
        if (arg === "nil") return null;
        const number = Number(arg);
        return Number.isFinite(number) ? number : arg;
      });
    return { ir: addWildBattleOptions(command, extra, 0) };
  }
  match = /^pbSet\(\s*([^,]+?)\s*,\s*(.+)\)$/s.exec(text);
  if (match) {
    const id = parseScriptValue(match[1], locals);
    const value = parseScriptValue(match[2], locals);
    if (containsScriptPayload(id) || containsScriptPayload(value)) {
      return { todo: true };
    }
    return { ir: { command: "SET_VARIABLE", id, value } };
  }
  return null;
}

/**
 * R8: the $Trainer assignments of the badge / dex scripts
 * ({@code $Trainer.badges[1] = true}, {@code $Trainer.pokedex = true},
 * {@code $Trainer.pokepc = true}) become their own IR commands.
 */
function trainerStatement(statement) {
  const text = statement.trim();
  let match = /^\$Trainer\.badges\[\s*(\d+)\s*\]\s*=\s*(true|false)$/.exec(text);
  if (match) {
    return { ir: { command: "SET_BADGE", badge: Number(match[1]), value: match[2] === "true" } };
  }
  match = /^\$Trainer\.(pokedex|pokepc)\s*=\s*(true|false)$/.exec(text);
  if (match) {
    return { ir: { command: "SET_TRAINER_FLAG", flag: match[1], value: match[2] === "true" } };
  }
  return null;
}

/**
 * P0c: translates one statement of a Pokemon construction script, falling back
 * to the flat HANDLERS table for the plain API calls that appear next to the
 * local variables ({@code pbSEPlay}, {@code pbWait}, ...).
 * @returns {{ir: object} | {todo: true} | {error: string} | null}
 */
function translateStatement(statement, locals, block) {
  const pokemon = pokemonStatement(statement, locals);
  if (pokemon) return pokemon;
  const trainer = trainerStatement(statement);
  if (trainer) return trainer;
  const stepName = statementName(statement);
  const args = statementArguments(statement);
  const stepHandler = stepName === null ? null : HANDLERS[stepName];
  if (args === null || typeof stepHandler !== "function") {
    return null;
  }
  try {
    return { ir: stepHandler(args, block) };
  } catch (error) {
    return { error: error.message };
  }
}

/**
 * P0d: the event-glue dialect of the catch scripts - local assignments,
 * {@code pbSet} / {@code pbSEPlay} / {@code pbWait} and the Ruby counted loop
 * {@code for i in <from>..<to> ... end} (the ball-shake block of every catch
 * common event). A loop becomes one REPEAT IR step whose body may wait
 * (pbWait) between iterations; {@code ...} is the exclusive range.
 * @returns {Array<object>|null} the steps, or null when a statement is outside
 *          this dialect (the whole block stays in the Java-handler bucket)
 */
function compileGlueStatements(statements, locals, block) {
  const steps = [];
  for (let i = 0; i < statements.length; i++) {
    const text = statements[i].trim();
    const loop = /^for\s+([A-Za-z_]\w*)\s+in\s+(.+?)\.\.(\.)?(.+)$/.exec(text);
    if (loop) {
      let depth = 1;
      let end = i + 1;
      for (; end < statements.length; end++) {
        const candidate = statements[end].trim();
        if (/^for\b/.test(candidate)) depth++;
        else if (candidate === "end") {
          depth--;
          if (depth === 0) break;
        }
      }
      if (end >= statements.length) return null; // unbalanced loop
      const exclusive = loop[3] !== undefined;
      locals.set(loop[1], "value");
      const body = compileGlueStatements(statements.slice(i + 1, end), locals, block);
      if (body === null || body.length === 0) return null;
      const from = parseScriptValue(loop[2], locals);
      const to = parseScriptValue(loop[4], locals);
      if (containsScriptPayload(from) || containsScriptPayload(to)) return null;
      const repeat = { command: "REPEAT", local: loop[1], from, to, steps: body };
      if (exclusive) repeat.exclusive = true;
      steps.push(repeat);
      i = end;
      continue;
    }
    if (text === "end") return null; // stray `end`
    const outcome = translateStatement(statements[i], locals, block);
    if (outcome === null || outcome.todo || outcome.error !== undefined) return null;
    if (containsScriptPayload(outcome.ir)) return null;
    steps.push(outcome.ir);
  }
  return steps;
}

/**
 * Compiles one block.
 * @returns {{id, source, category, status, ir?, apis, reason?}}
 */
export function compileBlock(block) {
  const name = block.calls && block.calls[0] ? block.calls[0].name : (block.apis && block.apis[0]) || null;
  const entry = {
    id: block.id,
    source: {
      mapId: block.mapId ?? null,
      eventId: block.eventId ?? null,
      page: block.page ?? null,
      commandIndex: block.commandIndex ?? null,
      file: block.file ?? null,
    },
    category: block.category,
    apis: block.apis || [],
    rubySource: block.rubySource || "",
  };
  const source = (block.rubySource || "").trim();

  if (block.category === "SIMPLE_EXPRESSION") {
    if (SIMPLE_CONDITION.test(source)) {
      return { ...entry, status: "TRANSLATED", ir: { command: "CONDITION", expression: source } };
    }
    return { ...entry, status: "UNSUPPORTED", reason: "complex expression needs the script translator" };
  }
  const handler = name ? HANDLERS[name] : null;
  // A block whose leading call is a stage 3 domain API stays in the Java-handler
  // bucket even when a later statement cannot translate (section 25).
  const blockIsDomain = name !== null && DOMAIN_APIS.has(name);
  // P0c: Pokemon construction scripts carry local variables and method calls
  // (`p = pbGenPkmn(:X, 5); p.makeShiny; p.iv = [...]; pbAddPokemon(p, 1)`),
  // the one dialect the flat HANDLERS table cannot express. A block that builds
  // a Pokemon - or only touches game variables through pbSet / pbGet - gets its
  // own pass; any statement it cannot translate keeps the block in the stage 3
  // "needs a Java handler" bucket.
  const statements = blockStatements(block);
  if (statements.some((statement) =>
    /pbGenPkmn\s*\(|PokeBattle_Pokemon\.new\s*\(|pbSet\s*\(|\$Trainer\.(?:badges|pokedex|pokepc|pokedexSeen|pokedexOwned)/.test(statement))) {
    if (statements.length === 0) {
      return { ...entry, status: "JAVA_HANDLER_REQUIRED", reason: "needs the Pokemon runtime (stage 3 domain)" };
    }
    const locals = new Map();
    const steps = compileGlueStatements(statements, locals, block);
    if (steps === null) {
      return { ...entry, status: "JAVA_HANDLER_REQUIRED", reason: "needs the Pokemon runtime (stage 3 domain)" };
    }
    const ir = steps.length === 1 ? steps[0] : { command: "SEQUENCE", steps };
    return { ...entry, status: "TRANSLATED", ir };
  }
  if (handler) {
    // R6.19: a block may hold several statements. Every one of them has to
    // translate on its own; the resulting IR is a SEQUENCE the runtime plays
    // step by step (a waiting step such as SHOW_TEXT resumes the rest).
    // R6.30: the project's cry pair is rewritten first.
    if (statements.length === 0) {
      return { ...entry, status: "UNSUPPORTED", reason: "empty script block" };
    }
    const steps = [];
    for (const statement of statements) {
      const stepName = statementName(statement);
      const args = statementArguments(statement);
      const stepHandler = stepName === null ? null : HANDLERS[stepName];
      if (args === null || typeof stepHandler !== "function") {
        // A statement inside the stage 3 domain keeps the whole block in the
        // "needs a Java handler" bucket instead of "unsupported".
        if (blockIsDomain || (stepName !== null && DOMAIN_APIS.has(stepName))) {
          return {
            ...entry,
            status: "JAVA_HANDLER_REQUIRED",
            reason: "needs the Pokemon runtime (stage 3 domain)",
          };
        }
        return {
          ...entry,
          status: "UNSUPPORTED",
          reason: stepName === null
            ? `statement is not a simple API call: ${statement.slice(0, 40)}`
            : `no handler for ${stepName} in "${statement.slice(0, 40)}"`,
        };
      }
      try {
        const stepIr = stepHandler(args, block);
        if (containsScriptPayload(stepIr)) {
          // A stage 3 domain API whose argument is not a literal still needs a
          // Java handler (section 25); only a non-domain API is "unsupported".
          if (blockIsDomain || (stepName !== null && DOMAIN_APIS.has(stepName))) {
            return {
              ...entry,
              status: "JAVA_HANDLER_REQUIRED",
              reason: "needs the Pokemon runtime (stage 3 domain)",
            };
          }
          return {
            ...entry,
            status: "UNSUPPORTED",
            reason: "argument is not a literal; needs a Java handler",
          };
        }
        steps.push(stepIr);
      } catch (error) {
        return { ...entry, status: "UNSUPPORTED", reason: `handler failed: ${error.message}` };
      }
    }
    const ir = steps.length === 1 ? steps[0] : { command: "SEQUENCE", steps };
    return { ...entry, status: "TRANSLATED", ir };
  }
  if (block.category === "PLUGIN_API") {
    return { ...entry, status: "JAVA_HANDLER_REQUIRED", reason: "project plugin API" };
  }
  if (name && DOMAIN_APIS.has(name)) {
    return { ...entry, status: "JAVA_HANDLER_REQUIRED", reason: "needs the Pokemon runtime (stage 3 domain)" };
  }
  return { ...entry, status: "UNSUPPORTED", reason: name ? `no handler for ${name}` : "not a simple API call" };
}

/** Compiles every block and summarises the coverage. */
export function compileBlocks(blocks) {
  const list = Array.isArray(blocks) ? blocks : blocks.blocks || [];
  const ir = [];
  const coverage = {
    version: IR_VERSION,
    blocks: list.length,
    translated: 0,
    unsupported: 0,
    javaHandlerRequired: 0,
    byApi: {},
    problems: { unsupported: [], javaHandlerRequired: [] },
  };
  for (const block of list) {
    const result = compileBlock(block);
    if (result.status === "TRANSLATED") {
      coverage.translated++;
      if (result.ir.command !== "CONDITION") ir.push({ id: result.id, source: result.source, ir: result.ir });
    } else if (result.status === "JAVA_HANDLER_REQUIRED") {
      coverage.javaHandlerRequired++;
      coverage.problems.javaHandlerRequired.push(problemOf(result));
    } else {
      coverage.unsupported++;
      coverage.problems.unsupported.push(problemOf(result));
    }
    const name = (block.calls && block.calls[0] && block.calls[0].name) || `<${block.category}>`;
    const entry = coverage.byApi[name] || (coverage.byApi[name] = { total: 0, translated: 0, unsupported: 0, javaHandlerRequired: 0 });
    entry.total++;
    if (result.status === "TRANSLATED") entry.translated++;
    else if (result.status === "JAVA_HANDLER_REQUIRED") entry.javaHandlerRequired++;
    else entry.unsupported++;
  }
  coverage.coveragePercent = list.length === 0 ? 0 : Math.round(((coverage.translated + coverage.javaHandlerRequired) / list.length) * 1000) / 10;
  return { ir, coverage };
}

/** Source-located problem record for the reports (sections 24/25/59). */
function problemOf(result) {
  return {
    id: result.id,
    category: result.category,
    apis: result.apis,
    source: result.source,
    reason: result.reason,
    rubySource: result.rubySource,
  };
}

/**
 * Release policy of project3 section 24: an unsupported script must fail the
 * build and report Map / Event / Page / Command / Source; only an explicit
 * development flag may continue (never in release).
 */
export function evaluateReleasePolicy(problems, { allowUnsupported = false } = {}) {
  const unsupported = (problems && problems.unsupported) || [];
  if (unsupported.length === 0) {
    return { exitCode: 0, blocked: 0, allowed: 0 };
  }
  if (allowUnsupported) {
    return { exitCode: 0, blocked: 0, allowed: unsupported.length };
  }
  return { exitCode: 1, blocked: unsupported.length, allowed: 0, first: unsupported.slice(0, 10) };
}

/**
 * Compiles generated/scripts/blocks.json into the Debug IR the runtime reads
 * (generated/scripts/ir.json) plus the coverage and handler reports under
 * build/reports. Shared by the standalone CLI and by build-data, so a data
 * rebuild can never orphan the IR again - the R6.11 door animation depends on
 * the translated {@code setTempSwitchOn("A")} block that once-shot-guards the
 * arrival pages.
 *
 * @returns {{coverage: object, policy: object, irPath: string}}
 */
export function writeScriptIr(builderRoot, { allowUnsupported = false } = {}) {
  const blocksPath = path.join(builderRoot, "generated", "scripts", "blocks.json");
  if (!existsSync(blocksPath)) {
    throw new Error(`blocks.json not found: ${blocksPath}`);
  }
  const blocks = JSON.parse(readFileSync(blocksPath, "utf8"));
  const { ir, coverage } = compileBlocks(blocks);
  const scriptsDir = path.join(builderRoot, "generated", "scripts");
  const reportDir = path.join(builderRoot, "build", "reports");
  mkdirSync(scriptsDir, { recursive: true });
  mkdirSync(reportDir, { recursive: true });
  const irPath = path.join(scriptsDir, "ir.json");
  writeFileSync(irPath, JSON.stringify({ version: coverage.version, commands: ir }, null, 1));
  writeFileSync(path.join(reportDir, "script-coverage.json"), JSON.stringify(coverage, null, 1));
  writeFileSync(path.join(reportDir, "java-handler-required.json"), JSON.stringify({
    version: coverage.version,
    count: coverage.problems.javaHandlerRequired.length,
    blocks: coverage.problems.javaHandlerRequired,
  }, null, 1));
  return { coverage, policy: evaluateReleasePolicy(coverage.problems, { allowUnsupported }), irPath };
}
