// Quest plugin data (plugin section "004_Quest_Data", 284_004_Quest_Data.rb): `module QuestModule` with one Ruby hash per quest
// (`Quest1 = { :ID => "1", :Name => "...", :Stage1 => "...", :Location1 => "...", :QuestDescription => "..." | [...], ... }`)
// -> generated/quests.json, which the runtime's quest log reads (QuestData#getName / getStageDescription / ... of
// 282_002_Quest_Main:197-257).
//
// Only the literal subset the plugin uses is understood (double / single quoted strings, arrays of them, comments, trailing
// commas); anything else fails the build instead of being guessed.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { readScriptSources } from "../../tools/scanner/index.js";

export const QUEST_SECTION_NAME = "004_Quest_Data";

/** Reads one Ruby string literal starting at `index` (a quote); returns { value, end }. */
function readString(source, index) {
  const quote = source[index];
  let value = "";
  let i = index + 1;
  while (i < source.length) {
    const ch = source[i];
    if (ch === "\\") {
      const next = source[i + 1];
      if (quote === '"') {
        if (next === "n") value += "\n";
        else if (next === "t") value += "\t";
        else value += next; // \" \\ and any other escaped character
      } else {
        value += next === "'" || next === "\\" ? next : "\\" + next; // single quotes only escape ' and \
      }
      i += 2;
      continue;
    }
    if (ch === quote) return { value, end: i + 1 };
    if (quote === '"' && ch === "#" && source[i + 1] === "{") {
      throw new Error("string interpolation is not supported");
    }
    value += ch;
    i += 1;
  }
  throw new Error("unterminated string");
}

/** Skips whitespace and `#` comments. */
function skip(source, index) {
  let i = index;
  for (;;) {
    while (i < source.length && /\s/.test(source[i])) i += 1;
    if (source[i] === "#") {
      while (i < source.length && source[i] !== "\n") i += 1;
      continue;
    }
    return i;
  }
}

/** Reads a hash value: a string, or an array of strings. */
function readValue(source, index) {
  let i = skip(source, index);
  if (source[i] === '"' || source[i] === "'") return readString(source, i);
  if (source[i] === "[") {
    const items = [];
    i += 1;
    for (;;) {
      i = skip(source, i);
      if (source[i] === "]") return { value: items, end: i + 1 };
      const item = readValue(source, i);
      if (typeof item.value !== "string") throw new Error("nested arrays are not supported");
      items.push(item.value);
      i = skip(source, item.end);
      if (source[i] === ",") i += 1;
    }
  }
  throw new Error(`unsupported value at "${source.slice(i, i + 20).replace(/\n/g, " ")}"`);
}

/** Reads `{ :Key => value, ... }` starting at the `{`; returns { entries, end }. */
function readHash(source, index) {
  const entries = [];
  let i = index + 1;
  for (;;) {
    i = skip(source, i);
    if (source[i] === "}") return { entries, end: i + 1 };
    const key = /^:([A-Za-z_][A-Za-z0-9_]*)\s*=>/.exec(source.slice(i));
    if (!key) throw new Error(`expected a :Symbol => at "${source.slice(i, i + 20).replace(/\n/g, " ")}"`);
    const value = readValue(source, i + key[0].length);
    entries.push([key[1], value.value]);
    i = skip(source, value.end);
    if (source[i] === ",") i += 1;
  }
}

/** The keyed stages / locations of one quest hash in number order: Stage1, Stage2 ... */
function numbered(entries, prefix) {
  const found = [];
  for (const [key, value] of entries) {
    const match = new RegExp(`^${prefix}(\\d+)$`).exec(key);
    if (match) found.push([Number(match[1]), value]);
  }
  found.sort((a, b) => a[0] - b[0]);
  return found.map(([number, value]) => ({ number, value }));
}

/**
 * Parses the quest module into [{ key, id, name, questGiver, stages: {n: text}, locations: {n: text}, description, reward }].
 * A quest with no fields (Quest0 = {}) is kept as an empty entry, like the plugin's.
 */
export function parseQuestData(source) {
  const text = source.replace(/\r\n/g, "\n");
  const quests = [];
  const header = /^\s*(Quest\d+)\s*=\s*\{/gm;
  let match;
  while ((match = header.exec(text))) {
    const braceAt = match.index + match[0].length - 1;
    let hash;
    try {
      hash = readHash(text, braceAt);
    } catch (error) {
      throw new Error(`${match[1]}: ${error.message}`);
    }
    header.lastIndex = hash.end;
    const get = (name) => {
      const found = hash.entries.find(([key]) => key === name);
      return found ? found[1] : undefined;
    };
    const stages = {};
    for (const { number, value } of numbered(hash.entries, "Stage")) stages[number] = value;
    const locations = {};
    for (const { number, value } of numbered(hash.entries, "Location")) locations[number] = value;
    quests.push({
      key: match[1],
      id: get("ID"),
      name: get("Name"),
      questGiver: get("QuestGiver"),
      stages,
      locations,
      description: get("QuestDescription"),
      reward: get("RewardString"),
    });
  }
  return quests;
}

/**
 * Exports the quest table of the project's plugin section to generated/quests.json.
 * Returns { ok, skipped?, quests, changed, errors[], warnings[] }; never throws.
 */
export function syncQuestData(builderRoot, projectPath) {
  const result = { ok: false, skipped: false, quests: 0, changed: false, errors: [], warnings: [] };
  const scripts = readScriptSources(projectPath);
  if (scripts.errors.length > 0) {
    result.errors.push(...scripts.errors.map((e) => `${e.file}: ${e.error}`));
    return result;
  }
  const section = scripts.sections.find((s) => s.name === QUEST_SECTION_NAME);
  const outFile = path.join(builderRoot, "generated", "quests.json");
  if (!section) {
    result.ok = true;
    result.skipped = true;
    result.warnings.push(`no "${QUEST_SECTION_NAME}" script section: quest table left untouched`);
    return result;
  }
  let quests;
  try {
    quests = parseQuestData(section.source);
  } catch (error) {
    result.errors.push(`${QUEST_SECTION_NAME} (section ${section.index}): ${error.message}`);
    return result;
  }
  const json = `${JSON.stringify({ format: "pokemon-builder/quests/1", total: quests.length, quests }, null, 2)}\n`;
  if (!existsSync(outFile) || readFileSync(outFile, "utf8").replace(/\r\n/g, "\n") !== json) {
    mkdirSync(path.dirname(outFile), { recursive: true });
    writeFileSync(outFile, json, "utf8");
    result.changed = true;
  }
  result.quests = quests.length;
  result.ok = true;
  return result;
}
