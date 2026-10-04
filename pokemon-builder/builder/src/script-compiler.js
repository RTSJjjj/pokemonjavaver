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
  pbCut() {
    return { command: "FIELD_MOVE", move: "CUT" };
  },
  pbRockSmash() {
    return { command: "FIELD_MOVE", move: "ROCK_SMASH" };
  },
  pbMessage(args) {
    return { command: "SHOW_TEXT", text: args[0] };
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
  toggle_liefeng_switches() {
    return { command: "TOGGLE_PLATE_SWITCHES" };
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
  "pbGenPkmn", "pbAddPokemon", "pbChoosePokemon", "pbChoosePokemonForTrade",
  "pbTrainerBattle", "pbDoubleTrainerBattle", "pbWildBattle", "pbTrainerIntro",
  "pbBerryPlant", "pbPickBerry", "pbStoreItem", "pbGetKeyItem", "pbDeleteItem",
  "pbPokeCenterPC", "pbShowMap", "pbSetPokemonCenter",
  "pbToggleFollowingPokemon", "pbRegisterPartner", "pbDeregisterPartner",
  "pbSet", "push",
]);

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
  if (handler) {
    // R6.19: a block may hold several statements. Every one of them has to
    // translate on its own; the resulting IR is a SEQUENCE the runtime plays
    // step by step (a waiting step such as SHOW_TEXT resumes the rest).
    // R6.30: the project's cry pair is rewritten first.
    const statements = rewriteCryStatements(callStatements(block));
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
        if (stepName !== null && DOMAIN_APIS.has(stepName)) {
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
