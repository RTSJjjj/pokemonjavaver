// Read-only validation of an RPG Maker XP / Pokemon Essentials project.
// The project is an input only: nothing in this module writes to it.

import { existsSync, statSync } from "node:fs";
import path from "node:path";

export function validateProject(projectPath) {
  const result = {
    path: projectPath,
    ok: false,
    errors: [],
    warnings: [],
    info: {},
  };

  if (!projectPath) {
    result.errors.push(
      "no project path given (pass it as an argument or set source.rmxpProject in builder-config.json)"
    );
    return result;
  }

  let stat;
  try {
    stat = statSync(projectPath);
  } catch (error) {
    result.errors.push("project path does not exist: " + projectPath);
    return result;
  }
  if (!stat.isDirectory()) {
    result.errors.push("project path is not a directory: " + projectPath);
    return result;
  }

  const dataDir = path.join(projectPath, "Data");
  if (existsSync(dataDir)) {
    result.info.dataDir = dataDir;
  } else {
    result.errors.push("missing Data directory: " + dataDir);
  }

  const rxproj = path.join(projectPath, "Game.rxproj");
  if (existsSync(rxproj)) {
    result.info.rxproj = rxproj;
  } else {
    result.warnings.push("Game.rxproj not found (not a standard RMXP project?): " + rxproj);
  }

  const pbsDir = path.join(projectPath, "PBS");
  if (existsSync(pbsDir)) {
    result.info.pbsDir = pbsDir;
  } else {
    result.warnings.push(
      "PBS directory not found: " + pbsDir + " (Pokemon Essentials data will be missing)"
    );
  }

  const graphicsDir = path.join(projectPath, "Graphics");
  if (existsSync(graphicsDir)) {
    result.info.graphicsDir = graphicsDir;
  } else {
    result.warnings.push("Graphics directory not found: " + graphicsDir);
  }

  result.ok = result.errors.length === 0;
  return result;
}