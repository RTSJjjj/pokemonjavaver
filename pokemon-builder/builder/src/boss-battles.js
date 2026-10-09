// Boss_Battles pipeline step: Scripts.rxdata section "Boss_Battles" -> BossBattleData.java.
//
// New bosses are registered as `def battleXxx` in that plugin section. This step exports the section from the
// project, runs the strict generator (tools/boss-battles/generate.mjs) and writes the Java table the event
// interpreter reads, so adding a boss needs no manual export / generate / copy.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { readScriptSources } from "../../tools/scanner/index.js";
import { generateBossBattleData } from "../../tools/boss-battles/generate.mjs";

export const BOSS_SECTION_NAME = "Boss_Battles";

const JAVA_RELATIVE = ["runtime", "core", "src", "main", "java", "pokemon", "runtime", "event", "BossBattleData.java"];

const normalizeEol = (text) => text.replace(/\r\n/g, "\n");

/** Writes {@code content} only when it differs (line endings ignored); returns true when the file changed. */
function writeIfChanged(file, content) {
  if (existsSync(file) && normalizeEol(readFileSync(file, "utf8")) === normalizeEol(content)) return false;
  mkdirSync(path.dirname(file), { recursive: true });
  writeFileSync(file, content, "utf8");
  return true;
}

/** `battleXxx` names the event scripts call (generated/scripts/blocks.json), or [] when it is not built yet. */
function calledBossNames(builderRoot) {
  const file = path.join(builderRoot, "generated", "scripts", "blocks.json");
  if (!existsSync(file)) return [];
  const parsed = JSON.parse(readFileSync(file, "utf8"));
  const blocks = Array.isArray(parsed) ? parsed : parsed.blocks || [];
  const names = new Set();
  for (const block of blocks) {
    for (const api of block.apis || []) {
      if (/^battle[A-Z]\w*$/.test(api)) names.add(api);
    }
  }
  return [...names].sort();
}

/**
 * Exports section 319, regenerates BossBattleData.java and cross-checks it.
 * Returns { ok, skipped?, defs, entries, changed, errors[], warnings[] }; never throws.
 * A project without the plugin section is skipped (not an error).
 */
export function syncBossBattles(builderRoot, projectPath) {
  const result = { ok: false, skipped: false, defs: 0, entries: 0, changed: false, errors: [], warnings: [] };
  const scripts = readScriptSources(projectPath);
  if (scripts.errors.length > 0) {
    result.errors.push(...scripts.errors.map((e) => `${e.file}: ${e.error}`));
    return result;
  }
  const section = scripts.sections.find((s) => s.name === BOSS_SECTION_NAME);
  if (!section) {
    result.ok = true;
    result.skipped = true;
    result.warnings.push(`no "${BOSS_SECTION_NAME}" script section: boss table left untouched`);
    return result;
  }

  let generated;
  try {
    generated = generateBossBattleData(section.source);
  } catch (error) {
    result.errors.push(`${BOSS_SECTION_NAME} (section ${section.index}): ${error.message}`);
    return result;
  }
  result.defs = generated.defCount;
  result.entries = generated.entries.length;

  // Every def is either transcribed or listed as not transcribed - none may vanish.
  if (generated.entries.length + generated.unsupported.length !== generated.defCount) {
    result.errors.push(`${BOSS_SECTION_NAME}: ${generated.defCount} defs but ${generated.entries.length} entries `
      + `+ ${generated.unsupported.length} not transcribed`);
    return result;
  }
  if (generated.unsupported.length > 0) {
    result.warnings.push(`not transcribed: ${generated.unsupported.join(", ")}`);
  }

  // Events must not call a battleXxx the table does not hold (the interpreter would report it unsupported).
  const known = new Set(generated.entries.map((e) => e.name));
  const notTranscribed = new Set(generated.unsupported);
  const missing = calledBossNames(builderRoot).filter((n) => !known.has(n) && !notTranscribed.has(n));
  if (missing.length > 0) {
    result.errors.push(`events call boss function(s) with no def in ${BOSS_SECTION_NAME}: ${missing.join(", ")}`);
    return result;
  }
  const calledButNotTranscribed = calledBossNames(builderRoot).filter((n) => notTranscribed.has(n));
  if (calledButNotTranscribed.length > 0) {
    result.errors.push(`events call boss function(s) the generator cannot transcribe: ${calledButNotTranscribed.join(", ")}`);
    return result;
  }

  const exportFile = path.join(builderRoot, "plugin-src", "ruby", `${section.index}_${BOSS_SECTION_NAME}.rb`);
  const exported = writeIfChanged(exportFile, section.source);
  const javaFile = path.join(builderRoot, ...JAVA_RELATIVE);
  const javaChanged = writeIfChanged(javaFile, generated.java);
  result.changed = exported || javaChanged;
  result.javaFile = javaFile;
  result.ok = true;
  return result;
}
