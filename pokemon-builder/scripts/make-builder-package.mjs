// Packs everything the builder needs - except the runtimes it runs on (Node.js, JDK, Android SDK) - into one folder and
// one zip: dist/PokemonBuilder/ and dist/PokemonBuilder.zip. ffmpeg (ffmpeg.exe / ffprobe.exe) travels inside.
//
//   node scripts/make-builder-package.mjs [--no-zip]
//
// What goes in: builder/ (CLI), tools/ (compilers + ffmpeg), runtime/ (the game's Gradle project, sources only),
// scripts/*.bat, plugin-src/, docs/ (the guides), builder.bat, builder-config.json (project path emptied), package.json,
// BuilderGUI.ps1 + 启动构建器.bat (the visual generator) and 使用说明.txt.
// What stays out: generated/, dist/, build/, logs/, Gradle / IDE caches, tests, hs_err logs, release keystores.

import {
  copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync,
} from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createDataPack } from "../builder/src/data-pack.js";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..");
const outRoot = path.join(root, "dist", "PokemonBuilder");
const zipPath = path.join(root, "dist", "PokemonBuilder.zip");
const wantZip = !process.argv.includes("--no-zip");

const SKIP_DIRS = new Set(["build", ".gradle", "node_modules", ".idea", ".git", "out", "__pycache__", "tests"]);
const SKIP_FILE = /(\.log|\.tmp|\.iml|local\.properties|\.pyc)$/i;

function copyTree(from, to, filter = () => true) {
  let files = 0;
  for (const entry of readdirSync(from, { withFileTypes: true })) {
    const source = path.join(from, entry.name);
    const target = path.join(to, entry.name);
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name) || !filter(source, true)) continue;
      mkdirSync(target, { recursive: true });
      files += copyTree(source, target, filter);
    } else if (entry.isFile()) {
      if (SKIP_FILE.test(entry.name) || !filter(source, false)) continue;
      mkdirSync(path.dirname(target), { recursive: true });
      copyFileSync(source, target);
      files++;
    }
  }
  return files;
}

function copyFile(relative, optional = false) {
  const from = path.join(root, relative);
  if (!existsSync(from)) {
    if (optional) return 0;
    throw new Error("missing " + relative);
  }
  const to = path.join(outRoot, relative);
  mkdirSync(path.dirname(to), { recursive: true });
  copyFileSync(from, to);
  return 1;
}

rmSync(outRoot, { recursive: true, force: true });
mkdirSync(outRoot, { recursive: true });
const counts = {};

counts.builder = copyTree(path.join(root, "builder"), path.join(outRoot, "builder"));
counts.tools = copyTree(path.join(root, "tools"), path.join(outRoot, "tools"), (source, isDir) => {
  // ffmpeg: only the two programs the audio step runs and the licence; the docs and ffplay are left out
  if (!source.split(path.sep).includes("ffmpeg")) return true;
  if (isDir) return !/[\\/]ffmpeg[\\/](doc|presets)$/.test(source);
  return /[\\/]ffmpeg[\\/]bin[\\/](ffmpeg|ffprobe)\.exe$/i.test(source) || /[\\/]ffmpeg[\\/](LICENSE|README\.txt)$/i.test(source);
});
const runtimeRoot = path.join(root, "runtime");
counts.runtime = copyTree(runtimeRoot, path.join(outRoot, "runtime"), (source, isDir) => {
  // sources only: inside a module (runtime/core, runtime/lwjgl3 ...) keep src/, gradle/ and keystore/; the screenshot
  // probe folders and other work directories that pile up there stay out
  const parts = path.relative(runtimeRoot, source).split(path.sep);
  const modules = ["core", "lwjgl3", "android", "android-legacy"];
  if (parts.length === 1) {
    return isDir ? [...modules, "gradle"].includes(parts[0]) : true;          // gradlew, settings.gradle, ...
  }
  if (modules.includes(parts[0]) && parts.length === 2) {
    return isDir ? ["src", "gradle", "keystore"].includes(parts[1]) : parts[1] === "build.gradle";
  }
  if (isDir) return true;
  // the preview keystore is part of the project; never ship a release keystore
  if (/\.(jks|keystore)$/i.test(source)) return /preview-release\.jks$/i.test(source);
  return true;
});
counts.scripts = copyTree(path.join(root, "scripts"), path.join(outRoot, "scripts"), (source, isDir) => isDir || /\.bat$/i.test(source));
counts.pluginSrc = copyTree(path.join(root, "plugin-src"), path.join(outRoot, "plugin-src"));
counts.docs = 0;
for (const name of ["user-guide.md", "player-guide.md", "android-build.md", "desktop-build.md", "release-1-plan.md"]) {
  counts.docs += copyFile(path.join("docs", name), true);
}
for (const name of ["builder.bat", "package.json", "BuilderGUI.ps1", "启动构建器.bat"]) {
  copyFile(name);
}

// builder-config.json: the project path is the user's to choose
const config = JSON.parse(readFileSync(path.join(root, "builder-config.json"), "utf8"));
config.source = { ...(config.source || {}), rmxpProject: "" };
writeFileSync(path.join(outRoot, "builder-config.json"), JSON.stringify(config, null, 2) + "\n", "utf8");

writeFileSync(path.join(outRoot, "使用说明.txt"), [
  "Pokemon 构建器",
  "================",
  "",
  "1. 双击「启动构建器.bat」打开可视化构建器。",
  "2. 点「浏览…」在资源管理器里选择你的游戏工程文件夹（RPG Maker XP 工程，里面有 Data、Graphics、Audio）。",
  "3. 点「导出 PC 版」或「导出 安卓版」。进度和日志在窗口里，成品在本文件夹的 dist\\ 里。",
  "",
  "需要你自己装好的运行环境（不随包提供）：",
  "  - Node.js 18 或更新（https://nodejs.org）",
  "  - JDK 17 或更新（设置好 JAVA_HOME，或把 java 放进 PATH）",
  "  - 导出安卓还需要 Android SDK（设置 ANDROID_HOME，或在 runtime\\local.properties 写 sdk.dir=...）",
  "已随包提供：ffmpeg（tools\\ffmpeg\\bin）。",
  "",
  "命令行用法（和窗口里的按钮等价）：",
  "  builder.bat build-pc \"D:\\Game\\你的工程\"",
  "  builder.bat build-android \"D:\\Game\\你的工程\" [--release]",
  "  可选：--no-encrypt（不加密资源）、--no-cache（全部重建）",
  "",
  "更多说明见 docs\\user-guide.md。",
  "",
].join("\r\n"), "utf8");

let total = 0;
let bytes = 0;
(function walk(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else {
      total++;
      bytes += statSync(full).size;
    }
  }
})(outRoot);
console.log("package folder: " + outRoot + " (" + total + " files, " + (bytes / 1048576).toFixed(0) + " MB)");
console.log("  " + JSON.stringify(counts));

if (wantZip) {
  const started = Date.now();
  const pack = createDataPack([{ root: outRoot, prefix: "PokemonBuilder" }], zipPath);
  console.log("zip: " + zipPath + " (" + (statSync(zipPath).size / 1048576).toFixed(0) + " MB, " + pack.files + " files, "
    + ((Date.now() - started) / 1000).toFixed(1) + " s)");
}
