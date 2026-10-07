// Incremental build cache (Phase 13).
//
// The cache reserves the incremental architecture: after every successful
// conversion it records, in build/cache.json, the fingerprint of every unit
// input (sha1 + bytes + mtime) and the IR files that unit produced, together
// with the dependency links between them. A later build compares the current
// fingerprints against the cached ones and only rewrites the units whose inputs
// or outputs changed - so "Map001 untouched is not recompiled, only Map002 is
// rebuilt" is enforced here, in one place.
//
// The cache is a pure accelerator: it is ignored (full rebuild) whenever its
// format, builder version, IR format or project path does not match, and every
// unit additionally verifies that its output files still exist on disk. A
// missing or corrupt cache never breaks a build and never produces a stale IR.

import { existsSync, mkdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";

import { sha1 } from "../../builder/src/util.js";

export const CACHE_FORMAT = "pokemon-builder/build-cache/2";

export function cacheFilePath(cacheDir) {
  return cacheDir ? path.join(cacheDir, "cache.json") : null;
}

// Fingerprint of one input file. Unreadable files are reported as an error
// entry (never silently skipped): the caller turns them into a hard failure.
export function fingerprintFile(relativePath, absolutePath) {
  let bytes;
  try {
    bytes = readFileSync(absolutePath);
  } catch (error) {
    return { file: relativePath, error: "cannot read file: " + String(error.message || error) };
  }
  let mtime = null;
  let mtimeMs = null;
  try {
    const stat = statSync(absolutePath);
    mtime = stat.mtime.toISOString();
    mtimeMs = stat.mtimeMs;
  } catch (error) {
    mtime = null;
    mtimeMs = null;
  }
  return { file: relativePath, bytes: bytes.length, sha1: sha1(bytes.toString("latin1")), mtime, mtimeMs };
}

// A unit is fresh only when it exists in the cache, every dependency still has
// exactly the cached fingerprint and every output file is still on disk.
export function unitIsFresh(cache, unitId, fingerprints, generatedDir) {
  if (!cache || !cache.units) return false;
  const unit = cache.units[unitId];
  if (!unit || !unit.fingerprints || !Array.isArray(unit.outputs)) return false;
  for (const [file, digest] of Object.entries(unit.fingerprints)) {
    const current = fingerprints[file];
    if (!current || current.error) return false;
    if (current.sha1 !== digest) return false;
  }
  for (const output of unit.outputs) {
    if (!existsSync(path.join(generatedDir, output))) return false;
  }
  return true;
}

export function loadCache(cacheDir, expected) {
  const file = cacheFilePath(cacheDir);
  if (!existsSync(file)) {
    return { used: false, reason: "no cache yet (first build)", cache: null, file };
  }
  let parsed;
  try {
    parsed = JSON.parse(readFileSync(file, "utf8"));
  } catch (error) {
    return { used: false, reason: "cache is not valid JSON: " + String(error.message || error), cache: null, file };
  }
  if (!parsed || typeof parsed !== "object" || !parsed.units) {
    return { used: false, reason: "cache has an unknown shape", cache: null, file };
  }
  if (parsed.format !== CACHE_FORMAT) {
    return { used: false, reason: "cache format changed (" + parsed.format + ")", cache: null, file };
  }
  if (expected && expected.irFormat && parsed.irFormat !== expected.irFormat) {
    return { used: false, reason: "IR format changed (" + expected.irFormat + ")", cache: null, file };
  }
  if (expected && expected.version && parsed.builderVersion !== expected.version) {
    return { used: false, reason: "builder version changed (" + expected.version + ")", cache: null, file };
  }
  if (expected && expected.project && parsed.project !== expected.project) {
    return { used: false, reason: "cache belongs to a different project", cache: null, file };
  }
  return { used: true, reason: "cache loaded", cache: parsed, file };
}

export function saveCache(cacheDir, cache) {
  const file = cacheFilePath(cacheDir);
  mkdirSync(cacheDir, { recursive: true });
  writeFileSync(file, JSON.stringify(cache, null, 2), "utf8");
  return file;
}
