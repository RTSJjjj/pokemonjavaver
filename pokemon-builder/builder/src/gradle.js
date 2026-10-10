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

const DRIVE_LETTERS = ["Y", "Z", "W", "V", "X", "U", "T", "S", "R", "Q", "P", "O", "N", "M", "L", "K", "J", "I", "H"];

/** Every free drive letter of the tried set, in order. */
export function freeDrives(options = {}) {
  const exists = options.exists || existsSync;
  return DRIVE_LETTERS.filter((letter) => !exists(letter + ":\\"));
}

/** First free drive letter of the tried set, or null. */
export function findFreeDrive(options = {}) {
  return freeDrives(options)[0] || null;
}

/**
 * Substs `builderRoot` to a free drive so every path inside the build is
 * ASCII. Tries the free letters in turn (a letter can look free yet refuse the
 * subst). Returns `{ letter, root, unmount() }` or null when no drive could be
 * mounted; `mountAsciiDrive.lastError` then holds what `subst` said.
 */
export function mountAsciiDrive(builderRoot, options = {}) {
  const spawn = options.spawn || spawnSync;
  const letters = options.letter ? [options.letter] : freeDrives(options);
  mountAsciiDrive.lastError = letters.length ? "" : "no free drive letter among " + DRIVE_LETTERS.join("");
  for (const letter of letters) {
    let result;
    try {
      result = spawn("subst", [letter + ":", builderRoot], {
        encoding: "utf8",
        windowsHide: true,
      });
    } catch (error) {
      mountAsciiDrive.lastError = String(error.message || error);
      continue;
    }
    if (!result || result.error || result.status !== 0) {
      mountAsciiDrive.lastError = result && result.error ? String(result.error.message || result.error)
        : String((result && (result.stderr || result.stdout)) || "subst failed").trim();
      continue;
    }
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
  return null;
}

/** The runtime's Gradle wrapper inside the builder checkout. */
export function gradleWrapper(builderRoot) {
  return path.join(builderRoot, "runtime", process.platform === "win32" ? "gradlew.bat" : "gradlew");
}

/** An Android SDK folder: it exists and has at least one of the folders every SDK install carries. */
export function isAndroidSdk(dir, exists = existsSync) {
  if (!dir || !exists(dir)) return false;
  return ["platforms", "build-tools", "platform-tools"].some((sub) => exists(path.join(dir, sub)));
}

/**
 * Makes ANDROID_HOME / ANDROID_SDK_ROOT trustworthy before Gradle reads them. settings.gradle includes the Android
 * modules as soon as either variable is non-empty, and the Android Gradle plugin then rejects a value that points
 * nowhere ("SDK location not found") - which is what a stale variable left over in a long-open window does.
 * A valid SDK replaces both variables; with none valid they are removed so the build says "SDK not found" and skips
 * the Android modules instead of failing half way. runtime/local.properties (sdk.dir) is left to Gradle.
 */
export function sanitizeAndroidSdkEnv(env, options = {}) {
  const exists = options.exists || existsSync;
  const candidates = [env.ANDROID_HOME, env.ANDROID_SDK_ROOT];
  if (env.LOCALAPPDATA) candidates.push(path.join(env.LOCALAPPDATA, "Android", "Sdk"));
  if (env.USERPROFILE) candidates.push(path.join(env.USERPROFILE, "Android", "Sdk"));
  if (env.HOME) candidates.push(path.join(env.HOME, "Android", "Sdk"));
  const valid = candidates.find((dir) => isAndroidSdk(dir, exists));
  if (valid) {
    env.ANDROID_HOME = valid;
    env.ANDROID_SDK_ROOT = valid;
  } else {
    delete env.ANDROID_HOME;
    delete env.ANDROID_SDK_ROOT;
  }
  return valid || null;
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
  sanitizeAndroidSdkEnv(env, { exists });
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
