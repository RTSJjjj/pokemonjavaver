// Gradle invocation for the build pipelines (project3 sections 45, 52-54).
//
// The desktop / Android builds drive the runtime's Gradle wrapper. Two Windows
// realities are handled here:
//   - the builder may live under a path with non-ASCII characters. Gradle
//     test workers then corrupt their @argfile classpath (the daemon writes it
//     in file.encoding, the JVM launcher reads the native encoding), which
//     breaks every test class. Running the wrapper from an auto-created subst
//     drive keeps every path ASCII (the same trick the acceptance workflow
//     uses with `subst X:`).
//   - JAVA_HOME comes from the detected runtime, so the wrapper does not
//     depend on a system wide Java setup.
//
// Everything is injectable for tests: `spawn` mirrors child_process.spawnSync.

import { spawnSync } from "node:child_process";
import { existsSync, statSync } from "node:fs";
import path from "node:path";

export function isAscii(value) {
  return /^[\x00-\x7F]*$/.test(String(value));
}

/** True when the build must be run through an ASCII drive (Windows only). */
export function needsAsciiDrive(builderRoot, platform = process.platform) {
  return platform === "win32" && !isAscii(builderRoot);
}

/** First free drive letter of the tried set, or null. */
export function findFreeDrive(options = {}) {
  const exists = options.exists || existsSync;
  for (const letter of ["Y", "Z", "W", "V"]) {
    if (!exists(letter + ":\\")) return letter;
  }
  return null;
}

/**
 * Substs `builderRoot` to a free drive so every path inside the build is
 * ASCII. Returns `{ letter, root, unmount() }` or null when no drive could be
 * mounted.
 */
export function mountAsciiDrive(builderRoot, options = {}) {
  const spawn = options.spawn || spawnSync;
  const letter = options.letter || findFreeDrive(options);
  if (!letter) return null;
  let result;
  try {
    result = spawn("subst", [letter + ":", builderRoot], {
      encoding: "utf8",
      windowsHide: true,
    });
  } catch (error) {
    return null;
  }
  if (!result || result.error || result.status !== 0) return null;
  return {
    letter,
    root: letter + ":",
    unmount() {
      try {
        spawn("subst", [letter + ":", "/d"], { encoding: "utf8", windowsHide: true });
      } catch (error) {
        // best effort: a leftover subst drive is harmless
      }
    },
  };
}

/** The runtime's Gradle wrapper inside the builder checkout. */
export function gradleWrapper(builderRoot) {
  return path.join(builderRoot, "runtime", process.platform === "win32" ? "gradlew.bat" : "gradlew");
}

/** Windows: batch files can only run through cmd.exe (Node cannot spawn .bat). */
function windowsCommandLine(wrapper, tasks) {
  const quote = (part) => (/[\s"]/.test(part) ? '"' + part.replace(/"/g, '""') + '"' : part);
  return [wrapper, ...tasks].map(quote).join(" ");
}

/**
 * Runs the runtime Gradle wrapper with the given tasks.
 *
 * @returns {{
 *   ok: boolean, missing?: boolean, mountFailed?: boolean,
 *   exitCode: number|null, output: string, cwd: string, executable: string
 * }}
 */
export function runGradleTasks(builderRoot, tasks, options = {}) {
  const spawn = options.spawn || spawnSync;
  const exists = options.exists || existsSync;
  const platform = options.platform || process.platform;
  const wrapper = options.executable || gradleWrapper(builderRoot);
  const runtimeDir = path.join(builderRoot, "runtime");
  if (!exists(wrapper)) {
    return { ok: false, missing: true, exitCode: null, output: "", cwd: runtimeDir, executable: wrapper };
  }

  let cwd = runtimeDir;
  let mounted = null;
  if (needsAsciiDrive(builderRoot, platform)) {
    mounted = mountAsciiDrive(builderRoot, { spawn, exists });
    if (!mounted) {
      return {
        ok: false,
        mountFailed: true,
        exitCode: null,
        output: "",
        cwd: runtimeDir,
        executable: wrapper,
      };
    }
    cwd = path.join(mounted.root + "\\", "runtime");
  }

  const env = { ...process.env, ...(options.env || {}) };
  if (options.javaHome) env.JAVA_HOME = options.javaHome;
  const command = platform === "win32"
    ? { executable: "cmd.exe", args: ["/d", "/s", "/c", windowsCommandLine(wrapper, tasks)] }
    : { executable: wrapper, args: tasks };
  try {
    const result = spawn(command.executable, command.args, {
      cwd,
      env,
      encoding: "utf8",
      windowsHide: true,
      maxBuffer: 64 * 1024 * 1024,
    });
    let output = String(result.stdout || "") + String(result.stderr || "");
    if (result.error) output += String(result.error.message || result.error) + "\n";
    return {
      ok: !result.error && result.status === 0,
      exitCode: typeof result.status === "number" ? result.status : null,
      output,
      cwd,
      executable: wrapper,
    };
  } catch (error) {
    return { ok: false, exitCode: null, output: String(error.message || error), cwd, executable: wrapper };
  } finally {
    if (mounted) mounted.unmount();
  }
}

// ---------------------------------------------------------------------------
// Artifact locations
// ---------------------------------------------------------------------------

export function desktopArtifacts(builderRoot) {
  const dist = path.join(builderRoot, "dist");
  return {
    dist,
    runnableJar: path.join(dist, "PokemonGame.jar"),
    windowsExe: path.join(dist, "Windows", "PokemonGame.exe"),
  };
}

/** APK path an `:module:assemble<Variant>` run writes (debug or L3 release). */
export function androidApkPath(builderRoot, target) {
  const variant = target.release ? "release" : "debug";
  return path.join(
    builderRoot,
    "runtime",
    target.gradleModule,
    "build",
    "outputs",
    "apk",
    variant,
    target.gradleModule + "-" + variant + ".apk",
  );
}

/** Missing artifact files among `paths` (for the package step). */
export function missingArtifacts(paths, options = {}) {
  const exists = options.exists || existsSync;
  return paths.filter((file) => !exists(file));
}

/** Human readable file size, or "" when the file does not exist. */
export function fileSizeLabel(file, options = {}) {
  const stat = options.stat || statSync;
  try {
    const bytes = stat(file).size;
    if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    if (bytes >= 1024) return Math.round(bytes / 1024) + " KB";
    return bytes + " bytes";
  } catch (error) {
    return "";
  }
}
