// Shared test fixtures: a minimal RMXP project, a scratch copy of the builder
// and helpers for running the scripts/*.bat wrappers through cmd.exe.

import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, readdirSync, cpSync, rmSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import os from "node:os";
import { fileURLToPath } from "node:url";

import { array, dump, hash, int, object, userdef, str, deflateText, nilValue } from "./marshal-writer.js";

export const BUILDER_SOURCE = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

export const REPORT_FILES = [
  "source-audit.md",
  "script-event-audit.md",
  "api-usage.md",
  "plugin-usage.md",
  "unsupported-scripts.md",
];

// Minimal but complete RMXP project: map info, one map with a scripted event
// page (355 + 655 continuation), common events and a two section
// Scripts.rxdata, so every pipeline stage has real work to do.
// RGSS Table payload ('u' userdef): dim0, dim1, dim2, dim0, total + uint16 LE
// values - the same layout the converter decodes (see data-converter).
function tableBytes(dim0, dim1, dim2, values) {
  const total = dim0 * dim1 * dim2;
  const bytes = Buffer.alloc(20 + total * 2);
  bytes.writeUInt32LE(dim0, 0);
  bytes.writeUInt32LE(dim1, 4);
  bytes.writeUInt32LE(dim2, 8);
  bytes.writeUInt32LE(dim0, 12);
  bytes.writeUInt32LE(total, 16);
  for (let i = 0; i < total; i++) {
    bytes.writeUInt16LE(values[i % values.length], 20 + i * 2);
  }
  return bytes;
}
export function makeProject(parent, name = "FakeProject") {
  const project = path.join(parent, name);
  mkdirSync(path.join(project, "Data"), { recursive: true });
  mkdirSync(path.join(project, "PBS"), { recursive: true });
  writeFileSync(path.join(project, "Game.rxproj"), "", "utf8");
  writeFileSync(
    path.join(project, "Data", "Scripts.rxdata"),
    dump(
      array(
        array(int(0), str("================"), str(deflateText(""))),
        array(int(1), str("Main"), str(deflateText("def pbMain\n  pbSet(1, 1)\nend\n"))),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "MapInfos.rxdata"),
    dump(hash([int(1), object("RPG::MapInfo", {
      "@name": str("Starting Map"),
      "@order": int(1),
      "@parent_id": int(0),
    })])),
  );
  writeFileSync(
    path.join(project, "Data", "Map001.rxdata"),
    dump(
      object("RPG::Map", {
        "@tileset_id": int(1),
        "@width": int(17),
        "@height": int(13),
        "@events": hash([
          int(1),
          object("RPG::Event", {
            "@id": int(1),
            "@name": str("Professor"),
            "@pages": array(
              object("RPG::Event::Page", {
                "@trigger": int(0),
                "@list": array(
                  object("RPG::EventCommand", {
                    "@code": int(355),
                    "@indent": int(0),
                    "@parameters": array(str("pbSet(1,1)")),
                  }),
                  object("RPG::EventCommand", {
                    "@code": int(655),
                    "@indent": int(0),
                    "@parameters": array(str("pbMain")),
                  }),
                ),
              }),
            ),
          }),
        ]),
      }),
    ),
  );
  // Stage 2 runtime data: the tileset passability / priority / terrain tables
  // and the System start position, in the real RGSS Table layout.
  writeFileSync(
    path.join(project, "Data", "Tilesets.rxdata"),
    dump(
      array(
        nilValue(),
        object("RPG::Tileset", {
          "@id": int(1),
          "@name": str("Fake Tiles"),
          "@tileset_name": str("Fake Tileset"),
          "@autotile_names": array(str(""), str(""), str(""), str(""), str(""), str(""), str("")),
          "@panorama_name": str(""),
          "@panorama_hue": int(0),
          "@fog_name": str(""),
          "@fog_hue": int(0),
          "@fog_opacity": int(64),
          "@fog_blend_type": int(0),
          "@fog_zoom": int(200),
          "@fog_sx": int(0),
          "@fog_sy": int(0),
          "@battleback_name": str(""),
          "@passages": userdef("Table", tableBytes(1, 6624, 1, [0, 0, 0x0f, 0x0f, 0x0f, 0x0f])),
          "@priorities": userdef("Table", tableBytes(1, 6624, 1, [0, 1, 1, 1, 1, 1])),
          "@terrain_tags": userdef("Table", tableBytes(1, 6624, 1, [0, 1, 1, 1, 1, 1])),
        }),
      ),
    ),
  );
  writeFileSync(
    path.join(project, "Data", "System.rxdata"),
    dump(
      object("RPG::System", {
        "@magic_number": int(16522614),
        "@start_map_id": int(1),
        "@start_x": int(9),
        "@start_y": int(7),
        "@edit_map_id": int(1),
        "@windowskin_name": str(""),
        "@title_name": str(""),
        "@gameover_name": str(""),
        "@battleback_name": str(""),
        "@elements": array(str(""), str("")),
        "@switches": array(str(""), str("SW1"), str("SW2")),
        "@variables": array(str(""), str("VAR1")),
        "@words": object("RPG::System::Words", { "@hp": str("HP"), "@attack": str("ATK") }),
      }),
    ),
  );  writeFileSync(path.join(project, "Data", "CommonEvents.rxdata"), dump(array(nilValue())));
  return project;
}

// Copies the runnable part of the builder into a scratch directory so the
// batch files can be executed without touching the real output directories.
export function copyBuilder() {
  const root = mkdtempSync(path.join(os.tmpdir(), "pb-builder-"));
  cpSync(path.join(BUILDER_SOURCE, "builder", "src"), path.join(root, "builder", "src"), {
    recursive: true,
  });
  cpSync(path.join(BUILDER_SOURCE, "tools"), path.join(root, "tools"), { recursive: true });
  cpSync(path.join(BUILDER_SOURCE, "scripts"), path.join(root, "scripts"), { recursive: true });
  cpSync(path.join(BUILDER_SOURCE, "builder.bat"), path.join(root, "builder.bat"));
  cpSync(path.join(BUILDER_SOURCE, "package.json"), path.join(root, "package.json"));
  writeFileSync(path.join(root, "builder-config.json"), CONFIG_JSON, "utf8");
  return root;
}

export const CONFIG_JSON = JSON.stringify(
  {
    projectName: "PokemonGame",
    version: "0.1.0",
    source: { rmxpProject: "" },
    targets: { desktop: true, android: true, androidLegacy: false },
    output: { generated: "generated", dist: "dist", logs: "logs" },
  },
  null,
  2
);

// windowsVerbatimArguments keeps Node from re-escaping the command line, and
// the extra outer quotes are what cmd /s /c needs so that quoted paths
// containing spaces survive.
export function runBat(builderRoot, batName, ...args) {
  const command = [path.join(builderRoot, "scripts", batName)]
    .concat(args)
    .map((arg) => '"' + arg + '"')
    .join(" ");
  // node:test marks the processes it spawns with NODE_TEST_CONTEXT. A nested
  // "node --test" run (scripts\test-builder.bat) would inherit that marker and
  // refuse to run any file ("recursively within a test file"), so the marker is
  // dropped: each wrapper invocation starts as a fresh, independent run.
  const env = { ...process.env };
  delete env.NODE_TEST_CONTEXT;
  return spawnSync("cmd.exe", ["/d", "/s", "/c", '"' + command + '"'], {
    encoding: "utf8",
    timeout: 120000,
    windowsVerbatimArguments: true,
    env,
  });
}

export function runAuditBat(builderRoot, ...args) {
  return runBat(builderRoot, "audit-project.bat", ...args);
}

export function runBuildDataBat(builderRoot, ...args) {
  return runBat(builderRoot, "build-data.bat", ...args);
}

export function runBuildPcBat(builderRoot, ...args) {
  return runBat(builderRoot, "build-pc.bat", ...args);
}

export function runBuildAndroidBat(builderRoot, ...args) {
  return runBat(builderRoot, "build-android.bat", ...args);
}

export function runBuildAndroidLegacyBat(builderRoot, ...args) {
  return runBat(builderRoot, "build-android-legacy.bat", ...args);
}

export function runCleanBuildBat(builderRoot, ...args) {
  return runBat(builderRoot, "clean-build.bat", ...args);
}

export function runTestBuilderBat(builderRoot, ...args) {
  return runBat(builderRoot, "test-builder.bat", ...args);
}

export function outputOf(result) {
  return (result.stdout || "") + (result.stderr || "");
}

// Content + length fingerprint of every file below a directory; used to prove
// that a build never writes into the source project.
export function snapshotProject(project) {
  const entries = [];
  const walk = (current, relative) => {
    for (const entry of readdirSync(current, { withFileTypes: true })) {
      const childRelative = relative ? path.join(relative, entry.name) : entry.name;
      const child = path.join(current, entry.name);
      if (entry.isDirectory()) walk(child, childRelative);
      else entries.push(childRelative + ":" + readFileSync(child).length);
    }
  };
  walk(project, "");
  return entries.sort();
}

export function scratch(prefix, test) {
  const dir = mkdtempSync(path.join(os.tmpdir(), prefix));
  test.after(() => rmSync(dir, { recursive: true, force: true }));
  return dir;
}