// Configuration loading for the Pokemon Builder CLI.
// Reads builder-config.json from the builder root and applies overrides
// (for example a project path passed on the command line).

import { readFileSync } from "node:fs";
import path from "node:path";

export class ConfigError extends Error {}

export const DEFAULT_CONFIG = {
  projectName: "PokemonGame",
  version: "0.1.0",
  source: {
    rmxpProject: "",
  },
  targets: {
    desktop: true,
    android: true,
    androidLegacy: false,
  },
  output: {
    generated: "generated",
    dist: "dist",
    logs: "logs",
  },
  // Audio Asset Compiler options (project2 requirement 9). CLI flags override
  // these; the preset provides values any explicit field does not set.
  audio: {
    preset: "standard",
    convertSE: true,
    seThresholdBytes: 262144,
    deduplicate: true,
    analyzeSilence: true,
    trimSilence: false,
    normalize: false,
  },
};

function isPlainObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function mergeInto(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (value === undefined) continue;
    if (isPlainObject(value) && isPlainObject(target[key])) {
      mergeInto(target[key], value);
    } else {
      target[key] = value;
    }
  }
  return target;
}

export function loadConfig(builderRoot, overrides = {}) {
  const configPath = path.join(builderRoot, "builder-config.json");
  let raw;
  try {
    raw = readFileSync(configPath, "utf8");
  } catch (error) {
    if (error && error.code === "ENOENT") {
      throw new ConfigError("builder-config.json not found: " + configPath);
    }
    throw new ConfigError("cannot read " + configPath + ": " + error.message);
  }
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    throw new ConfigError("builder-config.json is not valid JSON: " + error.message);
  }
  const config = mergeInto(structuredClone(DEFAULT_CONFIG), parsed);
  mergeInto(config, overrides);
  return config;
}

// Output directories always live inside the builder root. "build" is fixed by
// convention (it holds cache.json and reports/); the other three come from
// builder-config.json so users can rename them.
export function resolveOutputPaths(builderRoot, config) {
  return {
    root: builderRoot,
    build: path.resolve(builderRoot, "build"),
    generated: path.resolve(builderRoot, config.output.generated),
    dist: path.resolve(builderRoot, config.output.dist),
    logs: path.resolve(builderRoot, config.output.logs),
  };
}

// Safety guard used by destructive commands (clean): a path is only accepted
// when it really sits inside the builder root.
export function isInside(root, target) {
  const rel = path.relative(path.resolve(root), path.resolve(target));
  return rel === "" || (!rel.startsWith("..") && !path.isAbsolute(rel));
}