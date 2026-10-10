// Gradle's incremental cache of the Android module can keep an old runtime-data.zip: a slim (--no-data) export after a
// full one then still shipped the whole data pack (or, the other way round, two copies of it). The asset merge
// directories and the old APKs are therefore removed before every Android build; the dex / resource caches stay, so a
// rebuild is still incremental.

import { existsSync, readdirSync, rmSync } from "node:fs";
import path from "node:path";

/**
 * @param {string} androidBuildDir runtime/android/build
 * @returns {string[]} the paths that were removed
 */
export function purgeAndroidAssetCaches(androidBuildDir) {
  const removed = [];
  const drop = (target) => {
    if (existsSync(target)) {
      rmSync(target, { recursive: true, force: true });
      removed.push(target);
    }
  };
  drop(path.join(androidBuildDir, "outputs"));
  const intermediates = path.join(androidBuildDir, "intermediates");
  if (!existsSync(intermediates)) return removed;
  for (const name of readdirSync(intermediates)) {
    if (/assets/i.test(name)) drop(path.join(intermediates, name));
  }
  const incremental = path.join(intermediates, "incremental");
  if (existsSync(incremental)) {
    for (const name of readdirSync(incremental)) {
      if (/assets/i.test(name)) drop(path.join(incremental, name));
    }
  }
  return removed;
}

/** The whole module build directory (the "clean build cache" action). */
export function removeAndroidBuild(androidBuildDir) {
  if (!existsSync(androidBuildDir)) return false;
  rmSync(androidBuildDir, { recursive: true, force: true });
  return true;
}
