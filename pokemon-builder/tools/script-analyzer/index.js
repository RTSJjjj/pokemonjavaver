// Read-only Scripts.rxdata usage analyzer (Phase 5).
//
// Classifies every Ruby block collected from the event data (Phase 4) using
// the project's own script sources as the vocabulary:
//
//   ESSENTIALS_API    call/constant of Pokemon Essentials core (pb* helpers,
//                     PB*/PokeBattle_*/PField_*/... namespaces, $Trainer, ...)
//   PLUGIN_API        call/constant of a third party or project injected
//                     script section (everything that is NOT Essentials core)
//   RMXP_GLOBAL       RPG Maker XP runtime reference ($game_switches, Sprite,
//                     Viewport, Tone, ...) - provided by the engine itself
//   SIMPLE_EXPRESSION one plain statement, no control flow, nothing unknown
//   COMPLEX_RUBY      control flow, several statements or blocks of Ruby
//   UNKNOWN           nothing recognizable; the identifiers are reported
//                     instead of being dropped silently
//
// Unique pattern statistics (Total Script Blocks / Unique APIs / Unique
// Script Patterns / Usage Count) drive the migration estimate: work follows
// the number of distinct argument shapes, not the number of occurrences.
//
// Hard rules honoured here:
//   * nothing is silently ignored - unrecognised identifiers land in
//     `unresolvedIdentifiers` with their counts and a sample location;
//   * the source project is only ever read;
//   * if a section cannot be read the analysis fails loudly.

import { rubyText } from "../scanner/index.js";
import { countLines } from "../../builder/src/util.js";

export const ESSENTIALS_API = "ESSENTIALS_API";
export const PLUGIN_API = "PLUGIN_API";
export const RMXP_GLOBAL = "RMXP_GLOBAL";
export const SIMPLE_EXPRESSION = "SIMPLE_EXPRESSION";
export const COMPLEX_RUBY = "COMPLEX_RUBY";
export const UNKNOWN = "UNKNOWN";

export const CATEGORIES = [
  ESSENTIALS_API,
  PLUGIN_API,
  RMXP_GLOBAL,
  SIMPLE_EXPRESSION,
  COMPLEX_RUBY,
  UNKNOWN,
];

// Pokemon Essentials naming conventions: pbXxx helpers and the PB*/Poke*/...
// namespaces of the engine.
const ESSENTIALS_METHOD = /^pb[A-Z_a-z]/;
const ESSENTIALS_CONSTANT = /^(?:PB[A-Z]|PokeBattle_|Pokemon[A-Z_]|PField_|PItem_|PScreen_|PMinigame_|PBattle_|PSystem_)/;

// Strong plugin markers: a version number or an explicitly plugin labelled
// separator. Essentials keeps its core scripts in one block at the top of the
// script list, so the first of these marks the start of the plugin area.
const PLUGIN_MARKER = [
  (name) => /\d+\.\d+(?:\.\d+)?/.test(name),
  (name) => /^=+[^=]*=+$/.test(name) && /[^\x00-\x7f]/.test(name),
  (name) => name.includes("插件"),
];

const SEPARATOR = /^=+$/;
// Essentials core sections are English named; a CJK name marks a script the
// project injected into the core area (version checkers, custom UI, ...).
const ASCII_NAME = /^[\x20-\x7e]+$/;

// RPG Maker XP runtime: globals, engine classes and modules. Anything outside
// this list, the project symbols and the Ruby core list is reported as
// unresolved instead of being quietly accepted.
const RMXP_GLOBALS = new Set([
  "$game_system", "$game_temp", "$game_switches", "$game_variables", "$game_self_switches",
  "$game_map", "$game_player", "$game_party", "$game_troop", "$game_screen", "$game_pictures",
  "$game_timer", "$game_message", "$game_portraits", "$scene", "$map_factory", "$mytestate",
  "$data_actors", "$data_classes", "$data_skills", "$data_items", "$data_weapons", "$data_armors",
  "$data_enemies", "$data_troops", "$data_states", "$data_animations", "$data_tilesets",
  "$data_common_events", "$data_system", "$data_map_infos",
]);
const RMXP_CLASSES = new Set([
  "Sprite", "Window", "Viewport", "Bitmap", "Color", "Tone", "Rect", "Table", "Font", "Plane",
  "Tilemap", "Sprite_Character", "Win32API", "RGSSError", "RPG", "Audio",
]);

// Ruby 1.8 core / stdlib surface that event scripts realistically touch.
const RUBY_GLOBALS = new Set([
  "$stdout", "$stderr", "$stdin", "$!", "$?", "$;", "$/", "$\\", "$,", "$0", "$DEBUG",
  "$VERBOSE", "$TEST", "$PROGRAM_NAME", "$LOAD_PATH", "$LOADED_FEATURES", "$KCODE", "$SAFE",
]);
const RUBY_CLASSES = new Set([
  "Array", "String", "Hash", "Integer", "Float", "Symbol", "NilClass", "TrueClass",
  "FalseClass", "Object", "Module", "Class", "Proc", "Exception", "StandardError",
  "RuntimeError", "ArgumentError", "TypeError", "NameError", "NoMethodError",
  "ZeroDivisionError", "Range", "Regexp", "MatchData", "IO", "File", "Dir", "Time",
  "Math", "Random", "Comparable", "Enumerable", "Kernel", "Struct", "Marshal", "Zlib",
  "GC", "Mutex", "Thread", "ObjectSpace", "Errno",
]);
const RUBY_METHODS = new Set([
  "puts", "print", "p", "pp", "sprintf", "format", "require", "load", "rand", "srand",
  "sleep", "exit", "abort", "at_exit", "raise", "loop", "lambda", "proc", "new", "allocate",
  "to_s", "to_i", "to_f", "to_a", "to_sym", "inspect", "freeze", "dup", "clone", "hash",
  "class", "is_a?", "kind_of?", "instance_of?", "respond_to?", "send", "method", "methods",
  "instance_variable_get", "instance_variable_set", "attr_accessor", "attr_reader", "attr_writer",
  "length", "size", "push", "pop", "shift", "unshift", "map", "map!", "each", "each_with_index",
  "each_with_object", "select", "reject", "collect", "collect_concat", "inject", "reduce",
  "find", "detect", "count", "sum", "min", "max", "sort", "sort_by", "sort!", "reverse",
  "reverse!", "first", "last", "include?", "index", "rindex", "keys", "values", "delete",
  "delete_if", "merge", "merge!", "fetch", "store", "has_key?", "has_value?", "key?", "value?",
  "empty?", "nil?", "eql?", "equal?", "===", "call", "arity", "curry", "yield", "upto", "downto",
  "times", "step", "between?", "abs", "ceil", "floor", "round", "zero?", "positive?", "negative?",
  "chr", "ord", "ordinal", "slice", "gsub", "sub", "split", "join", "strip", "chomp", "chop",
  "chars", "bytes", "lines", "upcase", "downcase", "capitalize", "ljust", "rjust", "center",
  "start_with?", "end_with?", "match", "match?", "scan", "source", "format", "encode", "force_encoding",
]);

const RUBY_KEYWORDS = new Set([
  "if", "elsif", "else", "unless", "while", "until", "for", "in", "do", "end", "begin",
  "rescue", "ensure", "case", "when", "then", "return", "def", "class", "module", "yield",
  "self", "nil", "true", "false", "and", "or", "not", "next", "break", "redo", "retry",
  "super", "lambda", "proc", "defined?", "__method__", "__FILE__", "__LINE__",
]);

function isArray(value) {
  return Array.isArray(value);
}

function describeError(error) {
  return String((error && error.message) || error);
}

// 0 based line number of a position inside `code` (diagnostics only).
function lineAt(code, index) {
  const prefix = code.slice(0, index);
  return prefix.length === 0 ? 0 : countLines(prefix) - 1;
}

// Is `$name` at this position the target of an assignment? Index brackets are
// skipped with proper nesting ($g[[1,2]] = 3) and `==` / `=~` are rejected.
function assignsGlobalAt(source, from) {
  let cursor = from;
  for (;;) {
    while (cursor < source.length && /[ \t]/.test(source[cursor])) cursor += 1;
    if (source[cursor] !== "[") break;
    let depth = 0;
    while (cursor < source.length) {
      const char = source[cursor];
      if (char === "[") depth += 1;
      else if (char === "]") {
        depth -= 1;
        if (depth === 0) {
          cursor += 1;
          break;
        }
      }
      cursor += 1;
    }
    if (depth !== 0) return false;
  }
  while (cursor < source.length && /[ \t]/.test(source[cursor])) cursor += 1;
  if (source[cursor] !== "=") return false;
  return source[cursor + 1] !== "=";
}

// ---------------------------------------------------------------------------
// Project symbol table
// ---------------------------------------------------------------------------

// Splits a class/module name into its parts and returns them.
function namesOf(node) {
  return node.split("::").filter(Boolean);
}

export function collectDefinitions(sections) {
  const symbols = new Map();
  if (!isArray(sections)) return symbols;
  for (const section of sections) {
    const source = typeof section === "object" && section ? section.source : null;
    if (typeof source !== "string") continue;
    const index = typeof section.index === "number" ? section.index : null;
    const add = (name, kind) => {
      if (!name) return;
      let record = symbols.get(name);
      if (!record) {
        record = { name, kinds: new Set(), sections: new Set(), plugins: new Set() };
        symbols.set(name, record);
      }
      record.kinds.add(kind);
      if (index !== null) {
        record.sections.add(index);
        if (section.isPlugin) record.plugins.add(index);
      }
    };
    for (const match of source.matchAll(/^[ \t]*def\s+(?:[A-Za-z_]\w*(?:[.#]|::))?\s*([A-Za-z_]\w*[!?]?)/gm)) {
      add(match[1], "method");
    }
    // alias / alias_method define a second name for an existing method;
    // Essentials uses it heavily (alias pbGenPkmn pbNewPkmn).
    for (const match of source.matchAll(/^[ \t]*alias[ \t]+([A-Za-z_]\w*[!?]?)[ \t]+([A-Za-z_]\w*[!?]?)/gm)) {
      add(match[1], "method");
      add(match[2], "method");
    }
    for (const match of source.matchAll(/^[ \t]*alias_method[ \t]+:([A-Za-z_]\w*[!?]?)[ \t,]+:([A-Za-z_]\w*[!?]?)/gm)) {
      add(match[1], "method");
      add(match[2], "method");
    }
    for (const match of source.matchAll(/^[ \t]*(?:class|module)\s+([A-Z]\w*(?:::[A-Z]\w*)*)/gm)) {
      for (const part of namesOf(match[1])) add(part, "class/module");
    }
    // Globals assigned anywhere in the section define the project global set.
    for (const match of source.matchAll(/\$([A-Za-z_]\w*)/g)) {
      if (assignsGlobalAt(source, match.index + match[0].length)) add("$" + match[1], "global");
    }
  }
  return symbols;
}

// Marks sections as Essentials core or plugin, based on the first section that
// carries a strong plugin marker. The detected boundary is reported so a human
// can verify it.
// Globals that an event script assigns ($battle_item = [...]) are project
// state as well: the future runtime has to carry them. Registering them turns
// them from "unresolved" into a recognised, reported global.
export function collectEventGlobals(blocks, symbols) {
  const found = new Map();
  for (const block of Array.isArray(blocks) ? blocks : []) {
    const code = stripRubyComments(typeof block.rubySource === "string" ? block.rubySource : "");
    if (!code) continue;
    for (const match of code.matchAll(/\$([A-Za-z_]\w*)/g)) {
      if (!assignsGlobalAt(code, match.index + match[0].length)) continue;
      const name = "$" + match[1];
      let record = symbols.get(name);
      if (!record) {
        record = { name, kinds: new Set(), sections: new Set(), plugins: new Set(), eventAssignments: 0 };
        symbols.set(name, record);
      }
      record.kinds.add("global");
      record.sections.add(-1);
      record.eventAssignments = (record.eventAssignments || 0) + 1;
      if (!found.has(name)) {
        found.set(name, {
          name,
          assignments: 1,
          sample: {
            file: block.file,
            eventId: block.eventId === undefined ? null : block.eventId,
            eventName: block.eventName === undefined ? null : block.eventName,
          },
        });
      } else {
        found.get(name).assignments += 1;
      }
    }
  }
  return Array.from(found.values()).sort((a, b) => b.assignments - a.assignments || a.name.localeCompare(b.name));
}

export function classifySections(sections) {
  if (!isArray(sections)) return { sections: [], cutoff: null };
  let cutoff = null;
  const list = sections.map((section, index) => {
    const name = typeof section === "object" && section && typeof section.name === "string" ? section.name : "";
    const isPlugin = cutoff !== null || PLUGIN_MARKER.some((marker) => marker(name));
    if (isPlugin && cutoff === null) cutoff = index;
    return { ...section, index, isPlugin };
  });
  for (const section of list) {
    // Inside the core area a section only counts as core when it follows the
    // Essentials naming (ASCII identifier) - injected utility scripts with
    // CJK names are treated as project scripts, not as engine code.
    section.isCore = !section.isPlugin && (ASCII_NAME.test(section.name) || SEPARATOR.test(section.name));
  }
  return { sections: list, cutoff };
}

// ---------------------------------------------------------------------------
// Ruby scanning
// ---------------------------------------------------------------------------

// Very small Ruby scanner: comments and string literals are removed from the
// scanned text so identifiers inside them are never mistaken for code.
export function stripRubyComments(source) {
  let out = "";
  let index = 0;
  let inBlockComment = false;
  while (index < source.length) {
    if (inBlockComment) {
      const end = source.indexOf("\n=end", index);
      if (end < 0) return out;
      index = source.indexOf("\n", end + 1);
      if (index < 0) return out;
      index += 1;
      inBlockComment = false;
      continue;
    }
    const char = source[index];
    if (char === "#") {
      const end = source.indexOf("\n", index);
      if (end < 0) return out;
      out += "\n";
      index = end + 1;
      continue;
    }
    if (source.startsWith("=begin", index) && (index === 0 || source[index - 1] === "\n")) {
      const end = source.indexOf("\n=end", index);
      if (end < 0) return out;
      index = source.indexOf("\n", end + 1);
      if (index < 0) return out;
      out += "\n";
      index += 1;
      continue;
    }
    if (char === "\"" || char === "'") {
      const quote = char;
      out += " ";
      index += 1;
      while (index < source.length) {
        if (source[index] === "\\") {
          index += 2;
          continue;
        }
        if (source[index] === quote) {
          index += 1;
          break;
        }
        index += 1;
      }
      continue;
    }
    out += char;
    index += 1;
  }
  return out;
}

function argumentKind(text) {
  const value = text.trim();
  if (value === "") return "none";
  if (/^:[A-Za-z_]\w*[?!]?$/.test(value)) return "sym";
  if (/^"(?:[^"\\]|\\.)*"$/.test(value) || /^'(?:[^'\\]|\\.)*'$/.test(value)) return "str";
  if (/^-?\d+(?:\.\d+)?$/.test(value)) return "num";
  if (/^(?:true|false|nil)$/.test(value)) return "const";
  if (/^\$[A-Za-z_]\w*$/.test(value)) return "global";
  if (/^@[A-Za-z_]\w*$/.test(value)) return "ivar";
  if (/^[A-Za-z_]\w*(?:::[A-Za-z_]\w*)*$/.test(value)) return "name";
  if (/^\[.*\]$/.test(value)) return "array";
  if (/^\{.*\}$/.test(value)) return "hash";
  if (/^\(.*\)$/.test(value)) return "group";
  if (/^:?[A-Za-z_]\w*[?!]?/.test(value) && /\s/.test(value)) return "expr";
  return "expr";
}

// Splits an argument list on top level commas (respecting brackets, braces and
// the string literals already removed by stripRubyComments).
function splitArguments(text) {
  const parts = [];
  let depth = 0;
  let current = "";
  for (const char of text) {
    if (char === "(" || char === "[" || char === "{") depth += 1;
    if (char === ")" || char === "]" || char === "}") depth -= 1;
    if (char === "," && depth <= 0) {
      parts.push(current);
      current = "";
      continue;
    }
    current += char;
  }
  parts.push(current);
  return parts;
}

function readBalanced(code, start) {
  // start points at "("; returns the text inside and the index after ")".
  let depth = 0;
  let index = start;
  let body = "";
  while (index < code.length) {
    const char = code[index];
    if (char === "(") {
      depth += 1;
      if (depth === 1) {
        index += 1;
        continue;
      }
    } else if (char === ")") {
      depth -= 1;
      if (depth === 0) return { body, next: index + 1 };
    }
    body += char;
    index += 1;
  }
  return { body, next: index };
}

const ARGUMENT_START = /[\s]/;

// Scans one script and collects the identifiers it uses.
// Scans one script and collects the identifiers it uses: calls (with their
// normalized argument shape), constants, globals and the control flow
// keywords. Comments and string literals are stripped first so that
// identifiers inside them are never mistaken for code.
export function scanScript(code, lineOffset = 0, depth = 0) {
  const result = {
    calls: [],
    ivars: [],
    constants: [],
    globals: [],
    symbols: [],
    keywords: new Set(),
    hasBlock: false,
    statements: (code.match(/;/g) || []).length + 1,
  };
  if (typeof code !== "string" || code.length === 0) return result;

  let index = 0;
  let previousMeaningful = "";

  while (index < code.length) {
    const char = code[index];
    if (/\s/.test(char)) {
      index += 1;
      continue;
    }
    if (char === "@") {
      const ivar = /^@([A-Za-z_]\w*)/.exec(code.slice(index));
      if (ivar) {
        result.ivars = result.ivars || [];
        result.ivars.push({ name: "@" + ivar[1], line: lineAt(code, index) + lineOffset });
        index += ivar[0].length;
        previousMeaningful = "ivar";
        continue;
      }
      index += 1;
      continue;
    }
    if (char === "$") {
      const match = /^\$([A-Za-z_]\w*)/.exec(code.slice(index));
      if (match) {
        result.globals.push({ name: "$" + match[1], line: lineAt(code, index) + lineOffset });
        index += match[0].length;
        previousMeaningful = "global";
        continue;
      }
      index += 1;
      continue;
    }
    if (/[A-Za-z_]/.test(char)) {
      let match = /^[A-Za-z_]\w*/.exec(code.slice(index));
      const tail = code.slice(index + match[0].length);
      if (/^[!?]/.test(tail) && !/^[!?]=/.test(tail)) match = /^[A-Za-z_]\w*[!?]/.exec(code.slice(index));
      if (!match) {
        index += 1;
        continue;
      }
      const name = match[0];
      const after = code.slice(index + name.length);
      const trimmed = after.replace(/^\s*/, "");
      const isCall = trimmed.startsWith("(");
      const receiver = previousMeaningful === ".";
      if (isCall) {
        const parenStart = index + name.length + (after.length - trimmed.length);
        const { body, next } = readBalanced(code, parenStart);
        const line = lineAt(code, index) + lineOffset;
        result.calls.push({
          name,
          receiver,
          shape: splitArguments(body).map(argumentKind).join(","),
          line,
        });
        // The argument list is code as well: pbStartTrade(pbGet(1), ...) hides
        // calls inside the arguments, so scan them too.
        if (depth < 8) {
          const nested = scanScript(body, line, depth + 1);
          for (const entry of nested.calls) result.calls.push(entry);
          for (const entry of nested.constants) result.constants.push(entry);
          for (const entry of nested.globals) result.globals.push(entry);
          for (const entry of nested.symbols) result.symbols.push(entry);
          for (const entry of nested.ivars) result.ivars.push(entry);
          for (const keyword of nested.keywords) result.keywords.add(keyword);
          if (nested.hasBlock) result.hasBlock = true;
        }
        index = next;
        previousMeaningful = "call";
        continue;
      }
      if (RUBY_KEYWORDS.has(name)) {
        result.keywords.add(name);
        if (name === "do" || name === "begin" || name === "case" || name === "def" || name === "end") {
          result.hasBlock = true;
        }
        index += name.length;
        previousMeaningful = "keyword";
        continue;
      }
      // :SYMBOL literals and the tail of a qualified constant (A::B) are not
      // references of their own.
      if (previousMeaningful === ":" || previousMeaningful === "::") {
        index += name.length;
        previousMeaningful = "constant";
        continue;
      }
      if (/^[A-Z]/.test(name)) {
        result.constants.push({ name, line: lineAt(code, index) + lineOffset });
        index += name.length;
        previousMeaningful = "constant";
        continue;
      }
      // A bare identifier followed by something that can start an argument is
      // a possible call without parentheses (pbGenderSelector, pbSet, ...).
      const nextChar = trimmed[0] || "";
      const startsArgument = /["'$@:\[\dA-Za-z_]/.test(nextChar);
      const afterOperator = !/^(=|---|\+|&&|\|\||\?|,|\)|\]|\}|;|\.|!|<|>|%|\*|\/|~|&)/.test(trimmed);
      result.symbols.push({
        name,
        receiver,
        argumentFollows: startsArgument && afterOperator,
        endsStatement: trimmed === "" || trimmed === "\n" || trimmed === ";",
        line: lineAt(code, index) + lineOffset,
      });
      index += name.length;
      previousMeaningful = "symbol";
      continue;
    }
    if (char === ":") {
      // A single colon starts a symbol literal (:POTION); a double colon is a
      // namespace separator (PBItems::POTION).
      if (code[index + 1] === ":") {
        previousMeaningful = "::";
        index += 2;
        continue;
      }
      previousMeaningful = ":";
      index += 1;
      continue;
    }
    if (char === ".") previousMeaningful = ".";
    else previousMeaningful = char;
    index += 1;
  }
  return result;
}

// ---------------------------------------------------------------------------
// Local variables
// ---------------------------------------------------------------------------

export function collectLocals(code) {
  const locals = new Set();
  const add = (raw) => {
    for (const part of String(raw).split(",")) {
      const name = part.trim();
      if (name && !RUBY_KEYWORDS.has(name)) locals.add(name);
    }
  };
  for (const match of code.matchAll(/(^|[^\w.:$@])([A-Za-z_]\w*)\s*[-+*\/%|&^]?=(?![=>~])/g)) add(match[2]);
  for (const match of code.matchAll(/\bfor\s+([A-Za-z_]\w*(?:\s*,\s*[A-Za-z_]\w*)*)\s+in\b/g)) add(match[1]);
  for (const match of code.matchAll(/\|\s*([A-Za-z_]\w*(?:\s*,\s*[A-Za-z_]\w*)*)\s*\|/g)) add(match[1]);
  return locals;
}

// ---------------------------------------------------------------------------
// Classification
// ---------------------------------------------------------------------------

function essentialByName(name) {
  return ESSENTIALS_METHOD.test(name) || ESSENTIALS_CONSTANT.test(name);
}

// Where does an identifier come from?
//   "essential"  defined by an Essentials core section
//   "plugin"      defined only by plugin / injected sections, or unknown but
//                  Essentials named (a plugin provided extension)
//   "rmxp"       provided by the RPG Maker XP runtime
//   "ruby"       Ruby core / stdlib
//   "unresolved" nothing known about it - reported, never dropped
export function originOf(name, symbols, coreIndices) {
  const symbol = symbols.get(name);
  if (symbol && symbol.sections.size > 0) {
    return Array.from(symbol.sections).some((index) => coreIndices.has(index)) ? "essential" : "plugin";
  }
  if (symbol && symbol.sections.size > 0 && Array.from(symbol.sections).every((index) => index === -1)) {
    return "event";
  }
  if (RMXP_GLOBALS.has(name) || RMXP_CLASSES.has(name)) return "rmxp";
  if (RUBY_GLOBALS.has(name) || RUBY_CLASSES.has(name) || RUBY_METHODS.has(name)) return "ruby";
  // An Essentials named identifier without a visible definition is still an
  // engine API (Essentials builds PBItems / PBSpecies dynamically, and even
  // defines helpers through alias), so treat it as core rather than plugin.
  if (essentialByName(name)) return "essential";
  return "unresolved";
}

// One classification record per event script block.
export function classifyScript(rubySource, symbols, coreIndices) {
  const code = stripRubyComments(rubySource);
  const scan = scanScript(code);
  const locals = collectLocals(code);
  const used = {
    essential: new Set(),
    plugin: new Set(),
    eventGlobal: new Set(),
    rmxp: new Set(),
    ruby: new Set(),
    unresolved: new Set(),
    ivars: new Set(),
  };
  const calls = [];
  const note = (name) => {
    if (locals.has(name)) return;
    const origin = originOf(name, symbols, coreIndices);
    if (origin === "event") used.eventGlobal.add(name);
    else used[origin].add(name);
  };

  // Instance variables of the event interpreter (@ch_cmd) are state, not an
  // API; they are kept so reports can list what state a port has to carry.
  for (const ivar of scan.ivars) used.ivars.add(ivar.name);

  for (const call of scan.calls) {
    note(call.name);
    calls.push({
      name: call.name,
      pattern: call.name + "(" + call.shape + ")",
      shape: call.shape,
      receiver: call.receiver,
      line: call.line,
    });
  }
  for (const entry of scan.symbols) {
    const symbol = symbols.get(entry.name);
    const definedMethod = symbol ? symbol.kinds.has("method") : false;
    // An identifier that ends a line (pbTrainerEnd, pbSet) or is directly
    // followed by an argument is a Ruby call without parentheses. On a
    // receiver ($Trainer.pokemonCount, p.makeShiny) it is only counted when
    // the project defines that method - otherwise it is an attribute read
    // (p.iv) which describes object state, not an API call.
    const noParenCall = entry.receiver
      ? definedMethod && (entry.argumentFollows || entry.endsStatement)
      : entry.argumentFollows || entry.endsStatement;
    if (noParenCall) {
      const origin = originOf(entry.name, symbols, coreIndices);
      const defined = symbol !== undefined;
      if ((defined || origin !== "unresolved") && calls.every((call) => call.name !== entry.name)) {
        calls.push({
          name: entry.name,
          pattern: entry.name + "(<no-parens>)",
          shape: "<no-parens>",
          receiver: entry.receiver,
          line: entry.line,
        });
      }
      note(entry.name);
      continue;
    }
    if (entry.receiver) continue;
    note(entry.name);
  }
  for (const constant of scan.constants) note(constant.name);
  for (const global of scan.globals) note(global.name);

  const hasControlFlow =
    scan.keywords.has("if") || scan.keywords.has("unless") || scan.keywords.has("while") ||
    scan.keywords.has("until") || scan.keywords.has("for") || scan.keywords.has("case") ||
    scan.hasBlock;

  let category;
  if (used.essential.size > 0) category = ESSENTIALS_API;
  else if (used.plugin.size > 0 || used.eventGlobal.size > 0) category = PLUGIN_API;
  else if (used.rmxp.size > 0) category = RMXP_GLOBAL;
  else if (used.unresolved.size > 0) category = UNKNOWN;
  else if (hasControlFlow || scan.statements > 1 || countLines(rubySource.trim()) > 1) category = COMPLEX_RUBY;
  else category = SIMPLE_EXPRESSION;

  return {
    category,
    calls,
    essential: Array.from(used.essential),
    plugin: Array.from(used.plugin),
    eventGlobals: Array.from(used.eventGlobal),
    rmxp: Array.from(used.rmxp),
    ruby: Array.from(used.ruby),
    unresolved: Array.from(used.unresolved),
    interpreterState: Array.from(used.ivars),
    locals: Array.from(locals),
  };
}

// ---------------------------------------------------------------------------
// Aggregation
// ---------------------------------------------------------------------------

function emptyApiRecord(name, kind) {
  return {
    name,
    kind,
    category: "unknown",
    occurrences: 0,
    blocks: 0,
    maps: 0,
    uniquePatterns: 0,
    patterns: new Map(),
    files: new Set(),
    sample: null,
  };
}

// Full analysis: the event blocks come from the Phase 4 analyzer, the script
// sections from the Phase 3 scanner. Read-only.
export function analyzeScriptUsage(eventAnalysis, scriptSections) {
  const started = Date.now();
  const result = {
    analyzedAt: new Date().toISOString(),
    durationMs: 0,
    ok: false,
    errors: [],
    warnings: [],
    sections: { total: 0, readable: 0, coreSections: [], pluginSections: [], coreCutoff: null, problems: [] },
    classification: [],
    summary: {
      totalScriptBlocks: 0,
      byCategory: {},
      uniqueApis: 0,
      uniquePatterns: 0,
      unresolvedIdentifiers: 0,
      eventGlobals: 0,
      interpreterStateBlocks: 0,
    },
    apis: [],
    eventGlobals: [],
    unresolvedIdentifiers: [],
    sectionDetails: [],
  };

  const sectionReport = Array.isArray(scriptSections) ? scriptSections : [];
  const classified = classifySections(sectionReport);
  const sections = classified.sections;
  const coreIndices = new Set(sections.filter((section) => section.isCore).map((section) => section.index));
  result.sections.total = sectionReport.length;
  result.sections.readable = sections.filter((section) => typeof section.source === "string").length;
  result.sections.coreSections = Array.from(coreIndices);
  result.sections.pluginSections = sections.filter((section) => !section.isCore).map((section) => section.index);
  result.sections.coreCutoff = classified.cutoff;
  if (result.sections.readable === 0) {
    result.errors.push({ file: "Scripts.rxdata", error: "no readable script sections; cannot classify event scripts" });
    result.durationMs = Date.now() - started;
    return result;
  }

  const symbols = collectDefinitions(sections);
  const blocks = eventAnalysis && Array.isArray(eventAnalysis.scriptBlocks) ? eventAnalysis.scriptBlocks : [];
  // Which symbols each section defines - the plugin report needs it.
  const sectionSymbols = new Map();
  for (const [name, record] of symbols) {
    for (const index of record.sections) {
      if (index < 0) continue;
      if (!sectionSymbols.has(index)) sectionSymbols.set(index, []);
      sectionSymbols.get(index).push(name);
    }
  }
  result.sectionDetails = sections.map((section) => ({
    index: section.index,
    name: section.name,
    lines: typeof section.lines === "number" ? section.lines : null,
    isCore: Boolean(section.isCore),
    symbols: (sectionSymbols.get(section.index) || []).sort(),
  }));
  const eventGlobals = collectEventGlobals(blocks, symbols);
  result.eventGlobals = eventGlobals;
  result.summary.totalScriptBlocks = blocks.length;

  const apiRecords = new Map();
  const unresolvedRecords = new Map();

  const recordFor = (name, kind, category) => {
    let record = apiRecords.get(name);
    if (!record) {
      record = emptyApiRecord(name, kind);
      apiRecords.set(name, record);
    }
    record.category = category;
    return record;
  };

  // Counts every occurrence with its argument pattern.
  const bump = (name, kind, category, block, pattern) => {
    const record = recordFor(name, kind, category);
    record.occurrences += 1;
    const key = pattern || "(reference)";
    record.patterns.set(key, (record.patterns.get(key) || 0) + 1);
    if (!record.sample) {
      record.sample = {
        file: block.file,
        eventId: block.eventId === undefined ? null : block.eventId,
        eventName: block.eventName === undefined ? null : block.eventName,
        page: block.page === undefined ? null : block.page,
        commandIndex: block.commandIndex === undefined ? null : block.commandIndex,
      };
    }
  };

  // Counts a name once per script block (blocks / maps / files).
  const bumpOnce = (name, kind, category, block) => {
    const record = recordFor(name, kind, category);
    record.blocks += 1;
    record.files.add(block.file);
    if (block.mapId !== null && block.mapId !== undefined) record.maps += 1;
  };
  const bumpUnresolved = (name, block) => {
    let record = unresolvedRecords.get(name);
    if (!record) {
      record = { name, occurrences: 0, blocks: 0, sample: null };
      unresolvedRecords.set(name, record);
    }
    record.occurrences += 1;
    if (!record.sample) {
      record.sample = { file: block.file, eventId: block.eventId, eventName: block.eventName, page: block.page, commandIndex: block.commandIndex };
    }
  };

  for (const block of blocks) {
    const verdict = classifyScript(block.rubySource, symbols, coreIndices);
    const seen = new Set();
    const once = (name, kind, category) => {
      if (seen.has(name)) return;
      seen.add(name);
      bumpOnce(name, kind, category, block);
    };
    const category = verdict.category;
    result.summary.byCategory[category] = (result.summary.byCategory[category] || 0) + 1;
    result.classification.push({
      id: block.id,
      source: block.source,
      file: block.file,
      mapId: block.mapId === undefined ? null : block.mapId,
      mapName: block.mapName === undefined ? null : block.mapName,
      eventId: block.eventId === undefined ? null : block.eventId,
      eventName: block.eventName === undefined ? null : block.eventName,
      page: block.page === undefined ? null : block.page,
      commandIndex: block.commandIndex === undefined ? null : block.commandIndex,
      category,
      apis: verdict.essential.concat(verdict.plugin, verdict.eventGlobals),
      calls: verdict.calls,
      unresolved: verdict.unresolved,
      interpreterState: verdict.interpreterState.length > 0 ? verdict.interpreterState : null,
    });
    // Calls carry their argument pattern; names that are only referenced
    // (constants, globals, mentions without parentheses) are counted once.
    for (const call of verdict.calls) {
      const origin = originOf(call.name, symbols, coreIndices);
      const callCategory =
        origin === "essential" ? ESSENTIALS_API :
        origin === "plugin" ? PLUGIN_API :
        origin === "rmxp" ? RMXP_GLOBAL :
        origin === "ruby" ? "RUBY" : UNKNOWN;
      bump(call.name, "method", callCategory, block, call.pattern);
      once(call.name, "method", callCategory);
    }
    for (const name of verdict.essential) once(name, "api", ESSENTIALS_API);
    for (const name of verdict.eventGlobals) once(name, "event-global", PLUGIN_API);
    for (const name of verdict.plugin) once(name, "api", PLUGIN_API);
    for (const name of verdict.rmxp) once(name, "rmxp", RMXP_GLOBAL);
    for (const name of verdict.ruby) once(name, "ruby", "RUBY");
    for (const name of verdict.unresolved) bumpUnresolved(name, block);
  }

  result.apis = Array.from(apiRecords.values())
    .map((record) => ({
      name: record.name,
      kind: record.kind,
      category: record.category,
      occurrences: record.occurrences,
      blocks: record.blocks,
      maps: record.maps,
      uniquePatterns: record.patterns.size,
      patterns: Array.from(record.patterns.entries()).sort((a, b) => b[1] - a[1]).map(([pattern, count]) => ({ pattern, count })),
      files: Array.from(record.files).sort(),
      definedIn: symbols.get(record.name)
        ? Array.from(symbols.get(record.name).sections).filter((index) => index >= 0).sort((a, b) => a - b)
        : [],
      sample: record.sample,
    }))
    .sort((a, b) => b.occurrences - a.occurrences || a.name.localeCompare(b.name));

  result.unresolvedIdentifiers = Array.from(unresolvedRecords.values())
    .map((record) => ({ ...record, files: [] }))
    .sort((a, b) => b.occurrences - a.occurrences || a.name.localeCompare(b.name));

  result.summary.uniqueApis = result.apis.filter((api) => api.kind === "api" || api.kind === "method").length;
  result.summary.uniquePatterns = result.apis.reduce((sum, api) => sum + api.uniquePatterns, 0);
  result.summary.unresolvedIdentifiers = result.unresolvedIdentifiers.length;
  result.summary.eventGlobals = result.eventGlobals.length;
  result.summary.interpreterStateBlocks = result.classification.filter((record) => record.interpreterState).length;
  for (const problem of (scriptSections || [])) {
    if (problem && problem.problems) result.sections.problems.push(...problem.problems);
    if (problem && problem.errors) result.warnings.push(...problem.errors.map((entry) => entry.error));
  }
  result.ok = result.errors.length === 0;
  result.durationMs = Date.now() - started;
  return result;
}

// Console summary shared by the CLI and later the audit step.
export function formatScriptSummary(result, options = {}) {
  const top = typeof options.top === "number" && options.top > 0 ? options.top : 15;
  const lines = [];
  lines.push(
    "script sections: " + result.sections.readable + "/" + result.sections.total +
      " readable, core sections: " + result.sections.coreSections.length +
      ", plugin sections: " + result.sections.pluginSections.length +
      (result.sections.coreCutoff === null ? "" : " (plugin area starts at section " + result.sections.coreCutoff + ")"),
  );
  lines.push("classified script blocks: " + result.summary.totalScriptBlocks);
  for (const category of CATEGORIES) {
    const count = result.summary.byCategory[category] || 0;
    if (count > 0) lines.push("  " + category + ": " + count);
  }
  lines.push(
    "unique APIs: " + result.summary.uniqueApis +
      ", unique script patterns: " + result.summary.uniquePatterns +
      ", unresolved identifiers: " + result.summary.unresolvedIdentifiers,
  );
  const essentials = result.apis.filter((api) => api.category === ESSENTIALS_API).slice(0, top);
  if (essentials.length > 0) {
    lines.push("top Essentials APIs:");
    for (const api of essentials) {
      lines.push(
        "  " + api.name + " - occurrences " + api.occurrences +
          ", blocks " + api.blocks + ", maps " + api.maps +
          ", unique patterns " + api.uniquePatterns,
      );
    }
  }
  const plugins = result.apis.filter((api) => api.category === PLUGIN_API).slice(0, top);
  if (plugins.length > 0) {
    lines.push("top plugin APIs:");
    for (const api of plugins) {
      lines.push(
        "  " + api.name + " - occurrences " + api.occurrences +
          ", blocks " + api.blocks + ", unique patterns " + api.uniquePatterns,
      );
    }
  }
  if (result.eventGlobals && result.eventGlobals.length > 0) {
    lines.push("globals assigned by event scripts (project state):");
    for (const global of result.eventGlobals.slice(0, 10)) {
      lines.push(
        "  " + global.name + " - assigned " + global.assignments + " time(s), first at " +
          (global.sample ? global.sample.file + " event " + global.sample.eventId : "?"),
      );
    }
  }
  for (const identifier of result.unresolvedIdentifiers.slice(0, 10)) {
    lines.push(
      "unresolved: " + identifier.name + " - occurrences " + identifier.occurrences +
        " (first at " + (identifier.sample ? identifier.sample.eventId : "?") + ")",
    );
  }
  return lines;
}