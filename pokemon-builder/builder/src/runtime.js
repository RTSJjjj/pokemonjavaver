// Build targets and runtime prerequisite detection (Phase 9 / Phase 10).
//
// Read-only: this module only probes the environment and describes the build
// targets. It never writes files, never touches the RMXP project and never
// starts a long running process.
//
// Java detection order (first hit wins):
//   1. a JDK bundled with the builder  -> <builder root>/runtime/java
//   2. JAVA_HOME                       -> %JAVA_HOME%\bin\java.exe
//   3. PATH                            -> java -version
//
// A missing runtime never fails a build on its own: the caller decides what to
// do. build-pc / build-android skip packaging and say so loudly instead of
// pretending a package was produced.

import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";

const PROBE_TIMEOUT_MS = 20000;

// The two Android targets share the generated game data (generated/) but they
// never share a build configuration: the modern build targets API 21+ with
// every ABI. The legacy module is an R13 placeholder (project3 section 50):
// the current libGDX backend forces minSdk 21, so the real API 14 / ARMv7
// work needs the older backend and stays a separate milestone. Both build
// DEBUG APKs for the stage 2 technical preview (no signing setup yet).
export const ANDROID_TARGETS = {
  modern: {
    id: "android",
    command: "build-android",
    label: "Android",
    title: "Modern Android",
    apiLevel: "API 21+",
    minApiLevel: 21,
    abi: ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"],
    runtime: "libGDX Android runtime (Java)",
    gradleModule: "android",
    gradleTask: "assembleDebug",
    artifact: "PokemonGame-Android.apk",
    technicalPreview: true,
  },
  legacy: {
    id: "androidLegacy",
    command: "build-android-legacy",
    label: "Legacy Android",
    title: "Legacy Android",
    apiLevel: "API 21+ placeholder",
    minApiLevel: 21,
    abi: ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"],
    runtime: "libGDX Android runtime (placeholder backend; real API 14 later)",
    gradleModule: "android-legacy",
    gradleTask: "assembleDebug",
    artifact: "PokemonGame-Android-Legacy.apk",
    technicalPreview: true,
  },
};

export function androidTarget(legacy, release = false) {
  const base = legacy ? ANDROID_TARGETS.legacy : ANDROID_TARGETS.modern;
  if (!release || legacy) {
    return base;
  }
  return {
    ...base,
    gradleTask: "assembleRelease",
    artifact: "PokemonGame-Android-release.apk",
    release: true,
  };
}

// One console/log friendly line describing an Android target build.
export function formatTargetStatus(target) {
  return (
    "Android target: " +
    target.title +
    " (" +
    target.apiLevel +
    ", ABIs " +
    target.abi.join(" / ") +
    ", Gradle :" +
    target.gradleModule +
    ":" +
    target.gradleTask +
    " -> dist\\" +
    target.artifact +
    ")"
  );
}

function javaExecutableName() {
  return process.platform === "win32" ? "java.exe" : "java";
}

function gradleWrapperName() {
  return process.platform === "win32" ? "gradlew.bat" : "gradlew";
}

// Runs `<executable> <args>` and returns its combined output, or null when the
// executable is missing, unusable or returns a non-zero exit code.
// `run` is injectable so tests can probe without a real JDK / Gradle installed.
export function probeOutput(executable, args, run = spawnSync) {
  if (!executable) return null;
  let probe;
  try {
    probe = run(executable, args, {
      encoding: "utf8",
      timeout: PROBE_TIMEOUT_MS,
      windowsHide: true,
    });
  } catch (error) {
    return null;
  }
  if (!probe || probe.error || probe.status !== 0) return null;
  return String(probe.stdout || "") + "\n" + String(probe.stderr || "");
}

// `java -version` prints its banner to stderr on most JDKs, for example:
//   openjdk version "17.0.2" 2022-01-18
function javaVersionFromOutput(output) {
  const match =
    /version "([^"]+)"/.exec(output) || /\bversion\s+([0-9][^\s]*)/i.exec(output);
  if (match) return match[1];
  const first = output.split(/\r?\n/).find((line) => line.trim().length > 0);
  return first ? first.trim() : "";
}

// `gradle --version` prints several banners ("Welcome to Gradle 8.5!",
// "Gradle 8.5"), so the whole output is searched for the version token.
function gradleVersionFromOutput(output) {
  const match = /\bGradle\s+([0-9]+(?:\.[0-9]+)*)/i.exec(output);
  return match ? match[1] : "unknown";
}

// Version string of `<executable> -version`, or null when unusable.
export function probeJavaExecutable(executable, run = spawnSync) {
  const output = probeOutput(executable, ["-version"], run);
  if (!output) return null;
  return javaVersionFromOutput(output) || "unknown";
}

export function detectJavaRuntime(builderRoot = process.cwd(), options = {}) {
  const run = typeof options.run === "function" ? options.run : spawnSync;
  const env = options.env || process.env;
  const result = {
    found: false,
    source: null,
    javaHome: "",
    executable: "",
    version: "",
    probed: [],
    skipped: [],
  };

  const candidates = [];

  // 1. A JDK shipped with the builder (offline builds).
  const bundledHome = path.join(builderRoot, "runtime", "java");
  const bundledJava = path.join(bundledHome, "bin", javaExecutableName());
  if (existsSync(bundledJava)) {
    candidates.push({
      source: "bundled",
      home: bundledHome,
      executable: bundledJava,
      label: "bundled runtime/java (" + bundledHome + ")",
    });
  } else {
    result.skipped.push("bundled runtime/java is not installed: " + bundledHome);
  }

  // 2. JAVA_HOME (the usual CI / Android Studio setup). A JAVA_HOME without a
  //    usable java binary is skipped, not reported as a found runtime.
  const javaHome = String(env.JAVA_HOME || "").trim();
  if (!javaHome) {
    result.skipped.push("JAVA_HOME is not set");
  } else {
    const homeJava = path.join(javaHome, "bin", javaExecutableName());
    if (existsSync(homeJava)) {
      candidates.push({
        source: "JAVA_HOME",
        home: javaHome,
        executable: homeJava,
        label: "JAVA_HOME (" + javaHome + ")",
      });
    } else {
      result.skipped.push("JAVA_HOME has no java binary: " + javaHome);
    }
  }

  // 3. java on PATH.
  candidates.push({ source: "PATH", home: "", executable: "java", label: "PATH" });

  for (const candidate of candidates) {
    result.probed.push(candidate.label);
    const version = probeJavaExecutable(candidate.executable, run);
    if (version) {
      return {
        found: true,
        source: candidate.source,
        javaHome: candidate.home,
        executable: candidate.executable,
        version,
        probed: result.probed,
        skipped: result.skipped,
      };
    }
  }
  return result;
}

// Gradle drives the future Android runtime steps. It is looked up as the
// runtime's wrapper first, then on PATH.
export function detectGradle(builderRoot = process.cwd(), options = {}) {
  const run = typeof options.run === "function" ? options.run : spawnSync;
  const result = { found: false, source: null, executable: "", version: "", probed: [], skipped: [] };

  const wrapper = path.join(builderRoot, "runtime", gradleWrapperName());
  if (existsSync(wrapper)) {
    result.probed.push("builder gradlew (" + wrapper + ")");
    const output = probeOutput(wrapper, ["--version"], run);
    if (output) {
      return {
        found: true,
        source: "gradlew",
        executable: wrapper,
        version: gradleVersionFromOutput(output),
        probed: result.probed,
        skipped: result.skipped,
      };
    }
  } else {
    result.skipped.push("no gradlew in the runtime directory: " + wrapper);
  }

  result.probed.push("PATH (gradle)");
  const output = probeOutput("gradle", ["--version"], run);
  if (output) {
    return {
      found: true,
      source: "PATH",
      executable: "gradle",
      version: gradleVersionFromOutput(output),
      probed: result.probed,
      skipped: result.skipped,
    };
  }
  return result;
}

// One console/log friendly line describing the detection result.
export function formatRuntimeStatus(runtime) {
  if (runtime.found) {
    return "Java runtime: found (" + runtime.source + (runtime.version ? ", Java " + runtime.version : "") + ")";
  }
  return "Java runtime: not found (checked " + runtime.probed.join(", ") + ")";
}

export function formatGradleStatus(gradle) {
  if (gradle.found) {
    return "Gradle: found (" + gradle.source + (gradle.version ? ", " + gradle.version : "") + ")";
  }
  return "Gradle: not found (checked " + gradle.probed.join(", ") + ")";
}

/**
 * True when jpackage must bundle {@code javaHome} instead of running jlink:
 * the home has a java binary but no jmods/ directory (a jlink-produced
 * runtime image, e.g. the one shipped with some launchers).
 */
export function runtimeImageNeeded(javaHome, options = {}) {
  if (!javaHome) {
    return false;
  }
  const exists = options.exists || existsSync;
  const java = options.javaExecutable || path.join(javaHome, "bin", javaExecutableName());
  const jmods = options.jmodsDirectory || path.join(javaHome, "jmods");
  return exists(java) && !exists(jmods);
}
